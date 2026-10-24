package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 炼狱锅炉主体：完整 3×3×3 都使用这个方块。
 *
 * <p>它继承 Create 的流体储罐，因此蒸汽引擎可以直接把它识别为锅炉；
 * 具体的温度/应力逻辑由 {@link InfernalBoilerBlockEntity} 覆盖。</p>
 */
public class InfernalBoilerBlock extends FluidTankBlock {

    /** 成型后由整栋模型的 shell 方块渲染，27 个储罐方块不再单独绘制。 */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public InfernalBoilerBlock(BlockBehaviour.Properties properties) {
        super(properties, false);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : super.getRenderShape(state);
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
