package com.vibetanks.rendering;

import com.vibetanks.core.PowerUp;
import com.vibetanks.core.Tank;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Renders end-game statistics table and boss kill info.
 * Extracted from Game.java to reduce complexity.
 */
public class StatsRenderer {
    private final GraphicsContext gc;
    private final IconRenderer iconRenderer;
    private final int width;

    public StatsRenderer(GraphicsContext gc, IconRenderer iconRenderer, int width) {
        this.gc = gc;
        this.iconRenderer = iconRenderer;
        this.width = width;
    }

    /**
     * Render end-game statistics table.
     * @param startY Y position to start rendering
     * @param activePlayers Number of active players
     * @param playerNames Array of player display names
     * @param playerKills Array of player kill counts
     * @param playerScores Array of player total scores
     * @param playerLevelScores Array of player level scores
     * @param playerKillsByType Matrix of kills by enemy type [playerIndex][enemyTypeOrdinal]
     * @param victory Whether this is victory screen (for winner highlighting)
     * @param winnerIndex Index of winner (-1 if no winner or tie)
     * @param isTie Whether there's a tie for winner
     */
    public void renderEndGameStats(double startY, int activePlayers, String[] playerNames,
                                    int[] playerKills, int[] playerScores, int[] playerLevelScores,
                                    int[][] playerKillsByType, boolean victory, int winnerIndex, boolean isTie) {
        if (activePlayers == 0) return;

        // Table dimensions and position - wider columns for better visibility
        double tableX = width / 2.0 - 320;
        double tableY = startY;
        double rowHeight = 28;
        // Name, REG, ARM, FST, PWR, HVY, BSS, Total, LvlPts, TotalPts
        double[] colWidths = {110, 35, 35, 35, 35, 35, 35, 55, 60, 70};
        double totalWidth = 0;
        for (double w : colWidths) totalWidth += w;

        // Draw table background with border
        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRoundRect(tableX - 10, tableY - 10, totalWidth + 20, (activePlayers + 2) * rowHeight + 25, 15, 15);
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeRoundRect(tableX - 10, tableY - 10, totalWidth + 20, (activePlayers + 2) * rowHeight + 25, 15, 15);

        // Draw header with larger font
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setFill(Color.GOLD);

        double xPos = tableX;
        // First column is text "PLAYER"
        gc.fillText("PLAYER", xPos + 5, tableY + 18);
        xPos += colWidths[0];

        // Draw mini tank icons for each enemy type column (centered)
        Tank.EnemyType[] enemyTypes = {Tank.EnemyType.REGULAR, Tank.EnemyType.ARMORED, Tank.EnemyType.FAST,
                                        Tank.EnemyType.POWER, Tank.EnemyType.HEAVY, Tank.EnemyType.BOSS};
        for (int t = 0; t < 6; t++) {
            iconRenderer.drawMiniTank(xPos + (colWidths[t + 1] - 18) / 2, tableY + 2, enemyTypes[t]);
            xPos += colWidths[t + 1];
        }

        // Text headers for kills and points columns (centered)
        String[] textHeaders = {"KILLS", "LEVEL", "TOTAL"};
        for (int c = 0; c < textHeaders.length; c++) {
            gc.setFill(Color.GOLD);
            gc.fillText(textHeaders[c], xPos + 2, tableY + 18);
            xPos += colWidths[7 + c];
        }

        // Draw header line
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeLine(tableX - 5, tableY + 24, tableX + totalWidth + 5, tableY + 24);

        // Draw player rows with larger font
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        int totalKills = 0;
        int[] totalByType = new int[6];
        int totalLevelPoints = 0;
        int totalPoints = 0;

        Color[] typeColors = {Color.LIGHTGRAY, Color.SILVER, Color.LIGHTBLUE, Color.MAGENTA, Color.DARKGRAY, Color.RED};

        for (int i = 0; i < activePlayers; i++) {
            double rowY = tableY + 45 + i * rowHeight;
            xPos = tableX;

            // Player name color (cyan for all, winner gets gold medal)
            gc.setFill(Color.CYAN);

            // Get player name (truncate if too long)
            String name = playerNames[i];
            if (name.length() > 10) name = name.substring(0, 9) + "..";

            // Add gold medal for winner (only if not a tie)
            if (victory && activePlayers > 1 && !isTie && i == winnerIndex) {
                name = "\uD83E\uDD47 " + name; // Gold medal emoji
            }
            gc.fillText(name, xPos + 5, rowY);
            xPos += colWidths[0];

            // Kills by type columns (colors for each type, centered)
            for (int t = 0; t < 6; t++) {
                int killCount = playerKillsByType[i][t];
                totalByType[t] += killCount;
                gc.setFill(killCount > 0 ? typeColors[t] : Color.GRAY);
                String numStr = String.valueOf(killCount);
                gc.fillText(numStr, xPos + (colWidths[t + 1] - numStr.length() * 8) / 2, rowY);
                xPos += colWidths[t + 1];
            }

            // Total kills (centered)
            int kills = playerKills[i];
            totalKills += kills;
            gc.setFill(Color.WHITE);
            String killsStr = String.valueOf(kills);
            gc.fillText(killsStr, xPos + (colWidths[7] - killsStr.length() * 8) / 2, rowY);
            xPos += colWidths[7];

            // Level Points (centered)
            int levelScore = playerLevelScores[i];
            totalLevelPoints += levelScore;
            gc.setFill(Color.LIME);
            String lvlStr = String.valueOf(levelScore);
            gc.fillText(lvlStr, xPos + (colWidths[8] - lvlStr.length() * 8) / 2, rowY);
            xPos += colWidths[8];

            // Total Points (centered, larger font)
            int score = playerScores[i];
            totalPoints += score;
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            String scoreStr = String.valueOf(score);
            gc.fillText(scoreStr, xPos + (colWidths[9] - scoreStr.length() * 10) / 2, rowY);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        }

        // Draw totals row
        double totalsY = tableY + 45 + activePlayers * rowHeight + 8;
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeLine(tableX - 5, totalsY - 20, tableX + totalWidth + 5, totalsY - 20);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setFill(Color.GOLD);
        xPos = tableX;
        gc.fillText("TOTAL", xPos + 5, totalsY);
        xPos += colWidths[0];

        // Total by type (centered)
        for (int t = 0; t < 6; t++) {
            String numStr = String.valueOf(totalByType[t]);
            gc.fillText(numStr, xPos + (colWidths[t + 1] - numStr.length() * 8) / 2, totalsY);
            xPos += colWidths[t + 1];
        }

        // Total kills (centered)
        String totalKillsStr = String.valueOf(totalKills);
        gc.fillText(totalKillsStr, xPos + (colWidths[7] - totalKillsStr.length() * 8) / 2, totalsY);
        xPos += colWidths[7];

        // Total level points (centered)
        gc.setFill(Color.LIME);
        String totalLvlStr = String.valueOf(totalLevelPoints);
        gc.fillText(totalLvlStr, xPos + (colWidths[8] - totalLvlStr.length() * 8) / 2, totalsY);
        xPos += colWidths[8];

        // Grand total points (centered)
        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        String totalPtsStr = String.valueOf(totalPoints);
        gc.fillText(totalPtsStr, xPos + (colWidths[9] - totalPtsStr.length() * 10) / 2, totalsY);
    }

    /**
     * Render boss kill reward info box at top of screen.
     * @param bossKillerName Name of player who killed the boss
     * @param powerUpReward The power-up reward type
     */
    public void renderBossKillInfo(String bossKillerName, PowerUp.Type powerUpReward) {
        String powerUpName = getPowerUpDisplayName(powerUpReward);

        // Draw at top of screen with background box for visibility
        double boxX = width / 2.0 - 150;
        double boxY = 10;
        double boxWidth = 300;
        double boxHeight = 55;

        // Semi-transparent background
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

        // Gold border
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

        // Boss slain text
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.fillText("BOSS slain by " + bossKillerName + "!", boxX + 20, boxY + 25);

        // Reward text
        gc.setFill(Color.MAGENTA);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText("Reward: " + powerUpName, boxX + 20, boxY + 45);
    }

    private String getPowerUpDisplayName(PowerUp.Type type) {
        return switch (type) {
            case GUN -> "Gun (break steel)";
            case STAR -> "Star (faster shots)";
            case CAR -> "Car (speed boost)";
            case SHIP -> "Ship (swim)";
            case SAW -> "Saw (cut trees)";
            case TANK -> "Tank (extra life)";
            case SHIELD -> "Shield (protection)";
            case MACHINEGUN -> "Machinegun (wrap shots)";
            case SHOVEL -> "Shovel (steel base)";
            case FREEZE -> "Freeze";
            case BOMB -> "Bomb";
            case LASER -> "Laser (beam attack)";
        };
    }

    /**
     * Calculate winner index and tie status for stats display.
     * @param playerKills Array of player kill counts
     * @param activePlayers Number of active players
     * @return int array: [0] = winnerIndex (-1 if tie or single player), [1] = 1 if tie, 0 otherwise
     */
    public static int[] calculateWinner(int[] playerKills, int activePlayers) {
        int winnerIndex = -1;
        int highestKills = -1;
        boolean isTie = false;

        if (activePlayers > 1) {
            for (int i = 0; i < activePlayers; i++) {
                int kills = playerKills[i];
                if (kills > highestKills) {
                    highestKills = kills;
                    winnerIndex = i;
                    isTie = false;
                } else if (kills == highestKills) {
                    isTie = true;
                }
            }
        }

        return new int[]{isTie ? -1 : winnerIndex, isTie ? 1 : 0};
    }
}
