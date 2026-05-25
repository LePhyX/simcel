package com.simcel.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Grille 2D de l'automate cellulaire.
 *
 * <p>
 * Gère la structure de données, l'initialisation aléatoire et la restauration
 * de l'état initial. Aucune logique de simulation ne doit figurer ici.</p>
 *
 * <p>
 * La grille utilise la convention {@code cells[y][x]} en interne, mais toutes
 * les méthodes publiques acceptent des paramètres dans l'ordre {@code (x, y)}
 * pour plus de clarté.</p>
 */
public class Grid {

    private final int width;
    private final int height;
    private final Cell[][] cells;
    private final Cell[][] initialCells;

    /**
     * Crée une grille vide de dimensions {@code width × height}.
     *
     * <p>
     * Toutes les cellules sont initialisées à {@link CellState#VIDE} avec le
     * type {@link CellType#PRAIRIE}.</p>
     *
     * @param width nombre de colonnes, doit être &gt; 0
     * @param height nombre de lignes, doit être &gt; 0
     */
    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[height][width];
        this.initialCells = new Cell[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell blank = new Cell(CellType.PRAIRIE);
                blank.setState(CellState.VIDE);
                cells[y][x] = blank;
                initialCells[y][x] = blank.copy();
            }
        }
    }

    /**
     * Indique si les coordonnées {@code (x, y)} sont dans les limites de la
     * grille.
     *
     * @param x colonne
     * @param y ligne
     * @return {@code true} si {@code 0 <= x < width} et {@code 0 <= y < height}
     */
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Retourne la cellule à la colonne {@code x}, ligne {@code y}.
     *
     * @param x colonne
     * @param y ligne
     * @return cellule correspondante, jamais {@code null}
     * @throws ArrayIndexOutOfBoundsException si les coordonnées sont hors
     * limites
     */
    public Cell getCell(int x, int y) {
        return cells[y][x];
    }

    /**
     * Remplace la cellule à la colonne {@code x}, ligne {@code y}.
     *
     * @param x colonne
     * @param y ligne
     * @param cell nouvelle cellule, non {@code null}
     * @throws ArrayIndexOutOfBoundsException si les coordonnées sont hors
     * limites
     */
    public void setCell(int x, int y, Cell cell) {
        cells[y][x] = cell;
    }

    /**
     * Retourne les voisins de Moore (8 directions) de la cellule
     * {@code (x, y)}.
     *
     * <p>
     * Les positions hors limites sont ignorées ; la liste ne contient jamais
     * {@code null}. Une cellule de coin n'a que 3 voisins, une cellule de bord
     * en a 5.</p>
     *
     * @param x colonne de la cellule centrale
     * @param y ligne de la cellule centrale
     * @return liste non nulle des cellules voisines valides (3 à 8 éléments)
     */
    public List<Cell> getNeighbors(int x, int y) {
        List<Cell> neighbors = new ArrayList<>(8);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (isInBounds(nx, ny)) {
                    neighbors.add(cells[ny][nx]);
                }
            }
        }
        return neighbors;
    }

    /**
     * Peuple la grille aléatoirement selon les densités fournies et mémorise
     * l'état résultant comme état initial (utilisé par {@link #reset()}).
     *
     * <p>
     * Les densités doivent être comprises entre 0 et 1 et leur somme ne doit
     * pas dépasser 1. Les cellules restantes reçoivent l'état
     * {@link CellState#VIDE}.</p>
     *
     * @param densityForest proportion de cellules {@link CellType#FORET}
     * @param densityPrairie proportion de cellules {@link CellType#PRAIRIE}
     * @param densityBroussailles proportion de cellules
     * {@link CellType#BROUSSAILLES}
     * @param densityHumide proportion de cellules {@link CellType#ZONE_HUMIDE}
     * @param densityUrbaine proportion de cellules
     * {@link CellType#ZONE_URBAINE}
     * @throws IllegalArgumentException si la somme des densités dépasse 1.0
     */
    public void initRandom(double densityForest, double densityPrairie,
            double densityBroussailles, double densityHumide,
            double densityUrbaine) {
        double sum = densityForest + densityPrairie + densityBroussailles
                + densityHumide + densityUrbaine;
        if (sum > 1.0 + 1e-9) {
            throw new IllegalArgumentException(
                    "La somme des densités ne doit pas dépasser 1.0 (valeur reçue : " + sum + ")");
        }

        double thForest = densityForest;
        double thPrairie = thForest + densityPrairie;
        double thBroussailles = thPrairie + densityBroussailles;
        double thHumide = thBroussailles + densityHumide;
        double thUrbaine = thHumide + densityUrbaine;

        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double r = random.nextDouble();
                Cell cell;
                if (r < thForest) {
                    cell = new Cell(CellType.FORET);
                } else if (r < thPrairie) {
                    cell = new Cell(CellType.PRAIRIE);
                } else if (r < thBroussailles) {
                    cell = new Cell(CellType.BROUSSAILLES);
                } else if (r < thHumide) {
                    cell = new Cell(CellType.ZONE_HUMIDE);
                } else if (r < thUrbaine) {
                    cell = new Cell(CellType.ZONE_URBAINE);
                } else {
                    cell = new Cell(CellType.PRAIRIE);
                    cell.setState(CellState.VIDE);
                }
                cells[y][x] = cell;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initialCells[y][x] = cells[y][x].copy();
            }
        }
    }

    /**
     * Restaure la grille dans l'état mémorisé lors du dernier appel à
     * {@link #initRandom}.
     *
     * <p>
     * La copie est profonde : aucune référence n'est partagée entre
     * {@code cells} et {@code initialCells}.</p>
     */
    public void reset() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = initialCells[y][x].copy();
            }
        }
    }

    /**
     * Place un foyer de feu sur la cellule {@code (x, y)} via
     * {@link Cell#ignite()}.
     *
     * <p>
     * Sans effet si la cellule n'est pas inflammable ou si les coordonnées sont
     * hors limites.</p>
     *
     * @param x colonne
     * @param y ligne
     */
    public void setFire(int x, int y) {
        if (isInBounds(x, y)) {
            cells[y][x].ignite();
        }
    }

    /**
     * Retourne le nombre de colonnes.
     *
     * @return largeur de la grille
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retourne le nombre de lignes.
     *
     * @return hauteur de la grille
     */
    public int getHeight() {
        return height;
    }
}
