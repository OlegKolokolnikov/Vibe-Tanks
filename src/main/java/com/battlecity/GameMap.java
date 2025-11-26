package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class GameMap {
    private static final int TILE_SIZE = 32;

    private int width;
    private int height;
    private TileType[][] tiles;

    public enum TileType {
        EMPTY,
        BRICK,
        STEEL,
        WATER,
        TREES
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

        // Protect base with bricks
        tiles[23][11] = TileType.BRICK;
        tiles[23][12] = TileType.BRICK;
        tiles[23][13] = TileType.BRICK;
        tiles[23][14] = TileType.BRICK;
        tiles[22][11] = TileType.BRICK;
        tiles[22][14] = TileType.BRICK;
        tiles[21][11] = TileType.BRICK;
        tiles[21][14] = TileType.BRICK;
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
                    default:
                        // Empty tile - already black background
                        break;
                }
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public TileType getTile(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return TileType.STEEL; // treat out of bounds as steel
        }
        return tiles[row][col];
    }
}
