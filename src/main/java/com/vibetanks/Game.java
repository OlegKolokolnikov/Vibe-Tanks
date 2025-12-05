package com.vibetanks;

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
import java.util.Random;

public class Game {
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
    private double[][] playerStartPositions; // For respawning

    // Fixed start positions for each player (indexed by playerNumber - 1)
    private static final double[][] FIXED_START_POSITIONS = {
        {8 * 32, 24 * 32},   // Player 1
        {16 * 32, 24 * 32},  // Player 2
        {9 * 32, 24 * 32},   // Player 3
        {15 * 32, 24 * 32}   // Player 4
    };

    private AnimationTimer gameLoop;
    private long lastUpdate = 0;
    private static final long FRAME_TIME = 16_666_667; // ~60 FPS in nanoseconds

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

    // Player kills and score tracking
    private int[] playerKills = new int[4];
    private int[] playerScores = new int[4]; // Total score across all levels
    private int[] playerLevelScores = new int[4]; // Score for current level only
    private boolean winnerBonusAwarded = false;

    // Kills per enemy type per player: [playerIndex][enemyTypeOrdinal]
    // Enemy types: REGULAR=0, ARMORED=1, FAST=2, POWER=3, HEAVY=4, BOSS=5
    private int[][] playerKillsByType = new int[4][6];

    // For client sound effects (track previous state to detect changes)
    private int prevEnemyCount = 0;
    private Set<Long> seenBulletIds = new HashSet<>(); // Track bullet IDs we've already played sounds for
    private boolean firstStateReceived = false; // Skip sounds on first state to avoid burst
    private int respawnSyncFrames = 0; // Frames to wait after respawn before sending position

    // SHOVEL power-up - base protection with steel
    private int baseProtectionDuration = 0;
    private static final int BASE_PROTECTION_TIME = 3600; // 1 minute at 60 FPS
    private boolean isFlashing = false;
    private int flashCount = 0; // Counts the number of flashes (up to 10 for 5 complete flashes)
    private int flashTimer = 0; // Timer for each flash state (60 frames = 1 second)
    private static final int FLASH_DURATION = 60; // 1 second at 60 FPS
    private static final int TOTAL_FLASHES = 10; // 5 complete flashes (10 state changes)

    // FREEZE power-up - freeze enemies or players
    private int enemyFreezeDuration = 0;
    private int playerFreezeDuration = 0;
    private static final int FREEZE_TIME = 600; // 10 seconds at 60 FPS

    // Enemy team speed boost (when enemy picks up CAR)
    private int enemyTeamSpeedBoostDuration = 0;
    private Tank enemyWithPermanentSpeedBoost = null; // The enemy who picked up CAR keeps the boost
    private static final int ENEMY_SPEED_BOOST_TIME = 1800; // 30 seconds at 60 FPS
    private static final double ENEMY_TEAM_SPEED_BOOST = 0.3; // 30% speed boost

    // Victory dancing anime girl
    private ImageView victoryImageView;
    // Game over dancing death
    private ImageView gameOverImageView;
    private boolean gameOverSoundPlayed = false;

    // Dancing aliens/humans when enemies destroy base
    private List<DancingCharacter> dancingCharacters = new ArrayList<>();

    // UFO bonus enemy
    private UFO ufo;
    private boolean ufoSpawnedThisLevel = false;
    private int[] playerMachinegunKills = new int[4]; // Kills while player had machinegun
    private boolean ufoWasKilled = false; // Track if UFO was killed vs escaped
    private int ufoLostMessageTimer = 0; // Timer for "Lost it!" message (3 seconds = 180 frames)
    private int ufoKilledMessageTimer = 0; // Timer for "Zed is dead!" message (3 seconds = 180 frames)
    private static final int UFO_MESSAGE_DURATION = 180; // 3 seconds at 60 FPS
    private boolean dancingInitialized = false;

    // Easter egg collectible (spawns when UFO is killed)
    private EasterEgg easterEgg = null;

    // Victory dancing girls
    private List<DancingGirl> victoryDancingGirls = new ArrayList<>();
    private boolean victoryDancingInitialized = false;

    // Victory delay (5 seconds before showing victory screen)
    private boolean victoryConditionMet = false;
    private int victoryDelayTimer = 0;
    private static final int VICTORY_DELAY = 300; // 5 seconds at 60 FPS

    // Player nicknames (index 0-3 for players 1-4)
    private String[] playerNicknames = new String[4];

    // Boss kill tracking for victory screen
    private int bossKillerPlayerIndex = -1; // -1 = not killed yet, 0-3 = player index
    private PowerUp.Type bossKillPowerUpReward = null; // Power-up received for killing boss

    // Inner class for dancing characters
    private static class DancingCharacter {
        static final Color[] ALIEN_COLORS = {Color.LIME, Color.CYAN, Color.MAGENTA, Color.YELLOW};
        static final Color[] HUMAN_COLORS = {Color.PEACHPUFF, Color.TAN, Color.SANDYBROWN, Color.WHEAT};

        double x, y;
        boolean isAlien;
        int animFrame;
        int danceStyle; // 0-2 different dance moves
        int colorIndex;
        Color color;

        DancingCharacter(double x, double y, boolean isAlien, int danceStyle) {
            this.x = x;
            this.y = y;
            this.isAlien = isAlien;
            this.animFrame = 0;
            this.danceStyle = danceStyle;
            // Random colors for variety
            if (isAlien) {
                this.colorIndex = (int)(Math.random() * ALIEN_COLORS.length);
                this.color = ALIEN_COLORS[colorIndex];
            } else {
                this.colorIndex = (int)(Math.random() * HUMAN_COLORS.length);
                this.color = HUMAN_COLORS[colorIndex];
            }
        }

        // Constructor for network sync (with specific colorIndex)
        DancingCharacter(double x, double y, boolean isAlien, int animFrame, int danceStyle, int colorIndex) {
            this.x = x;
            this.y = y;
            this.isAlien = isAlien;
            this.animFrame = animFrame;
            this.danceStyle = danceStyle;
            this.colorIndex = colorIndex;
            if (isAlien) {
                this.color = ALIEN_COLORS[colorIndex % ALIEN_COLORS.length];
            } else {
                this.color = HUMAN_COLORS[colorIndex % HUMAN_COLORS.length];
            }
        }

        void update() {
            animFrame++;
        }

        void render(GraphicsContext gc) {
            int cycle = (animFrame / 8) % 4; // Animation cycle

            gc.save();
            gc.translate(x, y);

            if (isAlien) {
                renderAlien(gc, cycle);
            } else {
                renderHuman(gc, cycle);
            }

            gc.restore();
        }

        private void renderAlien(GraphicsContext gc, int cycle) {
            // Body bobbing
            double bob = Math.sin(animFrame * 0.3) * 3;

            // Alien body (oval)
            gc.setFill(color);
            gc.fillOval(-10, -20 + bob, 20, 25);

            // Big eyes
            gc.setFill(Color.BLACK);
            gc.fillOval(-7, -15 + bob, 6, 8);
            gc.fillOval(1, -15 + bob, 6, 8);
            gc.setFill(Color.WHITE);
            gc.fillOval(-5, -13 + bob, 2, 2);
            gc.fillOval(3, -13 + bob, 2, 2);

            // Antennae bobbing
            gc.setStroke(color);
            gc.setLineWidth(2);
            double antennaBob = Math.sin(animFrame * 0.5) * 5;
            gc.strokeLine(-5, -20 + bob, -8 + antennaBob, -30 + bob);
            gc.strokeLine(5, -20 + bob, 8 - antennaBob, -30 + bob);
            gc.setFill(color.brighter());
            gc.fillOval(-10 + antennaBob, -33 + bob, 5, 5);
            gc.fillOval(6 - antennaBob, -33 + bob, 5, 5);

            // Arms dancing
            double armAngle = Math.sin(animFrame * 0.4 + danceStyle) * 45;
            gc.setStroke(color);
            gc.setLineWidth(3);
            gc.save();
            gc.translate(-10, -10 + bob);
            gc.rotate(-45 + armAngle);
            gc.strokeLine(0, 0, -12, 0);
            gc.restore();
            gc.save();
            gc.translate(10, -10 + bob);
            gc.rotate(45 - armAngle);
            gc.strokeLine(0, 0, 12, 0);
            gc.restore();

            // Legs dancing
            double legMove = Math.sin(animFrame * 0.3 + danceStyle * 0.5) * 8;
            gc.strokeLine(-5, 5 + bob, -5 + legMove, 20);
            gc.strokeLine(5, 5 + bob, 5 - legMove, 20);
        }

        private void renderHuman(GraphicsContext gc, int cycle) {
            double bob = Math.sin(animFrame * 0.25) * 2;

            // Head
            gc.setFill(color);
            gc.fillOval(-8, -28 + bob, 16, 16);

            // Hair
            gc.setFill(Color.BROWN);
            gc.fillRect(-8, -28 + bob, 16, 6);

            // Eyes
            gc.setFill(Color.BLACK);
            gc.fillOval(-5, -22 + bob, 3, 3);
            gc.fillOval(2, -22 + bob, 3, 3);

            // Smile
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeArc(-4, -18 + bob, 8, 6, 180, 180, javafx.scene.shape.ArcType.OPEN);

            // Body
            gc.setFill(Color.DARKGREEN); // Military uniform
            gc.fillRect(-7, -12 + bob, 14, 18);

            // Arms dancing
            double armSwing = Math.sin(animFrame * 0.35 + danceStyle) * 40;
            gc.setStroke(color);
            gc.setLineWidth(4);
            gc.save();
            gc.translate(-7, -8 + bob);
            gc.rotate(-30 + armSwing);
            gc.strokeLine(0, 0, -10, 0);
            gc.restore();
            gc.save();
            gc.translate(7, -8 + bob);
            gc.rotate(30 - armSwing);
            gc.strokeLine(0, 0, 10, 0);
            gc.restore();

            // Legs dancing
            double legSwing = Math.sin(animFrame * 0.3 + danceStyle * 0.7) * 10;
            gc.setFill(Color.DARKGREEN);
            gc.save();
            gc.translate(-4, 6 + bob);
            gc.rotate(legSwing);
            gc.fillRect(-2, 0, 4, 14);
            gc.restore();
            gc.save();
            gc.translate(4, 6 + bob);
            gc.rotate(-legSwing);
            gc.fillRect(-2, 0, 4, 14);
            gc.restore();

            // Boots
            gc.setFill(Color.BLACK);
            gc.fillRect(-6 + legSwing/2, 18 + bob, 5, 4);
            gc.fillRect(1 - legSwing/2, 18 + bob, 5, 4);
        }
    }

    // Inner class for victory dancing girls
    private static class DancingGirl {
        static final Color[] DRESS_COLORS = {Color.RED, Color.HOTPINK, Color.CYAN, Color.YELLOW, Color.LIME, Color.ORANGE};
        static final Color[] HAIR_COLORS = {Color.BLACK, Color.BROWN, Color.SADDLEBROWN, Color.GOLD, Color.ORANGERED};

        double x, y;
        int animFrame;
        int danceStyle;
        Color dressColor;
        Color hairColor;
        int dressColorIndex;
        int hairColorIndex;

        DancingGirl(double x, double y, int danceStyle) {
            this.x = x;
            this.y = y;
            this.animFrame = (int)(Math.random() * 60); // Random start frame for variety
            this.danceStyle = danceStyle;
            this.dressColorIndex = (int)(Math.random() * DRESS_COLORS.length);
            this.hairColorIndex = (int)(Math.random() * HAIR_COLORS.length);
            this.dressColor = DRESS_COLORS[dressColorIndex];
            this.hairColor = HAIR_COLORS[hairColorIndex];
        }

        // Constructor for network sync
        DancingGirl(double x, double y, int animFrame, int danceStyle, int dressColorIndex, int hairColorIndex) {
            this.x = x;
            this.y = y;
            this.animFrame = animFrame;
            this.danceStyle = danceStyle;
            this.dressColorIndex = dressColorIndex;
            this.hairColorIndex = hairColorIndex;
            this.dressColor = DRESS_COLORS[dressColorIndex % DRESS_COLORS.length];
            this.hairColor = HAIR_COLORS[hairColorIndex % HAIR_COLORS.length];
        }

        void update() {
            animFrame++;
        }

        void render(GraphicsContext gc) {
            gc.save();
            gc.translate(x, y);

            double bob = Math.sin(animFrame * 0.2 + danceStyle) * 3;
            double sway = Math.sin(animFrame * 0.15 + danceStyle * 0.5) * 5;

            // Hair (long, flowing)
            gc.setFill(hairColor);
            double hairSway = Math.sin(animFrame * 0.1) * 8;
            gc.fillOval(-12 + hairSway * 0.3, -38 + bob, 24, 20);
            // Hair strands flowing down
            gc.fillRect(-10 + hairSway * 0.2, -28 + bob, 6, 25);
            gc.fillRect(4 + hairSway * 0.4, -28 + bob, 6, 25);

            // Face
            gc.setFill(Color.PEACHPUFF);
            gc.fillOval(-8, -36 + bob, 16, 18);

            // Eyes (cute anime style)
            gc.setFill(Color.WHITE);
            gc.fillOval(-6, -32 + bob, 5, 6);
            gc.fillOval(1, -32 + bob, 5, 6);
            gc.setFill(Color.rgb(50, 50, 150)); // Blue eyes
            gc.fillOval(-5, -31 + bob, 3, 4);
            gc.fillOval(2, -31 + bob, 3, 4);
            gc.setFill(Color.WHITE); // Eye shine
            gc.fillOval(-4, -31 + bob, 1, 1);
            gc.fillOval(3, -31 + bob, 1, 1);

            // Blush
            gc.setFill(Color.rgb(255, 180, 180, 0.6));
            gc.fillOval(-8, -27 + bob, 4, 2);
            gc.fillOval(4, -27 + bob, 4, 2);

            // Smile
            gc.setStroke(Color.rgb(200, 100, 100));
            gc.setLineWidth(1);
            gc.strokeArc(-3, -26 + bob, 6, 4, 180, 180, javafx.scene.shape.ArcType.OPEN);

            // Body/Dress (flowing)
            gc.setFill(dressColor);
            // Top of dress
            gc.fillRect(-8 + sway * 0.2, -18 + bob, 16, 12);

            // Skirt (swaying)
            double skirtSway = Math.sin(animFrame * 0.25 + danceStyle) * 8;
            gc.beginPath();
            gc.moveTo(-8 + sway * 0.2, -6 + bob);
            gc.lineTo(8 + sway * 0.2, -6 + bob);
            gc.lineTo(14 + skirtSway, 20 + bob);
            gc.lineTo(-14 - skirtSway, 20 + bob);
            gc.closePath();
            gc.fill();

            // Skirt folds
            gc.setStroke(dressColor.darker());
            gc.setLineWidth(1);
            gc.strokeLine(-4 + sway * 0.1, -6 + bob, -6 - skirtSway * 0.3, 18 + bob);
            gc.strokeLine(4 + sway * 0.1, -6 + bob, 6 + skirtSway * 0.3, 18 + bob);

            // Arms (dancing motion)
            double armAngle = Math.sin(animFrame * 0.3 + danceStyle) * 50;
            gc.setFill(Color.PEACHPUFF);
            gc.save();
            gc.translate(-8 + sway * 0.2, -14 + bob);
            gc.rotate(-60 + armAngle);
            gc.fillRect(0, 0, 4, 16);
            gc.restore();
            gc.save();
            gc.translate(8 + sway * 0.2, -14 + bob);
            gc.rotate(60 - armAngle);
            gc.fillRect(-4, 0, 4, 16);
            gc.restore();

            // Legs (under skirt, slight movement)
            double legMove = Math.sin(animFrame * 0.2 + danceStyle * 0.5) * 3;
            gc.setFill(Color.PEACHPUFF);
            gc.fillRect(-5 + legMove, 18 + bob, 4, 10);
            gc.fillRect(1 - legMove, 18 + bob, 4, 10);

            // Shoes
            gc.setFill(dressColor.darker());
            gc.fillRect(-6 + legMove, 27 + bob, 5, 3);
            gc.fillRect(0 - legMove, 27 + bob, 5, 3);

            gc.restore();
        }
    }

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
                if (gameOver || victory || victoryDancingInitialized || dancingInitialized) {
                    returnToMenu();
                } else if (isNetworkGame) {
                    // Multiplayer: toggle per-player pause with shield
                    int myPlayerIndex = network != null && !network.isHost()
                        ? network.getPlayerNumber() - 1 : 0;
                    if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
                        playerPaused[myPlayerIndex] = !playerPaused[myPlayerIndex];
                        Tank myTank = playerTanks.get(myPlayerIndex);
                        if (playerPaused[myPlayerIndex]) {
                            myTank.setPauseShield(true);
                        } else {
                            myTank.setPauseShield(false);
                        }
                    }
                } else {
                    // Single player: full game pause
                    paused = !paused;
                    pauseMenuSelection = 0;
                    // Stop sounds when paused
                    if (paused && soundManager != null) {
                        soundManager.stopGameplaySounds();
                    }
                }
                return;
            }

            // Pause menu navigation (single player only)
            if (paused && !isNetworkGame) {
                if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                    pauseMenuSelection = (pauseMenuSelection + 1) % 2;
                } else if (event.getCode() == KeyCode.ENTER) {
                    if (pauseMenuSelection == 0) {
                        // Resume
                        paused = false;
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
        // Generate new random level
        gameMap.nextLevel();

        // Reset game state
        victory = false;
        victoryConditionMet = false;
        victoryDelayTimer = 0;
        gameOver = false;
        gameOverSoundPlayed = false;
        dancingInitialized = false;
        dancingCharacters.clear();
        victoryDancingInitialized = false;
        victoryDancingGirls.clear();
        winnerBonusAwarded = false;
        bossKillerPlayerIndex = -1;
        bossKillPowerUpReward = null;

        // Reset kills and level scores for new round (total scores persist)
        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerLevelScores[i] = 0;
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }

        // Reset base
        base = new Base(12 * 32, 24 * 32);

        // Clear bullets, lasers, and power-ups
        bullets.clear();
        lasers.clear();
        powerUps.clear();

        // Reset player tanks (keep power-ups but reset position and give shield)
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            player.setPosition(FIXED_START_POSITIONS[i][0], FIXED_START_POSITIONS[i][1]);
            player.setDirection(Direction.UP);
            player.giveTemporaryShield(); // Brief spawn protection
        }

        // Clear enemy tanks and reset spawner
        enemyTanks.clear();
        enemySpawner = new EnemySpawner(totalEnemies, 5, gameMap);

        // Reset base protection state
        baseProtectionDuration = 0;
        isFlashing = false;
        flashCount = 0;
        flashTimer = 0;

        // Reset freeze states
        enemyFreezeDuration = 0;
        playerFreezeDuration = 0;

        // Reset enemy team speed boost
        enemyTeamSpeedBoostDuration = 0;
        enemyWithPermanentSpeedBoost = null;

        // Hide victory image
        if (victoryImageView != null) {
            victoryImageView.setVisible(false);
        }

        // Reset UFO state for new level
        ufo = null;
        ufoSpawnedThisLevel = false;
        ufoWasKilled = false;
        ufoLostMessageTimer = 0;
        ufoKilledMessageTimer = 0;
        easterEgg = null;
        for (int i = 0; i < playerMachinegunKills.length; i++) {
            playerMachinegunKills[i] = 0;
        }

        // Play intro sound for new level
        soundManager.playIntro();

        System.out.println("Starting Level " + gameMap.getLevelNumber());
    }

    private void restartCurrentLevel() {
        // Regenerate the same level (keep level number) or reload custom level
        gameMap.regenerateOrReloadLevel();

        // Reset game state
        victory = false;
        victoryConditionMet = false;
        victoryDelayTimer = 0;
        gameOver = false;
        gameOverSoundPlayed = false;
        dancingInitialized = false;
        dancingCharacters.clear();
        victoryDancingInitialized = false;
        victoryDancingGirls.clear();
        winnerBonusAwarded = false;
        bossKillerPlayerIndex = -1;
        bossKillPowerUpReward = null;

        // Reset kills and scores for new game after game over
        for (int i = 0; i < playerKills.length; i++) {
            playerKills[i] = 0;
            playerScores[i] = 0;
            playerLevelScores[i] = 0;
            for (int j = 0; j < 6; j++) {
                playerKillsByType[i][j] = 0;
            }
        }

        // Reset base
        base = new Base(12 * 32, 24 * 32);

        // Clear bullets, lasers, and power-ups
        bullets.clear();
        lasers.clear();
        powerUps.clear();

        // Reset player tanks (full reset including lives and power-ups)
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            player.setLives(3);
            player.setLaserDuration(0); // Clear laser power-up on restart
            player.respawn(FIXED_START_POSITIONS[i][0], FIXED_START_POSITIONS[i][1]);
        }

        // Clear enemy tanks and reset spawner
        enemyTanks.clear();
        enemySpawner = new EnemySpawner(totalEnemies, 5, gameMap);

        // Reset base protection state
        baseProtectionDuration = 0;
        isFlashing = false;
        flashCount = 0;
        flashTimer = 0;

        // Reset freeze states
        enemyFreezeDuration = 0;
        playerFreezeDuration = 0;

        // Reset enemy team speed boost
        enemyTeamSpeedBoostDuration = 0;
        enemyWithPermanentSpeedBoost = null;

        // Hide game over image
        if (gameOverImageView != null) {
            gameOverImageView.setVisible(false);
        }

        // Reset UFO state for restart
        ufo = null;
        ufoSpawnedThisLevel = false;
        ufoWasKilled = false;
        ufoLostMessageTimer = 0;
        ufoKilledMessageTimer = 0;
        easterEgg = null;
        for (int i = 0; i < playerMachinegunKills.length; i++) {
            playerMachinegunKills[i] = 0;
        }

        // Play intro sound for retry
        soundManager.playIntro();

        System.out.println("Restarting Level " + gameMap.getLevelNumber());
    }

    public void start() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= FRAME_TIME) {
                    update();
                    render();
                    lastUpdate = now;
                }
            }
        };
        gameLoop.start();
    }

    private double[] getRandomPowerUpSpawnPosition() {
        Random random = new Random();
        int maxAttempts = 100;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Random position within playable area (avoiding borders)
            int col = 2 + random.nextInt(22); // 2 to 23 (avoid border at 0,1 and 24,25)
            int row = 2 + random.nextInt(22);

            double x = col * 32;
            double y = row * 32;

            // Check if position is clear (not in wall, water, or trees)
            GameMap.TileType tile = gameMap.getTile(row, col);
            if (tile == GameMap.TileType.EMPTY) {
                return new double[]{x, y};
            }
        }

        // Fallback to center if no valid position found
        return new double[]{13 * 32, 13 * 32};
    }

    private PowerUp.Type applyRandomPowerUp(Tank player) {
        // Choose a random power-up type (excluding BOMB and FREEZE which affect game state)
        Random random = new Random();
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
        PowerUp.Type type = goodTypes[random.nextInt(goodTypes.length)];

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
        if (!bullet.isFromEnemy()) {
            int playerNum = bullet.getOwnerPlayerNumber();
            if (playerNum >= 1 && playerNum <= playerTanks.size()) {
                playerTanks.get(playerNum - 1).bulletDestroyed();
            }
        }
        // Enemies don't need this optimization - they have AI cooldowns
    }

    private void checkAndSpawnUFO() {
        // UFO spawn conditions:
        // 1. 10 or fewer enemies remaining (but more than 1 - BOSS not yet spawned)
        // 2. At least one player has machinegun and has killed 5+ enemies with it
        int remaining = enemySpawner.getRemainingEnemies();
        if (remaining > 10 || remaining <= 1) {
            return; // Not in the right enemy count range
        }

        // Check if any player qualifies (has machinegun and 5+ machinegun kills)
        boolean qualified = false;
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.getMachinegunCount() > 0 && playerMachinegunKills[i] >= 5) {
                qualified = true;
                break;
            }
        }

        if (!qualified) {
            return;
        }

        // Spawn UFO from random side
        Random random = new Random();
        boolean fromRight = random.nextBoolean();
        double startX = fromRight ? width + 10 : -58;
        double startY = 100 + random.nextInt(height - 300);

        ufo = new UFO(startX, startY, !fromRight); // Moving opposite to start side
        ufoSpawnedThisLevel = true;
        System.out.println("UFO spawned! From " + (fromRight ? "right" : "left") + " side");
    }

    private void update() {
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
                if (myTank.isAlive() && playerFreezeDuration <= 0 && !playerPaused[myPlayerIndex]) {
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
        boolean isPlayerFrozen = playerFreezeDuration > 0;
        inputHandler.handleInput(gameMap, bullets, soundManager, allTanks, base, isPlayerFrozen);

        // Update base protection from SHOVEL power-up
        if (baseProtectionDuration > 0) {
            baseProtectionDuration--;
            if (baseProtectionDuration == 0) {
                // Start flashing when timer expires
                isFlashing = true;
                flashCount = 0;
                flashTimer = 0;
                gameMap.setBaseProtection(GameMap.TileType.STEEL); // Start with steel
            }
        }

        // Handle flashing after protection expires
        if (isFlashing) {
            flashTimer++;
            if (flashTimer >= FLASH_DURATION) {
                flashTimer = 0;
                flashCount++;

                // Toggle between STEEL and BRICK (start with STEEL, then BRICK, etc.)
                if (flashCount % 2 == 0) {
                    gameMap.setBaseProtection(GameMap.TileType.STEEL);
                } else {
                    gameMap.setBaseProtection(GameMap.TileType.BRICK);
                }

                // Stop flashing after 5 complete flashes (10 state changes)
                if (flashCount >= TOTAL_FLASHES) {
                    isFlashing = false;
                    gameMap.setBaseProtection(GameMap.TileType.BRICK); // Final state is brick
                }
            }
        }

        // Update map (burning tiles)
        gameMap.update();

        // Spawn enemies if needed
        int enemyCountBefore = enemyTanks.size();
        enemySpawner.update(enemyTanks);
        // Apply temporary speed boost to newly spawned enemies if boost is active
        if (enemyTeamSpeedBoostDuration > 0 && enemyTanks.size() > enemyCountBefore) {
            for (int i = enemyCountBefore; i < enemyTanks.size(); i++) {
                Tank newEnemy = enemyTanks.get(i);
                if (newEnemy != enemyWithPermanentSpeedBoost) {
                    newEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                }
            }
        }

        // Check UFO spawn conditions (only host spawns UFO)
        if (!ufoSpawnedThisLevel && ufo == null) {
            checkAndSpawnUFO();
        }

        // Update UFO if exists (but not during victory delay - stop shooting)
        if (ufo != null && ufo.isAlive() && !victoryConditionMet) {
            ufo.update(bullets, width, height, soundManager);
            if (!ufo.isAlive()) {
                // UFO escaped (wasn't killed by player)
                if (!ufoWasKilled) {
                    ufoLostMessageTimer = UFO_MESSAGE_DURATION;
                    System.out.println("UFO escaped! Lost it!");
                }
                ufo = null;
            }
        }

        // Update "Lost it!" message timer
        if (ufoLostMessageTimer > 0) {
            ufoLostMessageTimer--;
        }

        // Update "Zed is dead!" message timer
        if (ufoKilledMessageTimer > 0) {
            ufoKilledMessageTimer--;
        }

        // Update player tanks and handle respawn
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive()) {
                player.update(gameMap, bullets, soundManager, allTanks, base);
            } else if (player.getLives() > 0) {
                // Player died but has lives left - respawn at FIXED start position
                soundManager.playExplosion();
                double respawnX = FIXED_START_POSITIONS[i][0];
                double respawnY = FIXED_START_POSITIONS[i][1];
                System.out.println("Player " + (i + 1) + " respawning at FIXED position: " + respawnX + ", " + respawnY);
                player.respawn(respawnX, respawnY);
            }
        }

        // Update freeze durations
        if (enemyFreezeDuration > 0) {
            enemyFreezeDuration--;
        }
        if (playerFreezeDuration > 0) {
            playerFreezeDuration--;
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
                System.out.println("Enemy team speed boost expired - only original enemy keeps the speed");
            }
        }

        // Update enemy tanks with AI (skip if frozen, except BOSS is unfreezable)
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                // BOSS tank is immune to freeze
                if (enemyFreezeDuration <= 0 || tank.getEnemyType() == Tank.EnemyType.BOSS) {
                    tank.updateAI(gameMap, bullets, allTanks, base, soundManager);
                }
            }
        }

        // Push apart overlapping tanks to prevent getting stuck
        pushApartOverlappingTanks(allTanks);

        // Update bullets
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update();

            // Check bullet collisions with map
            if (gameMap.checkBulletCollision(bullet, soundManager)) {
                notifyBulletDestroyed(bullet);
                bulletIterator.remove();
                continue;
            }

            // Check bullet out of bounds and handle wraparound through destroyed borders
            if (bullet.isOutOfBounds(width, height)) {
                // Try to wrap around if border is destroyed, otherwise remove bullet
                if (!bullet.handleWraparound(gameMap, width, height)) {
                    notifyBulletDestroyed(bullet);
                    bulletIterator.remove();
                    continue;
                }
            }

            // Check bullet collision with tanks
            boolean bulletRemoved = false;

            // Check collision with UFO (from player bullets)
            if (!bullet.isFromEnemy() && ufo != null && ufo.isAlive()) {
                if (ufo.collidesWith(bullet)) {
                    boolean destroyed = ufo.damage();
                    if (destroyed) {
                        soundManager.playExplosion();
                        // Award 20 points to the player who killed UFO
                        int killerPlayer = bullet.getOwnerPlayerNumber();
                        if (killerPlayer >= 1 && killerPlayer <= 4) {
                            addScore(killerPlayer - 1, 20);
                            System.out.println("UFO destroyed by Player " + killerPlayer + " - awarded 20 points!");
                        }
                        // Spawn easter egg at random position
                        double[] eggPos = getRandomPowerUpSpawnPosition();
                        easterEgg = new EasterEgg(eggPos[0], eggPos[1]);
                        System.out.println("Easter egg spawned at " + eggPos[0] + ", " + eggPos[1]);
                        ufoWasKilled = true;
                        ufoKilledMessageTimer = UFO_MESSAGE_DURATION;
                        System.out.println("Zed is dead!");
                        ufo = null;
                    }
                    notifyBulletDestroyed(bullet);
                    bulletIterator.remove();
                    continue;
                }
            }

            // Check collision with enemy tanks (from player bullets)
            if (!bullet.isFromEnemy()) {
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && bullet.collidesWith(enemy)) {
                        boolean dropPowerUp = enemy.damage();

                        // Handle power-up drops (POWER type drops on each hit, others on death with 30% chance)
                        if (dropPowerUp || (!enemy.isAlive() && Math.random() < 0.3)) {
                            double[] spawnPos = getRandomPowerUpSpawnPosition();
                            powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                        }

                        if (!enemy.isAlive()) {
                            soundManager.playExplosion();
                            // Track kill and score for the player who fired the bullet
                            int killerPlayer = bullet.getOwnerPlayerNumber();
                            if (killerPlayer >= 1 && killerPlayer <= 4) {
                                playerKills[killerPlayer - 1]++;

                                // Track kill by enemy type
                                int enemyTypeOrdinal = enemy.getEnemyType().ordinal();
                                if (enemyTypeOrdinal < 6) {
                                    playerKillsByType[killerPlayer - 1][enemyTypeOrdinal]++;
                                }

                                // Track machinegun kills for UFO spawn condition
                                Tank killer = playerTanks.get(killerPlayer - 1);
                                if (killer.getMachinegunCount() > 0) {
                                    playerMachinegunKills[killerPlayer - 1]++;
                                }

                                // Award points based on enemy type
                                int points = switch (enemy.getEnemyType()) {
                                    case POWER -> 2;  // Rainbow tank
                                    case HEAVY -> 5;  // Black tank
                                    case BOSS -> 10;  // Boss tank
                                    default -> 1;     // Regular, Armored, Fast
                                };
                                addScore(killerPlayer - 1, points);

                                // BOSS kill rewards the player with a random power-up
                                if (enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                                    System.out.println("BOSS killed by Player " + killerPlayer + " - awarding power-up!");
                                    bossKillerPlayerIndex = killerPlayer - 1;
                                    bossKillPowerUpReward = applyRandomPowerUp(killer);
                                }
                            }
                        }
                        notifyBulletDestroyed(bullet);
                        bulletIterator.remove();
                        bulletRemoved = true;
                        break;
                    }
                }
            } else {
                // Enemy bullet - check collision with players
                for (Tank player : playerTanks) {
                    if (player.isAlive() && bullet.collidesWith(player)) {
                        if (!player.hasShield() && !player.hasPauseShield()) {
                            boolean damaged = player.damage();
                            if (bullet.getSize() > 8) {
                                System.out.println("BOSS bullet hit player! Damaged: " + damaged + ", Player alive: " + player.isAlive());
                            }
                            if (!player.isAlive()) {
                                soundManager.playPlayerDeath();
                                String victimName = getPlayerNameForTank(player);
                                String killerType = bullet.getSize() > 8 ? "BOSS (bullet)" : "Enemy tank (bullet)";
                                System.out.println("KILL LOG: " + victimName + " was killed by " + killerType);
                                // Spawn power-up when player dies if more than 2 players
                                if (playerTanks.size() > 2) {
                                    double[] spawnPos = getRandomPowerUpSpawnPosition();
                                    powerUps.add(new PowerUp(spawnPos[0], spawnPos[1]));
                                    System.out.println("Power-up spawned for killed player (3+ players mode)");
                                }
                            }
                        } else if (bullet.getSize() > 8) {
                            System.out.println("BOSS bullet hit player but player has shield!");
                        }
                        notifyBulletDestroyed(bullet);
                        bulletIterator.remove();
                        bulletRemoved = true;
                        break;
                    }
                }
            }

            // Check collision with base (all bullets can hit base)
            if (!bulletRemoved && bullet.collidesWith(base)) {
                base.destroy();
                soundManager.playBaseDestroyed();
                notifyBulletDestroyed(bullet);
                bulletIterator.remove();
                bulletRemoved = true;
                gameOver = true;
            }
        }

        // Check bullet-to-bullet collisions (bullets annihilate each other)
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet1 = bullets.get(i);
            for (int j = i + 1; j < bullets.size(); j++) {
                Bullet bullet2 = bullets.get(j);
                if (bullet1.collidesWith(bullet2)) {
                    notifyBulletDestroyed(bullet1);
                    notifyBulletDestroyed(bullet2);
                    bullets.remove(j);
                    bullets.remove(i);
                    i--; // Adjust index after removal
                    break;
                }
            }
        }

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

        // Update power-ups
        Iterator<PowerUp> powerUpIterator = powerUps.iterator();
        while (powerUpIterator.hasNext()) {
            PowerUp powerUp = powerUpIterator.next();
            powerUp.update();

            // Check if collected by players
            boolean collected = false;
            for (Tank player : playerTanks) {
                if (player.isAlive() && powerUp.collidesWith(player)) {
                    // Handle special power-ups that affect game state, not just tank
                    if (powerUp.getType() == PowerUp.Type.SHOVEL) {
                        gameMap.setBaseProtection(GameMap.TileType.STEEL);
                        baseProtectionDuration = BASE_PROTECTION_TIME; // Reset timer to 1 minute
                        isFlashing = false; // Stop flashing if it was flashing
                        flashCount = 0;
                        flashTimer = 0;
                    } else if (powerUp.getType() == PowerUp.Type.FREEZE) {
                        // Player takes FREEZE - freeze all enemies for 10 seconds
                        enemyFreezeDuration = FREEZE_TIME;
                        System.out.println("FREEZE: Enemies frozen for 10 seconds!");
                    } else if (powerUp.getType() == PowerUp.Type.BOMB) {
                        // Player takes BOMB - explode all enemies on screen
                        for (Tank enemy : enemyTanks) {
                            if (enemy.isAlive()) {
                                // Force kill the enemy
                                while (enemy.isAlive()) {
                                    enemy.damage();
                                }
                                soundManager.playExplosion();
                            }
                        }
                        System.out.println("BOMB: All enemies destroyed!");
                    } else {
                        powerUp.applyEffect(player);
                    }
                    powerUpIterator.remove();
                    collected = true;
                    break;
                }
            }

            // Check if collected by enemies (if not already collected by players)
            if (!collected) {
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && powerUp.collidesWith(enemy)) {
                        // Handle special power-ups that affect game state
                        if (powerUp.getType() == PowerUp.Type.SHOVEL) {
                            // Enemy takes SHOVEL - remove base protection (make it "naked")
                            gameMap.setBaseProtection(GameMap.TileType.EMPTY);
                            baseProtectionDuration = 0; // Stop timer
                            isFlashing = false; // Stop flashing
                            flashCount = 0;
                            flashTimer = 0;
                        } else if (powerUp.getType() == PowerUp.Type.FREEZE) {
                            // Enemy takes FREEZE - freeze all players for 10 seconds (but they can still shoot)
                            playerFreezeDuration = FREEZE_TIME;
                            System.out.println("FREEZE: Players frozen for 10 seconds! (can still shoot)");
                        } else if (powerUp.getType() == PowerUp.Type.BOMB) {
                            // Enemy takes BOMB - explode all players on screen (bypasses shields!)
                            System.out.println("BOMB collected by enemy - damaging all players!");
                            for (Tank player : playerTanks) {
                                if (player.isAlive()) {
                                    // BOMB bypasses all shields - remove shields first then damage
                                    player.setShield(false);
                                    player.setPauseShield(false);
                                    player.damage();
                                    if (!player.isAlive()) {
                                        soundManager.playPlayerDeath();
                                        String victimName = getPlayerNameForTank(player);
                                        System.out.println("KILL LOG: " + victimName + " was killed by BOMB (collected by enemy)");
                                    } else {
                                        soundManager.playExplosion();
                                    }
                                }
                            }
                        } else if (powerUp.getType() == PowerUp.Type.CAR) {
                            // Enemy takes CAR - all enemies get temporary speed boost for 30 seconds
                            // The enemy who picked it up keeps permanent boost
                            powerUp.applyEffect(enemy); // Give permanent boost to this enemy
                            enemyWithPermanentSpeedBoost = enemy;
                            enemyTeamSpeedBoostDuration = ENEMY_SPEED_BOOST_TIME;
                            // Give temporary boost to all other enemies
                            for (Tank otherEnemy : enemyTanks) {
                                if (otherEnemy != enemy && otherEnemy.isAlive()) {
                                    otherEnemy.applyTempSpeedBoost(ENEMY_TEAM_SPEED_BOOST);
                                }
                            }
                            System.out.println("CAR: All enemies get speed boost for 30 seconds!");
                        } else {
                            powerUp.applyEffect(enemy);
                        }
                        powerUpIterator.remove();
                        collected = true;
                        break;
                    }
                }
            }

            // Remove expired power-ups (only if not already collected)
            if (!collected && powerUp.isExpired()) {
                powerUpIterator.remove();
            }
        }

        // Update easter egg (players and enemies can collect it)
        if (easterEgg != null) {
            easterEgg.update();

            boolean eggCollected = false;

            // Check if collected by any player
            for (int i = 0; i < playerTanks.size() && !eggCollected; i++) {
                Tank player = playerTanks.get(i);
                if (player.isAlive() && easterEgg.collidesWith(player)) {
                    // Give collecting player 3 extra lives
                    for (int j = 0; j < 3; j++) {
                        player.addLife();
                    }
                    System.out.println("Easter egg collected by Player " + (i + 1) + "! +3 lives!");

                    // Turn all enemies (except BOSS) into rainbow/POWER tanks
                    for (Tank enemy : enemyTanks) {
                        if (enemy.isAlive() && enemy.getEnemyType() != Tank.EnemyType.BOSS) {
                            enemy.setEnemyType(Tank.EnemyType.POWER);
                            System.out.println("Enemy turned into rainbow tank!");
                        }
                    }

                    easterEgg.collect();
                    easterEgg = null;
                    eggCollected = true;
                }
            }

            // Check if collected by any enemy
            for (Tank enemy : enemyTanks) {
                if (!eggCollected && easterEgg != null && enemy.isAlive() && easterEgg.collidesWith(enemy)) {
                    System.out.println("Easter egg collected by enemy! All enemies become HEAVY tanks!");

                    // Turn all enemies (except BOSS) into HEAVY (black) tanks
                    for (Tank e : enemyTanks) {
                        if (e.isAlive() && e.getEnemyType() != Tank.EnemyType.BOSS) {
                            e.setEnemyType(Tank.EnemyType.HEAVY);
                            System.out.println("Enemy turned into HEAVY (black) tank!");
                        }
                    }

                    easterEgg.collect();
                    easterEgg = null;
                    eggCollected = true;
                    break;
                }
            }

            // Remove if expired
            if (easterEgg != null && easterEgg.isExpired()) {
                System.out.println("Easter egg expired!");
                easterEgg = null;
            }
        }

        // Remove dead enemy tanks
        enemyTanks.removeIf(tank -> !tank.isAlive());

        // Check victory condition with 10 second delay
        if (enemySpawner.allEnemiesSpawned() && enemyTanks.isEmpty()) {
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

        // Check game over condition (all players dead with no lives OR base destroyed)
        boolean allPlayersDead = playerTanks.stream().allMatch(p -> !p.isAlive() && p.getLives() <= 0);
        if (allPlayersDead || !base.isAlive()) {
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
                if (playerFreezeDuration > 0) {
                    gc.setFill(Color.rgb(150, 200, 255, 0.5)); // Semi-transparent ice blue
                    gc.fillRect(tank.getX(), tank.getY(), tank.getSize(), tank.getSize());
                    // Draw snowflake/ice crystals
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1);
                    double cx = tank.getX() + tank.getSize() / 2;
                    double cy = tank.getY() + tank.getSize() / 2;
                    for (int i = 0; i < 6; i++) {
                        double angle = (Math.PI * i) / 3;
                        gc.strokeLine(cx, cy, cx + 10 * Math.cos(angle), cy + 10 * Math.sin(angle));
                    }
                }
            }
        }

        // Render enemy tanks
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
                // Draw ice effect if enemies are frozen (except BOSS which is immune)
                if (enemyFreezeDuration > 0 && tank.getEnemyType() != Tank.EnemyType.BOSS) {
                    gc.setFill(Color.rgb(150, 200, 255, 0.5)); // Semi-transparent ice blue
                    gc.fillRect(tank.getX(), tank.getY(), tank.getSize(), tank.getSize());
                    // Draw snowflake/ice crystals
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1);
                    double cx = tank.getX() + tank.getSize() / 2;
                    double cy = tank.getY() + tank.getSize() / 2;
                    for (int i = 0; i < 6; i++) {
                        double angle = (Math.PI * i) / 3;
                        gc.strokeLine(cx, cy, cx + 10 * Math.cos(angle), cy + 10 * Math.sin(angle));
                    }
                }
            }
        }

        // Render UFO (above tanks, below trees)
        if (ufo != null && ufo.isAlive()) {
            ufo.render(gc);
        }

        // Render "Lost it!" message when UFO escapes
        if (ufoLostMessageTimer > 0) {
            renderUfoLostMessage();
        }

        // Render "Zed is dead!" message when UFO is killed
        if (ufoKilledMessageTimer > 0) {
            renderUfoKilledMessage();
        }

        // Render trees ON TOP of tanks to make tanks partially visible in forest
        gameMap.renderTrees(gc);

        // Render burning trees with fire animation (on top of everything)
        gameMap.renderBurningTiles(gc);

        // Render UI
        renderUI();
    }

    private void renderUfoLostMessage() {
        // Save current graphics state
        gc.save();

        // Calculate fade effect (fade out in last second)
        double alpha = 1.0;
        if (ufoLostMessageTimer < 60) { // Last second
            alpha = ufoLostMessageTimer / 60.0;
        }

        // Pulsing effect
        double pulse = 1.0 + Math.sin(System.currentTimeMillis() / 100.0) * 0.1;
        int fontSize = (int)(50 * pulse);

        // Draw "Lost it!" message in the center of the screen
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, fontSize));

        // Shadow/outline effect
        gc.setFill(Color.rgb(0, 0, 0, alpha * 0.7));
        gc.fillText("Lost it!", width / 2 - 85 + 3, height / 3 + 3);

        // Main text with red color
        gc.setFill(Color.rgb(255, 50, 50, alpha));
        gc.fillText("Lost it!", width / 2 - 85, height / 3);

        // UFO icon above the text (simple representation)
        double iconX = width / 2;
        double iconY = height / 3 - 60;
        gc.setFill(Color.rgb(120, 120, 140, alpha));
        gc.fillOval(iconX - 25, iconY, 50, 20);
        gc.setFill(Color.rgb(150, 200, 255, alpha * 0.7));
        gc.fillOval(iconX - 12, iconY - 15, 24, 20);

        // Restore graphics state
        gc.restore();
    }

    private void renderUfoKilledMessage() {
        // Save current font to restore later
        javafx.scene.text.Font savedFont = gc.getFont();

        // Calculate fade effect (fade out in last second)
        double alpha = 1.0;
        if (ufoKilledMessageTimer < 60) { // Last second
            alpha = ufoKilledMessageTimer / 60.0;
        }

        // Fixed font size (no pulsing to avoid layout jumps)
        int fontSize = 50;

        // Draw "Zed is dead!" message in the center of the screen
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, fontSize));

        // Shadow/outline effect
        gc.setFill(Color.rgb(0, 0, 0, alpha * 0.7));
        gc.fillText("Zed is dead!", width / 2 - 130 + 3, height / 3 + 3);

        // Main text with green color (victory!)
        gc.setFill(Color.rgb(50, 255, 50, alpha));
        gc.fillText("Zed is dead!", width / 2 - 130, height / 3);

        // Explosion effect around text (pulsing particles only)
        double centerX = width / 2;
        double centerY = height / 3 - 40;
        gc.setFill(Color.rgb(255, 200, 50, alpha * 0.6));
        for (int i = 0; i < 8; i++) {
            double angle = (System.currentTimeMillis() / 50.0 + i * Math.PI / 4) % (2 * Math.PI);
            double dist = 50 + Math.sin(System.currentTimeMillis() / 100.0 + i) * 10;
            double starX = centerX + Math.cos(angle) * dist;
            double starY = centerY + Math.sin(angle) * dist * 0.5;
            gc.fillOval(starX - 5, starY - 5, 10, 10);
        }

        // Restore original font
        gc.setFont(savedFont);
    }

    private void renderBossHealthBar() {
        // Find BOSS tank if alive
        Tank boss = null;
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                boss = enemy;
                break;
            }
        }

        if (boss == null) return;

        // Draw BOSS health bar at the top center of the screen
        double barWidth = 300;
        double barHeight = 20;
        double barX = (width - barWidth) / 2;
        double barY = 10;

        // Background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX - 2, barY - 2, barWidth + 4, barHeight + 4);

        // Health bar background (red)
        gc.setFill(Color.DARKRED);
        gc.fillRect(barX, barY, barWidth, barHeight);

        // Current health (pulsing red like BOSS tank)
        double healthPercent = (double) boss.getHealth() / boss.getMaxHealth();
        double healthWidth = barWidth * healthPercent;

        // Pulsing red color matching BOSS tank
        double pulse = (Math.sin(System.currentTimeMillis() / 150.0) + 1) / 2; // 0 to 1
        int red = (int) (150 + pulse * 105); // 150 to 255
        int green = (int) (pulse * 50); // 0 to 50
        gc.setFill(Color.rgb(red, green, 0));
        gc.fillRect(barX, barY, healthWidth, barHeight);

        // Border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);

        // BOSS label
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        gc.fillText("BOSS", barX - 50, barY + 15);

        // Health text
        gc.fillText(boss.getHealth() + "/" + boss.getMaxHealth(), barX + barWidth + 10, barY + 15);
    }

    private void renderLaughingSkull(GraphicsContext gc, double centerX, double centerY) {
        double scale = 3.0; // Big skull for game over
        double time = System.currentTimeMillis() / 100.0;

        // Laughing animation - skull bobs up and down
        double bobY = Math.sin(time * 0.5) * 5;
        double cy = centerY + bobY;

        // Jaw opening animation for laughing
        double jawOpen = (Math.sin(time * 2) + 1) * 8 * scale;

        // Skull background glow (pulsing red)
        double pulse = (Math.sin(time * 0.3) + 1) / 2;
        gc.setFill(Color.rgb((int)(100 + pulse * 100), 0, 0, 0.3));
        gc.fillOval(centerX - 70 * scale, cy - 60 * scale, 140 * scale, 130 * scale);

        // Main skull (cream/bone color)
        gc.setFill(Color.rgb(255, 250, 240));
        gc.fillOval(centerX - 50 * scale, cy - 45 * scale, 100 * scale, 90 * scale);

        // Eye sockets (black with red glow inside)
        gc.setFill(Color.BLACK);
        gc.fillOval(centerX - 30 * scale, cy - 20 * scale, 25 * scale, 30 * scale);
        gc.fillOval(centerX + 5 * scale, cy - 20 * scale, 25 * scale, 30 * scale);

        // Evil red eyes (pulsing)
        gc.setFill(Color.rgb(255, (int)(50 * pulse), 0));
        gc.fillOval(centerX - 25 * scale, cy - 12 * scale, 15 * scale, 15 * scale);
        gc.fillOval(centerX + 10 * scale, cy - 12 * scale, 15 * scale, 15 * scale);

        // Eye highlights
        gc.setFill(Color.YELLOW);
        gc.fillOval(centerX - 22 * scale, cy - 10 * scale, 5 * scale, 5 * scale);
        gc.fillOval(centerX + 13 * scale, cy - 10 * scale, 5 * scale, 5 * scale);

        // Nose hole (triangle)
        gc.setFill(Color.BLACK);
        gc.fillPolygon(
            new double[]{centerX - 8 * scale, centerX + 8 * scale, centerX},
            new double[]{cy + 15 * scale, cy + 15 * scale, cy + 30 * scale},
            3
        );

        // Upper teeth row
        gc.setFill(Color.rgb(255, 250, 240));
        gc.fillRect(centerX - 30 * scale, cy + 32 * scale, 60 * scale, 12 * scale);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        for (int i = 0; i < 6; i++) {
            double toothX = centerX - 25 * scale + i * 10 * scale;
            gc.strokeLine(toothX, cy + 32 * scale, toothX, cy + 44 * scale);
        }

        // Lower jaw (moves for laughing)
        gc.setFill(Color.rgb(240, 235, 225));
        gc.fillRect(centerX - 28 * scale, cy + 32 * scale + jawOpen, 56 * scale, 10 * scale);
        for (int i = 0; i < 5; i++) {
            double toothX = centerX - 23 * scale + i * 10 * scale;
            gc.strokeLine(toothX, cy + 32 * scale + jawOpen, toothX, cy + 42 * scale + jawOpen);
        }

        // Skull outline
        gc.setStroke(Color.rgb(200, 180, 160));
        gc.setLineWidth(2);
        gc.strokeOval(centerX - 50 * scale, cy - 45 * scale, 100 * scale, 90 * scale);

        // Crossbones behind
        gc.setStroke(Color.rgb(255, 250, 240));
        gc.setLineWidth(8 * scale);
        gc.strokeLine(centerX - 80 * scale, cy - 50 * scale, centerX + 80 * scale, cy + 60 * scale);
        gc.strokeLine(centerX + 80 * scale, cy - 50 * scale, centerX - 80 * scale, cy + 60 * scale);

        // Bone ends
        gc.setFill(Color.rgb(255, 250, 240));
        double boneEndSize = 12 * scale;
        gc.fillOval(centerX - 85 * scale, cy - 55 * scale, boneEndSize, boneEndSize);
        gc.fillOval(centerX + 75 * scale, cy - 55 * scale, boneEndSize, boneEndSize);
        gc.fillOval(centerX - 85 * scale, cy + 55 * scale, boneEndSize, boneEndSize);
        gc.fillOval(centerX + 75 * scale, cy + 55 * scale, boneEndSize, boneEndSize);

        // "HA HA HA" text floating around
        gc.setFill(Color.RED);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        double textWave = Math.sin(time * 0.8) * 10;
        gc.fillText("HA", centerX - 100, cy - 30 + textWave);
        gc.fillText("HA", centerX + 80, cy - 20 - textWave);
        gc.fillText("HA", centerX - 90, cy + 70 - textWave);
        gc.fillText("HA", centerX + 70, cy + 80 + textWave);
    }

    private void renderPowerUpIcon(double x, double y, PowerUp.Type type) {
        int size = 16;

        // Background with border
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, size, size);
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, size, size);

        // Draw icon based on type (scaled down versions of PowerUp icons)
        switch (type) {
            case GUN:
                // Steel wall with crack and bullet
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x + 1, y + 2, 6, 12); // Wall
                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineWidth(0.5);
                gc.strokeRect(x + 2, y + 3, 4, 4);
                gc.strokeRect(x + 2, y + 9, 4, 4);
                // Crack
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(1);
                gc.strokeLine(x + 7, y + 6, x + 9, y + 4);
                gc.strokeLine(x + 7, y + 6, x + 9, y + 8);
                gc.strokeLine(x + 7, y + 10, x + 9, y + 9);
                gc.strokeLine(x + 7, y + 10, x + 9, y + 12);
                // Bullet
                gc.setFill(Color.RED);
                gc.fillOval(x + 10, y + 6, 5, 4);
                break;
            case STAR:
                // Orange star with red border
                double cx = x + size / 2;
                double cy = y + size / 2;
                double outerR = 6;
                double innerR = 2.5;
                double[] starX = new double[10];
                double[] starY = new double[10];
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI / 2 + (Math.PI * i / 5);
                    double r = (i % 2 == 0) ? outerR : innerR;
                    starX[i] = cx + r * Math.cos(angle);
                    starY[i] = cy - r * Math.sin(angle);
                }
                // Red border
                gc.setFill(Color.DARKRED);
                double[] starXB = new double[10];
                double[] starYB = new double[10];
                for (int i = 0; i < 10; i++) {
                    double angle = Math.PI / 2 + (Math.PI * i / 5);
                    double r = (i % 2 == 0) ? outerR + 1 : innerR + 0.5;
                    starXB[i] = cx + r * Math.cos(angle);
                    starYB[i] = cy - r * Math.sin(angle);
                }
                gc.fillPolygon(starXB, starYB, 10);
                // Orange star
                gc.setFill(Color.ORANGE);
                gc.fillPolygon(starX, starY, 10);
                break;
            case CAR:
                // Lightning bolt - speed symbol
                gc.setFill(Color.YELLOW);
                gc.fillPolygon(
                    new double[]{x + 9, x + 5, x + 7, x + 4, x + 10, x + 8, x + 12},
                    new double[]{y + 1, y + 7, y + 7, y + 15, y + 8, y + 8, y + 1},
                    7
                );
                // Green border
                gc.setStroke(Color.LIME);
                gc.setLineWidth(1);
                gc.strokePolygon(
                    new double[]{x + 9, x + 5, x + 7, x + 4, x + 10, x + 8, x + 12},
                    new double[]{y + 1, y + 7, y + 7, y + 15, y + 8, y + 8, y + 1},
                    7
                );
                break;
            case SHIP:
                // Boat on water
                gc.setFill(Color.BLUE);
                gc.fillRect(x + 1, y + 11, 14, 3); // Water
                gc.setFill(Color.CYAN);
                gc.fillPolygon(
                    new double[]{x + 3, x + 13, x + 12, x + 4},
                    new double[]{y + 9, y + 9, y + 12, y + 12},
                    4
                ); // Hull
                gc.fillPolygon(
                    new double[]{x + 8, x + 8, x + 12},
                    new double[]{y + 3, y + 9, y + 9},
                    3
                ); // Sail
                break;
            case SAW:
                // Circular saw
                gc.setFill(Color.BROWN);
                gc.fillOval(x + 3, y + 3, 10, 10);
                gc.setFill(Color.WHITE);
                gc.fillOval(x + 6, y + 6, 4, 4);
                break;
            case SHIELD:
                // Shield shape
                gc.setFill(Color.BLUE);
                gc.fillPolygon(
                    new double[]{x + 8, x + 3, x + 3, x + 8, x + 13, x + 13},
                    new double[]{y + 14, y + 10, y + 3, y + 2, y + 3, y + 10},
                    6
                );
                gc.setFill(Color.LIGHTBLUE);
                gc.fillPolygon(
                    new double[]{x + 8, x + 5, x + 5, x + 8, x + 11, x + 11},
                    new double[]{y + 12, y + 9, y + 5, y + 4, y + 5, y + 9},
                    6
                );
                break;
            case SHOVEL:
                // Shovel
                gc.setFill(Color.ORANGE);
                gc.fillRect(x + 7, y + 2, 2, 7); // Handle
                gc.fillPolygon(
                    new double[]{x + 4, x + 12, x + 11, x + 5},
                    new double[]{y + 9, y + 9, y + 14, y + 14},
                    4
                ); // Blade
                break;
            case TANK:
                // Tank with +1
                gc.setFill(Color.GREEN);
                gc.fillRect(x + 2, y + 8, 7, 5); // Body
                gc.fillRect(x + 4, y + 5, 3, 4); // Turret
                gc.setFill(Color.DARKGREEN);
                gc.fillRect(x + 11, y + 4, 1, 5); // +
                gc.fillRect(x + 9, y + 6, 5, 1);
                gc.fillRect(x + 11, y + 10, 1, 4); // 1
                break;
            case MACHINEGUN:
                // Rapid fire
                gc.setFill(Color.PURPLE);
                gc.fillRect(x + 2, y + 7, 6, 3); // Gun
                gc.setFill(Color.YELLOW);
                gc.fillOval(x + 9, y + 7, 2, 2);
                gc.fillOval(x + 11, y + 5, 2, 2);
                gc.fillOval(x + 11, y + 9, 2, 2);
                gc.fillOval(x + 13, y + 7, 2, 2);
                break;
        }
    }

    private Color getPowerUpColor(PowerUp.Type type) {
        return switch (type) {
            case GUN -> Color.RED;
            case STAR -> Color.YELLOW;
            case CAR -> Color.LIME;
            case SHIP -> Color.CYAN;
            case SHOVEL -> Color.ORANGE;
            case SAW -> Color.BROWN;
            case TANK -> Color.GREEN;
            case SHIELD -> Color.BLUE;
            case MACHINEGUN -> Color.PURPLE;
            case FREEZE -> Color.LIGHTBLUE;
            case BOMB -> Color.BLACK;
            case LASER -> Color.RED;
        };
    }

    private void initializeDancingCharacters() {
        if (dancingInitialized) return;
        dancingInitialized = true;

        // Raise the skull flag on the destroyed base
        base.raiseFlag();

        Random random = new Random();

        // Create dancing aliens/humans from enemy tank positions
        // If there are enemy tanks, use their positions; otherwise spawn around the destroyed base
        if (!enemyTanks.isEmpty()) {
            for (Tank enemy : enemyTanks) {
                // Each enemy tank spawns 1-2 characters
                int numCharacters = 1 + random.nextInt(2);
                for (int i = 0; i < numCharacters; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 40;
                    double offsetY = (random.nextDouble() - 0.5) * 40;
                    boolean isAlien = random.nextBoolean();
                    int danceStyle = random.nextInt(3);
                    dancingCharacters.add(new DancingCharacter(
                        enemy.getX() + 16 + offsetX,
                        enemy.getY() + 16 + offsetY,
                        isAlien,
                        danceStyle
                    ));
                }
            }
        }

        // Also spawn some around the destroyed base
        double baseX = base.getX() + 32;
        double baseY = base.getY() + 32;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 * i) / 6;
            double radius = 60 + random.nextDouble() * 30;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + Math.sin(angle) * radius;
            boolean isAlien = random.nextBoolean();
            int danceStyle = random.nextInt(3);
            dancingCharacters.add(new DancingCharacter(x, y, isAlien, danceStyle));
        }
    }

    private void initializeVictoryCelebration() {
        if (victoryDancingInitialized) return;
        victoryDancingInitialized = true;

        // Raise the Soviet victory flag on the base
        base.raiseVictoryFlag();

        Random random = new Random();

        // Get number of active players - use playerTanks.size() directly
        int activePlayers = playerTanks.size();

        // Spawn dancing girls based on player count (1-2 girls per player)
        int girlCount = activePlayers + random.nextInt(activePlayers + 1); // Players to 2x players

        // Position girls around the base
        double baseX = base.getX() + 16;
        double baseY = base.getY() - 20; // Above the base

        for (int i = 0; i < girlCount; i++) {
            // Spread girls in a semi-circle above the base
            double angle = Math.PI + (Math.PI * (i + 0.5) / girlCount); // Semi-circle above
            double radius = 80 + random.nextDouble() * 40;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + Math.sin(angle) * radius * 0.6; // Flatten the vertical spread
            int danceStyle = random.nextInt(4);

            victoryDancingGirls.add(new DancingGirl(x, y, danceStyle));
        }

        System.out.println("Victory celebration initialized with " + girlCount + " dancing girls for " + activePlayers + " players");
    }

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

    // Draw a mini tank icon for statistics table headers (looks like actual game tanks)
    private void drawMiniTank(GraphicsContext gc, double x, double y, Tank.EnemyType type) {
        double s = 18; // Mini tank size

        // Get tank color based on type
        Color bodyColor, turretColor, trackColor;
        switch (type) {
            case REGULAR -> { bodyColor = Color.DARKRED; turretColor = Color.RED; trackColor = Color.rgb(80, 40, 40); }
            case ARMORED -> { bodyColor = Color.rgb(100, 30, 30); turretColor = Color.rgb(140, 50, 50); trackColor = Color.rgb(60, 30, 30); }
            case FAST -> { bodyColor = Color.rgb(200, 80, 80); turretColor = Color.rgb(255, 120, 120); trackColor = Color.rgb(150, 60, 60); }
            case POWER -> { bodyColor = Color.PURPLE; turretColor = Color.MAGENTA; trackColor = Color.DARKMAGENTA; }
            case HEAVY -> { bodyColor = Color.rgb(50, 50, 50); turretColor = Color.DARKGRAY; trackColor = Color.BLACK; }
            case BOSS -> { bodyColor = Color.rgb(150, 30, 30); turretColor = Color.rgb(220, 50, 50); trackColor = Color.rgb(100, 20, 20); }
            default -> { bodyColor = Color.DARKRED; turretColor = Color.RED; trackColor = Color.rgb(80, 40, 40); }
        }

        // Draw tracks (left and right) with track pattern
        gc.setFill(trackColor);
        gc.fillRoundRect(x, y + 2, 4, s - 4, 2, 2);
        gc.fillRoundRect(x + s - 4, y + 2, 4, s - 4, 2, 2);

        // Track details (horizontal lines)
        gc.setStroke(Color.rgb(40, 40, 40));
        gc.setLineWidth(1);
        for (int i = 0; i < 3; i++) {
            double ty = y + 4 + i * 5;
            gc.strokeLine(x, ty, x + 4, ty);
            gc.strokeLine(x + s - 4, ty, x + s, ty);
        }

        // Draw tank body (between tracks)
        gc.setFill(bodyColor);
        gc.fillRect(x + 3, y + 3, s - 6, s - 6);

        // Draw turret (circular, centered)
        gc.setFill(turretColor);
        double turretSize = s * 0.5;
        gc.fillOval(x + s/2 - turretSize/2, y + s/2 - turretSize/2 + 1, turretSize, turretSize);

        // Draw barrel (pointing up)
        gc.setFill(turretColor);
        gc.fillRoundRect(x + s/2 - 2, y - 2, 4, s/2 + 2, 1, 1);

        // Barrel tip (darker)
        gc.setFill(bodyColor);
        gc.fillRect(x + s/2 - 2, y - 2, 4, 2);

        // Type-specific decorations
        switch (type) {
            case ARMORED -> {
                // Extra armor plate on turret
                gc.setFill(Color.GRAY);
                gc.fillRect(x + 5, y + s/2 - 1, s - 10, 3);
            }
            case HEAVY -> {
                // White dot indicator
                gc.setFill(Color.WHITE);
                gc.fillOval(x + s/2 - 2, y + s/2 - 1, 4, 4);
            }
            case FAST -> {
                // Speed lines behind
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1);
                gc.strokeLine(x + 2, y + s - 2, x + s - 2, y + s - 2);
            }
            case BOSS -> {
                // Skull mark (X)
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                double cx = x + s/2, cy = y + s/2;
                gc.strokeLine(cx - 3, cy - 2, cx + 3, cy + 4);
                gc.strokeLine(cx + 3, cy - 2, cx - 3, cy + 4);
            }
            case POWER -> {
                // Star sparkle
                gc.setFill(Color.YELLOW);
                gc.fillOval(x + s/2 - 1.5, y + s/2 - 0.5, 3, 3);
            }
            default -> {}
        }
    }

    private void renderUI() {
        gc.setFill(Color.WHITE);
        gc.fillText("Level: " + gameMap.getLevelNumber() + "  Enemies: " + enemySpawner.getRemainingEnemies(), 10, 20);

        // Display player info and power-ups - use playerTanks.size() directly
        for (int i = 0; i < playerTanks.size(); i++) {
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
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.GUN);
                xOffset += 20;
            }
            if (player.getStarCount() > 0) {
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.STAR);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getStarCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
            if (player.getCarCount() > 0) {
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.CAR);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getCarCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
            if (player.hasShip()) {
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SHIP);
                xOffset += 20;
            }
            if (player.hasSaw()) {
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SAW);
                xOffset += 20;
            }
            if (player.hasShield()) {
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.SHIELD);
                xOffset += 20;
            }
            if (player.getMachinegunCount() > 0) {
                renderPowerUpIcon(xOffset, yOffset, PowerUp.Type.MACHINEGUN);
                gc.setFill(Color.WHITE);
                gc.fillText("x" + player.getMachinegunCount(), xOffset + 15, yOffset + 12);
                xOffset += 35;
            }
        }

        // Render BOSS health indicator if BOSS is alive
        renderBossHealthBar();

        if (gameOver) {
            // Initialize dancing characters when base was destroyed (not when players died)
            if (!base.isAlive() && !dancingInitialized) {
                initializeDancingCharacters();
            }

            // Update and render dancing characters
            for (DancingCharacter dancer : dancingCharacters) {
                dancer.update();
                dancer.render(gc);
            }

            // Render laughing skull instead of GIF (synchronized for all players)
            renderLaughingSkull(gc, width / 2, height / 2 - 150);

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
            if (!victoryDancingInitialized) {
                soundManager.stopGameplaySounds();
                initializeVictoryCelebration();
            }

            // Update and render dancing girls
            for (DancingGirl girl : victoryDancingGirls) {
                girl.update();
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
        int activePlayers = playerTanks.size();
        if (activePlayers == 0) return;

        // Find winner (highest kills) - only if victory and more than 1 player
        int winnerIndex = -1;
        int highestKills = -1;
        boolean isTie = false;

        if (victory && activePlayers > 1) {
            for (int i = 0; i < activePlayers; i++) {
                int kills = playerKills[i];
                if (kills > highestKills) {
                    highestKills = kills;
                    winnerIndex = i;
                    isTie = false;
                } else if (kills == highestKills) {
                    isTie = true;
                }
            }

            // Award winner bonus (10 points) if no tie - only once
            if (!isTie && winnerIndex >= 0 && !winnerBonusAwarded) {
                addScore(winnerIndex, 10);
                winnerBonusAwarded = true;
            }
        }

        // Table dimensions and position - wider columns for better visibility
        double tableX = width / 2 - 320;
        double tableY = startY;
        double rowHeight = 28;
        // Name, REG, ARM, FST, PWR, HVY, BSS, Total, LvlPts, TotalPts
        double colWidths[] = {110, 35, 35, 35, 35, 35, 35, 55, 60, 70};
        double totalWidth = 0;
        for (double w : colWidths) totalWidth += w;

        // Draw table background with border
        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRoundRect(tableX - 10, tableY - 10, totalWidth + 20, (activePlayers + 2) * rowHeight + 25, 15, 15);
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeRoundRect(tableX - 10, tableY - 10, totalWidth + 20, (activePlayers + 2) * rowHeight + 25, 15, 15);

        // Draw header with larger font
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        gc.setFill(Color.GOLD);

        double xPos = tableX;
        // First column is text "PLAYER"
        gc.fillText("PLAYER", xPos + 5, tableY + 18);
        xPos += colWidths[0];

        // Draw mini tank icons for each enemy type column (centered)
        Tank.EnemyType[] enemyTypes = {Tank.EnemyType.REGULAR, Tank.EnemyType.ARMORED, Tank.EnemyType.FAST,
                                        Tank.EnemyType.POWER, Tank.EnemyType.HEAVY, Tank.EnemyType.BOSS};
        for (int t = 0; t < 6; t++) {
            drawMiniTank(gc, xPos + (colWidths[t + 1] - 18) / 2, tableY + 2, enemyTypes[t]);
            xPos += colWidths[t + 1];
        }

        // Text headers for kills and points columns (centered)
        String[] textHeaders = {"KILLS", "LEVEL", "TOTAL"};
        for (int c = 0; c < textHeaders.length; c++) {
            gc.setFill(Color.GOLD);
            gc.fillText(textHeaders[c], xPos + 2, tableY + 18);
            xPos += colWidths[7 + c];
        }

        // Draw header line
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeLine(tableX - 5, tableY + 24, tableX + totalWidth + 5, tableY + 24);

        // Draw player rows with larger font
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        int totalKills = 0;
        int[] totalByType = new int[6];
        int totalLevelPoints = 0;
        int totalPoints = 0;

        for (int i = 0; i < activePlayers; i++) {
            double rowY = tableY + 45 + i * rowHeight;
            xPos = tableX;

            // Player name color (cyan for all, winner gets gold medal)
            gc.setFill(Color.CYAN);

            // Get player nickname (truncate if too long)
            String name = getPlayerDisplayName(i);
            if (name.length() > 10) name = name.substring(0, 9) + "..";

            // Add gold medal for winner (only if not a tie)
            if (victory && activePlayers > 1 && !isTie && i == winnerIndex) {
                name = "\uD83E\uDD47 " + name; // Gold medal emoji
            }
            gc.fillText(name, xPos + 5, rowY);
            xPos += colWidths[0];

            // Kills by type columns (colors for each type, centered)
            Color[] typeColors = {Color.LIGHTGRAY, Color.SILVER, Color.LIGHTBLUE, Color.MAGENTA, Color.DARKGRAY, Color.RED};
            for (int t = 0; t < 6; t++) {
                int killCount = playerKillsByType[i][t];
                totalByType[t] += killCount;
                gc.setFill(killCount > 0 ? typeColors[t] : Color.GRAY);
                String numStr = String.valueOf(killCount);
                gc.fillText(numStr, xPos + (colWidths[t + 1] - numStr.length() * 8) / 2, rowY);
                xPos += colWidths[t + 1];
            }

            // Total kills (centered)
            int kills = playerKills[i];
            totalKills += kills;
            gc.setFill(Color.WHITE);
            String killsStr = String.valueOf(kills);
            gc.fillText(killsStr, xPos + (colWidths[7] - killsStr.length() * 8) / 2, rowY);
            xPos += colWidths[7];

            // Level Points (centered)
            int levelScore = playerLevelScores[i];
            totalLevelPoints += levelScore;
            gc.setFill(Color.LIME);
            String lvlStr = String.valueOf(levelScore);
            gc.fillText(lvlStr, xPos + (colWidths[8] - lvlStr.length() * 8) / 2, rowY);
            xPos += colWidths[8];

            // Total Points (centered, larger font)
            int score = playerScores[i];
            totalPoints += score;
            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
            String scoreStr = String.valueOf(score);
            gc.fillText(scoreStr, xPos + (colWidths[9] - scoreStr.length() * 10) / 2, rowY);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        }

        // Draw totals row
        double totalsY = tableY + 45 + activePlayers * rowHeight + 8;
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeLine(tableX - 5, totalsY - 20, tableX + totalWidth + 5, totalsY - 20);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        gc.setFill(Color.GOLD);
        xPos = tableX;
        gc.fillText("TOTAL", xPos + 5, totalsY);
        xPos += colWidths[0];

        // Total by type (centered)
        for (int t = 0; t < 6; t++) {
            String numStr = String.valueOf(totalByType[t]);
            gc.fillText(numStr, xPos + (colWidths[t + 1] - numStr.length() * 8) / 2, totalsY);
            xPos += colWidths[t + 1];
        }

        // Total kills (centered)
        String totalKillsStr = String.valueOf(totalKills);
        gc.fillText(totalKillsStr, xPos + (colWidths[7] - totalKillsStr.length() * 8) / 2, totalsY);
        xPos += colWidths[7];

        // Total level points (centered)
        gc.setFill(Color.LIME);
        String totalLvlStr = String.valueOf(totalLevelPoints);
        gc.fillText(totalLvlStr, xPos + (colWidths[8] - totalLvlStr.length() * 8) / 2, totalsY);
        xPos += colWidths[8];

        // Grand total points (centered)
        gc.setFill(Color.YELLOW);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
        String totalPtsStr = String.valueOf(totalPoints);
        gc.fillText(totalPtsStr, xPos + (colWidths[9] - totalPtsStr.length() * 10) / 2, totalsY);

        // Display boss kill info on victory screen - positioned at top of screen
        if (victory && bossKillerPlayerIndex >= 0 && bossKillPowerUpReward != null) {
            String killerName = getPlayerDisplayName(bossKillerPlayerIndex);
            String powerUpName = getPowerUpDisplayName(bossKillPowerUpReward);

            // Draw at top of screen with background box for visibility
            double boxX = width / 2 - 150;
            double boxY = 10;
            double boxWidth = 300;
            double boxHeight = 55;

            // Semi-transparent background
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

            // Gold border
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(2);
            gc.strokeRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

            // Boss slain text
            gc.setFill(Color.GOLD);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 18));
            gc.fillText("BOSS slain by " + killerName + "!", boxX + 20, boxY + 25);

            // Reward text
            gc.setFill(Color.MAGENTA);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
            gc.fillText("Reward: " + powerUpName, boxX + 20, boxY + 45);
        }
    }

    private String getPowerUpDisplayName(PowerUp.Type type) {
        return switch (type) {
            case GUN -> "Gun (break steel)";
            case STAR -> "Star (faster shots)";
            case CAR -> "Car (speed boost)";
            case SHIP -> "Ship (swim)";
            case SAW -> "Saw (cut trees)";
            case TANK -> "Tank (extra life)";
            case SHIELD -> "Shield (protection)";
            case MACHINEGUN -> "Machinegun (wrap shots)";
            case SHOVEL -> "Shovel (steel base)";
            case FREEZE -> "Freeze";
            case BOMB -> "Bomb";
            case LASER -> "Laser (beam attack)";
        };
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
        GameState state = new GameState();

        // Build player data using centralized PlayerData entity
        for (int i = 0; i < playerTanks.size() && i < 4; i++) {
            Tank tank = playerTanks.get(i);
            state.players[i].copyFromTank(tank, playerKills[i], playerScores[i], playerLevelScores[i], playerNicknames[i], playerKillsByType[i]);
        }
        // Debug: Log scores being sent
        if (playerScores[0] > 0 || playerScores[1] > 0) {
            System.out.println("BUILD_STATE: Sending scores P1=" + playerScores[0] + " P2=" + playerScores[1]);
        }

        // Enemy tanks
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

        // Bullets
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

        // Power-ups
        for (PowerUp powerUp : powerUps) {
            if (powerUp != null) {
                state.powerUps.add(new GameState.PowerUpData(
                    powerUp.getX(),
                    powerUp.getY(),
                    powerUp.getType().ordinal()
                ));
            }
        }

        // Game state
        state.gameOver = gameOver;
        state.victory = victory;
        state.remainingEnemies = enemySpawner.getRemainingEnemies();
        state.levelNumber = gameMap.getLevelNumber();
        state.baseAlive = base.isAlive();
        state.baseShowFlag = base.isShowingFlag();
        state.baseFlagHeight = base.getFlagHeight();
        state.baseShowVictoryFlag = base.isShowingVictoryFlag();
        state.baseVictoryFlagHeight = base.getVictoryFlagHeight();
        state.baseEasterEggMode = base.isEasterEggMode();
        state.connectedPlayers = network != null ? network.getConnectedPlayerCount() : playerCount;
        state.enemyFreezeDuration = enemyFreezeDuration;
        state.playerFreezeDuration = playerFreezeDuration;
        state.enemyTeamSpeedBoostDuration = enemyTeamSpeedBoostDuration;
        state.bossKillerPlayerIndex = bossKillerPlayerIndex;
        state.bossKillPowerUpReward = bossKillPowerUpReward != null ? bossKillPowerUpReward.ordinal() : -1;

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

        // Dancing characters for game over animation
        state.dancingInitialized = dancingInitialized;
        for (DancingCharacter dancer : dancingCharacters) {
            state.dancingCharacters.add(new GameState.DancingCharacterData(
                dancer.x, dancer.y, dancer.isAlien, dancer.animFrame,
                dancer.danceStyle, dancer.colorIndex
            ));
        }

        // Victory dancing girls
        state.victoryDancingInitialized = victoryDancingInitialized;
        for (DancingGirl girl : victoryDancingGirls) {
            state.victoryDancingGirls.add(new GameState.DancingGirlData(
                girl.x, girl.y, girl.animFrame,
                girl.danceStyle, girl.dressColorIndex, girl.hairColorIndex
            ));
        }

        // Map changes (legacy, keeping for compatibility)
        state.tileChanges.addAll(mapChanges);
        mapChanges.clear(); // Clear after sending

        // UFO data
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
        state.ufoLostMessageTimer = ufoLostMessageTimer;
        state.ufoKilledMessageTimer = ufoKilledMessageTimer;

        // Easter egg data
        if (easterEgg != null) {
            state.easterEggData = new GameState.EasterEggData(
                easterEgg.getX(), easterEgg.getY(), easterEgg.getLifetime()
            );
        } else {
            state.easterEggData = null;
        }

        // Send host's game settings to clients
        state.hostPlayerSpeed = GameSettings.getPlayerSpeedMultiplier();
        state.hostEnemySpeed = GameSettings.getEnemySpeedMultiplier();
        state.hostPlayerShootSpeed = GameSettings.getPlayerShootSpeedMultiplier();
        state.hostEnemyShootSpeed = GameSettings.getEnemyShootSpeedMultiplier();

        return state;
    }

    private void applyGameState(GameState state) {
        // Apply host's game settings (client uses host's settings in multiplayer)
        GameSettings.setHostSettings(
            state.hostPlayerSpeed,
            state.hostEnemySpeed,
            state.hostPlayerShootSpeed,
            state.hostEnemyShootSpeed
        );

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
            double[][] newStartPositions = new double[playerTanks.size()][2];
            for (int j = 0; j < playerStartPositions.length; j++) {
                newStartPositions[j] = playerStartPositions[j];
            }
            newStartPositions[playerNum - 1] = new double[]{x, y};
            playerStartPositions = newStartPositions;
        }

        // Get local player index - skip position updates for local player (they control their own position)
        int myPlayerIndex = network != null ? network.getPlayerNumber() - 1 : -1;

        // Update all players using centralized PlayerData
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
                System.out.println("Client first sync - accepting host position: " + pData.x + ", " + pData.y);
                respawnSyncFrames = 5; // Wait a few frames before sending position
            } else if (justRespawned) {
                System.out.println("Client respawning at host position: " + pData.x + ", " + pData.y);
                respawnSyncFrames = 5; // Wait a few frames before sending position
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
                System.out.println("APPLY_STATE: Player " + (i + 1) + " score updated: " + oldScore + " -> " + pData.score);
            }
            // Update kills by type
            if (pData.killsByType != null) {
                System.arraycopy(pData.killsByType, 0, playerKillsByType[i], 0, Math.min(6, pData.killsByType.length));
            }
            // Don't overwrite local player's nickname
            if (i != myPlayerIndex && pData.nickname != null) {
                playerNicknames[i] = pData.nickname;
            }
        }

        // Update enemy tanks - reuse existing tanks to preserve animation state
        // First, resize the list to match state
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

        // Update bullets (recreate from state) and detect new bullets for sound
        Set<Long> currentBulletIds = new HashSet<>();
        bullets.clear();
        for (GameState.BulletData bData : state.bullets) {
            currentBulletIds.add(bData.id);
            // Play shoot sound for bullets we haven't seen before
            // Skip on first state to avoid sound burst when joining mid-game
            if (firstStateReceived && !seenBulletIds.contains(bData.id)) {
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
        seenBulletIds = currentBulletIds;

        // Mark first state as received
        if (!firstStateReceived) {
            firstStateReceived = true;
        }

        // Update power-ups (recreate from state)
        powerUps.clear();
        for (GameState.PowerUpData pData : state.powerUps) {
            PowerUp powerUp = new PowerUp(
                pData.x, pData.y,
                PowerUp.Type.values()[pData.type]
            );
            powerUps.add(powerUp);
        }

        // Sync full map state from host
        if (state.mapTiles != null) {
            gameMap.importTiles(state.mapTiles);
        }

        // Sync burning tiles for fire animation
        if (state.burningTiles != null) {
            List<int[]> burningData = new ArrayList<>();
            for (GameState.BurningTileData bt : state.burningTiles) {
                burningData.add(new int[]{bt.row, bt.col, bt.framesRemaining});
            }
            gameMap.setBurningTiles(burningData);
        }

        // Sync dancing characters for game over animation
        dancingInitialized = state.dancingInitialized;
        if (state.dancingCharacters != null && !state.dancingCharacters.isEmpty()) {
            dancingCharacters.clear();
            for (GameState.DancingCharacterData dData : state.dancingCharacters) {
                dancingCharacters.add(new DancingCharacter(
                    dData.x, dData.y, dData.isAlien, dData.animFrame,
                    dData.danceStyle, dData.colorIndex
                ));
            }
        }

        // Sync victory dancing girls
        victoryDancingInitialized = state.victoryDancingInitialized;
        if (state.victoryDancingGirls != null && !state.victoryDancingGirls.isEmpty()) {
            victoryDancingGirls.clear();
            for (GameState.DancingGirlData gData : state.victoryDancingGirls) {
                victoryDancingGirls.add(new DancingGirl(
                    gData.x, gData.y, gData.animFrame,
                    gData.danceStyle, gData.dressColorIndex, gData.hairColorIndex
                ));
            }
        }

        // Sync UFO from host
        if (state.ufoData != null && state.ufoData.alive) {
            if (ufo == null) {
                ufo = new UFO(state.ufoData.x, state.ufoData.y, state.ufoData.movingRight);
            }
            ufo.setX(state.ufoData.x);
            ufo.setY(state.ufoData.y);
            ufo.setDx(state.ufoData.dx);
            ufo.setDy(state.ufoData.dy);
            ufo.setHealth(state.ufoData.health);
            ufo.setLifetime(state.ufoData.lifetime);
            ufo.setAlive(state.ufoData.alive);
        } else {
            ufo = null;
        }

        // Sync UFO message timers
        ufoLostMessageTimer = state.ufoLostMessageTimer;
        ufoKilledMessageTimer = state.ufoKilledMessageTimer;

        // Sync easter egg from host
        if (state.easterEggData != null) {
            if (easterEgg == null) {
                easterEgg = new EasterEgg(state.easterEggData.x, state.easterEggData.y);
            }
            easterEgg.setPosition(state.easterEggData.x, state.easterEggData.y);
            easterEgg.setLifetime(state.easterEggData.lifetime);
        } else {
            easterEgg = null;
        }

        // Check if level changed (host went to next level)
        boolean levelChanged = state.levelNumber != gameMap.getLevelNumber();
        // Check if game was restarted (gameOver went from true to false)
        boolean gameRestarted = gameOver && !state.gameOver && !state.victory;
        // Check if next level started (victory went from true to false)
        boolean nextLevelStarted = victory && !state.victory && !state.gameOver;

        if (levelChanged || gameRestarted || nextLevelStarted) {
            if (levelChanged) {
                System.out.println("Level changed from " + gameMap.getLevelNumber() + " to " + state.levelNumber);
            }
            if (gameRestarted) {
                System.out.println("Game restarted by host - resetting client state");
            }
            if (nextLevelStarted) {
                System.out.println("Next level started by host - resetting client state");
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
            // Clear visual state
            dancingInitialized = false;
            dancingCharacters.clear();
            victoryDancingInitialized = false;
            victoryDancingGirls.clear();
            gameOverSoundPlayed = false;
            // Hide end game images
            if (victoryImageView != null) victoryImageView.setVisible(false);
            if (gameOverImageView != null) gameOverImageView.setVisible(false);
            // Reset local player tank position to FIXED start position
            int localPlayerIdx = network != null ? network.getPlayerNumber() - 1 : -1;
            if (localPlayerIdx >= 0 && localPlayerIdx < playerTanks.size()) {
                Tank myTank = playerTanks.get(localPlayerIdx);
                myTank.setPosition(FIXED_START_POSITIONS[localPlayerIdx][0], FIXED_START_POSITIONS[localPlayerIdx][1]);
                myTank.setDirection(Direction.UP);
                myTank.giveTemporaryShield();
            }
            // Clear bullets, lasers, and power-ups for clean state
            bullets.clear();
            lasers.clear();
            powerUps.clear();
            // Clear enemy tanks - will be recreated from state
            enemyTanks.clear();
            // Reset spawner with proper enemy count
            enemySpawner = new EnemySpawner(totalEnemies, 5, gameMap);
            // Reset UFO state
            ufo = null;
            ufoLostMessageTimer = 0;
            ufoKilledMessageTimer = 0;
            // Play intro sound
            soundManager.playIntro();
        }

        // Update game state
        gameOver = state.gameOver;
        victory = state.victory;

        // Sync boss kill info for victory screen
        bossKillerPlayerIndex = state.bossKillerPlayerIndex;
        bossKillPowerUpReward = state.bossKillPowerUpReward >= 0 ? PowerUp.Type.values()[state.bossKillPowerUpReward] : null;

        // Update freeze state
        enemyFreezeDuration = state.enemyFreezeDuration;
        playerFreezeDuration = state.playerFreezeDuration;
        enemyTeamSpeedBoostDuration = state.enemyTeamSpeedBoostDuration;

        // Update remaining enemies count
        enemySpawner.setRemainingEnemies(state.remainingEnemies);

        // Update base - recreate if level changed, game restarted, next level, or if state differs
        if (levelChanged || gameRestarted || nextLevelStarted || (state.baseAlive && !base.isAlive())) {
            base = new Base(12 * 32, 24 * 32);
        } else if (!state.baseAlive && base.isAlive()) {
            base.destroy();
            soundManager.playBaseDestroyed();
        }

        // Update level number after base check
        while (gameMap.getLevelNumber() < state.levelNumber) {
            gameMap.nextLevel();
        }
        if (gameMap.getLevelNumber() > state.levelNumber) {
            gameMap.resetToLevel1();
        }

        // Sync flag state (skull flag for game over)
        base.setFlagState(state.baseShowFlag, state.baseFlagHeight);
        // Sync victory flag state
        base.setVictoryFlagState(state.baseShowVictoryFlag, state.baseVictoryFlagHeight);
        // Sync easter egg mode
        base.setEasterEggMode(state.baseEasterEggMode);

        // Play explosion sound when enemy dies
        int currentEnemyCount = enemyTanks.size();
        if (currentEnemyCount < prevEnemyCount) {
            soundManager.playExplosion();
        }
        prevEnemyCount = currentEnemyCount;
    }

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
        if (playerFreezeDuration <= 0) {
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
