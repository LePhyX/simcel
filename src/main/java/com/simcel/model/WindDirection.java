package com.simcel.model;

public enum WindDirection {
    N(0, -1),
    NE(1, -1),
    E(1, 0),
    SE(1, 1),
    S(0, 1),
    SO(-1, 1),
    O(-1, 0),
    NO(-1, -1);

    private final int dx;
    private final int dy;

    WindDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }
}
