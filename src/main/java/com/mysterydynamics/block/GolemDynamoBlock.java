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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 傀儡动力核心：一台 32 RPM / 512 SU 的应力源。
 *
 * <p>朝向：放置时正面朝向玩家（水平四向）。传动轴与正面垂直，
 * 所以 facing=north 时东西两面是输出端，facing=east/west 时南北两面是输出端。</p>
 *
 * <p>转向：只有手持机械动力扳手时才能调整——扳手右键顶/底面旋转整机朝向，
 * 扳手右键侧面翻转顺时针/逆时针。空手不再能改转向。</p>
 */
public class GolemDynamoBlock extends HorizontalKineticBlock implements IBE<GolemDynamoBlockEntity> {

    public GolemDynamoBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        // 输出轴与正面垂直：north/south -> X 轴（东西），east/west -> Z 轴（南北）
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GolemDynamoBlockEntity dynamo) {
            // 放置时标记，等 initialize 时按相邻传动方向对齐
            dynamo.markPlaced();
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Direction face = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // 顶面 / 底面：旋转整机朝向（应力输出端跟着转到另外两面）
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

        // 其余面：切换顺时针 / 逆时针（参考蒸汽引擎的 movementDirection）
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GolemDynamoBlockEntity dynamo) {
            dynamo.toggleDirection();
            notifyDirection(context.getPlayer(), dynamo);
            IWrenchable.playRotateSound(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** 用动作栏提示当前转向，方便确认“设置”确实生效。 */
    private static void notifyDirection(net.minecraft.world.entity.player.Player player,
                                        GolemDynamoBlockEntity dynamo) {
        if (player != null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            dynamo.isReversed() ? "mystery_dynamics.direction.ccw" : "mystery_dynamics.direction.cw"),
                    true);
        }
    }

    @Override
    public Class<GolemDynamoBlockEntity> getBlockEntityClass() {
        return GolemDynamoBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GolemDynamoBlockEntity> getBlockEntityType() {
        return MysteryDynamics.GOLEM_DYNAMO_BE.get();
    }
}
