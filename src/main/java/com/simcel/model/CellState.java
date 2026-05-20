package com.simcel.model;

public enum CellState {

    VIDE("#FFFFFF"),
    SAIN("#228B22"),
    EN_FEU("#FF4500"),
    BRULE("#3B3B3B"),
    EAU("#1E90FF"),
    ROCHER("#808080");

    private final String color;

    CellState(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public boolean isFlammable() {
        return this == SAIN;
    }

    public boolean isSpreading() {
        return this == EN_FEU;
    }
}
