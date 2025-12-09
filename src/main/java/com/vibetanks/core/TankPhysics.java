package com.vibetanks.core;

import java.util.List;

/**
 * Handles tank physics: movement, collision detection, wraparound, and ice sliding.
 * Extracted from Tank.java for better separation of concerns.
 */
public class TankPhysics {
    private static final double SPEED = 2.0;
    private static final double SLIDE_DISTANCE = 32.0; // One tile

    // Ice sliding state
    private boolean isSliding;
    private Direction slidingDirection;
    private double slideDistance;

    public TankPhysics() {
        this.isSliding = false;
        this.slideDistance = 0;
    }

    /**
     * Update ice sliding physics.
     * @return true if the tank moved during sliding
     */
    public boolean updateSliding(Tank tank, GameMap map, List<Tank> allTanks, Base base) {
        if (!isSliding || slideDistance <= 0) {
            return false;
        }

        double globalSpeedMult = tank.isPlayer()
            ? GameSettings.getEffectivePlayerSpeed()
            : GameSettings.getEffectiveEnemySpeed();
        double slideSpeed = SPEED * (tank.getSpeedMultiplier() + tank.getTempSpeedBoost()) * globalSpeedMult * 2.0;
        double slideStep = Math.min(slideSpeed, slideDistance);

        double newX = tank.getX() + slidingDirection.getDx() * slideStep;
        double newY = tank.getY() + slidingDirection.getDy() * slideStep;

        int mapWidth = map.getWidth() * 32;
        int mapHeight = map.getHeight() * 32;
        int tankSize = tank.getSize();

        // Handle wraparound during sliding
        WrapResult wrapResult = handleWraparound(newX, newY, tank.getX(), tank.getY(),
            tankSize, map, mapWidth, mapHeight);

        if (!wrapResult.canMove) {
            stopSliding();
            return false;
        }

        newX = wrapResult.newX;
        newY = wrapResult.newY;

        // Check collision with other tanks
        if (checkTankCollisions(newX, newY, tankSize, tank, allTanks)) {
            stopSliding();
            return false;
        }

        // Check collision with base
        if (base.isAlive() && checkCollision(newX, newY, base.getX(), base.getY(), tankSize, 32)) {
            stopSliding();
            return false;
        }

        // Check collision with map tiles
        if (map.checkTankCollision(newX, newY, tankSize, tank.canSwim())) {
            stopSliding();
            return false;
        }

        // Apply movement
        tank.setPosition(newX, newY);
        slideDistance -= slideStep;

        if (slideDistance <= 0) {
            isSliding = false;
        }

        return true;
    }

    /**
     * Move tank in the given direction.
     * @return true if the tank moved
     */
    public boolean move(Tank tank, Direction direction, GameMap map, List<Tank> otherTanks, Base base) {
        if (!tank.isAlive()) return false;

        double speed = calculateSpeed(tank, map);
        double newX = tank.getX() + direction.getDx() * speed;
        double newY = tank.getY() + direction.getDy() * speed;

        int mapWidth = map.getWidth() * 32;
        int mapHeight = map.getHeight() * 32;
        int tankSize = tank.getSize();

        // Handle wraparound
        WrapResult wrapResult = handleWraparound(newX, newY, tank.getX(), tank.getY(),
            tankSize, map, mapWidth, mapHeight);

        if (!wrapResult.canMove) {
            return false;
        }

        newX = wrapResult.newX;
        newY = wrapResult.newY;

        // Check collision with other tanks (with BOSS special handling)
        if (!handleTankCollisions(tank, newX, newY, otherTanks)) {
            return false;
        }

        // Check collision with base (with BOSS special handling)
        if (!handleBaseCollision(tank, newX, newY, base)) {
            return false;
        }

        // Handle movement (with BOSS special tile destruction)
        return handleTileMovement(tank, newX, newY, map);
    }

    /**
     * Start sliding on ice.
     */
    public void startSliding(Direction direction, GameMap map, double tankX, double tankY, int tankSize) {
        if (isOnIce(map, tankX, tankY, tankSize)) {
            isSliding = true;
            slidingDirection = direction;
            slideDistance = SLIDE_DISTANCE;
        }
    }

    public void stopSliding() {
        isSliding = false;
        slideDistance = 0;
    }

    public boolean isSliding() {
        return isSliding;
    }

    // Calculate movement speed with all modifiers
    private double calculateSpeed(Tank tank, GameMap map) {
        double speed = SPEED * (tank.getSpeedMultiplier() + tank.getTempSpeedBoost());

        // Apply global speed settings
        if (tank.isPlayer()) {
            speed *= GameSettings.getEffectivePlayerSpeed();
        } else {
            speed *= GameSettings.getEffectiveEnemySpeed();
        }

        // Apply 2x speed when on ice
        if (isOnIce(map, tank.getX(), tank.getY(), tank.getSize())) {
            speed *= 2.0;
        }

        return speed;
    }

    // Check if tank center is on ice
    public boolean isOnIce(GameMap map, double x, double y, int size) {
        int centerX = (int) ((x + size / 2) / 32);
        int centerY = (int) ((y + size / 2) / 32);
        return map.getTile(centerY, centerX) == GameMap.TileType.ICE;
    }

    // Handle map boundary wraparound
    private WrapResult handleWraparound(double newX, double newY, double oldX, double oldY,
                                         int tankSize, GameMap map, int mapWidth, int mapHeight) {
        boolean canMove = true;

        // Left edge wraparound
        if (newX < 0) {
            int row = (int)((oldY + tankSize/2) / 32);
            if (map.getTile(row, 0) == GameMap.TileType.EMPTY) {
                newX = mapWidth - tankSize;
            } else {
                canMove = false;
            }
        }

        // Right edge wraparound
        if (canMove && newX + tankSize > mapWidth) {
            int row = (int)((oldY + tankSize/2) / 32);
            if (map.getTile(row, map.getWidth() - 1) == GameMap.TileType.EMPTY) {
                newX = 0;
            } else {
                canMove = false;
            }
        }

        // Top edge wraparound
        if (canMove && newY < 0) {
            int col = (int)((oldX + tankSize/2) / 32);
            if (map.getTile(0, col) == GameMap.TileType.EMPTY) {
                newY = mapHeight - tankSize;
            } else {
                canMove = false;
            }
        }

        // Bottom edge wraparound
        if (canMove && newY + tankSize > mapHeight) {
            int col = (int)((oldX + tankSize/2) / 32);
            if (map.getTile(map.getHeight() - 1, col) == GameMap.TileType.EMPTY) {
                newY = 0;
            } else {
                canMove = false;
            }
        }

        return new WrapResult(newX, newY, canMove);
    }

    // Handle collisions with other tanks
    private boolean handleTankCollisions(Tank tank, double newX, double newY, List<Tank> otherTanks) {
        int tankSize = tank.getSize();

        for (Tank other : otherTanks) {
            if (other != tank && other.isAlive()) {
                if (checkCollision(newX, newY, other.getX(), other.getY(), tankSize, other.getSize())) {
                    // BOSS instantly kills any tank it touches
                    if (tank.getEnemyType() == Tank.EnemyType.BOSS) {
                        other.instantKill();
                        // Continue moving - BOSS doesn't stop for tanks
                    } else {
                        return false; // Can't move through other tanks
                    }
                }
            }
        }
        return true;
    }

    // Check if any tank collision exists (for sliding)
    private boolean checkTankCollisions(double newX, double newY, int tankSize, Tank self, List<Tank> allTanks) {
        for (Tank other : allTanks) {
            if (other != self && other.isAlive()) {
                if (checkCollision(newX, newY, other.getX(), other.getY(), tankSize, other.getSize())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Handle collision with base
    private boolean handleBaseCollision(Tank tank, double newX, double newY, Base base) {
        if (!base.isAlive()) return true;

        if (checkCollision(newX, newY, base.getX(), base.getY(), tank.getSize(), 32)) {
            // BOSS destroys the base on contact
            if (tank.getEnemyType() == Tank.EnemyType.BOSS) {
                base.destroy();
                return true; // Continue moving
            } else {
                return false; // Can't move through base
            }
        }
        return true;
    }

    // Handle tile-based movement (with BOSS destruction)
    private boolean handleTileMovement(Tank tank, double newX, double newY, GameMap map) {
        int tankSize = tank.getSize();

        if (tank.getEnemyType() == Tank.EnemyType.BOSS) {
            // Check if steel is in the way
            if (!hasBlockingSteel(map, newX, newY, tankSize)) {
                destroyTilesInPath(map, newX, newY, tankSize);
                tank.setPosition(newX, newY);
                return true;
            }
            return false;
        } else {
            // Check collision with map tiles
            if (!map.checkTankCollision(newX, newY, tankSize, tank.canSwim())) {
                tank.setPosition(newX, newY);
                return true;
            }
            return false;
        }
    }

    // Check if steel blocks the path for BOSS tank
    private boolean hasBlockingSteel(GameMap map, double newX, double newY, int tankSize) {
        int startCol = (int) newX / 32;
        int endCol = (int) (newX + tankSize - 1) / 32;
        int startRow = (int) newY / 32;
        int endRow = (int) (newY + tankSize - 1) / 32;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (map.getTile(row, col) == GameMap.TileType.STEEL) {
                    return true;
                }
            }
        }
        return false;
    }

    // BOSS tank destroys brick and trees it touches (not steel)
    private void destroyTilesInPath(GameMap map, double newX, double newY, int tankSize) {
        int startCol = (int) newX / 32;
        int endCol = (int) (newX + tankSize - 1) / 32;
        int startRow = (int) newY / 32;
        int endRow = (int) (newY + tankSize - 1) / 32;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                GameMap.TileType tile = map.getTile(row, col);
                if (tile == GameMap.TileType.BRICK || tile == GameMap.TileType.TREES) {
                    map.setTile(row, col, GameMap.TileType.EMPTY);
                }
            }
        }
    }

    // AABB collision check
    public static boolean checkCollision(double x1, double y1, double x2, double y2, int size1, int size2) {
        return x1 < x2 + size2 &&
               x1 + size1 > x2 &&
               y1 < y2 + size2 &&
               y1 + size1 > y2;
    }

    // Result of wraparound calculation
    private static class WrapResult {
        final double newX;
        final double newY;
        final boolean canMove;

        WrapResult(double newX, double newY, boolean canMove) {
            this.newX = newX;
            this.newY = newY;
            this.canMove = canMove;
        }
    }
}
