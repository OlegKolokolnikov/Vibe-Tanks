package com.vibetanks.network;

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

    // Lasers
    public List<LaserData> lasers = new ArrayList<>();

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

    // Enemy team speed boost (when enemy picks up CAR)
    public int enemyTeamSpeedBoostDuration;

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

    // Host game settings (synced to clients)
    public double hostPlayerSpeed = 1.0;
    public double hostEnemySpeed = 1.0;
    public double hostPlayerShootSpeed = 1.0;
    public double hostEnemyShootSpeed = 1.0;

    public static class EasterEggData implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x, y;
        public int lifetime;

        public EasterEggData(double x, double y, int lifetime) {
            this.x = x;
            this.y = y;
            this.lifetime = lifetime;
        }
    }

    public static class UFOData implements Serializable {
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
        public int row, col;
        public int framesRemaining;

        public BurningTileData(int row, int col, int framesRemaining) {
            this.row = row;
            this.col = col;
            this.framesRemaining = framesRemaining;
        }
    }

    public static class LaserData implements Serializable {
        private static final long serialVersionUID = 1L;
        public long id;
        public double startX, startY;
        public int direction;
        public boolean fromEnemy;
        public int ownerPlayerNumber;
        public int lifetime;
        public double length;

        public LaserData(long id, double startX, double startY, int direction, boolean fromEnemy, int ownerPlayerNumber, int lifetime, double length) {
            this.id = id;
            this.startX = startX;
            this.startY = startY;
            this.direction = direction;
            this.fromEnemy = fromEnemy;
            this.ownerPlayerNumber = ownerPlayerNumber;
            this.lifetime = lifetime;
            this.length = length;
        }
    }

    public static class EnemyData implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x, y;
        public int direction;
        public boolean alive;
        public int enemyType; // 0=REGULAR, 1=ARMORED, etc.
        public int health;
        public int maxHealth;
        public double tempSpeedBoost; // Temporary speed boost from team CAR pickup
        public double speedMultiplier; // Permanent speed (from CAR pickup)

        public EnemyData(double x, double y, int direction, boolean alive, int enemyType, int health, int maxHealth, double tempSpeedBoost, double speedMultiplier) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.alive = alive;
            this.enemyType = enemyType;
            this.health = health;
            this.maxHealth = maxHealth;
            this.tempSpeedBoost = tempSpeedBoost;
            this.speedMultiplier = speedMultiplier;
        }
    }

    public static class BulletData implements Serializable {
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
        public long id;
        public double x, y;
        public int type; // Ordinal of PowerUp.Type
        public int lifetime;

        public PowerUpData(long id, double x, double y, int type, int lifetime) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
            this.lifetime = lifetime;
        }
    }

    public static class TileChange implements Serializable {
        private static final long serialVersionUID = 1L;
        public int row, col;
        public int tileType; // Ordinal of GameMap.TileType

        public TileChange(int row, int col, int tileType) {
            this.row = row;
            this.col = col;
            this.tileType = tileType;
        }
    }
}
