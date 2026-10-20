package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * 与神秘时代（port.265+）的奥术工作台配方对接。
 *
 * <p>只有在检测到 thaumcraft 模组时才会加载本类，避免未安装时出现类缺失。</p>
 */
public final class ThaumcraftRecipes {

    private ThaumcraftRecipes() {
    }

    public static void register() {
        ItemStack frame = new ItemStack(MysteryDynamics.GOLEM_POWER_FRAME.get());
        ItemStack thaumium = stack("thaumcraft", "thaumium_ingot");
        ItemStack cogwheel = stack("create", "cogwheel");
        ItemStack casing = stack("create", "andesite_casing");
        ItemStack strawGolem = stack("thaumcraft", "golem_straw");
        ItemStack woodGolem = stack("thaumcraft", "golem_wood");
        ItemStack stick = new ItemStack(Items.STICK);

        // 空傀儡动力系统框架：三神秘锭 + 齿轮/安山机壳/齿轮，六种元要素各 5 点
        ThaumcraftApi.addArcaneCraftingRecipe(
                "STRAW_GOLEM_POWER",
                frame,
                new AspectList()
                        .add(Aspect.AIR, 5)
                        .add(Aspect.EARTH, 5)
                        .add(Aspect.FIRE, 5)
                        .add(Aspect.WATER, 5)
                        .add(Aspect.ORDER, 5)
                        .add(Aspect.ENTROPY, 5),
                "TTT", "GCG",
                'T', thaumium,
                'G', cogwheel,
                'C', casing);

        // 稻草傀儡动力系统：框架 / 稻草傀儡 / 木棍，水 3 + 地 6
        ThaumcraftApi.addArcaneCraftingRecipe(
                "STRAW_GOLEM_POWER",
                new ItemStack(MysteryDynamics.STRAW_GOLEM_POWER.get()),
                new AspectList().add(Aspect.WATER, 3).add(Aspect.EARTH, 6),
                "F", "S", "K",
                'F', frame,
                'S', strawGolem,
                'K', stick);

        // 木头傀儡动力系统：框架 / 木头傀儡，水 3 + 地 6 + 风 3
        ThaumcraftApi.addArcaneCraftingRecipe(
                "WOOD_GOLEM_POWER",
                new ItemStack(MysteryDynamics.WOOD_GOLEM_POWER.get()),
                new AspectList().add(Aspect.WATER, 3).add(Aspect.EARTH, 6).add(Aspect.AIR, 3),
                "F", "W",
                'F', frame,
                'W', woodGolem);
    }

    private static ItemStack stack(String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return new ItemStack(item);
    }
}
