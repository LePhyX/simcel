package com.simcel.model;

import java.io.Serializable;

/**
 * Capture immuable de l'état complet d'une simulation à un instant donné.
 *
 * <p>Contient une copie profonde de la grille courante et de la grille
 * initiale (utilisée pour le reset), ainsi que les paramètres
 * météorologiques et le numéro de tick. Utilisée par {@link SimulationIO}
 * pour la persistance binaire.</p>
 *
 * <p>Les tableaux de cellules sont copiés à la construction via
 * {@link Cell#copy()} ; le snapshot est donc indépendant du modèle vivant
 * et peut être sérialisé à tout moment sans risque de course.</p>
 *
 * @see SimulationIO
 * @see FireSimulator#createSnapshot()
 * @see FireSimulator#applySnapshot(SimulationSnapshot)
 */
public class SimulationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int          width;
    private final int          height;
    private final Cell[][]     cells;
    private final Cell[][]     initialCells;
    private final WindDirection windDirection;
    private final int           windStrength;
    private final int           humidity;
    private final int           tick;

    /**
     * Construit un snapshot à partir des tableaux de cellules et de l'environnement.
     *
     * <p>Les tableaux {@code cells} et {@code initialCells} sont stockés tels quels ;
     * c'est à l'appelant ({@link FireSimulator#createSnapshot()}) de fournir
     * des copies profondes via {@link Grid#copyCells()} et
     * {@link Grid#copyInitialCells()}.</p>
     *
     * @param width        nombre de colonnes de la grille
     * @param height       nombre de lignes de la grille
     * @param cells        copie profonde des cellules courantes ({@code Cell[height][width]})
     * @param initialCells copie profonde des cellules initiales ({@code Cell[height][width]})
     * @param env          conditions météorologiques au moment de la capture
     * @param tick         numéro du tick courant
     */
    public SimulationSnapshot(int width, int height,
                               Cell[][] cells, Cell[][] initialCells,
                               Environment env, int tick) {
        this.width         = width;
        this.height        = height;
        this.cells         = cells;
        this.initialCells  = initialCells;
        this.windDirection = env.getDirection();
        this.windStrength  = env.getWindStrength();
        this.humidity      = env.getHumidity();
        this.tick          = tick;
    }

    /**
     * Retourne le nombre de colonnes de la grille capturée.
     *
     * @return largeur (&gt; 0)
     */
    public int getWidth() { return width; }

    /**
     * Retourne le nombre de lignes de la grille capturée.
     *
     * @return hauteur (&gt; 0)
     */
    public int getHeight() { return height; }

    /**
     * Retourne le tableau des cellules courantes au moment de la capture.
     *
     * @return tableau {@code Cell[height][width]}, jamais {@code null}
     */
    public Cell[][] getCells() { return cells; }

    /**
     * Retourne le tableau des cellules initiales (état de référence pour le reset).
     *
     * @return tableau {@code Cell[height][width]}, jamais {@code null}
     */
    public Cell[][] getInitialCells() { return initialCells; }

    /**
     * Retourne la direction du vent enregistrée dans ce snapshot.
     *
     * @return direction, jamais {@code null}
     */
    public WindDirection getWindDirection() { return windDirection; }

    /**
     * Retourne l'intensité du vent enregistrée dans ce snapshot.
     *
     * @return valeur dans {@code [Environment.MIN_WIND, Environment.MAX_WIND]}
     */
    public int getWindStrength() { return windStrength; }

    /**
     * Retourne le taux d'humidité enregistré dans ce snapshot.
     *
     * @return valeur dans {@code [Environment.MIN_HUMIDITY, Environment.MAX_HUMIDITY]}
     */
    public int getHumidity() { return humidity; }

    /**
     * Retourne le numéro du tick au moment de la capture.
     *
     * @return tick (&ge; 0)
     */
    public int getTick() { return tick; }
}
