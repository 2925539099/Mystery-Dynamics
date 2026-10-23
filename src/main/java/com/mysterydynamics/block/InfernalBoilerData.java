package com.mysterydynamics.block;

import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

/**
 * 炼狱锅炉的锅炉数据。
 *
 * <p>Create 原版锅炉的应力受“热等级 / 引擎数量”限制；这里把总目标输出
 * 改写为：热等级 3 时 4096 SU，热等级 6 以上 8192 SU，超频再乘 2 并封顶 8192。
 * 每个蒸汽引擎按 1024 SU 计算，因此效率 = 目标 / (1024 × 引擎数)。</p>
 */
public class InfernalBoilerData extends BoilerData {

    public static final float BASE_TOTAL_STRESS = 4096.0F;
    public static final float MAX_TOTAL_STRESS = 8192.0F;

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
        boolean hasWater = hasWater(boiler);
        int heat = hasWater ? 3 : 0;
        if (hasWater && ModList.get().isLoaded("thaumcraft")) {
            heat += InfernalBoilerCompat.countBellows(boiler);
        }
        if (hasWater && boiler.fireVisTicks() > 0) {
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
    public int getTheoreticalHeatLevel() {
        return heatLevel;
    }

    @Override
    public float getEngineEfficiency(int totalTankSize) {
        if (controller == null || attachedEngines <= 0 || !hasWater(controller)) {
            return 0.0F;
        }
        int heat = Math.max(0, activeHeat);
        if (heat <= 0) {
            return 0.0F;
        }
        float target = BASE_TOTAL_STRESS * Math.min(2.0F, heat / 3.0F);
        if (controller.isOverclock()) {
            target *= 2.0F;
        }
        target = Math.min(MAX_TOTAL_STRESS, target);
        return Math.min(1.0F, target / (1024.0F * attachedEngines));
    }
}
