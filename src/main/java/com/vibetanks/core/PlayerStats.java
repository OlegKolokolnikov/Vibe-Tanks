package com.vibetanks.core;

import java.util.Arrays;

/**
 * Consolidated player statistics tracking.
 * Single source of truth for kills, scores, and kill breakdown by enemy type.
 * Eliminates state duplication across Game, ServerGameState, and GameContext.
 */
public class PlayerStats {
    private static final int MAX_PLAYERS = 4;
    private static final int ENEMY_TYPE_COUNT = 6; // REGULAR, ARMORED, FAST, POWER, HEAVY, BOSS

    private final int[] kills;
    private final int[] scores;
    private final int[] levelScores; // Score for current level only
    private final int[][] killsByType;

    public PlayerStats() {
        this.kills = new int[MAX_PLAYERS];
        this.scores = new int[MAX_PLAYERS];
        this.levelScores = new int[MAX_PLAYERS];
        this.killsByType = new int[MAX_PLAYERS][ENEMY_TYPE_COUNT];
    }

    /**
     * Record a kill for a player.
     * @param playerIndex 0-based player index (0-3)
     * @param enemyType the type of enemy killed
     */
    public void recordKill(int playerIndex, Tank.EnemyType enemyType) {
        if (!isValidPlayerIndex(playerIndex)) return;

        kills[playerIndex]++;
        int typeOrdinal = enemyType.ordinal();
        if (typeOrdinal < ENEMY_TYPE_COUNT) {
            killsByType[playerIndex][typeOrdinal]++;
        }

        // Add score based on enemy type
        int points = GameConstants.getScoreForEnemyType(enemyType);
        scores[playerIndex] += points;
        levelScores[playerIndex] += points;
    }

    /**
     * Add score to a player (for non-kill score gains).
     * @param playerIndex 0-based player index (0-3)
     * @param points points to add
     */
    public void addScore(int playerIndex, int points) {
        if (!isValidPlayerIndex(playerIndex)) return;
        scores[playerIndex] += points;
        levelScores[playerIndex] += points;
    }

    /**
     * Set score for a player (for network sync).
     * @param playerIndex 0-based player index (0-3)
     * @param score new score value
     */
    public void setScore(int playerIndex, int score) {
        if (!isValidPlayerIndex(playerIndex)) return;
        scores[playerIndex] = score;
    }

    /**
     * Set kills for a player (for network sync).
     * @param playerIndex 0-based player index (0-3)
     * @param killCount new kill count
     */
    public void setKills(int playerIndex, int killCount) {
        if (!isValidPlayerIndex(playerIndex)) return;
        kills[playerIndex] = killCount;
    }

    /**
     * Set kills by type for a player (for network sync).
     * @param playerIndex 0-based player index (0-3)
     * @param killsByTypeArray array of kills by enemy type
     */
    public void setKillsByType(int playerIndex, int[] killsByTypeArray) {
        if (!isValidPlayerIndex(playerIndex) || killsByTypeArray == null) return;
        System.arraycopy(killsByTypeArray, 0, killsByType[playerIndex], 0,
                        Math.min(ENEMY_TYPE_COUNT, killsByTypeArray.length));
    }

    /**
     * Get kills for a player.
     * @param playerIndex 0-based player index (0-3)
     * @return kill count
     */
    public int getKills(int playerIndex) {
        if (!isValidPlayerIndex(playerIndex)) return 0;
        return kills[playerIndex];
    }

    /**
     * Get total score for a player.
     * @param playerIndex 0-based player index (0-3)
     * @return score
     */
    public int getScore(int playerIndex) {
        if (!isValidPlayerIndex(playerIndex)) return 0;
        return scores[playerIndex];
    }

    /**
     * Get level score for a player.
     * @param playerIndex 0-based player index (0-3)
     * @return level score
     */
    public int getLevelScore(int playerIndex) {
        if (!isValidPlayerIndex(playerIndex)) return 0;
        return levelScores[playerIndex];
    }

    /**
     * Get kill count for specific enemy type.
     * @param playerIndex 0-based player index (0-3)
     * @param enemyType the enemy type
     * @return kill count for that type
     */
    public int getKillsByType(int playerIndex, Tank.EnemyType enemyType) {
        if (!isValidPlayerIndex(playerIndex)) return 0;
        int typeOrdinal = enemyType.ordinal();
        if (typeOrdinal >= ENEMY_TYPE_COUNT) return 0;
        return killsByType[playerIndex][typeOrdinal];
    }

    /**
     * Get all kills by type for a player.
     * @param playerIndex 0-based player index (0-3)
     * @return array of kills by type (copy)
     */
    public int[] getKillsByTypeArray(int playerIndex) {
        if (!isValidPlayerIndex(playerIndex)) return new int[ENEMY_TYPE_COUNT];
        return Arrays.copyOf(killsByType[playerIndex], ENEMY_TYPE_COUNT);
    }

    /**
     * Reset level scores (called when advancing to next level).
     */
    public void resetLevelScores() {
        Arrays.fill(levelScores, 0);
    }

    /**
     * Reset all statistics for a player.
     * @param playerIndex 0-based player index (0-3)
     */
    public void resetPlayer(int playerIndex) {
        if (!isValidPlayerIndex(playerIndex)) return;
        kills[playerIndex] = 0;
        scores[playerIndex] = 0;
        levelScores[playerIndex] = 0;
        Arrays.fill(killsByType[playerIndex], 0);
    }

    /**
     * Reset all statistics for all players.
     */
    public void resetAll() {
        Arrays.fill(kills, 0);
        Arrays.fill(scores, 0);
        Arrays.fill(levelScores, 0);
        for (int[] row : killsByType) {
            Arrays.fill(row, 0);
        }
    }

    /**
     * Reset kills and kill breakdown but preserve scores.
     */
    public void resetKillsOnly() {
        Arrays.fill(kills, 0);
        for (int[] row : killsByType) {
            Arrays.fill(row, 0);
        }
    }

    private boolean isValidPlayerIndex(int index) {
        return index >= 0 && index < MAX_PLAYERS;
    }

    // Direct array access for backwards compatibility during migration
    // These should be removed after full migration

    /**
     * @deprecated Use getKills(playerIndex) instead
     */
    @Deprecated
    public int[] getKillsArray() {
        return kills;
    }

    /**
     * @deprecated Use getScore(playerIndex) instead
     */
    @Deprecated
    public int[] getScoresArray() {
        return scores;
    }

    /**
     * @deprecated Use getLevelScore(playerIndex) instead
     */
    @Deprecated
    public int[] getLevelScoresArray() {
        return levelScores;
    }

    /**
     * @deprecated Use getKillsByTypeArray(playerIndex) instead
     */
    @Deprecated
    public int[][] getKillsByTypeMatrix() {
        return killsByType;
    }
}
