package com.mysterydynamics.block;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * 底座填充块同时也是 FluidTankBlockEntity，这样贴在底座上的蒸汽引擎
 * 能像识别原版锅炉一样识别到锅炉 controller。
 */
public class InfernalBoilerCasingBlockEntity extends FluidTankBlockEntity implements IFluidHandler {

    public InfernalBoilerCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void write(net.minecraft.nbt.CompoundTag tag,
                      net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        // 这是代理 BE，不保存 FluidTankBlockEntity 的 controller/width 数据。
        // Create 原版 write 会在 controller 为 null 时抛 NPE。
    }

    @Override
    protected void read(net.minecraft.nbt.CompoundTag tag,
                        net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        // 代理 BE 不读取储罐数据。
    }

    private InfernalBoilerBlockEntity controller() {
        if (level == null) {
            return null;
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (level.getBlockEntity(worldPosition.offset(dx, dy, dz))
                            instanceof InfernalBoilerBlockEntity boiler
                            && boiler.isController()) {
                        return boiler;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void tick() {
        // 这不是实际储罐，只是底座上的锅炉代理。
    }

    @Override
    public boolean isController() {
        return false;
    }

    @Override
    public FluidTankBlockEntity getControllerBE() {
        return controller();
    }

    @Override
    public FluidTank getTankInventory() {
        InfernalBoilerBlockEntity controller = controller();
        return controller == null ? super.getTankInventory() : controller.getTankInventory();
    }

    @Override
    public int getTotalTankSize() {
        InfernalBoilerBlockEntity controller = controller();
        return controller == null ? 0 : controller.getTotalTankSize();
    }

    @Override
    public int getTanks() {
        return getTankInventory() == null ? 0 : 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getTankInventory().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return getTankInventory().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return getTankInventory().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return getTankInventory().fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return getTankInventory().drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return getTankInventory().drain(maxDrain, action);
    }
}
