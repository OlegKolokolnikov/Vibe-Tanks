package com.battlecity;

import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputHandler {
    private Set<KeyCode> pressedKeys;
    private List<Tank> playerTanks;
    private Direction lastP1Direction;
    private Direction lastP2Direction;
    private boolean wasP1Moving;
    private boolean wasP2Moving;
    private GameMap gameMap;

    // Player 1 controls: WASD + Space
    // Player 2 controls: Arrow keys + Enter

    public InputHandler(Pane pane, List<Tank> playerTanks) {
        this.playerTanks = playerTanks;
        this.pressedKeys = new HashSet<>();
        this.wasP1Moving = false;
        this.wasP2Moving = false;

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

    public void handleInput(GameMap map, List<Bullet> bullets, SoundManager soundManager) {
        if (playerTanks.size() >= 1) {
            handlePlayer1Input(playerTanks.get(0), map, bullets, soundManager);
        }
        if (playerTanks.size() >= 2) {
            handlePlayer2Input(playerTanks.get(1), map, bullets, soundManager);
        }
    }

    private void handlePlayer1Input(Tank player, GameMap map, List<Bullet> bullets, SoundManager soundManager) {
        if (!player.isAlive()) return;

        boolean isMoving = false;
        // Movement
        if (pressedKeys.contains(KeyCode.W)) {
            player.move(Direction.UP, map);
            lastP1Direction = Direction.UP;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.S)) {
            player.move(Direction.DOWN, map);
            lastP1Direction = Direction.DOWN;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.A)) {
            player.move(Direction.LEFT, map);
            lastP1Direction = Direction.LEFT;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.D)) {
            player.move(Direction.RIGHT, map);
            lastP1Direction = Direction.RIGHT;
            isMoving = true;
        }

        // Check if just stopped moving on ice - trigger sliding
        if (wasP1Moving && !isMoving && lastP1Direction != null) {
            player.startSliding(lastP1Direction, map);
        }
        wasP1Moving = isMoving;

        // Shooting
        if (pressedKeys.contains(KeyCode.SPACE)) {
            player.shoot(bullets, soundManager);
        }
    }

    private void handlePlayer2Input(Tank player, GameMap map, List<Bullet> bullets, SoundManager soundManager) {
        if (!player.isAlive()) return;

        boolean isMoving = false;
        // Movement
        if (pressedKeys.contains(KeyCode.UP)) {
            player.move(Direction.UP, map);
            lastP2Direction = Direction.UP;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.DOWN)) {
            player.move(Direction.DOWN, map);
            lastP2Direction = Direction.DOWN;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.LEFT)) {
            player.move(Direction.LEFT, map);
            lastP2Direction = Direction.LEFT;
            isMoving = true;
        } else if (pressedKeys.contains(KeyCode.RIGHT)) {
            player.move(Direction.RIGHT, map);
            lastP2Direction = Direction.RIGHT;
            isMoving = true;
        }

        // Check if just stopped moving on ice - trigger sliding
        if (wasP2Moving && !isMoving && lastP2Direction != null) {
            player.startSliding(lastP2Direction, map);
        }
        wasP2Moving = isMoving;

        // Shooting
        if (pressedKeys.contains(KeyCode.ENTER)) {
            player.shoot(bullets, soundManager);
        }
    }
}
