package com.vibetanks;

import com.vibetanks.animation.CelebrationManager;
import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.rendering.EffectRenderer;
import com.vibetanks.rendering.GameRenderer;
import com.vibetanks.rendering.IconRenderer;
import com.vibetanks.rendering.StatsRenderer;
import com.vibetanks.network.GameState;
import com.vibetanks.network.GameStateApplier;
import com.vibetanks.network.GameStateBuilder;
import com.vibetanks.network.NetworkManager;
import com.vibetanks.network.PlayerData;
import com.vibetanks.network.PlayerInput;
import com.vibetanks.ui.InputHandler;
import com.vibetanks.ui.MenuScene;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class Game implements GameStateApplier.GameContext, LevelTransitionManager.LevelTransitionContext {
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
    private List<Bullet> bullets;
    private List<Laser> lasers;
    private List<PowerUp> powerUps;
    private EnemySpawner enemySpawner;
    private InputHandler inputHandler;
    private SoundManager soundManager;
    private Base base;
    private GameRenderer gameRenderer;
    private EffectRenderer effectRenderer;
    private IconRenderer iconRenderer;
    private StatsRenderer statsRenderer;
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
        try {
            Image victoryImage = null;

            // Try to load from local resources first
            try {
                java.io.File localFile = new java.io.File("src/main/resources/images/victory.gif");
                if (localFile.exists()) {
                    System.out.println("Loading victory image from local file: " + localFile.getPath());
                    victoryImage = new Image(localFile.toURI().toString());
                    if (!victoryImage.isError()) {
                        System.out.println("Successfully loaded victory image from local file!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not load from local file: " + e.getMessage());
            }

            // If local file failed, try resource path
            if (victoryImage == null || victoryImage.isError()) {
                try {
                    java.net.URL resourceUrl = getClass().getResource("/images/victory.gif");
                    if (resourceUrl != null) {
                        System.out.println("Loading victory image from resources");
                        victoryImage = new Image(resourceUrl.toString());
                        if (!victoryImage.isError()) {
                            System.out.println("Successfully loaded victory image from resources!");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not load from resources: " + e.getMessage());
                }
            }

            // If still no image, try URLs as fallback
            if (victoryImage == null || victoryImage.isError()) {
                String[] imageUrls = {
                    "https://i.imgur.com/7kZ8Lrb.gif",
                    "https://media.tenor.com/fSBeKScbxIkAAAAM/anime-dance.gif",
                    "https://media.giphy.com/media/nAvSNP8Y3F94hq9Rga/giphy.gif"
                };

                for (String url : imageUrls) {
                    try {
                        System.out.println("Trying to load victory image from URL: " + url);
                        victoryImage = new Image(url, true);
                        if (!victoryImage.isError()) {
                            System.out.println("Successfully loaded victory image from: " + url);
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to load from: " + url);
                    }
                }
            }

            if (victoryImage != null && !victoryImage.isError()) {
                victoryImageView = new ImageView(victoryImage);
                victoryImageView.setFitWidth(300);
                victoryImageView.setFitHeight(300);
                victoryImageView.setPreserveRatio(true);
                victoryImageView.setLayoutX(width / 2 - 150);
                victoryImageView.setLayoutY(height / 2 - 250);
                victoryImageView.setVisible(false);

                root.getChildren().add(victoryImageView);
                System.out.println("Victory image view added successfully!");
            } else {
                System.out.println("Could not load victory image from any source");
                System.out.println("Please place a 'victory.gif' file in: src/main/resources/images/");
                victoryImageView = null;
            }
        } catch (Exception e) {
            System.out.println("Could not load victory image: " + e.getMessage());
            e.printStackTrace();
            victoryImageView = null;
        }
    }

    private void loadGameOverImage() {
        try {
            Image gameOverImage = null;

            // Try to load from local resources first
            try {
                java.io.File localFile = new java.io.File("src/main/resources/images/gameover.gif");
                if (localFile.exists()) {
                    System.out.println("Loading game over image from local file: " + localFile.getPath());
                    gameOverImage = new Image(localFile.toURI().toString());
                    if (!gameOverImage.isError()) {
                        System.out.println("Successfully loaded game over image from local file!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not load from local file: " + e.getMessage());
            }

            // If local file failed, try resource path
            if (gameOverImage == null || gameOverImage.isError()) {
                try {
                    java.net.URL resourceUrl = getClass().getResource("/images/gameover.gif");
                    if (resourceUrl != null) {
                        System.out.println("Loading game over image from resources");
                        gameOverImage = new Image(resourceUrl.toString());
                        if (!gameOverImage.isError()) {
                            System.out.println("Successfully loaded game over image from resources!");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not load from resources: " + e.getMessage());
                }
            }

            // If still no image, try URLs as fallback
            if (gameOverImage == null || gameOverImage.isError()) {
                String[] imageUrls = {
                    "https://media.tenor.com/wD7yF6gA1XwAAAAM/skeleton-dance.gif",
                    "https://media.giphy.com/media/3oKIPsx2VAYAgEHC12/giphy.gif",
                    "https://i.imgur.com/bJPo2.gif",
                    "https://media1.tenor.com/m/p0wM4WV3XPAAAAAC/skeleton-dancing.gif"
                };

                for (String url : imageUrls) {
                    try {
                        System.out.println("Trying to load game over image from URL: " + url);
                        gameOverImage = new Image(url, true);
                        if (!gameOverImage.isError()) {
                            System.out.println("Successfully loaded game over image from: " + url);
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to load from: " + url);
                    }
                }
            }

            if (gameOverImage != null && !gameOverImage.isError()) {
                gameOverImageView = new ImageView(gameOverImage);
                gameOverImageView.setFitWidth(300);
                gameOverImageView.setFitHeight(300);
                gameOverImageView.setPreserveRatio(true);
                gameOverImageView.setLayoutX(width / 2 - 150);
                gameOverImageView.setLayoutY(height / 2 - 250);
                gameOverImageView.setVisible(false);

                root.getChildren().add(gameOverImageView);
                System.out.println("Game over image view added successfully!");
            } else {
                System.out.println("Could not load game over image from any source");
                System.out.println("Please place a 'gameover.gif' file in: src/main/resources/images/");
                gameOverImageView = null;
            }
        } catch (Exception e) {
            System.out.println("Could not load game over image: " + e.getMessage());
            e.printStackTrace();
            gameOverImageView = null;
        }
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
            System.out.println("INIT: Player " + (i + 1) + " start position: " +
                playerStartPositions[i][0] + ", " + playerStartPositions[i][1]);
        }
        System.out.println("INIT: playerStartPositions array size: " + playerStartPositions.length);

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
    }

    private void returnToMenu() {
        stop();
        // Clear host settings override when returning to menu
        GameSettings.clearHostSettings();
        MenuScene menuScene = new MenuScene(stage, width, height);
        stage.setScene(menuScene.getScene());
    }

    // Push apart tanks that are overlapping or touching to prevent them from getting stuck
    private void pushApartOverlappingTanks(List<Tank> allTanks) {
        final double PUSH_FORCE = 3.0; // Pixels to push per frame
        final double MIN_GAP = 4.0; // Minimum gap to maintain between tanks

        for (int i = 0; i < allTanks.size(); i++) {
            Tank tank1 = allTanks.get(i);
            if (!tank1.isAlive()) continue;

            for (int j = i + 1; j < allTanks.size(); j++) {
                Tank tank2 = allTanks.get(j);
                if (!tank2.isAlive()) continue;

                // Calculate distance between tank centers
                double dx = tank2.getX() - tank1.getX();
                double dy = tank2.getY() - tank1.getY();

                // Required separation (half sizes + gap)
                double requiredSepX = (tank1.getSize() + tank2.getSize()) / 2.0 + MIN_GAP;
                double requiredSepY = (tank1.getSize() + tank2.getSize()) / 2.0 + MIN_GAP;

                // Calculate overlap in each axis
                double overlapX = requiredSepX - Math.abs(dx);
                double overlapY = requiredSepY - Math.abs(dy);

                // If overlapping or too close in both dimensions
                if (overlapX > 0 && overlapY > 0) {
                    boolean tank1IsBoss = tank1.getEnemyType() == Tank.EnemyType.BOSS;
                    boolean tank2IsBoss = tank2.getEnemyType() == Tank.EnemyType.BOSS;

                    // BOSS tank kills any tank it touches (except other BOSS)
                    if (tank1IsBoss && !tank2IsBoss) {
                        tank2.instantKill();
                        soundManager.playExplosion();
                        String victimName = getPlayerNameForTank(tank2);
                        System.out.println("KILL LOG: " + victimName + " was killed by BOSS (contact)");
                        continue; // Don't push, tank is dead
                    }
                    if (tank2IsBoss && !tank1IsBoss) {
                        tank1.instantKill();
                        soundManager.playExplosion();
                        String victimName = getPlayerNameForTank(tank1);
                        System.out.println("KILL LOG: " + victimName + " was killed by BOSS (contact)");
                        continue; // Don't push, tank is dead
                    }

                    // Push along the axis with LESS overlap (faster separation)
                    double pushX = 0;
                    double pushY = 0;

                    if (overlapX < overlapY) {
                        // Push horizontally
                        pushX = (dx >= 0 ? 1 : -1) * PUSH_FORCE;
                    } else {
                        // Push vertically
                        pushY = (dy >= 0 ? 1 : -1) * PUSH_FORCE;
                    }

                    // If tanks are exactly aligned, add small perpendicular push
                    if (Math.abs(dx) < 1 && Math.abs(dy) < 1) {
                        pushX = (Math.random() > 0.5 ? 1 : -1) * PUSH_FORCE;
                        pushY = (Math.random() > 0.5 ? 1 : -1) * PUSH_FORCE;
                    }

                    double tank1Push = tank2IsBoss ? 1.0 : 0.5;
                    double tank2Push = tank1IsBoss ? 1.0 : 0.5;

                    // Check if new positions are valid before applying
                    double newX1 = tank1.getX() - pushX * tank1Push;
                    double newY1 = tank1.getY() - pushY * tank1Push;
                    double newX2 = tank2.getX() + pushX * tank2Push;
                    double newY2 = tank2.getY() + pushY * tank2Push;

                    // Apply push only if the new position doesn't collide with walls
                    if (!gameMap.checkTankCollision(newX1, newY1, tank1.getSize(), tank1.hasShip())) {
                        tank1.setPosition(newX1, newY1);
                    }
                    if (!gameMap.checkTankCollision(newX2, newY2, tank2.getSize(), tank2.hasShip())) {
                        tank2.setPosition(newX2, newY2);
                    }
                }
            }
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

        System.out.println("TEST MODE: Spawned BOSS at (" + bossX + ", " + bossY + ") facing DOWN");
        System.out.println("Player is at (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("Player has shield: " + player.hasShield() + ", pause shield: " + player.hasPauseShield());
        System.out.println("Press B again to spawn another BOSS");
    }

    private void tryTakeLifeFromTeammate() {
        // For local player (host or single player)
        int myPlayerIndex = isNetworkGame && network != null && !network.isHost()
            ? network.getPlayerNumber() - 1
            : 0; // Single player or host uses player 0
        tryTakeLifeFromTeammate(myPlayerIndex);
    }

    private void tryTakeLifeFromTeammate(int requestingPlayerIndex) {
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
                System.out.println("Player " + (requestingPlayerIndex + 1) + " took a life from Player " + (i + 1));
                return;
            }
        }
    }

    private void startNextLevel() {
        LevelTransitionManager.startNextLevel(this);
    }

    private void restartCurrentLevel() {
        LevelTransitionManager.restartCurrentLevel(this);
    }

    public void start() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Limit frame rate to ~60 FPS by skipping frames if not enough time has passed
                if (lastFrameTime != 0 && now - lastFrameTime < FRAME_TIME) {
                    return; // Not enough time has passed since last frame
                }
                lastFrameTime = now;

                update();
                render();

                // FPS counter
                fpsFrameCount++;
                long currentTime = System.currentTimeMillis();
                if (currentTime - fpsLastTime >= 5000) {
                    double fps = fpsFrameCount / 5.0;
                    System.out.println("[CLIENT FPS] " + String.format("%.1f", fps));
                    fpsFrameCount = 0;
                    fpsLastTime = currentTime;
                }
            }
        };
        gameLoop.start();
    }

    private double[] getRandomPowerUpSpawnPosition() {
        // Delegate to shared GameLogic
        return GameLogic.findPowerUpSpawnPosition(gameMap, 32);
    }

    private PowerUp.Type applyRandomPowerUp(Tank player) {
        // Choose a random power-up type (excluding BOMB and FREEZE which affect game state)
        PowerUp.Type[] goodTypes = {
            PowerUp.Type.GUN,
            PowerUp.Type.STAR,
            PowerUp.Type.CAR,
            PowerUp.Type.SHIP,
            PowerUp.Type.SAW,
            PowerUp.Type.TANK,
            PowerUp.Type.SHIELD,
            PowerUp.Type.MACHINEGUN
        };
        PowerUp.Type type = goodTypes[GameConstants.RANDOM.nextInt(goodTypes.length)];

        // Apply the power-up effect directly to the player
        PowerUp tempPowerUp = new PowerUp(0, 0, type);
        tempPowerUp.applyEffect(player);
        System.out.println("BOSS KILL REWARD: Player received " + type + "!");
        return type;
    }

    /**
     * Add points to a player's score and award extra life for every 100 points.
     */
    private void addScore(int playerIndex, int points) {
        if (playerIndex < 0 || playerIndex >= 4) return;

        int oldScore = playerScores[playerIndex];
        int newScore = oldScore + points;
        playerScores[playerIndex] = newScore;
        playerLevelScores[playerIndex] += points; // Also track level score
        System.out.println("SCORE: Player " + (playerIndex + 1) + " score: " + oldScore + " -> " + newScore + " (+" + points + ")");

        // Check if crossed a 100-point threshold (e.g., 0->100, 95->105, 199->201)
        int oldHundreds = oldScore / 100;
        int newHundreds = newScore / 100;

        if (newHundreds > oldHundreds && playerIndex < playerTanks.size()) {
            Tank player = playerTanks.get(playerIndex);
            int livesAwarded = newHundreds - oldHundreds;
            for (int i = 0; i < livesAwarded; i++) {
                player.addLife();
            }
            System.out.println("Player " + (playerIndex + 1) + " earned " + livesAwarded + " extra life(s) for reaching " + (newHundreds * 100) + " points!");
        }
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

        // Delegate to UFOManager
        ufoManager.checkAndSpawnUFO(playerTanks, width, height);
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
                System.out.println("[DEBUG] Local game updates per second: " + (localUpdateCount / 5.0));
                localUpdateCount = 0;
                lastLocalUpdateTime = now;
            }
        }

        // Network clients must always receive game state to detect level/game transitions
        // even during victory/gameOver screens
        if (isNetworkGame && network != null && !network.isHost()) {
            // CLIENT: Always receive and apply game state from host
            GameState state = network.getLatestGameState();
            if (state != null) {
                applyGameState(state);
            }

            // CLIENT: Handle game over/victory - send restart/next level requests
            if (gameOver || victory) {
                // Check if ENTER is pressed to request restart or next level
                if (inputHandler.isEnterPressed()) {
                    PlayerInput input = new PlayerInput();
                    input.requestNextLevel = victory;
                    input.requestRestart = gameOver;
                    input.nickname = NicknameManager.getNickname();
                    network.sendInput(input);
                }
                return;
            }

            if (paused) {
                return;
            }

            // CLIENT: Move locally and send position to host
            int myPlayerIndex = network.getPlayerNumber() - 1;
            if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
                Tank myTank = playerTanks.get(myPlayerIndex);

                // Capture input
                PlayerInput input = inputHandler.capturePlayerInput();

                // Apply movement locally (skip if paused or dead)
                if (myTank.isAlive() && !powerUpEffectManager.arePlayersFrozen() && !playerPaused[myPlayerIndex]) {
                    List<Tank> allTanks = new ArrayList<>();
                    allTanks.addAll(playerTanks);
                    allTanks.addAll(enemyTanks);

                    if (input.up) {
                        myTank.move(Direction.UP, gameMap, allTanks, base);
                    } else if (input.down) {
                        myTank.move(Direction.DOWN, gameMap, allTanks, base);
                    } else if (input.left) {
                        myTank.move(Direction.LEFT, gameMap, allTanks, base);
                    } else if (input.right) {
                        myTank.move(Direction.RIGHT, gameMap, allTanks, base);
                    }
                }

                // Shoot locally for sound (skip if paused)
                if (myTank.isAlive() && input.shoot && !playerPaused[myPlayerIndex]) {
                    if (myTank.hasLaser()) {
                        Laser laser = myTank.shootLaser(soundManager);
                        if (laser != null) {
                            lasers.add(laser);
                        }
                    } else {
                        myTank.shoot(bullets, soundManager);
                    }
                }

                // Send position and nickname to host (only if alive and we've received initial state from host)
                // This prevents sending our local init position before receiving the host's authoritative position
                // Also wait a few frames after respawn to ensure we have the correct position from server
                if (respawnSyncFrames > 0) {
                    respawnSyncFrames--;
                }
                if (myTank.isAlive() && firstStateReceived && respawnSyncFrames == 0) {
                    input.posX = myTank.getX();
                    input.posY = myTank.getY();
                    input.direction = myTank.getDirection().ordinal();
                } else {
                    // When dead, not yet synced, or just respawned - send invalid position so host knows not to use it
                    input.posX = -1;
                    input.posY = -1;
                    input.direction = 0;
                }
                // Always send local nickname from NicknameManager (not from array which may not be set yet)
                input.nickname = NicknameManager.getNickname();
                network.sendInput(input);
            }
            // Client skips rest of game logic
            return;
        }

        // HOST: Send game state to clients (do this BEFORE early return for gameOver/victory/paused)
        // This ensures clients receive end-game state (dancing characters, victory girls, etc.)
        if (isNetworkGame && network != null && network.isHost()) {
            long now = System.nanoTime();
            if (now - lastNetworkUpdate >= NETWORK_UPDATE_INTERVAL) {
                GameState state = buildGameState();
                network.sendGameState(state);
                lastNetworkUpdate = now;
            }

            // HOST: Check for client requests for next level/restart (during game over/victory)
            if (gameOver || victory) {
                for (int i = 2; i <= Math.min(playerTanks.size(), network.getConnectedPlayerCount()); i++) {
                    PlayerInput clientInput = network.getPlayerInput(i);
                    if (clientInput != null) {
                        if (clientInput.requestNextLevel && victory) {
                            System.out.println("HOST: Client " + i + " requested next level");
                            startNextLevel();
                            return;
                        }
                        if (clientInput.requestRestart && gameOver) {
                            System.out.println("HOST: Client " + i + " requested restart");
                            restartCurrentLevel();
                            return;
                        }
                    }
                }
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
                System.out.println("Network connection lost!");
                return;
            }

            if (network.isHost()) {
                // HOST: Add new player tanks if more players connected
                int connectedCount = network.getConnectedPlayerCount();
                while (playerTanks.size() < connectedCount && playerTanks.size() < 4) {
                    int playerNum = playerTanks.size() + 1;
                    double x, y;
                    switch (playerNum) {
                        case 2 -> { x = 16 * 32; y = 24 * 32; }
                        case 3 -> { x = 9 * 32; y = 24 * 32; }
                        case 4 -> { x = 15 * 32; y = 24 * 32; }
                        default -> { x = 8 * 32; y = 24 * 32; }
                    }
                    System.out.println("HOST: Adding Player " + playerNum + " tank (new player connected)");
                    Tank newPlayer = new Tank(x, y, Direction.UP, true, playerNum);
                    newPlayer.giveTemporaryShield(); // Give spawn protection
                    playerTanks.add(newPlayer);

                    // Update playerStartPositions array
                    double[][] newStartPositions = new double[playerTanks.size()][2];
                    for (int j = 0; j < playerStartPositions.length; j++) {
                        newStartPositions[j] = playerStartPositions[j];
                    }
                    newStartPositions[playerNum - 1] = new double[]{x, y};
                    playerStartPositions = newStartPositions;
                    System.out.println("HOST: Updated playerStartPositions for Player " + playerNum +
                        " to: " + x + ", " + y + " (array size: " + playerStartPositions.length + ")");
                }

                // HOST: Receive client positions and apply them (client-authoritative movement)
                for (int i = 2; i <= playerTanks.size(); i++) {
                    PlayerInput clientInput = network.getPlayerInput(i);
                    if (clientInput != null) {
                        Tank clientTank = playerTanks.get(i - 1);
                        // Accept client's position directly (only if valid - client sends -1,-1 when dead)
                        if (clientTank.isAlive() && clientInput.posX >= 0 && clientInput.posY >= 0) {
                            clientTank.setPosition(clientInput.posX, clientInput.posY);
                            clientTank.setDirection(Direction.values()[clientInput.direction]);
                        }
                        // Handle shooting on host (for bullet sync)
                        if (clientInput.shoot && clientTank.isAlive()) {
                            if (clientTank.hasLaser()) {
                                Laser laser = clientTank.shootLaser(soundManager);
                                if (laser != null) {
                                    lasers.add(laser);
                                }
                            } else {
                                clientTank.shoot(bullets, soundManager);
                            }
                        }
                        // Check if client is requesting a life transfer
                        if (clientInput.requestLife) {
                            tryTakeLifeFromTeammate(i - 1);
                        }
                        // Update client's nickname
                        if (clientInput.nickname != null) {
                            playerNicknames[i - 1] = clientInput.nickname;
                        }
                    }
                }
                // Host runs full game logic below
            }
        }

        // Create combined list of all tanks for collision detection
        List<Tank> allTanks = new ArrayList<>();
        allTanks.addAll(playerTanks);
        allTanks.addAll(enemyTanks);

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
                System.out.println("Player " + (i + 1) + " will respawn in 1 second at: " + respawnX + ", " + respawnY + " (lives left: " + player.getLives() + ")");
                player.respawn(respawnX, respawnY); // This now starts the timer
            } else if (player.getLives() == 1) {
                // Player died on last life - decrement to 0 so game over check works
                player.setLives(0);
                System.out.println("Player " + (i + 1) + " lost their last life!");
            }
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
            System.out.println("Enemy team speed boost expired - only original enemy keeps the speed");
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
                        System.out.println("UFO destroyed by Player " + killerPlayer + " - awarded 20 points!");
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
                        if (killer.getMachinegunCount() > 0) {
                            ufoManager.recordMachinegunKill(killerPlayer - 1);
                        }
                        addScore(killerPlayer - 1, ProjectileHandler.getScoreForKill(enemy.getEnemyType()));
                        if (enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                            System.out.println("BOSS killed by Player " + killerPlayer + " - awarding power-up!");
                            bossKillerPlayerIndex = killerPlayer - 1;
                            bossKillPowerUpReward = applyRandomPowerUp(killer);
                        }
                    }
                }

                // Handle power-up drop
                if (result.shouldDropPowerUp) {
                    double[] spawnPos = getRandomPowerUpSpawnPosition();
                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                }

                // Handle player killed - spawn power-up in 3+ player mode
                if (result.playerKilled && playerTanks.size() > 2) {
                    double[] spawnPos = getRandomPowerUpSpawnPosition();
                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                    System.out.println("Power-up spawned for killed player (3+ players mode)");
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

        // Update lasers and check collisions
        Iterator<Laser> laserIterator = lasers.iterator();
        while (laserIterator.hasNext()) {
            Laser laser = laserIterator.next();
            laser.update();

            // Remove expired lasers
            if (laser.isExpired()) {
                laserIterator.remove();
                continue;
            }

            // Laser collisions - damage any tank in the beam's path
            // Laser passes through everything except tanks and base (dealing 3 damage)
            if (!laser.isFromEnemy()) {
                // Player laser - hits enemies
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && laser.collidesWith(enemy)) {
                        // Deal 3 damage
                        for (int dmg = 0; dmg < 3 && enemy.isAlive(); dmg++) {
                            enemy.damage();
                        }
                        if (!enemy.isAlive()) {
                            soundManager.playExplosion();
                            // Track kill for the player who fired the laser
                            int killerPlayer = laser.getOwnerPlayerNumber();
                            if (killerPlayer >= 1 && killerPlayer <= 4) {
                                playerKills[killerPlayer - 1]++;
                                int enemyTypeOrdinal = enemy.getEnemyType().ordinal();
                                if (enemyTypeOrdinal < 6) {
                                    playerKillsByType[killerPlayer - 1][enemyTypeOrdinal]++;
                                }
                                // Award points based on enemy type
                                int points = switch (enemy.getEnemyType()) {
                                    case POWER -> 2;
                                    case HEAVY -> 5;
                                    case BOSS -> 10;
                                    default -> 1;
                                };
                                addScore(killerPlayer - 1, points);
                                // BOSS kill rewards
                                if (enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                                    Tank killer = playerTanks.get(killerPlayer - 1);
                                    bossKillerPlayerIndex = killerPlayer - 1;
                                    bossKillPowerUpReward = applyRandomPowerUp(killer);
                                }
                            }
                            // Chance for power-up drop
                            if (Math.random() < 0.3) {
                                double[] spawnPos = getRandomPowerUpSpawnPosition();
                                powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                            }
                        }
                    }
                }
                // Player laser can hit base (but shouldn't since it passes through obstacles)
                // However, if aimed directly at base, it will hit it
                if (laser.collidesWithBase(base) && base.isAlive()) {
                    base.destroy();
                    soundManager.playBaseDestroyed();
                    gameOver = true;
                }
            } else {
                // Enemy laser (if enemies could have lasers) - hits players
                for (Tank player : playerTanks) {
                    if (player.isAlive() && !player.hasShield() && !player.hasPauseShield() && laser.collidesWith(player)) {
                        // Deal 3 damage
                        for (int dmg = 0; dmg < 3 && player.isAlive(); dmg++) {
                            player.damage();
                        }
                        if (!player.isAlive()) {
                            soundManager.playPlayerDeath();
                        }
                    }
                }
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
            System.out.println("Easter egg collected by Player " + (playerIndex + 1) + "! +3 lives!");
            GameLogic.applyEasterEggEffect(enemyTanks, true);
        } else if (eggResult.easterEggCollectedByEnemy) {
            System.out.println("Easter egg collected by enemy! All enemies become HEAVY tanks!");
            GameLogic.applyEasterEggEffect(enemyTanks, false);
        } else if (eggResult.easterEggExpired) {
            System.out.println("Easter egg expired!");
        }

        // Remove dead enemy tanks using shared GameLogic
        GameLogic.removeDeadEnemies(enemyTanks);

        // Check victory condition with delay using shared GameLogic
        if (GameLogic.checkVictory(enemySpawner, enemyTanks)) {
            if (!victoryConditionMet) {
                victoryConditionMet = true;
                victoryDelayTimer = 0;
                System.out.println("All enemies defeated! Victory in 10 seconds...");
            }
            victoryDelayTimer++;
            if (victoryDelayTimer >= VICTORY_DELAY) {
                victory = true;
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
    private String getPlayerDisplayName(int playerIndex) {
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
        gc.setFill(Color.WHITE);
        gc.fillText("Level: " + gameMap.getLevelNumber() + "  Enemies: " + enemySpawner.getRemainingEnemies(), 10, 20);

        // Display player info and power-ups - only show connected players
        int displayCount = getDisplayPlayerCount();
        for (int i = 0; i < displayCount && i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            String playerName = getPlayerDisplayName(i);
            double yOffset = 40 + i * 60;

            // Display lives (show remaining respawns, not total lives), kills and score
            int displayLives = Math.max(0, player.getLives() - 1);
            gc.setFill(Color.WHITE);
            gc.fillText(playerName + " Lives: " + displayLives + "  Kills: " + playerKills[i] + "  Score: " + playerScores[i], 10, yOffset);

            // Display power-ups
            double xOffset = 10;
            yOffset += 10;

            if (player.hasGun()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.GUN);
                xOffset += 20;
            }
            if (player.getStarCount() > 0) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.STAR);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getStarCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
            if (player.getCarCount() > 0) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.CAR);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getCarCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
            if (player.hasShip()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SHIP);
                xOffset += 20;
            }
            if (player.hasSaw()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SAW);
                xOffset += 20;
            }
            if (player.hasShield()) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SHIELD);
                xOffset += 20;
            }
            if (player.getMachinegunCount() > 0) {
                iconRenderer.renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.MACHINEGUN);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getMachinegunCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
        }

        // Render BOSS health indicator if BOSS is alive
        effectRenderer.renderBossHealthBar(enemyTanks);

        if (gameOver) {
            // Initialize dancing characters when base was destroyed (not when players died)
            if (!base.isAlive() && !celebrationManager.isDancingInitialized()) {
                celebrationManager.initializeDancingCharacters(base, enemyTanks);
            }

            // Update and render dancing characters
            celebrationManager.updateDancingCharacters();
            for (DancingCharacter dancer : celebrationManager.getDancingCharacters()) {
                dancer.render(gc);
            }

            // Render laughing skull instead of GIF (synchronized for all players)
            effectRenderer.renderLaughingSkull(width / 2, height / 2 - 150);

            // Play sad sound once and stop gameplay sounds
            if (!gameOverSoundPlayed) {
                soundManager.stopGameplaySounds();
                soundManager.playSad();
                gameOverSoundPlayed = true;
            }

            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font(40));
            gc.fillText("GAME OVER", width / 2 - 120, height / 2 + 50);

            // Show statistics
            renderEndGameStats(height / 2 + 90);

            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font(22));
            gc.fillText("Press ENTER to restart", width / 2 - 110, height / 2 + 310);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(18));
            gc.fillText("Press ESC to return to menu", width / 2 - 115, height / 2 + 340);
        } else if (victory) {
            // Initialize victory celebration (Soviet flag + dancing girls)
            if (!celebrationManager.isVictoryDancingInitialized()) {
                soundManager.stopGameplaySounds();
                celebrationManager.initializeVictoryCelebration(base, playerTanks.size());
            }

            // Update and render dancing girls
            celebrationManager.updateVictoryGirls();
            for (DancingGirl girl : celebrationManager.getVictoryDancingGirls()) {
                girl.render(gc);
            }

            // Show dancing anime girl if available
            if (victoryImageView != null) {
                victoryImageView.setVisible(true);
            }

            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font(40));
            gc.fillText("LEVEL " + gameMap.getLevelNumber() + " COMPLETE!", width / 2 - 180, height / 2 + 50);

            // Show statistics
            renderEndGameStats(height / 2 + 90);

            gc.setFill(Color.LIME);
            gc.setFont(javafx.scene.text.Font.font(22));
            gc.fillText("Press ENTER for next level", width / 2 - 130, height / 2 + 310);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(18));
            gc.fillText("Press ESC to return to menu", width / 2 - 115, height / 2 + 340);
        } else if (paused) {
            // Draw pause menu overlay
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(0, 0, width, height);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 50));
            gc.fillText("PAUSED", width / 2 - 100, height / 2 - 80);

            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 30));

            // Resume option
            if (pauseMenuSelection == 0) {
                gc.setFill(Color.YELLOW);
                gc.fillText("> RESUME <", width / 2 - 80, height / 2);
            } else {
                gc.setFill(Color.WHITE);
                gc.fillText("  RESUME  ", width / 2 - 80, height / 2);
            }

            // Exit option
            if (pauseMenuSelection == 1) {
                gc.setFill(Color.YELLOW);
                gc.fillText("> EXIT <", width / 2 - 60, height / 2 + 50);
            } else {
                gc.setFill(Color.WHITE);
                gc.fillText("  EXIT  ", width / 2 - 60, height / 2 + 50);
            }

            gc.setFill(Color.GRAY);
            gc.setFont(javafx.scene.text.Font.font(16));
            gc.fillText("Use UP/DOWN to select, ENTER to confirm", width / 2 - 150, height / 2 + 120);
        } else {
            // Hide images when not in end state
            if (victoryImageView != null) {
                victoryImageView.setVisible(false);
            }
            if (gameOverImageView != null) {
                gameOverImageView.setVisible(false);
            }

            // Show hint to take life if player is dead and teammate has lives
            int myPlayerIndex = isNetworkGame && network != null && !network.isHost()
                ? network.getPlayerNumber() - 1 : 0;
            if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
                Tank myTank = playerTanks.get(myPlayerIndex);
                if (!myTank.isAlive() && myTank.getLives() <= 0) {
                    // Check if any teammate has spare lives
                    boolean canTakeLife = false;
                    for (int i = 0; i < playerTanks.size(); i++) {
                        if (i != myPlayerIndex && playerTanks.get(i).getLives() > 1) {
                            canTakeLife = true;
                            break;
                        }
                    }
                    if (canTakeLife) {
                        gc.setFill(Color.YELLOW);
                        gc.setFont(javafx.scene.text.Font.font(20));
                        gc.fillText("Press ENTER to take life from teammate", width / 2 - 180, height / 2);
                    }
                }
            }

            // Show pause indicator for multiplayer
            if (isNetworkGame) {
                int pausePlayerIndex = network != null && !network.isHost()
                    ? network.getPlayerNumber() - 1 : 0;
                if (pausePlayerIndex >= 0 && pausePlayerIndex < playerTanks.size() && playerPaused[pausePlayerIndex]) {
                    gc.setFill(Color.rgb(0, 0, 0, 0.5));
                    gc.fillRect(0, 0, width, 60);
                    gc.setFill(Color.YELLOW);
                    gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 30));
                    gc.fillText("PAUSED - Press ESC to resume", width / 2 - 200, 40);
                }
            }
        }
    }

    private void renderEndGameStats(double startY) {
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
            System.out.println("Closing network connection...");
            network.close();
        }
    }

    // ============ NETWORK MULTIPLAYER METHODS ============

    private GameState buildGameState() {
        int connectedPlayers = network != null ? network.getConnectedPlayerCount() : playerCount;
        GameState state = GameStateBuilder.build(
            playerTanks, playerKills, playerScores, playerLevelScores, playerNicknames, playerKillsByType,
            enemyTanks, bullets, lasers, powerUps,
            gameOver, victory, enemySpawner, gameMap, base, connectedPlayers,
            powerUpEffectManager, bossKillerPlayerIndex, bossKillPowerUpReward,
            celebrationManager, mapChanges, ufoManager
        );
        mapChanges.clear(); // Clear after building state
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
    @Override public void setRespawnSyncFrames(int frames) { respawnSyncFrames = frames; }
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

    private PlayerInput capturePlayerInput(Tank tank) {
        // Capture current keyboard state (arrow keys + space)
        return inputHandler.capturePlayerInput();
    }

    private void applyPlayerInput(Tank tank, PlayerInput input) {
        if (!tank.isAlive()) return;

        List<Tank> allTanks = new ArrayList<>();
        allTanks.addAll(playerTanks);
        allTanks.addAll(enemyTanks);

        // Apply movement (only if not frozen)
        if (!powerUpEffectManager.arePlayersFrozen()) {
            if (input.up) {
                tank.move(Direction.UP, gameMap, allTanks, base);
            } else if (input.down) {
                tank.move(Direction.DOWN, gameMap, allTanks, base);
            } else if (input.left) {
                tank.move(Direction.LEFT, gameMap, allTanks, base);
            } else if (input.right) {
                tank.move(Direction.RIGHT, gameMap, allTanks, base);
            }
        }

        // Apply shooting (always allowed, even when frozen)
        if (input.shoot) {
            // Use laser if tank has laser power-up, otherwise use normal bullets
            if (tank.hasLaser()) {
                Laser laser = tank.shootLaser(soundManager);
                if (laser != null) {
                    lasers.add(laser);
                }
            } else {
                tank.shoot(bullets, soundManager);
            }
        }
    }

}
