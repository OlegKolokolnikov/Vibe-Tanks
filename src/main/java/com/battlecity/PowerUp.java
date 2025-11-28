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

        // White background with border
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, SIZE, SIZE);
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, SIZE, SIZE);

        // Draw icon based on type
        gc.setFill(getTypeColor());
        switch (type) {
            case GUN:
                // Draw bullet breaking through wall - represents breaking steel
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x + 2, y + 4, 6, 16); // Steel wall
                gc.setFill(Color.RED);
                gc.fillPolygon(
                    new double[]{x + 22, x + 10, x + 10},
                    new double[]{y + 12, y + 8, y + 16},
                    3
                ); // Bullet pointing at wall
                // Crack in wall
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeLine(x + 5, y + 6, x + 5, y + 18);
                break;
            case STAR:
                // Draw 5-pointed star - classic speed star
                double cx = x + SIZE / 2;
                double cy = y + SIZE / 2;
                double outerR = 10;
                double innerR = 4;
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
                    new double[]{x + 18, x + 8, x + 8},
                    new double[]{y + 12, y + 6, y + 18},
                    3
                );
                // Speed lines
                gc.fillRect(x + 3, y + 8, 6, 2);
                gc.fillRect(x + 4, y + 12, 5, 2);
                gc.fillRect(x + 3, y + 16, 6, 2);
                break;
            case SHIP:
                // Draw boat/ship on water waves
                gc.setFill(Color.BLUE);
                // Water waves
                gc.fillRect(x + 2, y + 16, 20, 4);
                gc.setFill(Color.CYAN);
                // Boat hull
                gc.fillPolygon(
                    new double[]{x + 4, x + 20, x + 18, x + 6},
                    new double[]{y + 14, y + 14, y + 18, y + 18},
                    4
                );
                // Sail
                gc.fillPolygon(
                    new double[]{x + 12, x + 12, x + 18},
                    new double[]{y + 4, y + 14, y + 14},
                    3
                );
                break;
            case SHOVEL:
                // Draw shovel with dirt - represents fortifying base
                gc.setFill(Color.ORANGE);
                // Shovel handle
                gc.fillRect(x + 10, y + 2, 4, 12);
                // Shovel blade
                gc.fillPolygon(
                    new double[]{x + 6, x + 18, x + 16, x + 8},
                    new double[]{y + 14, y + 14, y + 22, y + 22},
                    4
                );
                // Dirt/steel color accent
                gc.setFill(Color.GRAY);
                gc.fillRect(x + 8, y + 16, 8, 4);
                break;
            case SAW:
                // Draw circular saw blade - represents cutting trees
                gc.setFill(Color.BROWN);
                gc.fillOval(x + 4, y + 4, 16, 16);
                // Saw teeth
                gc.setFill(Color.DARKGRAY);
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double tx = x + SIZE / 2 + 7 * Math.cos(angle);
                    double ty = y + SIZE / 2 + 7 * Math.sin(angle);
                    gc.fillRect(tx - 2, ty - 2, 4, 4);
                }
                // Center hole
                gc.setFill(Color.WHITE);
                gc.fillOval(x + 9, y + 9, 6, 6);
                break;
            case TANK:
                // Draw +1 with tank silhouette - represents extra life
                gc.setFill(Color.GREEN);
                // Mini tank body
                gc.fillRect(x + 4, y + 12, 10, 8);
                gc.fillRect(x + 7, y + 8, 4, 6); // Turret
                // +1 text
                gc.setFill(Color.DARKGREEN);
                gc.fillRect(x + 16, y + 6, 2, 8); // Vertical of +
                gc.fillRect(x + 14, y + 9, 6, 2); // Horizontal of +
                gc.fillRect(x + 16, y + 16, 2, 6); // 1
                break;
            case SHIELD:
                // Draw shield shape with emblem
                gc.setFill(Color.BLUE);
                // Shield outline
                gc.fillPolygon(
                    new double[]{x + 12, x + 4, x + 4, x + 12, x + 20, x + 20},
                    new double[]{y + 22, y + 16, y + 4, y + 2, y + 4, y + 16},
                    6
                );
                // Inner shield highlight
                gc.setFill(Color.LIGHTBLUE);
                gc.fillPolygon(
                    new double[]{x + 12, x + 7, x + 7, x + 12, x + 17, x + 17},
                    new double[]{y + 18, y + 14, y + 6, y + 5, y + 6, y + 14},
                    6
                );
                break;
            case MACHINEGUN:
                // Draw rapid fire bullets - represents fast shooting
                gc.setFill(Color.PURPLE);
                // Gun barrel
                gc.fillRect(x + 2, y + 10, 10, 4);
                gc.fillRect(x + 4, y + 14, 4, 4); // Grip
                // Multiple bullets flying
                gc.setFill(Color.YELLOW);
                gc.fillOval(x + 13, y + 10, 4, 4);
                gc.fillOval(x + 17, y + 8, 3, 3);
                gc.fillOval(x + 17, y + 13, 3, 3);
                gc.fillOval(x + 20, y + 10, 2, 2);
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
