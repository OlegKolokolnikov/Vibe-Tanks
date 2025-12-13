package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;

import java.util.List;

/**
 * Handles bullet and laser updates and collision detection.
 * Extracts projectile logic from Game.java to improve separation of concerns.
 *
 * Optimized with spatial partitioning for O(n) collision detection instead of O(n^2).
 * Uses object pooling to reduce GC pressure in hot paths.
 */
public class ProjectileHandler {
    private static final com.vibetanks.util.GameLogger LOG = com.vibetanks.util.GameLogger.getLogger(ProjectileHandler.class);

    // Spatial grids for efficient collision detection
    private static SpatialGrid<Tank> tankGrid;
    private static SpatialGrid<Bullet> bulletGrid;

    // Object pools for collision results (avoid allocation in hot path)
    private static final BulletCollisionResult bulletResultPool = new BulletCollisionResult();
    private static final LaserCollisionResult laserResultPool = new LaserCollisionResult();

    // Pre-allocated set for bullet-to-bullet collision detection
    private static final java.util.Set<Bullet> bulletRemovalSet = new java.util.HashSet<>(32);

    /**
     * Initialize or resize spatial grids for collision detection.
     * Call once at game start and when map size changes.
     */
    public static void initializeSpatialGrids(int mapWidth, int mapHeight) {
        tankGrid = new SpatialGrid<>(mapWidth, mapHeight);
        bulletGrid = new SpatialGrid<>(mapWidth, mapHeight);
    }

    /**
     * Update spatial grids with current entity positions.
     * Call at the start of each frame before collision detection.
     */
    public static void updateSpatialGrids(List<Tank> allTanks, List<Bullet> bullets) {
        // Clear grids
        if (tankGrid != null) {
            tankGrid.clear();
            // Insert all alive tanks
            for (Tank tank : allTanks) {
                if (tank.isAlive()) {
                    tankGrid.insertWithSize(tank, tank.getX(), tank.getY(), tank.getSize());
                }
            }
        }

        if (bulletGrid != null) {
            bulletGrid.clear();
            // Insert all bullets
            for (Bullet bullet : bullets) {
                bulletGrid.insert(bullet, bullet.getX(), bullet.getY());
            }
        }
    }

    /**
     * Result of bullet collision processing for a single bullet.
     * Reusable - call reset() before each use to avoid allocation.
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

        /** Reset all fields to default values for reuse */
        public void reset() {
            shouldRemove = false;
            hitEnemy = false;
            enemyKilled = false;
            killedEnemy = null;
            killerPlayerNumber = -1;
            shouldDropPowerUp = false;
            hitPlayer = false;
            playerKilled = false;
            killedPlayer = null;
            hitBase = false;
            hitUfo = false;
            ufoDestroyed = false;
        }
    }

    /**
     * Result of laser collision processing.
     * Reusable - call reset() before each use to avoid allocation.
     */
    public static class LaserCollisionResult {
        public boolean enemyKilled = false;
        public Tank killedEnemy = null;
        public int killerPlayerNumber = -1;
        public boolean shouldDropPowerUp = false;
        public boolean hitBase = false;
        public boolean hitUfo = false;
        public boolean ufoDestroyed = false;
        public boolean playerKilled = false;
        public Tank killedPlayer = null;
        public boolean isBossKill = false;

        /** Reset all fields to default values for reuse */
        public void reset() {
            enemyKilled = false;
            killedEnemy = null;
            killerPlayerNumber = -1;
            shouldDropPowerUp = false;
            hitBase = false;
            hitUfo = false;
            ufoDestroyed = false;
            playerKilled = false;
            killedPlayer = null;
            isBossKill = false;
        }
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

        // Use pooled result object to avoid allocation (reset for reuse)
        BulletCollisionResult result = bulletResultPool;
        result.reset();

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

        // Use spatial grid for efficient tank collision detection
        List<Tank> nearbyTanks = tankGrid != null
            ? tankGrid.getNearby(bullet.getX(), bullet.getY())
            : null;

        // Player bullets hit enemies
        if (!bullet.isFromEnemy()) {
            // Use spatial grid if available, fall back to full list
            Iterable<Tank> tanksToCheck = nearbyTanks != null ? nearbyTanks : enemyTanks;
            for (Tank enemy : tanksToCheck) {
                // Skip player tanks when checking enemy collisions
                if (enemy.isPlayer()) continue;
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
            // Use spatial grid if available, fall back to full list
            Iterable<Tank> tanksToCheck = nearbyTanks != null ? nearbyTanks : playerTanks;
            for (Tank player : tanksToCheck) {
                // Skip enemy tanks when checking player collisions
                if (!player.isPlayer()) continue;
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
     * Process bullet-to-bullet collisions using spatial partitioning.
     * Reduces complexity from O(n^2) to approximately O(n) by only checking nearby bullets.
     *
     * @param bullets List of bullets to check
     * @param playerTanks Player tanks for bullet destroyed notification
     */
    public static void processBulletToBulletCollisions(List<Bullet> bullets, List<Tank> playerTanks) {
        if (bullets.size() < 2) return;

        // Use spatial grid if available for O(n) collision detection
        if (bulletGrid != null) {
            processBulletCollisionsWithGrid(bullets, playerTanks);
        } else {
            // Fallback to original O(n^2) algorithm
            processBulletCollisionsBruteForce(bullets, playerTanks);
        }
    }

    /**
     * Spatial grid-based bullet collision detection.
     * Only checks bullets in nearby cells - O(n) average case.
     * Uses pre-allocated set to avoid allocation in hot path.
     */
    private static void processBulletCollisionsWithGrid(List<Bullet> bullets, List<Tank> playerTanks) {
        // Use pre-allocated set (clear for reuse)
        bulletRemovalSet.clear();

        for (Bullet bullet1 : bullets) {
            if (bulletRemovalSet.contains(bullet1)) continue;

            // Get nearby bullets from spatial grid
            List<Bullet> nearby = bulletGrid.getNearby(bullet1.getX(), bullet1.getY());

            for (Bullet bullet2 : nearby) {
                if (bullet1 == bullet2) continue;
                if (bulletRemovalSet.contains(bullet2)) continue;

                if (bullet1.collidesWith(bullet2)) {
                    GameLogic.notifyBulletDestroyed(bullet1, playerTanks);
                    GameLogic.notifyBulletDestroyed(bullet2, playerTanks);
                    bulletRemovalSet.add(bullet1);
                    bulletRemovalSet.add(bullet2);
                    break;
                }
            }
        }

        // Remove collided bullets
        bullets.removeAll(bulletRemovalSet);
    }

    /**
     * Original O(n^2) brute force algorithm - fallback when grid not initialized.
     */
    private static void processBulletCollisionsBruteForce(List<Bullet> bullets, List<Tank> playerTanks) {
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

        // Use pooled result object to avoid allocation (reset for reuse)
        LaserCollisionResult result = laserResultPool;
        result.reset();

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
                        result.playerKilled = true;
                        result.killedPlayer = player;
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

                        // Check if it's a BOSS kill
                        if (enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                            result.isBossKill = true;
                        }

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
}
