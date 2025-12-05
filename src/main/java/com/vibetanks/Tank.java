package com.vibetanks;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

public class Tank {
    public enum EnemyType {
        REGULAR,    // 1 shot, normal speed
        ARMORED,    // 2 shots, normal speed, bigger
        FAST,       // 1 shot, faster
        POWER,      // 2 shots, drops power-up on each hit, rainbow colors
        HEAVY,      // 3 shots, fast, black with white dot
        BOSS        // 12 shots, fast, black, 4x size, can destroy iron
    }

    private static final int BASE_SIZE = 28;
    private int size = BASE_SIZE; // Instance variable for tank size
    private static final double SPEED = 2.0;
    private static final int SHOOT_COOLDOWN = 30; // frames

    private double x;
    private double y;
    private Direction direction;
    private boolean isPlayer;
    private int playerNumber; // 1 or 2
    private EnemyType enemyType; // For enemy tanks
    private boolean alive;
    private int lives;
    private int health;
    private int maxHealth;

    private int shootCooldown;
    private int activeBulletCount; // Track how many bullets this tank has active
    private boolean hasShield;
    private int shieldDuration;
    private boolean hasPauseShield; // Shield while player is paused (multiplayer)
    private double speedMultiplier; // Base is 1.0, each CAR adds 0.3
    private double tempSpeedBoost; // Temporary speed boost from team CAR pickup
    private int bulletPower; // 1 = normal, 2 = can break steel
    private boolean canSwim; // SHIP power-up
    private boolean canDestroyTrees; // SAW power-up
    private int machinegunCount; // MACHINEGUN power-up (each adds one extra bullet, max 5)
    private int shootCooldownReduction; // STAR power-up (each star reduces cooldown)
    private int laserDuration; // LASER power-up duration (30 seconds = 1800 frames at 60 FPS)
    private static final int LASER_DURATION = 1800; // 30 seconds at 60 FPS
    private static final int LASER_COOLDOWN = 10; // Very fast shooting (6 shots per second)

    private Random random;

    // AI variables
    private int aiMoveCooldown;
    private int aiShootCooldown;
    private double lastX, lastY; // Track position to detect stuck
    private int stuckCounter; // Count frames stuck

    // Ice sliding variables
    private boolean isSliding;
    private Direction slidingDirection;
    private double slideDistance;

    // Track animation
    private int trackAnimationFrame;
    private boolean isMoving;
    private static final double SLIDE_DISTANCE = 32.0; // One tile

    public Tank(double x, double y, Direction direction, boolean isPlayer, int playerNumber) {
        this(x, y, direction, isPlayer, playerNumber, EnemyType.REGULAR);
    }

    public Tank(double x, double y, Direction direction, boolean isPlayer, int playerNumber, EnemyType enemyType) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.isPlayer = isPlayer;
        this.playerNumber = playerNumber;
        this.enemyType = enemyType;
        this.alive = true;
        this.lives = isPlayer ? 3 : 1;
        this.speedMultiplier = 1.0; // Default speed for all tanks

        // Set health and speed based on enemy type
        if (!isPlayer) {
            switch (enemyType) {
                case REGULAR -> this.maxHealth = 1;
                case ARMORED -> this.maxHealth = 2;
                case FAST -> {
                    this.maxHealth = 1;
                    this.speedMultiplier = 1.5;
                }
                case POWER -> this.maxHealth = 2;
                case HEAVY -> {
                    this.maxHealth = 3;
                    this.speedMultiplier = 1.5;
                    this.bulletPower = 2; // Can destroy iron/steel
                }
                case BOSS -> {
                    this.maxHealth = 12;
                    this.speedMultiplier = 1.5;
                    this.bulletPower = 2; // Can destroy iron/steel
                    this.size = BASE_SIZE * 4; // 4x bigger
                    this.canSwim = true; // BOSS can swim without SHIP
                    this.canDestroyTrees = true; // BOSS destroys everything
                }
            }
        } else {
            this.maxHealth = 1;
        }

        this.health = maxHealth;
        this.shootCooldown = 0;
        this.activeBulletCount = 0;
        this.hasShield = isPlayer; // Players start with shield
        this.shieldDuration = isPlayer ? 180 : 0; // 3 seconds
        if (isPlayer) {
            this.speedMultiplier = 1.0;
        }
        this.bulletPower = 1;
        this.canSwim = false;
        this.canDestroyTrees = false;
        this.machinegunCount = 0;
        this.shootCooldownReduction = 0;
        this.random = new Random();
        this.aiMoveCooldown = 60;
        this.aiShootCooldown = 90;
        this.lastX = x;
        this.lastY = y;
        this.stuckCounter = 0;
        this.isSliding = false;
        this.slideDistance = 0;
    }

    public void update(GameMap map, List<Bullet> bullets, SoundManager soundManager, List<Tank> allTanks, Base base) {
        if (!alive) return;

        // Update cooldowns
        if (shootCooldown > 0) shootCooldown--;
        if (shieldDuration > 0) {
            shieldDuration--;
            if (shieldDuration == 0) hasShield = false;
        }
        if (laserDuration > 0) {
            laserDuration--;
        }

        // Handle ice sliding
        if (isSliding && slideDistance > 0) {
            double globalSpeedMult = isPlayer ? GameSettings.getPlayerSpeedMultiplier() : GameSettings.getEnemySpeedMultiplier();
            double slideSpeed = SPEED * (speedMultiplier + tempSpeedBoost) * globalSpeedMult * 2.0; // Same speed as moving on ice
            double slideStep = Math.min(slideSpeed, slideDistance);

            double newX = x + slidingDirection.getDx() * slideStep;
            double newY = y + slidingDirection.getDy() * slideStep;

            // Check boundaries and handle wraparound during sliding
            int mapWidth = map.getWidth() * 32;
            int mapHeight = map.getHeight() * 32;
            boolean canSlide = true;

            // Left edge wraparound
            if (newX < 0) {
                int row = (int)((y + size/2) / 32);
                if (map.getTile(row, 0) == GameMap.TileType.EMPTY) {
                    newX = mapWidth - size;
                } else {
                    canSlide = false;
                }
            }

            // Right edge wraparound
            if (newX + size > mapWidth) {
                int row = (int)((y + size/2) / 32);
                if (map.getTile(row, map.getWidth() - 1) == GameMap.TileType.EMPTY) {
                    newX = 0;
                } else {
                    canSlide = false;
                }
            }

            // Top edge wraparound
            if (newY < 0) {
                int col = (int)((x + size/2) / 32);
                if (map.getTile(0, col) == GameMap.TileType.EMPTY) {
                    newY = mapHeight - size;
                } else {
                    canSlide = false;
                }
            }

            // Bottom edge wraparound
            if (newY + size > mapHeight) {
                int col = (int)((x + size/2) / 32);
                if (map.getTile(map.getHeight() - 1, col) == GameMap.TileType.EMPTY) {
                    newY = 0;
                } else {
                    canSlide = false;
                }
            }

            // Check collision with other tanks
            if (canSlide) {
                for (Tank other : allTanks) {
                    if (other != this && other.isAlive()) {
                        if (checkCollision(newX, newY, other.x, other.y, size, other.size)) {
                            canSlide = false;
                            break;
                        }
                    }
                }
            }

            // Check collision with base
            if (canSlide && base.isAlive()) {
                if (checkCollision(newX, newY, base.getX(), base.getY(), size, 32)) {
                    canSlide = false;
                }
            }

            // Check collision with map tiles
            if (canSlide && map.checkTankCollision(newX, newY, size, canSwim)) {
                canSlide = false;
            }

            if (canSlide) {
                x = newX;
                y = newY;
                slideDistance -= slideStep;
            } else {
                // Stop sliding if hit obstacle
                isSliding = false;
                slideDistance = 0;
            }

            if (slideDistance <= 0) {
                isSliding = false;
            }
        }
    }

    public void move(Direction newDirection, GameMap map, List<Tank> otherTanks, Base base) {
        if (!alive) return;

        this.direction = newDirection;
        double speed = SPEED * (speedMultiplier + tempSpeedBoost);

        // Apply global speed settings
        if (isPlayer) {
            speed *= GameSettings.getPlayerSpeedMultiplier();
        } else {
            speed *= GameSettings.getEnemySpeedMultiplier();
        }

        // Apply 2x speed when on ice
        if (isOnIce(map)) {
            speed *= 2.0;
        }

        double newX = x + direction.getDx() * speed;
        double newY = y + direction.getDy() * speed;

        // Check map boundaries and handle wraparound through destroyed borders
        int mapWidth = map.getWidth() * 32;
        int mapHeight = map.getHeight() * 32;

        // Left edge wraparound
        if (newX < 0) {
            // Check if left border column (col 0) is destroyed at tank's row
            int row = (int)((y + size/2) / 32);
            if (map.getTile(row, 0) == GameMap.TileType.EMPTY) {
                newX = mapWidth - size; // Wrap to right edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Right edge wraparound
        if (newX + size > mapWidth) {
            // Check if right border column (col 25) is destroyed at tank's row
            int row = (int)((y + size/2) / 32);
            if (map.getTile(row, map.getWidth() - 1) == GameMap.TileType.EMPTY) {
                newX = 0; // Wrap to left edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Top edge wraparound
        if (newY < 0) {
            // Check if top border row (row 0) is destroyed at tank's column
            int col = (int)((x + size/2) / 32);
            if (map.getTile(0, col) == GameMap.TileType.EMPTY) {
                newY = mapHeight - size; // Wrap to bottom edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Bottom edge wraparound
        if (newY + size > mapHeight) {
            // Check if bottom border row (row 25) is destroyed at tank's column
            int col = (int)((x + size/2) / 32);
            if (map.getTile(map.getHeight() - 1, col) == GameMap.TileType.EMPTY) {
                newY = 0; // Wrap to top edge
            } else {
                return; // Can't move through intact border
            }
        }

        // Check collision with other tanks
        for (Tank other : otherTanks) {
            if (other != this && other.isAlive()) {
                if (checkCollision(newX, newY, other.x, other.y, size, other.size)) {
                    // BOSS instantly kills any tank it touches (ignores shields)
                    if (enemyType == EnemyType.BOSS) {
                        other.instantKill();
                        // Continue moving - BOSS doesn't stop for tanks
                    } else {
                        return; // Can't move through other tanks
                    }
                }
            }
        }

        // Check collision with base (only if base is still alive)
        if (base.isAlive()) {
            if (checkCollision(newX, newY, base.getX(), base.getY(), size, 32)) {
                // BOSS destroys the base on contact
                if (enemyType == EnemyType.BOSS) {
                    base.destroy();
                    // Continue moving
                } else {
                    return; // Can't move through base
                }
            }
        }

        // BOSS tank destroys brick/trees but is blocked by steel
        if (enemyType == EnemyType.BOSS) {
            // Check if steel is in the way
            if (!hasBlockingSteel(map, newX, newY)) {
                destroyTilesInPath(map, newX, newY);
                x = newX;
                y = newY;
                isMoving = true;
                trackAnimationFrame++;
            }
        } else {
            // Check collision with map tiles (pass canSwim for SHIP power-up)
            if (!map.checkTankCollision(newX, newY, size, canSwim)) {
                x = newX;
                y = newY;
                // Animate tracks when moving
                isMoving = true;
                trackAnimationFrame++;
            }
        }
    }

    // Check if steel blocks the path for BOSS tank
    private boolean hasBlockingSteel(GameMap map, double newX, double newY) {
        int startCol = (int) newX / 32;
        int endCol = (int) (newX + size - 1) / 32;
        int startRow = (int) newY / 32;
        int endRow = (int) (newY + size - 1) / 32;

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
    private void destroyTilesInPath(GameMap map, double newX, double newY) {
        int startCol = (int) newX / 32;
        int endCol = (int) (newX + size - 1) / 32;
        int startRow = (int) newY / 32;
        int endRow = (int) (newY + size - 1) / 32;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                GameMap.TileType tile = map.getTile(row, col);
                // Destroy brick and trees (not steel)
                if (tile == GameMap.TileType.BRICK ||
                    tile == GameMap.TileType.TREES) {
                    map.setTile(row, col, GameMap.TileType.EMPTY);
                }
            }
        }
    }

    private boolean checkCollision(double x1, double y1, double x2, double y2, int size1, int size2) {
        return x1 < x2 + size2 &&
               x1 + size1 > x2 &&
               y1 < y2 + size2 &&
               y1 + size1 > y2;
    }

    private boolean isOnIce(GameMap map) {
        // Check if center of tank is on ice
        int centerX = (int) ((x + size / 2) / 32);
        int centerY = (int) ((y + size / 2) / 32);
        return map.getTile(centerY, centerX) == GameMap.TileType.ICE;
    }

    public void startSliding(Direction direction, GameMap map) {
        // Start sliding only if on ice
        if (isOnIce(map)) {
            isSliding = true;
            slidingDirection = direction;
            slideDistance = SLIDE_DISTANCE;
        }
    }

    public void shoot(List<Bullet> bullets, SoundManager soundManager) {
        // Can shoot if: cooldown is 0 OR no active bullets from this tank
        if (!alive || (shootCooldown > 0 && activeBulletCount > 0)) return;

        // BOSS tanks have bigger bullets (4x size = 32 pixels)
        int bulletSize = (enemyType == EnemyType.BOSS) ? 32 : 8;

        double bulletX = x + size / 2.0 - bulletSize / 2.0;
        double bulletY = y + size / 2.0 - bulletSize / 2.0;

        // Adjust bullet spawn position based on direction
        switch (direction) {
            case UP -> bulletY = y - bulletSize;
            case DOWN -> bulletY = y + size;
            case LEFT -> bulletX = x - bulletSize;
            case RIGHT -> bulletX = x + size;
        }

        // Calculate number of bullets to fire (1 base + machinegunCount, max 5)
        int bulletCount = Math.min(1 + machinegunCount, 5);
        double bulletSpacing = bulletSize * 3.0; // 3 bullets' worth of space

        // Fire bullets with spacing
        for (int i = 0; i < bulletCount; i++) {
            double offsetX = bulletX;
            double offsetY = bulletY;

            // Add spacing between bullets based on direction
            double totalSpacing = i * bulletSpacing;
            switch (direction) {
                case UP -> offsetY -= totalSpacing;
                case DOWN -> offsetY += totalSpacing;
                case LEFT -> offsetX -= totalSpacing;
                case RIGHT -> offsetX += totalSpacing;
            }

            bullets.add(new Bullet(offsetX, offsetY, direction, !isPlayer, bulletPower, canDestroyTrees, isPlayer ? playerNumber : 0, bulletSize));
            activeBulletCount++;
        }

        // Apply shoot cooldown reduction from STAR power-ups (min cooldown is 5 frames)
        int baseCooldown = Math.max(5, SHOOT_COOLDOWN - (shootCooldownReduction * 5));
        // Apply global shoot speed setting (higher speed = lower cooldown)
        double shootSpeedMult = isPlayer ? GameSettings.getPlayerShootSpeedMultiplier() : GameSettings.getEnemyShootSpeedMultiplier();
        shootCooldown = Math.max(3, (int)(baseCooldown / shootSpeedMult));
        soundManager.playShoot();
    }

    // Called when a bullet from this tank is destroyed
    public void bulletDestroyed() {
        if (activeBulletCount > 0) {
            activeBulletCount--;
        }
    }

    /**
     * Shoot a laser beam (when tank has LASER power-up active)
     * @return the laser beam or null if can't shoot yet
     */
    public Laser shootLaser(SoundManager soundManager) {
        if (!alive || laserDuration <= 0) return null;

        // Laser has very fast cooldown
        if (shootCooldown > 0) return null;

        // Calculate laser start position (front of tank)
        double laserX = x + size / 2.0;
        double laserY = y + size / 2.0;

        // Adjust position based on direction
        switch (direction) {
            case UP -> laserY = y;
            case DOWN -> laserY = y + size;
            case LEFT -> laserX = x;
            case RIGHT -> laserX = x + size;
        }

        // Set fast cooldown for laser
        shootCooldown = LASER_COOLDOWN;
        soundManager.playShoot(); // TODO: Could add a different laser sound

        return new Laser(laserX, laserY, direction, !isPlayer, isPlayer ? playerNumber : 0);
    }

    public void updateAI(GameMap map, List<Bullet> bullets, List<Tank> allTanks, Base base, SoundManager soundManager) {
        if (!alive) return;

        update(map, bullets, soundManager, allTanks, base);

        // Detect if stuck (position hasn't changed)
        if (Math.abs(x - lastX) < 0.1 && Math.abs(y - lastY) < 0.1) {
            stuckCounter++;
            if (stuckCounter > 3) { // Stuck for 3 frames - change direction immediately
                // Try different directions until one works
                Direction[] directions = Direction.values();
                Direction originalDirection = direction;
                for (int i = 0; i < 4; i++) {
                    direction = directions[random.nextInt(4)];
                    if (direction != originalDirection) {
                        break;
                    }
                }
                stuckCounter = 0;
                aiMoveCooldown = 60 + random.nextInt(120); // Commit to new direction longer
            }
        } else {
            stuckCounter = 0;
        }
        lastX = x;
        lastY = y;

        // Decrease AI cooldowns
        aiMoveCooldown--;
        aiShootCooldown--;

        // Randomly shoot
        if (aiShootCooldown <= 0) {
            shoot(bullets, soundManager);
            aiShootCooldown = 60 + random.nextInt(60);
        }

        // Change direction occasionally
        if (aiMoveCooldown <= 0) {
            // 70% move towards base, 30% random
            if (random.nextDouble() < 0.7) {
                moveTowardsBase(base);
            } else {
                direction = Direction.values()[random.nextInt(4)];
            }
            aiMoveCooldown = 30 + random.nextInt(90);
        }

        // Move in current direction
        move(direction, map, allTanks, base);
    }

    private void moveTowardsBase(Base base) {
        double baseX = base.getX();
        double baseY = base.getY();

        double dx = baseX - x;
        double dy = baseY - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            direction = dy > 0 ? Direction.DOWN : Direction.UP;
        }
    }

    public boolean damage() {
        if (!alive || hasShield || hasPauseShield) return false;

        // SHIP acts as one extra shot protection
        if (canSwim) {
            canSwim = false; // Lose SHIP ability but take no damage
            return false;
        }

        health--;
        boolean dropPowerUp = false;

        // POWER type drops power-up on EVERY hit (including death)
        if (!isPlayer && enemyType == EnemyType.POWER) {
            dropPowerUp = true;
        }

        if (health <= 0) {
            lives--;
            alive = false; // Tank dies and needs to respawn
        }

        return dropPowerUp;
    }

    // Instant kill - used by BOSS tank contact, ignores all shields
    public void instantKill() {
        health = 0;
        lives--;
        alive = false;
        // Remove all shields
        hasShield = false;
        hasPauseShield = false;
        System.out.println("Tank instantly killed by BOSS contact!");
    }

    public void render(GraphicsContext gc) {
        if (!alive) return;

        // Scale factor for rendering (1.0 for normal tanks, 4.0 for BOSS)
        double scale = (double) size / BASE_SIZE;

        // Draw shield if active (circle)
        if (hasShield) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2 * scale);
            gc.strokeOval(x - 4 * scale, y - 4 * scale, size + 8 * scale, size + 8 * scale);
        }

        // Draw pause shield (yellow/orange pulsing)
        if (hasPauseShield) {
            int pulse = (int) (System.currentTimeMillis() / 200) % 2;
            gc.setStroke(pulse == 0 ? Color.YELLOW : Color.ORANGE);
            gc.setLineWidth(3 * scale);
            gc.strokeOval(x - 6 * scale, y - 6 * scale, size + 12 * scale, size + 12 * scale);
        }

        // Draw ship indicator if active (triangle)
        if (canSwim) {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2 * scale);
            double centerX = x + size / 2;
            double topY = y - 6 * scale;
            double bottomY = y + size + 6 * scale;
            double leftX = x - 6 * scale;
            double rightX = x + size + 6 * scale;
            gc.strokePolygon(
                new double[]{leftX, rightX, centerX},
                new double[]{bottomY, bottomY, topY},
                3
            );
        }

        // Get tank color
        Color tankColor;
        Color darkColor;
        if (isPlayer) {
            tankColor = getPlayerColor(playerNumber);
            darkColor = tankColor.darker();
        } else {
            switch (enemyType) {
                case REGULAR -> { tankColor = Color.RED; darkColor = Color.DARKRED; }
                case ARMORED -> { tankColor = Color.DARKRED; darkColor = Color.rgb(80, 0, 0); }
                case FAST -> { tankColor = Color.rgb(255, 100, 100); darkColor = Color.rgb(200, 60, 60); }
                case POWER -> {
                    // Rainbow color animation for POWER tanks
                    int frame = (int) (System.currentTimeMillis() / 100) % 7;
                    Color[] rainbow = { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.PURPLE };
                    tankColor = rainbow[frame];
                    darkColor = tankColor.darker();
                }
                case BOSS -> {
                    // Pulsing red color for BOSS tank
                    double pulse = (Math.sin(System.currentTimeMillis() / 150.0) + 1) / 2; // 0 to 1
                    int red = (int) (150 + pulse * 105); // 150 to 255
                    int green = (int) (pulse * 50); // 0 to 50
                    tankColor = Color.rgb(red, green, 0);
                    darkColor = Color.rgb((int)(red * 0.6), 0, 0);
                }
                case HEAVY -> { tankColor = Color.DARKGRAY; darkColor = Color.BLACK; }
                default -> { tankColor = Color.RED; darkColor = Color.DARKRED; }
            }
        }

        // Calculate track animation offset (alternates every 4 frames)
        int trackOffset = (trackAnimationFrame / 4) % 2 == 0 ? 0 : (int)(3 * scale);

        // Draw tank based on direction
        gc.save();
        gc.translate(x + size / 2.0, y + size / 2.0);

        // Rotate based on direction
        switch (direction) {
            case UP -> gc.rotate(0);
            case RIGHT -> gc.rotate(90);
            case DOWN -> gc.rotate(180);
            case LEFT -> gc.rotate(270);
        }

        gc.translate(-size / 2.0, -size / 2.0);

        // Draw left track
        gc.setFill(darkColor);
        gc.fillRect(0, 0, 8 * scale, size);
        // Track details (animated)
        gc.setFill(Color.rgb(40, 40, 40));
        int trackCount = (int)(5 * scale);
        for (int i = 0; i < trackCount; i++) {
            int ty = (int)((i * 7 * scale + trackOffset) % size);
            gc.fillRect(1 * scale, ty, 6 * scale, 3 * scale);
        }

        // Draw right track
        gc.setFill(darkColor);
        gc.fillRect(size - 8 * scale, 0, 8 * scale, size);
        // Track details (animated)
        gc.setFill(Color.rgb(40, 40, 40));
        for (int i = 0; i < trackCount; i++) {
            int ty = (int)((i * 7 * scale + trackOffset) % size);
            gc.fillRect(size - 7 * scale, ty, 6 * scale, 3 * scale);
        }

        // Draw tank body (between tracks)
        gc.setFill(tankColor);
        gc.fillRect(6 * scale, 4 * scale, size - 12 * scale, size - 8 * scale);

        // Draw turret (circular base)
        gc.setFill(darkColor);
        gc.fillOval(size / 2.0 - 7 * scale, size / 2.0 - 7 * scale, 14 * scale, 14 * scale);
        gc.setFill(tankColor);
        gc.fillOval(size / 2.0 - 5 * scale, size / 2.0 - 5 * scale, 10 * scale, 10 * scale);

        // Draw cannon barrel
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(size / 2.0 - 2 * scale, -2 * scale, 4 * scale, size / 2.0 + 2 * scale);
        gc.setFill(Color.GRAY);
        gc.fillRect(size / 2.0 - 1 * scale, -2 * scale, 2 * scale, size / 2.0);

        // Special markings for enemy types
        if (!isPlayer) {
            switch (enemyType) {
                case ARMORED -> {
                    // Extra armor plates
                    gc.setFill(Color.GRAY);
                    gc.fillRect(8 * scale, 6 * scale, size - 16 * scale, 3 * scale);
                    gc.fillRect(8 * scale, size - 9 * scale, size - 16 * scale, 3 * scale);
                }
                case HEAVY -> {
                    // White dot indicator
                    gc.setFill(Color.WHITE);
                    gc.fillOval(size / 2.0 - 3 * scale, size / 2.0 - 3 * scale, 6 * scale, 6 * scale);
                }
                case FAST -> {
                    // Speed stripes
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(scale);
                    gc.strokeLine(10 * scale, size - 6 * scale, 14 * scale, size - 6 * scale);
                    gc.strokeLine(size - 14 * scale, size - 6 * scale, size - 10 * scale, size - 6 * scale);
                }
                case BOSS -> {
                    // Skull with crossbones
                    double cx = size / 2.0;
                    double cy = size / 2.0 - 2 * scale;

                    // Skull (white oval)
                    gc.setFill(Color.WHITE);
                    gc.fillOval(cx - 8 * scale, cy - 6 * scale, 16 * scale, 14 * scale);

                    // Eye sockets (black)
                    gc.setFill(Color.BLACK);
                    gc.fillOval(cx - 5 * scale, cy - 2 * scale, 4 * scale, 4 * scale);
                    gc.fillOval(cx + 1 * scale, cy - 2 * scale, 4 * scale, 4 * scale);

                    // Nose (black triangle)
                    gc.fillPolygon(
                        new double[]{cx - 1 * scale, cx + 1 * scale, cx},
                        new double[]{cy + 3 * scale, cy + 3 * scale, cy + 5 * scale},
                        3
                    );

                    // Teeth (white rectangles on black mouth)
                    gc.setFill(Color.BLACK);
                    gc.fillRect(cx - 4 * scale, cy + 5 * scale, 8 * scale, 3 * scale);
                    gc.setFill(Color.WHITE);
                    for (int i = 0; i < 4; i++) {
                        gc.fillRect(cx - 3.5 * scale + i * 2 * scale, cy + 5 * scale, 1.5 * scale, 3 * scale);
                    }

                    // Crossbones behind skull
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(2 * scale);
                    // Bone 1 (top-left to bottom-right)
                    gc.strokeLine(cx - 12 * scale, cy - 8 * scale, cx + 12 * scale, cy + 12 * scale);
                    // Bone 2 (top-right to bottom-left)
                    gc.strokeLine(cx + 12 * scale, cy - 8 * scale, cx - 12 * scale, cy + 12 * scale);

                    // Bone ends (small circles)
                    gc.setFill(Color.WHITE);
                    gc.fillOval(cx - 14 * scale, cy - 10 * scale, 4 * scale, 4 * scale);
                    gc.fillOval(cx + 10 * scale, cy - 10 * scale, 4 * scale, 4 * scale);
                    gc.fillOval(cx - 14 * scale, cy + 10 * scale, 4 * scale, 4 * scale);
                    gc.fillOval(cx + 10 * scale, cy + 10 * scale, 4 * scale, 4 * scale);
                }
                // POWER has rainbow colors - no extra markings needed
            }
        }

        gc.restore();

        // Reset moving flag (will be set again if tank moves next frame)
        isMoving = false;
    }

    public boolean collidesWith(double otherX, double otherY, int otherSize) {
        return x < otherX + otherSize &&
               x + size > otherX &&
               y < otherY + otherSize &&
               y + size > otherY;
    }

    // Get player color based on player number
    public static Color getPlayerColor(int playerNum) {
        return switch (playerNum) {
            case 1 -> Color.YELLOW;
            case 2 -> Color.LIME;
            case 3 -> Color.CYAN;
            case 4 -> Color.ORANGE;
            default -> Color.WHITE;
        };
    }

    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }
    public void addLife() { this.lives++; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public void setHealth(int health) { this.health = health; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public boolean hasShield() { return hasShield; }
    public boolean hasPauseShield() { return hasPauseShield; }
    public void setPauseShield(boolean pauseShield) { this.hasPauseShield = pauseShield; }
    public Direction getDirection() { return direction; }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    // Setters for network sync
    public void setShield(boolean shield) {
        this.hasShield = shield;
        if (!shield) {
            this.shieldDuration = 0;
        }
    }

    public void setShieldWithDuration(boolean shield, int duration) {
        this.hasShield = shield;
        this.shieldDuration = shield ? duration : 0;
    }

    public int getShieldDuration() {
        return shieldDuration;
    }

    public void setShip(boolean ship) {
        this.canSwim = ship;
    }

    // Setters for network sync of power-ups
    public void setGun(boolean hasGun) {
        this.bulletPower = hasGun ? 2 : 1;
    }

    public void setStarCount(int count) {
        this.shootCooldownReduction = count;
    }

    public void setCarCount(int count) {
        this.speedMultiplier = 1.0 + (count * 0.3);
    }

    public void setSaw(boolean hasSaw) {
        this.canDestroyTrees = hasSaw;
    }

    public void setMachinegunCount(int count) {
        this.machinegunCount = count;
    }

    public EnemyType getEnemyType() { return enemyType; }
    public void setEnemyType(EnemyType type) {
        this.enemyType = type;
        // Update size for BOSS tanks (needed for network sync)
        if (type == EnemyType.BOSS) {
            this.size = BASE_SIZE * 4;
        } else {
            this.size = BASE_SIZE;
        }
    }

    // Power-up status getters for UI display
    public boolean hasGun() { return bulletPower >= 2; }
    public int getStarCount() { return shootCooldownReduction; }
    public int getCarCount() {
        // Calculate CAR count based on speed multiplier (base 1.0, each adds 0.3)
        return (int)((speedMultiplier - 1.0) / 0.3);
    }
    public boolean hasShip() { return canSwim; }
    public boolean hasSaw() { return canDestroyTrees; }
    public int getMachinegunCount() { return machinegunCount; }

    // Power-up effects
    public void applyGun() {
        bulletPower = 2; // Can break steel walls
    }

    public void applyStar() {
        shootCooldownReduction++; // Each star reduces cooldown by 5 frames
    }

    public void applyCar() {
        speedMultiplier += 0.3; // Each car increases speed by 30%
        speedMultiplier = Math.min(speedMultiplier, 2.5); // Cap at 2.5x speed
    }

    public void applyTempSpeedBoost(double boost) {
        tempSpeedBoost = boost;
    }

    public void removeTempSpeedBoost() {
        tempSpeedBoost = 0;
    }

    public double getTempSpeedBoost() {
        return tempSpeedBoost;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
    }

    public void applyShip() {
        canSwim = true; // Can move through water
    }

    public void applySaw() {
        canDestroyTrees = true; // Bullets can destroy trees
    }

    public void applyMachinegun() {
        // Each MACHINEGUN adds one extra bullet (max 5 total bullets = 4 extra)
        if (machinegunCount < 4) {
            machinegunCount++;
        }
    }

    public void applyTank() {
        lives++; // Extra life
    }

    public void applyShield() {
        if (isPlayer) {
            // Players get shield for 1 minute (3600 frames at 60 FPS)
            hasShield = true;
            shieldDuration = 3600;
        } else {
            // Enemies get extra life
            lives++;
        }
    }

    public void applyLaser() {
        // LASER power-up: shoot laser beams for 30 seconds
        laserDuration = LASER_DURATION;
    }

    public boolean hasLaser() {
        return laserDuration > 0;
    }

    public int getLaserDuration() {
        return laserDuration;
    }

    public void setLaserDuration(int duration) {
        this.laserDuration = duration;
    }

    public boolean canSwim() {
        return canSwim;
    }

    public boolean canDestroyTrees() {
        return canDestroyTrees;
    }

    public void respawn(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.direction = Direction.UP;
        this.health = maxHealth;
        this.hasShield = true;
        this.shieldDuration = 180;
        this.alive = true;

        // Clear all power-ups on respawn
        this.bulletPower = 1;
        this.shootCooldownReduction = 0;
        this.speedMultiplier = 1.0;
        this.canSwim = false;
        this.canDestroyTrees = false;
        this.machinegunCount = 0;
    }

    public void giveTemporaryShield() {
        this.hasShield = true;
        this.shieldDuration = 180; // 3 seconds at 60 FPS
    }

    public void setPosition(double x, double y) {
        // Animate tracks if position changed (for network sync)
        if (this.x != x || this.y != y) {
            trackAnimationFrame++;
            isMoving = true;
        }
        this.x = x;
        this.y = y;
    }
}
