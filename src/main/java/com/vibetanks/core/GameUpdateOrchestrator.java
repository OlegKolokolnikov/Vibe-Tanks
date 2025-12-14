package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.network.GameState;
import com.vibetanks.util.GameLogger;

import java.util.Iterator;
import java.util.List;

/**
 * Orchestrates game update logic that is shared between Game.java (client) and ServerGameState.java (server).
 * This reduces duplication and ensures consistent behavior across both contexts.
 */
public class GameUpdateOrchestrator {
    private static final GameLogger LOG = GameLogger.getLogger(GameUpdateOrchestrator.class);
    private static final double ENEMY_TEAM_SPEED_BOOST = 0.5;

    /**
     * Context interface for game state access.
     * Implemented by Game and ServerGameState to provide access to game state.
     */
    public interface GameContext {
        GameMap getGameMap();
        Base getBase();
        List<Tank> getPlayerTanks();
        List<Tank> getEnemyTanks();
        List<Bullet> getBullets();
        List<Laser> getLasers();
        List<PowerUp> getPowerUps();
        SoundManager getSoundManager();
        EnemySpawner getEnemySpawner();
        PowerUpEffectManager getPowerUpEffectManager();
        int getWidth();
        int getHeight();

        // UFO-related
        UFO getUFO();
        EasterEgg getEasterEgg();
        void handleUFODestroyed(int killerPlayer, double eggX, double eggY);
        boolean isUfoSpawnedThisLevel();

        // Scoring and stats
        void addScore(int playerIndex, int points);
        void recordKill(int playerIndex, Tank.EnemyType type);
        void setBossKillReward(int playerIndex, PowerUp.Type reward);

        // State changes
        void setGameOver(boolean gameOver);
        void setVictory(boolean victory);

        // Power-up spawn position
        double[] getRandomPowerUpSpawnPosition();

        // Bullet destruction notification
        void notifyBulletDestroyed(Bullet bullet);

        // Tank cache for collision detection
        List<Tank> getAllTanksCache();

        // Sound event for network sync
        void queueSoundEvent(GameState.SoundType type);
    }

    /**
     * Result of update processing.
     */
    public static class UpdateResult {
        public boolean victoryConditionMet;
        public boolean gameOver;
        public boolean victory;
    }

    /**
     * Update player tanks: handle alive state, respawn timers, and death processing.
     */
    public static void updatePlayerTanks(GameContext ctx, List<Tank> allTanks, double[][] respawnPositions) {
        List<Tank> playerTanks = ctx.getPlayerTanks();
        GameMap gameMap = ctx.getGameMap();
        List<Bullet> bullets = ctx.getBullets();
        SoundManager soundManager = ctx.getSoundManager();
        Base base = ctx.getBase();

        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive()) {
                player.update(gameMap, bullets, soundManager, allTanks, base);
            } else if (player.isWaitingToRespawn()) {
                // Track if player completes respawn this frame
                boolean wasWaiting = player.isWaitingToRespawn();
                player.updateRespawnTimer();
                // Check if player just respawned (was waiting, now alive)
                if (wasWaiting && player.isAlive()) {
                    // Clear freeze in very easy mode when player respawns
                    ctx.getPowerUpEffectManager().clearPlayerFreezeOnRespawnIfVeryEasy();
                }
            } else if (player.getLives() > 0) {
                // Lives already decremented by damage(), just respawn
                double respawnX = respawnPositions[i][0];
                double respawnY = respawnPositions[i][1];
                LOG.info("Player {} will respawn at: {}, {} (lives left: {})",
                    i + 1, respawnX, respawnY, player.getLives());
                player.respawn(respawnX, respawnY);
            } else if (player.getLives() <= 0) {
                // Player has no lives left - game over for this player
                LOG.info("Player {} lost their last life!", i + 1);
            }
        }
    }

    /**
     * Update enemy tanks with AI (respecting freeze state).
     */
    public static void updateEnemyTanks(GameContext ctx, List<Tank> allTanks) {
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        GameMap gameMap = ctx.getGameMap();
        List<Bullet> bullets = ctx.getBullets();
        SoundManager soundManager = ctx.getSoundManager();
        Base base = ctx.getBase();
        PowerUpEffectManager effectManager = ctx.getPowerUpEffectManager();

        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                // BOSS tank is immune to freeze
                if (!effectManager.areEnemiesFrozen() || tank.getEnemyType() == Tank.EnemyType.BOSS) {
                    tank.updateAI(gameMap, bullets, allTanks, base, soundManager);
                }
            }
        }
    }

    /**
     * Update enemy spawning and apply speed boosts to new enemies.
     */
    public static void updateEnemySpawning(GameContext ctx) {
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        EnemySpawner spawner = ctx.getEnemySpawner();
        PowerUpEffectManager effectManager = ctx.getPowerUpEffectManager();

        int enemyCountBefore = enemyTanks.size();
        spawner.update(enemyTanks);

        // Apply temporary speed boost to newly spawned enemies if boost is active
        if (effectManager.isEnemySpeedBoostActive() && enemyTanks.size() > enemyCountBefore) {
            for (int i = enemyCountBefore; i < enemyTanks.size(); i++) {
                Tank newEnemy = enemyTanks.get(i);
                if (newEnemy != effectManager.getEnemyWithPermanentSpeedBoost()) {
                    newEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                }
            }
        }
    }

    /**
     * Update freeze timers and enemy speed boost.
     */
    public static void updateEffectTimers(GameContext ctx) {
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        PowerUpEffectManager effectManager = ctx.getPowerUpEffectManager();

        effectManager.updateFreezeTimers();

        boolean wasSpeedBoostActive = effectManager.isEnemySpeedBoostActive();
        effectManager.updateEnemySpeedBoost();
        if (wasSpeedBoostActive && !effectManager.isEnemySpeedBoostActive()) {
            // Speed boost expired - remove from all enemies except original collector
            for (Tank enemy : enemyTanks) {
                if (enemy != effectManager.getEnemyWithPermanentSpeedBoost()) {
                    enemy.removeTempSpeedBoost();
                }
            }
            LOG.info("Enemy team speed boost expired");
        }
    }

    /**
     * Process bullet collisions and handle results.
     */
    public static void processBullets(GameContext ctx) {
        List<Bullet> bullets = ctx.getBullets();
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        List<PowerUp> powerUps = ctx.getPowerUps();
        GameMap gameMap = ctx.getGameMap();
        Base base = ctx.getBase();
        SoundManager soundManager = ctx.getSoundManager();
        UFO ufo = ctx.getUFO();
        int width = ctx.getWidth();
        int height = ctx.getHeight();

        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();

            ProjectileHandler.BulletCollisionResult result = ProjectileHandler.processBullet(
                bullet, gameMap, enemyTanks, playerTanks, base, ufo, width, height, soundManager);

            if (result.shouldRemove) {
                ctx.notifyBulletDestroyed(bullet);
                bulletIterator.remove();

                // Handle UFO destruction
                if (result.ufoDestroyed) {
                    int killerPlayer = result.killerPlayerNumber;
                    if (killerPlayer >= 1 && killerPlayer <= 4) {
                        ctx.addScore(killerPlayer - 1, 20);
                        LOG.info("UFO destroyed by Player {}", killerPlayer);
                    }
                    double[] eggPos = ctx.getRandomPowerUpSpawnPosition();
                    ctx.handleUFODestroyed(killerPlayer, eggPos[0], eggPos[1]);
                }

                // Handle enemy killed
                if (result.enemyKilled && result.killedEnemy != null) {
                    handleEnemyKilled(ctx, result.killerPlayerNumber, result.killedEnemy);
                }

                // Handle power-up drop
                if (result.shouldDropPowerUp) {
                    double[] spawnPos = ctx.getRandomPowerUpSpawnPosition();
                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                    soundManager.playPowerUpSpawn();
                    ctx.queueSoundEvent(GameState.SoundType.POWERUP_SPAWN);
                }

                // Handle player killed - spawn power-up in 3+ player mode
                if (result.playerKilled && playerTanks.size() > 2) {
                    double[] spawnPos = ctx.getRandomPowerUpSpawnPosition();
                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                    soundManager.playPowerUpSpawn();
                    ctx.queueSoundEvent(GameState.SoundType.POWERUP_SPAWN);
                    LOG.info("Power-up spawned for killed player (3+ players mode)");
                }

                // Handle base hit
                if (result.hitBase) {
                    base.destroy();
                    soundManager.playBaseDestroyed();
                    ctx.setGameOver(true);
                }
            }
        }

        // Check bullet-to-bullet collisions
        ProjectileHandler.processBulletToBulletCollisions(bullets, playerTanks);
    }

    /**
     * Process laser collisions and handle results.
     */
    public static void processLasers(GameContext ctx) {
        List<Laser> lasers = ctx.getLasers();
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        List<PowerUp> powerUps = ctx.getPowerUps();
        Base base = ctx.getBase();
        SoundManager soundManager = ctx.getSoundManager();
        UFO ufo = ctx.getUFO();

        Iterator<Laser> laserIterator = lasers.iterator();
        while (laserIterator.hasNext()) {
            Laser laser = laserIterator.next();
            laser.update();

            if (laser.isExpired()) {
                laserIterator.remove();
                continue;
            }

            ProjectileHandler.LaserCollisionResult result = ProjectileHandler.processLaser(
                laser, enemyTanks, playerTanks, base, ufo, soundManager);

            // Handle enemy killed
            if (result.enemyKilled && result.killedEnemy != null) {
                int killerPlayer = result.killerPlayerNumber;
                Tank enemy = result.killedEnemy;
                if (killerPlayer >= 1 && killerPlayer <= 4) {
                    ctx.recordKill(killerPlayer - 1, enemy.getEnemyType());
                    ctx.addScore(killerPlayer - 1, GameConstants.getScoreForEnemyType(enemy.getEnemyType()));
                    if (result.isBossKill) {
                        Tank killer = playerTanks.get(killerPlayer - 1);
                        PowerUp.Type reward = applyRandomPowerUp(killer);
                        ctx.setBossKillReward(killerPlayer - 1, reward);
                    }
                }
            }

            // Handle power-up drop
            if (result.shouldDropPowerUp) {
                double[] spawnPos = ctx.getRandomPowerUpSpawnPosition();
                powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                soundManager.playPowerUpSpawn();
                ctx.queueSoundEvent(GameState.SoundType.POWERUP_SPAWN);
            }

            // Handle UFO destruction
            if (result.ufoDestroyed) {
                int killerPlayer = result.killerPlayerNumber;
                if (killerPlayer >= 1 && killerPlayer <= 4) {
                    ctx.addScore(killerPlayer - 1, 20);
                }
                double[] eggPos = ctx.getRandomPowerUpSpawnPosition();
                ctx.handleUFODestroyed(killerPlayer, eggPos[0], eggPos[1]);
            }

            // Handle base hit
            if (result.hitBase) {
                base.destroy();
                soundManager.playBaseDestroyed();
                ctx.setGameOver(true);
            }
        }
    }

    /**
     * Process power-up collection by players and enemies.
     */
    public static void processPowerUps(GameContext ctx) {
        List<PowerUp> powerUps = ctx.getPowerUps();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        List<Tank> enemyTanks = ctx.getEnemyTanks();
        GameMap gameMap = ctx.getGameMap();
        SoundManager soundManager = ctx.getSoundManager();
        PowerUpEffectManager effectManager = ctx.getPowerUpEffectManager();

        Iterator<PowerUp> powerUpIterator = powerUps.iterator();
        while (powerUpIterator.hasNext()) {
            PowerUp powerUp = powerUpIterator.next();
            powerUp.update();

            // Check player collection
            PowerUpHandler.PlayerCollectionResult playerResult =
                PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            if (playerResult.collected) {
                // Award 1 point for collecting power-up
                if (playerResult.collectorPlayerIndex >= 0) {
                    ctx.addScore(playerResult.collectorPlayerIndex, 1);
                }
                if (playerResult.activateShovel) {
                    // Check if player has ground shovel upgrade (1000 points)
                    boolean useGround = playerResult.collectorPlayerIndex >= 0 &&
                        playerResult.collectorPlayerIndex < playerTanks.size() &&
                        playerTanks.get(playerResult.collectorPlayerIndex).hasGroundShovel();
                    effectManager.activateBaseProtection(gameMap, useGround);
                } else if (playerResult.activateFreeze) {
                    effectManager.activateEnemyFreeze();
                } else if (playerResult.activateBomb) {
                    PowerUpHandler.applyPlayerBomb(enemyTanks, soundManager);
                }
                powerUpIterator.remove();
                continue;
            }

            // Check enemy collection
            PowerUpHandler.EnemyCollectionResult enemyResult =
                PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            if (enemyResult.collected) {
                if (enemyResult.removeShovel) {
                    effectManager.removeBaseProtection(gameMap);
                } else if (enemyResult.activateFreeze) {
                    effectManager.activatePlayerFreeze();
                } else if (enemyResult.activateBomb) {
                    PowerUpHandler.applyEnemyBomb(playerTanks, soundManager);
                } else if (enemyResult.activateCar) {
                    effectManager.activateEnemySpeedBoost(enemyResult.collectorEnemy);
                    PowerUpHandler.applyEnemyCarSpeedBoost(enemyTanks, enemyResult.collectorEnemy, ENEMY_TEAM_SPEED_BOOST);
                }
                powerUpIterator.remove();
                continue;
            }

            // Remove expired power-ups
            if (powerUp.isExpired()) {
                powerUpIterator.remove();
            }
        }
    }

    /**
     * Handle enemy killed - record stats, award points, handle boss rewards.
     */
    private static void handleEnemyKilled(GameContext ctx, int killerPlayer, Tank enemy) {
        List<Tank> playerTanks = ctx.getPlayerTanks();

        if (killerPlayer >= 1 && killerPlayer <= 4) {
            ctx.recordKill(killerPlayer - 1, enemy.getEnemyType());
            ctx.addScore(killerPlayer - 1, GameConstants.getScoreForEnemyType(enemy.getEnemyType()));

            if (enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                LOG.info("BOSS killed by Player {}", killerPlayer);
                Tank killer = playerTanks.get(killerPlayer - 1);
                PowerUp.Type reward = applyRandomPowerUp(killer);
                ctx.setBossKillReward(killerPlayer - 1, reward);
            }
        }
    }

    /**
     * Apply a random power-up to a tank.
     */
    public static PowerUp.Type applyRandomPowerUp(Tank tank) {
        return PowerUpHandler.applyRandomBossReward(tank);
    }

    /**
     * Check and update victory condition.
     * @return true if victory was achieved this frame
     */
    public static boolean checkVictoryCondition(EnemySpawner spawner, List<Tank> enemyTanks,
                                                 boolean victoryConditionMet, int victoryDelayTimer,
                                                 int victoryDelay) {
        if (GameLogic.checkVictory(spawner, enemyTanks)) {
            if (!victoryConditionMet) {
                LOG.info("All enemies defeated! Victory in {} seconds...", victoryDelay / 60);
                return true;
            }
        }
        return false;
    }
}
