package com.vibetanks.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Spatial partitioning grid for efficient collision detection.
 * Divides the game area into cells and provides O(1) lookup of nearby entities.
 *
 * Usage:
 * 1. Clear the grid at start of frame: grid.clear()
 * 2. Insert all entities: grid.insert(entity, x, y)
 * 3. Query nearby entities: grid.getNearby(x, y, radius)
 */
public class SpatialGrid<T> {
    private static final int DEFAULT_CELL_SIZE = 64; // 2 tiles

    private final int cellSize;
    private final int gridWidth;
    private final int gridHeight;
    private final List<T>[][] cells;

    // Pre-allocated list for query results to avoid allocation in hot path
    private final List<T> queryResult = new ArrayList<>(32);

    @SuppressWarnings("unchecked")
    public SpatialGrid(int mapWidth, int mapHeight) {
        this(mapWidth, mapHeight, DEFAULT_CELL_SIZE);
    }

    @SuppressWarnings("unchecked")
    public SpatialGrid(int mapWidth, int mapHeight, int cellSize) {
        this.cellSize = cellSize;
        this.gridWidth = (mapWidth + cellSize - 1) / cellSize;
        this.gridHeight = (mapHeight + cellSize - 1) / cellSize;
        this.cells = new List[gridHeight][gridWidth];

        // Initialize all cells with pre-allocated lists
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                cells[y][x] = new ArrayList<>(8);
            }
        }
    }

    /**
     * Clear all entities from the grid. Call at start of each frame.
     */
    public void clear() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                cells[y][x].clear();
            }
        }
    }

    /**
     * Insert an entity at the given position.
     */
    public void insert(T entity, double x, double y) {
        int cellX = getCellX(x);
        int cellY = getCellY(y);

        if (isValidCell(cellX, cellY)) {
            cells[cellY][cellX].add(entity);
        }
    }

    /**
     * Insert an entity that spans multiple cells (for larger objects like tanks).
     */
    public void insertWithSize(T entity, double x, double y, int size) {
        int minCellX = getCellX(x);
        int maxCellX = getCellX(x + size - 1);
        int minCellY = getCellY(y);
        int maxCellY = getCellY(y + size - 1);

        for (int cy = minCellY; cy <= maxCellY; cy++) {
            for (int cx = minCellX; cx <= maxCellX; cx++) {
                if (isValidCell(cx, cy)) {
                    List<T> cell = cells[cy][cx];
                    // Avoid adding duplicate entries
                    if (!cell.contains(entity)) {
                        cell.add(entity);
                    }
                }
            }
        }
    }

    /**
     * Get all entities in the same cell as the given position.
     * Returns a reusable list - do not store reference!
     */
    public List<T> getInCell(double x, double y) {
        queryResult.clear();
        int cellX = getCellX(x);
        int cellY = getCellY(y);

        if (isValidCell(cellX, cellY)) {
            queryResult.addAll(cells[cellY][cellX]);
        }
        return queryResult;
    }

    /**
     * Get all entities in the same cell and adjacent cells (3x3 area).
     * This is the main method for collision detection.
     * Returns a reusable list - do not store reference!
     */
    public List<T> getNearby(double x, double y) {
        queryResult.clear();
        int cellX = getCellX(x);
        int cellY = getCellY(y);

        // Check 3x3 grid of cells around the position
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = cellX + dx;
                int cy = cellY + dy;
                if (isValidCell(cx, cy)) {
                    queryResult.addAll(cells[cy][cx]);
                }
            }
        }
        return queryResult;
    }

    /**
     * Get all entities within a rectangular area.
     * Returns a reusable list - do not store reference!
     */
    public List<T> getInArea(double x, double y, int width, int height) {
        queryResult.clear();
        int minCellX = getCellX(x);
        int maxCellX = getCellX(x + width - 1);
        int minCellY = getCellY(y);
        int maxCellY = getCellY(y + height - 1);

        // Expand by 1 cell in each direction to catch entities on boundaries
        minCellX = Math.max(0, minCellX - 1);
        maxCellX = Math.min(gridWidth - 1, maxCellX + 1);
        minCellY = Math.max(0, minCellY - 1);
        maxCellY = Math.min(gridHeight - 1, maxCellY + 1);

        for (int cy = minCellY; cy <= maxCellY; cy++) {
            for (int cx = minCellX; cx <= maxCellX; cx++) {
                queryResult.addAll(cells[cy][cx]);
            }
        }
        return queryResult;
    }

    private int getCellX(double x) {
        return Math.max(0, Math.min(gridWidth - 1, (int) x / cellSize));
    }

    private int getCellY(double y) {
        return Math.max(0, Math.min(gridHeight - 1, (int) y / cellSize));
    }

    private boolean isValidCell(int cellX, int cellY) {
        return cellX >= 0 && cellX < gridWidth && cellY >= 0 && cellY < gridHeight;
    }

    /**
     * Get statistics for debugging.
     */
    public String getStats() {
        int totalEntities = 0;
        int maxInCell = 0;
        int nonEmptyCells = 0;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int count = cells[y][x].size();
                totalEntities += count;
                if (count > 0) {
                    nonEmptyCells++;
                    maxInCell = Math.max(maxInCell, count);
                }
            }
        }

        return String.format("SpatialGrid[%dx%d cells, %d entities, %d non-empty, max %d per cell]",
            gridWidth, gridHeight, totalEntities, nonEmptyCells, maxInCell);
    }
}
