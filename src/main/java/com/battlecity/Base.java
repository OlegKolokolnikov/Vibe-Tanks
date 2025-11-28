package com.battlecity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Base {
    private static final int SIZE = 32;

    private double x;
    private double y;
    private boolean alive;

    // Flag animation
    private boolean showFlag = false;
    private double flagHeight = 0;
    private static final double MAX_FLAG_HEIGHT = 50;
    private static final double FLAG_RISE_SPEED = 1.5;
    private int flagWaveFrame = 0;

    public Base(double x, double y) {
        this.x = x;
        this.y = y;
        this.alive = true;
    }

    public void destroy() {
        alive = false;
    }

    public void raiseFlag() {
        showFlag = true;
    }

    public void setFlagState(boolean show, double height) {
        this.showFlag = show;
        this.flagHeight = height;
    }

    public boolean isShowingFlag() {
        return showFlag;
    }

    public double getFlagHeight() {
        return flagHeight;
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

            // Draw rising flag with skull and crossbones
            if (showFlag) {
                // Animate flag rising
                if (flagHeight < MAX_FLAG_HEIGHT) {
                    flagHeight += FLAG_RISE_SPEED;
                }
                flagWaveFrame++;

                double poleX = x + SIZE / 2;
                double poleBottom = y + SIZE / 2;
                double poleTop = poleBottom - flagHeight;

                // Flag pole
                gc.setStroke(Color.SADDLEBROWN);
                gc.setLineWidth(3);
                gc.strokeLine(poleX, poleBottom, poleX, poleTop);

                // Pole ball on top
                gc.setFill(Color.GOLD);
                gc.fillOval(poleX - 3, poleTop - 6, 6, 6);

                // Only draw flag when pole is high enough
                if (flagHeight > 20) {
                    // Flag waving effect
                    double wave = Math.sin(flagWaveFrame * 0.15) * 3;

                    // Black flag background
                    double flagWidth = 36;
                    double flagH = 28;
                    double flagX = poleX + 2;
                    double flagY = poleTop;

                    gc.setFill(Color.BLACK);
                    gc.beginPath();
                    gc.moveTo(flagX, flagY);
                    gc.lineTo(flagX + flagWidth + wave, flagY + 2);
                    gc.lineTo(flagX + flagWidth + wave * 0.5, flagY + flagH / 2);
                    gc.lineTo(flagX + flagWidth - wave, flagY + flagH - 2);
                    gc.lineTo(flagX, flagY + flagH);
                    gc.closePath();
                    gc.fill();

                    // Skull
                    double skullX = flagX + flagWidth / 2 - 6 + wave * 0.3;
                    double skullY = flagY + 4;

                    // Skull head
                    gc.setFill(Color.WHITE);
                    gc.fillOval(skullX, skullY, 14, 12);

                    // Jaw
                    gc.fillRect(skullX + 2, skullY + 10, 10, 4);

                    // Eye sockets
                    gc.setFill(Color.BLACK);
                    gc.fillOval(skullX + 2, skullY + 4, 4, 4);
                    gc.fillOval(skullX + 8, skullY + 4, 4, 4);

                    // Nose hole
                    gc.fillOval(skullX + 5.5, skullY + 8, 3, 2);

                    // Teeth
                    gc.setFill(Color.WHITE);
                    for (int i = 0; i < 4; i++) {
                        gc.fillRect(skullX + 3 + i * 2, skullY + 12, 1.5, 2);
                    }

                    // Crossbones
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(3);
                    double boneY = skullY + 16;
                    double boneLen = 16;
                    // First bone (top-left to bottom-right)
                    gc.strokeLine(skullX - 2 + wave * 0.2, boneY - 2, skullX + boneLen - 2 + wave * 0.4, boneY + 8);
                    // Second bone (top-right to bottom-left)
                    gc.strokeLine(skullX + boneLen - 2 + wave * 0.4, boneY - 2, skullX - 2 + wave * 0.2, boneY + 8);

                    // Bone ends (rounded)
                    gc.setFill(Color.WHITE);
                    gc.fillOval(skullX - 4 + wave * 0.2, boneY - 4, 5, 5);
                    gc.fillOval(skullX + boneLen - 3 + wave * 0.4, boneY - 4, 5, 5);
                    gc.fillOval(skullX - 4 + wave * 0.2, boneY + 6, 5, 5);
                    gc.fillOval(skullX + boneLen - 3 + wave * 0.4, boneY + 6, 5, 5);
                }
            }
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public boolean isAlive() { return alive; }
}
