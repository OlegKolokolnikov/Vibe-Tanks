package com.vibetanks.core;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.audio.SoundManager;

import java.util.List;

/**
 * Manages level transitions (next level, restart).
 * Extracted from Game.java to reduce complexity.
 */
public class LevelTransitionManager {

    /**
     * Context interface for accessing and modifying game state during level transitions.
     */
    public interface LevelTransitionContext {
        // Game objects
        GameMap getGameMap();
        Base getBase();
        void setBase(Base base);
        EnemySpawner getEnemySpawner();
        void setEnemySpawner(EnemySpawner spawner);
        List<Tank> getPlayerTanks();
        List<Tank> getEnemyTanks();
        List<Bullet> getBullets();
        List<Laser> getLasers();
        List<PowerUp> getPowerUps();
        CelebrationManager getCelebrationManager();
        UFOManager getUFOManager();
        PowerUpEffectManager getPowerUpEffectManager();
        SoundManager getSoundManager();

        // State flags
        void setVictory(boolean value);
        void setVictoryConditionMet(boolean value);
        void setVictoryDelayTimer(int value);
        void setGameOver(boolean value);
        void setGameOverSoundPlayed(boolean value);
        void setWinnerBonusAwarded(boolean value);
        void setBossKillerPlayerIndex(int index);
        void setBossKillPowerUpReward(PowerUp.Type type);

        // Player stats
        int[] getPlayerKills();
        int[] getPlayerScores();
        int[] getPlayerLevelScores();
        int[][] getPlayerKillsByType();

        // UI
        void hideVictoryImage();
        void hideGameOverImage();

        // Configuration
        int getTotalEnemies();
        double[][] getFixedStartPositions();
    }

    /**
     * Start the next level after victory.
     * Keeps player power-ups and total scores, resets kills and level scores.
     */
    public static void startNextLevel(LevelTransitionContext ctx) {
        GameMap gameMap = ctx.getGameMap();

        // Generate new random level
        gameMap.nextLevel();

        // Reset game state flags
        resetGameStateFlags(ctx);

        // Reset kills and level scores for new round (total scores persist)
        resetKillsAndLevelScores(ctx);

        // Reset base
        ctx.setBase(new Base(GameConstants.BASE_X, GameConstants.BASE_Y));

        // Clear projectiles and power-ups
        clearProjectilesAndPowerUps(ctx);

        // Reset player tanks (keep power-ups but reset position and give shield)
        double[][] fixedPositions = ctx.getFixedStartPositions();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            player.setPosition(fixedPositions[i][0], fixedPositions[i][1]);
            player.setDirection(Direction.UP);
            player.giveTemporaryShield();
        }

        // Clear enemy tanks and reset spawner
        ctx.getEnemyTanks().clear();
        ctx.setEnemySpawner(new EnemySpawner(ctx.getTotalEnemies(), 5, gameMap));

        // Reset power-up effects
        ctx.getPowerUpEffectManager().reset();

        // Hide victory image
        ctx.hideVictoryImage();

        // Reset UFO state for new level
        ctx.getUFOManager().reset();

        // Play intro sound for new level
        ctx.getSoundManager().playIntro();

        System.out.println("Starting Level " + gameMap.getLevelNumber());
    }

    /**
     * Restart the current level after game over.
     * Resets everything including player power-ups, lives, and all scores.
     */
    public static void restartCurrentLevel(LevelTransitionContext ctx) {
        GameMap gameMap = ctx.getGameMap();

        // Regenerate the same level or reload custom level
        gameMap.regenerateOrReloadLevel();

        // Reset game state flags
        resetGameStateFlags(ctx);

        // Reset ALL scores (kills, level scores, and total scores)
        resetAllScores(ctx);

        // Reset base
        ctx.setBase(new Base(GameConstants.BASE_X, GameConstants.BASE_Y));

        // Clear projectiles and power-ups
        clearProjectilesAndPowerUps(ctx);

        // Reset player tanks (full reset including lives and power-ups)
        double[][] fixedPositions = ctx.getFixedStartPositions();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            player.setLives(3);
            player.setLaserDuration(0); // Clear laser power-up on restart
            player.spawnImmediate(fixedPositions[i][0], fixedPositions[i][1]);
        }

        // Clear enemy tanks and reset spawner
        ctx.getEnemyTanks().clear();
        ctx.setEnemySpawner(new EnemySpawner(ctx.getTotalEnemies(), 5, gameMap));

        // Reset power-up effects
        ctx.getPowerUpEffectManager().reset();

        // Hide game over image
        ctx.hideGameOverImage();

        // Reset UFO state for restart
        ctx.getUFOManager().reset();

        // Play intro sound for retry
        ctx.getSoundManager().playIntro();

        System.out.println("Restarting Level " + gameMap.getLevelNumber());
    }

    /**
     * Reset common game state flags used by both next level and restart.
     */
    private static void resetGameStateFlags(LevelTransitionContext ctx) {
        ctx.setVictory(false);
        ctx.setVictoryConditionMet(false);
        ctx.setVictoryDelayTimer(0);
        ctx.setGameOver(false);
        ctx.setGameOverSoundPlayed(false);
        ctx.getCelebrationManager().reset();
        ctx.setWinnerBonusAwarded(false);
        ctx.setBossKillerPlayerIndex(-1);
        ctx.setBossKillPowerUpReward(null);
    }

    /**
     * Reset kills and level scores (keep total scores for next level).
     */
    private static void resetKillsAndLevelScores(LevelTransitionContext ctx) {
        int[] playerKills = ctx.getPlayerKills();
        int[] playerLevelScores = ctx.getPlayerLevelScores();
        int[][] playerKillsByType = ctx.getPlayerKillsByType();

        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerLevelScores[i] = 0;
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }
    }

    /**
     * Reset ALL scores (kills, level scores, and total scores for restart).
     */
    private static void resetAllScores(LevelTransitionContext ctx) {
        int[] playerKills = ctx.getPlayerKills();
        int[] playerScores = ctx.getPlayerScores();
        int[] playerLevelScores = ctx.getPlayerLevelScores();
        int[][] playerKillsByType = ctx.getPlayerKillsByType();

        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerScores[i] = 0;
            playerLevelScores[i] = 0;
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }
    }

    /**
     * Clear all projectiles and power-ups.
     */
    private static void clearProjectilesAndPowerUps(LevelTransitionContext ctx) {
        ctx.getBullets().clear();
        ctx.getLasers().clear();
        ctx.getPowerUps().clear();
    }
}
