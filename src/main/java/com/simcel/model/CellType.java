package com.simcel.model;

/**
 * Types de terrain qu'une cellule peut représenter.
 *
 * <p>
 * Chaque type porte deux paramètres physiques utilisés par
 * {@link FireSimulator} pour calculer la propagation du feu :</p>
 * <ul>
 * <li><b>inflammability</b> : probabilité de base qu'une cellule de ce type
 * s'enflamme lorsqu'un voisin est en feu (entre 0.0 et 1.0).</li>
 * <li><b>burnDuration</b> : nombre de ticks nécessaires pour que la cellule
 * soit entièrement consumée une fois en feu.</li>
 * </ul>
 */
public enum CellType {

    /**
     * Forêt dense — inflammabilité élevée, combustion longue.
     */
    FORET(0.7, 8),
    /**
     * Prairie — très inflammable, combustion rapide.
     */
    PRAIRIE(0.9, 3),
    /**
     * Broussailles — inflammabilité modérée, combustion intermédiaire.
     */
    BROUSSAILLES(0.5, 5),
    /**
     * Zone humide — faible inflammabilité, combustion très lente.
     */
    ZONE_HUMIDE(0.2, 12),
    /**
     * Zone urbaine — inflammabilité faible, combustion modérée.
     */
    ZONE_URBAINE(0.3, 6);

    private final double inflammability;
    private final int burnDuration;

    CellType(double inflammability, int burnDuration) {
        this.inflammability = inflammability;
        this.burnDuration = burnDuration;
    }

    /**
     * Retourne la probabilité de base d'inflammation pour ce type de terrain.
     *
     * @return valeur dans {@code [0.0, 1.0]}
     */
    public double getInflammability() {
        return inflammability;
    }

    /**
     * Retourne la durée de combustion en nombre de ticks.
     *
     * @return durée positive en ticks
     */
    public int getBurnDuration() {
        return burnDuration;
    }
}
