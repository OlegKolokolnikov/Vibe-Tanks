package com.vibetanks;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class GameMap {
    private static final int TILE_SIZE = 32;
    private static final int BURN_DURATION = 60; // frames (1 second at 60 FPS)

    private int width;
    private int height;
    private TileType[][] tiles;
    private Random random = new Random();
    private int levelNumber = 1;
    private long currentLevelSeed; // Seed used for current level (for restart)
    private LevelData customLevelData; // Custom level data (if using custom level)

    // Track burning trees: key = row*1000+col, value = frames remaining
    private Map<Integer, Integer> burningTiles = new HashMap<>();

    public enum TileType {
        EMPTY,
        BRICK,
        STEEL,
        WATER,
        TREES,
        ICE
    }

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TileType[height][width];
        generateLevelForNumber(1);
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public void nextLevel() {
        levelNumber++;
        burningTiles.clear();
        generateLevelForNumber(levelNumber);
    }

    public void resetToLevel1() {
        levelNumber = 1;
        burningTiles.clear();
        generateLevelForNumber(1);
    }

    public void regenerateCurrentLevel() {
        // Keep the same level number AND same seed to get identical map
        burningTiles.clear();
        if (customLevelData != null) {
            // Custom level - just reload from data
            importTiles(customLevelData.getTiles());
        } else {
            // Random level - use same seed
            random.setSeed(currentLevelSeed);
            generateRandomLevel();
        }
    }

    /**
     * Generate level for a specific level number.
     * Checks if a custom level exists, otherwise generates random.
     */
    private void generateLevelForNumber(int num) {
        this.levelNumber = num;
        this.customLevelData = null; // Reset custom level

        // Check if custom level exists for this level number
        if (LevelManager.hasCustomLevel(num)) {
            LevelData customLevel = LevelManager.loadLevelByNumber(num);
            if (customLevel != null) {
                System.out.println("Loading custom level " + num);
                this.customLevelData = customLevel;
                importTiles(customLevel.getTiles());
                return;
            }
        }

        // No custom level, generate random
        System.out.println("Generating random level " + num);
        currentLevelSeed = System.currentTimeMillis();
        random.setSeed(currentLevelSeed);
        generateRandomLevel();
    }

    public void setLevelNumber(int level) {
        this.levelNumber = level;
    }

    private void generateLevel1() {
        // Initialize with empty tiles
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                tiles[row][col] = TileType.EMPTY;
            }
        }

        // Create border walls
        for (int i = 0; i < width; i++) {
            tiles[0][i] = TileType.STEEL;
            tiles[height - 1][i] = TileType.STEEL;
        }
        for (int i = 0; i < height; i++) {
            tiles[i][0] = TileType.STEEL;
            tiles[i][width - 1] = TileType.STEEL;
        }

        // Create some brick structures
        for (int row = 5; row < 8; row++) {
            for (int col = 5; col < 10; col++) {
                tiles[row][col] = TileType.BRICK;
            }
        }

        for (int row = 5; row < 8; row++) {
            for (int col = 16; col < 21; col++) {
                tiles[row][col] = TileType.BRICK;
            }
        }

        for (int row = 12; row < 15; row++) {
            for (int col = 8; col < 18; col++) {
                tiles[row][col] = TileType.BRICK;
            }
        }

        // Create steel walls
        for (int col = 10; col < 16; col++) {
            tiles[10][col] = TileType.STEEL;
        }

        // Add water
        for (int row = 18; row < 20; row++) {
            for (int col = 5; col < 10; col++) {
                tiles[row][col] = TileType.WATER;
            }
        }

        for (int row = 18; row < 20; row++) {
            for (int col = 16; col < 21; col++) {
                tiles[row][col] = TileType.WATER;
            }
        }

        // Add trees/grass (tanks can pass through)
        for (int row = 8; row < 12; row++) {
            for (int col = 20; col < 24; col++) {
                tiles[row][col] = TileType.TREES;
            }
        }

        // Add ice patches (tanks move 2x faster and slide)
        for (int row = 15; row < 18; row++) {
            for (int col = 3; col < 8; col++) {
                tiles[row][col] = TileType.ICE;
            }
        }

        for (int row = 15; row < 18; row++) {
            for (int col = 18; col < 23; col++) {
                tiles[row][col] = TileType.ICE;
            }
        }

        // Surround base with bricks
        tiles[23][11] = TileType.BRICK;
        tiles[23][12] = TileType.BRICK;
        tiles[23][13] = TileType.BRICK;
        tiles[24][11] = TileType.BRICK;
        tiles[24][13] = TileType.BRICK;
        tiles[25][11] = TileType.BRICK;
        tiles[25][12] = TileType.BRICK;
        tiles[25][13] = TileType.BRICK;
    }

    public void generateRandomLevel() {
        // Initialize with empty tiles
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                tiles[row][col] = TileType.EMPTY;
            }
        }

        // Create border walls (steel)
        for (int i = 0; i < width; i++) {
            tiles[0][i] = TileType.STEEL;
            tiles[height - 1][i] = TileType.STEEL;
        }
        for (int i = 0; i < height; i++) {
            tiles[i][0] = TileType.STEEL;
            tiles[i][width - 1] = TileType.STEEL;
        }

        // Generate 2-4 main geometric structures
        int numMainStructures = 2 + random.nextInt(3);
        for (int i = 0; i < numMainStructures; i++) {
            generateGeometricShape();
        }

        // Add 1-2 symmetric patterns
        if (random.nextDouble() < 0.7) {
            generateSymmetricPattern();
        }

        // Add some corridors/walls
        int numCorridors = 1 + random.nextInt(3);
        for (int i = 0; i < numCorridors; i++) {
            generateCorridor();
        }

        // Add some water features (1-3)
        int numWater = 1 + random.nextInt(3);
        for (int w = 0; w < numWater; w++) {
            generateWaterFeature();
        }

        // Add some tree patches (1-3)
        int numTrees = 1 + random.nextInt(3);
        for (int t = 0; t < numTrees; t++) {
            generateTreePatch();
        }

        // Add some ice patches (1-2)
        int numIce = 1 + random.nextInt(2);
        for (int i = 0; i < numIce; i++) {
            generateIcePatch();
        }

        // Add random scattered blocks for variety
        int numScattered = 5 + random.nextInt(10);
        for (int i = 0; i < numScattered; i++) {
            generateScatteredBlocks();
        }

        // Ensure base is surrounded by bricks
        createBaseProtection();

        // Clear spawn areas to ensure tanks can move
        clearSpawnAreas();
    }

    // Generate various geometric shapes
    private void generateGeometricShape() {
        int shapeType = random.nextInt(8);
        int centerCol = 4 + random.nextInt(width - 8);
        int centerRow = 5 + random.nextInt(height - 14);
        TileType type = random.nextDouble() < 0.75 ? TileType.BRICK : TileType.STEEL;

        switch (shapeType) {
            case 0 -> generateHollowRectangle(centerRow, centerCol, type);
            case 1 -> generateCross(centerRow, centerCol, type);
            case 2 -> generateDiamond(centerRow, centerCol, type);
            case 3 -> generateLShape(centerRow, centerCol, type);
            case 4 -> generateTShape(centerRow, centerCol, type);
            case 5 -> generateUShape(centerRow, centerCol, type);
            case 6 -> generateZigzag(centerRow, centerCol, type);
            case 7 -> generateSpiral(centerRow, centerCol, type);
        }
    }

    // Hollow rectangle (room-like)
    private void generateHollowRectangle(int startRow, int startCol, TileType type) {
        int w = 4 + random.nextInt(4); // 4-7 wide
        int h = 3 + random.nextInt(3); // 3-5 tall

        // Top and bottom walls
        for (int col = startCol; col < startCol + w; col++) {
            placeTile(startRow, col, type);
            placeTile(startRow + h - 1, col, type);
        }
        // Left and right walls
        for (int row = startRow; row < startRow + h; row++) {
            placeTile(row, startCol, type);
            placeTile(row, startCol + w - 1, type);
        }
        // Add opening on random side
        int opening = random.nextInt(4);
        switch (opening) {
            case 0 -> placeTile(startRow, startCol + w / 2, TileType.EMPTY); // top
            case 1 -> placeTile(startRow + h - 1, startCol + w / 2, TileType.EMPTY); // bottom
            case 2 -> placeTile(startRow + h / 2, startCol, TileType.EMPTY); // left
            case 3 -> placeTile(startRow + h / 2, startCol + w - 1, TileType.EMPTY); // right
        }
    }

    // Plus/Cross shape
    private void generateCross(int centerRow, int centerCol, TileType type) {
        int armLength = 2 + random.nextInt(3); // 2-4
        // Vertical arm
        for (int i = -armLength; i <= armLength; i++) {
            placeTile(centerRow + i, centerCol, type);
        }
        // Horizontal arm
        for (int i = -armLength; i <= armLength; i++) {
            placeTile(centerRow, centerCol + i, type);
        }
    }

    // Diamond shape
    private void generateDiamond(int centerRow, int centerCol, TileType type) {
        int size = 2 + random.nextInt(2); // 2-3
        for (int i = 0; i <= size; i++) {
            placeTile(centerRow - i, centerCol - (size - i), type);
            placeTile(centerRow - i, centerCol + (size - i), type);
            placeTile(centerRow + i, centerCol - (size - i), type);
            placeTile(centerRow + i, centerCol + (size - i), type);
        }
    }

    // L-shape
    private void generateLShape(int startRow, int startCol, TileType type) {
        int vertLen = 3 + random.nextInt(3); // 3-5
        int horizLen = 3 + random.nextInt(3); // 3-5
        boolean flipped = random.nextBoolean();
        boolean rotated = random.nextBoolean();

        // Vertical part
        for (int i = 0; i < vertLen; i++) {
            int col = flipped ? startCol + horizLen - 1 : startCol;
            placeTile(startRow + i, col, type);
        }
        // Horizontal part
        int horizRow = rotated ? startRow : startRow + vertLen - 1;
        for (int i = 0; i < horizLen; i++) {
            placeTile(horizRow, startCol + i, type);
        }
    }

    // T-shape
    private void generateTShape(int startRow, int startCol, TileType type) {
        int topWidth = 4 + random.nextInt(3); // 4-6
        int stemHeight = 2 + random.nextInt(3); // 2-4

        // Top bar
        for (int i = 0; i < topWidth; i++) {
            placeTile(startRow, startCol + i, type);
        }
        // Stem
        int stemCol = startCol + topWidth / 2;
        for (int i = 1; i <= stemHeight; i++) {
            placeTile(startRow + i, stemCol, type);
        }
    }

    // U-shape
    private void generateUShape(int startRow, int startCol, TileType type) {
        int w = 3 + random.nextInt(3); // 3-5
        int h = 3 + random.nextInt(2); // 3-4

        // Left wall
        for (int i = 0; i < h; i++) {
            placeTile(startRow + i, startCol, type);
        }
        // Right wall
        for (int i = 0; i < h; i++) {
            placeTile(startRow + i, startCol + w - 1, type);
        }
        // Bottom
        for (int i = 0; i < w; i++) {
            placeTile(startRow + h - 1, startCol + i, type);
        }
    }

    // Zigzag pattern
    private void generateZigzag(int startRow, int startCol, TileType type) {
        int segments = 2 + random.nextInt(2); // 2-3 zigzags
        int segLen = 2 + random.nextInt(2); // 2-3 length each
        boolean goingRight = random.nextBoolean();

        int row = startRow;
        int col = startCol;
        for (int s = 0; s < segments; s++) {
            // Horizontal segment
            for (int i = 0; i < segLen; i++) {
                placeTile(row, col + (goingRight ? i : -i), type);
            }
            col += goingRight ? segLen - 1 : -(segLen - 1);
            // Vertical segment
            for (int i = 1; i < segLen; i++) {
                placeTile(row + i, col, type);
            }
            row += segLen - 1;
            goingRight = !goingRight;
        }
    }

    // Small spiral
    private void generateSpiral(int centerRow, int centerCol, TileType type) {
        int[][] spiral = {
            {0, 0}, {0, 1}, {0, 2}, {0, 3},
            {1, 3}, {2, 3}, {2, 2}, {2, 1},
            {2, 0}, {1, 0}
        };
        for (int[] offset : spiral) {
            placeTile(centerRow + offset[0], centerCol + offset[1], type);
        }
    }

    // Generate symmetric pattern (mirrored left-right)
    private void generateSymmetricPattern() {
        int patternType = random.nextInt(3);
        int centerCol = width / 2;
        TileType type = random.nextDouble() < 0.7 ? TileType.BRICK : TileType.STEEL;

        switch (patternType) {
            case 0 -> generateSymmetricWalls(centerCol, type);
            case 1 -> generateSymmetricPillars(centerCol, type);
            case 2 -> generateSymmetricMaze(centerCol, type);
        }
    }

    private void generateSymmetricWalls(int centerCol, TileType type) {
        int wallRow = 8 + random.nextInt(6);
        int wallLength = 3 + random.nextInt(4);
        int offset = 3 + random.nextInt(4);

        // Left wall
        for (int i = 0; i < wallLength; i++) {
            placeTile(wallRow + i, centerCol - offset, type);
        }
        // Right wall (mirrored)
        for (int i = 0; i < wallLength; i++) {
            placeTile(wallRow + i, centerCol + offset - 1, type);
        }
    }

    private void generateSymmetricPillars(int centerCol, TileType type) {
        int numPillars = 2 + random.nextInt(2);
        int spacing = 4 + random.nextInt(3);

        for (int p = 0; p < numPillars; p++) {
            int row = 6 + p * spacing;
            int offset = 4 + random.nextInt(4);

            // Pillar (2x2)
            placeTile(row, centerCol - offset, type);
            placeTile(row + 1, centerCol - offset, type);
            placeTile(row, centerCol - offset + 1, type);
            placeTile(row + 1, centerCol - offset + 1, type);

            // Mirrored pillar
            placeTile(row, centerCol + offset - 2, type);
            placeTile(row + 1, centerCol + offset - 2, type);
            placeTile(row, centerCol + offset - 1, type);
            placeTile(row + 1, centerCol + offset - 1, type);
        }
    }

    private void generateSymmetricMaze(int centerCol, TileType type) {
        // Create small symmetric maze segments
        int[][] pattern = {
            {6, -6}, {6, -5}, {7, -5},
            {10, -8}, {10, -7}, {11, -7}, {11, -6},
            {14, -5}, {14, -4}, {14, -3}, {15, -3}
        };

        for (int[] pos : pattern) {
            placeTile(pos[0], centerCol + pos[1], type);
            placeTile(pos[0], centerCol - pos[1] - 1, type); // Mirror
        }
    }

    // Generate corridors/long walls
    private void generateCorridor() {
        boolean horizontal = random.nextBoolean();
        TileType type = random.nextDouble() < 0.6 ? TileType.BRICK : TileType.STEEL;

        if (horizontal) {
            int row = 5 + random.nextInt(height - 12);
            int startCol = 2 + random.nextInt(5);
            int length = 4 + random.nextInt(8);
            for (int i = 0; i < length; i++) {
                placeTile(row, startCol + i, type);
            }
            // Add gap
            if (length > 4) {
                placeTile(row, startCol + length / 2, TileType.EMPTY);
            }
        } else {
            int col = 3 + random.nextInt(width - 6);
            int startRow = 4 + random.nextInt(5);
            int length = 3 + random.nextInt(6);
            for (int i = 0; i < length; i++) {
                placeTile(startRow + i, col, type);
            }
            // Add gap
            if (length > 3) {
                placeTile(startRow + length / 2, col, TileType.EMPTY);
            }
        }
    }

    // Generate water features (pools, rivers)
    private void generateWaterFeature() {
        int featureType = random.nextInt(3);
        switch (featureType) {
            case 0 -> generateWaterPool();
            case 1 -> generateWaterRiver();
            case 2 -> generateWaterLake();
        }
    }

    private void generateWaterPool() {
        int poolWidth = 2 + random.nextInt(3);
        int poolHeight = 2 + random.nextInt(2);
        int startCol = 2 + random.nextInt(width - 4 - poolWidth);
        int startRow = 5 + random.nextInt(height - 12 - poolHeight);

        for (int row = startRow; row < startRow + poolHeight; row++) {
            for (int col = startCol; col < startCol + poolWidth; col++) {
                if (isValidForObstacle(row, col) && tiles[row][col] == TileType.EMPTY) {
                    tiles[row][col] = TileType.WATER;
                }
            }
        }
    }

    private void generateWaterRiver() {
        boolean horizontal = random.nextBoolean();
        if (horizontal) {
            int row = 8 + random.nextInt(height - 16);
            int startCol = 2 + random.nextInt(3);
            int length = 6 + random.nextInt(8);
            for (int i = 0; i < length; i++) {
                if (isValidForObstacle(row, startCol + i) && tiles[row][startCol + i] == TileType.EMPTY) {
                    tiles[row][startCol + i] = TileType.WATER;
                }
                // Add some width variation
                if (random.nextDouble() < 0.4 && isValidForObstacle(row + 1, startCol + i)) {
                    tiles[row + 1][startCol + i] = TileType.WATER;
                }
            }
        } else {
            int col = 4 + random.nextInt(width - 8);
            int startRow = 5 + random.nextInt(4);
            int length = 5 + random.nextInt(6);
            for (int i = 0; i < length; i++) {
                if (isValidForObstacle(startRow + i, col) && tiles[startRow + i][col] == TileType.EMPTY) {
                    tiles[startRow + i][col] = TileType.WATER;
                }
            }
        }
    }

    private void generateWaterLake() {
        int centerRow = 8 + random.nextInt(height - 16);
        int centerCol = 5 + random.nextInt(width - 10);
        int radius = 2 + random.nextInt(2);

        // Irregular lake shape
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                if (dr * dr + dc * dc <= radius * radius + random.nextInt(2)) {
                    int r = centerRow + dr;
                    int c = centerCol + dc;
                    if (isValidForObstacle(r, c) && tiles[r][c] == TileType.EMPTY) {
                        tiles[r][c] = TileType.WATER;
                    }
                }
            }
        }
    }

    private void generateTreePatch() {
        int shapeType = random.nextInt(3);
        switch (shapeType) {
            case 0 -> generateTreeCluster();
            case 1 -> generateTreeLine();
            case 2 -> generateTreeForest();
        }
    }

    private void generateTreeCluster() {
        int centerRow = 6 + random.nextInt(height - 14);
        int centerCol = 4 + random.nextInt(width - 8);
        int size = 2 + random.nextInt(2);

        for (int dr = -size; dr <= size; dr++) {
            for (int dc = -size; dc <= size; dc++) {
                if (random.nextDouble() < 0.7) {
                    int r = centerRow + dr;
                    int c = centerCol + dc;
                    if (isValidForObstacle(r, c) && tiles[r][c] == TileType.EMPTY) {
                        tiles[r][c] = TileType.TREES;
                    }
                }
            }
        }
    }

    private void generateTreeLine() {
        boolean horizontal = random.nextBoolean();
        int length = 3 + random.nextInt(5);

        if (horizontal) {
            int row = 5 + random.nextInt(height - 12);
            int startCol = 2 + random.nextInt(width - 4 - length);
            for (int i = 0; i < length; i++) {
                if (isValidForObstacle(row, startCol + i) && tiles[row][startCol + i] == TileType.EMPTY) {
                    tiles[row][startCol + i] = TileType.TREES;
                }
            }
        } else {
            int col = 3 + random.nextInt(width - 6);
            int startRow = 5 + random.nextInt(height - 12 - length);
            for (int i = 0; i < length; i++) {
                if (isValidForObstacle(startRow + i, col) && tiles[startRow + i][col] == TileType.EMPTY) {
                    tiles[startRow + i][col] = TileType.TREES;
                }
            }
        }
    }

    private void generateTreeForest() {
        int startRow = 5 + random.nextInt(height - 14);
        int startCol = 3 + random.nextInt(width - 8);
        int w = 3 + random.nextInt(3);
        int h = 2 + random.nextInt(3);

        for (int dr = 0; dr < h; dr++) {
            for (int dc = 0; dc < w; dc++) {
                int r = startRow + dr;
                int c = startCol + dc;
                if (isValidForObstacle(r, c) && tiles[r][c] == TileType.EMPTY) {
                    tiles[r][c] = TileType.TREES;
                }
            }
        }
    }

    private void generateIcePatch() {
        int shapeType = random.nextInt(2);
        switch (shapeType) {
            case 0 -> generateIceRink();
            case 1 -> generateIcePath();
        }
    }

    private void generateIceRink() {
        int startRow = 8 + random.nextInt(height - 16);
        int startCol = 4 + random.nextInt(width - 10);
        int w = 3 + random.nextInt(4);
        int h = 2 + random.nextInt(3);

        for (int dr = 0; dr < h; dr++) {
            for (int dc = 0; dc < w; dc++) {
                int r = startRow + dr;
                int c = startCol + dc;
                if (isValidForObstacle(r, c) && tiles[r][c] == TileType.EMPTY) {
                    tiles[r][c] = TileType.ICE;
                }
            }
        }
    }

    private void generateIcePath() {
        boolean horizontal = random.nextBoolean();
        int length = 4 + random.nextInt(6);

        if (horizontal) {
            int row = 10 + random.nextInt(height - 18);
            int startCol = 3 + random.nextInt(width - 6 - length);
            for (int i = 0; i < length; i++) {
                if (isValidForObstacle(row, startCol + i) && tiles[row][startCol + i] == TileType.EMPTY) {
                    tiles[row][startCol + i] = TileType.ICE;
                }
            }
        } else {
            int col = 4 + random.nextInt(width - 8);
            int startRow = 6 + random.nextInt(height - 14 - length);
            for (int i = 0; i < length; i++) {
                if (isValidForObstacle(startRow + i, col) && tiles[startRow + i][col] == TileType.EMPTY) {
                    tiles[startRow + i][col] = TileType.ICE;
                }
            }
        }
    }

    // Generate scattered random blocks
    private void generateScatteredBlocks() {
        int row = 3 + random.nextInt(height - 8);
        int col = 2 + random.nextInt(width - 4);
        TileType type = random.nextDouble() < 0.8 ? TileType.BRICK : TileType.STEEL;

        // Place 1-3 connected blocks
        int numBlocks = 1 + random.nextInt(3);
        for (int i = 0; i < numBlocks; i++) {
            if (isValidForObstacle(row, col) && tiles[row][col] == TileType.EMPTY) {
                tiles[row][col] = type;
            }
            // Move to adjacent cell
            if (random.nextBoolean()) {
                row += random.nextBoolean() ? 1 : -1;
            } else {
                col += random.nextBoolean() ? 1 : -1;
            }
        }
    }

    // Helper to place tile only if valid
    private void placeTile(int row, int col, TileType type) {
        if (isValidForObstacle(row, col)) {
            tiles[row][col] = type;
        }
    }

    private boolean isValidForObstacle(int row, int col) {
        // Don't place in border
        if (row <= 0 || row >= height - 1 || col <= 0 || col >= width - 1) {
            return false;
        }

        // Don't place in enemy spawn areas (top)
        if (row <= 2) {
            // Spawn points at cols 1, 12, 24
            if ((col >= 1 && col <= 3) || (col >= 11 && col <= 14) || (col >= 23 && col <= 25)) {
                return false;
            }
        }

        // Don't place in player spawn area (bottom)
        if (row >= 23) {
            // Player spawns around cols 8, 9, 15, 16
            if (col >= 7 && col <= 17) {
                return false;
            }
        }

        // Don't place too close to base (bottom center)
        if (row >= 22 && col >= 10 && col <= 15) {
            return false;
        }

        return true;
    }

    private void createBaseProtection() {
        // Base is at row 24, col 12-13 (2x2 area typically)
        // Surround with bricks
        tiles[23][11] = TileType.BRICK;
        tiles[23][12] = TileType.BRICK;
        tiles[23][13] = TileType.BRICK;
        tiles[23][14] = TileType.BRICK;
        tiles[24][11] = TileType.BRICK;
        tiles[24][14] = TileType.BRICK;
        tiles[25][11] = TileType.BRICK;
        tiles[25][12] = TileType.BRICK;
        tiles[25][13] = TileType.BRICK;
        tiles[25][14] = TileType.BRICK;

        // Add steel wall above base to protect from center spawn
        // Random width 1-5 blocks, centered above base
        int steelWidth = 1 + random.nextInt(5); // 1-5 blocks
        int startCol = 12 - steelWidth / 2; // Center around col 12-13
        int steelRow = 20 + random.nextInt(2); // Row 20 or 21
        for (int col = startCol; col < startCol + steelWidth && col < width - 1; col++) {
            if (col > 0) {
                tiles[steelRow][col] = TileType.STEEL;
            }
        }
    }

    private void clearSpawnAreas() {
        // Clear enemy spawn points (top, 2 tiles deep, 2 tiles wide each)
        // Spawn 1: col 1-2
        for (int row = 1; row <= 2; row++) {
            for (int col = 1; col <= 3; col++) {
                tiles[row][col] = TileType.EMPTY;
            }
        }
        // Spawn 2: col 12-13 (center)
        for (int row = 1; row <= 2; row++) {
            for (int col = 11; col <= 14; col++) {
                tiles[row][col] = TileType.EMPTY;
            }
        }
        // Spawn 3: col 24-25
        for (int row = 1; row <= 2; row++) {
            for (int col = 23; col <= 25; col++) {
                tiles[row][col] = TileType.EMPTY;
            }
        }

        // Clear player spawn points (bottom)
        // Player 1: col 8
        for (int row = 23; row <= 25; row++) {
            for (int col = 7; col <= 10; col++) {
                if (tiles[row][col] != TileType.BRICK || row < 23 || col < 11) {
                    tiles[row][col] = TileType.EMPTY;
                }
            }
        }
        // Player 2: col 16
        for (int row = 23; row <= 25; row++) {
            for (int col = 15; col <= 18; col++) {
                if (tiles[row][col] != TileType.BRICK || row < 23 || col > 14) {
                    tiles[row][col] = TileType.EMPTY;
                }
            }
        }

        // Ensure paths from spawns are clear (create corridors)
        // Vertical corridor from top spawns
        for (int row = 1; row <= 5; row++) {
            tiles[row][2] = TileType.EMPTY;
            tiles[row][12] = TileType.EMPTY;
            tiles[row][13] = TileType.EMPTY;
            tiles[row][24] = TileType.EMPTY;
        }

        // Vertical corridor from bottom spawns
        for (int row = 20; row <= 24; row++) {
            tiles[row][8] = TileType.EMPTY;
            tiles[row][9] = TileType.EMPTY;
            tiles[row][16] = TileType.EMPTY;
            tiles[row][17] = TileType.EMPTY;
        }
    }

    public boolean checkTankCollision(double x, double y, int tankSize) {
        return checkTankCollision(x, y, tankSize, false);
    }

    public boolean checkTankCollision(double x, double y, int tankSize, boolean canSwim) {
        int startCol = (int) x / TILE_SIZE;
        int endCol = (int) (x + tankSize - 1) / TILE_SIZE;
        int startRow = (int) y / TILE_SIZE;
        int endRow = (int) (y + tankSize - 1) / TILE_SIZE;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (row < 0 || row >= height || col < 0 || col >= width) {
                    return true; // collision with boundary
                }
                TileType tile = tiles[row][col];
                if (tile == TileType.BRICK || tile == TileType.STEEL) {
                    return true; // collision with solid tile
                }
                // Check water collision only if tank cannot swim
                if (tile == TileType.WATER && !canSwim) {
                    return true;
                }
            }
        }
        return false; // no collision
    }

    public boolean checkBulletCollision(Bullet bullet) {
        int col = (int) (bullet.getX() + bullet.getSize() / 2) / TILE_SIZE;
        int row = (int) (bullet.getY() + bullet.getSize() / 2) / TILE_SIZE;

        if (row < 0 || row >= height || col < 0 || col >= width) {
            return true; // out of bounds
        }

        TileType tile = tiles[row][col];

        if (tile == TileType.BRICK) {
            // Brick is destroyed by bullet
            tiles[row][col] = TileType.EMPTY;
            return true;
        } else if (tile == TileType.STEEL) {
            // Steel stops bullet but isn't destroyed (unless power bullet)
            if (bullet.getPower() >= 2) {
                tiles[row][col] = TileType.EMPTY;
            }
            return true;
        } else if (tile == TileType.TREES && bullet.canDestroyTrees()) {
            // Trees start burning with SAW power-up, then get destroyed
            int key = row * 1000 + col;
            if (!burningTiles.containsKey(key)) {
                burningTiles.put(key, BURN_DURATION);
            }
            return true;
        }

        return false; // no collision
    }

    public void render(GraphicsContext gc) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                TileType tile = tiles[row][col];
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
                        // Draw diagonal lines to represent ice texture
                        gc.strokeLine(x, y, x + TILE_SIZE, y + TILE_SIZE);
                        gc.strokeLine(x + TILE_SIZE, y, x, y + TILE_SIZE);
                        break;
                    default:
                        // Empty tile - already black background
                        break;
                }
            }
        }
    }

    // Render only terrain (no trees) - trees will be rendered on top of tanks
    public void renderWithoutTrees(GraphicsContext gc) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                TileType tile = tiles[row][col];
                if (tile == TileType.TREES) {
                    continue; // Skip trees - they'll be rendered later on top
                }

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
                    case ICE:
                        gc.setFill(Color.rgb(200, 230, 255));
                        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                        gc.setStroke(Color.rgb(150, 200, 255));
                        gc.setLineWidth(2);
                        gc.strokeLine(x, y, x + TILE_SIZE, y + TILE_SIZE);
                        gc.strokeLine(x + TILE_SIZE, y, x, y + TILE_SIZE);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // Render only trees - to be drawn on top of tanks
    public void renderTrees(GraphicsContext gc) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                TileType tile = tiles[row][col];
                if (tile == TileType.TREES) {
                    double x = col * TILE_SIZE;
                    double y = row * TILE_SIZE;

                    gc.setFill(Color.DARKGREEN);
                    gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    gc.setFill(Color.GREEN);
                    gc.fillOval(x + 4, y + 4, 10, 10);
                    gc.fillOval(x + 18, y + 8, 10, 10);
                    gc.fillOval(x + 8, y + 18, 10, 10);
                }
            }
        }
    }

    // Update burning tiles
    public void update() {
        Iterator<Map.Entry<Integer, Integer>> it = burningTiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            int framesLeft = entry.getValue() - 1;
            if (framesLeft <= 0) {
                // Burn complete - destroy the tree
                int key = entry.getKey();
                int row = key / 1000;
                int col = key % 1000;
                tiles[row][col] = TileType.EMPTY;
                it.remove();
            } else {
                entry.setValue(framesLeft);
            }
        }
    }

    // Render fire on burning tiles
    public void renderBurningTiles(GraphicsContext gc) {
        long time = System.currentTimeMillis();
        for (Map.Entry<Integer, Integer> entry : burningTiles.entrySet()) {
            int key = entry.getKey();
            int row = key / 1000;
            int col = key % 1000;
            double x = col * TILE_SIZE;
            double y = row * TILE_SIZE;

            // Draw tree underneath
            gc.setFill(Color.DARKGREEN);
            gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
            gc.setFill(Color.GREEN);
            gc.fillOval(x + 4, y + 4, 10, 10);
            gc.fillOval(x + 18, y + 8, 10, 10);
            gc.fillOval(x + 8, y + 18, 10, 10);

            // Draw animated fire on top
            int animFrame = (int)(time / 100) % 3;

            // Fire base (orange/red)
            gc.setFill(Color.ORANGE);
            gc.fillOval(x + 4, y + 12 - animFrame, 12, 16 + animFrame);
            gc.fillOval(x + 16, y + 14 - animFrame, 10, 14 + animFrame);

            // Fire middle (yellow)
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + 6, y + 14 - animFrame * 2, 8, 12 + animFrame);
            gc.fillOval(x + 18, y + 16 - animFrame * 2, 6, 10 + animFrame);

            // Fire tips (bright yellow/white)
            gc.setFill(Color.rgb(255, 255, 200));
            gc.fillOval(x + 8, y + 10 - animFrame * 2, 4, 8);
            gc.fillOval(x + 19, y + 14 - animFrame * 2, 3, 6);

            // Smoke particles
            gc.setFill(Color.rgb(80, 80, 80, 0.6));
            int smokeOffset = (int)(time / 50) % 10;
            gc.fillOval(x + 10, y - smokeOffset, 6, 6);
            gc.fillOval(x + 18, y + 2 - smokeOffset, 4, 4);
        }
    }

    public boolean hasBurningTiles() {
        return !burningTiles.isEmpty();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public TileType getTile(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return TileType.STEEL; // treat out of bounds as steel
        }
        return tiles[row][col];
    }

    public void setTile(int row, int col, TileType type) {
        if (row >= 0 && row < height && col >= 0 && col < width) {
            tiles[row][col] = type;
        }
    }

    // Export all tiles as int array for network sync
    public int[][] exportTiles() {
        int[][] result = new int[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result[row][col] = tiles[row][col].ordinal();
            }
        }
        return result;
    }

    // Import all tiles from int array for network sync
    public void importTiles(int[][] tileData) {
        if (tileData == null) return;
        for (int row = 0; row < Math.min(height, tileData.length); row++) {
            for (int col = 0; col < Math.min(width, tileData[row].length); col++) {
                tiles[row][col] = TileType.values()[tileData[row][col]];
            }
        }
    }

    // Export burning tiles for network sync
    public Map<Integer, Integer> exportBurningTiles() {
        return new HashMap<>(burningTiles);
    }

    // Import burning tiles from network sync
    public void importBurningTiles(Map<Integer, Integer> data) {
        burningTiles.clear();
        if (data != null) {
            burningTiles.putAll(data);
        }
    }

    // Set burning tiles directly (for network sync with list)
    public void setBurningTiles(java.util.List<int[]> tiles) {
        burningTiles.clear();
        if (tiles != null) {
            for (int[] tile : tiles) {
                int key = tile[0] * 1000 + tile[1];
                burningTiles.put(key, tile[2]);
            }
        }
    }

    // Base protection management (for SHOVEL power-up)
    public void setBaseProtection(TileType protectionType) {
        // Top wall
        tiles[23][11] = protectionType;
        tiles[23][12] = protectionType;
        tiles[23][13] = protectionType;
        // Left wall
        tiles[24][11] = protectionType;
        // Right wall
        tiles[24][13] = protectionType;
        // Bottom wall
        tiles[25][11] = protectionType;
        tiles[25][12] = protectionType;
        tiles[25][13] = protectionType;
    }

    public void resetBaseProtection() {
        setBaseProtection(TileType.BRICK);
    }

    // Custom level support
    public void setCustomLevel(LevelData levelData) {
        this.customLevelData = levelData;
        if (levelData != null) {
            importTiles(levelData.getTiles());
        }
    }

    public boolean hasCustomLevel() {
        return customLevelData != null;
    }

    public LevelData getCustomLevelData() {
        return customLevelData;
    }

    // Load custom level (replacing random generation)
    public void loadCustomLevel() {
        if (customLevelData != null) {
            importTiles(customLevelData.getTiles());
        }
    }

    // For custom levels, regenerate means reload from the same data
    public void regenerateOrReloadLevel() {
        burningTiles.clear();
        if (customLevelData != null) {
            loadCustomLevel();
        } else {
            random.setSeed(currentLevelSeed);
            generateRandomLevel();
        }
    }
}
