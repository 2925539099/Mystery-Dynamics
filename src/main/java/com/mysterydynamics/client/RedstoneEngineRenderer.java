package com.mysterydynamics.client;

import com.mysterydynamics.block.RedstoneEngineBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 红石发动机的旋转输出轴：复用创造马达的 SHAFT_HALF 半轴模型。
 */
public class RedstoneEngineRenderer extends KineticBlockEntityRenderer<RedstoneEngineBlockEntity> {

    public RedstoneEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(RedstoneEngineBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state,
                state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }
}
