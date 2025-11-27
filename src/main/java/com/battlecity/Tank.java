package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

public class Tank {
    public enum EnemyType {
        REGULAR,    // 1 shot, normal speed
        ARMORED,    // 2 shots, normal speed, bigger
        FAST,       // 1 shot, faster
        POWER,      // 2 shots, drops power-up on each hit, rainbow colors
        HEAVY       // 3 shots, fast, black with white dot
    }

    private static final int SIZE = 28;
    private static final double SPEED = 2.0;
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
    private boolean hasShield;
    private int shieldDuration;
    private double speedMultiplier; // Base is 1.0, each CAR adds 0.3
    private int bulletPower; // 1 = normal, 2 = can break steel
    private boolean canSwim; // SHIP power-up
    private boolean canDestroyTrees; // SAW power-up
    private int machinegunCount; // MACHINEGUN power-up (each adds one extra bullet, max 5)
    private int shootCooldownReduction; // STAR power-up (each star reduces cooldown)

    private Random random;

    // AI variables
    private int aiMoveCooldown;
    private int aiShootCooldown;
    private double lastX, lastY; // Track position to detect stuck
    private int stuckCounter; // Count frames stuck

    // Ice sliding variables
    private boolean isSliding;
    private Direction slidingDirection;
    private double slideDistance;
    private static final double SLIDE_DISTANCE = 32.0; // One tile

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
                }
            }
        } else {
            this.maxHealth = 1;
        }

        this.health = maxHealth;
        this.shootCooldown = 0;
        this.hasShield = isPlayer; // Players start with shield
        this.shieldDuration = isPlayer ? 180 : 0; // 3 seconds
        if (isPlayer) {
            this.speedMultiplier = 1.0;
        }
        this.bulletPower = 1;
        this.canSwim = false;
        this.canDestroyTrees = false;
        this.machinegunCount = 0;
        this.shootCooldownReduction = 0;
        this.random = new Random();
        this.aiMoveCooldown = 60;
        this.aiShootCooldown = 90;
        this.lastX = x;
        this.lastY = y;
        this.stuckCounter = 0;
        this.isSliding = false;
        this.slideDistance = 0;
    }

    public void update(GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base) {
        if (!alive) return;

        // Update cooldowns
        if (shootCooldown > 0) shootCooldown--;
        if (shieldDuration > 0) {
            shieldDuration--;
            if (shieldDuration == 0) hasShield = false;
        }

        // Handle ice sliding
        if (isSliding && slideDistance > 0) {
            double slideSpeed = SPEED * speedMultiplier * 2.0; // Same speed as moving on ice
            double slideStep = Math.min(slideSpeed, slideDistance);

            double newX = x + slidingDirection.getDx() * slideStep;
            double newY = y + slidingDirection.getDy() * slideStep;

            // Check boundaries and handle wraparound during sliding
            int mapWidth = map.getWidth() * 32;
            int mapHeight = map.getHeight() * 32;
            boolean canSlide = true;

            // Left edge wraparound
            if (newX < 0) {
                int row = (int)((y + SIZE/2) / 32);
                if (map.getTile(row, 0) == GameMap.TileType.EMPTY) {
                    newX = mapWidth - SIZE;
                } else {
                    canSlide = false;
                }
            }

            // Right edge wraparound
            if (newX + SIZE > mapWidth) {
                int row = (int)((y + SIZE/2) / 32);
                if (map.getTile(row, map.getWidth() - 1) == GameMap.TileType.EMPTY) {
                    newX = 0;
                } else {
                    canSlide = false;
                }
            }

            // Top edge wraparound
            if (newY < 0) {
                int col = (int)((x + SIZE/2) / 32);
                if (map.getTile(0, col) == GameMap.TileType.EMPTY) {
                    newY = mapHeight - SIZE;
                } else {
                    canSlide = false;
                }
            }

            // Bottom edge wraparound
            if (newY + SIZE > mapHeight) {
                int col = (int)((x + SIZE/2) / 32);
                if (map.getTile(map.getHeight() - 1, col) == GameMap.TileType.EMPTY) {
                    newY = 0;
                } else {
                    canSlide = false;
                }
            }

            // Check collision with other tanks
            if (canSlide) {
                for (Tank other : allTanks) {
                    if (other != this && other.isAlive()) {
                        if (checkCollision(newX, newY, other.x, other.y, SIZE, SIZE)) {
                            canSlide = false;
                            break;
                        }
                    }
                }
            }

            // Check collision with base
            if (canSlide && base.isAlive()) {
                if (checkCollision(newX, newY, base.getX(), base.getY(), SIZE, 32)) {
                    canSlide = false;
                }
            }

            // Check collision with map tiles
            if (canSlide && map.checkTankCollision(newX, newY, SIZE, canSwim)) {
                canSlide = false;
            }

            if (canSlide) {
                x = newX;
                y = newY;
                slideDistance -= slideStep;
            } else {
                // Stop sliding if hit obstacle
                isSliding = false;
                slideDistance = 0;
            }

            if (slideDistance <= 0) {
                isSliding = false;
            }
        }
    }

    public void move(Direction newDirection, GameMap map, List<Tank> otherTanks, Base base) {
        if (!alive) return;

        this.direction = newDirection;
        double speed = SPEED * speedMultiplier;

        // Apply 2x speed when on ice
        if (isOnIce(map)) {
            speed *= 2.0;
        }

        double newX = x + direction.getDx() * speed;
        double newY = y + direction.getDy() * speed;

        // Check map boundaries and handle wraparound through destroyed borders
        int mapWidth = map.getWidth() * 32;
        int mapHeight = map.getHeight() * 32;

        // Left edge wraparound
        if (newX < 0) {
            // Check if left border column (col 0) is destroyed at tank's row
            int row = (int)((y + SIZE/2) / 32);
            if (map.getTile(row, 0) == GameMap.TileType.EMPTY) {
                newX = mapWidth - SIZE; // Wrap to right edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Right edge wraparound
        if (newX + SIZE > mapWidth) {
            // Check if right border column (col 25) is destroyed at tank's row
            int row = (int)((y + SIZE/2) / 32);
            if (map.getTile(row, map.getWidth() - 1) == GameMap.TileType.EMPTY) {
                newX = 0; // Wrap to left edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Top edge wraparound
        if (newY < 0) {
            // Check if top border row (row 0) is destroyed at tank's column
            int col = (int)((x + SIZE/2) / 32);
            if (map.getTile(0, col) == GameMap.TileType.EMPTY) {
                newY = mapHeight - SIZE; // Wrap to bottom edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Bottom edge wraparound
        if (newY + SIZE > mapHeight) {
            // Check if bottom border row (row 25) is destroyed at tank's column
            int col = (int)((x + SIZE/2) / 32);
            if (map.getTile(map.getHeight() - 1, col) == GameMap.TileType.EMPTY) {
                newY = 0; // Wrap to top edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Check collision with other tanks
        for (Tank other : otherTanks) {
            if (other != this && other.isAlive()) {
                if (checkCollision(newX, newY, other.x, other.y, SIZE, SIZE)) {
                    return; // Can't move through other tanks
                }
            }
        }

        // Check collision with base (only if base is still alive)
        if (base.isAlive()) {
            if (checkCollision(newX, newY, base.getX(), base.getY(), SIZE, 32)) {
                return; // Can't move through base
            }
        }

        // Check collision with map tiles (pass canSwim for SHIP power-up)
        if (!map.checkTankCollision(newX, newY, SIZE, canSwim)) {
            x = newX;
            y = newY;
        }
    }

    private boolean checkCollision(double x1, double y1, double x2, double y2, int size1, int size2) {
        return x1 < x2 + size2 &&
               x1 + size1 > x2 &&
               y1 < y2 + size2 &&
               y1 + size1 > y2;
    }

    private boolean isOnIce(GameMap map) {
        // Check if center of tank is on ice
        int centerX = (int) ((x + SIZE / 2) / 32);
        int centerY = (int) ((y + SIZE / 2) / 32);
        return map.getTile(centerY, centerX) == GameMap.TileType.ICE;
    }

    public void startSliding(Direction direction, GameMap map) {
        // Start sliding only if on ice
        if (isOnIce(map)) {
            isSliding = true;
            slidingDirection = direction;
            slideDistance = SLIDE_DISTANCE;
        }
    }

    public void shoot(List<Bullet> bullets, SoundManager soundManager) {
        if (!alive || shootCooldown > 0) return;

        double bulletX = x + SIZE / 2.0 - 4;
        double bulletY = y + SIZE / 2.0 - 4;

        // Adjust bullet spawn position based on direction
        switch (direction) {
            case UP -> bulletY = y - 8;
            case DOWN -> bulletY = y + SIZE;
            case LEFT -> bulletX = x - 8;
            case RIGHT -> bulletX = x + SIZE;
        }

        // Calculate number of bullets to fire (1 base + machinegunCount, max 5)
        int bulletCount = Math.min(1 + machinegunCount, 5);
        double bulletSpacing = 24.0; // 3 bullets' worth of space (3 * 8 pixels)

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

            bullets.add(new Bullet(offsetX, offsetY, direction, !isPlayer, bulletPower, canDestroyTrees));
        }

        // Apply shoot cooldown reduction from STAR power-ups (min cooldown is 5 frames)
        shootCooldown = Math.max(5, SHOOT_COOLDOWN - (shootCooldownReduction * 5));
        soundManager.playShoot();
    }

    public void updateAI(GameMap map, List<Bullet> bullets, List<Tank> allTanks, Base base, SoundManager soundManager) {
        if (!alive) return;

        update(map, bullets, soundManager, allTanks, base);

        // Detect if stuck (position hasn't changed)
        if (Math.abs(x - lastX) < 0.1 && Math.abs(y - lastY) < 0.1) {
            stuckCounter++;
            if (stuckCounter > 3) { // Stuck for 3 frames - change direction immediately
                // Try different directions until one works
                Direction[] directions = Direction.values();
                Direction originalDirection = direction;
                for (int i = 0; i < 4; i++) {
                    direction = directions[random.nextInt(4)];
                    if (direction != originalDirection) {
                        break;
                    }
                }
                stuckCounter = 0;
                aiMoveCooldown = 60 + random.nextInt(120); // Commit to new direction longer
            }
        } else {
            stuckCounter = 0;
        }
        lastX = x;
        lastY = y;

        // Decrease AI cooldowns
        aiMoveCooldown--;
        aiShootCooldown--;

        // Randomly shoot
        if (aiShootCooldown <= 0) {
            shoot(bullets, soundManager);
            aiShootCooldown = 60 + random.nextInt(60);
        }

        // Change direction occasionally
        if (aiMoveCooldown <= 0) {
            // 70% move towards base, 30% random
            if (random.nextDouble() < 0.7) {
                moveTowardsBase(base);
            } else {
                direction = Direction.values()[random.nextInt(4)];
            }
            aiMoveCooldown = 30 + random.nextInt(90);
        }

        // Move in current direction
        move(direction, map, allTanks, base);
    }

    private void moveTowardsBase(Base base) {
        double baseX = base.getX();
        double baseY = base.getY();

        double dx = baseX - x;
        double dy = baseY - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            direction = dy > 0 ? Direction.DOWN : Direction.UP;
        }
    }

    public boolean damage() {
        if (!alive || hasShield) return false;

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

    public void render(GraphicsContext gc) {
        if (!alive) return;

        // Draw shield if active
        if (hasShield) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            gc.strokeOval(x - 4, y - 4, SIZE + 8, SIZE + 8);
        }

        // Draw tank body based on type
        if (isPlayer) {
            gc.setFill(playerNumber == 1 ? Color.YELLOW : Color.LIME);
            gc.fillRect(x, y, SIZE, SIZE);
        } else {
            // Draw different enemy types
            switch (enemyType) {
                case REGULAR -> {
                    gc.setFill(Color.RED);
                    gc.fillRect(x, y, SIZE, SIZE);
                }
                case ARMORED -> {
                    // Bigger, slightly darker tank
                    gc.setFill(Color.DARKRED);
                    gc.fillRect(x - 2, y - 2, SIZE + 4, SIZE + 4);
                    gc.setFill(Color.RED);
                    gc.fillRect(x, y, SIZE, SIZE);
                }
                case FAST -> {
                    // Lighter red, sleeker
                    gc.setFill(Color.rgb(255, 100, 100));
                    gc.fillRect(x, y, SIZE, SIZE);
                }
                case POWER -> {
                    // Rainbow/animated colors
                    int frame = (int) (System.currentTimeMillis() / 100) % 7;
                    Color[] rainbow = {
                        Color.RED, Color.ORANGE, Color.YELLOW,
                        Color.GREEN, Color.CYAN, Color.BLUE, Color.PURPLE
                    };
                    gc.setFill(rainbow[frame]);
                    gc.fillRect(x, y, SIZE, SIZE);
                }
                case HEAVY -> {
                    // Black tank with white dot in center
                    gc.setFill(Color.BLACK);
                    gc.fillRect(x - 3, y - 3, SIZE + 6, SIZE + 6);
                    gc.setFill(Color.WHITE);
                    gc.fillOval(x + SIZE / 2 - 4, y + SIZE / 2 - 4, 8, 8);
                }
            }
        }

        // Draw tank direction indicator
        gc.setFill(Color.DARKGRAY);
        switch (direction) {
            case UP -> gc.fillRect(x + SIZE / 2 - 4, y, 8, 12);
            case DOWN -> gc.fillRect(x + SIZE / 2 - 4, y + SIZE - 12, 8, 12);
            case LEFT -> gc.fillRect(x, y + SIZE / 2 - 4, 12, 8);
            case RIGHT -> gc.fillRect(x + SIZE - 12, y + SIZE / 2 - 4, 12, 8);
        }
    }

    public boolean collidesWith(double otherX, double otherY, int otherSize) {
        return x < otherX + otherSize &&
               x + SIZE > otherX &&
               y < otherY + otherSize &&
               y + SIZE > otherY;
    }

    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public boolean isAlive() { return alive; }
    public int getLives() { return lives; }
    public boolean hasShield() { return hasShield; }
    public Direction getDirection() { return direction; }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
    public EnemyType getEnemyType() { return enemyType; }

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
            // Players get shield for 1 minute (3600 frames at 60 FPS)
            hasShield = true;
            shieldDuration = 3600;
        } else {
            // Enemies get extra life
            lives++;
        }
    }

    public boolean canSwim() {
        return canSwim;
    }

    public boolean canDestroyTrees() {
        return canDestroyTrees;
    }

    public void respawn(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.direction = Direction.UP;
        this.health = maxHealth;
        this.hasShield = true;
        this.shieldDuration = 180;
        this.alive = true;

        // Clear all power-ups on respawn
        this.bulletPower = 1;
        this.shootCooldownReduction = 0;
        this.speedMultiplier = 1.0;
        this.canSwim = false;
        this.canDestroyTrees = false;
        this.machinegunCount = 0;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
