package com.vibetanks.core;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.audio.SoundManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevelTransitionManager Tests")
class LevelTransitionManagerTest {

    private TestLevelTransitionContext context;

    @BeforeEach
    void setUp() {
        context = new TestLevelTransitionContext();
    }

    @Nested
    @DisplayName("Start Next Level Tests")
    class StartNextLevelTests {

        @Test
        @DisplayName("startNextLevel should increment level number")
        void startNextLevelShouldIncrementLevel() {
            int initialLevel = context.gameMap.getLevelNumber();

            LevelTransitionManager.startNextLevel(context);

            assertEquals(initialLevel + 1, context.gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("startNextLevel should reset victory flag")
        void startNextLevelShouldResetVictoryFlag() {
            context.victory = true;

            LevelTransitionManager.startNextLevel(context);

            assertFalse(context.victory);
        }

        @Test
        @DisplayName("startNextLevel should reset victoryConditionMet")
        void startNextLevelShouldResetVictoryConditionMet() {
            context.victoryConditionMet = true;

            LevelTransitionManager.startNextLevel(context);

            assertFalse(context.victoryConditionMet);
        }

        @Test
        @DisplayName("startNextLevel should reset game over flag")
        void startNextLevelShouldResetGameOverFlag() {
            context.gameOver = true;

            LevelTransitionManager.startNextLevel(context);

            assertFalse(context.gameOver);
        }

        @Test
        @DisplayName("startNextLevel should clear enemy tanks")
        void startNextLevelShouldClearEnemyTanks() {
            context.enemyTanks.add(new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR));

            LevelTransitionManager.startNextLevel(context);

            assertTrue(context.enemyTanks.isEmpty());
        }

        @Test
        @DisplayName("startNextLevel should clear bullets")
        void startNextLevelShouldClearBullets() {
            context.bullets.add(new Bullet(100, 100, Direction.UP, true, 1, false));

            LevelTransitionManager.startNextLevel(context);

            assertTrue(context.bullets.isEmpty());
        }

        @Test
        @DisplayName("startNextLevel should clear lasers")
        void startNextLevelShouldClearLasers() {
            context.lasers.add(new Laser(200, 200, Direction.UP, false, 1));

            LevelTransitionManager.startNextLevel(context);

            assertTrue(context.lasers.isEmpty());
        }

        @Test
        @DisplayName("startNextLevel should clear power-ups")
        void startNextLevelShouldClearPowerUps() {
            context.powerUps.add(new PowerUp(300, 300));

            LevelTransitionManager.startNextLevel(context);

            assertTrue(context.powerUps.isEmpty());
        }

        @Test
        @DisplayName("startNextLevel should reset kills but keep total scores")
        void startNextLevelShouldResetKillsButKeepTotalScores() {
            context.playerKills[0] = 10;
            context.playerScores[0] = 500;
            context.playerLevelScores[0] = 100;

            LevelTransitionManager.startNextLevel(context);

            assertEquals(0, context.playerKills[0]);
            assertEquals(500, context.playerScores[0]); // Total score preserved
            assertEquals(0, context.playerLevelScores[0]);
        }

        @Test
        @DisplayName("startNextLevel should give players temporary shield")
        void startNextLevelShouldGivePlayersTemporaryShield() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            context.playerTanks.add(player);

            LevelTransitionManager.startNextLevel(context);

            assertTrue(player.hasShield());
        }

        @Test
        @DisplayName("startNextLevel should reposition players")
        void startNextLevelShouldRepositionPlayers() {
            Tank player = new Tank(500, 500, Direction.DOWN, true, 1);
            context.playerTanks.add(player);

            LevelTransitionManager.startNextLevel(context);

            // Player should be at fixed position
            assertEquals(context.fixedPositions[0][0], player.getX());
            assertEquals(context.fixedPositions[0][1], player.getY());
        }

        @Test
        @DisplayName("startNextLevel should create new base")
        void startNextLevelShouldCreateNewBase() {
            context.base.destroy();

            LevelTransitionManager.startNextLevel(context);

            assertTrue(context.base.isAlive());
        }

        @Test
        @DisplayName("startNextLevel should create new enemy spawner")
        void startNextLevelShouldCreateNewEnemySpawner() {
            EnemySpawner oldSpawner = context.enemySpawner;

            LevelTransitionManager.startNextLevel(context);

            assertNotSame(oldSpawner, context.enemySpawner);
        }

        @Test
        @DisplayName("startNextLevel should reset UFO manager")
        void startNextLevelShouldResetUFOManager() {
            context.ufoManager.setUfoSpawnedThisLevel(true);

            LevelTransitionManager.startNextLevel(context);

            assertFalse(context.ufoManager.isUfoSpawnedThisLevel());
        }
    }

    @Nested
    @DisplayName("Restart Current Level Tests")
    class RestartCurrentLevelTests {

        @Test
        @DisplayName("restartCurrentLevel should keep same level number")
        void restartCurrentLevelShouldKeepSameLevelNumber() {
            context.gameMap.setLevelNumber(5);
            int levelNumber = context.gameMap.getLevelNumber();

            LevelTransitionManager.restartCurrentLevel(context);

            assertEquals(levelNumber, context.gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("restartCurrentLevel should reset all scores")
        void restartCurrentLevelShouldResetAllScores() {
            context.playerKills[0] = 10;
            context.playerScores[0] = 500;
            context.playerLevelScores[0] = 100;

            LevelTransitionManager.restartCurrentLevel(context);

            assertEquals(0, context.playerKills[0]);
            assertEquals(0, context.playerScores[0]); // Total score also reset
            assertEquals(0, context.playerLevelScores[0]);
        }

        @Test
        @DisplayName("restartCurrentLevel should reset player lives to 3")
        void restartCurrentLevelShouldResetPlayerLivesToThree() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            player.setLives(1);
            context.playerTanks.add(player);

            LevelTransitionManager.restartCurrentLevel(context);

            assertEquals(3, player.getLives());
        }

        @Test
        @DisplayName("restartCurrentLevel should clear player laser duration")
        void restartCurrentLevelShouldClearPlayerLaserDuration() {
            Tank player = new Tank(200, 700, Direction.UP, true, 1);
            player.setLaserDuration(100);
            context.playerTanks.add(player);

            LevelTransitionManager.restartCurrentLevel(context);

            assertEquals(0, player.getLaserDuration());
        }

        @Test
        @DisplayName("restartCurrentLevel should reset game over flag")
        void restartCurrentLevelShouldResetGameOverFlag() {
            context.gameOver = true;

            LevelTransitionManager.restartCurrentLevel(context);

            assertFalse(context.gameOver);
        }

        @Test
        @DisplayName("restartCurrentLevel should clear all projectiles")
        void restartCurrentLevelShouldClearAllProjectiles() {
            context.bullets.add(new Bullet(100, 100, Direction.UP, true, 1, false));
            context.lasers.add(new Laser(200, 200, Direction.UP, false, 1));

            LevelTransitionManager.restartCurrentLevel(context);

            assertTrue(context.bullets.isEmpty());
            assertTrue(context.lasers.isEmpty());
        }

        @Test
        @DisplayName("restartCurrentLevel should reset victory flags")
        void restartCurrentLevelShouldResetVictoryFlags() {
            context.victory = true;
            context.victoryConditionMet = true;

            LevelTransitionManager.restartCurrentLevel(context);

            assertFalse(context.victory);
            assertFalse(context.victoryConditionMet);
        }

        @Test
        @DisplayName("restartCurrentLevel should reset power-up effects")
        void restartCurrentLevelShouldResetPowerUpEffects() {
            context.powerUpEffectManager.setEnemyFreezeDuration(100);
            context.powerUpEffectManager.setBaseProtectionDuration(200);

            LevelTransitionManager.restartCurrentLevel(context);

            assertEquals(0, context.powerUpEffectManager.getEnemyFreezeDuration());
            assertEquals(0, context.powerUpEffectManager.getBaseProtectionDuration());
        }

        @Test
        @DisplayName("restartCurrentLevel should call hideGameOverImage")
        void restartCurrentLevelShouldCallHideGameOverImage() {
            context.gameOverImageHidden = false;

            LevelTransitionManager.restartCurrentLevel(context);

            assertTrue(context.gameOverImageHidden);
        }
    }

    @Nested
    @DisplayName("Multiple Player Tests")
    class MultiplePlayerTests {

        @Test
        @DisplayName("startNextLevel should reposition all players")
        void startNextLevelShouldRepositionAllPlayers() {
            Tank player1 = new Tank(100, 100, Direction.DOWN, true, 1);
            Tank player2 = new Tank(200, 200, Direction.DOWN, true, 2);
            context.playerTanks.add(player1);
            context.playerTanks.add(player2);

            LevelTransitionManager.startNextLevel(context);

            assertEquals(context.fixedPositions[0][0], player1.getX());
            assertEquals(context.fixedPositions[0][1], player1.getY());
            assertEquals(context.fixedPositions[1][0], player2.getX());
            assertEquals(context.fixedPositions[1][1], player2.getY());
        }

        @Test
        @DisplayName("restartCurrentLevel should reset all player lives")
        void restartCurrentLevelShouldResetAllPlayerLives() {
            Tank player1 = new Tank(100, 100, Direction.UP, true, 1);
            Tank player2 = new Tank(200, 200, Direction.UP, true, 2);
            player1.setLives(1);
            player2.setLives(0);
            context.playerTanks.add(player1);
            context.playerTanks.add(player2);

            LevelTransitionManager.restartCurrentLevel(context);

            assertEquals(3, player1.getLives());
            assertEquals(3, player2.getLives());
        }

        @Test
        @DisplayName("startNextLevel should reset kills for all players")
        void startNextLevelShouldResetKillsForAllPlayers() {
            context.playerKills[0] = 5;
            context.playerKills[1] = 10;
            context.playerKills[2] = 15;

            LevelTransitionManager.startNextLevel(context);

            assertEquals(0, context.playerKills[0]);
            assertEquals(0, context.playerKills[1]);
            assertEquals(0, context.playerKills[2]);
        }
    }

    /**
     * Test implementation of LevelTransitionContext
     */
    private static class TestLevelTransitionContext implements LevelTransitionManager.LevelTransitionContext {
        GameMap gameMap = new GameMap(26, 26);
        Base base = new Base(400, 700);
        EnemySpawner enemySpawner = new EnemySpawner(20, 5, gameMap);
        List<Tank> playerTanks = new ArrayList<>();
        List<Tank> enemyTanks = new ArrayList<>();
        List<Bullet> bullets = new ArrayList<>();
        List<Laser> lasers = new ArrayList<>();
        List<PowerUp> powerUps = new ArrayList<>();
        CelebrationManager celebrationManager = new CelebrationManager();
        UFOManager ufoManager = new UFOManager();
        PowerUpEffectManager powerUpEffectManager = new PowerUpEffectManager();
        SoundManager soundManager = new SoundManager();

        boolean victory = false;
        boolean victoryConditionMet = false;
        int victoryDelayTimer = 0;
        boolean gameOver = false;
        boolean gameOverSoundPlayed = false;
        boolean winnerBonusAwarded = false;
        int bossKillerPlayerIndex = -1;
        PowerUp.Type bossKillPowerUpReward = null;
        boolean victoryImageHidden = false;
        boolean gameOverImageHidden = false;

        int[] playerKills = new int[4];
        int[] playerScores = new int[4];
        int[] playerLevelScores = new int[4];
        int[][] playerKillsByType = new int[4][6];

        double[][] fixedPositions = {
            {280, 720}, {520, 720}, {280, 680}, {520, 680}
        };

        @Override
        public GameMap getGameMap() { return gameMap; }

        @Override
        public Base getBase() { return base; }

        @Override
        public void setBase(Base base) { this.base = base; }

        @Override
        public EnemySpawner getEnemySpawner() { return enemySpawner; }

        @Override
        public void setEnemySpawner(EnemySpawner spawner) { this.enemySpawner = spawner; }

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
        public CelebrationManager getCelebrationManager() { return celebrationManager; }

        @Override
        public UFOManager getUFOManager() { return ufoManager; }

        @Override
        public PowerUpEffectManager getPowerUpEffectManager() { return powerUpEffectManager; }

        @Override
        public SoundManager getSoundManager() { return soundManager; }

        @Override
        public void setVictory(boolean value) { this.victory = value; }

        @Override
        public void setVictoryConditionMet(boolean value) { this.victoryConditionMet = value; }

        @Override
        public void setVictoryDelayTimer(int value) { this.victoryDelayTimer = value; }

        @Override
        public void setGameOver(boolean value) { this.gameOver = value; }

        @Override
        public void setGameOverSoundPlayed(boolean value) { this.gameOverSoundPlayed = value; }

        @Override
        public void setWinnerBonusAwarded(boolean value) { this.winnerBonusAwarded = value; }

        @Override
        public void setBossKillerPlayerIndex(int index) { this.bossKillerPlayerIndex = index; }

        @Override
        public void setBossKillPowerUpReward(PowerUp.Type type) { this.bossKillPowerUpReward = type; }

        @Override
        public int[] getPlayerKills() { return playerKills; }

        @Override
        public int[] getPlayerScores() { return playerScores; }

        @Override
        public int[] getPlayerLevelScores() { return playerLevelScores; }

        @Override
        public int[][] getPlayerKillsByType() { return playerKillsByType; }

        @Override
        public void hideVictoryImage() { this.victoryImageHidden = true; }

        @Override
        public void hideGameOverImage() { this.gameOverImageHidden = true; }

        @Override
        public int getTotalEnemies() { return 20; }

        @Override
        public double[][] getFixedStartPositions() { return fixedPositions; }
    }
}
