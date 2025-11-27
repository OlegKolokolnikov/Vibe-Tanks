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
    private int shootCooldownReduction; // STAR power-up (each star reduces cooldown)

    private Random random;

    // AI variables
    private int aiMoveCooldown;
    private int aiShootCooldown;
    private double lastX, lastY; // Track position to detect stuck
    private int stuckCounter; // Count frames stuck

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
        this.shootCooldownReduction = 0;
        this.random = new Random();
        this.aiMoveCooldown = 60;
        this.aiShootCooldown = 90;
        this.lastX = x;
        this.lastY = y;
        this.stuckCounter = 0;
    }

    public void update(GameMap map, List<Bullet> bullets, SoundManager soundManager) {
        if (!alive) return;

        // Update cooldowns
        if (shootCooldown > 0) shootCooldown--;
        if (shieldDuration > 0) {
            shieldDuration--;
            if (shieldDuration == 0) hasShield = false;
        }
    }

    public void move(Direction newDirection, GameMap map) {
        if (!alive) return;

        this.direction = newDirection;
        double speed = SPEED * speedMultiplier;

        double newX = x + direction.getDx() * speed;
        double newY = y + direction.getDy() * speed;

        // Check map boundaries
        if (newX < 0 || newX + SIZE > map.getWidth() * 32 ||
            newY < 0 || newY + SIZE > map.getHeight() * 32) {
            return;
        }

        // Check collision with map tiles (pass canSwim for SHIP power-up)
        if (!map.checkTankCollision(newX, newY, SIZE, canSwim)) {
            x = newX;
            y = newY;
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

        bullets.add(new Bullet(bulletX, bulletY, direction, !isPlayer, bulletPower, canDestroyTrees));
        // Apply shoot cooldown reduction from STAR power-ups (min cooldown is 5 frames)
        shootCooldown = Math.max(5, SHOOT_COOLDOWN - (shootCooldownReduction * 5));
        soundManager.playShoot();
    }

    public void updateAI(GameMap map, List<Bullet> bullets, List<Tank> players, Base base, SoundManager soundManager) {
        if (!alive) return;

        update(map, bullets, soundManager);

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
        move(direction, map);
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

        // POWER type drops power-up on every hit
        if (!isPlayer && enemyType == EnemyType.POWER && health > 0) {
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
    public EnemyType getEnemyType() { return enemyType; }

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
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
