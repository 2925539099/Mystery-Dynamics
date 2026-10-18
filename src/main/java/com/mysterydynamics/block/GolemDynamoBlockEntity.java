package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 翻转输出方向。
     *
     * <p>同一传动网络里的所有傀儡动力核心会一起翻转，避免同一根轴上出现
     * 一正一反的应力源（Create 会把方向冲突的动力部件判定为损坏）。
     * 只让网络中“没有上级来源”的那台重算转速，其余核心由网络传播更新。</p>
     *
     * @return 是否成功；网络里存在其它动力源（如水车、风车）时返回 false，不做任何修改
     */
    public boolean toggleDirection() {
        boolean target = !reversed;

        // 客户端只做本地预测，真正的同步由服务端的 sendData 完成
        if (level == null || level.isClientSide) {
            reversed = target;
            return true;
        }

        if (hasNetwork()) {
            for (KineticBlockEntity member : getOrCreateNetwork().members.keySet()) {
                if (!(member instanceof GolemDynamoBlockEntity) && member.isSource()) {
                    // 网络里还有别的动力源，强行反转会触发 Create 的方向冲突损坏
                    return false;
                }
            }
        }

        List<GolemDynamoBlockEntity> group = collectConnectedDynamos();
        for (GolemDynamoBlockEntity dynamo : group) {
            dynamo.reversed = target;
            dynamo.setChanged();
            dynamo.sendData();
        }

        // 必须由网络里“没有上级来源”的那台来重算：跟随者如果直接
        // updateGeneratedRotation()，Create 会把方向冲突判定成损坏并拆掉它。
        GolemDynamoBlockEntity driver = this;
        for (GolemDynamoBlockEntity dynamo : group) {
            if (!dynamo.hasSource()) {
                driver = dynamo;
                break;
            }
        }
        driver.updateGeneratedRotation();
        return true;
    }

    /** 收集同一动力学网络里的所有傀儡动力核心（包含自己）。 */
    private List<GolemDynamoBlockEntity> collectConnectedDynamos() {
        List<GolemDynamoBlockEntity> group = new ArrayList<>();
        group.add(this);
        if (!hasNetwork()) {
            return group;
        }
        KineticNetwork network = getOrCreateNetwork();
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member instanceof GolemDynamoBlockEntity dynamo && dynamo != this) {
                group.add(dynamo);
            }
        }
        return group;
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
