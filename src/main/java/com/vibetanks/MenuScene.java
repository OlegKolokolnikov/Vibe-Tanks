package com.vibetanks;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;
import java.util.Optional;

public class MenuScene {
    private Scene scene;
    private Stage stage;
    private int windowWidth;
    private int windowHeight;

    // Static reference to ensure only one NetworkManager exists at a time
    private static NetworkManager currentNetworkManager = null;

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
        Label title = new Label("VIBE TANKS");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        title.setTextFill(Color.YELLOW);

        // Subtitle
        Label subtitle = new Label("Tank Battle Game");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        subtitle.setTextFill(Color.WHITE);

        // Play button
        Button playButton = new Button("PLAY");
        styleButton(playButton);
        playButton.setOnAction(e -> startGame(1, 100));

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
            playButton,
            hostButton,
            joinButton,
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
        // Close any existing network manager first
        if (currentNetworkManager != null) {
            System.out.println("Closing previous network manager...");
            currentNetworkManager.close();

            // Wait longer for OS to release the port (Windows needs more time)
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                System.err.println("Interrupted while waiting for port release");
            }

            currentNetworkManager = null;
            System.out.println("Previous network manager cleanup complete");
        }

        currentNetworkManager = new NetworkManager();
        NetworkManager network = currentNetworkManager;

        // Show IP address dialog
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Hosting Game");
        alert.setHeaderText("Waiting for players...");
        alert.setContentText("Getting IP addresses...");

        // Close network when dialog is closed (user cancels)
        alert.setOnHidden(e -> {
            if (!network.isConnected()) {
                // Only cleanup if not connected (if connected, game will handle cleanup)
                System.out.println("Host dialog closed - cleaning up network...");
                network.close();
                System.out.println("Network cleanup complete");
            }
        });

        // Start hosting
        if (network.startHost()) {
            alert.show();

            // Get public IP in background and update dialog
            new Thread(() -> {
                String localIP = network.getLocalIP();
                String publicIP = network.getPublicIP();
                javafx.application.Platform.runLater(() -> {
                    alert.setContentText(
                        "Local IP (LAN): " + localIP + "\n" +
                        "Public IP (Internet): " + publicIP + "\n" +
                        "Port: 25565\n\n" +
                        "Share PUBLIC IP with friends in other cities!\n" +
                        "Use LOCAL IP for same network.\n\n" +
                        "Waiting for connection..."
                    );
                });
            }).start();

            // Wait for connection in background
            Thread connectionThread = new Thread(() -> {
                while (!network.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Connection wait interrupted - stopping...");
                        break;
                    }
                }

                // Connection established, start game
                if (network.isConnected()) {
                    javafx.application.Platform.runLater(() -> {
                        alert.close();
                        startNetworkGame(network, true);
                    });
                }
            });
            connectionThread.setDaemon(true);
            connectionThread.start();
        } else {
            alert.setAlertType(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setHeaderText("Failed to Host");
            alert.setContentText("Could not start server. Port may be in use.");
            alert.show();
        }
    }

    private void joinGame() {
        // Create custom dialog for IP selection
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setTitle("Join Game");

        VBox dialogRoot = new VBox(15);
        dialogRoot.setPadding(new Insets(20));
        dialogRoot.setStyle("-fx-background-color: #2a2a2a;");

        // Title
        Label titleLabel = new Label("Enter Host IP Address");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.LIGHTBLUE);

        // IP input field
        TextField ipField = new TextField();
        ipField.setPromptText("Enter IP address...");
        ipField.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-prompt-text-fill: gray;");
        ipField.setFont(Font.font("Arial", 14));

        // Saved IPs section
        Label savedLabel = new Label("Recent IPs:");
        savedLabel.setTextFill(Color.LIGHTGRAY);
        savedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        VBox savedIPsBox = new VBox(5);
        savedIPsBox.setStyle("-fx-background-color: #333; -fx-padding: 10; -fx-background-radius: 5;");

        // Populate saved IPs
        Runnable refreshSavedIPs = () -> {
            savedIPsBox.getChildren().clear();
            List<String> savedIPs = IPHistoryManager.getSavedIPs();

            if (savedIPs.isEmpty()) {
                Label emptyLabel = new Label("No saved IPs");
                emptyLabel.setTextFill(Color.GRAY);
                emptyLabel.setFont(Font.font("Arial", 12));
                savedIPsBox.getChildren().add(emptyLabel);
            } else {
                for (String ip : savedIPs) {
                    HBox ipRow = new HBox(10);
                    ipRow.setAlignment(Pos.CENTER_LEFT);

                    // IP button (click to select)
                    Button ipButton = new Button(ip);
                    ipButton.setStyle(
                        "-fx-background-color: #444; -fx-text-fill: lightblue; " +
                        "-fx-border-color: #555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;"
                    );
                    ipButton.setPrefWidth(200);
                    ipButton.setOnMouseEntered(e -> ipButton.setStyle(
                        "-fx-background-color: #555; -fx-text-fill: white; " +
                        "-fx-border-color: lightblue; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;"
                    ));
                    ipButton.setOnMouseExited(e -> ipButton.setStyle(
                        "-fx-background-color: #444; -fx-text-fill: lightblue; " +
                        "-fx-border-color: #555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;"
                    ));
                    ipButton.setOnAction(e -> ipField.setText(ip));
                    HBox.setHgrow(ipButton, Priority.ALWAYS);

                    // Delete button
                    Button deleteButton = new Button("X");
                    deleteButton.setStyle(
                        "-fx-background-color: #5a2a2a; -fx-text-fill: #ff6666; " +
                        "-fx-border-color: #ff6666; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;" +
                        "-fx-min-width: 25; -fx-min-height: 25; -fx-max-width: 25; -fx-max-height: 25;"
                    );
                    deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                        "-fx-background-color: #7a3a3a; -fx-text-fill: white; " +
                        "-fx-border-color: #ff6666; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;" +
                        "-fx-min-width: 25; -fx-min-height: 25; -fx-max-width: 25; -fx-max-height: 25;"
                    ));
                    deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                        "-fx-background-color: #5a2a2a; -fx-text-fill: #ff6666; " +
                        "-fx-border-color: #ff6666; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;" +
                        "-fx-min-width: 25; -fx-min-height: 25; -fx-max-width: 25; -fx-max-height: 25;"
                    ));
                    final String ipToDelete = ip;
                    final Runnable[] refreshRef = new Runnable[1];
                    deleteButton.setOnAction(e -> {
                        IPHistoryManager.removeIP(ipToDelete);
                        // Refresh the list
                        savedIPsBox.getChildren().clear();
                        List<String> updatedIPs = IPHistoryManager.getSavedIPs();
                        if (updatedIPs.isEmpty()) {
                            Label emptyLbl = new Label("No saved IPs");
                            emptyLbl.setTextFill(Color.GRAY);
                            emptyLbl.setFont(Font.font("Arial", 12));
                            savedIPsBox.getChildren().add(emptyLbl);
                        } else {
                            // Need to rebuild the UI - trigger refresh by re-running joinGame logic
                            dialogStage.close();
                            joinGame();
                        }
                    });

                    ipRow.getChildren().addAll(ipButton, deleteButton);
                    savedIPsBox.getChildren().add(ipRow);
                }
            }
        };
        refreshSavedIPs.run();

        // Scroll pane for saved IPs (in case there are many)
        ScrollPane scrollPane = new ScrollPane(savedIPsBox);
        scrollPane.setStyle("-fx-background: #333; -fx-background-color: #333;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(150);
        scrollPane.setMaxHeight(150);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button connectButton = new Button("Connect");
        connectButton.setStyle(
            "-fx-background-color: #2a5a2a; -fx-text-fill: lightgreen; " +
            "-fx-border-color: lightgreen; -fx-border-width: 2; -fx-cursor: hand;"
        );
        connectButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        connectButton.setPrefWidth(100);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: #5a2a2a; -fx-text-fill: #ff9999; " +
            "-fx-border-color: #ff9999; -fx-border-width: 2; -fx-cursor: hand;"
        );
        cancelButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        cancelButton.setPrefWidth(100);

        buttonBox.getChildren().addAll(connectButton, cancelButton);

        // Add all to dialog
        dialogRoot.getChildren().addAll(titleLabel, ipField, savedLabel, scrollPane, buttonBox);

        // Handle connect
        connectButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No IP");
                alert.setHeaderText("Please enter an IP address");
                alert.showAndWait();
                return;
            }

            dialogStage.close();

            NetworkManager network = new NetworkManager();
            if (network.joinHost(ip)) {
                // Save successful IP
                IPHistoryManager.addIP(ip);
                startNetworkGame(network, false);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Failed");
                alert.setHeaderText("Could not connect to host");
                alert.setContentText("Make sure the IP address is correct and the host is ready.");
                alert.showAndWait();
            }
        });

        // Handle cancel
        cancelButton.setOnAction(e -> dialogStage.close());

        // Handle Enter key in text field
        ipField.setOnAction(e -> connectButton.fire());

        Scene dialogScene = new Scene(dialogRoot, 320, 350);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private void startNetworkGame(NetworkManager network, boolean isHost) {
        javafx.scene.layout.Pane gameRoot = new javafx.scene.layout.Pane();
        Scene gameScene = new Scene(gameRoot, windowWidth, windowHeight);

        // Network game supports up to 4 players
        // Both host and client need all 4 tanks so GameState can update them all
        int playerCount = 4;
        Game game = new Game(gameRoot, windowWidth, windowHeight, playerCount, 100, stage, network);
        game.start();

        stage.setScene(gameScene);
    }

    public Scene getScene() {
        return scene;
    }
}
