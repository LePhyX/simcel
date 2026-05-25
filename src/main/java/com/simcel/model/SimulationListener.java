package com.simcel.model;

/**
 * Observateur du cycle de vie de la simulation (pattern Observer).
 *
 * <p>
 * Les implémentations sont enregistrées via
 * {@link FireSimulator#addListener(SimulationListener)} et reçoivent une
 * notification à chaque tick et à la fin de la simulation.</p>
 */
public interface SimulationListener {

    /**
     * Appelé après chaque tick de simulation, une fois tous les changements
     * d'état appliqués à la grille.
     *
     * @param tick numéro du tick qui vient de s'écouler (commence à 1)
     * @param grid référence à la grille dans son état mis à jour
     */
    void onTick(int tick, Grid grid);

    /**
     * Appelé lorsque la simulation se termine (fin naturelle ou arrêt forcé).
     */
    void onSimulationEnd();
}
