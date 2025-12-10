package com.vibetanks.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameStateApplier Tests")
class GameStateApplierTest {

    @Nested
    @DisplayName("Interface Definition Tests")
    class InterfaceDefinitionTests {

        @Test
        @DisplayName("GameContext interface should exist")
        void gameContextInterfaceShouldExist() {
            assertNotNull(GameStateApplier.GameContext.class);
        }

        @Test
        @DisplayName("GameContext should declare player-related methods")
        void gameContextShouldDeclarePlayerRelatedMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerTanks"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerKills"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerScores"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerLevelScores"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerKillsByType"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerNicknames"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPlayerStartPositions"));
        }

        @Test
        @DisplayName("GameContext should declare game object methods")
        void gameContextShouldDeclareGameObjectMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getEnemyTanks"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getBullets"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getLasers"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPowerUps"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getGameMap"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getBase"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getEnemySpawner"));
        }

        @Test
        @DisplayName("GameContext should declare manager methods")
        void gameContextShouldDeclareManagerMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getCelebrationManager"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getUFOManager"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getPowerUpEffectManager"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getSoundManager"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getNetwork"));
        }

        @Test
        @DisplayName("GameContext should declare state tracking methods")
        void gameContextShouldDeclareStateTrackingMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("isFirstStateReceived"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setFirstStateReceived", boolean.class));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getSeenBulletIds"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getSeenLaserIds"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("getSeenBurningTileKeys"));
        }

        @Test
        @DisplayName("GameContext should declare game state flag methods")
        void gameContextShouldDeclareGameStateFlagMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("isGameOver"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setGameOver", boolean.class));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("isVictory"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setVictory", boolean.class));
        }

        @Test
        @DisplayName("GameContext should declare setter methods")
        void gameContextShouldDeclareSetterMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setNetworkConnectedPlayers", int.class));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setRespawnSyncFrames", int.class));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setBossKillerPlayerIndex", int.class));
        }

        @Test
        @DisplayName("GameContext should declare UI methods")
        void gameContextShouldDeclareUiMethods() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.GameContext.class.getMethod("hideVictoryImage"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("hideGameOverImage"));
            assertNotNull(GameStateApplier.GameContext.class.getMethod("setGameOverSoundPlayed", boolean.class));
        }
    }

    @Nested
    @DisplayName("Static Method Tests")
    class StaticMethodTests {

        @Test
        @DisplayName("apply method should exist")
        void applyMethodShouldExist() throws NoSuchMethodException {
            assertNotNull(GameStateApplier.class.getMethod("apply",
                GameState.class, GameStateApplier.GameContext.class));
        }

        @Test
        @DisplayName("apply method should be static")
        void applyMethodShouldBeStatic() throws NoSuchMethodException {
            var method = GameStateApplier.class.getMethod("apply",
                GameState.class, GameStateApplier.GameContext.class);
            assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
        }
    }

    @Nested
    @DisplayName("GameState Field Tests")
    class GameStateFieldTests {

        @Test
        @DisplayName("GameState should have player data")
        void gameStateShouldHavePlayerData() {
            GameState state = new GameState();
            assertNotNull(state.players);
            assertEquals(4, state.players.length);
        }

        @Test
        @DisplayName("GameState should have enemy list")
        void gameStateShouldHaveEnemyList() {
            GameState state = new GameState();
            assertNotNull(state.enemies);
        }

        @Test
        @DisplayName("GameState should have bullet list")
        void gameStateShouldHaveBulletList() {
            GameState state = new GameState();
            assertNotNull(state.bullets);
        }

        @Test
        @DisplayName("GameState should have laser list")
        void gameStateShouldHaveLaserList() {
            GameState state = new GameState();
            assertNotNull(state.lasers);
        }

        @Test
        @DisplayName("GameState should have power-up list")
        void gameStateShouldHavePowerUpList() {
            GameState state = new GameState();
            assertNotNull(state.powerUps);
        }

        @Test
        @DisplayName("GameState should have game flags")
        void gameStateShouldHaveGameFlags() {
            GameState state = new GameState();
            assertFalse(state.gameOver);
            assertFalse(state.victory);
        }

        @Test
        @DisplayName("GameState should have level number")
        void gameStateShouldHaveLevelNumber() {
            GameState state = new GameState();
            state.levelNumber = 5;
            assertEquals(5, state.levelNumber);
        }

        @Test
        @DisplayName("GameState should have base state")
        void gameStateShouldHaveBaseState() {
            GameState state = new GameState();
            state.baseAlive = true;
            assertTrue(state.baseAlive);
        }

        @Test
        @DisplayName("GameState should have connected players count")
        void gameStateShouldHaveConnectedPlayersCount() {
            GameState state = new GameState();
            state.connectedPlayers = 3;
            assertEquals(3, state.connectedPlayers);
        }

        @Test
        @DisplayName("GameState should have freeze durations")
        void gameStateShouldHaveFreezeDurations() {
            GameState state = new GameState();
            state.enemyFreezeDuration = 100;
            state.playerFreezeDuration = 50;
            assertEquals(100, state.enemyFreezeDuration);
            assertEquals(50, state.playerFreezeDuration);
        }

        @Test
        @DisplayName("GameState should have boss kill info")
        void gameStateShouldHaveBossKillInfo() {
            GameState state = new GameState();
            state.bossKillerPlayerIndex = 2;
            state.bossKillPowerUpReward = 3;
            assertEquals(2, state.bossKillerPlayerIndex);
            assertEquals(3, state.bossKillPowerUpReward);
        }

        @Test
        @DisplayName("GameState should have cat escape state")
        void gameStateShouldHaveCatEscapeState() {
            GameState state = new GameState();
            state.catEscaping = true;
            state.catEscapeX = 100;
            state.catEscapeY = 200;
            assertTrue(state.catEscaping);
            assertEquals(100, state.catEscapeX);
            assertEquals(200, state.catEscapeY);
        }

        @Test
        @DisplayName("GameState should have host settings")
        void gameStateShouldHaveHostSettings() {
            GameState state = new GameState();
            state.hostPlayerSpeed = 1.5;
            state.hostEnemySpeed = 0.8;
            assertEquals(1.5, state.hostPlayerSpeed);
            assertEquals(0.8, state.hostEnemySpeed);
        }
    }

    @Nested
    @DisplayName("PlayerData Tests")
    class PlayerDataTests {

        @Test
        @DisplayName("PlayerData should have position fields")
        void playerDataShouldHavePositionFields() {
            PlayerData data = new PlayerData();
            data.x = 100;
            data.y = 200;
            assertEquals(100, data.x);
            assertEquals(200, data.y);
        }

        @Test
        @DisplayName("PlayerData should have alive flag")
        void playerDataShouldHaveAliveFlag() {
            PlayerData data = new PlayerData();
            data.alive = true;
            assertTrue(data.alive);
        }

        @Test
        @DisplayName("PlayerData should have score fields")
        void playerDataShouldHaveScoreFields() {
            PlayerData data = new PlayerData();
            data.kills = 10;
            data.score = 500;
            data.levelScore = 100;
            assertEquals(10, data.kills);
            assertEquals(500, data.score);
            assertEquals(100, data.levelScore);
        }

        @Test
        @DisplayName("PlayerData should have nickname")
        void playerDataShouldHaveNickname() {
            PlayerData data = new PlayerData();
            data.nickname = "TestPlayer";
            assertEquals("TestPlayer", data.nickname);
        }
    }

    @Nested
    @DisplayName("EnemyData Tests")
    class EnemyDataTests {

        @Test
        @DisplayName("EnemyData constructor should set all fields")
        void enemyDataConstructorShouldSetAllFields() {
            GameState.EnemyData data = new GameState.EnemyData(
                100, 200, 1, true, 2, 3, 5, 0.5, 1.5
            );

            assertEquals(100, data.x);
            assertEquals(200, data.y);
            assertEquals(1, data.direction);
            assertTrue(data.alive);
            assertEquals(2, data.enemyType);
            assertEquals(3, data.health);
            assertEquals(5, data.maxHealth);
            assertEquals(0.5, data.tempSpeedBoost);
            assertEquals(1.5, data.speedMultiplier);
        }
    }

    @Nested
    @DisplayName("BulletData Tests")
    class BulletDataTests {

        @Test
        @DisplayName("BulletData constructor should set all fields")
        void bulletDataConstructorShouldSetAllFields() {
            GameState.BulletData data = new GameState.BulletData(
                123L, 100, 200, 0, false, 2, true, 1, 8
            );

            assertEquals(123L, data.id);
            assertEquals(100, data.x);
            assertEquals(200, data.y);
            assertEquals(0, data.direction);
            assertFalse(data.fromEnemy);
            assertEquals(2, data.power);
            assertTrue(data.canDestroyTrees);
            assertEquals(1, data.ownerPlayerNumber);
            assertEquals(8, data.size);
        }
    }

    @Nested
    @DisplayName("LaserData Tests")
    class LaserDataTests {

        @Test
        @DisplayName("LaserData constructor should set all fields")
        void laserDataConstructorShouldSetAllFields() {
            GameState.LaserData data = new GameState.LaserData(
                456L, 100, 200, 2, false, 1, 30, 400
            );

            assertEquals(456L, data.id);
            assertEquals(100, data.startX);
            assertEquals(200, data.startY);
            assertEquals(2, data.direction);
            assertFalse(data.fromEnemy);
            assertEquals(1, data.ownerPlayerNumber);
            assertEquals(30, data.lifetime);
            assertEquals(400, data.length);
        }
    }

    @Nested
    @DisplayName("PowerUpData Tests")
    class PowerUpDataTests {

        @Test
        @DisplayName("PowerUpData constructor should set all fields")
        void powerUpDataConstructorShouldSetAllFields() {
            GameState.PowerUpData data = new GameState.PowerUpData(
                789L, 300, 400, 1, 500
            );

            assertEquals(789L, data.id);
            assertEquals(300, data.x);
            assertEquals(400, data.y);
            assertEquals(1, data.type);
            assertEquals(500, data.lifetime);
        }
    }

    @Nested
    @DisplayName("SoundEvent Tests")
    class SoundEventTests {

        @Test
        @DisplayName("SoundEvent types should exist")
        void soundEventTypesShouldExist() {
            assertNotNull(GameState.SoundType.SHOOT);
            assertNotNull(GameState.SoundType.EXPLOSION);
            assertNotNull(GameState.SoundType.LASER);
            assertNotNull(GameState.SoundType.PLAYER_DEATH);
            assertNotNull(GameState.SoundType.BASE_DESTROYED);
            assertNotNull(GameState.SoundType.TREE_BURN);
            assertNotNull(GameState.SoundType.VICTORY);
        }

        @Test
        @DisplayName("SoundEvent should store type and player")
        void soundEventShouldStoreTypeAndPlayer() {
            GameState.SoundEvent event = new GameState.SoundEvent(GameState.SoundType.LASER, 2);

            assertEquals(GameState.SoundType.LASER, event.type);
            assertEquals(2, event.playerNumber);
        }
    }
}
