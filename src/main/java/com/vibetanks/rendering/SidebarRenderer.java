package com.vibetanks.rendering;

import com.vibetanks.core.GameConstants;
import com.vibetanks.core.Tank;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Random;

/**
 * Renders the sidebar like in original Battle City.
 * Shows remaining enemies, player lives, and level number with flag.
 */
public class SidebarRenderer {
    private final GraphicsContext gc;
    private final int gameFieldWidth;
    private final int sidebarWidth;
    private final int height;

    // Colors matching original Battle City sidebar
    private static final Color SIDEBAR_BG = Color.rgb(99, 99, 99);  // Gray background
    private static final Color ENEMY_ICON_COLOR = Color.BLACK;
    private static final Color PLAYER_ICON_COLOR = Color.rgb(255, 180, 0);  // Orange/yellow for player icon
    private static final Color TEXT_COLOR = Color.BLACK;
    private static final Color FLAG_POLE_COLOR = Color.BLACK;
    private static final Color FLAG_COLOR = Color.rgb(255, 180, 0);  // Orange flag

    // Layout constants
    private static final int ENEMY_ICON_SIZE = 18;  // Bigger icons to fill space
    private static final int ENEMY_ICON_SPACING = 6;
    private static final int ENEMY_COLUMNS = 2;
    private static final int SECTION_PADDING = 10;

    public SidebarRenderer(GraphicsContext gc, int gameFieldWidth, int height) {
        this.gc = gc;
        this.gameFieldWidth = gameFieldWidth;
        this.sidebarWidth = GameConstants.SIDEBAR_WIDTH;
        this.height = height;
    }

    // Fixed Y positions for each section (static layout)
    private static final int ENEMY_SECTION_Y = 10;
    private static final int PLAYER_SECTION_Y = 550;  // Fixed position in lower part of sidebar
    private static final int FLAG_SECTION_Y = 710;    // Near bottom (with room for level number below)

    /**
     * Render the complete sidebar.
     * @param remainingEnemies Number of enemies not yet spawned
     * @param playerTanks List of player tanks
     * @param levelNumber Current level number
     */
    public void render(int remainingEnemies, List<Tank> playerTanks, int levelNumber) {
        double sidebarX = gameFieldWidth;

        // Draw sidebar background
        gc.setFill(SIDEBAR_BG);
        gc.fillRect(sidebarX, 0, sidebarWidth, height);

        // Render enemy icons at top (fixed position)
        renderEnemyIcons(sidebarX, ENEMY_SECTION_Y, remainingEnemies);

        // Render player lives at fixed position
        renderPlayerLives(sidebarX, PLAYER_SECTION_Y, playerTanks);

        // Render flag with level number at fixed position near bottom
        renderFlag(sidebarX, FLAG_SECTION_Y, levelNumber);
    }

    /**
     * Render enemy tank icons in a 2-column grid.
     * Each icon represents one enemy that hasn't spawned yet.
     */
    private double renderEnemyIcons(double sidebarX, double startY, int remainingEnemies) {
        double iconX = sidebarX + SECTION_PADDING;
        double iconY = startY;

        int iconsPerColumn = (int) Math.ceil(remainingEnemies / (double) ENEMY_COLUMNS);
        int iconCount = 0;

        for (int i = 0; i < remainingEnemies; i++) {
            int col = i % ENEMY_COLUMNS;
            int row = i / ENEMY_COLUMNS;

            double x = iconX + col * (ENEMY_ICON_SIZE + ENEMY_ICON_SPACING);
            double y = iconY + row * (ENEMY_ICON_SIZE + ENEMY_ICON_SPACING);

            drawEnemyIcon(x, y, ENEMY_ICON_SIZE);
            iconCount++;
        }

        // Return Y position after the last row of icons
        int rows = (int) Math.ceil(remainingEnemies / (double) ENEMY_COLUMNS);
        return startY + rows * (ENEMY_ICON_SIZE + ENEMY_ICON_SPACING);
    }

    /**
     * Draw a small enemy tank icon (simplified silhouette).
     */
    private void drawEnemyIcon(double x, double y, int size) {
        gc.setFill(ENEMY_ICON_COLOR);

        // Tank body
        double bodyWidth = size * 0.7;
        double bodyHeight = size * 0.6;
        gc.fillRect(x + (size - bodyWidth) / 2, y + size * 0.3, bodyWidth, bodyHeight);

        // Tank tracks (left and right)
        double trackWidth = size * 0.2;
        gc.fillRect(x, y + size * 0.2, trackWidth, size * 0.7);
        gc.fillRect(x + size - trackWidth, y + size * 0.2, trackWidth, size * 0.7);

        // Barrel
        double barrelWidth = size * 0.2;
        double barrelHeight = size * 0.4;
        gc.fillRect(x + (size - barrelWidth) / 2, y, barrelWidth, barrelHeight);
    }

    /**
     * Render player lives section.
     * Shows player icons with remaining lives count.
     */
    private double renderPlayerLives(double sidebarX, double startY, List<Tank> playerTanks) {
        double yOffset = startY;

        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);

            // Draw player indicator (I, II, III, IV in Roman numerals style)
            drawPlayerIcon(sidebarX + SECTION_PADDING, yOffset, i + 1);

            // Draw lives count
            int displayLives = Math.max(0, player.getLives() - 1);
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            gc.fillText(String.valueOf(displayLives), sidebarX + sidebarWidth - 20, yOffset + 18);

            yOffset += 35;
        }

        return yOffset;
    }

    /**
     * Draw a player icon with player number indication.
     * Uses "IP", "IIP" style like original Battle City.
     */
    private void drawPlayerIcon(double x, double y, int playerNumber) {
        gc.setFill(PLAYER_ICON_COLOR);

        // Draw "I" marks for player number
        String romanNumeral = getRomanNumeral(playerNumber);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText(romanNumeral, x, y + 12);

        // Draw "P" for player
        gc.fillText("P", x + romanNumeral.length() * 8 + 2, y + 12);

        // Draw small tank icon below
        drawPlayerTankIcon(x, y + 15, 20, playerNumber);
    }

    /**
     * Get Roman numeral for player number.
     */
    private String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(number);
        };
    }

    /**
     * Draw a small player tank icon.
     */
    private void drawPlayerTankIcon(double x, double y, int size, int playerNumber) {
        // Player colors
        Color tankColor = switch (playerNumber) {
            case 1 -> Color.GOLD;
            case 2 -> Color.LIMEGREEN;
            case 3 -> Color.DEEPSKYBLUE;
            case 4 -> Color.HOTPINK;
            default -> Color.GOLD;
        };

        gc.setFill(tankColor);

        // Simplified tank shape
        double bodyW = size * 0.6;
        double bodyH = size * 0.5;
        gc.fillRect(x + (size - bodyW) / 2, y + size * 0.35, bodyW, bodyH);

        // Tracks
        double trackW = size * 0.15;
        gc.fillRect(x + 2, y + size * 0.25, trackW, size * 0.6);
        gc.fillRect(x + size - trackW - 2, y + size * 0.25, trackW, size * 0.6);

        // Barrel
        double barrelW = size * 0.15;
        gc.fillRect(x + (size - barrelW) / 2, y, barrelW, size * 0.45);
    }

    /**
     * Render flag with level number at the bottom of sidebar.
     * Level number appears under the flag.
     * Flag colors change based on level:
     * - Normal levels: 1 random color
     * - Levels divisible by 10: 2 colors (striped)
     * - Levels divisible by 100: 3 colors (striped)
     * Colors are seeded by level number for multiplayer sync.
     */
    private void renderFlag(double sidebarX, double y, int levelNumber) {
        double flagX = sidebarX + SECTION_PADDING;

        // Flag pole
        gc.setFill(FLAG_POLE_COLOR);
        gc.fillRect(flagX + 10, y, 3, 40);

        // Determine number of colors based on level
        int colorCount = 1;
        if (levelNumber % 100 == 0 && levelNumber > 0) {
            colorCount = 3;
        } else if (levelNumber % 10 == 0 && levelNumber > 0) {
            colorCount = 2;
        }

        // Generate colors seeded by level number (ensures sync across all players)
        Color[] flagColors = generateFlagColors(levelNumber, colorCount);

        // Draw flag with stripes
        drawStripedFlag(flagX + 13, y + 3, 17, 18, flagColors);

        // Level number under the flag
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        gc.fillText(String.valueOf(levelNumber), flagX + 5, y + 65);
    }

    /**
     * Generate random colors for flag, seeded by level number for consistency.
     */
    private Color[] generateFlagColors(int levelNumber, int colorCount) {
        // Use level number as seed for deterministic colors across all clients
        Random random = new Random(levelNumber * 12345L);
        Color[] colors = new Color[colorCount];

        for (int i = 0; i < colorCount; i++) {
            // Generate vibrant colors (avoid too dark or too light)
            double hue = random.nextDouble() * 360;
            double saturation = 0.7 + random.nextDouble() * 0.3;  // 70-100%
            double brightness = 0.7 + random.nextDouble() * 0.3;  // 70-100%
            colors[i] = Color.hsb(hue, saturation, brightness);
        }

        return colors;
    }

    /**
     * Draw a flag with horizontal stripes.
     */
    private void drawStripedFlag(double x, double y, double width, double height, Color[] colors) {
        int stripeCount = colors.length;
        double stripeHeight = height / stripeCount;

        for (int i = 0; i < stripeCount; i++) {
            gc.setFill(colors[i]);

            // Calculate stripe polygon (triangular flag shape)
            double stripeY = y + i * stripeHeight;
            double nextStripeY = y + (i + 1) * stripeHeight;

            // For triangular flag, calculate x positions based on y
            double leftX = x;
            double rightTopX = x + width * (1 - (stripeY - y) / height);
            double rightBottomX = x + width * (1 - (nextStripeY - y) / height);

            // Draw stripe as polygon
            double[] xPoints = {leftX, rightTopX, rightBottomX, leftX};
            double[] yPoints = {stripeY, stripeY, nextStripeY, nextStripeY};
            gc.fillPolygon(xPoints, yPoints, 4);
        }
    }
}
