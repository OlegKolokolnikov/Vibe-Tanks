package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bullet {
    private static final int SIZE = 8;
    private static final double SPEED = 4.0;
    private static long nextId = 1; // Global bullet ID counter

    private long id;
    private double x;
    private double y;
    private Direction direction;
    private boolean fromEnemy;
    private int power;
    private boolean canDestroyTrees;
    private int ownerPlayerNumber; // 1-4 for player bullets, 0 for enemy

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees) {
        this(x, y, direction, fromEnemy, power, canDestroyTrees, 0);
    }

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.power = power;
        this.canDestroyTrees = canDestroyTrees;
        this.ownerPlayerNumber = ownerPlayerNumber;
    }

    // Constructor with explicit ID (for network sync)
    public Bullet(long id, double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.power = power;
        this.canDestroyTrees = canDestroyTrees;
        this.ownerPlayerNumber = ownerPlayerNumber;
    }

    public void update() {
        x += direction.getDx() * SPEED;
        y += direction.getDy() * SPEED;
    }

    public void render(GraphicsContext gc) {
        if (fromEnemy) {
            gc.setFill(Color.RED);
        } else {
            // Use player's tank color for their bullets
            gc.setFill(Tank.getPlayerColor(ownerPlayerNumber));
        }
        gc.fillOval(x, y, SIZE, SIZE);
    }

    public boolean collidesWith(Tank tank) {
        return x < tank.getX() + tank.getSize() &&
               x + SIZE > tank.getX() &&
               y < tank.getY() + tank.getSize() &&
               y + SIZE > tank.getY();
    }

    public boolean collidesWith(Base base) {
        return x < base.getX() + base.getSize() &&
               x + SIZE > base.getX() &&
               y < base.getY() + base.getSize() &&
               y + SIZE > base.getY();
    }

    public boolean collidesWith(Bullet other) {
        return x < other.x + SIZE &&
               x + SIZE > other.x &&
               y < other.y + SIZE &&
               y + SIZE > other.y;
    }

    public boolean isOutOfBounds(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }

    // Check if bullet is out of bounds
    // Returns true if bullet should continue, false if it should be removed
    public boolean handleWraparound(GameMap map, int width, int height) {
        // Remove bullets that go off-screen (no wrapping allowed)
        if (x < 0 || x > width || y < 0 || y > height) {
            return false; // Bullet is out of bounds, remove it
        }

        return true; // Bullet is within bounds
    }

    public long getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public Direction getDirection() { return direction; }
    public boolean isFromEnemy() { return fromEnemy; }
    public int getPower() { return power; }
    public boolean canDestroyTrees() { return canDestroyTrees; }
    public int getOwnerPlayerNumber() { return ownerPlayerNumber; }
}
