package com.vibetanks.network;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.util.GameLogger;

import java.util.*;

/**
 * Applies GameState received from network to local game objects.
 * Extracted from Game.java to reduce complexity.
 */
public class GameStateApplier {
    private static final GameLogger LOG = GameLogger.getLogger(GameStateApplier.class);

    /**
     * Context interface for accessing and modifying game state.
     * Game.java implements this to provide access to its fields.
     */
    public interface GameContext {
        // Getters for game objects
        List<Tank> getPlayerTanks();
        List<Tank> getEnemyTanks();
        List<Bullet> getBullets();
        List<Laser> getLasers();
        List<PowerUp> getPowerUps();
        GameMap getGameMap();
        Base getBase();
        EnemySpawner getEnemySpawner();
        CelebrationManager getCelebrationManager();
        UFOManager getUFOManager();
        PowerUpEffectManager getPowerUpEffectManager();
        SoundManager getSoundManager();
        NetworkManager getNetwork();

        // Player data arrays
        int[] getPlayerKills();
        int[] getPlayerScores();
        int[] getPlayerLevelScores();
        int[][] getPlayerKillsByType();
        String[] getPlayerNicknames();
        double[][] getPlayerStartPositions();

        // State tracking
        boolean isFirstStateReceived();
        void setFirstStateReceived(boolean value);
        Set<Long> getSeenBulletIds();
        void setSeenBulletIds(Set<Long> ids);
        Set<Long> getSeenLaserIds();
        void setSeenLaserIds(Set<Long> ids);
        Set<Integer> getSeenBurningTileKeys();
        void setSeenBurningTileKeys(Set<Integer> keys);
        int getPrevEnemyCount();
        void setPrevEnemyCount(int count);

        // Game state flags
        boolean isGameOver();
        void setGameOver(boolean value);
        boolean isVictory();
        void setVictory(boolean value);
        void setNetworkConnectedPlayers(int count);
        void setRespawnSyncFrames(int frames);
        void setPlayerStartPositions(double[][] positions);
        void setBossKillerPlayerIndex(int index);
        void setBossKillPowerUpReward(PowerUp.Type type);

        // Object management
        void setBase(Base base);
        void setEnemySpawner(EnemySpawner spawner);
        int getTotalEnemies();

        // UI elements
        void hideVictoryImage();
        void hideGameOverImage();
        void setGameOverSoundPlayed(boolean value);

        // Fixed start positions constant
        double[][] getFixedStartPositions();
    }

    /**
     * Apply a GameState to the game context.
     * @param state The GameState received from the network
     * @param ctx The game context to apply state to
     */
    public static void apply(GameState state, GameContext ctx) {
        // Apply host's game settings
        applyHostSettings(state);

        // Track connected players from server
        ctx.setNetworkConnectedPlayers(state.connectedPlayers);

        // Dynamically add tanks if more players connected
        addNewPlayerTanks(state, ctx);

        // Get local player index
        NetworkManager network = ctx.getNetwork();
        int myPlayerIndex = network != null ? network.getPlayerNumber() - 1 : -1;
        int localPlayerNum = network != null ? network.getPlayerNumber() : 1;

        // Update player tanks
        updatePlayerTanks(state, ctx, myPlayerIndex);

        // Update enemy tanks
        updateEnemyTanks(state, ctx);

        // Update bullets
        updateBullets(state, ctx, localPlayerNum);

        // Update lasers
        updateLasers(state, ctx, localPlayerNum);

        // Mark first state as received
        if (!ctx.isFirstStateReceived()) {
            ctx.setFirstStateReceived(true);
        }

        // Update power-ups
        updatePowerUps(state, ctx);

        // Sync map state
        syncMapState(state, ctx);

        // Sync celebration state
        syncCelebrationState(state, ctx);

        // Sync UFO and easter egg
        syncUFOState(state, ctx);

        // Check for level change / restart
        boolean levelChanged = state.levelNumber != ctx.getGameMap().getLevelNumber();
        boolean gameRestarted = ctx.isGameOver() && !state.gameOver && !state.victory;
        boolean nextLevelStarted = ctx.isVictory() && !state.victory && !state.gameOver;

        if (levelChanged || gameRestarted || nextLevelStarted) {
            handleLevelTransition(state, ctx, levelChanged, gameRestarted, nextLevelStarted);
        }

        // Update game state flags
        ctx.setGameOver(state.gameOver);
        ctx.setVictory(state.victory);

        // Sync boss kill info
        ctx.setBossKillerPlayerIndex(state.bossKillerPlayerIndex);
        ctx.setBossKillPowerUpReward(state.bossKillPowerUpReward >= 0 ?
            PowerUp.Type.values()[state.bossKillPowerUpReward] : null);

        // Update freeze state
        PowerUpEffectManager effectManager = ctx.getPowerUpEffectManager();
        effectManager.setEnemyFreezeDuration(state.enemyFreezeDuration);
        effectManager.setPlayerFreezeDuration(state.playerFreezeDuration);
        effectManager.setEnemyTeamSpeedBoostDuration(state.enemyTeamSpeedBoostDuration);

        // Update remaining enemies count
        ctx.getEnemySpawner().setRemainingEnemies(state.remainingEnemies);

        // Update base
        updateBase(state, ctx, levelChanged, gameRestarted, nextLevelStarted);

        // Update level number
        GameMap gameMap = ctx.getGameMap();
        while (gameMap.getLevelNumber() < state.levelNumber) {
            gameMap.nextLevel();
        }
        if (gameMap.getLevelNumber() > state.levelNumber) {
            gameMap.resetToLevel1();
        }

        // Sync flag states
        Base base = ctx.getBase();
        base.setFlagState(state.baseShowFlag, state.baseFlagHeight);
        base.setVictoryFlagState(state.baseShowVictoryFlag, state.baseVictoryFlagHeight);
        base.setCatMode(state.baseCatMode);

        // Play explosion sound when enemy dies
        int currentEnemyCount = ctx.getEnemyTanks().size();
        if (currentEnemyCount < ctx.getPrevEnemyCount()) {
            ctx.getSoundManager().playExplosion();
        }
        ctx.setPrevEnemyCount(currentEnemyCount);

        // Play sound events from server
        playSoundEvents(state, ctx);
    }

    private static void playSoundEvents(GameState state, GameContext ctx) {
        if (state.soundEvents == null || state.soundEvents.isEmpty()) {
            return;
        }

        SoundManager soundManager = ctx.getSoundManager();
        for (GameState.SoundEvent event : state.soundEvents) {
            switch (event.type) {
                case LASER -> soundManager.playLaser();
                case PLAYER_DEATH -> soundManager.playPlayerDeath();
                case BASE_DESTROYED -> soundManager.playBaseDestroyed();
                case TREE_BURN -> soundManager.playTreeBurn();
                // SHOOT and EXPLOSION are handled locally for responsiveness
            }
        }
    }

    private static void applyHostSettings(GameState state) {
        if (state.hostPlayerSpeed != 1.0 || state.hostEnemySpeed != 1.0) {
            LOG.debug("[DEBUG] Received host settings: playerSpeed={}, enemySpeed={}",
                state.hostPlayerSpeed, state.hostEnemySpeed);
        }
        GameSettings.setHostSettings(
            state.hostPlayerSpeed,
            state.hostEnemySpeed,
            state.hostPlayerShootSpeed,
            state.hostEnemyShootSpeed
        );
    }

    private static void addNewPlayerTanks(GameState state, GameContext ctx) {
        List<Tank> playerTanks = ctx.getPlayerTanks();
        double[][] playerStartPositions = ctx.getPlayerStartPositions();

        while (playerTanks.size() < state.connectedPlayers && playerTanks.size() < 4) {
            int playerNum = playerTanks.size() + 1;
            double x, y;
            switch (playerNum) {
                case 2 -> { x = 16 * 32; y = 24 * 32; }
                case 3 -> { x = 9 * 32; y = 24 * 32; }
                case 4 -> { x = 15 * 32; y = 24 * 32; }
                default -> { x = 8 * 32; y = 24 * 32; }
            }
            LOG.info("Adding Player {} tank (new player connected)", playerNum);
            playerTanks.add(new Tank(x, y, Direction.UP, true, playerNum));

            // Update playerStartPositions array for respawn
            double[][] newStartPositions = new double[playerTanks.size()][2];
            for (int j = 0; j < playerStartPositions.length; j++) {
                newStartPositions[j] = playerStartPositions[j];
            }
            newStartPositions[playerNum - 1] = new double[]{x, y};
            ctx.setPlayerStartPositions(newStartPositions);
            playerStartPositions = newStartPositions;
        }
    }

    private static void updatePlayerTanks(GameState state, GameContext ctx, int myPlayerIndex) {
        List<Tank> playerTanks = ctx.getPlayerTanks();
        int[] playerKills = ctx.getPlayerKills();
        int[] playerScores = ctx.getPlayerScores();
        int[] playerLevelScores = ctx.getPlayerLevelScores();
        int[][] playerKillsByType = ctx.getPlayerKillsByType();
        String[] playerNicknames = ctx.getPlayerNicknames();
        SoundManager soundManager = ctx.getSoundManager();
        boolean firstStateReceived = ctx.isFirstStateReceived();

        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank tank = playerTanks.get(i);
            PlayerData pData = state.players[i];

            // Skip position update for local player (client-authoritative movement)
            // EXCEPT when:
            // 1. First state received - need to sync initial position from host
            // 2. Respawning (was dead, now alive) - accept respawn position from host
            boolean isLocalPlayer = (i == myPlayerIndex);
            boolean isFirstSync = isLocalPlayer && !firstStateReceived;
            boolean justRespawned = isLocalPlayer && !tank.isAlive() && pData.alive;
            boolean justDied = tank.isAlive() && !pData.alive;
            boolean skipPosition = isLocalPlayer && !isFirstSync && !justRespawned;

            if (isFirstSync) {
                LOG.info("Client first sync - accepting host position: {}, {}", pData.x, pData.y);
                ctx.setRespawnSyncFrames(5);
            } else if (justRespawned) {
                LOG.info("Client respawning at host position: {}, {}", pData.x, pData.y);
                ctx.setRespawnSyncFrames(5);
            }

            // Play player death sound when a player dies
            if (justDied && firstStateReceived) {
                soundManager.playPlayerDeath();
            }

            pData.applyToTank(tank, skipPosition);

            // Update kills, scores, and nicknames
            playerKills[i] = pData.kills;
            int oldScore = playerScores[i];
            playerScores[i] = pData.score;
            playerLevelScores[i] = pData.levelScore;
            if (pData.score != oldScore) {
                LOG.debug("APPLY_STATE: Player {} score updated: {} -> {}", i + 1, oldScore, pData.score);
            }
            // Update kills by type
            if (pData.killsByType != null) {
                System.arraycopy(pData.killsByType, 0, playerKillsByType[i], 0, Math.min(6, pData.killsByType.length));
            }
            // Update nicknames - local player uses NicknameManager, others from state
            if (i == myPlayerIndex) {
                // Ensure local player's nickname is in the correct slot
                String localNickname = NicknameManager.getNickname();
                if (localNickname != null) {
                    playerNicknames[i] = localNickname;
                }
            } else if (pData.nickname != null) {
                playerNicknames[i] = pData.nickname;
            }
        }
    }

    private static void updateEnemyTanks(GameState state, GameContext ctx) {
        List<Tank> enemyTanks = ctx.getEnemyTanks();

        // Resize list to match state
        while (enemyTanks.size() > state.enemies.size()) {
            enemyTanks.remove(enemyTanks.size() - 1);
        }
        while (enemyTanks.size() < state.enemies.size()) {
            enemyTanks.add(new Tank(0, 0, Direction.UP, false, 0, Tank.EnemyType.REGULAR));
        }

        // Update each enemy tank's state
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
    }

    private static void updateBullets(GameState state, GameContext ctx, int localPlayerNum) {
        List<Bullet> bullets = ctx.getBullets();
        Set<Long> seenBulletIds = ctx.getSeenBulletIds();
        SoundManager soundManager = ctx.getSoundManager();
        boolean firstStateReceived = ctx.isFirstStateReceived();

        Set<Long> currentBulletIds = new HashSet<>();
        bullets.clear();

        for (GameState.BulletData bData : state.bullets) {
            currentBulletIds.add(bData.id);
            // Play shoot sound for bullets we haven't seen before
            // Skip on first state to avoid sound burst when joining mid-game
            // Skip for local player's bullets - they already played sound when shooting locally
            if (firstStateReceived && !seenBulletIds.contains(bData.id) && bData.ownerPlayerNumber != localPlayerNum) {
                soundManager.playShoot();
            }
            Bullet bullet = new Bullet(
                bData.id,
                bData.x, bData.y,
                Direction.values()[bData.direction],
                bData.fromEnemy,
                bData.power,
                bData.canDestroyTrees,
                bData.ownerPlayerNumber,
                bData.size
            );
            bullets.add(bullet);
        }
        // Update seen bullets - keep only current bullets to prevent memory leak
        ctx.setSeenBulletIds(currentBulletIds);
    }

    private static void updateLasers(GameState state, GameContext ctx, int localPlayerNum) {
        List<Laser> lasers = ctx.getLasers();
        Set<Long> seenLaserIds = ctx.getSeenLaserIds();
        SoundManager soundManager = ctx.getSoundManager();
        boolean firstStateReceived = ctx.isFirstStateReceived();

        Set<Long> currentLaserIds = new HashSet<>();
        lasers.clear();

        if (state.lasers != null) {
            for (GameState.LaserData lData : state.lasers) {
                currentLaserIds.add(lData.id);
                // Play laser sound for lasers we haven't seen before
                // Skip on first state to avoid sound burst when joining mid-game
                // Skip for local player's lasers - they already played sound when shooting locally
                if (firstStateReceived && !seenLaserIds.contains(lData.id) && lData.ownerPlayerNumber != localPlayerNum) {
                    soundManager.playLaser();
                }
                Laser laser = new Laser(
                    lData.startX, lData.startY,
                    Direction.values()[lData.direction],
                    lData.fromEnemy,
                    lData.ownerPlayerNumber
                );
                laser.setId(lData.id);
                lasers.add(laser);
            }
        }
        // Update seen lasers - keep only current lasers to prevent memory leak
        ctx.setSeenLaserIds(currentLaserIds);
    }

    private static void updatePowerUps(GameState state, GameContext ctx) {
        List<PowerUp> powerUps = ctx.getPowerUps();
        powerUps.clear();

        for (GameState.PowerUpData pData : state.powerUps) {
            PowerUp powerUp = new PowerUp(
                pData.x, pData.y,
                PowerUp.Type.values()[pData.type]
            );
            powerUps.add(powerUp);
        }
    }

    private static void syncMapState(GameState state, GameContext ctx) {
        GameMap gameMap = ctx.getGameMap();
        SoundManager soundManager = ctx.getSoundManager();
        boolean firstStateReceived = ctx.isFirstStateReceived();
        Set<Integer> seenBurningTileKeys = ctx.getSeenBurningTileKeys();

        // Sync full map state from host
        if (state.mapTiles != null) {
            gameMap.importTiles(state.mapTiles);
        }

        // Sync burning tiles for fire animation
        if (state.burningTiles != null) {
            Set<Integer> currentBurningKeys = new HashSet<>();
            List<int[]> burningData = new ArrayList<>();
            for (GameState.BurningTileData bt : state.burningTiles) {
                int key = bt.row * 1000 + bt.col;
                currentBurningKeys.add(key);
                // Play tree burn sound for new burning tiles
                if (firstStateReceived && !seenBurningTileKeys.contains(key)) {
                    soundManager.playTreeBurn();
                }
                burningData.add(new int[]{bt.row, bt.col, bt.framesRemaining});
            }
            ctx.setSeenBurningTileKeys(currentBurningKeys);
            gameMap.setBurningTiles(burningData);
        }
    }

    private static void syncCelebrationState(GameState state, GameContext ctx) {
        CelebrationManager celebrationManager = ctx.getCelebrationManager();
        Base base = ctx.getBase();
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        SoundManager soundManager = ctx.getSoundManager();

        // Sync dancing characters for game over animation
        // For network clients: initialize dancing locally when server signals game over with base destroyed
        if (state.gameOver && !state.baseAlive && !celebrationManager.isDancingInitialized()) {
            celebrationManager.initializeDancingCharacters(base, enemyTanks);
        }
        // If server restarted, reset dancing state
        if (!state.gameOver) {
            celebrationManager.setDancingInitialized(false);
            celebrationManager.getDancingCharacters().clear();
        }

        // Sync victory dancing girls
        // For network clients: initialize victory celebration locally when server signals victory
        if (state.victory && !celebrationManager.isVictoryDancingInitialized()) {
            soundManager.stopGameplaySounds();
            celebrationManager.initializeVictoryCelebration(base, playerTanks.size());
        }
        // If server restarted or went to next level, reset victory state
        if (!state.victory) {
            celebrationManager.setVictoryDancingInitialized(false);
            celebrationManager.getVictoryDancingGirls().clear();
        }
    }

    private static void syncUFOState(GameState state, GameContext ctx) {
        UFOManager ufoManager = ctx.getUFOManager();

        // Sync UFO from host
        UFO syncedUfo = null;
        if (state.ufoData != null && state.ufoData.alive) {
            syncedUfo = ufoManager.getUFO();
            if (syncedUfo == null) {
                syncedUfo = new UFO(state.ufoData.x, state.ufoData.y, state.ufoData.movingRight);
            }
            syncedUfo.setX(state.ufoData.x);
            syncedUfo.setY(state.ufoData.y);
            syncedUfo.setDx(state.ufoData.dx);
            syncedUfo.setDy(state.ufoData.dy);
            syncedUfo.setHealth(state.ufoData.health);
            syncedUfo.setLifetime(state.ufoData.lifetime);
            syncedUfo.setAlive(state.ufoData.alive);
        }

        // Sync Easter egg from host
        EasterEgg syncedEgg = null;
        if (state.easterEggData != null) {
            syncedEgg = ufoManager.getEasterEgg();
            if (syncedEgg == null) {
                syncedEgg = new EasterEgg(state.easterEggData.x, state.easterEggData.y);
            }
            syncedEgg.setPosition(state.easterEggData.x, state.easterEggData.y);
            syncedEgg.setLifetime(state.easterEggData.lifetime);
        }

        ufoManager.applyNetworkState(syncedUfo, syncedEgg,
            state.ufoLostMessageTimer, state.ufoKilledMessageTimer);
    }

    private static void handleLevelTransition(GameState state, GameContext ctx,
            boolean levelChanged, boolean gameRestarted, boolean nextLevelStarted) {

        GameMap gameMap = ctx.getGameMap();
        NetworkManager network = ctx.getNetwork();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        int[] playerScores = ctx.getPlayerScores();
        int[] playerKills = ctx.getPlayerKills();
        int[] playerLevelScores = ctx.getPlayerLevelScores();
        int[][] playerKillsByType = ctx.getPlayerKillsByType();

        if (levelChanged) {
            LOG.info("Level changed from {} to {}", gameMap.getLevelNumber(), state.levelNumber);
        }
        if (gameRestarted) {
            LOG.info("Game restarted by host - resetting client state");
        }
        if (nextLevelStarted) {
            LOG.info("Next level started by host - resetting client state");
        }

        // Reset client state for new level or restart
        if (state.levelNumber == 1 && gameMap.getLevelNumber() > 1) {
            // Game was restarted to level 1 - reset scores
            for (int i = 0; i < playerScores.length; i++) {
                playerScores[i] = 0;
            }
        }

        // Reset kills and level scores for new level/restart
        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerLevelScores[i] = 0;
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }

        // Clear visual state via CelebrationManager
        ctx.getCelebrationManager().reset();
        ctx.setGameOverSoundPlayed(false);

        // Hide end game images
        ctx.hideVictoryImage();
        ctx.hideGameOverImage();

        // Reset local player tank position to FIXED start position
        int localPlayerIdx = network != null ? network.getPlayerNumber() - 1 : -1;
        if (localPlayerIdx >= 0 && localPlayerIdx < playerTanks.size()) {
            Tank myTank = playerTanks.get(localPlayerIdx);
            double[][] fixedPositions = ctx.getFixedStartPositions();
            myTank.setPosition(fixedPositions[localPlayerIdx][0], fixedPositions[localPlayerIdx][1]);
            myTank.setDirection(Direction.UP);
            myTank.giveTemporaryShield();
        }

        // Clear bullets, lasers, and power-ups for clean state
        ctx.getBullets().clear();
        ctx.getLasers().clear();
        ctx.getPowerUps().clear();

        // Clear enemy tanks - will be recreated from state
        ctx.getEnemyTanks().clear();

        // Reset spawner with proper enemy count
        ctx.setEnemySpawner(new EnemySpawner(ctx.getTotalEnemies(), 5, gameMap));

        // Reset UFO state via UFOManager
        ctx.getUFOManager().reset();

        // Play intro sound
        ctx.getSoundManager().playIntro();
    }

    private static void updateBase(GameState state, GameContext ctx,
            boolean levelChanged, boolean gameRestarted, boolean nextLevelStarted) {

        Base base = ctx.getBase();
        SoundManager soundManager = ctx.getSoundManager();

        // Update base - recreate if level changed, game restarted, next level, or if state differs
        if (levelChanged || gameRestarted || nextLevelStarted || (state.baseAlive && !base.isAlive())) {
            ctx.setBase(new Base(12 * 32, 24 * 32));
        } else if (!state.baseAlive && base.isAlive()) {
            base.destroy();
            soundManager.playBaseDestroyed();
        }
    }
}
