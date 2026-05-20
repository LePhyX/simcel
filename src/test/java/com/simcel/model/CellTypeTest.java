package com.simcel.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellTypeTest {

    @Test
    void foretHasCorrectValues() {
        assertEquals(0.7, CellType.FORET.getInflammability());
        assertEquals(8, CellType.FORET.getBurnDuration());
    }

    @Test
    void prairieHasCorrectValues() {
        assertEquals(0.9, CellType.PRAIRIE.getInflammability());
        assertEquals(3, CellType.PRAIRIE.getBurnDuration());
    }

    @Test
    void broussaillesHasCorrectValues() {
        assertEquals(0.5, CellType.BROUSSAILLES.getInflammability());
        assertEquals(5, CellType.BROUSSAILLES.getBurnDuration());
    }

    @Test
    void zoneHumideHasCorrectValues() {
        assertEquals(0.2, CellType.ZONE_HUMIDE.getInflammability());
        assertEquals(12, CellType.ZONE_HUMIDE.getBurnDuration());
    }

    @Test
    void zoneUrbainHasCorrectValues() {
        assertEquals(0.3, CellType.ZONE_URBAINE.getInflammability());
        assertEquals(6, CellType.ZONE_URBAINE.getBurnDuration());
    }

    @Test
    void prairieMoreFlammableThanZoneHumide() {
        assertTrue(CellType.PRAIRIE.getInflammability() > CellType.ZONE_HUMIDE.getInflammability());
    }

    @Test
    void foretBurnsLongerThanPrairie() {
        assertTrue(CellType.FORET.getBurnDuration() > CellType.PRAIRIE.getBurnDuration());
    }

    @Test
    void allInflammabilitiesBetweenZeroAndOne() {
        for (CellType type : CellType.values()) {
            assertTrue(type.getInflammability() >= 0.0 && type.getInflammability() <= 1.0,
                type.name() + " inflammability out of range: " + type.getInflammability());
        }
    }

    @Test
    void allBurnDurationsStrictlyPositive() {
        for (CellType type : CellType.values()) {
            assertTrue(type.getBurnDuration() > 0,
                type.name() + " burnDuration must be > 0, got: " + type.getBurnDuration());
        }
    }
}
