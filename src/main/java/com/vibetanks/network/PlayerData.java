package com.vibetanks.network;

import com.vibetanks.core.Direction;
import com.vibetanks.core.Tank;
import com.vibetanks.util.GameLogger;

import java.io.Serializable;

/**
 * Centralized entity for player data - used for display and network sync.
 * Single source of truth for all player information.
 */
public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final GameLogger LOG = GameLogger.getLogger(PlayerData.class);

    // Position and movement
    public double x, y;
    public int direction; // 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT

    // Status
    public int lives;
    public boolean alive;
    public int respawnTimer; // Frames until respawn, 0 = not waiting
    public double pendingRespawnX, pendingRespawnY;
    public int kills;
    public int score; // Total score
    public int levelScore; // Score for current level

    // Kills per enemy type: [REGULAR, ARMORED, FAST, POWER, HEAVY, BOSS]
    public int[] killsByType = new int[6];

    // Power-ups
    public boolean hasShield;
    public int shieldDuration; // Shield duration in frames
    public boolean hasPauseShield;
    public boolean hasShip;
    public boolean hasGun;
    public int starCount;
    public int carCount;
    public boolean hasSaw;
    public int machinegunCount;
    public int laserDuration; // LASER power-up duration in frames

    // Identity
    public String nickname;
    public int playerNumber; // 1-4
    public boolean connected = true; // Whether player is still connected (for network sync)

    public PlayerData() {
        this.lives = 3;
        this.alive = true;
        this.direction = 0;
    }

    public PlayerData(int playerNumber) {
        this();
        this.playerNumber = playerNumber;
    }

    /**
     * Copy data from a Tank object
     */
    public void copyFromTank(Tank tank, int kills, int score, int levelScore, String nickname, int[] killsByType) {
        this.x = tank.getX();
        this.y = tank.getY();
        this.direction = tank.getDirection().ordinal();
        this.lives = tank.getLives();
        this.alive = tank.isAlive();
        this.respawnTimer = tank.getRespawnTimer();
        this.pendingRespawnX = tank.getPendingRespawnX();
        this.pendingRespawnY = tank.getPendingRespawnY();
        this.hasShield = tank.hasShield();
        this.shieldDuration = tank.getShieldDuration();
        this.hasPauseShield = tank.hasPauseShield();
        this.hasShip = tank.hasShip();
        if (this.hasShip) {
            LOG.debug("copyFromTank: Player {} hasShip=true", tank.getPlayerNumber());
        }
        this.hasGun = tank.hasGun();
        this.starCount = tank.getStarCount();
        this.carCount = tank.getCarCount();
        this.hasSaw = tank.hasSaw();
        this.machinegunCount = tank.getMachinegunCount();
        this.laserDuration = tank.getLaserDuration();
        this.kills = kills;
        this.score = score;
        this.levelScore = levelScore;
        this.nickname = nickname;
        if (killsByType != null) {
            System.arraycopy(killsByType, 0, this.killsByType, 0, Math.min(6, killsByType.length));
        }
    }

    /**
     * Apply data to a Tank object
     */
    public void applyToTank(Tank tank, boolean skipPosition) {
        tank.setLives(this.lives);
        tank.setAlive(this.alive);
        // Sync respawn state including pending position
        if (this.respawnTimer > 0) {
            tank.setPendingRespawn(this.pendingRespawnX, this.pendingRespawnY, this.respawnTimer);
        } else {
            tank.setRespawnTimer(0);
        }
        if (this.alive && !skipPosition) {
            tank.setPosition(this.x, this.y);
            tank.setDirection(Direction.values()[this.direction]);
        }
        tank.setShieldWithDuration(this.hasShield, this.shieldDuration);
        tank.setPauseShield(this.hasPauseShield);
        tank.setShip(this.hasShip);
        if (this.hasShip) {
            LOG.debug("applyToTank: Setting hasShip=true for player {}", tank.getPlayerNumber());
        }
        tank.setGun(this.hasGun);
        tank.setStarCount(this.starCount);
        tank.setCarCount(this.carCount);
        tank.setSaw(this.hasSaw);
        tank.setMachinegunCount(this.machinegunCount);
        tank.setLaserDuration(this.laserDuration);
    }

    /**
     * Get display name (nickname or "P1", "P2", etc.)
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        return "P" + playerNumber;
    }

    /**
     * Get lives for display (remaining respawns = lives - 1)
     */
    public int getDisplayLives() {
        return Math.max(0, lives - 1);
    }
}
