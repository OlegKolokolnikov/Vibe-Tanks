package com.vibetanks.core;

import com.vibetanks.audio.SoundManager;

import java.util.List;

/**
 * Handles enemy tank AI behavior: movement decisions, shooting, and pathfinding.
 * Extracted from Tank.java for better separation of concerns.
 */
public class TankAI {
    private int aiMoveCooldown;
    private int aiShootCooldown;
    private double lastX, lastY; // Track position to detect stuck
    private int stuckCounter; // Count frames stuck

    public TankAI(double initialX, double initialY) {
        this.aiMoveCooldown = GameConstants.AI_MOVE_COOLDOWN_BASE;
        this.aiShootCooldown = GameConstants.AI_SHOOT_COOLDOWN_BASE + 30; // Initial offset
        this.lastX = initialX;
        this.lastY = initialY;
        this.stuckCounter = 0;
    }

    /**
     * Update AI behavior for an enemy tank.
     * Handles stuck detection, shooting, direction changes, and movement.
     */
    public void update(Tank tank, TankPhysics physics, GameMap map, List<Bullet> bullets,
                       List<Tank> allTanks, Base base, SoundManager soundManager) {
        if (!tank.isAlive()) return;

        // Detect if stuck (position hasn't changed)
        Direction newDirection = detectAndHandleStuck(tank);
        if (newDirection != null) {
            tank.setDirection(newDirection);
        }
        lastX = tank.getX();
        lastY = tank.getY();

        // Decrease AI cooldowns
        aiMoveCooldown--;
        aiShootCooldown--;

        // Randomly shoot
        if (aiShootCooldown <= 0) {
            tank.shoot(bullets, soundManager);
            aiShootCooldown = GameConstants.AI_SHOOT_COOLDOWN_BASE +
                              GameConstants.RANDOM.nextInt(GameConstants.AI_SHOOT_COOLDOWN_RANDOM);
        }

        // Change direction occasionally
        if (aiMoveCooldown <= 0) {
            Direction decidedDirection = decideDirection(tank, base);
            tank.setDirection(decidedDirection);
            aiMoveCooldown = GameConstants.AI_MOVE_COOLDOWN_BASE / 2 +
                             GameConstants.RANDOM.nextInt(GameConstants.AI_MOVE_COOLDOWN_RANDOM);
        }

        // Move in current direction
        boolean moved = physics.move(tank, tank.getDirection(), map, allTanks, base);
        if (moved) {
            tank.incrementTrackAnimation();
        }
    }

    /**
     * Detect if tank is stuck and handle it by changing direction.
     * @return new direction if stuck, null otherwise
     */
    private Direction detectAndHandleStuck(Tank tank) {
        if (Math.abs(tank.getX() - lastX) < 0.1 && Math.abs(tank.getY() - lastY) < 0.1) {
            stuckCounter++;
            if (stuckCounter > GameConstants.AI_STUCK_THRESHOLD) {
                Direction[] directions = Direction.values();
                Direction originalDirection = tank.getDirection();
                Direction newDirection = originalDirection;

                for (int i = 0; i < 4; i++) {
                    newDirection = directions[GameConstants.RANDOM.nextInt(4)];
                    if (newDirection != originalDirection) {
                        break;
                    }
                }
                stuckCounter = 0;
                aiMoveCooldown = GameConstants.AI_STUCK_COOLDOWN_BASE +
                                 GameConstants.RANDOM.nextInt(GameConstants.AI_STUCK_COOLDOWN_RANDOM);
                return newDirection;
            }
        } else {
            stuckCounter = 0;
        }
        return null;
    }

    /**
     * Decide which direction to move.
     * AI_TARGET_BASE_CHANCE to move towards base, remainder random.
     */
    private Direction decideDirection(Tank tank, Base base) {
        if (GameConstants.RANDOM.nextDouble() < GameConstants.AI_TARGET_BASE_CHANCE) {
            return calculateDirectionTowardsBase(tank, base);
        } else {
            return Direction.values()[GameConstants.RANDOM.nextInt(4)];
        }
    }

    /**
     * Calculate direction that moves towards the base.
     */
    private Direction calculateDirectionTowardsBase(Tank tank, Base base) {
        double dx = base.getX() - tank.getX();
        double dy = base.getY() - tank.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
    }

    // For network sync
    public void setLastPosition(double x, double y) {
        this.lastX = x;
        this.lastY = y;
    }

    public void resetCooldowns() {
        this.aiMoveCooldown = GameConstants.AI_MOVE_COOLDOWN_BASE;
        this.aiShootCooldown = GameConstants.AI_SHOOT_COOLDOWN_BASE + 30;
        this.stuckCounter = 0;
    }
}
