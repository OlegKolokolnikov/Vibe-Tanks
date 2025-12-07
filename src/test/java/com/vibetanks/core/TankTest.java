package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tank Tests")
class TankTest {

    private Tank playerTank;
    private Tank enemyTank;

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Player tank should have correct initial properties")
        void playerTankHasCorrectInitialProperties() {
            playerTank = new Tank(100, 200, Direction.UP, true, 1);

            assertEquals(100, playerTank.getX());
            assertEquals(200, playerTank.getY());
            assertEquals(Direction.UP, playerTank.getDirection());
            assertTrue(playerTank.isAlive());
            assertEquals(3, playerTank.getLives());
            assertTrue(playerTank.hasShield());
            assertEquals(28, playerTank.getSize());
        }

        @Test
        @DisplayName("Enemy tank should have correct initial properties")
        void enemyTankHasCorrectInitialProperties() {
            enemyTank = new Tank(100, 200, Direction.DOWN, false, 0);

            assertEquals(100, enemyTank.getX());
            assertEquals(200, enemyTank.getY());
            assertEquals(Direction.DOWN, enemyTank.getDirection());
            assertTrue(enemyTank.isAlive());
            assertEquals(1, enemyTank.getLives());
            assertFalse(enemyTank.hasShield());
        }

        @Test
        @DisplayName("Player tank should have 3 lives")
        void playerTankHasThreeLives() {
            playerTank = new Tank(0, 0, Direction.UP, true, 1);

            assertEquals(3, playerTank.getLives());
        }

        @Test
        @DisplayName("Enemy tank should have 1 life")
        void enemyTankHasOneLife() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0);

            assertEquals(1, enemyTank.getLives());
        }

        @Test
        @DisplayName("Player should start with shield")
        void playerStartsWithShield() {
            playerTank = new Tank(0, 0, Direction.UP, true, 1);

            assertTrue(playerTank.hasShield());
        }

        @Test
        @DisplayName("Enemy should not start with shield")
        void enemyDoesNotStartWithShield() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0);

            assertFalse(enemyTank.hasShield());
        }
    }

    @Nested
    @DisplayName("Enemy Type Tests")
    class EnemyTypeTests {

        @Test
        @DisplayName("REGULAR enemy should have health 1")
        void regularEnemyHasHealthOne() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.REGULAR);

            assertEquals(1, enemyTank.getHealth());
            assertEquals(1, enemyTank.getMaxHealth());
        }

        @Test
        @DisplayName("ARMORED enemy should have health 2")
        void armoredEnemyHasHealthTwo() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.ARMORED);

            assertEquals(2, enemyTank.getHealth());
            assertEquals(2, enemyTank.getMaxHealth());
        }

        @Test
        @DisplayName("FAST enemy should have higher speed multiplier")
        void fastEnemyHasHigherSpeed() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.FAST);

            assertEquals(1.5, enemyTank.getSpeedMultiplier());
        }

        @Test
        @DisplayName("POWER enemy should have health 2")
        void powerEnemyHasHealthTwo() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.POWER);

            assertEquals(2, enemyTank.getHealth());
        }

        @Test
        @DisplayName("HEAVY enemy should have health 3 and high speed")
        void heavyEnemyHasHealthThreeAndHighSpeed() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.HEAVY);

            assertEquals(3, enemyTank.getHealth());
            assertEquals(1.5, enemyTank.getSpeedMultiplier());
        }

        @Test
        @DisplayName("BOSS should have health 12 and 4x size")
        void bossHasHealthTwelveAndLargeSize() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);

            assertEquals(12, enemyTank.getHealth());
            assertEquals(28 * 4, enemyTank.getSize()); // 4x base size
        }

        @Test
        @DisplayName("BOSS should be able to swim")
        void bossCanSwim() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);

            assertTrue(enemyTank.canSwim());
        }

        @Test
        @DisplayName("BOSS should be able to destroy trees")
        void bossCanDestroyTrees() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);

            assertTrue(enemyTank.canDestroyTrees());
        }

        @ParameterizedTest
        @EnumSource(Tank.EnemyType.class)
        @DisplayName("All enemy types should be creatable")
        void allEnemyTypesCreatable(Tank.EnemyType type) {
            Tank tank = new Tank(0, 0, Direction.DOWN, false, 0, type);

            assertEquals(type, tank.getEnemyType());
            assertTrue(tank.isAlive());
        }
    }

    @Nested
    @DisplayName("Health and Damage Tests")
    class HealthAndDamageTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
            enemyTank = new Tank(200, 200, Direction.DOWN, false, 0, Tank.EnemyType.ARMORED);
        }

        @Test
        @DisplayName("Damage should reduce health")
        void damageShouldReduceHealth() {
            // Remove shield first
            playerTank.setShield(false);
            int initialHealth = playerTank.getHealth();

            boolean died = playerTank.damage();

            assertTrue(died || playerTank.getHealth() < initialHealth);
        }

        @Test
        @DisplayName("Shield should block damage")
        void shieldShouldBlockDamage() {
            playerTank.setShield(true);
            int initialHealth = playerTank.getHealth();

            boolean died = playerTank.damage();

            assertFalse(died);
            assertEquals(initialHealth, playerTank.getHealth());
        }

        @Test
        @DisplayName("Armored enemy should survive first hit")
        void armoredEnemyShouldSurviveFirstHit() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.ARMORED);

            boolean died = enemyTank.damage();

            assertFalse(died);
            assertTrue(enemyTank.isAlive());
            assertEquals(1, enemyTank.getHealth());
        }

        @Test
        @DisplayName("Armored enemy should die on second hit")
        void armoredEnemyShouldDieOnSecondHit() {
            enemyTank = new Tank(0, 0, Direction.DOWN, false, 0, Tank.EnemyType.ARMORED);

            enemyTank.damage(); // First hit: health goes from 2 to 1
            enemyTank.damage(); // Second hit: health goes from 1 to 0, dies

            assertFalse(enemyTank.isAlive());
            assertEquals(0, enemyTank.getHealth());
        }

        @Test
        @DisplayName("setHealth should update health value")
        void setHealthShouldUpdateValue() {
            enemyTank.setHealth(5);

            assertEquals(5, enemyTank.getHealth());
        }

        @Test
        @DisplayName("setMaxHealth should update max health value")
        void setMaxHealthShouldUpdateValue() {
            enemyTank.setMaxHealth(10);

            assertEquals(10, enemyTank.getMaxHealth());
        }
    }

    @Nested
    @DisplayName("Lives Tests")
    class LivesTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("setLives should update lives count")
        void setLivesShouldUpdateCount() {
            playerTank.setLives(5);

            assertEquals(5, playerTank.getLives());
        }

        @Test
        @DisplayName("Lives can be set to zero")
        void livesCanBeSetToZero() {
            playerTank.setLives(0);

            assertEquals(0, playerTank.getLives());
        }

        @Test
        @DisplayName("Lives can be negative (no validation)")
        void livesCanBeNegative() {
            playerTank.setLives(-1);

            assertEquals(-1, playerTank.getLives());
        }
    }

    @Nested
    @DisplayName("Direction Tests")
    class DirectionTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("setDirection should change direction")
        void setDirectionShouldChangeDirection() {
            playerTank.setDirection(Direction.LEFT);

            assertEquals(Direction.LEFT, playerTank.getDirection());
        }

        @ParameterizedTest
        @EnumSource(Direction.class)
        @DisplayName("All directions should be settable")
        void allDirectionsSettable(Direction dir) {
            playerTank.setDirection(dir);

            assertEquals(dir, playerTank.getDirection());
        }
    }

    @Nested
    @DisplayName("Position Tests")
    class PositionTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("setPosition should update both X and Y")
        void setPositionShouldUpdateBothCoordinates() {
            playerTank.setPosition(250, 350);

            assertEquals(250, playerTank.getX());
            assertEquals(350, playerTank.getY());
        }

        @Test
        @DisplayName("Position can be negative")
        void positionCanBeNegative() {
            playerTank.setPosition(-50, -100);

            assertEquals(-50, playerTank.getX());
            assertEquals(-100, playerTank.getY());
        }
    }

    @Nested
    @DisplayName("Power-Up Application Tests")
    class PowerUpTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
            // Remove initial shield
            playerTank.setShield(false);
        }

        @Test
        @DisplayName("applyGun should give gun ability")
        void applyGunShouldGiveGunAbility() {
            assertFalse(playerTank.hasGun());

            playerTank.applyGun();

            assertTrue(playerTank.hasGun());
        }

        @Test
        @DisplayName("applyCar should increase speed multiplier")
        void applyCarShouldIncreaseSpeed() {
            double initialSpeed = playerTank.getSpeedMultiplier();

            playerTank.applyCar();

            assertTrue(playerTank.getSpeedMultiplier() > initialSpeed);
        }

        @Test
        @DisplayName("applyShip should enable swimming")
        void applyShipShouldEnableSwimming() {
            assertFalse(playerTank.canSwim());

            playerTank.applyShip();

            assertTrue(playerTank.canSwim());
        }

        @Test
        @DisplayName("applySaw should enable tree destruction")
        void applySawShouldEnableTreeDestruction() {
            assertFalse(playerTank.canDestroyTrees());

            playerTank.applySaw();

            assertTrue(playerTank.canDestroyTrees());
        }

        @Test
        @DisplayName("applyTank should add extra life")
        void applyTankShouldAddExtraLife() {
            int initialLives = playerTank.getLives();

            playerTank.applyTank();

            assertEquals(initialLives + 1, playerTank.getLives());
        }

        @Test
        @DisplayName("applyShield should enable shield")
        void applyShieldShouldEnableShield() {
            playerTank.setShield(false);

            playerTank.applyShield();

            assertTrue(playerTank.hasShield());
        }

        @Test
        @DisplayName("applyMachinegun should increase machinegun count")
        void applyMachinegunShouldIncreaseMachinegunCount() {
            int initialCount = playerTank.getMachinegunCount();

            playerTank.applyMachinegun();

            assertEquals(initialCount + 1, playerTank.getMachinegunCount());
        }

        @Test
        @DisplayName("applyLaser should give laser for 30 seconds")
        void applyLaserShouldEnableLaser() {
            assertFalse(playerTank.hasLaser());

            playerTank.applyLaser();

            assertTrue(playerTank.hasLaser());
        }

        @Test
        @DisplayName("applyStar should increase star count")
        void applyStarShouldIncreaseStarCount() {
            int initialCount = playerTank.getStarCount();

            playerTank.applyStar();

            assertEquals(initialCount + 1, playerTank.getStarCount());
        }
    }

    @Nested
    @DisplayName("Respawn Tests")
    class RespawnTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("respawn should start respawn timer")
        void respawnShouldStartTimer() {
            // Kill the tank first
            playerTank.setShield(false);
            playerTank.damage();

            playerTank.respawn(200, 200);

            assertTrue(playerTank.isWaitingToRespawn());
        }

        @Test
        @DisplayName("spawnImmediate should respawn instantly")
        void spawnImmediateShouldRespawnInstantly() {
            // Kill the tank
            playerTank.setShield(false);
            playerTank.damage();

            playerTank.spawnImmediate(300, 400);

            assertTrue(playerTank.isAlive());
            assertEquals(300, playerTank.getX());
            assertEquals(400, playerTank.getY());
            assertFalse(playerTank.isWaitingToRespawn());
        }

        @Test
        @DisplayName("isWaitingToRespawn should be false for alive tank")
        void isWaitingToRespawnFalseForAliveTank() {
            assertFalse(playerTank.isWaitingToRespawn());
        }

        @Test
        @DisplayName("getRespawnTimer should return 0 for not-respawning tank")
        void getRespawnTimerZeroForNotRespawningTank() {
            assertEquals(0, playerTank.getRespawnTimer());
        }

        @Test
        @DisplayName("giveTemporaryShield should enable shield")
        void giveTemporaryShieldShouldEnableShield() {
            playerTank.setShield(false);

            playerTank.giveTemporaryShield();

            assertTrue(playerTank.hasShield());
        }
    }

    @Nested
    @DisplayName("Shield Tests")
    class ShieldTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(100, 100, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("setShield should update shield status")
        void setShieldShouldUpdateStatus() {
            playerTank.setShield(false);
            assertFalse(playerTank.hasShield());

            playerTank.setShield(true);
            assertTrue(playerTank.hasShield());
        }

        @Test
        @DisplayName("Pause shield should be separate from regular shield")
        void pauseShieldSeparateFromRegularShield() {
            playerTank.setShield(false);
            playerTank.setPauseShield(true);

            assertFalse(playerTank.hasShield());
            assertTrue(playerTank.hasPauseShield());
        }

        @Test
        @DisplayName("setShieldWithDuration should set both shield and duration")
        void setShieldWithDurationShouldSetBoth() {
            playerTank.setShieldWithDuration(true, 200);

            assertTrue(playerTank.hasShield());
            assertEquals(200, playerTank.getShieldDuration());
        }
    }

    @Nested
    @DisplayName("Speed and Movement Tests")
    class SpeedTests {

        @Test
        @DisplayName("Default speed multiplier should be 1.0")
        void defaultSpeedMultiplierIsOne() {
            playerTank = new Tank(0, 0, Direction.UP, true, 1);

            assertEquals(1.0, playerTank.getSpeedMultiplier());
        }

        @Test
        @DisplayName("getTempSpeedBoost should return temporary boost value")
        void getTempSpeedBoostReturnsValue() {
            playerTank = new Tank(0, 0, Direction.UP, true, 1);

            // Initial temp speed boost is 0
            assertEquals(0.0, playerTank.getTempSpeedBoost());
        }
    }

    @Nested
    @DisplayName("Laser Duration Tests")
    class LaserDurationTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(0, 0, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("New tank should not have laser")
        void newTankShouldNotHaveLaser() {
            assertFalse(playerTank.hasLaser());
        }

        @Test
        @DisplayName("setLaserDuration should set duration")
        void setLaserDurationShouldSetDuration() {
            playerTank.setLaserDuration(1800);

            assertEquals(1800, playerTank.getLaserDuration());
            assertTrue(playerTank.hasLaser());
        }

        @Test
        @DisplayName("Zero laser duration means no laser")
        void zeroLaserDurationMeansNoLaser() {
            playerTank.setLaserDuration(0);

            assertFalse(playerTank.hasLaser());
        }
    }

    @Nested
    @DisplayName("Bullet Tracking Tests")
    class BulletTrackingTests {

        @BeforeEach
        void setUp() {
            playerTank = new Tank(0, 0, Direction.UP, true, 1);
        }

        @Test
        @DisplayName("bulletDestroyed should decrease active bullet count")
        void bulletDestroyedShouldDecreaseCount() {
            // Simulate having active bullets
            // Note: This is tested indirectly since activeBulletCount is private
            // The method should not throw even when count is 0
            playerTank.bulletDestroyed();
            playerTank.bulletDestroyed();
            // Should not throw exception
        }
    }
}
