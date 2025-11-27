package com.battlecity;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.Optional;

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

        // Host Game button (Online Multiplayer)
        Button hostButton = new Button("HOST GAME (ONLINE)");
        styleButton(hostButton);
        hostButton.setStyle(
            "-fx-background-color: #2a5a2a;" +
            "-fx-text-fill: lightgreen;" +
            "-fx-border-color: lightgreen;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );
        hostButton.setOnMouseEntered(e -> hostButton.setStyle(
            "-fx-background-color: #3a7a3a;" +
            "-fx-text-fill: lightgreen;" +
            "-fx-border-color: lightgreen;" +
            "-fx-border-width: 3px;" +
            "-fx-cursor: hand;"
        ));
        hostButton.setOnMouseExited(e -> hostButton.setStyle(
            "-fx-background-color: #2a5a2a;" +
            "-fx-text-fill: lightgreen;" +
            "-fx-border-color: lightgreen;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
        hostButton.setOnAction(e -> hostGame());

        // Join Game button (Online Multiplayer)
        Button joinButton = new Button("JOIN GAME (ONLINE)");
        styleButton(joinButton);
        joinButton.setStyle(
            "-fx-background-color: #2a2a5a;" +
            "-fx-text-fill: lightblue;" +
            "-fx-border-color: lightblue;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );
        joinButton.setOnMouseEntered(e -> joinButton.setStyle(
            "-fx-background-color: #3a3a7a;" +
            "-fx-text-fill: lightblue;" +
            "-fx-border-color: lightblue;" +
            "-fx-border-width: 3px;" +
            "-fx-cursor: hand;"
        ));
        joinButton.setOnMouseExited(e -> joinButton.setStyle(
            "-fx-background-color: #2a2a5a;" +
            "-fx-text-fill: lightblue;" +
            "-fx-border-color: lightblue;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
        joinButton.setOnAction(e -> joinGame());

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

        Label controls = new Label("Controls: Arrow Keys + SPACE to shoot");
        controls.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        controls.setTextFill(Color.LIGHTGRAY);

        Label multiplayerInfo = new Label("Online: Host game or join up to 4 players!");
        multiplayerInfo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        multiplayerInfo.setTextFill(Color.LIGHTBLUE);

        menuLayout.getChildren().addAll(
            title,
            subtitle,
            onePlayerButton,
            hostButton,
            joinButton,
            testButton,
            explanationButton,
            instructions,
            controls,
            multiplayerInfo
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

    private void hostGame() {
        NetworkManager network = new NetworkManager();

        // Show IP address dialog
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Hosting Game");
        alert.setHeaderText("Waiting for Player 2...");
        alert.setContentText("Your IP: " + network.getLocalIP() + "\nPort: 25565\n\nWaiting for connection...");

        // Start hosting
        if (network.startHost()) {
            alert.show();

            // Wait for connection in background
            new Thread(() -> {
                while (!network.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                // Connection established, start game
                javafx.application.Platform.runLater(() -> {
                    alert.close();
                    startNetworkGame(network, true);
                });
            }).start();
        } else {
            alert.setAlertType(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setHeaderText("Failed to Host");
            alert.setContentText("Could not start server. Port may be in use.");
            alert.show();
        }
    }

    private void joinGame() {
        TextInputDialog dialog = new TextInputDialog("192.168.1.1");
        dialog.setTitle("Join Game");
        dialog.setHeaderText("Enter Host IP Address");
        dialog.setContentText("IP:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ip -> {
            NetworkManager network = new NetworkManager();

            if (network.joinHost(ip)) {
                startNetworkGame(network, false);
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Connection Failed");
                alert.setHeaderText("Could not connect to host");
                alert.setContentText("Make sure the IP address is correct and the host is ready.");
                alert.showAndWait();
            }
        });
    }

    private void startNetworkGame(NetworkManager network, boolean isHost) {
        javafx.scene.layout.Pane gameRoot = new javafx.scene.layout.Pane();
        Scene gameScene = new Scene(gameRoot, windowWidth, windowHeight);

        // Network game supports up to 4 players
        // Host will have all connected players, clients will have 1 (themselves)
        int playerCount = isHost ? 4 : 1;
        Game game = new Game(gameRoot, windowWidth, windowHeight, playerCount, 100, stage, network);
        game.start();

        stage.setScene(gameScene);
    }

    public Scene getScene() {
        return scene;
    }
}
