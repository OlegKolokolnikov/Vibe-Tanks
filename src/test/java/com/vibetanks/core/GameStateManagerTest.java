package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameStateManager Tests")
class GameStateManagerTest {

    private GameStateManager manager;

    @BeforeEach
    void setUp() {
        manager = new GameStateManager();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("New manager should start at level 1")
        void newManagerShouldStartAtLevel1() {
            assertEquals(1, manager.getCurrentLevel());
        }

        @Test
        @DisplayName("New manager should not have victory")
        void newManagerShouldNotHaveVictory() {
            assertFalse(manager.isVictory());
            assertFalse(manager.isVictoryConditionMet());
        }

        @Test
        @DisplayName("New manager should not have game over")
        void newManagerShouldNotHaveGameOver() {
            assertFalse(manager.isGameOver());
        }

        @Test
        @DisplayName("New manager should have no boss killer")
        void newManagerShouldHaveNoBossKiller() {
            assertEquals(-1, manager.getBossKillerPlayerIndex());
            assertNull(manager.getBossKillPowerUpReward());
        }

        @Test
        @DisplayName("Dancing characters should be empty initially")
        void dancingCharactersShouldBeEmptyInitially() {
            assertTrue(manager.getDancingCharacters().isEmpty());
            assertFalse(manager.isDancingInitialized());
        }

        @Test
        @DisplayName("Victory dancing girls should be empty initially")
        void victoryDancingGirlsShouldBeEmptyInitially() {
            assertTrue(manager.getVictoryDancingGirls().isEmpty());
            assertFalse(manager.isVictoryDancingInitialized());
        }
    }

    @Nested
    @DisplayName("Victory Condition Tests")
    class VictoryConditionTests {

        @Test
        @DisplayName("updateVictoryCondition should set victoryConditionMet")
        void updateVictoryConditionShouldSetFlag() {
            assertFalse(manager.isVictoryConditionMet());

            manager.updateVictoryCondition(true);

            assertTrue(manager.isVictoryConditionMet());
        }

        @Test
        @DisplayName("updateVictoryCondition should increment delay timer")
        void updateVictoryConditionShouldIncrementTimer() {
            manager.updateVictoryCondition(true);
            int firstTimer = manager.getVictoryDelayTimer();

            manager.updateVictoryCondition(true);

            assertEquals(firstTimer + 1, manager.getVictoryDelayTimer());
        }

        @Test
        @DisplayName("updateVictoryCondition should not trigger immediately")
        void updateVictoryConditionShouldNotTriggerImmediately() {
            boolean triggered = manager.updateVictoryCondition(true);

            assertFalse(triggered);
            assertFalse(manager.isVictory());
        }

        @Test
        @DisplayName("updateVictoryCondition should not proceed if enemies not defeated")
        void updateVictoryConditionShouldNotProceedIfEnemiesNotDefeated() {
            manager.updateVictoryCondition(false);

            assertFalse(manager.isVictoryConditionMet());
            assertEquals(0, manager.getVictoryDelayTimer());
        }
    }

    @Nested
    @DisplayName("Game Over Tests")
    class GameOverTests {

        @Test
        @DisplayName("checkGameOver should return true when all players dead")
        void checkGameOverShouldReturnTrueWhenAllPlayersDead() {
            boolean result = manager.checkGameOver(true, false);

            assertTrue(result);
            assertTrue(manager.isGameOver());
        }

        @Test
        @DisplayName("checkGameOver should return true when base destroyed")
        void checkGameOverShouldReturnTrueWhenBaseDestroyed() {
            boolean result = manager.checkGameOver(false, true);

            assertTrue(result);
            assertTrue(manager.isGameOver());
        }

        @Test
        @DisplayName("checkGameOver should return false when game continues")
        void checkGameOverShouldReturnFalseWhenGameContinues() {
            boolean result = manager.checkGameOver(false, false);

            assertFalse(result);
            assertFalse(manager.isGameOver());
        }
    }

    @Nested
    @DisplayName("Level Management Tests")
    class LevelManagementTests {

        @Test
        @DisplayName("nextLevel should increment level")
        void nextLevelShouldIncrementLevel() {
            assertEquals(1, manager.getCurrentLevel());

            manager.nextLevel();

            assertEquals(2, manager.getCurrentLevel());
        }

        @Test
        @DisplayName("nextLevel should reset state flags")
        void nextLevelShouldResetStateFlags() {
            manager.setVictory(true);
            manager.setGameOver(true);
            manager.setVictoryConditionMet(true);

            manager.nextLevel();

            assertFalse(manager.isVictory());
            assertFalse(manager.isGameOver());
            assertFalse(manager.isVictoryConditionMet());
        }

        @Test
        @DisplayName("resetToLevel1 should reset to level 1")
        void resetToLevel1ShouldResetToLevel1() {
            manager.nextLevel();
            manager.nextLevel();
            assertEquals(3, manager.getCurrentLevel());

            manager.resetToLevel1();

            assertEquals(1, manager.getCurrentLevel());
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("resetForNewLevel should clear victory state")
        void resetForNewLevelShouldClearVictoryState() {
            manager.setVictory(true);
            manager.setVictoryConditionMet(true);
            manager.setVictoryDelayTimer(100);

            manager.resetForNewLevel();

            assertFalse(manager.isVictory());
            assertFalse(manager.isVictoryConditionMet());
            assertEquals(0, manager.getVictoryDelayTimer());
        }

        @Test
        @DisplayName("resetForNewLevel should clear game over state")
        void resetForNewLevelShouldClearGameOverState() {
            manager.setGameOver(true);
            manager.setGameOverSoundPlayed(true);

            manager.resetForNewLevel();

            assertFalse(manager.isGameOver());
            assertFalse(manager.isGameOverSoundPlayed());
        }

        @Test
        @DisplayName("resetForNewLevel should clear boss kill info")
        void resetForNewLevelShouldClearBossKillInfo() {
            manager.setBossKillerPlayerIndex(2);
            manager.setBossKillPowerUpReward(PowerUp.Type.GUN);

            manager.resetForNewLevel();

            assertEquals(-1, manager.getBossKillerPlayerIndex());
            assertNull(manager.getBossKillPowerUpReward());
        }

        @Test
        @DisplayName("resetForNewLevel should clear dancing state")
        void resetForNewLevelShouldClearDancingState() {
            manager.setDancingInitialized(true);
            manager.setVictoryDancingInitialized(true);

            manager.resetForNewLevel();

            assertFalse(manager.isDancingInitialized());
            assertFalse(manager.isVictoryDancingInitialized());
            assertTrue(manager.getDancingCharacters().isEmpty());
            assertTrue(manager.getVictoryDancingGirls().isEmpty());
        }

        @Test
        @DisplayName("resetForRestart should call resetForNewLevel")
        void resetForRestartShouldCallResetForNewLevel() {
            manager.setVictory(true);
            manager.setGameOver(true);

            manager.resetForRestart();

            assertFalse(manager.isVictory());
            assertFalse(manager.isGameOver());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("All state setters should work correctly")
        void allStateSettersShouldWork() {
            manager.setVictory(true);
            manager.setVictoryConditionMet(true);
            manager.setVictoryDelayTimer(50);
            manager.setGameOver(true);
            manager.setGameOverSoundPlayed(true);
            manager.setWinnerBonusAwarded(true);
            manager.setCurrentLevel(5);

            assertTrue(manager.isVictory());
            assertTrue(manager.isVictoryConditionMet());
            assertEquals(50, manager.getVictoryDelayTimer());
            assertTrue(manager.isGameOver());
            assertTrue(manager.isGameOverSoundPlayed());
            assertTrue(manager.isWinnerBonusAwarded());
            assertEquals(5, manager.getCurrentLevel());
        }

        @Test
        @DisplayName("Boss kill setters should work correctly")
        void bossKillSettersShouldWork() {
            manager.setBossKillerPlayerIndex(1);
            manager.setBossKillPowerUpReward(PowerUp.Type.SHIP);

            assertEquals(1, manager.getBossKillerPlayerIndex());
            assertEquals(PowerUp.Type.SHIP, manager.getBossKillPowerUpReward());
        }

        @Test
        @DisplayName("Dancing state setters should work correctly")
        void dancingStateSettersShouldWork() {
            manager.setDancingInitialized(true);
            manager.setVictoryDancingInitialized(true);

            assertTrue(manager.isDancingInitialized());
            assertTrue(manager.isVictoryDancingInitialized());
        }
    }
}
