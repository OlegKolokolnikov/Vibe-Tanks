package com.vibetanks.core;

import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import com.vibetanks.audio.SoundManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages game state transitions including:
 * - Victory/Game Over detection and delays
 * - Level transitions (next level, restart)
 * - Dancing animations for victory/game over
 * - Score tracking between levels
 */
public class GameStateManager {

    // Victory delay (frames before showing victory screen)
    private boolean victoryConditionMet = false;
    private int victoryDelayTimer = 0;
    private static final int VICTORY_DELAY = GameConstants.VICTORY_DELAY;

    // Game state flags
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean gameOverSoundPlayed = false;
    private boolean winnerBonusAwarded = false;

    // Current level
    private int currentLevel = 1;

    // Boss kill tracking for victory screen
    private int bossKillerPlayerIndex = -1;
    private PowerUp.Type bossKillPowerUpReward = null;

    // Dancing animations
    private List<DancingCharacter> dancingCharacters = new ArrayList<>();
    private boolean dancingInitialized = false;
    private List<DancingGirl> victoryDancingGirls = new ArrayList<>();
    private boolean victoryDancingInitialized = false;

    public GameStateManager() {
    }

    /**
     * Update victory condition with delay.
     *
     * @param allEnemiesDefeated True if all enemies spawned and defeated
     * @return True if victory should be triggered
     */
    public boolean updateVictoryCondition(boolean allEnemiesDefeated) {
        if (allEnemiesDefeated) {
            if (!victoryConditionMet) {
                victoryConditionMet = true;
                victoryDelayTimer = 0;
                System.out.println("All enemies defeated! Victory in 10 seconds...");
            }
            victoryDelayTimer++;
            if (victoryDelayTimer >= VICTORY_DELAY) {
                victory = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Check and set game over condition.
     *
     * @param allPlayersDead True if all players are dead with no lives
     * @param baseDestroyed True if base is destroyed
     * @return True if game over should be triggered
     */
    public boolean checkGameOver(boolean allPlayersDead, boolean baseDestroyed) {
        if (allPlayersDead || baseDestroyed) {
            gameOver = true;
            return true;
        }
        return false;
    }

    /**
     * Reset state for a new level.
     */
    public void resetForNewLevel() {
        victoryConditionMet = false;
        victoryDelayTimer = 0;
        gameOver = false;
        victory = false;
        gameOverSoundPlayed = false;
        winnerBonusAwarded = false;
        bossKillerPlayerIndex = -1;
        bossKillPowerUpReward = null;
        dancingInitialized = false;
        dancingCharacters.clear();
        victoryDancingInitialized = false;
        victoryDancingGirls.clear();
    }

    /**
     * Reset state for restarting the current level.
     */
    public void resetForRestart() {
        resetForNewLevel();
    }

    /**
     * Advance to the next level.
     */
    public void nextLevel() {
        currentLevel++;
        resetForNewLevel();
    }

    /**
     * Reset to level 1.
     */
    public void resetToLevel1() {
        currentLevel = 1;
        resetForNewLevel();
    }

    // Getters and setters

    public boolean isVictoryConditionMet() {
        return victoryConditionMet;
    }

    public void setVictoryConditionMet(boolean victoryConditionMet) {
        this.victoryConditionMet = victoryConditionMet;
    }

    public int getVictoryDelayTimer() {
        return victoryDelayTimer;
    }

    public void setVictoryDelayTimer(int victoryDelayTimer) {
        this.victoryDelayTimer = victoryDelayTimer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isVictory() {
        return victory;
    }

    public void setVictory(boolean victory) {
        this.victory = victory;
    }

    public boolean isGameOverSoundPlayed() {
        return gameOverSoundPlayed;
    }

    public void setGameOverSoundPlayed(boolean gameOverSoundPlayed) {
        this.gameOverSoundPlayed = gameOverSoundPlayed;
    }

    public boolean isWinnerBonusAwarded() {
        return winnerBonusAwarded;
    }

    public void setWinnerBonusAwarded(boolean winnerBonusAwarded) {
        this.winnerBonusAwarded = winnerBonusAwarded;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getBossKillerPlayerIndex() {
        return bossKillerPlayerIndex;
    }

    public void setBossKillerPlayerIndex(int bossKillerPlayerIndex) {
        this.bossKillerPlayerIndex = bossKillerPlayerIndex;
    }

    public PowerUp.Type getBossKillPowerUpReward() {
        return bossKillPowerUpReward;
    }

    public void setBossKillPowerUpReward(PowerUp.Type bossKillPowerUpReward) {
        this.bossKillPowerUpReward = bossKillPowerUpReward;
    }

    public List<DancingCharacter> getDancingCharacters() {
        return dancingCharacters;
    }

    public void setDancingCharacters(List<DancingCharacter> dancingCharacters) {
        this.dancingCharacters = dancingCharacters;
    }

    public boolean isDancingInitialized() {
        return dancingInitialized;
    }

    public void setDancingInitialized(boolean dancingInitialized) {
        this.dancingInitialized = dancingInitialized;
    }

    public List<DancingGirl> getVictoryDancingGirls() {
        return victoryDancingGirls;
    }

    public void setVictoryDancingGirls(List<DancingGirl> victoryDancingGirls) {
        this.victoryDancingGirls = victoryDancingGirls;
    }

    public boolean isVictoryDancingInitialized() {
        return victoryDancingInitialized;
    }

    public void setVictoryDancingInitialized(boolean victoryDancingInitialized) {
        this.victoryDancingInitialized = victoryDancingInitialized;
    }
}
