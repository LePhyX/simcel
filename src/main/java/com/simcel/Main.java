package com.simcel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.simcel.controller.SimulationController;
import com.simcel.model.CellState;
import com.simcel.model.Environment;
import com.simcel.model.FireSimulator;
import com.simcel.model.Grid;
import com.simcel.model.SimulationListener;
import com.simcel.model.WindDirection;
import com.simcel.view.ConsoleView;
import com.simcel.view.MainWindow;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application SimCel.
 *
 * <p>
 * Supporte deux modes de lancement :</p>
 * <ul>
 * <li><b>Mode graphique (défaut)</b> : démarre une application JavaFX.</li>
 * <li><b>Mode console</b> : activé par le flag {@code --headless}, lance une
 * simulation textuelle ANSI sans interface graphique. Paramètres optionnels
 * :</li>
 * </ul>
 * <pre>
 *   --width   &lt;n&gt;   largeur de la grille  (défaut : 40)
 *   --height  &lt;n&gt;   hauteur de la grille  (défaut : 20)
 *   --ticks   &lt;n&gt;   nombre maximum de ticks (défaut : 100)
 *   --delay   &lt;ms&gt;  délai entre deux ticks  (défaut : 200)
 * </pre>
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Grid grid = new Grid(50, 30);
        grid.initRandom(0.6, 0.2, 0.1, 0.05, 0.05);
        grid.setFire(25, 15);
        grid.saveInitialState();

        Environment env               = new Environment();
        FireSimulator simulator       = new FireSimulator(grid, env);
        SimulationController ctrl     = new SimulationController(simulator, 200);

        new MainWindow(simulator, ctrl).show(primaryStage);
    }

    /**
     * Méthode principale. Délègue vers {@link #runCLI} si {@code --headless}
     * est présent, sinon lance JavaFX.
     *
     * @param args arguments de la ligne de commande
     * @throws InterruptedException si le thread principal est interrompu
     * pendant l'attente de fin de simulation
     */
    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("--headless")) {
            runCLI(args);
        } else {
            launch(args);
        }
    }

    // -------------------------------------------------------------------------
    // Mode console
    // -------------------------------------------------------------------------
    /**
     * Lance la simulation en mode console (headless).
     *
     * @param args arguments de la ligne de commande
     * @throws InterruptedException si le thread est interrompu pendant
     * l'attente
     */
    private static void runCLI(String[] args) throws InterruptedException {
        int width = 40;
        int height = 20;
        int maxTicks = 100;
        int delay = 200;
        WindDirection windDir = WindDirection.N;
        int windStrength = 0;
        int humidity = 0;

        for (int i = 1; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--width" ->
                    width = Integer.parseInt(args[++i]);
                case "--height" ->
                    height = Integer.parseInt(args[++i]);
                case "--ticks" ->
                    maxTicks = Integer.parseInt(args[++i]);
                case "--delay" ->
                    delay = Math.max(1, Integer.parseInt(args[++i]));
                case "--wind-direction" ->
                    windDir = WindDirection.valueOf(args[++i].toUpperCase());
                case "--wind-strength" ->
                    windStrength = Integer.parseInt(args[++i]);
                case "--humidity" ->
                    humidity = Integer.parseInt(args[++i]);
            }
        }

        Grid grid = new Grid(width, height);
        grid.initRandom(0.6, 0.2, 0.1, 0.05, 0.05);
        grid.setFire(width / 2, height / 2);

        Environment env = new Environment(windDir, windStrength, humidity);
        FireSimulator simulator = new FireSimulator(grid, env);
        SimulationController ctrl = new SimulationController(simulator, delay);
        ConsoleView view = new ConsoleView();

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
            public void onSimulationEnd() {
            }
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

    /**
     * Indique si la grille contient au moins une cellule
     * {@link CellState#EN_FEU}.
     *
     * @param grid grille à inspecter
     * @return {@code true} si un foyer actif est présent
     */
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
