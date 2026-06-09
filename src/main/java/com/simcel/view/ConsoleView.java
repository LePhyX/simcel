package com.simcel.view;

import java.util.Scanner;

import com.simcel.controller.SimulationController;
import com.simcel.model.Cell;
import com.simcel.model.Environment;
import com.simcel.model.Grid;
import com.simcel.model.WindDirection;

/**
 * Vue console de la simulation utilisant les séquences d'échappement ANSI.
 *
 * <p>
 * Affiche la grille sous forme de blocs colorés, avec une légende et des
 * statistiques en temps réel. L'écran est effacé avant chaque rendu pour
 * simuler une animation.</p>
 *
 * <p>
 * <b>Compatibilité :</b> requiert un terminal supportant les codes ANSI (Linux,
 * macOS, Windows Terminal).</p>
 */
public class ConsoleView {

    private static final String CLEAR = "\033[H\033[2J";
    private static final String RESET = "\033[0m";
    private static final String GRAY = "\033[90m";
    private static final String GREEN_BOLD = "\033[1;92m";
    private static final String RED_BOLD = "\033[1;91m";
    private static final String YELLOW_BOLD = "\033[1;33m";
    private static final String BG_BLACK = "\033[40m";
    private static final String BLUE_BOLD = "\033[1;94m";
    private static final String WHITE_BOLD = "\033[1;97m";

    /**
     * Affiche la légende des couleurs sur la sortie standard.
     */
    public void printLegend() {
        System.out.println("=== Simulation de Propagation de Feu ===");
        System.out.println(
                "  " + GRAY + "░░" + RESET + " VIDE"
                + "  " + GREEN_BOLD + "██" + RESET + " SAIN"
                + "  " + RED_BOLD + "▓▓" + RESET + " EN FEU"
                + "  " + BG_BLACK + "  " + RESET + " BRÛLÉ"
                + "  " + BLUE_BOLD + "~~" + RESET + " EAU"
                + "  " + WHITE_BOLD + "▒▒" + RESET + " ROCHER"
        );
        System.out.println("=========================================");
    }

    /**
     * Efface le terminal puis affiche dans l'ordre : légende, statistiques,
     * grille.
     *
     * <p>
     * Le rendu est construit dans un {@link StringBuilder} avant d'être écrit
     * en une seule fois pour limiter le scintillement.</p>
     *
     * @param tick numéro du tick courant
     * @param grid grille à afficher
     */
    public void render(int tick, Grid grid) {
        System.out.print(CLEAR);
        System.out.flush();

        printLegend();
        printStats(tick, grid);

        int width = grid.getWidth();
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
     * Affiche une ligne de statistiques pour le tick courant : nombre de
     * cellules saines, en feu, brûlées et taux de destruction.
     *
     * @param tick numéro du tick courant
     * @param grid grille à analyser
     */
    public void printStats(int tick, Grid grid) {
        int sain = 0, enFeu = 0, brule = 0;
        int total = grid.getWidth() * grid.getHeight();

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                switch (grid.getCell(x, y).getState()) {
                    case SAIN ->
                        sain++;
                    case EN_FEU ->
                        enFeu++;
                    case BRULE ->
                        brule++;
                    default -> {
                        /* VIDE, EAU, ROCHER : ignorés */ }
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

    /**
     * Lance un thread daemon qui lit les commandes depuis stdin et les traite
     * via {@link #parseCommand}.
     *
     * @param env  environnement à modifier en temps réel
     * @param ctrl contrôleur dont la vitesse peut être ajustée
     */
    public void startCommandListener(Environment env, SimulationController ctrl) {
        Thread t = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("> ");
                System.out.flush();
                while (scanner.hasNextLine()) {
                    parseCommand(scanner.nextLine(), env, ctrl);
                    System.out.print("> ");
                    System.out.flush();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Analyse et exécute une commande textuelle.
     *
     * @param line ligne saisie par l'utilisateur
     * @param env  environnement cible
     * @param ctrl contrôleur cible
     */
    void parseCommand(String line, Environment env, SimulationController ctrl) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens[0].isEmpty()) {
            return;
        }
        switch (tokens[0].toLowerCase()) {
            case "wind" -> {
                if (tokens.length < 3) {
                    System.out.println("Usage : wind <direction> <intensité>");
                    return;
                }
                WindDirection dir;
                try {
                    dir = WindDirection.valueOf(tokens[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Direction invalide. Valeurs acceptées : N, NE, E, SE, S, SO, O, NO");
                    return;
                }
                int strength;
                try {
                    strength = Integer.parseInt(tokens[2]);
                } catch (NumberFormatException e) {
                    System.out.println("Valeur numérique attendue pour intensité");
                    return;
                }
                if (strength < Environment.MIN_WIND || strength > Environment.MAX_WIND) {
                    System.out.println("Intensité invalide. Valeurs acceptées : "
                            + Environment.MIN_WIND + " à " + Environment.MAX_WIND);
                    return;
                }
                env.setDirection(dir);
                env.setWindStrength(strength);
            }
            case "humidity" -> {
                if (tokens.length < 2) {
                    System.out.println("Usage : humidity <valeur>");
                    return;
                }
                int h;
                try {
                    h = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Valeur numérique attendue pour humidité");
                    return;
                }
                if (h < Environment.MIN_HUMIDITY || h > Environment.MAX_HUMIDITY) {
                    System.out.println("Humidité invalide. Valeurs acceptées : "
                            + Environment.MIN_HUMIDITY + " à " + Environment.MAX_HUMIDITY);
                    return;
                }
                env.setHumidity(h);
            }
            case "speed" -> {
                if (tokens.length < 2) {
                    System.out.println("Usage : speed <ms>");
                    return;
                }
                int ms;
                try {
                    ms = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Valeur numérique attendue pour speed");
                    return;
                }
                ctrl.setTickDelay(ms);
            }
            case "help" -> printHelp();
            default -> System.out.println("Commande inconnue. Tapez 'help' pour la liste des commandes.");
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------
    /**
     * Affiche la liste des commandes disponibles.
     */
    private void printHelp() {
        System.out.println("Commandes disponibles :");
        System.out.println("  wind <direction> <intensité>  — Modifier le vent        (ex : wind NE 3)");
        System.out.println("  humidity <valeur>             — Modifier l'humidité     (0–100)");
        System.out.println("  speed <ms>                    — Modifier la vitesse     (délai en ms)");
        System.out.println("  help                          — Afficher ce message");
    }

    /**
     * Retourne la séquence ANSI correspondant à l'état de la cellule.
     */
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
