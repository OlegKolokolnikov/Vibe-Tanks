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
    private int tankCollisionCounter; // Count frames colliding with another tank
    private static final int TANK_COLLISION_THRESHOLD = 15; // Frames before avoiding other tank
    private static final double TANK_DETECTION_DISTANCE = 48; // Distance to check for other tanks

    public TankAI(double initialX, double initialY) {
        this.aiMoveCooldown = GameConstants.AI_MOVE_COOLDOWN_BASE;
        this.aiShootCooldown = GameConstants.AI_SHOOT_COOLDOWN_BASE + 30; // Initial offset
        this.lastX = initialX;
        this.lastY = initialY;
        this.stuckCounter = 0;
        this.tankCollisionCounter = 0;
    }

    /**
     * Update AI behavior for an enemy tank.
     * Handles stuck detection, shooting, direction changes, and movement.
     */
    public void update(Tank tank, TankPhysics physics, GameMap map, List<Bullet> bullets,
                       List<Tank> allTanks, Base base, SoundManager soundManager) {
        if (!tank.isAlive()) return;

        // Check if colliding with another tank in current direction
        boolean collidingWithTank = isTankInDirection(tank, tank.getDirection(), allTanks);
        if (collidingWithTank) {
            tankCollisionCounter++;
            // If pushing against another tank for too long, change direction
            if (tankCollisionCounter > TANK_COLLISION_THRESHOLD) {
                Direction avoidDirection = getAvoidDirection(tank, allTanks);
                tank.setDirection(avoidDirection);
                tankCollisionCounter = 0;
                aiMoveCooldown = GameConstants.AI_MOVE_COOLDOWN_BASE / 2 +
                                 GameConstants.RANDOM.nextInt(GameConstants.AI_MOVE_COOLDOWN_RANDOM / 2);
            }
        } else {
            tankCollisionCounter = 0;
        }

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
            Direction decidedDirection = decideDirection(tank, base, allTanks);
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
     * Avoids directions blocked by other tanks.
     */
    private Direction decideDirection(Tank tank, Base base, List<Tank> allTanks) {
        Direction preferred;
        if (GameConstants.RANDOM.nextDouble() < GameConstants.AI_TARGET_BASE_CHANCE) {
            preferred = calculateDirectionTowardsBase(tank, base);
        } else {
            preferred = Direction.values()[GameConstants.RANDOM.nextInt(4)];
        }

        // If preferred direction is blocked by another tank, try alternatives
        if (isTankInDirection(tank, preferred, allTanks)) {
            // Try perpendicular directions first
            Direction[] perpendicular = getPerpendicularDirections(preferred);
            for (Direction dir : perpendicular) {
                if (!isTankInDirection(tank, dir, allTanks)) {
                    return dir;
                }
            }
            // Try opposite direction as last resort
            Direction opposite = getOppositeDirection(preferred);
            if (!isTankInDirection(tank, opposite, allTanks)) {
                return opposite;
            }
        }

        return preferred;
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

    /**
     * Check if there's another tank in the given direction within detection distance.
     */
    private boolean isTankInDirection(Tank tank, Direction direction, List<Tank> allTanks) {
        double tankCenterX = tank.getX() + tank.getSize() / 2.0;
        double tankCenterY = tank.getY() + tank.getSize() / 2.0;

        for (Tank other : allTanks) {
            if (other == tank || !other.isAlive()) continue;

            double otherCenterX = other.getX() + other.getSize() / 2.0;
            double otherCenterY = other.getY() + other.getSize() / 2.0;

            double dx = otherCenterX - tankCenterX;
            double dy = otherCenterY - tankCenterY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > TANK_DETECTION_DISTANCE) continue;

            // Check if other tank is in the specified direction
            boolean inDirection = switch (direction) {
                case UP -> dy < 0 && Math.abs(dx) < tank.getSize();
                case DOWN -> dy > 0 && Math.abs(dx) < tank.getSize();
                case LEFT -> dx < 0 && Math.abs(dy) < tank.getSize();
                case RIGHT -> dx > 0 && Math.abs(dy) < tank.getSize();
            };

            if (inDirection) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a direction that avoids other tanks.
     */
    private Direction getAvoidDirection(Tank tank, List<Tank> allTanks) {
        Direction current = tank.getDirection();

        // Try perpendicular directions first (more likely to break deadlock)
        Direction[] perpendicular = getPerpendicularDirections(current);
        // Shuffle perpendicular to add variety
        if (GameConstants.RANDOM.nextBoolean()) {
            Direction temp = perpendicular[0];
            perpendicular[0] = perpendicular[1];
            perpendicular[1] = temp;
        }

        for (Direction dir : perpendicular) {
            if (!isTankInDirection(tank, dir, allTanks)) {
                return dir;
            }
        }

        // Try opposite direction
        Direction opposite = getOppositeDirection(current);
        if (!isTankInDirection(tank, opposite, allTanks)) {
            return opposite;
        }

        // All directions blocked, pick random perpendicular
        return perpendicular[GameConstants.RANDOM.nextInt(2)];
    }

    /**
     * Get perpendicular directions to the given direction.
     */
    private Direction[] getPerpendicularDirections(Direction dir) {
        return switch (dir) {
            case UP, DOWN -> new Direction[]{Direction.LEFT, Direction.RIGHT};
            case LEFT, RIGHT -> new Direction[]{Direction.UP, Direction.DOWN};
        };
    }

    /**
     * Get opposite direction.
     */
    private Direction getOppositeDirection(Direction dir) {
        return switch (dir) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
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
        this.tankCollisionCounter = 0;
    }
}
