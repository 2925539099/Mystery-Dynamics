package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 红石发动机方块实体。
 *
 * <p>信号 0 时停转；1~15 线性映射到 32~256 RPM 与 128~8192 SU。
 * 转速/容量都由缓存的 {@link #signal} 计算，不会每 tick 读取世界。</p>
 */
public class RedstoneEngineBlockEntity extends GeneratingKineticBlockEntity {

    public static final float MIN_RPM = 32.0F;
    public static final float MAX_RPM = 256.0F;
    public static final float MIN_STRESS = 128.0F;
    public static final float MAX_STRESS = 8192.0F;

    private int signal;
    private boolean reversed;
    /** 由 Block#setPlacedBy 标记：下一次 initialize 时按相邻传动对齐方向。 */
    private boolean pendingPlacementAlign;

    public RedstoneEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getSignal() {
        return signal;
    }

    public boolean isReversed() {
        return reversed;
    }

    /** 放置时调用：记录信号，并让 initialize 只在对齐时执行一次转速更新。 */
    public void markPlaced(int signal) {
        this.signal = Mth.clamp(signal, 0, 15);
        this.pendingPlacementAlign = true;
    }

    /** 红石信号变化时调用；只有真的变化才重算。 */
    public void updateSignal(int newSignal) {
        int clamped = Mth.clamp(newSignal, 0, 15);
        if (clamped == signal) {
            return;
        }
        signal = clamped;
        setChanged();
        sendData();
        updateGeneratedRotation();
    }

    public static float rpmFor(int signal) {
        if (signal <= 0) {
            return 0;
        }
        float t = (signal - 1) / 14.0F;
        return MIN_RPM + (MAX_RPM - MIN_RPM) * t;
    }

    public static float stressFor(int signal) {
        if (signal <= 0) {
            return 0;
        }
        float t = (signal - 1) / 14.0F;
        return MIN_STRESS + (MAX_STRESS - MIN_STRESS) * t;
    }

    public RedstoneEngineBlock block() {
        return (RedstoneEngineBlock) getBlockState().getBlock();
    }

    @Override
    public float getGeneratedSpeed() {
        float rpm = rpmFor(signal);
        return reversed ? -rpm : rpm;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float rpm = rpmFor(signal);
        float capacity = rpm <= 0 ? 0 : stressFor(signal) / rpm;
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }
        // 只有刚放下（需要对齐）时才更新一次；区块加载不触发网络重算。
        if (pendingPlacementAlign) {
            pendingPlacementAlign = false;
            alignToConnection();
            updateGeneratedRotation();
        }
    }

    /** 放置对齐：相邻网络是逆时针时跟随，顺时针则保持初始方向。 */
    public void alignToConnection() {
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

    /** 翻转方向；同一网络里的红石发动机会一起翻转。 */
    public void toggleDirection() {
        boolean target = !reversed;

        if (level == null || level.isClientSide) {
            reversed = target;
            return;
        }

        List<RedstoneEngineBlockEntity> group = collectConnectedEngines();
        for (RedstoneEngineBlockEntity engine : group) {
            engine.reversed = target;
            engine.setChanged();
            engine.sendData();
        }

        RedstoneEngineBlockEntity driver = this;
        for (RedstoneEngineBlockEntity engine : group) {
            if (!engine.hasSource()) {
                driver = engine;
                break;
            }
        }
        driver.updateGeneratedRotation();
    }

    private List<RedstoneEngineBlockEntity> collectConnectedEngines() {
        List<RedstoneEngineBlockEntity> group = new ArrayList<>();
        group.add(this);
        if (!hasNetwork()) {
            return group;
        }
        KineticNetwork network = getOrCreateNetwork();
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member instanceof RedstoneEngineBlockEntity engine && engine != this) {
                group.add(engine);
            }
        }
        return group;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("Signal", signal);
        compound.putBoolean("Reversed", reversed);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        signal = Mth.clamp(compound.getInt("Signal"), 0, 15);
        reversed = compound.getBoolean("Reversed");
    }
}
