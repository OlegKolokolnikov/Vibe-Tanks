package com.vibetanks.rendering;

import com.vibetanks.core.PowerUp;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Handles rendering of power-up icons.
 * Extracted from PowerUp.java for better separation of concerns.
 */
public class PowerUpRenderer {
    private static final int SIZE = 32;

    /**
     * Render a power-up at the specified position.
     * @param gc Graphics context
     * @param x X position
     * @param y Y position
     * @param type Power-up type
     * @param lifetime Remaining lifetime (for flashing effect)
     */
    public static void render(GraphicsContext gc, double x, double y, PowerUp.Type type, int lifetime) {
        // Flashing effect when about to expire
        if (lifetime < 120 && lifetime % 20 < 10) {
            return;
        }

        // Semi-transparent white background with border
        gc.setFill(Color.rgb(255, 255, 255, 0.5));
        gc.fillRect(x, y, SIZE, SIZE);
        gc.setStroke(Color.rgb(100, 100, 100, 0.6));
        gc.setLineWidth(1);
        gc.strokeRect(x, y, SIZE, SIZE);

        // Draw icon based on type
        renderIcon(gc, x, y, type);
    }

    private static void renderIcon(GraphicsContext gc, double x, double y, PowerUp.Type type) {
        switch (type) {
            case GUN -> renderGun(gc, x, y);
            case STAR -> renderStar(gc, x, y);
            case CAR -> renderCar(gc, x, y);
            case SHIP -> renderShip(gc, x, y);
            case SHOVEL -> renderShovel(gc, x, y);
            case SAW -> renderSaw(gc, x, y);
            case TANK -> renderTank(gc, x, y);
            case SHIELD -> renderShield(gc, x, y);
            case MACHINEGUN -> renderMachinegun(gc, x, y);
            case FREEZE -> renderFreeze(gc, x, y);
            case BOMB -> renderBomb(gc, x, y);
            case LASER -> renderLaser(gc, x, y);
        }
    }

    private static void renderGun(GraphicsContext gc, double x, double y) {
        // Steel wall on left side
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(x + 2, y + 4, 12, 24);
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(x + 4, y + 6, 8, 8);
        gc.strokeRect(x + 4, y + 18, 8, 8);

        // Crack/explosion in wall
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2);
        gc.strokeLine(x + 14, y + 12, x + 18, y + 8);
        gc.strokeLine(x + 14, y + 12, x + 18, y + 16);
        gc.strokeLine(x + 14, y + 12, x + 18, y + 12);
        gc.strokeLine(x + 14, y + 20, x + 17, y + 17);
        gc.strokeLine(x + 14, y + 20, x + 17, y + 23);

        // Big red bullet coming from right
        gc.setFill(Color.RED);
        gc.fillOval(x + 20, y + 12, 10, 8);
        gc.setFill(Color.ORANGE);
        gc.fillOval(x + 22, y + 14, 4, 4);
    }

    private static void renderStar(GraphicsContext gc, double x, double y) {
        double cx = x + SIZE / 2.0;
        double cy = y + SIZE / 2.0;
        double outerR = 13;
        double innerR = 5;
        double[] starX = new double[10];
        double[] starY = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + (Math.PI * i / 5);
            double r = (i % 2 == 0) ? outerR : innerR;
            starX[i] = cx + r * Math.cos(angle);
            starY[i] = cy - r * Math.sin(angle);
        }
        // Red border
        gc.setFill(Color.DARKRED);
        double[] starXBorder = new double[10];
        double[] starYBorder = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + (Math.PI * i / 5);
            double r = (i % 2 == 0) ? outerR + 2 : innerR + 1;
            starXBorder[i] = cx + r * Math.cos(angle);
            starYBorder[i] = cy - r * Math.sin(angle);
        }
        gc.fillPolygon(starXBorder, starYBorder, 10);
        // Orange star on top
        gc.setFill(Color.ORANGE);
        gc.fillPolygon(starX, starY, 10);
    }

    private static void renderCar(GraphicsContext gc, double x, double y) {
        // Lightning bolt shape
        gc.setFill(Color.YELLOW);
        gc.fillPolygon(
            new double[]{x + 18, x + 10, x + 14, x + 8, x + 20, x + 16, x + 24},
            new double[]{y + 3, y + 14, y + 14, y + 29, y + 17, y + 17, y + 3},
            7
        );
        // Inner highlight
        gc.setFill(Color.WHITE);
        gc.fillPolygon(
            new double[]{x + 17, x + 13, x + 15, x + 12, x + 18, x + 16, x + 20},
            new double[]{y + 7, y + 14, y + 14, y + 24, y + 17, y + 17, y + 7},
            7
        );
        // Green border/glow
        gc.setStroke(Color.LIME);
        gc.setLineWidth(2);
        gc.strokePolygon(
            new double[]{x + 18, x + 10, x + 14, x + 8, x + 20, x + 16, x + 24},
            new double[]{y + 3, y + 14, y + 14, y + 29, y + 17, y + 17, y + 3},
            7
        );
    }

    private static void renderShip(GraphicsContext gc, double x, double y) {
        // Water waves at bottom
        gc.setFill(Color.rgb(30, 100, 180));
        gc.fillRect(x + 1, y + 23, 30, 8);
        // Wave highlights
        gc.setFill(Color.rgb(100, 160, 220));
        gc.fillOval(x + 3, y + 24, 8, 3);
        gc.fillOval(x + 14, y + 25, 10, 3);
        gc.fillOval(x + 25, y + 24, 6, 3);

        // Hull (brown wooden ship)
        gc.setFill(Color.rgb(120, 70, 40));
        gc.fillPolygon(
            new double[]{x + 3, x + 29, x + 26, x + 6},
            new double[]{y + 18, y + 18, y + 25, y + 25},
            4
        );
        // Hull stripe
        gc.setFill(Color.rgb(90, 50, 30));
        gc.fillRect(x + 5, y + 20, 22, 2);

        // Mast
        gc.setFill(Color.rgb(100, 60, 35));
        gc.fillRect(x + 15, y + 4, 3, 16);

        // Main sail (white/cream)
        gc.setFill(Color.rgb(245, 240, 230));
        gc.fillPolygon(
            new double[]{x + 18, x + 18, x + 28},
            new double[]{y + 5, y + 16, y + 16},
            3
        );
        // Sail shadow
        gc.setFill(Color.rgb(200, 195, 185));
        gc.fillPolygon(
            new double[]{x + 18, x + 18, x + 23},
            new double[]{y + 10, y + 16, y + 16},
            3
        );

        // Flag on top
        gc.setFill(Color.rgb(200, 50, 50));
        gc.fillRect(x + 18, y + 3, 6, 4);
    }

    private static void renderShovel(GraphicsContext gc, double x, double y) {
        // Wooden handle
        gc.setFill(Color.rgb(140, 90, 50));
        gc.fillRect(x + 14, y + 2, 4, 14);
        // Handle grip (darker wood)
        gc.setFill(Color.rgb(100, 65, 35));
        gc.fillRect(x + 13, y + 2, 6, 4);

        // Metal collar connecting handle to blade
        gc.setFill(Color.rgb(120, 125, 130));
        gc.fillRect(x + 12, y + 14, 8, 3);

        // Shovel blade (metallic)
        gc.setFill(Color.rgb(160, 165, 170));
        gc.fillPolygon(
            new double[]{x + 8, x + 24, x + 22, x + 16, x + 10},
            new double[]{y + 17, y + 17, y + 28, y + 30, y + 28},
            5
        );

        // Blade edge (shiny)
        gc.setFill(Color.rgb(200, 205, 210));
        gc.fillPolygon(
            new double[]{x + 10, x + 22, x + 21, x + 16, x + 11},
            new double[]{y + 26, y + 26, y + 28, y + 30, y + 28},
            5
        );

        // Blade shadow/depth
        gc.setFill(Color.rgb(100, 105, 110));
        gc.fillRect(x + 12, y + 19, 8, 4);

        // Dirt on blade
        gc.setFill(Color.rgb(100, 70, 40));
        gc.fillOval(x + 11, y + 22, 5, 3);
        gc.fillOval(x + 17, y + 23, 4, 3);
    }

    private static void renderSaw(GraphicsContext gc, double x, double y) {
        double cx = x + SIZE / 2.0;
        double cy = y + SIZE / 2.0;

        // Draw big visible teeth first (dark steel)
        gc.setFill(Color.rgb(80, 85, 90));
        int teethCount = 8;
        for (int i = 0; i < teethCount; i++) {
            double angle = (Math.PI * 2 * i) / teethCount;
            // Big rectangular tooth
            double tx = cx + 11 * Math.cos(angle);
            double ty = cy + 11 * Math.sin(angle);
            gc.save();
            gc.translate(tx, ty);
            gc.rotate(Math.toDegrees(angle));
            gc.fillRect(-3, -4, 6, 8);
            gc.restore();
        }

        // Main blade disk (orange/gold like wood cutting saw)
        gc.setFill(Color.rgb(210, 150, 50));
        gc.fillOval(x + 6, y + 6, 20, 20);

        // Inner ring
        gc.setFill(Color.rgb(180, 120, 40));
        gc.fillOval(x + 9, y + 9, 14, 14);

        // Center hole
        gc.setFill(Color.rgb(50, 50, 55));
        gc.fillOval(x + 13, y + 13, 6, 6);

        // Shine
        gc.setFill(Color.rgb(240, 200, 100, 0.6));
        gc.fillOval(x + 10, y + 10, 5, 4);
    }

    private static void renderTank(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.GREEN);
        // Mini tank body
        gc.fillRect(x + 5, y + 16, 14, 11);
        gc.fillRect(x + 9, y + 10, 6, 8); // Turret
        // +1 text
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(x + 21, y + 8, 3, 11); // Vertical of +
        gc.fillRect(x + 18, y + 12, 9, 3); // Horizontal of +
        gc.fillRect(x + 21, y + 21, 3, 8); // 1
    }

    private static void renderShield(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.BLUE);
        // Shield outline
        gc.fillPolygon(
            new double[]{x + 16, x + 5, x + 5, x + 16, x + 27, x + 27},
            new double[]{y + 29, y + 21, y + 5, y + 3, y + 5, y + 21},
            6
        );
        // Inner shield highlight
        gc.setFill(Color.LIGHTBLUE);
        gc.fillPolygon(
            new double[]{x + 16, x + 9, x + 9, x + 16, x + 23, x + 23},
            new double[]{y + 24, y + 19, y + 8, y + 7, y + 8, y + 19},
            6
        );
    }

    private static void renderMachinegun(GraphicsContext gc, double x, double y) {
        // Gun body (dark metal)
        gc.setFill(Color.rgb(50, 50, 55));
        gc.fillRect(x + 2, y + 12, 16, 8);
        // Barrel with holes
        gc.setFill(Color.rgb(70, 70, 75));
        gc.fillRect(x + 4, y + 14, 12, 4);
        // Barrel holes (cooling vents)
        gc.setFill(Color.rgb(30, 30, 35));
        gc.fillOval(x + 5, y + 14.5, 2, 3);
        gc.fillOval(x + 8, y + 14.5, 2, 3);
        gc.fillOval(x + 11, y + 14.5, 2, 3);
        // Stock/grip
        gc.setFill(Color.rgb(60, 45, 30)); // Wood color
        gc.fillRect(x + 2, y + 20, 6, 6);
        // Muzzle flash
        gc.setFill(Color.rgb(255, 200, 50));
        gc.fillOval(x + 17, y + 12, 6, 8);
        gc.setFill(Color.rgb(255, 255, 200));
        gc.fillOval(x + 18, y + 14, 3, 4);
        // Flying bullets (tracer rounds)
        gc.setFill(Color.rgb(255, 180, 50));
        gc.fillRect(x + 23, y + 11, 5, 2);
        gc.fillRect(x + 25, y + 15, 5, 2);
        gc.fillRect(x + 24, y + 19, 5, 2);
        // Bullet tips
        gc.setFill(Color.rgb(200, 150, 50));
        gc.fillOval(x + 27, y + 10.5, 2, 3);
        gc.fillOval(x + 29, y + 14.5, 2, 3);
        gc.fillOval(x + 28, y + 18.5, 2, 3);
    }

    private static void renderFreeze(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillOval(x + 6, y + 6, 20, 20);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        // Snowflake lines
        double scx = x + SIZE / 2.0;
        double scy = y + SIZE / 2.0;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * i) / 3;
            gc.strokeLine(scx, scy, scx + 12 * Math.cos(angle), scy + 12 * Math.sin(angle));
        }
        // Center dot
        gc.setFill(Color.CYAN);
        gc.fillOval(scx - 4, scy - 4, 8, 8);
    }

    private static void renderBomb(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.BLACK);
        gc.fillOval(x + 5, y + 10, 20, 20);
        // Fuse
        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(3);
        gc.strokeLine(x + 19, y + 10, x + 24, y + 5);
        // Spark
        gc.setFill(Color.ORANGE);
        gc.fillOval(x + 21, y + 2, 7, 7);
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 23, y + 4, 4, 4);
        // Bomb highlight
        gc.setFill(Color.DARKGRAY);
        gc.fillOval(x + 9, y + 15, 6, 6);
    }

    private static void renderLaser(GraphicsContext gc, double x, double y) {
        // Laser emitter device
        gc.setFill(Color.rgb(60, 60, 65));
        gc.fillRect(x + 2, y + 10, 12, 12);
        // Emitter lens housing
        gc.setFill(Color.rgb(80, 80, 85));
        gc.fillOval(x + 4, y + 12, 8, 8);
        // Lens (glowing red)
        gc.setFill(Color.rgb(200, 50, 50));
        gc.fillOval(x + 6, y + 14, 4, 4);
        // Lens highlight
        gc.setFill(Color.rgb(255, 150, 150));
        gc.fillOval(x + 6.5, y + 14.5, 2, 2);

        // Laser beam (gradient effect with glow)
        // Outer glow
        gc.setFill(Color.rgb(255, 100, 100, 0.4));
        gc.fillRect(x + 12, y + 13, 19, 6);
        // Middle beam
        gc.setFill(Color.rgb(255, 50, 50));
        gc.fillRect(x + 12, y + 14.5, 19, 3);
        // Core (bright)
        gc.setFill(Color.rgb(255, 200, 200));
        gc.fillRect(x + 12, y + 15.5, 19, 1);

        // Impact point sparks
        gc.setFill(Color.rgb(255, 255, 200));
        gc.fillOval(x + 28, y + 12, 3, 3);
        gc.fillOval(x + 29, y + 17, 2, 2);
        gc.setFill(Color.rgb(255, 150, 50));
        gc.fillOval(x + 26, y + 11, 2, 2);
        gc.fillOval(x + 27, y + 19, 2, 2);
    }

    /**
     * Get the color associated with a power-up type.
     */
    public static Color getTypeColor(PowerUp.Type type) {
        return switch (type) {
            case GUN -> Color.RED;
            case STAR -> Color.YELLOW;
            case CAR -> Color.LIME;
            case SHIP -> Color.CYAN;
            case SHOVEL -> Color.ORANGE;
            case SAW -> Color.BROWN;
            case TANK -> Color.GREEN;
            case SHIELD -> Color.BLUE;
            case MACHINEGUN -> Color.PURPLE;
            case FREEZE -> Color.LIGHTBLUE;
            case BOMB -> Color.BLACK;
            case LASER -> Color.RED;
        };
    }
}
