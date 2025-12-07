package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("EnemySpawner Tests")
class EnemySpawnerTest {

    private EnemySpawner spawner;
    private GameMap mockMap;
    private List<Tank> enemyTanks;

    @BeforeEach
    void setUp() {
        mockMap = mock(GameMap.class);
        when(mockMap.getLevelNumber()).thenReturn(1);
        enemyTanks = new ArrayList<>();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize with correct values")
        void constructorInitializesCorrectly() {
            spawner = new EnemySpawner(20, 5, mockMap);

            assertEquals(20, spawner.getRemainingEnemies());
            assertFalse(spawner.allEnemiesSpawned());
        }

        @Test
        @DisplayName("Constructor should get level number from map")
        void constructorGetsLevelFromMap() {
            when(mockMap.getLevelNumber()).thenReturn(5);
            spawner = new EnemySpawner(20, 5, mockMap);

            verify(mockMap).getLevelNumber();
        }
    }

    @Nested
    @DisplayName("Remaining Enemies Tests")
    class RemainingEnemiesTests {

        @BeforeEach
        void setUp() {
            spawner = new EnemySpawner(20, 5, mockMap);
        }

        @Test
        @DisplayName("getRemainingEnemies should return total at start")
        void getRemainingEnemiesReturnsTotal() {
            assertEquals(20, spawner.getRemainingEnemies());
        }

        @Test
        @DisplayName("setRemainingEnemies should update spawned count")
        void setRemainingEnemiesUpdatesCount() {
            spawner.setRemainingEnemies(15);

            assertEquals(15, spawner.getRemainingEnemies());
        }

        @Test
        @DisplayName("setRemainingEnemies to zero should mark all spawned")
        void setRemainingToZeroMarksAllSpawned() {
            spawner.setRemainingEnemies(0);

            assertEquals(0, spawner.getRemainingEnemies());
            assertTrue(spawner.allEnemiesSpawned());
        }
    }

    @Nested
    @DisplayName("Spawn Logic Tests")
    class SpawnLogicTests {

        @BeforeEach
        void setUp() {
            spawner = new EnemySpawner(20, 5, mockMap);
        }

        @Test
        @DisplayName("Update should not spawn immediately (cooldown)")
        void updateDoesNotSpawnImmediately() {
            spawner.update(enemyTanks);

            assertTrue(enemyTanks.isEmpty());
        }

        @Test
        @DisplayName("Update should spawn after cooldown expires")
        void updateSpawnsAfterCooldown() {
            // SPAWN_DELAY is 50 frames
            for (int i = 0; i < 50; i++) {
                spawner.update(enemyTanks);
            }

            assertEquals(1, enemyTanks.size());
        }

        @Test
        @DisplayName("Should not spawn if at max on screen")
        void shouldNotSpawnIfAtMax() {
            // Add 5 tanks (max on screen)
            for (int i = 0; i < 5; i++) {
                enemyTanks.add(new Tank(100 * i, 100, Direction.DOWN, false, 0));
            }

            // Run many updates
            for (int i = 0; i < 200; i++) {
                spawner.update(enemyTanks);
            }

            // Should still have only 5
            assertEquals(5, enemyTanks.size());
        }

        @Test
        @DisplayName("Should spawn more after enemies are killed")
        void shouldSpawnMoreAfterEnemiesKilled() {
            // Spawn initial enemies
            for (int i = 0; i < 250; i++) {
                spawner.update(enemyTanks);
            }

            int initialCount = enemyTanks.size();
            assertTrue(initialCount <= 5);

            // Remove some enemies (simulating kills)
            enemyTanks.clear();

            // Should spawn more
            for (int i = 0; i < 100; i++) {
                spawner.update(enemyTanks);
            }

            assertTrue(enemyTanks.size() > 0);
        }

        @Test
        @DisplayName("Should stop spawning when all enemies spawned")
        void shouldStopWhenAllSpawned() {
            // Use only 1 enemy to avoid BOSS collision issues
            // (BOSS spawns at fixed position that may collide with random positions)
            spawner = new EnemySpawner(1, 10, mockMap);

            // Spawn the single enemy
            for (int i = 0; i < 100; i++) {
                spawner.update(enemyTanks);
            }

            // Should have spawned 1 enemy (the BOSS)
            assertEquals(1, enemyTanks.size());
            assertTrue(spawner.allEnemiesSpawned());

            // Clear and try to spawn more
            enemyTanks.clear();
            for (int i = 0; i < 100; i++) {
                spawner.update(enemyTanks);
            }

            // Should still be 0 (no more to spawn)
            assertEquals(0, enemyTanks.size());
        }
    }

    @Nested
    @DisplayName("All Enemies Spawned Tests")
    class AllEnemiesSpawnedTests {

        @Test
        @DisplayName("allEnemiesSpawned should return false initially")
        void allEnemiesSpawnedFalseInitially() {
            spawner = new EnemySpawner(20, 5, mockMap);

            assertFalse(spawner.allEnemiesSpawned());
        }

        @Test
        @DisplayName("allEnemiesSpawned should return true when count reaches total")
        void allEnemiesSpawnedTrueWhenComplete() {
            spawner = new EnemySpawner(1, 5, mockMap);

            // Spawn the single enemy
            for (int i = 0; i < 60; i++) {
                spawner.update(enemyTanks);
            }

            assertTrue(spawner.allEnemiesSpawned());
        }

        @Test
        @DisplayName("allEnemiesSpawned with zero total should be true immediately")
        void allEnemiesSpawnedWithZeroTotal() {
            spawner = new EnemySpawner(0, 5, mockMap);

            assertTrue(spawner.allEnemiesSpawned());
        }
    }

    @Nested
    @DisplayName("Enemy Type Distribution Tests")
    class EnemyTypeDistributionTests {

        @Test
        @DisplayName("Last enemy should be BOSS")
        void lastEnemyShouldBeBoss() {
            spawner = new EnemySpawner(1, 5, mockMap);

            // Spawn the last (and only) enemy
            for (int i = 0; i < 60; i++) {
                spawner.update(enemyTanks);
            }

            assertEquals(1, enemyTanks.size());
            assertEquals(Tank.EnemyType.BOSS, enemyTanks.get(0).getEnemyType());
        }

        @Test
        @DisplayName("BOSS should have correct health based on level")
        void bossHealthBasedOnLevel() {
            when(mockMap.getLevelNumber()).thenReturn(3);
            spawner = new EnemySpawner(1, 5, mockMap);

            // Spawn the BOSS
            for (int i = 0; i < 60; i++) {
                spawner.update(enemyTanks);
            }

            Tank boss = enemyTanks.get(0);
            // BOSS_BASE_HEALTH (12) + (level - 1) = 12 + 2 = 14
            assertEquals(14, boss.getHealth());
            assertEquals(14, boss.getMaxHealth());
        }

        @Test
        @DisplayName("Spawned enemies should face DOWN initially")
        void spawnedEnemiesFaceDown() {
            spawner = new EnemySpawner(3, 5, mockMap);

            for (int i = 0; i < 200; i++) {
                spawner.update(enemyTanks);
            }

            for (Tank enemy : enemyTanks) {
                assertEquals(Direction.DOWN, enemy.getDirection());
            }
        }

        @Test
        @DisplayName("Spawned enemies should have 1 life (or more for boss)")
        void spawnedEnemiesHaveCorrectLives() {
            spawner = new EnemySpawner(3, 5, mockMap);

            for (int i = 0; i < 200; i++) {
                spawner.update(enemyTanks);
            }

            for (Tank enemy : enemyTanks) {
                // Enemies have 1 life (boss might have different health but still 1 life)
                assertEquals(1, enemy.getLives());
            }
        }
    }

    @Nested
    @DisplayName("Collision Avoidance Tests")
    class CollisionAvoidanceTests {

        @Test
        @DisplayName("Should not spawn on top of existing tank")
        void shouldNotSpawnOnExistingTank() {
            spawner = new EnemySpawner(10, 10, mockMap);

            // Place tanks at all spawn positions
            enemyTanks.add(new Tank(32, 32, Direction.DOWN, false, 0));
            enemyTanks.add(new Tank(12 * 32, 32, Direction.DOWN, false, 0));
            enemyTanks.add(new Tank(24 * 32, 32, Direction.DOWN, false, 0));

            int initialSize = enemyTanks.size();

            // Try to spawn
            for (int i = 0; i < 100; i++) {
                spawner.update(enemyTanks);
            }

            // No new tanks should have spawned because all positions blocked
            assertEquals(initialSize, enemyTanks.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Spawner with one enemy should spawn BOSS")
        void spawnerWithOneEnemySpawnsBoss() {
            spawner = new EnemySpawner(1, 5, mockMap);

            for (int i = 0; i < 60; i++) {
                spawner.update(enemyTanks);
            }

            assertEquals(Tank.EnemyType.BOSS, enemyTanks.get(0).getEnemyType());
        }

        @Test
        @DisplayName("Spawner with negative total should not spawn")
        void spawnerWithNegativeTotalShouldNotSpawn() {
            spawner = new EnemySpawner(-1, 5, mockMap);

            for (int i = 0; i < 100; i++) {
                spawner.update(enemyTanks);
            }

            assertTrue(enemyTanks.isEmpty());
            assertTrue(spawner.allEnemiesSpawned());
        }

        @Test
        @DisplayName("Spawner with zero max on screen should not spawn")
        void spawnerWithZeroMaxOnScreenShouldNotSpawn() {
            spawner = new EnemySpawner(20, 0, mockMap);

            for (int i = 0; i < 100; i++) {
                spawner.update(enemyTanks);
            }

            assertTrue(enemyTanks.isEmpty());
        }
    }
}
