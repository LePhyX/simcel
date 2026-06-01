package com.simcel.view;

import com.simcel.model.CellState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Panneau affichant la légende des états de cellules de la simulation.
 *
 * <p>Pour chaque valeur de {@link CellState}, une entrée est créée avec un
 * carré coloré (la couleur définie par {@link CellState#getColor()}) et un
 * libellé en français. Le panneau est purement statique : il n'implémente pas
 * {@link com.simcel.model.SimulationListener} car les couleurs ne changent pas
 * au cours de la simulation.</p>
 */
public class LegendPanel extends VBox {

    /**
     * Crée le panneau de légende avec toutes les entrées de {@link CellState}.
     */
    public LegendPanel() {
        super(4);
        setPadding(new Insets(10, 6, 6, 6));

        Label title = new Label("Légende");
        title.getStyleClass().add("label-title");
        getChildren().addAll(title, new Separator());

        for (CellState state : CellState.values()) {
            getChildren().add(buildEntry(state));
        }
    }

    // -------------------------------------------------------------------------

    private static HBox buildEntry(CellState state) {
        Rectangle swatch = new Rectangle(11, 11, Color.web(state.getColor()));
        swatch.setStroke(Color.web("#444466"));
        swatch.setStrokeWidth(0.5);

        Label lbl = new Label(labelFor(state));
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");

        HBox row = new HBox(6, swatch, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String labelFor(CellState state) {
        return switch (state) {
            case VIDE   -> "Vide";
            case SAIN   -> "Sain";
            case EN_FEU -> "En feu";
            case BRULE  -> "Brûlé";
            case EAU    -> "Eau";
            case ROCHER -> "Rocher";
        };
    }
}
