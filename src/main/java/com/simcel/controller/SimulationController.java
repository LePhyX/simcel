package com.simcel.controller;

import com.simcel.model.FireSimulator;
import com.simcel.model.SimulationState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimulationController {

    private final FireSimulator simulator;
    private volatile SimulationState state;
    private volatile int currentTick;
    private volatile int tickDelay;

    private ScheduledExecutorService executor;

    public SimulationController(FireSimulator simulator, int tickDelay) {
        this.simulator   = simulator;
        this.state       = SimulationState.IDLE;
        this.currentTick = 0;
        this.tickDelay   = tickDelay;
    }

    /** Lance la boucle de simulation dans un thread séparé. No-op si déjà RUNNING. */
    public synchronized void start() {
        if (state == SimulationState.RUNNING) return;
        state    = SimulationState.RUNNING;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::doTick, 0, tickDelay, TimeUnit.MILLISECONDS);
    }

    /** Suspend la boucle sans réinitialiser currentTick. No-op si non RUNNING. */
    public synchronized void pause() {
        if (state != SimulationState.RUNNING) return;
        state = SimulationState.PAUSED;
        shutdownExecutor();
    }

    /**
     * Exécute exactement un tick manuellement.
     * No-op si l'état est RUNNING.
     */
    public synchronized void step() {
        if (state == SimulationState.RUNNING) return;
        doTick();
    }

    /** Arrête la boucle et remet currentTick à 0. */
    public synchronized void stop() {
        shutdownExecutor();
        state       = SimulationState.IDLE;
        currentTick = 0;
    }

    /** Arrête la boucle, remet la grille dans son état initial et repasse en IDLE. */
    public synchronized void reset() {
        stop();
        simulator.getGrid().reset();
    }

    /** Modifie l'intervalle entre deux ticks ; redémarre la boucle si elle est RUNNING. */
    public synchronized void setTickDelay(int ms) {
        this.tickDelay = ms;
        if (state == SimulationState.RUNNING) {
            shutdownExecutor();
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this::doTick, 0, ms, TimeUnit.MILLISECONDS);
        }
    }

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

    public SimulationState getState()  { return state; }
    public int getCurrentTick()        { return currentTick; }
    public int getTickDelay()          { return tickDelay; }
}
