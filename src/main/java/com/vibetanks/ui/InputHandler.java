package com.vibetanks.ui;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.Base;
import com.vibetanks.core.Bullet;
import com.vibetanks.core.Direction;
import com.vibetanks.core.GameMap;
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
    private LinkedList<KeyCode> directionKeyOrder; // Track order of direction key presses
    private List<Tank> playerTanks;
    private Direction lastDirection;
    private boolean wasMoving;
    private GameMap gameMap;

    // Direction keys for priority tracking
    private static final Set<KeyCode> DIRECTION_KEYS = Set.of(
        KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT,
        KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D
    );

    // All players use: Arrow keys + Space

    public InputHandler(Pane pane, List<Tank> playerTanks) {
        this.playerTanks = playerTanks;
        this.pressedKeys = new HashSet<>();
        this.directionKeyOrder = new LinkedList<>();
        this.wasMoving = false;

        pane.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            pressedKeys.add(code);
            // Track direction key order - add to end (most recent)
            if (DIRECTION_KEYS.contains(code)) {
                directionKeyOrder.remove(code); // Remove if already present
                directionKeyOrder.addLast(code); // Add as most recent
            }
            event.consume();
        });

        pane.setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            pressedKeys.remove(code);
            directionKeyOrder.remove(code);
            event.consume();
        });

        // Request focus so key events work
        pane.setFocusTraversable(true);
        pane.requestFocus();
    }

    public void handleInput(GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base) {
        handleInput(map, bullets, lasers, soundManager, allTanks, base, false);
    }

    public void handleInput(GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        // Handle single local player with arrow keys + space
        if (playerTanks.size() >= 1) {
            handlePlayerInput(playerTanks.get(0), map, bullets, lasers, soundManager, allTanks, base, movementFrozen);
        }
    }

    private void handlePlayerInput(Tank player, GameMap map, List<Bullet> bullets, List<Laser> lasers, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        if (!player.isAlive()) return;

        boolean isMoving = false;

        // Movement with arrow keys or WASD (skip if frozen)
        // Use most recently pressed direction key for priority
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

        // Shooting with space (allowed even when frozen)
        if (pressedKeys.contains(KeyCode.SPACE)) {
            // Use laser if tank has laser power-up, otherwise use normal bullets
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

    // Get the most recently pressed direction (last in the order list)
    private Direction getMostRecentDirection() {
        if (directionKeyOrder.isEmpty()) {
            return null;
        }
        // Get the most recent direction key (last in list)
        KeyCode mostRecent = directionKeyOrder.getLast();
        return keyCodeToDirection(mostRecent);
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
