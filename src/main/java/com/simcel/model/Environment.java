package com.simcel.model;

/**
 * Conditions environnementales globales de la simulation.
 *
 * <p>Regroupe les trois paramètres météorologiques qui influencent la propagation
 * du feu :</p>
 * <ul>
 *   <li><b>direction</b> : direction du vent ({@link WindDirection}) ;</li>
 *   <li><b>windStrength</b> : intensité du vent, clampée dans
 *       {@code [MIN_WIND, MAX_WIND]} (0 = calme, 5 = tempête) ;</li>
 *   <li><b>humidity</b> : taux d'humidité ambiant, clampé dans
 *       {@code [MIN_HUMIDITY, MAX_HUMIDITY]}.</li>
 * </ul>
 *
 * <p>Les setters garantissent que les valeurs restent dans leurs plages
 * respectives ; aucune exception n'est levée pour des valeurs hors bornes
 * (elles sont silencieusement ramenées au min/max).</p>
 */
public class Environment implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** Intensité minimale du vent. */
    public static final int MIN_WIND     = 0;
    /** Intensité maximale du vent. */
    public static final int MAX_WIND     = 5;
    /** Humidité minimale (%). */
    public static final int MIN_HUMIDITY = 0;
    /** Humidité maximale (%). */
    public static final int MAX_HUMIDITY = 100;

    private volatile WindDirection direction;
    private volatile int windStrength;
    private volatile int humidity;

    /**
     * Crée un environnement par défaut : vent nul vers le Nord, humidité 50 %.
     */
    public Environment() {
        this.direction    = WindDirection.N;
        this.windStrength = MIN_WIND;
        this.humidity     = 50;
    }

    /**
     * Crée un environnement avec les paramètres fournis.
     *
     * @param direction    direction du vent, non {@code null}
     * @param windStrength intensité du vent (clampée dans {@code [MIN_WIND, MAX_WIND]})
     * @param humidity     humidité en pourcentage (clampée dans {@code [MIN_HUMIDITY, MAX_HUMIDITY]})
     * @throws IllegalArgumentException si {@code direction} est {@code null}
     */
    public Environment(WindDirection direction, int windStrength, int humidity) {
        setDirection(direction);
        setWindStrength(windStrength);
        setHumidity(humidity);
    }

    /**
     * Retourne la direction du vent.
     *
     * @return direction courante, jamais {@code null}
     */
    public WindDirection getDirection() {
        return direction;
    }

    /**
     * Retourne l'intensité du vent.
     *
     * @return valeur dans {@code [MIN_WIND, MAX_WIND]}
     */
    public int getWindStrength() {
        return windStrength;
    }

    /**
     * Retourne le taux d'humidité ambiant.
     *
     * @return valeur dans {@code [MIN_HUMIDITY, MAX_HUMIDITY]}
     */
    public int getHumidity() {
        return humidity;
    }

    /**
     * Modifie la direction du vent.
     *
     * @param direction nouvelle direction, non {@code null}
     * @throws IllegalArgumentException si {@code direction} est {@code null}
     */
    public void setDirection(WindDirection direction) {
        if (direction == null) {
            throw new IllegalArgumentException("WindDirection cannot be null");
        }
        this.direction = direction;
    }

    /**
     * Modifie l'intensité du vent.
     * La valeur est clampée silencieusement dans {@code [MIN_WIND, MAX_WIND]}.
     *
     * @param windStrength intensité souhaitée
     */
    public void setWindStrength(int windStrength) {
        this.windStrength = clamp(windStrength, MIN_WIND, MAX_WIND);
    }

    /**
     * Modifie le taux d'humidité.
     * La valeur est clampée silencieusement dans {@code [MIN_HUMIDITY, MAX_HUMIDITY]}.
     *
     * @param humidity humidité souhaitée en pourcentage
     */
    public void setHumidity(int humidity) {
        this.humidity = clamp(humidity, MIN_HUMIDITY, MAX_HUMIDITY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
