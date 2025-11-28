package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GameMap {
    private static final int TILE_SIZE = 32;
    private static final int BURN_DURATION = 60; // frames (1 second at 60 FPS)

    private int width;
    private int height;
    private TileType[][] tiles;

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

        // Completely surround base with bricks (base is at row 24, col 12)
        // Top wall
        tiles[23][11] = TileType.BRICK;
        tiles[23][12] = TileType.BRICK;
        tiles[23][13] = TileType.BRICK;
        // Left wall
        tiles[24][11] = TileType.BRICK;
        // Right wall
        tiles[24][13] = TileType.BRICK;
        // Bottom wall
        tiles[25][11] = TileType.BRICK;
        tiles[25][12] = TileType.BRICK;
        tiles[25][13] = TileType.BRICK;
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
