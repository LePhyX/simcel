package com.simcel.model;

/**
 * Les huit directions cardinales et inter-cardinales du vent.
 *
 * <p>
 * Chaque constante porte un vecteur unitaire {@code (dx, dy)} exprimé dans le
 * repère de la grille, où l'axe X pointe vers l'Est et l'axe Y pointe vers le
 * Sud (convention écran) :</p>
 * <pre>
 *   NO  N  NE
 *    O  ·  E
 *   SO  S  SE
 * </pre>
 */
public enum WindDirection {

    /**
     * Nord : le vent souffle vers le bas ({@code dy = -1}).
     */
    N(0, -1),
    /**
     * Nord-Est.
     */
    NE(1, -1),
    /**
     * Est : le vent souffle vers la droite ({@code dx = 1}).
     */
    E(1, 0),
    /**
     * Sud-Est.
     */
    SE(1, 1),
    /**
     * Sud : le vent souffle vers le haut ({@code dy = 1}).
     */
    S(0, 1),
    /**
     * Sud-Ouest.
     */
    SO(-1, 1),
    /**
     * Ouest : le vent souffle vers la gauche ({@code dx = -1}).
     */
    O(-1, 0),
    /**
     * Nord-Ouest.
     */
    NO(-1, -1);

    private final int dx;
    private final int dy;

    WindDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Composante horizontale du vecteur vent ({@code -1}, {@code 0} ou
     * {@code 1}).
     *
     * @return composante X
     */
    public int getDx() {
        return dx;
    }

    /**
     * Composante verticale du vecteur vent ({@code -1}, {@code 0} ou
     * {@code 1}).
     *
     * @return composante Y
     */
    public int getDy() {
        return dy;
    }
}
