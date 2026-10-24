package com.mysterydynamics.block;

import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.network.chat.MutableComponent;

import net.neoforged.fml.ModList;

/**
 * 炼狱锅炉的锅炉数据。
 *
 * <p>Create 原版锅炉的应力受“热等级 / 引擎数量”限制；这里把目标输出
 * 改写为：热等级 3 时 4096 SU，热等级 6 以上 8192 SU，超频再乘 2。
 * 最终值不得超过已链接蒸汽引擎的最大总输出（每台 1024 SU），
 * 因此效率 = min(目标, 引擎数 × 1024) / (引擎数 × 1024)。</p>
 */
public class InfernalBoilerData extends BoilerData {

    public static final float BASE_TOTAL_STRESS = 4096.0F;
    /** Create 蒸汽引擎单台最大应力输出。 */
    public static final float ENGINE_STRESS = 1024.0F;
    /** 原版满级锅炉护目镜显示：18 热 × 16 × 1024 = 294912 SU。 */
    public static final float VANILLA_MAX_BOILER_STRESS = 18.0F * 16.0F * ENGINE_STRESS;
    /** 超频时按原版满级锅炉最大应力的两倍计算。 */
    public static final float OVERCLOCK_TOTAL_STRESS = VANILLA_MAX_BOILER_STRESS * 2.0F;

    private InfernalBoilerBlockEntity controller;
    private int heatLevel;

    @Override
    public void tick(FluidTankBlockEntity tank) {
        super.tick(tank);
        if (!(tank instanceof InfernalBoilerBlockEntity boiler) || !tank.isController()) {
            return;
        }
        this.controller = boiler;
        updateCustomHeat(boiler);
        // 只在服务端写引擎数；客户端使用同步下来的值。
        // 否则客户端 linkedEngineCount 永远是0，护目镜会在0和真实值之间闪烁。
        if (boiler.getLevel() != null && !boiler.getLevel().isClientSide) {
            this.attachedEngines = boiler.linkedEngineCount();
        }
    }

    @Override
    public boolean updateTemperature(FluidTankBlockEntity tank) {
        if (!(tank instanceof InfernalBoilerBlockEntity boiler)) {
            return super.updateTemperature(tank);
        }
        int before = activeHeat;
        updateCustomHeat(boiler);
        return before != activeHeat;
    }

    private void updateCustomHeat(InfernalBoilerBlockEntity boiler) {
        int heat;
        if (boiler.isOverclock()) {
            // 超频按原版满级锅炉的 18 热计算，才能显示/输出两倍最大应力。
            heat = 18;
        } else {
            // 按设计默认就是三级热量；水不再作为进入三级的前提。
            heat = 3;
            if (ModList.get().isLoaded("thaumcraft")) {
                heat += InfernalBoilerCompat.countBellows(boiler);
            }
            if (boiler.fireVisTicks() > 0) {
                heat += 9;
            }
        }
        this.heatLevel = Math.min(18, heat);
        this.activeHeat = heatLevel;
        this.passiveHeat = false;
        this.needsHeatLevelUpdate = false;
    }

    private boolean hasWater(FluidTankBlockEntity tank) {
        // 有任意储量（正常情况就是水）或仍有管道供水，都允许工作。
        // 完全空仓且没有 waterSupply 时才停机。
        return tank.getTankInventory().getFluidAmount() > 0
                || (tank.boiler != null && tank.boiler.waterSupply > 0.0F);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean isPassive(int attachedEngines) {
        return false;
    }

    @Override
    public MutableComponent getHeatLevelTextComponent() {
        int heat = Math.min(18, Math.max(0, activeHeat));
        if (isPassive()) {
            return CreateLang.translateDirect("boiler.passive");
        }
        if (heat == 0) {
            return CreateLang.translateDirect("boiler.idle");
        }
        if (heat == 18) {
            return CreateLang.translateDirect("boiler.max_lvl");
        }
        return CreateLang.translateDirect("boiler.lvl", String.valueOf(heat));
    }

    @Override
    public int getTheoreticalHeatLevel() {
        return heatLevel;
    }

    @Override
    public float getEngineEfficiency(int totalTankSize) {
        if (controller == null || attachedEngines <= 0) {
            return 0.0F;
        }
        int heat = Math.max(0, activeHeat);
        if (heat <= 0) {
            return 0.0F;
        }
        if (!hasWater(controller)) {
            return 0.0F;
        }
        float deviceMax = ENGINE_STRESS * attachedEngines;
        double configuredCapacity = BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get());
        if (configuredCapacity > 0.0D) {
            deviceMax = (float) configuredCapacity * attachedEngines;
        }
        float capacity = configuredCapacity > 0.0D ? (float) configuredCapacity : ENGINE_STRESS;
        float target = BASE_TOTAL_STRESS * Math.min(2.0F, heat / 3.0F);
        if (controller.isOverclock()) {
            // 超频硬上限 589824：引擎多于满级18台时自动降低单台效率，
            // 保证护目镜公式 eff * 16 * max(heat, engines) * capacity 不超过两倍原版满级。
            int effectiveCount = Math.max(heat, attachedEngines);
            float maxEfficiency = OVERCLOCK_TOTAL_STRESS
                    / (16.0F * effectiveCount * capacity);
            return Math.min(2.0F, maxEfficiency);
        }
        // 上限是“链接的应力输出设备”的最大总输出，而不是固定值。
        target = Math.min(target, deviceMax);
        return Math.min(1.0F, target / deviceMax);
    }
}
