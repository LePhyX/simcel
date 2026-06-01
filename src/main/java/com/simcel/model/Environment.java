package com.simcel.model;

/**
 * Conditions environnementales globales de la simulation.
 *
 * <p>
 * Regroupe les trois paramètres météorologiques qui influencent la propagation
 * du feu :</p>
 * <ul>
 * <li><b>direction</b> : direction du vent ({@link WindDirection}) ;</li>
 * <li><b>windStrength</b> : intensité du vent, valeur entière clampée dans
 * {@code [0, 5]} (0 = calme, 5 = tempête) ;</li>
 * <li><b>humidity</b> : taux d'humidité ambiant, valeur entière clampée dans
 * {@code [0, 100]}.</li>
 * </ul>
 *
 * <p>
 * Les setters garantissent que les valeurs restent dans leurs plages
 * respectives ; aucune exception n'est levée pour des valeurs hors bornes
 * (elles sont silencieusement ramenées au min/max).</p>
 */
public class Environment {

    private volatile WindDirection direction;
    private volatile int windStrength;
    private volatile int humidity;

    /**
     * Crée un environnement par défaut : vent nul vers le Nord, humidité 50 %.
     */
    public Environment() {
        this.direction = WindDirection.N;
        this.windStrength = 0;
        this.humidity = 50;
    }

    /**
     * Crée un environnement avec les paramètres fournis.
     *
     * @param direction direction du vent, non {@code null}
     * @param windStrength intensité du vent (clampée dans {@code [0, 5]})
     * @param humidity humidité en pourcentage (clampée dans {@code [0, 100]})
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
     * @return valeur dans {@code [0, 5]}
     */
    public int getWindStrength() {
        return windStrength;
    }

    /**
     * Retourne le taux d'humidité ambiant.
     *
     * @return valeur dans {@code [0, 100]}
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
     * Modifie l'intensité du vent. La valeur est clampée dans {@code [0, 5]}.
     *
     * @param windStrength intensité souhaitée
     */
    public void setWindStrength(int windStrength) {
        this.windStrength = Math.max(0, Math.min(5, windStrength));
    }

    /**
     * Modifie le taux d'humidité. La valeur est clampée dans {@code [0, 100]}.
     *
     * @param humidity humidité souhaitée en pourcentage
     */
    public void setHumidity(int humidity) {
        this.humidity = Math.max(0, Math.min(100, humidity));
    }
}
