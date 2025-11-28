package com.battlecity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    // Player 1 data
    public double p1X, p1Y;
    public int p1Direction; // 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT
    public int p1Lives;
    public boolean p1Alive;
    public boolean p1HasShield;
    public boolean p1HasShip;

    // Player 2 data
    public double p2X, p2Y;
    public int p2Direction;
    public int p2Lives;
    public boolean p2Alive;
    public boolean p2HasShield;
    public boolean p2HasShip;

    // Player 3 data
    public double p3X, p3Y;
    public int p3Direction;
    public int p3Lives;
    public boolean p3Alive;
    public boolean p3HasShield;
    public boolean p3HasShip;

    // Player 4 data
    public double p4X, p4Y;
    public int p4Direction;
    public int p4Lives;
    public boolean p4Alive;
    public boolean p4HasShield;
    public boolean p4HasShip;

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
    public int connectedPlayers; // Number of connected players (1-4)

    // Base
    public boolean baseAlive;

    // Map changes (for destructible tiles)
    public List<TileChange> tileChanges = new ArrayList<>();

    // Full map state for syncing (stores tile type ordinals)
    public int[][] mapTiles;

    // Player kills count
    public int p1Kills, p2Kills, p3Kills, p4Kills;

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
        public long id;
        public double x, y;
        public int direction;
        public boolean fromEnemy;
        public int power;
        public boolean canDestroyTrees;
        public int ownerPlayerNumber;

        public BulletData(long id, double x, double y, int direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.fromEnemy = fromEnemy;
            this.power = power;
            this.canDestroyTrees = canDestroyTrees;
            this.ownerPlayerNumber = ownerPlayerNumber;
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
