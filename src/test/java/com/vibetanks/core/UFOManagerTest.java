package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UFOManager Tests")
class UFOManagerTest {

    private UFOManager manager;

    @BeforeEach
    void setUp() {
        manager = new UFOManager();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("New manager should have no UFO")
        void newManagerShouldHaveNoUFO() {
            assertNull(manager.getUFO());
        }

        @Test
        @DisplayName("New manager should have no easter egg")
        void newManagerShouldHaveNoEasterEgg() {
            assertNull(manager.getEasterEgg());
        }

        @Test
        @DisplayName("New manager should not have spawned UFO this level")
        void newManagerShouldNotHaveSpawnedUFO() {
            assertFalse(manager.isUfoSpawnedThisLevel());
        }

        @Test
        @DisplayName("New manager should not have killed UFO")
        void newManagerShouldNotHaveKilledUFO() {
            assertFalse(manager.isUfoWasKilled());
        }

        @Test
        @DisplayName("New manager should have zero message timers")
        void newManagerShouldHaveZeroMessageTimers() {
            assertEquals(0, manager.getUfoLostMessageTimer());
            assertEquals(0, manager.getUfoKilledMessageTimer());
        }
    }

    @Nested
    @DisplayName("UFO Spawning Tests")
    class UFOSpawningTests {

        @Test
        @DisplayName("checkAndSpawnUFO should not spawn without machinegun")
        void checkAndSpawnUFOShouldNotSpawnWithoutMachinegun() {
            List<Tank> players = new ArrayList<>();
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            players.add(player);
            int[] kills = {10}; // Has kills but no machinegun

            manager.checkAndSpawnUFO(players, kills, 800, 600);

            assertNull(manager.getUFO());
            assertFalse(manager.isUfoSpawnedThisLevel());
        }

        @Test
        @DisplayName("checkAndSpawnUFO should not spawn with insufficient kills")
        void checkAndSpawnUFOShouldNotSpawnWithInsufficientKills() {
            List<Tank> players = new ArrayList<>();
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setMachinegunCount(1);
            players.add(player);
            int[] kills = {3}; // Has machinegun but only 3 kills (need 5)

            manager.checkAndSpawnUFO(players, kills, 800, 600);

            assertNull(manager.getUFO());
        }

        @Test
        @DisplayName("checkAndSpawnUFO should spawn with machinegun and 5+ kills")
        void checkAndSpawnUFOShouldSpawnWithMachinegunAndKills() {
            List<Tank> players = new ArrayList<>();
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setMachinegunCount(1);
            players.add(player);
            int[] kills = {5}; // Has machinegun and 5 kills

            manager.checkAndSpawnUFO(players, kills, 800, 600);

            assertNotNull(manager.getUFO());
            assertTrue(manager.isUfoSpawnedThisLevel());
        }

        @Test
        @DisplayName("checkAndSpawnUFO should only spawn once per level")
        void checkAndSpawnUFOShouldOnlySpawnOnce() {
            List<Tank> players = new ArrayList<>();
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setMachinegunCount(1);
            players.add(player);
            int[] kills = {5};

            manager.checkAndSpawnUFO(players, kills, 800, 600);
            UFO firstUfo = manager.getUFO();

            // Try to spawn again
            manager.setUFO(null); // Clear UFO but keep spawned flag
            manager.checkAndSpawnUFO(players, kills, 800, 600);

            // Should not spawn new UFO
            assertNull(manager.getUFO());
            assertTrue(manager.isUfoSpawnedThisLevel());
        }
    }

    @Nested
    @DisplayName("UFO Destruction Tests")
    class UFODestructionTests {

        @Test
        @DisplayName("handleUFODestroyed should spawn easter egg")
        void handleUFODestroyedShouldSpawnEasterEgg() {
            manager.handleUFODestroyed(1, 200, 300);

            assertNotNull(manager.getEasterEgg());
            assertEquals(200, manager.getEasterEgg().getX());
            assertEquals(300, manager.getEasterEgg().getY());
        }

        @Test
        @DisplayName("handleUFODestroyed should set killed message timer")
        void handleUFODestroyedShouldSetMessageTimer() {
            manager.handleUFODestroyed(1, 200, 300);

            assertTrue(manager.getUfoKilledMessageTimer() > 0);
        }

        @Test
        @DisplayName("handleUFODestroyed should set ufoWasKilled flag")
        void handleUFODestroyedShouldSetKilledFlag() {
            manager.handleUFODestroyed(1, 200, 300);

            assertTrue(manager.isUfoWasKilled());
        }

        @Test
        @DisplayName("handleUFODestroyed should clear UFO")
        void handleUFODestroyedShouldClearUFO() {
            // First spawn UFO
            List<Tank> players = new ArrayList<>();
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setMachinegunCount(1);
            players.add(player);
            manager.checkAndSpawnUFO(players, new int[]{5}, 800, 600);
            assertNotNull(manager.getUFO());

            manager.handleUFODestroyed(1, 200, 300);

            assertNull(manager.getUFO());
        }
    }

    @Nested
    @DisplayName("Easter Egg Update Tests")
    class EasterEggUpdateTests {

        @Test
        @DisplayName("updateEasterEgg should return empty result when no easter egg")
        void updateEasterEggShouldReturnEmptyResultWhenNoEasterEgg() {
            List<Tank> players = new ArrayList<>();
            List<Tank> enemies = new ArrayList<>();

            UFOManager.UpdateResult result = manager.updateEasterEgg(players, enemies);

            assertFalse(result.easterEggCollectedByPlayer);
            assertFalse(result.easterEggCollectedByEnemy);
            assertFalse(result.easterEggExpired);
        }

        @Test
        @DisplayName("Easter egg should expire when lifetime runs out")
        void easterEggShouldExpireWhenLifetimeRunsOut() {
            manager.handleUFODestroyed(1, 200, 300);
            manager.getEasterEgg().setLifetime(1);

            List<Tank> players = new ArrayList<>();
            List<Tank> enemies = new ArrayList<>();

            UFOManager.UpdateResult result = manager.updateEasterEgg(players, enemies);

            assertTrue(result.easterEggExpired);
            assertNull(manager.getEasterEgg());
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("reset should clear all state")
        void resetShouldClearAllState() {
            // Set up some state
            List<Tank> players = new ArrayList<>();
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setMachinegunCount(1);
            players.add(player);
            manager.checkAndSpawnUFO(players, new int[]{5}, 800, 600);
            manager.handleUFODestroyed(1, 200, 300);

            manager.reset();

            assertNull(manager.getUFO());
            assertNull(manager.getEasterEgg());
            assertFalse(manager.isUfoSpawnedThisLevel());
            assertFalse(manager.isUfoWasKilled());
            assertEquals(0, manager.getUfoLostMessageTimer());
            assertEquals(0, manager.getUfoKilledMessageTimer());
        }
    }

    @Nested
    @DisplayName("Network Sync Tests")
    class NetworkSyncTests {

        @Test
        @DisplayName("Setters should update state correctly")
        void settersShouldUpdateStateCorrectly() {
            UFO ufo = new UFO(100, 100, true);
            EasterEgg egg = new EasterEgg(200, 200);

            manager.setUFO(ufo);
            manager.setEasterEgg(egg);
            manager.setUfoSpawnedThisLevel(true);
            manager.setUfoWasKilled(true);
            manager.setUfoLostMessageTimer(50);
            manager.setUfoKilledMessageTimer(60);

            assertEquals(ufo, manager.getUFO());
            assertEquals(egg, manager.getEasterEgg());
            assertTrue(manager.isUfoSpawnedThisLevel());
            assertTrue(manager.isUfoWasKilled());
            assertEquals(50, manager.getUfoLostMessageTimer());
            assertEquals(60, manager.getUfoKilledMessageTimer());
        }

        @Test
        @DisplayName("applyNetworkState should update all relevant state")
        void applyNetworkStateShouldUpdateAllState() {
            UFO ufo = new UFO(100, 100, true);
            EasterEgg egg = new EasterEgg(200, 200);

            manager.applyNetworkState(ufo, egg, 30, 40);

            assertEquals(ufo, manager.getUFO());
            assertEquals(egg, manager.getEasterEgg());
            assertEquals(30, manager.getUfoLostMessageTimer());
            assertEquals(40, manager.getUfoKilledMessageTimer());
        }
    }
}
