package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 炼狱锅炉主体：完整 3×3×3 都使用这个方块。
 *
 * <p>它继承 Create 的流体储罐，因此蒸汽引擎可以直接把它识别为锅炉；
 * 具体的温度/应力逻辑由 {@link InfernalBoilerBlockEntity} 覆盖。</p>
 */
public class InfernalBoilerBlock extends FluidTankBlock {

    public InfernalBoilerBlock(BlockBehaviour.Properties properties) {
        super(properties, false);
    }

    @Override
    public Class<FluidTankBlockEntity> getBlockEntityClass() {
        return FluidTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return MysteryDynamics.INFERNAL_BOILER_BE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfernalBoilerBlockEntity(MysteryDynamics.INFERNAL_BOILER_BE.get(), pos, state);
    }
}
