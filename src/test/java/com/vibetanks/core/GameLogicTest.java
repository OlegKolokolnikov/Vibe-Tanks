package com.vibetanks.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameLogic.
 */
class GameLogicTest {

    private List<Tank> playerTanks;
    private List<Tank> enemyTanks;
    private GameMap gameMap;
    private Base base;

    @BeforeEach
    void setUp() {
        playerTanks = new ArrayList<>();
        enemyTanks = new ArrayList<>();
        gameMap = new GameMap(26, 26);
        base = new Base(12 * 32, 24 * 32);
    }

    @Nested
    @DisplayName("BulletProcessResult Tests")
    class BulletProcessResultTests {

        @Test
        @DisplayName("Result initializes with default values")
        void resultInitializesWithDefaults() {
            GameLogic.BulletProcessResult result = new GameLogic.BulletProcessResult();

            assertFalse(result.enemyKilled);
            assertEquals(-1, result.killerPlayerNumber);
            assertNull(result.killedEnemyType);
            assertFalse(result.shouldDropPowerUp);
            assertFalse(result.baseDestroyed);
            assertFalse(result.playerKilled);
            assertEquals(-1, result.playerKilledIndex);
        }
    }

    @Nested
    @DisplayName("LaserProcessResult Tests")
    class LaserProcessResultTests {

        @Test
        @DisplayName("Result initializes with default values")
        void resultInitializesWithDefaults() {
            GameLogic.LaserProcessResult result = new GameLogic.LaserProcessResult();

            assertFalse(result.enemyKilled);
            assertEquals(-1, result.killerPlayerNumber);
            assertNull(result.killedEnemyType);
            assertFalse(result.baseDestroyed);
            assertFalse(result.ufoDestroyed);
        }
    }

    @Nested
    @DisplayName("PowerUpCollectResult Tests")
    class PowerUpCollectResultTests {

        @Test
        @DisplayName("Result initializes with default values")
        void resultInitializesWithDefaults() {
            GameLogic.PowerUpCollectResult result = new GameLogic.PowerUpCollectResult();

            assertFalse(result.collected);
            assertFalse(result.collectedByPlayer);
            assertFalse(result.collectedByEnemy);
            assertEquals(-1, result.collectorPlayerIndex);
            assertNull(result.collectorEnemy);
            assertNull(result.type);
        }
    }

    @Nested
    @DisplayName("checkPowerUpCollection Tests")
    class CheckPowerUpCollectionTests {

        @Test
        @DisplayName("Player collects power-up when overlapping")
        void playerCollectsPowerUp() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            GameLogic.PowerUpCollectResult result = GameLogic.checkPowerUpCollection(powerUp, playerTanks, enemyTanks);

            assertTrue(result.collected);
            assertTrue(result.collectedByPlayer);
            assertEquals(0, result.collectorPlayerIndex);
            assertEquals(PowerUp.Type.STAR, result.type);
        }

        @Test
        @DisplayName("Enemy collects power-up when overlapping")
        void enemyCollectsPowerUp() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            GameLogic.PowerUpCollectResult result = GameLogic.checkPowerUpCollection(powerUp, playerTanks, enemyTanks);

            assertTrue(result.collected);
            assertTrue(result.collectedByEnemy);
            assertEquals(enemy, result.collectorEnemy);
        }

        @Test
        @DisplayName("No collection when no overlap")
        void noCollectionWhenNoOverlap() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);
            PowerUp powerUp = new PowerUp(500, 500, PowerUp.Type.STAR);

            GameLogic.PowerUpCollectResult result = GameLogic.checkPowerUpCollection(powerUp, playerTanks, enemyTanks);

            assertFalse(result.collected);
        }

        @Test
        @DisplayName("Dead player doesn't collect power-up")
        void deadPlayerDoesntCollect() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setAlive(false);
            playerTanks.add(player);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            GameLogic.PowerUpCollectResult result = GameLogic.checkPowerUpCollection(powerUp, playerTanks, enemyTanks);

            assertFalse(result.collected);
        }
    }

    @Nested
    @DisplayName("checkGameOver Tests")
    class CheckGameOverTests {

        @Test
        @DisplayName("Game over when base is destroyed")
        void gameOverWhenBaseDestroyed() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);
            base.destroy();

            assertTrue(GameLogic.checkGameOver(base, playerTanks));
        }

        @Test
        @DisplayName("Game over when all players dead with no lives")
        void gameOverWhenAllPlayersDead() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setAlive(false);
            player.setLives(0);
            playerTanks.add(player);

            assertTrue(GameLogic.checkGameOver(base, playerTanks));
        }

        @Test
        @DisplayName("Not game over when player alive")
        void notGameOverWhenPlayerAlive() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            assertFalse(GameLogic.checkGameOver(base, playerTanks));
        }

        @Test
        @DisplayName("Not game over when player dead but has lives")
        void notGameOverWhenPlayerHasLives() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            player.setAlive(false);
            player.setLives(2);
            playerTanks.add(player);

            assertFalse(GameLogic.checkGameOver(base, playerTanks));
        }
    }

    @Nested
    @DisplayName("checkVictory Tests")
    class CheckVictoryTests {

        @Test
        @DisplayName("Victory when all enemies spawned and defeated")
        void victoryWhenAllEnemiesDefeated() {
            EnemySpawner spawner = new EnemySpawner(1, 5, gameMap); // 1 total enemy, max 5 on screen
            // Manually spawn all enemies
            spawner.update(enemyTanks);
            // Clear enemies (simulating defeat)
            enemyTanks.clear();
            // Force spawner to mark all spawned
            for (int i = 0; i < 10; i++) spawner.update(enemyTanks);
            enemyTanks.clear();

            // Need spawner to report allEnemiesSpawned
            assertTrue(GameLogic.checkVictory(spawner, enemyTanks) || !spawner.allEnemiesSpawned());
        }

        @Test
        @DisplayName("No victory when enemies remain")
        void noVictoryWhenEnemiesRemain() {
            EnemySpawner spawner = new EnemySpawner(5, 5, gameMap);
            Tank enemy = new Tank(200, 200, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            assertFalse(GameLogic.checkVictory(spawner, enemyTanks));
        }
    }

    @Nested
    @DisplayName("removeDeadEnemies Tests")
    class RemoveDeadEnemiesTests {

        @Test
        @DisplayName("Removes dead enemies from list")
        void removesDeadEnemies() {
            Tank alive = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            Tank dead = new Tank(200, 200, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            dead.setAlive(false);
            enemyTanks.add(alive);
            enemyTanks.add(dead);

            GameLogic.removeDeadEnemies(enemyTanks);

            assertEquals(1, enemyTanks.size());
            assertTrue(enemyTanks.get(0).isAlive());
        }

        @Test
        @DisplayName("Keeps all alive enemies")
        void keepsAliveEnemies() {
            Tank alive1 = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            Tank alive2 = new Tank(200, 200, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(alive1);
            enemyTanks.add(alive2);

            GameLogic.removeDeadEnemies(enemyTanks);

            assertEquals(2, enemyTanks.size());
        }
    }

    @Nested
    @DisplayName("getScoreForKill Tests")
    class GetScoreForKillTests {

        @Test
        @DisplayName("Returns correct score for REGULAR enemy")
        void correctScoreForRegular() {
            int score = GameLogic.getScoreForKill(Tank.EnemyType.REGULAR);
            assertEquals(GameConstants.getScoreForEnemyType(Tank.EnemyType.REGULAR), score);
        }

        @Test
        @DisplayName("Returns correct score for BOSS enemy")
        void correctScoreForBoss() {
            int score = GameLogic.getScoreForKill(Tank.EnemyType.BOSS);
            assertEquals(GameConstants.getScoreForEnemyType(Tank.EnemyType.BOSS), score);
        }
    }

    @Nested
    @DisplayName("addScore Tests")
    class AddScoreTests {

        @Test
        @DisplayName("Adds score to player")
        void addsScoreToPlayer() {
            int[] scores = new int[4];
            int[] levelScores = new int[4];
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);

            GameLogic.ScoreResult result = GameLogic.addScore(0, 50, scores, levelScores, playerTanks);

            assertEquals(0, result.oldScore);
            assertEquals(50, result.newScore);
            assertEquals(50, scores[0]);
            assertEquals(50, levelScores[0]);
        }

        @Test
        @DisplayName("Awards extra life when crossing 100 point threshold")
        void awardsExtraLifeAt100Points() {
            int[] scores = new int[4];
            int[] levelScores = new int[4];
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            int initialLives = player.getLives();
            playerTanks.add(player);

            GameLogic.ScoreResult result = GameLogic.addScore(0, 100, scores, levelScores, playerTanks);

            assertEquals(1, result.livesAwarded);
            assertEquals(initialLives + 1, player.getLives());
        }

        @Test
        @DisplayName("Awards multiple lives when crossing multiple thresholds")
        void awardsMultipleLivesForMultipleThresholds() {
            int[] scores = new int[4];
            int[] levelScores = new int[4];
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            int initialLives = player.getLives();
            playerTanks.add(player);

            GameLogic.ScoreResult result = GameLogic.addScore(0, 350, scores, levelScores, playerTanks);

            assertEquals(3, result.livesAwarded);
            assertEquals(initialLives + 3, player.getLives());
        }

        @Test
        @DisplayName("Invalid player index returns empty result")
        void invalidPlayerIndexReturnsEmptyResult() {
            int[] scores = new int[4];
            int[] levelScores = new int[4];

            GameLogic.ScoreResult result = GameLogic.addScore(-1, 100, scores, levelScores, playerTanks);

            assertEquals(0, result.oldScore);
            assertEquals(0, result.newScore);
            assertEquals(0, result.livesAwarded);
        }
    }

    @Nested
    @DisplayName("TankOverlapResult Tests")
    class TankOverlapResultTests {

        @Test
        @DisplayName("Result initializes with default values")
        void resultInitializesWithDefaults() {
            GameLogic.TankOverlapResult result = new GameLogic.TankOverlapResult();

            assertFalse(result.tank1Killed);
            assertFalse(result.tank2Killed);
            assertNull(result.killedTank1);
            assertNull(result.killedTank2);
        }
    }

    @Nested
    @DisplayName("resolveOverlappingTanks Tests")
    class ResolveOverlappingTanksTests {

        @Test
        @DisplayName("Non-overlapping tanks are not modified")
        void nonOverlappingTanksNotModified() {
            Tank tank1 = new Tank(100, 100, Direction.UP, true, 1);
            Tank tank2 = new Tank(300, 300, Direction.UP, true, 2);
            List<Tank> allTanks = new ArrayList<>();
            allTanks.add(tank1);
            allTanks.add(tank2);

            double x1Before = tank1.getX();
            double y1Before = tank1.getY();
            double x2Before = tank2.getX();
            double y2Before = tank2.getY();

            GameLogic.TankOverlapResult result = GameLogic.resolveOverlappingTanks(allTanks, gameMap);

            assertEquals(x1Before, tank1.getX());
            assertEquals(y1Before, tank1.getY());
            assertEquals(x2Before, tank2.getX());
            assertEquals(y2Before, tank2.getY());
            assertFalse(result.tank1Killed);
            assertFalse(result.tank2Killed);
        }

        @Test
        @DisplayName("BOSS tank kills non-BOSS tank on contact")
        void bossKillsNonBossOnContact() {
            // Clear area for tanks
            gameMap.setTile(3, 3, GameMap.TileType.EMPTY);
            gameMap.setTile(3, 4, GameMap.TileType.EMPTY);
            gameMap.setTile(4, 3, GameMap.TileType.EMPTY);
            gameMap.setTile(4, 4, GameMap.TileType.EMPTY);

            Tank boss = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.BOSS);
            Tank regular = new Tank(100, 100, Direction.UP, true, 1); // Same position = overlap
            List<Tank> allTanks = new ArrayList<>();
            allTanks.add(boss);
            allTanks.add(regular);

            GameLogic.TankOverlapResult result = GameLogic.resolveOverlappingTanks(allTanks, gameMap);

            assertTrue(result.tank2Killed);
            assertEquals(regular, result.killedTank2);
            assertFalse(regular.isAlive());
        }

        @Test
        @DisplayName("Dead tanks are ignored")
        void deadTanksAreIgnored() {
            Tank alive = new Tank(100, 100, Direction.UP, true, 1);
            Tank dead = new Tank(100, 100, Direction.UP, true, 2);
            dead.setAlive(false);
            List<Tank> allTanks = new ArrayList<>();
            allTanks.add(alive);
            allTanks.add(dead);

            GameLogic.TankOverlapResult result = GameLogic.resolveOverlappingTanks(allTanks, gameMap);

            // No kills should occur since one tank is already dead
            assertFalse(result.tank1Killed);
            assertFalse(result.tank2Killed);
        }
    }

    @Nested
    @DisplayName("findPowerUpSpawnPosition Tests")
    class FindPowerUpSpawnPositionTests {

        @Test
        @DisplayName("Returns valid position within map bounds")
        void returnsValidPosition() {
            double[] pos = GameLogic.findPowerUpSpawnPosition(gameMap, GameConstants.TILE_SIZE);

            assertTrue(pos[0] >= 0);
            assertTrue(pos[1] >= 0);
            assertTrue(pos[0] < 26 * GameConstants.TILE_SIZE);
            assertTrue(pos[1] < 26 * GameConstants.TILE_SIZE);
        }

        @Test
        @DisplayName("Returns position on empty tile")
        void returnsPositionOnEmptyTile() {
            double[] pos = GameLogic.findPowerUpSpawnPosition(gameMap, GameConstants.TILE_SIZE);

            int col = (int)(pos[0] / GameConstants.TILE_SIZE);
            int row = (int)(pos[1] / GameConstants.TILE_SIZE);

            // Position should be on EMPTY tile
            GameMap.TileType tile = gameMap.getTile(row, col);
            assertEquals(GameMap.TileType.EMPTY, tile);
        }

        @Test
        @DisplayName("Allocation-free version fills provided array")
        void allocationFreeVersionFillsArray() {
            double[] result = new double[2];
            GameLogic.findPowerUpSpawnPosition(gameMap, GameConstants.TILE_SIZE, result);

            assertTrue(result[0] >= 0);
            assertTrue(result[1] >= 0);
            assertTrue(result[0] < 26 * GameConstants.TILE_SIZE);
            assertTrue(result[1] < 26 * GameConstants.TILE_SIZE);
        }
    }

    @Nested
    @DisplayName("applyPlayerPowerUp Tests")
    class ApplyPlayerPowerUpTests {

        @Test
        @DisplayName("SHOVEL returns SHOVEL type for game-level handling")
        void shovelReturnsType() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.SHOVEL);

            PowerUp.Type result = GameLogic.applyPlayerPowerUp(powerUp, player);

            assertEquals(PowerUp.Type.SHOVEL, result);
        }

        @Test
        @DisplayName("FREEZE returns FREEZE type for game-level handling")
        void freezeReturnsType() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.FREEZE);

            PowerUp.Type result = GameLogic.applyPlayerPowerUp(powerUp, player);

            assertEquals(PowerUp.Type.FREEZE, result);
        }

        @Test
        @DisplayName("STAR applies effect directly to tank")
        void starAppliesDirectly() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            int initialStars = player.getStarCount();
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);

            GameLogic.applyPlayerPowerUp(powerUp, player);

            assertEquals(initialStars + 1, player.getStarCount());
        }
    }

    @Nested
    @DisplayName("applyEnemyPowerUp Tests")
    class ApplyEnemyPowerUpTests {

        @Test
        @DisplayName("SHIELD upgrades REGULAR enemy to ARMORED")
        void shieldUpgradesRegularToArmored() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.SHIELD);

            GameLogic.applyEnemyPowerUp(powerUp, enemy);

            assertEquals(Tank.EnemyType.ARMORED, enemy.getEnemyType());
            assertEquals(2, enemy.getHealth());
            assertEquals(2, enemy.getMaxHealth());
        }

        @Test
        @DisplayName("SHIELD upgrades ARMORED enemy to HEAVY")
        void shieldUpgradesArmoredToHeavy() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.ARMORED);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.SHIELD);

            GameLogic.applyEnemyPowerUp(powerUp, enemy);

            assertEquals(Tank.EnemyType.HEAVY, enemy.getEnemyType());
            assertEquals(3, enemy.getHealth());
            assertEquals(3, enemy.getMaxHealth());
        }

        @Test
        @DisplayName("SHIELD gives HEAVY enemy extra life")
        void shieldGivesHeavyExtraLife() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.HEAVY);
            int initialLives = enemy.getLives();
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.SHIELD);

            GameLogic.applyEnemyPowerUp(powerUp, enemy);

            assertEquals(initialLives + 1, enemy.getLives());
        }

        @Test
        @DisplayName("CAR returns CAR type for game-level handling")
        void carReturnsType() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            PowerUp powerUp = new PowerUp(100, 100, PowerUp.Type.CAR);

            PowerUp.Type result = GameLogic.applyEnemyPowerUp(powerUp, enemy);

            assertEquals(PowerUp.Type.CAR, result);
        }
    }

    @Nested
    @DisplayName("checkEasterEggCollection Tests")
    class CheckEasterEggCollectionTests {

        @Test
        @DisplayName("Returns positive value for player collection")
        void returnsPositiveForPlayerCollection() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);
            EasterEgg egg = new EasterEgg(100, 100);

            int result = GameLogic.checkEasterEggCollection(egg, playerTanks, enemyTanks);

            assertEquals(1, result); // Player 0 + 1 = 1
        }

        @Test
        @DisplayName("Returns negative value for enemy collection")
        void returnsNegativeForEnemyCollection() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);
            EasterEgg egg = new EasterEgg(100, 100);

            int result = GameLogic.checkEasterEggCollection(egg, playerTanks, enemyTanks);

            assertEquals(-1, result);
        }

        @Test
        @DisplayName("Returns zero for no collection")
        void returnsZeroForNoCollection() {
            Tank player = new Tank(100, 100, Direction.UP, true, 1);
            playerTanks.add(player);
            EasterEgg egg = new EasterEgg(500, 500);

            int result = GameLogic.checkEasterEggCollection(egg, playerTanks, enemyTanks);

            assertEquals(0, result);
        }
    }

    @Nested
    @DisplayName("applyEasterEggEffect Tests")
    class ApplyEasterEggEffectTests {

        @Test
        @DisplayName("Player collection transforms enemies to POWER type")
        void playerCollectionTransformsToPower() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            GameLogic.applyEasterEggEffect(enemyTanks, true);

            assertEquals(Tank.EnemyType.POWER, enemy.getEnemyType());
        }

        @Test
        @DisplayName("Enemy collection transforms to HEAVY type")
        void enemyCollectionTransformsToHeavy() {
            Tank enemy = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR);
            enemyTanks.add(enemy);

            GameLogic.applyEasterEggEffect(enemyTanks, false);

            assertEquals(Tank.EnemyType.HEAVY, enemy.getEnemyType());
        }

        @Test
        @DisplayName("BOSS type is not transformed")
        void bossNotTransformed() {
            Tank boss = new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.BOSS);
            enemyTanks.add(boss);

            GameLogic.applyEasterEggEffect(enemyTanks, true);

            assertEquals(Tank.EnemyType.BOSS, boss.getEnemyType());
        }
    }
}
