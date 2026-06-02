package com.simcel.view;

import com.simcel.controller.SimulationController;
import com.simcel.model.Cell;
import com.simcel.model.CellState;
import com.simcel.model.CellType;
import com.simcel.model.Environment;
import com.simcel.model.FireSimulator;
import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import com.simcel.model.WindDirection;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
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
 *
 * <p>Un <b>mode édition</b> permet de peindre le terrain et de placer des
 * foyers interactivement : le clic ou le glisser sur la grille modifie les
 * cellules selon l'outil sélectionné. L'état résultant devient le nouvel
 * état initial (utilisé par le bouton Réinitialiser).</p>
 */
public class MainWindow {

    private static final double CELL_SIZE         = 14.0;
    private static final double RIGHT_WIDTH       = 300.0;
    private static final double RIGHT_CHART_W     = 280.0;
    private static final double RIGHT_CHART_H     = 220.0;

    private static final double ZOOM_FACTOR = 1.15;
    private static final double MIN_SCALE   = 0.2;
    private static final double MAX_SCALE   = 8.0;

    private static final double DENSITY_FORET   = 0.60;
    private static final double DENSITY_PRAIRIE = 0.20;
    private static final double DENSITY_BROUSS  = 0.10;
    private static final double DENSITY_HUMIDE  = 0.05;
    private static final double DENSITY_URBAINE = 0.05;

    // -------------------------------------------------------------------------
    // Outils d'édition
    // -------------------------------------------------------------------------

    /**
     * Outil de peinture sélectionnable dans le mode édition.
     *
     * <p>Chaque outil connaît son libellé, sa couleur de bouton et sait
     * créer la {@link Cell} correspondante via {@link #createCell()}.</p>
     */
    private enum EditTool {
        FORET        ("Forêt",    CellType.FORET.getHealthyColor(),        "white"),
        PRAIRIE      ("Prairie",  CellType.PRAIRIE.getHealthyColor(),      "white"),
        BROUSSAILLES ("Brouss.",  CellType.BROUSSAILLES.getHealthyColor(), "white"),
        ZONE_HUMIDE  ("Humide",   CellType.ZONE_HUMIDE.getHealthyColor(),  "white"),
        ZONE_URBAINE ("Urbain",   CellType.ZONE_URBAINE.getHealthyColor(), "white"),
        VIDE         ("Effacer",  CellState.VIDE.getColor(),               "black"),
        EAU          ("Eau",      CellState.EAU.getColor(),                "white"),
        ROCHER       ("Rocher",   CellState.ROCHER.getColor(),             "black"),
        FOYER        ("Foyer 🔥", CellState.EN_FEU.getColor(),             "white");

        final String label, bgColor, fgColor;

        EditTool(String label, String bgColor, String fgColor) {
            this.label   = label;
            this.bgColor = bgColor;
            this.fgColor = fgColor;
        }

        /**
         * Crée la cellule à placer dans la grille.
         * Retourne {@code null} pour {@link #FOYER} (cas traité séparément).
         */
        Cell createCell() {
            return switch (this) {
                case FORET        -> new Cell(CellType.FORET);
                case PRAIRIE      -> new Cell(CellType.PRAIRIE);
                case BROUSSAILLES -> new Cell(CellType.BROUSSAILLES);
                case ZONE_HUMIDE  -> new Cell(CellType.ZONE_HUMIDE);
                case ZONE_URBAINE -> new Cell(CellType.ZONE_URBAINE);
                case VIDE   -> { Cell c = new Cell(CellType.PRAIRIE); c.setState(CellState.VIDE);   yield c; }
                case EAU    -> { Cell c = new Cell(CellType.PRAIRIE); c.setState(CellState.EAU);    yield c; }
                case ROCHER -> { Cell c = new Cell(CellType.PRAIRIE); c.setState(CellState.ROCHER); yield c; }
                case FOYER  -> null;
            };
        }
    }

    private boolean      editMode     = false;
    private EditTool     selectedTool = EditTool.FOYER;
    private VBox         editToolsBox;
    private ToggleButton btnEditToggle;
    /** Référence conservée pour changer le curseur lors du basculement de mode. */
    private Pane         gridPane;

    // -------------------------------------------------------------------------
    // Composants principaux
    // -------------------------------------------------------------------------

    private FireSimulator        simulator;
    private SimulationController controller;
    private Grid                 grid;

    private GridView        gridView;
    private StatisticsPanel statisticsPanel;
    private ChartView       chartView;
    private LegendPanel     legendPanel;
    private WindIndicator   windIndicator;
    private BorderPane      root;

    private final Button btnStart    = new Button("▶  Démarrer");
    private final Button btnPause    = new Button("⏸  Pause");
    private final Button btnStep     = new Button("⏭  Pas à pas");
    private final Button btnStepBack = new Button("⏮  Pas en arrière");
    private final Button btnReset    = new Button("↺  Réinitialiser");
    private final Button btnSave     = new Button("💾  Sauvegarder");
    private final Button btnLoad     = new Button("📂  Charger");

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
        chartView       = new ChartView(RIGHT_CHART_W, RIGHT_CHART_H);
        legendPanel     = new LegendPanel();
        windIndicator   = new WindIndicator(env);

        simulator.addListener(gridView);
        simulator.addListener(statisticsPanel);
        simulator.addListener(chartView);
        simulator.addListener(stoppingListener());

        VBox rightBar = new VBox(8, chartView, windIndicator);
        rightBar.getStyleClass().add("right-panel");
        rightBar.setPadding(new Insets(8));

        VBox bottomBar = new VBox(buildRightPanel(), statisticsPanel);

        root = new BorderPane();
        root.setTop(legendPanel);
        root.setCenter(buildGridPane());
        root.setBottom(bottomBar);
        root.setRight(rightBar);

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
    // Zone grille : zoom, déplacement et édition
    // -------------------------------------------------------------------------

    /**
     * Construit le conteneur de la grille et y attache les handlers de zoom,
     * déplacement et édition.
     *
     * <ul>
     *   <li>Molette : zoom centré sur le curseur (tous modes).</li>
     *   <li>Mode navigation — Clic-glisser : déplacement ; double-clic : reset vue.</li>
     *   <li>Mode édition — Clic/glisser : peinture de cellules.</li>
     * </ul>
     */
    private Pane buildGridPane() {
        gridPane = new Pane(gridView);
        gridPane.getStyleClass().add("grid-pane");
        gridPane.setPrefSize(gridView.getWidth(), gridView.getHeight());

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(gridPane.widthProperty());
        clip.heightProperty().bind(gridPane.heightProperty());
        gridPane.setClip(clip);

        gridPane.setCursor(Cursor.OPEN_HAND);

        attachZoom(gridPane);
        attachPan(gridPane);

        return gridPane;
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
     * Attache les handlers de déplacement/édition au panneau.
     *
     * <p>En mode navigation : clic-glisser déplace la grille, double-clic
     * réinitialise la vue.</p>
     * <p>En mode édition : clic et glisser peignent les cellules ; le relâché
     * sauvegarde le nouvel état initial.</p>
     *
     * @param pane conteneur de la grille
     */
    private void attachPan(Pane pane) {
        final double[] dragOrigin      = {0, 0};
        final double[] translateOrigin = {0, 0};

        pane.setOnMousePressed(e -> {
            if (editMode) {
                paintCellAt(e.getSceneX(), e.getSceneY());
            } else {
                dragOrigin[0]      = e.getX();
                dragOrigin[1]      = e.getY();
                translateOrigin[0] = gridView.getTranslateX();
                translateOrigin[1] = gridView.getTranslateY();
                pane.setCursor(Cursor.CLOSED_HAND);
            }
        });

        pane.setOnMouseDragged(e -> {
            if (editMode) {
                paintCellAt(e.getSceneX(), e.getSceneY());
            } else {
                gridView.setTranslateX(translateOrigin[0] + e.getX() - dragOrigin[0]);
                gridView.setTranslateY(translateOrigin[1] + e.getY() - dragOrigin[1]);
            }
        });

        pane.setOnMouseReleased(e -> {
            if (editMode) {
                // Persist la peinture comme état initial pour le prochain Reset
                grid.saveInitialState();
            } else {
                pane.setCursor(Cursor.OPEN_HAND);
            }
        });

        pane.setOnMouseClicked(e -> {
            if (!editMode && e.getClickCount() == 2) resetView();
        });
    }

    /**
     * Peint la cellule sous les coordonnées scène données avec l'outil courant.
     *
     * <p>Convertit les coordonnées scène en coordonnées locales du canvas
     * (en tenant compte du zoom et du déplacement), puis modifie la cellule
     * et redessine la vue.</p>
     *
     * @param sceneX abscisse dans le repère scène
     * @param sceneY ordonnée dans le repère scène
     */
    private void paintCellAt(double sceneX, double sceneY) {
        Point2D local  = gridView.sceneToLocal(sceneX, sceneY);
        int[]   coords = gridView.getCellAt(local.getX(), local.getY());
        if (coords == null) return;
        int x = coords[0], y = coords[1];
        if (selectedTool == EditTool.FOYER) {
            // Crée une prairie saine puis l'enflamme, quelle que soit la cellule existante
            Cell c = new Cell(CellType.PRAIRIE);
            grid.setCell(x, y, c);
            grid.setFire(x, y);
        } else {
            grid.setCell(x, y, selectedTool.createCell());
        }
        gridView.refresh(grid);
    }

    /**
     * Active ou désactive le mode édition.
     *
     * @param on {@code true} pour activer le mode édition
     */
    private void toggleEditMode(boolean on) {
        editMode = on;
        editToolsBox.setVisible(on);
        editToolsBox.setManaged(on);
        gridPane.setCursor(on ? Cursor.CROSSHAIR : Cursor.OPEN_HAND);
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
        if (btnEditToggle != null) btnEditToggle.setDisable(false);
    }

    private void setRunningState() {
        btnStart.setDisable(true);
        btnPause.setDisable(false);
        btnStep.setDisable(true);
        btnStepBack.setDisable(true);
        if (btnEditToggle != null) {
            if (editMode) {
                btnEditToggle.setSelected(false);
                toggleEditMode(false);
            }
            btnEditToggle.setDisable(true);
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Réinitialisation de la simulation avec une nouvelle grille
    // -------------------------------------------------------------------------

    private void reinitialize(int width, int height) {
        controller.stop();
        if (editMode) {
            btnEditToggle.setSelected(false);
            toggleEditMode(false);
        }

        Grid newGrid = new Grid(width, height);
        newGrid.initRandom(DENSITY_FORET, DENSITY_PRAIRIE, DENSITY_BROUSS, DENSITY_HUMIDE, DENSITY_URBAINE);
        newGrid.setFire(width / 2, height / 2);
        newGrid.saveInitialState();

        Environment env  = simulator.getEnvironment();
        int         delay = controller.getTickDelay();

        simulator  = new FireSimulator(newGrid, env);
        controller = new SimulationController(simulator, delay);
        grid       = newGrid;

        gridView = new GridView(grid, CELL_SIZE);
        simulator.addListener(gridView);
        simulator.addListener(statisticsPanel);
        simulator.addListener(chartView);
        simulator.addListener(stoppingListener());

        root.setCenter(buildGridPane());
        statisticsPanel.reset();
        chartView.clear();
        setIdleState();
    }

    private VBox buildGridSection() {
        Label titleGrid = new Label("Taille de la grille");
        titleGrid.getStyleClass().add("label-title");

        Label lblWidth = new Label("Largeur : " + grid.getWidth());
        Slider sliderWidth = new Slider(10, 300, grid.getWidth());
        sliderWidth.setShowTickLabels(true);
        sliderWidth.setShowTickMarks(true);
        sliderWidth.setMajorTickUnit(100);
        sliderWidth.setMinorTickCount(9);
        sliderWidth.setSnapToTicks(false);
        sliderWidth.valueProperty().addListener((obs, old, val) ->
                lblWidth.setText("Largeur : " + val.intValue()));

        Label lblHeight = new Label("Hauteur : " + grid.getHeight());
        Slider sliderHeight = new Slider(10, 200, grid.getHeight());
        sliderHeight.setShowTickLabels(true);
        sliderHeight.setShowTickMarks(true);
        sliderHeight.setMajorTickUnit(50);
        sliderHeight.setMinorTickCount(9);
        sliderHeight.setSnapToTicks(false);
        sliderHeight.valueProperty().addListener((obs, old, val) ->
                lblHeight.setText("Hauteur : " + val.intValue()));

        Button btnNew = new Button("↺  Nouvelle grille");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e ->
                reinitialize((int) sliderWidth.getValue(), (int) sliderHeight.getValue()));

        VBox section = new VBox(6,
                titleGrid,
                lblWidth, sliderWidth,
                lblHeight, sliderHeight,
                btnNew);
        section.setPadding(new Insets(0, 10, 0, 10));
        return section;
    }

    // -------------------------------------------------------------------------
    // Panneau de contrôle
    // -------------------------------------------------------------------------

    private HBox buildRightPanel() {
        for (Button b : new Button[]{btnStart, btnPause, btnStep, btnStepBack, btnReset, btnSave, btnLoad}) {
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

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Simulation SimCel (*.simcel)", "*.simcel"));

        btnSave.setOnAction(e -> {
            fileChooser.setTitle("Sauvegarder la simulation");
            java.io.File file = fileChooser.showSaveDialog(null);
            if (file == null) return;
            if (!file.getName().endsWith(".simcel")) {
                file = new java.io.File(file.getAbsolutePath() + ".simcel");
            }
            try {
                controller.saveToFile(file);
            } catch (java.io.IOException ex) {
                showError("Erreur lors de la sauvegarde : " + ex.getMessage());
            }
        });

        btnLoad.setOnAction(e -> {
            fileChooser.setTitle("Charger une simulation");
            java.io.File file = fileChooser.showOpenDialog(null);
            if (file == null) return;
            try {
                controller.loadFromFile(file);
                gridView.refresh(grid);
                statisticsPanel.reset();
                chartView.clear();
                windIndicator.refresh(simulator.getEnvironment());
                setIdleState();
            } catch (java.io.IOException | ClassNotFoundException ex) {
                showError("Erreur lors du chargement : " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                showError("Fichier incompatible : " + ex.getMessage());
            }
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
                lblSpeed, sliderSpeed,
                new Separator(),
                btnSave, btnLoad);
        controls.setPadding(new Insets(0, 10, 0, 10));

        VBox sectGrid = buildGridSection();
        VBox sectEnv  = buildEnvSection();
        VBox sectEdit = buildEditSection();
        HBox.setHgrow(controls,  Priority.ALWAYS);
        HBox.setHgrow(sectGrid,  Priority.ALWAYS);
        HBox.setHgrow(sectEnv,   Priority.ALWAYS);
        HBox.setHgrow(sectEdit,  Priority.ALWAYS);

        HBox bottom = new HBox(0,
                controls,
                new Separator(Orientation.VERTICAL),
                sectGrid,
                new Separator(Orientation.VERTICAL),
                sectEnv,
                new Separator(Orientation.VERTICAL),
                sectEdit);
        bottom.getStyleClass().add("right-panel");
        bottom.setPadding(new Insets(8));
        return bottom;
    }

    /**
     * Construit la section « Paramètres météo » du panneau droit.
     *
     * <p>Contient un menu déroulant pour la direction du vent, un slider pour
     * l'intensité (0–5) et un slider pour l'humidité (0–100). Chaque
     * modification prend effet immédiatement et rafraîchit le WindIndicator.</p>
     */
    private VBox buildEnvSection() {
        Environment env = simulator.getEnvironment();

        Label lblDir = new Label("Direction du vent :");
        ComboBox<WindDirection> cbDirection = new ComboBox<>();
        cbDirection.getItems().addAll(WindDirection.values());
        cbDirection.setValue(env.getDirection());
        cbDirection.setMaxWidth(Double.MAX_VALUE);
        cbDirection.setOnAction(e -> {
            env.setDirection(cbDirection.getValue());
            windIndicator.refresh(env);
        });

        Label lblStrength = new Label("Force du vent (0–5) :");
        Slider sliderStrength = new Slider(0, 5, env.getWindStrength());
        sliderStrength.setShowTickLabels(true);
        sliderStrength.setShowTickMarks(true);
        sliderStrength.setMajorTickUnit(1);
        sliderStrength.setMinorTickCount(0);
        sliderStrength.setSnapToTicks(true);
        sliderStrength.valueProperty().addListener((obs, old, val) -> {
            env.setWindStrength(val.intValue());
            windIndicator.refresh(env);
        });

        Label lblHumidity = new Label("Humidité (0–100 %) :");
        Slider sliderHumidity = new Slider(0, 100, env.getHumidity());
        sliderHumidity.setShowTickLabels(true);
        sliderHumidity.setShowTickMarks(true);
        sliderHumidity.setMajorTickUnit(50);
        sliderHumidity.valueProperty().addListener((obs, old, val) -> {
            env.setHumidity(val.intValue());
            windIndicator.refresh(env);
        });

        Label titleEnv = new Label("Paramètres météo");
        titleEnv.getStyleClass().add("label-title");

        VBox section = new VBox(6,
                titleEnv,
                lblDir, cbDirection,
                lblStrength, sliderStrength,
                lblHumidity, sliderHumidity);
        section.setPadding(new Insets(0, 10, 0, 10));
        return section;
    }

    /**
     * Construit la section « Edition du terrain » du panneau droit.
     *
     * <p>Elle comprend un bouton bascule pour activer/désactiver le mode
     * édition, et une palette de 9 outils (5 types de terrain + Vide, Eau,
     * Rocher, Foyer) affichée uniquement quand le mode est actif.</p>
     */
    private VBox buildEditSection() {
        btnEditToggle = new ToggleButton("✏  Mode édition");
        btnEditToggle.setMaxWidth(Double.MAX_VALUE);
        btnEditToggle.getStyleClass().add("edit-toggle");
        btnEditToggle.setOnAction(e -> toggleEditMode(btnEditToggle.isSelected()));

        ToggleGroup toolGroup = new ToggleGroup();
        TilePane toolTile = new TilePane(4, 4);
        toolTile.setPrefColumns(3);

        for (EditTool tool : EditTool.values()) {
            String base     = "-fx-background-color:" + tool.bgColor
                            + "; -fx-text-fill:" + tool.fgColor + ";";
            String selected = base + "-fx-border-color:white; -fx-border-width:2;"
                            + "-fx-border-radius:4;";

            ToggleButton btn = new ToggleButton(tool.label);
            btn.setToggleGroup(toolGroup);
            btn.getStyleClass().add("tool-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle(tool == EditTool.FOYER ? selected : base);
            btn.setOnAction(e -> selectedTool = tool);
            btn.selectedProperty().addListener((obs, old, sel) ->
                    btn.setStyle(sel ? selected : base));
            if (tool == EditTool.FOYER) btn.setSelected(true);
            toolTile.getChildren().add(btn);
        }

        Label hint = new Label("Cliquer ou glisser pour peindre");
        hint.getStyleClass().add("label-hint");

        editToolsBox = new VBox(4, toolTile, hint);
        editToolsBox.setVisible(false);
        editToolsBox.setManaged(false);

        Label titleEdit = new Label("Edition du terrain");
        titleEdit.getStyleClass().add("label-title");

        return new VBox(6, titleEdit, btnEditToggle, editToolsBox);
    }
}
