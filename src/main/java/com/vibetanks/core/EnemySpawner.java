package com.vibetanks.core;

import com.vibetanks.util.GameLogger;
import java.util.List;
import java.util.Random;

public class EnemySpawner {
    private static final GameLogger LOG = GameLogger.getLogger(EnemySpawner.class);
    private int totalEnemies;
    private int maxOnScreen;
    private int spawnedCount;
    private int spawnCooldown;
    private int powerTanksSpawned; // Track POWER tanks for easy mode guarantees
    private static final int SPAWN_DELAY = GameConstants.SPAWN_DELAY;
    private static final int BOSS_BASE_HEALTH = GameConstants.BOSS_BASE_HEALTH;

    // POWER tank limits based on difficulty
    private static final int EASY_MODE_MIN_POWER_TANKS = 15;
    private static final int VERY_EASY_MODE_MIN_POWER_TANKS = 20;
    private static final int HARD_MODE_MAX_POWER_TANKS = 10;

    private final Random random = GameConstants.RANDOM; // Use shared Random instance
    private GameMap map;
    private int levelNumber;

    // Spawn positions (top of map)
    private static final double[][] SPAWN_POSITIONS = {
        {32, 32},
        {12 * 32, 32},
        {24 * 32, 32}
    };

    public EnemySpawner(int totalEnemies, int maxOnScreen, GameMap map) {
        this.totalEnemies = totalEnemies;
        this.maxOnScreen = maxOnScreen;
        this.spawnedCount = 0;
        this.spawnCooldown = SPAWN_DELAY;
        this.powerTanksSpawned = 0;
        this.map = map;
        this.levelNumber = map.getLevelNumber();
    }

    public void update(List<Tank> enemyTanks) {
        // Check if we can spawn more enemies
        if (spawnedCount >= totalEnemies) {
            return; // all enemies spawned
        }

        if (enemyTanks.size() >= maxOnScreen) {
            return; // max enemies on screen
        }

        spawnCooldown--;
        if (spawnCooldown <= 0) {
            spawnEnemy(enemyTanks);
            spawnCooldown = SPAWN_DELAY;
        }
    }

    private void spawnEnemy(List<Tank> enemyTanks) {
        // Determine enemy type first to check size for collision
        Tank.EnemyType type;
        int remaining = totalEnemies - spawnedCount;
        double rand = random.nextDouble();

        // Calculate POWER tank limits based on difficulty
        int minPowerTanks = 0;
        int maxPowerTanks = Integer.MAX_VALUE;
        if (GameSettings.isVeryEasyModeActive(levelNumber)) {
            minPowerTanks = VERY_EASY_MODE_MIN_POWER_TANKS;
        } else if (GameSettings.isEasyModeActive(levelNumber)) {
            minPowerTanks = EASY_MODE_MIN_POWER_TANKS;
        } else if (GameSettings.isHardModeActive()) {
            maxPowerTanks = HARD_MODE_MAX_POWER_TANKS;
        }

        // Calculate how many more POWER tanks we need
        int powerTanksNeeded = minPowerTanks - powerTanksSpawned;
        // Reserve slots for HEAVY (getHeavyThreshold) and BOSS (1)
        int nonPowerSlotsRemaining = remaining - getHeavyThreshold() - 1;
        // Check if we've hit the hard mode max
        boolean atPowerMax = powerTanksSpawned >= maxPowerTanks;

        if (remaining == 1) {
            // Last enemy is the BOSS (4x bigger)
            type = Tank.EnemyType.BOSS;
        } else if (remaining <= getHeavyThreshold()) {
            // Last N enemies (except the very last) are HEAVY
            // Single player local: 6 (5 HEAVY + 1 BOSS), otherwise: 10 (9 HEAVY + 1 BOSS)
            type = Tank.EnemyType.HEAVY;
        } else if (powerTanksNeeded > 0 && nonPowerSlotsRemaining <= powerTanksNeeded) {
            // Force POWER tank to meet minimum quota before HEAVY/BOSS phase
            type = Tank.EnemyType.POWER;
        } else if (rand < GameConstants.SPAWN_REGULAR_THRESHOLD) {
            // 50% REGULAR
            type = Tank.EnemyType.REGULAR;
        } else if (rand < GameConstants.SPAWN_FAST_THRESHOLD) {
            // 20% FAST
            type = Tank.EnemyType.FAST;
        } else if (rand < GameConstants.SPAWN_ARMORED_THRESHOLD) {
            // 15% ARMORED
            type = Tank.EnemyType.ARMORED;
        } else if (atPowerMax) {
            // Hard mode: at POWER limit, spawn ARMORED instead
            type = Tank.EnemyType.ARMORED;
        } else {
            // 15% POWER
            type = Tank.EnemyType.POWER;
        }

        // Get expected tank size for collision check
        int tankSize = (type == Tank.EnemyType.BOSS) ? 28 * 4 : 28;

        // Find a valid spawn position (check both tanks and map tiles)
        double[] spawnPos = findValidSpawnPosition(type, tankSize, enemyTanks);

        if (spawnPos != null) {
            Tank enemy = new Tank(spawnPos[0], spawnPos[1], Direction.DOWN, false, 0, type);

            // BOSS health increases with level: 12 + (level - 1)
            if (type == Tank.EnemyType.BOSS) {
                int bossHealth = BOSS_BASE_HEALTH + (levelNumber - 1);
                enemy.setHealth(bossHealth);
                enemy.setMaxHealth(bossHealth);

                // Hard mode: BOSS is 10% faster
                if (GameSettings.isHardModeActive()) {
                    enemy.setSpeedMultiplier(enemy.getSpeedMultiplier() * 1.1);
                    LOG.info("BOSS spawned with {} health (Level {}) - HARD MODE: 10% faster!", bossHealth, levelNumber);
                } else {
                    LOG.info("BOSS spawned with {} health (Level {})", bossHealth, levelNumber);
                }
            }

            // Hard mode: POWER tanks get extra armor (+1 health)
            if (type == Tank.EnemyType.POWER && GameSettings.isHardModeActive()) {
                enemy.setMaxHealth(enemy.getMaxHealth() + 1);
                enemy.setHealth(enemy.getMaxHealth());
                LOG.info("POWER tank spawned in HARD MODE - extra armor (3 shots needed)");
            }

            // Easy mode: HEAVY tanks can't destroy steel after 3 consecutive losses on this level
            if (type == Tank.EnemyType.HEAVY && GameSettings.isEasyModeActive(levelNumber)) {
                enemy.setGun(false); // Remove steel-destroying ability (bulletPower = 1)
                LOG.info("HEAVY tank spawned in EASY MODE - cannot destroy steel");
            }

            enemyTanks.add(enemy);
            spawnedCount++;

            // Track POWER tanks for easy mode guarantees
            if (type == Tank.EnemyType.POWER) {
                powerTanksSpawned++;
            }
        }
    }

    // Track BOSS spawn wait time to prevent infinite blocking
    private int bossSpawnWaitFrames = 0;
    private static final int BOSS_MAX_WAIT_FRAMES = 180; // ~3 seconds at 60fps

    /**
     * Find a valid spawn position that doesn't collide with tanks or map tiles.
     * Returns null if no valid position found.
     */
    private double[] findValidSpawnPosition(Tank.EnemyType type, int tankSize, List<Tank> enemyTanks) {
        // BOSS prefers center, but has fallback positions to prevent infinite blocking
        if (type == Tank.EnemyType.BOSS) {
            // Try center first (preferred for BOSS)
            double[] centerPos = {12 * 32, 32};
            if (isSpawnPositionValid(centerPos, tankSize, enemyTanks)) {
                bossSpawnWaitFrames = 0;
                return centerPos;
            }

            // Increment wait counter
            bossSpawnWaitFrames++;

            // If waited too long, try alternative positions
            if (bossSpawnWaitFrames > BOSS_MAX_WAIT_FRAMES) {
                LOG.info("BOSS spawn blocked too long, trying alternative positions...");

                // Alternative BOSS spawn positions (left and right of center)
                double[][] bossAlternatives = {
                    {6 * 32, 32},   // Left side
                    {18 * 32, 32},  // Right side
                    {12 * 32, 4 * 32}, // Center but lower
                };

                for (double[] altPos : bossAlternatives) {
                    if (isSpawnPositionValid(altPos, tankSize, enemyTanks)) {
                        LOG.info("BOSS spawning at alternative position: ({}, {})", altPos[0], altPos[1]);
                        bossSpawnWaitFrames = 0;
                        return altPos;
                    }
                }

                // Force spawn at center anyway if all else fails (enemies will be pushed)
                if (bossSpawnWaitFrames > BOSS_MAX_WAIT_FRAMES * 2) {
                    LOG.warn("BOSS force-spawning at center after extended wait");
                    bossSpawnWaitFrames = 0;
                    return centerPos;
                }
            }

            LOG.debug("BOSS spawn blocked at center, waiting... (frame {})", bossSpawnWaitFrames);
            return null;
        }

        // For normal tanks, try positions in random order
        int startIndex = random.nextInt(SPAWN_POSITIONS.length);
        for (int i = 0; i < SPAWN_POSITIONS.length; i++) {
            int index = (startIndex + i) % SPAWN_POSITIONS.length;
            double[] pos = SPAWN_POSITIONS[index];
            if (isSpawnPositionValid(pos, tankSize, enemyTanks)) {
                return pos;
            }
        }

        // No valid position found
        return null;
    }

    /**
     * Check if a spawn position is valid (no collision with tanks or map tiles).
     */
    private boolean isSpawnPositionValid(double[] pos, int tankSize, List<Tank> enemyTanks) {
        // Check collision with other tanks
        for (Tank tank : enemyTanks) {
            if (Collider.checkSquare(pos[0], pos[1], tankSize,
                                     tank.getX(), tank.getY(), tank.getSize())) {
                return false;
            }
        }

        // Check collision with map tiles
        if (map.checkTankCollision(pos[0], pos[1], tankSize)) {
            return false;
        }

        return true;
    }

    public int getRemainingEnemies() {
        return totalEnemies - spawnedCount;
    }

    public void setRemainingEnemies(int remaining) {
        this.spawnedCount = totalEnemies - remaining;
    }

    public boolean allEnemiesSpawned() {
        return spawnedCount >= totalEnemies;
    }

    /**
     * Reset the spawner for a new level or game restart.
     */
    public void reset(int newTotalEnemies, GameMap newMap) {
        this.totalEnemies = newTotalEnemies;
        this.spawnedCount = 0;
        this.spawnCooldown = SPAWN_DELAY;
        this.powerTanksSpawned = 0;
        this.bossSpawnWaitFrames = 0;
        this.map = newMap;
        this.levelNumber = newMap.getLevelNumber();
    }

    /**
     * Get the threshold for HEAVY tank spawning.
     * In single player local games: 6 (5 HEAVY + 1 BOSS)
     * In multiplayer/network games: 10 (9 HEAVY + 1 BOSS)
     */
    private int getHeavyThreshold() {
        return GameSettings.isSinglePlayerLocalGame() ? 6 : 10;
    }
}
