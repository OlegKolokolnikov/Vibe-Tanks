package com.vibetanks.ui;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.Base;
import com.vibetanks.core.Bullet;
import com.vibetanks.core.Direction;
import com.vibetanks.core.GameMap;
import com.vibetanks.core.GameSettings;
import com.vibetanks.core.Laser;
import com.vibetanks.core.Tank;
import com.vibetanks.network.PlayerInput;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class InputHandler {
    private Set<KeyCode> pressedKeys;
    private LinkedList<KeyCode> directionKeyOrder; // Track order of direction key presses (single player / player 2)
    private LinkedList<KeyCode> player1DirectionKeyOrder; // Track order of direction key presses (player 1 in local multiplayer)
    private List<Tank> playerTanks;
    private Direction lastDirection;
    private Direction lastDirectionPlayer1;
    private Direction lastDirectionPlayer2;
    private boolean wasMoving;
    private boolean wasMovingPlayer1;
    private boolean wasMovingPlayer2;
    private GameMap gameMap;

    // Direction keys for single player / player 2 (arrows)
    private static final Set<KeyCode> DIRECTION_KEYS = Set.of(
        KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT,
        KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D
    );

    // Direction keys for player 1 in local multiplayer (WASD only)
    private static final Set<KeyCode> PLAYER1_DIRECTION_KEYS = Set.of(
        KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D
    );

    // Direction keys for player 2 in local multiplayer (arrows only)
    private static final Set<KeyCode> PLAYER2_DIRECTION_KEYS = Set.of(
        KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT
    );

    public InputHandler(Pane pane, List<Tank> playerTanks) {
        this.playerTanks = playerTanks;
        this.pressedKeys = new HashSet<>();
        this.directionKeyOrder = new LinkedList<>();
        this.player1DirectionKeyOrder = new LinkedList<>();
        this.wasMoving = false;
        this.wasMovingPlayer1 = false;
        this.wasMovingPlayer2 = false;

        pane.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            pressedKeys.add(code);
            // Track direction key order - add to end (most recent)
            if (DIRECTION_KEYS.contains(code)) {
                directionKeyOrder.remove(code); // Remove if already present
                directionKeyOrder.addLast(code); // Add as most recent
            }
            // Also track player 1 specific keys in local multiplayer
            if (PLAYER1_DIRECTION_KEYS.contains(code)) {
                player1DirectionKeyOrder.remove(code);
                player1DirectionKeyOrder.addLast(code);
            }
            event.consume();
        });

        pane.setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            pressedKeys.remove(code);
            directionKeyOrder.remove(code);
            player1DirectionKeyOrder.remove(code);
            event.consume();
        });

        // Request focus so key events work
        pane.setFocusTraversable(true);
        pane.requestFocus();

        // Clear any lingering keys when focus is gained
        pane.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                pressedKeys.clear();
                directionKeyOrder.clear();
                player1DirectionKeyOrder.clear();
            }
        });
    }

    public void handleInput(GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base) {
        handleInput(map, bullets, lasers, soundManager, allTanks, base, false);
    }

    public void handleInput(GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        // Check if local multiplayer mode
        if (GameSettings.isLocalMultiplayerMode() && playerTanks.size() >= 2) {
            // Local 2-player mode: Player 1 = WASD + SPACE, Player 2 = Arrows + ENTER
            handlePlayer1Input(playerTanks.get(0), map, bullets, lasers, soundManager, allTanks, base, movementFrozen);
            handlePlayer2Input(playerTanks.get(1), map, bullets, lasers, soundManager, allTanks, base, movementFrozen);
        } else if (playerTanks.size() >= 1) {
            // Single player mode: Arrow keys or WASD + SPACE
            handlePlayerInput(playerTanks.get(0), map, bullets, lasers, soundManager, allTanks, base, movementFrozen);
        }
    }

    /**
     * Handle shooting for a player with the specified shoot key.
     */
    private void handleShooting(Tank player, KeyCode shootKey, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager) {
        if (pressedKeys.contains(shootKey)) {
            if (player.hasLaser()) {
                Laser laser = player.shootLaser(soundManager);
                if (laser != null) {
                    lasers.add(laser);
                }
            } else {
                player.shoot(bullets, soundManager);
            }
        }
    }

    // Single player input handler (arrows or WASD + SPACE)
    private void handlePlayerInput(Tank player, GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        if (!player.isAlive()) return;

        boolean isMoving = false;

        // Movement with arrow keys or WASD (skip if frozen)
        if (!movementFrozen) {
            Direction moveDirection = getMostRecentDirection();
            if (moveDirection != null) {
                player.move(moveDirection, map, allTanks, base);
                lastDirection = moveDirection;
                isMoving = true;
            }

            // Check if just stopped moving on ice - trigger sliding
            if (wasMoving && !isMoving && lastDirection != null) {
                player.startSliding(lastDirection, map);
            }
            wasMoving = isMoving;
        }

        handleShooting(player, KeyCode.SPACE, bullets, lasers, soundManager);
    }

    // Player 1 input handler for local multiplayer (WASD + SPACE)
    private void handlePlayer1Input(Tank player, GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        if (!player.isAlive()) return;

        boolean isMoving = false;

        // Movement with WASD only (skip if frozen)
        if (!movementFrozen) {
            Direction moveDirection = getMostRecentPlayer1Direction();
            if (moveDirection != null) {
                player.move(moveDirection, map, allTanks, base);
                lastDirectionPlayer1 = moveDirection;
                isMoving = true;
            }

            // Check if just stopped moving on ice - trigger sliding
            if (wasMovingPlayer1 && !isMoving && lastDirectionPlayer1 != null) {
                player.startSliding(lastDirectionPlayer1, map);
            }
            wasMovingPlayer1 = isMoving;
        }

        handleShooting(player, KeyCode.SPACE, bullets, lasers, soundManager);
    }

    // Player 2 input handler for local multiplayer (Arrows + ENTER)
    private void handlePlayer2Input(Tank player, GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        if (!player.isAlive()) return;

        boolean isMoving = false;

        // Movement with arrow keys only (skip if frozen)
        if (!movementFrozen) {
            Direction moveDirection = getMostRecentPlayer2Direction();
            if (moveDirection != null) {
                player.move(moveDirection, map, allTanks, base);
                lastDirectionPlayer2 = moveDirection;
                isMoving = true;
            }

            // Check if just stopped moving on ice - trigger sliding
            if (wasMovingPlayer2 && !isMoving && lastDirectionPlayer2 != null) {
                player.startSliding(lastDirectionPlayer2, map);
            }
            wasMovingPlayer2 = isMoving;
        }

        handleShooting(player, KeyCode.ENTER, bullets, lasers, soundManager);
    }

    // Get the most recently pressed direction (last in the order list) - for single player
    private Direction getMostRecentDirection() {
        if (directionKeyOrder.isEmpty()) {
            return null;
        }
        // Get the most recent direction key (last in list)
        KeyCode mostRecent = directionKeyOrder.getLast();
        return keyCodeToDirection(mostRecent);
    }

    // Get the most recently pressed direction for Player 1 (WASD only)
    private Direction getMostRecentPlayer1Direction() {
        if (player1DirectionKeyOrder.isEmpty()) {
            return null;
        }
        KeyCode mostRecent = player1DirectionKeyOrder.getLast();
        return keyCodeToPlayer1Direction(mostRecent);
    }

    // Get the most recently pressed direction for Player 2 (arrows only)
    private Direction getMostRecentPlayer2Direction() {
        // Filter directionKeyOrder to only include arrow keys
        for (int i = directionKeyOrder.size() - 1; i >= 0; i--) {
            KeyCode code = directionKeyOrder.get(i);
            if (PLAYER2_DIRECTION_KEYS.contains(code)) {
                return keyCodeToPlayer2Direction(code);
            }
        }
        return null;
    }

    private Direction keyCodeToDirection(KeyCode code) {
        return switch (code) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }

    private Direction keyCodeToPlayer1Direction(KeyCode code) {
        return switch (code) {
            case W -> Direction.UP;
            case S -> Direction.DOWN;
            case A -> Direction.LEFT;
            case D -> Direction.RIGHT;
            default -> null;
        };
    }

    private Direction keyCodeToPlayer2Direction(KeyCode code) {
        return switch (code) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case LEFT -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }

    // Capture input state (for network) - use most recent direction + space + enter
    public PlayerInput capturePlayerInput() {
        Direction mostRecent = getMostRecentDirection();
        return new PlayerInput(
            mostRecent == Direction.UP,
            mostRecent == Direction.DOWN,
            mostRecent == Direction.LEFT,
            mostRecent == Direction.RIGHT,
            pressedKeys.contains(KeyCode.SPACE),
            pressedKeys.contains(KeyCode.ENTER)
        );
    }

    // Check if ENTER is pressed (for life request)
    public boolean isEnterPressed() {
        return pressedKeys.contains(KeyCode.ENTER);
    }

}
