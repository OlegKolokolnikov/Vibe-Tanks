package com.vibetanks.core;

import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;

import java.util.List;

/**
 * Handles level transitions including starting new levels and restarting.
 * Extracts level transition logic from Game.java to improve separation of concerns.
 */
public class LevelTransitionHandler {

    /**
     * Reset player kill and score tracking.
     *
     * @param playerKills Array of player kills to reset
     * @param playerScores Array of player scores (reset only if resetScores is true)
     * @param playerLevelScores Array of level scores to reset
     * @param playerKillsByType Matrix of kills by enemy type to reset
     * @param resetScores Whether to reset total scores
     */
    public static void resetPlayerStats(
            int[] playerKills,
            int[] playerScores,
            int[] playerLevelScores,
            int[][] playerKillsByType,
            boolean resetScores
    ) {
        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerLevelScores[i] = 0;
            if (resetScores) {
                playerScores[i] = 0;
            }
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }
    }

    /**
     * Reset player tanks for new level (keep power-ups, reset position).
     *
     * @param playerTanks List of player tanks
     * @param startPositions Fixed start positions
     */
    public static void resetPlayerTanksForNextLevel(List<Tank> playerTanks, double[][] startPositions) {
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            player.setPosition(startPositions[i][0], startPositions[i][1]);
            player.setDirection(Direction.UP);
            player.giveTemporaryShield();
        }
    }

    /**
     * Reset player tanks for restart (full reset including lives and power-ups).
     *
     * @param playerTanks List of player tanks
     * @param startPositions Fixed start positions
     */
    public static void resetPlayerTanksForRestart(List<Tank> playerTanks, double[][] startPositions) {
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            player.setLives(3);
            player.setLaserDuration(0); // Clear laser power-up
            player.spawnImmediate(startPositions[i][0], startPositions[i][1]);
        }
    }

    /**
     * Clear all projectiles and collectibles.
     *
     * @param bullets List of bullets to clear
     * @param lasers List of lasers to clear
     * @param powerUps List of power-ups to clear
     */
    public static void clearProjectilesAndCollectibles(
            List<Bullet> bullets,
            List<Laser> lasers,
            List<PowerUp> powerUps
    ) {
        bullets.clear();
        lasers.clear();
        powerUps.clear();
    }

    /**
     * Reset UFO machinegun kill tracking.
     *
     * @param playerMachinegunKills Array to reset
     */
    public static void resetMachinegunKills(int[] playerMachinegunKills) {
        for (int i = 0; i < playerMachinegunKills.length; i++) {
            playerMachinegunKills[i] = 0;
        }
    }

    /**
     * Clear dancing animation lists.
     *
     * @param dancingCharacters List to clear
     * @param victoryDancingGirls List to clear
     */
    public static void clearDancingAnimations(
            List<DancingCharacter> dancingCharacters,
            List<DancingGirl> victoryDancingGirls
    ) {
        dancingCharacters.clear();
        victoryDancingGirls.clear();
    }

    /**
     * Create a new enemy spawner for the level.
     *
     * @param totalEnemies Total enemies to spawn
     * @param maxOnScreen Maximum enemies on screen at once
     * @param gameMap The game map
     * @return New enemy spawner
     */
    public static EnemySpawner createEnemySpawner(int totalEnemies, int maxOnScreen, GameMap gameMap) {
        return new EnemySpawner(totalEnemies, maxOnScreen, gameMap);
    }

    /**
     * Create a new base at the standard position.
     *
     * @return New base instance
     */
    public static Base createBase() {
        return new Base(12 * 32, 24 * 32);
    }
}
