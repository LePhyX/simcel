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
 * Graphique d'évolution temporelle en aires empilées (stacked area).
 *
 * <p>Affiche trois zones superposées en pourcentage du total de cellules :
 * Brûlées (bas), En feu (milieu), Saines (haut). L'axe Y va de 0 à 100 %.</p>
 *
 * <p>Appeler {@link #resize(double, double)} lorsque le conteneur change
 * de taille pour adapter le canvas dynamiquement.</p>
 */
public class ChartView extends Canvas implements SimulationListener {

    private static final int    MAX_POINTS = 300;
    private static final double PADDING    = 35.0;

    private static final Color BG_COLOR      = Color.web("#1a1a2e");
    private static final Color COLOR_SAIN    = Color.web("#228B22");
    private static final Color COLOR_FEU     = Color.web("#FF4500");
    private static final Color COLOR_BRULE   = Color.web("#888888");
    private static final Color COLOR_AXIS    = Color.web("#555555");
    private static final Color COLOR_LABEL   = Color.web("#888888");

    /** Pourcentages relatifs aux cellules totales (sain+feu+brulé). */
    private final List<Double> dataSainPct  = new ArrayList<>();
    private final List<Double> dataFeuPct   = new ArrayList<>();
    private final List<Double> dataBrulePct = new ArrayList<>();

    /**
     * Crée le graphique avec les dimensions initiales données.
     *
     * @param width  largeur en pixels
     * @param height hauteur en pixels
     */
    public ChartView(double width, double height) {
        super(width, height);
        drawEmpty();
    }

    /**
     * Redimensionne le canvas et redessine le graphique.
     * Doit être appelé depuis le thread JavaFX.
     *
     * @param w nouvelle largeur en pixels
     * @param h nouvelle hauteur en pixels
     */
    public void resize(double w, double h) {
        setWidth(w);
        setHeight(h);
        if (dataSainPct.isEmpty()) drawEmpty();
        else redraw();
    }

    /**
     * Collecte les données sur le thread de simulation, puis redessine
     * sur le thread JavaFX.
     */
    @Override
    public void onTick(int tick, Grid grid) {
        int sain = 0, enFeu = 0, brule = 0;
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
        int total = grid.getWidth() * grid.getHeight();
        final double sp = total > 0 ? sain  * 100.0 / total : 0;
        final double fp = total > 0 ? enFeu * 100.0 / total : 0;
        final double bp = total > 0 ? brule * 100.0 / total : 0;

        Platform.runLater(() -> {
            addPoint(sp, fp, bp);
            redraw();
        });
    }

    @Override
    public void onSimulationEnd() {}

    /**
     * Vide toutes les séries et redessine le graphique vide.
     * Doit être appelé depuis le thread JavaFX.
     */
    public void clear() {
        dataSainPct.clear();
        dataFeuPct.clear();
        dataBrulePct.clear();
        drawEmpty();
    }

    /**
     * Supprime le dernier point de chaque série (utilisé lors d'un pas en arrière).
     * Doit être appelé depuis le thread JavaFX.
     */
    public void removeLastPoint() {
        if (dataSainPct.isEmpty()) return;
        dataSainPct.remove(dataSainPct.size() - 1);
        dataFeuPct.remove(dataFeuPct.size() - 1);
        dataBrulePct.remove(dataBrulePct.size() - 1);
        if (dataSainPct.isEmpty()) drawEmpty();
        else redraw();
    }

    // -------------------------------------------------------------------------
    // Rendu interne
    // -------------------------------------------------------------------------

    private void addPoint(double sain, double feu, double brule) {
        dataSainPct.add(sain);
        dataFeuPct.add(feu);
        dataBrulePct.add(brule);
        if (dataSainPct.size() > MAX_POINTS) {
            dataSainPct.remove(0);
            dataFeuPct.remove(0);
            dataBrulePct.remove(0);
        }
    }

    private void drawEmpty() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, getWidth(), getHeight());
        drawAxes(gc);
        drawLegend(gc);
    }

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        int n = dataSainPct.size();
        if (n >= 2) {
            double plotW = w - 2 * PADDING;
            double plotH = h - 2 * PADDING;
            drawStackedAreas(gc, n, plotW, plotH);
        }

        drawAxes(gc);
        drawLegend(gc);
    }

    /**
     * Dessine les trois aires empilées : Brûlés (bas), En feu (milieu),
     * Sains (haut). Chaque aire est un polygone fermé entre son sommet et
     * le sommet de la couche précédente.
     */
    private void drawStackedAreas(GraphicsContext gc, int n, double plotW, double plotH) {
        double bottom = PADDING + plotH;

        // Précalcul des coordonnées Y pour chaque couche (sommet de chaque aire)
        double[] bruleTopY = new double[n];
        double[] feuTopY   = new double[n];
        double[] sainTopY  = new double[n];
        double[] xs        = new double[n];

        for (int i = 0; i < n; i++) {
            xs[i]        = PADDING + i * plotW / (n - 1);
            bruleTopY[i] = bottom - clamp(dataBrulePct.get(i)) * plotH / 100.0;
            feuTopY[i]   = bruleTopY[i] - clamp(dataFeuPct.get(i)) * plotH / 100.0;
            sainTopY[i]  = feuTopY[i]   - clamp(dataSainPct.get(i)) * plotH / 100.0;
        }

        // Aire Brûlés : du sommet brule vers le bas du graphique
        fillArea(gc, xs, bruleTopY, null, bottom, n, COLOR_BRULE.deriveColor(0, 1, 1, 0.85));

        // Aire En feu : du sommet feu vers le sommet brule
        fillArea(gc, xs, feuTopY, bruleTopY, bottom, n, COLOR_FEU.deriveColor(0, 1, 1, 0.85));

        // Aire Sains : du sommet sain vers le sommet feu
        fillArea(gc, xs, sainTopY, feuTopY, bottom, n, COLOR_SAIN.deriveColor(0, 1, 1, 0.85));
    }

    /**
     * Remplit un polygone en aire entre {@code topY} (sommet courant, gauche→droite)
     * et {@code bottomY} (bas de la couche, droite→gauche).
     * Si {@code bottomY} est {@code null}, utilise la constante {@code flatBottom}.
     */
    private static void fillArea(GraphicsContext gc, double[] xs, double[] topY,
                                  double[] bottomY, double flatBottom, int n, Color color) {
        double[] polyX = new double[2 * n];
        double[] polyY = new double[2 * n];

        for (int i = 0; i < n; i++) {
            polyX[i] = xs[i];
            polyY[i] = topY[i];
        }
        for (int i = 0; i < n; i++) {
            polyX[n + i] = xs[n - 1 - i];
            polyY[n + i] = bottomY == null ? flatBottom : bottomY[n - 1 - i];
        }

        gc.setFill(color);
        gc.fillPolygon(polyX, polyY, 2 * n);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private void drawAxes(GraphicsContext gc) {
        double w = getWidth(), h = getHeight();
        gc.setStroke(COLOR_AXIS);
        gc.setLineWidth(1.0);
        gc.strokeLine(PADDING, PADDING, PADDING, h - PADDING);
        gc.strokeLine(PADDING, h - PADDING, w - PADDING, h - PADDING);

        gc.setFill(COLOR_LABEL);
        gc.setFont(Font.font(10));

        // Labels axe Y : 0%, 50%, 100%
        gc.fillText("100%", 2, PADDING + 4);
        gc.fillText(" 50%", 2, PADDING + (h - 2 * PADDING) / 2 + 4);
        gc.fillText("  0%", 2, h - PADDING + 4);

        // Labels axe X
        int n = dataSainPct.size();
        gc.fillText("t=0", PADDING, h - PADDING + 14);
        if (n > 1) {
            String tMax = "t=" + (n - 1);
            gc.fillText(tMax, w - PADDING - tMax.length() * 5.5, h - PADDING + 14);
        }

        // Titre
        gc.setFill(COLOR_LABEL);
        gc.setFont(Font.font(11));
        gc.fillText("Évolution temporelle", PADDING, PADDING - 10);
    }

    private void drawLegend(GraphicsContext gc) {
        double x = getWidth() - PADDING - 85;
        double y = PADDING + 10;

        drawLegendEntry(gc, x, y,      COLOR_SAIN,  "Sains");
        drawLegendEntry(gc, x, y + 15, COLOR_FEU,   "En feu");
        drawLegendEntry(gc, x, y + 30, COLOR_BRULE, "Brûlés");
    }

    private static void drawLegendEntry(GraphicsContext gc, double x, double y,
                                        Color color, String label) {
        gc.setFill(color);
        gc.fillRect(x, y - 7, 12, 8);
        gc.setFill(Color.web("#cccccc"));
        gc.setFont(Font.font(10));
        gc.fillText(label, x + 16, y);
    }
}
