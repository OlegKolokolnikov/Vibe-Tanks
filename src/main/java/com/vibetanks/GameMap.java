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
        generateLevel1();
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public void nextLevel() {
        levelNumber++;
        burningTiles.clear();
        // Generate new seed for new level
        currentLevelSeed = System.currentTimeMillis();
        random.setSeed(currentLevelSeed);
        generateRandomLevel();
    }

    public void resetToLevel1() {
        levelNumber = 1;
        burningTiles.clear();
        // Generate new seed for fresh start
        currentLevelSeed = System.currentTimeMillis();
        random.setSeed(currentLevelSeed);
        generateRandomLevel();
    }

    public void regenerateCurrentLevel() {
        // Keep the same level number AND same seed to get identical map
        burningTiles.clear();
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

        // Define reserved areas (keep empty for tank movement)
        // Player spawn area (bottom, rows 23-25, cols 7-17)
        // Enemy spawn areas (top, row 1-2, cols around 1, 12, 24)
        // Base area (bottom center, rows 23-25, cols 11-14)

        // Generate random structures
        int numStructures = 8 + random.nextInt(6); // 8-13 structures
        for (int s = 0; s < numStructures; s++) {
            generateRandomStructure();
        }

        // Add some water pools (2-4)
        int numWater = 2 + random.nextInt(3);
        for (int w = 0; w < numWater; w++) {
            generateWaterPool();
        }

        // Add some tree patches (2-4)
        int numTrees = 2 + random.nextInt(3);
        for (int t = 0; t < numTrees; t++) {
            generateTreePatch();
        }

        // Add some ice patches (1-3)
        int numIce = 1 + random.nextInt(3);
        for (int i = 0; i < numIce; i++) {
            generateIcePatch();
        }

        // Ensure base is surrounded by bricks
        createBaseProtection();

        // Clear spawn areas to ensure tanks can move
        clearSpawnAreas();
    }

    private void generateRandomStructure() {
        // Random structure size
        int structWidth = 2 + random.nextInt(4); // 2-5 tiles wide
        int structHeight = 2 + random.nextInt(3); // 2-4 tiles high

        // Random position (avoid borders and reserved areas)
        int startCol = 2 + random.nextInt(width - 4 - structWidth);
        int startRow = 3 + random.nextInt(height - 10 - structHeight); // Avoid bottom area

        // Choose tile type (mostly brick, some steel)
        TileType type = random.nextDouble() < 0.8 ? TileType.BRICK : TileType.STEEL;

        // Create structure
        for (int row = startRow; row < startRow + structHeight; row++) {
            for (int col = startCol; col < startCol + structWidth; col++) {
                if (isValidForObstacle(row, col)) {
                    tiles[row][col] = type;
                }
            }
        }

        // Sometimes add a gap in the middle for variety
        if (random.nextDouble() < 0.3 && structWidth >= 3 && structHeight >= 2) {
            int gapCol = startCol + structWidth / 2;
            int gapRow = startRow + structHeight / 2;
            if (isValidForObstacle(gapRow, gapCol)) {
                tiles[gapRow][gapCol] = TileType.EMPTY;
            }
        }
    }

    private void generateWaterPool() {
        int poolWidth = 2 + random.nextInt(4);
        int poolHeight = 2 + random.nextInt(3);
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

    private void generateTreePatch() {
        int patchWidth = 2 + random.nextInt(4);
        int patchHeight = 2 + random.nextInt(3);
        int startCol = 2 + random.nextInt(width - 4 - patchWidth);
        int startRow = 4 + random.nextInt(height - 10 - patchHeight);

        for (int row = startRow; row < startRow + patchHeight; row++) {
            for (int col = startCol; col < startCol + patchWidth; col++) {
                if (isValidForObstacle(row, col) && tiles[row][col] == TileType.EMPTY) {
                    tiles[row][col] = TileType.TREES;
                }
            }
        }
    }

    private void generateIcePatch() {
        int patchWidth = 2 + random.nextInt(4);
        int patchHeight = 2 + random.nextInt(3);
        int startCol = 2 + random.nextInt(width - 4 - patchWidth);
        int startRow = 5 + random.nextInt(height - 12 - patchHeight);

        for (int row = startRow; row < startRow + patchHeight; row++) {
            for (int col = startCol; col < startCol + patchWidth; col++) {
                if (isValidForObstacle(row, col) && tiles[row][col] == TileType.EMPTY) {
                    tiles[row][col] = TileType.ICE;
                }
            }
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
}
