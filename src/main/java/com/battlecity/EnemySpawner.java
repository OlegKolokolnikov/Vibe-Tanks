package com.battlecity;

import java.util.List;
import java.util.Random;

public class EnemySpawner {
    private int totalEnemies;
    private int maxOnScreen;
    private int spawnedCount;
    private int spawnCooldown;
    private static final int SPAWN_DELAY = 50; // ~0.8 seconds

    private Random random;
    private GameMap map;

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
        this.random = new Random();
        this.map = map;
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
}
