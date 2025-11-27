package com.battlecity;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ExplanationScene {
    private Scene scene;
    private Stage stage;
    private Scene menuScene;

    public ExplanationScene(Stage stage, Scene menuScene, int width, int height) {
        this.stage = stage;
        this.menuScene = menuScene;

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: black;");

        // Title
        Label title = new Label("GAME EXPLANATION");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        title.setTextFill(Color.YELLOW);

        // Content container
        VBox content = new VBox(15);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(10));

        // Power-ups section
        Label powerUpsTitle = new Label("POWER-UPS:");
        powerUpsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        powerUpsTitle.setTextFill(Color.CYAN);
        content.getChildren().add(powerUpsTitle);

        addPowerUpExplanation(content, "GUN (Red)", "Ability to break steel/iron walls");
        addPowerUpExplanation(content, "STAR (Yellow)", "Shooting faster (stackable - each star increases speed)");
        addPowerUpExplanation(content, "CAR (Green)", "Tank moves faster (stackable - each car increases speed by 30%)");
        addPowerUpExplanation(content, "SHIP (Cyan)", "Can swim through water + 1 extra shot protection (lose SHIP on hit)");
        addPowerUpExplanation(content, "SHOVEL (Orange)", "Base surrounded by steel for 1 minute");
        addPowerUpExplanation(content, "SAW (Brown)", "Able to destroy forest/trees");
        addPowerUpExplanation(content, "TANK (Green)", "Extra life");
        addPowerUpExplanation(content, "SHIELD (Blue)", "Shield for 1 minute (Players) / Extra life (Enemies)");
        addPowerUpExplanation(content, "MACHINEGUN (Purple)", "Shoots multiple bullets in a line (stackable, max 5 bullets)");

        // Enemy tanks section
        Label enemiesTitle = new Label("\nENEMY TANK TYPES:");
        enemiesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        enemiesTitle.setTextFill(Color.CYAN);
        content.getChildren().add(enemiesTitle);

        addEnemyExplanation(content, "REGULAR (Red)", "1 shot to kill, normal speed");
        addEnemyExplanation(content, "ARMORED (Dark Red)", "2 shots to kill, normal speed, slightly bigger");
        addEnemyExplanation(content, "FAST (Light Red/Pink)", "1 shot to kill, 1.5x faster speed");
        addEnemyExplanation(content, "POWER (Rainbow)", "2 shots to kill, drops power-up on EVERY hit");
        addEnemyExplanation(content, "HEAVY (Black)", "3 shots to kill, 1.5x faster, appears in last 5 enemies");

        // Game mechanics section
        Label mechanicsTitle = new Label("\nGAME MECHANICS:");
        mechanicsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        mechanicsTitle.setTextFill(Color.CYAN);
        content.getChildren().add(mechanicsTitle);

        addMechanicExplanation(content, "ICE Blocks", "Move 2x faster on ice, slide 1 block after releasing movement key");
        addMechanicExplanation(content, "Bullet Collision", "Bullets destroy each other when they collide");
        addMechanicExplanation(content, "Tank Collision", "Tanks cannot pass through each other or the base");
        addMechanicExplanation(content, "Death & Respawn", "Lose all power-ups on death, respawn with shield at start position");

        // Controls section
        Label controlsTitle = new Label("\nCONTROLS:");
        controlsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        controlsTitle.setTextFill(Color.CYAN);
        content.getChildren().add(controlsTitle);

        addControlExplanation(content, "Player 1", "WASD to move, SPACE to shoot");
        addControlExplanation(content, "Player 2", "Arrow keys to move, ENTER to shoot");
        addControlExplanation(content, "Menu", "ESC to return to menu during game");

        // Wrap content in ScrollPane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: black; -fx-background-color: black;");
        scrollPane.setPrefHeight(height - 150);

        // Back button
        Button backButton = new Button("BACK");
        backButton.setPrefSize(200, 50);
        backButton.setStyle("-fx-font-size: 20px; -fx-background-color: #444; -fx-text-fill: white;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-font-size: 20px; -fx-background-color: #666; -fx-text-fill: white;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-font-size: 20px; -fx-background-color: #444; -fx-text-fill: white;"));
        backButton.setOnAction(e -> stage.setScene(menuScene));

        root.getChildren().addAll(title, scrollPane, backButton);

        scene = new Scene(root, width, height);
    }

    private void addPowerUpExplanation(VBox container, String name, String description) {
        Label label = new Label("• " + name + ": " + description);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);
        container.getChildren().add(label);
    }

    private void addEnemyExplanation(VBox container, String name, String description) {
        Label label = new Label("• " + name + ": " + description);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);
        container.getChildren().add(label);
    }

    private void addMechanicExplanation(VBox container, String name, String description) {
        Label label = new Label("• " + name + ": " + description);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);
        container.getChildren().add(label);
    }

    private void addControlExplanation(VBox container, String name, String description) {
        Label label = new Label("• " + name + ": " + description);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);
        container.getChildren().add(label);
    }

    public Scene getScene() {
        return scene;
    }
}
