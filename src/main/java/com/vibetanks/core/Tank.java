package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.rendering.TankRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.vibetanks.util.GameLogger;

import java.util.List;

public class Tank {
    private static final GameLogger LOG = GameLogger.getLogger(Tank.class);
    public enum EnemyType {
        REGULAR,    // 1 shot, normal speed
        ARMORED,    // 2 shots, normal speed, bigger
        FAST,       // 1 shot, faster
        POWER,      // 2 shots, drops power-up on each hit, rainbow colors
        HEAVY,      // 3 shots, fast, black with white dot
        BOSS        // 12 shots, fast, black, 4x size, can destroy iron
    }

    public static final int BASE_SIZE = 28;
    private int size = BASE_SIZE; // Instance variable for tank size
    private static final double SPEED = 2.0;
    private static final double TARGET_FPS = 60.0; // Game is designed for 60 FPS
    private static final int SHOOT_COOLDOWN = 30; // frames

    private double x;
    private double y;
    private Direction direction;
    private boolean isPlayer;
    private int playerNumber; // 1 or 2
    private EnemyType enemyType; // For enemy tanks
    private boolean alive;
    private int lives;
    private int health;
    private int maxHealth;

    private int shootCooldown;
    private int activeBulletCount; // Track how many bullets this tank has active
    private boolean hasShield;
    private int shieldDuration;
    private boolean hasPauseShield; // Shield while player is paused (multiplayer)
    private double speedMultiplier; // Base is 1.0, each CAR adds 0.3
    private double tempSpeedBoost; // Temporary speed boost from team CAR pickup
    private int bulletPower; // 1 = normal, 2 = can break steel
    private boolean canSwim; // SHIP power-up
    private boolean canDestroyTrees; // SAW power-up
    private int machinegunCount; // MACHINEGUN power-up (each adds one extra bullet, max 5)
    private int shootCooldownReduction; // STAR power-up (each star reduces cooldown)
    private int laserDuration; // LASER power-up duration (30 seconds = 1800 frames at 60 FPS)
    private static final int LASER_COOLDOWN = 10; // Very fast shooting (6 shots per second)

    // Extracted components for better separation of concerns
    private final TankPhysics physics;
    private TankAI ai; // Only initialized for enemy tanks

    // Respawn delay (1 second = 60 frames at 60 FPS)
    private int respawnTimer = 0;
    private double pendingRespawnX, pendingRespawnY;
    private static final int RESPAWN_DELAY = 60; // 1 second

    // Track animation
    private int trackAnimationFrame;
    private boolean isMoving;

    public Tank(double x, double y, Direction direction, boolean isPlayer, int playerNumber) {
        this(x, y, direction, isPlayer, playerNumber, EnemyType.REGULAR);
    }

    public Tank(double x, double y, Direction direction, boolean isPlayer, int playerNumber, EnemyType enemyType) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.isPlayer = isPlayer;
        this.playerNumber = playerNumber;
        this.enemyType = enemyType;
        this.alive = true;
        this.lives = isPlayer ? 3 : 1;
        this.speedMultiplier = 1.0; // Default speed for all tanks

        // Initialize physics component
        this.physics = new TankPhysics();

        // Initialize AI for enemy tanks only
        if (!isPlayer) {
            this.ai = new TankAI(x, y);
        }

        // Set health and speed based on enemy type
        if (!isPlayer) {
            switch (enemyType) {
                case REGULAR -> this.maxHealth = 1;
                case ARMORED -> this.maxHealth = 2;
                case FAST -> {
                    this.maxHealth = 1;
                    this.speedMultiplier = 1.5;
                }
                case POWER -> this.maxHealth = 2;
                case HEAVY -> {
                    this.maxHealth = 3;
                    this.speedMultiplier = 1.5;
                    this.bulletPower = 2; // Can destroy iron/steel
                }
                case BOSS -> {
                    this.maxHealth = 12;
                    this.speedMultiplier = 1.5;
                    this.bulletPower = 2; // Can destroy iron/steel
                    this.size = BASE_SIZE * 4; // 4x bigger
                    this.canSwim = true; // BOSS can swim without SHIP
                    this.canDestroyTrees = true; // BOSS destroys everything
                }
            }
        } else {
            this.maxHealth = 1;
        }

        this.health = maxHealth;
        this.shootCooldown = 0;
        this.activeBulletCount = 0;
        this.hasShield = isPlayer; // Players start with shield
        this.shieldDuration = isPlayer ? 180 : 0; // 3 seconds
        if (isPlayer) {
            this.speedMultiplier = 1.0;
            this.bulletPower = 1;
            this.canSwim = false;
            this.canDestroyTrees = false;
        } else if (enemyType != EnemyType.BOSS && enemyType != EnemyType.HEAVY) {
            // Only reset these for non-BOSS/HEAVY enemies (BOSS and HEAVY set these in switch)
            this.bulletPower = 1;
            this.canSwim = false;
            this.canDestroyTrees = false;
        }
        this.machinegunCount = 0;
        this.shootCooldownReduction = 0;
    }

    public void update(GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base) {
        if (!alive) return;

        // Update cooldowns
        if (shootCooldown > 0) shootCooldown--;
        if (shieldDuration > 0) {
            shieldDuration--;
            if (shieldDuration == 0) hasShield = false;
        }
        if (laserDuration > 0) {
            laserDuration--;
        }

        // Handle ice sliding via physics component
        physics.updateSliding(this, map, allTanks, base);
    }

    public void move(Direction newDirection, GameMap map, List<Tank> otherTanks, Base base) {
        if (!alive) return;

        this.direction = newDirection;
        boolean moved = physics.move(this, direction, map, otherTanks, base);
        if (moved) {
            isMoving = true;
            trackAnimationFrame++;
        }
    }

    public void startSliding(Direction direction, GameMap map) {
        physics.startSliding(direction, map, x, y, size);
    }

    public boolean isOnIce(GameMap map) {
        return physics.isOnIce(map, x, y, size);
    }

    public void shoot(List<Bullet> bullets, SoundManager soundManager) {
        // Can shoot if: cooldown is 0 OR no active bullets from this tank
        if (!alive || (shootCooldown > 0 && activeBulletCount > 0)) return;

        // BOSS tanks have bigger bullets (4x size = 32 pixels)
        int bulletSize = (enemyType == EnemyType.BOSS) ? 32 : 8;

        double bulletX = x + size / 2.0 - bulletSize / 2.0;
        double bulletY = y + size / 2.0 - bulletSize / 2.0;

        // Adjust bullet spawn position based on direction
        switch (direction) {
            case UP -> bulletY = y - bulletSize;
            case DOWN -> bulletY = y + size;
            case LEFT -> bulletX = x - bulletSize;
            case RIGHT -> bulletX = x + size;
        }

        // Calculate number of bullets to fire (1 base + machinegunCount, max 5)
        int bulletCount = Math.min(1 + machinegunCount, 5);
        double bulletSpacing = bulletSize * 3.0; // 3 bullets' worth of space

        // Fire bullets with spacing
        for (int i = 0; i < bulletCount; i++) {
            double offsetX = bulletX;
            double offsetY = bulletY;

            // Add spacing between bullets based on direction
            double totalSpacing = i * bulletSpacing;
            switch (direction) {
                case UP -> offsetY -= totalSpacing;
                case DOWN -> offsetY += totalSpacing;
                case LEFT -> offsetX -= totalSpacing;
                case RIGHT -> offsetX += totalSpacing;
            }

            bullets.add(new Bullet(offsetX, offsetY, direction, !isPlayer, bulletPower, canDestroyTrees, isPlayer ? playerNumber : 0, bulletSize));
            activeBulletCount++;
        }

        // Apply shoot cooldown reduction from STAR power-ups (min cooldown is 5 frames)
        int baseCooldown = Math.max(5, SHOOT_COOLDOWN - (shootCooldownReduction * 5));
        // Apply global shoot speed setting (higher speed = lower cooldown)
        double shootSpeedMult = isPlayer ? GameSettings.getPlayerShootSpeedMultiplier() : GameSettings.getEnemyShootSpeedMultiplier();
        shootCooldown = Math.max(3, (int)(baseCooldown / shootSpeedMult));
        soundManager.playShoot();
    }

    // Called when a bullet from this tank is destroyed
    public void bulletDestroyed() {
        if (activeBulletCount > 0) {
            activeBulletCount--;
        }
    }

    /**
     * Shoot a laser beam (when tank has LASER power-up active)
     * @return the laser beam or null if can't shoot yet
     */
    public Laser shootLaser(SoundManager soundManager) {
        if (!alive || laserDuration <= 0) return null;

        // Laser has very fast cooldown
        if (shootCooldown > 0) return null;

        // Calculate laser start position (front of tank)
        double laserX = x + size / 2.0;
        double laserY = y + size / 2.0;

        // Adjust position based on direction
        switch (direction) {
            case UP -> laserY = y;
            case DOWN -> laserY = y + size;
            case LEFT -> laserX = x;
            case RIGHT -> laserX = x + size;
        }

        // Set fast cooldown for laser
        shootCooldown = LASER_COOLDOWN;
        soundManager.playLaser();

        return new Laser(laserX, laserY, direction, !isPlayer, isPlayer ? playerNumber : 0);
    }

    public void updateAI(GameMap map, List<Bullet> bullets, List<Tank> allTanks, Base base, SoundManager soundManager) {
        if (!alive || ai == null) return;

        // Update cooldowns and sliding first
        update(map, bullets, soundManager, allTanks, base);

        // Delegate AI behavior to TankAI component
        ai.update(this, physics, map, bullets, allTanks, base, soundManager);
    }

    // Helper method for TankAI to increment track animation
    public void incrementTrackAnimation() {
        isMoving = true;
        trackAnimationFrame++;
    }

    public boolean damage() {
        if (!alive || hasShield || hasPauseShield) return false;

        // SHIP acts as one extra shot protection
        if (canSwim) {
            canSwim = false; // Lose SHIP ability but take no damage
            return false;
        }

        health--;
        boolean dropPowerUp = false;

        // POWER type drops power-up on EVERY hit (including death)
        if (!isPlayer && enemyType == EnemyType.POWER) {
            dropPowerUp = true;
        }

        if (health <= 0) {
            lives--;
            alive = false; // Tank dies and needs to respawn
        }

        return dropPowerUp;
    }

    // Instant kill - used by BOSS tank contact, ignores all shields
    public void instantKill() {
        health = 0;
        lives--;
        alive = false;
        // Remove all shields
        hasShield = false;
        hasPauseShield = false;
        LOG.info("Tank instantly killed by BOSS contact!");
    }

    public void render(GraphicsContext gc) {
        TankRenderer.render(gc, this);
        // Reset moving flag (will be set again if tank moves next frame)
        isMoving = false;
    }

    public boolean collidesWith(double otherX, double otherY, int otherSize) {
        return Collider.checkSquare(x, y, size, otherX, otherY, otherSize);
    }

    // Get player color based on player number
    public static Color getPlayerColor(int playerNum) {
        return switch (playerNum) {
            case 1 -> Color.YELLOW;
            case 2 -> Color.LIME;
            case 3 -> Color.CYAN;
            case 4 -> Color.ORANGE;
            default -> Color.WHITE;
        };
    }

    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public boolean isPlayer() { return isPlayer; }
    public int getPlayerNumber() { return playerNumber; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }
    public void addLife() { this.lives++; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public void setHealth(int health) { this.health = health; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public boolean hasShield() { return hasShield; }
    public boolean hasPauseShield() { return hasPauseShield; }
    public void setPauseShield(boolean pauseShield) { this.hasPauseShield = pauseShield; }
    public Direction getDirection() { return direction; }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    // Setters for network sync
    public void setShield(boolean shield) {
        this.hasShield = shield;
        if (!shield) {
            this.shieldDuration = 0;
        }
    }

    public void setShieldWithDuration(boolean shield, int duration) {
        this.hasShield = shield;
        this.shieldDuration = shield ? duration : 0;
    }

    public int getShieldDuration() {
        return shieldDuration;
    }

    public void setShip(boolean ship) {
        this.canSwim = ship;
    }

    // Setters for network sync of power-ups
    public void setGun(boolean hasGun) {
        this.bulletPower = hasGun ? 2 : 1;
    }

    public void setStarCount(int count) {
        this.shootCooldownReduction = count;
    }

    public void setCarCount(int count) {
        this.speedMultiplier = 1.0 + (count * 0.3);
    }

    public void setSaw(boolean hasSaw) {
        this.canDestroyTrees = hasSaw;
    }

    public void setMachinegunCount(int count) {
        this.machinegunCount = count;
    }

    public EnemyType getEnemyType() { return enemyType; }
    public void setEnemyType(EnemyType type) {
        this.enemyType = type;
        // Update size for BOSS tanks (needed for network sync)
        if (type == EnemyType.BOSS) {
            this.size = BASE_SIZE * 4;
        } else {
            this.size = BASE_SIZE;
        }
    }

    // Power-up status getters for UI display
    public boolean hasGun() { return bulletPower >= 2; }
    public int getStarCount() { return shootCooldownReduction; }
    public int getCarCount() {
        // Calculate CAR count based on speed multiplier (base 1.0, each adds 0.3)
        return (int)((speedMultiplier - 1.0) / 0.3);
    }
    public boolean hasShip() { return canSwim; }
    public boolean hasSaw() { return canDestroyTrees; }
    public int getMachinegunCount() { return machinegunCount; }

    // Power-up effects
    public void applyGun() {
        bulletPower = 2; // Can break steel walls
    }

    public void applyStar() {
        shootCooldownReduction++; // Each star reduces cooldown by 5 frames
    }

    public void applyCar() {
        speedMultiplier += 0.3; // Each car increases speed by 30%
        speedMultiplier = Math.min(speedMultiplier, 2.5); // Cap at 2.5x speed
    }

    public void applyTempSpeedBoost(double boost) {
        tempSpeedBoost = boost;
    }

    public void removeTempSpeedBoost() {
        tempSpeedBoost = 0;
    }

    public double getTempSpeedBoost() {
        return tempSpeedBoost;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
    }

    public void applyShip() {
        canSwim = true; // Can move through water
    }

    public void applySaw() {
        canDestroyTrees = true; // Bullets can destroy trees
    }

    public void applyMachinegun() {
        // Each MACHINEGUN adds one extra bullet (max 5 total bullets = 4 extra)
        if (machinegunCount < 4) {
            machinegunCount++;
        }
    }

    public void applyTank() {
        lives++; // Extra life
    }

    public void applyShield() {
        if (isPlayer) {
            // Players get shield for 1 minute
            hasShield = true;
            shieldDuration = GameConstants.SHIELD_DURATION;
        } else {
            // Enemies get extra life
            lives++;
        }
    }

    public void applyLaser() {
        // LASER power-up: shoot laser beams for 30 seconds
        laserDuration = GameConstants.LASER_DURATION;
    }

    public boolean hasLaser() {
        return laserDuration > 0;
    }

    public int getLaserDuration() {
        return laserDuration;
    }

    public void setLaserDuration(int duration) {
        this.laserDuration = duration;
    }

    public boolean canSwim() {
        return canSwim;
    }

    public int getTrackAnimationFrame() {
        return trackAnimationFrame;
    }

    public boolean canDestroyTrees() {
        return canDestroyTrees;
    }

    public void respawn(double newX, double newY) {
        // Start respawn timer instead of immediately respawning
        this.pendingRespawnX = newX;
        this.pendingRespawnY = newY;
        this.respawnTimer = RESPAWN_DELAY;
    }

    // Immediate spawn without delay (for level start)
    public void spawnImmediate(double newX, double newY) {
        this.pendingRespawnX = newX;
        this.pendingRespawnY = newY;
        this.respawnTimer = 0;
        completeRespawn();
    }

    // Called each frame to update respawn timer
    public void updateRespawnTimer() {
        if (respawnTimer > 0) {
            respawnTimer--;
            if (respawnTimer == 0) {
                // Actually respawn now
                completeRespawn();
            }
        }
    }

    // Check if tank is waiting to respawn
    public boolean isWaitingToRespawn() {
        return respawnTimer > 0;
    }

    // Get respawn timer for network sync
    public int getRespawnTimer() {
        return respawnTimer;
    }

    // Set respawn timer (for network sync)
    public void setRespawnTimer(int timer) {
        this.respawnTimer = timer;
    }

    // Get pending respawn position (for network sync)
    public double getPendingRespawnX() {
        return pendingRespawnX;
    }

    public double getPendingRespawnY() {
        return pendingRespawnY;
    }

    // Set pending respawn position (for network sync)
    public void setPendingRespawn(double x, double y, int timer) {
        this.pendingRespawnX = x;
        this.pendingRespawnY = y;
        this.respawnTimer = timer;
    }

    private void completeRespawn() {
        this.x = pendingRespawnX;
        this.y = pendingRespawnY;
        this.direction = Direction.UP;
        this.health = maxHealth;
        this.hasShield = true;
        this.shieldDuration = GameConstants.TEMPORARY_SHIELD_DURATION;
        this.alive = true;

        // Clear all power-ups on respawn
        this.bulletPower = 1;
        this.shootCooldownReduction = 0;
        this.speedMultiplier = 1.0;
        this.canSwim = false;
        this.canDestroyTrees = false;
        this.machinegunCount = 0;
        this.laserDuration = 0; // Reset laser power-up
        this.tempSpeedBoost = 0; // Reset temporary speed boost
    }

    public void giveTemporaryShield() {
        this.hasShield = true;
        this.shieldDuration = GameConstants.TEMPORARY_SHIELD_DURATION;
    }

    public void setPosition(double x, double y) {
        // Animate tracks if position changed (for network sync)
        if (this.x != x || this.y != y) {
            trackAnimationFrame++;
            isMoving = true;
        }
        this.x = x;
        this.y = y;
    }
}
