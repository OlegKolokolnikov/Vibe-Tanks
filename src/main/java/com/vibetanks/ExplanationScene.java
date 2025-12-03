package com.vibetanks;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
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

        addPowerUpExplanation(content, PowerUp.Type.GUN, "GUN", "Ability to break steel/iron walls");
        addPowerUpExplanation(content, PowerUp.Type.STAR, "STAR", "Shooting faster (stackable - each star increases speed)");
        addPowerUpExplanation(content, PowerUp.Type.CAR, "CAR", "Tank moves faster (stackable - each car increases speed by 30%)");
        addPowerUpExplanation(content, PowerUp.Type.SHIP, "SHIP", "Can swim through water + 1 extra shot protection (lose SHIP on hit)");
        addPowerUpExplanation(content, PowerUp.Type.SHOVEL, "SHOVEL", "Base surrounded by steel for 1 minute");
        addPowerUpExplanation(content, PowerUp.Type.SAW, "SAW", "Able to destroy forest/trees");
        addPowerUpExplanation(content, PowerUp.Type.TANK, "TANK", "Extra life");
        addPowerUpExplanation(content, PowerUp.Type.SHIELD, "SHIELD", "Shield for 1 minute (Players) / Extra life (Enemies)");
        addPowerUpExplanation(content, PowerUp.Type.MACHINEGUN, "MACHINEGUN", "Shoots multiple bullets in a line (stackable, max 5 bullets)");
        addPowerUpExplanation(content, PowerUp.Type.FREEZE, "FREEZE", "Freeze all enemies for 10 seconds (or players if enemy takes it)");
        addPowerUpExplanation(content, PowerUp.Type.BOMB, "BOMB", "Destroy all enemies on screen (or damage all players if enemy takes it)");

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
        addEnemyExplanation(content, "BOSS (4x size, Rainbow)", "12 health, immune to freeze, big bullets - spawns LAST!");

        // Game mechanics section
        Label mechanicsTitle = new Label("\nGAME MECHANICS:");
        mechanicsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        mechanicsTitle.setTextFill(Color.CYAN);
        content.getChildren().add(mechanicsTitle);

        addMechanicExplanation(content, "ICE Blocks", "Move 2x faster on ice, slide 1 block after releasing movement key");
        addMechanicExplanation(content, "Bullet Collision", "Bullets destroy each other when they collide");
        addMechanicExplanation(content, "Tank Collision", "Tanks cannot pass through each other or the base");
        addMechanicExplanation(content, "Death & Respawn", "Lose all power-ups on death, respawn with shield at start position");
        addMechanicExplanation(content, "Score System", "1 point per kill, bonus for special enemies. Extra life every 100 points!");

        // Controls section
        Label controlsTitle = new Label("\nCONTROLS:");
        controlsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        controlsTitle.setTextFill(Color.CYAN);
        content.getChildren().add(controlsTitle);

        addControlExplanation(content, "Movement", "Arrow Keys or WASD to move");
        addControlExplanation(content, "Shoot", "SPACE to shoot");
        addControlExplanation(content, "Menu/Pause", "ESC to pause or return to menu");
        addControlExplanation(content, "Next Level", "ENTER after victory to continue");
        addControlExplanation(content, "Take Life", "ENTER when dead to take life from teammate (multiplayer)");

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

    private void addPowerUpExplanation(VBox container, PowerUp.Type type, String name, String description) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Create small canvas to render power-up icon
        Canvas iconCanvas = new Canvas(32, 32);
        GraphicsContext gc = iconCanvas.getGraphicsContext2D();

        // Create temporary power-up to render it
        PowerUp tempPowerUp = new PowerUp(0, 0, type);
        tempPowerUp.render(gc);

        // Text description
        Label label = new Label(name + ": " + description);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.WHITE);
        label.setWrapText(true);

        row.getChildren().addAll(iconCanvas, label);
        container.getChildren().add(row);
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
