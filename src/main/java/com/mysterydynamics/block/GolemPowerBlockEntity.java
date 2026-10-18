package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 傀儡动力系统的方块实体。
 *
 * <p>转速 / 容量都从方块本身读取（{@link GolemPowerBlock}），
 * 所以 8 台设备共用同一个方块实体类型。</p>
 */
public class GolemPowerBlockEntity extends GeneratingKineticBlockEntity {

    private boolean reversed;
    /** 由 Block#setPlacedBy 标记：下一次 initialize 时按相邻传动对齐方向。 */
    private boolean pendingPlacementAlign;

    public GolemPowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isReversed() {
        return reversed;
    }

    public GolemPowerBlock block() {
        return (GolemPowerBlock) getBlockState().getBlock();
    }

    public void markPlaced() {
        pendingPlacementAlign = true;
    }

    /** 放置对齐：相邻网络是逆时针时跟随，顺时针则保持初始方向。 */
    public void alignToConnection() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction.Axis axis = ((IRotate) getBlockState().getBlock()).getRotationAxis(getBlockState());
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() != axis) {
                continue;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(dir));
            if (!(neighbour instanceof KineticBlockEntity kinetic)) {
                continue;
            }
            float speed = kinetic.getTheoreticalSpeed();
            if (speed < 0) {
                reversed = true;
                setChanged();
                sendData();
                return;
            }
            if (speed > 0) {
                return;
            }
        }
    }

    /** 翻转方向；同一网络里的所有傀儡动力系统一起翻转。 */
    public void toggleDirection() {
        boolean target = !reversed;

        if (level == null || level.isClientSide) {
            reversed = target;
            return;
        }

        List<GolemPowerBlockEntity> group = collectConnectedDevices();
        for (GolemPowerBlockEntity dynamo : group) {
            dynamo.reversed = target;
            dynamo.setChanged();
            dynamo.sendData();
        }

        // 由网络里“没有上级来源”的那台负责重算，避免跟随者被判定为方向冲突拆掉
        GolemPowerBlockEntity driver = this;
        for (GolemPowerBlockEntity dynamo : group) {
            if (!dynamo.hasSource()) {
                driver = dynamo;
                break;
            }
        }
        driver.updateGeneratedRotation();
    }

    private List<GolemPowerBlockEntity> collectConnectedDevices() {
        List<GolemPowerBlockEntity> group = new ArrayList<>();
        group.add(this);
        if (!hasNetwork()) {
            return group;
        }
        KineticNetwork network = getOrCreateNetwork();
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member instanceof GolemPowerBlockEntity dynamo && dynamo != this) {
                group.add(dynamo);
            }
        }
        return group;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            if (pendingPlacementAlign) {
                pendingPlacementAlign = false;
                alignToConnection();
            }
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return reversed ? -block().rpm() : block().rpm();
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = block().capacityPerRpm();
        return lastCapacityProvided;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Reversed", reversed);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        reversed = compound.getBoolean("Reversed");
    }
}
