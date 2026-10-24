package com.mysterydynamics.mixin;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create 的 SteamEngineBlockEntity.isValid() 只认 AllBlocks.FLUID_TANK 这个精确方块。
 * 我们的锅炉主体/底座/外壳不是 Create 的 FLUID_TANK，所以引擎永远判定无效。
 * 这里把炼狱锅炉的结构方块也视为合法热源/储罐后方块。
 */
@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class SteamEngineBlockEntityMixin {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void mysterydynamics$acceptInfernalBoiler(CallbackInfoReturnable<Boolean> cir) {
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) (Object) this;
        if (engine.getLevel() == null) {
            return;
        }
        Direction back = SteamEngineBlock.getConnectedDirection(engine.getBlockState()).getOpposite();
        BlockState behind = engine.getLevel().getBlockState(engine.getBlockPos().relative(back));
        if (behind.is(MysteryDynamics.INFERNAL_BOILER.get())) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 原版把锅炉效率 clamp 到 [0,1]。炼狱锅炉超频时效率需要到 2.0，
     * 才能让 12 台引擎显示/输出满级锅炉两倍的 589824 SU。
     */
    @Redirect(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"),
            remap = false)
    private static float mysterydynamics$allowOverclockEfficiency(float value, float min, float max) {
        return Mth.clamp(value, min, Math.max(max, 2.0F));
    }
}
