package com.simcel.controller;

import java.io.File;
import java.io.IOException;

import com.simcel.model.FireSimulator;
import com.simcel.model.SimulationIO;
import com.simcel.model.SimulationSnapshot;
import com.simcel.model.SimulationState;

/**
 * Contrôleur de la boucle de simulation.
 *
 * <p>
 * Orchestre le cycle de vie du {@link FireSimulator} en gérant un thread
 * ({@link Thread}) qui appelle {@code tick()} à intervalle régulier via
 * {@link Thread#sleep(long)}.</p>
 *
 * <p>
 * Diagramme d'états :</p>
 * <pre>
 *   IDLE ──start()──► RUNNING ──pause()──► PAUSED
 *    ▲                   │                     │
 *    └────stop()─────────┘◄────────────────────┘
 * </pre>
 */
public class SimulationController {

    private final FireSimulator simulator;
    private SimulationState state;
    private int tickDelay;

    private Thread simulationThread;

    /**
     * Crée un contrôleur pour le simulateur donné.
     *
     * @param simulator simulateur à piloter, non {@code null}
     * @param tickDelay intervalle entre deux ticks en millisecondes (&gt; 0)
     */
    public SimulationController(FireSimulator simulator, int tickDelay) {
        this.simulator = simulator;
        this.state = SimulationState.IDLE;
        this.tickDelay = tickDelay;
    }

    /**
     * Lance la boucle de simulation dans un thread séparé. Sans effet si la
     * simulation est déjà {@link SimulationState#RUNNING}.
     */
    public void start() {
        if (state == SimulationState.RUNNING) {
            return;
        }
        state = SimulationState.RUNNING;
        simulationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    doTick();
                    try {
                        Thread.sleep(tickDelay);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    /**
     * Suspend la boucle sans réinitialiser l'état de la grille. Sans effet si
     * la simulation n'est pas {@link SimulationState#RUNNING}.
     */
    public void pause() {
        if (state != SimulationState.RUNNING) {
            return;
        }
        state = SimulationState.PAUSED;
        stopThread();
    }

    /**
     * Exécute exactement un tick manuellement. Sans effet si la simulation est
     * {@link SimulationState#RUNNING}.
     */
    public void step() {
        if (state == SimulationState.RUNNING) {
            return;
        }
        doTick();
    }

    /**
     * Arrête la boucle et repasse en {@link SimulationState#IDLE}. L'état de la
     * grille n'est pas modifié ; utiliser {@link #reset()} pour revenir à
     * l'état initial.
     */
    public void stop() {
        stopThread();
        state = SimulationState.IDLE;
    }

    /**
     * Arrête la boucle, remet la grille dans son état initial et repasse en
     * {@link SimulationState#IDLE}.
     */
    public void reset() {
        stop();
        simulator.getGrid().reset();
        simulator.clearHistory();
    }

    /**
     * Recule la simulation d'un tick en restaurant l'état précédent. Sans effet
     * si la simulation est {@link SimulationState#RUNNING} ou si l'historique
     * est vide.
     *
     * @return {@code true} si un état précédent a été restauré
     */
    public boolean stepBack() {
        if (state == SimulationState.RUNNING) {
            return false;
        }
        return simulator.stepBack();
    }

    /**
     * Modifie l'intervalle entre deux ticks. Si la simulation est en cours, la
     * boucle est redémarrée avec le nouvel intervalle.
     *
     * @param ms nouvel intervalle en millisecondes (&gt; 0)
     */
    public void setTickDelay(int ms) {
        this.tickDelay = ms;
        if (ms >= 1 && state == SimulationState.RUNNING) {
            stopThread();
            simulationThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        doTick();
                        try {
                            Thread.sleep(tickDelay);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
    }

    /**
     * Retourne l'intervalle courant entre deux ticks.
     *
     * @return délai en millisecondes
     */
    public int getTickDelay() {
        return tickDelay;
    }

    /**
     * Sauvegarde l'état courant de la simulation dans un fichier binaire.
     *
     * @param file fichier destination
     * @throws IOException si l'écriture échoue
     */
    public void saveToFile(File file) throws IOException {
        SimulationIO.save(file, simulator.createSnapshot());
    }

    /**
     * Restaure l'état de la simulation depuis un fichier binaire. Met la
     * simulation en pause si elle est en cours, puis applique le snapshot.
     *
     * @param file fichier source
     * @throws IOException si la lecture échoue
     * @throws ClassNotFoundException si le format de fichier est incompatible
     * @throws IllegalArgumentException si les dimensions de la grille diffèrent
     */
    public void loadFromFile(File file) throws IOException, ClassNotFoundException {
        if (state == SimulationState.RUNNING) {
            pause();
        }
        SimulationSnapshot snapshot = SimulationIO.load(file);
        simulator.applySnapshot(snapshot);
        state = SimulationState.PAUSED;
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    private void doTick() {
        simulator.tick();
    }

    private void stopThread() {
        if (simulationThread != null) {
            simulationThread.interrupt();
            simulationThread = null;
        }
    }
}
