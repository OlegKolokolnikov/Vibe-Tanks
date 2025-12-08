package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;

import java.util.List;

/**
 * Handles power-up collection and effects.
 * Extracts power-up logic from Game.java to improve separation of concerns.
 */
public class PowerUpHandler {

    /**
     * Result of power-up collection by a player.
     */
    public static class PlayerCollectionResult {
        public boolean collected = false;
        public PowerUp.Type type = null;

        // Special effects that need game-level handling
        public boolean activateShovel = false;        // Enable base protection
        public boolean activateFreeze = false;        // Freeze enemies
        public boolean activateBomb = false;          // Destroy all enemies
    }

    /**
     * Result of power-up collection by an enemy.
     */
    public static class EnemyCollectionResult {
        public boolean collected = false;
        public PowerUp.Type type = null;
        public Tank collectorEnemy = null;

        // Special effects that need game-level handling
        public boolean removeShovel = false;          // Remove base protection
        public boolean activateFreeze = false;        // Freeze players
        public boolean activateBomb = false;          // Damage all players
        public boolean activateCar = false;           // Team speed boost
    }

    /**
     * Check if a power-up is collected by any player and apply effects.
     *
     * @param powerUp The power-up to check
     * @param playerTanks List of player tanks
     * @return Result of collection attempt
     */
    public static PlayerCollectionResult checkPlayerCollection(PowerUp powerUp, List<Tank> playerTanks) {
        PlayerCollectionResult result = new PlayerCollectionResult();

        for (Tank player : playerTanks) {
            if (player.isAlive() && powerUp.collidesWith(player)) {
                result.collected = true;
                result.type = powerUp.getType();

                // Handle special power-ups that need game-level effects
                switch (powerUp.getType()) {
                    case SHOVEL -> result.activateShovel = true;
                    case FREEZE -> result.activateFreeze = true;
                    case BOMB -> result.activateBomb = true;
                    default -> powerUp.applyEffect(player);  // Standard power-ups apply directly
                }

                return result;
            }
        }

        return result;
    }

    /**
     * Check if a power-up is collected by any enemy and apply effects.
     *
     * @param powerUp The power-up to check
     * @param enemyTanks List of enemy tanks
     * @return Result of collection attempt
     */
    public static EnemyCollectionResult checkEnemyCollection(PowerUp powerUp, List<Tank> enemyTanks) {
        EnemyCollectionResult result = new EnemyCollectionResult();

        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && powerUp.collidesWith(enemy)) {
                result.collected = true;
                result.type = powerUp.getType();
                result.collectorEnemy = enemy;

                // Handle special power-ups
                switch (powerUp.getType()) {
                    case SHOVEL -> result.removeShovel = true;
                    case FREEZE -> result.activateFreeze = true;
                    case BOMB -> result.activateBomb = true;
                    case CAR -> {
                        result.activateCar = true;
                        powerUp.applyEffect(enemy);  // Give permanent boost to collector
                    }
                    default -> powerUp.applyEffect(enemy);
                }

                return result;
            }
        }

        return result;
    }

    /**
     * Apply BOMB effect when collected by player - destroy all enemies.
     *
     * @param enemyTanks List of enemy tanks to destroy
     * @param soundManager Sound manager for explosion sounds
     * @return Number of enemies destroyed
     */
    public static int applyPlayerBomb(List<Tank> enemyTanks, SoundManager soundManager) {
        int destroyed = 0;
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive()) {
                while (enemy.isAlive()) {
                    enemy.damage();
                }
                soundManager.playExplosion();
                destroyed++;
            }
        }
        System.out.println("BOMB: All enemies destroyed! (" + destroyed + " kills)");
        return destroyed;
    }

    /**
     * Apply BOMB effect when collected by enemy - damage all players (bypasses shields).
     *
     * @param playerTanks List of player tanks to damage
     * @param soundManager Sound manager for sounds
     * @return Number of players killed
     */
    public static int applyEnemyBomb(List<Tank> playerTanks, SoundManager soundManager) {
        int killed = 0;
        System.out.println("BOMB collected by enemy - damaging all players!");
        for (Tank player : playerTanks) {
            if (player.isAlive()) {
                // BOMB bypasses all shields
                player.setShield(false);
                player.setPauseShield(false);
                player.damage();
                if (!player.isAlive()) {
                    soundManager.playPlayerDeath();
                    killed++;
                } else {
                    soundManager.playExplosion();
                }
            }
        }
        return killed;
    }

    /**
     * Apply CAR effect when collected by enemy - give speed boost to all enemies.
     *
     * @param enemyTanks List of enemy tanks
     * @param collectorEnemy The enemy who picked up CAR (gets permanent boost)
     * @param speedBoostMultiplier Speed boost multiplier
     */
    public static void applyEnemyCarSpeedBoost(List<Tank> enemyTanks, Tank collectorEnemy, double speedBoostMultiplier) {
        for (Tank enemy : enemyTanks) {
            if (enemy != collectorEnemy && enemy.isAlive()) {
                enemy.applyTempSpeedBoost(speedBoostMultiplier);
            }
        }
        System.out.println("CAR: All enemies get speed boost for 30 seconds!");
    }

    /**
     * Remove temporary speed boost from all enemies except the one who collected CAR.
     *
     * @param enemyTanks List of enemy tanks
     * @param collectorEnemy The enemy with permanent boost (null to remove from all)
     */
    public static void removeEnemyTeamSpeedBoost(List<Tank> enemyTanks, Tank collectorEnemy) {
        for (Tank enemy : enemyTanks) {
            if (enemy != collectorEnemy) {
                enemy.removeTempSpeedBoost();
            }
        }
        System.out.println("Enemy team speed boost expired - only original enemy keeps the speed");
    }
}
