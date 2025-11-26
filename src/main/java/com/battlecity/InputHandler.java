package com.battlecity;

import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputHandler {
    private Set<KeyCode> pressedKeys;
    private List<Tank> playerTanks;

    // Player 1 controls: WASD + Space
    // Player 2 controls: Arrow keys + Enter

    public InputHandler(Pane pane, List<Tank> playerTanks) {
        this.playerTanks = playerTanks;
        this.pressedKeys = new HashSet<>();

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

        // Movement
        if (pressedKeys.contains(KeyCode.W)) {
            player.move(Direction.UP, map);
        } else if (pressedKeys.contains(KeyCode.S)) {
            player.move(Direction.DOWN, map);
        } else if (pressedKeys.contains(KeyCode.A)) {
            player.move(Direction.LEFT, map);
        } else if (pressedKeys.contains(KeyCode.D)) {
            player.move(Direction.RIGHT, map);
        }

        // Shooting
        if (pressedKeys.contains(KeyCode.SPACE)) {
            player.shoot(bullets, soundManager);
        }
    }

    private void handlePlayer2Input(Tank player, GameMap map, List<Bullet> bullets, SoundManager soundManager) {
        if (!player.isAlive()) return;

        // Movement
        if (pressedKeys.contains(KeyCode.UP)) {
            player.move(Direction.UP, map);
        } else if (pressedKeys.contains(KeyCode.DOWN)) {
            player.move(Direction.DOWN, map);
        } else if (pressedKeys.contains(KeyCode.LEFT)) {
            player.move(Direction.LEFT, map);
        } else if (pressedKeys.contains(KeyCode.RIGHT)) {
            player.move(Direction.RIGHT, map);
        }

        // Shooting
        if (pressedKeys.contains(KeyCode.ENTER)) {
            player.shoot(bullets, soundManager);
        }
    }
}
