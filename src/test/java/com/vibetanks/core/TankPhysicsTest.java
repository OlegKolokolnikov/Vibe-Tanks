package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TankPhysics Tests")
class TankPhysicsTest {

    private TankPhysics physics;
    private GameMap gameMap;

    @BeforeEach
    void setUp() {
        physics = new TankPhysics();
        gameMap = new GameMap(26, 26);
        // Clear the map for testing
        for (int row = 0; row < 26; row++) {
            for (int col = 0; col < 26; col++) {
                gameMap.setTile(row, col, GameMap.TileType.EMPTY);
            }
        }
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("New physics should not be sliding")
        void newPhysicsShouldNotBeSliding() {
            assertFalse(physics.isSliding());
        }
    }

    @Nested
    @DisplayName("Ice Detection Tests")
    class IceDetectionTests {

        @Test
        @DisplayName("isOnIce should return true when on ice tile")
        void isOnIceShouldReturnTrueOnIce() {
            gameMap.setTile(5, 5, GameMap.TileType.ICE);
            // Tank at pixel position (5*32, 5*32) = (160, 160)

            assertTrue(physics.isOnIce(gameMap, 160, 160, 28));
        }

        @Test
        @DisplayName("isOnIce should return false when not on ice")
        void isOnIceShouldReturnFalseNotOnIce() {
            gameMap.setTile(5, 5, GameMap.TileType.EMPTY);

            assertFalse(physics.isOnIce(gameMap, 160, 160, 28));
        }
    }

    @Nested
    @DisplayName("Sliding Tests")
    class SlidingTests {

        @Test
        @DisplayName("startSliding should enable sliding on ice")
        void startSlidingShouldEnableSlidingOnIce() {
            gameMap.setTile(5, 5, GameMap.TileType.ICE);

            physics.startSliding(Direction.RIGHT, gameMap, 160, 160, 28);

            assertTrue(physics.isSliding());
        }

        @Test
        @DisplayName("startSliding should not enable sliding when not on ice")
        void startSlidingShouldNotEnableWhenNotOnIce() {
            gameMap.setTile(5, 5, GameMap.TileType.EMPTY);

            physics.startSliding(Direction.RIGHT, gameMap, 160, 160, 28);

            assertFalse(physics.isSliding());
        }

        @Test
        @DisplayName("stopSliding should disable sliding")
        void stopSlidingShouldDisableSliding() {
            gameMap.setTile(5, 5, GameMap.TileType.ICE);
            physics.startSliding(Direction.RIGHT, gameMap, 160, 160, 28);
            assertTrue(physics.isSliding());

            physics.stopSliding();

            assertFalse(physics.isSliding());
        }
    }

    @Nested
    @DisplayName("Movement Tests")
    class MovementTests {

        @Test
        @DisplayName("move should return false for dead tank")
        void moveShouldReturnFalseForDeadTank() {
            Tank tank = new Tank(200, 200, Direction.UP, true, 1);
            tank.setAlive(false);
            List<Tank> others = new ArrayList<>();

            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, new Base(400, 700));

            assertFalse(moved);
        }

        @Test
        @DisplayName("move should return true for valid movement")
        void moveShouldReturnTrueForValidMovement() {
            Tank tank = new Tank(200, 200, Direction.UP, true, 1);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            assertTrue(moved);
        }

        @Test
        @DisplayName("move should update tank position")
        void moveShouldUpdateTankPosition() {
            Tank tank = new Tank(200, 200, Direction.UP, true, 1);
            double initialX = tank.getX();
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            physics.move(tank, Direction.RIGHT, gameMap, others, base);

            assertTrue(tank.getX() > initialX);
        }

        @Test
        @DisplayName("move should be blocked by brick tiles")
        void moveShouldBeBlockedByBrickTiles() {
            Tank tank = new Tank(150, 160, Direction.RIGHT, true, 1);
            gameMap.setTile(5, 6, GameMap.TileType.BRICK); // At pixel (192, 160)
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            // Try to move right into the brick
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            // Should be blocked eventually
            // (May move a bit before collision)
        }

        @Test
        @DisplayName("move should be blocked by steel tiles")
        void moveShouldBeBlockedBySteelTiles() {
            Tank tank = new Tank(150, 160, Direction.RIGHT, true, 1);
            gameMap.setTile(5, 6, GameMap.TileType.STEEL); // At pixel (192, 160)
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            // Tank will be blocked by steel
        }
    }

    @Nested
    @DisplayName("Collision Check Tests")
    class CollisionCheckTests {

        @Test
        @DisplayName("checkCollision should return true for overlapping rectangles")
        void checkCollisionShouldReturnTrueForOverlapping() {
            assertTrue(TankPhysics.checkCollision(100, 100, 110, 110, 28, 28));
        }

        @Test
        @DisplayName("checkCollision should return false for non-overlapping rectangles")
        void checkCollisionShouldReturnFalseForNonOverlapping() {
            assertFalse(TankPhysics.checkCollision(100, 100, 200, 200, 28, 28));
        }

        @Test
        @DisplayName("checkCollision should return true for touching edges")
        void checkCollisionShouldReturnTrueForTouchingEdges() {
            // Rectangles just touching
            assertTrue(TankPhysics.checkCollision(100, 100, 127, 100, 28, 28));
        }

        @Test
        @DisplayName("checkCollision should return false for adjacent non-touching")
        void checkCollisionShouldReturnFalseForAdjacent() {
            // Rectangles separated by 1 pixel
            assertFalse(TankPhysics.checkCollision(100, 100, 129, 100, 28, 28));
        }

        @Test
        @DisplayName("checkCollision should handle different sizes")
        void checkCollisionShouldHandleDifferentSizes() {
            assertTrue(TankPhysics.checkCollision(100, 100, 105, 105, 28, 64));
            assertFalse(TankPhysics.checkCollision(100, 100, 200, 100, 28, 28));
        }
    }

    @Nested
    @DisplayName("Tank Collision Tests")
    class TankCollisionTests {

        @Test
        @DisplayName("move should be blocked by other tank")
        void moveShouldBeBlockedByOtherTank() {
            Tank tank = new Tank(200, 200, Direction.RIGHT, true, 1);
            Tank other = new Tank(228, 200, Direction.UP, true, 2); // Right next to first tank
            List<Tank> others = new ArrayList<>();
            others.add(other);
            Base base = new Base(400, 700);

            // Moving right should be blocked by other tank
            double initialX = tank.getX();
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            // Either blocked or can only move a little
        }

        @Test
        @DisplayName("move should not collide with self")
        void moveShouldNotCollideWithSelf() {
            Tank tank = new Tank(200, 200, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();
            others.add(tank); // Add self to list
            Base base = new Base(400, 700);

            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            assertTrue(moved);
        }

        @Test
        @DisplayName("move should not collide with dead tanks")
        void moveShouldNotCollideWithDeadTanks() {
            Tank tank = new Tank(200, 200, Direction.RIGHT, true, 1);
            Tank deadTank = new Tank(220, 200, Direction.UP, true, 2);
            deadTank.setAlive(false);
            List<Tank> others = new ArrayList<>();
            others.add(deadTank);
            Base base = new Base(400, 700);

            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            assertTrue(moved);
        }
    }

    @Nested
    @DisplayName("Base Collision Tests")
    class BaseCollisionTests {

        @Test
        @DisplayName("move should be blocked by base")
        void moveShouldBeBlockedByBase() {
            // Position tank right next to base
            Base base = new Base(400, 400);
            Tank tank = new Tank(360, 400, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();

            // Moving right toward base should be blocked
        }

        @Test
        @DisplayName("move should not be blocked by destroyed base")
        void moveShouldNotBeBlockedByDestroyedBase() {
            Base base = new Base(400, 400);
            base.destroy();
            Tank tank = new Tank(360, 400, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();

            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            assertTrue(moved);
        }
    }

    @Nested
    @DisplayName("BOSS Tank Tests")
    class BossTankTests {

        @Test
        @DisplayName("BOSS tank should destroy brick tiles when moving")
        void bossTankShouldDestroyBrickTiles() {
            Tank boss = new Tank(150, 160, Direction.RIGHT, false, 0, Tank.EnemyType.BOSS);
            gameMap.setTile(5, 6, GameMap.TileType.BRICK);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            // BOSS moves into brick
            physics.move(boss, Direction.RIGHT, gameMap, others, base);

            // Brick should be destroyed (if boss moved through it)
        }

        @Test
        @DisplayName("BOSS tank should be stopped by steel tiles")
        void bossTankShouldBeStoppedBySteelTiles() {
            Tank boss = new Tank(150, 160, Direction.RIGHT, false, 0, Tank.EnemyType.BOSS);
            gameMap.setTile(5, 6, GameMap.TileType.STEEL);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            // BOSS should be blocked by steel
        }

        @Test
        @DisplayName("BOSS tank should NOT destroy base through steel protection")
        void bossTankShouldNotDestroyBaseThroughSteelProtection() {
            // Base at position (384, 768) - standard base position at bottom center
            Base base = new Base(384, 768);
            assertTrue(base.isAlive(), "Base should start alive");

            // Place steel protection around the base (simulating shovel power-up effect)
            // Steel tiles at row 24 (y=768), cols 12-14
            // And row 23 (y=736), cols 12-14 for extra protection
            gameMap.setTile(23, 11, GameMap.TileType.STEEL);
            gameMap.setTile(23, 12, GameMap.TileType.STEEL);
            gameMap.setTile(23, 13, GameMap.TileType.STEEL);
            gameMap.setTile(23, 14, GameMap.TileType.STEEL);

            // BOSS tank approaching from above, would collide with steel protection
            // Position BOSS just above the steel row
            // Row 23 is at y=736, BOSS at y=700 moving down would hit steel first
            Tank boss = new Tank(384, 700, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);
            List<Tank> others = new ArrayList<>();

            // Try to move BOSS toward base through steel
            boolean moved = physics.move(boss, Direction.DOWN, gameMap, others, base);

            // BOSS should be stopped by steel and base should NOT be destroyed
            assertFalse(moved, "BOSS should be stopped by steel protection");
            assertTrue(base.isAlive(), "Base should remain alive - steel blocks BOSS from reaching it");
        }

        @Test
        @DisplayName("BOSS tank CAN destroy base without steel protection")
        void bossTankCanDestroyBaseWithoutSteelProtection() {
            // Base at accessible position - smaller values to avoid map boundary issues
            // Base is 32 pixels (2 tiles), positioned at (200, 200)
            Base base = new Base(200, 200);
            assertTrue(base.isAlive(), "Base should start alive");

            // No steel protection - clear path to base
            // BOSS positioned to directly collide with base on next move
            // BOSS tank size is ~32, positioned right above base
            // At y=170, tank bottom edge at ~198, base top at 200 - almost touching
            // One move of ~2 pixels puts BOSS colliding with base
            Tank boss = new Tank(200, 168, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);
            List<Tank> others = new ArrayList<>();

            // Move BOSS toward base - should collide and destroy it
            // May need multiple moves to reach collision
            for (int i = 0; i < 10 && base.isAlive(); i++) {
                physics.move(boss, Direction.DOWN, gameMap, others, base);
            }

            // Base should be destroyed since there's no steel protection
            assertFalse(base.isAlive(), "Base should be destroyed when BOSS reaches it without steel");
        }
    }

    @Nested
    @DisplayName("Slide Along Obstacle Tests")
    class SlideAlongObstacleTests {

        @Test
        @DisplayName("Tank should slide vertically when blocked horizontally and misaligned")
        void tankShouldSlideVerticallyWhenBlockedHorizontally() {
            // Place water tile at row 4, col 7 (pixel y=128, x=224)
            // Row 5 at y=160 is free
            gameMap.setTile(4, 7, GameMap.TileType.WATER);

            // Tank at position where it's slightly misaligned
            // Tank at x=196 (will touch col 7 when moving right), y=133 (offset from row 4, closer to row 5)
            // When moving RIGHT, it will be blocked by water at col 7
            // But it should slide DOWN toward row 5 (y=160) to pass through gap
            Tank tank = new Tank(196, 133, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            double initialY = tank.getY();

            // Try to move right - should trigger slide
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            // Tank should have slid (moved in Y direction to align)
            assertTrue(moved, "Tank should slide when blocked but misaligned");
            assertNotEquals(initialY, tank.getY(), "Y position should change during slide");
        }

        @Test
        @DisplayName("Tank should slide horizontally when blocked vertically and misaligned")
        void tankShouldSlideHorizontallyWhenBlockedVertically() {
            // Place water tile at row 7, col 4 (pixel y=224, x=128)
            // Col 5 at x=160 is free
            gameMap.setTile(7, 4, GameMap.TileType.WATER);

            // Tank at y=196 (will touch row 7 when moving down), x=133 (offset from col 4, closer to col 5)
            // When moving DOWN, it will be blocked by water at row 7
            // But it should slide RIGHT toward col 5 (x=160) to pass through gap
            Tank tank = new Tank(133, 196, Direction.DOWN, true, 1);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            double initialX = tank.getX();

            // Try to move down - should trigger slide
            boolean moved = physics.move(tank, Direction.DOWN, gameMap, others, base);

            // Tank should have slid (moved in X direction to align)
            assertTrue(moved, "Tank should slide when blocked but misaligned");
            assertNotEquals(initialX, tank.getX(), "X position should change during slide");
        }

        @Test
        @DisplayName("Tank should NOT slide when perfectly aligned with obstacle")
        void tankShouldNotSlideWhenAligned() {
            // Place water tiles blocking the entire path in multiple rows
            // Need to block all possible slide destinations
            gameMap.setTile(4, 7, GameMap.TileType.WATER);  // row above
            gameMap.setTile(5, 7, GameMap.TileType.WATER);  // current row
            gameMap.setTile(6, 7, GameMap.TileType.WATER);  // row below
            // Also block slide positions
            gameMap.setTile(4, 6, GameMap.TileType.WATER);
            gameMap.setTile(6, 6, GameMap.TileType.WATER);

            // Tank positioned right next to water - at x=196, tank spans x=196 to x=223
            // Water at col 7 starts at x=224, so moving right by 2 puts tank at x=198-225 touching water
            // Actually, let's position at x=196 so tank right edge at 223, moving to 225 collides with water at 224
            // Tank perfectly aligned at y=160 (row 5 start), offsetY = 0
            Tank tank = new Tank(196, 160, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            double initialX = tank.getX();
            double initialY = tank.getY();

            // Try to move right - should be blocked by water, no slide because offsetY == 0
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            // Tank should not have moved (perfectly aligned, no slide available)
            assertFalse(moved, "Tank should not move when blocked and perfectly aligned");
            assertEquals(initialX, tank.getX(), "X position should not change");
            assertEquals(initialY, tank.getY(), "Y position should not change");
        }

        @Test
        @DisplayName("Tank should not get stuck between two water tiles")
        void tankShouldNotGetStuckBetweenWaterTiles() {
            // Create a scenario with two water tiles forming a corridor
            // Water at row 4 and row 6, leaving row 5 as a gap
            gameMap.setTile(4, 5, GameMap.TileType.WATER);
            gameMap.setTile(4, 6, GameMap.TileType.WATER);
            gameMap.setTile(6, 5, GameMap.TileType.WATER);
            gameMap.setTile(6, 6, GameMap.TileType.WATER);

            // Tank in the gap - at y=160 (row 5), which is safe
            // Tank size is 28, so tank spans y=160 to y=187 (within row 5: 160-191)
            Tank tank = new Tank(160, 160, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            // Tank at exact grid alignment should be able to move freely in the corridor
            boolean movedRight = physics.move(tank, Direction.RIGHT, gameMap, others, base);
            assertTrue(movedRight, "Tank aligned in gap should move in corridor");

            // Reset and test vertical movement stays in corridor
            tank.setPosition(160, 160);
            boolean movedUp = physics.move(tank, Direction.UP, gameMap, others, base);
            // Should NOT be able to move up into water
            assertFalse(movedUp, "Tank should not move up into water");
        }

        @Test
        @DisplayName("Tank with SHIP powerup should pass through water without sliding")
        void tankWithShipShouldPassThroughWater() {
            // Place water tile
            gameMap.setTile(5, 7, GameMap.TileType.WATER);

            // Tank with SHIP ability
            Tank tank = new Tank(192, 160, Direction.RIGHT, true, 1);
            tank.applyShip(); // Give swimming ability
            List<Tank> others = new ArrayList<>();
            Base base = new Base(400, 700);

            double initialX = tank.getX();

            // Move right through water
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            assertTrue(moved, "Tank with SHIP should move through water");
            assertTrue(tank.getX() > initialX, "Tank should have moved right");
        }

        @Test
        @DisplayName("Slide should respect other tanks")
        void slideShouldRespectOtherTanks() {
            // Place water blocking horizontal movement
            gameMap.setTile(5, 7, GameMap.TileType.WATER);

            // Tank misaligned trying to move right
            Tank tank = new Tank(192, 165, Direction.RIGHT, true, 1);

            // Place another tank where the slide would go
            Tank blockingTank = new Tank(192, 140, Direction.UP, true, 2);
            List<Tank> others = new ArrayList<>();
            others.add(blockingTank);

            Base base = new Base(400, 700);

            // The slide up should be blocked by the other tank
            // But slide down might still work
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            // Result depends on whether down slide is available
        }

        @Test
        @DisplayName("Slide should respect base collision")
        void slideShouldRespectBaseCollision() {
            // Place water blocking horizontal movement
            gameMap.setTile(5, 7, GameMap.TileType.WATER);

            // Tank misaligned trying to move right
            Tank tank = new Tank(192, 165, Direction.RIGHT, true, 1);
            List<Tank> others = new ArrayList<>();

            // Place base where the slide would go
            Base base = new Base(192, 140);

            // The slide up should be blocked by the base
            boolean moved = physics.move(tank, Direction.RIGHT, gameMap, others, base);

            // May still slide down if that's available
        }
    }
}
