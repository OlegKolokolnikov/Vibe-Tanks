package com.battlecity;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MenuScene {
    private Scene scene;
    private Stage stage;
    private int windowWidth;
    private int windowHeight;

    public MenuScene(Stage stage, int windowWidth, int windowHeight) {
        this.stage = stage;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        createMenu();
    }

    private void createMenu() {
        VBox menuLayout = new VBox(30);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setStyle("-fx-background-color: black;");

        // Title
        Label title = new Label("BATTLE CITY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        title.setTextFill(Color.YELLOW);

        // Subtitle
        Label subtitle = new Label("Tank Battle Game");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        subtitle.setTextFill(Color.WHITE);

        // 1 Player button
        Button onePlayerButton = new Button("1 PLAYER");
        styleButton(onePlayerButton);
        onePlayerButton.setOnAction(e -> startGame(1, 100));

        // 2 Players button
        Button twoPlayersButton = new Button("2 PLAYERS");
        styleButton(twoPlayersButton);
        twoPlayersButton.setOnAction(e -> startGame(2, 100));

        // Test button
        Button testButton = new Button("TEST (20 ENEMIES)");
        styleButton(testButton);
        testButton.setStyle(
            "-fx-background-color: #555555;" +
            "-fx-text-fill: orange;" +
            "-fx-border-color: orange;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );
        testButton.setOnMouseEntered(e -> testButton.setStyle(
            "-fx-background-color: #777777;" +
            "-fx-text-fill: orange;" +
            "-fx-border-color: orange;" +
            "-fx-border-width: 3px;" +
            "-fx-cursor: hand;"
        ));
        testButton.setOnMouseExited(e -> testButton.setStyle(
            "-fx-background-color: #555555;" +
            "-fx-text-fill: orange;" +
            "-fx-border-color: orange;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
        testButton.setOnAction(e -> startGame(1, 20));

        // Explanation button
        Button explanationButton = new Button("EXPLANATION");
        styleButton(explanationButton);
        explanationButton.setOnAction(e -> showExplanation());

        // Instructions
        Label instructions = new Label("Defend your base from 100 enemy tanks!");
        instructions.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        instructions.setTextFill(Color.LIGHTGRAY);

        Label controls1 = new Label("Player 1: WASD + SPACE to shoot");
        controls1.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        controls1.setTextFill(Color.LIGHTGRAY);

        Label controls2 = new Label("Player 2: Arrow Keys + ENTER to shoot");
        controls2.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        controls2.setTextFill(Color.LIGHTGRAY);

        menuLayout.getChildren().addAll(
            title,
            subtitle,
            onePlayerButton,
            twoPlayersButton,
            testButton,
            explanationButton,
            instructions,
            controls1,
            controls2
        );

        scene = new Scene(menuLayout, windowWidth, windowHeight);
    }

    private void styleButton(Button button) {
        button.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        button.setPrefWidth(300);
        button.setPrefHeight(60);
        button.setStyle(
            "-fx-background-color: #333333;" +
            "-fx-text-fill: white;" +
            "-fx-border-color: yellow;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #555555;" +
            "-fx-text-fill: yellow;" +
            "-fx-border-color: yellow;" +
            "-fx-border-width: 3px;" +
            "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: #333333;" +
            "-fx-text-fill: white;" +
            "-fx-border-color: yellow;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
    }

    private void startGame(int playerCount, int totalEnemies) {
        javafx.scene.layout.Pane gameRoot = new javafx.scene.layout.Pane();
        Scene gameScene = new Scene(gameRoot, windowWidth, windowHeight);

        Game game = new Game(gameRoot, windowWidth, windowHeight, playerCount, totalEnemies, stage);
        game.start();

        stage.setScene(gameScene);
    }

    private void showExplanation() {
        ExplanationScene explanationScene = new ExplanationScene(stage, scene, windowWidth, windowHeight);
        stage.setScene(explanationScene.getScene());
    }

    public Scene getScene() {
        return scene;
    }
}
