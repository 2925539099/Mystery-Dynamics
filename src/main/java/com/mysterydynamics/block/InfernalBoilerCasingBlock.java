package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 成型后的底座填充块；本身不渲染，由锅炉 shell 模型绘制。 */
public class InfernalBoilerCasingBlock extends Block implements EntityBlock {

    public InfernalBoilerCasingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfernalBoilerCasingBlockEntity(
                MysteryDynamics.INFERNAL_BOILER_CASING_BE.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !InfernalBoilerStructure.isRestoring()) {
            InfernalBoilerStructure.requestDismantle(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
