package com.vibetanks;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.rendering.EffectRenderer;
import com.vibetanks.rendering.GameRenderer;
import com.vibetanks.rendering.HUDRenderer;
import com.vibetanks.rendering.IconRenderer;
import com.vibetanks.rendering.ImageLoader;
import com.vibetanks.rendering.StatsRenderer;
import com.vibetanks.network.GameState;
import com.vibetanks.network.GameStateApplier;
import com.vibetanks.network.GameStateBuilder;
import com.vibetanks.network.NetworkGameHandler;
import com.vibetanks.network.NetworkManager;
import com.vibetanks.network.PlayerData;
import com.vibetanks.network.PlayerInput;
import com.vibetanks.ui.InputHandler;
import com.vibetanks.ui.MenuScene;
import com.vibetanks.util.GameLogger;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class Game implements GameStateApplier.GameContext, LevelTransitionManager.LevelTransitionContext,
        HUDRenderer.PlayerNameProvider, HUDRenderer.EndGameStatsProvider, HUDRenderer.GameOverState,
        NetworkGameHandler.HostContext, NetworkGameHandler.ClientContext {
    private static final GameLogger LOG = GameLogger.getLogger(Game.class);

    private final Pane root;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final int width;
    private final int height;
    private final int playerCount;
    private final int totalEnemies;
    private final Stage stage;

    private GameMap gameMap;
    private List<Tank> playerTanks;
    private List<Tank> enemyTanks;
    private List<Tank> allTanksCache = new ArrayList<>(); // Reusable list for collision detection
    private double[] spawnPositionCache = new double[2]; // Reusable for power-up spawn positions
    private List<Bullet> bullets;
    private List<Laser> lasers;
    private List<PowerUp> powerUps;
    private List<GameState.SoundEvent> pendingSoundEvents = new ArrayList<>(); // Sound events for network sync
    private EnemySpawner enemySpawner;
    private InputHandler inputHandler;
    private SoundManager soundManager;
    private Base base;
    private GameRenderer gameRenderer;
    private EffectRenderer effectRenderer;
    private IconRenderer iconRenderer;
    private StatsRenderer statsRenderer;
    private HUDRenderer hudRenderer;
    private double[][] playerStartPositions; // For respawning

    // Fixed start positions - use shared constants
    private static final double[][] FIXED_START_POSITIONS = GameConstants.PLAYER_START_POSITIONS;

    private AnimationTimer gameLoop;
    private long lastFrameTime = 0; // For frame rate limiting
    private static final long FRAME_TIME = 16_666_667; // ~60 FPS in nanoseconds

    // FPS counter for debugging
    private int fpsFrameCount = 0;
    private long fpsLastTime = System.currentTimeMillis();

    // Network multiplayer
    private NetworkManager network;
    private boolean isNetworkGame = false;
    private long lastNetworkUpdate = 0;
    private static final long NETWORK_UPDATE_INTERVAL = 16_666_667; // ~60 updates per second (match frame rate)
    private List<GameState.TileChange> mapChanges = new ArrayList<>();

    private boolean gameOver = false;
    private boolean victory = false;
    private boolean paused = false; // Local pause (single player only)
    private int pauseMenuSelection = 0; // 0 = Resume, 1 = Exit
    private boolean[] playerPaused = new boolean[4]; // Per-player pause for multiplayer

    // Player kills and score tracking (consolidated in PlayerStats)
    private PlayerStats playerStats = new PlayerStats();
    // Legacy arrays - delegate to playerStats for backwards compatibility during migration
    private int[] playerKills = playerStats.getKillsArray();
    private int[] playerScores = playerStats.getScoresArray();
    private int[] playerLevelScores = playerStats.getLevelScoresArray();
    private boolean winnerBonusAwarded = false;

    // Kills per enemy type per player: [playerIndex][enemyTypeOrdinal]
    // Enemy types: REGULAR=0, ARMORED=1, FAST=2, POWER=3, HEAVY=4, BOSS=5
    private int[][] playerKillsByType = playerStats.getKillsByTypeMatrix();

    // For client sound effects (track previous state to detect changes)
    private int prevEnemyCount = 0;
    private Set<Long> seenBulletIds = new HashSet<>(); // Track bullet IDs we've already played sounds for
    private Set<Long> seenLaserIds = new HashSet<>(); // Track laser IDs we've already played sounds for
    private Set<Integer> seenBurningTileKeys = new HashSet<>(); // Track burning tile keys for sound
    private boolean firstStateReceived = false; // Skip sounds on first state to avoid burst
    private int respawnSyncFrames = 0; // Frames to wait after respawn before sending position

    // Power-up effects manager (base protection, freeze, speed boost)
    private PowerUpEffectManager powerUpEffectManager = new PowerUpEffectManager();
    private static final double ENEMY_TEAM_SPEED_BOOST = GameConstants.ENEMY_TEAM_SPEED_BOOST;

    // Victory dancing anime girl
    private ImageView victoryImageView;
    // Game over dancing death
    private ImageView gameOverImageView;
    private boolean gameOverSoundPlayed = false;

    // UFO and Easter egg management (extracted to UFOManager)
    private UFOManager ufoManager = new UFOManager();

    // Dancing characters and victory celebration (extracted to CelebrationManager)
    private CelebrationManager celebrationManager = new CelebrationManager();

    // Track actual connected players (for network games)
    private int networkConnectedPlayers = 1;

    // Victory delay (5 seconds before showing victory screen) - use shared constant
    private boolean victoryConditionMet = false;
    private int victoryDelayTimer = 0;
    private static final int VICTORY_DELAY = GameConstants.VICTORY_DELAY;

    // Player nicknames (index 0-3 for players 1-4)
    private String[] playerNicknames = new String[4];

    // Boss kill tracking for victory screen
    private int bossKillerPlayerIndex = -1; // -1 = not killed yet, 0-3 = player index
    private PowerUp.Type bossKillPowerUpReward = null; // Power-up received for killing boss

    // Inner class for dancing characters
    // DancingCharacter and DancingGirl classes extracted to com.vibetanks.animation package

    // Constructor for local game
    public Game(Pane root, int width, int height, int playerCount, int totalEnemies, Stage stage) {
        this(root, width, height, playerCount, totalEnemies, stage, (NetworkManager) null);
    }

    // Constructor for custom level (test from editor)
    public Game(Pane root, int width, int height, int playerCount, int totalEnemies, Stage stage, LevelData customLevel) {
        this(root, width, height, playerCount, totalEnemies, stage, (NetworkManager) null);
        if (customLevel != null && gameMap != null) {
            gameMap.setCustomLevel(customLevel);
        }
    }

    // Constructor for network game
    public Game(Pane root, int width, int height, int playerCount, int totalEnemies, Stage stage, NetworkManager network) {
        this.root = root;
        this.width = width;
        this.height = height;
        this.playerCount = playerCount;
        this.totalEnemies = totalEnemies;
        this.stage = stage;
        this.network = network;
        this.isNetworkGame = (network != null);

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(false); // Canvas should not take focus
        root.getChildren().add(canvas);

        // Load local player's nickname
        String localNickname = NicknameManager.getNickname();
        int myPlayerIndex = isNetworkGame && network != null ? network.getPlayerNumber() - 1 : 0;
        if (myPlayerIndex >= 0 && myPlayerIndex < 4) {
            playerNicknames[myPlayerIndex] = localNickname;
        }

        // Load dancing anime girl GIF for victory screen
        loadVictoryImage();
        // Load dancing death GIF for game over screen
        loadGameOverImage();

        initialize();
    }

    private void loadVictoryImage() {
        ImageLoader.LoadResult result = ImageLoader.loadVictoryImage(root, width, height, getClass());
        victoryImageView = result.imageView;
    }

    private void loadGameOverImage() {
        ImageLoader.LoadResult result = ImageLoader.loadGameOverImage(root, width, height, getClass());
        gameOverImageView = result.imageView;
    }

    private void initialize() {
        // Initialize game objects
        gameMap = new GameMap(26, 26);
        bullets = new ArrayList<>();
        lasers = new ArrayList<>();
        powerUps = new ArrayList<>();

        // Initialize base at bottom center
        base = new Base(12 * 32, 24 * 32);

        // Initialize player tanks based on player count
        // For network games, start with enough tanks for this player's number
        // Additional tanks will be added/synced when game state is received
        playerTanks = new ArrayList<>();
        int myPlayerNum = isNetworkGame && network != null ? network.getPlayerNumber() : 1;
        int initialPlayers = isNetworkGame ? Math.max(2, myPlayerNum) : playerCount;
        if (initialPlayers >= 1) {
            playerTanks.add(new Tank(8 * 32, 24 * 32, Direction.UP, true, 1)); // Player 1
        }
        if (initialPlayers >= 2) {
            playerTanks.add(new Tank(16 * 32, 24 * 32, Direction.UP, true, 2)); // Player 2
        }
        if (initialPlayers >= 3) {
            playerTanks.add(new Tank(9 * 32, 24 * 32, Direction.UP, true, 3)); // Player 3 (next to Player 1)
        }
        if (initialPlayers >= 4) {
            playerTanks.add(new Tank(15 * 32, 24 * 32, Direction.UP, true, 4)); // Player 4 (next to Player 2)
        }

        // Store player start positions for respawn
        playerStartPositions = new double[playerTanks.size()][2];
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            playerStartPositions[i][0] = player.getX();
            playerStartPositions[i][1] = player.getY();
            LOG.debug("INIT: Player {} start position: {}, {}", i + 1,
                playerStartPositions[i][0], playerStartPositions[i][1]);
        }
        LOG.debug("INIT: playerStartPositions array size: {}", playerStartPositions.length);

        // Initialize enemy tanks list
        enemyTanks = new ArrayList<>();

        // Initialize enemy spawner
        enemySpawner = new EnemySpawner(totalEnemies, 5, gameMap);

        // Initialize input handler
        inputHandler = new InputHandler(root, playerTanks);

        // Add key handlers for game states
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            // Pause menu handling
            if (event.getCode() == KeyCode.ESCAPE) {
                // ESC on victory/game over screen returns to menu
                // Check both flags and visual state (for client sync)
                if (gameOver || victory || celebrationManager.isVictoryDancingInitialized() || celebrationManager.isDancingInitialized()) {
                    returnToMenu();
                } else {
                    // Toggle pause menu for all game modes
                    paused = !paused;
                    pauseMenuSelection = 0;

                    if (isNetworkGame) {
                        // Multiplayer: also toggle shield
                        int myPlayerIndex = network != null && !network.isHost()
                            ? network.getPlayerNumber() - 1 : 0;
                        if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
                            playerPaused[myPlayerIndex] = paused;
                            Tank myTank = playerTanks.get(myPlayerIndex);
                            myTank.setPauseShield(paused);
                        }
                    }

                    // Stop sounds when paused
                    if (paused && soundManager != null) {
                        soundManager.stopGameplaySounds();
                    }
                }
                return;
            }

            // Pause menu navigation (all game modes)
            if (paused) {
                if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                    pauseMenuSelection = (pauseMenuSelection + 1) % 2;
                } else if (event.getCode() == KeyCode.ENTER) {
                    if (pauseMenuSelection == 0) {
                        // Resume
                        paused = false;
                        if (isNetworkGame) {
                            int myPlayerIndex = network != null && !network.isHost()
                                ? network.getPlayerNumber() - 1 : 0;
                            if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
                                playerPaused[myPlayerIndex] = false;
                                playerTanks.get(myPlayerIndex).setPauseShield(false);
                            }
                        }
                    } else {
                        // Exit
                        returnToMenu();
                    }
                }
                return;
            }

            // ENTER to start next level after victory
            // For network clients, this is handled in the update loop via PlayerInput
            if (event.getCode() == KeyCode.ENTER && victory) {
                if (!isNetworkGame || (network != null && network.isHost())) {
                    startNextLevel();
                }
                // Client will send requestNextLevel in update loop
                return;
            }

            // ENTER to restart current level after game over
            // For network clients, this is handled in the update loop via PlayerInput
            if (event.getCode() == KeyCode.ENTER && gameOver) {
                if (!isNetworkGame || (network != null && network.isHost())) {
                    restartCurrentLevel();
                }
                // Client will send requestRestart in update loop
                return;
            }

            // ENTER to take life from another player (when dead)
            if (event.getCode() == KeyCode.ENTER && !gameOver && !victory) {
                tryTakeLifeFromTeammate();
            }

            // TEST MODE: Press B to spawn BOSS tank that shoots at player
            if (event.getCode() == KeyCode.B && !gameOver && !victory) {
                spawnTestBoss();
            }
        });

        // Initialize sound manager
        soundManager = new SoundManager();
        soundManager.playIntro();

        // Initialize renderers
        gameRenderer = new GameRenderer(gc, width, height);
        effectRenderer = gameRenderer.getEffectRenderer();
        iconRenderer = gameRenderer.getIconRenderer();
        statsRenderer = new StatsRenderer(gc, iconRenderer, width);
        hudRenderer = new HUDRenderer(gc, iconRenderer, effectRenderer, statsRenderer, width, height);
    }

    private void returnToMenu() {
        stop();
        // Clear host settings override when returning to menu
        GameSettings.clearHostSettings();
        MenuScene menuScene = new MenuScene(stage, width, height);
        stage.setScene(menuScene.getScene());
    }

    // Push apart tanks that are overlapping - delegates to GameLogic
    private void pushApartOverlappingTanks(List<Tank> allTanks) {
        GameLogic.TankOverlapResult result = GameLogic.resolveOverlappingTanks(allTanks, gameMap);

        // Handle BOSS contact kills (play sounds and log)
        if (result.tank1Killed && result.killedTank1 != null) {
            soundManager.playExplosion();
            String victimName = getPlayerNameForTank(result.killedTank1);
            LOG.info("KILL LOG: {} was killed by BOSS (contact)", victimName);
        }
        if (result.tank2Killed && result.killedTank2 != null) {
            soundManager.playExplosion();
            String victimName = getPlayerNameForTank(result.killedTank2);
            LOG.info("KILL LOG: {} was killed by BOSS (contact)", victimName);
        }
    }

    // TEST MODE: Spawn a BOSS tank directly above player 1 facing down
    private void spawnTestBoss() {
        if (playerTanks.isEmpty()) return;

        Tank player = playerTanks.get(0);

        // Clear all existing enemies
        enemyTanks.clear();

        // Spawn BOSS tank 200 pixels above player, facing DOWN
        double bossX = player.getX();
        double bossY = player.getY() - 200;

        // Make sure BOSS is on screen
        if (bossY < 0) bossY = 50;

        Tank boss = new Tank(bossX, bossY, Direction.DOWN, false, 0, Tank.EnemyType.BOSS);
        enemyTanks.add(boss);

        LOG.debug("TEST MODE: Spawned BOSS at ({}, {}) facing DOWN", bossX, bossY);
        LOG.debug("Player is at ({}, {})", player.getX(), player.getY());
        LOG.debug("Player has shield: {}, pause shield: {}", player.hasShield(), player.hasPauseShield());
        LOG.debug("Press B again to spawn another BOSS");
    }

    private void tryTakeLifeFromTeammate() {
        // For local player (host or single player)
        int myPlayerIndex = isNetworkGame && network != null && !network.isHost()
            ? network.getPlayerNumber() - 1
            : 0; // Single player or host uses player 0
        tryTakeLifeFromTeammate(myPlayerIndex);
    }

    @Override
    public void tryTakeLifeFromTeammate(int requestingPlayerIndex) {
        if (requestingPlayerIndex < 0 || requestingPlayerIndex >= playerTanks.size()) return;

        Tank myTank = playerTanks.get(requestingPlayerIndex);

        // Only allow if player is dead (no lives left)
        if (myTank.isAlive() || myTank.getLives() > 0) return;

        // Find a teammate with lives to spare (more than 1 life)
        for (int i = 0; i < playerTanks.size(); i++) {
            if (i == requestingPlayerIndex) continue;

            Tank teammate = playerTanks.get(i);
            if (teammate.getLives() > 1) {
                // Transfer one life
                teammate.setLives(teammate.getLives() - 1);
                myTank.setLives(1);
                myTank.respawn(FIXED_START_POSITIONS[requestingPlayerIndex][0], FIXED_START_POSITIONS[requestingPlayerIndex][1]);
                LOG.info("Player {} took a life from Player {}", requestingPlayerIndex + 1, i + 1);
                return;
            }
        }
    }

    @Override
    public void startNextLevel() {
        LevelTransitionManager.startNextLevel(this);
    }

    @Override
    public void restartCurrentLevel() {
        LevelTransitionManager.restartCurrentLevel(this);
    }

    public void start() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Limit frame rate to ~60 FPS - skip if not enough time has passed
                // This prevents the game from running too fast on high refresh rate displays (e.g., Mac 120Hz)
                if (lastFrameTime != 0 && now - lastFrameTime < FRAME_TIME) {
                    return;
                }
                lastFrameTime = now;

                // Always run exactly one update per frame - consistent speed
                update();
                render();

                // FPS counter
                fpsFrameCount++;
                long currentTime = System.currentTimeMillis();
                if (currentTime - fpsLastTime >= 5000) {
                    double fps = fpsFrameCount / 5.0;
                    LOG.info("[FPS] {} (target: 60)", String.format("%.1f", fps));
                    fpsFrameCount = 0;
                    fpsLastTime = currentTime;
                }
            }
        };
        gameLoop.start();
    }

    private double[] getRandomPowerUpSpawnPosition() {
        // Delegate to shared GameLogic, reuse cached array
        GameLogic.findPowerUpSpawnPosition(gameMap, 32, spawnPositionCache);
        return spawnPositionCache;
    }

    private PowerUp.Type applyRandomPowerUp(Tank player) {
        return PowerUpHandler.applyRandomBossReward(player);
    }

    /**
     * Add points to a player's score and award extra life for every 100 points.
     */
    private void addScore(int playerIndex, int points) {
        GameLogic.addScore(playerIndex, points, playerScores, playerLevelScores, playerTanks);
    }


    /**
     * Notify the owner tank that their bullet was destroyed.
     * This allows the tank to shoot again immediately.
     */
    private void notifyBulletDestroyed(Bullet bullet) {
        // Delegate to shared GameLogic
        GameLogic.notifyBulletDestroyed(bullet, playerTanks);
    }

    private void checkAndSpawnUFO() {
        // UFO spawn conditions:
        // 1. 10 or fewer enemies remaining (but more than 1 - BOSS not yet spawned)
        // 2. At least one player has machinegun and has killed 5+ enemies with it
        int remaining = enemySpawner.getRemainingEnemies();
        if (remaining > 10 || remaining <= 1) {
            return; // Not in the right enemy count range
        }

        // Delegate to UFOManager (pass total kills per player)
        ufoManager.checkAndSpawnUFO(playerTanks, playerKills, width, height);
    }

    // Debug: count updates per second
    private int localUpdateCount = 0;
    private long lastLocalUpdateTime = System.currentTimeMillis();

    private void update() {
        // Debug: count local updates per second (only for non-network or host games)
        if (!isNetworkGame || (network != null && network.isHost())) {
            localUpdateCount++;
            long now = System.currentTimeMillis();
            if (now - lastLocalUpdateTime >= 5000) {
                LOG.debug("[DEBUG] Local game updates per second: {}", localUpdateCount / 5.0);
                localUpdateCount = 0;
                lastLocalUpdateTime = now;
            }
        }

        // Network clients: delegate to NetworkGameHandler
        if (isNetworkGame && network != null && !network.isHost()) {
            NetworkGameHandler.ClientUpdateResult clientResult = NetworkGameHandler.handleClientUpdate(this);
            if (clientResult.skipMainUpdate) {
                return;
            }
        }

        // HOST: Send game state and handle client requests via NetworkGameHandler
        if (isNetworkGame && network != null && network.isHost()) {
            NetworkGameHandler.HostUpdateResult hostResult = NetworkGameHandler.handleHostUpdate(this);
            if (hostResult.skipMainUpdate) {
                return;
            }
        }

        if (gameOver || victory || paused) {
            return;
        }

        // Network game handling (HOST only now)
        if (isNetworkGame && network != null) {
            if (!network.isConnected()) {
                // Connection lost
                gameOver = true;
                LOG.warn("Network connection lost!");
                return;
            }

            if (network.isHost()) {
                // HOST: Add new player tanks and receive client inputs via NetworkGameHandler
                NetworkGameHandler.handleNewPlayerConnections(this);
                NetworkGameHandler.receiveClientInputs(this);
                // Host runs full game logic below
            }
        }

        // Reuse combined list of all tanks for collision detection (avoid allocation in hot path)
        allTanksCache.clear();
        allTanksCache.addAll(playerTanks);
        allTanksCache.addAll(enemyTanks);
        List<Tank> allTanks = allTanksCache;

        // Handle player input (local or host) - pass freeze state
        boolean isPlayerFrozen = powerUpEffectManager.arePlayersFrozen();
        inputHandler.handleInput(gameMap, bullets, lasers, soundManager, allTanks, base, isPlayerFrozen);

        // Update base protection from SHOVEL power-up (via PowerUpEffectManager)
        powerUpEffectManager.updateBaseProtection(gameMap);

        // Update map (burning tiles)
        gameMap.update();

        // Spawn enemies if needed
        int enemyCountBefore = enemyTanks.size();
        enemySpawner.update(enemyTanks);
        // Apply temporary speed boost to newly spawned enemies if boost is active
        if (powerUpEffectManager.isEnemySpeedBoostActive() && enemyTanks.size() > enemyCountBefore) {
            for (int i = enemyCountBefore; i < enemyTanks.size(); i++) {
                Tank newEnemy = enemyTanks.get(i);
                if (newEnemy != powerUpEffectManager.getEnemyWithPermanentSpeedBoost()) {
                    newEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                }
            }
        }

        // Check UFO spawn conditions (only host spawns UFO)
        if (!ufoManager.isUfoSpawnedThisLevel() && ufoManager.getUFO() == null) {
            checkAndSpawnUFO();
        }

        // Update UFO and message timers via UFOManager
        ufoManager.updateUFO(bullets, width, height, soundManager, victoryConditionMet);

        // Update player tanks and handle respawn
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive()) {
                player.update(gameMap, bullets, soundManager, allTanks, base);
            } else if (player.isWaitingToRespawn()) {
                // Player is waiting for respawn delay
                player.updateRespawnTimer();
            } else if (player.getLives() > 1) {
                // Player died but has lives left - decrement life and start respawn timer
                player.setLives(player.getLives() - 1);
                double respawnX = FIXED_START_POSITIONS[i][0];
                double respawnY = FIXED_START_POSITIONS[i][1];
                LOG.info("Player {} will respawn in 1 second at: {}, {} (lives left: {})",
                    i + 1, respawnX, respawnY, player.getLives());
                player.respawn(respawnX, respawnY); // This now starts the timer
            } else if (player.getLives() == 1) {
                // Player on last displayed life (internal=1) - decrement and respawn one more time
                player.setLives(0);
                double respawnX = FIXED_START_POSITIONS[i][0];
                double respawnY = FIXED_START_POSITIONS[i][1];
                LOG.info("Player {} on last life, respawning at: {}, {}", i + 1, respawnX, respawnY);
                player.respawn(respawnX, respawnY);
            }
            // When lives == 0 and player dies, they stay dead (game over check handles this)
        }

        // Update freeze durations (via PowerUpEffectManager)
        powerUpEffectManager.updateFreezeTimers();

        // Update enemy team speed boost duration
        boolean wasSpeedBoostActive = powerUpEffectManager.isEnemySpeedBoostActive();
        powerUpEffectManager.updateEnemySpeedBoost();
        if (wasSpeedBoostActive && !powerUpEffectManager.isEnemySpeedBoostActive()) {
            // Speed boost just expired - remove temporary speed boost from all enemies except the one who picked it up
            for (Tank enemy : enemyTanks) {
                if (enemy != powerUpEffectManager.getEnemyWithPermanentSpeedBoost()) {
                    enemy.removeTempSpeedBoost();
                }
            }
            LOG.info("Enemy team speed boost expired - only original enemy keeps the speed");
        }

        // Update enemy tanks with AI (skip if frozen, except BOSS is unfreezable)
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                // BOSS tank is immune to freeze
                if (!powerUpEffectManager.areEnemiesFrozen() || tank.getEnemyType() == Tank.EnemyType.BOSS) {
                    tank.updateAI(gameMap, bullets, allTanks, base, soundManager);
                }
            }
        }

        // Push apart overlapping tanks to prevent getting stuck
        pushApartOverlappingTanks(allTanks);

        // Update bullets using ProjectileHandler
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();

            // Use ProjectileHandler for collision detection
            ProjectileHandler.BulletCollisionResult result = ProjectileHandler.processBullet(
                    bullet, gameMap, enemyTanks, playerTanks, base, ufoManager.getUFO(), width, height, soundManager);

            if (result.shouldRemove) {
                notifyBulletDestroyed(bullet);
                bulletIterator.remove();

                // Handle UFO destruction
                if (result.ufoDestroyed) {
                    int killerPlayer = result.killerPlayerNumber;
                    if (killerPlayer >= 1 && killerPlayer <= 4) {
                        addScore(killerPlayer - 1, 20);
                        LOG.info("UFO destroyed by Player {} - awarded 20 points!", killerPlayer);
                    }
                    double[] eggPos = getRandomPowerUpSpawnPosition();
                    ufoManager.handleUFODestroyed(killerPlayer, eggPos[0], eggPos[1]);
                }

                // Handle enemy killed - track kills and award points
                if (result.enemyKilled && result.killedEnemy != null) {
                    int killerPlayer = result.killerPlayerNumber;
                    Tank enemy = result.killedEnemy;
                    if (killerPlayer >= 1 && killerPlayer <= 4) {
                        playerKills[killerPlayer - 1]++;
                        int enemyTypeOrdinal = enemy.getEnemyType().ordinal();
                        if (enemyTypeOrdinal < 6) {
                            playerKillsByType[killerPlayer - 1][enemyTypeOrdinal]++;
                        }
                        Tank killer = playerTanks.get(killerPlayer - 1);
                        addScore(killerPlayer - 1, GameConstants.getScoreForEnemyType(enemy.getEnemyType()));
                        if (enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                            LOG.info("BOSS killed by Player {} (tank playerNumber={}) - awarding power-up!",
                                killerPlayer, killer.getPlayerNumber());
                            bossKillerPlayerIndex = killerPlayer - 1;
                            bossKillPowerUpReward = applyRandomPowerUp(killer);
                            LOG.info("After reward: killer.hasShip={}", killer.hasShip());
                        }
                    }
                }

                // Handle power-up drop
                if (result.shouldDropPowerUp) {
                    double[] spawnPos = getRandomPowerUpSpawnPosition();
                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                }

                // Handle player killed - queue death sound and spawn power-up in 3+ player mode
                if (result.playerKilled) {
                    queueSoundEvent(GameState.SoundType.PLAYER_DEATH);
                    if (playerTanks.size() > 2) {
                        double[] spawnPos = getRandomPowerUpSpawnPosition();
                        powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                        LOG.info("Power-up spawned for killed player (3+ players mode)");
                    }
                }

                // Handle base hit
                if (result.hitBase) {
                    base.destroy();
                    soundManager.playBaseDestroyed();
                    gameOver = true;
                }
            }
        }

        // Check bullet-to-bullet collisions using ProjectileHandler
        ProjectileHandler.processBulletToBulletCollisions(bullets, playerTanks);

        // Update lasers and check collisions using ProjectileHandler
        Iterator<Laser> laserIterator = lasers.iterator();
        while (laserIterator.hasNext()) {
            Laser laser = laserIterator.next();
            laser.update();

            // Remove expired lasers
            if (laser.isExpired()) {
                laserIterator.remove();
                continue;
            }

            // Process laser collisions via ProjectileHandler (pass gameMap for steel blocking check)
            ProjectileHandler.LaserCollisionResult laserResult = ProjectileHandler.processLaser(
                    laser, enemyTanks, playerTanks, base, ufoManager.getUFO(), soundManager, gameMap);

            // Handle enemy killed - track kills and award points
            if (laserResult.enemyKilled && laserResult.killedEnemy != null) {
                int killerPlayer = laserResult.killerPlayerNumber;
                Tank enemy = laserResult.killedEnemy;
                if (killerPlayer >= 1 && killerPlayer <= 4) {
                    playerKills[killerPlayer - 1]++;
                    int enemyTypeOrdinal = enemy.getEnemyType().ordinal();
                    if (enemyTypeOrdinal < 6) {
                        playerKillsByType[killerPlayer - 1][enemyTypeOrdinal]++;
                    }
                    addScore(killerPlayer - 1, GameConstants.getScoreForEnemyType(enemy.getEnemyType()));
                    // BOSS kill rewards
                    if (laserResult.isBossKill) {
                        Tank killer = playerTanks.get(killerPlayer - 1);
                        bossKillerPlayerIndex = killerPlayer - 1;
                        bossKillPowerUpReward = applyRandomPowerUp(killer);
                    }
                }
            }

            // Handle power-up drop
            if (laserResult.shouldDropPowerUp) {
                double[] spawnPos = getRandomPowerUpSpawnPosition();
                powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
            }

            // Handle player killed - queue death sound and spawn power-up in 3+ player mode
            if (laserResult.playerKilled) {
                queueSoundEvent(GameState.SoundType.PLAYER_DEATH);
                if (playerTanks.size() > 2) {
                    double[] spawnPos = getRandomPowerUpSpawnPosition();
                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                    LOG.info("Power-up spawned for killed player by laser (3+ players mode)");
                }
            }

            // Handle UFO destruction
            if (laserResult.ufoDestroyed) {
                int killerPlayer = laserResult.killerPlayerNumber;
                if (killerPlayer >= 1 && killerPlayer <= 4) {
                    addScore(killerPlayer - 1, 20);
                    LOG.info("UFO destroyed by laser from Player {} - awarded 20 points!", killerPlayer);
                }
                double[] eggPos = getRandomPowerUpSpawnPosition();
                ufoManager.handleUFODestroyed(killerPlayer, eggPos[0], eggPos[1]);
            }

            // Handle base hit
            if (laserResult.hitBase) {
                base.destroy();
                soundManager.playBaseDestroyed();
                gameOver = true;
            }
        }

        // Update power-ups using PowerUpHandler
        Iterator<PowerUp> powerUpIterator = powerUps.iterator();
        while (powerUpIterator.hasNext()) {
            PowerUp powerUp = powerUpIterator.next();
            powerUp.update();

            // Check if collected by players using PowerUpHandler
            PowerUpHandler.PlayerCollectionResult playerResult =
                    PowerUpHandler.checkPlayerCollection(powerUp, playerTanks);

            if (playerResult.collected) {
                // Handle game-level effects for special power-ups (via PowerUpEffectManager)
                if (playerResult.activateShovel) {
                    powerUpEffectManager.activateBaseProtection(gameMap);
                } else if (playerResult.activateFreeze) {
                    powerUpEffectManager.activateEnemyFreeze();
                } else if (playerResult.activateBomb) {
                    PowerUpHandler.applyPlayerBomb(enemyTanks, soundManager);
                }
                powerUpIterator.remove();
                continue;
            }

            // Check if collected by enemies using PowerUpHandler
            PowerUpHandler.EnemyCollectionResult enemyResult =
                    PowerUpHandler.checkEnemyCollection(powerUp, enemyTanks);

            if (enemyResult.collected) {
                // Handle game-level effects for special power-ups (via PowerUpEffectManager)
                if (enemyResult.removeShovel) {
                    powerUpEffectManager.removeBaseProtection(gameMap);
                } else if (enemyResult.activateFreeze) {
                    powerUpEffectManager.activatePlayerFreeze();
                } else if (enemyResult.activateBomb) {
                    PowerUpHandler.applyEnemyBomb(playerTanks, soundManager);
                } else if (enemyResult.activateCar) {
                    powerUpEffectManager.activateEnemySpeedBoost(enemyResult.collectorEnemy);
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

        // Update easter egg via UFOManager
        UFOManager.UpdateResult eggResult = ufoManager.updateEasterEgg(playerTanks, enemyTanks);
        if (eggResult.easterEggCollectedByPlayer) {
            int playerIndex = eggResult.easterEggCollectorIndex;
            Tank player = playerTanks.get(playerIndex);
            // Give collecting player 3 extra lives
            for (int j = 0; j < 3; j++) {
                player.addLife();
            }
            LOG.info("Easter egg collected by Player {}! +3 lives!", playerIndex + 1);
            GameLogic.applyEasterEggEffect(enemyTanks, true);
            // Turn base into cat if boss has spawned (remaining == 0 means boss was the last spawned)
            if (enemySpawner.getRemainingEnemies() == 0) {
                base.setCatMode(true);
                LOG.info("Base transformed to cat!");
            }
        } else if (eggResult.easterEggCollectedByEnemy) {
            LOG.info("Easter egg collected by enemy! All enemies become HEAVY tanks!");
            GameLogic.applyEasterEggEffect(enemyTanks, false);
        } else if (eggResult.easterEggExpired) {
            LOG.info("Easter egg expired!");
        }

        // Remove dead enemy tanks using shared GameLogic
        GameLogic.removeDeadEnemies(enemyTanks);

        // Check victory condition with delay using shared GameLogic
        if (GameLogic.checkVictory(enemySpawner, enemyTanks)) {
            if (!victoryConditionMet) {
                victoryConditionMet = true;
                victoryDelayTimer = 0;
                LOG.info("All enemies defeated! Victory in 10 seconds...");
            }
            victoryDelayTimer++;
            if (victoryDelayTimer >= VICTORY_DELAY && !victory) {
                victory = true;
                soundManager.playVictory();
                queueSoundEvent(GameState.SoundType.VICTORY);

                // Start cat escape animation if base is cat and protection was broken
                if (base.isCatMode() && gameMap.isBaseProtectionBroken()) {
                    base.startCatEscape();
                    LOG.info("Cat escaping from damaged base!");
                }
            }
        }

        // Check game over condition using shared GameLogic
        if (GameLogic.checkGameOver(base, playerTanks)) {
            gameOver = true;
        }
    }

    private void render() {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        // Render map WITHOUT trees (trees will be rendered on top of tanks)
        gameMap.renderWithoutTrees(gc);

        // Render base
        base.render(gc);

        // Render power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.render(gc);
        }

        // Render easter egg
        EasterEgg easterEgg = ufoManager.getEasterEgg();
        if (easterEgg != null) {
            easterEgg.render(gc);
        }

        // Render bullets
        for (Bullet bullet : bullets) {
            bullet.render(gc);
        }

        // Render lasers
        for (Laser laser : lasers) {
            laser.render(gc);
        }

        // Render player tanks
        for (Tank tank : playerTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
                // Draw ice effect if players are frozen
                if (powerUpEffectManager.arePlayersFrozen()) {
                    effectRenderer.renderFreezeEffect(tank);
                }
            }
        }

        // Render enemy tanks
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
                // Draw ice effect if enemies are frozen (except BOSS which is immune)
                if (powerUpEffectManager.areEnemiesFrozen() && tank.getEnemyType() != Tank.EnemyType.BOSS) {
                    effectRenderer.renderFreezeEffect(tank);
                }
            }
        }

        // Render UFO (above tanks, below trees)
        UFO ufo = ufoManager.getUFO();
        if (ufo != null && ufo.isAlive()) {
            ufo.render(gc);
        }

        // Render UFO messages via UFOManager
        if (ufoManager.getUfoLostMessageTimer() > 0) {
            effectRenderer.renderUfoLostMessage(ufoManager.getUfoLostMessageTimer());
        }
        if (ufoManager.getUfoKilledMessageTimer() > 0) {
            effectRenderer.renderUfoKilledMessage(ufoManager.getUfoKilledMessageTimer());
        }

        // Render trees ON TOP of tanks to make tanks partially visible in forest
        gameMap.renderTrees(gc);

        // Render burning trees with fire animation (on top of everything)
        gameMap.renderBurningTiles(gc);

        // Render UI
        renderUI();
    }

    // UFO message rendering, boss health bar, and laughing skull moved to EffectRenderer

    // Power-up icon rendering and getPowerUpColor moved to IconRenderer
    // Dancing character initialization moved to CelebrationManager

    /**
     * Get the display name for a player (nickname if set, otherwise "P1", "P2", etc.)
     * For local player: use NicknameManager directly
     * For other players: use synced nicknames from GameState
     */
    @Override
    public String getPlayerDisplayName(int playerIndex) {
        // Determine local player index
        int myPlayerIndex = isNetworkGame && network != null ? network.getPlayerNumber() - 1 : 0;

        // For local player, always use NicknameManager directly
        if (playerIndex == myPlayerIndex) {
            String localNickname = NicknameManager.getNickname();
            if (localNickname != null) {
                return localNickname;
            }
        } else {
            // For other players, use synced nickname from array
            if (playerIndex >= 0 && playerIndex < 4 && playerNicknames[playerIndex] != null) {
                return playerNicknames[playerIndex];
            }
        }
        return "P" + (playerIndex + 1);
    }

    /**
     * Get the number of players to display in HUD and stats.
     * For network games, use the server's connected player count.
     * For local games, use playerTanks size.
     */
    private int getDisplayPlayerCount() {
        if (isNetworkGame && network != null && !network.isHost()) {
            // Client: use server's connected player count
            return networkConnectedPlayers;
        }
        // Host or local game: use actual tank count
        return playerTanks.size();
    }

    // Get player name for a tank (used for kill logging)
    private String getPlayerNameForTank(Tank tank) {
        for (int i = 0; i < playerTanks.size(); i++) {
            if (playerTanks.get(i) == tank) {
                return getPlayerDisplayName(i);
            }
        }
        // Not a player tank (must be enemy)
        if (tank.getEnemyType() != null) {
            return "Enemy " + tank.getEnemyType().name();
        }
        return "Unknown tank";
    }

    private void renderUI() {
        // Render main HUD (level info, player stats, power-ups)
        hudRenderer.renderHUD(gameMap.getLevelNumber(), enemySpawner.getRemainingEnemies(),
                getDisplayPlayerCount(), playerTanks, playerKills, playerScores, this);

        // Render BOSS health indicator if BOSS is alive
        effectRenderer.renderBossHealthBar(enemyTanks);

        if (gameOver) {
            hudRenderer.renderGameOverScreen(base, celebrationManager, enemyTanks, soundManager, this, this);
        } else if (victory) {
            hudRenderer.renderVictoryScreen(gameMap.getLevelNumber(), base, celebrationManager,
                    playerTanks, soundManager, victoryImageView, this);
        } else if (paused) {
            hudRenderer.renderPauseMenu(pauseMenuSelection);
        } else {
            // Hide images when not in end state
            hudRenderer.hideEndGameImages(victoryImageView, gameOverImageView);

            // Show hint to take life if player is dead and teammate has lives
            int myPlayerIndex = isNetworkGame && network != null && !network.isHost()
                ? network.getPlayerNumber() - 1 : 0;
            hudRenderer.renderTakeLifeHint(playerTanks, myPlayerIndex);

            // Show pause indicator for multiplayer
            if (isNetworkGame) {
                int pausePlayerIndex = network != null && !network.isHost()
                    ? network.getPlayerNumber() - 1 : 0;
                hudRenderer.renderMultiplayerPauseIndicator(playerPaused, pausePlayerIndex);
            }
        }
    }

    @Override
    public void renderEndGameStats(double startY) {
        int activePlayers = getDisplayPlayerCount();
        if (activePlayers == 0) return;

        // Calculate winner for stats display
        int[] winnerResult = StatsRenderer.calculateWinner(playerKills, activePlayers);
        int winnerIndex = winnerResult[0];
        boolean isTie = winnerResult[1] == 1;

        // Award winner bonus (10 points) if no tie - only once (on victory)
        if (victory && activePlayers > 1 && !isTie && winnerIndex >= 0 && !winnerBonusAwarded) {
            addScore(winnerIndex, 10);
            winnerBonusAwarded = true;
        }

        // Build player names array
        String[] playerNames = new String[activePlayers];
        for (int i = 0; i < activePlayers; i++) {
            playerNames[i] = getPlayerDisplayName(i);
        }

        // Delegate rendering to StatsRenderer
        statsRenderer.renderEndGameStats(startY, activePlayers, playerNames,
                playerKills, playerScores, playerLevelScores, playerKillsByType,
                victory, winnerIndex, isTie);

        // Display boss kill info on victory screen
        if (victory && bossKillerPlayerIndex >= 0 && bossKillPowerUpReward != null) {
            statsRenderer.renderBossKillInfo(getPlayerDisplayName(bossKillerPlayerIndex), bossKillPowerUpReward);
        }
    }

    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        // Stop all sounds
        if (soundManager != null) {
            soundManager.stopGameplaySounds();
        }
        // Close network connection if it exists
        if (network != null) {
            LOG.info("Closing network connection...");
            network.close();
        }
    }

    // ============ NETWORK MULTIPLAYER METHODS ============

    @Override
    public GameState buildGameState() {
        int connectedPlayers = network != null ? network.getConnectedPlayerCount() : playerCount;
        GameState state = GameStateBuilder.build(
            playerTanks, playerKills, playerScores, playerLevelScores, playerNicknames, playerKillsByType,
            enemyTanks, bullets, lasers, powerUps,
            gameOver, victory, enemySpawner, gameMap, base, connectedPlayers,
            powerUpEffectManager, bossKillerPlayerIndex, bossKillPowerUpReward,
            celebrationManager, mapChanges, ufoManager, pendingSoundEvents
        );
        mapChanges.clear(); // Clear after building state
        pendingSoundEvents.clear(); // Clear sound events after sending
        return state;
    }

    private void applyGameState(GameState state) {
        GameStateApplier.apply(state, this);
    }

    // ============ GameStateApplier.GameContext IMPLEMENTATION ============

    @Override public List<Tank> getPlayerTanks() { return playerTanks; }
    @Override public List<Tank> getEnemyTanks() { return enemyTanks; }
    @Override public List<Bullet> getBullets() { return bullets; }
    @Override public List<Laser> getLasers() { return lasers; }
    @Override public List<PowerUp> getPowerUps() { return powerUps; }
    @Override public GameMap getGameMap() { return gameMap; }
    @Override public Base getBase() { return base; }
    @Override public EnemySpawner getEnemySpawner() { return enemySpawner; }
    @Override public CelebrationManager getCelebrationManager() { return celebrationManager; }
    @Override public UFOManager getUFOManager() { return ufoManager; }
    @Override public PowerUpEffectManager getPowerUpEffectManager() { return powerUpEffectManager; }
    @Override public SoundManager getSoundManager() { return soundManager; }
    @Override public NetworkManager getNetwork() { return network; }

    @Override public int[] getPlayerKills() { return playerKills; }
    @Override public int[] getPlayerScores() { return playerScores; }
    @Override public int[] getPlayerLevelScores() { return playerLevelScores; }
    @Override public int[][] getPlayerKillsByType() { return playerKillsByType; }
    @Override public String[] getPlayerNicknames() { return playerNicknames; }
    @Override public double[][] getPlayerStartPositions() { return playerStartPositions; }

    @Override public boolean isFirstStateReceived() { return firstStateReceived; }
    @Override public void setFirstStateReceived(boolean value) { firstStateReceived = value; }
    @Override public Set<Long> getSeenBulletIds() { return seenBulletIds; }
    @Override public void setSeenBulletIds(Set<Long> ids) { seenBulletIds = ids; }
    @Override public Set<Long> getSeenLaserIds() { return seenLaserIds; }
    @Override public void setSeenLaserIds(Set<Long> ids) { seenLaserIds = ids; }
    @Override public Set<Integer> getSeenBurningTileKeys() { return seenBurningTileKeys; }
    @Override public void setSeenBurningTileKeys(Set<Integer> keys) { seenBurningTileKeys = keys; }
    @Override public int getPrevEnemyCount() { return prevEnemyCount; }
    @Override public void setPrevEnemyCount(int count) { prevEnemyCount = count; }

    @Override public boolean isGameOver() { return gameOver; }
    @Override public void setGameOver(boolean value) { gameOver = value; }
    @Override public boolean isVictory() { return victory; }
    @Override public void setVictory(boolean value) { victory = value; }
    @Override public void setNetworkConnectedPlayers(int count) { networkConnectedPlayers = count; }
    @Override public int getRespawnSyncFrames() { return respawnSyncFrames; }
    @Override public void setRespawnSyncFrames(int frames) { respawnSyncFrames = frames; }
    // isFirstStateReceived() is already implemented below
    @Override public boolean[] getPlayerPaused() { return playerPaused; }
    @Override public void setPlayerStartPositions(double[][] positions) { playerStartPositions = positions; }
    @Override public void setBossKillerPlayerIndex(int index) { bossKillerPlayerIndex = index; }
    @Override public void setBossKillPowerUpReward(PowerUp.Type type) { bossKillPowerUpReward = type; }

    @Override public void setBase(Base newBase) { base = newBase; }
    @Override public void setEnemySpawner(EnemySpawner spawner) { enemySpawner = spawner; }
    @Override public int getTotalEnemies() { return totalEnemies; }

    @Override public void hideVictoryImage() { if (victoryImageView != null) victoryImageView.setVisible(false); }
    @Override public void hideGameOverImage() { if (gameOverImageView != null) gameOverImageView.setVisible(false); }
    @Override public void setGameOverSoundPlayed(boolean value) { gameOverSoundPlayed = value; }

    @Override public double[][] getFixedStartPositions() { return FIXED_START_POSITIONS; }

    // ============ LevelTransitionManager.LevelTransitionContext IMPLEMENTATION ============
    // Note: Most methods are shared with GameStateApplier.GameContext above

    @Override public void setVictoryConditionMet(boolean value) { victoryConditionMet = value; }
    @Override public void setVictoryDelayTimer(int value) { victoryDelayTimer = value; }
    @Override public void setWinnerBonusAwarded(boolean value) { winnerBonusAwarded = value; }

    // ============ HUDRenderer.GameOverState IMPLEMENTATION ============

    @Override public boolean isGameOverSoundPlayed() { return gameOverSoundPlayed; }
    // setGameOverSoundPlayed is already implemented above

    // ============ NetworkGameHandler.HostContext IMPLEMENTATION ============
    // Note: getNetwork() is already implemented in GameStateApplier.GameContext above

    @Override public boolean isPaused() { return paused; }
    @Override public long getLastNetworkUpdate() { return lastNetworkUpdate; }
    @Override public void setLastNetworkUpdate(long time) { lastNetworkUpdate = time; }
    @Override public long getNetworkUpdateInterval() { return NETWORK_UPDATE_INTERVAL; }
    @Override public void queueSoundEvent(GameState.SoundType type) {
        pendingSoundEvents.add(new GameState.SoundEvent(type));
    }
    @Override public void queueSoundEvent(GameState.SoundType type, int playerNumber) {
        pendingSoundEvents.add(new GameState.SoundEvent(type, playerNumber));
    }

    // ============ NetworkGameHandler.ClientContext IMPLEMENTATION ============
    // Note: Many methods are already implemented via other interfaces

    @Override
    public GameStateApplier.GameContext getGameStateContext() { return this; }

    @Override
    public boolean isEnterPressed() { return inputHandler.isEnterPressed(); }

    @Override
    public PlayerInput capturePlayerInput() {
        return inputHandler.capturePlayerInput();
    }

}
