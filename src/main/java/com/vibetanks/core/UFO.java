package com.vibetanks.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

public class UFO {
    private static final int SIZE = 48;
    private static final double SPEED = 1.5;
    private static final int SHOOT_COOLDOWN = GameConstants.UFO_SHOOT_COOLDOWN;
    private static final int LIFETIME = GameConstants.UFO_LIFETIME;
    private static final int DIRECTION_CHANGE_INTERVAL = GameConstants.UFO_DIRECTION_CHANGE;

    private double x, y;
    private double dx, dy; // Movement direction
    private boolean alive;
    private int lifetime;
    private int shootCooldown;
    private int directionChangeTimer;
    private final Random random = GameConstants.RANDOM; // Use shared Random instance
    private boolean movingRight; // General direction (left to right or right to left)
    private int health = 3; // UFO takes 3 hits to destroy

    // Animation
    private double hoverOffset;
    private int lightFrame;

    public UFO(double startX, double startY, boolean movingRight) {
        this.x = startX;
        this.y = startY;
        this.movingRight = movingRight;
        this.alive = true;
        this.lifetime = LIFETIME;
        this.shootCooldown = SHOOT_COOLDOWN;
        this.directionChangeTimer = DIRECTION_CHANGE_INTERVAL;
        this.hoverOffset = 0;
        this.lightFrame = 0;

        // Initial direction
        this.dx = movingRight ? SPEED : -SPEED;
        this.dy = 0;
        randomizeDirection();
    }

    private void randomizeDirection() {
        // Keep general horizontal direction but add random vertical movement
        double baseX = movingRight ? SPEED : -SPEED;
        double randomY = (random.nextDouble() - 0.5) * SPEED * 1.5;

        this.dx = baseX + (random.nextDouble() - 0.5) * 0.5;
        this.dy = randomY;

        // Normalize to maintain consistent speed
        double magnitude = Math.sqrt(dx * dx + dy * dy);
        if (magnitude > 0) {
            dx = (dx / magnitude) * SPEED;
            dy = (dy / magnitude) * SPEED;
        }

        // Ensure still moving in general direction
        if (movingRight && dx < 0.3) dx = 0.5;
        if (!movingRight && dx > -0.3) dx = -0.5;
    }

    public void update(List<Bullet> bullets, int mapWidth, int mapHeight, com.vibetanks.audio.SoundManager soundManager) {
        if (!alive) return;

        lifetime--;
        if (lifetime <= 0) {
            alive = false;
            return;
        }

        // Change direction periodically
        directionChangeTimer--;
        if (directionChangeTimer <= 0) {
            randomizeDirection();
            directionChangeTimer = DIRECTION_CHANGE_INTERVAL + random.nextInt(60);
        }

        // Move
        x += dx;
        y += dy;

        // Keep within vertical bounds (bounce)
        if (y < 50) {
            y = 50;
            dy = Math.abs(dy);
        }
        if (y > mapHeight - SIZE - 50) {
            y = mapHeight - SIZE - 50;
            dy = -Math.abs(dy);
        }

        // Check if reached other side
        if (movingRight && x > mapWidth) {
            alive = false;
            return;
        }
        if (!movingRight && x < -SIZE) {
            alive = false;
            return;
        }

        // Shooting
        shootCooldown--;
        if (shootCooldown <= 0) {
            shoot(bullets, soundManager);
            shootCooldown = SHOOT_COOLDOWN + random.nextInt(30);
        }

        // Animation
        hoverOffset = Math.sin(System.currentTimeMillis() / 200.0) * 3;
        lightFrame = (int)(System.currentTimeMillis() / 100) % 8;
    }

    private void shoot(List<Bullet> bullets, com.vibetanks.audio.SoundManager soundManager) {
        // Shoot downward
        double bulletX = x + SIZE / 2.0 - 4;
        double bulletY = y + SIZE;
        bullets.add(new Bullet(bulletX, bulletY, Direction.DOWN, true, 1, false, 0, 8));
        soundManager.playShoot();
    }

    public boolean damage() {
        health--;
        if (health <= 0) {
            alive = false;
            return true; // Destroyed
        }
        return false;
    }

    public void render(GraphicsContext gc) {
        if (!alive) return;

        double renderY = y + hoverOffset;

        // UFO body (metallic gray ellipse)
        gc.setFill(Color.rgb(120, 120, 140));
        gc.fillOval(x, renderY + SIZE * 0.3, SIZE, SIZE * 0.4);

        // UFO dome (glass-like)
        gc.setFill(Color.rgb(150, 200, 255, 0.7));
        gc.fillOval(x + SIZE * 0.25, renderY, SIZE * 0.5, SIZE * 0.5);

        // UFO dome highlight
        gc.setFill(Color.rgb(200, 230, 255, 0.5));
        gc.fillOval(x + SIZE * 0.3, renderY + SIZE * 0.05, SIZE * 0.2, SIZE * 0.2);

        // UFO rim (darker)
        gc.setStroke(Color.rgb(80, 80, 100));
        gc.setLineWidth(2);
        gc.strokeOval(x, renderY + SIZE * 0.3, SIZE, SIZE * 0.4);

        // Rotating lights on bottom
        gc.setFill(getLightColor(lightFrame));
        for (int i = 0; i < 4; i++) {
            double angle = (lightFrame + i * 2) * Math.PI / 4;
            double lightX = x + SIZE / 2 + Math.cos(angle) * SIZE * 0.35 - 3;
            double lightY = renderY + SIZE * 0.5 + Math.sin(angle) * SIZE * 0.1 - 3;
            gc.fillOval(lightX, lightY, 6, 6);
        }

        // Beam effect when shooting (brief flash)
        if (shootCooldown > SHOOT_COOLDOWN - 10) {
            gc.setFill(Color.rgb(0, 255, 100, 0.3));
            gc.fillRect(x + SIZE / 2 - 5, renderY + SIZE * 0.7, 10, 50);
        }

        // Health indicator (small dots)
        gc.setFill(Color.LIME);
        for (int i = 0; i < health; i++) {
            gc.fillOval(x + SIZE / 2 - 10 + i * 8, renderY - 10, 6, 6);
        }
    }

    private Color getLightColor(int frame) {
        return switch (frame % 4) {
            case 0 -> Color.RED;
            case 1 -> Color.YELLOW;
            case 2 -> Color.LIME;
            case 3 -> Color.CYAN;
            default -> Color.WHITE;
        };
    }

    public boolean collidesWith(Bullet bullet) {
        if (!alive || bullet.isFromEnemy()) return false;
        double bx = bullet.getX();
        double by = bullet.getY();
        int bs = bullet.getSize();
        return bx < x + SIZE && bx + bs > x && by < y + SIZE && by + bs > y;
    }

    // Getters for network sync
    public double getX() { return x; }
    public double getY() { return y; }
    public double getDx() { return dx; }
    public double getDy() { return dy; }
    public boolean isAlive() { return alive; }
    public int getHealth() { return health; }
    public int getLifetime() { return lifetime; }
    public boolean isMovingRight() { return movingRight; }

    // Setters for network sync
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public void setHealth(int health) { this.health = health; }
    public void setLifetime(int lifetime) { this.lifetime = lifetime; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
}
