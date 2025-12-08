package com.vibetanks.rendering;

import com.vibetanks.core.PowerUp;
import com.vibetanks.core.Tank;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renders various icons for the game UI, including power-up icons and mini tank icons
 * for the statistics screen.
 */
public class IconRenderer {
    private final GraphicsContext gc;

    public IconRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * Render a small power-up icon at the specified position.
     */
    public void renderPowerUpIcon(double x, double y, PowerUp.Type type) {
        int size = 16;

        // Background with border
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, size, size);
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, size, size);

        // Draw icon based on type (scaled down versions of PowerUp icons)
        switch (type) {
            case GUN:
                // Steel wall with crack and bullet
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x + 1, y + 2, 6, 12);
                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineWidth(0.5);
                gc.strokeRect(x + 2, y + 3, 4, 4);
                gc.strokeRect(x + 2, y + 9, 4, 4);
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(1);
                gc.strokeLine(x + 7, y + 6, x + 9, y + 4);
                gc.strokeLine(x + 7, y + 6, x + 9, y + 8);
                gc.strokeLine(x + 7, y + 10, x + 9, y + 9);
                gc.strokeLine(x + 7, y + 10, x + 9, y + 12);
                gc.setFill(Color.RED);
                gc.fillOval(x + 10, y + 6, 5, 4);
                break;
            case STAR:
                double cx = x + size / 2;
                double cy = y + size / 2;
                double outerR = 6;
                double innerR = 2.5;
                double[] starX = new double[10];
                double[] starY = new double[10];
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI / 2 + (Math.PI * i / 5);
                    double r = (i % 2 == 0) ? outerR : innerR;
                    starX[i] = cx + r * Math.cos(angle);
                    starY[i] = cy - r * Math.sin(angle);
                }
                gc.setFill(Color.DARKRED);
                double[] starXB = new double[10];
                double[] starYB = new double[10];
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI / 2 + (Math.PI * i / 5);
                    double r = (i % 2 == 0) ? outerR + 1 : innerR + 0.5;
                    starXB[i] = cx + r * Math.cos(angle);
                    starYB[i] = cy - r * Math.sin(angle);
                }
                gc.fillPolygon(starXB, starYB, 10);
                gc.setFill(Color.ORANGE);
                gc.fillPolygon(starX, starY, 10);
                break;
            case CAR:
                gc.setFill(Color.YELLOW);
                gc.fillPolygon(
                    new double[]{x + 9, x + 5, x + 7, x + 4, x + 10, x + 8, x + 12},
                    new double[]{y + 1, y + 7, y + 7, y + 15, y + 8, y + 8, y + 1},
                    7
                );
                gc.setStroke(Color.LIME);
                gc.setLineWidth(1);
                gc.strokePolygon(
                    new double[]{x + 9, x + 5, x + 7, x + 4, x + 10, x + 8, x + 12},
                    new double[]{y + 1, y + 7, y + 7, y + 15, y + 8, y + 8, y + 1},
                    7
                );
                break;
            case SHIP:
                gc.setFill(Color.BLUE);
                gc.fillRect(x + 1, y + 11, 14, 3);
                gc.setFill(Color.CYAN);
                gc.fillPolygon(
                    new double[]{x + 3, x + 13, x + 12, x + 4},
                    new double[]{y + 9, y + 9, y + 12, y + 12},
                    4
                );
                gc.fillPolygon(
                    new double[]{x + 8, x + 8, x + 12},
                    new double[]{y + 3, y + 9, y + 9},
                    3
                );
                break;
            case SAW:
                gc.setFill(Color.BROWN);
                gc.fillOval(x + 3, y + 3, 10, 10);
                gc.setFill(Color.WHITE);
                gc.fillOval(x + 6, y + 6, 4, 4);
                break;
            case SHIELD:
                gc.setFill(Color.BLUE);
                gc.fillPolygon(
                    new double[]{x + 8, x + 3, x + 3, x + 8, x + 13, x + 13},
                    new double[]{y + 14, y + 10, y + 3, y + 2, y + 3, y + 10},
                    6
                );
                gc.setFill(Color.LIGHTBLUE);
                gc.fillPolygon(
                    new double[]{x + 8, x + 5, x + 5, x + 8, x + 11, x + 11},
                    new double[]{y + 12, y + 9, y + 5, y + 4, y + 5, y + 9},
                    6
                );
                break;
            case SHOVEL:
                gc.setFill(Color.ORANGE);
                gc.fillRect(x + 7, y + 2, 2, 7);
                gc.fillPolygon(
                    new double[]{x + 4, x + 12, x + 11, x + 5},
                    new double[]{y + 9, y + 9, y + 14, y + 14},
                    4
                );
                break;
            case TANK:
                gc.setFill(Color.GREEN);
                gc.fillRect(x + 2, y + 8, 7, 5);
                gc.fillRect(x + 4, y + 5, 3, 4);
                gc.setFill(Color.DARKGREEN);
                gc.fillRect(x + 11, y + 4, 1, 5);
                gc.fillRect(x + 9, y + 6, 5, 1);
                gc.fillRect(x + 11, y + 10, 1, 4);
                break;
            case MACHINEGUN:
                gc.setFill(Color.PURPLE);
                gc.fillRect(x + 2, y + 7, 6, 3);
                gc.setFill(Color.YELLOW);
                gc.fillOval(x + 9, y + 7, 2, 2);
                gc.fillOval(x + 11, y + 5, 2, 2);
                gc.fillOval(x + 11, y + 9, 2, 2);
                gc.fillOval(x + 13, y + 7, 2, 2);
                break;
            default:
                break;
        }
    }

    /**
     * Get the color associated with a power-up type.
     */
    public static Color getPowerUpColor(PowerUp.Type type) {
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

    /**
     * Draw a mini tank icon for statistics table headers.
     */
    public void drawMiniTank(double x, double y, Tank.EnemyType type) {
        double s = 18;

        Color bodyColor, turretColor, trackColor;
        switch (type) {
            case REGULAR -> { bodyColor = Color.DARKRED; turretColor = Color.RED; trackColor = Color.rgb(80, 40, 40); }
            case ARMORED -> { bodyColor = Color.rgb(100, 30, 30); turretColor = Color.rgb(140, 50, 50); trackColor = Color.rgb(60, 30, 30); }
            case FAST -> { bodyColor = Color.rgb(200, 80, 80); turretColor = Color.rgb(255, 120, 120); trackColor = Color.rgb(150, 60, 60); }
            case POWER -> { bodyColor = Color.PURPLE; turretColor = Color.MAGENTA; trackColor = Color.DARKMAGENTA; }
            case HEAVY -> { bodyColor = Color.rgb(50, 50, 50); turretColor = Color.DARKGRAY; trackColor = Color.BLACK; }
            case BOSS -> { bodyColor = Color.rgb(150, 30, 30); turretColor = Color.rgb(220, 50, 50); trackColor = Color.rgb(100, 20, 20); }
            default -> { bodyColor = Color.DARKRED; turretColor = Color.RED; trackColor = Color.rgb(80, 40, 40); }
        }

        // Draw tracks with pattern
        gc.setFill(trackColor);
        gc.fillRoundRect(x, y + 2, 4, s - 4, 2, 2);
        gc.fillRoundRect(x + s - 4, y + 2, 4, s - 4, 2, 2);

        gc.setStroke(Color.rgb(40, 40, 40));
        gc.setLineWidth(1);
        for (int i = 0; i < 3; i++) {
            double ty = y + 4 + i * 5;
            gc.strokeLine(x, ty, x + 4, ty);
            gc.strokeLine(x + s - 4, ty, x + s, ty);
        }

        // Draw tank body
        gc.setFill(bodyColor);
        gc.fillRect(x + 3, y + 3, s - 6, s - 6);

        // Draw turret
        gc.setFill(turretColor);
        double turretSize = s * 0.5;
        gc.fillOval(x + s/2 - turretSize/2, y + s/2 - turretSize/2 + 1, turretSize, turretSize);

        // Draw barrel
        gc.setFill(turretColor);
        gc.fillRoundRect(x + s/2 - 2, y - 2, 4, s/2 + 2, 1, 1);
        gc.setFill(bodyColor);
        gc.fillRect(x + s/2 - 2, y - 2, 4, 2);

        // Type-specific decorations
        switch (type) {
            case ARMORED -> {
                gc.setFill(Color.GRAY);
                gc.fillRect(x + 5, y + s/2 - 1, s - 10, 3);
            }
            case HEAVY -> {
                gc.setFill(Color.WHITE);
                gc.fillOval(x + s/2 - 2, y + s/2 - 1, 4, 4);
            }
            case FAST -> {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1);
                gc.strokeLine(x + 2, y + s - 2, x + s - 2, y + s - 2);
            }
            case BOSS -> {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                double cx = x + s/2, cy = y + s/2;
                gc.strokeLine(cx - 3, cy - 2, cx + 3, cy + 4);
                gc.strokeLine(cx + 3, cy - 2, cx - 3, cy + 4);
            }
            case POWER -> {
                gc.setFill(Color.YELLOW);
                gc.fillOval(x + s/2 - 1.5, y + s/2 - 0.5, 3, 3);
            }
            default -> {}
        }
    }
}
