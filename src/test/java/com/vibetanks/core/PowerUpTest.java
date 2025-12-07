package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PowerUp Tests")
class PowerUpTest {

    private PowerUp powerUp;

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor with type should set all properties correctly")
        void constructorWithTypeSetsProperly() {
            powerUp = new PowerUp(100, 200, PowerUp.Type.GUN);

            assertEquals(100, powerUp.getX());
            assertEquals(200, powerUp.getY());
            assertEquals(PowerUp.Type.GUN, powerUp.getType());
            assertFalse(powerUp.isExpired());
        }

        @ParameterizedTest
        @EnumSource(PowerUp.Type.class)
        @DisplayName("Constructor should accept all power-up types")
        void constructorAcceptsAllTypes(PowerUp.Type type) {
            powerUp = new PowerUp(50, 75, type);

            assertEquals(type, powerUp.getType());
        }

        @Test
        @DisplayName("Random constructor should always create valid power-up")
        void randomConstructorCreatesValidPowerUp() {
            powerUp = new PowerUp(100, 200);

            assertNotNull(powerUp.getType());
            assertEquals(100, powerUp.getX());
            assertEquals(200, powerUp.getY());
            assertFalse(powerUp.isExpired());
        }

        @RepeatedTest(50)
        @DisplayName("Random constructor should generate variety of types")
        void randomConstructorGeneratesVariety() {
            // This is a statistical test - over 50 trials we should see at least 2 different types
            Set<PowerUp.Type> seenTypes = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                powerUp = new PowerUp(0, 0);
                seenTypes.add(powerUp.getType());
            }
            assertTrue(seenTypes.size() >= 2, "Should generate at least 2 different types");
        }
    }

    @Nested
    @DisplayName("Lifetime Tests")
    class LifetimeTests {

        @BeforeEach
        void setUp() {
            powerUp = new PowerUp(100, 100, PowerUp.Type.STAR);
        }

        @Test
        @DisplayName("New power-up should not be expired")
        void newPowerUpNotExpired() {
            assertFalse(powerUp.isExpired());
        }

        @Test
        @DisplayName("Update should decrease lifetime")
        void updateDecreasesLifetime() {
            assertFalse(powerUp.isExpired());

            // Update many times but not enough to expire (LIFETIME = 600)
            for (int i = 0; i < 599; i++) {
                powerUp.update();
            }

            assertFalse(powerUp.isExpired());
        }

        @Test
        @DisplayName("Power-up should expire after LIFETIME updates")
        void powerUpExpiresAfterLifetime() {
            // LIFETIME is 600 (10 seconds at 60 FPS)
            for (int i = 0; i < 600; i++) {
                powerUp.update();
            }

            assertTrue(powerUp.isExpired());
        }

        @Test
        @DisplayName("Power-up should expire exactly at lifetime boundary")
        void powerUpExpiresExactlyAtBoundary() {
            // Update 599 times - should not be expired
            for (int i = 0; i < 599; i++) {
                powerUp.update();
                assertFalse(powerUp.isExpired(), "Should not expire at update " + i);
            }

            // 600th update should make it expire
            powerUp.update();
            assertTrue(powerUp.isExpired(), "Should expire at update 600");
        }
    }

    @Nested
    @DisplayName("Type Tests")
    class TypeTests {

        @Test
        @DisplayName("All 12 power-up types should exist")
        void allTwelvePowerUpTypesExist() {
            assertEquals(12, PowerUp.Type.values().length);
        }

        @Test
        @DisplayName("All expected types should be present")
        void allExpectedTypesPresent() {
            Set<String> expectedTypes = Set.of(
                "GUN", "STAR", "CAR", "SHIP", "SHOVEL", "SAW",
                "TANK", "SHIELD", "MACHINEGUN", "FREEZE", "BOMB", "LASER"
            );

            Set<String> actualTypes = new HashSet<>();
            for (PowerUp.Type type : PowerUp.Type.values()) {
                actualTypes.add(type.name());
            }

            assertEquals(expectedTypes, actualTypes);
        }

        @Test
        @DisplayName("LASER should be the last type (for rarity logic)")
        void laserIsLastType() {
            PowerUp.Type[] types = PowerUp.Type.values();
            assertEquals(PowerUp.Type.LASER, types[types.length - 1]);
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("getX should return correct X coordinate")
        void getXReturnsCorrectValue() {
            powerUp = new PowerUp(123.5, 456.7, PowerUp.Type.CAR);
            assertEquals(123.5, powerUp.getX());
        }

        @Test
        @DisplayName("getY should return correct Y coordinate")
        void getYReturnsCorrectValue() {
            powerUp = new PowerUp(123.5, 456.7, PowerUp.Type.CAR);
            assertEquals(456.7, powerUp.getY());
        }

        @Test
        @DisplayName("getType should return correct type")
        void getTypeReturnsCorrectValue() {
            powerUp = new PowerUp(0, 0, PowerUp.Type.BOMB);
            assertEquals(PowerUp.Type.BOMB, powerUp.getType());
        }
    }

    @Nested
    @DisplayName("Position Tests")
    class PositionTests {

        @Test
        @DisplayName("Power-up can be created at origin")
        void powerUpAtOrigin() {
            powerUp = new PowerUp(0, 0, PowerUp.Type.STAR);

            assertEquals(0, powerUp.getX());
            assertEquals(0, powerUp.getY());
        }

        @Test
        @DisplayName("Power-up can be created at negative coordinates")
        void powerUpAtNegativeCoordinates() {
            powerUp = new PowerUp(-50, -100, PowerUp.Type.STAR);

            assertEquals(-50, powerUp.getX());
            assertEquals(-100, powerUp.getY());
        }

        @Test
        @DisplayName("Power-up can be created at large coordinates")
        void powerUpAtLargeCoordinates() {
            powerUp = new PowerUp(10000, 20000, PowerUp.Type.STAR);

            assertEquals(10000, powerUp.getX());
            assertEquals(20000, powerUp.getY());
        }
    }

    @Nested
    @DisplayName("Type Enum Tests")
    class TypeEnumTests {

        @Test
        @DisplayName("Type ordinals should be sequential starting from 0")
        void typeOrdinalsAreSequential() {
            PowerUp.Type[] types = PowerUp.Type.values();
            for (int i = 0; i < types.length; i++) {
                assertEquals(i, types[i].ordinal());
            }
        }

        @Test
        @DisplayName("GUN should be first type")
        void gunIsFirstType() {
            assertEquals(0, PowerUp.Type.GUN.ordinal());
        }

        @Test
        @DisplayName("Types can be retrieved by ordinal")
        void typesCanBeRetrievedByOrdinal() {
            assertEquals(PowerUp.Type.GUN, PowerUp.Type.values()[0]);
            assertEquals(PowerUp.Type.STAR, PowerUp.Type.values()[1]);
            assertEquals(PowerUp.Type.CAR, PowerUp.Type.values()[2]);
        }

        @Test
        @DisplayName("Type can be converted to string and back")
        void typeCanBeConvertedToStringAndBack() {
            for (PowerUp.Type type : PowerUp.Type.values()) {
                assertEquals(type, PowerUp.Type.valueOf(type.name()));
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Multiple updates past expiry should keep power-up expired")
        void multipleUpdatesPastExpiry() {
            powerUp = new PowerUp(0, 0, PowerUp.Type.TANK);

            for (int i = 0; i < 1000; i++) {
                powerUp.update();
            }

            assertTrue(powerUp.isExpired());
        }

        @Test
        @DisplayName("Power-up type should not change after creation")
        void typeDoesNotChange() {
            powerUp = new PowerUp(0, 0, PowerUp.Type.FREEZE);

            // Update many times
            for (int i = 0; i < 100; i++) {
                powerUp.update();
            }

            assertEquals(PowerUp.Type.FREEZE, powerUp.getType());
        }

        @Test
        @DisplayName("Position should not change after creation")
        void positionDoesNotChange() {
            powerUp = new PowerUp(150, 250, PowerUp.Type.SHIELD);

            // Update many times
            for (int i = 0; i < 100; i++) {
                powerUp.update();
            }

            assertEquals(150, powerUp.getX());
            assertEquals(250, powerUp.getY());
        }
    }
}
