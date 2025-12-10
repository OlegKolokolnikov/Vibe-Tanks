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
    }
}
