package com.vibetanks.rendering;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Renders HUD elements, game state screens (game over, victory, pause).
 * Extracted from Game.java to reduce complexity.
 */
public class HUDRenderer {
    private final GraphicsContext gc;
    private final IconRenderer iconRenderer;
    private final EffectRenderer effectRenderer;
    private final StatsRenderer statsRenderer;
    private final int width;
    private final int height;

    public HUDRenderer(GraphicsContext gc, IconRenderer iconRenderer, EffectRenderer effectRenderer,
                       StatsRenderer statsRenderer, int width, int height) {
        this.gc = gc;
        this.iconRenderer = iconRenderer;
        this.effectRenderer = effectRenderer;
        this.statsRenderer = statsRenderer;
        this.width = width;
        this.height = height;
    }

    /**
     * Render the main HUD (player stats, power-up icons).
     * Level, enemies, and lives are now shown in the sidebar.
     */
    public void renderHUD(int levelNumber, int remainingEnemies, int displayPlayerCount,
                          List<Tank> playerTanks, int[] playerKills, int[] playerScores,
                          PlayerNameProvider nameProvider) {
        // Display player info and power-ups (kills and score only - lives moved to sidebar)
        for (int i = 0; i < displayPlayerCount && i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            String playerName = nameProvider.getPlayerDisplayName(i);
            double yOffset = 20 + i * 40;

            // Display kills and score (lives now in sidebar)
            gc.setFill(Color.WHITE);
            gc.fillText(playerName + "  Kills: " + playerKills[i] + "  Score: " + playerScores[i], 10, yOffset);

            // Display power-ups
            renderPlayerPowerUps(player, 10, yOffset + 10);
        }
    }

    /**
     * Render power-up icons for a player.
     */
    private void renderPlayerPowerUps(Tank player, double startX, double yOffset) {
        double xOffset = startX;

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
        }
    }

    /**
     * Render game over screen with dancing characters and stats.
     */
    public void renderGameOverScreen(Base base, CelebrationManager celebrationManager,
                                      List<Tank> enemyTanks, SoundManager soundManager,
                                      GameOverState state, EndGameStatsProvider statsProvider) {
        // Initialize dancing characters when base was destroyed
        if (!base.isAlive() && !celebrationManager.isDancingInitialized()) {
            celebrationManager.initializeDancingCharacters(base, enemyTanks);
        }

        // Update and render dancing characters
        celebrationManager.updateDancingCharacters();
        for (DancingCharacter dancer : celebrationManager.getDancingCharacters()) {
            dancer.render(gc);
        }

        // Render laughing skull
        effectRenderer.renderLaughingSkull(width / 2, height / 2 - 150);

        // Play sad sound once and stop gameplay sounds
        if (!state.isGameOverSoundPlayed()) {
            soundManager.stopGameplaySounds();
            soundManager.playSad();
            state.setGameOverSoundPlayed(true);
        }

        gc.setFill(Color.RED);
        gc.setFont(Font.font(40));
        gc.fillText("GAME OVER", width / 2 - 120, height / 2 + 50);

        // Show statistics
        statsProvider.renderEndGameStats(height / 2 + 90);

        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font(22));
        gc.fillText("Press ENTER to restart", width / 2 - 110, height / 2 + 310);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Press ESC to return to menu", width / 2 - 115, height / 2 + 340);
    }

    /**
     * Render victory screen with dancing girls and stats.
     */
    public void renderVictoryScreen(int levelNumber, Base base, CelebrationManager celebrationManager,
                                     List<Tank> playerTanks, SoundManager soundManager,
                                     ImageView victoryImageView, EndGameStatsProvider statsProvider) {
        // Initialize victory celebration
        if (!celebrationManager.isVictoryDancingInitialized()) {
            soundManager.stopGameplaySounds();
            celebrationManager.initializeVictoryCelebration(base, playerTanks.size());
        }

        // Update and render dancing girls
        celebrationManager.updateVictoryGirls();
        for (DancingGirl girl : celebrationManager.getVictoryDancingGirls()) {
            girl.render(gc);
        }

        // Show dancing anime girl if available
        if (victoryImageView != null) {
            victoryImageView.setVisible(true);
        }

        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font(40));
        gc.fillText("LEVEL " + levelNumber + " COMPLETE!", width / 2 - 180, height / 2 + 50);

        // Show statistics
        statsProvider.renderEndGameStats(height / 2 + 90);

        gc.setFill(Color.LIME);
        gc.setFont(Font.font(22));
        gc.fillText("Press ENTER for next level", width / 2 - 130, height / 2 + 310);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Press ESC to return to menu", width / 2 - 115, height / 2 + 340);
    }

    /**
     * Render pause menu overlay.
     */
    public void renderPauseMenu(int pauseMenuSelection) {
        // Draw semi-transparent overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 50));
        gc.fillText("PAUSED", width / 2 - 100, height / 2 - 80);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        // Resume option
        if (pauseMenuSelection == 0) {
            gc.setFill(Color.YELLOW);
            gc.fillText("> RESUME <", width / 2 - 80, height / 2);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillText("  RESUME  ", width / 2 - 80, height / 2);
        }

        // Exit option
        if (pauseMenuSelection == 1) {
            gc.setFill(Color.YELLOW);
            gc.fillText("> EXIT <", width / 2 - 60, height / 2 + 50);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillText("  EXIT  ", width / 2 - 60, height / 2 + 50);
        }

        gc.setFill(Color.GRAY);
        gc.setFont(Font.font(16));
        gc.fillText("Use UP/DOWN to select, ENTER to confirm", width / 2 - 150, height / 2 + 120);
    }

    /**
     * Render hint to take life from teammate when player is dead.
     */
    public void renderTakeLifeHint(List<Tank> playerTanks, int myPlayerIndex) {
        if (myPlayerIndex < 0 || myPlayerIndex >= playerTanks.size()) return;

        Tank myTank = playerTanks.get(myPlayerIndex);
        if (!myTank.isAlive() && myTank.getLives() <= 0) {
            // Check if any teammate has spare lives
            boolean canTakeLife = false;
            for (int i = 0; i < playerTanks.size(); i++) {
                if (i != myPlayerIndex && playerTanks.get(i).getLives() > 1) {
                    canTakeLife = true;
                    break;
                }
            }
            if (canTakeLife) {
                gc.setFill(Color.YELLOW);
                gc.setFont(Font.font(20));
                gc.fillText("Press ENTER to take life from teammate", width / 2 - 180, height / 2);
            }
        }
    }

    /**
     * Render pause indicator for multiplayer games.
     */
    public void renderMultiplayerPauseIndicator(boolean[] playerPaused, int pausePlayerIndex) {
        if (pausePlayerIndex >= 0 && pausePlayerIndex < playerPaused.length && playerPaused[pausePlayerIndex]) {
            gc.setFill(Color.rgb(0, 0, 0, 0.5));
            gc.fillRect(0, 0, width, 60);
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));
            gc.fillText("PAUSED - Press ESC to resume", width / 2 - 200, 40);
        }
    }

    /**
     * Hide end-game images during normal gameplay.
     */
    public void hideEndGameImages(ImageView victoryImageView, ImageView gameOverImageView) {
        if (victoryImageView != null) {
            victoryImageView.setVisible(false);
        }
        if (gameOverImageView != null) {
            gameOverImageView.setVisible(false);
        }
    }

    /**
     * Interface for providing player display names.
     */
    public interface PlayerNameProvider {
        String getPlayerDisplayName(int playerIndex);
    }

    /**
     * Interface for providing end game stats rendering.
     */
    public interface EndGameStatsProvider {
        void renderEndGameStats(double startY);
    }

    /**
     * State holder for game over sound tracking.
     */
    public interface GameOverState {
        boolean isGameOverSoundPlayed();
        void setGameOverSoundPlayed(boolean value);
    }
}
