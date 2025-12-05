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

    // Easter egg mode (when UFO is killed)
    private boolean easterEggMode = false;
    private int easterEggAnimFrame = 0;

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
            if (easterEggMode) {
                // Draw colorful Easter egg instead of eagle
                easterEggAnimFrame++;
                renderEasterEgg(gc);
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

    private void renderEasterEgg(GraphicsContext gc) {
        // Colorful background
        gc.setFill(Color.rgb(200, 255, 200)); // Light green grass
        gc.fillRect(x, y, SIZE, SIZE);

        // Easter egg - oval shape
        double eggX = x + 4;
        double eggY = y + 2;
        double eggWidth = 24;
        double eggHeight = 28;

        // Color cycling based on animation frame
        int colorIndex = (easterEggAnimFrame / 30) % 6;
        Color baseColor = switch (colorIndex) {
            case 0 -> Color.rgb(255, 182, 193); // Pink
            case 1 -> Color.rgb(173, 216, 230); // Light blue
            case 2 -> Color.rgb(144, 238, 144); // Light green
            case 3 -> Color.rgb(255, 255, 153); // Light yellow
            case 4 -> Color.rgb(221, 160, 221); // Plum
            default -> Color.rgb(255, 218, 185); // Peach
        };

        // Egg base color
        gc.setFill(baseColor);
        gc.fillOval(eggX, eggY, eggWidth, eggHeight);

        // Egg outline
        gc.setStroke(Color.rgb(139, 90, 43)); // Brown outline
        gc.setLineWidth(2);
        gc.strokeOval(eggX, eggY, eggWidth, eggHeight);

        // Decorative zigzag band across middle
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        double bandY = eggY + eggHeight / 2 - 2;
        gc.beginPath();
        gc.moveTo(eggX + 2, bandY);
        for (int i = 0; i < 6; i++) {
            double px = eggX + 4 + i * 3.5;
            double py = bandY + ((i % 2 == 0) ? -3 : 3);
            gc.lineTo(px, py);
        }
        gc.stroke();

        // Decorative dots
        gc.setFill(Color.MAGENTA);
        gc.fillOval(eggX + 6, eggY + 6, 4, 4);
        gc.fillOval(eggX + 14, eggY + 6, 4, 4);

        gc.setFill(Color.CYAN);
        gc.fillOval(eggX + 10, eggY + 20, 4, 4);

        gc.setFill(Color.ORANGE);
        gc.fillOval(eggX + 5, eggY + 18, 3, 3);
        gc.fillOval(eggX + 16, eggY + 18, 3, 3);

        // Star on top
        gc.setFill(Color.GOLD);
        double starX = eggX + eggWidth / 2;
        double starY = eggY + 4;
        double starSize = 4;
        double[] starXPoints = new double[10];
        double[] starYPoints = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + (Math.PI * i / 5);
            double r = (i % 2 == 0) ? starSize : starSize * 0.4;
            starXPoints[i] = starX + r * Math.cos(angle);
            starYPoints[i] = starY - r * Math.sin(angle);
        }
        gc.fillPolygon(starXPoints, starYPoints, 10);

        // Sparkle effect (rotating)
        double sparkleAngle = (easterEggAnimFrame * 0.1) % (2 * Math.PI);
        gc.setFill(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            double angle = sparkleAngle + (i * Math.PI / 2);
            double sx = x + SIZE / 2 + Math.cos(angle) * 18;
            double sy = y + SIZE / 2 + Math.sin(angle) * 14;
            gc.fillOval(sx - 2, sy - 2, 4, 4);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return SIZE; }
    public boolean isAlive() { return alive; }

    // Easter egg mode
    public void setEasterEggMode(boolean easterEgg) {
        this.easterEggMode = easterEgg;
    }

    public boolean isEasterEggMode() {
        return easterEggMode;
    }
}
