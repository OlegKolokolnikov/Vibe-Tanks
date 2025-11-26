package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

public class PowerUp {
    private static final int SIZE = 24;
    private static final int LIFETIME = 600; // 10 seconds

    public enum Type {
        SHIELD,   // Temporary invincibility
        SPEED,    // Increased movement speed
        POWER,    // More powerful bullets
        LIFE      // Extra life
    }

    private double x;
    private double y;
    private Type type;
    private int lifetime;

    public PowerUp(double x, double y) {
        this.x = x;
        this.y = y;
        this.lifetime = LIFETIME;

        // Randomly choose power-up type
        Random random = new Random();
        Type[] types = Type.values();
        this.type = types[random.nextInt(types.length)];
    }

    public void update() {
        lifetime--;
    }

    public boolean isExpired() {
        return lifetime <= 0;
    }

    public boolean collidesWith(Tank tank) {
        return x < tank.getX() + tank.getSize() &&
               x + SIZE > tank.getX() &&
               y < tank.getY() + tank.getSize() &&
               y + SIZE > tank.getY();
    }

    public void applyEffect(Tank tank) {
        switch (type) {
            case SHIELD:
                tank.applyShield();
                break;
            case SPEED:
                tank.applySpeed();
                break;
            case POWER:
                tank.applyPower();
                break;
            case LIFE:
                tank.addLife();
                break;
        }
    }

    public void render(GraphicsContext gc) {
        // Flashing effect when about to expire
        if (lifetime < 120 && lifetime % 20 < 10) {
            return;
        }

        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, SIZE, SIZE);

        // Draw icon based on type
        gc.setFill(getTypeColor());
        switch (type) {
            case SHIELD:
                // Draw shield icon
                gc.fillOval(x + 4, y + 4, SIZE - 8, SIZE - 8);
                gc.setStroke(Color.CYAN);
                gc.setLineWidth(2);
                gc.strokeOval(x + 4, y + 4, SIZE - 8, SIZE - 8);
                break;
            case SPEED:
                // Draw speed icon (arrow)
                gc.fillPolygon(
                    new double[]{x + SIZE / 2, x + SIZE - 4, x + 4},
                    new double[]{y + 4, y + SIZE / 2, y + SIZE / 2},
                    3
                );
                break;
            case POWER:
                // Draw power icon (star)
                double centerX = x + SIZE / 2;
                double centerY = y + SIZE / 2;
                double[] xPoints = new double[5];
                double[] yPoints = new double[5];
                for (int i = 0; i < 5; i++) {
                    double angle = Math.PI / 2 + (2 * Math.PI * i / 5);
                    xPoints[i] = centerX + 8 * Math.cos(angle);
                    yPoints[i] = centerY - 8 * Math.sin(angle);
                }
                gc.fillPolygon(xPoints, yPoints, 5);
                break;
            case LIFE:
                // Draw life icon (heart)
                gc.fillText("+1", x + 6, y + SIZE / 2 + 4);
                break;
        }
    }

    private Color getTypeColor() {
        return switch (type) {
            case SHIELD -> Color.CYAN;
            case SPEED -> Color.LIME;
            case POWER -> Color.ORANGE;
            case LIFE -> Color.RED;
        };
    }

    public Type getType() { return type; }
}
