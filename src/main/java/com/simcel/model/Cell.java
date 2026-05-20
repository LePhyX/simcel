package com.simcel.model;

public class Cell {

    private CellState state;
    private final CellType type;
    private int remainingBurnTime;

    public Cell(CellType type) {
        this.type = type;
        this.state = CellState.SAIN;
        this.remainingBurnTime = type.getBurnDuration();
    }

    public CellState getState() {
        return state;
    }

    public CellType getType() {
        return type;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    public void setState(CellState state) {
        this.state = state;
    }

    public void decrementBurnTime() {
        if (remainingBurnTime > 0) {
            remainingBurnTime--;
        }
    }

    public void ignite() {
        if (state.isFlammable()) {
            state = CellState.EN_FEU;
            remainingBurnTime = type.getBurnDuration();
        }
    }

    public void burnOut() {
        state = CellState.BRULE;
        remainingBurnTime = 0;
    }

    public Cell copy() {
        Cell copy = new Cell(this.type);
        copy.state = this.state;
        copy.remainingBurnTime = this.remainingBurnTime;
        return copy;
    }
}
