package com.vibetanks.core;

import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevelTransitionHandler Tests")
class LevelTransitionHandlerTest {

    @Nested
    @DisplayName("Reset Player Stats Tests")
    class ResetPlayerStatsTests {

        private int[] playerKills;
        private int[] playerScores;
        private int[] playerLevelScores;
        private int[][] playerKillsByType;

        @BeforeEach
        void setUp() {
            playerKills = new int[]{5, 10, 15, 20};
            playerScores = new int[]{100, 200, 300, 400};
            playerLevelScores = new int[]{50, 60, 70, 80};
            playerKillsByType = new int[4][6];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 6; j++) {
                    playerKillsByType[i][j] = i + j;
                }
            }
        }

        @Test
        @DisplayName("Should reset kills without resetting total scores")
        void shouldResetKillsWithoutResettingTotalScores() {
            LevelTransitionHandler.resetPlayerStats(playerKills, playerScores, playerLevelScores, playerKillsByType, false);

            for (int kills : playerKills) {
                assertEquals(0, kills);
            }
            // Total scores preserved
            assertEquals(100, playerScores[0]);
            assertEquals(200, playerScores[1]);
        }

        @Test
        @DisplayName("Should reset kills and total scores when resetScores is true")
        void shouldResetKillsAndTotalScoresWhenResetScoresTrue() {
            LevelTransitionHandler.resetPlayerStats(playerKills, playerScores, playerLevelScores, playerKillsByType, true);

            for (int kills : playerKills) {
                assertEquals(0, kills);
            }
            for (int score : playerScores) {
                assertEquals(0, score);
            }
        }

        @Test
        @DisplayName("Should reset level scores")
        void shouldResetLevelScores() {
            LevelTransitionHandler.resetPlayerStats(playerKills, playerScores, playerLevelScores, playerKillsByType, false);

            for (int levelScore : playerLevelScores) {
                assertEquals(0, levelScore);
            }
        }

        @Test
        @DisplayName("Should reset kills by type")
        void shouldResetKillsByType() {
            LevelTransitionHandler.resetPlayerStats(playerKills, playerScores, playerLevelScores, playerKillsByType, false);

            for (int[] killsByType : playerKillsByType) {
                for (int kills : killsByType) {
                    assertEquals(0, kills);
                }
            }
        }
    }

    @Nested
    @DisplayName("Reset Player Tanks For Next Level Tests")
    class ResetPlayerTanksForNextLevelTests {

        @Test
        @DisplayName("Should reposition tanks to start positions")
        void shouldRepositionTanksToStartPositions() {
            List<Tank> playerTanks = new ArrayList<>();
            playerTanks.add(new Tank(500, 500, Direction.DOWN, true, 1));
            playerTanks.add(new Tank(600, 600, Direction.LEFT, true, 2));

            double[][] startPositions = {{100, 200}, {150, 250}};

            LevelTransitionHandler.resetPlayerTanksForNextLevel(playerTanks, startPositions);

            assertEquals(100, playerTanks.get(0).getX());
            assertEquals(200, playerTanks.get(0).getY());
            assertEquals(150, playerTanks.get(1).getX());
            assertEquals(250, playerTanks.get(1).getY());
        }

        @Test
        @DisplayName("Should set direction to UP")
        void shouldSetDirectionToUp() {
            List<Tank> playerTanks = new ArrayList<>();
            Tank tank = new Tank(500, 500, Direction.DOWN, true, 1);
            playerTanks.add(tank);

            double[][] startPositions = {{100, 200}};

            LevelTransitionHandler.resetPlayerTanksForNextLevel(playerTanks, startPositions);

            assertEquals(Direction.UP, tank.getDirection());
        }

        @Test
        @DisplayName("Should give temporary shield")
        void shouldGiveTemporaryShield() {
            List<Tank> playerTanks = new ArrayList<>();
            Tank tank = new Tank(500, 500, Direction.DOWN, true, 1);
            playerTanks.add(tank);

            double[][] startPositions = {{100, 200}};

            LevelTransitionHandler.resetPlayerTanksForNextLevel(playerTanks, startPositions);

            assertTrue(tank.hasShield());
        }
    }

    @Nested
    @DisplayName("Reset Player Tanks For Restart Tests")
    class ResetPlayerTanksForRestartTests {

        @Test
        @DisplayName("Should reset lives to 3")
        void shouldResetLivesToThree() {
            List<Tank> playerTanks = new ArrayList<>();
            Tank tank = new Tank(500, 500, Direction.DOWN, true, 1);
            tank.setLives(1);
            playerTanks.add(tank);

            double[][] startPositions = {{100, 200}};

            LevelTransitionHandler.resetPlayerTanksForRestart(playerTanks, startPositions);

            assertEquals(3, tank.getLives());
        }

        @Test
        @DisplayName("Should clear laser duration")
        void shouldClearLaserDuration() {
            List<Tank> playerTanks = new ArrayList<>();
            Tank tank = new Tank(500, 500, Direction.DOWN, true, 1);
            tank.setLaserDuration(100);
            playerTanks.add(tank);

            double[][] startPositions = {{100, 200}};

            LevelTransitionHandler.resetPlayerTanksForRestart(playerTanks, startPositions);

            assertEquals(0, tank.getLaserDuration());
        }

        @Test
        @DisplayName("Should spawn at start position")
        void shouldSpawnAtStartPosition() {
            List<Tank> playerTanks = new ArrayList<>();
            Tank tank = new Tank(500, 500, Direction.DOWN, true, 1);
            tank.setAlive(false);
            playerTanks.add(tank);

            double[][] startPositions = {{100, 200}};

            LevelTransitionHandler.resetPlayerTanksForRestart(playerTanks, startPositions);

            assertEquals(100, tank.getX());
            assertEquals(200, tank.getY());
            assertTrue(tank.isAlive());
        }
    }

    @Nested
    @DisplayName("Clear Projectiles And Collectibles Tests")
    class ClearProjectilesAndCollectiblesTests {

        @Test
        @DisplayName("Should clear all lists")
        void shouldClearAllLists() {
            List<Bullet> bullets = new ArrayList<>();
            bullets.add(new Bullet(100, 100, Direction.UP, true, 1, false));

            List<Laser> lasers = new ArrayList<>();
            lasers.add(new Laser(200, 200, Direction.UP, false, 1));

            List<PowerUp> powerUps = new ArrayList<>();
            powerUps.add(new PowerUp(300, 300));

            LevelTransitionHandler.clearProjectilesAndCollectibles(bullets, lasers, powerUps);

            assertTrue(bullets.isEmpty());
            assertTrue(lasers.isEmpty());
            assertTrue(powerUps.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty lists")
        void shouldHandleEmptyLists() {
            List<Bullet> bullets = new ArrayList<>();
            List<Laser> lasers = new ArrayList<>();
            List<PowerUp> powerUps = new ArrayList<>();

            assertDoesNotThrow(() ->
                LevelTransitionHandler.clearProjectilesAndCollectibles(bullets, lasers, powerUps));
        }
    }

    @Nested
    @DisplayName("Reset Machinegun Kills Tests")
    class ResetMachinegunKillsTests {

        @Test
        @DisplayName("Should reset all machinegun kills to zero")
        void shouldResetAllMachinegunKillsToZero() {
            int[] playerMachinegunKills = {5, 10, 15, 20};

            LevelTransitionHandler.resetMachinegunKills(playerMachinegunKills);

            for (int kills : playerMachinegunKills) {
                assertEquals(0, kills);
            }
        }
    }

    @Nested
    @DisplayName("Clear Dancing Animations Tests")
    class ClearDancingAnimationsTests {

        @Test
        @DisplayName("Should clear dancing characters list")
        void shouldClearDancingCharactersList() {
            List<DancingCharacter> dancingCharacters = new ArrayList<>();
            dancingCharacters.add(new DancingCharacter(100, 200, true, 1));

            List<DancingGirl> victoryDancingGirls = new ArrayList<>();

            LevelTransitionHandler.clearDancingAnimations(dancingCharacters, victoryDancingGirls);

            assertTrue(dancingCharacters.isEmpty());
        }

        @Test
        @DisplayName("Should clear victory dancing girls list")
        void shouldClearVictoryDancingGirlsList() {
            List<DancingCharacter> dancingCharacters = new ArrayList<>();

            List<DancingGirl> victoryDancingGirls = new ArrayList<>();
            victoryDancingGirls.add(new DancingGirl(100, 200, 1));

            LevelTransitionHandler.clearDancingAnimations(dancingCharacters, victoryDancingGirls);

            assertTrue(victoryDancingGirls.isEmpty());
        }
    }

    @Nested
    @DisplayName("Factory Methods Tests")
    class FactoryMethodsTests {

        @Test
        @DisplayName("createEnemySpawner should return new spawner")
        void createEnemySpawnerShouldReturnNewSpawner() {
            GameMap gameMap = new GameMap(26, 26);

            EnemySpawner spawner = LevelTransitionHandler.createEnemySpawner(20, 5, gameMap);

            assertNotNull(spawner);
            assertEquals(20, spawner.getRemainingEnemies());
        }

        @Test
        @DisplayName("createBase should return base at standard position")
        void createBaseShouldReturnBaseAtStandardPosition() {
            Base base = LevelTransitionHandler.createBase();

            assertNotNull(base);
            assertEquals(12 * 32, base.getX());
            assertEquals(24 * 32, base.getY());
            assertTrue(base.isAlive());
        }
    }
}
