package com.simcel.view;

import com.simcel.model.Cell;
import com.simcel.model.CellState;
import com.simcel.model.Grid;

public class ConsoleView {

    private static final String CLEAR = "\033[H\033[2J";

    public void printLegend() {
        System.out.println("=== Simulation de Propagation de Feu ===");
        System.out.println("Légende : '.' VIDE  'V' SAIN  'F' EN_FEU  'X' BRULÉ  '~' EAU  'R' ROCHER");
        System.out.println("=========================================");
    }

    /** Efface le terminal puis affiche la grille en ASCII. */
    public void render(Grid grid) {
        System.out.print(CLEAR);
        System.out.flush();

        int width  = grid.getWidth();
        int height = grid.getHeight();

        StringBuilder sb = new StringBuilder((width + 1) * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(toChar(grid.getCell(x, y)));
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }

    /** Affiche les statistiques du tick courant sur une ligne. */
    public void printStats(int tick, Grid grid) {
        int sain = 0, enFeu = 0, brule = 0, total = 0;

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                CellState state = grid.getCell(x, y).getState();
                total++;
                if (state == CellState.SAIN)    sain++;
                else if (state == CellState.EN_FEU) enFeu++;
                else if (state == CellState.BRULE)  brule++;
            }
        }

        double tauxDestruction = total > 0 ? (brule * 100.0 / total) : 0.0;
        System.out.printf("Tick %4d | Sains: %5d | En feu: %5d | Brûlés: %5d | Destruction: %5.1f%%%n",
                tick, sain, enFeu, brule, tauxDestruction);
    }

    private char toChar(Cell cell) {
        return switch (cell.getState()) {
            case VIDE   -> '.';
            case SAIN   -> 'V';
            case EN_FEU -> 'F';
            case BRULE  -> 'X';
            case EAU    -> '~';
            case ROCHER -> 'R';
        };
    }
}
