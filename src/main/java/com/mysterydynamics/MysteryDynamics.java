package com.mysterydynamics;

import com.mysterydynamics.block.GolemPowerBlock;
import com.mysterydynamics.block.GolemPowerBlockEntity;
import com.mysterydynamics.block.GolemPowerFrameBlock;
import com.mysterydynamics.block.RedstoneEngineBlock;
import com.mysterydynamics.block.RedstoneEngineBlockEntity;
import com.simibubi.create.api.stress.BlockStressValues;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * 神秘动力学（Mystery Dynamics）。
 *
 * <p>8 台傀儡动力系统（应力源）+ 1 个空框架。转速 / 应力来自「傀儡动力设计.txt」。</p>
 */
@Mod(MysteryDynamics.MODID)
public class MysteryDynamics {

    public static final String MODID = "mystery_dynamics";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ---- 8 台傀儡动力系统：id, 转速 RPM, 总应力 SU ----
    public static final DeferredBlock<GolemPowerBlock> STRAW_GOLEM_POWER =
            registerGolem("straw_golem_power", 160.0F, 128.0F);
    public static final DeferredBlock<GolemPowerBlock> WOOD_GOLEM_POWER =
            registerGolem("wood_golem_power", 128.0F, 256.0F);
    public static final DeferredBlock<GolemPowerBlock> FLESH_GOLEM_POWER =
            registerGolem("flesh_golem_power", 160.0F, 384.0F);
    public static final DeferredBlock<GolemPowerBlock> CLAY_GOLEM_POWER =
            registerGolem("clay_golem_power", 128.0F, 512.0F);
    public static final DeferredBlock<GolemPowerBlock> TALLOW_GOLEM_POWER =
            registerGolem("tallow_golem_power", 256.0F, 128.0F);
    public static final DeferredBlock<GolemPowerBlock> STONE_GOLEM_POWER =
            registerGolem("stone_golem_power", 64.0F, 1024.0F);
    public static final DeferredBlock<GolemPowerBlock> IRON_GOLEM_POWER =
            registerGolem("iron_golem_power", 32.0F, 2048.0F);
    public static final DeferredBlock<GolemPowerBlock> MYSTERY_GOLEM_POWER =
            registerGolem("mystery_golem_power", 128.0F, 4096.0F);

    public static final List<DeferredBlock<GolemPowerBlock>> GENERATORS = List.of(
            STRAW_GOLEM_POWER, WOOD_GOLEM_POWER, FLESH_GOLEM_POWER, CLAY_GOLEM_POWER,
            TALLOW_GOLEM_POWER, STONE_GOLEM_POWER, IRON_GOLEM_POWER, MYSTERY_GOLEM_POWER);

    /** 空框架：纯装饰。 */
    public static final DeferredBlock<GolemPowerFrameBlock> GOLEM_POWER_FRAME = BLOCKS.register(
            "golem_power_frame", () -> new GolemPowerFrameBlock(deviceProperties()));

    /** 红石发动机：红石信号 1~15 → 32~256 RPM、128~8192 SU。 */
    public static final DeferredBlock<RedstoneEngineBlock> REDSTONE_ENGINE = BLOCKS.register(
            "redstone_engine", () -> new RedstoneEngineBlock(deviceProperties()));

    // ---- 物品 ----
    public static final DeferredItem<BlockItem> STRAW_ITEM = ITEMS.registerSimpleBlockItem(STRAW_GOLEM_POWER);
    public static final DeferredItem<BlockItem> WOOD_ITEM = ITEMS.registerSimpleBlockItem(WOOD_GOLEM_POWER);
    public static final DeferredItem<BlockItem> FLESH_ITEM = ITEMS.registerSimpleBlockItem(FLESH_GOLEM_POWER);
    public static final DeferredItem<BlockItem> CLAY_ITEM = ITEMS.registerSimpleBlockItem(CLAY_GOLEM_POWER);
    public static final DeferredItem<BlockItem> TALLOW_ITEM = ITEMS.registerSimpleBlockItem(TALLOW_GOLEM_POWER);
    public static final DeferredItem<BlockItem> STONE_ITEM = ITEMS.registerSimpleBlockItem(STONE_GOLEM_POWER);
    public static final DeferredItem<BlockItem> IRON_ITEM = ITEMS.registerSimpleBlockItem(IRON_GOLEM_POWER);
    public static final DeferredItem<BlockItem> MYSTERY_ITEM = ITEMS.registerSimpleBlockItem(MYSTERY_GOLEM_POWER);
    public static final DeferredItem<BlockItem> FRAME_ITEM = ITEMS.registerSimpleBlockItem(GOLEM_POWER_FRAME);
    public static final DeferredItem<BlockItem> REDSTONE_ENGINE_ITEM = ITEMS.registerSimpleBlockItem(REDSTONE_ENGINE);

    // ---- 方块实体（8 台设备共用） ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemPowerBlockEntity>> GOLEM_POWER_BE =
            BLOCK_ENTITY_TYPES.register("golem_power", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new GolemPowerBlockEntity(blockEntityType(), pos, state),
                    GENERATORS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneEngineBlockEntity>> REDSTONE_ENGINE_BE =
            BLOCK_ENTITY_TYPES.register("redstone_engine", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RedstoneEngineBlockEntity(redstoneEngineType(), pos, state),
                    REDSTONE_ENGINE.get()).build(null));

    /** 独立创造模式标签页。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mystery_dynamics"))
                    .icon(() -> new ItemStack(MYSTERY_ITEM.get()))
                    .displayItems((params, output) -> {
                        GENERATORS.forEach(block -> output.accept(block.get()));
                        output.accept(GOLEM_POWER_FRAME.get());
                        output.accept(REDSTONE_ENGINE.get());
                    })
                    .build());

    public MysteryDynamics(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(MysteryDynamics::commonSetup);
    }

    private static DeferredBlock<GolemPowerBlock> registerGolem(String name, float rpm, float stressUnits) {
        return BLOCKS.register(name, () -> new GolemPowerBlock(deviceProperties(), rpm, stressUnits));
    }

    private static BlockBehaviour.Properties deviceProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                // 与石头一致：硬度 1.5，爆炸抗性 6.0
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                // 模型不是满方块（前后开口），不能按满方块遮挡邻居，
                // 否则贴着设备的方块面会被剔除，出现“透视”破洞
                .noOcclusion();
    }

    private static BlockEntityType<?> blockEntityType() {
        return GOLEM_POWER_BE.get();
    }

    private static BlockEntityType<?> redstoneEngineType() {
        return REDSTONE_ENGINE_BE.get();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (DeferredBlock<GolemPowerBlock> holder : GENERATORS) {
                GolemPowerBlock block = holder.get();
                BlockStressValues.CAPACITIES.register(block, () -> (double) block.capacityPerRpm());
                BlockStressValues.RPM.register(block,
                        new BlockStressValues.GeneratedRpm(Math.round(block.rpm()), false));
            }
            // 红石发动机的容量随信号变化，具体由方块实体覆盖；这里登记满信号值供提示使用
            BlockStressValues.CAPACITIES.register(REDSTONE_ENGINE.get(),
                    () -> (double) (RedstoneEngineBlockEntity.MAX_STRESS / RedstoneEngineBlockEntity.MAX_RPM));
            BlockStressValues.RPM.register(REDSTONE_ENGINE.get(),
                    new BlockStressValues.GeneratedRpm(Math.round(RedstoneEngineBlockEntity.MAX_RPM), false));
            // 安装了神秘时代时才注册奥术工作台配方（未安装时不加载 compat 类）
            if (net.neoforged.fml.ModList.get().isLoaded("thaumcraft")) {
                com.mysterydynamics.compat.ThaumcraftRecipes.register();
            }
        });
    }
}
