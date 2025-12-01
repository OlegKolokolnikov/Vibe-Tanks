package com.vibetanks;

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
    public boolean p1HasPauseShield;
    public boolean p1HasShip;
    public boolean p1HasGun;
    public int p1StarCount;
    public int p1CarCount;
    public boolean p1HasSaw;
    public int p1MachinegunCount;

    // Player 2 data
    public double p2X, p2Y;
    public int p2Direction;
    public int p2Lives;
    public boolean p2Alive;
    public boolean p2HasShield;
    public boolean p2HasPauseShield;
    public boolean p2HasShip;
    public boolean p2HasGun;
    public int p2StarCount;
    public int p2CarCount;
    public boolean p2HasSaw;
    public int p2MachinegunCount;

    // Player 3 data
    public double p3X, p3Y;
    public int p3Direction;
    public int p3Lives;
    public boolean p3Alive;
    public boolean p3HasShield;
    public boolean p3HasPauseShield;
    public boolean p3HasShip;
    public boolean p3HasGun;
    public int p3StarCount;
    public int p3CarCount;
    public boolean p3HasSaw;
    public int p3MachinegunCount;

    // Player 4 data
    public double p4X, p4Y;
    public int p4Direction;
    public int p4Lives;
    public boolean p4Alive;
    public boolean p4HasShield;
    public boolean p4HasPauseShield;
    public boolean p4HasShip;
    public boolean p4HasGun;
    public int p4StarCount;
    public int p4CarCount;
    public boolean p4HasSaw;
    public int p4MachinegunCount;

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
    public int levelNumber; // Current level number for sync

    // Freeze state
    public int enemyFreezeDuration;
    public int playerFreezeDuration;

    // Base
    public boolean baseAlive;
    public boolean baseShowFlag;
    public double baseFlagHeight;
    public boolean baseShowVictoryFlag;
    public double baseVictoryFlagHeight;
    public boolean baseEasterEggMode;

    // Map changes (for destructible tiles)
    public List<TileChange> tileChanges = new ArrayList<>();

    // Full map state for syncing (stores tile type ordinals)
    public int[][] mapTiles;

    // Burning tiles (row*1000+col -> frames remaining)
    public List<BurningTileData> burningTiles = new ArrayList<>();

    // Player kills count
    public int p1Kills, p2Kills, p3Kills, p4Kills;

    // Player scores
    public int p1Score, p2Score, p3Score, p4Score;

    // Dancing characters for game over animation (when base is destroyed)
    public List<DancingCharacterData> dancingCharacters = new ArrayList<>();
    public boolean dancingInitialized;

    // Dancing girls for victory animation
    public List<DancingGirlData> victoryDancingGirls = new ArrayList<>();
    public boolean victoryDancingInitialized;

    // UFO bonus enemy
    public UFOData ufoData;
    public int ufoLostMessageTimer; // Timer for "Lost it!" message

    public static class UFOData implements Serializable {
        public double x, y;
        public double dx, dy;
        public boolean alive;
        public int health;
        public int lifetime;
        public boolean movingRight;

        public UFOData(double x, double y, double dx, double dy, boolean alive, int health, int lifetime, boolean movingRight) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.alive = alive;
            this.health = health;
            this.lifetime = lifetime;
            this.movingRight = movingRight;
        }
    }

    public static class DancingCharacterData implements Serializable {
        public double x, y;
        public boolean isAlien;
        public int animFrame;
        public int danceStyle;
        public int colorIndex; // Index for color arrays

        public DancingCharacterData(double x, double y, boolean isAlien, int animFrame, int danceStyle, int colorIndex) {
            this.x = x;
            this.y = y;
            this.isAlien = isAlien;
            this.animFrame = animFrame;
            this.danceStyle = danceStyle;
            this.colorIndex = colorIndex;
        }
    }

    public static class DancingGirlData implements Serializable {
        public double x, y;
        public int animFrame;
        public int danceStyle;
        public int dressColorIndex;
        public int hairColorIndex;

        public DancingGirlData(double x, double y, int animFrame, int danceStyle, int dressColorIndex, int hairColorIndex) {
            this.x = x;
            this.y = y;
            this.animFrame = animFrame;
            this.danceStyle = danceStyle;
            this.dressColorIndex = dressColorIndex;
            this.hairColorIndex = hairColorIndex;
        }
    }

    public static class BurningTileData implements Serializable {
        public int row, col;
        public int framesRemaining;

        public BurningTileData(int row, int col, int framesRemaining) {
            this.row = row;
            this.col = col;
            this.framesRemaining = framesRemaining;
        }
    }

    public static class EnemyData implements Serializable {
        public double x, y;
        public int direction;
        public boolean alive;
        public int enemyType; // 0=REGULAR, 1=ARMORED, etc.
        public int health;
        public int maxHealth;

        public EnemyData(double x, double y, int direction, boolean alive, int enemyType, int health, int maxHealth) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.alive = alive;
            this.enemyType = enemyType;
            this.health = health;
            this.maxHealth = maxHealth;
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
        public int size; // Bullet size (bigger for BOSS)

        public BulletData(long id, double x, double y, int direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber, int size) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.fromEnemy = fromEnemy;
            this.power = power;
            this.canDestroyTrees = canDestroyTrees;
            this.ownerPlayerNumber = ownerPlayerNumber;
            this.size = size;
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
