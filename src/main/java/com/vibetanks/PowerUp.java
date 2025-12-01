package com.vibetanks;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

public class PowerUp {
    private static final int SIZE = 32; // Same size as tank
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
        MACHINEGUN, // Bullets can wrap through destroyed borders
        FREEZE,     // Freeze enemies for 10 seconds (or freeze players if enemy takes it)
        BOMB        // Explode all enemies (or all players if enemy takes it)
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
            case FREEZE:
                // FREEZE is handled in Game class (affects all enemies or players)
                break;
            case BOMB:
                // BOMB is handled in Game class (affects all enemies or players)
                break;
        }
    }

    public void render(GraphicsContext gc) {
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

        // Draw icon based on type (scaled for SIZE=32)
        gc.setFill(getTypeColor());
        switch (type) {
            case GUN:
                // Draw bullet breaking through wall - represents breaking steel
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x + 3, y + 5, 8, 22); // Steel wall
                gc.setFill(Color.RED);
                gc.fillPolygon(
                    new double[]{x + 29, x + 13, x + 13},
                    new double[]{y + 16, y + 10, y + 22},
                    3
                ); // Bullet pointing at wall
                // Crack in wall
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(2);
                gc.strokeLine(x + 7, y + 8, x + 7, y + 24);
                break;
            case STAR:
                // Draw 5-pointed star - classic speed star
                double cx = x + SIZE / 2;
                double cy = y + SIZE / 2;
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
                gc.fillPolygon(starX, starY, 10);
                break;
            case CAR:
                // Draw speed lines with arrow - represents speed boost
                gc.setFill(Color.LIME);
                // Arrow pointing right
                gc.fillPolygon(
                    new double[]{x + 24, x + 10, x + 10},
                    new double[]{y + 16, y + 8, y + 24},
                    3
                );
                // Speed lines
                gc.fillRect(x + 4, y + 10, 8, 3);
                gc.fillRect(x + 5, y + 15, 7, 3);
                gc.fillRect(x + 4, y + 20, 8, 3);
                break;
            case SHIP:
                // Draw boat/ship on water waves
                gc.setFill(Color.BLUE);
                // Water waves
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
                break;
            case SHOVEL:
                // Draw shovel with dirt - represents fortifying base
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
                break;
            case SAW:
                // Draw circular saw blade - represents cutting trees
                gc.setFill(Color.BROWN);
                gc.fillOval(x + 5, y + 5, 22, 22);
                // Saw teeth
                gc.setFill(Color.DARKGRAY);
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double tx = x + SIZE / 2 + 10 * Math.cos(angle);
                    double ty = y + SIZE / 2 + 10 * Math.sin(angle);
                    gc.fillRect(tx - 3, ty - 3, 6, 6);
                }
                // Center hole
                gc.setFill(Color.WHITE);
                gc.fillOval(x + 12, y + 12, 8, 8);
                break;
            case TANK:
                // Draw +1 with tank silhouette - represents extra life
                gc.setFill(Color.GREEN);
                // Mini tank body
                gc.fillRect(x + 5, y + 16, 14, 11);
                gc.fillRect(x + 9, y + 10, 6, 8); // Turret
                // +1 text
                gc.setFill(Color.DARKGREEN);
                gc.fillRect(x + 21, y + 8, 3, 11); // Vertical of +
                gc.fillRect(x + 18, y + 12, 9, 3); // Horizontal of +
                gc.fillRect(x + 21, y + 21, 3, 8); // 1
                break;
            case SHIELD:
                // Draw shield shape with emblem
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
                break;
            case MACHINEGUN:
                // Draw rapid fire bullets - represents fast shooting
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
                break;
            case FREEZE:
                // Draw snowflake/ice crystal - represents freezing
                gc.setFill(Color.LIGHTBLUE);
                gc.fillOval(x + 6, y + 6, 20, 20); // Ice background
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                // Snowflake lines
                double scx = x + SIZE / 2;
                double scy = y + SIZE / 2;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * i) / 3;
                    gc.strokeLine(scx, scy, scx + 12 * Math.cos(angle), scy + 12 * Math.sin(angle));
                }
                // Center dot
                gc.setFill(Color.CYAN);
                gc.fillOval(scx - 4, scy - 4, 8, 8);
                break;
            case BOMB:
                // Draw bomb with fuse - represents explosion
                gc.setFill(Color.BLACK);
                gc.fillOval(x + 5, y + 10, 20, 20); // Bomb body
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
            case FREEZE -> Color.LIGHTBLUE;
            case BOMB -> Color.BLACK;
        };
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Type getType() { return type; }
}
