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
            // Draw base as classic Battle City eagle
            // Background
            gc.setFill(Color.rgb(252, 216, 168)); // Tan background
            gc.fillRect(x, y, SIZE, SIZE);

            // Eagle body - black
            gc.setFill(Color.BLACK);
            // Head
            gc.fillRect(x + 12, y + 4, 8, 8);
            // Body center
            gc.fillRect(x + 8, y + 12, 16, 12);
            // Wings
            gc.fillRect(x + 4, y + 16, 6, 8);
            gc.fillRect(x + 22, y + 16, 6, 8);
            // Tail/legs
            gc.fillRect(x + 10, y + 24, 4, 6);
            gc.fillRect(x + 18, y + 24, 4, 6);

            // Orange/red details
            gc.setFill(Color.rgb(252, 116, 96));
            gc.fillRect(x + 10, y + 16, 4, 4);
            gc.fillRect(x + 18, y + 16, 4, 4);
            gc.fillRect(x + 14, y + 12, 4, 8);
        } else {
            // Draw destroyed base - rubble
            gc.setFill(Color.rgb(80, 48, 0)); // Dark brown
            gc.fillRect(x, y, SIZE, SIZE);
            gc.setFill(Color.rgb(120, 72, 0));
            gc.fillRect(x + 4, y + 4, 8, 8);
            gc.fillRect(x + 20, y + 8, 10, 10);
            gc.fillRect(x + 8, y + 20, 12, 10);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public boolean isAlive() { return alive; }
}
