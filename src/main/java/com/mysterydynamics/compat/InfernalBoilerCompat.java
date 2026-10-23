package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.blocks.BellowsBlock;
import thaumcraft.common.registry.TCBlocks;
import thaumcraft.common.vis.TCVisHelper;

/** 只在安装神秘时代时加载的兼容层。 */
public final class InfernalBoilerCompat {

    private InfernalBoilerCompat() {
    }

    public static void register() {
        WandTriggerRegistry.registerWandBlockTrigger(new InfernalBoilerTrigger(), 0,
                TCBlocks.WARDED_GLASS.get());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT,
                MysteryDynamics.INFERNAL_BOILER_BE.get(),
                (be, side) -> new InfernalBoilerEssentiaHandler(be, side));
    }

    /** 尝试从附近节点/中继器抽取 1 点 Ignis，给锅炉 200 tick 的 +9 热等级。 */
    public static void tickVis(InfernalBoilerBlockEntity boiler) {
        Level level = boiler.getLevel();
        if (level == null || level.isClientSide || boiler.fireVisTicks() > 0) {
            return;
        }
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        int drained = TCVisHelper.drainFromNearbyEnergizedNodes(level, boiler.getBlockPos(), Aspect.FIRE, 1);
        if (drained <= 0) {
            drained = TCVisHelper.drainFromNearbyRelays(level, boiler.getBlockPos(), Aspect.FIRE, 1,
                    TCVisHelper.newVisited(boiler.getBlockPos()));
        }
        if (drained > 0) {
            boiler.setFireVisTicks(200);
        }
    }

    /** 东侧三格内朝西的未通红石风箱，每个 +2 热等级，最多 3 个。 */
    public static int countBellows(InfernalBoilerBlockEntity boiler) {
        Level level = boiler.getLevel();
        if (level == null || boiler.boilerOrigin().equals(net.minecraft.core.BlockPos.ZERO)) {
            return 0;
        }
        int count = 0;
        for (int y = 2; y <= 4 && count < 3; y++) {
            for (int z = 0; z <= 2 && count < 3; z++) {
                var pos = boiler.boilerOrigin().offset(3, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BellowsBlock
                        && state.getValue(BellowsBlock.FACING) == Direction.WEST
                        && !level.hasNeighborSignal(pos)) {
                    count++;
                }
            }
        }
        return count * 2;
    }
}
