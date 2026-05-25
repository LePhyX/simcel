package com.simcel.model;

public interface SimulationListener {
    void onTick(int tick, Grid grid);
    void onSimulationEnd();
}
