package com.vibetanks;

import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputHandler {
    private Set<KeyCode> pressedKeys;
    private List<Tank> playerTanks;
    private Direction lastDirection;
    private boolean wasMoving;
    private GameMap gameMap;

    // All players use: Arrow keys + Space

    public InputHandler(Pane pane, List<Tank> playerTanks) {
        this.playerTanks = playerTanks;
        this.pressedKeys = new HashSet<>();
        this.wasMoving = false;

        pane.setOnKeyPressed(event -> {
            pressedKeys.add(event.getCode());
            event.consume();
        });

        pane.setOnKeyReleased(event -> {
            pressedKeys.remove(event.getCode());
            event.consume();
        });

        // Request focus so key events work
        pane.setFocusTraversable(true);
        pane.requestFocus();
    }

    public void handleInput(GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base) {
        handleInput(map, bullets, soundManager, allTanks, base, false);
    }

    public void handleInput(GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        // Handle single local player with arrow keys + space
        if (playerTanks.size() >= 1) {
            handlePlayerInput(playerTanks.get(0), map, bullets, soundManager, allTanks, base, movementFrozen);
        }
    }

    private void handlePlayerInput(Tank player, GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base, boolean movementFrozen) {
        if (!player.isAlive()) return;

        boolean isMoving = false;

        // Movement with arrow keys or WASD (skip if frozen)
        if (!movementFrozen) {
            if (pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W)) {
                player.move(Direction.UP, map, allTanks, base);
                lastDirection = Direction.UP;
                isMoving = true;
            } else if (pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S)) {
                player.move(Direction.DOWN, map, allTanks, base);
                lastDirection = Direction.DOWN;
                isMoving = true;
            } else if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) {
                player.move(Direction.LEFT, map, allTanks, base);
                lastDirection = Direction.LEFT;
                isMoving = true;
            } else if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) {
                player.move(Direction.RIGHT, map, allTanks, base);
                lastDirection = Direction.RIGHT;
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
            player.shoot(bullets, soundManager);
        }
    }

    // Capture input state (for network) - arrow keys/WASD + space + enter
    public PlayerInput capturePlayerInput() {
        return new PlayerInput(
            pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W),
            pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S),
            pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A),
            pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D),
            pressedKeys.contains(KeyCode.SPACE),
            pressedKeys.contains(KeyCode.ENTER)
        );
    }

    // Check if ENTER is pressed (for life request)
    public boolean isEnterPressed() {
        return pressedKeys.contains(KeyCode.ENTER);
    }
}
