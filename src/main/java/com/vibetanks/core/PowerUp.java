package com.vibetanks.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class PowerUp {
    private static final int SIZE = 32; // Same size as tank
    private static final int LIFETIME = GameConstants.POWERUP_LIFETIME;
    private static long nextId = 1;

    /**
     * Reset power-up ID counter. Call this at level start/restart to prevent overflow.
     */
    public static void resetIdCounter() {
        nextId = 1;
    }

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
        BOMB,       // Explode all enemies (or all players if enemy takes it)
        LASER       // Shoot laser beam that passes through obstacles, deals 3 damage (30 seconds, rare)
    }

    private long id;
    private double x;
    private double y;
    private Type type;
    private int lifetime;

    public PowerUp(double x, double y) {
        this.id = nextId++;
        this.x = x;
        this.y = y;

        // Calculate lifetime based on difficulty mode (60 frames = 1 second)
        if (GameSettings.isHardModeActive()) {
            // Hard Mode: 10 seconds
            this.lifetime = 600;
        } else if (GameSettings.isVeryEasyModeActiveForCurrentLevel()) {
            // Very Easy Mode: 25 seconds
            this.lifetime = 1500;
        } else if (GameSettings.isEasyModeActive(GameSettings.getCurrentLevel())) {
            // Easy Mode: 20 seconds
            this.lifetime = 1200;
        } else {
            // Normal Mode: 15 seconds
            this.lifetime = 900;
        }

        // Very Easy Mode: increased LASER (20%) and SHOVEL (20%) spawn chances
        if (GameSettings.isVeryEasyModeActiveForCurrentLevel()) {
            int roll = GameConstants.RANDOM.nextInt(100);
            if (roll < 20) {
                // 20% chance for LASER (up from 5%)
                this.type = Type.LASER;
            } else if (roll < 40) {
                // 20% chance for SHOVEL (up from ~9%)
                this.type = Type.SHOVEL;
            } else {
                // 60% chance for other power-ups (excluding LASER and SHOVEL)
                Type[] types = Type.values();
                Type chosen;
                do {
                    int index = GameConstants.RANDOM.nextInt(types.length);
                    chosen = types[index];
                } while (chosen == Type.LASER || chosen == Type.SHOVEL);
                this.type = chosen;
            }
        }
        // Normal mode: LASER is rare - 5% chance
        else if (GameConstants.RANDOM.nextInt(100) < 5) {
            // 5% chance for LASER
            this.type = Type.LASER;
        } else {
            // 95% chance for other power-ups (excluding LASER)
            Type[] types = Type.values();
            int index = GameConstants.RANDOM.nextInt(types.length - 1); // Exclude LASER (last item)
            this.type = types[index];
        }
    }

    public PowerUp(double x, double y, Type type) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.lifetime = LIFETIME;
        this.type = type;
    }

    /**
     * Constructor with explicit ID (for network sync).
     */
    public PowerUp(long id, double x, double y, Type type, int lifetime) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.lifetime = lifetime;
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
            case LASER:
                tank.applyLaser();
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
                // Draw a bullet hitting and cracking steel wall
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
                break;
            case STAR:
                // Draw 5-pointed star - orange with red border
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
                // Red border (draw larger star behind)
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
                break;
            case CAR:
                // Draw a lightning bolt - universal speed symbol
                gc.setFill(Color.YELLOW);
                // Lightning bolt shape
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
            case LASER:
                // Draw laser beam - red beam with glow
                // Laser emitter/gun
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x + 3, y + 12, 10, 8);
                gc.setFill(Color.GRAY);
                gc.fillRect(x + 5, y + 14, 6, 4);
                // Laser beam (red with glow effect)
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
            case LASER -> Color.RED;
        };
    }

    public long getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Type getType() { return type; }
    public int getLifetime() { return lifetime; }
}
