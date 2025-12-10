package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UFO Tests")
class UFOTest {

    private UFO ufo;

    @BeforeEach
    void setUp() {
        ufo = new UFO(100, 200, true);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should set position correctly")
        void constructorSetsPositionCorrectly() {
            assertEquals(100, ufo.getX());
            assertEquals(200, ufo.getY());
        }

        @Test
        @DisplayName("Constructor should set moving direction")
        void constructorSetsMovingDirection() {
            assertTrue(ufo.isMovingRight());

            UFO leftUfo = new UFO(100, 200, false);
            assertFalse(leftUfo.isMovingRight());
        }

        @Test
        @DisplayName("New UFO should be alive")
        void newUfoShouldBeAlive() {
            assertTrue(ufo.isAlive());
        }

        @Test
        @DisplayName("New UFO should have 3 health")
        void newUfoShouldHaveFullHealth() {
            assertEquals(3, ufo.getHealth());
        }

        @Test
        @DisplayName("New UFO should have full lifetime")
        void newUfoShouldHaveFullLifetime() {
            assertTrue(ufo.getLifetime() > 0);
        }

        @Test
        @DisplayName("Constructor sets initial velocity based on direction")
        void constructorSetsVelocity() {
            // Moving right - dx should be positive
            assertTrue(ufo.getDx() > 0);

            UFO leftUfo = new UFO(100, 200, false);
            // Moving left - dx should be negative
            assertTrue(leftUfo.getDx() < 0);
        }
    }

    @Nested
    @DisplayName("Damage Tests")
    class DamageTests {

        @Test
        @DisplayName("damage should decrease health")
        void damageShouldDecreaseHealth() {
            assertEquals(3, ufo.getHealth());

            ufo.damage();

            assertEquals(2, ufo.getHealth());
        }

        @Test
        @DisplayName("damage should return false when not destroyed")
        void damageShouldReturnFalseWhenNotDestroyed() {
            assertFalse(ufo.damage());
            assertFalse(ufo.damage());
        }

        @Test
        @DisplayName("damage should return true when destroyed")
        void damageShouldReturnTrueWhenDestroyed() {
            ufo.damage();
            ufo.damage();
            assertTrue(ufo.damage());
        }

        @Test
        @DisplayName("UFO should die after 3 hits")
        void ufoShouldDieAfterThreeHits() {
            ufo.damage();
            assertTrue(ufo.isAlive());

            ufo.damage();
            assertTrue(ufo.isAlive());

            ufo.damage();
            assertFalse(ufo.isAlive());
        }
    }

    @Nested
    @DisplayName("Collision Tests")
    class CollisionTests {

        @Test
        @DisplayName("collidesWith should return false for enemy bullets")
        void collidesWithReturnsFalseForEnemyBullets() {
            Bullet enemyBullet = new Bullet(100, 200, Direction.UP, true, 1, false, 0, 8);

            assertFalse(ufo.collidesWith(enemyBullet));
        }

        @Test
        @DisplayName("collidesWith should return true for player bullets hitting UFO")
        void collidesWithReturnsTrueForPlayerBullets() {
            Bullet playerBullet = new Bullet(110, 210, Direction.UP, false, 1, false, 1, 8);

            assertTrue(ufo.collidesWith(playerBullet));
        }

        @Test
        @DisplayName("collidesWith should return false for bullets outside UFO")
        void collidesWithReturnsFalseForMissedBullets() {
            Bullet missedBullet = new Bullet(500, 500, Direction.UP, false, 1, false, 1, 8);

            assertFalse(ufo.collidesWith(missedBullet));
        }

        @Test
        @DisplayName("collidesWith should return false when UFO is dead")
        void collidesWithReturnsFalseWhenDead() {
            ufo.setAlive(false);
            Bullet playerBullet = new Bullet(110, 210, Direction.UP, false, 1, false, 1, 8);

            assertFalse(ufo.collidesWith(playerBullet));
        }
    }

    @Nested
    @DisplayName("Setter Tests for Network Sync")
    class SetterTests {

        @Test
        @DisplayName("setX should update position")
        void setXShouldUpdatePosition() {
            ufo.setX(300);
            assertEquals(300, ufo.getX());
        }

        @Test
        @DisplayName("setY should update position")
        void setYShouldUpdatePosition() {
            ufo.setY(400);
            assertEquals(400, ufo.getY());
        }

        @Test
        @DisplayName("setDx should update velocity")
        void setDxShouldUpdateVelocity() {
            ufo.setDx(5.0);
            assertEquals(5.0, ufo.getDx());
        }

        @Test
        @DisplayName("setDy should update velocity")
        void setDyShouldUpdateVelocity() {
            ufo.setDy(3.0);
            assertEquals(3.0, ufo.getDy());
        }

        @Test
        @DisplayName("setAlive should update alive status")
        void setAliveShouldUpdateStatus() {
            ufo.setAlive(false);
            assertFalse(ufo.isAlive());
        }

        @Test
        @DisplayName("setHealth should update health")
        void setHealthShouldUpdateHealth() {
            ufo.setHealth(1);
            assertEquals(1, ufo.getHealth());
        }

        @Test
        @DisplayName("setLifetime should update lifetime")
        void setLifetimeShouldUpdateLifetime() {
            ufo.setLifetime(500);
            assertEquals(500, ufo.getLifetime());
        }

        @Test
        @DisplayName("setMovingRight should update direction")
        void setMovingRightShouldUpdateDirection() {
            ufo.setMovingRight(false);
            assertFalse(ufo.isMovingRight());
        }
    }
}
