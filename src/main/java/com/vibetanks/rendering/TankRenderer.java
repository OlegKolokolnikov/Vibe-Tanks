package com.vibetanks.rendering;

import com.vibetanks.core.Direction;
import com.vibetanks.core.FrameTime;
import com.vibetanks.core.Tank;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renders tanks to a graphics context.
 * Extracted from Tank.java to separate rendering from game logic.
 */
public class TankRenderer {
    // Pre-allocated color result array to avoid allocation in hot path
    private static final Color[] colorResult = new Color[2];

    // Static rainbow colors for POWER tank (avoid allocation each frame)
    private static final Color[] RAINBOW_COLORS = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.PURPLE
    };

    /**
     * Render a tank to the given graphics context.
     *
     * @param gc The graphics context to render to
     * @param tank The tank to render
     */
    public static void render(GraphicsContext gc, Tank tank) {
        if (!tank.isAlive()) return;

        double x = tank.getX();
        double y = tank.getY();
        int size = tank.getSize();
        Direction direction = tank.getDirection();
        boolean isPlayer = tank.isPlayer();
        int playerNumber = tank.getPlayerNumber();
        Tank.EnemyType enemyType = tank.getEnemyType();
        int trackAnimationFrame = tank.getTrackAnimationFrame();

        // Scale factor for rendering (1.0 for normal tanks, 4.0 for BOSS)
        double scale = (double) size / Tank.BASE_SIZE;

        // Draw shields
        renderShields(gc, tank, x, y, size, scale);

        // Draw ship indicator if active
        if (tank.canSwim()) {
            renderShipIndicator(gc, x, y, size, scale);
        }

        // Get tank colors (with color override for enemies that collected LIFE/STEEL)
        Color[] colors = getTankColors(isPlayer, playerNumber, enemyType, tank.getColorOverrideIndex());
        Color tankColor = colors[0];
        Color darkColor = colors[1];

        // Calculate track animation offset
        int trackOffset = (trackAnimationFrame / 4) % 2 == 0 ? 0 : (int)(3 * scale);

        // Draw tank with rotation
        gc.save();
        gc.translate(x + size / 2.0, y + size / 2.0);
        applyRotation(gc, direction);
        gc.translate(-size / 2.0, -size / 2.0);

        // Draw tracks
        renderTracks(gc, size, scale, trackOffset, darkColor);

        // Draw tank body
        gc.setFill(tankColor);
        gc.fillRect(6 * scale, 4 * scale, size - 12 * scale, size - 8 * scale);

        // Draw turret
        renderTurret(gc, size, scale, tankColor, darkColor);

        // Draw cannon
        renderCannon(gc, size, scale);

        // Draw enemy type markings
        if (!isPlayer) {
            renderEnemyMarkings(gc, enemyType, size, scale);
        }

        gc.restore();
    }

    private static void renderShields(GraphicsContext gc, Tank tank, double x, double y, int size, double scale) {
        // Draw shield if active (animated waving circle)
        if (tank.hasShield()) {
            long time = FrameTime.getFrameTime();

            for (int i = 0; i < 3; i++) {
                double phase = time / 150.0 + i * Math.PI * 2 / 3;
                double wave = Math.sin(phase) * 2 * scale;
                double breathe = Math.sin(time / 300.0) * 1.5 * scale;

                double alpha = 0.6 + 0.4 * Math.sin(phase + i);
                gc.setStroke(Color.color(0, 0.8 + 0.2 * Math.sin(phase), 1, alpha));
                gc.setLineWidth((2 - i * 0.5) * scale);

                double offset = 4 * scale + wave + breathe + i * 2 * scale;
                gc.strokeOval(x - offset, y - offset, size + offset * 2, size + offset * 2);
            }
        }

        // Draw pause shield (yellow/orange pulsing)
        if (tank.hasPauseShield()) {
            int pulse = (int) (FrameTime.getFrameTime() / 200) % 2;
            gc.setStroke(pulse == 0 ? Color.YELLOW : Color.ORANGE);
            gc.setLineWidth(3 * scale);
            gc.strokeOval(x - 6 * scale, y - 6 * scale, size + 12 * scale, size + 12 * scale);
        }
    }

    private static void renderShipIndicator(GraphicsContext gc, double x, double y, int size, double scale) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2 * scale);
        double centerX = x + size / 2;
        double topY = y - 6 * scale;
        double bottomY = y + size + 6 * scale;
        double leftX = x - 6 * scale;
        double rightX = x + size + 6 * scale;
        gc.strokePolygon(
            new double[]{leftX, rightX, centerX},
            new double[]{bottomY, bottomY, topY},
            3
        );
    }

    private static Color[] getTankColors(boolean isPlayer, int playerNumber, Tank.EnemyType enemyType, int colorOverrideIndex) {
        Color tankColor;
        Color darkColor;

        if (isPlayer) {
            tankColor = Tank.getPlayerColor(playerNumber);
            darkColor = tankColor.darker();
        } else {
            // Check for color override (enemy collected LIFE/STEEL powerup)
            if (colorOverrideIndex >= 0 && colorOverrideIndex < RAINBOW_COLORS.length) {
                tankColor = RAINBOW_COLORS[colorOverrideIndex];
                darkColor = tankColor.darker();
            } else {
                switch (enemyType) {
                    case REGULAR -> { tankColor = Color.RED; darkColor = Color.DARKRED; }
                    case ARMORED -> { tankColor = Color.DARKRED; darkColor = Color.rgb(80, 0, 0); }
                    case FAST -> { tankColor = Color.rgb(255, 100, 100); darkColor = Color.rgb(200, 60, 60); }
                    case POWER -> {
                        int frame = (int) (FrameTime.getFrameTime() / 100) % 7;
                        tankColor = RAINBOW_COLORS[frame];
                        darkColor = tankColor.darker();
                    }
                    case BOSS -> {
                        double pulse = (Math.sin(FrameTime.getFrameTime() / 150.0) + 1) / 2;
                        int red = (int) (150 + pulse * 105);
                        int green = (int) (pulse * 50);
                        tankColor = Color.rgb(red, green, 0);
                        darkColor = Color.rgb((int)(red * 0.6), 0, 0);
                    }
                    case HEAVY -> { tankColor = Color.DARKGRAY; darkColor = Color.BLACK; }
                    default -> { tankColor = Color.RED; darkColor = Color.DARKRED; }
                }
            }
        }

        // Reuse pre-allocated array to avoid allocation in hot path
        colorResult[0] = tankColor;
        colorResult[1] = darkColor;
        return colorResult;
    }

    private static void applyRotation(GraphicsContext gc, Direction direction) {
        switch (direction) {
            case UP -> gc.rotate(0);
            case RIGHT -> gc.rotate(90);
            case DOWN -> gc.rotate(180);
            case LEFT -> gc.rotate(270);
        }
    }

    private static void renderTracks(GraphicsContext gc, int size, double scale, int trackOffset, Color darkColor) {
        // Left track
        gc.setFill(darkColor);
        gc.fillRect(0, 0, 8 * scale, size);
        gc.setFill(Color.rgb(40, 40, 40));
        int trackCount = (int)(5 * scale);
        for (int i = 0; i < trackCount; i++) {
            int ty = (int)((i * 7 * scale + trackOffset) % size);
            gc.fillRect(1 * scale, ty, 6 * scale, 3 * scale);
        }

        // Right track
        gc.setFill(darkColor);
        gc.fillRect(size - 8 * scale, 0, 8 * scale, size);
        gc.setFill(Color.rgb(40, 40, 40));
        for (int i = 0; i < trackCount; i++) {
            int ty = (int)((i * 7 * scale + trackOffset) % size);
            gc.fillRect(size - 7 * scale, ty, 6 * scale, 3 * scale);
        }
    }

    private static void renderTurret(GraphicsContext gc, int size, double scale, Color tankColor, Color darkColor) {
        gc.setFill(darkColor);
        gc.fillOval(size / 2.0 - 7 * scale, size / 2.0 - 7 * scale, 14 * scale, 14 * scale);
        gc.setFill(tankColor);
        gc.fillOval(size / 2.0 - 5 * scale, size / 2.0 - 5 * scale, 10 * scale, 10 * scale);
    }

    private static void renderCannon(GraphicsContext gc, int size, double scale) {
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(size / 2.0 - 2 * scale, -2 * scale, 4 * scale, size / 2.0 + 2 * scale);
        gc.setFill(Color.GRAY);
        gc.fillRect(size / 2.0 - 1 * scale, -2 * scale, 2 * scale, size / 2.0);
    }

    private static void renderEnemyMarkings(GraphicsContext gc, Tank.EnemyType enemyType, int size, double scale) {
        switch (enemyType) {
            case ARMORED -> {
                gc.setFill(Color.GRAY);
                gc.fillRect(8 * scale, 6 * scale, size - 16 * scale, 3 * scale);
                gc.fillRect(8 * scale, size - 9 * scale, size - 16 * scale, 3 * scale);
            }
            case HEAVY -> {
                gc.setFill(Color.WHITE);
                gc.fillOval(size / 2.0 - 3 * scale, size / 2.0 - 3 * scale, 6 * scale, 6 * scale);
            }
            case FAST -> {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(scale);
                gc.strokeLine(10 * scale, size - 6 * scale, 14 * scale, size - 6 * scale);
                gc.strokeLine(size - 14 * scale, size - 6 * scale, size - 10 * scale, size - 6 * scale);
            }
            case BOSS -> {
                renderBossSkull(gc, size, scale);
            }
            // POWER has rainbow colors - no extra markings needed
        }
    }

    private static void renderBossSkull(GraphicsContext gc, int size, double scale) {
        double cx = size / 2.0;
        double cy = size / 2.0 - 2 * scale;

        // Skull (white oval)
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 8 * scale, cy - 6 * scale, 16 * scale, 14 * scale);

        // Eye sockets (black)
        gc.setFill(Color.BLACK);
        gc.fillOval(cx - 5 * scale, cy - 2 * scale, 4 * scale, 4 * scale);
        gc.fillOval(cx + 1 * scale, cy - 2 * scale, 4 * scale, 4 * scale);

        // Nose (black triangle)
        gc.fillPolygon(
            new double[]{cx - 1 * scale, cx + 1 * scale, cx},
            new double[]{cy + 3 * scale, cy + 3 * scale, cy + 5 * scale},
            3
        );

        // Teeth (white rectangles on black mouth)
        gc.setFill(Color.BLACK);
        gc.fillRect(cx - 4 * scale, cy + 5 * scale, 8 * scale, 3 * scale);
        gc.setFill(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            gc.fillRect(cx - 3.5 * scale + i * 2 * scale, cy + 5 * scale, 1.5 * scale, 3 * scale);
        }

        // Crossbones behind skull
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2 * scale);
        gc.strokeLine(cx - 12 * scale, cy - 8 * scale, cx + 12 * scale, cy + 12 * scale);
        gc.strokeLine(cx + 12 * scale, cy - 8 * scale, cx - 12 * scale, cy + 12 * scale);

        // Bone ends (small circles)
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 14 * scale, cy - 10 * scale, 4 * scale, 4 * scale);
        gc.fillOval(cx + 10 * scale, cy - 10 * scale, 4 * scale, 4 * scale);
        gc.fillOval(cx - 14 * scale, cy + 10 * scale, 4 * scale, 4 * scale);
        gc.fillOval(cx + 10 * scale, cy + 10 * scale, 4 * scale, 4 * scale);
    }
}
