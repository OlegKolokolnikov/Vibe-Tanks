package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Laser Tests")
class LaserTest {

    private Laser laser;

    // Play area constants (from Laser class)
    private static final double PLAY_AREA_WIDTH = 26 * 32; // 832 pixels
    private static final double PLAY_AREA_HEIGHT = 26 * 32; // 832 pixels

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should set all properties correctly")
        void constructorSetsAllProperties() {
            laser = new Laser(100, 200, Direction.UP, false, 1);

            assertEquals(100, laser.getStartX());
            assertEquals(200, laser.getStartY());
            assertEquals(Direction.UP, laser.getDirection());
            assertFalse(laser.isFromEnemy());
            assertEquals(1, laser.getOwnerPlayerNumber());
        }

        @Test
        @DisplayName("Constructor should auto-generate unique IDs")
        void constructorAutoGeneratesUniqueIds() {
            Laser laser1 = new Laser(0, 0, Direction.UP, false, 1);
            Laser laser2 = new Laser(0, 0, Direction.UP, false, 1);
            Laser laser3 = new Laser(0, 0, Direction.UP, false, 1);

            assertNotEquals(laser1.getId(), laser2.getId());
            assertNotEquals(laser2.getId(), laser3.getId());
            assertNotEquals(laser1.getId(), laser3.getId());
        }

        @Test
        @DisplayName("New laser should not be expired")
        void newLaserNotExpired() {
            laser = new Laser(100, 100, Direction.RIGHT, false, 1);

            assertFalse(laser.isExpired());
        }

        @Test
        @DisplayName("Enemy laser should be marked as from enemy")
        void enemyLaserMarkedCorrectly() {
            laser = new Laser(100, 100, Direction.DOWN, true, 0);

            assertTrue(laser.isFromEnemy());
            assertEquals(0, laser.getOwnerPlayerNumber());
        }
    }

    @Nested
    @DisplayName("Length Calculation Tests")
    class LengthCalculationTests {

        @Test
        @DisplayName("UP direction should calculate length to top edge")
        void upDirectionLengthToTopEdge() {
            laser = new Laser(400, 300, Direction.UP, false, 1);

            // Length should be distance from startY to top (0)
            assertEquals(300, laser.getLength());
        }

        @Test
        @DisplayName("DOWN direction should calculate length to bottom edge")
        void downDirectionLengthToBottomEdge() {
            laser = new Laser(400, 300, Direction.DOWN, false, 1);

            // Length should be distance from startY to bottom minus beam width
            double expectedLength = PLAY_AREA_HEIGHT - 300 - 12; // 12 is BEAM_WIDTH
            assertEquals(expectedLength, laser.getLength());
        }

        @Test
        @DisplayName("LEFT direction should calculate length to left edge")
        void leftDirectionLengthToLeftEdge() {
            laser = new Laser(400, 300, Direction.LEFT, false, 1);

            // Length should be distance from startX to left (0)
            assertEquals(400, laser.getLength());
        }

        @Test
        @DisplayName("RIGHT direction should calculate length to right edge")
        void rightDirectionLengthToRightEdge() {
            laser = new Laser(400, 300, Direction.RIGHT, false, 1);

            // Length should be distance from startX to right edge minus beam width
            double expectedLength = PLAY_AREA_WIDTH - 400 - 12; // 12 is BEAM_WIDTH
            assertEquals(expectedLength, laser.getLength());
        }

        @Test
        @DisplayName("Laser at top edge shooting UP should have zero length")
        void laserAtTopEdgeShootingUpHasZeroLength() {
            laser = new Laser(400, 0, Direction.UP, false, 1);

            assertEquals(0, laser.getLength());
        }

        @Test
        @DisplayName("Laser at left edge shooting LEFT should have zero length")
        void laserAtLeftEdgeShootingLeftHasZeroLength() {
            laser = new Laser(0, 300, Direction.LEFT, false, 1);

            assertEquals(0, laser.getLength());
        }

        @Test
        @DisplayName("Length should never be negative")
        void lengthShouldNeverBeNegative() {
            // Even with invalid coordinates, length should be clamped to 0
            laser = new Laser(-100, -100, Direction.UP, false, 1);

            assertTrue(laser.getLength() >= 0);
        }
    }

    @Nested
    @DisplayName("Lifetime Tests")
    class LifetimeTests {

        @BeforeEach
        void setUp() {
            laser = new Laser(100, 100, Direction.UP, false, 1);
        }

        @Test
        @DisplayName("Update should decrease lifetime")
        void updateDecreasesLifetime() {
            int initialLifetime = laser.getLifetime();
            laser.update();

            assertEquals(initialLifetime - 1, laser.getLifetime());
        }

        @Test
        @DisplayName("Laser should expire after LIFETIME updates")
        void laserExpiresAfterLifetime() {
            // LIFETIME is 15
            for (int i = 0; i < 15; i++) {
                assertFalse(laser.isExpired(), "Should not be expired at update " + i);
                laser.update();
            }

            assertTrue(laser.isExpired());
        }

        @Test
        @DisplayName("Multiple updates past expiry should keep laser expired")
        void multipleUpdatesPastExpiry() {
            for (int i = 0; i < 100; i++) {
                laser.update();
            }

            assertTrue(laser.isExpired());
        }
    }

    @Nested
    @DisplayName("ID Tests")
    class IdTests {

        @Test
        @DisplayName("setId should change laser ID")
        void setIdChangesId() {
            laser = new Laser(0, 0, Direction.UP, false, 1);
            long originalId = laser.getId();

            laser.setId(999L);

            assertEquals(999L, laser.getId());
            assertNotEquals(originalId, laser.getId());
        }

        @Test
        @DisplayName("ID should be preserved after updates")
        void idPreservedAfterUpdates() {
            laser = new Laser(0, 0, Direction.UP, false, 1);
            long id = laser.getId();

            for (int i = 0; i < 10; i++) {
                laser.update();
            }

            assertEquals(id, laser.getId());
        }
    }

    @Nested
    @DisplayName("Direction Tests")
    class DirectionTests {

        @Test
        @DisplayName("All directions should be supported")
        void allDirectionsSupported() {
            for (Direction dir : Direction.values()) {
                Laser dirLaser = new Laser(400, 400, dir, false, 1);

                assertEquals(dir, dirLaser.getDirection());
                assertTrue(dirLaser.getLength() >= 0);
            }
        }

        @Test
        @DisplayName("Direction should not change after creation")
        void directionDoesNotChange() {
            laser = new Laser(100, 100, Direction.LEFT, false, 1);

            for (int i = 0; i < 10; i++) {
                laser.update();
            }

            assertEquals(Direction.LEFT, laser.getDirection());
        }
    }

    @Nested
    @DisplayName("Position Tests")
    class PositionTests {

        @Test
        @DisplayName("Start position should not change after creation")
        void startPositionDoesNotChange() {
            laser = new Laser(150.5, 250.5, Direction.RIGHT, false, 1);

            for (int i = 0; i < 10; i++) {
                laser.update();
            }

            assertEquals(150.5, laser.getStartX());
            assertEquals(250.5, laser.getStartY());
        }

        @Test
        @DisplayName("Laser can be created at any position")
        void laserCanBeCreatedAtAnyPosition() {
            // Origin
            laser = new Laser(0, 0, Direction.DOWN, false, 1);
            assertEquals(0, laser.getStartX());
            assertEquals(0, laser.getStartY());

            // Center
            laser = new Laser(416, 416, Direction.UP, false, 1);
            assertEquals(416, laser.getStartX());
            assertEquals(416, laser.getStartY());

            // Edge
            laser = new Laser(831, 831, Direction.LEFT, false, 1);
            assertEquals(831, laser.getStartX());
            assertEquals(831, laser.getStartY());
        }
    }

    @Nested
    @DisplayName("Owner Tests")
    class OwnerTests {

        @Test
        @DisplayName("Player 1 laser should have owner 1")
        void player1LaserHasCorrectOwner() {
            laser = new Laser(100, 100, Direction.UP, false, 1);

            assertEquals(1, laser.getOwnerPlayerNumber());
            assertFalse(laser.isFromEnemy());
        }

        @Test
        @DisplayName("Player 2 laser should have owner 2")
        void player2LaserHasCorrectOwner() {
            laser = new Laser(100, 100, Direction.UP, false, 2);

            assertEquals(2, laser.getOwnerPlayerNumber());
        }

        @Test
        @DisplayName("Enemy laser should have owner 0")
        void enemyLaserHasZeroOwner() {
            laser = new Laser(100, 100, Direction.UP, true, 0);

            assertEquals(0, laser.getOwnerPlayerNumber());
            assertTrue(laser.isFromEnemy());
        }

        @Test
        @DisplayName("All player numbers 1-4 should be valid")
        void allPlayerNumbersValid() {
            for (int i = 1; i <= 4; i++) {
                laser = new Laser(100, 100, Direction.UP, false, i);
                assertEquals(i, laser.getOwnerPlayerNumber());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Laser at exact center should have correct length")
        void laserAtCenterHasCorrectLength() {
            double centerX = PLAY_AREA_WIDTH / 2;
            double centerY = PLAY_AREA_HEIGHT / 2;

            Laser upLaser = new Laser(centerX, centerY, Direction.UP, false, 1);
            assertEquals(centerY, upLaser.getLength());

            Laser leftLaser = new Laser(centerX, centerY, Direction.LEFT, false, 1);
            assertEquals(centerX, leftLaser.getLength());
        }

        @Test
        @DisplayName("Laser beyond play area should have zero or negative-clamped length")
        void laserBeyondPlayAreaHasClampedLength() {
            laser = new Laser(1000, 1000, Direction.RIGHT, false, 1);

            // Length should be clamped to at least 0
            assertTrue(laser.getLength() >= 0);
        }

        @Test
        @DisplayName("Lifetime should be accessible")
        void lifetimeIsAccessible() {
            laser = new Laser(100, 100, Direction.UP, false, 1);

            assertTrue(laser.getLifetime() > 0);
            assertEquals(15, laser.getLifetime()); // LIFETIME = 15
        }
    }
}
