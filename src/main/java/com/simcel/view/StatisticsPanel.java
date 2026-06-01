package com.simcel.view;

import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;

/**
 * Barre de statistiques horizontale affichée sous la grille.
 *
 * <p>Implémente {@link SimulationListener} : les comptages sont effectués sur
 * le thread de simulation, seule la mise à jour des labels est déléguée au
 * thread JavaFX via {@link Platform#runLater}.</p>
 */
public class StatisticsPanel extends HBox implements SimulationListener {

    private final Label lblTick        = new Label("Tick : 0");
    private final Label lblSain        = new Label("Sains : —");
    private final Label lblFeu         = new Label("En feu : —");
    private final Label lblBrule       = new Label("Brûlés : —");
    private final Label lblDestruction = new Label("Destruction : —");

    /** Crée la barre de statistiques. */
    public StatisticsPanel() {
        super(12);
        setPadding(new Insets(6, 14, 6, 14));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("stats-bar");

        Label title = new Label("Statistiques :");
        title.getStyleClass().add("label-title");

        getChildren().addAll(
                title,
                vSep(), lblTick,
                vSep(), lblSain,
                vSep(), lblFeu,
                vSep(), lblBrule,
                vSep(), lblDestruction);
    }

    /**
     * Calcule les comptages sur le thread de simulation, puis met à jour les
     * labels sur le thread JavaFX.
     *
     * @param tick numéro du tick
     * @param grid grille courante
     */
    @Override
    public void onTick(int tick, Grid grid) {
        int sain = 0, enFeu = 0, brule = 0;
        int total = grid.getWidth() * grid.getHeight();

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
        final double taux = total > 0 ? fBrule * 100.0 / total : 0.0;

        Platform.runLater(() -> {
            lblTick.setText("Tick : " + tick);
            lblSain.setText("Sains : " + fSain);
            lblFeu.setText("En feu : " + fFeu);
            lblBrule.setText("Brûlés : " + fBrule);
            lblDestruction.setText(String.format("Destruction : %.1f %%", taux));
        });
    }

    @Override
    public void onSimulationEnd() {}

    /**
     * Remet les labels à leur état initial.
     * Doit être appelé depuis le thread JavaFX.
     */
    public void reset() {
        lblTick.setText("Tick : 0");
        lblSain.setText("Sains : —");
        lblFeu.setText("En feu : —");
        lblBrule.setText("Brûlés : —");
        lblDestruction.setText("Destruction : —");
    }

    private static Separator vSep() {
        Separator s = new Separator(Orientation.VERTICAL);
        s.setMinHeight(20);
        return s;
    }
}
