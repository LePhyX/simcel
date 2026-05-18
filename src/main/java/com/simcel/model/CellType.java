package com.simcel.model;

public enum CellType {

    FORET(0.7, 8),
    PRAIRIE(0.9, 3),
    BROUSSAILLES(0.5, 5),
    ZONE_HUMIDE(0.2, 12),
    ZONE_URBAINE(0.3, 6);

    private final double inflammability;
    private final int burnDuration;

    CellType(double inflammability, int burnDuration) {
        this.inflammability = inflammability;
        this.burnDuration = burnDuration;
    }

    public double getInflammability() {
        return inflammability;
    }

    public int getBurnDuration() {
        return burnDuration;
    }
}
