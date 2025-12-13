package com.vibetanks.core;

import com.vibetanks.rendering.PowerUpRenderer;
import javafx.scene.canvas.GraphicsContext;

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

        // LASER is rare - 5% chance
        if (GameConstants.RANDOM.nextInt(100) < 5) {
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
        // Delegate to PowerUpRenderer for cleaner separation of concerns
        PowerUpRenderer.render(gc, x, y, type, lifetime);
    }

    public long getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Type getType() { return type; }
    public int getLifetime() { return lifetime; }
}
