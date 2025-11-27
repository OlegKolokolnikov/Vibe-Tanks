package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

public class PowerUp {
    private static final int SIZE = 24;
    private static final int LIFETIME = 600; // 10 seconds

    public enum Type {
        GUN,        // Ability to break iron/steel walls
        STAR,       // Shooting faster (stackable)
        CAR,        // Tank becomes faster (stackable)
        SHIP,       // Tank can swim through water
        SHOVEL,     // Base surrounded by steel for 1 minute
        SAW,        // Able to destroy forest/trees
        TANK,       // Extra life
        SHIELD,     // Shield for 1 minute (players) or extra life (enemies)
        MACHINEGUN  // Bullets can wrap through destroyed borders
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

    public PowerUp(double x, double y, Type type) {
        this.x = x;
        this.y = y;
        this.lifetime = LIFETIME;
        this.type = type;
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
            case GUN:
                tank.applyGun();
                break;
            case STAR:
                tank.applyStar();
                break;
            case CAR:
                tank.applyCar();
                break;
            case SHIP:
                tank.applyShip();
                break;
            case SHOVEL:
                // SHOVEL is handled in Game class (affects map, not tank)
                break;
            case SAW:
                tank.applySaw();
                break;
            case TANK:
                tank.applyTank();
                break;
            case SHIELD:
                tank.applyShield();
                break;
            case MACHINEGUN:
                tank.applyMachinegun();
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
            case GUN:
                // Draw gun icon - rectangle representing gun
                gc.fillRect(x + 6, y + 10, 4, 10);
                gc.fillRect(x + 10, y + 8, 8, 4);
                gc.fillRect(x + 14, y + 12, 6, 6);
                break;
            case STAR:
                // Draw star icon
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
            case CAR:
                // Draw car/tank icon
                gc.fillRect(x + 6, y + 10, 12, 8);
                gc.fillOval(x + 6, y + 16, 4, 4);
                gc.fillOval(x + 14, y + 16, 4, 4);
                gc.fillRect(x + 10, y + 6, 4, 4);
                break;
            case SHIP:
                // Draw ship/boat icon
                gc.fillPolygon(
                    new double[]{x + SIZE / 2, x + 4, x + SIZE - 4},
                    new double[]{y + 6, y + SIZE - 6, y + SIZE - 6},
                    3
                );
                gc.fillRect(x + SIZE / 2 - 2, y + 10, 4, 8);
                break;
            case SHOVEL:
                // Draw shovel icon
                gc.fillRect(x + SIZE / 2 - 2, y + 4, 4, 12);
                gc.fillPolygon(
                    new double[]{x + SIZE / 2 - 4, x + SIZE / 2 + 4, x + SIZE / 2},
                    new double[]{y + 14, y + 14, y + 20},
                    3
                );
                break;
            case SAW:
                // Draw saw icon (zigzag pattern)
                gc.fillOval(x + 4, y + 4, 16, 16);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double x1 = x + SIZE / 2 + 6 * Math.cos(angle);
                    double y1 = y + SIZE / 2 + 6 * Math.sin(angle);
                    double x2 = x + SIZE / 2 + 10 * Math.cos(angle);
                    double y2 = y + SIZE / 2 + 10 * Math.sin(angle);
                    gc.strokeLine(x1, y1, x2, y2);
                }
                break;
            case TANK:
                // Draw tank icon (helmet shape)
                gc.fillOval(x + 6, y + 8, 12, 12);
                gc.fillRect(x + 4, y + 12, 16, 6);
                break;
            case SHIELD:
                // Draw shield icon
                gc.fillOval(x + 6, y + 6, 12, 14);
                gc.setFill(Color.WHITE);
                gc.fillOval(x + 9, y + 10, 6, 6);
                break;
            case MACHINEGUN:
                // Draw machine gun icon (gun with bullets)
                gc.fillRect(x + 8, y + 10, 8, 4); // Gun barrel
                gc.fillRect(x + 6, y + 12, 4, 6); // Gun grip
                // Draw bullet stream
                gc.fillRect(x + 16, y + 11, 3, 2);
                gc.fillRect(x + 19, y + 11, 2, 2);
                break;
        }
    }

    private Color getTypeColor() {
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
        };
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Type getType() { return type; }
}
