package com.simcel.view;

import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Graphique d'évolution temporelle des populations de cellules.
 *
 * <p>Implémente {@link SimulationListener} et se redessine après chaque tick.
 * Le graphique affiche trois courbes : cellules saines, en feu et brûlées,
 * tracées sur un {@link Canvas} sans dépendance à {@code javafx.charts}.</p>
 *
 * <p>Les données sont collectées sur le thread de simulation ; seul le
 * redessinage est délégué au thread JavaFX via {@link Platform#runLater}.</p>
 */
public class ChartView extends Canvas implements SimulationListener {

    private static final int    MAX_POINTS  = 300;
    private static final double PADDING     = 30.0;
    private static final Color  COLOR_SAIN  = Color.web("#228B22");
    private static final Color  COLOR_FEU   = Color.web("#FF4500");
    private static final Color  COLOR_BRULE = Color.web("#888888");

    private final List<Integer> dataSain  = new ArrayList<>();
    private final List<Integer> dataFeu   = new ArrayList<>();
    private final List<Integer> dataBrule = new ArrayList<>();

    private int totalCells = 1;

    /**
     * Crée le graphique avec les dimensions données.
     *
     * @param width  largeur en pixels
     * @param height hauteur en pixels
     */
    public ChartView(double width, double height) {
        super(width, height);
        drawEmpty();
    }

    /**
     * Collecte les comptages sur le thread de simulation, puis délègue le
     * redessinage au thread JavaFX.
     *
     * @param tick numéro du tick
     * @param grid grille courante
     */
    @Override
    public void onTick(int tick, Grid grid) {
        int sain = 0, enFeu = 0, brule = 0;
        totalCells = grid.getWidth() * grid.getHeight();

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                switch (grid.getCell(x, y).getState()) {
                    case SAIN   -> sain++;
                    case EN_FEU -> enFeu++;
                    case BRULE  -> brule++;
                    default     -> {}
                }
            }
        }

        final int fSain = sain, fFeu = enFeu, fBrule = brule;
        Platform.runLater(() -> {
            addPoint(fSain, fFeu, fBrule);
            redraw();
        });
    }

    /**
     * Aucune action requise à la fin de simulation : le graphique conserve
     * les courbes du dernier tick.
     */
    @Override
    public void onSimulationEnd() {}

    /**
     * Vide toutes les séries et redessine le graphique vide.
     * Doit être appelé depuis le thread JavaFX.
     */
    public void clear() {
        dataSain.clear();
        dataFeu.clear();
        dataBrule.clear();
        drawEmpty();
    }

    // -------------------------------------------------------------------------
    // Rendu interne
    // -------------------------------------------------------------------------

    /**
     * Ajoute un point aux trois séries. Si la fenêtre glissante est atteinte
     * ({@link #MAX_POINTS}), le point le plus ancien est supprimé.
     */
    private void addPoint(int sain, int feu, int brule) {
        dataSain.add(sain);
        dataFeu.add(feu);
        dataBrule.add(brule);
        if (dataSain.size() > MAX_POINTS) {
            dataSain.remove(0);
            dataFeu.remove(0);
            dataBrule.remove(0);
        }
    }

    /** Dessine le fond et les axes sans aucune donnée (état initial ou après {@link #clear()}). */
    private void drawEmpty() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, getWidth(), getHeight());
        drawAxes(gc);
        drawLegend(gc);
    }

    /** Efface le canvas et redessine axes, courbes et légende à partir des données courantes. */
    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, w, h);

        drawAxes(gc);

        int n = dataSain.size();
        if (n < 2) return;

        double plotW = w - 2 * PADDING;
        double plotH = h - 2 * PADDING;

        drawSeries(gc, dataSain,  n, plotW, plotH, COLOR_SAIN);
        drawSeries(gc, dataFeu,   n, plotW, plotH, COLOR_FEU);
        drawSeries(gc, dataBrule, n, plotW, plotH, COLOR_BRULE);

        drawLegend(gc);
    }

    /**
     * Trace une série de données comme une polyligne colorée.
     *
     * @param gc    contexte graphique
     * @param data  valeurs à tracer
     * @param n     nombre de points
     * @param plotW largeur de la zone de tracé (hors padding)
     * @param plotH hauteur de la zone de tracé (hors padding)
     * @param color couleur de la courbe
     */
    private void drawSeries(GraphicsContext gc, List<Integer> data,
                            int n, double plotW, double plotH, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < n; i++) {
            double x = PADDING + i * plotW / (n - 1);
            double y = PADDING + plotH - (data.get(i) * plotH / totalCells);
            if (i == 0) gc.moveTo(x, y);
            else        gc.lineTo(x, y);
        }
        gc.stroke();
    }

    /** Dessine l'axe vertical et l'axe horizontal ainsi que le titre du graphique. */
    private void drawAxes(GraphicsContext gc) {
        double w = getWidth();
        double h = getHeight();
        gc.setStroke(Color.web("#555555"));
        gc.setLineWidth(1.0);
        gc.strokeLine(PADDING, PADDING, PADDING, h - PADDING);
        gc.strokeLine(PADDING, h - PADDING, w - PADDING, h - PADDING);

        gc.setFill(Color.web("#888888"));
        gc.setFont(Font.font(10));
        gc.fillText("Évolution temporelle", PADDING, PADDING - 8);
    }

    /** Dessine la légende des trois séries en haut à droite du graphique. */
    private void drawLegend(GraphicsContext gc) {
        double x = getWidth() - PADDING - 80;
        double y = PADDING + 10;

        drawLegendEntry(gc, x, y,      COLOR_SAIN,  "Sains");
        drawLegendEntry(gc, x, y + 14, COLOR_FEU,   "En feu");
        drawLegendEntry(gc, x, y + 28, COLOR_BRULE, "Brûlés");
    }

    /**
     * Dessine une entrée de légende (carré coloré + libellé).
     *
     * @param gc    contexte graphique
     * @param x     abscisse de l'entrée
     * @param y     ordonnée de l'entrée
     * @param color couleur du carré
     * @param label texte affiché à droite du carré
     */
    private void drawLegendEntry(GraphicsContext gc, double x, double y,
                                 Color color, String label) {
        gc.setFill(color);
        gc.fillRect(x, y - 7, 12, 4);
        gc.setFill(Color.web("#cccccc"));
        gc.setFont(Font.font(10));
        gc.fillText(label, x + 16, y);
    }
}
