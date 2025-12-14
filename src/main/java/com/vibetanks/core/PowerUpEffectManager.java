package com.vibetanks.core;

import com.vibetanks.util.GameLogger;

/**
 * Manages time-based power-up effects (base protection, freeze, speed boost).
 * Extracted from Game.java to reduce complexity.
 */
public class PowerUpEffectManager {
    private static final GameLogger LOG = GameLogger.getLogger(PowerUpEffectManager.class);
    // Base protection (SHOVEL power-up)
    private int baseProtectionDuration = 0;
    private boolean isFlashing = false;
    private int flashCount = 0;
    private int flashTimer = 0;
    private static final int BASE_PROTECTION_TIME = GameConstants.BASE_PROTECTION_TIME;
    private static final int FLASH_DURATION = GameConstants.FLASH_DURATION;
    private static final int TOTAL_FLASHES = 10; // 5 complete flashes (10 state changes)

    // Freeze effects
    private int enemyFreezeDuration = 0;
    private int playerFreezeDuration = 0;
    private static final int FREEZE_TIME = GameConstants.FREEZE_TIME;

    // Enemy speed boost (CAR power-up)
    private int enemyTeamSpeedBoostDuration = 0;
    private Tank enemyWithPermanentSpeedBoost = null;
    private static final int ENEMY_SPEED_BOOST_TIME = GameConstants.ENEMY_SPEED_BOOST_TIME;

    /**
     * Reset all power-up effect state for new level/game restart.
     */
    public void reset() {
        baseProtectionDuration = 0;
        isFlashing = false;
        flashCount = 0;
        flashTimer = 0;
        enemyFreezeDuration = 0;
        playerFreezeDuration = 0;
        enemyTeamSpeedBoostDuration = 0;
        enemyWithPermanentSpeedBoost = null;
    }

    // ========== Base Protection (SHOVEL) ==========

    /**
     * Activate base protection (called when player collects SHOVEL).
     * @param gameMap The game map to apply steel protection to
     */
    public void activateBaseProtection(GameMap gameMap) {
        activateBaseProtection(gameMap, false);
    }

    /**
     * Activate base protection (called when player collects SHOVEL).
     * @param gameMap The game map to apply steel protection to
     * @param useGround If true, use GROUND (indestructible) instead of STEEL (1000 point upgrade)
     */
    public void activateBaseProtection(GameMap gameMap, boolean useGround) {
        GameMap.TileType protectionType = useGround ? GameMap.TileType.GROUND : GameMap.TileType.STEEL;
        gameMap.setBaseProtection(protectionType);
        baseProtectionDuration = BASE_PROTECTION_TIME;
        isFlashing = false;
        flashCount = 0;
        flashTimer = 0;
        if (useGround) {
            LOG.info("SHOVEL: Base protected with GROUND (1000 point upgrade)!");
        }
    }

    /**
     * Remove base protection (called when enemy collects SHOVEL).
     * @param gameMap The game map to remove protection from
     */
    public void removeBaseProtection(GameMap gameMap) {
        gameMap.setBaseProtection(GameMap.TileType.EMPTY);
        baseProtectionDuration = 0;
        isFlashing = false;
        flashCount = 0;
        flashTimer = 0;
    }

    /**
     * Update base protection timer and flashing state.
     * @param gameMap The game map to update protection on
     */
    public void updateBaseProtection(GameMap gameMap) {
        // Update base protection countdown
        if (baseProtectionDuration > 0) {
            baseProtectionDuration--;
            if (baseProtectionDuration == 0) {
                // Start flashing when timer expires
                isFlashing = true;
                flashCount = 0;
                flashTimer = 0;
                gameMap.setBaseProtection(GameMap.TileType.STEEL); // Start with steel
            }
        }

        // Handle flashing after protection expires
        if (isFlashing) {
            flashTimer++;
            if (flashTimer >= FLASH_DURATION) {
                flashTimer = 0;
                flashCount++;

                // Toggle between STEEL and BRICK (start with STEEL, then BRICK, etc.)
                if (flashCount % 2 == 0) {
                    gameMap.setBaseProtection(GameMap.TileType.STEEL);
                } else {
                    gameMap.setBaseProtection(GameMap.TileType.BRICK);
                }

                // Stop flashing after 5 complete flashes (10 state changes)
                if (flashCount >= TOTAL_FLASHES) {
                    isFlashing = false;
                    gameMap.setBaseProtection(GameMap.TileType.BRICK); // Final state is brick
                }
            }
        }
    }

    // ========== Freeze Effects ==========

    /**
     * Activate enemy freeze (called when player collects FREEZE).
     * Duration varies by difficulty mode:
     * - Hard: 10 seconds
     * - Normal: 15 seconds
     * - Easy: 15 seconds
     * - Very Easy: 20 seconds
     */
    public void activateEnemyFreeze() {
        int seconds;
        if (GameSettings.isHardModeActive()) {
            seconds = 10;
        } else if (GameSettings.isVeryEasyModeActiveForCurrentLevel()) {
            seconds = 20;
        } else if (GameSettings.isEasyModeActive(GameSettings.getCurrentLevel())) {
            seconds = 15;
        } else {
            seconds = 15; // Normal mode
        }
        enemyFreezeDuration = seconds * 60; // 60 frames per second
        LOG.info("FREEZE: Enemies frozen for {} seconds!", seconds);
    }

    /**
     * Activate player freeze (called when enemy collects FREEZE).
     * Duration varies by difficulty mode:
     * - Hard: 10 seconds
     * - Normal: 15 seconds
     * - Easy: 10 seconds
     * - Very Easy: 10 seconds
     */
    public void activatePlayerFreeze() {
        int seconds;
        if (GameSettings.isHardModeActive()) {
            seconds = 10;
        } else if (GameSettings.isVeryEasyModeActiveForCurrentLevel()) {
            seconds = 10;
        } else if (GameSettings.isEasyModeActive(GameSettings.getCurrentLevel())) {
            seconds = 10;
        } else {
            seconds = 15; // Normal mode
        }
        playerFreezeDuration = seconds * 60; // 60 frames per second
        LOG.info("FREEZE: Players frozen for {} seconds! (can still shoot)", seconds);
    }

    /**
     * Update freeze timers (call each frame).
     */
    public void updateFreezeTimers() {
        if (enemyFreezeDuration > 0) {
            enemyFreezeDuration--;
        }
        if (playerFreezeDuration > 0) {
            playerFreezeDuration--;
        }
    }

    /**
     * Check if enemies are currently frozen.
     */
    public boolean areEnemiesFrozen() {
        return enemyFreezeDuration > 0;
    }

    /**
     * Check if players are currently frozen.
     */
    public boolean arePlayersFrozen() {
        return playerFreezeDuration > 0;
    }

    // ========== Enemy Speed Boost (CAR) ==========

    /**
     * Activate enemy team speed boost (called when enemy collects CAR).
     * @param collector The enemy that collected the CAR power-up
     */
    public void activateEnemySpeedBoost(Tank collector) {
        enemyWithPermanentSpeedBoost = collector;
        enemyTeamSpeedBoostDuration = ENEMY_SPEED_BOOST_TIME;
    }

    /**
     * Update enemy speed boost timer (call each frame).
     */
    public void updateEnemySpeedBoost() {
        if (enemyTeamSpeedBoostDuration > 0) {
            enemyTeamSpeedBoostDuration--;
        }
    }

    /**
     * Check if enemy team speed boost is active.
     */
    public boolean isEnemySpeedBoostActive() {
        return enemyTeamSpeedBoostDuration > 0;
    }

    /**
     * Get the enemy with permanent speed boost (from collecting CAR).
     */
    public Tank getEnemyWithPermanentSpeedBoost() {
        return enemyWithPermanentSpeedBoost;
    }

    /**
     * Clear permanent speed boost enemy (when that enemy dies).
     */
    public void clearPermanentSpeedBoostEnemy() {
        enemyWithPermanentSpeedBoost = null;
    }

    // ========== Getters for network sync ==========

    public int getBaseProtectionDuration() { return baseProtectionDuration; }
    public boolean isFlashing() { return isFlashing; }
    public int getFlashCount() { return flashCount; }
    public int getFlashTimer() { return flashTimer; }
    public int getEnemyFreezeDuration() { return enemyFreezeDuration; }
    public int getPlayerFreezeDuration() { return playerFreezeDuration; }
    public int getEnemyTeamSpeedBoostDuration() { return enemyTeamSpeedBoostDuration; }

    // ========== Setters for network sync ==========

    public void setBaseProtectionDuration(int value) { this.baseProtectionDuration = value; }
    public void setFlashing(boolean value) { this.isFlashing = value; }
    public void setFlashCount(int value) { this.flashCount = value; }
    public void setFlashTimer(int value) { this.flashTimer = value; }
    public void setEnemyFreezeDuration(int value) { this.enemyFreezeDuration = value; }
    public void setPlayerFreezeDuration(int value) { this.playerFreezeDuration = value; }
    public void setEnemyTeamSpeedBoostDuration(int value) { this.enemyTeamSpeedBoostDuration = value; }
    public void setEnemyWithPermanentSpeedBoost(Tank tank) { this.enemyWithPermanentSpeedBoost = tank; }
}
