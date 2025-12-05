package com.vibetanks;

import java.io.Serializable;

public class PlayerInput implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean up, down, left, right;
    public boolean shoot;
    public boolean requestLife; // Request to take life from teammate
    public boolean requestNextLevel; // Request to start next level (after victory)
    public boolean requestRestart; // Request to restart current level (after game over)

    // Client sends its position to host (client-authoritative movement)
    public double posX, posY;
    public int direction; // Direction ordinal

    // Client sends its nickname to host
    public String nickname;

    public PlayerInput() {
        this.up = false;
        this.down = false;
        this.left = false;
        this.right = false;
        this.shoot = false;
        this.requestLife = false;
        this.requestNextLevel = false;
        this.requestRestart = false;
        this.posX = 0;
        this.posY = 0;
        this.direction = 0;
        this.nickname = null;
    }

    public PlayerInput(boolean up, boolean down, boolean left, boolean right, boolean shoot) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.shoot = shoot;
        this.requestLife = false;
        this.posX = 0;
        this.posY = 0;
        this.direction = 0;
    }

    public PlayerInput(boolean up, boolean down, boolean left, boolean right, boolean shoot, boolean requestLife) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.shoot = shoot;
        this.requestLife = requestLife;
        this.posX = 0;
        this.posY = 0;
        this.direction = 0;
    }
}
