package com.vibetanks.network;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import com.vibetanks.core.*;

import java.util.List;
import java.util.Map;

/**
 * Builds GameState objects for network transmission.
 * Extracted from Game.java to reduce complexity.
 */
public class GameStateBuilder {

    /**
     * Build a complete GameState from game objects.
     */
    public static GameState build(
            List<Tank> playerTanks,
            int[] playerKills,
            int[] playerScores,
            int[] playerLevelScores,
            String[] playerNicknames,
            int[][] playerKillsByType,
            List<Tank> enemyTanks,
            List<Bullet> bullets,
            List<Laser> lasers,
            List<PowerUp> powerUps,
            boolean gameOver,
            boolean victory,
            EnemySpawner enemySpawner,
            GameMap gameMap,
            Base base,
            int connectedPlayers,
            PowerUpEffectManager powerUpEffectManager,
            int bossKillerPlayerIndex,
            PowerUp.Type bossKillPowerUpReward,
            CelebrationManager celebrationManager,
            List<GameState.TileChange> mapChanges,
            UFOManager ufoManager,
            List<GameState.SoundEvent> soundEvents
    ) {
        GameState state = new GameState();

        // Build player data
        buildPlayerData(state, playerTanks, playerKills, playerScores, playerLevelScores, playerNicknames, playerKillsByType);

        // Build enemy data
        buildEnemyData(state, enemyTanks);

        // Build projectile data
        buildBulletData(state, bullets);
        buildLaserData(state, lasers);

        // Build power-up data
        buildPowerUpData(state, powerUps);

        // Build game state flags
        state.gameOver = gameOver;
        state.victory = victory;
        state.remainingEnemies = enemySpawner.getRemainingEnemies();
        state.levelNumber = gameMap.getLevelNumber();

        // Build base state
        buildBaseData(state, base);

        // Build effect manager state
        state.connectedPlayers = connectedPlayers;
        state.enemyFreezeDuration = powerUpEffectManager.getEnemyFreezeDuration();
        state.playerFreezeDuration = powerUpEffectManager.getPlayerFreezeDuration();
        state.enemyTeamSpeedBoostDuration = powerUpEffectManager.getEnemyTeamSpeedBoostDuration();

        // Build boss kill info
        state.bossKillerPlayerIndex = bossKillerPlayerIndex;
        state.bossKillPowerUpReward = bossKillPowerUpReward != null ? bossKillPowerUpReward.ordinal() : -1;

        // Build map data
        buildMapData(state, gameMap, mapChanges);

        // Build celebration data
        buildCelebrationData(state, celebrationManager);

        // Build UFO data
        buildUfoData(state, ufoManager);

        // Build host settings
        state.hostPlayerSpeed = GameSettings.getPlayerSpeedMultiplier();
        state.hostEnemySpeed = GameSettings.getEnemySpeedMultiplier();
        state.hostPlayerShootSpeed = GameSettings.getPlayerShootSpeedMultiplier();
        state.hostEnemyShootSpeed = GameSettings.getEnemyShootSpeedMultiplier();

        // Add sound events for network sync
        if (soundEvents != null && !soundEvents.isEmpty()) {
            state.soundEvents.addAll(soundEvents);
        }

        return state;
    }

    private static void buildPlayerData(GameState state, List<Tank> playerTanks,
                                         int[] playerKills, int[] playerScores, int[] playerLevelScores,
                                         String[] playerNicknames, int[][] playerKillsByType) {
        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank tank = playerTanks.get(i);
            state.players[i].copyFromTank(tank, playerKills[i], playerScores[i], playerLevelScores[i],
                                          playerNicknames[i], playerKillsByType[i]);
        }
    }

    private static void buildEnemyData(GameState state, List<Tank> enemyTanks) {
        for (Tank enemy : enemyTanks) {
            if (enemy != null) {
                state.enemies.add(new GameState.EnemyData(
                    enemy.getX(),
                    enemy.getY(),
                    enemy.getDirection().ordinal(),
                    enemy.isAlive(),
                    enemy.getEnemyType().ordinal(),
                    enemy.getHealth(),
                    enemy.getMaxHealth(),
                    enemy.getTempSpeedBoost(),
                    enemy.getSpeedMultiplier()
                ));
            }
        }
    }

    private static void buildBulletData(GameState state, List<Bullet> bullets) {
        for (Bullet bullet : bullets) {
            if (bullet != null) {
                state.bullets.add(new GameState.BulletData(
                    bullet.getId(),
                    bullet.getX(),
                    bullet.getY(),
                    bullet.getDirection().ordinal(),
                    bullet.isFromEnemy(),
                    bullet.getPower(),
                    bullet.canDestroyTrees(),
                    bullet.getOwnerPlayerNumber(),
                    bullet.getSize()
                ));
            }
        }
    }

    private static void buildLaserData(GameState state, List<Laser> lasers) {
        for (Laser laser : lasers) {
            if (laser != null) {
                state.lasers.add(new GameState.LaserData(
                    laser.getId(),
                    laser.getStartX(),
                    laser.getStartY(),
                    laser.getDirection().ordinal(),
                    laser.isFromEnemy(),
                    laser.getOwnerPlayerNumber(),
                    laser.getLifetime(),
                    laser.getLength()
                ));
            }
        }
    }

    private static void buildPowerUpData(GameState state, List<PowerUp> powerUps) {
        for (PowerUp powerUp : powerUps) {
            if (powerUp != null) {
                state.powerUps.add(new GameState.PowerUpData(
                    powerUp.getId(),
                    powerUp.getX(),
                    powerUp.getY(),
                    powerUp.getType().ordinal(),
                    powerUp.getLifetime()
                ));
            }
        }
    }

    private static void buildBaseData(GameState state, Base base) {
        state.baseAlive = base.isAlive();
        state.baseShowFlag = base.isShowingFlag();
        state.baseFlagHeight = base.getFlagHeight();
        state.baseShowVictoryFlag = base.isShowingVictoryFlag();
        state.baseVictoryFlagHeight = base.getVictoryFlagHeight();
        state.baseCatMode = base.isCatMode();
    }

    private static void buildMapData(GameState state, GameMap gameMap, List<GameState.TileChange> mapChanges) {
        // Full map state for sync
        state.mapTiles = gameMap.exportTiles();

        // Burning tiles for fire animation sync
        Map<Integer, Integer> burning = gameMap.exportBurningTiles();
        for (Map.Entry<Integer, Integer> entry : burning.entrySet()) {
            int key = entry.getKey();
            int row = key / 1000;
            int col = key % 1000;
            state.burningTiles.add(new GameState.BurningTileData(row, col, entry.getValue()));
        }

        // Map changes (legacy, keeping for compatibility)
        state.tileChanges.addAll(mapChanges);
    }

    private static void buildCelebrationData(GameState state, CelebrationManager celebrationManager) {
        // Dancing characters for game over animation
        state.dancingInitialized = celebrationManager.isDancingInitialized();
        for (DancingCharacter dancer : celebrationManager.getDancingCharacters()) {
            state.dancingCharacters.add(new GameState.DancingCharacterData(
                dancer.getX(), dancer.getY(), dancer.isAlien(), dancer.getAnimFrame(),
                dancer.getDanceStyle(), dancer.getColorIndex()
            ));
        }

        // Victory dancing girls
        state.victoryDancingInitialized = celebrationManager.isVictoryDancingInitialized();
        for (DancingGirl girl : celebrationManager.getVictoryDancingGirls()) {
            state.victoryDancingGirls.add(new GameState.DancingGirlData(
                girl.getX(), girl.getY(), girl.getAnimFrame(),
                girl.getDanceStyle(), girl.getDressColorIndex(), girl.getHairColorIndex()
            ));
        }
    }

    private static void buildUfoData(GameState state, UFOManager ufoManager) {
        // UFO data
        UFO ufo = ufoManager.getUFO();
        if (ufo != null && ufo.isAlive()) {
            state.ufoData = new GameState.UFOData(
                ufo.getX(), ufo.getY(),
                ufo.getDx(), ufo.getDy(),
                ufo.isAlive(), ufo.getHealth(),
                ufo.getLifetime(), ufo.isMovingRight()
            );
        } else {
            state.ufoData = null;
        }

        // UFO message timers
        state.ufoLostMessageTimer = ufoManager.getUfoLostMessageTimer();
        state.ufoKilledMessageTimer = ufoManager.getUfoKilledMessageTimer();

        // Easter egg data
        EasterEgg easterEgg = ufoManager.getEasterEgg();
        if (easterEgg != null) {
            state.easterEggData = new GameState.EasterEggData(
                easterEgg.getX(), easterEgg.getY(), easterEgg.getLifetime()
            );
        } else {
            state.easterEggData = null;
        }
    }
}
