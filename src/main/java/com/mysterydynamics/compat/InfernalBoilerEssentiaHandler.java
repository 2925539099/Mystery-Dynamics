package com.mysterydynamics.compat;

import com.mysterydynamics.block.InfernalBoilerBlockEntity;

import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/** 炼狱锅炉的源质接口：只允许正面输入火源质。 */
public final class InfernalBoilerEssentiaHandler implements IEssentiaTransport {

    private final InfernalBoilerBlockEntity boiler;
    private final Direction side;

    public InfernalBoilerEssentiaHandler(InfernalBoilerBlockEntity boiler, Direction side) {
        this.boiler = boiler;
        this.side = side;
    }

    private boolean isInputSide(Direction direction) {
        return direction != null && direction == target().frontDirection();
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
        return target().fireEssentia() > 0 ? Aspect.FIRE : null;
    }

    @Override
    public int getEssentiaAmount(Direction direction) {
        return target().fireEssentia();
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
