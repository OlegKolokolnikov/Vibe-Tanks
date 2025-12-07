package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bullet Tests")
class BulletTest {

    private Bullet bullet;

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Simple constructor should set defaults correctly")
        void simpleConstructorSetsDefaults() {
            bullet = new Bullet(100, 200, Direction.UP, false, 1, false);

            assertEquals(100, bullet.getX());
            assertEquals(200, bullet.getY());
            assertEquals(Direction.UP, bullet.getDirection());
            assertFalse(bullet.isFromEnemy());
            assertEquals(1, bullet.getPower());
            assertFalse(bullet.canDestroyTrees());
            assertEquals(0, bullet.getOwnerPlayerNumber());
            assertEquals(8, bullet.getSize()); // DEFAULT_SIZE
        }

        @Test
        @DisplayName("Constructor with owner should set player number")
        void constructorWithOwnerSetsPlayerNumber() {
            bullet = new Bullet(100, 200, Direction.RIGHT, false, 2, true, 1);

            assertEquals(1, bullet.getOwnerPlayerNumber());
            assertEquals(2, bullet.getPower());
            assertTrue(bullet.canDestroyTrees());
        }

        @Test
        @DisplayName("Full constructor should set all properties")
        void fullConstructorSetsAllProperties() {
            bullet = new Bullet(50, 75, Direction.LEFT, true, 3, true, 2, 16);

            assertEquals(50, bullet.getX());
            assertEquals(75, bullet.getY());
            assertEquals(Direction.LEFT, bullet.getDirection());
            assertTrue(bullet.isFromEnemy());
            assertEquals(3, bullet.getPower());
            assertTrue(bullet.canDestroyTrees());
            assertEquals(2, bullet.getOwnerPlayerNumber());
            assertEquals(16, bullet.getSize());
        }

        @Test
        @DisplayName("Constructor with explicit ID should use given ID")
        void constructorWithExplicitIdUsesGivenId() {
            bullet = new Bullet(999L, 100, 200, Direction.DOWN, false, 1, false, 1, 8);

            assertEquals(999L, bullet.getId());
        }

        @Test
        @DisplayName("Each bullet should get unique auto-generated ID")
        void eachBulletGetsUniqueId() {
            Bullet bullet1 = new Bullet(0, 0, Direction.UP, false, 1, false);
            Bullet bullet2 = new Bullet(0, 0, Direction.UP, false, 1, false);
            Bullet bullet3 = new Bullet(0, 0, Direction.UP, false, 1, false);

            assertNotEquals(bullet1.getId(), bullet2.getId());
            assertNotEquals(bullet2.getId(), bullet3.getId());
            assertNotEquals(bullet1.getId(), bullet3.getId());
        }
    }

    @Nested
    @DisplayName("Movement Tests")
    class MovementTests {

        @Test
        @DisplayName("Update should move bullet UP correctly")
        void updateMovesBulletUp() {
            bullet = new Bullet(100, 200, Direction.UP, false, 1, false);
            double initialY = bullet.getY();

            bullet.update();

            assertEquals(100, bullet.getX()); // X unchanged
            assertEquals(initialY - 6.0, bullet.getY()); // SPEED = 6.0
        }

        @Test
        @DisplayName("Update should move bullet DOWN correctly")
        void updateMovesBulletDown() {
            bullet = new Bullet(100, 200, Direction.DOWN, false, 1, false);
            double initialY = bullet.getY();

            bullet.update();

            assertEquals(100, bullet.getX());
            assertEquals(initialY + 6.0, bullet.getY());
        }

        @Test
        @DisplayName("Update should move bullet LEFT correctly")
        void updateMovesBulletLeft() {
            bullet = new Bullet(100, 200, Direction.LEFT, false, 1, false);
            double initialX = bullet.getX();

            bullet.update();

            assertEquals(initialX - 6.0, bullet.getX());
            assertEquals(200, bullet.getY()); // Y unchanged
        }

        @Test
        @DisplayName("Update should move bullet RIGHT correctly")
        void updateMovesBulletRight() {
            bullet = new Bullet(100, 200, Direction.RIGHT, false, 1, false);
            double initialX = bullet.getX();

            bullet.update();

            assertEquals(initialX + 6.0, bullet.getX());
            assertEquals(200, bullet.getY());
        }

        @Test
        @DisplayName("Multiple updates should accumulate movement")
        void multipleUpdatesAccumulateMovement() {
            bullet = new Bullet(100, 200, Direction.RIGHT, false, 1, false);

            bullet.update();
            bullet.update();
            bullet.update();

            assertEquals(100 + 6.0 * 3, bullet.getX());
        }
    }

    @Nested
    @DisplayName("Bounds Tests")
    class BoundsTests {

        @Test
        @DisplayName("Bullet at origin should not be out of bounds")
        void bulletAtOriginNotOutOfBounds() {
            bullet = new Bullet(0, 0, Direction.UP, false, 1, false);

            assertFalse(bullet.isOutOfBounds(800, 600));
        }

        @Test
        @DisplayName("Bullet with negative X should be out of bounds")
        void bulletNegativeXIsOutOfBounds() {
            bullet = new Bullet(-1, 100, Direction.UP, false, 1, false);

            assertTrue(bullet.isOutOfBounds(800, 600));
        }

        @Test
        @DisplayName("Bullet with negative Y should be out of bounds")
        void bulletNegativeYIsOutOfBounds() {
            bullet = new Bullet(100, -1, Direction.UP, false, 1, false);

            assertTrue(bullet.isOutOfBounds(800, 600));
        }

        @Test
        @DisplayName("Bullet beyond width should be out of bounds")
        void bulletBeyondWidthIsOutOfBounds() {
            bullet = new Bullet(801, 100, Direction.UP, false, 1, false);

            assertTrue(bullet.isOutOfBounds(800, 600));
        }

        @Test
        @DisplayName("Bullet beyond height should be out of bounds")
        void bulletBeyondHeightIsOutOfBounds() {
            bullet = new Bullet(100, 601, Direction.UP, false, 1, false);

            assertTrue(bullet.isOutOfBounds(800, 600));
        }

        @Test
        @DisplayName("handleWraparound should return false when out of bounds")
        void handleWraparoundReturnsFalseWhenOutOfBounds() {
            bullet = new Bullet(-10, 100, Direction.LEFT, false, 1, false);

            assertFalse(bullet.handleWraparound(null, 800, 600));
        }

        @Test
        @DisplayName("handleWraparound should return true when in bounds")
        void handleWraparoundReturnsTrueWhenInBounds() {
            bullet = new Bullet(100, 100, Direction.UP, false, 1, false);

            assertTrue(bullet.handleWraparound(null, 800, 600));
        }
    }

    @Nested
    @DisplayName("Collision Tests")
    class CollisionTests {

        @Test
        @DisplayName("Bullets should collide when overlapping")
        void bulletsCollideWhenOverlapping() {
            Bullet bullet1 = new Bullet(100, 100, Direction.UP, false, 1, false);
            Bullet bullet2 = new Bullet(104, 104, Direction.DOWN, true, 1, false);

            assertTrue(bullet1.collidesWith(bullet2));
            assertTrue(bullet2.collidesWith(bullet1));
        }

        @Test
        @DisplayName("Bullets should not collide when far apart")
        void bulletsDoNotCollideWhenFarApart() {
            Bullet bullet1 = new Bullet(100, 100, Direction.UP, false, 1, false);
            Bullet bullet2 = new Bullet(200, 200, Direction.DOWN, true, 1, false);

            assertFalse(bullet1.collidesWith(bullet2));
            assertFalse(bullet2.collidesWith(bullet1));
        }

        @Test
        @DisplayName("Bullets should not collide when just touching edge")
        void bulletsDoNotCollideWhenJustTouchingEdge() {
            Bullet bullet1 = new Bullet(100, 100, Direction.UP, false, 1, false);
            // bullet1 is at (100, 100) with size 8, so it ends at (108, 108)
            Bullet bullet2 = new Bullet(108, 100, Direction.DOWN, true, 1, false);

            assertFalse(bullet1.collidesWith(bullet2));
        }

        @Test
        @DisplayName("Bigger bullets should have larger collision area")
        void biggerBulletsHaveLargerCollisionArea() {
            Bullet smallBullet = new Bullet(100, 100, Direction.UP, false, 1, false, 0, 8);
            Bullet bigBullet = new Bullet(100, 100, Direction.UP, false, 1, false, 0, 32);

            // Check that big bullet has larger size
            assertEquals(8, smallBullet.getSize());
            assertEquals(32, bigBullet.getSize());
        }
    }

    @Nested
    @DisplayName("Property Tests")
    class PropertyTests {

        @Test
        @DisplayName("Enemy bullet should be marked as from enemy")
        void enemyBulletIsMarkedAsFromEnemy() {
            bullet = new Bullet(100, 100, Direction.UP, true, 1, false);

            assertTrue(bullet.isFromEnemy());
        }

        @Test
        @DisplayName("Player bullet should not be marked as from enemy")
        void playerBulletIsNotFromEnemy() {
            bullet = new Bullet(100, 100, Direction.UP, false, 1, false);

            assertFalse(bullet.isFromEnemy());
        }

        @Test
        @DisplayName("Power should be stored correctly")
        void powerIsStoredCorrectly() {
            Bullet normalBullet = new Bullet(0, 0, Direction.UP, false, 1, false);
            Bullet powerBullet = new Bullet(0, 0, Direction.UP, false, 2, false);

            assertEquals(1, normalBullet.getPower());
            assertEquals(2, powerBullet.getPower());
        }

        @Test
        @DisplayName("Can destroy trees property should be stored correctly")
        void canDestroyTreesIsStoredCorrectly() {
            Bullet normalBullet = new Bullet(0, 0, Direction.UP, false, 1, false);
            Bullet sawBullet = new Bullet(0, 0, Direction.UP, false, 1, true);

            assertFalse(normalBullet.canDestroyTrees());
            assertTrue(sawBullet.canDestroyTrees());
        }
    }
}
