package com.simcel.model;

import java.util.ArrayList;
import java.util.List;

public class FireSimulator {

    private final Grid grid;
    private final Environment environment;
    private final List<SimulationListener> listeners;
    private int currentTick;

    public FireSimulator(Grid g, Environment e) {
        this.grid = g;
        this.environment = e;
        this.listeners = new ArrayList<>();
        this.currentTick = 0;
    }

    public void tick() {
        // Double buffering : snapshot des états avant tout calcul
        CellState[][] snapshot = new CellState[grid.getWidth()][grid.getHeight()];
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                snapshot[x][y] = grid.getCell(x, y).getState();
            }
        }

        // Phase propagation : on collecte les ignitions sans les appliquer
        List<Cell> aEnflammer = new ArrayList<>();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (snapshot[x][y] == CellState.EN_FEU) {
                    Cell src = grid.getCell(x, y);
                    for (Cell voisin : grid.getNeighbors(x, y)) {
                        if (voisin.getState() == CellState.SAIN) {
                            double p = computeInflammationProbability(src, voisin);
                            if (Math.random() < p) {
                                aEnflammer.add(voisin);
                            }
                        }
                    }
                }
            }
        }

        // Application des ignitions : toutes effectuées après le calcul
        for (Cell cell : aEnflammer) {
            cell.ignite();
        }

        // Phase extinction : basée sur le snapshot (cellules EN_FEU avant ce tick)
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (snapshot[x][y] == CellState.EN_FEU) {
                    Cell cell = grid.getCell(x, y);
                    cell.decrementBurnTime();
                    if (cell.getRemainingBurnTime() == 0) {
                        cell.burnOut();
                    }
                }
            }
        }

        currentTick++;
        notifyListeners(currentTick);
    }

    public double computeInflammationProbability(Cell src, Cell tgt) {
        return tgt.getType().getInflammability();
    }

    public void addListener(SimulationListener l) {
        listeners.add(l);
    }

    public void removeListener(SimulationListener l) {
        listeners.remove(l);
    }

    private void notifyListeners(int tick) {
        for (SimulationListener listener : listeners) {
            listener.onTick(tick, grid);
        }
    }
}
