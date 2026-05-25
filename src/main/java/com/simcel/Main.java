package com.simcel;

import com.simcel.model.CellState;
import com.simcel.model.Environment;
import com.simcel.model.FireSimulator;
import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import com.simcel.controller.SimulationController;
import com.simcel.view.ConsoleView;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulation de Propagation de Feu");
        primaryStage.show();
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("--headless")) {
            runCLI(args);
        } else {
            launch(args);
        }
    }

    private static void runCLI(String[] args) throws InterruptedException {
        int width    = 40;
        int height   = 20;
        int maxTicks = 100;
        int delay    = 200;

        for (int i = 1; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--width"  -> width    = Integer.parseInt(args[i + 1]);
                case "--height" -> height   = Integer.parseInt(args[i + 1]);
                case "--ticks"  -> maxTicks = Integer.parseInt(args[i + 1]);
                case "--delay"  -> delay    = Integer.parseInt(args[i + 1]);
            }
        }

        Grid grid = new Grid(width, height);
        grid.initRandom(0.6, 0.2, 0.1, 0.05, 0.05);
        grid.setFire(width / 2, height / 2);

        Environment env           = new Environment();
        FireSimulator simulator   = new FireSimulator(grid, env);
        SimulationController ctrl = new SimulationController(simulator, delay);
        ConsoleView view          = new ConsoleView();

        int finalMaxTicks = maxTicks;
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger lastTick = new AtomicInteger(0);

        simulator.addListener(new SimulationListener() {
            @Override
            public void onTick(int tick, Grid g) {
                view.render(tick, g);
                lastTick.set(tick);

                if (tick >= finalMaxTicks || !hasActiveFire(g)) {
                    done.countDown();
                }
            }

            @Override
            public void onSimulationEnd() {}
        });

        view.printLegend();
        Thread.sleep(1000);
        ctrl.start();
        done.await();
        ctrl.stop();

        System.out.println("\n=== Simulation terminée ===");
        System.out.printf("Ticks écoulés : %d%n", lastTick.get());
        view.printStats(lastTick.get(), grid);

    }

    private static boolean hasActiveFire(Grid grid) {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                if (grid.getCell(x, y).getState() == CellState.EN_FEU) {
                    return true;
                }
            }
        }
        return false;
    }
}
