package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** 炼狱锅炉正面观察窗；overclock 时显示超级燃烧层。 */
public class InfernalBoilerFaceBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<InfernalBoilerFaceBlock> CODEC =
            simpleCodec(InfernalBoilerFaceBlock::new);
    public static final BooleanProperty OVERCLOCK = BooleanProperty.create("overclock");

    public InfernalBoilerFaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(OVERCLOCK, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, OVERCLOCK);
    }
}
