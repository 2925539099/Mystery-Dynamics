package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 红石发动机：输出随红石信号变化（1~15 → 32~256 RPM，128~8192 SU）。
 *
 * <p>正面是输出轴，扳手顶/底面旋转朝向，侧面翻转转向；转向会像傀儡动力系统一样
 * 在同一网络内同步。</p>
 */
public class RedstoneEngineBlock extends HorizontalKineticBlock implements IBE<RedstoneEngineBlockEntity> {

    public RedstoneEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    /** 输出轴朝向玩家看的方向（背对玩家），方便直接接在要驱动的机械上。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            engine.markPlaced(level.getBestNeighborSignal(pos));
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            engine.updateSignal(level.getBestNeighborSignal(pos));
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Direction face = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (face.getAxis() == Direction.Axis.Y) {
            BlockState rotated = getRotatedBlockState(state, face);
            if (!rotated.canSurvive(level, pos)) {
                return InteractionResult.PASS;
            }
            KineticBlockEntity.switchToBlockState(level, pos, updateAfterWrenched(rotated, context));
            if (level.getBlockState(pos) != state) {
                IWrenchable.playRotateSound(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            engine.toggleDirection();
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                engine.isReversed() ? "mystery_dynamics.direction.ccw"
                                        : "mystery_dynamics.direction.cw"),
                        true);
            }
            IWrenchable.playRotateSound(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<RedstoneEngineBlockEntity> getBlockEntityClass() {
        return RedstoneEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneEngineBlockEntity> getBlockEntityType() {
        return MysteryDynamics.REDSTONE_ENGINE_BE.get();
    }
}
