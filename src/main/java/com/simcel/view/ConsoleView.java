package com.simcel.view;

import com.simcel.model.Cell;
import com.simcel.model.CellState;
import com.simcel.model.Grid;

public class ConsoleView {

    private static final String CLEAR = "\033[H\033[2J";
    private static final String RESET = "\033[0m";

    // Couleurs foreground
    private static final String GRAY = "\033[90m";
    private static final String GREEN_BOLD = "\033[1;92m";
    private static final String RED_BOLD = "\033[1;91m";
    private static final String YELLOW_BOLD = "\033[1;33m";
    private static final String BG_BLACK    = "\033[40m";
    private static final String BLUE_BOLD = "\033[1;94m";
    private static final String WHITE_BOLD = "\033[1;97m";

    public void printLegend() {
        System.out.println("=== Simulation de Propagation de Feu ===");
        System.out.println(
            "  " + GRAY       + "░░" + RESET + " VIDE" +
            "  " + GREEN_BOLD + "██" + RESET + " SAIN" +
            "  " + RED_BOLD   + "▓▓" + RESET + " EN FEU" +
            "  " + BG_BLACK   + "  " + RESET + " BRÛLÉ" +
            "  " + BLUE_BOLD  + "~~" + RESET + " EAU" +
            "  " + WHITE_BOLD + "▒▒" + RESET + " ROCHER"
        );
        System.out.println("=========================================");
    }

    /** Efface le terminal puis affiche légende, stats et grille dans cet ordre. */
    public void render(int tick, Grid grid) {
        System.out.print(CLEAR);
        System.out.flush();

        printLegend();
        printStats(tick, grid);

        int width  = grid.getWidth();
        int height = grid.getHeight();

        StringBuilder sb = new StringBuilder((width * 10 + 1) * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(toColorBlock(grid.getCell(x, y)));
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }

    /**
     * Affiche les statistiques du tick courant sur une ligne.
     */
    public void printStats(int tick, Grid grid) {
        int sain = 0, enFeu = 0, brule = 0, total = 0;

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                CellState state = grid.getCell(x, y).getState();
                total++;
                if (state == CellState.SAIN) {
                    sain++; 
                }else if (state == CellState.EN_FEU) {
                    enFeu++; 
                }else if (state == CellState.BRULE) {
                    brule++;
                }
            }
        }

        double tauxDestruction = total > 0 ? (brule * 100.0 / total) : 0.0;
        System.out.printf(
                "Tick %4d │ "
                + GREEN_BOLD + "Sains: %5d" + RESET + " │ "
                + RED_BOLD + "En feu: %5d" + RESET + " │ "
                + YELLOW_BOLD + "Brûlés: %5d" + RESET + " │ "
                + "Destruction: %5.1f%%%n",
                tick, sain, enFeu, brule, tauxDestruction);
    }

    private String toColorBlock(Cell cell) {
        return switch (cell.getState()) {
            case VIDE ->
                GRAY + "░░" + RESET;
            case SAIN ->
                GREEN_BOLD + "██" + RESET;
            case EN_FEU ->
                RED_BOLD + "▓▓" + RESET;
            case BRULE ->
                BG_BLACK + "  " + RESET;
            case EAU ->
                BLUE_BOLD + "~~" + RESET;
            case ROCHER ->
                WHITE_BOLD + "▒▒" + RESET;
        };
    }
}
