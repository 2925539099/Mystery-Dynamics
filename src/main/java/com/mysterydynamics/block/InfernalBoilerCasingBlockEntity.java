package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 底座填充块自己的流体接口；把管道的水转给整栋锅炉的 controller。 */
public class InfernalBoilerCasingBlockEntity extends BlockEntity implements IFluidHandler {

    public InfernalBoilerCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private IFluidHandler tank() {
        if (level == null) {
            return null;
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (level.getBlockEntity(worldPosition.offset(dx, dy, dz))
                            instanceof InfernalBoilerBlockEntity boiler) {
                        FluidTankBlockEntity controller = boiler.isController()
                                ? boiler
                                : boiler.getControllerBE();
                        if (controller != null) {
                            return controller.getTankInventory();
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int getTanks() {
        return tank() == null ? 0 : 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        IFluidHandler handler = tank();
        return handler == null ? FluidStack.EMPTY : handler.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        IFluidHandler handler = tank();
        return handler == null ? 0 : handler.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        IFluidHandler handler = tank();
        return handler != null && handler.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        IFluidHandler handler = tank();
        return handler == null ? 0 : handler.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        IFluidHandler handler = tank();
        return handler == null ? FluidStack.EMPTY : handler.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        IFluidHandler handler = tank();
        return handler == null ? FluidStack.EMPTY : handler.drain(maxDrain, action);
    }
}
