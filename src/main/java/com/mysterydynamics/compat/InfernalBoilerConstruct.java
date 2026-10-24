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
        int width = 4;
        int height = 5;
        int depth = 4;
        List<ItemStack> blocks = new ArrayList<>(width * height * depth);
        // ConstructDisplay 的 y 轴向下画；所以列表从顶层开始倒序加入，
        // 页面里才是底座在下、炉顶在上，而不是整栋倒过来。
        for (int y = height - 1; y >= 0; y--) {
            for (int z = depth - 1; z >= 0; z--) {
                for (int x = width - 1; x >= 0; x--) {
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
        return new ResearchBookApi.Construct(cost, List.copyOf(blocks), width, height, depth);
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

