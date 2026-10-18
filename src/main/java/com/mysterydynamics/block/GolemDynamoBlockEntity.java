package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 应力源方块实体。
 *
 * <p>容量按“每 RPM”登记：{@link #CAPACITY_PER_RPM} × {@link #GENERATED_RPM} = 512 SU。</p>
 *
 * <p>转向通过 {@code reversed} 翻转 {@link #getGeneratedSpeed()} 的符号，
 * 与蒸汽引擎的 {@code movementDirection} 是同一套思路。</p>
 */
public class GolemDynamoBlockEntity extends GeneratingKineticBlockEntity {

    /** 额定转速 32 RPM。 */
    public static final float GENERATED_RPM = 32.0F;
    /** 每 RPM 提供的应力容量：16 × 32 = 512 SU。 */
    public static final float CAPACITY_PER_RPM = 16.0F;

    private boolean reversed;

    public GolemDynamoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isReversed() {
        return reversed;
    }

    /** 顶面/底面用扳手右键时调用：翻转输出方向并重建动力学网络。 */
    public void toggleDirection() {
        reversed = !reversed;
        setChanged();
        sendData();
        updateGeneratedRotation();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return reversed ? -GENERATED_RPM : GENERATED_RPM;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = CAPACITY_PER_RPM;
        return CAPACITY_PER_RPM;
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
