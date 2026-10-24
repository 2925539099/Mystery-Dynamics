package com.mysterydynamics.block;

import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.network.chat.MutableComponent;

import net.minecraft.world.level.material.Fluids;
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
        // 按设计默认就是三级热量；水不再作为进入三级的前提。
        int heat = 3;
        if (ModList.get().isLoaded("thaumcraft")) {
            heat += InfernalBoilerCompat.countBellows(boiler);
        }
        if (boiler.fireVisTicks() > 0) {
            heat += 9;
        }
        this.heatLevel = Math.min(18, heat);
        this.activeHeat = heatLevel;
        this.passiveHeat = false;
        this.needsHeatLevelUpdate = false;
    }

    private boolean hasWater(FluidTankBlockEntity tank) {
        return tank.getTankInventory().getFluid().getFluid() == Fluids.WATER
                && tank.getTankInventory().getFluidAmount() > 0;
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
        float deviceMax = ENGINE_STRESS * attachedEngines;
        double configuredCapacity = BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get());
        if (configuredCapacity > 0.0D) {
            deviceMax = (float) configuredCapacity * attachedEngines;
        }
        float target = BASE_TOTAL_STRESS * Math.min(2.0F, heat / 3.0F);
        if (controller.isOverclock()) {
            target *= 2.0F;
        }
        // 上限是“链接的应力输出设备”的最大总输出，而不是固定值。
        target = Math.min(target, deviceMax);
        return Math.min(1.0F, target / deviceMax);
    }
}
