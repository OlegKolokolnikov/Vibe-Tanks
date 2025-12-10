package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EasterEgg Tests")
class EasterEggTest {

    private EasterEgg easterEgg;

    @BeforeEach
    void setUp() {
        easterEgg = new EasterEgg(100, 200);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should set position correctly")
        void constructorSetsPositionCorrectly() {
            assertEquals(100, easterEgg.getX());
            assertEquals(200, easterEgg.getY());
        }

        @Test
        @DisplayName("New easter egg should have lifetime")
        void newEasterEggShouldHaveLifetime() {
            assertTrue(easterEgg.getLifetime() > 0);
        }

        @Test
        @DisplayName("New easter egg should not be collected")
        void newEasterEggShouldNotBeCollected() {
            assertFalse(easterEgg.isCollected());
        }

        @Test
        @DisplayName("New easter egg should not be expired")
        void newEasterEggShouldNotBeExpired() {
            assertFalse(easterEgg.isExpired());
        }
    }

    @Nested
    @DisplayName("Lifetime Tests")
    class LifetimeTests {

        @Test
        @DisplayName("update should decrease lifetime")
        void updateShouldDecreaseLifetime() {
            int initialLifetime = easterEgg.getLifetime();

            easterEgg.update();

            assertEquals(initialLifetime - 1, easterEgg.getLifetime());
        }

        @Test
        @DisplayName("Easter egg should expire when lifetime reaches zero")
        void easterEggShouldExpireWhenLifetimeZero() {
            easterEgg.setLifetime(1);
            assertFalse(easterEgg.isExpired());

            easterEgg.update();

            assertTrue(easterEgg.isExpired());
        }

        @Test
        @DisplayName("setLifetime should update lifetime")
        void setLifetimeShouldUpdateLifetime() {
            easterEgg.setLifetime(500);
            assertEquals(500, easterEgg.getLifetime());
        }
    }

    @Nested
    @DisplayName("Collection Tests")
    class CollectionTests {

        @Test
        @DisplayName("collect should mark easter egg as collected")
        void collectShouldMarkAsCollected() {
            easterEgg.collect();

            assertTrue(easterEgg.isCollected());
        }

        @Test
        @DisplayName("Collected easter egg should be expired")
        void collectedEasterEggShouldBeExpired() {
            easterEgg.collect();

            assertTrue(easterEgg.isExpired());
        }
    }

    @Nested
    @DisplayName("Collision Tests")
    class CollisionTests {

        @Test
        @DisplayName("collidesWith should return true for overlapping tank")
        void collidesWithReturnsTrueForOverlappingTank() {
            Tank tank = new Tank(90, 190, Direction.UP, true, 1);

            assertTrue(easterEgg.collidesWith(tank));
        }

        @Test
        @DisplayName("collidesWith should return false for distant tank")
        void collidesWithReturnsFalseForDistantTank() {
            Tank tank = new Tank(500, 500, Direction.UP, true, 1);

            assertFalse(easterEgg.collidesWith(tank));
        }

        @Test
        @DisplayName("collidesWith should return true for tank at same position")
        void collidesWithReturnsTrueForSamePosition() {
            Tank tank = new Tank(100, 200, Direction.UP, true, 1);

            assertTrue(easterEgg.collidesWith(tank));
        }
    }

    @Nested
    @DisplayName("Position Tests")
    class PositionTests {

        @Test
        @DisplayName("setPosition should update both coordinates")
        void setPositionShouldUpdateBothCoordinates() {
            easterEgg.setPosition(300, 400);

            assertEquals(300, easterEgg.getX());
            assertEquals(400, easterEgg.getY());
        }
    }
}
