package com.simcel.view;

import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Barre d'en-tête applicative affichée au sommet de la fenêtre.
 *
 * <p>Affiche le nom de l'application, le statut courant (en pause / en cours),
 * le tick courant, les dimensions de la grille, ainsi que les boutons
 * Sauvegarder et Charger.</p>
 */
public class HeaderBar extends HBox implements SimulationListener {

    private final Label lblStatus;
    private final Label lblTick;
    private final Label lblGridSize;

    /**
     * Crée la barre d'en-tête.
     *
     * @param btnSave   bouton de sauvegarde à intégrer dans la barre
     * @param btnLoad   bouton de chargement à intégrer dans la barre
     * @param gridWidth largeur initiale de la grille
     * @param gridHeight hauteur initiale de la grille
     */
    public HeaderBar(Button btnSave, Button btnLoad, int gridWidth, int gridHeight) {
        super(12);
        setPadding(new Insets(8, 16, 8, 16));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("header-bar");

        Label appName = new Label("SimCel");
        appName.getStyleClass().add("header-title");

        Label subtitle = new Label("Propagation de feu · automate cellulaire");
        subtitle.getStyleClass().add("header-subtitle");

        lblStatus = new Label("● EN PAUSE");
        lblStatus.getStyleClass().add("header-status-paused");

        lblTick = new Label("T 0");
        lblTick.getStyleClass().add("header-info");

        lblGridSize = new Label("GRILLE " + gridWidth + "×" + gridHeight);
        lblGridSize.getStyleClass().add("header-info");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnSave.getStyleClass().add("header-btn");
        btnLoad.getStyleClass().add("header-btn");

        getChildren().addAll(appName, subtitle, spacer,
                lblStatus, lblTick, lblGridSize, btnSave, btnLoad);
    }

    /** Met à jour le statut affiché dans la barre. */
    public void setRunning(boolean running) {
        if (running) {
            lblStatus.setText("● EN COURS");
            lblStatus.getStyleClass().setAll("header-status-running");
        } else {
            lblStatus.setText("● EN PAUSE");
            lblStatus.getStyleClass().setAll("header-status-paused");
        }
    }

    /** Met à jour les dimensions de grille affichées. */
    public void setGridSize(int w, int h) {
        lblGridSize.setText("GRILLE " + w + "×" + h);
    }

    /** Remet le compteur de tick à zéro. */
    public void resetTick() {
        lblTick.setText("T 0");
    }

    @Override
    public void onTick(int tick, Grid g) {
        Platform.runLater(() -> lblTick.setText("T " + tick));
    }

}
