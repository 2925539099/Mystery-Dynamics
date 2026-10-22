package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mysterydynamics.block.RedstoneEngineBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 红石发动机的旋转输出轴：复用创造马达的 SHAFT_HALF 半轴模型。
 *
 * <p>Create 的 {@link KineticBlockEntityRenderer#renderSafe} 在 Flywheel 可视化开启时会直接
 * return（把旋转交给视觉系统）。我们只画一根半轴，没有注册 Flywheel visual，所以这里自己画，
 * 避免“轴不显示”。</p>
 */
public class RedstoneEngineRenderer extends KineticBlockEntityRenderer<RedstoneEngineBlockEntity> {

    public RedstoneEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RedstoneEngineBlockEntity be, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        SuperByteBuffer shaft = getRotatedModel(be, state);
        VertexConsumer consumer = buffer.getBuffer(getRenderType(be, state));
        renderRotatingBuffer(be, shaft, poseStack, consumer, light);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(RedstoneEngineBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state,
                state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }
}
