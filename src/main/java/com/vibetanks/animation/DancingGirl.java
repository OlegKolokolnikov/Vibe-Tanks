package com.vibetanks.animation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Dancing girl animation for victory celebration screen.
 * Renders cheerful dancing girls when players achieve victory.
 */
public class DancingGirl {
    public static final Color[] DRESS_COLORS = {Color.RED, Color.HOTPINK, Color.CYAN, Color.YELLOW, Color.LIME, Color.ORANGE};
    public static final Color[] HAIR_COLORS = {Color.BLACK, Color.BROWN, Color.SADDLEBROWN, Color.GOLD, Color.ORANGERED};

    private double x, y;
    private int animFrame;
    private int danceStyle;
    private Color dressColor;
    private Color hairColor;
    private int dressColorIndex;
    private int hairColorIndex;

    public DancingGirl(double x, double y, int danceStyle) {
        this.x = x;
        this.y = y;
        this.animFrame = (int)(Math.random() * 60); // Random start frame for variety
        this.danceStyle = danceStyle;
        this.dressColorIndex = (int)(Math.random() * DRESS_COLORS.length);
        this.hairColorIndex = (int)(Math.random() * HAIR_COLORS.length);
        this.dressColor = DRESS_COLORS[dressColorIndex];
        this.hairColor = HAIR_COLORS[hairColorIndex];
    }

    /**
     * Constructor for network sync.
     */
    public DancingGirl(double x, double y, int animFrame, int danceStyle, int dressColorIndex, int hairColorIndex) {
        this.x = x;
        this.y = y;
        this.animFrame = animFrame;
        this.danceStyle = danceStyle;
        this.dressColorIndex = dressColorIndex;
        this.hairColorIndex = hairColorIndex;
        this.dressColor = DRESS_COLORS[dressColorIndex % DRESS_COLORS.length];
        this.hairColor = HAIR_COLORS[hairColorIndex % HAIR_COLORS.length];
    }

    public void update() {
        animFrame++;
    }

    public void render(GraphicsContext gc) {
        gc.save();
        gc.translate(x, y);

        double bob = Math.sin(animFrame * 0.2 + danceStyle) * 3;
        double sway = Math.sin(animFrame * 0.15 + danceStyle * 0.5) * 5;

        // Hair (long, flowing)
        gc.setFill(hairColor);
        double hairSway = Math.sin(animFrame * 0.1) * 8;
        gc.fillOval(-12 + hairSway * 0.3, -38 + bob, 24, 20);
        // Hair strands flowing down
        gc.fillRect(-10 + hairSway * 0.2, -28 + bob, 6, 25);
        gc.fillRect(4 + hairSway * 0.4, -28 + bob, 6, 25);

        // Face
        gc.setFill(Color.PEACHPUFF);
        gc.fillOval(-8, -36 + bob, 16, 18);

        // Eyes (cute anime style)
        gc.setFill(Color.WHITE);
        gc.fillOval(-6, -32 + bob, 5, 6);
        gc.fillOval(1, -32 + bob, 5, 6);
        gc.setFill(Color.rgb(50, 50, 150)); // Blue eyes
        gc.fillOval(-5, -31 + bob, 3, 4);
        gc.fillOval(2, -31 + bob, 3, 4);
        gc.setFill(Color.WHITE); // Eye shine
        gc.fillOval(-4, -31 + bob, 1, 1);
        gc.fillOval(3, -31 + bob, 1, 1);

        // Blush
        gc.setFill(Color.rgb(255, 180, 180, 0.6));
        gc.fillOval(-8, -27 + bob, 4, 2);
        gc.fillOval(4, -27 + bob, 4, 2);

        // Smile
        gc.setStroke(Color.rgb(200, 100, 100));
        gc.setLineWidth(1);
        gc.strokeArc(-3, -26 + bob, 6, 4, 180, 180, javafx.scene.shape.ArcType.OPEN);

        // Body/Dress (flowing)
        gc.setFill(dressColor);
        // Top of dress
        gc.fillRect(-8 + sway * 0.2, -18 + bob, 16, 12);

        // Skirt (swaying)
        double skirtSway = Math.sin(animFrame * 0.25 + danceStyle) * 8;
        gc.beginPath();
        gc.moveTo(-8 + sway * 0.2, -6 + bob);
        gc.lineTo(8 + sway * 0.2, -6 + bob);
        gc.lineTo(14 + skirtSway, 20 + bob);
        gc.lineTo(-14 - skirtSway, 20 + bob);
        gc.closePath();
        gc.fill();

        // Skirt folds
        gc.setStroke(dressColor.darker());
        gc.setLineWidth(1);
        gc.strokeLine(-4 + sway * 0.1, -6 + bob, -6 - skirtSway * 0.3, 18 + bob);
        gc.strokeLine(4 + sway * 0.1, -6 + bob, 6 + skirtSway * 0.3, 18 + bob);

        // Arms (dancing motion)
        double armAngle = Math.sin(animFrame * 0.3 + danceStyle) * 50;
        gc.setFill(Color.PEACHPUFF);
        gc.save();
        gc.translate(-8 + sway * 0.2, -14 + bob);
        gc.rotate(-60 + armAngle);
        gc.fillRect(0, 0, 4, 16);
        gc.restore();
        gc.save();
        gc.translate(8 + sway * 0.2, -14 + bob);
        gc.rotate(60 - armAngle);
        gc.fillRect(-4, 0, 4, 16);
        gc.restore();

        // Legs (under skirt, slight movement)
        double legMove = Math.sin(animFrame * 0.2 + danceStyle * 0.5) * 3;
        gc.setFill(Color.PEACHPUFF);
        gc.fillRect(-5 + legMove, 18 + bob, 4, 10);
        gc.fillRect(1 - legMove, 18 + bob, 4, 10);

        // Shoes
        gc.setFill(dressColor.darker());
        gc.fillRect(-6 + legMove, 27 + bob, 5, 3);
        gc.fillRect(0 - legMove, 27 + bob, 5, 3);

        gc.restore();
    }

    // Getters for network sync
    public double getX() { return x; }
    public double getY() { return y; }
    public int getAnimFrame() { return animFrame; }
    public int getDanceStyle() { return danceStyle; }
    public int getDressColorIndex() { return dressColorIndex; }
    public int getHairColorIndex() { return hairColorIndex; }
}
