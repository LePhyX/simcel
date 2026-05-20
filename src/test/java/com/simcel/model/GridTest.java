package com.simcel.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridTest {

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid(3, 3);
    }

    // ------------------------------------------------------------------ //
    //  isInBounds                                                          //
    // ------------------------------------------------------------------ //

    @Test
    void isInBounds_coordonneeValide_retourneTrue() {
        assertTrue(grid.isInBounds(1, 1));
    }

    @Test
    void isInBounds_origine_retourneTrue() {
        assertTrue(grid.isInBounds(0, 0));
    }

    @Test
    void isInBounds_limiteExacteMaximum_retourneTrue() {
        assertTrue(grid.isInBounds(2, 2));
    }

    @Test
    void isInBounds_xNegatif_retourneFalse() {
        assertFalse(grid.isInBounds(-1, 0));
    }

    @Test
    void isInBounds_yNegatif_retourneFalse() {
        assertFalse(grid.isInBounds(0, -1));
    }

    @Test
    void isInBounds_xEgalWidth_retourneFalse() {
        assertFalse(grid.isInBounds(3, 0));
    }

    @Test
    void isInBounds_yEgalHeight_retourneFalse() {
        assertFalse(grid.isInBounds(0, 3));
    }

    // ------------------------------------------------------------------ //
    //  getNeighbors                                                        //
    // ------------------------------------------------------------------ //

    @Test
    void getNeighbors_coinHautGauche_retourne3Voisins() {
        assertEquals(3, grid.getNeighbors(0, 0).size());
    }

    @Test
    void getNeighbors_coinHautDroit_retourne3Voisins() {
        assertEquals(3, grid.getNeighbors(2, 0).size());
    }

    @Test
    void getNeighbors_coinBasGauche_retourne3Voisins() {
        assertEquals(3, grid.getNeighbors(0, 2).size());
    }

    @Test
    void getNeighbors_coinBasDroit_retourne3Voisins() {
        assertEquals(3, grid.getNeighbors(2, 2).size());
    }

    @Test
    void getNeighbors_bordHaut_retourne5Voisins() {
        assertEquals(5, grid.getNeighbors(1, 0).size());
    }

    @Test
    void getNeighbors_bordBas_retourne5Voisins() {
        assertEquals(5, grid.getNeighbors(1, 2).size());
    }

    @Test
    void getNeighbors_bordGauche_retourne5Voisins() {
        assertEquals(5, grid.getNeighbors(0, 1).size());
    }

    @Test
    void getNeighbors_bordDroit_retourne5Voisins() {
        assertEquals(5, grid.getNeighbors(2, 1).size());
    }

    @Test
    void getNeighbors_celluleCentrale_retourne8Voisins() {
        assertEquals(8, grid.getNeighbors(1, 1).size());
    }

    @Test
    void getNeighbors_neRetourneJamaisNull() {
        List<Cell> center  = grid.getNeighbors(1, 1);
        List<Cell> corner  = grid.getNeighbors(0, 0);
        assertNotNull(center);
        assertNotNull(corner);
        center.forEach(c -> assertNotNull(c, "un voisin ne doit pas être null"));
        corner.forEach(c -> assertNotNull(c, "un voisin ne doit pas être null"));
    }

    // ------------------------------------------------------------------ //
    //  reset                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void reset_apresSetFire_restaureEtatSain() {
        // 100 % forêt → toutes les cellules démarrent SAIN
        grid.initRandom(1.0, 0.0, 0.0, 0.0, 0.0);

        assertEquals(CellState.SAIN, grid.getCell(1, 1).getState());

        grid.setFire(1, 1);
        assertEquals(CellState.EN_FEU, grid.getCell(1, 1).getState());

        grid.reset();
        assertEquals(CellState.SAIN, grid.getCell(1, 1).getState());
    }

    @Test
    void reset_restaureToutesLesCellules() {
        grid.initRandom(1.0, 0.0, 0.0, 0.0, 0.0);

        grid.setFire(0, 0);
        grid.setFire(1, 1);
        grid.setFire(2, 2);

        grid.reset();

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                assertEquals(CellState.SAIN, grid.getCell(x, y).getState(),
                        "La cellule (" + x + "," + y + ") devrait être SAIN après reset");
            }
        }
    }

    @Test
    void reset_copieProfondeInitialCells_deuxiemeResetFonctionneCorrectement() {
        grid.initRandom(1.0, 0.0, 0.0, 0.0, 0.0);

        // Premier cycle modification / reset
        grid.setFire(1, 1);
        grid.reset();
        assertEquals(CellState.SAIN, grid.getCell(1, 1).getState());

        // Deuxième cycle : initialCells ne doit pas avoir été altéré par le premier reset
        grid.setFire(1, 1);
        grid.reset();
        assertEquals(CellState.SAIN, grid.getCell(1, 1).getState());
    }

    @Test
    void reset_copieProfondeRetourneNouvelObjet_pasDeReferencePartageeAvecInitialCells() {
        grid.initRandom(1.0, 0.0, 0.0, 0.0, 0.0);
        grid.reset();

        // On récupère la référence retournée par reset() et on la modifie directement
        Cell cellApresReset = grid.getCell(1, 1);
        cellApresReset.setState(CellState.BRULE);

        // Un second reset() doit pouvoir restaurer SAIN, prouvant que initialCells
        // n'a pas été altéré (i.e. cellApresReset n'est pas l'objet de initialCells)
        grid.reset();
        assertEquals(CellState.SAIN, grid.getCell(1, 1).getState());
    }

    // ------------------------------------------------------------------ //
    //  initRandom — validation de la somme des densités                   //
    // ------------------------------------------------------------------ //

    @Test
    void initRandom_sommeDensitesDepasseUn_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> grid.initRandom(0.5, 0.4, 0.3, 0.0, 0.0));
    }

    @Test
    void initRandom_sommeEgaleUn_neLeveAucuneException() {
        assertDoesNotThrow(() -> grid.initRandom(0.2, 0.2, 0.2, 0.2, 0.2));
    }

    @Test
    void initRandom_sommeInferieureUn_cellsRestantesVides() {
        // 50 % forêt, 50 % vide attendu
        grid.initRandom(0.5, 0.0, 0.0, 0.0, 0.0);
        // Aucune cellule ne doit être null
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                assertNotNull(grid.getCell(x, y));
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  setFire — états non inflammables                                    //
    // ------------------------------------------------------------------ //

    @Test
    void setFire_celluleEau_etatInchange() {
        // ZONE_HUMIDE → état EAU, non inflammable
        grid.initRandom(0.0, 0.0, 0.0, 1.0, 0.0);
        assertEquals(CellState.EAU, grid.getCell(1, 1).getState());

        grid.setFire(1, 1);
        assertEquals(CellState.EAU, grid.getCell(1, 1).getState());
    }

    @Test
    void setFire_celluleDejaEnFeu_etatInchange() {
        grid.initRandom(1.0, 0.0, 0.0, 0.0, 0.0);
        grid.setFire(1, 1);
        assertEquals(CellState.EN_FEU, grid.getCell(1, 1).getState());

        // Deuxième setFire sur une cellule déjà EN_FEU
        grid.setFire(1, 1);
        assertEquals(CellState.EN_FEU, grid.getCell(1, 1).getState());
    }

    @Test
    void setFire_celluleRocher_etatInchange() {
        grid.initRandom(1.0, 0.0, 0.0, 0.0, 0.0);
        grid.getCell(1, 1).setState(CellState.ROCHER);

        grid.setFire(1, 1);
        assertEquals(CellState.ROCHER, grid.getCell(1, 1).getState());
    }

    @Test
    void setFire_coordonneesHorsLimites_sansException() {
        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> grid.setFire(-1, 0));
        assertDoesNotThrow(() -> grid.setFire(0, 99));
    }
}
