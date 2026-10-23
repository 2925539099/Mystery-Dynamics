package com.mysterydynamics.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** 炼狱锅炉正面观察窗；overclock 时显示超级燃烧层。 */
public class InfernalBoilerFaceBlock extends Block {

    public static final BooleanProperty OVERCLOCK = BooleanProperty.create("overclock");

    public InfernalBoilerFaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OVERCLOCK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OVERCLOCK);
    }
}
