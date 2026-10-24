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
                || state.is(TCBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get())
                || state.is(TCBlocks.ALCHEMICAL_FURNACE.get())
                || state.is(TCBlocks.ADVANCED_ALCHEMICAL_FURNACE.get())
                || state.is(TCBlocks.ARCANE_FURNACE.get())
                || state.is(TCBlocks.ARCANE_FURNACE_CASING.get())
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

    /**
     * 只有模型里 pasted6 贴图对应的三个面能装风箱：
     * 东侧 (4,0,1)/(4,0,2)，北侧 (2,0,-1)。
     * 每个 +2 热等级，最多三个。
     */
    public static int countBellows(InfernalBoilerBlockEntity boiler) {
        Level level = boiler.getLevel();
        if (level == null) {
            return 0;
        }
        var origin = boiler.boilerOrigin();
        if (origin.equals(net.minecraft.core.BlockPos.ZERO)) {
            origin = boiler.getBlockPos().offset(-1, -3, -1);
        }
        int rotation = boiler.rotation();
        int count = 0;
        int[][] positions = {{4, 0, 1}, {4, 0, 2}, {2, 0, -1}};
        for (int[] p : positions) {
            var pos = InfernalBoilerStructure.world(origin, p[0], p[1], p[2], rotation);
            if (level.getBlockState(pos).getBlock() instanceof BellowsBlock) {
                count++;
            }
        }
        return count * 2;
    }

    private static boolean touchesBoiler(Level level, net.minecraft.core.BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(MysteryDynamics.INFERNAL_BOILER.get())
                    || state.is(MysteryDynamics.INFERNAL_BOILER_CASING.get())
                    || state.is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())
                    || state.is(MysteryDynamics.INFERNAL_BOILER_FACE.get())) {
                return true;
            }
        }
        return false;
    }
}
