package com.simcel;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulation de Propagation de Feu");
        primaryStage.show();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--headless")) {
            System.out.println("Mode console (headless) — simulation de propagation de feu");
        } else {
            launch(args);
        }
    }
}
