package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

public class Tank {
    private static final int SIZE = 28;
    private static final double SPEED = 2.0;
    private static final int SHOOT_COOLDOWN = 30; // frames

    private double x;
    private double y;
    private Direction direction;
    private boolean isPlayer;
    private int playerNumber; // 1 or 2
    private boolean alive;
    private int lives;
    private int health;

    private int shootCooldown;
    private boolean hasShield;
    private int shieldDuration;
    private double speedMultiplier;
    private int speedDuration;
    private int bulletPower;
    private int powerDuration;

    private Random random;

    // AI variables
    private int aiMoveCooldown;
    private int aiShootCooldown;

    public Tank(double x, double y, Direction direction, boolean isPlayer, int playerNumber) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.isPlayer = isPlayer;
        this.playerNumber = playerNumber;
        this.alive = true;
        this.lives = isPlayer ? 3 : 1;
        this.health = 1;
        this.shootCooldown = 0;
        this.hasShield = isPlayer; // Players start with shield
        this.shieldDuration = isPlayer ? 180 : 0; // 3 seconds
        this.speedMultiplier = 1.0;
        this.speedDuration = 0;
        this.bulletPower = 1;
        this.powerDuration = 0;
        this.random = new Random();
        this.aiMoveCooldown = 60;
        this.aiShootCooldown = 90;
    }

    public void update(GameMap map, List<Bullet> bullets, SoundManager soundManager) {
        if (!alive) return;

        // Update cooldowns
        if (shootCooldown > 0) shootCooldown--;
        if (shieldDuration > 0) {
            shieldDuration--;
            if (shieldDuration == 0) hasShield = false;
        }
        if (speedDuration > 0) {
            speedDuration--;
            if (speedDuration == 0) speedMultiplier = 1.0;
        }
        if (powerDuration > 0) {
            powerDuration--;
            if (powerDuration == 0) bulletPower = 1;
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

        // Check collision with map tiles
        if (!map.checkTankCollision(newX, newY, SIZE)) {
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

        bullets.add(new Bullet(bulletX, bulletY, direction, !isPlayer, bulletPower));
        shootCooldown = SHOOT_COOLDOWN;
        soundManager.playShoot();
    }

    public void updateAI(GameMap map, List<Bullet> bullets, List<Tank> players, Base base, SoundManager soundManager) {
        if (!alive) return;

        update(map, bullets, soundManager);

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

    public void damage() {
        if (!alive || hasShield) return;

        health--;
        if (health <= 0) {
            lives--;
            if (lives > 0) {
                health = 1;
                hasShield = true;
                shieldDuration = 180;
            } else {
                alive = false;
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (!alive) return;

        // Draw shield if active
        if (hasShield) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            gc.strokeOval(x - 4, y - 4, SIZE + 8, SIZE + 8);
        }

        // Draw tank body
        if (isPlayer) {
            gc.setFill(playerNumber == 1 ? Color.YELLOW : Color.LIME);
        } else {
            gc.setFill(Color.RED);
        }
        gc.fillRect(x, y, SIZE, SIZE);

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

    // Power-up effects
    public void applyShield() {
        hasShield = true;
        shieldDuration = 300; // 5 seconds
    }

    public void applySpeed() {
        speedMultiplier = 1.5;
        speedDuration = 300; // 5 seconds
    }

    public void applyPower() {
        bulletPower = 2;
        powerDuration = 300; // 5 seconds
    }

    public void addLife() {
        lives++;
    }
}
