package com.simcel.model;

/**
 * États possibles d'une cellule dans la simulation.
 *
 * <p>
 * Chaque constante porte la couleur hexadécimale associée utilisée par
 * l'interface graphique, ainsi que deux prédicats sémantiques :
 * {@link #isFlammable()} et {@link #isSpreading()}.</p>
 */
public enum CellState {

    /**
     * Cellule vide, sans végétation ni obstacle.
     */
    VIDE("#FFFFFF"),
    /**
     * Cellule saine et inflammable.
     */
    SAIN("#228B22"),
    /**
     * Cellule actuellement en combustion.
     */
    EN_FEU("#FF4500"),
    /**
     * Cellule entièrement consumée, ne peut plus brûler.
     */
    BRULE("#3B3B3B"),
    /**
     * Cellule d'eau, ignifuge et non traversable par le feu.
     */
    EAU("#1E90FF"),
    /**
     * Cellule rocheuse, ignifuge et non traversable par le feu.
     */
    ROCHER("#808080");

    private final String color;

    CellState(String color) {
        this.color = color;
    }

    /**
     * Retourne la couleur hexadécimale associée à cet état (format
     * {@code #RRGGBB}).
     *
     * @return couleur hexadécimale, jamais {@code null}
     */
    public String getColor() {
        return color;
    }

    /**
     * Indique si une cellule dans cet état peut prendre feu.
     *
     * @return {@code true} uniquement pour {@link #SAIN}
     */
    public boolean isFlammable() {
        return this == SAIN;
    }

    /**
     * Indique si une cellule dans cet état propage le feu à ses voisins.
     *
     * @return {@code true} uniquement pour {@link #EN_FEU}
     */
    public boolean isSpreading() {
        return this == EN_FEU;
    }
}
