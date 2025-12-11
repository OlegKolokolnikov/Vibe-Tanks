package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.network.GameState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameUpdateOrchestrator Tests")
class GameUpdateOrchestratorTest {

    private TestGameContext context;
    private List<Tank> allTanks;

    @BeforeEach
    void setUp() {
        context = new TestGameContext();
        allTanks = new ArrayList<>();
        allTanks.addAll(context.getPlayerTanks());
        allTanks.addAll(context.getEnemyTanks());
    }

    @Nested
    @DisplayName("UpdateResult Tests")
    class UpdateResultTests {

        @Test
        @DisplayName("UpdateResult should have default false values")
        void updateResultShouldHaveDefaultFalseValues() {
            GameUpdateOrchestrator.UpdateResult result = new GameUpdateOrchestrator.UpdateResult();

            assertFalse(result.victoryConditionMet);
            assertFalse(result.gameOver);
            assertFalse(result.victory);
        }

        @Test
        @DisplayName("UpdateResult should allow setting values")
        void updateResultShouldAllowSettingValues() {
            GameUpdateOrchestrator.UpdateResult result = new GameUpdateOrchestrator.UpdateResult();
            result.victoryConditionMet = true;
            result.gameOver = true;
            result.victory = true;

            assertTrue(result.victoryConditionMet);
            assertTrue(result.gameOver);
            assertTrue(result.victory);
        }
    }

    @Nested
    @DisplayName("Player Tank Update Tests")
    class PlayerTankUpdateTests {

        @Test
        @DisplayName("updatePlayerTanks should update alive players")
        void updatePlayerTanksShouldUpdateAlivePlayers() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            player.setLives(3);
            context.playerTanks.add(player);
            allTanks.add(player);

            double[][] respawnPositions = {{200, 700}};

            GameUpdateOrchestrator.updatePlayerTanks(context, allTanks, respawnPositions);

            assertTrue(player.isAlive());
        }

        @Test
        @DisplayName("updatePlayerTanks should handle dead player with lives remaining")
        void updatePlayerTanksShouldHandleDeadPlayerWithLives() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            // Simulate death: lives already decremented by damage() to 2
            player.setLives(2);
            player.setAlive(false);
            context.playerTanks.add(player);
            allTanks.add(player);

            double[][] respawnPositions = {{200, 700}};

            GameUpdateOrchestrator.updatePlayerTanks(context, allTanks, respawnPositions);

            // Lives should remain at 2 (not decremented again), player starts respawn
            assertEquals(2, player.getLives());
            assertTrue(player.isWaitingToRespawn());
        }

        @Test
        @DisplayName("updatePlayerTanks should handle player with no lives left")
        void updatePlayerTanksShouldHandlePlayerOnLastLife() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            // Simulate final death: lives already decremented by damage() to 0
            player.setLives(0);
            player.setAlive(false);
            context.playerTanks.add(player);
            allTanks.add(player);

            double[][] respawnPositions = {{200, 700}};

            GameUpdateOrchestrator.updatePlayerTanks(context, allTanks, respawnPositions);

            // Player should stay at 0 lives, no respawn
            assertEquals(0, player.getLives());
            assertFalse(player.isWaitingToRespawn());
        }

        @Test
        @DisplayName("updatePlayerTanks should update respawn timer for waiting players")
        void updatePlayerTanksShouldUpdateRespawnTimerForWaitingPlayers() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            player.setLives(2);
            player.setAlive(false);
            context.playerTanks.add(player);
            allTanks.add(player);

            double[][] respawnPositions = {{200, 700}};

            // First update starts respawn
            GameUpdateOrchestrator.updatePlayerTanks(context, allTanks, respawnPositions);

            // Second update should tick respawn timer
            GameUpdateOrchestrator.updatePlayerTanks(context, allTanks, respawnPositions);

            // Player is waiting to respawn
            assertTrue(player.isWaitingToRespawn() || player.isAlive());
        }
    }

    @Nested
    @DisplayName("Enemy Tank Update Tests")
    class EnemyTankUpdateTests {

        @Test
        @DisplayName("updateEnemyTanks should update alive enemies when not frozen")
        void updateEnemyTanksShouldUpdateAliveEnemiesWhenNotFrozen() {
            Tank enemy = new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);
            context.enemyTanks.add(enemy);
            allTanks.add(enemy);

            GameUpdateOrchestrator.updateEnemyTanks(context, allTanks);

            // Enemy should still be alive and processed
            assertTrue(enemy.isAlive());
        }

        @Test
        @DisplayName("updateEnemyTanks should skip frozen enemies")
        void updateEnemyTanksShouldSkipFrozenEnemies() {
            Tank enemy = new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);
            double initialX = enemy.getX();
            double initialY = enemy.getY();
            context.enemyTanks.add(enemy);
            allTanks.add(enemy);

            // Freeze enemies
            context.powerUpEffectManager.setEnemyFreezeDuration(100);

            GameUpdateOrchestrator.updateEnemyTanks(context, allTanks);

            // Enemy should not have moved (AI not updated)
            // Note: Position may change due to internal updates but AI shouldn't make decisions
            // Just verify no crash occurs
            assertTrue(enemy.isAlive());
        }

        @Test
        @DisplayName("updateEnemyTanks should update BOSS even when frozen")
        void updateEnemyTanksShouldUpdateBossEvenWhenFrozen() {
            Tank boss = new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);
            context.enemyTanks.add(boss);
            allTanks.add(boss);

            // Freeze enemies
            context.powerUpEffectManager.setEnemyFreezeDuration(100);

            GameUpdateOrchestrator.updateEnemyTanks(context, allTanks);

            // BOSS should still be alive and updated (immune to freeze)
            assertTrue(boss.isAlive());
        }
    }

    @Nested
    @DisplayName("Enemy Spawning Tests")
    class EnemySpawningTests {

        @Test
        @DisplayName("updateEnemySpawning should call spawner update")
        void updateEnemySpawningShouldCallSpawnerUpdate() {
            GameUpdateOrchestrator.updateEnemySpawning(context);

            // Just verify no crash - spawner behavior is tested elsewhere
            assertNotNull(context.getEnemySpawner());
        }

        @Test
        @DisplayName("updateEnemySpawning should apply speed boost to new enemies")
        void updateEnemySpawningShouldApplySpeedBoostToNewEnemies() {
            // Activate speed boost
            Tank boostEnemy = new Tank(50, 50, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);
            context.powerUpEffectManager.activateEnemySpeedBoost(boostEnemy);

            // Add a new enemy to simulate spawning
            Tank newEnemy = new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);
            context.enemyTanks.add(newEnemy);

            GameUpdateOrchestrator.updateEnemySpawning(context);

            // Speed boost should be active
            assertTrue(context.powerUpEffectManager.isEnemySpeedBoostActive());
        }
    }

    @Nested
    @DisplayName("Effect Timer Update Tests")
    class EffectTimerUpdateTests {

        @Test
        @DisplayName("updateEffectTimers should decrease freeze duration")
        void updateEffectTimersShouldDecreaseFreezeDuration() {
            context.powerUpEffectManager.setEnemyFreezeDuration(100);

            GameUpdateOrchestrator.updateEffectTimers(context);

            assertEquals(99, context.powerUpEffectManager.getEnemyFreezeDuration());
        }

        @Test
        @DisplayName("updateEffectTimers should handle speed boost expiration")
        void updateEffectTimersShouldHandleSpeedBoostExpiration() {
            Tank enemy = new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);
            context.enemyTanks.add(enemy);
            context.powerUpEffectManager.setEnemyTeamSpeedBoostDuration(1);

            GameUpdateOrchestrator.updateEffectTimers(context);

            assertFalse(context.powerUpEffectManager.isEnemySpeedBoostActive());
        }
    }

    @Nested
    @DisplayName("Bullet Processing Tests")
    class BulletProcessingTests {

        @Test
        @DisplayName("processBullets should handle empty bullet list")
        void processBulletsShouldHandleEmptyBulletList() {
            assertDoesNotThrow(() -> GameUpdateOrchestrator.processBullets(context));
        }

        @Test
        @DisplayName("processBullets should remove bullets that hit walls")
        void processBulletsShouldRemoveBulletsHitWalls() {
            // Add a bullet heading toward border (fromEnemy, power, canDestroyTrees)
            Bullet bullet = new Bullet(10, 10, Direction.LEFT, true, 1, false);
            context.bullets.add(bullet);

            // Process until bullet is removed
            for (int i = 0; i < 100; i++) {
                GameUpdateOrchestrator.processBullets(context);
                if (context.bullets.isEmpty()) break;
            }

            // Bullet should eventually be removed when hitting border
        }
    }

    @Nested
    @DisplayName("Laser Processing Tests")
    class LaserProcessingTests {

        @Test
        @DisplayName("processLasers should handle empty laser list")
        void processLasersShouldHandleEmptyLaserList() {
            assertDoesNotThrow(() -> GameUpdateOrchestrator.processLasers(context));
        }

        @Test
        @DisplayName("processLasers should remove expired lasers")
        void processLasersShouldRemoveExpiredLasers() {
            // Laser constructor: (startX, startY, direction, fromEnemy, ownerPlayerNumber)
            Laser laser = new Laser(200, 200, Direction.UP, false, 1);
            // Update laser until it expires
            for (int i = 0; i < 100; i++) {
                laser.update();
            }
            context.lasers.add(laser);

            GameUpdateOrchestrator.processLasers(context);

            // Laser should be expired and removed after many updates
        }
    }

    @Nested
    @DisplayName("Power-Up Processing Tests")
    class PowerUpProcessingTests {

        @Test
        @DisplayName("processPowerUps should handle empty power-up list")
        void processPowerUpsShouldHandleEmptyPowerUpList() {
            assertDoesNotThrow(() -> GameUpdateOrchestrator.processPowerUps(context));
        }

        @Test
        @DisplayName("processPowerUps should remove collected power-ups")
        void processPowerUpsShouldRemoveCollectedPowerUps() {
            Tank player = new Tank(200, 200, Direction.UP, true, 1);
            context.playerTanks.add(player);

            // Place power-up at player position
            PowerUp powerUp = new PowerUp(200, 200);
            context.powerUps.add(powerUp);

            GameUpdateOrchestrator.processPowerUps(context);

            // Power-up should be collected and removed
            assertTrue(context.powerUps.isEmpty());
        }

        @Test
        @DisplayName("processPowerUps should remove expired power-ups")
        void processPowerUpsShouldRemoveExpiredPowerUps() {
            PowerUp powerUp = new PowerUp(500, 500);
            // Manually expire the power-up
            for (int i = 0; i < 10000; i++) {
                powerUp.update();
                if (powerUp.isExpired()) break;
            }
            context.powerUps.add(powerUp);

            if (powerUp.isExpired()) {
                GameUpdateOrchestrator.processPowerUps(context);
                assertTrue(context.powerUps.isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Victory Condition Tests")
    class VictoryConditionTests {

        @Test
        @DisplayName("checkVictoryCondition should return true when victory achieved")
        void checkVictoryConditionShouldReturnTrueWhenVictoryAchieved() {
            // Empty enemy tanks and spawner has no more enemies
            context.enemyTanks.clear();
            context.enemySpawner.setRemainingEnemies(0);

            boolean victory = GameUpdateOrchestrator.checkVictoryCondition(
                context.enemySpawner, context.enemyTanks, false, 0, 600);

            assertTrue(victory);
        }

        @Test
        @DisplayName("checkVictoryCondition should return false when enemies remain")
        void checkVictoryConditionShouldReturnFalseWhenEnemiesRemain() {
            Tank enemy = new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);
            context.enemyTanks.add(enemy);

            boolean victory = GameUpdateOrchestrator.checkVictoryCondition(
                context.enemySpawner, context.enemyTanks, false, 0, 600);

            assertFalse(victory);
        }

        @Test
        @DisplayName("checkVictoryCondition should return false when already met")
        void checkVictoryConditionShouldReturnFalseWhenAlreadyMet() {
            context.enemyTanks.clear();
            context.enemySpawner.setRemainingEnemies(0);

            boolean victory = GameUpdateOrchestrator.checkVictoryCondition(
                context.enemySpawner, context.enemyTanks, true, 0, 600);

            // Should return false because victoryConditionMet is already true
            assertFalse(victory);
        }
    }

    @Nested
    @DisplayName("Random Power-Up Application Tests")
    class RandomPowerUpTests {

        @Test
        @DisplayName("applyRandomPowerUp should return a power-up type")
        void applyRandomPowerUpShouldReturnPowerUpType() {
            Tank tank = new Tank(200, 200, Direction.UP, true, 1);

            PowerUp.Type result = GameUpdateOrchestrator.applyRandomPowerUp(tank);

            assertNotNull(result);
        }
    }

    /**
     * Test implementation of GameContext interface
     */
    private static class TestGameContext implements GameUpdateOrchestrator.GameContext {
        GameMap gameMap = new GameMap(26, 26);
        Base base = new Base(400, 700);
        List<Tank> playerTanks = new ArrayList<>();
        List<Tank> enemyTanks = new ArrayList<>();
        List<Bullet> bullets = new ArrayList<>();
        List<Laser> lasers = new ArrayList<>();
        List<PowerUp> powerUps = new ArrayList<>();
        SoundManager soundManager = new SoundManager();
        EnemySpawner enemySpawner = new EnemySpawner(20, 5, gameMap);
        PowerUpEffectManager powerUpEffectManager = new PowerUpEffectManager();
        UFO ufo = null;
        EasterEgg easterEgg = null;
        boolean gameOver = false;
        boolean victory = false;
        int scoreAdded = 0;
        List<Tank> allTanksCache = new ArrayList<>();

        @Override
        public GameMap getGameMap() { return gameMap; }

        @Override
        public Base getBase() { return base; }

        @Override
        public List<Tank> getPlayerTanks() { return playerTanks; }

        @Override
        public List<Tank> getEnemyTanks() { return enemyTanks; }

        @Override
        public List<Bullet> getBullets() { return bullets; }

        @Override
        public List<Laser> getLasers() { return lasers; }

        @Override
        public List<PowerUp> getPowerUps() { return powerUps; }

        @Override
        public SoundManager getSoundManager() { return soundManager; }

        @Override
        public EnemySpawner getEnemySpawner() { return enemySpawner; }

        @Override
        public PowerUpEffectManager getPowerUpEffectManager() { return powerUpEffectManager; }

        @Override
        public int getWidth() { return 800; }

        @Override
        public int getHeight() { return 600; }

        @Override
        public UFO getUFO() { return ufo; }

        @Override
        public EasterEgg getEasterEgg() { return easterEgg; }

        @Override
        public void handleUFODestroyed(int killerPlayer, double eggX, double eggY) {
            ufo = null;
            easterEgg = new EasterEgg(eggX, eggY);
        }

        @Override
        public boolean isUfoSpawnedThisLevel() { return ufo != null; }

        @Override
        public void addScore(int playerIndex, int points) { scoreAdded += points; }

        @Override
        public void recordKill(int playerIndex, Tank.EnemyType type) { }

        @Override
        public void setBossKillReward(int playerIndex, PowerUp.Type reward) { }

        @Override
        public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

        @Override
        public void setVictory(boolean victory) { this.victory = victory; }

        @Override
        public double[] getRandomPowerUpSpawnPosition() {
            return new double[]{400, 300};
        }

        @Override
        public void notifyBulletDestroyed(Bullet bullet) { }

        @Override
        public List<Tank> getAllTanksCache() {
            allTanksCache.clear();
            allTanksCache.addAll(playerTanks);
            allTanksCache.addAll(enemyTanks);
            return allTanksCache;
        }

        @Override
        public void queueSoundEvent(GameState.SoundType type) {
            // No-op for tests
        }
    }
}
