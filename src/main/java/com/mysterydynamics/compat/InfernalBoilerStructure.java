package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.TCBlocks;

/**
 * 炼狱锅炉结构。
 *
 * <pre>
 * 以正面守卫者玻璃为基准：
 *   glass = origin + (1,1,3)
 *   y0: 4×4 底层
 *   y1: 4×4 炉膛层
 *   y2: 4×4，中心 3×3 为神秘方块，边缘为下界砖/黑曜石
 *   y3/4: 3×3 神秘方块
 * </pre>
 */
public final class InfernalBoilerStructure {

    private InfernalBoilerStructure() {
    }

    /** 从任意一个结构方块位置找出对应的正面玻璃位置。 */
    public static BlockPos findGlass(Level level, BlockPos clicked) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos candidate = clicked.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).is(TCBlocks.WARDED_GLASS.get())
                            && matches(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static boolean matches(Level level, BlockPos glassPos) {        if (!level.getBlockState(glassPos).is(TCBlocks.WARDED_GLASS.get())) {
            return false;
        }
        BlockPos origin = glassPos.offset(-1, -1, -3);
        return matchesLayer(level, origin, 0, new Block[][]{
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
                {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
                {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
        }) && matchesLayer(level, origin, 1, new Block[][]{
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
                {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ALCHEMICAL_CONSTRUCT.get(), Blocks.OBSIDIAN},
                {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ARCANE_FURNACE.get(), Blocks.OBSIDIAN},
                {Blocks.NETHER_BRICKS, TCBlocks.WARDED_GLASS.get(), Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
        }) && matchesLayer(level, origin, 2, new Block[][]{
                {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.NETHER_BRICKS},
                {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
                {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
        }) && matchesBody(level, origin);
    }

    private static boolean matchesLayer(Level level, BlockPos origin, int y, Block[][] pattern) {
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (!level.getBlockState(origin.offset(x, y, z)).is(pattern[z][x])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchesBody(Level level, BlockPos origin) {
        for (int y = 3; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    if (!level.getBlockState(origin.offset(x, y, z)).is(TCBlocks.THAUMIUM_BLOCK.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean form(Level level, BlockPos glassPos) {
        if (!matches(level, glassPos)) {
            return false;
        }
        BlockPos origin = glassPos.offset(-1, -1, -3);
        BlockPos center = origin.offset(1, 3, 1);
        BlockPos face = glassPos;

        // 先放中心，让 Create 的连通性把 controller 选在中心附近
        level.setBlock(center, MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState(), 3);
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    if (x == 1 && y == 3 && z == 1) {
                        continue;
                    }
                    level.setBlock(origin.offset(x, y, z),
                            MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState(), 3);
                }
            }
        }

        level.setBlock(face, MysteryDynamics.INFERNAL_BOILER_FACE.get().defaultBlockState(), 3);

        if (level.getBlockEntity(center) instanceof InfernalBoilerBlockEntity centerBoiler) {
            FluidTankBlockEntity controller = centerBoiler.getControllerBE();
            if (controller == null) {
                ConnectivityHandler.formMulti(centerBoiler);
                controller = centerBoiler.getControllerBE();
            }
            if (controller instanceof InfernalBoilerBlockEntity boiler) {
                boiler.setFormation(origin, face);
            } else {
                centerBoiler.setFormation(origin, face);
            }
        }

        BlockState centerState = level.getBlockState(center);
        FluidTankBlock.updateBoilerState(centerState, level, center);
        level.updateNeighborsAt(face, MysteryDynamics.INFERNAL_BOILER_FACE.get());
        return true;
    }
}
