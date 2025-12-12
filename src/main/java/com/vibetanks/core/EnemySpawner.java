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
    private static final int SPAWN_DELAY = GameConstants.SPAWN_DELAY;
    private static final int BOSS_BASE_HEALTH = GameConstants.BOSS_BASE_HEALTH;

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

        if (remaining == 1) {
            // Last enemy is the BOSS (4x bigger)
            type = Tank.EnemyType.BOSS;
        } else if (remaining <= 10) {
            // Last 10 enemies (except the very last) are HEAVY
            type = Tank.EnemyType.HEAVY;
        } else if (rand < 0.5) {
            // 50% REGULAR
            type = Tank.EnemyType.REGULAR;
        } else if (rand < 0.7) {
            // 20% FAST
            type = Tank.EnemyType.FAST;
        } else if (rand < 0.85) {
            // 15% ARMORED
            type = Tank.EnemyType.ARMORED;
        } else {
            // 15% POWER
            type = Tank.EnemyType.POWER;
        }

        // Choose spawn position - BOSS spawns in center, others randomly
        double[] spawnPos;
        if (type == Tank.EnemyType.BOSS) {
            // BOSS spawns in the center-top of the map
            spawnPos = new double[]{12 * 32, 32};
        } else {
            spawnPos = SPAWN_POSITIONS[random.nextInt(SPAWN_POSITIONS.length)];
        }

        // Get expected tank size for collision check
        int tankSize = (type == Tank.EnemyType.BOSS) ? 28 * 4 : 28;

        // Check if spawn position is clear
        boolean positionClear = true;
        for (Tank tank : enemyTanks) {
            // Check collision using the size of the tank we're about to spawn
            double otherX = tank.getX();
            double otherY = tank.getY();
            int otherSize = tank.getSize();
            if (spawnPos[0] < otherX + otherSize &&
                spawnPos[0] + tankSize > otherX &&
                spawnPos[1] < otherY + otherSize &&
                spawnPos[1] + tankSize > otherY) {
                positionClear = false;
                break;
            }
        }

        if (positionClear) {
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
        }
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
        this.map = newMap;
        this.levelNumber = newMap.getLevelNumber();
    }
}
