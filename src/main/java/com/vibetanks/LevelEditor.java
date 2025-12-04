package com.vibetanks;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Level Editor for creating custom Vibe Tanks levels.
 * Allows placing tiles to design custom maps.
 */
public class LevelEditor {
    private static final int TILE_SIZE = 32;
    private static final int MAP_WIDTH = 26;
    private static final int MAP_HEIGHT = 26;

    private Scene scene;
    private Stage stage;
    private Scene menuScene;
    private Canvas canvas;
    private GraphicsContext gc;

    private GameMap.TileType[][] tiles;
    private GameMap.TileType selectedTile = GameMap.TileType.BRICK;
    private boolean isDrawing = false;
    private int lastDrawRow = -1;
    private int lastDrawCol = -1;

    // Toolbar buttons for tile selection
    private ToggleGroup tileGroup;

    public LevelEditor(Stage stage, Scene menuScene, int windowWidth, int windowHeight) {
        this.stage = stage;
        this.menuScene = menuScene;
        this.tiles = new GameMap.TileType[MAP_HEIGHT][MAP_WIDTH];

        createUI(windowWidth, windowHeight);
        initializeLevel();
    }

    private void createUI(int windowWidth, int windowHeight) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Top toolbar with tile selection
        HBox toolbar = createToolbar();
        root.setTop(toolbar);

        // Bottom panel with action buttons (create first to calculate available height)
        HBox bottomPanel = createBottomPanel();
        root.setBottom(bottomPanel);

        // Canvas for level editing - use ScrollPane to fit within available space
        canvas = new Canvas(MAP_WIDTH * TILE_SIZE, MAP_HEIGHT * TILE_SIZE);
        gc = canvas.getGraphicsContext2D();

        // Handle mouse events for drawing
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);

        // Put canvas in a ScrollPane so it can scroll if needed
        ScrollPane scrollPane = new ScrollPane();
        StackPane canvasContainer = new StackPane(canvas);
        canvasContainer.setStyle("-fx-background-color: black;");
        scrollPane.setContent(canvasContainer);
        scrollPane.setStyle("-fx-background: black; -fx-background-color: black;");
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        root.setCenter(scrollPane);

        scene = new Scene(root, windowWidth, windowHeight);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #2a2a2a;");

        Label titleLabel = new Label("LEVEL EDITOR");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.YELLOW);
        HBox.setMargin(titleLabel, new Insets(0, 20, 0, 0));

        tileGroup = new ToggleGroup();

        // Create tile selection buttons
        ToggleButton emptyBtn = createTileButton("EMPTY", GameMap.TileType.EMPTY, Color.BLACK);
        ToggleButton brickBtn = createTileButton("BRICK", GameMap.TileType.BRICK, Color.rgb(139, 69, 19));
        ToggleButton steelBtn = createTileButton("STEEL", GameMap.TileType.STEEL, Color.DARKGRAY);
        ToggleButton waterBtn = createTileButton("WATER", GameMap.TileType.WATER, Color.BLUE);
        ToggleButton treesBtn = createTileButton("TREES", GameMap.TileType.TREES, Color.DARKGREEN);
        ToggleButton iceBtn = createTileButton("ICE", GameMap.TileType.ICE, Color.rgb(200, 230, 255));

        // Select brick by default
        brickBtn.setSelected(true);

        toolbar.getChildren().addAll(
            titleLabel,
            emptyBtn, brickBtn, steelBtn, waterBtn, treesBtn, iceBtn
        );

        return toolbar;
    }

    private ToggleButton createTileButton(String name, GameMap.TileType type, Color color) {
        ToggleButton btn = new ToggleButton(name);
        btn.setToggleGroup(tileGroup);
        btn.setPrefSize(70, 40);

        String baseColor = toHex(color);
        String hoverColor = toHex(color.brighter());

        btn.setStyle(
            "-fx-background-color: " + baseColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-border-color: gray;" +
            "-fx-border-width: 2px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 10px;"
        );

        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                selectedTile = type;
                btn.setStyle(
                    "-fx-background-color: " + baseColor + ";" +
                    "-fx-text-fill: yellow;" +
                    "-fx-border-color: yellow;" +
                    "-fx-border-width: 3px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 10px;"
                );
            } else {
                btn.setStyle(
                    "-fx-background-color: " + baseColor + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-border-color: gray;" +
                    "-fx-border-width: 2px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 10px;"
                );
            }
        });

        return btn;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    private HBox createBottomPanel() {
        HBox panel = new HBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #2a2a2a;");

        Button newBtn = createActionButton("NEW", "#5a5a2a", "#ffff99");
        newBtn.setOnAction(e -> newLevel());

        Button loadBtn = createActionButton("LOAD", "#2a5a5a", "#99ffff");
        loadBtn.setOnAction(e -> loadLevel());

        Button saveBtn = createActionButton("SAVE", "#2a5a2a", "#99ff99");
        saveBtn.setOnAction(e -> saveLevel());

        Button testBtn = createActionButton("TEST", "#5a2a5a", "#ff99ff");
        testBtn.setOnAction(e -> testLevel());

        Button backBtn = createActionButton("BACK", "#5a2a2a", "#ff9999");
        backBtn.setOnAction(e -> stage.setScene(menuScene));

        // Info label
        Label infoLabel = new Label("Left-click: Draw | Right-click: Erase");
        infoLabel.setTextFill(Color.LIGHTGRAY);
        infoLabel.setFont(Font.font("Arial", 11));
        HBox.setMargin(infoLabel, new Insets(0, 0, 0, 30));

        panel.getChildren().addAll(newBtn, loadBtn, saveBtn, testBtn, backBtn, infoLabel);
        return panel;
    }

    private Button createActionButton(String text, String bgColor, String textColor) {
        Button btn = new Button(text);
        btn.setPrefSize(80, 35);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        btn.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: " + textColor + ";" +
            "-fx-border-color: " + textColor + ";" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: " + bgColor.replace("2a", "4a") + ";" +
            "-fx-text-fill: white;" +
            "-fx-border-color: white;" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: " + textColor + ";" +
            "-fx-border-color: " + textColor + ";" +
            "-fx-border-width: 2px;" +
            "-fx-cursor: hand;"
        ));

        return btn;
    }

    private void initializeLevel() {
        // Initialize with empty tiles
        for (int row = 0; row < MAP_HEIGHT; row++) {
            for (int col = 0; col < MAP_WIDTH; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }

        // Add border walls (steel)
        for (int i = 0; i < MAP_WIDTH; i++) {
            tiles[0][i] = GameMap.TileType.STEEL;
            tiles[MAP_HEIGHT - 1][i] = GameMap.TileType.STEEL;
        }
        for (int i = 0; i < MAP_HEIGHT; i++) {
            tiles[i][0] = GameMap.TileType.STEEL;
            tiles[i][MAP_WIDTH - 1] = GameMap.TileType.STEEL;
        }

        // Add base protection (brick)
        createBaseProtection();

        render();
    }

    private void createBaseProtection() {
        // Base is at row 24-25, col 12-13
        // Surround with bricks
        tiles[23][11] = GameMap.TileType.BRICK;
        tiles[23][12] = GameMap.TileType.BRICK;
        tiles[23][13] = GameMap.TileType.BRICK;
        tiles[23][14] = GameMap.TileType.BRICK;
        tiles[24][11] = GameMap.TileType.BRICK;
        tiles[24][14] = GameMap.TileType.BRICK;
        tiles[25][11] = GameMap.TileType.BRICK;
        tiles[25][12] = GameMap.TileType.BRICK;
        tiles[25][13] = GameMap.TileType.BRICK;
        tiles[25][14] = GameMap.TileType.BRICK;
    }

    private void handleMousePressed(MouseEvent e) {
        isDrawing = true;
        drawTile(e);
    }

    private void handleMouseDragged(MouseEvent e) {
        if (isDrawing) {
            drawTile(e);
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        isDrawing = false;
        lastDrawRow = -1;
        lastDrawCol = -1;
    }

    private void drawTile(MouseEvent e) {
        int col = (int) (e.getX() / TILE_SIZE);
        int row = (int) (e.getY() / TILE_SIZE);

        // Check bounds
        if (row < 0 || row >= MAP_HEIGHT || col < 0 || col >= MAP_WIDTH) {
            return;
        }

        // Don't redraw same tile to improve performance
        if (row == lastDrawRow && col == lastDrawCol) {
            return;
        }
        lastDrawRow = row;
        lastDrawCol = col;

        // Protect border (can't edit row 0, row 25, col 0, col 25)
        if (row == 0 || row == MAP_HEIGHT - 1 || col == 0 || col == MAP_WIDTH - 1) {
            return;
        }

        // Right-click erases (sets to empty)
        if (e.getButton() == MouseButton.SECONDARY) {
            tiles[row][col] = GameMap.TileType.EMPTY;
        } else {
            tiles[row][col] = selectedTile;
        }

        render();
    }

    private void render() {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw tiles
        for (int row = 0; row < MAP_HEIGHT; row++) {
            for (int col = 0; col < MAP_WIDTH; col++) {
                renderTile(row, col);
            }
        }

        // Draw base indicator
        drawBase();

        // Draw spawn point indicators
        drawSpawnPoints();

        // Draw grid lines (subtle)
        gc.setStroke(Color.rgb(40, 40, 40));
        gc.setLineWidth(1);
        for (int i = 0; i <= MAP_WIDTH; i++) {
            gc.strokeLine(i * TILE_SIZE, 0, i * TILE_SIZE, MAP_HEIGHT * TILE_SIZE);
        }
        for (int i = 0; i <= MAP_HEIGHT; i++) {
            gc.strokeLine(0, i * TILE_SIZE, MAP_WIDTH * TILE_SIZE, i * TILE_SIZE);
        }
    }

    private void renderTile(int row, int col) {
        GameMap.TileType tile = tiles[row][col];
        double x = col * TILE_SIZE;
        double y = row * TILE_SIZE;

        switch (tile) {
            case BRICK:
                gc.setFill(Color.rgb(139, 69, 19));
                gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                gc.setStroke(Color.rgb(100, 50, 10));
                gc.strokeRect(x, y, TILE_SIZE / 2, TILE_SIZE / 2);
                gc.strokeRect(x + TILE_SIZE / 2, y, TILE_SIZE / 2, TILE_SIZE / 2);
                gc.strokeRect(x, y + TILE_SIZE / 2, TILE_SIZE / 2, TILE_SIZE / 2);
                gc.strokeRect(x + TILE_SIZE / 2, y + TILE_SIZE / 2, TILE_SIZE / 2, TILE_SIZE / 2);
                break;
            case STEEL:
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                gc.setStroke(Color.LIGHTGRAY);
                gc.strokeRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                break;
            case WATER:
                gc.setFill(Color.BLUE);
                gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                gc.setFill(Color.LIGHTBLUE);
                gc.fillOval(x + 8, y + 8, 8, 8);
                gc.fillOval(x + 18, y + 18, 6, 6);
                break;
            case TREES:
                gc.setFill(Color.DARKGREEN);
                gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                gc.setFill(Color.GREEN);
                gc.fillOval(x + 4, y + 4, 10, 10);
                gc.fillOval(x + 18, y + 8, 10, 10);
                gc.fillOval(x + 8, y + 18, 10, 10);
                break;
            case ICE:
                gc.setFill(Color.rgb(200, 230, 255));
                gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                gc.setStroke(Color.rgb(150, 200, 255));
                gc.setLineWidth(2);
                gc.strokeLine(x, y, x + TILE_SIZE, y + TILE_SIZE);
                gc.strokeLine(x + TILE_SIZE, y, x, y + TILE_SIZE);
                gc.setLineWidth(1);
                break;
            default:
                // Empty - black background already
                break;
        }
    }

    private void drawBase() {
        // Draw base indicator at center bottom
        double x = 12 * TILE_SIZE;
        double y = 24 * TILE_SIZE;

        // Eagle/base icon
        gc.setFill(Color.GOLD);
        gc.fillRect(x, y, TILE_SIZE * 2, TILE_SIZE * 2);
        gc.setFill(Color.DARKGOLDENROD);
        gc.fillRect(x + 8, y + 8, TILE_SIZE * 2 - 16, TILE_SIZE * 2 - 16);

        // Label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.fillText("BASE", x + 12, y + 35);
    }

    private void drawSpawnPoints() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));

        // Enemy spawn points (top)
        gc.setFill(Color.rgb(255, 100, 100, 0.5));

        // Spawn 1
        gc.fillRect(1 * TILE_SIZE, 1 * TILE_SIZE, TILE_SIZE * 2, TILE_SIZE * 2);
        gc.setFill(Color.RED);
        gc.fillText("E1", 1 * TILE_SIZE + 10, 2 * TILE_SIZE);

        // Spawn 2
        gc.setFill(Color.rgb(255, 100, 100, 0.5));
        gc.fillRect(12 * TILE_SIZE, 1 * TILE_SIZE, TILE_SIZE * 2, TILE_SIZE * 2);
        gc.setFill(Color.RED);
        gc.fillText("E2", 12 * TILE_SIZE + 10, 2 * TILE_SIZE);

        // Spawn 3
        gc.setFill(Color.rgb(255, 100, 100, 0.5));
        gc.fillRect(23 * TILE_SIZE, 1 * TILE_SIZE, TILE_SIZE * 2, TILE_SIZE * 2);
        gc.setFill(Color.RED);
        gc.fillText("E3", 23 * TILE_SIZE + 10, 2 * TILE_SIZE);

        // Player spawn points (bottom)
        gc.setFill(Color.rgb(100, 255, 100, 0.5));

        // Player 1
        gc.fillRect(8 * TILE_SIZE, 24 * TILE_SIZE, TILE_SIZE * 2, TILE_SIZE * 2);
        gc.setFill(Color.LIME);
        gc.fillText("P1", 8 * TILE_SIZE + 10, 25 * TILE_SIZE);

        // Player 2
        gc.setFill(Color.rgb(100, 255, 100, 0.5));
        gc.fillRect(16 * TILE_SIZE, 24 * TILE_SIZE, TILE_SIZE * 2, TILE_SIZE * 2);
        gc.setFill(Color.LIME);
        gc.fillText("P2", 16 * TILE_SIZE + 10, 25 * TILE_SIZE);
    }

    private void newLevel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("New Level");
        confirm.setHeaderText("Create new level?");
        confirm.setContentText("This will clear the current level. Are you sure?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                initializeLevel();
            }
        });
    }

    private void saveLevel() {
        // Show save dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setTitle("Save Level");

        VBox dialogRoot = new VBox(15);
        dialogRoot.setPadding(new Insets(20));
        dialogRoot.setAlignment(Pos.CENTER);
        dialogRoot.setStyle("-fx-background-color: #2a2a2a;");

        Label titleLabel = new Label("Save Level");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.YELLOW);

        // Level number field
        HBox levelNumBox = new HBox(10);
        levelNumBox.setAlignment(Pos.CENTER);
        Label levelNumLabel = new Label("Level #:");
        levelNumLabel.setTextFill(Color.WHITE);
        Spinner<Integer> levelNumSpinner = new Spinner<>(1, 999, 1);
        levelNumSpinner.setEditable(true);
        levelNumSpinner.setPrefWidth(80);
        levelNumSpinner.setStyle("-fx-background-color: #444;");
        levelNumBox.getChildren().addAll(levelNumLabel, levelNumSpinner);

        // Show existing levels info
        List<Integer> existingLevels = LevelManager.getCustomLevelNumbers();
        Label existingLabel = new Label(existingLevels.isEmpty() ?
            "No custom levels yet" :
            "Existing: " + existingLevels.toString());
        existingLabel.setTextFill(Color.LIGHTGRAY);
        existingLabel.setFont(Font.font("Arial", 11));

        TextField nameField = new TextField();
        nameField.setPromptText("Level name (optional)...");
        nameField.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-prompt-text-fill: gray;");
        nameField.setMaxWidth(200);

        TextField authorField = new TextField();
        authorField.setPromptText("Author (optional)...");
        authorField.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-prompt-text-fill: gray;");
        authorField.setMaxWidth(200);

        // Pre-fill author with nickname if available
        String nickname = NicknameManager.getNickname();
        if (nickname != null && !nickname.isEmpty()) {
            authorField.setText(nickname);
        }

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Save");
        saveBtn.setStyle(
            "-fx-background-color: #2a5a2a; -fx-text-fill: lightgreen; " +
            "-fx-border-color: lightgreen; -fx-border-width: 2; -fx-cursor: hand;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #5a2a2a; -fx-text-fill: #ff9999; " +
            "-fx-border-color: #ff9999; -fx-border-width: 2; -fx-cursor: hand;"
        );

        buttonBox.getChildren().addAll(saveBtn, cancelBtn);
        dialogRoot.getChildren().addAll(titleLabel, levelNumBox, existingLabel, nameField, authorField, buttonBox);

        saveBtn.setOnAction(e -> {
            int levelNum = levelNumSpinner.getValue();

            // Check if level already exists
            if (LevelManager.hasCustomLevel(levelNum)) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Overwrite Level");
                confirm.setHeaderText("Level " + levelNum + " already exists!");
                confirm.setContentText("Do you want to overwrite it?");
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }

            // Create level data
            int[][] tileData = new int[MAP_HEIGHT][MAP_WIDTH];
            for (int row = 0; row < MAP_HEIGHT; row++) {
                for (int col = 0; col < MAP_WIDTH; col++) {
                    tileData[row][col] = tiles[row][col].ordinal();
                }
            }

            LevelData level = new LevelData(levelNum, nameField.getText().trim(), authorField.getText().trim(), MAP_WIDTH, MAP_HEIGHT, tileData);

            if (LevelManager.saveLevel(level)) {
                dialogStage.close();
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Level Saved");
                success.setHeaderText("Level " + levelNum + " saved!");
                success.setContentText("This level will be used when you play level " + levelNum + " in the game.");
                success.showAndWait();
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Save Failed");
                error.setHeaderText("Failed to save level");
                error.showAndWait();
            }
        });

        cancelBtn.setOnAction(e -> dialogStage.close());
        nameField.setOnAction(e -> saveBtn.fire());

        Scene dialogScene = new Scene(dialogRoot, 280, 200);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private void loadLevel() {
        List<Integer> levelNumbers = LevelManager.getCustomLevelNumbers();

        if (levelNumbers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Levels");
            alert.setHeaderText("No saved levels found");
            alert.setContentText("Create and save a level first!");
            alert.showAndWait();
            return;
        }

        // Show level selection dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);
        dialogStage.setTitle("Load Level");

        VBox dialogRoot = new VBox(15);
        dialogRoot.setPadding(new Insets(20));
        dialogRoot.setStyle("-fx-background-color: #2a2a2a;");

        Label titleLabel = new Label("Select Level to Edit");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.CYAN);

        VBox levelsList = new VBox(5);
        levelsList.setStyle("-fx-background-color: #333; -fx-padding: 10;");

        for (int levelNum : levelNumbers) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            LevelData levelData = LevelManager.loadLevelByNumber(levelNum);
            String displayName = "Level " + levelNum;
            if (levelData != null && levelData.getName() != null && !levelData.getName().isEmpty()) {
                displayName += " - " + levelData.getName();
            }

            Button levelBtn = new Button(displayName);
            levelBtn.setPrefWidth(180);
            levelBtn.setStyle(
                "-fx-background-color: #444; -fx-text-fill: lightcyan; " +
                "-fx-border-color: #555; -fx-cursor: hand;"
            );
            levelBtn.setOnMouseEntered(e -> levelBtn.setStyle(
                "-fx-background-color: #555; -fx-text-fill: white; " +
                "-fx-border-color: cyan; -fx-cursor: hand;"
            ));
            levelBtn.setOnMouseExited(e -> levelBtn.setStyle(
                "-fx-background-color: #444; -fx-text-fill: lightcyan; " +
                "-fx-border-color: #555; -fx-cursor: hand;"
            ));
            final int lvlNum = levelNum;
            levelBtn.setOnAction(e -> {
                dialogStage.close();
                doLoadLevelByNumber(lvlNum);
            });

            Button deleteBtn = new Button("X");
            deleteBtn.setStyle(
                "-fx-background-color: #5a2a2a; -fx-text-fill: #ff6666; " +
                "-fx-border-color: #ff6666; -fx-min-width: 25; -fx-min-height: 25; -fx-cursor: hand;"
            );
            final int delNum = levelNum;
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Level");
                confirm.setHeaderText("Delete Level " + delNum + "?");
                confirm.setContentText("This cannot be undone! Level " + delNum + " will use random generation.");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        LevelManager.deleteLevelByNumber(delNum);
                        dialogStage.close();
                        loadLevel(); // Refresh dialog
                    }
                });
            });

            row.getChildren().addAll(levelBtn, deleteBtn);
            levelsList.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(levelsList);
        scrollPane.setStyle("-fx-background: #333; -fx-background-color: #333;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #5a2a2a; -fx-text-fill: #ff9999; " +
            "-fx-border-color: #ff9999; -fx-border-width: 2; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> dialogStage.close());

        dialogRoot.getChildren().addAll(titleLabel, scrollPane, cancelBtn);

        Scene dialogScene = new Scene(dialogRoot, 280, 320);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private void doLoadLevelByNumber(int levelNum) {
        LevelData level = LevelManager.loadLevelByNumber(levelNum);
        if (level == null) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Load Failed");
            error.setHeaderText("Failed to load level " + levelNum);
            error.showAndWait();
            return;
        }

        int[][] tileData = level.getTiles();
        for (int row = 0; row < Math.min(MAP_HEIGHT, tileData.length); row++) {
            for (int col = 0; col < Math.min(MAP_WIDTH, tileData[row].length); col++) {
                tiles[row][col] = GameMap.TileType.values()[tileData[row][col]];
            }
        }

        render();

        String header = "Level " + levelNum + " loaded";
        if (level.getName() != null && !level.getName().isEmpty()) {
            header += " - " + level.getName();
        }

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Level Loaded");
        info.setHeaderText(header);
        if (level.getAuthor() != null && !level.getAuthor().isEmpty()) {
            info.setContentText("Author: " + level.getAuthor());
        }
        info.showAndWait();
    }

    private void testLevel() {
        // Create level data and start game with it
        int[][] tileData = new int[MAP_HEIGHT][MAP_WIDTH];
        for (int row = 0; row < MAP_HEIGHT; row++) {
            for (int col = 0; col < MAP_WIDTH; col++) {
                tileData[row][col] = tiles[row][col].ordinal();
            }
        }

        // Level number 0 for test (won't save)
        LevelData testLevel = new LevelData(0, "Test Level", "", MAP_WIDTH, MAP_HEIGHT, tileData);

        // Start game with custom level
        javafx.scene.layout.Pane gameRoot = new javafx.scene.layout.Pane();
        Scene gameScene = new Scene(gameRoot, (int) canvas.getWidth(), (int) canvas.getHeight());

        Game game = new Game(gameRoot, (int) canvas.getWidth(), (int) canvas.getHeight(), 1, 25, stage, testLevel);
        game.start();

        stage.setScene(gameScene);
    }

    public Scene getScene() {
        return scene;
    }
}
