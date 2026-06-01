package com.simcel.view;

import com.simcel.controller.SimulationController;
import com.simcel.model.CellState;
import com.simcel.model.Environment;
import com.simcel.model.FireSimulator;
import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Fenêtre principale de l'application JavaFX.
 *
 * <p>Assemble les trois vues ({@link GridView}, {@link StatisticsPanel},
 * {@link ChartView}) dans un {@link BorderPane} : la grille occupe le centre,
 * le panneau de contrôle et les statistiques sont sur la droite.</p>
 *
 * <p>La zone de grille supporte le <b>zoom</b> (molette de défilement, centré
 * sur le curseur) et le <b>déplacement</b> (clic-glisser). Un double-clic
 * réinitialise la vue à son état par défaut.</p>
 */
public class MainWindow {

    private static final double CELL_SIZE    = 14.0;
    private static final double RIGHT_WIDTH  = 300.0;
    private static final double CHART_HEIGHT = 200.0;

    private static final double ZOOM_FACTOR = 1.15;
    private static final double MIN_SCALE   = 0.2;
    private static final double MAX_SCALE   = 8.0;

    private final FireSimulator        simulator;
    private final SimulationController controller;
    private final Grid                 grid;

    private GridView        gridView;
    private StatisticsPanel statisticsPanel;
    private ChartView       chartView;
    private LegendPanel     legendPanel;
    private WindIndicator   windIndicator;

    private final Button btnStart    = new Button("▶  Démarrer");
    private final Button btnPause    = new Button("⏸  Pause");
    private final Button btnStep     = new Button("⏭  Pas à pas");
    private final Button btnStepBack = new Button("⏮  Pas en arrière");
    private final Button btnReset    = new Button("↺  Réinitialiser");

    /**
     * Crée la fenêtre principale.
     *
     * @param simulator  moteur de simulation, non {@code null}
     * @param controller contrôleur de la boucle, non {@code null}
     */
    public MainWindow(FireSimulator simulator, SimulationController controller) {
        this.simulator  = simulator;
        this.controller = controller;
        this.grid       = simulator.getGrid();
    }

    /**
     * Construit la scène, enregistre les listeners et affiche la fenêtre.
     *
     * @param stage stage JavaFX fourni par {@code Application.start()}
     */
    public void show(Stage stage) {
        Environment env = simulator.getEnvironment();
        gridView        = new GridView(grid, CELL_SIZE);
        statisticsPanel = new StatisticsPanel();
        chartView       = new ChartView(RIGHT_WIDTH - 20, CHART_HEIGHT);
        legendPanel     = new LegendPanel();
        windIndicator   = new WindIndicator(env);

        simulator.addListener(gridView);
        simulator.addListener(statisticsPanel);
        simulator.addListener(chartView);
        simulator.addListener(stoppingListener());

        BorderPane root = new BorderPane();
        root.setCenter(buildGridPane());
        root.setRight(buildRightPanel());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            getClass().getResource("/com/simcel/style.css").toExternalForm()
        );

        stage.setTitle("SimCel — Simulation de Propagation de Feu");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> controller.stop());
        stage.show();
    }

    // -------------------------------------------------------------------------
    // Zone grille avec zoom et déplacement
    // -------------------------------------------------------------------------

    /**
     * Construit le conteneur de la grille et y attache les handlers de zoom
     * et de déplacement.
     *
     * <ul>
     *   <li>Molette : zoom centré sur le curseur.</li>
     *   <li>Clic-glisser : déplacement libre.</li>
     *   <li>Double-clic : réinitialisation de la vue.</li>
     * </ul>
     */
    private Pane buildGridPane() {
        Pane pane = new Pane(gridView);
        pane.getStyleClass().add("grid-pane");
        pane.setPrefSize(gridView.getWidth(), gridView.getHeight());

        // Masquer le rendu hors des bords du conteneur
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);

        pane.setCursor(Cursor.OPEN_HAND);

        attachZoom(pane);
        attachPan(pane);

        return pane;
    }

    /**
     * Attache le handler de zoom (molette) au panneau.
     * La formule garantit que le point sous le curseur reste fixe après le zoom.
     *
     * @param pane conteneur de la grille
     */
    private void attachZoom(Pane pane) {
        pane.setOnScroll(e -> {
            // Sur certains drivers Linux, getDeltaY() vaut 0 pour un sens de molette ;
            // on se rabat sur getTextDeltaY() (signe inverse) comme fallback.
            double delta = e.getDeltaY() != 0 ? e.getDeltaY() : -e.getTextDeltaY();
            if (delta == 0) { e.consume(); return; }

            double oldScale = gridView.getScaleX();
            double factor   = delta > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
            double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, oldScale * factor));
            double ratio    = newScale / oldScale;

            // Pivot sur la position du curseur dans le repère du conteneur
            Bounds b      = gridView.getBoundsInParent();
            double pivotX = e.getX() - (b.getMinX() + b.getWidth()  / 2.0);
            double pivotY = e.getY() - (b.getMinY() + b.getHeight() / 2.0);

            gridView.setScaleX(newScale);
            gridView.setScaleY(newScale);
            gridView.setTranslateX(gridView.getTranslateX() + pivotX * (1 - ratio));
            gridView.setTranslateY(gridView.getTranslateY() + pivotY * (1 - ratio));

            e.consume();
        });
    }

    /**
     * Attache les handlers de déplacement (clic-glisser) et de
     * réinitialisation de la vue (double-clic) au panneau.
     *
     * @param pane conteneur de la grille
     */
    private void attachPan(Pane pane) {
        final double[] dragOrigin      = {0, 0};
        final double[] translateOrigin = {0, 0};

        pane.setOnMousePressed(e -> {
            dragOrigin[0]      = e.getX();
            dragOrigin[1]      = e.getY();
            translateOrigin[0] = gridView.getTranslateX();
            translateOrigin[1] = gridView.getTranslateY();
            pane.setCursor(Cursor.CLOSED_HAND);
        });

        pane.setOnMouseDragged(e -> {
            gridView.setTranslateX(translateOrigin[0] + e.getX() - dragOrigin[0]);
            gridView.setTranslateY(translateOrigin[1] + e.getY() - dragOrigin[1]);
        });

        pane.setOnMouseReleased(e -> pane.setCursor(Cursor.OPEN_HAND));

        pane.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) resetView();
        });
    }

    /**
     * Réinitialise le zoom et la position de la grille à leurs valeurs par défaut.
     */
    public void resetView() {
        gridView.setScaleX(1.0);
        gridView.setScaleY(1.0);
        gridView.setTranslateX(0.0);
        gridView.setTranslateY(0.0);
    }

    // -------------------------------------------------------------------------
    // Listener d'arrêt automatique
    // -------------------------------------------------------------------------

    private SimulationListener stoppingListener() {
        return new SimulationListener() {
            @Override
            public void onTick(int tick, Grid g) {
                if (!hasActiveFire(g)) {
                    controller.stop();
                    Platform.runLater(() -> setIdleState());
                }
            }

            @Override
            public void onSimulationEnd() {}
        };
    }

    private boolean hasActiveFire(Grid g) {
        for (int y = 0; y < g.getHeight(); y++)
            for (int x = 0; x < g.getWidth(); x++)
                if (g.getCell(x, y).getState() == CellState.EN_FEU)
                    return true;
        return false;
    }

    // -------------------------------------------------------------------------
    // Gestion des états des boutons
    // -------------------------------------------------------------------------

    private void setIdleState() {
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        btnStep.setDisable(false);
        btnStepBack.setDisable(false);
    }

    private void setRunningState() {
        btnStart.setDisable(true);
        btnPause.setDisable(false);
        btnStep.setDisable(true);
        btnStepBack.setDisable(true);
    }

    // -------------------------------------------------------------------------
    // Panneau de contrôle
    // -------------------------------------------------------------------------

    private VBox buildRightPanel() {
        for (Button b : new Button[]{btnStart, btnPause, btnStep, btnStepBack, btnReset}) {
            b.setMaxWidth(Double.MAX_VALUE);
        }
        setIdleState();

        btnStart.setOnAction(e -> {
            controller.start();
            setRunningState();
        });

        btnPause.setOnAction(e -> {
            controller.pause();
            setIdleState();
        });

        btnStep.setOnAction(e -> controller.step());

        btnStepBack.setOnAction(e -> {
            controller.stepBack();
            chartView.removeLastPoint();
        });

        btnReset.setOnAction(e -> {
            controller.reset();
            gridView.refresh(grid);
            resetView();
            statisticsPanel.reset();
            chartView.clear();
            setIdleState();
        });

        Label lblSpeed = new Label("Vitesse (ms/tick) :");
        Slider sliderSpeed = new Slider(50, 1000, controller.getTickDelay());
        sliderSpeed.setShowTickLabels(true);
        sliderSpeed.setShowTickMarks(true);
        sliderSpeed.setMajorTickUnit(300);
        sliderSpeed.valueProperty().addListener(
                (obs, old, val) -> controller.setTickDelay(val.intValue()));

        Label titleControls = new Label("Contrôles");
        titleControls.getStyleClass().add("label-title");

        VBox controls = new VBox(6,
                titleControls, new Separator(),
                btnStart, btnPause, btnStep, btnStepBack, btnReset,
                new Separator(),
                lblSpeed, sliderSpeed);
        controls.setPadding(new Insets(10));

        HBox legendRow = new HBox(8, legendPanel, windIndicator);

        VBox right = new VBox(8, controls, statisticsPanel, chartView, legendRow);
        right.getStyleClass().add("right-panel");
        right.setPadding(new Insets(8));
        right.setPrefWidth(RIGHT_WIDTH);
        return right;
    }
}
