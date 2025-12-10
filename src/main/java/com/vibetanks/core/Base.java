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

    // Cat escape animation (when victory with broken protection in cat mode)
    private boolean catEscaping = false;
    private double catEscapeX = 0;
    private double catEscapeY = 0;
    private int catEscapeFrame = 0;
    private double toyX = 0;
    private double toyY = 0;
    private int toyType = 0; // 0 = yarn ball, 1 = mouse, 2 = feather

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
            if (catEscaping) {
                // Cat is escaping - render empty base and escaping cat with toy
                catEscapeFrame++;
                renderEmptyBase(gc);
                renderEscapingCat(gc);
                renderToy(gc);
            } else if (catMode) {
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

    /**
     * Render empty base (cat has left).
     */
    private void renderEmptyBase(GraphicsContext gc) {
        // Empty cream background where cat used to be
        gc.setFill(Color.rgb(255, 240, 220));
        gc.fillRect(x, y, SIZE, SIZE);

        // Small cat bed/cushion left behind
        gc.setFill(Color.rgb(180, 100, 80)); // Brown cushion
        gc.fillOval(x + 4, y + 20, 24, 10);
        gc.setFill(Color.rgb(200, 120, 100)); // Lighter center
        gc.fillOval(x + 8, y + 22, 16, 6);

        // Some cat fur left on the cushion
        gc.setFill(Color.rgb(255, 180, 100)); // Orange fur bits
        gc.fillOval(x + 10, y + 23, 3, 2);
        gc.fillOval(x + 16, y + 24, 2, 2);
        gc.fillOval(x + 20, y + 22, 2, 3);
    }

    /**
     * Render escaping cat animation - cat runs toward the toy.
     */
    private void renderEscapingCat(GraphicsContext gc) {
        // Cat moves toward the toy
        double targetX = toyX - 20;
        double targetY = toyY + 10;

        // Calculate movement (cat gets faster as it approaches toy)
        double dx = targetX - catEscapeX;
        double dy = targetY - catEscapeY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 5) {
            // Move cat toward toy
            double speed = Math.min(4, 1 + catEscapeFrame * 0.05);
            catEscapeX += (dx / distance) * speed;
            catEscapeY += (dy / distance) * speed;
        }

        // Running leg animation
        double legCycle = Math.sin(catEscapeFrame * 0.4) * 4;

        // Cat body (slightly stretched when running)
        gc.setFill(Color.rgb(255, 165, 0)); // Orange cat

        // Body
        gc.fillOval(catEscapeX + 2, catEscapeY + 8, 24, 14);

        // Head (turned toward toy)
        gc.fillOval(catEscapeX + 20, catEscapeY + 2, 14, 12);

        // Ears (alert, pointing forward)
        gc.beginPath();
        gc.moveTo(catEscapeX + 26, catEscapeY + 4);
        gc.lineTo(catEscapeX + 30, catEscapeY - 4);
        gc.lineTo(catEscapeX + 32, catEscapeY + 4);
        gc.closePath();
        gc.fill();
        gc.beginPath();
        gc.moveTo(catEscapeX + 30, catEscapeY + 2);
        gc.lineTo(catEscapeX + 36, catEscapeY - 2);
        gc.lineTo(catEscapeX + 34, catEscapeY + 6);
        gc.closePath();
        gc.fill();

        // Inner ears (pink)
        gc.setFill(Color.rgb(255, 182, 193));
        gc.beginPath();
        gc.moveTo(catEscapeX + 27, catEscapeY + 3);
        gc.lineTo(catEscapeX + 30, catEscapeY - 2);
        gc.lineTo(catEscapeX + 31, catEscapeY + 4);
        gc.closePath();
        gc.fill();

        // Tail (up and excited)
        gc.setFill(Color.rgb(255, 165, 0));
        double tailWave = Math.sin(catEscapeFrame * 0.3) * 3;
        gc.fillOval(catEscapeX - 4, catEscapeY + 8, 8, 6);
        gc.fillOval(catEscapeX - 8 + tailWave, catEscapeY + 2, 6, 8);
        gc.fillOval(catEscapeX - 10 + tailWave * 1.5, catEscapeY - 4, 5, 8);

        // Legs (running animation)
        gc.setFill(Color.rgb(255, 165, 0));
        // Front legs
        gc.fillOval(catEscapeX + 18, catEscapeY + 16 + legCycle, 5, 8);
        gc.fillOval(catEscapeX + 22, catEscapeY + 16 - legCycle, 5, 8);
        // Back legs
        gc.fillOval(catEscapeX + 4, catEscapeY + 16 - legCycle, 5, 8);
        gc.fillOval(catEscapeX + 8, catEscapeY + 16 + legCycle, 5, 8);

        // Eyes (wide and excited)
        gc.setFill(Color.rgb(50, 205, 50)); // Green eyes
        gc.fillOval(catEscapeX + 24, catEscapeY + 5, 4, 5);
        gc.fillOval(catEscapeX + 30, catEscapeY + 5, 4, 5);
        // Pupils (dilated - excited)
        gc.setFill(Color.BLACK);
        gc.fillOval(catEscapeX + 25, catEscapeY + 6, 3, 4);
        gc.fillOval(catEscapeX + 31, catEscapeY + 6, 3, 4);

        // Nose
        gc.setFill(Color.rgb(255, 105, 180));
        gc.beginPath();
        gc.moveTo(catEscapeX + 34, catEscapeY + 9);
        gc.lineTo(catEscapeX + 32, catEscapeY + 11);
        gc.lineTo(catEscapeX + 36, catEscapeY + 11);
        gc.closePath();
        gc.fill();

        // Whiskers (flowing back from speed)
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.setLineWidth(1);
        gc.strokeLine(catEscapeX + 34, catEscapeY + 10, catEscapeX + 42, catEscapeY + 8);
        gc.strokeLine(catEscapeX + 34, catEscapeY + 11, catEscapeX + 42, catEscapeY + 11);
        gc.strokeLine(catEscapeX + 34, catEscapeY + 12, catEscapeX + 42, catEscapeY + 14);
    }

    /**
     * Render the cat toy.
     */
    private void renderToy(GraphicsContext gc) {
        switch (toyType) {
            case 0 -> renderYarnBall(gc);
            case 1 -> renderToyMouse(gc);
            case 2 -> renderFeatherToy(gc);
        }
    }

    private void renderYarnBall(GraphicsContext gc) {
        // Bouncing animation
        double bounce = Math.abs(Math.sin(catEscapeFrame * 0.1)) * 5;

        // Main yarn ball
        gc.setFill(Color.rgb(255, 100, 150)); // Pink yarn
        gc.fillOval(toyX, toyY - bounce, 20, 20);

        // Yarn texture lines
        gc.setStroke(Color.rgb(255, 150, 180));
        gc.setLineWidth(1);
        gc.strokeArc(toyX + 2, toyY + 2 - bounce, 16, 16, 30, 120, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(toyX + 4, toyY + 4 - bounce, 12, 12, 60, 180, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(toyX + 3, toyY + 5 - bounce, 14, 10, -30, 90, javafx.scene.shape.ArcType.OPEN);

        // Trailing yarn string
        gc.setStroke(Color.rgb(255, 100, 150));
        gc.setLineWidth(2);
        double stringWave = Math.sin(catEscapeFrame * 0.15) * 8;
        gc.beginPath();
        gc.moveTo(toyX + 5, toyY + 18 - bounce);
        gc.bezierCurveTo(
            toyX - 10 + stringWave, toyY + 30,
            toyX - 20 - stringWave, toyY + 40,
            toyX - 30, toyY + 35
        );
        gc.stroke();
    }

    private void renderToyMouse(GraphicsContext gc) {
        // Jiggle animation
        double jiggle = Math.sin(catEscapeFrame * 0.2) * 2;

        // Mouse body (grey)
        gc.setFill(Color.rgb(150, 150, 150));
        gc.fillOval(toyX + jiggle, toyY, 22, 14);

        // Mouse head
        gc.fillOval(toyX + 18 + jiggle, toyY + 2, 10, 10);

        // Ears
        gc.setFill(Color.rgb(255, 180, 180)); // Pink inner ear
        gc.fillOval(toyX + 20 + jiggle, toyY - 2, 6, 6);
        gc.fillOval(toyX + 26 + jiggle, toyY - 2, 6, 6);

        // Eyes (beady)
        gc.setFill(Color.BLACK);
        gc.fillOval(toyX + 24 + jiggle, toyY + 5, 3, 3);

        // Nose
        gc.setFill(Color.rgb(255, 150, 150));
        gc.fillOval(toyX + 27 + jiggle, toyY + 7, 4, 3);

        // Tail (curvy)
        gc.setStroke(Color.rgb(255, 180, 180));
        gc.setLineWidth(2);
        double tailWave = Math.sin(catEscapeFrame * 0.15) * 5;
        gc.beginPath();
        gc.moveTo(toyX + jiggle, toyY + 7);
        gc.bezierCurveTo(
            toyX - 10 + tailWave, toyY + 5,
            toyX - 15 - tailWave, toyY + 15,
            toyX - 25, toyY + 10
        );
        gc.stroke();

        // Whiskers
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.setLineWidth(1);
        gc.strokeLine(toyX + 28 + jiggle, toyY + 6, toyX + 34 + jiggle, toyY + 4);
        gc.strokeLine(toyX + 28 + jiggle, toyY + 8, toyX + 34 + jiggle, toyY + 8);
    }

    private void renderFeatherToy(GraphicsContext gc) {
        // Swaying animation
        double sway = Math.sin(catEscapeFrame * 0.08) * 15;

        // Stick
        gc.setStroke(Color.rgb(139, 90, 43)); // Brown stick
        gc.setLineWidth(3);
        gc.strokeLine(toyX + 10, toyY + 30, toyX + 10 + sway * 0.3, toyY);

        // String from stick to feathers
        gc.setStroke(Color.rgb(200, 200, 200));
        gc.setLineWidth(1);
        gc.beginPath();
        gc.moveTo(toyX + 10 + sway * 0.3, toyY);
        gc.bezierCurveTo(
            toyX + 15 + sway * 0.5, toyY - 15,
            toyX + 20 + sway * 0.7, toyY - 25,
            toyX + 25 + sway, toyY - 20
        );
        gc.stroke();

        // Feathers (multiple colors)
        double featherX = toyX + 25 + sway;
        double featherY = toyY - 20;
        double featherSway = Math.sin(catEscapeFrame * 0.12) * 3;

        // Blue feather
        gc.setFill(Color.rgb(100, 150, 255));
        renderFeather(gc, featherX, featherY, featherSway, 0);

        // Purple feather
        gc.setFill(Color.rgb(180, 100, 255));
        renderFeather(gc, featherX + 5, featherY + 3, featherSway * 1.2, 15);

        // Pink feather
        gc.setFill(Color.rgb(255, 150, 200));
        renderFeather(gc, featherX - 3, featherY + 2, featherSway * 0.8, -10);

        // Sparkles around feathers
        gc.setFill(Color.YELLOW);
        double sparkle = (catEscapeFrame % 20) / 20.0;
        if (sparkle < 0.5) {
            gc.fillOval(featherX + 15 + featherSway, featherY - 10, 3, 3);
            gc.fillOval(featherX - 5 + featherSway, featherY - 5, 2, 2);
        }
    }

    private void renderFeather(GraphicsContext gc, double fx, double fy, double sway, double angle) {
        gc.save();
        gc.translate(fx, fy);
        gc.rotate(angle + sway);

        // Feather shape
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.bezierCurveTo(-4, -8, -6, -16, -2, -24);
        gc.bezierCurveTo(2, -16, 0, -8, 0, 0);
        gc.fill();

        // Feather spine
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeLine(0, 0, -1, -22);

        gc.restore();
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
     * Start cat escape animation (called when victory with broken protection in cat mode).
     */
    public void startCatEscape() {
        if (catMode && !catEscaping) {
            catEscaping = true;
            catEscapeX = x;
            catEscapeY = y;
            catEscapeFrame = 0;
            // Place toy to the right of the base (cat will run towards it)
            toyX = x + 200;
            toyY = y - 50;
            toyType = GameConstants.RANDOM.nextInt(3); // Random toy
        }
    }

    public boolean isCatEscaping() {
        return catEscaping;
    }

    public double getCatEscapeX() {
        return catEscapeX;
    }

    public double getCatEscapeY() {
        return catEscapeY;
    }

    public int getCatEscapeFrame() {
        return catEscapeFrame;
    }

    public double getToyX() {
        return toyX;
    }

    public double getToyY() {
        return toyY;
    }

    public int getToyType() {
        return toyType;
    }

    /**
     * Set cat escape state (for network sync).
     */
    public void setCatEscapeState(boolean escaping, double escapeX, double escapeY, int frame, double toyX, double toyY, int toyType) {
        this.catEscaping = escaping;
        this.catEscapeX = escapeX;
        this.catEscapeY = escapeY;
        this.catEscapeFrame = frame;
        this.toyX = toyX;
        this.toyY = toyY;
        this.toyType = toyType;
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
        this.catEscaping = false;
        this.catEscapeX = 0;
        this.catEscapeY = 0;
        this.catEscapeFrame = 0;
        this.toyX = 0;
        this.toyY = 0;
        this.toyType = 0;
    }
}
