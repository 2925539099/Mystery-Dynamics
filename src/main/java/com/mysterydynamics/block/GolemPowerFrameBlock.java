package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 空傀儡动力系统框架：装饰方块，不参与传动，也不产生应力。
 *
 * <p>挖掘参数与傀儡动力系统完全一致（石头硬度、斧/镐可挖），
 * 并且同样支持扳手旋转与潜行扳手拆卸。</p>
 */
public class GolemPowerFrameBlock extends HorizontalDirectionalBlock implements IWrenchable {

    public static final MapCodec<GolemPowerFrameBlock> CODEC = simpleCodec(GolemPowerFrameBlock::new);

    public GolemPowerFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** 扳手右键顶面/底面：旋转 90°。潜行扳手拆卸由 {@link IWrenchable} 默认实现处理。 */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace().getAxis() != Direction.Axis.Y) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
        if (!rotated.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, rotated, 3);
        if (!level.isClientSide) {
            IWrenchable.playRotateSound(level, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
