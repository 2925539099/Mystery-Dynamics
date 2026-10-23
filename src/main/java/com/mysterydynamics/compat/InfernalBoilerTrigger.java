package com.mysterydynamics.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.common.items.TCFunctionalItems;
import thaumcraft.common.research.TCResearchManager;

/** 法杖右击结构任意方块时尝试成型。 */
public final class InfernalBoilerTrigger implements IWandTriggerManager {

    @Override
    public boolean performTrigger(Level level, ItemStack stack, Player player, BlockPos pos,
                                  Direction face, int event) {
        if (level.isClientSide || player == null) {
            return false;
        }
        if (!(stack.getItem() instanceof TCFunctionalItems.WandCastingItem wand)) {
            return false;
        }
        if (!TCResearchManager.has(player, "INFERNAL_BOILER")) {
            player.displayClientMessage(Component.translatable("mystery_dynamics.infernal_boiler.locked"), true);
            return false;
        }
        BlockPos glass = InfernalBoilerStructure.findGlass(level, pos);
        if (glass == null) {
            return false;
        }
        if (!InfernalBoilerStructure.matches(level, glass)) {
            return false;
        }

        AspectList cost = new AspectList()
                .add(Aspect.FIRE, 100)
                .add(Aspect.EARTH, 100)
                .add(Aspect.AIR, 30)
                .add(Aspect.ENTROPY, 30)
                .add(Aspect.WATER, 30)
                .add(Aspect.ORDER, 30);
        if (!wand.consumeVisCost(stack, player, cost, true)) {
            player.displayClientMessage(Component.translatable("mystery_dynamics.infernal_boiler.no_vis"), true);
            return false;
        }
        if (!InfernalBoilerStructure.form(level, glass)) {
            return false;
        }

        player.displayClientMessage(Component.translatable("mystery_dynamics.infernal_boiler.formed"), true);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
