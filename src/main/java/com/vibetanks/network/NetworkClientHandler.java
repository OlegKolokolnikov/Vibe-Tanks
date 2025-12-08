package com.vibetanks.network;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;

import java.util.*;

/**
 * Handles network client-side game state synchronization.
 * Responsible for:
 * - Applying game state received from host
 * - Detecting changes for sound effects (new bullets, dying enemies, etc.)
 * - Syncing player positions, enemies, bullets, lasers, power-ups
 * - Handling level transitions and game restarts
 */
public class NetworkClientHandler {

    // Callback interfaces for Game to handle state changes
    @FunctionalInterface
    public interface DancingInitializer {
        void initializeDancing();
    }

    @FunctionalInterface
    public interface VictoryCelebrationInitializer {
        void initializeVictoryCelebration();
    }

    @FunctionalInterface
    public interface EnemySpawnerFactory {
        EnemySpawner create(int totalEnemies, int maxOnScreen, GameMap gameMap);
    }

    // State tracking for sound effects (detect changes)
    private int prevEnemyCount = 0;
    private Set<Long> seenBulletIds = new HashSet<>();
    private Set<Long> seenLaserIds = new HashSet<>();
    private Set<Integer> seenBurningTileKeys = new HashSet<>();
    private boolean firstStateReceived = false;
    private int respawnSyncFrames = 0;

    // Track connected players
    private int networkConnectedPlayers = 1;

    public NetworkClientHandler() {
    }

    /**
     * Check if first state has been received from host.
     */
    public boolean isFirstStateReceived() {
        return firstStateReceived;
    }

    /**
     * Get respawn sync frames (frames to wait after respawn before sending position).
     */
    public int getRespawnSyncFrames() {
        return respawnSyncFrames;
    }

    /**
     * Decrement respawn sync frames.
     */
    public void decrementRespawnSyncFrames() {
        if (respawnSyncFrames > 0) {
            respawnSyncFrames--;
        }
    }

    /**
     * Get number of connected players tracked from server.
     */
    public int getNetworkConnectedPlayers() {
        return networkConnectedPlayers;
    }

    /**
     * Apply game state received from host.
     * Returns true if level changed, game restarted, or next level started.
     */
    public boolean applyGameState(
            GameState state,
            NetworkManager network,
            GameMap gameMap,
            List<Tank> playerTanks,
            List<Tank> enemyTanks,
            List<Bullet> bullets,
            List<Laser> lasers,
            List<PowerUp> powerUps,
            Base base,
            EnemySpawner enemySpawner,
            UFO[] ufoHolder,
            EasterEgg[] easterEggHolder,
            int[] playerKills,
            int[] playerScores,
            int[] playerLevelScores,
            int[][] playerKillsByType,
            String[] playerNicknames,
            double[][] playerStartPositions,
            double[][] fixedStartPositions,
            int totalEnemies,
            SoundManager soundManager,
            DancingInitializer dancingInitializer,
            VictoryCelebrationInitializer victoryInitializer,
            EnemySpawnerFactory spawnerFactory,
            int[] freezeDurations,
            int[] messageTimers,
            boolean[] gameFlags,
            int[] bossKillInfo
    ) {
        // Apply host's game settings
        if (state.hostPlayerSpeed != 1.0 || state.hostEnemySpeed != 1.0) {
            System.out.println("[DEBUG] Received host settings: playerSpeed=" + state.hostPlayerSpeed +
                ", enemySpeed=" + state.hostEnemySpeed);
        }
        GameSettings.setHostSettings(
            state.hostPlayerSpeed,
            state.hostEnemySpeed,
            state.hostPlayerShootSpeed,
            state.hostEnemyShootSpeed
        );

        // Track connected players from server
        networkConnectedPlayers = state.connectedPlayers;

        // Dynamically add tanks if more players connected
        while (playerTanks.size() < state.connectedPlayers && playerTanks.size() < 4) {
            int playerNum = playerTanks.size() + 1;
            double x, y;
            switch (playerNum) {
                case 2 -> { x = 16 * 32; y = 24 * 32; }
                case 3 -> { x = 9 * 32; y = 24 * 32; }
                case 4 -> { x = 15 * 32; y = 24 * 32; }
                default -> { x = 8 * 32; y = 24 * 32; }
            }
            System.out.println("Adding Player " + playerNum + " tank (new player connected)");
            playerTanks.add(new Tank(x, y, Direction.UP, true, playerNum));

            // Update playerStartPositions array for respawn
            if (playerStartPositions.length < playerNum) {
                double[][] newStartPositions = new double[playerTanks.size()][2];
                for (int j = 0; j < playerStartPositions.length; j++) {
                    newStartPositions[j] = playerStartPositions[j];
                }
                newStartPositions[playerNum - 1] = new double[]{x, y};
                // Note: caller needs to update their reference
            }
        }

        // Get local player index
        int myPlayerIndex = network != null ? network.getPlayerNumber() - 1 : -1;

        // Update all players
        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank tank = playerTanks.get(i);
            PlayerData pData = state.players[i];

            boolean isLocalPlayer = (i == myPlayerIndex);
            boolean isFirstSync = isLocalPlayer && !firstStateReceived;
            boolean justRespawned = isLocalPlayer && !tank.isAlive() && pData.alive;
            boolean justDied = tank.isAlive() && !pData.alive;
            boolean skipPosition = isLocalPlayer && !isFirstSync && !justRespawned;

            if (isFirstSync) {
                System.out.println("Client first sync - accepting host position: " + pData.x + ", " + pData.y);
                respawnSyncFrames = 5;
            } else if (justRespawned) {
                System.out.println("Client respawning at host position: " + pData.x + ", " + pData.y);
                respawnSyncFrames = 5;
            }

            if (justDied && firstStateReceived) {
                soundManager.playPlayerDeath();
            }

            pData.applyToTank(tank, skipPosition);

            // Update stats
            playerKills[i] = pData.kills;
            int oldScore = playerScores[i];
            playerScores[i] = pData.score;
            playerLevelScores[i] = pData.levelScore;
            if (pData.score != oldScore) {
                System.out.println("APPLY_STATE: Player " + (i + 1) + " score updated: " + oldScore + " -> " + pData.score);
            }
            if (pData.killsByType != null) {
                System.arraycopy(pData.killsByType, 0, playerKillsByType[i], 0, Math.min(6, pData.killsByType.length));
            }
            if (i != myPlayerIndex && pData.nickname != null) {
                playerNicknames[i] = pData.nickname;
            }
        }

        // Update enemy tanks
        while (enemyTanks.size() > state.enemies.size()) {
            enemyTanks.remove(enemyTanks.size() - 1);
        }
        while (enemyTanks.size() < state.enemies.size()) {
            enemyTanks.add(new Tank(0, 0, Direction.UP, false, 0, Tank.EnemyType.REGULAR));
        }
        for (int i = 0; i < state.enemies.size(); i++) {
            GameState.EnemyData eData = state.enemies.get(i);
            Tank enemy = enemyTanks.get(i);
            enemy.setAlive(eData.alive);
            enemy.setEnemyType(Tank.EnemyType.values()[eData.enemyType]);
            enemy.setHealth(eData.health);
            enemy.setMaxHealth(eData.maxHealth);
            enemy.applyTempSpeedBoost(eData.tempSpeedBoost);
            enemy.setSpeedMultiplier(eData.speedMultiplier);
            if (eData.alive) {
                enemy.setPosition(eData.x, eData.y);
                enemy.setDirection(Direction.values()[eData.direction]);
            }
        }

        // Update bullets and detect new ones for sound
        Set<Long> currentBulletIds = new HashSet<>();
        bullets.clear();
        int localPlayerNum = network != null ? network.getPlayerNumber() : 1;
        for (GameState.BulletData bData : state.bullets) {
            currentBulletIds.add(bData.id);
            if (firstStateReceived && !seenBulletIds.contains(bData.id) && bData.ownerPlayerNumber != localPlayerNum) {
                soundManager.playShoot();
            }
            Bullet bullet = new Bullet(
                bData.id, bData.x, bData.y,
                Direction.values()[bData.direction],
                bData.fromEnemy, bData.power,
                bData.canDestroyTrees, bData.ownerPlayerNumber, bData.size
            );
            bullets.add(bullet);
        }
        seenBulletIds = currentBulletIds;

        // Update lasers
        Set<Long> currentLaserIds = new HashSet<>();
        lasers.clear();
        if (state.lasers != null) {
            for (GameState.LaserData lData : state.lasers) {
                currentLaserIds.add(lData.id);
                if (firstStateReceived && !seenLaserIds.contains(lData.id) && lData.ownerPlayerNumber != localPlayerNum) {
                    soundManager.playLaser();
                }
                Laser laser = new Laser(
                    lData.startX, lData.startY,
                    Direction.values()[lData.direction],
                    lData.fromEnemy, lData.ownerPlayerNumber
                );
                laser.setId(lData.id);
                lasers.add(laser);
            }
        }
        seenLaserIds = currentLaserIds;

        if (!firstStateReceived) {
            firstStateReceived = true;
        }

        // Update power-ups
        powerUps.clear();
        for (GameState.PowerUpData pData : state.powerUps) {
            PowerUp powerUp = new PowerUp(pData.x, pData.y, PowerUp.Type.values()[pData.type]);
            powerUps.add(powerUp);
        }

        // Sync map
        if (state.mapTiles != null) {
            gameMap.importTiles(state.mapTiles);
        }

        // Sync burning tiles
        if (state.burningTiles != null) {
            Set<Integer> currentBurningKeys = new HashSet<>();
            List<int[]> burningData = new ArrayList<>();
            for (GameState.BurningTileData bt : state.burningTiles) {
                int key = bt.row * 1000 + bt.col;
                currentBurningKeys.add(key);
                if (firstStateReceived && !seenBurningTileKeys.contains(key)) {
                    soundManager.playTreeBurn();
                }
                burningData.add(new int[]{bt.row, bt.col, bt.framesRemaining});
            }
            seenBurningTileKeys = currentBurningKeys;
            gameMap.setBurningTiles(burningData);
        }

        // gameFlags: [0]=gameOver, [1]=victory, [2]=dancingInitialized, [3]=victoryDancingInitialized, [4]=gameOverSoundPlayed
        boolean dancingInitialized = gameFlags[2];
        boolean victoryDancingInitialized = gameFlags[3];

        // Sync dancing characters
        if (state.gameOver && !state.baseAlive && !dancingInitialized) {
            dancingInitializer.initializeDancing();
            gameFlags[2] = true;
        }
        if (!state.gameOver) {
            gameFlags[2] = false;
        }

        // Sync victory dancing
        if (state.victory && !victoryDancingInitialized) {
            soundManager.stopGameplaySounds();
            victoryInitializer.initializeVictoryCelebration();
            gameFlags[3] = true;
        }
        if (!state.victory) {
            gameFlags[3] = false;
        }

        // Sync UFO
        if (state.ufoData != null && state.ufoData.alive) {
            if (ufoHolder[0] == null) {
                ufoHolder[0] = new UFO(state.ufoData.x, state.ufoData.y, state.ufoData.movingRight);
            }
            UFO ufo = ufoHolder[0];
            ufo.setX(state.ufoData.x);
            ufo.setY(state.ufoData.y);
            ufo.setDx(state.ufoData.dx);
            ufo.setDy(state.ufoData.dy);
            ufo.setHealth(state.ufoData.health);
            ufo.setLifetime(state.ufoData.lifetime);
            ufo.setAlive(state.ufoData.alive);
        } else {
            ufoHolder[0] = null;
        }

        // Sync message timers: [0]=ufoLostMessageTimer, [1]=ufoKilledMessageTimer
        messageTimers[0] = state.ufoLostMessageTimer;
        messageTimers[1] = state.ufoKilledMessageTimer;

        // Sync easter egg
        if (state.easterEggData != null) {
            if (easterEggHolder[0] == null) {
                easterEggHolder[0] = new EasterEgg(state.easterEggData.x, state.easterEggData.y);
            }
            easterEggHolder[0].setPosition(state.easterEggData.x, state.easterEggData.y);
            easterEggHolder[0].setLifetime(state.easterEggData.lifetime);
        } else {
            easterEggHolder[0] = null;
        }

        // Check level/game state transitions
        boolean levelChanged = state.levelNumber != gameMap.getLevelNumber();
        boolean gameRestarted = gameFlags[0] && !state.gameOver && !state.victory;
        boolean nextLevelStarted = gameFlags[1] && !state.victory && !state.gameOver;

        if (levelChanged || gameRestarted || nextLevelStarted) {
            handleLevelTransition(state, network, gameMap, playerTanks, enemyTanks, bullets, lasers, powerUps,
                playerKills, playerScores, playerLevelScores, playerKillsByType, fixedStartPositions,
                totalEnemies, soundManager, spawnerFactory, ufoHolder, messageTimers, gameFlags);
        }

        // Update game state flags
        gameFlags[0] = state.gameOver;
        gameFlags[1] = state.victory;

        // Sync boss kill info: [0]=bossKillerPlayerIndex, [1]=bossKillPowerUpReward
        bossKillInfo[0] = state.bossKillerPlayerIndex;
        bossKillInfo[1] = state.bossKillPowerUpReward;

        // freezeDurations: [0]=enemyFreezeDuration, [1]=playerFreezeDuration, [2]=enemyTeamSpeedBoostDuration
        freezeDurations[0] = state.enemyFreezeDuration;
        freezeDurations[1] = state.playerFreezeDuration;
        freezeDurations[2] = state.enemyTeamSpeedBoostDuration;

        // Update remaining enemies
        enemySpawner.setRemainingEnemies(state.remainingEnemies);

        // Update base
        if (levelChanged || gameRestarted || nextLevelStarted || (state.baseAlive && !base.isAlive())) {
            // Base handled in level transition
        } else if (!state.baseAlive && base.isAlive()) {
            base.destroy();
            soundManager.playBaseDestroyed();
        }

        // Update level number
        while (gameMap.getLevelNumber() < state.levelNumber) {
            gameMap.nextLevel();
        }
        if (gameMap.getLevelNumber() > state.levelNumber) {
            gameMap.resetToLevel1();
        }

        // Sync flag states
        base.setFlagState(state.baseShowFlag, state.baseFlagHeight);
        base.setVictoryFlagState(state.baseShowVictoryFlag, state.baseVictoryFlagHeight);
        base.setEasterEggMode(state.baseEasterEggMode);

        // Play explosion sound when enemy dies
        int currentEnemyCount = enemyTanks.size();
        if (currentEnemyCount < prevEnemyCount) {
            soundManager.playExplosion();
        }
        prevEnemyCount = currentEnemyCount;

        return levelChanged || gameRestarted || nextLevelStarted;
    }

    private void handleLevelTransition(
            GameState state,
            NetworkManager network,
            GameMap gameMap,
            List<Tank> playerTanks,
            List<Tank> enemyTanks,
            List<Bullet> bullets,
            List<Laser> lasers,
            List<PowerUp> powerUps,
            int[] playerKills,
            int[] playerScores,
            int[] playerLevelScores,
            int[][] playerKillsByType,
            double[][] fixedStartPositions,
            int totalEnemies,
            SoundManager soundManager,
            EnemySpawnerFactory spawnerFactory,
            UFO[] ufoHolder,
            int[] messageTimers,
            boolean[] gameFlags
    ) {
        System.out.println("Level transition detected - resetting client state");

        // Reset scores if restarting to level 1
        if (state.levelNumber == 1 && gameMap.getLevelNumber() > 1) {
            for (int i = 0; i < playerScores.length; i++) {
                playerScores[i] = 0;
            }
        }

        // Reset kills and level scores
        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerLevelScores[i] = 0;
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }

        // Clear visual/game state
        gameFlags[2] = false; // dancingInitialized
        gameFlags[3] = false; // victoryDancingInitialized
        gameFlags[4] = false; // gameOverSoundPlayed

        // Reset local player position
        int localPlayerIdx = network != null ? network.getPlayerNumber() - 1 : -1;
        if (localPlayerIdx >= 0 && localPlayerIdx < playerTanks.size()) {
            Tank myTank = playerTanks.get(localPlayerIdx);
            myTank.setPosition(fixedStartPositions[localPlayerIdx][0], fixedStartPositions[localPlayerIdx][1]);
            myTank.setDirection(Direction.UP);
            myTank.giveTemporaryShield();
        }

        // Clear entities
        bullets.clear();
        lasers.clear();
        powerUps.clear();
        enemyTanks.clear();

        // Reset UFO state
        ufoHolder[0] = null;
        messageTimers[0] = 0;
        messageTimers[1] = 0;

        // Play intro sound
        soundManager.playIntro();
    }

    /**
     * Reset state for a new game.
     */
    public void reset() {
        prevEnemyCount = 0;
        seenBulletIds.clear();
        seenLaserIds.clear();
        seenBurningTileKeys.clear();
        firstStateReceived = false;
        respawnSyncFrames = 0;
        networkConnectedPlayers = 1;
    }
}
