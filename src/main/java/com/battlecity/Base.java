package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Base {
    private static final int SIZE = 32;

    private double x;
    private double y;
    private boolean alive;

    public Base(double x, double y) {
        this.x = x;
        this.y = y;
        this.alive = true;
    }

    public void destroy() {
        alive = false;
    }

    public void render(GraphicsContext gc) {
        if (alive) {
            // Draw base as a flag/eagle
            gc.setFill(Color.GOLD);
            gc.fillRect(x, y, SIZE, SIZE);
            gc.setFill(Color.RED);
            gc.fillPolygon(
                new double[]{x + SIZE / 2, x + 10, x + SIZE - 10},
                new double[]{y + 8, y + 20, y + 20},
                3
            );
        } else {
            // Draw destroyed base
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y, SIZE, SIZE);
            gc.setStroke(Color.RED);
            gc.strokeLine(x, y, x + SIZE, y + SIZE);
            gc.strokeLine(x + SIZE, y, x, y + SIZE);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public boolean isAlive() { return alive; }
}
