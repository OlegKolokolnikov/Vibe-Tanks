package com.vibetanks.core;

import java.util.Arrays;

/**
 * Data class for level serialization to/from JSON.
 * Contains all tile data and metadata for a custom level.
 */
public class LevelData {
    private int levelNumber; // The level number (1, 2, 3, etc.)
    private String name;
    private String author;
    private long createdAt;
    private int width;
    private int height;
    private int[][] tiles; // TileType ordinal values

    // Default constructor for JSON deserialization
    public LevelData() {
    }

    public LevelData(int levelNumber, String name, String author, int width, int height, int[][] tiles) {
        this.levelNumber = levelNumber;
        this.name = name;
        this.author = author;
        this.createdAt = System.currentTimeMillis();
        this.width = width;
        this.height = height;
        this.tiles = tiles;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public void setLevelNumber(int levelNumber) {
        this.levelNumber = levelNumber;
    }

    // Create from GameMap
    public static LevelData fromGameMap(GameMap map, int levelNumber, String name, String author) {
        return new LevelData(levelNumber, name, author, map.getWidth(), map.getHeight(), map.exportTiles());
    }

    // Apply to GameMap
    public void applyToGameMap(GameMap map) {
        if (tiles != null) {
            map.importTiles(tiles);
        }
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int[][] getTiles() {
        return tiles;
    }

    public void setTiles(int[][] tiles) {
        this.tiles = tiles;
    }

    // Deep copy of tiles
    public int[][] getTilesCopy() {
        if (tiles == null) return null;
        int[][] copy = new int[tiles.length][];
        for (int i = 0; i < tiles.length; i++) {
            copy[i] = Arrays.copyOf(tiles[i], tiles[i].length);
        }
        return copy;
    }
}
