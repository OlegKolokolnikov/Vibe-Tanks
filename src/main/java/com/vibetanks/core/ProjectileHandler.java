package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;

import java.util.Iterator;
import java.util.List;

/**
 * Handles bullet and laser updates and collision detection.
 * Extracts projectile logic from Game.java to improve separation of concerns.
 */
public class ProjectileHandler {

    /**
     * Result of bullet collision processing for a single bullet.
     */
    public static class BulletCollisionResult {
        public boolean shouldRemove = false;
        public boolean hitEnemy = false;
        public boolean enemyKilled = false;
        public Tank killedEnemy = null;
        public int killerPlayerNumber = -1;
        public boolean shouldDropPowerUp = false;
        public boolean hitPlayer = false;
        public boolean playerKilled = false;
        public Tank killedPlayer = null;
        public boolean hitBase = false;
        public boolean hitUfo = false;
        public boolean ufoDestroyed = false;
    }

    /**
     * Result of laser collision processing.
     */
    public static class LaserCollisionResult {
        public boolean enemyKilled = false;
        public Tank killedEnemy = null;
        public int killerPlayerNumber = -1;
        public boolean shouldDropPowerUp = false;
        public boolean hitBase = false;
        public boolean hitUfo = false;
        public boolean ufoDestroyed = false;
    }

    /**
     * Process a single bullet's movement and collisions.
     *
     * @param bullet The bullet to process
     * @param gameMap The game map
     * @param enemyTanks List of enemy tanks
     * @param playerTanks List of player tanks
     * @param base The base
     * @param ufo The UFO (can be null)
     * @param mapWidth Map width in pixels
     * @param mapHeight Map height in pixels
     * @param soundManager Sound manager for collision sounds
     * @return Result of collision processing
     */
    public static BulletCollisionResult processBullet(
            Bullet bullet,
            GameMap gameMap,
            List<Tank> enemyTanks,
            List<Tank> playerTanks,
            Base base,
            UFO ufo,
            int mapWidth,
            int mapHeight,
            SoundManager soundManager) {

        BulletCollisionResult result = new BulletCollisionResult();

        // Update bullet position
        bullet.update();

        // Check map collision
        if (gameMap.checkBulletCollision(bullet, soundManager)) {
            result.shouldRemove = true;
            return result;
        }

        // Check out of bounds with wraparound
        if (bullet.isOutOfBounds(mapWidth, mapHeight)) {
            if (!bullet.handleWraparound(gameMap, mapWidth, mapHeight)) {
                result.shouldRemove = true;
                return result;
            }
        }

        // Check UFO collision (player bullets only)
        if (!bullet.isFromEnemy() && ufo != null && ufo.isAlive()) {
            if (ufo.collidesWith(bullet)) {
                result.hitUfo = true;
                boolean destroyed = ufo.damage();
                if (destroyed) {
                    result.ufoDestroyed = true;
                    result.killerPlayerNumber = bullet.getOwnerPlayerNumber();
                    soundManager.playExplosion();
                }
                result.shouldRemove = true;
                return result;
            }
        }

        // Player bullets hit enemies
        if (!bullet.isFromEnemy()) {
            for (Tank enemy : enemyTanks) {
                if (enemy.isAlive() && bullet.collidesWith(enemy)) {
                    result.hitEnemy = true;
                    boolean dropPowerUp = enemy.damage();

                    // Check for power-up drop
                    if (dropPowerUp || (!enemy.isAlive() && GameConstants.RANDOM.nextDouble() < 0.3)) {
                        result.shouldDropPowerUp = true;
                    }

                    if (!enemy.isAlive()) {
                        result.enemyKilled = true;
                        result.killedEnemy = enemy;
                        result.killerPlayerNumber = bullet.getOwnerPlayerNumber();
                        soundManager.playExplosion();
                    }

                    result.shouldRemove = true;
                    return result;
                }
            }
        } else {
            // Enemy bullets hit players
            for (Tank player : playerTanks) {
                if (player.isAlive() && bullet.collidesWith(player)) {
                    if (!player.hasShield() && !player.hasPauseShield()) {
                        result.hitPlayer = true;
                        player.damage();

                        if (!player.isAlive()) {
                            result.playerKilled = true;
                            result.killedPlayer = player;
                            soundManager.playPlayerDeath();
                        }
                    }
                    result.shouldRemove = true;
                    return result;
                }
            }
        }

        // Check base collision (all bullets)
        if (bullet.collidesWith(base) && base.isAlive()) {
            result.hitBase = true;
            result.shouldRemove = true;
            return result;
        }

        return result;
    }

    /**
     * Process bullet-to-bullet collisions.
     * Removes colliding bullets from the list.
     *
     * @param bullets List of bullets to check
     * @param playerTanks Player tanks for bullet destroyed notification
     */
    public static void processBulletToBulletCollisions(List<Bullet> bullets, List<Tank> playerTanks) {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet1 = bullets.get(i);
            for (int j = i + 1; j < bullets.size(); j++) {
                Bullet bullet2 = bullets.get(j);
                if (bullet1.collidesWith(bullet2)) {
                    GameLogic.notifyBulletDestroyed(bullet1, playerTanks);
                    GameLogic.notifyBulletDestroyed(bullet2, playerTanks);
                    bullets.remove(j);
                    bullets.remove(i);
                    i--;
                    break;
                }
            }
        }
    }

    /**
     * Process a single laser's collisions.
     *
     * @param laser The laser to process
     * @param enemyTanks List of enemy tanks
     * @param playerTanks List of player tanks
     * @param base The base
     * @param ufo The UFO (can be null)
     * @param soundManager Sound manager
     * @return Result of collision processing
     */
    public static LaserCollisionResult processLaser(
            Laser laser,
            List<Tank> enemyTanks,
            List<Tank> playerTanks,
            Base base,
            UFO ufo,
            SoundManager soundManager) {

        LaserCollisionResult result = new LaserCollisionResult();

        if (laser.isFromEnemy()) {
            // Enemy laser hits players
            for (Tank player : playerTanks) {
                if (player.isAlive() && !player.hasShield() && !player.hasPauseShield()
                        && laser.collidesWith(player)) {
                    // Deal 3 damage
                    for (int dmg = 0; dmg < 3 && player.isAlive(); dmg++) {
                        player.damage();
                    }
                    if (!player.isAlive()) {
                        soundManager.playPlayerDeath();
                    }
                }
            }
        } else {
            // Player laser hits enemies
            for (Tank enemy : enemyTanks) {
                if (enemy.isAlive() && laser.collidesWith(enemy)) {
                    // Deal 3 damage
                    for (int dmg = 0; dmg < 3 && enemy.isAlive(); dmg++) {
                        enemy.damage();
                    }
                    if (!enemy.isAlive()) {
                        result.enemyKilled = true;
                        result.killedEnemy = enemy;
                        result.killerPlayerNumber = laser.getOwnerPlayerNumber();
                        soundManager.playExplosion();

                        // 30% chance for power-up drop
                        if (GameConstants.RANDOM.nextDouble() < 0.3) {
                            result.shouldDropPowerUp = true;
                        }
                    }
                }
            }

            // Player laser hits UFO
            if (ufo != null && ufo.isAlive() && laser.collidesWithUFO(ufo)) {
                result.hitUfo = true;
                for (int dmg = 0; dmg < 3 && ufo.isAlive(); dmg++) {
                    boolean destroyed = ufo.damage();
                    if (destroyed) {
                        result.ufoDestroyed = true;
                        result.killerPlayerNumber = laser.getOwnerPlayerNumber();
                        soundManager.playExplosion();
                        break;
                    }
                }
            }

            // Player laser hits base
            if (laser.collidesWithBase(base) && base.isAlive()) {
                result.hitBase = true;
            }
        }

        return result;
    }

    /**
     * Get score for killing an enemy.
     *
     * @param enemyType The type of enemy killed
     * @return Points to award
     */
    public static int getScoreForKill(Tank.EnemyType enemyType) {
        return switch (enemyType) {
            case POWER -> 2;  // Rainbow tank
            case HEAVY -> 5;  // Black tank
            case BOSS -> 10;  // Boss tank
            default -> 1;     // Regular, Armored, Fast
        };
    }
}
