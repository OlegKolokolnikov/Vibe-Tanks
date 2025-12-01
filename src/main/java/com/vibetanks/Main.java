package com.vibetanks;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    private static final int WINDOW_WIDTH = 832;  // 26 tiles * 32 pixels
    private static final int WINDOW_HEIGHT = 832; // 26 tiles * 32 pixels

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Vibe Tanks");

        // Clean up any zombie processes on port 25565 from previous runs
        System.out.println("Performing startup port cleanup...");
        NetworkManager.cleanupPortOnStartup();

        MenuScene menuScene = new MenuScene(primaryStage, WINDOW_WIDTH, WINDOW_HEIGHT);

        primaryStage.setScene(menuScene.getScene());
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
