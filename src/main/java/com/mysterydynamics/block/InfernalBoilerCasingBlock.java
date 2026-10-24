package com.mysterydynamics.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 成型后的底座填充块；本身不渲染，由锅炉 shell 模型绘制。 */
public class InfernalBoilerCasingBlock extends Block {

    public InfernalBoilerCasingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
