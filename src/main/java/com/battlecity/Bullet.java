package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bullet {
    private static final int SIZE = 8;
    private static final double SPEED = 4.0;

    private double x;
    private double y;
    private Direction direction;
    private boolean fromEnemy;
    private int power;
    private boolean canDestroyTrees;
    private boolean canWrapAround; // MACHINEGUN power-up enables wraparound

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees, boolean canWrapAround) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.power = power;
        this.canDestroyTrees = canDestroyTrees;
        this.canWrapAround = canWrapAround;
    }

    public void update() {
        x += direction.getDx() * SPEED;
        y += direction.getDy() * SPEED;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(fromEnemy ? Color.RED : Color.YELLOW);
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

    // Check and handle wraparound through destroyed borders
    // Returns true if bullet should continue, false if it should be removed
    public boolean handleWraparound(GameMap map, int width, int height) {
        // If bullet doesn't have MACHINEGUN power-up, it stops at borders
        if (!canWrapAround) {
            if (x < 0 || x > width || y < 0 || y > height) {
                return false; // Remove bullet at border
            }
            return true; // Bullet is within bounds
        }

        // MACHINEGUN power-up: bullets can wrap through destroyed borders
        // Left edge
        if (x < 0) {
            int row = (int)((y + SIZE/2) / 32);
            if (row >= 0 && row < map.getHeight() && map.getTile(row, 0) == GameMap.TileType.EMPTY) {
                x = width - SIZE; // Wrap to right edge
                return true;
            }
            return false; // Border intact, bullet should be removed
        }

        // Right edge
        if (x > width) {
            int row = (int)((y + SIZE/2) / 32);
            if (row >= 0 && row < map.getHeight() && map.getTile(row, map.getWidth() - 1) == GameMap.TileType.EMPTY) {
                x = 0; // Wrap to left edge
                return true;
            }
            return false; // Border intact, bullet should be removed
        }

        // Top edge
        if (y < 0) {
            int col = (int)((x + SIZE/2) / 32);
            if (col >= 0 && col < map.getWidth() && map.getTile(0, col) == GameMap.TileType.EMPTY) {
                y = height - SIZE; // Wrap to bottom edge
                return true;
            }
            return false; // Border intact, bullet should be removed
        }

        // Bottom edge
        if (y > height) {
            int col = (int)((x + SIZE/2) / 32);
            if (col >= 0 && col < map.getWidth() && map.getTile(map.getHeight() - 1, col) == GameMap.TileType.EMPTY) {
                y = 0; // Wrap to top edge
                return true;
            }
            return false; // Border intact, bullet should be removed
        }

        return true; // Bullet is within bounds
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public Direction getDirection() { return direction; }
    public boolean isFromEnemy() { return fromEnemy; }
    public int getPower() { return power; }
    public boolean canDestroyTrees() { return canDestroyTrees; }
}
