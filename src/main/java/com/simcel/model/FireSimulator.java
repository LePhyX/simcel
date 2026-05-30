package com.simcel.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Moteur de simulation de propagation du feu sur une {@link Grid}.
 *
 * <p>
 * L'algorithme repose sur un double buffering : un snapshot des états est pris
 * en début de tick afin que toutes les décisions de propagation soient basées
 * sur l'état <em>avant</em> le tick, et non sur un état partiellement mis à
 * jour.</p>
 *
 * <h2>Déroulement d'un tick</h2>
 * <ol>
 * <li>Snapshot des états courants.</li>
 * <li>Pour chaque cellule {@link CellState#EN_FEU}, calcul stochastique de
 * propagation vers les voisins {@link CellState#SAIN}.</li>
 * <li>Application atomique des nouvelles ignitions.</li>
 * <li>Décrémentation du temps de combustion des cellules en feu ; extinction de
 * celles dont le compteur atteint 0.</li>
 * <li>Notification des {@link SimulationListener}.</li>
 * </ol>
 */
public class FireSimulator {

    private final Grid grid;
    private final Environment environment;
    private final List<SimulationListener> listeners;
    private final Random random;
    private int currentTick;

    /**
     * Crée un simulateur pour la grille et l'environnement donnés.
     *
     * @param grid grille de simulation, non {@code null}
     * @param environment conditions environnementales, non {@code null}
     */
    public FireSimulator(Grid grid, Environment environment) {
        this.grid = grid;
        this.environment = environment;
        this.listeners = new ArrayList<>();
        this.random = new Random();
        this.currentTick = 0;
    }

    /**
     * Avance la simulation d'un pas de temps.
     *
     * <p>
     * Toutes les modifications de la grille sont appliquées de manière atomique
     * après la phase de calcul, garantissant l'indépendance des décisions par
     * rapport à l'ordre de parcours des cellules.</p>
     */
    public void tick() {
        CellState[][] snapshot = takeSnapshot();

        Set<Cell> toIgnite = collectIgnitions(snapshot);

        for (Cell cell : toIgnite) {
            cell.ignite();
        }

        applyExtinctions(snapshot);

        currentTick++;
        notifyListeners(currentTick);
    }

    /**
     * Calcule la probabilité qu'une cellule cible s'enflamme à partir d'une
     * cellule source en feu, en intégrant le vent et l'humidité.
     *
     * <p>
     * La formule appliquée est :</p>
     * <pre>
     *   windFactor     = 1.0 + (windStrength / 5.0) × cos(θ)
     *   humidityFactor = 1.0 − humidity / 100.0
     *   P = clamp(baseInflammability × windFactor × humidityFactor, 0.0, 1.0)
     * </pre>
     * <p>
     * où θ est l'angle entre le vecteur vent et le vecteur source→cible. Si
     * l'intensité du vent est nulle, {@code windFactor} vaut {@code 1.0}.</p>
     *
     * @param srcX colonne de la cellule source
     * @param srcY ligne de la cellule source
     * @param tgtX colonne de la cellule cible
     * @param tgtY ligne de la cellule cible
     * @param tgt  cellule cible (candidate à l'inflammation)
     * @return probabilité dans {@code [0.0, 1.0]}
     */
    public double computeInflammationProbability(int srcX, int srcY, int tgtX, int tgtY, Cell tgt) {
        double base = tgt.getType().getInflammability();
        double windFactor = applyWindFactor(tgtX - srcX, tgtY - srcY);
        double p = applyHumidityFactor(base * windFactor);
        return Math.max(0.0, Math.min(1.0, p));
    }

    /**
     * Retourne la grille associée à ce simulateur.
     *
     * @return grille, jamais {@code null}
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Retourne le numéro du tick courant (0 avant le premier tick).
     *
     * @return tick courant, &ge; 0
     */
    public int getCurrentTick() {
        return currentTick;
    }

    /**
     * Enregistre un observateur qui sera notifié à chaque tick.
     *
     * @param listener observateur à ajouter, non {@code null}
     */
    public void addListener(SimulationListener listener) {
        listeners.add(listener);
    }

    /**
     * Retire un observateur précédemment enregistré.
     *
     * @param listener observateur à retirer
     */
    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------
    /**
     * Capture les états actuels dans un tableau indépendant.
     */
    private CellState[][] takeSnapshot() {
        int w = grid.getWidth();
        int h = grid.getHeight();
        CellState[][] snapshot = new CellState[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                snapshot[x][y] = grid.getCell(x, y).getState();
            }
        }
        return snapshot;
    }

    /**
     * Parcourt les cellules en feu du snapshot et collecte les voisins à
     * enflammer. Un {@link LinkedHashSet} garantit qu'une même cellule ne peut
     * être ajoutée qu'une seule fois, même si plusieurs voisins en feu la
     * ciblent simultanément.
     */
    private Set<Cell> collectIgnitions(CellState[][] snapshot) {
        Set<Cell> toIgnite = new LinkedHashSet<>();
        int w = grid.getWidth();
        int h = grid.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (snapshot[x][y] == CellState.EN_FEU) {
                    for (int ndy = -1; ndy <= 1; ndy++) {
                        for (int ndx = -1; ndx <= 1; ndx++) {
                            if (ndx == 0 && ndy == 0) {
                                continue;
                            }
                            int nx = x + ndx;
                            int ny = y + ndy;
                            if (grid.isInBounds(nx, ny)) {
                                Cell neighbor = grid.getCell(nx, ny);
                                if (neighbor.getState() == CellState.SAIN) {
                                    double p = computeInflammationProbability(x, y, nx, ny, neighbor);
                                    if (random.nextDouble() < p) {
                                        toIgnite.add(neighbor);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return toIgnite;
    }

    /**
     * Décrémente les compteurs de combustion et éteint les cellules épuisées.
     */
    private void applyExtinctions(CellState[][] snapshot) {
        int w = grid.getWidth();
        int h = grid.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (snapshot[x][y] == CellState.EN_FEU) {
                    Cell cell = grid.getCell(x, y);
                    cell.decrementBurnTime();
                    if (cell.getRemainingBurnTime() == 0) {
                        cell.burnOut();
                    }
                }
            }
        }
    }

    /**
     * Envoie la notification de tick à tous les observateurs enregistrés.
     */
    private void notifyListeners(int tick) {
        for (SimulationListener listener : listeners) {
            listener.onTick(tick, grid);
        }
    }

    /**
     * Applique le facteur d'humidité à une probabilité intermédiaire.
     *
     * <pre>
     *   P_finale = p × (1.0 − humidity / 100.0)
     * </pre>
     *
     * @param p probabilité avant correction d'humidité
     * @return probabilité réduite par l'humidité ambiante
     */
    private double applyHumidityFactor(double p) {
        return p * (1.0 - environment.getHumidity() / 100.0);
    }

    /**
     * Calcule le facteur multiplicatif du vent pour une propagation dans la
     * direction {@code (pdx, pdy)}.
     *
     * <p>
     * Retourne {@code 1.0} si l'intensité du vent est nulle. Sinon, utilise le
     * cosinus de l'angle entre le vecteur vent et le vecteur de propagation
     * ({@code cosθ = dot(vent, propagation) / (|vent| × |propagation|)}) :</p>
     * <pre>
     *   windFactor = 1.0 + (windStrength / 5.0) × cosθ
     * </pre>
     *
     * @param pdx composante X du vecteur source→cible
     * @param pdy composante Y du vecteur source→cible
     * @return facteur vent (peut être &lt; 1 ou &gt; 1 ; le clamp est à la charge de l'appelant)
     */
    private double applyWindFactor(int pdx, int pdy) {
        int windStrength = environment.getWindStrength();
        if (windStrength == 0) {
            return 1.0;
        }
        int wdx = environment.getDirection().getDx();
        int wdy = environment.getDirection().getDy();
        double dot = wdx * pdx + wdy * pdy;
        double windMag = Math.sqrt(wdx * wdx + wdy * wdy);
        double propMag = Math.sqrt(pdx * pdx + pdy * pdy);
        double cosTheta = dot / (windMag * propMag);
        return 1.0 + (windStrength / 5.0) * cosTheta;
    }
}
