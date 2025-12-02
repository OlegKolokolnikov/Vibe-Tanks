package com.vibetanks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 2L; // Incremented for new format

    // Centralized player data array (up to 4 players)
    public PlayerData[] players = new PlayerData[4];

    public GameState() {
        for (int i = 0; i < 4; i++) {
            players[i] = new PlayerData(i + 1);
        }
    }

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

    // Boss kill tracking for victory screen
    public int bossKillerPlayerIndex = -1; // -1 = not killed, 0-3 = player index
    public int bossKillPowerUpReward = -1; // PowerUp.Type ordinal, -1 = none

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

    // Dancing characters for game over animation (when base is destroyed)
    public List<DancingCharacterData> dancingCharacters = new ArrayList<>();
    public boolean dancingInitialized;

    // Dancing girls for victory animation
    public List<DancingGirlData> victoryDancingGirls = new ArrayList<>();
    public boolean victoryDancingInitialized;

    // UFO bonus enemy
    public UFOData ufoData;
    public int ufoLostMessageTimer; // Timer for "Lost it!" message
    public int ufoKilledMessageTimer; // Timer for "Zed is dead!" message

    // Easter egg collectible
    public EasterEggData easterEggData;

    public static class EasterEggData implements Serializable {
        public double x, y;
        public int lifetime;

        public EasterEggData(double x, double y, int lifetime) {
            this.x = x;
            this.y = y;
            this.lifetime = lifetime;
        }
    }

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
