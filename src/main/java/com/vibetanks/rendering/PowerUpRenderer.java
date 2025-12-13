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

        // White background with border
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, SIZE, SIZE);
        gc.setStroke(Color.DARKGRAY);
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
        // Water waves
        gc.setFill(Color.BLUE);
        gc.fillRect(x + 3, y + 21, 26, 6);
        gc.setFill(Color.CYAN);
        // Boat hull
        gc.fillPolygon(
            new double[]{x + 5, x + 27, x + 24, x + 8},
            new double[]{y + 19, y + 19, y + 24, y + 24},
            4
        );
        // Sail
        gc.fillPolygon(
            new double[]{x + 16, x + 16, x + 24},
            new double[]{y + 5, y + 19, y + 19},
            3
        );
    }

    private static void renderShovel(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.ORANGE);
        // Shovel handle
        gc.fillRect(x + 13, y + 3, 6, 16);
        // Shovel blade
        gc.fillPolygon(
            new double[]{x + 8, x + 24, x + 21, x + 11},
            new double[]{y + 19, y + 19, y + 29, y + 29},
            4
        );
        // Dirt/steel color accent
        gc.setFill(Color.GRAY);
        gc.fillRect(x + 11, y + 21, 10, 5);
    }

    private static void renderSaw(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.BROWN);
        gc.fillOval(x + 5, y + 5, 22, 22);
        // Saw teeth
        gc.setFill(Color.DARKGRAY);
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            double tx = x + SIZE / 2.0 + 10 * Math.cos(angle);
            double ty = y + SIZE / 2.0 + 10 * Math.sin(angle);
            gc.fillRect(tx - 3, ty - 3, 6, 6);
        }
        // Center hole
        gc.setFill(Color.WHITE);
        gc.fillOval(x + 12, y + 12, 8, 8);
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
        gc.setFill(Color.PURPLE);
        // Gun barrel
        gc.fillRect(x + 3, y + 13, 14, 6);
        gc.fillRect(x + 5, y + 19, 6, 6); // Grip
        // Multiple bullets flying
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 17, y + 13, 6, 6);
        gc.fillOval(x + 23, y + 10, 4, 4);
        gc.fillOval(x + 23, y + 18, 4, 4);
        gc.fillOval(x + 27, y + 14, 3, 3);
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
        // Laser emitter/gun
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(x + 3, y + 12, 10, 8);
        gc.setFill(Color.GRAY);
        gc.fillRect(x + 5, y + 14, 6, 4);
        // Laser beam
        gc.setFill(Color.DARKRED);
        gc.fillRect(x + 13, y + 14, 18, 4);
        gc.setFill(Color.RED);
        gc.fillRect(x + 13, y + 15, 18, 2);
        gc.setFill(Color.WHITE);
        gc.fillRect(x + 13, y + 15.5, 18, 1);
        // Glow particles
        gc.setFill(Color.ORANGE);
        gc.fillOval(x + 28, y + 13, 3, 3);
        gc.fillOval(x + 26, y + 17, 2, 2);
        gc.fillOval(x + 29, y + 16, 2, 2);
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
