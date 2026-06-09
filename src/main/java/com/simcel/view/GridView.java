package com.simcel.view;

import com.simcel.model.Cell;
import com.simcel.model.CellState;
import com.simcel.model.CellType;
import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Vue graphique de la grille de simulation.
 *
 * <p>Étend {@link Canvas} et implémente {@link SimulationListener} pour se
 * redessiner automatiquement après chaque tick. Le rendu est toujours exécuté
 * sur le thread JavaFX via {@link Platform#runLater}.</p>
 *
 * <p><b>Thread safety :</b> un snapshot des couleurs est capturé sur le thread
 * de simulation avant d'être transmis au thread JavaFX, évitant ainsi toute
 * race condition avec le tick suivant.</p>
 *
 * <p><b>Couleurs :</b> les cellules à l'état {@link CellState#SAIN} sont
 * colorées selon leur {@link CellType} (via {@link CellType#getHealthyColor()})
 * afin de distinguer visuellement les types de terrain. Les autres états
 * utilisent la couleur de {@link CellState#getColor()}.</p>
 *
 * <p><b>Performance :</b> les objets {@link Color} sont pré-calculés une fois
 * par état/type pour éviter le parsing hexadécimal à chaque cellule à chaque tick.</p>
 */
public final class GridView extends Canvas implements SimulationListener {

    /**
     * Cache des couleurs pour les états non-SAIN, indexé par {@link CellState#ordinal()}.
     */
    private static final Color[] STATE_COLORS;

    /**
     * Cache des couleurs pour les cellules {@link CellState#SAIN},
     * indexé par {@link CellType#ordinal()}.
     */
    private static final Color[] TYPE_SAIN_COLORS;

    static {
        CellState[] states = CellState.values();
        STATE_COLORS = new Color[states.length];
        for (CellState s : states) {
            STATE_COLORS[s.ordinal()] = Color.web(s.getColor());
        }

        CellType[] types = CellType.values();
        TYPE_SAIN_COLORS = new Color[types.length];
        for (CellType t : types) {
            TYPE_SAIN_COLORS[t.ordinal()] = Color.web(t.getHealthyColor());
        }
    }

    /** Fond de la grille visible entre les cellules (joue le rôle de grille). */
    private static final Color GRID_BG   = Color.web("#0a0a1a");
    /** Espace entre cellules en pixels. */
    private static final double GRID_GAP = 1.0;

    private final double cellSize;

    /**
     * Crée la vue pour la grille donnée avec la taille de cellule indiquée.
     *
     * @param grid     grille à représenter
     * @param cellSize côté d'une cellule en pixels (recommandé : &ge; 4)
     */
    public GridView(Grid grid, double cellSize) {
        super(grid.getWidth() * cellSize, grid.getHeight() * cellSize);
        this.cellSize = cellSize;
        refresh(grid);
    }

    // -------------------------------------------------------------------------
    // SimulationListener
    // -------------------------------------------------------------------------

    /**
     * Appelé après chaque tick : capture un snapshot des couleurs sur le thread
     * de simulation, puis délègue le rendu au thread JavaFX.
     *
     * @param tick numéro du tick
     * @param grid grille mise à jour
     */
    @Override
    public void onTick(int tick, Grid grid) {
        Color[][] snapshot = snapshot(grid);
        Platform.runLater(() -> draw(snapshot));
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Redessine la grille immédiatement à partir de son état courant.
     * Doit être appelé depuis le thread JavaFX.
     *
     * @param grid grille à afficher
     */
    public void refresh(Grid grid) {
        draw(snapshot(grid));
    }

    /**
     * Convertit des coordonnées pixel (position d'un clic souris) en
     * coordonnées de grille {@code [col, ligne]}.
     *
     * @param pixelX abscisse en pixels dans le canvas
     * @param pixelY ordonnée en pixels dans le canvas
     * @return tableau {@code [x, y]} dans l'espace grille, ou {@code null} si
     *         hors limites
     */
    public int[] getCellAt(double pixelX, double pixelY) {
        int col = (int) (pixelX / cellSize);
        int row = (int) (pixelY / cellSize);
        int cols = (int) (getWidth()  / cellSize);
        int rows = (int) (getHeight() / cellSize);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return null;
        return new int[]{col, row};
    }

    /**
     * Retourne la taille d'une cellule en pixels.
     *
     * @return taille en pixels
     */
    public double getCellSize() {
        return cellSize;
    }

    // -------------------------------------------------------------------------
    // Rendu interne
    // -------------------------------------------------------------------------

    private Color[][] snapshot(Grid grid) {
        int w = grid.getWidth();
        int h = grid.getHeight();
        Color[][] snap = new Color[w][h];
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                snap[x][y] = colorOf(grid.getCell(x, y));
        return snap;
    }

    /**
     * Retourne la couleur d'affichage d'une cellule.
     * Les cellules {@link CellState#SAIN} utilisent la couleur de leur
     * {@link CellType}; les autres utilisent la couleur de leur {@link CellState}.
     */
    private Color colorOf(Cell cell) {
        if (cell.getState() == CellState.SAIN) {
            return TYPE_SAIN_COLORS[cell.getType().ordinal()];
        }
        return STATE_COLORS[cell.getState().ordinal()];
    }

    private void draw(Color[][] snapshot) {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        int cols = snapshot.length;
        int rows = snapshot[0].length;
        double fill = cellSize - GRID_GAP;

        gc.setFill(GRID_BG);
        gc.fillRect(0, 0, w, h);

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                gc.setFill(snapshot[x][y]);
                gc.fillRect(x * cellSize, y * cellSize, fill, fill);
            }
        }
    }
}
