package com.vibetanks.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerInput Tests")
class PlayerInputTest {

    private PlayerInput input;

    @Nested
    @DisplayName("Default Constructor Tests")
    class DefaultConstructorTests {

        @BeforeEach
        void setUp() {
            input = new PlayerInput();
        }

        @Test
        @DisplayName("All movement flags should be false by default")
        void allMovementFlagsFalseByDefault() {
            assertFalse(input.up);
            assertFalse(input.down);
            assertFalse(input.left);
            assertFalse(input.right);
        }

        @Test
        @DisplayName("Shoot flag should be false by default")
        void shootFlagFalseByDefault() {
            assertFalse(input.shoot);
        }

        @Test
        @DisplayName("Request flags should be false by default")
        void requestFlagsFalseByDefault() {
            assertFalse(input.requestLife);
            assertFalse(input.requestNextLevel);
            assertFalse(input.requestRestart);
        }

        @Test
        @DisplayName("Position should be zero by default")
        void positionZeroByDefault() {
            assertEquals(0, input.posX);
            assertEquals(0, input.posY);
        }

        @Test
        @DisplayName("Direction should be zero by default")
        void directionZeroByDefault() {
            assertEquals(0, input.direction);
        }

        @Test
        @DisplayName("Nickname should be null by default")
        void nicknameNullByDefault() {
            assertNull(input.nickname);
        }
    }

    @Nested
    @DisplayName("Movement Constructor Tests")
    class MovementConstructorTests {

        @Test
        @DisplayName("Constructor with movement flags should set them correctly")
        void constructorWithMovementFlags() {
            input = new PlayerInput(true, false, true, false, true);

            assertTrue(input.up);
            assertFalse(input.down);
            assertTrue(input.left);
            assertFalse(input.right);
            assertTrue(input.shoot);
        }

        @Test
        @DisplayName("Constructor should set requestLife to false")
        void constructorSetsRequestLifeToFalse() {
            input = new PlayerInput(true, true, true, true, true);

            assertFalse(input.requestLife);
        }

        @Test
        @DisplayName("Constructor should set position to zero")
        void constructorSetsPositionToZero() {
            input = new PlayerInput(true, true, true, true, true);

            assertEquals(0, input.posX);
            assertEquals(0, input.posY);
            assertEquals(0, input.direction);
        }
    }

    @Nested
    @DisplayName("Full Constructor Tests")
    class FullConstructorTests {

        @Test
        @DisplayName("Constructor with requestLife should set it correctly")
        void constructorWithRequestLife() {
            input = new PlayerInput(false, false, false, false, false, true);

            assertTrue(input.requestLife);
        }

        @Test
        @DisplayName("All false inputs should create input with all false flags")
        void allFalseInputs() {
            input = new PlayerInput(false, false, false, false, false, false);

            assertFalse(input.up);
            assertFalse(input.down);
            assertFalse(input.left);
            assertFalse(input.right);
            assertFalse(input.shoot);
            assertFalse(input.requestLife);
        }

        @Test
        @DisplayName("All true inputs should create input with all true flags")
        void allTrueInputs() {
            input = new PlayerInput(true, true, true, true, true, true);

            assertTrue(input.up);
            assertTrue(input.down);
            assertTrue(input.left);
            assertTrue(input.right);
            assertTrue(input.shoot);
            assertTrue(input.requestLife);
        }
    }

    @Nested
    @DisplayName("Field Modification Tests")
    class FieldModificationTests {

        @BeforeEach
        void setUp() {
            input = new PlayerInput();
        }

        @Test
        @DisplayName("Movement flags can be modified")
        void movementFlagsCanBeModified() {
            input.up = true;
            input.down = true;
            input.left = true;
            input.right = true;

            assertTrue(input.up);
            assertTrue(input.down);
            assertTrue(input.left);
            assertTrue(input.right);
        }

        @Test
        @DisplayName("Position can be modified")
        void positionCanBeModified() {
            input.posX = 100.5;
            input.posY = 200.75;
            input.direction = 2;

            assertEquals(100.5, input.posX);
            assertEquals(200.75, input.posY);
            assertEquals(2, input.direction);
        }

        @Test
        @DisplayName("Request flags can be modified")
        void requestFlagsCanBeModified() {
            input.requestLife = true;
            input.requestNextLevel = true;
            input.requestRestart = true;

            assertTrue(input.requestLife);
            assertTrue(input.requestNextLevel);
            assertTrue(input.requestRestart);
        }

        @Test
        @DisplayName("Nickname can be modified")
        void nicknameCanBeModified() {
            input.nickname = "Player1";

            assertEquals("Player1", input.nickname);
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("PlayerInput should be serializable")
        void playerInputShouldBeSerializable() {
            input = new PlayerInput(true, false, true, false, true, true);
            input.posX = 150.5;
            input.posY = 250.5;
            input.direction = 3;
            input.nickname = "TestPlayer";
            input.requestNextLevel = true;
            input.requestRestart = false;

            // Serialize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(input);
            } catch (IOException e) {
                fail("Serialization should not throw exception: " + e.getMessage());
            }

            // Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            PlayerInput deserialized = null;
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                deserialized = (PlayerInput) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                fail("Deserialization should not throw exception: " + e.getMessage());
            }

            // Verify
            assertNotNull(deserialized);
            assertEquals(input.up, deserialized.up);
            assertEquals(input.down, deserialized.down);
            assertEquals(input.left, deserialized.left);
            assertEquals(input.right, deserialized.right);
            assertEquals(input.shoot, deserialized.shoot);
            assertEquals(input.requestLife, deserialized.requestLife);
            assertEquals(input.requestNextLevel, deserialized.requestNextLevel);
            assertEquals(input.requestRestart, deserialized.requestRestart);
            assertEquals(input.posX, deserialized.posX);
            assertEquals(input.posY, deserialized.posY);
            assertEquals(input.direction, deserialized.direction);
            assertEquals(input.nickname, deserialized.nickname);
        }

        @Test
        @DisplayName("Default PlayerInput should serialize correctly")
        void defaultPlayerInputShouldSerializeCorrectly() throws Exception {
            input = new PlayerInput();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(input);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            PlayerInput deserialized = (PlayerInput) ois.readObject();
            ois.close();

            assertFalse(deserialized.up);
            assertFalse(deserialized.down);
            assertFalse(deserialized.left);
            assertFalse(deserialized.right);
            assertFalse(deserialized.shoot);
            assertEquals(0, deserialized.posX);
            assertEquals(0, deserialized.posY);
        }
    }

    @Nested
    @DisplayName("Movement Combination Tests")
    class MovementCombinationTests {

        @BeforeEach
        void setUp() {
            input = new PlayerInput();
        }

        @Test
        @DisplayName("Opposite directions can both be true (game handles this)")
        void oppositeDirectionsCanBothBeTrue() {
            input.up = true;
            input.down = true;

            assertTrue(input.up);
            assertTrue(input.down);
        }

        @Test
        @DisplayName("All directions can be true simultaneously")
        void allDirectionsCanBeTrueSimultaneously() {
            input.up = true;
            input.down = true;
            input.left = true;
            input.right = true;

            assertTrue(input.up);
            assertTrue(input.down);
            assertTrue(input.left);
            assertTrue(input.right);
        }

        @Test
        @DisplayName("Movement and shoot can be true simultaneously")
        void movementAndShootCanBeTrueSimultaneously() {
            input.up = true;
            input.shoot = true;

            assertTrue(input.up);
            assertTrue(input.shoot);
        }
    }

    @Nested
    @DisplayName("Position Value Tests")
    class PositionValueTests {

        @BeforeEach
        void setUp() {
            input = new PlayerInput();
        }

        @Test
        @DisplayName("Position can be negative")
        void positionCanBeNegative() {
            input.posX = -100;
            input.posY = -200;

            assertEquals(-100, input.posX);
            assertEquals(-200, input.posY);
        }

        @Test
        @DisplayName("Position can be very large")
        void positionCanBeVeryLarge() {
            input.posX = Double.MAX_VALUE;
            input.posY = Double.MAX_VALUE;

            assertEquals(Double.MAX_VALUE, input.posX);
            assertEquals(Double.MAX_VALUE, input.posY);
        }

        @Test
        @DisplayName("Direction ordinal can be set to any value")
        void directionOrdinalCanBeAnyValue() {
            input.direction = 0; // UP
            assertEquals(0, input.direction);

            input.direction = 1; // DOWN
            assertEquals(1, input.direction);

            input.direction = 2; // LEFT
            assertEquals(2, input.direction);

            input.direction = 3; // RIGHT
            assertEquals(3, input.direction);
        }
    }
}
