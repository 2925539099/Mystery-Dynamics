package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import java.lang.ref.WeakReference;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

/**
 * 炼狱锅炉主体方块实体。
 *
 * <p>储罐的成型、容量、流体能力继承 Create 的 {@link FluidTankBlockEntity}；
 * 锅炉温度、超频和应力由 {@link InfernalBoilerData} 接管。</p>
 */
public class InfernalBoilerBlockEntity extends FluidTankBlockEntity {

    public static final int MAX_FIRE_ESSENTIA = 256;
    /** 与 Create 满级 3×3×32 储罐相同的容量：288 × 8000 mB。 */
    public static final int MAX_WATER_CAPACITY = 2_304_000;

    private BlockPos boilerOrigin = BlockPos.ZERO;
    private BlockPos facePos = BlockPos.ZERO;
    private Direction frontDirection = Direction.SOUTH;
    private int rotation;
    private int fireEssentia;
    private int fireVisTicks;

    public InfernalBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.boiler = new InfernalBoilerData();
    }

    @Override
    public void applyFluidTankSize(int blocks) {
        if (tankInventory != null) {
            tankInventory.setCapacity(MAX_WATER_CAPACITY);
            forceFluidLevelUpdate = true;
        }
    }

    @Override
    public int getTotalTankSize() {
        // 尺寸按 Create 满级 3×3×32 锅炉显示；实际结构与容量仍按 3×3×3。
        return 288;
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos origin = boilerOrigin.equals(BlockPos.ZERO)
                ? getBlockPos().offset(-1, -3, -1)
                : boilerOrigin;
        // 整栋模型是 4×4×5，必须把渲染裁剪盒扩大到整栋，
        // 否则视角稍偏、controller 离开视锥时模型会整栋消失。
        return new AABB(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 4.0D, origin.getY() + 5.0D, origin.getZ() + 4.0D)
                .inflate(0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        boolean changed = false;
        if (fireEssentia > 0 && level.getGameTime() % 20L == 0L) {
            fireEssentia--;
            changed = true;
        }

        if (ModList.get().isLoaded("thaumcraft")) {
            InfernalBoilerCompat.tickVis(this);
        }
        if (fireVisTicks > 0) {
            fireVisTicks--;
            changed = true;
        }

        updateFaceState();
        updateShellState();
        if (level.getGameTime() % 10L == 0L) {
            linkNearbySteamEngines();
        }
        if (changed) {
            setChanged();
        }
    }

    /**
     * Create 的 SteamEngineBlockEntity 只会从“背后的方块”重新找 tank。
     * 我们的引擎常贴在 casing/外壳上，若该位置没有可识别 BE，引擎就会一直空闲。
     * 这里把整栋结构附近的蒸汽引擎直接挂到真正的锅炉 controller 上。
     */
    private void linkNearbySteamEngines() {
        if (boilerOrigin.equals(BlockPos.ZERO) || level == null) {
            return;
        }
        for (int x = -1; x <= 4; x++) {
            for (int y = -1; y <= 5; y++) {
                for (int z = -1; z <= 4; z++) {
                    BlockPos pos = boilerOrigin.offset(x, y, z);
                    if (!(level.getBlockState(pos).getBlock() instanceof SteamEngineBlock)) {
                        continue;
                    }
                    if (level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine) {
                        engine.source = new WeakReference<>(this);
                    }
                }
            }
        }
    }

    private void updateShellState() {
        if (boilerOrigin.equals(BlockPos.ZERO) || level == null || !level.isLoaded(boilerOrigin)) {
            return;
        }
        BlockState state = level.getBlockState(boilerOrigin);
        if (!state.is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())) {
            return;
        }
        boolean overclock = isOverclock();
        if (state.getValue(InfernalBoilerShellBlock.OVERCLOCK) != overclock) {
            level.setBlock(boilerOrigin,
                    state.setValue(InfernalBoilerShellBlock.OVERCLOCK, overclock), 3);
        }
    }

    private void updateFaceState() {
        if (facePos.equals(BlockPos.ZERO) || level == null || !level.isLoaded(facePos)) {
            return;
        }
        BlockState state = level.getBlockState(facePos);
        if (!state.is(MysteryDynamics.INFERNAL_BOILER_FACE.get())) {
            return;
        }
        boolean overclock = isOverclock();
        if (state.getValue(InfernalBoilerFaceBlock.OVERCLOCK) != overclock) {
            level.setBlock(facePos, state.setValue(InfernalBoilerFaceBlock.OVERCLOCK, overclock), 3);
        }
    }

    public void setFormation(BlockPos origin, BlockPos face, Direction front, int rotation) {
        this.boilerOrigin = origin;
        this.facePos = face;
        this.frontDirection = front;
        this.rotation = rotation;
        setChanged();
    }

    public BlockPos boilerOrigin() {
        return boilerOrigin;
    }

    public BlockPos facePos() {
        return facePos;
    }

    public Direction frontDirection() {
        return frontDirection;
    }

    public int rotation() {
        return rotation;
    }

    public int fireEssentia() {
        return fireEssentia;
    }

    public int addFireEssentia(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, MAX_FIRE_ESSENTIA - fireEssentia);
        if (accepted > 0) {
            fireEssentia += accepted;
            setChanged();
        }
        return accepted;
    }

    public boolean isOverclock() {
        return fireEssentia > 0;
    }

    public int fireVisTicks() {
        return fireVisTicks;
    }

    public void setFireVisTicks(int ticks) {
        this.fireVisTicks = Math.max(0, Math.min(400, ticks));
        setChanged();
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("BoilerOrigin", boilerOrigin.asLong());
        tag.putLong("FacePos", facePos.asLong());
        tag.putString("Front", frontDirection.getSerializedName());
        tag.putInt("Rotation", rotation);
        tag.putInt("FireEssentia", fireEssentia);
        tag.putInt("FireVisTicks", fireVisTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        boilerOrigin = BlockPos.of(tag.getLong("BoilerOrigin"));
        facePos = BlockPos.of(tag.getLong("FacePos"));
        Direction loadedFront = Direction.byName(tag.getString("Front"));
        frontDirection = loadedFront == null || loadedFront.getAxis().isVertical()
                ? Direction.SOUTH : loadedFront;
        rotation = Math.max(0, Math.min(3, tag.getInt("Rotation")));
        fireEssentia = tag.getInt("FireEssentia");
        fireVisTicks = tag.getInt("FireVisTicks");
    }
}
