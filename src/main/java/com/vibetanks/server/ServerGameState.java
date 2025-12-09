package com.vibetanks.server;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.network.GameState;
import com.vibetanks.network.PlayerInput;
import com.vibetanks.util.GameLogger;

import java.util.*;

/**
 * Headless game state for dedicated server.
 * Runs all game logic without graphics/rendering.
 */
public class ServerGameState {
    private static final GameLogger LOG = GameLogger.getLogger(ServerGameState.class);

    // Use shared constants
    private static final int MAP_SIZE = GameConstants.MAP_SIZE;
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;
    private static final int TOTAL_ENEMIES = GameConstants.TOTAL_ENEMIES;
    private static final int MAX_ENEMIES_ON_SCREEN = GameConstants.MAX_ENEMIES_ON_SCREEN;

    private GameMap gameMap;
    private List<Tank> playerTanks;
    private List<Tank> enemyTanks;
    private List<Bullet> bullets;
    private List<Laser> lasers;
    private List<PowerUp> powerUps;
    private Base base;
    private EnemySpawner enemySpawner;

    // UFO bonus enemy
    private UFO ufo = null;
    private boolean ufoSpawnedThisLevel = false;

    // Easter egg collectible (spawns when UFO is killed)
    private EasterEgg easterEgg = null;

    private int currentLevel = 1;
    private boolean gameOver = false;
    private boolean victory = false;

    // Player stats (consolidated)
    private PlayerStats playerStats;
    private String[] playerNicknames;

    // Freeze timers
    private int enemyFreezeDuration = 0;
    private int playerFreezeDuration = 0;

    // Enemy team speed boost (when enemy picks up CAR)
    private int enemyTeamSpeedBoostDuration = 0;
    private int enemyWithPermanentSpeedBoostIndex = -1; // Index in enemyTanks list, -1 = none
    private static final int ENEMY_SPEED_BOOST_TIME = GameConstants.ENEMY_SPEED_BOOST_TIME;
    private static final double ENEMY_TEAM_SPEED_BOOST = GameConstants.ENEMY_TEAM_SPEED_BOOST;

    // Base protection
    private int baseProtectionDuration = 0;

    // Dancing characters state (for game over animation sync)
    private boolean dancingInitialized = false;
    private List<ServerDancingCharacter> dancingCharacters = new ArrayList<>();

    // Victory dancing girls state
    private boolean victoryDancingInitialized = false;
    private List<ServerDancingGirl> victoryDancingGirls = new ArrayList<>();

    // Sound manager (muted for server)
    private SoundManager soundManager;

    // Track actual connected players (may differ from playerTanks.size())
    private int actualConnectedPlayers = 1;

    public ServerGameState(int initialPlayers) {
        // Create a sound manager but it won't actually play sounds on server
        this.soundManager = new SoundManager();

        playerStats = new PlayerStats();
        playerNicknames = new String[4];

        actualConnectedPlayers = Math.max(1, initialPlayers);
        initialize(actualConnectedPlayers);
    }

    public void setConnectedPlayers(int count) {
        this.actualConnectedPlayers = count;
        // Add tanks for new players if needed
        while (playerTanks.size() < count && playerTanks.size() < 4) {
            int i = playerTanks.size();
            double[] pos = GameConstants.getPlayerStartPosition(i);
            Tank player = new Tank(pos[0], pos[1], Direction.UP, true, i + 1);
            player.giveTemporaryShield();
            playerTanks.add(player);
            LOG.info("Added tank for Player {}", i + 1);
        }
    }

    private void initialize(int playerCount) {
        // Reset IDs to prevent overflow after extended play
        Bullet.resetIdCounter();
        Laser.resetIdCounter();
        PowerUp.resetIdCounter();

        gameMap = new GameMap(MAP_SIZE, MAP_SIZE);
        bullets = new ArrayList<>();
        lasers = new ArrayList<>();
        powerUps = new ArrayList<>();
        base = new Base(GameConstants.BASE_X, GameConstants.BASE_Y);

        // Initialize player tanks
        playerTanks = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            double[] pos = GameConstants.getPlayerStartPosition(i);
            Tank player = new Tank(pos[0], pos[1], Direction.UP, true, i + 1);
            player.giveTemporaryShield();
            playerTanks.add(player);
        }

        // Initialize enemies
        enemyTanks = new ArrayList<>();
        enemySpawner = new EnemySpawner(TOTAL_ENEMIES, MAX_ENEMIES_ON_SCREEN, gameMap);

        // Reset game state
        gameOver = false;
        victory = false;
        enemyFreezeDuration = 0;
        playerFreezeDuration = 0;
        enemyTeamSpeedBoostDuration = 0;
        enemyWithPermanentSpeedBoostIndex = -1;
        baseProtectionDuration = 0;
        dancingInitialized = false;
        victoryDancingInitialized = false;
        dancingCharacters.clear();
        victoryDancingGirls.clear();
        ufo = null;
        ufoSpawnedThisLevel = false;
        easterEgg = null;

        LOG.info("Game initialized with {} player(s)", playerCount);
    }

    public void addPlayer(int playerNumber) {
        while (playerTanks.size() < playerNumber) {
            int idx = playerTanks.size();
            if (idx < 4) {
                double[] pos = GameConstants.getPlayerStartPosition(idx);
                Tank player = new Tank(pos[0], pos[1], Direction.UP, true, idx + 1);
                player.giveTemporaryShield();
                playerTanks.add(player);
                LOG.info("Added Player {} tank to game", idx + 1);
            }
        }
    }

    public void processInput(int playerNumber, PlayerInput input) {
        if (playerNumber < 1 || playerNumber > playerTanks.size()) return;

        Tank player = playerTanks.get(playerNumber - 1);
        if (!player.isAlive()) {
            // Handle life request
            if (input.requestLife) {
                tryTakeLifeFromTeammate(playerNumber - 1);
            }
            return;
        }

        // Movement
        List<Tank> allTanks = new ArrayList<>();
        allTanks.addAll(playerTanks);
        allTanks.addAll(enemyTanks);

        if (playerFreezeDuration <= 0) {
            if (input.up) {
                player.move(Direction.UP, gameMap, allTanks, base);
            } else if (input.down) {
                player.move(Direction.DOWN, gameMap, allTanks, base);
            } else if (input.left) {
                player.move(Direction.LEFT, gameMap, allTanks, base);
            } else if (input.right) {
                player.move(Direction.RIGHT, gameMap, allTanks, base);
            }
        }

        // Shooting
        if (input.shoot) {
            if (player.hasLaser()) {
                Laser laser = player.shootLaser(soundManager);
                if (laser != null) {
                    lasers.add(laser);
                }
            } else {
                player.shoot(bullets, soundManager);
            }
        }

        // Update nickname
        if (input.nickname != null) {
            playerNicknames[playerNumber - 1] = input.nickname;
        }

        // Accept client position with server-side validation
        if (input.posX >= 0 && input.posY >= 0) {
            // Validate position is within map bounds
            int mapPixelSize = MAP_SIZE * TILE_SIZE;
            if (input.posX < mapPixelSize - player.getSize() &&
                input.posY < mapPixelSize - player.getSize()) {

                // Anti-cheat: validate player didn't move too far (max ~10 pixels per frame at 2.5x speed)
                // Base speed is ~2-3 pixels/frame, with CAR power-ups max 2.5x = ~7.5 pixels
                // Allow some margin for network jitter: 15 pixels max per update
                double dx = Math.abs(input.posX - player.getX());
                double dy = Math.abs(input.posY - player.getY());
                double maxMovePerFrame = 15.0; // Max allowed movement per frame

                boolean validMove = (dx <= maxMovePerFrame && dy <= maxMovePerFrame) ||
                                   player.isWaitingToRespawn() || // Allow teleport after respawn
                                   (dx == 0 && dy == 0); // No movement is always valid

                // Validate position doesn't collide with walls (unless player has SHIP for water)
                if (validMove && !gameMap.checkTankCollision(input.posX, input.posY, player.getSize(), player.canSwim())) {
                    player.setPosition(input.posX, input.posY);
                }
                // Always accept direction change
                if (input.direction >= 0 && input.direction < 4) {
                    player.setDirection(Direction.values()[input.direction]);
                }
            }
        }
    }

    private void tryTakeLifeFromTeammate(int playerIndex) {
        Tank deadPlayer = playerTanks.get(playerIndex);
        if (deadPlayer.isAlive() || deadPlayer.isWaitingToRespawn()) return;

        // Find teammate with lives to spare
        for (int i = 0; i < playerTanks.size(); i++) {
            if (i == playerIndex) continue;
            Tank teammate = playerTanks.get(i);
            if (teammate.getLives() > 1) {
                teammate.setLives(teammate.getLives() - 1);
                deadPlayer.setLives(1);
                double[] pos = GameConstants.getPlayerStartPosition(playerIndex);
                deadPlayer.respawn(pos[0], pos[1]);
                LOG.info("Player {} took life from Player {}", playerIndex + 1, i + 1);
                break;
            }
        }
    }

    // Debug: count updates per second
    private int updateCount = 0;
    private long lastUpdateCountTime = System.currentTimeMillis();

    public void update() {
        // Debug: count updates per second
        updateCount++;
        long now = System.currentTimeMillis();
        if (now - lastUpdateCountTime >= 5000) {
            LOG.debug("Server updates per second: {}", updateCount / 5.0);
            updateCount = 0;
            lastUpdateCountTime = now;
        }

        // Handle game over / victory animations
        if (gameOver) {
            // Initialize and update dancing characters for game over
            initializeDancingCharacters();
            for (ServerDancingCharacter dancer : dancingCharacters) {
                dancer.update();
            }
            return;
        }
        if (victory) {
            // Initialize and update victory celebration
            initializeVictoryCelebration();
            for (ServerDancingGirl girl : victoryDancingGirls) {
                girl.update();
            }
            return;
        }

        // Update freeze timers
        if (enemyFreezeDuration > 0) enemyFreezeDuration--;
        if (playerFreezeDuration > 0) playerFreezeDuration--;
        if (baseProtectionDuration > 0) {
            baseProtectionDuration--;
            if (baseProtectionDuration == 0) {
                gameMap.setBaseProtection(GameMap.TileType.BRICK);
            }
        }

        // Update enemy team speed boost duration
        if (enemyTeamSpeedBoostDuration > 0) {
            enemyTeamSpeedBoostDuration--;
            if (enemyTeamSpeedBoostDuration == 0) {
                // Remove temporary speed boost from all enemies except the one who picked it up
                for (int i = 0; i < enemyTanks.size(); i++) {
                    if (i != enemyWithPermanentSpeedBoostIndex) {
                        enemyTanks.get(i).removeTempSpeedBoost();
                    }
                }
                enemyWithPermanentSpeedBoostIndex = -1; // Clear the index
                LOG.debug("Enemy team speed boost expired");
            }
        }

        // Spawn enemies if needed
        int enemyCountBefore = enemyTanks.size();
        if (enemyFreezeDuration <= 0) {
            enemySpawner.update(enemyTanks);
        }
        // Apply temporary speed boost to newly spawned enemies if boost is active
        if (enemyTeamSpeedBoostDuration > 0 && enemyTanks.size() > enemyCountBefore) {
            for (int i = enemyCountBefore; i < enemyTanks.size(); i++) {
                // New enemies always get temp boost (they're not the one who picked up CAR)
                enemyTanks.get(i).applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
            }
        }

        // Update all tanks
        List<Tank> allTanks = new ArrayList<>();
        allTanks.addAll(playerTanks);
        allTanks.addAll(enemyTanks);

        for (Tank player : playerTanks) {
            player.update(gameMap, bullets, soundManager, allTanks, base);
            player.updateRespawnTimer(); // Handle respawn delay
        }

        // Update enemy AI
        for (Tank enemy : enemyTanks) {
            // Boss is immune to freeze, other enemies check freeze duration
            if (enemyFreezeDuration <= 0 || enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                enemy.updateAI(gameMap, bullets, allTanks, base, soundManager);
            }
        }

        // Update bullets
        updateBullets();

        // Update lasers
        updateLasers();

        // Update map (burning tiles, etc.)
        gameMap.update();

        // Update power-ups
        updatePowerUps();

        // Update UFO
        updateUFO();

        // Update Easter egg
        updateEasterEgg();

        // Check game over FIRST (takes priority over victory)
        if (GameLogic.checkGameOver(base, playerTanks)) {
            gameOver = true;
            victory = false; // Game over takes priority
        }

        // Check victory condition ONLY if not game over
        if (!gameOver && GameLogic.checkVictory(enemySpawner, enemyTanks)) {
            victory = true;
        }
    }

    private void updateBullets() {
        Iterator<Bullet> iter = bullets.iterator();
        while (iter.hasNext()) {
            Bullet bullet = iter.next();
            bullet.update();

            // Check map collision (pass soundManager to play tree burn sound)
            if (gameMap.checkBulletCollision(bullet, soundManager)) {
                notifyBulletDestroyed(bullet);
                iter.remove();
                continue;
            }

            // Check out of bounds
            if (bullet.isOutOfBounds(MAP_SIZE * TILE_SIZE, MAP_SIZE * TILE_SIZE)) {
                if (!bullet.handleWraparound(gameMap, MAP_SIZE * TILE_SIZE, MAP_SIZE * TILE_SIZE)) {
                    notifyBulletDestroyed(bullet);
                    iter.remove();
                    continue;
                }
            }

            boolean removed = false;

            // Player bullets hit enemies
            if (!bullet.isFromEnemy()) {
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && bullet.collidesWith(enemy)) {
                        boolean dropPowerUp = enemy.damage();

                        // Handle power-up drops (POWER type drops on each hit, others on death with 30% chance)
                        if (dropPowerUp || (!enemy.isAlive() && Math.random() < 0.3)) {
                            spawnPowerUp();
                        }

                        if (!enemy.isAlive()) {
                            int killer = bullet.getOwnerPlayerNumber();
                            if (killer >= 1 && killer <= 4) {
                                playerStats.recordKill(killer - 1, enemy.getEnemyType());
                            }
                        }
                        notifyBulletDestroyed(bullet);
                        iter.remove();
                        removed = true;
                        break;
                    }
                }
            } else {
                // Enemy bullets hit players
                for (Tank player : playerTanks) {
                    if (player.isAlive() && !player.hasShield() && !player.hasPauseShield()
                            && bullet.collidesWith(player)) {
                        player.damage();
                        if (!player.isAlive() && !player.isWaitingToRespawn() && player.getLives() > 0) {
                            player.setLives(player.getLives() - 1);
                            // Respawn even when lives becomes 0 (last life with 0 displayed)
                            int idx = playerTanks.indexOf(player);
                            LOG.info("Player {} will respawn in 1 second (lives: {})", idx + 1, player.getLives());
                            double[] pos = GameConstants.getPlayerStartPosition(idx);
                            player.respawn(pos[0], pos[1]);
                        }
                        notifyBulletDestroyed(bullet);
                        iter.remove();
                        removed = true;
                        break;
                    }
                }
            }

            // Base collision
            if (!removed && bullet.collidesWith(base)) {
                base.destroy();
                gameOver = true;
                notifyBulletDestroyed(bullet);
                iter.remove();
            }
        }

        // Remove dead enemies
        enemyTanks.removeIf(e -> !e.isAlive());
    }

    private void updateLasers() {
        Iterator<Laser> iter = lasers.iterator();
        while (iter.hasNext()) {
            Laser laser = iter.next();
            laser.update();

            if (laser.isExpired()) {
                iter.remove();
                continue;
            }

            // Laser damage (3 damage to anything in path)
            if (!laser.isFromEnemy()) {
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && laser.collidesWith(enemy)) {
                        for (int i = 0; i < 3 && enemy.isAlive(); i++) {
                            enemy.damage();
                        }
                        if (!enemy.isAlive()) {
                            int killer = laser.getOwnerPlayerNumber();
                            if (killer >= 1 && killer <= 4) {
                                playerStats.recordKill(killer - 1, enemy.getEnemyType());
                            }
                        }
                    }
                }
                // UFO hit
                if (ufo != null && ufo.isAlive() && laser.collidesWithUFO(ufo)) {
                    // Laser deals 3 damage to UFO
                    for (int i = 0; i < 3 && ufo.isAlive(); i++) {
                        boolean destroyed = ufo.damage();
                        if (destroyed) {
                            // Spawn easter egg at random position
                            double[] eggPos = GameLogic.findPowerUpSpawnPosition(gameMap, TILE_SIZE);
                            easterEgg = new EasterEgg(eggPos[0], eggPos[1]);
                            LOG.info("UFO destroyed by laser! Easter egg spawned at random position");
                            break;
                        }
                    }
                }
                // Base hit
                if (laser.collidesWithBase(base) && base.isAlive()) {
                    base.destroy();
                    gameOver = true;
                }
            }
        }

        enemyTanks.removeIf(e -> !e.isAlive());
    }

    private void updatePowerUps() {
        Iterator<PowerUp> iter = powerUps.iterator();
        while (iter.hasNext()) {
            PowerUp powerUp = iter.next();
            powerUp.update();

            if (powerUp.isExpired()) {
                iter.remove();
                continue;
            }

            boolean collected = false;

            // Check player collection
            for (Tank player : playerTanks) {
                if (player.isAlive() && powerUp.collidesWith(player)) {
                    applyPowerUp(powerUp, player);
                    iter.remove();
                    collected = true;
                    break;
                }
            }

            // Check enemy collection
            if (!collected) {
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && powerUp.collidesWith(enemy)) {
                        applyEnemyPowerUp(powerUp, enemy);
                        iter.remove();
                        break;
                    }
                }
            }
        }
    }

    private void updateUFO() {
        // UFO spawns when a player has machinegun AND has 5+ total kills
        // Also requires 2-10 enemies remaining
        int remaining = enemySpawner.getRemainingEnemies();
        if (ufo == null && !ufoSpawnedThisLevel && remaining > 1 && remaining <= 10) {
            for (int i = 0; i < playerTanks.size() && i < 4; i++) {
                Tank player = playerTanks.get(i);
                int totalKills = playerStats.getKills(i);
                if (player.getMachinegunCount() > 0 && totalKills >= 5) {
                    int mapPixelSize = MAP_SIZE * TILE_SIZE;
                    boolean movingRight = Math.random() < 0.5;
                    double startX = movingRight ? -48 : mapPixelSize;
                    double startY = 50 + Math.random() * (mapPixelSize - 150);
                    ufo = new UFO(startX, startY, movingRight);
                    ufoSpawnedThisLevel = true;
                    LOG.info("UFO spawn triggered! Player {} has machinegun and {} total kills", i + 1, totalKills);
                    break;
                }
            }
        }

        if (ufo != null && ufo.isAlive()) {
            int mapPixelSize = MAP_SIZE * TILE_SIZE;
            ufo.update(bullets, mapPixelSize, mapPixelSize, soundManager);

            // Check bullet hits on UFO
            Iterator<Bullet> iter = bullets.iterator();
            while (iter.hasNext()) {
                Bullet bullet = iter.next();
                if (ufo.collidesWith(bullet)) {
                    boolean destroyed = ufo.damage();
                    notifyBulletDestroyed(bullet);
                    iter.remove();
                    if (destroyed) {
                        // Spawn easter egg at random position
                        double[] eggPos = GameLogic.findPowerUpSpawnPosition(gameMap, TILE_SIZE);
                        easterEgg = new EasterEgg(eggPos[0], eggPos[1]);
                        LOG.info("UFO destroyed! Easter egg spawned at random position");
                    }
                    break;
                }
            }
        }

        // Remove dead/expired UFO
        if (ufo != null && !ufo.isAlive()) {
            ufo = null;
        }
    }

    private void updateEasterEgg() {
        if (easterEgg == null) return;

        easterEgg.update();

        if (easterEgg.isExpired()) {
            easterEgg = null;
            return;
        }

        // Check collection using shared logic
        int collectionResult = GameLogic.checkEasterEggCollection(easterEgg, playerTanks, enemyTanks);

        if (collectionResult > 0) {
            // Player collected (result is playerIndex + 1)
            int playerIndex = collectionResult - 1;
            Tank player = playerTanks.get(playerIndex);
            GameLogic.applyEasterEggEffect(enemyTanks, true);
            player.setLives(player.getLives() + 3);
            // Turn base into cat if boss has spawned (remaining == 0 means boss was the last spawned)
            if (enemySpawner.getRemainingEnemies() == 0) {
                base.setCatMode(true);
                LOG.info("Player collected Easter egg after boss spawn! Base transformed to cat!");
            }
            easterEgg.collect();
            easterEgg = null;
            LOG.info("Player collected Easter egg! Enemies transformed to POWER, +3 lives.");
        } else if (collectionResult < 0) {
            // Enemy collected
            GameLogic.applyEasterEggEffect(enemyTanks, false);
            easterEgg.collect();
            easterEgg = null;
            LOG.info("Enemy collected Easter egg! Enemies transformed to HEAVY.");
        }
    }

    private void applyEnemyPowerUp(PowerUp powerUp, Tank enemy) {
        switch (powerUp.getType()) {
            case SHOVEL -> {
                // Enemy removes base protection
                gameMap.setBaseProtection(GameMap.TileType.EMPTY);
                baseProtectionDuration = 0;
            }
            case FREEZE -> {
                // Freeze players for 10 seconds
                playerFreezeDuration = GameConstants.FREEZE_TIME;
                LOG.info("FREEZE: Players frozen for 10 seconds!");
            }
            case BOMB -> {
                // Damage all players
                LOG.info("BOMB collected by enemy - damaging all players!");
                for (int i = 0; i < playerTanks.size(); i++) {
                    Tank player = playerTanks.get(i);
                    if (player.isAlive()) {
                        player.setShield(false);
                        player.setPauseShield(false);
                        player.damage();
                        // Handle life decrement and respawn after bomb damage
                        if (!player.isAlive() && !player.isWaitingToRespawn() && player.getLives() > 0) {
                            player.setLives(player.getLives() - 1);
                            // Respawn even when lives becomes 0 (last life with 0 displayed)
                            LOG.info("Player {} will respawn in 1 second (lives: {})", i + 1, player.getLives());
                            double[] pos = GameConstants.getPlayerStartPosition(i);
                            player.respawn(pos[0], pos[1]);
                        }
                    }
                }
            }
            case CAR -> {
                // All enemies get temporary speed boost for 30 seconds
                powerUp.applyEffect(enemy); // Give permanent boost to this enemy
                enemyWithPermanentSpeedBoostIndex = enemyTanks.indexOf(enemy);
                enemyTeamSpeedBoostDuration = ENEMY_SPEED_BOOST_TIME;
                // Give temporary boost to all other enemies
                for (int i = 0; i < enemyTanks.size(); i++) {
                    Tank otherEnemy = enemyTanks.get(i);
                    if (i != enemyWithPermanentSpeedBoostIndex && otherEnemy.isAlive()) {
                        otherEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                    }
                }
                LOG.info("CAR: All enemies get speed boost for 30 seconds!");
            }
            default -> powerUp.applyEffect(enemy);
        }
    }

    private void applyPowerUp(PowerUp powerUp, Tank player) {
        switch (powerUp.getType()) {
            case SHOVEL -> {
                gameMap.setBaseProtection(GameMap.TileType.STEEL);
                baseProtectionDuration = GameConstants.BASE_PROTECTION_TIME;
            }
            case FREEZE -> {
                enemyFreezeDuration = GameConstants.FREEZE_TIME;
            }
            case BOMB -> {
                for (Tank enemy : enemyTanks) {
                    while (enemy.isAlive()) enemy.damage();
                }
            }
            default -> powerUp.applyEffect(player);
        }
    }

    private void spawnPowerUp() {
        double[] pos = GameLogic.findPowerUpSpawnPosition(gameMap, TILE_SIZE);
        powerUps.add(new PowerUp(pos[0], pos[1]));
    }

    private void notifyBulletDestroyed(Bullet bullet) {
        GameLogic.notifyBulletDestroyed(bullet, playerTanks);
    }

    public GameState buildNetworkState() {
        GameState state = new GameState();
        state.levelNumber = currentLevel;
        state.gameOver = gameOver;
        state.victory = victory;
        state.baseAlive = base.isAlive();
        state.remainingEnemies = enemySpawner.getRemainingEnemies();
        state.connectedPlayers = actualConnectedPlayers;

        // Players - use array assignment
        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank tank = playerTanks.get(i);
            state.players[i].copyFromTank(tank, playerStats.getKills(i), playerStats.getScore(i),
                playerStats.getLevelScore(i), playerNicknames[i], playerStats.getKillsByTypeArray(i));
        }

        // Enemies
        for (Tank enemy : enemyTanks) {
            state.enemies.add(new GameState.EnemyData(
                enemy.getX(), enemy.getY(),
                enemy.getDirection().ordinal(),
                enemy.isAlive(),
                enemy.getEnemyType().ordinal(),
                enemy.getHealth(),
                enemy.getMaxHealth(),
                enemy.getTempSpeedBoost(),
                enemy.getSpeedMultiplier()
            ));
        }

        // Bullets
        for (Bullet bullet : bullets) {
            state.bullets.add(new GameState.BulletData(
                bullet.getId(),
                bullet.getX(), bullet.getY(),
                bullet.getDirection().ordinal(),
                bullet.isFromEnemy(),
                bullet.getPower(),
                bullet.canDestroyTrees(),
                bullet.getOwnerPlayerNumber(),
                bullet.getSize()
            ));
        }

        // Lasers
        for (Laser laser : lasers) {
            state.lasers.add(new GameState.LaserData(
                laser.getId(),
                laser.getStartX(), laser.getStartY(),
                laser.getDirection().ordinal(),
                laser.isFromEnemy(),
                laser.getOwnerPlayerNumber(),
                laser.getLifetime(),
                laser.getLength()
            ));
        }

        // Power-ups
        for (PowerUp powerUp : powerUps) {
            state.powerUps.add(new GameState.PowerUpData(
                powerUp.getId(),
                powerUp.getX(), powerUp.getY(),
                powerUp.getType().ordinal(),
                powerUp.getLifetime()
            ));
        }

        // Map tiles (for destroyed walls)
        state.mapTiles = gameMap.exportTiles();

        // Burning tiles for fire animation
        java.util.Map<Integer, Integer> burning = gameMap.exportBurningTiles();
        for (java.util.Map.Entry<Integer, Integer> entry : burning.entrySet()) {
            int key = entry.getKey();
            int row = key / 1000;
            int col = key % 1000;
            state.burningTiles.add(new GameState.BurningTileData(row, col, entry.getValue()));
        }

        // Base state
        state.baseShowFlag = base.isShowingFlag();
        state.baseFlagHeight = base.getFlagHeight();
        state.baseShowVictoryFlag = base.isShowingVictoryFlag();
        state.baseVictoryFlagHeight = base.getVictoryFlagHeight();

        // Freeze durations
        state.enemyFreezeDuration = enemyFreezeDuration;
        state.playerFreezeDuration = playerFreezeDuration;
        state.enemyTeamSpeedBoostDuration = enemyTeamSpeedBoostDuration;

        // Dancing state for game over animation
        state.dancingInitialized = dancingInitialized;
        for (ServerDancingCharacter dancer : dancingCharacters) {
            state.dancingCharacters.add(new GameState.DancingCharacterData(
                dancer.x, dancer.y, dancer.isAlien, dancer.animFrame, dancer.danceStyle, dancer.colorIndex
            ));
        }

        // Victory dancing girls
        state.victoryDancingInitialized = victoryDancingInitialized;
        for (ServerDancingGirl girl : victoryDancingGirls) {
            state.victoryDancingGirls.add(new GameState.DancingGirlData(
                girl.x, girl.y, girl.animFrame, girl.danceStyle, girl.dressColorIndex, girl.hairColorIndex
            ));
        }

        // UFO state
        if (ufo != null && ufo.isAlive()) {
            state.ufoData = new GameState.UFOData(
                ufo.getX(), ufo.getY(),
                ufo.getDx(), ufo.getDy(),
                ufo.isAlive(),
                ufo.getHealth(),
                ufo.getLifetime(),
                ufo.isMovingRight()
            );
        } else {
            state.ufoData = null;
        }

        // Easter egg state
        if (easterEgg != null) {
            state.easterEggData = new GameState.EasterEggData(
                easterEgg.getX(), easterEgg.getY(), easterEgg.getLifetime()
            );
        } else {
            state.easterEggData = null;
        }

        // Base cat mode (when player collects easter egg after boss spawned)
        state.baseCatMode = base.isCatMode();

        // Host settings - use server's GameSettings
        state.hostPlayerSpeed = GameSettings.getPlayerSpeedMultiplier();
        state.hostEnemySpeed = GameSettings.getEnemySpeedMultiplier();
        state.hostPlayerShootSpeed = GameSettings.getPlayerShootSpeedMultiplier();
        state.hostEnemyShootSpeed = GameSettings.getEnemyShootSpeedMultiplier();

        return state;
    }

    public void restartLevel() {
        LOG.info("Restarting level {}", currentLevel);

        // Reset kills for this level (scores persist across restarts)
        playerStats.resetKillsOnly();
        playerStats.resetLevelScores();

        initialize(playerTanks.size());
    }

    public void nextLevel() {
        currentLevel++;
        LOG.info("Starting level {}", currentLevel);

        // Reset kills and level scores, keep total scores
        playerStats.resetKillsOnly();
        playerStats.resetLevelScores();

        initialize(playerTanks.size());
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isVictory() { return victory; }
    public int getCurrentLevel() { return currentLevel; }
    public int getRemainingEnemies() {
        return enemySpawner != null ? enemySpawner.getRemainingEnemies() + enemyTanks.size() : 0;
    }

    /**
     * Initialize dancing characters for game over animation.
     * Called when base is destroyed.
     */
    private void initializeDancingCharacters() {
        if (dancingInitialized) return;
        dancingInitialized = true;

        // Raise the skull flag on the destroyed base
        base.raiseFlag();

        // Create dancing aliens/humans from enemy tank positions
        if (!enemyTanks.isEmpty()) {
            for (Tank enemy : enemyTanks) {
                // Each enemy tank spawns 1-2 characters
                int numCharacters = 1 + GameConstants.RANDOM.nextInt(2);
                for (int i = 0; i < numCharacters; i++) {
                    double offsetX = (GameConstants.RANDOM.nextDouble() - 0.5) * 40;
                    double offsetY = (GameConstants.RANDOM.nextDouble() - 0.5) * 40;
                    boolean isAlien = GameConstants.RANDOM.nextBoolean();
                    int danceStyle = GameConstants.RANDOM.nextInt(3);
                    int colorIndex = GameConstants.RANDOM.nextInt(4); // 4 colors available
                    dancingCharacters.add(new ServerDancingCharacter(
                        enemy.getX() + 16 + offsetX,
                        enemy.getY() + 16 + offsetY,
                        isAlien, danceStyle, colorIndex
                    ));
                }
            }
        }

        // Also spawn some around the destroyed base
        double baseX = base.getX() + 32;
        double baseY = base.getY() + 32;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 * i) / 6;
            double radius = 60 + GameConstants.RANDOM.nextDouble() * 30;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + Math.sin(angle) * radius;
            boolean isAlien = GameConstants.RANDOM.nextBoolean();
            int danceStyle = GameConstants.RANDOM.nextInt(3);
            int colorIndex = GameConstants.RANDOM.nextInt(4);
            dancingCharacters.add(new ServerDancingCharacter(x, y, isAlien, danceStyle, colorIndex));
        }

        LOG.info("Dancing characters initialized: {}", dancingCharacters.size());
    }

    /**
     * Initialize victory celebration with dancing girls.
     * Called when all enemies are defeated.
     */
    private void initializeVictoryCelebration() {
        if (victoryDancingInitialized) return;
        victoryDancingInitialized = true;

        // Raise the victory flag on the base
        base.raiseVictoryFlag();

        // Get number of active players
        int activePlayers = playerTanks.size();

        // Spawn dancing girls based on player count (1-2 girls per player)
        int girlCount = activePlayers + GameConstants.RANDOM.nextInt(activePlayers + 1);

        // Position girls around the base
        double baseX = base.getX() + 16;
        double baseY = base.getY() - 20;

        for (int i = 0; i < girlCount; i++) {
            // Spread girls in a semi-circle above the base
            double angle = Math.PI + (Math.PI * (i + 0.5) / girlCount);
            double radius = 80 + GameConstants.RANDOM.nextDouble() * 40;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + Math.sin(angle) * radius * 0.6;
            int danceStyle = GameConstants.RANDOM.nextInt(4);
            int dressColorIndex = GameConstants.RANDOM.nextInt(6); // 6 dress colors
            int hairColorIndex = GameConstants.RANDOM.nextInt(5);  // 5 hair colors
            int startFrame = GameConstants.RANDOM.nextInt(60);     // Random start frame

            victoryDancingGirls.add(new ServerDancingGirl(x, y, startFrame, danceStyle, dressColorIndex, hairColorIndex));
        }

        LOG.info("Victory celebration initialized with {} dancing girls", girlCount);
    }

    /**
     * Server-side dancing character for game over animation.
     * Stores position and visual attributes to sync to clients.
     */
    private static class ServerDancingCharacter {
        double x, y;
        boolean isAlien;
        int animFrame;
        int danceStyle;
        int colorIndex;

        ServerDancingCharacter(double x, double y, boolean isAlien, int danceStyle, int colorIndex) {
            this.x = x;
            this.y = y;
            this.isAlien = isAlien;
            this.animFrame = 0;
            this.danceStyle = danceStyle;
            this.colorIndex = colorIndex;
        }

        void update() {
            animFrame++;
        }
    }

    /**
     * Server-side dancing girl for victory animation.
     * Stores position and visual attributes to sync to clients.
     */
    private static class ServerDancingGirl {
        double x, y;
        int animFrame;
        int danceStyle;
        int dressColorIndex;
        int hairColorIndex;

        ServerDancingGirl(double x, double y, int animFrame, int danceStyle, int dressColorIndex, int hairColorIndex) {
            this.x = x;
            this.y = y;
            this.animFrame = animFrame;
            this.danceStyle = danceStyle;
            this.dressColorIndex = dressColorIndex;
            this.hairColorIndex = hairColorIndex;
        }

        void update() {
            animFrame++;
        }
    }
}
