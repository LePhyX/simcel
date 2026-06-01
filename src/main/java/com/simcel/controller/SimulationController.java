package com.simcel.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.simcel.model.FireSimulator;
import com.simcel.model.SimulationState;

/**
 * Contrôleur de la boucle de simulation.
 *
 * <p>
 * Orchestre le cycle de vie du {@link FireSimulator} en gérant un thread
 * planifié ({@link ScheduledExecutorService}) qui appelle {@code tick()} à
 * intervalle régulier. Toutes les méthodes publiques sont {@code synchronized}
 * pour garantir leur thread-safety.</p>
 *
 * <p>
 * Diagramme d'états :</p>
 * <pre>
 *   IDLE ──start()──► RUNNING ──pause()──► PAUSED
 *    ▲                   │                    │
 *    └────stop()─────────┘◄────────────────────┘
 * </pre>
 */
public class SimulationController {

    private final FireSimulator simulator;
    private volatile SimulationState state;
    private volatile int currentTick;
    private volatile int tickDelay;

    private ScheduledExecutorService executor;

    /**
     * Crée un contrôleur pour le simulateur donné.
     *
     * @param simulator simulateur à piloter, non {@code null}
     * @param tickDelay intervalle entre deux ticks en millisecondes (&gt; 0)
     */
    public SimulationController(FireSimulator simulator, int tickDelay) {
        this.simulator = simulator;
        this.state = SimulationState.IDLE;
        this.currentTick = 0;
        this.tickDelay = tickDelay;
    }

    /**
     * Lance la boucle de simulation dans un thread séparé.
     *
     * <p>
     * Sans effet si la simulation est déjà {@link SimulationState#RUNNING}.</p>
     */
    public synchronized void start() {
        if (state == SimulationState.RUNNING) {
            return;
        }
        state = SimulationState.RUNNING;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::doTick, 0, tickDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Suspend la boucle sans réinitialiser le compteur de ticks.
     *
     * <p>
     * Sans effet si la simulation n'est pas
     * {@link SimulationState#RUNNING}.</p>
     */
    public synchronized void pause() {
        if (state != SimulationState.RUNNING) {
            return;
        }
        state = SimulationState.PAUSED;
        shutdownExecutor();
    }

    /**
     * Exécute exactement un tick manuellement, quel que soit l'état courant.
     *
     * <p>
     * Sans effet si la simulation est {@link SimulationState#RUNNING} (pour
     * éviter les doubles appels concurrents).</p>
     */
    public synchronized void step() {
        if (state == SimulationState.RUNNING) {
            return;
        }
        doTick();
    }

    /**
     * Arrête la boucle et remet le compteur de ticks du contrôleur à zéro.
     *
     * <p>
     * L'état de la grille n'est pas modifié ; utiliser {@link #reset()} pour
     * revenir à l'état initial.</p>
     */
    public synchronized void stop() {
        shutdownExecutor();
        state = SimulationState.IDLE;
        currentTick = 0;
    }

    /**
     * Arrête la boucle, remet la grille dans son état initial et repasse en
     * {@link SimulationState#IDLE}.
     */
    public synchronized void reset() {
        stop();
        simulator.getGrid().reset();
        simulator.clearHistory();
    }

    /**
     * Recule la simulation d'un tick en restaurant l'état précédent.
     *
     * <p>Sans effet si la simulation est {@link SimulationState#RUNNING} ou
     * si l'historique est vide.</p>
     *
     * @return {@code true} si un état précédent a été restauré
     */
    public synchronized boolean stepBack() {
        if (state == SimulationState.RUNNING) return false;
        return simulator.stepBack();
    }

    /**
     * Modifie l'intervalle entre deux ticks.
     *
     * <p>
     * Si la simulation est en cours, la boucle est redémarrée avec le nouvel
     * intervalle.</p>
     *
     * @param ms nouvel intervalle en millisecondes (&gt; 0)
     */
    public synchronized void setTickDelay(int ms) {
        this.tickDelay = ms;
        if (state == SimulationState.RUNNING) {
            shutdownExecutor();
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this::doTick, 0, ms, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Retourne l'état courant du contrôleur.
     *
     * @return état courant, jamais {@code null}
     */
    public SimulationState getState() {
        return state;
    }

    /**
     * Retourne le nombre de ticks écoulés depuis le dernier {@link #stop()}.
     *
     * @return compteur de ticks, &ge; 0
     */
    public int getCurrentTick() {
        return currentTick;
    }

    /**
     * Retourne l'intervalle courant entre deux ticks.
     *
     * @return délai en millisecondes
     */
    public int getTickDelay() {
        return tickDelay;
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------
    private void doTick() {
        simulator.tick();
        currentTick++;
    }

    private void shutdownExecutor() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
