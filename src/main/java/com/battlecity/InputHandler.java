package com.battlecity;

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
        // Handle single local player with arrow keys + space
        if (playerTanks.size() >= 1) {
            handlePlayerInput(playerTanks.get(0), map, bullets, soundManager, allTanks, base);
        }
    }

    private void handlePlayerInput(Tank player, GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base) {
        if (!player.isAlive()) return;

        boolean isMoving = false;
        // Movement with arrow keys
        if (pressedKeys.contains(KeyCode.UP)) {
            player.move(Direction.UP, map, allTanks, base);
            lastDirection = Direction.UP;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.DOWN)) {
            player.move(Direction.DOWN, map, allTanks, base);
            lastDirection = Direction.DOWN;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.LEFT)) {
            player.move(Direction.LEFT, map, allTanks, base);
            lastDirection = Direction.LEFT;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.RIGHT)) {
            player.move(Direction.RIGHT, map, allTanks, base);
            lastDirection = Direction.RIGHT;
            isMoving = true;
        }

        // Check if just stopped moving on ice - trigger sliding
        if (wasMoving && !isMoving && lastDirection != null) {
            player.startSliding(lastDirection, map);
        }
        wasMoving = isMoving;

        // Shooting with space
        if (pressedKeys.contains(KeyCode.SPACE)) {
            player.shoot(bullets, soundManager);
        }
    }

    // Capture input state (for network) - arrow keys + space
    public PlayerInput capturePlayerInput() {
        return new PlayerInput(
            pressedKeys.contains(KeyCode.UP),
            pressedKeys.contains(KeyCode.DOWN),
            pressedKeys.contains(KeyCode.LEFT),
            pressedKeys.contains(KeyCode.RIGHT),
            pressedKeys.contains(KeyCode.SPACE)
        );
    }
}
