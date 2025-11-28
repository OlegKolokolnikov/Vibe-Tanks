package com.battlecity;

import java.util.List;
import java.util.Random;

public class EnemySpawner {
    private int totalEnemies;
    private int maxOnScreen;
    private int spawnedCount;
    private int spawnCooldown;
    private static final int SPAWN_DELAY = 90; // 1.5 seconds

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
        // Choose random spawn position
        double[] spawnPos = SPAWN_POSITIONS[random.nextInt(SPAWN_POSITIONS.length)];

        // Check if spawn position is clear
        boolean positionClear = true;
        for (Tank tank : enemyTanks) {
            if (tank.collidesWith(spawnPos[0], spawnPos[1], tank.getSize())) {
                positionClear = false;
                break;
            }
        }

        if (positionClear) {
            // Determine enemy type based on progression and randomness
            Tank.EnemyType type;
            int remaining = totalEnemies - spawnedCount;
            double rand = random.nextDouble();

            if (remaining <= 10) {
                // Last 10 enemies are HEAVY
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
