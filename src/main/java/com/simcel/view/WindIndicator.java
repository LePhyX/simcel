package com.simcel.view;

import com.simcel.model.Environment;
import com.simcel.model.WindDirection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Indicateur visuel de la direction et de l'intensité du vent.
 *
 * <p>Affiche une boussole (lecture seule) avec la direction active mise
 * en évidence. La direction se change via un ComboBox ; la force via des
 * boutons numérotés 0–{@value #MAX_WIND_STRENGTH} ; l'humidité via un
 * slider avec labels SEC / HUMIDE.</p>
 *
 * <p>Appeler {@link #refresh(Environment)} après une modification externe
 * (ex. chargement de fichier) pour synchroniser l'affichage.</p>
 */
public class WindIndicator extends VBox {

    private static final double SIZE               = 160.0;
    private static final double R                  = 52.0;
    private static final int    MAX_WIND_STRENGTH  = Environment.MAX_WIND;
    private static final double LABEL_RADIUS_EXTRA = 17.0;
    private static final double ARROWHEAD_ANGLE    = Math.PI / 5;
    private static final double ARROWHEAD_LENGTH   = 9.0;
    private static final double ARROW_MIN_LENGTH   = 8.0;

    private static final Color COLOR_BG       = Color.web("#1a1a2e");
    private static final Color COLOR_CIRCLE   = Color.web("#444466");
    private static final Color COLOR_TICK     = Color.web("#555577");
    private static final Color COLOR_ACTIVE   = Color.web("#ff6633");
    private static final Color COLOR_INACTIVE = Color.web("#888899");
    private static final Color COLOR_CENTER   = Color.web("#ffffff");

    private static final String[] WIND_LABELS =
            {"Calme", "Légère brise", "Brise", "Soutenu", "Fort", "Violent"};

    private Environment          env;
    private final Canvas         compass;
    private final Label          lblWindType;
    private final Label          lblHumidityVal;
    private final ToggleButton[] forceButtons;
    private final Slider         sliderHumidity;
    private final ComboBox<WindDirection> cbDirection;

    /**
     * Crée l'indicateur à partir des conditions météorologiques données.
     *
     * @param env conditions météorologiques initiales, non {@code null}
     */
    public WindIndicator(Environment env) {
        super(6);
        this.env = env;
        setPadding(new Insets(10, 10, 10, 10));

        Label title = new Label("Conditions météo");
        title.getStyleClass().add("label-title");
        Label subtitle = new Label("Influence la propagation");
        subtitle.setStyle("-fx-text-fill: #888899; -fx-font-size: 10px;");

        // --- Boussole (lecture seule) ---
        compass = new Canvas(SIZE, SIZE);
        drawCompass();
        VBox compassBox = new VBox(compass);
        compassBox.setAlignment(Pos.CENTER);

        // --- Force du vent ---
        Label lblForceTitle = new Label("Force du vent");
        lblForceTitle.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
        lblWindType = new Label(WIND_LABELS[env.getWindStrength()]);
        lblWindType.setStyle("-fx-text-fill: #888899; -fx-font-size: 10px;");
        Region forceHeaderSpacer = new Region();
        HBox.setHgrow(forceHeaderSpacer, Priority.ALWAYS);
        HBox forceHeader = new HBox(6, lblForceTitle, forceHeaderSpacer, lblWindType);
        forceHeader.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup forceGroup = new ToggleGroup();
        forceButtons = new ToggleButton[MAX_WIND_STRENGTH + 1];
        HBox forceBox = new HBox(3);
        forceBox.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i <= MAX_WIND_STRENGTH; i++) {
            final int val = i;
            ToggleButton btn = new ToggleButton(String.valueOf(i));
            btn.setToggleGroup(forceGroup);
            btn.getStyleClass().add("force-btn");
            btn.setSelected(i == env.getWindStrength());
            btn.setOnAction(e -> {
                env.setWindStrength(val);
                lblWindType.setText(WIND_LABELS[val]);
                drawCompass();
                if (!btn.isSelected()) btn.setSelected(true);
            });
            forceButtons[i] = btn;
            forceBox.getChildren().add(btn);
        }
        // Empêche la désélection totale dans le groupe
        forceGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });

        // --- Direction du vent (ComboBox) ---
        Label lblDirTitle = new Label("Direction du vent :");
        lblDirTitle.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
        cbDirection = new ComboBox<>();
        cbDirection.getItems().addAll(WindDirection.values());
        cbDirection.setValue(env.getDirection());
        cbDirection.setMaxWidth(Double.MAX_VALUE);
        cbDirection.setOnAction(e -> {
            env.setDirection(cbDirection.getValue());
            drawCompass();
        });

        // --- Humidité ---
        Label lblHumidityTitle = new Label("Humidité");
        lblHumidityTitle.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
        lblHumidityVal = new Label(env.getHumidity() + " %");
        lblHumidityVal.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");
        Region humHeaderSpacer = new Region();
        HBox.setHgrow(humHeaderSpacer, Priority.ALWAYS);
        HBox humidityHeader = new HBox(lblHumidityTitle, humHeaderSpacer, lblHumidityVal);
        humidityHeader.setAlignment(Pos.CENTER_LEFT);

        sliderHumidity = new Slider(0, 100, env.getHumidity());
        sliderHumidity.setMaxWidth(Double.MAX_VALUE);
        sliderHumidity.valueProperty().addListener((obs, old, val) -> {
            env.setHumidity(val.intValue());
            lblHumidityVal.setText(val.intValue() + " %");
        });

        Label lblSec = new Label("SEC");
        lblSec.setStyle("-fx-text-fill: #888899; -fx-font-size: 9px;");
        Label lblHumide = new Label("HUMIDE");
        lblHumide.setStyle("-fx-text-fill: #888899; -fx-font-size: 9px;");
        Region humLabelSpacer = new Region();
        HBox.setHgrow(humLabelSpacer, Priority.ALWAYS);
        HBox humidityLabels = new HBox(lblSec, humLabelSpacer, lblHumide);

        getChildren().addAll(
                title, subtitle,
                new Separator(),
                compassBox,
                forceHeader, forceBox,
                lblDirTitle, cbDirection,
                new Separator(),
                humidityHeader, sliderHumidity, humidityLabels
        );
    }

    /**
     * Synchronise l'affichage avec un nouvel état d'environnement.
     * Doit être appelé depuis le thread JavaFX.
     *
     * @param env nouvelles conditions météorologiques, non {@code null}
     */
    public void refresh(Environment env) {
        this.env = env;
        cbDirection.setValue(env.getDirection());
        for (int i = 0; i <= MAX_WIND_STRENGTH; i++) {
            forceButtons[i].setSelected(i == env.getWindStrength());
        }
        lblWindType.setText(WIND_LABELS[env.getWindStrength()]);
        lblHumidityVal.setText(env.getHumidity() + " %");
        sliderHumidity.setValue(env.getHumidity());
        drawCompass();
    }

    // -------------------------------------------------------------------------
    // Rendu interne
    // -------------------------------------------------------------------------

    private void drawCompass() {
        GraphicsContext gc = compass.getGraphicsContext2D();
        double cx = SIZE / 2, cy = SIZE / 2;

        gc.setFill(COLOR_BG);
        gc.fillRect(0, 0, SIZE, SIZE);

        gc.setStroke(COLOR_CIRCLE);
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - R, cy - R, R * 2, R * 2);

        WindDirection active = env.getDirection();
        for (WindDirection d : WindDirection.values()) {
            double angle = Math.atan2(d.getDy(), d.getDx());
            drawTick(gc, cx, cy, angle);
            drawDirLabel(gc, cx, cy, angle, d.name(), d == active);
        }

        drawWindArrow(gc, cx, cy, active, env.getWindStrength());

        // Nom de la direction active au centre
        gc.setFill(COLOR_CENTER);
        gc.setFont(Font.font("monospace", FontWeight.BOLD, 15));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(active.name(), cx, cy + 5);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static void drawTick(GraphicsContext gc, double cx, double cy, double angle) {
        gc.setStroke(COLOR_TICK);
        gc.setLineWidth(1.0);
        gc.strokeLine(
                cx + (R - 7) * Math.cos(angle), cy + (R - 7) * Math.sin(angle),
                cx + R       * Math.cos(angle), cy + R       * Math.sin(angle));
    }

    private static void drawDirLabel(GraphicsContext gc, double cx, double cy,
                                     double angle, String name, boolean active) {
        double lr      = R + LABEL_RADIUS_EXTRA;
        double offsetX = name.length() > 1 ? -6 : -3;
        double lx      = cx + lr * Math.cos(angle) + offsetX;
        double ly      = cy + lr * Math.sin(angle) + 4;

        if (active) {
            gc.setFill(COLOR_ACTIVE);
            gc.setFont(Font.font("monospace", FontWeight.BOLD, 10));
        } else {
            gc.setFill(COLOR_INACTIVE);
            gc.setFont(Font.font("monospace", 10));
        }
        gc.fillText(name, lx, ly);
    }

    private static void drawWindArrow(GraphicsContext gc, double cx, double cy,
                                      WindDirection dir, int strength) {
        double angle = Math.atan2(dir.getDy(), dir.getDx());
        double len   = strength == 0
                ? ARROW_MIN_LENGTH
                : R * 0.65 * (0.4 + 0.6 * strength / (double) MAX_WIND_STRENGTH);
        double ax = cx + len * Math.cos(angle);
        double ay = cy + len * Math.sin(angle);

        Color c = strength == 0 ? COLOR_TICK : COLOR_ACTIVE;
        gc.setStroke(c);
        gc.setLineWidth(strength == 0 ? 1.0 : 2.5);
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
