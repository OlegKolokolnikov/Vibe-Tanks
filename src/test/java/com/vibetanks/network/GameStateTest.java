package com.vibetanks.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameState Tests")
class GameStateTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize 4 players")
        void constructorInitializesFourPlayers() {
            assertNotNull(gameState.players);
            assertEquals(4, gameState.players.length);
        }

        @Test
        @DisplayName("All player data should be initialized")
        void allPlayerDataInitialized() {
            for (int i = 0; i < 4; i++) {
                assertNotNull(gameState.players[i]);
                assertEquals(i + 1, gameState.players[i].playerNumber);
            }
        }

        @Test
        @DisplayName("Constructor should initialize empty lists")
        void constructorInitializesEmptyLists() {
            assertNotNull(gameState.enemies);
            assertTrue(gameState.enemies.isEmpty());

            assertNotNull(gameState.bullets);
            assertTrue(gameState.bullets.isEmpty());

            assertNotNull(gameState.lasers);
            assertTrue(gameState.lasers.isEmpty());

            assertNotNull(gameState.powerUps);
            assertTrue(gameState.powerUps.isEmpty());

            assertNotNull(gameState.tileChanges);
            assertTrue(gameState.tileChanges.isEmpty());

            assertNotNull(gameState.burningTiles);
            assertTrue(gameState.burningTiles.isEmpty());

            assertNotNull(gameState.dancingCharacters);
            assertTrue(gameState.dancingCharacters.isEmpty());

            assertNotNull(gameState.victoryDancingGirls);
            assertTrue(gameState.victoryDancingGirls.isEmpty());
        }

        @Test
        @DisplayName("Constructor should initialize default values")
        void constructorInitializesDefaults() {
            assertFalse(gameState.gameOver);
            assertFalse(gameState.victory);
            assertEquals(0, gameState.remainingEnemies);
            assertEquals(0, gameState.connectedPlayers);
            assertEquals(-1, gameState.bossKillerPlayerIndex);
            assertEquals(-1, gameState.bossKillPowerUpReward);
        }

        @Test
        @DisplayName("Constructor should initialize host settings to defaults")
        void constructorInitializesHostSettings() {
            assertEquals(1.0, gameState.hostPlayerSpeed);
            assertEquals(1.0, gameState.hostEnemySpeed);
            assertEquals(1.0, gameState.hostPlayerShootSpeed);
            assertEquals(1.0, gameState.hostEnemyShootSpeed);
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("GameState should be serializable")
        void gameStateShouldBeSerializable() throws IOException, ClassNotFoundException {
            gameState.gameOver = true;
            gameState.victory = true;
            gameState.remainingEnemies = 10;
            gameState.connectedPlayers = 3;
            gameState.levelNumber = 5;

            // Serialize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(gameState);
            oos.close();

            // Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameState deserialized = (GameState) ois.readObject();
            ois.close();

            assertEquals(gameState.gameOver, deserialized.gameOver);
            assertEquals(gameState.victory, deserialized.victory);
            assertEquals(gameState.remainingEnemies, deserialized.remainingEnemies);
            assertEquals(gameState.connectedPlayers, deserialized.connectedPlayers);
            assertEquals(gameState.levelNumber, deserialized.levelNumber);
        }

        @Test
        @DisplayName("EnemyData should be serializable")
        void enemyDataShouldBeSerializable() throws IOException, ClassNotFoundException {
            gameState.enemies.add(new GameState.EnemyData(100, 200, 1, true, 2, 3, 5, 1.2, 1.5, -1));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(gameState);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameState deserialized = (GameState) ois.readObject();
            ois.close();

            assertEquals(1, deserialized.enemies.size());
            GameState.EnemyData enemy = deserialized.enemies.get(0);
            assertEquals(100, enemy.x);
            assertEquals(200, enemy.y);
            assertEquals(1, enemy.direction);
            assertTrue(enemy.alive);
            assertEquals(2, enemy.enemyType);
            assertEquals(3, enemy.health);
            assertEquals(5, enemy.maxHealth);
        }

        @Test
        @DisplayName("BulletData should be serializable")
        void bulletDataShouldBeSerializable() throws IOException, ClassNotFoundException {
            gameState.bullets.add(new GameState.BulletData(123L, 50, 60, 2, false, 1, true, 1, 8, false));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(gameState);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameState deserialized = (GameState) ois.readObject();
            ois.close();

            assertEquals(1, deserialized.bullets.size());
            GameState.BulletData bullet = deserialized.bullets.get(0);
            assertEquals(123L, bullet.id);
            assertEquals(50, bullet.x);
            assertEquals(60, bullet.y);
            assertFalse(bullet.fromEnemy);
            assertTrue(bullet.canDestroyTrees);
            assertEquals(1, bullet.ownerPlayerNumber);
        }

        @Test
        @DisplayName("LaserData should be serializable")
        void laserDataShouldBeSerializable() throws IOException, ClassNotFoundException {
            gameState.lasers.add(new GameState.LaserData(456L, 100, 200, 0, true, 0, 15, 300.0));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(gameState);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameState deserialized = (GameState) ois.readObject();
            ois.close();

            assertEquals(1, deserialized.lasers.size());
            GameState.LaserData laser = deserialized.lasers.get(0);
            assertEquals(456L, laser.id);
            assertEquals(100, laser.startX);
            assertEquals(200, laser.startY);
            assertTrue(laser.fromEnemy);
            assertEquals(15, laser.lifetime);
            assertEquals(300.0, laser.length);
        }
    }

    @Nested
    @DisplayName("Inner Class Tests")
    class InnerClassTests {

        @Test
        @DisplayName("EnemyData constructor should set all fields")
        void enemyDataConstructorSetsFields() {
            GameState.EnemyData enemy = new GameState.EnemyData(
                    100, 200, 1, true, 2, 3, 5, 1.2, 1.5, 2
            );

            assertEquals(100, enemy.x);
            assertEquals(200, enemy.y);
            assertEquals(1, enemy.direction);
            assertTrue(enemy.alive);
            assertEquals(2, enemy.enemyType);
            assertEquals(3, enemy.health);
            assertEquals(5, enemy.maxHealth);
            assertEquals(1.2, enemy.tempSpeedBoost);
            assertEquals(1.5, enemy.speedMultiplier);
            assertEquals(2, enemy.colorOverrideIndex);
        }

        @Test
        @DisplayName("BulletData constructor should set all fields")
        void bulletDataConstructorSetsFields() {
            GameState.BulletData bullet = new GameState.BulletData(
                    123L, 50, 60, 2, false, 2, true, 1, 8, false
            );

            assertEquals(123L, bullet.id);
            assertEquals(50, bullet.x);
            assertEquals(60, bullet.y);
            assertEquals(2, bullet.direction);
            assertFalse(bullet.fromEnemy);
            assertEquals(2, bullet.power);
            assertTrue(bullet.canDestroyTrees);
            assertEquals(1, bullet.ownerPlayerNumber);
            assertEquals(8, bullet.size);
        }

        @Test
        @DisplayName("PowerUpData constructor should set all fields")
        void powerUpDataConstructorSetsFields() {
            GameState.PowerUpData powerUp = new GameState.PowerUpData(1, 100, 200, 3, 600);

            assertEquals(1, powerUp.id);
            assertEquals(100, powerUp.x);
            assertEquals(200, powerUp.y);
            assertEquals(3, powerUp.type);
            assertEquals(600, powerUp.lifetime);
        }

        @Test
        @DisplayName("TileChange constructor should set all fields")
        void tileChangeConstructorSetsFields() {
            GameState.TileChange change = new GameState.TileChange(5, 10, 2);

            assertEquals(5, change.row);
            assertEquals(10, change.col);
            assertEquals(2, change.tileType);
        }

        @Test
        @DisplayName("BurningTileData constructor should set all fields")
        void burningTileDataConstructorSetsFields() {
            GameState.BurningTileData burning = new GameState.BurningTileData(3, 4, 60);

            assertEquals(3, burning.row);
            assertEquals(4, burning.col);
            assertEquals(60, burning.framesRemaining);
        }

        @Test
        @DisplayName("UFOData constructor should set all fields")
        void ufoDataConstructorSetsFields() {
            GameState.UFOData ufo = new GameState.UFOData(100, 50, 2.0, 1.0, true, 5, 300, true);

            assertEquals(100, ufo.x);
            assertEquals(50, ufo.y);
            assertEquals(2.0, ufo.dx);
            assertEquals(1.0, ufo.dy);
            assertTrue(ufo.alive);
            assertEquals(5, ufo.health);
            assertEquals(300, ufo.lifetime);
            assertTrue(ufo.movingRight);
        }

        @Test
        @DisplayName("DancingCharacterData constructor should set all fields")
        void dancingCharacterDataConstructorSetsFields() {
            GameState.DancingCharacterData dancer = new GameState.DancingCharacterData(
                    100, 200, true, 5, 2, 3
            );

            assertEquals(100, dancer.x);
            assertEquals(200, dancer.y);
            assertTrue(dancer.isAlien);
            assertEquals(5, dancer.animFrame);
            assertEquals(2, dancer.danceStyle);
            assertEquals(3, dancer.colorIndex);
        }

        @Test
        @DisplayName("DancingGirlData constructor should set all fields")
        void dancingGirlDataConstructorSetsFields() {
            GameState.DancingGirlData girl = new GameState.DancingGirlData(
                    150, 250, 3, 1, 2, 4
            );

            assertEquals(150, girl.x);
            assertEquals(250, girl.y);
            assertEquals(3, girl.animFrame);
            assertEquals(1, girl.danceStyle);
            assertEquals(2, girl.dressColorIndex);
            assertEquals(4, girl.hairColorIndex);
        }

        @Test
        @DisplayName("EasterEggData constructor should set all fields")
        void easterEggDataConstructorSetsFields() {
            GameState.EasterEggData egg = new GameState.EasterEggData(200, 300, 100);

            assertEquals(200, egg.x);
            assertEquals(300, egg.y);
            assertEquals(100, egg.lifetime);
        }
    }

    @Nested
    @DisplayName("Base State Tests")
    class BaseStateTests {

        @Test
        @DisplayName("Base state fields should be modifiable")
        void baseStateFieldsModifiable() {
            gameState.baseAlive = false;
            gameState.baseShowFlag = true;
            gameState.baseFlagHeight = 25.5;
            gameState.baseShowVictoryFlag = true;
            gameState.baseVictoryFlagHeight = 30.0;
            gameState.baseCatMode = true;

            assertFalse(gameState.baseAlive);
            assertTrue(gameState.baseShowFlag);
            assertEquals(25.5, gameState.baseFlagHeight);
            assertTrue(gameState.baseShowVictoryFlag);
            assertEquals(30.0, gameState.baseVictoryFlagHeight);
            assertTrue(gameState.baseCatMode);
        }
    }

    @Nested
    @DisplayName("Freeze State Tests")
    class FreezeStateTests {

        @Test
        @DisplayName("Freeze durations should be modifiable")
        void freezeDurationsModifiable() {
            gameState.enemyFreezeDuration = 300;
            gameState.playerFreezeDuration = 150;

            assertEquals(300, gameState.enemyFreezeDuration);
            assertEquals(150, gameState.playerFreezeDuration);
        }

        @Test
        @DisplayName("Enemy team speed boost should be modifiable")
        void enemyTeamSpeedBoostModifiable() {
            gameState.enemyTeamSpeedBoostDuration = 200;

            assertEquals(200, gameState.enemyTeamSpeedBoostDuration);
        }
    }

    @Nested
    @DisplayName("Map State Tests")
    class MapStateTests {

        @Test
        @DisplayName("Map tiles can be set")
        void mapTilesCanBeSet() {
            int[][] tiles = new int[26][26];
            tiles[5][5] = 1;
            gameState.mapTiles = tiles;

            assertNotNull(gameState.mapTiles);
            assertEquals(1, gameState.mapTiles[5][5]);
        }
    }

    @Nested
    @DisplayName("Cat Escape State Tests")
    class CatEscapeStateTests {

        @Test
        @DisplayName("Cat escape fields should have default values")
        void catEscapeFieldsHaveDefaults() {
            assertFalse(gameState.catEscaping);
            assertEquals(0, gameState.catEscapeX);
            assertEquals(0, gameState.catEscapeY);
            assertEquals(0, gameState.catEscapeFrame);
            assertEquals(0, gameState.toyX);
            assertEquals(0, gameState.toyY);
            assertEquals(0, gameState.toyType);
        }

        @Test
        @DisplayName("Cat escape fields should be modifiable")
        void catEscapeFieldsModifiable() {
            gameState.catEscaping = true;
            gameState.catEscapeX = 150.5;
            gameState.catEscapeY = 250.75;
            gameState.catEscapeFrame = 45;
            gameState.toyX = 400.0;
            gameState.toyY = 180.0;
            gameState.toyType = 2;

            assertTrue(gameState.catEscaping);
            assertEquals(150.5, gameState.catEscapeX);
            assertEquals(250.75, gameState.catEscapeY);
            assertEquals(45, gameState.catEscapeFrame);
            assertEquals(400.0, gameState.toyX);
            assertEquals(180.0, gameState.toyY);
            assertEquals(2, gameState.toyType);
        }

        @Test
        @DisplayName("Cat escape state should be serializable")
        void catEscapeStateShouldBeSerializable() throws IOException, ClassNotFoundException {
            gameState.catEscaping = true;
            gameState.catEscapeX = 120.0;
            gameState.catEscapeY = 220.0;
            gameState.catEscapeFrame = 30;
            gameState.toyX = 350.0;
            gameState.toyY = 170.0;
            gameState.toyType = 1;

            // Serialize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(gameState);
            oos.close();

            // Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameState deserialized = (GameState) ois.readObject();
            ois.close();

            assertTrue(deserialized.catEscaping);
            assertEquals(120.0, deserialized.catEscapeX);
            assertEquals(220.0, deserialized.catEscapeY);
            assertEquals(30, deserialized.catEscapeFrame);
            assertEquals(350.0, deserialized.toyX);
            assertEquals(170.0, deserialized.toyY);
            assertEquals(1, deserialized.toyType);
        }

        @Test
        @DisplayName("Toy type should support all three types")
        void toyTypeSupportAllTypes() {
            for (int type = 0; type <= 2; type++) {
                gameState.toyType = type;
                assertEquals(type, gameState.toyType, "Toy type " + type + " should be settable");
            }
        }
    }

    @Nested
    @DisplayName("Sound Event Tests")
    class SoundEventTests {

        @Test
        @DisplayName("Sound events list should be initialized")
        void soundEventsListInitialized() {
            assertNotNull(gameState.soundEvents);
            assertTrue(gameState.soundEvents.isEmpty());
        }

        @Test
        @DisplayName("SoundEvent constructor with type only")
        void soundEventConstructorWithTypeOnly() {
            GameState.SoundEvent event = new GameState.SoundEvent(GameState.SoundType.VICTORY);

            assertEquals(GameState.SoundType.VICTORY, event.type);
            assertEquals(0, event.playerNumber);
        }

        @Test
        @DisplayName("SoundEvent constructor with type and player number")
        void soundEventConstructorWithTypeAndPlayer() {
            GameState.SoundEvent event = new GameState.SoundEvent(GameState.SoundType.PLAYER_DEATH, 2);

            assertEquals(GameState.SoundType.PLAYER_DEATH, event.type);
            assertEquals(2, event.playerNumber);
        }

        @Test
        @DisplayName("All SoundType values should exist")
        void allSoundTypesExist() {
            GameState.SoundType[] types = GameState.SoundType.values();
            assertTrue(types.length >= 7, "Should have at least 7 sound types");

            // Verify specific types exist
            assertNotNull(GameState.SoundType.SHOOT);
            assertNotNull(GameState.SoundType.EXPLOSION);
            assertNotNull(GameState.SoundType.PLAYER_DEATH);
            assertNotNull(GameState.SoundType.LASER);
            assertNotNull(GameState.SoundType.BASE_DESTROYED);
            assertNotNull(GameState.SoundType.TREE_BURN);
            assertNotNull(GameState.SoundType.VICTORY);
        }

        @Test
        @DisplayName("Sound events should be serializable")
        void soundEventsShouldBeSerializable() throws IOException, ClassNotFoundException {
            gameState.soundEvents.add(new GameState.SoundEvent(GameState.SoundType.VICTORY));
            gameState.soundEvents.add(new GameState.SoundEvent(GameState.SoundType.PLAYER_DEATH, 1));

            // Serialize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(gameState);
            oos.close();

            // Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameState deserialized = (GameState) ois.readObject();
            ois.close();

            assertEquals(2, deserialized.soundEvents.size());
            assertEquals(GameState.SoundType.VICTORY, deserialized.soundEvents.get(0).type);
            assertEquals(GameState.SoundType.PLAYER_DEATH, deserialized.soundEvents.get(1).type);
            assertEquals(1, deserialized.soundEvents.get(1).playerNumber);
        }
    }
}
