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

        @BeforeEach
        void setUp() {
            // Reset ID counter before each test for predictable IDs
            Laser.resetIdCounter();
        }

        @Test
        @DisplayName("First laser after reset should have ID 1")
        void firstLaserHasIdOne() {
            laser = new Laser(0, 0, Direction.UP, false, 1);
            assertEquals(1, laser.getId());
        }

        @Test
        @DisplayName("Sequential lasers should have incrementing IDs")
        void sequentialLasersHaveIncrementingIds() {
            Laser l1 = new Laser(0, 0, Direction.UP, false, 1);
            Laser l2 = new Laser(10, 20, Direction.DOWN, false, 2);
            Laser l3 = new Laser(30, 40, Direction.LEFT, true, 0);

            assertEquals(1, l1.getId());
            assertEquals(2, l2.getId());
            assertEquals(3, l3.getId());
        }

        @Test
        @DisplayName("Reset counter should start IDs from 1 again")
        void resetCounterStartsFromOne() {
            // Create some lasers
            new Laser(0, 0, Direction.UP, false, 1);
            new Laser(0, 0, Direction.DOWN, false, 2);
            new Laser(0, 0, Direction.LEFT, false, 3);

            // Reset and create new laser
            Laser.resetIdCounter();
            laser = new Laser(0, 0, Direction.RIGHT, false, 1);

            assertEquals(1, laser.getId());
        }

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
        @DisplayName("setId should not affect ID counter")
        void setIdDoesNotAffectCounter() {
            Laser l1 = new Laser(0, 0, Direction.UP, false, 1);
            assertEquals(1, l1.getId());

            // Manually set ID to a high value
            l1.setId(999L);

            // Next auto ID should continue from 2, not 1000
            Laser l2 = new Laser(0, 0, Direction.DOWN, false, 1);
            assertEquals(2, l2.getId());
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

        @Test
        @DisplayName("ID should be preserved even after expiry")
        void idPreservedAfterExpiry() {
            laser = new Laser(0, 0, Direction.UP, false, 1);
            long id = laser.getId();

            // Update past expiry
            for (int i = 0; i < 100; i++) {
                laser.update();
            }

            assertTrue(laser.isExpired());
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

    @Nested
    @DisplayName("Steel Blocking Tests")
    class SteelBlockingTests {

        private GameMap gameMap;

        @BeforeEach
        void setUp() {
            gameMap = new GameMap(26, 26);
            // Clear the map
            for (int row = 0; row < 26; row++) {
                for (int col = 0; col < 26; col++) {
                    gameMap.setTile(row, col, GameMap.TileType.EMPTY);
                }
            }
        }

        @Test
        @DisplayName("Laser should NOT hit base when steel blocks the path")
        void laserShouldNotHitBaseThroughSteel() {
            // Base at row 24 (y=768), col 12-13 (x=384)
            Base base = new Base(384, 768);
            assertTrue(base.isAlive());

            // Place steel tiles at row 23 (y=736) to block laser from above
            gameMap.setTile(23, 12, GameMap.TileType.STEEL);
            gameMap.setTile(23, 13, GameMap.TileType.STEEL);

            // Laser shooting DOWN from row 20 (y=640) toward base
            // Laser x = 400 (col 12.5) should pass through steel
            laser = new Laser(400, 640, Direction.DOWN, false, 1);

            // Without map, laser would hit base
            assertTrue(laser.collidesWithBase(base, null), "Without map, laser should hit base");

            // With map, steel should block the laser
            assertFalse(laser.collidesWithBase(base, gameMap), "Steel should block laser from hitting base");
        }

        @Test
        @DisplayName("Laser should hit base when no steel blocks the path")
        void laserShouldHitBaseWithoutSteelBlocking() {
            // Base at row 24 (y=768), col 12-13 (x=384)
            Base base = new Base(384, 768);

            // No steel protection - laser has clear path
            laser = new Laser(400, 640, Direction.DOWN, false, 1);

            assertTrue(laser.collidesWithBase(base, gameMap), "Laser should hit base without steel blocking");
        }

        @Test
        @DisplayName("Laser from LEFT should be blocked by steel")
        void laserFromLeftShouldBeBlockedBySteel() {
            // Base at (400, 400)
            Base base = new Base(400, 400);

            // Place steel at col 11 (x=352) to block laser from left
            gameMap.setTile(12, 11, GameMap.TileType.STEEL);
            gameMap.setTile(13, 11, GameMap.TileType.STEEL);

            // Laser shooting RIGHT from x=100 toward base at y=416 (row 13)
            laser = new Laser(100, 416, Direction.RIGHT, false, 1);

            assertFalse(laser.collidesWithBase(base, gameMap), "Steel should block laser from left");
        }

        @Test
        @DisplayName("Laser from RIGHT should be blocked by steel")
        void laserFromRightShouldBeBlockedBySteel() {
            // Base at (300, 400)
            Base base = new Base(300, 400);

            // Place steel at col 11 (x=352) to block laser from right
            gameMap.setTile(12, 11, GameMap.TileType.STEEL);
            gameMap.setTile(13, 11, GameMap.TileType.STEEL);

            // Laser shooting LEFT from x=500 toward base at y=416 (row 13)
            laser = new Laser(500, 416, Direction.LEFT, false, 1);

            assertFalse(laser.collidesWithBase(base, gameMap), "Steel should block laser from right");
        }

        @Test
        @DisplayName("Laser from below should be blocked by steel")
        void laserFromBelowShouldBeBlockedBySteel() {
            // Base at (400, 300)
            Base base = new Base(400, 300);

            // Place steel at row 11 (y=352) to block laser from below
            gameMap.setTile(11, 12, GameMap.TileType.STEEL);
            gameMap.setTile(11, 13, GameMap.TileType.STEEL);

            // Laser shooting UP from y=600 toward base at x=416 (col 13)
            laser = new Laser(416, 600, Direction.UP, false, 1);

            assertFalse(laser.collidesWithBase(base, gameMap), "Steel should block laser from below");
        }

        @Test
        @DisplayName("Brick tiles should NOT block laser to base")
        void brickTilesShouldNotBlockLaser() {
            // Base at row 24 (y=768)
            Base base = new Base(384, 768);

            // Place brick (not steel) tiles - laser passes through brick
            gameMap.setTile(23, 12, GameMap.TileType.BRICK);
            gameMap.setTile(23, 13, GameMap.TileType.BRICK);

            laser = new Laser(400, 640, Direction.DOWN, false, 1);

            assertTrue(laser.collidesWithBase(base, gameMap), "Brick should NOT block laser");
        }
    }
}
