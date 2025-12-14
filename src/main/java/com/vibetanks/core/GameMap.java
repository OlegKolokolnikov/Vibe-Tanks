package com.vibetanks.core;

import com.vibetanks.rendering.TileRenderer;
import com.vibetanks.util.GameLogger;
import javafx.scene.canvas.GraphicsContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class GameMap {
    private static final GameLogger LOG = GameLogger.getLogger(GameMap.class);
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;
    private static final int BURN_DURATION = GameConstants.BURN_DURATION;

    private int width;
    private int height;
    private TileType[][] tiles;
    private TileType[][] previousTiles; // For delta encoding - tracks last synced state
    private boolean deltaEncodingEnabled = true;
    private final Random random = GameConstants.RANDOM; // Use shared Random instance
    private final LevelGenerator levelGenerator; // Extracted level generation logic
    private int levelNumber = 1;
    private long currentLevelSeed; // Seed used for current level (for restart)
    private LevelData customLevelData; // Custom level data (if using custom level)

    // Track burning trees: key = encoded position, value = frames remaining
    private Map<Long, Integer> burningTiles = new HashMap<>();

    // Safe position encoding that works for any map size (uses long to avoid overflow)
    private static long encodePosition(int row, int col) {
        return ((long) row << 16) | (col & 0xFFFF);
    }

    private static int decodeRow(long key) {
        return (int) (key >> 16);
    }

    private static int decodeCol(long key) {
        return (int) (key & 0xFFFF);
    }

    // Delta encoding: list of tile changes since last sync
    private final java.util.List<int[]> pendingChanges = new java.util.ArrayList<>(64);

    public enum TileType {
        EMPTY,
        BRICK,
        STEEL,
        WATER,
        TREES,
        ICE,
        GROUND  // Indestructible ground at bottom of map
    }

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TileType[height][width];
        this.previousTiles = new TileType[height][width];
        this.levelGenerator = new LevelGenerator(width, height, random);
        generateLevelForNumber(1);
        // Initialize previousTiles with current state
        copyTilesToPrevious();
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public void nextLevel() {
        levelNumber++;
        burningTiles.clear();
        generateLevelForNumber(levelNumber);
    }

    public void resetToLevel1() {
        levelNumber = 1;
        burningTiles.clear();
        generateLevelForNumber(1);
    }

    public void regenerateCurrentLevel() {
        // Keep the same level number AND same seed to get identical map
        burningTiles.clear();
        if (customLevelData != null) {
            // Custom level - just reload from data
            importTiles(customLevelData.getTiles());
        } else {
            // Random level - use same seed
            random.setSeed(currentLevelSeed);
            generateRandomLevel();
        }
    }

    /**
     * Generate level for a specific level number.
     * Checks if a custom level exists, otherwise generates random.
     */
    private void generateLevelForNumber(int num) {
        this.levelNumber = num;
        this.customLevelData = null; // Reset custom level

        // Check if custom level exists for this level number
        if (LevelManager.hasCustomLevel(num)) {
            LevelData customLevel = LevelManager.loadLevelByNumber(num);
            if (customLevel != null) {
                LOG.info("Loading custom level {}", num);
                this.customLevelData = customLevel;
                importTiles(customLevel.getTiles());
                return;
            }
        }

        // No custom level, generate random
        LOG.info("Generating random level {}", num);
        currentLevelSeed = System.currentTimeMillis();
        random.setSeed(currentLevelSeed);
        generateRandomLevel();
    }

    public void setLevelNumber(int level) {
        this.levelNumber = level;
    }

    public void generateRandomLevel() {
        LOG.info("Generating random level {}", levelNumber);
        levelGenerator.generateRandomLevel(tiles);
    }

    public boolean checkTankCollision(double x, double y, int tankSize) {
        return checkTankCollision(x, y, tankSize, false);
    }

    public boolean checkTankCollision(double x, double y, int tankSize, boolean canSwim) {
        // Check map boundaries directly (integer division can miss negative values)
        if (x < 0 || y < 0 || x + tankSize > width * TILE_SIZE || y + tankSize > height * TILE_SIZE) {
            return true; // collision with boundary
        }

        int startCol = (int) x / TILE_SIZE;
        int endCol = (int) (x + tankSize - 1) / TILE_SIZE;
        int startRow = (int) y / TILE_SIZE;
        int endRow = (int) (y + tankSize - 1) / TILE_SIZE;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (row < 0 || row >= height || col < 0 || col >= width) {
                    return true; // collision with boundary (shouldn't happen now but keep as safety)
                }
                TileType tile = tiles[row][col];
                if (tile == TileType.BRICK || tile == TileType.STEEL || tile == TileType.GROUND) {
                    return true; // collision with solid tile
                }
                // Check water collision only if tank cannot swim
                if (tile == TileType.WATER && !canSwim) {
                    return true;
                }
            }
        }
        return false; // no collision
    }

    public boolean checkBulletCollision(Bullet bullet) {
        return checkBulletCollision(bullet, null);
    }

    public boolean checkBulletCollision(Bullet bullet, com.vibetanks.audio.SoundManager soundManager) {
        // Check all tiles the bullet overlaps with (not just center)
        // This prevents bullets from slipping through edges of solid tiles
        int bulletSize = bullet.getSize();
        int minCol = (int) bullet.getX() / TILE_SIZE;
        int maxCol = (int) (bullet.getX() + bulletSize - 1) / TILE_SIZE;
        int minRow = (int) bullet.getY() / TILE_SIZE;
        int maxRow = (int) (bullet.getY() + bulletSize - 1) / TILE_SIZE;

        // Check all overlapping tiles
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (row < 0 || row >= height || col < 0 || col >= width) {
                    return true; // out of bounds
                }

                TileType tile = tiles[row][col];

                if (tile == TileType.BRICK) {
                    // Brick is destroyed by bullet
                    tiles[row][col] = TileType.EMPTY;
                    return true;
                } else if (tile == TileType.STEEL) {
                    // Steel stops bullet but isn't destroyed (unless power bullet)
                    if (bullet.getPower() >= 2) {
                        tiles[row][col] = TileType.EMPTY;
                    }
                    return true;
                } else if (tile == TileType.GROUND) {
                    // Ground is completely indestructible - stops all bullets
                    return true;
                } else if (tile == TileType.TREES) {
                    // Check if tree is already burning - bullets pass through burning trees
                    long key = encodePosition(row, col);
                    if (burningTiles.containsKey(key)) {
                        // Tree is burning - bullets pass through
                        continue;
                    }
                    // Tree is not burning - only SAW bullets can start fire
                    if (bullet.canDestroyTrees()) {
                        burningTiles.put(key, BURN_DURATION);
                        // Play tree burn sound
                        if (soundManager != null) {
                            soundManager.playTreeBurn();
                        }
                        return true;
                    }
                    // Normal bullets pass through non-burning trees
                    continue;
                }
            }
        }

        return false; // no collision
    }

    public void render(GraphicsContext gc) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                TileType tile = tiles[row][col];
                double x = col * TILE_SIZE;
                double y = row * TILE_SIZE;
                TileRenderer.renderTile(gc, tile, x, y);
            }
        }
    }

    // Render only terrain (no trees) - trees will be rendered on top of tanks
    public void renderWithoutTrees(GraphicsContext gc) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                TileType tile = tiles[row][col];
                double x = col * TILE_SIZE;
                double y = row * TILE_SIZE;
                TileRenderer.renderTile(gc, tile, x, y, true);
            }
        }
    }

    // Render only trees - to be drawn on top of tanks
    public void renderTrees(GraphicsContext gc) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (tiles[row][col] == TileType.TREES) {
                    double x = col * TILE_SIZE;
                    double y = row * TILE_SIZE;
                    TileRenderer.renderTrees(gc, x, y);
                }
            }
        }
    }

    // Update burning tiles
    public void update() {
        Iterator<Map.Entry<Long, Integer>> it = burningTiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Integer> entry = it.next();
            int framesLeft = entry.getValue() - 1;
            if (framesLeft <= 0) {
                // Burn complete - destroy the tree
                long key = entry.getKey();
                int row = decodeRow(key);
                int col = decodeCol(key);
                tiles[row][col] = TileType.EMPTY;
                it.remove();
            } else {
                entry.setValue(framesLeft);
            }
        }
    }

    // Render fire on burning tiles
    public void renderBurningTiles(GraphicsContext gc) {
        long time = FrameTime.getFrameTime();
        for (Map.Entry<Long, Integer> entry : burningTiles.entrySet()) {
            long key = entry.getKey();
            int row = decodeRow(key);
            int col = decodeCol(key);
            double x = col * TILE_SIZE;
            double y = row * TILE_SIZE;
            TileRenderer.renderBurningTree(gc, x, y, time);
        }
    }

    public boolean hasBurningTiles() {
        return !burningTiles.isEmpty();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public TileType getTile(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return TileType.STEEL; // treat out of bounds as steel
        }
        return tiles[row][col];
    }

    public void setTile(int row, int col, TileType type) {
        if (row >= 0 && row < height && col >= 0 && col < width) {
            TileType oldType = tiles[row][col];
            tiles[row][col] = type;
            // Track change for delta encoding
            if (deltaEncodingEnabled && oldType != type) {
                pendingChanges.add(new int[]{row, col, type.ordinal()});
            }
        }
    }

    // Export all tiles as int array for network sync
    public int[][] exportTiles() {
        int[][] result = new int[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result[row][col] = tiles[row][col].ordinal();
            }
        }
        return result;
    }

    // Import all tiles from int array for network sync
    public void importTiles(int[][] tileData) {
        if (tileData == null) return;
        for (int row = 0; row < Math.min(height, tileData.length); row++) {
            for (int col = 0; col < Math.min(width, tileData[row].length); col++) {
                tiles[row][col] = TileType.values()[tileData[row][col]];
            }
        }
    }

    // ============ DELTA ENCODING FOR NETWORK SYNC ============

    /**
     * Export only tiles that have changed since last sync.
     * Returns list of [row, col, tileOrdinal] arrays.
     * Much more efficient than full export when few tiles change.
     */
    public java.util.List<int[]> exportDeltaTiles() {
        java.util.List<int[]> changes = new java.util.ArrayList<>();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (tiles[row][col] != previousTiles[row][col]) {
                    changes.add(new int[]{row, col, tiles[row][col].ordinal()});
                }
            }
        }
        return changes;
    }

    /**
     * Get pending changes tracked via setTile().
     * Returns a copy of the list and clears internal tracking.
     */
    public java.util.List<int[]> getPendingChanges() {
        java.util.List<int[]> result = new java.util.ArrayList<>(pendingChanges);
        pendingChanges.clear();
        return result;
    }

    /**
     * Check if there are any pending changes.
     */
    public boolean hasPendingChanges() {
        return !pendingChanges.isEmpty();
    }

    /**
     * Apply delta changes from network sync.
     * @param changes List of [row, col, tileOrdinal] arrays
     */
    public void applyDeltaTiles(java.util.List<int[]> changes) {
        if (changes == null) return;
        // Temporarily disable change tracking during import
        boolean wasEnabled = deltaEncodingEnabled;
        deltaEncodingEnabled = false;
        for (int[] change : changes) {
            int row = change[0];
            int col = change[1];
            int ordinal = change[2];
            if (row >= 0 && row < height && col >= 0 && col < width && ordinal >= 0 && ordinal < TileType.values().length) {
                tiles[row][col] = TileType.values()[ordinal];
            }
        }
        deltaEncodingEnabled = wasEnabled;
    }

    /**
     * Mark current tiles as synced (for delta tracking).
     * Call after sending/receiving full state.
     */
    public void markTilesSynced() {
        copyTilesToPrevious();
        pendingChanges.clear();
    }

    /**
     * Check if full sync is needed (e.g., after level change).
     * Returns true if more than 10% of tiles changed.
     */
    public boolean needsFullSync() {
        int changes = 0;
        int threshold = (width * height) / 10; // 10% threshold
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (tiles[row][col] != previousTiles[row][col]) {
                    changes++;
                    if (changes > threshold) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void copyTilesToPrevious() {
        for (int row = 0; row < height; row++) {
            System.arraycopy(tiles[row], 0, previousTiles[row], 0, width);
        }
    }

    public void setDeltaEncodingEnabled(boolean enabled) {
        this.deltaEncodingEnabled = enabled;
    }

    // Export burning tiles for network sync
    public Map<Long, Integer> exportBurningTiles() {
        return new HashMap<>(burningTiles);
    }

    // Import burning tiles from network sync
    public void importBurningTiles(Map<Long, Integer> data) {
        burningTiles.clear();
        if (data != null) {
            burningTiles.putAll(data);
        }
    }

    // Set burning tiles directly (for network sync with list)
    public void setBurningTiles(java.util.List<int[]> tiles) {
        burningTiles.clear();
        if (tiles != null) {
            for (int[] tile : tiles) {
                long key = encodePosition(tile[0], tile[1]);
                burningTiles.put(key, tile[2]);
            }
        }
    }

    // Base protection management (for SHOVEL power-up)
    public void setBaseProtection(TileType protectionType) {
        // Set protection tiles, but preserve GROUND (indestructible) tiles
        int[][] protectionPositions = {
            {23, 11}, {23, 12}, {23, 13},  // Top wall
            {24, 11}, {24, 13},             // Left and right walls
            {25, 11}, {25, 12}, {25, 13}    // Bottom wall
        };
        for (int[] pos : protectionPositions) {
            // Don't replace GROUND tiles - they're already indestructible
            if (tiles[pos[0]][pos[1]] != TileType.GROUND) {
                tiles[pos[0]][pos[1]] = protectionType;
            }
        }
    }

    public void resetBaseProtection() {
        setBaseProtection(TileType.BRICK);
    }

    /**
     * Check if base protection has been broken (any protection tile is EMPTY).
     * Returns true if at least one protection tile is destroyed.
     */
    public boolean isBaseProtectionBroken() {
        // Check all protection tiles - if any are EMPTY, protection is broken
        int[][] protectionTiles = {{23, 11}, {23, 12}, {23, 13}, {24, 11}, {24, 13}, {25, 11}, {25, 12}, {25, 13}};
        for (int[] pos : protectionTiles) {
            if (tiles[pos[0]][pos[1]] == TileType.EMPTY) {
                return true;
            }
        }
        return false;
    }

    // Custom level support
    public void setCustomLevel(LevelData levelData) {
        this.customLevelData = levelData;
        if (levelData != null) {
            importTiles(levelData.getTiles());
        }
    }

    public boolean hasCustomLevel() {
        return customLevelData != null;
    }

    public LevelData getCustomLevelData() {
        return customLevelData;
    }

    // Load custom level (replacing random generation)
    public void loadCustomLevel() {
        if (customLevelData != null) {
            importTiles(customLevelData.getTiles());
        }
    }

    // For custom levels, regenerate means reload from the same data
    public void regenerateOrReloadLevel() {
        burningTiles.clear();
        if (customLevelData != null) {
            loadCustomLevel();
        } else {
            random.setSeed(currentLevelSeed);
            generateRandomLevel();
        }
    }
}
