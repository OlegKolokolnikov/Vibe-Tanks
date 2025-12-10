package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.util.GameLogger;

import java.util.List;

/**
 * Manages UFO spawning, updates, and easter egg drops.
 * Extracted from Game.java to reduce complexity.
 */
public class UFOManager {
    private static final GameLogger LOG = GameLogger.getLogger(UFOManager.class);
    private static final int UFO_MESSAGE_DURATION = GameConstants.UFO_MESSAGE_DURATION;

    private UFO ufo;
    private EasterEgg easterEgg;
    private boolean ufoSpawnedThisLevel = false;
    private boolean ufoWasKilled = false;
    private int ufoLostMessageTimer = 0;
    private int ufoKilledMessageTimer = 0;


    /**
     * Result of UFO update containing events that occurred.
     */
    public static class UpdateResult {
        public boolean ufoEscaped = false;
        public boolean easterEggCollectedByPlayer = false;
        public int easterEggCollectorIndex = -1;
        public boolean easterEggCollectedByEnemy = false;
        public boolean easterEggExpired = false;
    }

    public UFOManager() {
        reset();
    }

    /**
     * Reset all UFO state for new level.
     */
    public void reset() {
        ufo = null;
        easterEgg = null;
        ufoSpawnedThisLevel = false;
        ufoWasKilled = false;
        ufoLostMessageTimer = 0;
        ufoKilledMessageTimer = 0;
    }

    /**
     * Check if UFO should spawn and spawn it if conditions are met.
     * UFO has a random chance to spawn each frame once conditions are satisfied,
     * making its appearance less predictable.
     *
     * @param playerTanks list of player tanks
     * @param playerKills array of total kills per player (index 0-3)
     * @param mapWidth map width in pixels
     * @param mapHeight map height in pixels
     */
    public void checkAndSpawnUFO(List<Tank> playerTanks, int[] playerKills, int mapWidth, int mapHeight) {
        if (ufoSpawnedThisLevel || ufo != null) {
            return;
        }

        // Check if any player meets the spawn conditions (machinegun + 5 kills)
        boolean conditionsMet = false;
        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank player = playerTanks.get(i);
            int totalKills = (playerKills != null && i < playerKills.length) ? playerKills[i] : 0;
            if (player.getMachinegunCount() > 0 && totalKills >= 5) {
                conditionsMet = true;
                break;
            }
        }

        // If conditions are met, roll for random spawn chance each frame
        // At 0.1% per frame (60 FPS), UFO spawns on average after ~17 seconds
        if (conditionsMet && GameConstants.RANDOM.nextDouble() < GameConstants.UFO_SPAWN_CHANCE) {
            LOG.info("UFO spawn triggered by random chance!");
            spawnUFO(mapWidth, mapHeight);
        }
    }

    private void spawnUFO(int mapWidth, int mapHeight) {
        boolean fromRight = GameConstants.RANDOM.nextBoolean();
        double startX = fromRight ? mapWidth + 48 : -48;
        double startY = 100 + GameConstants.RANDOM.nextDouble() * 200;
        ufo = new UFO(startX, startY, !fromRight);
        ufoSpawnedThisLevel = true;
        LOG.debug("UFO spawned at {}, {}", startX, startY);
    }

    /**
     * Update UFO and message timers.
     * @return true if UFO escaped during this update
     */
    public boolean updateUFO(List<Bullet> bullets, int width, int height,
                             SoundManager soundManager, boolean victoryConditionMet) {
        boolean escaped = false;

        // Update UFO if exists (but not during victory delay - stop shooting)
        if (ufo != null && ufo.isAlive() && !victoryConditionMet) {
            ufo.update(bullets, width, height, soundManager);
            if (!ufo.isAlive()) {
                // UFO escaped (wasn't killed by player)
                if (!ufoWasKilled) {
                    ufoLostMessageTimer = UFO_MESSAGE_DURATION;
                    LOG.info("UFO escaped! Lost it!");
                    escaped = true;
                }
                ufo = null;
            }
        }

        // Update message timers
        if (ufoLostMessageTimer > 0) {
            ufoLostMessageTimer--;
        }
        if (ufoKilledMessageTimer > 0) {
            ufoKilledMessageTimer--;
        }

        return escaped;
    }

    /**
     * Handle UFO being destroyed by a player bullet.
     * @param killerPlayerNumber 1-based player number who killed UFO
     * @param eggSpawnX X position for easter egg
     * @param eggSpawnY Y position for easter egg
     */
    public void handleUFODestroyed(int killerPlayerNumber, double eggSpawnX, double eggSpawnY) {
        easterEgg = new EasterEgg(eggSpawnX, eggSpawnY);
        LOG.debug("Easter egg spawned at {}, {}", eggSpawnX, eggSpawnY);
        ufoWasKilled = true;
        ufoKilledMessageTimer = UFO_MESSAGE_DURATION;
        LOG.info("Poor ALF :( Killed by Player {}", killerPlayerNumber);
        ufo = null;
    }

    /**
     * Update easter egg and check for collection.
     */
    public UpdateResult updateEasterEgg(List<Tank> playerTanks, List<Tank> enemyTanks) {
        UpdateResult result = new UpdateResult();

        if (easterEgg == null) {
            return result;
        }

        easterEgg.update();

        int collectionResult = GameLogic.checkEasterEggCollection(easterEgg, playerTanks, enemyTanks);

        if (collectionResult > 0) {
            // Player collected it
            result.easterEggCollectedByPlayer = true;
            result.easterEggCollectorIndex = collectionResult - 1;
            easterEgg.collect();
            easterEgg = null;
        } else if (collectionResult == -1) {
            // Enemy collected it
            result.easterEggCollectedByEnemy = true;
            easterEgg.collect();
            easterEgg = null;
        } else if (easterEgg.isExpired()) {
            result.easterEggExpired = true;
            easterEgg = null;
        }

        return result;
    }

    // Getters for rendering and network sync
    public UFO getUFO() { return ufo; }
    public EasterEgg getEasterEgg() { return easterEgg; }
    public boolean isUfoSpawnedThisLevel() { return ufoSpawnedThisLevel; }
    public boolean isUfoWasKilled() { return ufoWasKilled; }
    public int getUfoLostMessageTimer() { return ufoLostMessageTimer; }
    public int getUfoKilledMessageTimer() { return ufoKilledMessageTimer; }

    // Setters for network sync (client receiving state from host)
    public void setUFO(UFO ufo) { this.ufo = ufo; }
    public void setEasterEgg(EasterEgg easterEgg) { this.easterEgg = easterEgg; }
    public void setUfoSpawnedThisLevel(boolean value) { this.ufoSpawnedThisLevel = value; }
    public void setUfoWasKilled(boolean value) { this.ufoWasKilled = value; }
    public void setUfoLostMessageTimer(int value) { this.ufoLostMessageTimer = value; }
    public void setUfoKilledMessageTimer(int value) { this.ufoKilledMessageTimer = value; }

    /**
     * Apply UFO state from network sync.
     */
    public void applyNetworkState(UFO networkUfo, EasterEgg networkEasterEgg,
                                   int lostTimer, int killedTimer) {
        this.ufo = networkUfo;
        this.easterEgg = networkEasterEgg;
        this.ufoLostMessageTimer = lostTimer;
        this.ufoKilledMessageTimer = killedTimer;
    }
}
