package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.util.GameLogger;

import java.util.Iterator;
import java.util.List;

/**
 * Shared game logic used by both Game (client) and ServerGameState (dedicated server).
 * Eliminates code duplication for collision detection, entity updates, and game rules.
 */
public class GameLogic {
    private static final GameLogger LOG = GameLogger.getLogger(GameLogic.class);

    /**
     * Result of processing bullets, containing information about what happened.
     */
    public static class BulletProcessResult {
        public boolean enemyKilled = false;
        public int killerPlayerNumber = -1;
        public Tank.EnemyType killedEnemyType = null;
        public boolean shouldDropPowerUp = false;
        public boolean baseDestroyed = false;
        public boolean playerKilled = false;
        public int playerKilledIndex = -1;
    }

    /**
     * Result of processing lasers.
     */
    public static class LaserProcessResult {
        public boolean enemyKilled = false;
        public int killerPlayerNumber = -1;
        public Tank.EnemyType killedEnemyType = null;
        public boolean baseDestroyed = false;
        public boolean ufoDestroyed = false;
    }

    /**
     * Result of power-up collection.
     */
    public static class PowerUpCollectResult {
        public boolean collected = false;
        public boolean collectedByPlayer = false;
        public boolean collectedByEnemy = false;
        public int collectorPlayerIndex = -1;
        public Tank collectorEnemy = null;
        public PowerUp.Type type = null;
    }

    /**
     * Process bullet collisions with map, bounds, tanks, and base.
     *
     * @param bullet The bullet to process
     * @param gameMap The game map
     * @param enemyTanks List of enemy tanks
     * @param playerTanks List of player tanks
     * @param base The base
     * @param mapPixelSize Size of the map in pixels
     * @param soundManager Sound manager (can be null for server)
     * @return Result containing what happened during processing
     */
    public static BulletProcessResult processBulletCollisions(
            Bullet bullet, GameMap gameMap, List<Tank> enemyTanks, List<Tank> playerTanks,
            Base base, int mapPixelSize, SoundManager soundManager) {

        BulletProcessResult result = new BulletProcessResult();

        // Check map collision
        if (gameMap.checkBulletCollision(bullet, soundManager)) {
            return result; // Bullet hit wall, remove it
        }

        // Check out of bounds with wraparound
        if (bullet.isOutOfBounds(mapPixelSize, mapPixelSize)) {
            if (!bullet.handleWraparound(gameMap, mapPixelSize, mapPixelSize)) {
                return result; // Bullet left map, remove it
            }
        }

        // Player bullets hit enemies
        if (!bullet.isFromEnemy()) {
            for (Tank enemy : enemyTanks) {
                if (enemy.isAlive() && bullet.collidesWith(enemy)) {
                    boolean dropPowerUp = enemy.damage();

                    // Check for power-up drop (POWER type drops on hit, others 30% chance on death)
                    if (dropPowerUp || (!enemy.isAlive() && GameConstants.RANDOM.nextDouble() < 0.3)) {
                        result.shouldDropPowerUp = true;
                    }

                    if (!enemy.isAlive()) {
                        result.enemyKilled = true;
                        result.killerPlayerNumber = bullet.getOwnerPlayerNumber();
                        result.killedEnemyType = enemy.getEnemyType();
                    }
                    return result;
                }
            }
        } else {
            // Enemy bullets hit players
            for (int i = 0; i < playerTanks.size(); i++) {
                Tank player = playerTanks.get(i);
                if (player.isAlive() && !player.hasShield() && !player.hasPauseShield()
                        && bullet.collidesWith(player)) {
                    player.damage();
                    if (!player.isAlive()) {
                        result.playerKilled = true;
                        result.playerKilledIndex = i;
                    }
                    return result;
                }
            }
        }

        // Base collision (all bullets can hit base)
        if (bullet.collidesWith(base) && base.isAlive()) {
            base.destroy();
            result.baseDestroyed = true;
        }

        return result;
    }

    /**
     * Process laser collisions with tanks, UFO, and base.
     *
     * @param laser The laser to process
     * @param enemyTanks List of enemy tanks
     * @param ufo The UFO (can be null)
     * @param base The base
     * @return Result containing what happened
     */
    public static LaserProcessResult processLaserCollisions(
            Laser laser, List<Tank> enemyTanks, UFO ufo, Base base) {
        return processLaserCollisions(laser, enemyTanks, ufo, base, null);
    }

    /**
     * Process laser collisions with tanks, UFO, and base, with steel blocking check.
     *
     * @param laser The laser to process
     * @param enemyTanks List of enemy tanks
     * @param ufo The UFO (can be null)
     * @param base The base
     * @param gameMap The game map (for steel blocking check, can be null)
     * @return Result containing what happened
     */
    public static LaserProcessResult processLaserCollisions(
            Laser laser, List<Tank> enemyTanks, UFO ufo, Base base, GameMap gameMap) {

        LaserProcessResult result = new LaserProcessResult();

        if (laser.isFromEnemy()) {
            return result; // Enemy lasers not implemented yet
        }

        // Player laser hits enemies
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && laser.collidesWith(enemy)) {
                // Deal 3 damage
                for (int i = 0; i < 3 && enemy.isAlive(); i++) {
                    enemy.damage();
                }
                if (!enemy.isAlive()) {
                    result.enemyKilled = true;
                    result.killerPlayerNumber = laser.getOwnerPlayerNumber();
                    result.killedEnemyType = enemy.getEnemyType();
                }
            }
        }

        // UFO hit
        if (ufo != null && ufo.isAlive() && laser.collidesWithUFO(ufo)) {
            for (int i = 0; i < 3 && ufo.isAlive(); i++) {
                boolean destroyed = ufo.damage();
                if (destroyed) {
                    result.ufoDestroyed = true;
                    break;
                }
            }
        }

        // Base hit - check steel blocking if map provided
        if (laser.collidesWithBase(base, gameMap) && base.isAlive()) {
            base.destroy();
            result.baseDestroyed = true;
        }

        return result;
    }

    /**
     * Check if a power-up collides with any tank and return collection result.
     *
     * @param powerUp The power-up to check
     * @param playerTanks List of player tanks
     * @param enemyTanks List of enemy tanks
     * @return Result indicating who collected the power-up
     */
    public static PowerUpCollectResult checkPowerUpCollection(
            PowerUp powerUp, List<Tank> playerTanks, List<Tank> enemyTanks) {

        PowerUpCollectResult result = new PowerUpCollectResult();
        result.type = powerUp.getType();

        // Check player collection
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive() && powerUp.collidesWith(player)) {
                result.collected = true;
                result.collectedByPlayer = true;
                result.collectorPlayerIndex = i;
                return result;
            }
        }

        // Check enemy collection
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && powerUp.collidesWith(enemy)) {
                result.collected = true;
                result.collectedByEnemy = true;
                result.collectorEnemy = enemy;
                return result;
            }
        }

        return result;
    }

    /**
     * Apply power-up effect to a player tank.
     * Handles special power-ups that affect game state (SHOVEL, FREEZE, BOMB).
     *
     * @param powerUp The power-up
     * @param player The player tank
     * @return The power-up type for special handling by caller
     */
    public static PowerUp.Type applyPlayerPowerUp(PowerUp powerUp, Tank player) {
        PowerUp.Type type = powerUp.getType();

        switch (type) {
            case SHOVEL:
            case FREEZE:
            case BOMB:
                // These require game-level handling, just return the type
                break;
            default:
                // Standard power-ups apply directly to tank
                powerUp.applyEffect(player);
                break;
        }

        return type;
    }

    /**
     * Apply power-up effect when collected by an enemy tank.
     *
     * @param powerUp The power-up
     * @param enemy The enemy tank
     * @return The power-up type for special handling by caller
     */
    public static PowerUp.Type applyEnemyPowerUp(PowerUp powerUp, Tank enemy) {
        PowerUp.Type type = powerUp.getType();

        switch (type) {
            case SHOVEL:
            case FREEZE:
            case BOMB:
            case CAR:
                // These require game-level handling
                break;
            case SHIELD:
                // Enemies get an extra life instead of shield
                enemy.applyTank();
                return type;
            default:
                powerUp.applyEffect(enemy);
                break;
        }

        return type;
    }

    /**
     * Find a random empty position for spawning a power-up.
     *
     * @param gameMap The game map
     * @param tileSize Size of each tile in pixels
     * @return Array containing [x, y] position in pixels
     */
    public static double[] findPowerUpSpawnPosition(GameMap gameMap, int tileSize) {
        double[] result = new double[2];
        findPowerUpSpawnPosition(gameMap, tileSize, result);
        return result;
    }

    /**
     * Find a random empty position for spawning a power-up (allocation-free version).
     *
     * @param gameMap The game map
     * @param tileSize Size of each tile in pixels
     * @param result Array to store [x, y] position in pixels (must be length >= 2)
     */
    public static void findPowerUpSpawnPosition(GameMap gameMap, int tileSize, double[] result) {
        int maxAttempts = 100;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Random position within playable area (avoiding borders)
            int col = 2 + GameConstants.RANDOM.nextInt(22); // 2 to 23
            int row = 2 + GameConstants.RANDOM.nextInt(22);

            // Check if position is clear (only spawn on empty tiles)
            GameMap.TileType tile = gameMap.getTile(row, col);
            if (tile == GameMap.TileType.EMPTY) {
                result[0] = col * tileSize;
                result[1] = row * tileSize;
                return;
            }
        }

        // Fallback to center if no valid position found
        result[0] = 13 * tileSize;
        result[1] = 13 * tileSize;
    }

    /**
     * Check if easter egg collides with any tank.
     *
     * @param easterEgg The easter egg
     * @param playerTanks List of player tanks
     * @param enemyTanks List of enemy tanks
     * @return 0 = no collision, positive = player index + 1, negative = enemy collected
     */
    public static int checkEasterEggCollection(EasterEgg easterEgg, List<Tank> playerTanks, List<Tank> enemyTanks) {
        // Check player collection
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive() && easterEgg.collidesWith(player)) {
                return i + 1; // Return player index + 1 (positive)
            }
        }

        // Check enemy collection
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && easterEgg.collidesWith(enemy)) {
                return -1; // Enemy collected
            }
        }

        return 0; // No collision
    }

    /**
     * Transform enemies when easter egg is collected.
     *
     * @param enemyTanks List of enemy tanks
     * @param collectedByPlayer True if player collected, false if enemy collected
     */
    public static void applyEasterEggEffect(List<Tank> enemyTanks, boolean collectedByPlayer) {
        Tank.EnemyType newType = collectedByPlayer ? Tank.EnemyType.POWER : Tank.EnemyType.HEAVY;

        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && enemy.getEnemyType() != Tank.EnemyType.BOSS) {
                enemy.setEnemyType(newType);
            }
        }
    }

    /**
     * Notify bullet owner that their bullet was destroyed.
     * Allows the tank to shoot again immediately.
     *
     * @param bullet The destroyed bullet
     * @param playerTanks List of player tanks
     */
    public static void notifyBulletDestroyed(Bullet bullet, List<Tank> playerTanks) {
        if (!bullet.isFromEnemy()) {
            int playerNum = bullet.getOwnerPlayerNumber();
            if (playerNum >= 1 && playerNum <= playerTanks.size()) {
                playerTanks.get(playerNum - 1).bulletDestroyed();
            }
        }
    }

    /**
     * Remove dead enemies from the list.
     *
     * @param enemyTanks List of enemy tanks
     */
    public static void removeDeadEnemies(List<Tank> enemyTanks) {
        enemyTanks.removeIf(e -> !e.isAlive());
    }

    /**
     * Check if game is over (base destroyed or all players dead).
     *
     * @param base The base
     * @param playerTanks List of player tanks
     * @return true if game over
     */
    public static boolean checkGameOver(Base base, List<Tank> playerTanks) {
        if (!base.isAlive()) {
            return true;
        }

        for (Tank player : playerTanks) {
            if (player.isAlive() || player.getLives() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if victory condition is met (all enemies spawned and defeated).
     *
     * @param enemySpawner The enemy spawner
     * @param enemyTanks List of enemy tanks
     * @return true if victory
     */
    public static boolean checkVictory(EnemySpawner enemySpawner, List<Tank> enemyTanks) {
        return enemySpawner.allEnemiesSpawned() && enemyTanks.isEmpty();
    }

    /**
     * Get score for killing an enemy.
     * Delegates to GameConstants.
     *
     * @param enemyType The type of enemy killed
     * @return Points awarded
     */
    public static int getScoreForKill(Tank.EnemyType enemyType) {
        return GameConstants.getScoreForEnemyType(enemyType);
    }

    /**
     * Result of adding score to a player.
     */
    public static class ScoreResult {
        public int oldScore;
        public int newScore;
        public int livesAwarded = 0;
    }

    /**
     * Add points to a player's score and calculate extra lives for crossing 100-point thresholds.
     *
     * @param playerIndex Index of the player (0-3)
     * @param points Points to add
     * @param playerScores Array of player total scores
     * @param playerLevelScores Array of player level scores
     * @param playerTanks List of player tanks (for awarding lives)
     * @return ScoreResult with old/new score and lives awarded
     */
    public static ScoreResult addScore(int playerIndex, int points, int[] playerScores,
                                        int[] playerLevelScores, List<Tank> playerTanks) {
        ScoreResult result = new ScoreResult();
        if (playerIndex < 0 || playerIndex >= 4) return result;

        result.oldScore = playerScores[playerIndex];
        result.newScore = result.oldScore + points;
        playerScores[playerIndex] = result.newScore;
        playerLevelScores[playerIndex] += points;

        LOG.debug("SCORE: Player {} score: {} -> {} (+{})", playerIndex + 1, result.oldScore, result.newScore, points);

        // Check if crossed a 100-point threshold (e.g., 0->100, 95->105, 199->201)
        int oldHundreds = result.oldScore / 100;
        int newHundreds = result.newScore / 100;

        if (newHundreds > oldHundreds && playerIndex < playerTanks.size()) {
            Tank player = playerTanks.get(playerIndex);
            result.livesAwarded = newHundreds - oldHundreds;
            for (int i = 0; i < result.livesAwarded; i++) {
                player.addLife();
            }
            LOG.info("Player {} earned {} extra lives for reaching {} points",
                playerIndex + 1, result.livesAwarded, newHundreds * 100);
        }

        return result;
    }

    /**
     * Result of tank overlap resolution.
     */
    public static class TankOverlapResult {
        public boolean tank1Killed = false;
        public boolean tank2Killed = false;
        public Tank killedTank1 = null;
        public Tank killedTank2 = null;
    }

    /**
     * Push apart overlapping tanks to prevent them from getting stuck.
     * Also handles BOSS tank contact kills.
     *
     * @param allTanks Combined list of player and enemy tanks
     * @param gameMap Game map for collision checking
     * @return Result indicating any tanks killed by BOSS contact
     */
    public static TankOverlapResult resolveOverlappingTanks(List<Tank> allTanks, GameMap gameMap) {
        TankOverlapResult result = new TankOverlapResult();
        final double PUSH_FORCE = 3.0; // Pixels to push per frame
        final double MIN_GAP = 4.0; // Minimum gap to maintain between tanks

        for (int i = 0; i < allTanks.size(); i++) {
            Tank tank1 = allTanks.get(i);
            if (!tank1.isAlive()) continue;

            for (int j = i + 1; j < allTanks.size(); j++) {
                Tank tank2 = allTanks.get(j);
                if (!tank2.isAlive()) continue;

                // Calculate center positions (getX/getY return top-left corner)
                double center1X = tank1.getX() + tank1.getSize() / 2.0;
                double center1Y = tank1.getY() + tank1.getSize() / 2.0;
                double center2X = tank2.getX() + tank2.getSize() / 2.0;
                double center2Y = tank2.getY() + tank2.getSize() / 2.0;

                // Calculate distance between tank centers
                double dx = center2X - center1X;
                double dy = center2Y - center1Y;

                // Required separation (half sizes + gap)
                double requiredSepX = (tank1.getSize() + tank2.getSize()) / 2.0 + MIN_GAP;
                double requiredSepY = (tank1.getSize() + tank2.getSize()) / 2.0 + MIN_GAP;

                // Calculate overlap in each axis
                double overlapX = requiredSepX - Math.abs(dx);
                double overlapY = requiredSepY - Math.abs(dy);

                // If overlapping or too close in both dimensions
                if (overlapX > 0 && overlapY > 0) {
                    boolean tank1IsBoss = tank1.getEnemyType() == Tank.EnemyType.BOSS;
                    boolean tank2IsBoss = tank2.getEnemyType() == Tank.EnemyType.BOSS;

                    // BOSS tank kills any tank it touches (except other BOSS)
                    if (tank1IsBoss && !tank2IsBoss) {
                        tank2.instantKill();
                        result.tank2Killed = true;
                        result.killedTank2 = tank2;
                        continue; // Don't push, tank is dead
                    }
                    if (tank2IsBoss && !tank1IsBoss) {
                        tank1.instantKill();
                        result.tank1Killed = true;
                        result.killedTank1 = tank1;
                        continue; // Don't push, tank is dead
                    }

                    // Push along the axis with LESS overlap (faster separation)
                    double pushX = 0;
                    double pushY = 0;

                    if (overlapX < overlapY) {
                        // Push horizontally
                        pushX = (dx >= 0 ? 1 : -1) * PUSH_FORCE;
                    } else {
                        // Push vertically
                        pushY = (dy >= 0 ? 1 : -1) * PUSH_FORCE;
                    }

                    // If tanks are exactly aligned, add small perpendicular push
                    if (Math.abs(dx) < 1 && Math.abs(dy) < 1) {
                        pushX = (GameConstants.RANDOM.nextDouble() > 0.5 ? 1 : -1) * PUSH_FORCE;
                        pushY = (GameConstants.RANDOM.nextDouble() > 0.5 ? 1 : -1) * PUSH_FORCE;
                    }

                    double tank1Push = tank2IsBoss ? 1.0 : 0.5;
                    double tank2Push = tank1IsBoss ? 1.0 : 0.5;

                    // Check if new positions are valid before applying
                    double newX1 = tank1.getX() - pushX * tank1Push;
                    double newY1 = tank1.getY() - pushY * tank1Push;
                    double newX2 = tank2.getX() + pushX * tank2Push;
                    double newY2 = tank2.getY() + pushY * tank2Push;

                    // Apply push only if the new position doesn't collide with walls
                    if (!gameMap.checkTankCollision(newX1, newY1, tank1.getSize(), tank1.hasShip())) {
                        tank1.setPosition(newX1, newY1);
                    }
                    if (!gameMap.checkTankCollision(newX2, newY2, tank2.getSize(), tank2.hasShip())) {
                        tank2.setPosition(newX2, newY2);
                    }
                }
            }
        }

        return result;
    }
}
