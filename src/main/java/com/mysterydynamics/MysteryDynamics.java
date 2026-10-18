package com.mysterydynamics;

import com.mysterydynamics.block.GolemDynamoBlock;
import com.mysterydynamics.block.GolemDynamoBlockEntity;
import com.simibubi.create.api.stress.BlockStressValues;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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

@Mod(MysteryDynamics.MODID)
public class MysteryDynamics {

    public static final String MODID = "mystery_dynamics";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<GolemDynamoBlock> GOLEM_DYNAMO = BLOCKS.registerBlock(
            "golem_dynamo",
            GolemDynamoBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static final DeferredItem<BlockItem> GOLEM_DYNAMO_ITEM =
            ITEMS.registerSimpleBlockItem(GOLEM_DYNAMO);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemDynamoBlockEntity>> GOLEM_DYNAMO_BE =
            BLOCK_ENTITY_TYPES.register("golem_dynamo",
                    () -> BlockEntityType.Builder.of(
                            // 用方法间接取类型，避免静态初始化器的自引用
                            (pos, state) -> new GolemDynamoBlockEntity(blockEntityType(), pos, state),
                            GOLEM_DYNAMO.get()).build(null));

    private static BlockEntityType<?> blockEntityType() {
        return GOLEM_DYNAMO_BE.get();
    }

    /** 独立创造模式标签页，图标就是这台傀儡动力核心。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mystery_dynamics"))
                    .icon(() -> new ItemStack(GOLEM_DYNAMO_ITEM.get()))
                    .displayItems((params, output) -> output.accept(GOLEM_DYNAMO_ITEM.get()))
                    .build());

    public MysteryDynamics(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(MysteryDynamics::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 每 RPM 的容量：16 * 32 RPM = 512 SU
            BlockStressValues.CAPACITIES.register(GOLEM_DYNAMO.get(),
                    () -> (double) GolemDynamoBlockEntity.CAPACITY_PER_RPM);
            // 仅用于护目镜提示；真实转速由 BlockEntity#getGeneratedSpeed 决定
            BlockStressValues.RPM.register(GOLEM_DYNAMO.get(),
                    new BlockStressValues.GeneratedRpm((int) GolemDynamoBlockEntity.GENERATED_RPM, false));
        });
    }
}
