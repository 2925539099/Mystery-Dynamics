package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerStructure;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 成型后的底座填充块。
 *
 * <p>继承 FluidTankBlock 是为了让蒸汽引擎把底座也视为锅炉的一部分；
 * 实际流体/热量仍由邻近的 controller 提供。</p>
 */
public class InfernalBoilerCasingBlock extends FluidTankBlock {

    public InfernalBoilerCasingBlock(BlockBehaviour.Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfernalBoilerCasingBlockEntity(
                MysteryDynamics.INFERNAL_BOILER_CASING_BE.get(), pos, state);
    }

    @Override
    public Class<FluidTankBlockEntity> getBlockEntityClass() {
        return FluidTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return MysteryDynamics.INFERNAL_BOILER_CASING_BE.get();
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
