package com.vibetanks.network;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameStateBuilder Tests")
class GameStateBuilderTest {

    private List<Tank> playerTanks;
    private List<Tank> enemyTanks;
    private List<Bullet> bullets;
    private List<Laser> lasers;
    private List<PowerUp> powerUps;
    private int[] playerKills;
    private int[] playerScores;
    private int[] playerLevelScores;
    private String[] playerNicknames;
    private int[][] playerKillsByType;
    private EnemySpawner enemySpawner;
    private GameMap gameMap;
    private Base base;
    private PowerUpEffectManager powerUpEffectManager;
    private CelebrationManager celebrationManager;
    private UFOManager ufoManager;

    @BeforeEach
    void setUp() {
        playerTanks = new ArrayList<>();
        enemyTanks = new ArrayList<>();
        bullets = new ArrayList<>();
        lasers = new ArrayList<>();
        powerUps = new ArrayList<>();
        playerKills = new int[4];
        playerScores = new int[4];
        playerLevelScores = new int[4];
        playerNicknames = new String[4];
        playerKillsByType = new int[4][6];
        gameMap = new GameMap(26, 26);
        enemySpawner = new EnemySpawner(20, 5, gameMap);
        base = new Base(400, 700);
        powerUpEffectManager = new PowerUpEffectManager();
        celebrationManager = new CelebrationManager();
        ufoManager = new UFOManager();
    }

    private GameState buildState() {
        return GameStateBuilder.build(
            playerTanks, playerKills, playerScores, playerLevelScores,
            playerNicknames, playerKillsByType, null, enemyTanks, bullets, lasers, powerUps,
            false, false, enemySpawner, gameMap, base, 1,
            powerUpEffectManager, -1, null, celebrationManager,
            new ArrayList<>(), ufoManager, new ArrayList<>()
        );
    }

    @Nested
    @DisplayName("Basic State Building Tests")
    class BasicStateBuildingTests {

        @Test
        @DisplayName("build should return non-null GameState")
        void buildShouldReturnNonNullGameState() {
            GameState state = buildState();
            assertNotNull(state);
        }

        @Test
        @DisplayName("build should set gameOver flag")
        void buildShouldSetGameOverFlag() {
            GameState state = GameStateBuilder.build(
                playerTanks, playerKills, playerScores, playerLevelScores,
                playerNicknames, playerKillsByType, null, enemyTanks, bullets, lasers, powerUps,
                true, false, enemySpawner, gameMap, base, 1,
                powerUpEffectManager, -1, null, celebrationManager,
                new ArrayList<>(), ufoManager, new ArrayList<>()
            );

            assertTrue(state.gameOver);
        }

        @Test
        @DisplayName("build should set victory flag")
        void buildShouldSetVictoryFlag() {
            GameState state = GameStateBuilder.build(
                playerTanks, playerKills, playerScores, playerLevelScores,
                playerNicknames, playerKillsByType, null, enemyTanks, bullets, lasers, powerUps,
                false, true, enemySpawner, gameMap, base, 1,
                powerUpEffectManager, -1, null, celebrationManager,
                new ArrayList<>(), ufoManager, new ArrayList<>()
            );

            assertTrue(state.victory);
        }

        @Test
        @DisplayName("build should set level number")
        void buildShouldSetLevelNumber() {
            gameMap.nextLevel();
            gameMap.nextLevel();

            GameState state = buildState();

            assertEquals(3, state.levelNumber);
        }

        @Test
        @DisplayName("build should set connected players")
        void buildShouldSetConnectedPlayers() {
            GameState state = GameStateBuilder.build(
                playerTanks, playerKills, playerScores, playerLevelScores,
                playerNicknames, playerKillsByType, null, enemyTanks, bullets, lasers, powerUps,
                false, false, enemySpawner, gameMap, base, 3,
                powerUpEffectManager, -1, null, celebrationManager,
                new ArrayList<>(), ufoManager, new ArrayList<>()
            );

            assertEquals(3, state.connectedPlayers);
        }
    }

    @Nested
    @DisplayName("Player Data Building Tests")
    class PlayerDataBuildingTests {

        @Test
        @DisplayName("build should include player tank data")
        void buildShouldIncludePlayerTankData() {
            Tank player = new Tank(100, 200, Direction.UP, true, 1);
            player.setLives(2);
            playerTanks.add(player);
            playerKills[0] = 5;
            playerScores[0] = 100;
            playerLevelScores[0] = 50;
            playerNicknames[0] = "TestPlayer";

            GameState state = buildState();

            assertEquals(100, state.players[0].x);
            assertEquals(200, state.players[0].y);
            assertEquals(5, state.players[0].kills);
            assertEquals(100, state.players[0].score);
            assertEquals(50, state.players[0].levelScore);
            assertEquals("TestPlayer", state.players[0].nickname);
        }

        @Test
        @DisplayName("build should handle multiple players")
        void buildShouldHandleMultiplePlayers() {
            playerTanks.add(new Tank(100, 200, Direction.UP, true, 1));
            playerTanks.add(new Tank(300, 400, Direction.DOWN, true, 2));
            playerNicknames[0] = "Player1";
            playerNicknames[1] = "Player2";

            GameState state = buildState();

            assertEquals(100, state.players[0].x);
            assertEquals(300, state.players[1].x);
        }

        @Test
        @DisplayName("build should limit to 4 players")
        void buildShouldLimitToFourPlayers() {
            for (int i = 0; i < 5; i++) {
                playerTanks.add(new Tank(i * 100, 200, Direction.UP, true, i + 1));
            }

            GameState state = buildState();

            // Should only have 4 players initialized
            assertEquals(4, state.players.length);
        }
    }

    @Nested
    @DisplayName("Enemy Data Building Tests")
    class EnemyDataBuildingTests {

        @Test
        @DisplayName("build should include enemy tank data")
        void buildShouldIncludeEnemyTankData() {
            Tank enemy = new Tank(500, 100, Direction.DOWN, false, 0, Tank.EnemyType.FAST);
            enemyTanks.add(enemy);

            GameState state = buildState();

            assertEquals(1, state.enemies.size());
            assertEquals(500, state.enemies.get(0).x);
            assertEquals(100, state.enemies.get(0).y);
            assertEquals(Tank.EnemyType.FAST.ordinal(), state.enemies.get(0).enemyType);
        }

        @Test
        @DisplayName("build should handle multiple enemies")
        void buildShouldHandleMultipleEnemies() {
            enemyTanks.add(new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR));
            enemyTanks.add(new Tank(200, 100, Direction.DOWN, false, 0, Tank.EnemyType.FAST));
            enemyTanks.add(new Tank(300, 100, Direction.DOWN, false, 0, Tank.EnemyType.HEAVY));

            GameState state = buildState();

            assertEquals(3, state.enemies.size());
        }

        @Test
        @DisplayName("build should skip null enemies")
        void buildShouldSkipNullEnemies() {
            enemyTanks.add(new Tank(100, 100, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR));
            enemyTanks.add(null);
            enemyTanks.add(new Tank(200, 100, Direction.DOWN, false, 0, Tank.EnemyType.FAST));

            GameState state = buildState();

            assertEquals(2, state.enemies.size());
        }
    }

    @Nested
    @DisplayName("Bullet Data Building Tests")
    class BulletDataBuildingTests {

        @Test
        @DisplayName("build should include bullet data")
        void buildShouldIncludeBulletData() {
            Bullet bullet = new Bullet(150, 250, Direction.UP, false, 1, true);
            bullets.add(bullet);

            GameState state = buildState();

            assertEquals(1, state.bullets.size());
            assertEquals(150, state.bullets.get(0).x);
            assertEquals(250, state.bullets.get(0).y);
            assertEquals(Direction.UP.ordinal(), state.bullets.get(0).direction);
            assertFalse(state.bullets.get(0).fromEnemy);
            assertTrue(state.bullets.get(0).canDestroyTrees);
        }

        @Test
        @DisplayName("build should skip null bullets")
        void buildShouldSkipNullBullets() {
            bullets.add(new Bullet(100, 100, Direction.UP, false, 1, false));
            bullets.add(null);

            GameState state = buildState();

            assertEquals(1, state.bullets.size());
        }
    }

    @Nested
    @DisplayName("Laser Data Building Tests")
    class LaserDataBuildingTests {

        @Test
        @DisplayName("build should include laser data")
        void buildShouldIncludeLaserData() {
            Laser laser = new Laser(200, 300, Direction.LEFT, false, 1);
            lasers.add(laser);

            GameState state = buildState();

            assertEquals(1, state.lasers.size());
            assertEquals(200, state.lasers.get(0).startX);
            assertEquals(300, state.lasers.get(0).startY);
            assertEquals(Direction.LEFT.ordinal(), state.lasers.get(0).direction);
        }

        @Test
        @DisplayName("build should skip null lasers")
        void buildShouldSkipNullLasers() {
            lasers.add(new Laser(100, 100, Direction.UP, false, 1));
            lasers.add(null);

            GameState state = buildState();

            assertEquals(1, state.lasers.size());
        }
    }

    @Nested
    @DisplayName("PowerUp Data Building Tests")
    class PowerUpDataBuildingTests {

        @Test
        @DisplayName("build should include power-up data")
        void buildShouldIncludePowerUpData() {
            PowerUp powerUp = new PowerUp(400, 500, PowerUp.Type.SHIELD);
            powerUps.add(powerUp);

            GameState state = buildState();

            assertEquals(1, state.powerUps.size());
            assertEquals(400, state.powerUps.get(0).x);
            assertEquals(500, state.powerUps.get(0).y);
            assertEquals(PowerUp.Type.SHIELD.ordinal(), state.powerUps.get(0).type);
        }

        @Test
        @DisplayName("build should skip null power-ups")
        void buildShouldSkipNullPowerUps() {
            powerUps.add(new PowerUp(100, 100));
            powerUps.add(null);

            GameState state = buildState();

            assertEquals(1, state.powerUps.size());
        }
    }

    @Nested
    @DisplayName("Base Data Building Tests")
    class BaseDataBuildingTests {

        @Test
        @DisplayName("build should include base alive state")
        void buildShouldIncludeBaseAliveState() {
            GameState state = buildState();
            assertTrue(state.baseAlive);
        }

        @Test
        @DisplayName("build should include base destroyed state")
        void buildShouldIncludeBaseDestroyedState() {
            base.destroy();

            GameState state = buildState();

            assertFalse(state.baseAlive);
        }

        @Test
        @DisplayName("build should include base flag state")
        void buildShouldIncludeBaseFlagState() {
            base.setFlagState(true, 50);

            GameState state = buildState();

            assertTrue(state.baseShowFlag);
            assertEquals(50, state.baseFlagHeight);
        }

        @Test
        @DisplayName("build should include cat mode")
        void buildShouldIncludeCatMode() {
            base.setCatMode(true);

            GameState state = buildState();

            assertTrue(state.baseCatMode);
        }
    }

    @Nested
    @DisplayName("Effect Manager Data Building Tests")
    class EffectManagerDataBuildingTests {

        @Test
        @DisplayName("build should include enemy freeze duration")
        void buildShouldIncludeEnemyFreezeDuration() {
            powerUpEffectManager.setEnemyFreezeDuration(100);

            GameState state = buildState();

            assertEquals(100, state.enemyFreezeDuration);
        }

        @Test
        @DisplayName("build should include player freeze duration")
        void buildShouldIncludePlayerFreezeDuration() {
            powerUpEffectManager.setPlayerFreezeDuration(50);

            GameState state = buildState();

            assertEquals(50, state.playerFreezeDuration);
        }

        @Test
        @DisplayName("build should include enemy speed boost duration")
        void buildShouldIncludeEnemySpeedBoostDuration() {
            powerUpEffectManager.setEnemyTeamSpeedBoostDuration(200);

            GameState state = buildState();

            assertEquals(200, state.enemyTeamSpeedBoostDuration);
        }
    }

    @Nested
    @DisplayName("Boss Kill Data Building Tests")
    class BossKillDataBuildingTests {

        @Test
        @DisplayName("build should include boss killer player index")
        void buildShouldIncludeBossKillerPlayerIndex() {
            GameState state = GameStateBuilder.build(
                playerTanks, playerKills, playerScores, playerLevelScores,
                playerNicknames, playerKillsByType, null, enemyTanks, bullets, lasers, powerUps,
                false, false, enemySpawner, gameMap, base, 1,
                powerUpEffectManager, 2, null, celebrationManager,
                new ArrayList<>(), ufoManager, new ArrayList<>()
            );

            assertEquals(2, state.bossKillerPlayerIndex);
        }

        @Test
        @DisplayName("build should include boss kill power-up reward")
        void buildShouldIncludeBossKillPowerUpReward() {
            GameState state = GameStateBuilder.build(
                playerTanks, playerKills, playerScores, playerLevelScores,
                playerNicknames, playerKillsByType, null, enemyTanks, bullets, lasers, powerUps,
                false, false, enemySpawner, gameMap, base, 1,
                powerUpEffectManager, 0, PowerUp.Type.TANK, celebrationManager,
                new ArrayList<>(), ufoManager, new ArrayList<>()
            );

            assertEquals(PowerUp.Type.TANK.ordinal(), state.bossKillPowerUpReward);
        }

        @Test
        @DisplayName("build should set -1 for no boss kill reward")
        void buildShouldSetMinusOneForNoBossKillReward() {
            GameState state = buildState();

            assertEquals(-1, state.bossKillPowerUpReward);
        }
    }

    @Nested
    @DisplayName("Remaining Enemies Tests")
    class RemainingEnemiesTests {

        @Test
        @DisplayName("build should include remaining enemies count")
        void buildShouldIncludeRemainingEnemiesCount() {
            enemySpawner.setRemainingEnemies(15);

            GameState state = buildState();

            assertEquals(15, state.remainingEnemies);
        }
    }

    @Nested
    @DisplayName("Host Settings Tests")
    class HostSettingsTests {

        @Test
        @DisplayName("build should include host player speed")
        void buildShouldIncludeHostPlayerSpeed() {
            GameState state = buildState();

            assertEquals(GameSettings.getPlayerSpeedMultiplier(), state.hostPlayerSpeed);
        }

        @Test
        @DisplayName("build should include host enemy speed")
        void buildShouldIncludeHostEnemySpeed() {
            GameState state = buildState();

            assertEquals(GameSettings.getEnemySpeedMultiplier(), state.hostEnemySpeed);
        }
    }
}
