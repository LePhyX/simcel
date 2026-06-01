package com.simcel.model;

import java.io.Serializable;

/**
 * Snapshot immutable of the simulation state at a given tick.
 *
 * <p>Stores a deep copy of the grid cells, the initial cells used for reset,
 * the environment parameters, and the current tick number. Used by
 * {@link SimulationIO} to serialize and deserialize simulation state.</p>
 */
public class SimulationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;
    private final Cell[][] cells;
    private final Cell[][] initialCells;
    private final WindDirection windDirection;
    private final int windStrength;
    private final int humidity;
    private final int tick;

    /**
     * Creates a snapshot from raw grid arrays and environment data.
     *
     * @param width        grid column count
     * @param height       grid row count
     * @param cells        deep-copied current cell states
     * @param initialCells deep-copied initial cell states
     * @param env          current environment conditions
     * @param tick         current tick number
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

    /** @return grid column count */
    public int getWidth()  { return width; }

    /** @return grid row count */
    public int getHeight() { return height; }

    /** @return deep copy of current cell states */
    public Cell[][] getCells() { return cells; }

    /** @return deep copy of initial cell states */
    public Cell[][] getInitialCells() { return initialCells; }

    /** @return wind direction at save time */
    public WindDirection getWindDirection() { return windDirection; }

    /** @return wind strength at save time */
    public int getWindStrength() { return windStrength; }

    /** @return humidity at save time */
    public int getHumidity() { return humidity; }

    /** @return tick number at save time */
    public int getTick() { return tick; }
}
