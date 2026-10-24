package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlock;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import com.mysterydynamics.block.InfernalBoilerFaceBlock;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.TCBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * 炼狱锅炉结构。
 *
 * <p>以守卫者玻璃所在的面作为正面，支持四个水平朝向。
 * 局部坐标以 4×4 底座的西北角为原点，正面玻璃在局部 (1,1,3)。</p>
 */
public final class InfernalBoilerStructure {

    private InfernalBoilerStructure() {
    }

    private static final ThreadLocal<Boolean> RESTORING = ThreadLocal.withInitial(() -> false);

    private static final Block[][] LAYER_0 = {
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
            {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
            {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
    };

    private static final Block[][] LAYER_1 = {
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
            {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ALCHEMICAL_CONSTRUCT.get(), Blocks.OBSIDIAN},
            {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ARCANE_FURNACE.get(), Blocks.OBSIDIAN},
            {Blocks.NETHER_BRICKS, TCBlocks.WARDED_GLASS.get(), Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
    };

    private static final Block[][] LAYER_2 = {
            {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.NETHER_BRICKS},
            {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
            {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
    };

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

    /** 失败时给出最接近朝向的具体错位方块，方便排查。 */
    public static String describeMismatch(Level level, BlockPos clicked) {
        BlockPos glass = nearbyGlass(level, clicked);
        if (glass == null) {
            return "未找到守卫者玻璃";
        }
        int bestRotation = -1;
        int bestCount = Integer.MAX_VALUE;
        List<String> bestDetails = List.of();
        for (int rotation = 0; rotation < 4; rotation++) {
            int gx = rotateX(1, 3, rotation);
            int gz = rotateZ(1, 3, rotation);
            BlockPos origin = glass.offset(-gx, -1, -gz);
            List<String> details = new ArrayList<>();
            int count = 0;
            for (int y = 0; y <= 4; y++) {
                for (int x = 0; x <= 3; x++) {
                    for (int z = 0; z <= 3; z++) {
                        Block expected = expectedAt(x, y, z);
                        if (expected == null) {
                            continue;
                        }
                        BlockState actual = level.getBlockState(world(origin, x, y, z, rotation));
                        if (!accepted(expected, actual)) {
                            count++;
                            if (details.size() < 4) {
                                details.add("(" + x + "," + y + "," + z + ") 期望 "
                                        + BuiltInRegistries.BLOCK.getKey(expected).getPath()
                                        + "，实际 "
                                        + BuiltInRegistries.BLOCK.getKey(actual.getBlock()).getPath());
                            }
                        }
                    }
                }
            }
            if (count < bestCount) {
                bestCount = count;
                bestRotation = rotation;
                bestDetails = details;
            }
        }
        if (bestCount == 0) {
            return "结构匹配，但成型被其他条件阻止";
        }
        StringBuilder message = new StringBuilder("结构未匹配：错误 ")
                .append(bestCount).append(" 处，检测朝向 ").append(facingFor(bestRotation));
        for (String detail : bestDetails) {
            message.append("；").append(detail);
        }
        return message.toString();
    }

    private static BlockPos nearbyGlass(Level level, BlockPos clicked) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos candidate = clicked.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).is(TCBlocks.WARDED_GLASS.get())) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static Block expectedAt(int x, int y, int z) {
        if (y == 0) {
            return LAYER_0[z][x];
        }
        if (y == 1) {
            return LAYER_1[z][x];
        }
        if (y == 2) {
            return LAYER_2[z][x];
        }
        if (y <= 4 && x <= 2 && z <= 2) {
            return TCBlocks.THAUMIUM_BLOCK.get();
        }
        return null;
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
        return matchesLayer(level, origin, rotation, 0, LAYER_0)
                && matchesLayer(level, origin, rotation, 1, LAYER_1)
                && matchesLayer(level, origin, rotation, 2, LAYER_2)
                && matchesBody(level, origin, rotation);
    }

    private static boolean matchesLayer(Level level, BlockPos origin, int rotation, int y, Block[][] pattern) {
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                Block expected = pattern[z][x];
                BlockState actual = level.getBlockState(world(origin, x, y, z, rotation));
                if (accepted(expected, actual)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private static boolean accepted(Block expected, BlockState actual) {
        if (actual.is(expected)) {
            return true;
        }
        // 神秘时代里“炼金炉”实际注册名是 alchemical_furnace；
        // 同时兼容奥术熔炉核心、炼金炉外壳等不同摆放形式。
        if (expected == TCBlocks.ARCANE_FURNACE.get()
                && (actual.is(TCBlocks.ALCHEMICAL_FURNACE.get())
                || actual.is(TCBlocks.ADVANCED_ALCHEMICAL_FURNACE.get())
                || actual.is(TCBlocks.ARCANE_FURNACE.get())
                || actual.is(TCBlocks.ARCANE_FURNACE_CASING.get()))) {
            return true;
        }
        // 允许使用高级炼金构材。
        return expected == TCBlocks.ALCHEMICAL_CONSTRUCT.get()
                && actual.is(TCBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get());
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
        RESTORING.set(true);

        // 成型后整个 4×4×2 底座换成不可见填充块，由 shell 模型绘制。
        for (int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    level.setBlock(world(origin, x, y, z, rotation),
                            MysteryDynamics.INFERNAL_BOILER_CASING.get().defaultBlockState(), 3);
                }
            }
        }

        // 第三层的 4×4 外圈也是多结构的一部分，同样交给 shell；
        // 中心 3×3 随后会被锅炉主体覆盖。
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 3; z++) {
                level.setBlock(world(origin, x, 2, z, rotation),
                        MysteryDynamics.INFERNAL_BOILER_CASING.get().defaultBlockState(), 3);
            }
        }

        // 先放中心，让 Create 的连通性把 controller 选在中心附近
        level.setBlock(center, MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState()
                .setValue(InfernalBoilerBlock.FORMED, true), 3);
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    if (x == 1 && y == 3 && z == 1) {
                        continue;
                    }
                    level.setBlock(world(origin, x, y, z, rotation),
                            MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState()
                                    .setValue(InfernalBoilerBlock.FORMED, true), 3);
                }
            }
        }

        // 整栋模型承载方块放在底座西北角，模型从该位置向外延伸 4×4×5。
        level.setBlock(origin, MysteryDynamics.INFERNAL_BOILER_SHELL.get().defaultBlockState(), 3);

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
        RESTORING.set(false);
        return true;
    }

    /** 破坏成型锅炉时恢复成建造时的五层方块。 */
    public static void dismantle(Level level, BlockPos origin, int rotation) {
        if (level.isClientSide || isRestoring()) {
            return;
        }
        RESTORING.set(true);
        try {
            for (int y = 0; y <= 2; y++) {
                Block[][] layer = y == 0 ? LAYER_0 : y == 1 ? LAYER_1 : LAYER_2;
                for (int x = 0; x <= 3; x++) {
                    for (int z = 0; z <= 3; z++) {
                        Block block = layer[z][x];
                        BlockState restored = block == TCBlocks.ARCANE_FURNACE.get()
                                ? TCBlocks.ALCHEMICAL_FURNACE.get().defaultBlockState()
                                : block.defaultBlockState();
                        level.setBlock(world(origin, x, y, z, rotation), restored, 3);
                    }
                }
            }
            for (int y = 2; y <= 4; y++) {
                for (int x = 0; x <= 2; x++) {
                    for (int z = 0; z <= 2; z++) {
                        level.setBlock(world(origin, x, y, z, rotation),
                                TCBlocks.THAUMIUM_BLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
        } finally {
            RESTORING.set(false);
        }
    }

    public static boolean isRestoring() {
        return RESTORING.get();
    }

    /** 在任意已成型方块被破坏时请求恢复整栋结构。 */
    public static void requestDismantle(Level level, BlockPos around) {
        if (level.isClientSide || isRestoring() || level.getServer() == null) {
            return;
        }
        InfernalBoilerBlockEntity controller = findController(level, around);
        if (controller == null) {
            return;
        }
        BlockPos origin = controller.boilerOrigin();
        int rotation = controller.rotation();
        if (origin.equals(BlockPos.ZERO)) {
            return;
        }
        level.getServer().execute(() -> dismantle(level, origin, rotation));
    }

    private static InfernalBoilerBlockEntity findController(Level level, BlockPos around) {
        for (int dx = -3; dx <= 4; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -3; dz <= 4; dz++) {
                    if (level.getBlockEntity(around.offset(dx, dy, dz))
                            instanceof InfernalBoilerBlockEntity boiler
                            && boiler.isController()
                            && !boiler.boilerOrigin().equals(BlockPos.ZERO)) {
                        return boiler;
                    }
                }
            }
        }
        return null;
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
