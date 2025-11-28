package com.battlecity;

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
    private List<PowerUp> powerUps;
    private EnemySpawner enemySpawner;
    private InputHandler inputHandler;
    private SoundManager soundManager;
    private Base base;
    private double[][] playerStartPositions; // For respawning

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

    // Player kills tracking
    private int[] playerKills = new int[4];

    // For client sound effects (track previous state to detect changes)
    private int prevEnemyCount = 0;
    private Set<Long> seenBulletIds = new HashSet<>(); // Track bullet IDs we've already played sounds for

    // SHOVEL power-up - base protection with steel
    private int baseProtectionDuration = 0;
    private static final int BASE_PROTECTION_TIME = 3600; // 1 minute at 60 FPS
    private boolean isFlashing = false;
    private int flashCount = 0; // Counts the number of flashes (up to 10 for 5 complete flashes)
    private int flashTimer = 0; // Timer for each flash state (60 frames = 1 second)
    private static final int FLASH_DURATION = 60; // 1 second at 60 FPS
    private static final int TOTAL_FLASHES = 10; // 5 complete flashes (10 state changes)

    // Victory dancing anime girl
    private ImageView victoryImageView;
    // Game over dancing death
    private ImageView gameOverImageView;
    private boolean gameOverSoundPlayed = false;

    // Dancing aliens/humans when enemies destroy base
    private List<DancingCharacter> dancingCharacters = new ArrayList<>();
    private boolean dancingInitialized = false;

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

    // Constructor for local game
    public Game(Pane root, int width, int height, int playerCount, int totalEnemies, Stage stage) {
        this(root, width, height, playerCount, totalEnemies, stage, null);
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
        powerUps = new ArrayList<>();

        // Initialize base at bottom center
        base = new Base(12 * 32, 24 * 32);

        // Initialize player tanks based on player count
        // For network games, start with 2 tanks (host + first client)
        // Additional tanks will be added when more players connect
        playerTanks = new ArrayList<>();
        int initialPlayers = isNetworkGame ? 2 : playerCount; // Network games start with 2
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
        }

        // Initialize enemy tanks list
        enemyTanks = new ArrayList<>();

        // Initialize enemy spawner
        enemySpawner = new EnemySpawner(totalEnemies, 10, gameMap);

        // Initialize input handler
        inputHandler = new InputHandler(root, playerTanks);

        // Add ESC key handler to return to menu - combine with existing handler
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && (gameOver || victory)) {
                returnToMenu();
            }
        });

        // Initialize sound manager
        soundManager = new SoundManager();
        soundManager.playIntro();
    }

    private void returnToMenu() {
        stop();
        MenuScene menuScene = new MenuScene(stage, width, height);
        stage.setScene(menuScene.getScene());
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

    private void update() {
        if (gameOver || victory) {
            return;
        }

        // Network game handling
        if (isNetworkGame && network != null) {
            if (!network.isConnected()) {
                // Connection lost
                gameOver = true;
                System.out.println("Network connection lost!");
                return;
            }

            if (network.isHost()) {
                // HOST: Receive and apply client inputs for all players
                for (int i = 2; i <= playerTanks.size(); i++) {
                    PlayerInput clientInput = network.getPlayerInput(i);
                    if (clientInput != null) {
                        applyPlayerInput(playerTanks.get(i - 1), clientInput);
                    }
                }
                // Host runs full game logic below
            } else {
                // CLIENT: Send input to host (server-authoritative model)
                int myPlayerIndex = network.getPlayerNumber() - 1;
                if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
                    Tank myTank = playerTanks.get(myPlayerIndex);
                    if (myTank.isAlive()) {
                        // Capture and send input to host
                        PlayerInput input = inputHandler.capturePlayerInput();
                        network.sendInput(input);
                    }
                }

                // CLIENT: Receive and apply game state from host
                GameState state = network.getLatestGameState();
                if (state != null) {
                    applyGameState(state);
                }
                // Client skips rest of game logic
                return;
            }
        }

        // Create combined list of all tanks for collision detection
        List<Tank> allTanks = new ArrayList<>();
        allTanks.addAll(playerTanks);
        allTanks.addAll(enemyTanks);

        // Handle player input (local or host)
        inputHandler.handleInput(gameMap, bullets, soundManager, allTanks, base);

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
        enemySpawner.update(enemyTanks);

        // Update player tanks and handle respawn
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive()) {
                player.update(gameMap, bullets, soundManager, allTanks, base);
            } else if (player.getLives() > 0) {
                // Player died but has lives left - respawn at start position
                soundManager.playExplosion();
                player.respawn(playerStartPositions[i][0], playerStartPositions[i][1]);
            }
        }

        // Update enemy tanks with AI
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                tank.updateAI(gameMap, bullets, allTanks, base, soundManager);
            }
        }

        // Update bullets
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update();

            // Check bullet collisions with map
            if (gameMap.checkBulletCollision(bullet)) {
                bulletIterator.remove();
                continue;
            }

            // Check bullet out of bounds and handle wraparound through destroyed borders
            if (bullet.isOutOfBounds(width, height)) {
                // Try to wrap around if border is destroyed, otherwise remove bullet
                if (!bullet.handleWraparound(gameMap, width, height)) {
                    bulletIterator.remove();
                    continue;
                }
            }

            // Check bullet collision with tanks
            boolean bulletRemoved = false;

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
                            // Track kill for the player who fired the bullet
                            int killerPlayer = bullet.getOwnerPlayerNumber();
                            if (killerPlayer >= 1 && killerPlayer <= 4) {
                                playerKills[killerPlayer - 1]++;
                            }
                        }
                        bulletIterator.remove();
                        bulletRemoved = true;
                        break;
                    }
                }
            } else {
                // Enemy bullet - check collision with players
                for (Tank player : playerTanks) {
                    if (player.isAlive() && bullet.collidesWith(player)) {
                        if (!player.hasShield()) {
                            player.damage();
                            if (!player.isAlive()) {
                                soundManager.playExplosion();
                            }
                        }
                        bulletIterator.remove();
                        bulletRemoved = true;
                        break;
                    }
                }
            }

            // Check collision with base (all bullets can hit base)
            if (!bulletRemoved && bullet.collidesWith(base)) {
                base.destroy();
                soundManager.playExplosion();
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
                    bullets.remove(j);
                    bullets.remove(i);
                    i--; // Adjust index after removal
                    break;
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
                    // Handle SHOVEL power-up specially (affects map, not tank)
                    if (powerUp.getType() == PowerUp.Type.SHOVEL) {
                        gameMap.setBaseProtection(GameMap.TileType.STEEL);
                        baseProtectionDuration = BASE_PROTECTION_TIME; // Reset timer to 1 minute
                        isFlashing = false; // Stop flashing if it was flashing
                        flashCount = 0;
                        flashTimer = 0;
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
                        // Handle SHOVEL power-up specially (affects map, not tank)
                        if (powerUp.getType() == PowerUp.Type.SHOVEL) {
                            // Enemy takes SHOVEL - remove base protection (make it "naked")
                            gameMap.setBaseProtection(GameMap.TileType.EMPTY);
                            baseProtectionDuration = 0; // Stop timer
                            isFlashing = false; // Stop flashing
                            flashCount = 0;
                            flashTimer = 0;
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

        // Remove dead enemy tanks
        enemyTanks.removeIf(tank -> !tank.isAlive());

        // Check victory condition
        if (enemySpawner.allEnemiesSpawned() && enemyTanks.isEmpty()) {
            victory = true;
        }

        // Check game over condition (all players dead with no lives OR base destroyed)
        boolean allPlayersDead = playerTanks.stream().allMatch(p -> !p.isAlive() && p.getLives() <= 0);
        if (allPlayersDead || !base.isAlive()) {
            gameOver = true;
        }

        // HOST: Send game state to client periodically
        if (isNetworkGame && network != null && network.isHost()) {
            long now = System.nanoTime();
            if (now - lastNetworkUpdate >= NETWORK_UPDATE_INTERVAL) {
                GameState state = buildGameState();
                network.sendGameState(state);
                lastNetworkUpdate = now;
            }
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

        // Render bullets
        for (Bullet bullet : bullets) {
            bullet.render(gc);
        }

        // Render player tanks
        for (Tank tank : playerTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
            }
        }

        // Render enemy tanks
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                tank.render(gc);
            }
        }

        // Render trees ON TOP of tanks to make tanks partially visible in forest
        gameMap.renderTrees(gc);

        // Render burning trees with fire animation (on top of everything)
        gameMap.renderBurningTiles(gc);

        // Render UI
        renderUI();
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
                // Bullet breaking wall
                gc.setFill(Color.DARKGRAY);
                gc.fillRect(x + 1, y + 3, 4, 10); // Wall
                gc.setFill(Color.RED);
                gc.fillPolygon(
                    new double[]{x + 14, x + 7, x + 7},
                    new double[]{y + 8, y + 5, y + 11},
                    3
                ); // Bullet
                break;
            case STAR:
                // 5-pointed star
                gc.setFill(Color.YELLOW);
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
                gc.fillPolygon(starX, starY, 10);
                break;
            case CAR:
                // Speed arrow with lines
                gc.setFill(Color.LIME);
                gc.fillPolygon(
                    new double[]{x + 12, x + 6, x + 6},
                    new double[]{y + 8, y + 4, y + 12},
                    3
                );
                gc.fillRect(x + 2, y + 5, 4, 1);
                gc.fillRect(x + 2, y + 8, 4, 1);
                gc.fillRect(x + 2, y + 11, 4, 1);
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

    private void renderUI() {
        gc.setFill(Color.WHITE);
        gc.fillText("Enemies: " + enemySpawner.getRemainingEnemies(), 10, 20);

        // Display player info and power-ups
        int connectedCount = isNetworkGame && network != null ? network.getConnectedPlayerCount() : playerTanks.size();
        for (int i = 0; i < Math.min(playerTanks.size(), connectedCount); i++) {
            Tank player = playerTanks.get(i);
            int playerNum = i + 1;
            double yOffset = 40 + i * 60;

            // Display lives and kills
            gc.setFill(Color.WHITE);
            gc.fillText("P" + playerNum + " Lives: " + player.getLives() + "  Kills: " + playerKills[i], 10, yOffset);

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

            // Show dancing death if available
            if (gameOverImageView != null) {
                gameOverImageView.setVisible(true);
            }

            // Play sad sound once
            if (!gameOverSoundPlayed) {
                soundManager.playSad();
                gameOverSoundPlayed = true;
            }

            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font(40));
            gc.fillText("GAME OVER", width / 2 - 120, height / 2 + 50);

            // Show statistics
            renderEndGameStats(height / 2 + 90);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(20));
            gc.fillText("Press ESC to return to menu", width / 2 - 120, height / 2 + 220);
        } else if (victory) {
            // Show dancing anime girl if available
            if (victoryImageView != null) {
                victoryImageView.setVisible(true);
            }

            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font(40));
            gc.fillText("VICTORY!", width / 2 - 100, height / 2 + 50);

            // Show statistics
            renderEndGameStats(height / 2 + 90);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(20));
            gc.fillText("Press ESC to return to menu", width / 2 - 120, height / 2 + 220);
        } else {
            // Hide images when not in end state
            if (victoryImageView != null) {
                victoryImageView.setVisible(false);
            }
            if (gameOverImageView != null) {
                gameOverImageView.setVisible(false);
            }
        }
    }

    private void renderEndGameStats(double startY) {
        gc.setFont(javafx.scene.text.Font.font(18));
        gc.setFill(Color.WHITE);
        gc.fillText("=== STATISTICS ===", width / 2 - 80, startY);

        int totalKills = 0;
        int connectedCount = isNetworkGame && network != null ? network.getConnectedPlayerCount() : playerTanks.size();

        for (int i = 0; i < Math.min(playerTanks.size(), connectedCount); i++) {
            Tank player = playerTanks.get(i);
            int kills = playerKills[i];
            totalKills += kills;

            gc.setFill(Color.CYAN);
            gc.fillText("Player " + (i + 1) + ": " + kills + " kills", width / 2 - 60, startY + 25 + i * 22);
        }

        gc.setFill(Color.YELLOW);
        gc.fillText("Total: " + totalKills + " kills", width / 2 - 60, startY + 25 + connectedCount * 22 + 10);
    }

    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
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

        // Player 1 data
        if (playerTanks.size() >= 1) {
            Tank p1 = playerTanks.get(0);
            state.p1X = p1.getX();
            state.p1Y = p1.getY();
            state.p1Direction = p1.getDirection().ordinal();
            state.p1Lives = p1.getLives();
            state.p1Alive = p1.isAlive();
            state.p1HasShield = p1.hasShield();
            state.p1HasShip = p1.hasShip();
        }

        // Player 2 data
        if (playerTanks.size() >= 2) {
            Tank p2 = playerTanks.get(1);
            state.p2X = p2.getX();
            state.p2Y = p2.getY();
            state.p2Direction = p2.getDirection().ordinal();
            state.p2Lives = p2.getLives();
            state.p2Alive = p2.isAlive();
            state.p2HasShield = p2.hasShield();
            state.p2HasShip = p2.hasShip();
        }

        // Player 3 data
        if (playerTanks.size() >= 3) {
            Tank p3 = playerTanks.get(2);
            state.p3X = p3.getX();
            state.p3Y = p3.getY();
            state.p3Direction = p3.getDirection().ordinal();
            state.p3Lives = p3.getLives();
            state.p3Alive = p3.isAlive();
            state.p3HasShield = p3.hasShield();
            state.p3HasShip = p3.hasShip();
        }

        // Player 4 data
        if (playerTanks.size() >= 4) {
            Tank p4 = playerTanks.get(3);
            state.p4X = p4.getX();
            state.p4Y = p4.getY();
            state.p4Direction = p4.getDirection().ordinal();
            state.p4Lives = p4.getLives();
            state.p4Alive = p4.isAlive();
            state.p4HasShield = p4.hasShield();
            state.p4HasShip = p4.hasShip();
        }

        // Enemy tanks
        for (Tank enemy : enemyTanks) {
            if (enemy != null) {
                state.enemies.add(new GameState.EnemyData(
                    enemy.getX(),
                    enemy.getY(),
                    enemy.getDirection().ordinal(),
                    enemy.isAlive(),
                    enemy.getEnemyType().ordinal()
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
                    bullet.getOwnerPlayerNumber()
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
        state.baseAlive = base.isAlive();
        state.baseShowFlag = base.isShowingFlag();
        state.baseFlagHeight = base.getFlagHeight();
        state.connectedPlayers = network != null ? network.getConnectedPlayerCount() : playerCount;

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

        // Player kills
        state.p1Kills = playerKills[0];
        state.p2Kills = playerKills[1];
        state.p3Kills = playerKills[2];
        state.p4Kills = playerKills[3];

        // Dancing characters for game over animation
        state.dancingInitialized = dancingInitialized;
        for (DancingCharacter dancer : dancingCharacters) {
            state.dancingCharacters.add(new GameState.DancingCharacterData(
                dancer.x, dancer.y, dancer.isAlien, dancer.animFrame,
                dancer.danceStyle, dancer.colorIndex
            ));
        }

        // Map changes (legacy, keeping for compatibility)
        state.tileChanges.addAll(mapChanges);
        mapChanges.clear(); // Clear after sending

        return state;
    }

    private void applyGameState(GameState state) {
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
        }

        // Get local player index for client-side prediction
        int myPlayerIndex = network != null ? network.getPlayerNumber() - 1 : -1;
        double correctionThreshold = 32.0; // Snap to server position if drift > 1 tile

        // Update Player 1 - server authoritative
        if (playerTanks.size() >= 1 && state.p1Alive) {
            Tank p1 = playerTanks.get(0);
            p1.setPosition(state.p1X, state.p1Y);
            p1.setDirection(Direction.values()[state.p1Direction]);
            p1.setShield(state.p1HasShield);
            p1.setShip(state.p1HasShip);
        }

        // Update Player 2 - server authoritative
        if (playerTanks.size() >= 2 && state.p2Alive) {
            Tank p2 = playerTanks.get(1);
            p2.setPosition(state.p2X, state.p2Y);
            p2.setDirection(Direction.values()[state.p2Direction]);
            p2.setShield(state.p2HasShield);
            p2.setShip(state.p2HasShip);
        }

        // Update Player 3 - server authoritative
        if (playerTanks.size() >= 3 && state.p3Alive) {
            Tank p3 = playerTanks.get(2);
            p3.setPosition(state.p3X, state.p3Y);
            p3.setDirection(Direction.values()[state.p3Direction]);
            p3.setShield(state.p3HasShield);
            p3.setShip(state.p3HasShip);
        }

        // Update Player 4 - server authoritative
        if (playerTanks.size() >= 4 && state.p4Alive) {
            Tank p4 = playerTanks.get(3);
            p4.setPosition(state.p4X, state.p4Y);
            p4.setDirection(Direction.values()[state.p4Direction]);
            p4.setShield(state.p4HasShield);
            p4.setShip(state.p4HasShip);
        }

        // Update enemy tanks - reuse existing tanks to preserve animation state
        // First, resize the list to match state
        while (enemyTanks.size() > state.enemies.size()) {
            enemyTanks.remove(enemyTanks.size() - 1);
        }
        while (enemyTanks.size() < state.enemies.size()) {
            enemyTanks.add(new Tank(0, 0, Direction.UP, false, 0, Tank.EnemyType.REGULAR));
        }
        // Update each enemy tank's position (setPosition will animate tracks)
        for (int i = 0; i < state.enemies.size(); i++) {
            GameState.EnemyData eData = state.enemies.get(i);
            Tank enemy = enemyTanks.get(i);
            if (eData.alive) {
                enemy.setPosition(eData.x, eData.y);
                enemy.setDirection(Direction.values()[eData.direction]);
                enemy.setEnemyType(Tank.EnemyType.values()[eData.enemyType]);
            }
        }

        // Update bullets (recreate from state) and detect new bullets for sound
        Set<Long> currentBulletIds = new HashSet<>();
        bullets.clear();
        for (GameState.BulletData bData : state.bullets) {
            currentBulletIds.add(bData.id);
            // Play shoot sound for bullets we haven't seen before
            if (!seenBulletIds.contains(bData.id)) {
                soundManager.playShoot();
            }
            Bullet bullet = new Bullet(
                bData.id,
                bData.x, bData.y,
                Direction.values()[bData.direction],
                bData.fromEnemy,
                bData.power,
                bData.canDestroyTrees,
                bData.ownerPlayerNumber
            );
            bullets.add(bullet);
        }
        // Update seen bullets - keep only current bullets to prevent memory leak
        seenBulletIds = currentBulletIds;

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

        // Update kills tracking
        playerKills[0] = state.p1Kills;
        playerKills[1] = state.p2Kills;
        playerKills[2] = state.p3Kills;
        playerKills[3] = state.p4Kills;

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

        // Update game state
        gameOver = state.gameOver;
        victory = state.victory;

        // Update remaining enemies count
        enemySpawner.setRemainingEnemies(state.remainingEnemies);

        // Update base
        if (!state.baseAlive && base.isAlive()) {
            base.destroy();
            soundManager.playExplosion();
        }

        // Sync flag state
        base.setFlagState(state.baseShowFlag, state.baseFlagHeight);

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

        // Apply movement
        if (input.up) {
            tank.move(Direction.UP, gameMap, allTanks, base);
        } else if (input.down) {
            tank.move(Direction.DOWN, gameMap, allTanks, base);
        } else if (input.left) {
            tank.move(Direction.LEFT, gameMap, allTanks, base);
        } else if (input.right) {
            tank.move(Direction.RIGHT, gameMap, allTanks, base);
        }

        // Apply shooting
        if (input.shoot) {
            tank.shoot(bullets, soundManager);
        }
    }
}
