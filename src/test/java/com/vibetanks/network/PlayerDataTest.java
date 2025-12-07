package com.vibetanks.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerData Tests")
class PlayerDataTest {

    private PlayerData playerData;

    @BeforeEach
    void setUp() {
        playerData = new PlayerData();
    }

    @Nested
    @DisplayName("Default Constructor Tests")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Default constructor should set lives to 3")
        void defaultConstructorSetsLivesToThree() {
            assertEquals(3, playerData.lives);
        }

        @Test
        @DisplayName("Default constructor should set alive to true")
        void defaultConstructorSetsAliveToTrue() {
            assertTrue(playerData.alive);
        }

        @Test
        @DisplayName("Default constructor should set direction to 0 (UP)")
        void defaultConstructorSetsDirectionToUp() {
            assertEquals(0, playerData.direction);
        }

        @Test
        @DisplayName("Default constructor should initialize killsByType array")
        void defaultConstructorInitializesKillsByType() {
            assertNotNull(playerData.killsByType);
            assertEquals(6, playerData.killsByType.length);
        }

        @Test
        @DisplayName("Default constructor should set all power-ups to false/zero")
        void defaultConstructorSetsPowerUpsToDefault() {
            assertFalse(playerData.hasShield);
            assertFalse(playerData.hasPauseShield);
            assertFalse(playerData.hasShip);
            assertFalse(playerData.hasGun);
            assertFalse(playerData.hasSaw);
            assertEquals(0, playerData.starCount);
            assertEquals(0, playerData.carCount);
            assertEquals(0, playerData.machinegunCount);
            assertEquals(0, playerData.laserDuration);
        }
    }

    @Nested
    @DisplayName("Player Number Constructor Tests")
    class PlayerNumberConstructorTests {

        @Test
        @DisplayName("Constructor with player number should set player number")
        void constructorSetsPlayerNumber() {
            playerData = new PlayerData(2);

            assertEquals(2, playerData.playerNumber);
        }

        @Test
        @DisplayName("Constructor with player number should inherit default values")
        void constructorInheritsDefaults() {
            playerData = new PlayerData(3);

            assertEquals(3, playerData.lives);
            assertTrue(playerData.alive);
            assertEquals(0, playerData.direction);
        }

        @Test
        @DisplayName("All player numbers 1-4 should be valid")
        void allPlayerNumbersValid() {
            for (int i = 1; i <= 4; i++) {
                PlayerData p = new PlayerData(i);
                assertEquals(i, p.playerNumber);
            }
        }
    }

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("getDisplayName with nickname should return nickname")
        void getDisplayNameWithNicknameReturnsNickname() {
            playerData = new PlayerData(1);
            playerData.nickname = "TestPlayer";

            assertEquals("TestPlayer", playerData.getDisplayName());
        }

        @Test
        @DisplayName("getDisplayName without nickname should return P+number")
        void getDisplayNameWithoutNicknameReturnsPNumber() {
            playerData = new PlayerData(2);

            assertEquals("P2", playerData.getDisplayName());
        }

        @Test
        @DisplayName("getDisplayName with null nickname should return P+number")
        void getDisplayNameWithNullNicknameReturnsPNumber() {
            playerData = new PlayerData(3);
            playerData.nickname = null;

            assertEquals("P3", playerData.getDisplayName());
        }

        @Test
        @DisplayName("getDisplayName with empty nickname should return P+number")
        void getDisplayNameWithEmptyNicknameReturnsPNumber() {
            playerData = new PlayerData(4);
            playerData.nickname = "";

            assertEquals("P4", playerData.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Display Lives Tests")
    class DisplayLivesTests {

        @Test
        @DisplayName("getDisplayLives should return lives minus 1")
        void getDisplayLivesReturnsLivesMinusOne() {
            playerData.lives = 3;

            assertEquals(2, playerData.getDisplayLives());
        }

        @Test
        @DisplayName("getDisplayLives with 1 life should return 0")
        void getDisplayLivesWithOneLiveReturnsZero() {
            playerData.lives = 1;

            assertEquals(0, playerData.getDisplayLives());
        }

        @Test
        @DisplayName("getDisplayLives with 0 lives should return 0")
        void getDisplayLivesWithZeroLivesReturnsZero() {
            playerData.lives = 0;

            assertEquals(0, playerData.getDisplayLives());
        }

        @Test
        @DisplayName("getDisplayLives should never return negative")
        void getDisplayLivesShouldNeverReturnNegative() {
            playerData.lives = -5;

            assertEquals(0, playerData.getDisplayLives());
        }
    }

    @Nested
    @DisplayName("Position and Movement Tests")
    class PositionAndMovementTests {

        @Test
        @DisplayName("Position fields should be settable")
        void positionFieldsSettable() {
            playerData.x = 100.5;
            playerData.y = 200.75;

            assertEquals(100.5, playerData.x);
            assertEquals(200.75, playerData.y);
        }

        @Test
        @DisplayName("Direction field should be settable")
        void directionFieldSettable() {
            playerData.direction = 3; // RIGHT

            assertEquals(3, playerData.direction);
        }

        @Test
        @DisplayName("Pending respawn position should be settable")
        void pendingRespawnPositionSettable() {
            playerData.pendingRespawnX = 150.0;
            playerData.pendingRespawnY = 250.0;
            playerData.respawnTimer = 120;

            assertEquals(150.0, playerData.pendingRespawnX);
            assertEquals(250.0, playerData.pendingRespawnY);
            assertEquals(120, playerData.respawnTimer);
        }
    }

    @Nested
    @DisplayName("Score and Kills Tests")
    class ScoreAndKillsTests {

        @Test
        @DisplayName("Score fields should be settable")
        void scoreFieldsSettable() {
            playerData.score = 1500;
            playerData.levelScore = 500;
            playerData.kills = 10;

            assertEquals(1500, playerData.score);
            assertEquals(500, playerData.levelScore);
            assertEquals(10, playerData.kills);
        }

        @Test
        @DisplayName("KillsByType should track kills per enemy type")
        void killsByTypeTracksKillsPerType() {
            playerData.killsByType[0] = 5; // REGULAR
            playerData.killsByType[1] = 3; // ARMORED
            playerData.killsByType[2] = 2; // FAST
            playerData.killsByType[3] = 4; // POWER
            playerData.killsByType[4] = 1; // HEAVY
            playerData.killsByType[5] = 1; // BOSS

            assertEquals(5, playerData.killsByType[0]);
            assertEquals(3, playerData.killsByType[1]);
            assertEquals(2, playerData.killsByType[2]);
            assertEquals(4, playerData.killsByType[3]);
            assertEquals(1, playerData.killsByType[4]);
            assertEquals(1, playerData.killsByType[5]);
        }
    }

    @Nested
    @DisplayName("Power-up Status Tests")
    class PowerUpStatusTests {

        @Test
        @DisplayName("Shield fields should be settable")
        void shieldFieldsSettable() {
            playerData.hasShield = true;
            playerData.shieldDuration = 180;
            playerData.hasPauseShield = true;

            assertTrue(playerData.hasShield);
            assertEquals(180, playerData.shieldDuration);
            assertTrue(playerData.hasPauseShield);
        }

        @Test
        @DisplayName("Movement power-ups should be settable")
        void movementPowerUpsSettable() {
            playerData.hasShip = true;
            playerData.hasSaw = true;
            playerData.carCount = 3;

            assertTrue(playerData.hasShip);
            assertTrue(playerData.hasSaw);
            assertEquals(3, playerData.carCount);
        }

        @Test
        @DisplayName("Weapon power-ups should be settable")
        void weaponPowerUpsSettable() {
            playerData.hasGun = true;
            playerData.starCount = 2;
            playerData.machinegunCount = 150;
            playerData.laserDuration = 300;

            assertTrue(playerData.hasGun);
            assertEquals(2, playerData.starCount);
            assertEquals(150, playerData.machinegunCount);
            assertEquals(300, playerData.laserDuration);
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("PlayerData should be serializable")
        void playerDataShouldBeSerializable() throws IOException, ClassNotFoundException {
            playerData = new PlayerData(2);
            playerData.x = 100;
            playerData.y = 200;
            playerData.lives = 2;
            playerData.kills = 5;
            playerData.nickname = "TestUser";
            playerData.hasShield = true;

            // Serialize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(playerData);
            oos.close();

            // Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            PlayerData deserialized = (PlayerData) ois.readObject();
            ois.close();

            assertEquals(2, deserialized.playerNumber);
            assertEquals(100, deserialized.x);
            assertEquals(200, deserialized.y);
            assertEquals(2, deserialized.lives);
            assertEquals(5, deserialized.kills);
            assertEquals("TestUser", deserialized.nickname);
            assertTrue(deserialized.hasShield);
        }

        @Test
        @DisplayName("KillsByType should be serialized correctly")
        void killsByTypeShouldSerialize() throws IOException, ClassNotFoundException {
            playerData.killsByType[0] = 10;
            playerData.killsByType[5] = 1;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(playerData);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            PlayerData deserialized = (PlayerData) ois.readObject();
            ois.close();

            assertEquals(10, deserialized.killsByType[0]);
            assertEquals(1, deserialized.killsByType[5]);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Large score values should work")
        void largeScoreValuesWork() {
            playerData.score = Integer.MAX_VALUE;

            assertEquals(Integer.MAX_VALUE, playerData.score);
        }

        @Test
        @DisplayName("Negative position values should work")
        void negativePositionValuesWork() {
            playerData.x = -100;
            playerData.y = -200;

            assertEquals(-100, playerData.x);
            assertEquals(-200, playerData.y);
        }

        @Test
        @DisplayName("All direction values 0-3 should be valid")
        void allDirectionValuesValid() {
            for (int dir = 0; dir < 4; dir++) {
                playerData.direction = dir;
                assertEquals(dir, playerData.direction);
            }
        }
    }
}
