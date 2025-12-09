package com.vibetanks.rendering;

import com.vibetanks.core.GameConstants;
import com.vibetanks.core.GameMap.TileType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renders map tiles to a graphics context.
 * Extracted from GameMap.java to separate rendering from game logic.
 */
public class TileRenderer {

    private static final int TILE_SIZE = GameConstants.TILE_SIZE;

    /**
     * Render a single tile at the specified position.
     *
     * @param gc The graphics context to render to
     * @param tile The tile type to render
     * @param x The x position
     * @param y The y position
     */
    public static void renderTile(GraphicsContext gc, TileType tile, double x, double y) {
        switch (tile) {
            case BRICK -> renderBrick(gc, x, y);
            case STEEL -> renderSteel(gc, x, y);
            case WATER -> renderWater(gc, x, y);
            case TREES -> renderTrees(gc, x, y);
            case ICE -> renderIce(gc, x, y);
            default -> {
                // Empty tile - already black background
            }
        }
    }

    /**
     * Render a tile, optionally skipping trees (for layered rendering).
     */
    public static void renderTile(GraphicsContext gc, TileType tile, double x, double y, boolean skipTrees) {
        if (skipTrees && tile == TileType.TREES) {
            return;
        }
        renderTile(gc, tile, x, y);
    }

    private static void renderBrick(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.rgb(139, 69, 19));
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        gc.setStroke(Color.rgb(100, 50, 10));
        gc.strokeRect(x, y, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
        gc.strokeRect(x + TILE_SIZE / 2.0, y, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
        gc.strokeRect(x, y + TILE_SIZE / 2.0, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
        gc.strokeRect(x + TILE_SIZE / 2.0, y + TILE_SIZE / 2.0, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
    }

    private static void renderSteel(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        gc.setStroke(Color.LIGHTGRAY);
        gc.strokeRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
    }

    private static void renderWater(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.BLUE);
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        gc.setFill(Color.LIGHTBLUE);
        gc.fillOval(x + 8, y + 8, 8, 8);
        gc.fillOval(x + 18, y + 18, 6, 6);
    }

    /**
     * Render trees tile.
     */
    public static void renderTrees(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        gc.setFill(Color.GREEN);
        gc.fillOval(x + 4, y + 4, 10, 10);
        gc.fillOval(x + 18, y + 8, 10, 10);
        gc.fillOval(x + 8, y + 18, 10, 10);
    }

    private static void renderIce(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.rgb(200, 230, 255));
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        gc.setStroke(Color.rgb(150, 200, 255));
        gc.setLineWidth(2);
        // Draw diagonal lines to represent ice texture
        gc.strokeLine(x, y, x + TILE_SIZE, y + TILE_SIZE);
        gc.strokeLine(x + TILE_SIZE, y, x, y + TILE_SIZE);
    }

    /**
     * Render a burning tree with animated fire effect.
     *
     * @param gc The graphics context
     * @param x The x position
     * @param y The y position
     * @param time Current time in milliseconds for animation
     */
    public static void renderBurningTree(GraphicsContext gc, double x, double y, long time) {
        // Draw tree underneath
        renderTrees(gc, x, y);

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
