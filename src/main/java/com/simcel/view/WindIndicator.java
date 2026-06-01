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
 *   <li>Cinq indicateurs ●/○ pour la force du vent.</li>
 *   <li>Le taux d'humidité.</li>
 * </ul>
 *
 * <p>Le composant est statique : il lit l'{@link Environment} une seule fois
 * à la construction et ne se met pas à jour pendant la simulation.</p>
 */
public class WindIndicator extends VBox {

    private static final double SIZE = 96.0;
    private static final double R    = 32.0;

    /**
     * Crée l'indicateur à partir des conditions météorologiques données.
     *
     * @param env conditions météorologiques, non {@code null}
     */
    public WindIndicator(Environment env) {
        super(4);
        setPadding(new Insets(10, 6, 6, 6));

        Label title = new Label("Conditions météo");
        title.getStyleClass().add("label-title");

        Canvas compass = new Canvas(SIZE, SIZE);
        drawCompass(compass.getGraphicsContext2D(), env);

        VBox compassBox = new VBox(compass);
        compassBox.setAlignment(Pos.CENTER);

        getChildren().addAll(title, new Separator(), compassBox,
                buildStrengthRow(env), buildHumidityRow(env));
    }

    // -------------------------------------------------------------------------
    // Rendu interne
    // -------------------------------------------------------------------------

    /** Dessine la boussole complète : cercle, marques, flèche et labels. */
    private static void drawCompass(GraphicsContext gc, Environment env) {
        double cx = SIZE / 2;
        double cy = SIZE / 2;

        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, SIZE, SIZE);

        gc.setStroke(Color.web("#444466"));
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - R, cy - R, R * 2, R * 2);

        WindDirection active = env.getDirection();
        for (WindDirection d : WindDirection.values()) {
            double angle = Math.atan2(d.getDy(), d.getDx());
            drawTick(gc, cx, cy, angle);
            drawDirLabel(gc, cx, cy, angle, d.name(), d == active);
        }

        drawArrow(gc, cx, cy, active, env.getWindStrength());

        gc.setFill(Color.web("#dddddd"));
        gc.fillOval(cx - 2.5, cy - 2.5, 5, 5);
    }

    /** Dessine un tiret sur le cercle à l'angle donné. */
    private static void drawTick(GraphicsContext gc, double cx, double cy, double angle) {
        gc.setStroke(Color.web("#555577"));
        gc.setLineWidth(1.0);
        gc.strokeLine(
                cx + (R - 4) * Math.cos(angle), cy + (R - 4) * Math.sin(angle),
                cx + R       * Math.cos(angle), cy + R       * Math.sin(angle));
    }

    /**
     * Dessine le label d'une direction, mis en évidence en orange si active.
     *
     * @param active {@code true} si cette direction est la direction courante du vent
     */
    private static void drawDirLabel(GraphicsContext gc, double cx, double cy,
                                     double angle, String name, boolean active) {
        double lr = R + 13;
        double offsetX = name.length() > 1 ? -5 : -3;
        double lx = cx + lr * Math.cos(angle) + offsetX;
        double ly = cy + lr * Math.sin(angle) + 4;

        if (active) {
            gc.setFill(Color.web("#ff6633"));
            gc.setFont(Font.font("monospace", FontWeight.BOLD, 9));
        } else {
            gc.setFill(Color.web("#888899"));
            gc.setFont(Font.font("monospace", 9));
        }
        gc.fillText(name, lx, ly);
    }

    /**
     * Dessine la flèche de vent. Longueur proportionnelle à l'intensité.
     * Gris si vent nul, orange sinon.
     */
    private static void drawArrow(GraphicsContext gc, double cx, double cy,
                                  WindDirection dir, int strength) {
        double angle = Math.atan2(dir.getDy(), dir.getDx());
        double len   = strength == 0 ? 6 : R * 0.55 * (0.4 + 0.6 * strength / 5.0);
        double ax    = cx + len * Math.cos(angle);
        double ay    = cy + len * Math.sin(angle);

        Color c = strength == 0 ? Color.web("#555577") : Color.web("#ff6633");
        gc.setStroke(c);
        gc.setLineWidth(strength == 0 ? 1.0 : 2.0);
        gc.strokeLine(cx, cy, ax, ay);

        if (strength > 0) {
            double ha = Math.PI / 5;
            double hl = 6;
            gc.strokeLine(ax, ay, ax - hl * Math.cos(angle - ha), ay - hl * Math.sin(angle - ha));
            gc.strokeLine(ax, ay, ax - hl * Math.cos(angle + ha), ay - hl * Math.sin(angle + ha));
        }
    }

    /** Ligne affichant cinq indicateurs ●/○ pour la force du vent. */
    private static HBox buildStrengthRow(Environment env) {
        Label lbl  = new Label("Force : ");
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");

        StringBuilder sb = new StringBuilder();
        int s = env.getWindStrength();
        for (int i = 0; i < 5; i++) sb.append(i < s ? "●" : "○");
        Label dots = new Label(sb.toString());
        dots.setStyle("-fx-text-fill: #ff6633; -fx-font-size: 10px;");

        HBox row = new HBox(3, lbl, dots);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Ligne affichant le taux d'humidité. */
    private static HBox buildHumidityRow(Environment env) {
        Label lbl = new Label(String.format("Humidité : %d %%", env.getHumidity()));
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");
        return new HBox(lbl);
    }
}
