package com.simcel.view;

import com.simcel.model.CellState;
import com.simcel.model.CellType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Panneau affichant la légende des types de terrain et des états de cellules.
 *
 * <p>La légende est divisée en deux sections :
 * <ul>
 *   <li><b>Terrain</b> — les cinq types de terrain à l'état sain, chacun
 *       coloré selon {@link CellType#getHealthyColor()}.</li>
 *   <li><b>États</b> — les états spéciaux ({@link CellState#VIDE},
 *       {@link CellState#EN_FEU}, {@link CellState#BRULE},
 *       {@link CellState#EAU}, {@link CellState#ROCHER}).</li>
 * </ul>
 *
 * <p>Le panneau est purement statique (pas de {@link com.simcel.model.SimulationListener})
 * car les couleurs ne changent pas au cours de la simulation.</p>
 */
public class LegendPanel extends VBox {

    /**
     * Crée le panneau de légende.
     */
    public LegendPanel() {
        super(3);
        setPadding(new Insets(10, 6, 6, 6));

        Label title = new Label("Légende");
        title.getStyleClass().add("label-title");
        getChildren().addAll(title, new Separator());

        getChildren().add(sectionLabel("Terrain"));
        for (CellType type : CellType.values()) {
            getChildren().add(buildEntry(Color.web(type.getHealthyColor()), labelFor(type)));
        }

        getChildren().addAll(new Separator(), sectionLabel("États"));
        for (CellState state : CellState.values()) {
            if (state != CellState.SAIN) {
                getChildren().add(buildEntry(Color.web(state.getColor()), labelFor(state)));
            }
        }
    }

    // -------------------------------------------------------------------------

    private static Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #888899; -fx-font-size: 10px;");
        return lbl;
    }

    private static HBox buildEntry(Color color, String text) {
        Rectangle swatch = new Rectangle(11, 11, color);
        swatch.setStroke(Color.web("#444466"));
        swatch.setStrokeWidth(0.5);

        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");

        HBox row = new HBox(6, swatch, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String labelFor(CellType type) {
        return switch (type) {
            case FORET        -> "Forêt";
            case PRAIRIE      -> "Prairie";
            case BROUSSAILLES -> "Broussailles";
            case ZONE_HUMIDE  -> "Zone humide";
            case ZONE_URBAINE -> "Zone urbaine";
        };
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
