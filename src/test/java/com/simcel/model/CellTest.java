package com.simcel.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    @Test
    void initialStateIsSain() {
        Cell cell = new Cell(CellType.FORET);
        assertEquals(CellState.SAIN, cell.getState());
    }

    @Test
    void initialTypeIsPreserved() {
        Cell cell = new Cell(CellType.PRAIRIE);
        assertEquals(CellType.PRAIRIE, cell.getType());
    }

    @Test
    void initialRemainingBurnTimeEqualsTypeBurnDuration() {
        Cell cell = new Cell(CellType.FORET);
        assertEquals(CellType.FORET.getBurnDuration(), cell.getRemainingBurnTime());
    }

    @Test
    void igniteOnSainCellSetsEnFeu() {
        Cell cell = new Cell(CellType.PRAIRIE);
        cell.ignite();
        assertEquals(CellState.EN_FEU, cell.getState());
    }

    @Test
    void igniteResetsRemainingBurnTime() {
        Cell cell = new Cell(CellType.FORET);
        cell.decrementBurnTime();
        cell.ignite();
        assertEquals(CellType.FORET.getBurnDuration(), cell.getRemainingBurnTime());
    }

    @Test
    void igniteOnEauDoesNothing() {
        Cell cell = new Cell(CellType.ZONE_HUMIDE);
        cell.setState(CellState.EAU);
        int burnTimeBefore = cell.getRemainingBurnTime();
        cell.ignite();
        assertEquals(CellState.EAU, cell.getState());
        assertEquals(burnTimeBefore, cell.getRemainingBurnTime());
    }

    @Test
    void igniteOnRocherDoesNothing() {
        Cell cell = new Cell(CellType.ZONE_URBAINE);
        cell.setState(CellState.ROCHER);
        cell.ignite();
        assertEquals(CellState.ROCHER, cell.getState());
    }

    @Test
    void igniteOnBruleDoesNothing() {
        Cell cell = new Cell(CellType.PRAIRIE);
        cell.setState(CellState.BRULE);
        cell.ignite();
        assertEquals(CellState.BRULE, cell.getState());
    }

    @Test
    void decrementBurnTimeReducesCounter() {
        Cell cell = new Cell(CellType.FORET);
        int before = cell.getRemainingBurnTime();
        cell.decrementBurnTime();
        assertEquals(before - 1, cell.getRemainingBurnTime());
    }

    @Test
    void decrementBurnTimeDoesNotGoBelowZero() {
        Cell cell = new Cell(CellType.PRAIRIE);
        cell.burnOut();
        cell.decrementBurnTime();
        assertEquals(0, cell.getRemainingBurnTime());
    }

    @Test
    void burnOutSetsBruleAndZero() {
        Cell cell = new Cell(CellType.FORET);
        cell.burnOut();
        assertEquals(CellState.BRULE, cell.getState());
        assertEquals(0, cell.getRemainingBurnTime());
    }

    @Test
    void fullTransitionSainToEnFeuToBrule() {
        Cell cell = new Cell(CellType.BROUSSAILLES);
        assertEquals(CellState.SAIN, cell.getState());
        cell.ignite();
        assertEquals(CellState.EN_FEU, cell.getState());
        cell.burnOut();
        assertEquals(CellState.BRULE, cell.getState());
        assertEquals(0, cell.getRemainingBurnTime());
    }
}
