package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.blocks.BellowsBlock;
import thaumcraft.common.items.TCFunctionalItems;
import thaumcraft.common.registry.TCBlocks;
import thaumcraft.common.vis.TCVisHelper;

/** 只在安装神秘时代时加载的兼容层。 */
public final class InfernalBoilerCompat {

    private InfernalBoilerCompat() {
    }

    public static void register() {
        // 不只在玻璃上：点击任意结构方块都能触发成型。
        WandTriggerRegistry.registerWandBlockTrigger(new InfernalBoilerTrigger(), 0,
                InfernalBoilerCompat::isStructureBlock,
                "infernal_boiler_structure");

        // 守卫者玻璃的 useItemOn 会拦截法杖，所以额外在交互事件里处理所有结构方块。
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true,
                InfernalBoilerCompat::onRightClickBlock);
    }

    private static boolean isStructureBlock(BlockState state) {
        return state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.NETHER_BRICKS)
                || state.is(TCBlocks.THAUMIUM_BLOCK.get())
                || state.is(TCBlocks.ALCHEMICAL_CONSTRUCT.get())
                || state.is(TCBlocks.ARCANE_FURNACE.get())
                || state.is(TCBlocks.WARDED_GLASS.get());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof TCFunctionalItems.WandCastingItem)) {
            return;
        }
        if (!isStructureBlock(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        Direction face = event.getFace();
        if (new InfernalBoilerTrigger().performTrigger(event.getLevel(), stack, player,
                event.getPos(), face == null ? Direction.UP : face, 0)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
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

    /** 锅炉模型东侧、朝锅炉、未通红石的风箱，每个 +2 热等级，最多 3 个。 */
    public static int countBellows(InfernalBoilerBlockEntity boiler) {
        Level level = boiler.getLevel();
        if (level == null || boiler.boilerOrigin().equals(net.minecraft.core.BlockPos.ZERO)) {
            return 0;
        }
        Direction front = boiler.frontDirection();
        Direction side = front.getCounterClockWise();
        int count = 0;
        for (int y = 2; y <= 4 && count < 3; y++) {
            for (int z = 0; z <= 2 && count < 3; z++) {
                var bodyPos = InfernalBoilerStructure.world(
                        boiler.boilerOrigin(), 2, y, z, boiler.rotation());
                var pos = bodyPos.offset(side.getStepX(), 0, side.getStepZ());
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BellowsBlock
                        && state.getValue(BellowsBlock.FACING) == side.getOpposite()
                        && !level.hasNeighborSignal(pos)) {
                    count++;
                }
            }
        }
        return count * 2;
    }
}
