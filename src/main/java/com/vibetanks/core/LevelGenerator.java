package com.vibetanks.core;

import com.vibetanks.util.GameLogger;

import java.util.Random;

/**
 * Generates random level layouts for the game map.
 * Extracted from GameMap to separate level generation logic from tile management.
 */
public class LevelGenerator {
    private static final GameLogger LOG = GameLogger.getLogger(LevelGenerator.class);

    private final Random random;
    private final int width;
    private final int height;
    private GameMap.TileType[][] tiles;

    public LevelGenerator(int width, int height, Random random) {
        this.width = width;
        this.height = height;
        this.random = random;
    }

    /**
     * Generate a random level into the provided tiles array.
     *
     * @param tiles The tiles array to populate
     */
    public void generateRandomLevel(GameMap.TileType[][] tiles) {
        this.tiles = tiles;

        // Initialize with empty tiles
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }

        // Create border walls based on difficulty:
        // - Hard mode: all borders are STEEL (original behavior)
        // - Very easy mode (5 losses): all borders are GROUND (indestructible)
        // - Normal/Easy mode: bottom is GROUND, others are STEEL
        GameMap.TileType borderType;
        GameMap.TileType bottomBorderType;

        if (GameSettings.isHardModeActive()) {
            // Hard mode: all steel borders
            borderType = GameMap.TileType.STEEL;
            bottomBorderType = GameMap.TileType.STEEL;
        } else if (GameSettings.isVeryEasyModeActiveForCurrentLevel()) {
            // Very easy mode (5 losses): all ground borders
            borderType = GameMap.TileType.GROUND;
            bottomBorderType = GameMap.TileType.GROUND;
        } else {
            // Normal/Easy mode: steel borders except bottom is ground
            borderType = GameMap.TileType.STEEL;
            bottomBorderType = GameMap.TileType.GROUND;
        }

        for (int i = 0; i < width; i++) {
            tiles[0][i] = borderType;
            tiles[height - 1][i] = bottomBorderType;
        }
        // Side borders
        for (int i = 0; i < height - 1; i++) {
            tiles[i][0] = borderType;
            tiles[i][width - 1] = borderType;
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

        // Ensure less than 50% empty space
        ensureMinimumContent();

        LOG.info("Generating random level {}", "N/A");
    }

    /**
     * Calculate percentage of empty tiles (excluding borders and spawn areas).
     */
    private double calculateEmptyPercentage() {
        int totalPlayable = 0;
        int emptyCount = 0;

        // Count only playable area (excluding border tiles)
        for (int row = 1; row < height - 1; row++) {
            for (int col = 1; col < width - 1; col++) {
                totalPlayable++;
                if (tiles[row][col] == GameMap.TileType.EMPTY) {
                    emptyCount++;
                }
            }
        }

        return (double) emptyCount / totalPlayable;
    }

    /**
     * Ensure the level has appropriate content based on mode.
     * Normal mode: less than 50% empty space (more obstacles)
     * Hard mode: more than 50% empty space but at least 10% content (more open)
     */
    private void ensureMinimumContent() {
        int maxAttempts = 50; // Prevent infinite loops
        int attempts = 0;

        if (GameSettings.isHardModeActive()) {
            // Hard mode: ensure empty space is between 50% and 90%
            double emptyPct = calculateEmptyPercentage();

            // If too much content (empty < 50%), remove some tiles
            while (emptyPct < 0.50 && attempts < maxAttempts) {
                removeRandomContent();
                emptyPct = calculateEmptyPercentage();
                attempts++;
            }

            // If too empty (empty > 90%), add some content
            while (emptyPct > 0.90 && attempts < maxAttempts) {
                generateScatteredBlocks();
                emptyPct = calculateEmptyPercentage();
                attempts++;
            }

            if (attempts > 0) {
                clearSpawnAreas();
                LOG.info("Adjusted {} passes to meet hard mode 50-90% empty requirement", attempts);
            }
        } else {
            // Normal mode: ensure less than 50% empty space
            while (calculateEmptyPercentage() > 0.50 && attempts < maxAttempts) {
                // Add more content to fill empty space
                int contentType = random.nextInt(5);
                switch (contentType) {
                    case 0 -> generateGeometricShape();
                    case 1 -> generateCorridor();
                    case 2 -> generateScatteredBlocks();
                    case 3 -> generateTreePatch();
                    case 4 -> {
                        // Add a few scattered blocks at once
                        for (int i = 0; i < 3; i++) {
                            generateScatteredBlocks();
                        }
                    }
                }
                attempts++;
            }

            // Re-clear spawn areas after adding content
            if (attempts > 0) {
                clearSpawnAreas();
                LOG.info("Added {} content passes to meet 50% fill requirement", attempts);
            }
        }
    }

    /**
     * Remove random content tiles to create more open space (for hard mode).
     */
    private void removeRandomContent() {
        // Try to find and remove a non-essential tile
        int attempts = 0;
        while (attempts < 20) {
            int row = 2 + random.nextInt(height - 6);
            int col = 2 + random.nextInt(width - 4);

            GameMap.TileType tile = tiles[row][col];
            // Only remove destructible content (not borders, base protection, or ground)
            if (tile == GameMap.TileType.BRICK || tile == GameMap.TileType.TREES ||
                tile == GameMap.TileType.ICE || tile == GameMap.TileType.WATER) {
                // Don't remove base protection area
                if (!(row >= 23 && row <= 25 && col >= 11 && col <= 13)) {
                    tiles[row][col] = GameMap.TileType.EMPTY;
                    return;
                }
            }
            attempts++;
        }
    }

    private void placeTile(int row, int col, GameMap.TileType type) {
        if (row > 0 && row < height - 1 && col > 0 && col < width - 1) {
            // Don't overwrite base area (rows 23-24, cols 11-13)
            if (row >= 23 && row <= 24 && col >= 11 && col <= 13) {
                return;
            }
            tiles[row][col] = type;
        }
    }

    private void generateGeometricShape() {
        int shapeType = random.nextInt(8);
        int centerCol = 4 + random.nextInt(width - 8);
        int centerRow = 5 + random.nextInt(height - 14);
        GameMap.TileType type = random.nextDouble() < 0.75 ? GameMap.TileType.BRICK : GameMap.TileType.STEEL;

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

    private void generateHollowRectangle(int startRow, int startCol, GameMap.TileType type) {
        int w = 4 + random.nextInt(4);
        int h = 3 + random.nextInt(3);

        for (int col = startCol; col < startCol + w; col++) {
            placeTile(startRow, col, type);
            placeTile(startRow + h - 1, col, type);
        }
        for (int row = startRow; row < startRow + h; row++) {
            placeTile(row, startCol, type);
            placeTile(row, startCol + w - 1, type);
        }
        int opening = random.nextInt(4);
        switch (opening) {
            case 0 -> placeTile(startRow, startCol + w / 2, GameMap.TileType.EMPTY);
            case 1 -> placeTile(startRow + h - 1, startCol + w / 2, GameMap.TileType.EMPTY);
            case 2 -> placeTile(startRow + h / 2, startCol, GameMap.TileType.EMPTY);
            case 3 -> placeTile(startRow + h / 2, startCol + w - 1, GameMap.TileType.EMPTY);
        }
    }

    private void generateCross(int centerRow, int centerCol, GameMap.TileType type) {
        int armLength = 2 + random.nextInt(3);
        for (int i = -armLength; i <= armLength; i++) {
            placeTile(centerRow + i, centerCol, type);
        }
        for (int i = -armLength; i <= armLength; i++) {
            placeTile(centerRow, centerCol + i, type);
        }
    }

    private void generateDiamond(int centerRow, int centerCol, GameMap.TileType type) {
        int size = 2 + random.nextInt(2);
        for (int i = 0; i <= size; i++) {
            placeTile(centerRow - i, centerCol - (size - i), type);
            placeTile(centerRow - i, centerCol + (size - i), type);
            placeTile(centerRow + i, centerCol - (size - i), type);
            placeTile(centerRow + i, centerCol + (size - i), type);
        }
    }

    private void generateLShape(int startRow, int startCol, GameMap.TileType type) {
        int vertLen = 3 + random.nextInt(3);
        int horizLen = 3 + random.nextInt(3);
        boolean flipped = random.nextBoolean();
        boolean rotated = random.nextBoolean();

        for (int i = 0; i < vertLen; i++) {
            int col = flipped ? startCol + horizLen - 1 : startCol;
            placeTile(startRow + i, col, type);
        }
        int horizRow = rotated ? startRow : startRow + vertLen - 1;
        for (int i = 0; i < horizLen; i++) {
            placeTile(horizRow, startCol + i, type);
        }
    }

    private void generateTShape(int startRow, int startCol, GameMap.TileType type) {
        int topWidth = 4 + random.nextInt(3);
        int stemHeight = 2 + random.nextInt(3);

        for (int i = 0; i < topWidth; i++) {
            placeTile(startRow, startCol + i, type);
        }
        int stemCol = startCol + topWidth / 2;
        for (int i = 1; i <= stemHeight; i++) {
            placeTile(startRow + i, stemCol, type);
        }
    }

    private void generateUShape(int startRow, int startCol, GameMap.TileType type) {
        int w = 3 + random.nextInt(3);
        int h = 3 + random.nextInt(2);

        for (int i = 0; i < h; i++) {
            placeTile(startRow + i, startCol, type);
        }
        for (int i = 0; i < h; i++) {
            placeTile(startRow + i, startCol + w - 1, type);
        }
        for (int i = 0; i < w; i++) {
            placeTile(startRow + h - 1, startCol + i, type);
        }
    }

    private void generateZigzag(int startRow, int startCol, GameMap.TileType type) {
        int segments = 2 + random.nextInt(2);
        int segLen = 2 + random.nextInt(2);
        boolean goingRight = random.nextBoolean();

        int row = startRow;
        int col = startCol;
        for (int s = 0; s < segments; s++) {
            for (int i = 0; i < segLen; i++) {
                placeTile(row, col + (goingRight ? i : -i), type);
            }
            col += goingRight ? segLen - 1 : -(segLen - 1);
            for (int i = 1; i < segLen; i++) {
                placeTile(row + i, col, type);
            }
            row += segLen - 1;
            goingRight = !goingRight;
        }
    }

    private void generateSpiral(int centerRow, int centerCol, GameMap.TileType type) {
        int[][] spiral = {
            {0, 0}, {0, 1}, {0, 2}, {0, 3},
            {1, 3}, {2, 3}, {2, 2}, {2, 1},
            {2, 0}, {1, 0}
        };
        for (int[] offset : spiral) {
            placeTile(centerRow + offset[0], centerCol + offset[1], type);
        }
    }

    private void generateSymmetricPattern() {
        int patternType = random.nextInt(3);
        int centerCol = width / 2;
        GameMap.TileType type = random.nextDouble() < 0.7 ? GameMap.TileType.BRICK : GameMap.TileType.STEEL;

        switch (patternType) {
            case 0 -> generateSymmetricWalls(centerCol, type);
            case 1 -> generateSymmetricPillars(centerCol, type);
            case 2 -> generateSymmetricMaze(centerCol, type);
        }
    }

    private void generateSymmetricWalls(int centerCol, GameMap.TileType type) {
        int wallRow = 8 + random.nextInt(6);
        int wallLength = 3 + random.nextInt(4);
        int offset = 3 + random.nextInt(4);

        for (int i = 0; i < wallLength; i++) {
            placeTile(wallRow + i, centerCol - offset, type);
        }
        for (int i = 0; i < wallLength; i++) {
            placeTile(wallRow + i, centerCol + offset - 1, type);
        }
    }

    private void generateSymmetricPillars(int centerCol, GameMap.TileType type) {
        int numPillars = 2 + random.nextInt(2);
        int spacing = 4 + random.nextInt(3);

        for (int p = 0; p < numPillars; p++) {
            int row = 6 + p * spacing;
            int offset = 4 + random.nextInt(4);

            placeTile(row, centerCol - offset, type);
            placeTile(row + 1, centerCol - offset, type);
            placeTile(row, centerCol - offset + 1, type);
            placeTile(row + 1, centerCol - offset + 1, type);

            placeTile(row, centerCol + offset - 2, type);
            placeTile(row + 1, centerCol + offset - 2, type);
            placeTile(row, centerCol + offset - 1, type);
            placeTile(row + 1, centerCol + offset - 1, type);
        }
    }

    private void generateSymmetricMaze(int centerCol, GameMap.TileType type) {
        int[][] pattern = {
            {6, -6}, {6, -5}, {7, -5},
            {10, -8}, {10, -7}, {11, -7}, {11, -6},
            {14, -5}, {14, -4}, {14, -3}, {15, -3}
        };

        for (int[] pos : pattern) {
            placeTile(pos[0], centerCol + pos[1], type);
            placeTile(pos[0], centerCol - pos[1] - 1, type);
        }
    }

    private void generateCorridor() {
        boolean horizontal = random.nextBoolean();
        GameMap.TileType type = random.nextDouble() < 0.6 ? GameMap.TileType.BRICK : GameMap.TileType.STEEL;

        if (horizontal) {
            int row = 5 + random.nextInt(height - 12);
            int startCol = 2 + random.nextInt(5);
            int length = 4 + random.nextInt(8);
            for (int i = 0; i < length; i++) {
                placeTile(row, startCol + i, type);
            }
            if (length > 4) {
                placeTile(row, startCol + length / 2, GameMap.TileType.EMPTY);
            }
        } else {
            int col = 3 + random.nextInt(width - 6);
            int startRow = 4 + random.nextInt(5);
            int length = 3 + random.nextInt(6);
            for (int i = 0; i < length; i++) {
                placeTile(startRow + i, col, type);
            }
            if (length > 3) {
                placeTile(startRow + length / 2, col, GameMap.TileType.EMPTY);
            }
        }
    }

    private void generateWaterFeature() {
        int featureType = random.nextInt(3);
        switch (featureType) {
            case 0 -> generateWaterPool();
            case 1 -> generateWaterRiver();
            case 2 -> generateWaterLake();
        }
    }

    private void generateWaterPool() {
        int poolRow = 6 + random.nextInt(height - 14);
        int poolCol = 3 + random.nextInt(width - 8);
        int poolWidth = 2 + random.nextInt(2);
        int poolHeight = 2 + random.nextInt(2);

        for (int r = 0; r < poolHeight; r++) {
            for (int c = 0; c < poolWidth; c++) {
                placeTile(poolRow + r, poolCol + c, GameMap.TileType.WATER);
            }
        }
    }

    private void generateWaterRiver() {
        boolean horizontal = random.nextBoolean();
        if (horizontal) {
            int row = 8 + random.nextInt(height - 16);
            int startCol = 2;
            int endCol = width - 3;
            for (int c = startCol; c <= endCol; c++) {
                int wobble = random.nextInt(3) - 1;
                int actualRow = Math.max(1, Math.min(height - 2, row + wobble));
                placeTile(actualRow, c, GameMap.TileType.WATER);
                if (random.nextDouble() < 0.3) {
                    placeTile(actualRow + 1, c, GameMap.TileType.WATER);
                }
            }
        } else {
            int col = 6 + random.nextInt(width - 12);
            int startRow = 2;
            int endRow = height - 8;
            for (int r = startRow; r <= endRow; r++) {
                int wobble = random.nextInt(3) - 1;
                int actualCol = Math.max(1, Math.min(width - 2, col + wobble));
                placeTile(r, actualCol, GameMap.TileType.WATER);
            }
        }
    }

    private void generateWaterLake() {
        int lakeRow = 8 + random.nextInt(height - 18);
        int lakeCol = 5 + random.nextInt(width - 12);
        int lakeSize = 3 + random.nextInt(2);

        for (int r = -lakeSize; r <= lakeSize; r++) {
            for (int c = -lakeSize; c <= lakeSize; c++) {
                if (r * r + c * c <= lakeSize * lakeSize) {
                    placeTile(lakeRow + r, lakeCol + c, GameMap.TileType.WATER);
                }
            }
        }
    }

    private void generateTreePatch() {
        int patchType = random.nextInt(3);
        switch (patchType) {
            case 0 -> generateTreeCluster();
            case 1 -> generateTreeLine();
            case 2 -> generateTreeForest();
        }
    }

    private void generateTreeCluster() {
        int clusterRow = 4 + random.nextInt(height - 10);
        int clusterCol = 3 + random.nextInt(width - 8);
        int numTrees = 3 + random.nextInt(4);

        for (int i = 0; i < numTrees; i++) {
            int offsetRow = random.nextInt(3) - 1;
            int offsetCol = random.nextInt(3) - 1;
            placeTile(clusterRow + offsetRow, clusterCol + offsetCol, GameMap.TileType.TREES);
        }
    }

    private void generateTreeLine() {
        boolean horizontal = random.nextBoolean();
        if (horizontal) {
            int row = 3 + random.nextInt(height - 8);
            int startCol = 2 + random.nextInt(5);
            int length = 3 + random.nextInt(5);
            for (int i = 0; i < length; i++) {
                placeTile(row, startCol + i, GameMap.TileType.TREES);
            }
        } else {
            int col = 2 + random.nextInt(width - 6);
            int startRow = 3 + random.nextInt(5);
            int length = 3 + random.nextInt(5);
            for (int i = 0; i < length; i++) {
                placeTile(startRow + i, col, GameMap.TileType.TREES);
            }
        }
    }

    private void generateTreeForest() {
        int forestRow = 5 + random.nextInt(height - 12);
        int forestCol = 4 + random.nextInt(width - 10);
        int forestWidth = 3 + random.nextInt(3);
        int forestHeight = 2 + random.nextInt(2);

        for (int r = 0; r < forestHeight; r++) {
            for (int c = 0; c < forestWidth; c++) {
                if (random.nextDouble() < 0.7) {
                    placeTile(forestRow + r, forestCol + c, GameMap.TileType.TREES);
                }
            }
        }
    }

    private void generateIcePatch() {
        int patchType = random.nextInt(2);
        switch (patchType) {
            case 0 -> generateIceRink();
            case 1 -> generateIcePath();
        }
    }

    private void generateIceRink() {
        int rinkRow = 6 + random.nextInt(height - 14);
        int rinkCol = 4 + random.nextInt(width - 10);
        int rinkWidth = 3 + random.nextInt(3);
        int rinkHeight = 2 + random.nextInt(2);

        for (int r = 0; r < rinkHeight; r++) {
            for (int c = 0; c < rinkWidth; c++) {
                placeTile(rinkRow + r, rinkCol + c, GameMap.TileType.ICE);
            }
        }
    }

    private void generateIcePath() {
        boolean horizontal = random.nextBoolean();
        if (horizontal) {
            int row = 8 + random.nextInt(height - 16);
            int startCol = 3 + random.nextInt(5);
            int length = 4 + random.nextInt(6);
            for (int i = 0; i < length; i++) {
                placeTile(row, startCol + i, GameMap.TileType.ICE);
            }
        } else {
            int col = 5 + random.nextInt(width - 10);
            int startRow = 4 + random.nextInt(5);
            int length = 3 + random.nextInt(5);
            for (int i = 0; i < length; i++) {
                placeTile(startRow + i, col, GameMap.TileType.ICE);
            }
        }
    }

    private void generateScatteredBlocks() {
        int row = 2 + random.nextInt(height - 6);
        int col = 2 + random.nextInt(width - 4);

        double typeRoll = random.nextDouble();
        GameMap.TileType type;
        if (typeRoll < 0.5) {
            type = GameMap.TileType.BRICK;
        } else if (typeRoll < 0.7) {
            type = GameMap.TileType.STEEL;
        } else if (typeRoll < 0.85) {
            type = GameMap.TileType.TREES;
        } else {
            type = GameMap.TileType.WATER;
        }

        int size = 1 + random.nextInt(2);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                placeTile(row + r, col + c, type);
            }
        }
    }

    private void createBaseProtection() {
        // Base is at row 24, col 12 (1 tile, 32x32)
        // Surround with bricks in a U-shape (no gaps)
        tiles[23][11] = GameMap.TileType.BRICK;  // Top-left
        tiles[23][12] = GameMap.TileType.BRICK;  // Top (above base)
        tiles[23][13] = GameMap.TileType.BRICK;  // Top-right
        tiles[24][11] = GameMap.TileType.BRICK;  // Left of base
        tiles[24][13] = GameMap.TileType.BRICK;  // Right of base
        // In hard mode, bottom is STEEL so we need brick protection at row 25
        // In normal mode, bottom is GROUND (indestructible) so no need
        if (GameSettings.isHardModeActive()) {
            tiles[25][11] = GameMap.TileType.BRICK;  // Bottom-left
            tiles[25][12] = GameMap.TileType.BRICK;  // Bottom (below base)
            tiles[25][13] = GameMap.TileType.BRICK;  // Bottom-right
        }

        // Add wall above base to protect from center spawn
        // Random width 1-5 blocks, centered above base
        // In easy mode (3 losses): use GROUND (indestructible earth), otherwise STEEL
        GameMap.TileType wallType = GameSettings.isEasyModeActiveForCurrentLevel()
            ? GameMap.TileType.GROUND : GameMap.TileType.STEEL;
        int wallWidth = 1 + random.nextInt(5); // 1-5 blocks
        int startCol = 12 - wallWidth / 2; // Center around col 12-13
        int wallRow = 20 + random.nextInt(2); // Row 20 or 21
        for (int col = startCol; col < startCol + wallWidth && col < width - 1; col++) {
            if (col > 0) {
                tiles[wallRow][col] = wallType;
            }
        }
    }

    private void clearSpawnAreas() {
        // Clear only minimal spawn points - just enough for tanks to spawn (2x2 tiles)
        // Content variety comes from random generation, not from clearing

        // Enemy spawn 1 (left): minimal 2x2 at tile (1,1)
        for (int row = 1; row <= 2; row++) {
            for (int col = 1; col <= 2; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }

        // Enemy spawn 2 (center): wider for BOSS tank (4x5 tiles)
        for (int row = 1; row <= 5; row++) {
            for (int col = 10; col <= 16; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }

        // Enemy spawn 3 (right): minimal 2x2
        for (int row = 1; row <= 2; row++) {
            for (int col = width - 3; col <= width - 2; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }

        // Player spawn points (bottom) - minimal clearance
        // Player 1: around col 8-9
        for (int row = 23; row <= height - 2; row++) {
            for (int col = 7; col <= 9; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }
        // Player 2: around col 16-17
        for (int row = 23; row <= height - 2; row++) {
            for (int col = 16; col <= 18; col++) {
                tiles[row][col] = GameMap.TileType.EMPTY;
            }
        }

        // Re-apply base protection to ensure it's never cleared
        tiles[23][11] = GameMap.TileType.BRICK;
        tiles[23][12] = GameMap.TileType.BRICK;
        tiles[23][13] = GameMap.TileType.BRICK;
        tiles[24][11] = GameMap.TileType.BRICK;
        tiles[24][13] = GameMap.TileType.BRICK;

        // Add random obstacles near each enemy spawn (not blocking spawn itself)
        addObstaclesNearSpawn(3, 1, 6, 5);   // Near left spawn (rows 3-6, cols 1-5)
        addObstaclesNearSpawn(6, 8, 10, 18); // Near center spawn (rows 6-10, cols 8-18)
        addObstaclesNearSpawn(3, width - 6, 6, width - 2); // Near right spawn
    }

    /**
     * Add random obstacles in an area near a spawn point.
     * Places 1-3 small obstacle groups to ensure tanks have something to navigate.
     * Only places on empty tiles to preserve existing content (water, ice, etc).
     */
    private void addObstaclesNearSpawn(int minRow, int minCol, int maxRow, int maxCol) {
        int numObstacles = 1 + random.nextInt(3); // 1-3 obstacle groups

        for (int i = 0; i < numObstacles; i++) {
            int row = minRow + random.nextInt(Math.max(1, maxRow - minRow));
            int col = minCol + random.nextInt(Math.max(1, maxCol - minCol));

            // Random obstacle type
            GameMap.TileType type;
            double typeRoll = random.nextDouble();
            if (typeRoll < 0.6) {
                type = GameMap.TileType.BRICK;
            } else if (typeRoll < 0.8) {
                type = GameMap.TileType.STEEL;
            } else {
                type = GameMap.TileType.TREES;
            }

            // Place 1-2 tile obstacle (only on empty tiles)
            int size = 1 + random.nextInt(2);
            for (int r = 0; r < size && row + r < maxRow; r++) {
                for (int c = 0; c < size && col + c < maxCol; c++) {
                    int targetRow = row + r;
                    int targetCol = col + c;
                    if (targetRow > 0 && targetRow < height - 1 &&
                        targetCol > 0 && targetCol < width - 1 &&
                        tiles[targetRow][targetCol] == GameMap.TileType.EMPTY) {
                        tiles[targetRow][targetCol] = type;
                    }
                }
            }
        }
    }
}
