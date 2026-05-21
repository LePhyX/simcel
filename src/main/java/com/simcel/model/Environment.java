package com.simcel.model;

public class Environment {

    private WindDirection direction;
    private int windStrength;
    private int humidity;

    public Environment() {
        this.direction = WindDirection.N;
        this.windStrength = 0;
        this.humidity = 50;
    }

    public Environment(WindDirection direction, int windStrength, int humidity) {
        setDirection(direction);
        setWindStrength(windStrength);
        setHumidity(humidity);
    }

    public WindDirection getDirection() { return direction; }
    public int getWindStrength()        { return windStrength; }
    public int getHumidity()            { return humidity; }

    public void setDirection(WindDirection d) {
        if (d == null) throw new IllegalArgumentException("WindDirection cannot be null");
        this.direction = d;
    }

    public void setWindStrength(int v) {
        this.windStrength = Math.max(0, Math.min(5, v));
    }

    public void setHumidity(int v) {
        this.humidity = Math.max(0, Math.min(100, v));
    }
}
