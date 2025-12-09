package com.vibetanks.ui;

import com.vibetanks.Game;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.GameSettings;
import com.vibetanks.core.NicknameManager;
import com.vibetanks.network.IPHistoryManager;
import com.vibetanks.network.NetworkManager;
import com.vibetanks.util.GameLogger;
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

public class MenuScene {
    private static final GameLogger LOG = GameLogger.getLogger(MenuScene.class);

    private Scene scene;
    private Stage stage;
    private int windowWidth;
    private int windowHeight;
    private SoundManager soundManager;

    // Static reference to ensure only one NetworkManager exists at a time
    private static NetworkManager currentNetworkManager = null;

    public MenuScene(Stage stage, int windowWidth, int windowHeight) {
        this.stage = stage;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.soundManager = new SoundManager();
        createMenu();
    }

    private void createMenu() {
        VBox menuLayout = new VBox(20);
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
        playButton.setOnAction(e -> startGame(1, GameSettings.getEnemyCount()));

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

        // Level Editor button
        Button editorButton = new Button("LEVEL EDITOR");
        styleButton(editorButton);
        editorButton.setStyle(
            "-fx-background-color: #5a2a5a;" +
            "-fx-text-fill: #ff99ff;" +
            "-fx-border-color: #ff99ff;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );
        editorButton.setOnMouseEntered(e -> editorButton.setStyle(
            "-fx-background-color: #7a3a7a;" +
            "-fx-text-fill: #ff99ff;" +
            "-fx-border-color: #ff99ff;" +
            "-fx-border-width: 3px;" +
            "-fx-cursor: hand;"
        ));
        editorButton.setOnMouseExited(e -> editorButton.setStyle(
            "-fx-background-color: #5a2a5a;" +
            "-fx-text-fill: #ff99ff;" +
            "-fx-border-color: #ff99ff;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
        editorButton.setOnAction(e -> showLevelEditor());

        // Options button
        Button optionsButton = new Button("OPTIONS");
        styleButton(optionsButton);
        optionsButton.setStyle(
            "-fx-background-color: #4a3a2a;" +
            "-fx-text-fill: #ffcc66;" +
            "-fx-border-color: #ffcc66;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );
        optionsButton.setOnMouseEntered(e -> optionsButton.setStyle(
            "-fx-background-color: #6a5a3a;" +
            "-fx-text-fill: #ffcc66;" +
            "-fx-border-color: #ffcc66;" +
            "-fx-border-width: 3px;" +
            "-fx-cursor: hand;"
        ));
        optionsButton.setOnMouseExited(e -> optionsButton.setStyle(
            "-fx-background-color: #4a3a2a;" +
            "-fx-text-fill: #ffcc66;" +
            "-fx-border-color: #ffcc66;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
        optionsButton.setOnAction(e -> showOptions());

        // Explanation button
        Button explanationButton = new Button("EXPLANATION");
        styleButton(explanationButton);
        explanationButton.setOnAction(e -> showExplanation());

        // Instructions
        Label instructions = new Label("Defend your base from enemy tanks!");
        instructions.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        instructions.setTextFill(Color.LIGHTGRAY);

        Label controls = new Label("Controls: Arrow Keys or WASD + SPACE to shoot");
        controls.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        controls.setTextFill(Color.LIGHTGRAY);

        Label multiplayerInfo = new Label("Online: Host or join a game with 2 players!");
        multiplayerInfo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        multiplayerInfo.setTextFill(Color.LIGHTBLUE);

        // Nickname button (small, at bottom)
        Button nicknameButton = new Button(getNicknameButtonText());
        nicknameButton.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        nicknameButton.setPrefWidth(200);
        nicknameButton.setPrefHeight(30);
        nicknameButton.setStyle(
            "-fx-background-color: #222222;" +
            "-fx-text-fill: #aaaaaa;" +
            "-fx-border-color: #555555;" +
            "-fx-border-width: 1px;" +
            "-fx-cursor: hand;"
        );
        nicknameButton.setOnMouseEntered(e -> nicknameButton.setStyle(
            "-fx-background-color: #333333;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-border-color: #888888;" +
            "-fx-border-width: 1px;" +
            "-fx-cursor: hand;"
        ));
        nicknameButton.setOnMouseExited(e -> nicknameButton.setStyle(
            "-fx-background-color: #222222;" +
            "-fx-text-fill: #aaaaaa;" +
            "-fx-border-color: #555555;" +
            "-fx-border-width: 1px;" +
            "-fx-cursor: hand;"
        ));
        nicknameButton.setOnAction(e -> showNicknameDialog(nicknameButton));

        // Credits label at the bottom
        Label creditsLabel = new Label("Designed by Oleg and Artiom");
        creditsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        creditsLabel.setTextFill(Color.GRAY);

        menuLayout.getChildren().addAll(
            title,
            subtitle,
            playButton,
            hostButton,
            joinButton,
            editorButton,
            optionsButton,
            explanationButton,
            instructions,
            controls,
            multiplayerInfo,
            nicknameButton,
            creditsLabel
        );

        scene = new Scene(menuLayout, windowWidth, windowHeight);
    }

    private void styleButton(Button button) {
        button.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        button.setPrefWidth(280);
        button.setPrefHeight(45);
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
        ExplanationScene explanationScene = new ExplanationScene(stage, scene, windowWidth, windowHeight, soundManager);
        explanationScene.startMusic();
        stage.setScene(explanationScene.getScene());
    }

    private void showLevelEditor() {
        LevelEditor levelEditor = new LevelEditor(stage, scene, windowWidth, windowHeight);
        stage.setScene(levelEditor.getScene());
    }

    private void showOptions() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setTitle("Game Options");

        VBox dialogRoot = new VBox(20);
        dialogRoot.setPadding(new Insets(25));
        dialogRoot.setAlignment(Pos.CENTER);
        dialogRoot.setStyle("-fx-background-color: #2a2a2a;");

        // Title
        Label titleLabel = new Label("GAME OPTIONS");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#ffcc66"));

        // Player Speed
        VBox playerSpeedBox = new VBox(5);
        playerSpeedBox.setAlignment(Pos.CENTER);
        Label playerSpeedLabel = new Label("Player Speed: " + String.format("%.0f%%", GameSettings.getPlayerSpeedMultiplier() * 100));
        playerSpeedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playerSpeedLabel.setTextFill(Color.LIGHTGREEN);

        Slider playerSpeedSlider = new Slider(0.5, 2.0, GameSettings.getPlayerSpeedMultiplier());
        playerSpeedSlider.setShowTickLabels(true);
        playerSpeedSlider.setShowTickMarks(true);
        playerSpeedSlider.setMajorTickUnit(0.5);
        playerSpeedSlider.setMinorTickCount(4);
        playerSpeedSlider.setBlockIncrement(0.1);
        playerSpeedSlider.setPrefWidth(250);
        playerSpeedSlider.setStyle("-fx-control-inner-background: #444;");
        playerSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            playerSpeedLabel.setText("Player Speed: " + String.format("%.0f%%", newVal.doubleValue() * 100));
        });
        playerSpeedBox.getChildren().addAll(playerSpeedLabel, playerSpeedSlider);

        // Enemy Speed
        VBox enemySpeedBox = new VBox(5);
        enemySpeedBox.setAlignment(Pos.CENTER);
        Label enemySpeedLabel = new Label("Enemy Speed: " + String.format("%.0f%%", GameSettings.getEnemySpeedMultiplier() * 100));
        enemySpeedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        enemySpeedLabel.setTextFill(Color.LIGHTCORAL);

        Slider enemySpeedSlider = new Slider(0.5, 2.0, GameSettings.getEnemySpeedMultiplier());
        enemySpeedSlider.setShowTickLabels(true);
        enemySpeedSlider.setShowTickMarks(true);
        enemySpeedSlider.setMajorTickUnit(0.5);
        enemySpeedSlider.setMinorTickCount(4);
        enemySpeedSlider.setBlockIncrement(0.1);
        enemySpeedSlider.setPrefWidth(250);
        enemySpeedSlider.setStyle("-fx-control-inner-background: #444;");
        enemySpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            enemySpeedLabel.setText("Enemy Speed: " + String.format("%.0f%%", newVal.doubleValue() * 100));
        });
        enemySpeedBox.getChildren().addAll(enemySpeedLabel, enemySpeedSlider);

        // Player Shoot Speed
        VBox playerShootBox = new VBox(5);
        playerShootBox.setAlignment(Pos.CENTER);
        Label playerShootLabel = new Label("Player Shoot Speed: " + String.format("%.0f%%", GameSettings.getPlayerShootSpeedMultiplier() * 100));
        playerShootLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playerShootLabel.setTextFill(Color.CYAN);

        Slider playerShootSlider = new Slider(0.5, 3.0, GameSettings.getPlayerShootSpeedMultiplier());
        playerShootSlider.setShowTickLabels(true);
        playerShootSlider.setShowTickMarks(true);
        playerShootSlider.setMajorTickUnit(0.5);
        playerShootSlider.setMinorTickCount(4);
        playerShootSlider.setBlockIncrement(0.1);
        playerShootSlider.setPrefWidth(250);
        playerShootSlider.setStyle("-fx-control-inner-background: #444;");
        playerShootSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            playerShootLabel.setText("Player Shoot Speed: " + String.format("%.0f%%", newVal.doubleValue() * 100));
        });
        playerShootBox.getChildren().addAll(playerShootLabel, playerShootSlider);

        // Enemy Shoot Speed
        VBox enemyShootBox = new VBox(5);
        enemyShootBox.setAlignment(Pos.CENTER);
        Label enemyShootLabel = new Label("Enemy Shoot Speed: " + String.format("%.0f%%", GameSettings.getEnemyShootSpeedMultiplier() * 100));
        enemyShootLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        enemyShootLabel.setTextFill(Color.ORANGE);

        Slider enemyShootSlider = new Slider(0.5, 3.0, GameSettings.getEnemyShootSpeedMultiplier());
        enemyShootSlider.setShowTickLabels(true);
        enemyShootSlider.setShowTickMarks(true);
        enemyShootSlider.setMajorTickUnit(0.5);
        enemyShootSlider.setMinorTickCount(4);
        enemyShootSlider.setBlockIncrement(0.1);
        enemyShootSlider.setPrefWidth(250);
        enemyShootSlider.setStyle("-fx-control-inner-background: #444;");
        enemyShootSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            enemyShootLabel.setText("Enemy Shoot Speed: " + String.format("%.0f%%", newVal.doubleValue() * 100));
        });
        enemyShootBox.getChildren().addAll(enemyShootLabel, enemyShootSlider);

        // Enemy Count
        VBox enemyCountBox = new VBox(5);
        enemyCountBox.setAlignment(Pos.CENTER);
        Label enemyCountLabel = new Label("Enemy Count: " + GameSettings.getEnemyCount());
        enemyCountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        enemyCountLabel.setTextFill(Color.LIGHTYELLOW);

        Slider enemyCountSlider = new Slider(5, 100, GameSettings.getEnemyCount());
        enemyCountSlider.setShowTickLabels(true);
        enemyCountSlider.setShowTickMarks(true);
        enemyCountSlider.setMajorTickUnit(25);
        enemyCountSlider.setMinorTickCount(4);
        enemyCountSlider.setBlockIncrement(5);
        enemyCountSlider.setPrefWidth(250);
        enemyCountSlider.setStyle("-fx-control-inner-background: #444;");
        enemyCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            enemyCountLabel.setText("Enemy Count: " + newVal.intValue());
        });
        enemyCountBox.getChildren().addAll(enemyCountLabel, enemyCountSlider);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Save");
        saveButton.setStyle(
            "-fx-background-color: #2a5a2a; -fx-text-fill: lightgreen; " +
            "-fx-border-color: lightgreen; -fx-border-width: 2; -fx-cursor: hand;"
        );
        saveButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        saveButton.setPrefWidth(80);

        Button resetButton = new Button("Reset");
        resetButton.setStyle(
            "-fx-background-color: #5a5a2a; -fx-text-fill: #ffff99; " +
            "-fx-border-color: #ffff99; -fx-border-width: 2; -fx-cursor: hand;"
        );
        resetButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        resetButton.setPrefWidth(80);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: #5a2a2a; -fx-text-fill: #ff9999; " +
            "-fx-border-color: #ff9999; -fx-border-width: 2; -fx-cursor: hand;"
        );
        cancelButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        cancelButton.setPrefWidth(80);

        buttonBox.getChildren().addAll(saveButton, resetButton, cancelButton);

        // Handle save
        saveButton.setOnAction(e -> {
            GameSettings.setPlayerSpeedMultiplier(playerSpeedSlider.getValue());
            GameSettings.setEnemySpeedMultiplier(enemySpeedSlider.getValue());
            GameSettings.setPlayerShootSpeedMultiplier(playerShootSlider.getValue());
            GameSettings.setEnemyShootSpeedMultiplier(enemyShootSlider.getValue());
            GameSettings.setEnemyCount((int) enemyCountSlider.getValue());
            dialogStage.close();
        });

        // Handle reset
        resetButton.setOnAction(e -> {
            playerSpeedSlider.setValue(1.0);
            enemySpeedSlider.setValue(1.0);
            playerShootSlider.setValue(1.0);
            enemyShootSlider.setValue(1.0);
            enemyCountSlider.setValue(25);
            playerSpeedLabel.setText("Player Speed: 100%");
            enemySpeedLabel.setText("Enemy Speed: 100%");
            playerShootLabel.setText("Player Shoot Speed: 100%");
            enemyShootLabel.setText("Enemy Shoot Speed: 100%");
            enemyCountLabel.setText("Enemy Count: 25");
        });

        // Handle cancel
        cancelButton.setOnAction(e -> dialogStage.close());

        // Use ScrollPane for the content to handle smaller screens
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(playerSpeedBox, enemySpeedBox, playerShootBox, enemyShootBox, enemyCountBox);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #2a2a2a; -fx-background-color: #2a2a2a;");
        scrollPane.setPrefHeight(300);

        dialogRoot.getChildren().addAll(titleLabel, scrollPane, buttonBox);

        Scene dialogScene = new Scene(dialogRoot, 320, 450);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private void hostGame() {
        // Close any existing network manager first
        if (currentNetworkManager != null) {
            LOG.info("Closing previous network manager...");
            currentNetworkManager.close();

            // Wait longer for OS to release the port (Windows needs more time)
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                LOG.warn("Interrupted while waiting for port release");
            }

            currentNetworkManager = null;
            LOG.info("Previous network manager cleanup complete");
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
                LOG.info("Host dialog closed - cleaning up network...");
                network.close();
                LOG.info("Network cleanup complete");
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
                        LOG.info("Connection wait interrupted - stopping...");
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
        Game game = new Game(gameRoot, windowWidth, windowHeight, playerCount, GameSettings.getEnemyCount(), stage, network);
        game.start();

        stage.setScene(gameScene);
    }

    private String getNicknameButtonText() {
        String nickname = NicknameManager.getNickname();
        if (nickname != null) {
            return "Nickname: " + nickname;
        }
        return "Set Nickname";
    }

    private void showNicknameDialog(Button nicknameButton) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setTitle("Set Nickname");

        VBox dialogRoot = new VBox(15);
        dialogRoot.setPadding(new Insets(20));
        dialogRoot.setAlignment(Pos.CENTER);
        dialogRoot.setStyle("-fx-background-color: #2a2a2a;");

        // Title
        Label titleLabel = new Label("Enter Your Nickname");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.YELLOW);

        // Info label
        Label infoLabel = new Label("Max " + NicknameManager.getMaxLength() + " characters. Shown to all players.");
        infoLabel.setFont(Font.font("Arial", 11));
        infoLabel.setTextFill(Color.LIGHTGRAY);

        // Nickname input field
        TextField nicknameField = new TextField();
        String currentNickname = NicknameManager.getNickname();
        if (currentNickname != null) {
            nicknameField.setText(currentNickname);
        }
        nicknameField.setPromptText("Enter nickname...");
        nicknameField.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-prompt-text-fill: gray;");
        nicknameField.setFont(Font.font("Arial", 14));
        nicknameField.setMaxWidth(200);

        // Limit input length
        nicknameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > NicknameManager.getMaxLength()) {
                nicknameField.setText(oldVal);
            }
        });

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Save");
        saveButton.setStyle(
            "-fx-background-color: #2a5a2a; -fx-text-fill: lightgreen; " +
            "-fx-border-color: lightgreen; -fx-border-width: 2; -fx-cursor: hand;"
        );
        saveButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        saveButton.setPrefWidth(80);

        Button clearButton = new Button("Clear");
        clearButton.setStyle(
            "-fx-background-color: #5a5a2a; -fx-text-fill: #ffff99; " +
            "-fx-border-color: #ffff99; -fx-border-width: 2; -fx-cursor: hand;"
        );
        clearButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        clearButton.setPrefWidth(80);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: #5a2a2a; -fx-text-fill: #ff9999; " +
            "-fx-border-color: #ff9999; -fx-border-width: 2; -fx-cursor: hand;"
        );
        cancelButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        cancelButton.setPrefWidth(80);

        buttonBox.getChildren().addAll(saveButton, clearButton, cancelButton);

        dialogRoot.getChildren().addAll(titleLabel, infoLabel, nicknameField, buttonBox);

        // Handle save
        saveButton.setOnAction(e -> {
            String nickname = nicknameField.getText().trim();
            if (!nickname.isEmpty()) {
                NicknameManager.setNickname(nickname);
                nicknameButton.setText(getNicknameButtonText());
            }
            dialogStage.close();
        });

        // Handle clear
        clearButton.setOnAction(e -> {
            NicknameManager.clearNickname();
            nicknameButton.setText(getNicknameButtonText());
            dialogStage.close();
        });

        // Handle cancel
        cancelButton.setOnAction(e -> dialogStage.close());

        // Handle Enter key
        nicknameField.setOnAction(e -> saveButton.fire());

        Scene dialogScene = new Scene(dialogRoot, 280, 180);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    public Scene getScene() {
        return scene;
    }
}
