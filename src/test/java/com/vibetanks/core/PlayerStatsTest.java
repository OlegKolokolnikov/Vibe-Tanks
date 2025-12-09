package com.vibetanks.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerStats.
 */
class PlayerStatsTest {

    private PlayerStats stats;

    @BeforeEach
    void setUp() {
        stats = new PlayerStats();
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("New PlayerStats has zero kills for all players")
        void newStatsHasZeroKills() {
            for (int i = 0; i < 4; i++) {
                assertEquals(0, stats.getKills(i));
            }
        }

        @Test
        @DisplayName("New PlayerStats has zero scores for all players")
        void newStatsHasZeroScores() {
            for (int i = 0; i < 4; i++) {
                assertEquals(0, stats.getScore(i));
            }
        }

        @Test
        @DisplayName("New PlayerStats has zero level scores for all players")
        void newStatsHasZeroLevelScores() {
            for (int i = 0; i < 4; i++) {
                assertEquals(0, stats.getLevelScore(i));
            }
        }
    }

    @Nested
    @DisplayName("recordKill Tests")
    class RecordKillTests {

        @Test
        @DisplayName("Recording a kill increments kill count")
        void recordKillIncrementsKillCount() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            assertEquals(1, stats.getKills(0));
        }

        @Test
        @DisplayName("Recording a kill adds correct score for REGULAR enemy")
        void recordKillAddsScoreForRegular() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            assertEquals(GameConstants.getScoreForEnemyType(Tank.EnemyType.REGULAR), stats.getScore(0));
        }

        @Test
        @DisplayName("Recording a kill adds correct score for BOSS enemy")
        void recordKillAddsScoreForBoss() {
            stats.recordKill(0, Tank.EnemyType.BOSS);
            assertEquals(GameConstants.getScoreForEnemyType(Tank.EnemyType.BOSS), stats.getScore(0));
        }

        @Test
        @DisplayName("Recording a kill updates level score")
        void recordKillUpdatesLevelScore() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            assertEquals(stats.getScore(0), stats.getLevelScore(0));
        }

        @Test
        @DisplayName("Recording a kill updates kills by type")
        void recordKillUpdatesKillsByType() {
            stats.recordKill(0, Tank.EnemyType.ARMORED);
            stats.recordKill(0, Tank.EnemyType.ARMORED);
            stats.recordKill(0, Tank.EnemyType.FAST);

            assertEquals(2, stats.getKillsByType(0, Tank.EnemyType.ARMORED));
            assertEquals(1, stats.getKillsByType(0, Tank.EnemyType.FAST));
            assertEquals(0, stats.getKillsByType(0, Tank.EnemyType.REGULAR));
        }

        @Test
        @DisplayName("Recording kill for invalid player index does nothing")
        void recordKillInvalidPlayerIndexDoesNothing() {
            stats.recordKill(-1, Tank.EnemyType.REGULAR);
            stats.recordKill(5, Tank.EnemyType.REGULAR);
            // Should not throw and kills should remain 0
            for (int i = 0; i < 4; i++) {
                assertEquals(0, stats.getKills(i));
            }
        }
    }

    @Nested
    @DisplayName("addScore Tests")
    class AddScoreTests {

        @Test
        @DisplayName("Adding score increases total score")
        void addScoreIncreasesTotalScore() {
            stats.addScore(1, 50);
            assertEquals(50, stats.getScore(1));
        }

        @Test
        @DisplayName("Adding score increases level score")
        void addScoreIncreasesLevelScore() {
            stats.addScore(1, 50);
            assertEquals(50, stats.getLevelScore(1));
        }

        @Test
        @DisplayName("Adding score does not affect kills")
        void addScoreDoesNotAffectKills() {
            stats.addScore(1, 100);
            assertEquals(0, stats.getKills(1));
        }

        @Test
        @DisplayName("Adding score for invalid player index does nothing")
        void addScoreInvalidPlayerIndexDoesNothing() {
            stats.addScore(-1, 100);
            stats.addScore(10, 100);
            for (int i = 0; i < 4; i++) {
                assertEquals(0, stats.getScore(i));
            }
        }
    }

    @Nested
    @DisplayName("setScore and setKills Tests")
    class SetterTests {

        @Test
        @DisplayName("setScore updates player score")
        void setScoreUpdatesPlayerScore() {
            stats.setScore(2, 1000);
            assertEquals(1000, stats.getScore(2));
        }

        @Test
        @DisplayName("setKills updates player kills")
        void setKillsUpdatesPlayerKills() {
            stats.setKills(3, 25);
            assertEquals(25, stats.getKills(3));
        }

        @Test
        @DisplayName("setKillsByType updates kills array")
        void setKillsByTypeUpdatesArray() {
            int[] killsByType = {1, 2, 3, 4, 5, 6};
            stats.setKillsByType(0, killsByType);

            assertEquals(1, stats.getKillsByType(0, Tank.EnemyType.REGULAR));
            assertEquals(2, stats.getKillsByType(0, Tank.EnemyType.ARMORED));
            assertEquals(3, stats.getKillsByType(0, Tank.EnemyType.FAST));
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("resetLevelScores resets only level scores")
        void resetLevelScoresResetsOnlyLevelScores() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            int totalScore = stats.getScore(0);
            int totalKills = stats.getKills(0);

            stats.resetLevelScores();

            assertEquals(0, stats.getLevelScore(0));
            assertEquals(totalScore, stats.getScore(0)); // Total score preserved
            assertEquals(totalKills, stats.getKills(0)); // Kills preserved
        }

        @Test
        @DisplayName("resetPlayer resets all stats for specific player")
        void resetPlayerResetsAllStatsForPlayer() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            stats.recordKill(1, Tank.EnemyType.REGULAR);

            stats.resetPlayer(0);

            assertEquals(0, stats.getKills(0));
            assertEquals(0, stats.getScore(0));
            assertEquals(0, stats.getLevelScore(0));
            // Player 1 should be unaffected
            assertEquals(1, stats.getKills(1));
        }

        @Test
        @DisplayName("resetAll resets stats for all players")
        void resetAllResetsAllPlayers() {
            for (int i = 0; i < 4; i++) {
                stats.recordKill(i, Tank.EnemyType.REGULAR);
            }

            stats.resetAll();

            for (int i = 0; i < 4; i++) {
                assertEquals(0, stats.getKills(i));
                assertEquals(0, stats.getScore(i));
                assertEquals(0, stats.getLevelScore(i));
            }
        }

        @Test
        @DisplayName("resetKillsOnly resets kills but preserves scores")
        void resetKillsOnlyPreservesScores() {
            stats.recordKill(0, Tank.EnemyType.BOSS);
            int scoreBeforeReset = stats.getScore(0);

            stats.resetKillsOnly();

            assertEquals(0, stats.getKills(0));
            assertEquals(scoreBeforeReset, stats.getScore(0));
        }
    }

    @Nested
    @DisplayName("getKillsByTypeArray Tests")
    class GetKillsByTypeArrayTests {

        @Test
        @DisplayName("Returns copy of array, not original")
        void returnsCopyOfArray() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            int[] copy = stats.getKillsByTypeArray(0);
            copy[0] = 999;

            // Original should be unmodified
            assertEquals(1, stats.getKillsByType(0, Tank.EnemyType.REGULAR));
        }

        @Test
        @DisplayName("Returns empty array for invalid player index")
        void returnsEmptyArrayForInvalidIndex() {
            int[] result = stats.getKillsByTypeArray(-1);
            assertNotNull(result);
            assertEquals(6, result.length);
            for (int val : result) {
                assertEquals(0, val);
            }
        }
    }

    @Nested
    @DisplayName("Boundary Tests")
    class BoundaryTests {

        @Test
        @DisplayName("Player index 0 is valid")
        void playerIndex0IsValid() {
            stats.recordKill(0, Tank.EnemyType.REGULAR);
            assertEquals(1, stats.getKills(0));
        }

        @Test
        @DisplayName("Player index 3 is valid")
        void playerIndex3IsValid() {
            stats.recordKill(3, Tank.EnemyType.REGULAR);
            assertEquals(1, stats.getKills(3));
        }

        @Test
        @DisplayName("Player index 4 is invalid")
        void playerIndex4IsInvalid() {
            assertEquals(0, stats.getKills(4));
        }

        @Test
        @DisplayName("Negative player index returns 0")
        void negativePlayerIndexReturns0() {
            assertEquals(0, stats.getKills(-1));
            assertEquals(0, stats.getScore(-1));
            assertEquals(0, stats.getLevelScore(-1));
        }
    }
}
