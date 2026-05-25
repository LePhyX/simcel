package com.simcel.model;

/**
 * États du cycle de vie du contrôleur de simulation.
 *
 * <p>Les transitions autorisées sont :</p>
 * <pre>
 *   IDLE ──start()──► RUNNING ──pause()──► PAUSED
 *    ▲                   │                    │
 *    └────stop()─────────┘◄───────resume()────┘
 * </pre>
 */
public enum SimulationState {

    /** La simulation est arrêtée et réinitialisée. */
    IDLE,

    /** La boucle de simulation tourne dans un thread dédié. */
    RUNNING,

    /** La boucle est suspendue ; les ticks ne s'accumulent plus. */
    PAUSED
}
