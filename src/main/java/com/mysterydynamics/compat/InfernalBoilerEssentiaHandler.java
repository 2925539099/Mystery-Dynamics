package com.mysterydynamics.compat;

import com.mysterydynamics.block.InfernalBoilerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * 炼狱锅炉的源质接口。
 *
 * <p>只有模型 pasted7 面对应的那个底座位置能接入，且只接受火源质。
 * 这个接口不是“任意面都能接”的锅炉能力。</p>
 */
public final class InfernalBoilerEssentiaHandler implements IEssentiaTransport {

    private final InfernalBoilerBlockEntity boiler;
    private final Direction side;
    private final BlockPos accessPos;

    public InfernalBoilerEssentiaHandler(InfernalBoilerBlockEntity boiler, Direction side) {
        this(boiler, side, boiler.getBlockPos());
    }

    public InfernalBoilerEssentiaHandler(InfernalBoilerBlockEntity boiler, Direction side, BlockPos accessPos) {
        this.boiler = boiler;
        this.side = side;
        this.accessPos = accessPos;
    }

    private InfernalBoilerBlockEntity target() {
        if (boiler.isController()) {
            return boiler;
        }
        if (boiler.getControllerBE() instanceof InfernalBoilerBlockEntity controller) {
            return controller;
        }
        return boiler;
    }

    private boolean isInputPort() {
        InfernalBoilerBlockEntity controller = target();
        BlockPos origin = controller.boilerOrigin();
        if (origin.equals(BlockPos.ZERO)) {
            return false;
        }
        BlockPos port = InfernalBoilerStructure.world(origin, 2, 1, 3, controller.rotation());
        return accessPos.equals(port);
    }

    private boolean isInputSide(Direction direction) {
        return direction != null && direction == target().frontDirection() && isInputPort();
    }

    @Override
    public boolean isConnectable(Direction direction) {
        return isInputSide(direction);
    }

    @Override
    public boolean canInputFrom(Direction direction) {
        return isInputSide(direction);
    }

    @Override
    public boolean canOutputTo(Direction direction) {
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(Direction direction) {
        return isInputSide(direction) ? Aspect.FIRE : null;
    }

    @Override
    public int getSuctionAmount(Direction direction) {
        return isInputSide(direction) ? 64 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction direction) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction direction) {
        if (aspect != Aspect.FIRE || !isInputSide(direction)) {
            return 0;
        }
        return target().addFireEssentia(amount);
    }

    @Override
    public Aspect getEssentiaType(Direction direction) {
        return isInputSide(direction) && target().fireEssentia() > 0 ? Aspect.FIRE : null;
    }

    @Override
    public int getEssentiaAmount(Direction direction) {
        return isInputSide(direction) ? target().fireEssentia() : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
