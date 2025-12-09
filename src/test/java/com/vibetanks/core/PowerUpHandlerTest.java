package com.vibetanks.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PowerUpHandler.
 */
class PowerUpHandlerTest {

    private List<Tank> playerTanks;
    private List<Tank> enemyTanks;

    @BeforeEach
    void setUp() {
        playerTanks = new ArrayList<>();
        enemyTanks = new ArrayList<>();
    }

    @Nested
    @DisplayName("PlayerCollectionResult Tests")
    class PlayerCollectionResultTests {

        @Test
        @DisplayName("Result initializes with default values")
        void resultInitializesWithDefaults() {
            PowerUpHandler.PlayerCollectionResult result = new PowerUpHandler.PlayerCollectionResult();

            assertFalse(result.collected);
            assertNull(result.type);
            assertFalse(result.activateShovel);
            assertFalse(result.activateFreeze);
            assertFalse(result.activateBomb);
        }
    }

    @Nested
    @DisplayName("EnemyCollectionResult Tests")
    class EnemyCollectionResultTests {

        @Test
        @DisplayName("Result initializes with default values")
        void resultInitializesWithDefaults() {
            PowerUpHandler.EnemyCollectionResult result = new PowerUpHandler.EnemyCollectionResult();

            assertFalse(result.collected);
            assertNull(result.type);
            assertNull(result.collectorEnemy);
            assertFalse(result.removeShovel);
            assertFalse(result.activateFreeze);
            assertFalse(result.activateBomb);
            assertFalse(result.activateCar);
        }
    }

    @Nested
    @DisplayName("checkPlayerCollection Tests")
    class CheckPlayerCollectionTests {

        @Test
        @DisplayName("Player collects power-up when overlapping")
        void playerCollectsPowerUp() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertTrue(result.collected);
            assertEquals(PowerUp.Type.STAR, result.type);
        }

        @Test
        @DisplayName("Player doesn't collect power-up when not overlapping")
        void playerDoesntCollectDistantPowerUp() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(500, 500, PowerUp.Type.STAR);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertFalse(result.collected);
        }

        @Test
        @DisplayName("Dead player doesn't collect power-up")
        void deadPlayerDoesntCollect() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setAlive(false);
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertFalse(result.collected);
        }

        @Test
        @DisplayName("SHOVEL power-up sets activateShovel flag")
        void shovelSetsFlag() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.SHOVEL);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertTrue(result.collected);
            assertTrue(result.activateShovel);
            assertFalse(result.activateFreeze);
            assertFalse(result.activateBomb);
        }

        @Test
        @DisplayName("FREEZE power-up sets activateFreeze flag")
        void freezeSetsFlag() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.FREEZE);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertTrue(result.collected);
            assertTrue(result.activateFreeze);
        }

        @Test
        @DisplayName("BOMB power-up sets activateBomb flag")
        void bombSetsFlag() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.BOMB);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertTrue(result.collected);
            assertTrue(result.activateBomb);
        }

        @Test
        @DisplayName("STAR power-up applies effect directly")
        void starAppliesEffectDirectly() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            int initialStars = player.getStarCount();
            playerTanks.add(player);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            PowerUpHandler.PlayerCollectionResult result = PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            assertTrue(result.collected);
            assertEquals(initialStars + 1, player.getStarCount());
        }
    }

    @Nested
    @DisplayName("checkEnemyCollection Tests")
    class CheckEnemyCollectionTests {

        @Test
        @DisplayName("Enemy collects power-up when overlapping")
        void enemyCollectsPowerUp() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            PowerUpHandler.EnemyCollectionResult result = PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            assertTrue(result.collected);
            assertEquals(PowerUp.Type.STAR, result.type);
            assertEquals(enemy, result.collectorEnemy);
        }

        @Test
        @DisplayName("SHOVEL sets removeShovel flag for enemy")
        void shovelSetsRemoveFlag() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.SHOVEL);

            PowerUpHandler.EnemyCollectionResult result = PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            assertTrue(result.collected);
            assertTrue(result.removeShovel);
        }

        @Test
        @DisplayName("FREEZE sets activateFreeze flag for enemy")
        void freezeSetsFlag() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.FREEZE);

            PowerUpHandler.EnemyCollectionResult result = PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            assertTrue(result.collected);
            assertTrue(result.activateFreeze);
        }

        @Test
        @DisplayName("BOMB sets activateBomb flag for enemy")
        void bombSetsFlag() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.BOMB);

            PowerUpHandler.EnemyCollectionResult result = PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            assertTrue(result.collected);
            assertTrue(result.activateBomb);
        }

        @Test
        @DisplayName("CAR sets activateCar flag and applies effect to collector")
        void carSetsFlag() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.CAR);

            PowerUpHandler.EnemyCollectionResult result = PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            assertTrue(result.collected);
            assertTrue(result.activateCar);
            assertEquals(enemy, result.collectorEnemy);
        }
    }

    @Nested
    @DisplayName("applyEnemyCarSpeedBoost Tests")
    class ApplyEnemyCarSpeedBoostTests {

        @Test
        @DisplayName("Speed boost applied to all enemies except collector")
        void speedBoostAppliedToAllExceptCollector() {
            Tank collector = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            Tank other1 = new Tank(200, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            Tank other2 = new Tank(300, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(collector);
            enemyTanks.add(other1);
            enemyTanks.add(other2);

            PowerUpHandler.applyEnemyCarSpeedBoost(enemyTanks, collector, 1.5);

            // Other enemies should have temp speed boost
            assertTrue(other1.getTempSpeedBoost() > 1.0);
            assertTrue(other2.getTempSpeedBoost() > 1.0);
        }
    }

    @Nested
    @DisplayName("removeEnemyTeamSpeedBoost Tests")
    class RemoveEnemyTeamSpeedBoostTests {

        @Test
        @DisplayName("Speed boost removed from all except collector")
        void speedBoostRemovedFromAllExceptCollector() {
            Tank collector = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            Tank other = new Tank(200, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            other.applyTempSpeedBoost(1.5);
            enemyTanks.add(collector);
            enemyTanks.add(other);

            // Verify boost was applied
            assertTrue(other.getTempSpeedBoost() > 1.0);

            PowerUpHandler.removeEnemyTeamSpeedBoost(enemyTanks, collector);

            // After removal, temp boost should be reset (0.0 means no boost applied)
            assertEquals(0.0, other.getTempSpeedBoost(), 0.01);
        }
    }

    @Nested
    @DisplayName("applyRandomBossReward Tests")
    class ApplyRandomBossRewardTests {

        @Test
        @DisplayName("Boss reward applies some power-up to player")
        void bossRewardAppliesPowerUp() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);

            PowerUp.Type reward = PowerUpHandler.applyRandomBossReward(player);

            assertNotNull(reward);
            // Verify it's one of the allowed types (not BOMB or FREEZE)
            assertNotEquals(PowerUp.Type.BOMB, reward);
            assertNotEquals(PowerUp.Type.FREEZE, reward);
            assertNotEquals(PowerUp.Type.SHOVEL, reward);
        }
    }
}
