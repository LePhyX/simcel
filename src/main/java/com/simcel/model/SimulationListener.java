package com.simcel.model;

/**
 * Observateur du cycle de vie de la simulation (patron Observer).
 *
 * <p>Les implémentations sont enregistrées via
 * {@link FireSimulator#addListener(SimulationListener)} et reçoivent une
 * notification à chaque tick.</p>
 *
 * <p>Interface fonctionnelle : une expression lambda suffit pour une
 * implémentation simple.</p>
 */
@FunctionalInterface
public interface SimulationListener {

    /**
     * Appelé après chaque tick, une fois tous les changements d'état
     * appliqués à la grille.
     *
     * @param tick numéro du tick écoulé (commence à 1)
     * @param grid grille dans son état mis à jour
     */
    void onTick(int tick, Grid grid);
}
