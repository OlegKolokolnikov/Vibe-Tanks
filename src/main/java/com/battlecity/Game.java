package com.battlecity;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class Game {
    private final Pane root;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final int width;
    private final int height;
    private final int playerCount;
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

    private boolean gameOver = false;
    private boolean victory = false;

    // SHOVEL power-up - base protection with steel
    private int baseProtectionDuration = 0;
    private static final int BASE_PROTECTION_TIME = 3600; // 1 minute at 60 FPS

    public Game(Pane root, int width, int height, int playerCount, Stage stage) {
        this.root = root;
        this.width = width;
        this.height = height;
        this.playerCount = playerCount;
        this.stage = stage;

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(false); // Canvas should not take focus
        root.getChildren().add(canvas);

        initialize();
    }

    private void initialize() {
        // Initialize game objects
        gameMap = new GameMap(26, 26);
        bullets = new ArrayList<>();
        powerUps = new ArrayList<>();

        // Initialize base at bottom center
        base = new Base(12 * 32, 24 * 32);

        // Initialize player tanks based on player count
        playerTanks = new ArrayList<>();
        if (playerCount >= 1) {
            playerTanks.add(new Tank(8 * 32, 24 * 32, Direction.UP, true, 1)); // Player 1
        }
        if (playerCount >= 2) {
            playerTanks.add(new Tank(16 * 32, 24 * 32, Direction.UP, true, 2)); // Player 2
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

        // Initialize enemy spawner (100 total enemies, max 10 on screen)
        enemySpawner = new EnemySpawner(100, 10, gameMap);

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

    private void update() {
        if (gameOver || victory) {
            return;
        }

        // Handle player input
        inputHandler.handleInput(gameMap, bullets, soundManager);

        // Update base protection from SHOVEL power-up
        if (baseProtectionDuration > 0) {
            baseProtectionDuration--;
            if (baseProtectionDuration == 0) {
                gameMap.resetBaseProtection();
            }
        }

        // Spawn enemies if needed
        enemySpawner.update(enemyTanks);

        // Update player tanks and handle respawn
        for (int i = 0; i < playerTanks.size(); i++) {
            Tank player = playerTanks.get(i);
            if (player.isAlive()) {
                player.update(gameMap, bullets, soundManager);
            } else if (player.getLives() > 0) {
                // Player died but has lives left - respawn at start position
                soundManager.playExplosion();
                player.respawn(playerStartPositions[i][0], playerStartPositions[i][1]);
            }
        }

        // Update enemy tanks with AI
        for (Tank tank : enemyTanks) {
            if (tank.isAlive()) {
                tank.updateAI(gameMap, bullets, playerTanks, base, soundManager);
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

            // Check bullet out of bounds
            if (bullet.isOutOfBounds(width, height)) {
                bulletIterator.remove();
                continue;
            }

            // Check bullet collision with tanks
            boolean bulletRemoved = false;

            // Check collision with enemy tanks (from player bullets)
            if (!bullet.isFromEnemy()) {
                for (Tank enemy : enemyTanks) {
                    if (enemy.isAlive() && bullet.collidesWith(enemy)) {
                        enemy.damage();
                        if (!enemy.isAlive()) {
                            soundManager.playExplosion();
                            // Random power-up drop
                            if (Math.random() < 0.3) {
                                powerUps.add(new PowerUp(enemy.getX(), enemy.getY()));
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
                        baseProtectionDuration = BASE_PROTECTION_TIME;
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
                            gameMap.setBaseProtection(GameMap.TileType.STEEL);
                            baseProtectionDuration = BASE_PROTECTION_TIME;
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

        // Check game over condition
        boolean allPlayersDead = playerTanks.stream().noneMatch(Tank::isAlive);
        if (allPlayersDead || !base.isAlive()) {
            gameOver = true;
        }
    }

    private void render() {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        // Render map
        gameMap.render(gc);

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

        // Render UI
        renderUI();
    }

    private void renderUI() {
        gc.setFill(Color.WHITE);
        gc.fillText("Enemies: " + enemySpawner.getRemainingEnemies(), 10, 20);

        int playerNum = 1;
        for (Tank player : playerTanks) {
            gc.fillText("P" + playerNum + " Lives: " + player.getLives(), 10, 40 + (playerNum - 1) * 20);
            playerNum++;
        }

        if (gameOver) {
            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font(40));
            gc.fillText("GAME OVER", width / 2 - 120, height / 2);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(20));
            gc.fillText("Press ESC to return to menu", width / 2 - 120, height / 2 + 40);
        } else if (victory) {
            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font(40));
            gc.fillText("VICTORY!", width / 2 - 100, height / 2);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(20));
            gc.fillText("Press ESC to return to menu", width / 2 - 120, height / 2 + 40);
        }
    }

    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }
}
