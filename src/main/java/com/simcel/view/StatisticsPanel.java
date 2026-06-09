package com.simcel.view;

import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Panneau de statistiques vertical affiché dans le panneau droit.
 *
 * <p>Implémente {@link SimulationListener} : les comptages sont effectués sur
 * le thread de simulation, seule la mise à jour des labels et barres de
 * progression est déléguée au thread JavaFX via {@link Platform#runLater}.</p>
 */
public class StatisticsPanel extends VBox implements SimulationListener {

    private final Label       lblTick        = new Label("—");
    private final Label       lblSainPct     = new Label("—");
    private final Label       lblFeuPct      = new Label("—");
    private final Label       lblBrulePct    = new Label("—");
    private final Label       lblDestruction = new Label("—");

    private final ProgressBar pbSain  = new ProgressBar(0);
    private final ProgressBar pbFeu   = new ProgressBar(0);
    private final ProgressBar pbBrule = new ProgressBar(0);

    /** Crée le panneau de statistiques. */
    public StatisticsPanel() {
        super(4);
        setPadding(new Insets(10, 10, 10, 10));

        Label title = new Label("Statistiques");
        title.getStyleClass().add("label-title");

        for (ProgressBar pb : new ProgressBar[]{pbSain, pbFeu, pbBrule}) {
            pb.setMaxWidth(Double.MAX_VALUE);
        }
        pbSain.getStyleClass().add("pb-sain");
        pbFeu.getStyleClass().add("pb-feu");
        pbBrule.getStyleClass().add("pb-brule");

        getChildren().addAll(
                title,
                new Separator(),
                buildSimpleRow("Tick",             lblTick),
                buildProgressRow("Sains",          lblSainPct,     pbSain),
                buildProgressRow("En feu",         lblFeuPct,      pbFeu),
                buildProgressRow("Brûlés",         lblBrulePct,    pbBrule),
                buildSimpleRow("Surface détruite", lblDestruction)
        );
    }

    private HBox buildSimpleRow(String labelText, Label value) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #888899; -fx-font-size: 11px;");
        value.setStyle("-fx-text-fill: #e8e8f0; -fx-font-size: 12px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(lbl, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));
        return row;
    }

    private VBox buildProgressRow(String labelText, Label value, ProgressBar pb) {
        HBox header = buildSimpleRow(labelText, value);
        VBox row = new VBox(2, header, pb);
        row.setPadding(new Insets(2, 0, 4, 0));
        return row;
    }

    /**
     * Calcule les comptages sur le thread de simulation, puis met à jour
     * les labels et barres de progression sur le thread JavaFX.
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

        final int    fSain    = sain;
        final int    fFeu     = enFeu;
        final int    fBrule   = brule;
        final double sainPct  = total > 0 ? fSain  * 100.0 / total : 0.0;
        final double feuPct   = total > 0 ? fFeu   * 100.0 / total : 0.0;
        final double brulePct = total > 0 ? fBrule * 100.0 / total : 0.0;

        Platform.runLater(() -> {
            lblTick.setText(String.valueOf(tick));
            lblSainPct.setText(String.format("%.1f %%", sainPct));
            lblFeuPct.setText(String.format("%.1f %%", feuPct));
            lblBrulePct.setText(String.format("%.1f %%", brulePct));
            lblDestruction.setText(fBrule + " cellules");
            pbSain.setProgress(sainPct / 100.0);
            pbFeu.setProgress(feuPct / 100.0);
            pbBrule.setProgress(brulePct / 100.0);
        });
    }

    /**
     * Remet les labels et barres à leur état initial.
     * Doit être appelé depuis le thread JavaFX.
     */
    public void reset() {
        lblTick.setText("—");
        lblSainPct.setText("—");
        lblFeuPct.setText("—");
        lblBrulePct.setText("—");
        lblDestruction.setText("—");
        pbSain.setProgress(0);
        pbFeu.setProgress(0);
        pbBrule.setProgress(0);
    }
}
