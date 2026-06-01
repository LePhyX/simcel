package com.simcel.view;

import com.simcel.model.Environment;
import com.simcel.model.WindDirection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Indicateur visuel de la direction et de l'intensité du vent.
 *
 * <p>Affiche une boussole sur un {@link Canvas} avec :</p>
 * <ul>
 *   <li>Une flèche orange pointant dans la direction active du vent
 *       (dont la longueur est proportionnelle à l'intensité).</li>
 *   <li>Le label de la direction active mis en évidence en orange.</li>
 *   <li>Cinq indicateurs ●/○ pour la force du vent (0–{@value #MAX_WIND_STRENGTH}).</li>
 *   <li>Le taux d'humidité.</li>
 * </ul>
 *
 * <p>Appeler {@link #refresh(Environment)} après chaque modification de
 * l'environnement pour mettre à jour l'affichage.</p>
 */
public class WindIndicator extends VBox {

    /** Taille du canvas de la boussole en pixels. */
    private static final double SIZE = 96.0;
    /** Rayon du cercle de la boussole en pixels. */
    private static final double R    = 32.0;

    /** Intensité maximale du vent (nombre de cases dans l'indicateur ●/○). */
    private static final int    MAX_WIND_STRENGTH  = Environment.MAX_WIND;
    /** Rayon supplémentaire par rapport à R pour placer les labels de direction. */
    private static final double LABEL_RADIUS_EXTRA = 13.0;
    /** Demi-angle des branches de la tête de flèche (radians). */
    private static final double ARROWHEAD_ANGLE    = Math.PI / 5;
    /** Longueur des branches de la tête de flèche en pixels. */
    private static final double ARROWHEAD_LENGTH   = 6.0;
    /** Longueur minimale de la flèche (vent nul) en pixels. */
    private static final double ARROW_MIN_LENGTH   = 6.0;

    private static final Color COLOR_BG       = Color.web("#1a1a2e");
    private static final Color COLOR_CIRCLE   = Color.web("#444466");
    private static final Color COLOR_TICK     = Color.web("#555577");
    private static final Color COLOR_ACTIVE   = Color.web("#ff6633");
    private static final Color COLOR_INACTIVE = Color.web("#888899");
    private static final Color COLOR_CENTER   = Color.web("#dddddd");

    private final Canvas compass;
    private final Label  lblStrength;
    private final Label  lblHumidity;

    /**
     * Crée l'indicateur à partir des conditions météorologiques données.
     *
     * @param env conditions météorologiques initiales, non {@code null}
     */
    public WindIndicator(Environment env) {
        super(4);
        setPadding(new Insets(10, 6, 6, 6));

        Label title = new Label("Conditions météo");
        title.getStyleClass().add("label-title");

        compass = new Canvas(SIZE, SIZE);
        drawCompass(compass.getGraphicsContext2D(), env);

        VBox compassBox = new VBox(compass);
        compassBox.setAlignment(Pos.CENTER);

        Label forceLbl = new Label("Force : ");
        forceLbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");
        lblStrength = new Label(buildStrengthText(env));
        lblStrength.setStyle("-fx-text-fill: #ff6633; -fx-font-size: 10px;");
        HBox strengthRow = new HBox(3, forceLbl, lblStrength);
        strengthRow.setAlignment(Pos.CENTER_LEFT);

        lblHumidity = new Label(buildHumidityText(env));
        lblHumidity.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");

        getChildren().addAll(title, new Separator(), compassBox,
                strengthRow, new HBox(lblHumidity));
    }

    /**
     * Redessine la boussole et met à jour les labels force et humidité.
     * Doit être appelé depuis le thread JavaFX.
     *
     * @param env nouvelles conditions météorologiques, non {@code null}
     */
    public void refresh(Environment env) {
        drawCompass(compass.getGraphicsContext2D(), env);
        lblStrength.setText(buildStrengthText(env));
        lblHumidity.setText(buildHumidityText(env));
    }

    // -------------------------------------------------------------------------
    // Rendu interne
    // -------------------------------------------------------------------------

    /**
     * Construit la chaîne d'indicateurs ●/○ représentant la force du vent.
     *
     * @param env conditions météorologiques courantes
     * @return chaîne de {@value #MAX_WIND_STRENGTH} caractères ● ou ○
     */
    private static String buildStrengthText(Environment env) {
        StringBuilder sb = new StringBuilder(MAX_WIND_STRENGTH);
        int s = env.getWindStrength();
        for (int i = 0; i < MAX_WIND_STRENGTH; i++) sb.append(i < s ? "●" : "○");
        return sb.toString();
    }

    /**
     * Formate le taux d'humidité sous forme lisible.
     *
     * @param env conditions météorologiques courantes
     * @return chaîne formatée, ex. {@code "Humidité : 50 %"}
     */
    private static String buildHumidityText(Environment env) {
        return String.format("Humidité : %d %%", env.getHumidity());
    }

    /**
     * Dessine la boussole complète : fond, cercle, marques cardinales,
     * flèche de vent et point central.
     *
     * @param gc  contexte graphique du canvas
     * @param env conditions météorologiques à représenter
     */
    private static void drawCompass(GraphicsContext gc, Environment env) {
        double cx = SIZE / 2;
        double cy = SIZE / 2;

        gc.setFill(COLOR_BG);
        gc.fillRect(0, 0, SIZE, SIZE);

        gc.setStroke(COLOR_CIRCLE);
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - R, cy - R, R * 2, R * 2);

        WindDirection active = env.getDirection();
        for (WindDirection d : WindDirection.values()) {
            double angle = Math.atan2(d.getDy(), d.getDx());
            drawTick(gc, cx, cy, angle);
            drawDirLabel(gc, cx, cy, angle, d.name(), d == active);
        }

        drawArrow(gc, cx, cy, active, env.getWindStrength());

        gc.setFill(COLOR_CENTER);
        gc.fillOval(cx - 2.5, cy - 2.5, 5, 5);
    }

    /**
     * Dessine un tiret de repère sur le bord du cercle à l'angle donné.
     *
     * @param gc    contexte graphique
     * @param cx    abscisse du centre
     * @param cy    ordonnée du centre
     * @param angle angle en radians
     */
    private static void drawTick(GraphicsContext gc, double cx, double cy, double angle) {
        gc.setStroke(COLOR_TICK);
        gc.setLineWidth(1.0);
        gc.strokeLine(
                cx + (R - 4) * Math.cos(angle), cy + (R - 4) * Math.sin(angle),
                cx + R       * Math.cos(angle), cy + R       * Math.sin(angle));
    }

    /**
     * Dessine le label d'une direction cardinale, mis en évidence en orange
     * si c'est la direction active du vent.
     *
     * @param gc     contexte graphique
     * @param cx     abscisse du centre
     * @param cy     ordonnée du centre
     * @param angle  angle en radians de cette direction
     * @param name   nom de la direction (ex. "N", "NE")
     * @param active {@code true} si cette direction est la direction courante du vent
     */
    private static void drawDirLabel(GraphicsContext gc, double cx, double cy,
                                     double angle, String name, boolean active) {
        double lr      = R + LABEL_RADIUS_EXTRA;
        double offsetX = name.length() > 1 ? -5 : -3;
        double lx      = cx + lr * Math.cos(angle) + offsetX;
        double ly      = cy + lr * Math.sin(angle) + 4;

        if (active) {
            gc.setFill(COLOR_ACTIVE);
            gc.setFont(Font.font("monospace", FontWeight.BOLD, 9));
        } else {
            gc.setFill(COLOR_INACTIVE);
            gc.setFont(Font.font("monospace", 9));
        }
        gc.fillText(name, lx, ly);
    }

    /**
     * Dessine la flèche de vent pointant dans la direction donnée.
     * La longueur est proportionnelle à l'intensité ; la flèche est grise
     * si le vent est nul, orange sinon. La tête de flèche (deux branches)
     * n'est tracée que pour un vent non nul.
     *
     * @param gc       contexte graphique
     * @param cx       abscisse du centre
     * @param cy       ordonnée du centre
     * @param dir      direction du vent
     * @param strength intensité du vent (0–{@value #MAX_WIND_STRENGTH})
     */
    private static void drawArrow(GraphicsContext gc, double cx, double cy,
                                  WindDirection dir, int strength) {
        double angle = Math.atan2(dir.getDy(), dir.getDx());
        double len   = strength == 0
                ? ARROW_MIN_LENGTH
                : R * 0.55 * (0.4 + 0.6 * strength / MAX_WIND_STRENGTH);
        double ax = cx + len * Math.cos(angle);
        double ay = cy + len * Math.sin(angle);

        Color c = strength == 0 ? COLOR_TICK : COLOR_ACTIVE;
        gc.setStroke(c);
        gc.setLineWidth(strength == 0 ? 1.0 : 2.0);
        gc.strokeLine(cx, cy, ax, ay);

        if (strength > 0) {
            gc.strokeLine(ax, ay,
                    ax - ARROWHEAD_LENGTH * Math.cos(angle - ARROWHEAD_ANGLE),
                    ay - ARROWHEAD_LENGTH * Math.sin(angle - ARROWHEAD_ANGLE));
            gc.strokeLine(ax, ay,
                    ax - ARROWHEAD_LENGTH * Math.cos(angle + ARROWHEAD_ANGLE),
                    ay - ARROWHEAD_LENGTH * Math.sin(angle + ARROWHEAD_ANGLE));
        }
    }
}
