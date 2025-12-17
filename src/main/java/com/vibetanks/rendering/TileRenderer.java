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
            case GROUND -> renderGround(gc, x, y);
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
        // Mortar/grout background (gray cement)
        gc.setFill(Color.rgb(140, 135, 130));
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Brick dimensions (4 rows, fitting within 32x32)
        double brickWidth = 14;
        double brickHeight = 7;
        double mortarGap = 1;

        // Row 1 - two full bricks
        gc.setFill(Color.rgb(178, 85, 55)); // Terracotta red
        gc.fillRect(x + 1, y + 1, brickWidth, brickHeight);
        gc.setFill(Color.rgb(165, 75, 50)); // Slightly darker
        gc.fillRect(x + 17, y + 1, brickWidth, brickHeight);

        // Row 2 - offset pattern (half brick, full, half)
        gc.setFill(Color.rgb(185, 90, 60));
        gc.fillRect(x + 1, y + 9, 6, brickHeight); // Half brick left
        gc.setFill(Color.rgb(170, 80, 52));
        gc.fillRect(x + 9, y + 9, brickWidth, brickHeight); // Full brick
        gc.setFill(Color.rgb(175, 82, 55));
        gc.fillRect(x + 25, y + 9, 6, brickHeight); // Half brick right

        // Row 3 - two full bricks
        gc.setFill(Color.rgb(172, 78, 48));
        gc.fillRect(x + 1, y + 17, brickWidth, brickHeight);
        gc.setFill(Color.rgb(180, 88, 58));
        gc.fillRect(x + 17, y + 17, brickWidth, brickHeight);

        // Row 4 - offset pattern (half brick, full, half)
        gc.setFill(Color.rgb(168, 76, 50));
        gc.fillRect(x + 1, y + 25, 6, 6); // Half brick left (trimmed)
        gc.setFill(Color.rgb(182, 86, 56));
        gc.fillRect(x + 9, y + 25, brickWidth, 6); // Full brick (trimmed)
        gc.setFill(Color.rgb(175, 80, 52));
        gc.fillRect(x + 25, y + 25, 6, 6); // Half brick right (trimmed)

        // Add subtle texture/weathering to some bricks
        gc.setFill(Color.rgb(150, 70, 45, 0.4));
        gc.fillRect(x + 3, y + 3, 4, 3);
        gc.fillRect(x + 19, y + 19, 5, 3);
        gc.setFill(Color.rgb(200, 100, 70, 0.3));
        gc.fillRect(x + 20, y + 3, 3, 2);
        gc.fillRect(x + 11, y + 11, 4, 2);
    }

    private static void renderSteel(GraphicsContext gc, double x, double y) {
        // Steel plate base
        gc.setFill(Color.rgb(90, 95, 100));
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Metallic sheen/highlight
        gc.setFill(Color.rgb(130, 135, 140));
        gc.fillRect(x + 2, y + 2, TILE_SIZE - 4, 3);
        gc.fillRect(x + 2, y + 2, 3, TILE_SIZE - 4);

        // Darker edge (shadow)
        gc.setFill(Color.rgb(60, 65, 70));
        gc.fillRect(x + 2, y + TILE_SIZE - 5, TILE_SIZE - 4, 3);
        gc.fillRect(x + TILE_SIZE - 5, y + 2, 3, TILE_SIZE - 4);

        // Rivets in corners
        gc.setFill(Color.rgb(70, 75, 80));
        gc.fillOval(x + 4, y + 4, 4, 4);
        gc.fillOval(x + TILE_SIZE - 8, y + 4, 4, 4);
        gc.fillOval(x + 4, y + TILE_SIZE - 8, 4, 4);
        gc.fillOval(x + TILE_SIZE - 8, y + TILE_SIZE - 8, 4, 4);
        // Rivet highlights
        gc.setFill(Color.rgb(120, 125, 130));
        gc.fillOval(x + 5, y + 5, 2, 2);
        gc.fillOval(x + TILE_SIZE - 7, y + 5, 2, 2);
        gc.fillOval(x + 5, y + TILE_SIZE - 7, 2, 2);
        gc.fillOval(x + TILE_SIZE - 7, y + TILE_SIZE - 7, 2, 2);

        // Barbed wire - horizontal strand
        gc.setStroke(Color.rgb(50, 50, 55));
        gc.setLineWidth(1.5);
        gc.strokeLine(x, y + 12, x + TILE_SIZE, y + 12);
        gc.strokeLine(x, y + 20, x + TILE_SIZE, y + 20);

        // Barbed wire - twisted parts between strands
        gc.setLineWidth(1);
        for (int i = 0; i < 4; i++) {
            double bx = x + 4 + i * 8;
            // Twist pattern
            gc.strokeLine(bx, y + 12, bx + 3, y + 20);
            gc.strokeLine(bx + 3, y + 12, bx, y + 20);
        }

        // Barbs (small spikes)
        gc.setStroke(Color.rgb(40, 40, 45));
        gc.setLineWidth(1);
        for (int i = 0; i < 5; i++) {
            double bx = x + 3 + i * 7;
            // Top wire barbs
            gc.strokeLine(bx, y + 12, bx - 2, y + 9);
            gc.strokeLine(bx, y + 12, bx + 2, y + 9);
            // Bottom wire barbs
            gc.strokeLine(bx + 3, y + 20, bx + 1, y + 23);
            gc.strokeLine(bx + 3, y + 20, bx + 5, y + 23);
        }
    }

    private static void renderWater(GraphicsContext gc, double x, double y) {
        // Base: medium blue water
        gc.setFill(Color.rgb(30, 100, 180));
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Darker blue patches (depth variation)
        gc.setFill(Color.rgb(20, 70, 140));
        gc.fillOval(x + 4, y + 8, 8, 6);
        gc.fillOval(x + 18, y + 4, 10, 7);
        gc.fillOval(x + 10, y + 20, 12, 8);
        gc.fillOval(x + 24, y + 18, 6, 8);

        // Dark blue dots (deep spots)
        gc.setFill(Color.rgb(15, 50, 110));
        gc.fillOval(x + 6, y + 10, 4, 3);
        gc.fillOval(x + 22, y + 6, 5, 4);
        gc.fillOval(x + 14, y + 22, 6, 4);
        gc.fillOval(x + 2, y + 24, 4, 4);

        // Wave lines (lighter blue)
        gc.setStroke(Color.rgb(60, 140, 220));
        gc.setLineWidth(1.5);
        // Wavy horizontal lines
        gc.strokeLine(x + 2, y + 6, x + 12, y + 8);
        gc.strokeLine(x + 16, y + 14, x + 28, y + 12);
        gc.strokeLine(x + 4, y + 26, x + 18, y + 28);

        // White foam/highlights
        gc.setFill(Color.rgb(220, 240, 255));
        gc.fillOval(x + 8, y + 5, 3, 2);
        gc.fillOval(x + 24, y + 12, 4, 2);
        gc.fillOval(x + 12, y + 18, 3, 2);

        // Bright white sparkles
        gc.setFill(Color.rgb(255, 255, 255));
        gc.fillOval(x + 10, y + 6, 2, 1);
        gc.fillOval(x + 26, y + 14, 2, 1);
        gc.fillOval(x + 6, y + 22, 2, 1);
    }

    /**
     * Render trees tile.
     * Trees are semi-transparent so tanks underneath are partially visible.
     */
    public static void renderTrees(GraphicsContext gc, double x, double y) {
        // Tree 1 (left side) - trunk and foliage
        // Trunk
        gc.setFill(Color.rgb(101, 67, 33, 0.95)); // Brown trunk
        gc.fillRect(x + 4, y + 20, 4, 10);
        // Foliage layers (darker at bottom, lighter at top)
        gc.setFill(Color.rgb(34, 100, 34, 0.88)); // Dark green base
        gc.fillOval(x, y + 14, 12, 9);
        gc.setFill(Color.rgb(34, 139, 34, 0.85)); // Forest green middle
        gc.fillOval(x + 1, y + 8, 10, 9);
        gc.setFill(Color.rgb(50, 160, 50, 0.8)); // Lighter green top
        gc.fillOval(x + 2, y + 3, 8, 7);

        // Tree 2 (center) - trunk and foliage
        // Trunk
        gc.setFill(Color.rgb(85, 55, 28, 0.95)); // Brown trunk
        gc.fillRect(x + 14, y + 22, 4, 8);
        // Foliage layers
        gc.setFill(Color.rgb(30, 90, 30, 0.88));
        gc.fillOval(x + 9, y + 16, 12, 9);
        gc.setFill(Color.rgb(40, 130, 40, 0.85));
        gc.fillOval(x + 10, y + 10, 10, 9);
        gc.setFill(Color.rgb(55, 155, 55, 0.8));
        gc.fillOval(x + 12, y + 5, 8, 7);

        // Tree 3 (right side) - trunk and foliage
        // Trunk
        gc.setFill(Color.rgb(90, 60, 30, 0.95)); // Slightly different brown
        gc.fillRect(x + 24, y + 18, 4, 12);
        // Foliage layers
        gc.setFill(Color.rgb(34, 100, 34, 0.88));
        gc.fillOval(x + 19, y + 12, 12, 9);
        gc.setFill(Color.rgb(34, 139, 34, 0.85));
        gc.fillOval(x + 20, y + 6, 10, 9);
        gc.setFill(Color.rgb(60, 170, 60, 0.8));
        gc.fillOval(x + 21, y + 1, 8, 7);

        // Leaf highlights (lighter spots)
        gc.setFill(Color.rgb(100, 200, 100, 0.7));
        gc.fillOval(x + 4, y + 5, 3, 3);
        gc.fillOval(x + 14, y + 7, 3, 3);
        gc.fillOval(x + 23, y + 3, 3, 3);
    }

    private static void renderIce(GraphicsContext gc, double x, double y) {
        // Base: white/light gray ice surface
        gc.setFill(Color.rgb(240, 245, 250));
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Add slight gray variation patches
        gc.setFill(Color.rgb(220, 225, 230));
        gc.fillRect(x + 2, y + 4, 10, 8);
        gc.fillRect(x + 18, y + 16, 12, 10);
        gc.fillRect(x + 8, y + 22, 8, 6);

        // Darker gray spots (frozen bubbles/imperfections)
        gc.setFill(Color.rgb(200, 205, 210));
        gc.fillOval(x + 6, y + 6, 4, 4);
        gc.fillOval(x + 22, y + 10, 3, 3);
        gc.fillOval(x + 14, y + 20, 5, 4);
        gc.fillOval(x + 4, y + 18, 3, 3);
        gc.fillOval(x + 26, y + 24, 4, 4);

        // Small white dots (snow/frost specks)
        gc.setFill(Color.rgb(255, 255, 255));
        gc.fillOval(x + 10, y + 12, 2, 2);
        gc.fillOval(x + 20, y + 6, 2, 2);
        gc.fillOval(x + 16, y + 26, 2, 2);
        gc.fillOval(x + 28, y + 18, 2, 2);

        // Crack lines (light gray)
        gc.setStroke(Color.rgb(180, 185, 195));
        gc.setLineWidth(1);
        // Main crack
        gc.strokeLine(x + 4, y + 2, x + 14, y + 14);
        gc.strokeLine(x + 14, y + 14, x + 12, y + 24);
        // Branch crack
        gc.strokeLine(x + 14, y + 14, x + 24, y + 18);
        // Small crack
        gc.strokeLine(x + 20, y + 4, x + 26, y + 10);
    }

    private static void renderGround(GraphicsContext gc, double x, double y) {
        // Base color: khaki/olive green mix
        gc.setFill(Color.rgb(107, 142, 35)); // Olive drab
        gc.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Add brown/tan patches for texture variety
        gc.setFill(Color.rgb(139, 119, 101)); // Khaki brown
        gc.fillRect(x + 2, y + 2, 8, 6);
        gc.fillRect(x + 18, y + 12, 10, 8);
        gc.fillRect(x + 6, y + 20, 7, 7);

        // Add darker green patches
        gc.setFill(Color.rgb(85, 107, 47)); // Dark olive green
        gc.fillRect(x + 14, y + 3, 6, 5);
        gc.fillRect(x + 4, y + 12, 8, 6);
        gc.fillRect(x + 22, y + 22, 6, 6);

        // Add subtle grass-like strokes
        gc.setStroke(Color.rgb(60, 90, 40)); // Darker green for grass texture
        gc.setLineWidth(1);
        gc.strokeLine(x + 8, y + 8, x + 10, y + 4);
        gc.strokeLine(x + 20, y + 18, x + 22, y + 14);
        gc.strokeLine(x + 26, y + 6, x + 28, y + 2);
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
