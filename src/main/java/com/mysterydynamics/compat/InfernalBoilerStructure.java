package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import com.mysterydynamics.block.InfernalBoilerFaceBlock;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.TCBlocks;

/**
 * 炼狱锅炉结构。
 *
 * <p>以守卫者玻璃所在的面作为正面，支持四个水平朝向。
 * 局部坐标以 4×4 底座的西北角为原点，正面玻璃在局部 (1,1,3)。</p>
 */
public final class InfernalBoilerStructure {

    private InfernalBoilerStructure() {
    }

    public record Match(BlockPos origin, BlockPos glass, int rotation, Direction facing) {
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

    public static boolean matches(Level level, BlockPos glassPos) {
        return findMatch(level, glassPos) != null;
    }

    /** 依次尝试四个朝向，返回第一个匹配的结构。 */
    public static Match findMatch(Level level, BlockPos glassPos) {
        if (!level.getBlockState(glassPos).is(TCBlocks.WARDED_GLASS.get())) {
            return null;
        }
        for (int rotation = 0; rotation < 4; rotation++) {
            int gx = rotateX(1, 3, rotation);
            int gz = rotateZ(1, 3, rotation);
            BlockPos origin = glassPos.offset(-gx, -1, -gz);
            if (matchesAll(level, origin, rotation)) {
                return new Match(origin, glassPos, rotation, facingFor(rotation));
            }
        }
        return null;
    }

    private static boolean matchesAll(Level level, BlockPos origin, int rotation) {
        return matchesLayer(level, origin, rotation, 0, new Block[][]{
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
                {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
                {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
        }) && matchesLayer(level, origin, rotation, 1, new Block[][]{
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
                {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ALCHEMICAL_CONSTRUCT.get(), Blocks.OBSIDIAN},
                {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ARCANE_FURNACE.get(), Blocks.OBSIDIAN},
                {Blocks.NETHER_BRICKS, TCBlocks.WARDED_GLASS.get(), Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
        }) && matchesLayer(level, origin, rotation, 2, new Block[][]{
                {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.NETHER_BRICKS},
                {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
                {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
                {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
        }) && matchesBody(level, origin, rotation);
    }

    private static boolean matchesLayer(Level level, BlockPos origin, int rotation, int y, Block[][] pattern) {
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                if (!level.getBlockState(world(origin, x, y, z, rotation)).is(pattern[z][x])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchesBody(Level level, BlockPos origin, int rotation) {
        for (int y = 3; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    if (!level.getBlockState(world(origin, x, y, z, rotation))
                            .is(TCBlocks.THAUMIUM_BLOCK.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean form(Level level, BlockPos glassPos) {
        Match match = findMatch(level, glassPos);
        if (match == null) {
            return false;
        }
        BlockPos origin = match.origin();
        int rotation = match.rotation();
        BlockPos center = world(origin, 1, 3, 1, rotation);
        BlockPos face = match.glass();

        // 先放中心，让 Create 的连通性把 controller 选在中心附近
        level.setBlock(center, MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState(), 3);
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    if (x == 1 && y == 3 && z == 1) {
                        continue;
                    }
                    level.setBlock(world(origin, x, y, z, rotation),
                            MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState(), 3);
                }
            }
        }

        BlockState faceState = MysteryDynamics.INFERNAL_BOILER_FACE.get().defaultBlockState()
                .setValue(InfernalBoilerFaceBlock.FACING, match.facing());
        level.setBlock(face, faceState, 3);

        if (level.getBlockEntity(center) instanceof InfernalBoilerBlockEntity centerBoiler) {
            FluidTankBlockEntity controller = centerBoiler.getControllerBE();
            if (controller == null) {
                ConnectivityHandler.formMulti(centerBoiler);
                controller = centerBoiler.getControllerBE();
            }
            if (controller instanceof InfernalBoilerBlockEntity boiler) {
                boiler.setFormation(origin, face, match.facing(), rotation);
            } else {
                centerBoiler.setFormation(origin, face, match.facing(), rotation);
            }
        }

        BlockState centerState = level.getBlockState(center);
        FluidTankBlock.updateBoilerState(centerState, level, center);
        level.updateNeighborsAt(face, MysteryDynamics.INFERNAL_BOILER_FACE.get());
        return true;
    }

    /** 局部坐标 (x,y,z) 旋转到世界坐标。rotation 0 为正面朝南。 */
    public static BlockPos world(BlockPos origin, int x, int y, int z, int rotation) {
        return origin.offset(rotateX(x, z, rotation), y, rotateZ(x, z, rotation));
    }

    private static int rotateX(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> z;
            case 2 -> 3 - x;
            case 3 -> 3 - z;
            default -> x;
        };
    }

    private static int rotateZ(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> 3 - x;
            case 2 -> 3 - z;
            case 3 -> x;
            default -> z;
        };
    }

    public static Direction facingFor(int rotation) {
        return switch (rotation) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.WEST;
            default -> Direction.SOUTH;
        };
    }
}
