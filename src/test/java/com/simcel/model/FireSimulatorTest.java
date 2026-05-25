package com.simcel.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireSimulatorTest {

    private void toutesLesCellulesASain(Grid grid) {
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                grid.getCell(x, y).setState(CellState.SAIN);
            }
        }
    }

    private FireSimulator simulateurProba1(Grid grid, Environment env) {
        return new FireSimulator(grid, env) {
            @Override
            public double computeInflammationProbability(Cell src, Cell tgt) {
                return 1.0;
            }
        };
    }

    @Test
    void tick_propagationBasique_tousLesVoisinsSainsDeviennentEnFeu() {
        Grid grid = new Grid(3, 3);
        toutesLesCellulesASain(grid);
        grid.setFire(1, 1);

        FireSimulator sim = simulateurProba1(grid, new Environment());
        sim.tick();

        for (Cell voisin : grid.getNeighbors(1, 1)) {
            assertEquals(CellState.EN_FEU, voisin.getState());
        }
    }

    @Test
    void tick_extinctionPrairie_cellulesDevientBruleApres3Ticks() {
        // PRAIRIE a burnDuration = 3
        Grid grid = new Grid(1, 1);
        grid.getCell(0, 0).setState(CellState.SAIN);
        grid.setFire(0, 0);

        FireSimulator sim = new FireSimulator(grid, new Environment());

        sim.tick();
        assertEquals(CellState.EN_FEU, grid.getCell(0, 0).getState());
        sim.tick();
        assertEquals(CellState.EN_FEU, grid.getCell(0, 0).getState());
        sim.tick();
        assertEquals(CellState.BRULE, grid.getCell(0, 0).getState());
    }

    @Test
    void tick_voisinsEauEtRocher_restentInchanges() {
        // Grille 3x1 : EAU | EN_FEU | ROCHER
        Grid grid = new Grid(3, 1);
        grid.getCell(0, 0).setState(CellState.EAU);
        grid.getCell(1, 0).setState(CellState.SAIN);
        grid.setFire(1, 0);
        grid.getCell(2, 0).setState(CellState.ROCHER);

        FireSimulator sim = simulateurProba1(grid, new Environment());
        sim.tick();

        assertEquals(CellState.EAU, grid.getCell(0, 0).getState());
        assertEquals(CellState.ROCHER, grid.getCell(2, 0).getState());
    }

    @Test
    void tick_doubleBuffering_celluleEnflammeePendantTickNNePropagePasAuMemeTickN() {
        // Grille 3x1 : A(0,0) EN_FEU | B(1,0) SAIN | C(2,0) SAIN
        // A et C ne sont pas voisins (distance 2). B s'enflamme au tick 1.
        // C doit rester SAIN après le tick 1, puis devenir EN_FEU au tick 2.
        Grid grid = new Grid(3, 1);
        toutesLesCellulesASain(grid);
        grid.setFire(0, 0);

        FireSimulator sim = simulateurProba1(grid, new Environment());

        sim.tick();
        assertEquals(CellState.EN_FEU, grid.getCell(1, 0).getState(),
                "B doit être EN_FEU après tick 1");
        assertEquals(CellState.SAIN, grid.getCell(2, 0).getState(),
                "C doit rester SAIN après tick 1 : double buffering");

        sim.tick();
        assertEquals(CellState.EN_FEU, grid.getCell(2, 0).getState(),
                "C doit être EN_FEU après tick 2");
    }
}
