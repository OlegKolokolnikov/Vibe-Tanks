package com.vibetanks.rendering;

import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.network.NetworkManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Handles all game rendering including the main game view, UI, and end-game screens.
 * Extracted from Game.java to improve separation of concerns.
 */
public class GameRenderer {
    private final GraphicsContext gc;
    private final int width;
    private final int height;
    private final EffectRenderer effectRenderer;
    private final IconRenderer iconRenderer;

    public GameRenderer(GraphicsContext gc, int width, int height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
        this.effectRenderer = new EffectRenderer(gc, width, height);
        this.iconRenderer = new IconRenderer(gc);
    }

    /**
     * Get the effect renderer for special effects.
     */
    public EffectRenderer getEffectRenderer() {
        return effectRenderer;
    }

    /**
     * Get the icon renderer for UI icons.
     */
    public IconRenderer getIconRenderer() {
        return iconRenderer;
    }

    /**
     * Clear the canvas with black background.
     */
    public void clear() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);
    }

    /**
     * Render the main game world (map, entities, effects).
     */
    public void renderGameWorld(GameMap gameMap, Base base, List<PowerUp> powerUps,
                                 EasterEgg easterEgg, List<Bullet> bullets, List<Laser> lasers,
                                 List<Tank> playerTanks, List<Tank> enemyTanks,
                                 int playerFreezeDuration, int enemyFreezeDuration,
                                 UFO ufo, int ufoLostMessageTimer, int ufoKilledMessageTimer) {
        // Clear canvas
        clear();

        // Render map WITHOUT trees (trees will be rendered on top of tanks)
        gameMap.renderWithoutTrees(gc);

        // Render base
        base.render(gc);

        // Render power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.render(gc);
        }

        // Render easter egg
        if (easterEgg != null) {
            easterEgg.render(gc);
        }

        // Render bullets
        for (Bullet bullet : bullets) {
            bullet.render(gc);
        }

        // Render lasers
        for (Laser laser : lasers) {
            laser.render(gc);
        }

        // Render player tanks
        for (Tank tank : playerTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
                // Draw ice effect if players are frozen
                if (playerFreezeDuration > 0) {
                    effectRenderer.renderFreezeEffect(tank);
                }
            }
        }

        // Render enemy tanks
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
                // Draw ice effect if enemies are frozen (except BOSS which is immune)
                if (enemyFreezeDuration > 0 && tank.getEnemyType() != Tank.EnemyType.BOSS) {
                    effectRenderer.renderFreezeEffect(tank);
                }
            }
        }

        // Render UFO (above tanks, below trees)
        if (ufo != null && ufo.isAlive()) {
            ufo.render(gc);
        }

        // Render "Lost it!" message when UFO escapes
        if (ufoLostMessageTimer > 0) {
            effectRenderer.renderUfoLostMessage(ufoLostMessageTimer);
        }

        // Render "Zed is dead!" message when UFO is killed
        if (ufoKilledMessageTimer > 0) {
            effectRenderer.renderUfoKilledMessage(ufoKilledMessageTimer);
        }

        // Render trees ON TOP of tanks to make tanks partially visible in forest
        gameMap.renderTrees(gc);

        // Render burning trees with fire animation (on top of everything)
        gameMap.renderBurningTiles(gc);
    }

    /**
     * Render the HUD (heads-up display) with player stats and power-ups.
     */
    public void renderHUD(GameMap gameMap, EnemySpawner enemySpawner, List<Tank> playerTanks,
                          List<Tank> enemyTanks, int displayCount,
                          int[] playerKills, int[] playerScores,
                          PlayerDisplayNameProvider nameProvider) {
        gc.setFill(Color.WHITE);
        gc.fillText("Level: " + gameMap.getLevelNumber() + "  Enemies: " + enemySpawner.getRemainingEnemies(), 10, 20);

        // Display player info and power-ups
        for (int i = 0; i < displayCount && i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            String playerName = nameProvider.getDisplayName(i);
            double yOffset = 40 + i * 60;

            // Display lives, kills and score
            int displayLives = Math.max(0, player.getLives() - 1);
            gc.setFill(Color.WHITE);
            gc.fillText(playerName + " Lives: " + displayLives + "  Kills: " + playerKills[i] + "  Score: " + playerScores[i], 10, yOffset);

            // Display power-ups
            double xOffset = 10;
            yOffset += 10;

            if (player.hasGun()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.GUN);
                xOffset += 20;
            }
            if (player.getStarCount() > 0) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.STAR);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getStarCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
            if (player.getCarCount() > 0) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.CAR);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getCarCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
            if (player.hasShip()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SHIP);
                xOffset += 20;
            }
            if (player.hasSaw()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SAW);
                xOffset += 20;
            }
            if (player.hasShield()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SHIELD);
                xOffset += 20;
            }
            if (player.getMachinegunCount() > 0) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.MACHINEGUN);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getMachinegunCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
        }

        // Render BOSS health indicator if BOSS is alive
        effectRenderer.renderBossHealthBar(enemyTanks);
    }

    /**
     * Render the game over screen.
     */
    public void renderGameOver(List<DancingCharacter> dancingCharacters) {
        // Update and render dancing characters
        for (DancingCharacter dancer : dancingCharacters) {
            dancer.update();
            dancer.render(gc);
        }

        // Render laughing skull
        effectRenderer.renderLaughingSkull(width / 2.0, height / 2.0 - 150);

        gc.setFill(Color.RED);
        gc.setFont(Font.font(40));
        gc.fillText("GAME OVER", width / 2.0 - 120, height / 2.0 + 50);
    }

    /**
     * Render the game over prompt.
     */
    public void renderGameOverPrompt() {
        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font(22));
        gc.fillText("Press ENTER to restart", width / 2.0 - 110, height / 2.0 + 310);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Press ESC to return to menu", width / 2.0 - 115, height / 2.0 + 340);
    }

    /**
     * Render the victory screen.
     */
    public void renderVictory(GameMap gameMap, List<DancingGirl> victoryDancingGirls) {
        // Update and render dancing girls
        for (DancingGirl girl : victoryDancingGirls) {
            girl.update();
            girl.render(gc);
        }

        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font(40));
        gc.fillText("LEVEL " + gameMap.getLevelNumber() + " COMPLETE!", width / 2.0 - 180, height / 2.0 + 50);
    }

    /**
     * Render the victory prompt.
     */
    public void renderVictoryPrompt() {
        gc.setFill(Color.LIME);
        gc.setFont(Font.font(22));
        gc.fillText("Press ENTER for next level", width / 2.0 - 130, height / 2.0 + 310);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Press ESC to return to menu", width / 2.0 - 115, height / 2.0 + 340);
    }

    /**
     * Render the pause menu overlay.
     */
    public void renderPauseMenu(int pauseMenuSelection) {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 50));
        gc.fillText("PAUSED", width / 2.0 - 100, height / 2.0 - 80);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        // Resume option
        if (pauseMenuSelection == 0) {
            gc.setFill(Color.YELLOW);
            gc.fillText("> RESUME <", width / 2.0 - 80, height / 2.0);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillText("  RESUME  ", width / 2.0 - 80, height / 2.0);
        }

        // Exit option
        if (pauseMenuSelection == 1) {
            gc.setFill(Color.YELLOW);
            gc.fillText("> EXIT <", width / 2.0 - 60, height / 2.0 + 50);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillText("  EXIT  ", width / 2.0 - 60, height / 2.0 + 50);
        }

        gc.setFill(Color.GRAY);
        gc.setFont(Font.font(16));
        gc.fillText("Use UP/DOWN to select, ENTER to confirm", width / 2.0 - 150, height / 2.0 + 120);
    }

    /**
     * Render the "take life from teammate" hint.
     */
    public void renderTakeLifeHint() {
        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font(20));
        gc.fillText("Press ENTER to take life from teammate", width / 2.0 - 180, height / 2.0);
    }

    /**
     * Render the multiplayer pause indicator.
     */
    public void renderMultiplayerPauseIndicator() {
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(0, 0, width, 60);
        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        gc.fillText("PAUSED - Press ESC to resume", width / 2.0 - 200, 40);
    }

    /**
     * Render the end-game statistics table.
     */
    public void renderEndGameStats(double startY, int activePlayers, boolean victory,
                                    int[] playerKills, int[] playerScores, int[] playerLevelScores,
                                    int[][] playerKillsByType, boolean winnerBonusAwarded,
                                    PlayerDisplayNameProvider nameProvider,
                                    WinnerBonusCallback winnerBonusCallback) {
        if (activePlayers == 0) return;

        // Find winner (highest kills) - only if victory and more than 1 player
        int winnerIndex = -1;
        int highestKills = -1;
        boolean isTie = false;

        if (victory && activePlayers > 1) {
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

            // Award winner bonus (10 points) if no tie - only once
            if (!isTie && winnerIndex >= 0 && !winnerBonusAwarded && winnerBonusCallback != null) {
                winnerBonusCallback.awardWinnerBonus(winnerIndex);
            }
        }

        // Table dimensions and position
        double tableX = width / 2.0 - 320;
        double tableY = startY;
        double rowHeight = 28;
        double[] colWidths = {110, 35, 35, 35, 35, 35, 35, 55, 60, 70};
        double totalWidth = 0;
        for (double w : colWidths) totalWidth += w;

        // Draw table background with border
        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRoundRect(tableX - 10, tableY - 10, totalWidth + 20, (activePlayers + 2) * rowHeight + 25, 15, 15);
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeRoundRect(tableX - 10, tableY - 10, totalWidth + 20, (activePlayers + 2) * rowHeight + 25, 15, 15);

        // Draw header
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setFill(Color.GOLD);

        double xPos = tableX;
        gc.fillText("PLAYER", xPos + 5, tableY + 18);
        xPos += colWidths[0];

        // Draw mini tank icons for each enemy type
        Tank.EnemyType[] enemyTypes = {Tank.EnemyType.REGULAR, Tank.EnemyType.ARMORED, Tank.EnemyType.FAST,
                                       Tank.EnemyType.POWER, Tank.EnemyType.HEAVY, Tank.EnemyType.BOSS};
        for (int t = 0; t < 6; t++) {
            iconRenderer.drawMiniTank(xPos + (colWidths[t + 1] - 18) / 2, tableY + 2, enemyTypes[t]);
            xPos += colWidths[t + 1];
        }

        // Text headers
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

        // Draw player rows
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        int totalKills = 0;
        int[] totalByType = new int[6];
        int totalLevelPoints = 0;
        int totalPoints = 0;

        for (int i = 0; i < activePlayers; i++) {
            double rowY = tableY + 45 + i * rowHeight;
            xPos = tableX;

            gc.setFill(Color.CYAN);

            String name = nameProvider.getDisplayName(i);
            if (name.length() > 10) name = name.substring(0, 9) + "..";

            // Add gold medal for winner
            if (victory && activePlayers > 1 && !isTie && i == winnerIndex) {
                name = "\uD83E\uDD47 " + name;
            }
            gc.fillText(name, xPos + 5, rowY);
            xPos += colWidths[0];

            // Kills by type
            Color[] typeColors = {Color.LIGHTGRAY, Color.SILVER, Color.LIGHTBLUE, Color.MAGENTA, Color.DARKGRAY, Color.RED};
            for (int t = 0; t < 6; t++) {
                int killCount = playerKillsByType[i][t];
                totalByType[t] += killCount;
                gc.setFill(killCount > 0 ? typeColors[t] : Color.GRAY);
                String numStr = String.valueOf(killCount);
                gc.fillText(numStr, xPos + (colWidths[t + 1] - numStr.length() * 8) / 2, rowY);
                xPos += colWidths[t + 1];
            }

            // Total kills
            int kills = playerKills[i];
            totalKills += kills;
            gc.setFill(Color.WHITE);
            String killsStr = String.valueOf(kills);
            gc.fillText(killsStr, xPos + (colWidths[7] - killsStr.length() * 8) / 2, rowY);
            xPos += colWidths[7];

            // Level Points
            int levelScore = playerLevelScores[i];
            totalLevelPoints += levelScore;
            gc.setFill(Color.LIME);
            String lvlStr = String.valueOf(levelScore);
            gc.fillText(lvlStr, xPos + (colWidths[8] - lvlStr.length() * 8) / 2, rowY);
            xPos += colWidths[8];

            // Total Points
            int score = playerScores[i];
            totalPoints += score;
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            String scoreStr = String.valueOf(score);
            gc.fillText(scoreStr, xPos + (colWidths[9] - scoreStr.length() * 10) / 2, rowY);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        }

        // Draw totals row
        double totalsY = tableY + 45 + activePlayers * rowHeight + 5;
        gc.setStroke(Color.GOLD);
        gc.strokeLine(tableX - 5, totalsY - 10, tableX + totalWidth + 5, totalsY - 10);

        xPos = tableX;
        gc.setFill(Color.GOLD);
        gc.fillText("TOTAL", xPos + 5, totalsY + 10);
        xPos += colWidths[0];

        for (int t = 0; t < 6; t++) {
            gc.setFill(Color.GOLD);
            String numStr = String.valueOf(totalByType[t]);
            gc.fillText(numStr, xPos + (colWidths[t + 1] - numStr.length() * 8) / 2, totalsY + 10);
            xPos += colWidths[t + 1];
        }

        gc.setFill(Color.GOLD);
        gc.fillText(String.valueOf(totalKills), xPos + (colWidths[7] - String.valueOf(totalKills).length() * 8) / 2, totalsY + 10);
        xPos += colWidths[7];

        gc.fillText(String.valueOf(totalLevelPoints), xPos + (colWidths[8] - String.valueOf(totalLevelPoints).length() * 8) / 2, totalsY + 10);
        xPos += colWidths[8];

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText(String.valueOf(totalPoints), xPos + (colWidths[9] - String.valueOf(totalPoints).length() * 10) / 2, totalsY + 10);
    }

    /**
     * Interface for providing player display names.
     */
    @FunctionalInterface
    public interface PlayerDisplayNameProvider {
        String getDisplayName(int playerIndex);
    }

    /**
     * Interface for awarding winner bonus.
     */
    @FunctionalInterface
    public interface WinnerBonusCallback {
        void awardWinnerBonus(int winnerIndex);
    }
}
