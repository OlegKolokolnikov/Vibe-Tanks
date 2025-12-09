package com.vibetanks.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Base {
    private static final int SIZE = 32;

    private double x;
    private double y;
    private boolean alive;

    // Flag animation (skull flag for game over)
    private boolean showFlag = false;
    private double flagHeight = 0;
    private static final double MAX_FLAG_HEIGHT = 50;
    private static final double FLAG_RISE_SPEED = 1.5;
    private int flagWaveFrame = 0;

    // Victory flag animation (Soviet flag)
    private boolean showVictoryFlag = false;
    private double victoryFlagHeight = 0;
    private int victoryFlagWaveFrame = 0;

    // Cat mode (when player collects easter egg after boss spawned)
    private boolean catMode = false;
    private int catAnimFrame = 0;

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

    public void raiseVictoryFlag() {
        showVictoryFlag = true;
    }

    public void setFlagState(boolean show, double height) {
        this.showFlag = show;
        this.flagHeight = height;
    }

    public void setVictoryFlagState(boolean show, double height) {
        this.showVictoryFlag = show;
        this.victoryFlagHeight = height;
    }

    public boolean isShowingFlag() {
        return showFlag;
    }

    public double getFlagHeight() {
        return flagHeight;
    }

    public boolean isShowingVictoryFlag() {
        return showVictoryFlag;
    }

    public double getVictoryFlagHeight() {
        return victoryFlagHeight;
    }

    public void render(GraphicsContext gc) {
        if (alive) {
            if (catMode) {
                // Draw cute cat instead of eagle
                catAnimFrame++;
                renderCat(gc);
            } else {
                // Draw base as classic eagle symbol
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
            }

            // Draw rising Soviet victory flag
            if (showVictoryFlag) {
                // Animate flag rising
                if (victoryFlagHeight < MAX_FLAG_HEIGHT) {
                    victoryFlagHeight += FLAG_RISE_SPEED;
                }
                victoryFlagWaveFrame++;

                double poleX = x + SIZE / 2;
                double poleBottom = y;
                double poleTop = poleBottom - victoryFlagHeight;

                // Flag pole
                gc.setStroke(Color.SADDLEBROWN);
                gc.setLineWidth(3);
                gc.strokeLine(poleX, poleBottom, poleX, poleTop);

                // Pole ball on top (golden star)
                gc.setFill(Color.GOLD);
                gc.fillOval(poleX - 4, poleTop - 8, 8, 8);

                // Only draw flag when pole is high enough
                if (victoryFlagHeight > 20) {
                    // Flag waving effect
                    double wave = Math.sin(victoryFlagWaveFrame * 0.15) * 4;

                    // Soviet red flag
                    double flagWidth = 42;
                    double flagH = 28;
                    double flagX = poleX + 2;
                    double flagY = poleTop;

                    // Red flag background
                    gc.setFill(Color.rgb(204, 0, 0)); // Soviet red
                    gc.beginPath();
                    gc.moveTo(flagX, flagY);
                    gc.lineTo(flagX + flagWidth + wave, flagY + 2);
                    gc.lineTo(flagX + flagWidth + wave * 0.5, flagY + flagH / 2);
                    gc.lineTo(flagX + flagWidth - wave, flagY + flagH - 2);
                    gc.lineTo(flagX, flagY + flagH);
                    gc.closePath();
                    gc.fill();

                    // Gold hammer and sickle
                    double symbolX = flagX + 8 + wave * 0.3;
                    double symbolY = flagY + 5;

                    gc.setFill(Color.GOLD);
                    gc.setStroke(Color.GOLD);
                    gc.setLineWidth(2);

                    // Sickle (curved arc)
                    gc.strokeArc(symbolX, symbolY, 12, 12, 45, 180, javafx.scene.shape.ArcType.OPEN);

                    // Hammer handle
                    gc.fillRect(symbolX + 6, symbolY + 4, 3, 14);

                    // Hammer head
                    gc.fillRect(symbolX + 2, symbolY + 14, 10, 4);

                    // Gold star above
                    double starX = symbolX + 6;
                    double starY = symbolY - 2;
                    double starSize = 6;
                    double[] starXPoints = new double[10];
                    double[] starYPoints = new double[10];
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.PI / 2 + (Math.PI * i / 5);
                        double r = (i % 2 == 0) ? starSize : starSize * 0.4;
                        starXPoints[i] = starX + r * Math.cos(angle);
                        starYPoints[i] = starY - r * Math.sin(angle);
                    }
                    gc.fillPolygon(starXPoints, starYPoints, 10);
                }
            }
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

    private void renderCat(GraphicsContext gc) {
        // Warm background
        gc.setFill(Color.rgb(255, 240, 220)); // Cream background
        gc.fillRect(x, y, SIZE, SIZE);

        // Tail animation
        double tailWave = Math.sin(catAnimFrame * 0.1) * 3;

        // Tail (behind body)
        gc.setFill(Color.rgb(255, 165, 0)); // Orange
        gc.fillOval(x + 22 + tailWave, y + 18, 8, 6);
        gc.fillOval(x + 26 + tailWave * 1.5, y + 14, 6, 8);

        // Body
        gc.setFill(Color.rgb(255, 165, 0)); // Orange cat
        gc.fillOval(x + 6, y + 14, 20, 16);

        // Head
        gc.fillOval(x + 4, y + 4, 18, 16);

        // Ears
        double earTwitch = Math.sin(catAnimFrame * 0.15) * 1;
        // Left ear
        gc.beginPath();
        gc.moveTo(x + 4, y + 8);
        gc.lineTo(x + 2 + earTwitch, y - 2);
        gc.lineTo(x + 10, y + 4);
        gc.closePath();
        gc.fill();
        // Right ear
        gc.beginPath();
        gc.moveTo(x + 16, y + 4);
        gc.lineTo(x + 22 - earTwitch, y - 2);
        gc.lineTo(x + 22, y + 8);
        gc.closePath();
        gc.fill();

        // Inner ears (pink)
        gc.setFill(Color.rgb(255, 182, 193));
        gc.beginPath();
        gc.moveTo(x + 5, y + 6);
        gc.lineTo(x + 4 + earTwitch, y + 1);
        gc.lineTo(x + 9, y + 5);
        gc.closePath();
        gc.fill();
        gc.beginPath();
        gc.moveTo(x + 17, y + 5);
        gc.lineTo(x + 20 - earTwitch, y + 1);
        gc.lineTo(x + 21, y + 6);
        gc.closePath();
        gc.fill();

        // Eyes - blinking animation
        boolean blinking = (catAnimFrame % 120) < 8;
        gc.setFill(Color.rgb(50, 205, 50)); // Green eyes
        if (blinking) {
            gc.fillRect(x + 7, y + 10, 4, 1);
            gc.fillRect(x + 15, y + 10, 4, 1);
        } else {
            gc.fillOval(x + 7, y + 8, 5, 5);
            gc.fillOval(x + 14, y + 8, 5, 5);
            // Pupils
            gc.setFill(Color.BLACK);
            gc.fillOval(x + 9, y + 9, 2, 3);
            gc.fillOval(x + 16, y + 9, 2, 3);
        }

        // Nose
        gc.setFill(Color.rgb(255, 105, 180)); // Pink nose
        gc.beginPath();
        gc.moveTo(x + 13, y + 13);
        gc.lineTo(x + 11, y + 15);
        gc.lineTo(x + 15, y + 15);
        gc.closePath();
        gc.fill();

        // Whiskers
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.setLineWidth(1);
        // Left whiskers
        gc.strokeLine(x + 4, y + 14, x - 2, y + 12);
        gc.strokeLine(x + 4, y + 16, x - 2, y + 16);
        gc.strokeLine(x + 4, y + 18, x - 2, y + 20);
        // Right whiskers
        gc.strokeLine(x + 22, y + 14, x + 28, y + 12);
        gc.strokeLine(x + 22, y + 16, x + 28, y + 16);
        gc.strokeLine(x + 22, y + 18, x + 28, y + 20);

        // Mouth
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.strokeLine(x + 13, y + 15, x + 13, y + 17);
        gc.strokeArc(x + 10, y + 15, 4, 4, 180, 90, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(x + 12, y + 15, 4, 4, 270, 90, javafx.scene.shape.ArcType.OPEN);

        // Paws
        gc.setFill(Color.rgb(255, 165, 0));
        gc.fillOval(x + 6, y + 26, 6, 5);
        gc.fillOval(x + 14, y + 26, 6, 5);

        // Stripes on forehead
        gc.setFill(Color.rgb(200, 120, 0)); // Darker orange
        gc.fillRect(x + 12, y + 2, 2, 4);
        gc.fillRect(x + 9, y + 3, 2, 3);
        gc.fillRect(x + 15, y + 3, 2, 3);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public boolean isAlive() { return alive; }

    // Cat mode (when player collects easter egg after boss spawned)
    public void setCatMode(boolean catMode) {
        this.catMode = catMode;
    }

    public boolean isCatMode() {
        return catMode;
    }

    /**
     * Reset the base to initial state (for new level or game restart).
     */
    public void reset() {
        this.alive = true;
        this.showFlag = false;
        this.flagHeight = 0;
        this.flagWaveFrame = 0;
        this.showVictoryFlag = false;
        this.victoryFlagHeight = 0;
        this.victoryFlagWaveFrame = 0;
        this.catMode = false;
        this.catAnimFrame = 0;
    }
}
