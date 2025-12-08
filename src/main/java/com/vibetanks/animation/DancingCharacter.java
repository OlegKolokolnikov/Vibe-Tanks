package com.vibetanks.animation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Dancing character animation for game over screen.
 * Renders either aliens (if enemies won) or humans (if players won defending).
 */
public class DancingCharacter {
    public static final Color[] ALIEN_COLORS = {Color.LIME, Color.CYAN, Color.MAGENTA, Color.YELLOW};
    public static final Color[] HUMAN_COLORS = {Color.PEACHPUFF, Color.TAN, Color.SANDYBROWN, Color.WHEAT};

    private double x, y;
    private boolean isAlien;
    private int animFrame;
    private int danceStyle; // 0-2 different dance moves
    private int colorIndex;
    private Color color;

    public DancingCharacter(double x, double y, boolean isAlien, int danceStyle) {
        this.x = x;
        this.y = y;
        this.isAlien = isAlien;
        this.animFrame = 0;
        this.danceStyle = danceStyle;
        // Random colors for variety
        if (isAlien) {
            this.colorIndex = (int)(Math.random() * ALIEN_COLORS.length);
            this.color = ALIEN_COLORS[colorIndex];
        } else {
            this.colorIndex = (int)(Math.random() * HUMAN_COLORS.length);
            this.color = HUMAN_COLORS[colorIndex];
        }
    }

    /**
     * Constructor for network sync (with specific colorIndex).
     */
    public DancingCharacter(double x, double y, boolean isAlien, int animFrame, int danceStyle, int colorIndex) {
        this.x = x;
        this.y = y;
        this.isAlien = isAlien;
        this.animFrame = animFrame;
        this.danceStyle = danceStyle;
        this.colorIndex = colorIndex;
        if (isAlien) {
            this.color = ALIEN_COLORS[colorIndex % ALIEN_COLORS.length];
        } else {
            this.color = HUMAN_COLORS[colorIndex % HUMAN_COLORS.length];
        }
    }

    public void update() {
        animFrame++;
    }

    public void render(GraphicsContext gc) {
        int cycle = (animFrame / 8) % 4; // Animation cycle

        gc.save();
        gc.translate(x, y);

        if (isAlien) {
            renderAlien(gc, cycle);
        } else {
            renderHuman(gc, cycle);
        }

        gc.restore();
    }

    private void renderAlien(GraphicsContext gc, int cycle) {
        // Body bobbing
        double bob = Math.sin(animFrame * 0.3) * 3;

        // Alien body (oval)
        gc.setFill(color);
        gc.fillOval(-10, -20 + bob, 20, 25);

        // Big eyes
        gc.setFill(Color.BLACK);
        gc.fillOval(-7, -15 + bob, 6, 8);
        gc.fillOval(1, -15 + bob, 6, 8);
        gc.setFill(Color.WHITE);
        gc.fillOval(-5, -13 + bob, 2, 2);
        gc.fillOval(3, -13 + bob, 2, 2);

        // Antennae bobbing
        gc.setStroke(color);
        gc.setLineWidth(2);
        double antennaBob = Math.sin(animFrame * 0.5) * 5;
        gc.strokeLine(-5, -20 + bob, -8 + antennaBob, -30 + bob);
        gc.strokeLine(5, -20 + bob, 8 - antennaBob, -30 + bob);
        gc.setFill(color.brighter());
        gc.fillOval(-10 + antennaBob, -33 + bob, 5, 5);
        gc.fillOval(6 - antennaBob, -33 + bob, 5, 5);

        // Arms dancing
        double armAngle = Math.sin(animFrame * 0.4 + danceStyle) * 45;
        gc.setStroke(color);
        gc.setLineWidth(3);
        gc.save();
        gc.translate(-10, -10 + bob);
        gc.rotate(-45 + armAngle);
        gc.strokeLine(0, 0, -12, 0);
        gc.restore();
        gc.save();
        gc.translate(10, -10 + bob);
        gc.rotate(45 - armAngle);
        gc.strokeLine(0, 0, 12, 0);
        gc.restore();

        // Legs dancing
        double legMove = Math.sin(animFrame * 0.3 + danceStyle * 0.5) * 8;
        gc.strokeLine(-5, 5 + bob, -5 + legMove, 20);
        gc.strokeLine(5, 5 + bob, 5 - legMove, 20);
    }

    private void renderHuman(GraphicsContext gc, int cycle) {
        double bob = Math.sin(animFrame * 0.25) * 2;

        // Head
        gc.setFill(color);
        gc.fillOval(-8, -28 + bob, 16, 16);

        // Hair
        gc.setFill(Color.BROWN);
        gc.fillRect(-8, -28 + bob, 16, 6);

        // Eyes
        gc.setFill(Color.BLACK);
        gc.fillOval(-5, -22 + bob, 3, 3);
        gc.fillOval(2, -22 + bob, 3, 3);

        // Smile
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeArc(-4, -18 + bob, 8, 6, 180, 180, javafx.scene.shape.ArcType.OPEN);

        // Body
        gc.setFill(Color.DARKGREEN); // Military uniform
        gc.fillRect(-7, -12 + bob, 14, 18);

        // Arms dancing
        double armSwing = Math.sin(animFrame * 0.35 + danceStyle) * 40;
        gc.setStroke(color);
        gc.setLineWidth(4);
        gc.save();
        gc.translate(-7, -8 + bob);
        gc.rotate(-30 + armSwing);
        gc.strokeLine(0, 0, -10, 0);
        gc.restore();
        gc.save();
        gc.translate(7, -8 + bob);
        gc.rotate(30 - armSwing);
        gc.strokeLine(0, 0, 10, 0);
        gc.restore();

        // Legs dancing
        double legSwing = Math.sin(animFrame * 0.3 + danceStyle * 0.7) * 10;
        gc.setFill(Color.DARKGREEN);
        gc.save();
        gc.translate(-4, 6 + bob);
        gc.rotate(legSwing);
        gc.fillRect(-2, 0, 4, 14);
        gc.restore();
        gc.save();
        gc.translate(4, 6 + bob);
        gc.rotate(-legSwing);
        gc.fillRect(-2, 0, 4, 14);
        gc.restore();

        // Boots
        gc.setFill(Color.BLACK);
        gc.fillRect(-6 + legSwing/2, 18 + bob, 5, 4);
        gc.fillRect(1 - legSwing/2, 18 + bob, 5, 4);
    }

    // Getters for network sync
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isAlien() { return isAlien; }
    public int getAnimFrame() { return animFrame; }
    public int getDanceStyle() { return danceStyle; }
    public int getColorIndex() { return colorIndex; }
}
