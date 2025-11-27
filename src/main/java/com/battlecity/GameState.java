package com.battlecity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    // Player 1 (host) data
    public double p1X, p1Y;
    public int p1Direction; // 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT
    public int p1Lives;
    public boolean p1Alive;
    public boolean p1HasShield;

    // Player 2 (client) data
    public double p2X, p2Y;
    public int p2Direction;
    public int p2Lives;
    public boolean p2Alive;
    public boolean p2HasShield;

    // Enemy tanks
    public List<EnemyData> enemies = new ArrayList<>();

    // Bullets
    public List<BulletData> bullets = new ArrayList<>();

    // Power-ups
    public List<PowerUpData> powerUps = new ArrayList<>();

    // Game state
    public boolean gameOver;
    public boolean victory;
    public int remainingEnemies;

    // Base
    public boolean baseAlive;

    // Map changes (for destructible tiles)
    public List<TileChange> tileChanges = new ArrayList<>();

    public static class EnemyData implements Serializable {
        public double x, y;
        public int direction;
        public boolean alive;
        public int enemyType; // 0=REGULAR, 1=ARMORED, etc.

        public EnemyData(double x, double y, int direction, boolean alive, int enemyType) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.alive = alive;
            this.enemyType = enemyType;
        }
    }

    public static class BulletData implements Serializable {
        public double x, y;
        public int direction;
        public boolean fromEnemy;
        public int power;
        public boolean canDestroyTrees;

        public BulletData(double x, double y, int direction, boolean fromEnemy, int power, boolean canDestroyTrees) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.fromEnemy = fromEnemy;
            this.power = power;
            this.canDestroyTrees = canDestroyTrees;
        }
    }

    public static class PowerUpData implements Serializable {
        public double x, y;
        public int type; // Ordinal of PowerUp.Type

        public PowerUpData(double x, double y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    public static class TileChange implements Serializable {
        public int row, col;
        public int tileType; // Ordinal of GameMap.TileType

        public TileChange(int row, int col, int tileType) {
            this.row = row;
            this.col = col;
            this.tileType = tileType;
        }
    }
}
