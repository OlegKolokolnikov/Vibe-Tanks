package com.vibetanks;

import com.vibetanks.core.GameConstants;
import com.vibetanks.ui.MenuScene;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    private static final int GAME_FIELD_SIZE = 832;  // 26 tiles * 32 pixels (game field stays same size)
    private static final int WINDOW_WIDTH = GAME_FIELD_SIZE + GameConstants.SIDEBAR_WIDTH;  // Game field + sidebar
    private static final int WINDOW_HEIGHT = GAME_FIELD_SIZE; // Height stays the same

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Vibe Tanks");

        // Note: Port cleanup removed - it was killing dedicated servers
        // If you have port conflicts, manually close the other process

        MenuScene menuScene = new MenuScene(primaryStage, WINDOW_WIDTH, WINDOW_HEIGHT);

        primaryStage.setScene(menuScene.getScene());
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Fix for 30 FPS on Windows - disable VSync lock and enable fullspeed mode
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("javafx.animation.fullspeed", "true");  // Bypass VSync timing
        System.setProperty("prism.vsync", "false");                // Disable VSync
        System.setProperty("quantum.multithreaded", "false");
        launch(args);
    }
}
