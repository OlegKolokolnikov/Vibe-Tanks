package com.vibetanks.core;

import java.util.Random;

/**
 * Shared game constants to eliminate duplication between Game.java and ServerGameState.java.
 * Single source of truth for all game configuration values.
 */
public final class GameConstants {
    private GameConstants() {} // Prevent instantiation

    /**
     * Shared Random instance to avoid creating new Random objects in hot paths.
     * Thread-safe for game use (single game thread).
     */
    public static final Random RANDOM = new Random();

    // Map dimensions
    public static final int MAP_SIZE = 26;
    public static final int TILE_SIZE = 32;

    // Enemy settings
    public static final int TOTAL_ENEMIES = 20;
    public static final int MAX_ENEMIES_ON_SCREEN = 5;

    // Fixed start positions for each player (up to 4 players)
    public static final double[][] PLAYER_START_POSITIONS = {
        {8 * TILE_SIZE, 24 * TILE_SIZE},   // Player 1
        {16 * TILE_SIZE, 24 * TILE_SIZE},  // Player 2
        {9 * TILE_SIZE, 24 * TILE_SIZE},   // Player 3
        {15 * TILE_SIZE, 24 * TILE_SIZE}   // Player 4
    };

    // Base position
    public static final double BASE_X = 12 * TILE_SIZE;
    public static final double BASE_Y = 24 * TILE_SIZE;

    // Timing constants (all in frames at 60 FPS)
    public static final int BASE_PROTECTION_TIME = 3600;  // 1 minute
    public static final int FLASH_DURATION = 60;          // 1 second for flashing warning
    public static final int FREEZE_TIME = 600;            // 10 seconds
    public static final int ENEMY_SPEED_BOOST_TIME = 1800; // 30 seconds
    public static final int UFO_MESSAGE_DURATION = 180;   // 3 seconds
    public static final int VICTORY_DELAY = 300;          // 5 seconds

    // Enemy team speed boost
    public static final double ENEMY_TEAM_SPEED_BOOST = 0.3; // 30% speed boost

    // Shield durations
    public static final int SHIELD_DURATION = 3600;           // 1 minute (full shield power-up)
    public static final int TEMPORARY_SHIELD_DURATION = 180;  // 3 seconds (respawn/start shield)

    // Power-up effect durations
    public static final int LASER_DURATION = 1800;            // 30 seconds
    public static final int POWERUP_LIFETIME = 600;           // 10 seconds
    public static final int UFO_LIFETIME = 1800;              // 30 seconds
    public static final int EASTER_EGG_LIFETIME = 600;        // 10 seconds

    // UFO settings
    // 0.001 = 0.1% per frame, at 60 FPS = ~6% per second, avg spawn ~17 seconds after conditions met
    public static final double UFO_SPAWN_CHANCE = 0.0035; // ~4.8 seconds average at 60 FPS
    public static final int UFO_SHOOT_COOLDOWN = 90;      // Frames between UFO shots
    public static final int UFO_DIRECTION_CHANGE = 60;    // Change direction every second

    // Enemy spawner settings
    public static final int SPAWN_DELAY = 50;             // ~0.8 seconds between spawns
    public static final int BOSS_BASE_HEALTH = 12;        // Base health for boss (increases per level)

    // Tank settings
    public static final int TANK_SHOOT_COOLDOWN = 30;     // Frames between shots
    public static final int LASER_SHOOT_COOLDOWN = 10;    // Very fast laser shooting
    public static final int RESPAWN_DELAY = 60;           // 1 second respawn delay
    public static final double TANK_BASE_SPEED = 2.0;     // Base movement speed
    public static final double SLIDE_DISTANCE = 32.0;     // One tile slide on ice

    // Bullet/Laser settings
    public static final double BULLET_SPEED = 6.0;
    public static final int BULLET_SIZE = 8;
    public static final int LASER_BEAM_WIDTH = 12;
    public static final int LASER_LIFETIME = 15;          // Laser visible frames

    // Map burning
    public static final int BURN_DURATION = 60;           // 1 second burn time

    // AI behavior constants
    public static final int AI_MOVE_COOLDOWN_BASE = 60;      // Base frames between direction changes
    public static final int AI_MOVE_COOLDOWN_RANDOM = 90;    // Random additional frames
    public static final int AI_SHOOT_COOLDOWN_BASE = 60;     // Base frames between shots
    public static final int AI_SHOOT_COOLDOWN_RANDOM = 60;   // Random additional frames
    public static final int AI_STUCK_THRESHOLD = 3;          // Frames before considered stuck
    public static final int AI_STUCK_COOLDOWN_BASE = 60;     // Base cooldown after unstuck
    public static final int AI_STUCK_COOLDOWN_RANDOM = 120;  // Random additional cooldown
    public static final double AI_TARGET_BASE_CHANCE = 0.7;  // 70% chance to move towards base

    // Scoring
    public static final int SCORE_REGULAR = 1;
    public static final int SCORE_ARMORED = 1;
    public static final int SCORE_FAST = 1;
    public static final int SCORE_POWER = 2;
    public static final int SCORE_HEAVY = 5;
    public static final int SCORE_BOSS = 10;

    // Enemy spawn probabilities (cumulative thresholds)
    public static final double SPAWN_REGULAR_THRESHOLD = 0.5;   // 50% REGULAR (0.0 - 0.5)
    public static final double SPAWN_FAST_THRESHOLD = 0.7;      // 20% FAST (0.5 - 0.7)
    public static final double SPAWN_ARMORED_THRESHOLD = 0.85;  // 15% ARMORED (0.7 - 0.85)
    // Remaining 15% is POWER (0.85 - 1.0)

    /**
     * Get score for killing an enemy type.
     */
    public static int getScoreForEnemyType(Tank.EnemyType type) {
        return switch (type) {
            case POWER -> SCORE_POWER;
            case HEAVY -> SCORE_HEAVY;
            case BOSS -> SCORE_BOSS;
            default -> SCORE_REGULAR;
        };
    }

    /**
     * Get player start position.
     * @param playerIndex 0-based player index (0-3)
     * @return [x, y] coordinates
     */
    public static double[] getPlayerStartPosition(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= PLAYER_START_POSITIONS.length) {
            return PLAYER_START_POSITIONS[0];
        }
        return PLAYER_START_POSITIONS[playerIndex];
    }
}
