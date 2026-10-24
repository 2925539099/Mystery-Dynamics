package com.mysterydynamics.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchBookApi;
import thaumcraft.common.registry.TCBlocks;

import java.util.ArrayList;
import java.util.List;

/** Thaumonomicon 澶氭柟鍧楃粨鏋勯〉锛氱偧鐙遍攨鐐?4脳4脳5銆?*/
public final class InfernalBoilerConstruct {

    private InfernalBoilerConstruct() {
    }

    public static ResearchBookApi.Construct create() {
        // 4×4×5 在 3×3 的展示格子里会挤在一起。这里改用 7×5×7 的展示网格：
        // 真实方块放在偶数格，中间留空，间隔翻倍；展示顺序与 ConstructDisplay 一致。
        int width = 7;
        int height = 5;
        int depth = 7;
        List<ItemStack> blocks = new ArrayList<>(width * height * depth);
        for (int y = 0; y < height; y++) {
            for (int dz = depth - 1; dz >= 0; dz--) {
                for (int dx = width - 1; dx >= 0; dx--) {
                    if ((dx & 1) != 0 || (dz & 1) != 0) {
                        blocks.add(null);
                        continue;
                    }
                    int x = dx / 2;
                    // 页面当前是前后反的，翻转 z；x 保持。
                    int z = 3 - dz / 2;
                    blocks.add(stackAt(x, y, z));
                }
            }
        }

        AspectList cost = new AspectList()
                .add(Aspect.FIRE, 100)
                .add(Aspect.EARTH, 100)
                .add(Aspect.AIR, 30)
                .add(Aspect.ENTROPY, 30)
                .add(Aspect.WATER, 30)
                .add(Aspect.ORDER, 30);
        return new ResearchBookApi.Construct(cost, blocks, width, height, depth);
    }

    private static ItemStack stackAt(int x, int y, int z) {
        if (y == 0) {
            return switch (z) {
                case 0, 3 -> (x == 0 || x == 3)
                        ? new ItemStack(Blocks.NETHER_BRICKS)
                        : new ItemStack(Blocks.OBSIDIAN);
                default -> new ItemStack(Blocks.OBSIDIAN);
            };
        }
        if (y == 1) {
            if (z == 3) {
                return switch (x) {
                    case 0, 3 -> new ItemStack(Blocks.NETHER_BRICKS);
                    case 1 -> new ItemStack(TCBlocks.WARDED_GLASS.get());
                    default -> new ItemStack(Blocks.OBSIDIAN);
                };
            }
            if (z == 2 && x == 2) {
                return new ItemStack(TCBlocks.ALCHEMICAL_FURNACE.get());
            }
            if (z == 1 && x == 2) {
                return new ItemStack(TCBlocks.ALCHEMICAL_CONSTRUCT.get());
            }
            if (z == 1 || z == 2) {
                if (x == 1) {
                    return new ItemStack(Items.LAVA_BUCKET);
                }
                if (x == 0 || x == 3) {
                    return new ItemStack(Blocks.OBSIDIAN);
                }
            }
            return (x == 0 || x == 3) && (z == 0 || z == 3)
                    ? new ItemStack(Blocks.NETHER_BRICKS)
                    : new ItemStack(Blocks.OBSIDIAN);
        }
        if (y == 2) {
            if (x <= 2 && z <= 2) {
                return new ItemStack(TCBlocks.THAUMIUM_BLOCK.get());
            }
            if (x == 3 && z == 0 || z == 3 && (x == 0 || x == 3)) {
                return new ItemStack(Blocks.NETHER_BRICKS);
            }
            return new ItemStack(Blocks.OBSIDIAN);
        }
        if (x <= 2 && z <= 2) {
            return new ItemStack(TCBlocks.THAUMIUM_BLOCK.get());
        }
        return ItemStack.EMPTY;
    }
}

