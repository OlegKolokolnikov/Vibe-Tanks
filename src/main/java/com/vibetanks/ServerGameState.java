package com.vibetanks;

import java.util.*;

/**
 * Headless game state for dedicated server.
 * Runs all game logic without graphics/rendering.
 */
public class ServerGameState {
    private static final int MAP_SIZE = 26;
    private static final int TILE_SIZE = 32;
    private static final int TOTAL_ENEMIES = 20;
    private static final int MAX_ENEMIES_ON_SCREEN = 5;

    // Fixed start positions for each player
    private static final double[][] PLAYER_START_POSITIONS = {
        {8 * TILE_SIZE, 24 * TILE_SIZE},   // Player 1
        {16 * TILE_SIZE, 24 * TILE_SIZE},  // Player 2
        {9 * TILE_SIZE, 24 * TILE_SIZE},   // Player 3
        {15 * TILE_SIZE, 24 * TILE_SIZE}   // Player 4
    };

    private GameMap gameMap;
    private List<Tank> playerTanks;
    private List<Tank> enemyTanks;
    private List<Bullet> bullets;
    private List<Laser> lasers;
    private List<PowerUp> powerUps;
    private Base base;
    private EnemySpawner enemySpawner;

    private int currentLevel = 1;
    private boolean gameOver = false;
    private boolean victory = false;

    // Player stats
    private int[] playerKills;
    private int[] playerScores;
    private int[][] playerKillsByType;
    private String[] playerNicknames;

    // Freeze timers
    private int enemyFreezeDuration = 0;
    private int playerFreezeDuration = 0;

    // Enemy team speed boost (when enemy picks up CAR)
    private int enemyTeamSpeedBoostDuration = 0;
    private Tank enemyWithPermanentSpeedBoost = null;
    private static final int ENEMY_SPEED_BOOST_TIME = 1800; // 30 seconds at 60 FPS
    private static final double ENEMY_TEAM_SPEED_BOOST = 0.3; // 30% speed boost

    // Base protection
    private int baseProtectionDuration = 0;

    // Dancing characters state (for game over animation sync)
    private boolean dancingInitialized = false;

    // Sound manager (muted for server)
    private SoundManager soundManager;

    public ServerGameState(int initialPlayers) {
        // Create a sound manager but it won't actually play sounds on server
        this.soundManager = new SoundManager();

        playerKills = new int[4];
        playerScores = new int[4];
        playerKillsByType = new int[4][6];
        playerNicknames = new String[4];

        initialize(Math.max(1, initialPlayers));
    }

    private void initialize(int playerCount) {
        gameMap = new GameMap(MAP_SIZE, MAP_SIZE);
        bullets = new ArrayList<>();
        lasers = new ArrayList<>();
        powerUps = new ArrayList<>();
        base = new Base(12 * TILE_SIZE, 24 * TILE_SIZE);

        // Initialize player tanks
        playerTanks = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            Tank player = new Tank(
                PLAYER_START_POSITIONS[i][0],
                PLAYER_START_POSITIONS[i][1],
                Direction.UP, true, i + 1
            );
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
        enemyWithPermanentSpeedBoost = null;
        baseProtectionDuration = 0;
        dancingInitialized = false;

        System.out.println("[*] Game initialized with " + playerCount + " player(s)");
    }

    public void addPlayer(int playerNumber) {
        while (playerTanks.size() < playerNumber) {
            int idx = playerTanks.size();
            if (idx < 4) {
                Tank player = new Tank(
                    PLAYER_START_POSITIONS[idx][0],
                    PLAYER_START_POSITIONS[idx][1],
                    Direction.UP, true, idx + 1
                );
                player.giveTemporaryShield();
                playerTanks.add(player);
                System.out.println("[*] Added Player " + (idx + 1) + " tank to game");
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

        // Accept client position (client-authoritative movement)
        if (input.posX >= 0 && input.posY >= 0) {
            player.setPosition(input.posX, input.posY);
            if (input.direction >= 0 && input.direction < 4) {
                player.setDirection(Direction.values()[input.direction]);
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
                deadPlayer.respawn(
                    PLAYER_START_POSITIONS[playerIndex][0],
                    PLAYER_START_POSITIONS[playerIndex][1]
                );
                System.out.println("[*] Player " + (playerIndex + 1) + " took life from Player " + (i + 1));
                break;
            }
        }
    }

    public void update() {
        if (gameOver || victory) return;

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
                for (Tank enemy : enemyTanks) {
                    if (enemy != enemyWithPermanentSpeedBoost) {
                        enemy.removeTempSpeedBoost();
                    }
                }
                System.out.println("[*] Enemy team speed boost expired");
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
                Tank newEnemy = enemyTanks.get(i);
                if (newEnemy != enemyWithPermanentSpeedBoost) {
                    newEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                }
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
        if (enemyFreezeDuration <= 0) {
            for (Tank enemy : enemyTanks) {
                enemy.updateAI(gameMap, bullets, allTanks, base, soundManager);
            }
        }

        // Update bullets
        updateBullets();

        // Update lasers
        updateLasers();

        // Update power-ups
        updatePowerUps();

        // Check victory condition
        if (enemySpawner.allEnemiesSpawned() && enemyTanks.isEmpty()) {
            victory = true;
        }

        // Check game over
        if (!base.isAlive()) {
            gameOver = true;
            // Initialize dancing when base is destroyed
            if (!dancingInitialized) {
                dancingInitialized = true;
                System.out.println("[*] Game over - base destroyed, dancing initialized");
            }
        } else {
            boolean allDead = true;
            for (Tank player : playerTanks) {
                if (player.isAlive() || player.getLives() > 0) {
                    allDead = false;
                    break;
                }
            }
            if (allDead) {
                gameOver = true;
            }
        }
    }

    private void updateBullets() {
        Iterator<Bullet> iter = bullets.iterator();
        while (iter.hasNext()) {
            Bullet bullet = iter.next();
            bullet.update();

            // Check map collision
            if (gameMap.checkBulletCollision(bullet)) {
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
                        enemy.damage();
                        if (!enemy.isAlive()) {
                            int killer = bullet.getOwnerPlayerNumber();
                            if (killer >= 1 && killer <= 4) {
                                playerKills[killer - 1]++;
                                int enemyType = enemy.getEnemyType().ordinal();
                                if (enemyType < 6) {
                                    playerKillsByType[killer - 1][enemyType]++;
                                }
                                int points = switch (enemy.getEnemyType()) {
                                    case POWER -> 2;
                                    case HEAVY -> 5;
                                    case BOSS -> 10;
                                    default -> 1;
                                };
                                playerScores[killer - 1] += points;
                            }
                            // Chance for power-up
                            if (Math.random() < 0.3) {
                                spawnPowerUp();
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
                            if (player.getLives() > 0) {
                                int idx = playerTanks.indexOf(player);
                                System.out.println("[*] Player " + (idx + 1) + " will respawn in 1 second");
                                player.respawn(
                                    PLAYER_START_POSITIONS[idx][0],
                                    PLAYER_START_POSITIONS[idx][1]
                                );
                            }
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
                                playerKills[killer - 1]++;
                                playerScores[killer - 1] += 1;
                            }
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

    private void applyEnemyPowerUp(PowerUp powerUp, Tank enemy) {
        switch (powerUp.getType()) {
            case SHOVEL -> {
                // Enemy removes base protection
                gameMap.setBaseProtection(GameMap.TileType.EMPTY);
                baseProtectionDuration = 0;
            }
            case FREEZE -> {
                // Freeze players for 10 seconds
                playerFreezeDuration = 600;
                System.out.println("[*] FREEZE: Players frozen for 10 seconds!");
            }
            case BOMB -> {
                // Damage all players
                System.out.println("[*] BOMB collected by enemy - damaging all players!");
                for (Tank player : playerTanks) {
                    if (player.isAlive()) {
                        player.setShield(false);
                        player.setPauseShield(false);
                        player.damage();
                    }
                }
            }
            case CAR -> {
                // All enemies get temporary speed boost for 30 seconds
                powerUp.applyEffect(enemy); // Give permanent boost to this enemy
                enemyWithPermanentSpeedBoost = enemy;
                enemyTeamSpeedBoostDuration = ENEMY_SPEED_BOOST_TIME;
                // Give temporary boost to all other enemies
                for (Tank otherEnemy : enemyTanks) {
                    if (otherEnemy != enemy && otherEnemy.isAlive()) {
                        otherEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                    }
                }
                System.out.println("[*] CAR: All enemies get speed boost for 30 seconds!");
            }
            default -> powerUp.applyEffect(enemy);
        }
    }

    private void applyPowerUp(PowerUp powerUp, Tank player) {
        switch (powerUp.getType()) {
            case SHOVEL -> {
                gameMap.setBaseProtection(GameMap.TileType.STEEL);
                baseProtectionDuration = 3600;
            }
            case FREEZE -> {
                enemyFreezeDuration = 600;
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
        Random random = new Random();
        double x = (1 + random.nextInt(24)) * TILE_SIZE;
        double y = (1 + random.nextInt(22)) * TILE_SIZE;
        powerUps.add(new PowerUp(x, y));
    }

    private void notifyBulletDestroyed(Bullet bullet) {
        int ownerNum = bullet.getOwnerPlayerNumber();
        if (ownerNum >= 1 && ownerNum <= playerTanks.size()) {
            playerTanks.get(ownerNum - 1).bulletDestroyed();
        }
    }

    public GameState buildNetworkState() {
        GameState state = new GameState();
        state.levelNumber = currentLevel;
        state.gameOver = gameOver;
        state.victory = victory;
        state.baseAlive = base.isAlive();
        state.remainingEnemies = enemySpawner.getRemainingEnemies();
        state.connectedPlayers = playerTanks.size();

        // Players - use array assignment
        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank tank = playerTanks.get(i);
            state.players[i].copyFromTank(tank, playerKills[i], playerScores[i], playerScores[i],
                playerNicknames[i], playerKillsByType[i]);
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

        // Power-ups
        for (PowerUp powerUp : powerUps) {
            state.powerUps.add(new GameState.PowerUpData(
                powerUp.getX(), powerUp.getY(),
                powerUp.getType().ordinal()
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

        // Host settings - use server's GameSettings
        state.hostPlayerSpeed = GameSettings.getPlayerSpeedMultiplier();
        state.hostEnemySpeed = GameSettings.getEnemySpeedMultiplier();
        state.hostPlayerShootSpeed = GameSettings.getPlayerShootSpeedMultiplier();
        state.hostEnemyShootSpeed = GameSettings.getEnemyShootSpeedMultiplier();

        return state;
    }

    public void restartLevel() {
        System.out.println("[*] Restarting level " + currentLevel);

        // Reset stats for this level
        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            Arrays.fill(playerKillsByType[i], 0);
        }

        initialize(playerTanks.size());
    }

    public void nextLevel() {
        currentLevel++;
        System.out.println("[*] Starting level " + currentLevel);

        // Keep scores, reset level-specific stuff
        initialize(playerTanks.size());
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isVictory() { return victory; }
    public int getCurrentLevel() { return currentLevel; }
    public int getRemainingEnemies() {
        return enemySpawner != null ? enemySpawner.getRemainingEnemies() + enemyTanks.size() : 0;
    }
}
