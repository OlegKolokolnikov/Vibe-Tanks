package com.battlecity;

import java.io.Serializable;

public class PlayerInput implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean up, down, left, right;
    public boolean shoot;
    public boolean requestLife; // Request to take life from teammate

    public PlayerInput() {
        this.up = false;
        this.down = false;
        this.left = false;
        this.right = false;
        this.shoot = false;
        this.requestLife = false;
    }

    public PlayerInput(boolean up, boolean down, boolean left, boolean right, boolean shoot) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.shoot = shoot;
        this.requestLife = false;
    }

    public PlayerInput(boolean up, boolean down, boolean left, boolean right, boolean shoot, boolean requestLife) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.shoot = shoot;
        this.requestLife = requestLife;
    }
}
