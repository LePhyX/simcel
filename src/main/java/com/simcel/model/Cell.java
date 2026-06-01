package com.simcel.model;

/**
 * Unité élémentaire de la grille de simulation.
 *
 * <p>
 * Une cellule est caractérisée par un {@link CellType} immuable (le type de
 * terrain) et un {@link CellState} mutable (l'état courant). Elle maintient
 * également un compteur de temps de combustion restant, décrémenté à chaque
 * tick tant que la cellule est {@link CellState#EN_FEU}.</p>
 *
 * <p>
 * Le type de terrain ne change jamais au cours de la simulation ; seuls l'état
 * et le temps de combustion évoluent.</p>
 */
public class Cell implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private CellState state;
    private final CellType type;
    private int remainingBurnTime;

    /**
     * Crée une cellule saine du type donné.
     *
     * <p>
     * L'état initial est {@link CellState#SAIN} et le temps de combustion est
     * initialisé à {@link CellType#getBurnDuration()}.</p>
     *
     * @param type type de terrain, non {@code null}
     */
    public Cell(CellType type) {
        this.type = type;
        this.state = CellState.SAIN;
        this.remainingBurnTime = type.getBurnDuration();
    }

    /**
     * Retourne l'état courant de la cellule.
     *
     * @return état courant, jamais {@code null}
     */
    public CellState getState() {
        return state;
    }

    /**
     * Retourne le type de terrain de la cellule.
     *
     * @return type immuable, jamais {@code null}
     */
    public CellType getType() {
        return type;
    }

    /**
     * Retourne le nombre de ticks de combustion restants.
     *
     * @return valeur &ge; 0
     */
    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    /**
     * Force l'état de la cellule à la valeur donnée, sans vérification de
     * cohérence. Préférer {@link #ignite()} ou {@link #burnOut()} pour les
     * transitions métier.
     *
     * @param state nouvel état, non {@code null}
     */
    public void setState(CellState state) {
        this.state = state;
    }

    /**
     * Décrémente le compteur de combustion d'une unité, sans passer en dessous
     * de zéro.
     */
    public void decrementBurnTime() {
        if (remainingBurnTime > 0) {
            remainingBurnTime--;
        }
    }

    /**
     * Enflamme la cellule si elle est inflammable
     * ({@link CellState#isFlammable()}).
     *
     * <p>
     * Le temps de combustion est réinitialisé à la valeur par défaut du type de
     * terrain, de sorte qu'une cellule rallumée brûle à nouveau pour sa durée
     * complète.</p>
     */
    public void ignite() {
        if (state.isFlammable()) {
            state = CellState.EN_FEU;
            remainingBurnTime = type.getBurnDuration();
        }
    }

    /**
     * Marque la cellule comme entièrement consumée ({@link CellState#BRULE}) et
     * remet le compteur à zéro.
     */
    public void burnOut() {
        state = CellState.BRULE;
        remainingBurnTime = 0;
    }

    /**
     * Retourne une copie profonde de cette cellule.
     *
     * <p>
     * La copie partage le même {@link CellType} (immuable) mais possède ses
     * propres champs d'état et de temps de combustion.</p>
     *
     * @return nouvelle instance {@code Cell} à l'état identique
     */
    public Cell copy() {
        Cell copy = new Cell(this.type);
        copy.state = this.state;
        copy.remainingBurnTime = this.remainingBurnTime;
        return copy;
    }
}
