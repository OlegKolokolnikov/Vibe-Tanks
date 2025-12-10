package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PowerUpEffectManager Tests")
class PowerUpEffectManagerTest {

    private PowerUpEffectManager manager;
    private GameMap gameMap;

    @BeforeEach
    void setUp() {
        manager = new PowerUpEffectManager();
        gameMap = new GameMap(26, 26);
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("New manager should have no active effects")
        void newManagerShouldHaveNoActiveEffects() {
            assertFalse(manager.areEnemiesFrozen());
            assertFalse(manager.arePlayersFrozen());
            assertFalse(manager.isEnemySpeedBoostActive());
            assertEquals(0, manager.getBaseProtectionDuration());
            assertEquals(0, manager.getEnemyFreezeDuration());
            assertEquals(0, manager.getPlayerFreezeDuration());
        }

        @Test
        @DisplayName("New manager should not be flashing")
        void newManagerShouldNotBeFlashing() {
            assertFalse(manager.isFlashing());
            assertEquals(0, manager.getFlashCount());
            assertEquals(0, manager.getFlashTimer());
        }
    }

    @Nested
    @DisplayName("Base Protection (SHOVEL) Tests")
    class BaseProtectionTests {

        @Test
        @DisplayName("activateBaseProtection should set duration")
        void activateBaseProtectionShouldSetDuration() {
            manager.activateBaseProtection(gameMap);

            assertTrue(manager.getBaseProtectionDuration() > 0);
        }

        @Test
        @DisplayName("activateBaseProtection should reset flashing state")
        void activateBaseProtectionShouldResetFlashing() {
            manager.setFlashing(true);
            manager.setFlashCount(5);

            manager.activateBaseProtection(gameMap);

            assertFalse(manager.isFlashing());
            assertEquals(0, manager.getFlashCount());
        }

        @Test
        @DisplayName("removeBaseProtection should clear duration")
        void removeBaseProtectionShouldClearDuration() {
            manager.activateBaseProtection(gameMap);
            manager.removeBaseProtection(gameMap);

            assertEquals(0, manager.getBaseProtectionDuration());
            assertFalse(manager.isFlashing());
        }

        @Test
        @DisplayName("updateBaseProtection should decrease duration")
        void updateBaseProtectionShouldDecreaseDuration() {
            manager.setBaseProtectionDuration(100);
            int initial = manager.getBaseProtectionDuration();

            manager.updateBaseProtection(gameMap);

            assertEquals(initial - 1, manager.getBaseProtectionDuration());
        }

        @Test
        @DisplayName("updateBaseProtection should start flashing when duration expires")
        void updateBaseProtectionShouldStartFlashingWhenExpired() {
            manager.setBaseProtectionDuration(1);

            manager.updateBaseProtection(gameMap);

            assertEquals(0, manager.getBaseProtectionDuration());
            assertTrue(manager.isFlashing());
        }
    }

    @Nested
    @DisplayName("Enemy Freeze Tests")
    class EnemyFreezeTests {

        @Test
        @DisplayName("activateEnemyFreeze should freeze enemies")
        void activateEnemyFreezeShouldFreezeEnemies() {
            assertFalse(manager.areEnemiesFrozen());

            manager.activateEnemyFreeze();

            assertTrue(manager.areEnemiesFrozen());
            assertTrue(manager.getEnemyFreezeDuration() > 0);
        }

        @Test
        @DisplayName("updateFreezeTimers should decrease enemy freeze duration")
        void updateFreezeTimersShouldDecreaseEnemyFreeze() {
            manager.setEnemyFreezeDuration(100);

            manager.updateFreezeTimers();

            assertEquals(99, manager.getEnemyFreezeDuration());
        }

        @Test
        @DisplayName("Enemies should unfreeze when duration expires")
        void enemiesShouldUnfreezeWhenExpired() {
            manager.setEnemyFreezeDuration(1);
            assertTrue(manager.areEnemiesFrozen());

            manager.updateFreezeTimers();

            assertFalse(manager.areEnemiesFrozen());
        }
    }

    @Nested
    @DisplayName("Player Freeze Tests")
    class PlayerFreezeTests {

        @Test
        @DisplayName("activatePlayerFreeze should freeze players")
        void activatePlayerFreezeShouldFreezePlayers() {
            assertFalse(manager.arePlayersFrozen());

            manager.activatePlayerFreeze();

            assertTrue(manager.arePlayersFrozen());
            assertTrue(manager.getPlayerFreezeDuration() > 0);
        }

        @Test
        @DisplayName("updateFreezeTimers should decrease player freeze duration")
        void updateFreezeTimersShouldDecreasePlayerFreeze() {
            manager.setPlayerFreezeDuration(100);

            manager.updateFreezeTimers();

            assertEquals(99, manager.getPlayerFreezeDuration());
        }

        @Test
        @DisplayName("Players should unfreeze when duration expires")
        void playersShouldUnfreezeWhenExpired() {
            manager.setPlayerFreezeDuration(1);
            assertTrue(manager.arePlayersFrozen());

            manager.updateFreezeTimers();

            assertFalse(manager.arePlayersFrozen());
        }
    }

    @Nested
    @DisplayName("Enemy Speed Boost (CAR) Tests")
    class EnemySpeedBoostTests {

        @Test
        @DisplayName("activateEnemySpeedBoost should enable speed boost")
        void activateEnemySpeedBoostShouldEnable() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);

            manager.activateEnemySpeedBoost(enemy);

            assertTrue(manager.isEnemySpeedBoostActive());
            assertEquals(enemy, manager.getEnemyWithPermanentSpeedBoost());
        }

        @Test
        @DisplayName("updateEnemySpeedBoost should decrease duration")
        void updateEnemySpeedBoostShouldDecreaseDuration() {
            manager.setEnemyTeamSpeedBoostDuration(100);

            manager.updateEnemySpeedBoost();

            assertEquals(99, manager.getEnemyTeamSpeedBoostDuration());
        }

        @Test
        @DisplayName("Speed boost should deactivate when duration expires")
        void speedBoostShouldDeactivateWhenExpired() {
            manager.setEnemyTeamSpeedBoostDuration(1);
            assertTrue(manager.isEnemySpeedBoostActive());

            manager.updateEnemySpeedBoost();

            assertFalse(manager.isEnemySpeedBoostActive());
        }

        @Test
        @DisplayName("clearPermanentSpeedBoostEnemy should clear reference")
        void clearPermanentSpeedBoostEnemyShouldClearReference() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            manager.activateEnemySpeedBoost(enemy);
            assertNotNull(manager.getEnemyWithPermanentSpeedBoost());

            manager.clearPermanentSpeedBoostEnemy();

            assertNull(manager.getEnemyWithPermanentSpeedBoost());
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("reset should clear all effects")
        void resetShouldClearAllEffects() {
            // Activate various effects
            manager.activateBaseProtection(gameMap);
            manager.activateEnemyFreeze();
            manager.activatePlayerFreeze();
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            manager.activateEnemySpeedBoost(enemy);
            manager.setFlashing(true);

            manager.reset();

            assertEquals(0, manager.getBaseProtectionDuration());
            assertFalse(manager.isFlashing());
            assertEquals(0, manager.getFlashCount());
            assertEquals(0, manager.getFlashTimer());
            assertEquals(0, manager.getEnemyFreezeDuration());
            assertEquals(0, manager.getPlayerFreezeDuration());
            assertEquals(0, manager.getEnemyTeamSpeedBoostDuration());
            assertNull(manager.getEnemyWithPermanentSpeedBoost());
        }
    }

    @Nested
    @DisplayName("Network Sync Setter Tests")
    class NetworkSyncSetterTests {

        @Test
        @DisplayName("All setters should work correctly")
        void allSettersShouldWork() {
            manager.setBaseProtectionDuration(500);
            manager.setFlashing(true);
            manager.setFlashCount(3);
            manager.setFlashTimer(10);
            manager.setEnemyFreezeDuration(200);
            manager.setPlayerFreezeDuration(150);
            manager.setEnemyTeamSpeedBoostDuration(300);

            assertEquals(500, manager.getBaseProtectionDuration());
            assertTrue(manager.isFlashing());
            assertEquals(3, manager.getFlashCount());
            assertEquals(10, manager.getFlashTimer());
            assertEquals(200, manager.getEnemyFreezeDuration());
            assertEquals(150, manager.getPlayerFreezeDuration());
            assertEquals(300, manager.getEnemyTeamSpeedBoostDuration());
        }

        @Test
        @DisplayName("setEnemyWithPermanentSpeedBoost should set tank reference")
        void setEnemyWithPermanentSpeedBoostShouldSetTank() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.FAST);

            manager.setEnemyWithPermanentSpeedBoost(enemy);

            assertEquals(enemy, manager.getEnemyWithPermanentSpeedBoost());
        }
    }
}
