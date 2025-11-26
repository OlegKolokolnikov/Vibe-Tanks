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

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.power = power;
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

    public boolean isOutOfBounds(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public Direction getDirection() { return direction; }
    public boolean isFromEnemy() { return fromEnemy; }
    public int getPower() { return power; }
}
