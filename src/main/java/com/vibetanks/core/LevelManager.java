package com.vibetanks.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages saving and loading of custom levels in JSON format.
 * Levels are stored in a 'levels' folder next to the game.
 */

public class LevelManager {
    private static final String LEVELS_FOLDER = "levels";

    /**
     * Get or create the levels directory
     */
    public static Path getLevelsDirectory() {
        Path levelsDir = Paths.get(LEVELS_FOLDER);
        if (!Files.exists(levelsDir)) {
            try {
                Files.createDirectories(levelsDir);
            } catch (IOException e) {
                System.err.println("Failed to create levels directory: " + e.getMessage());
            }
        }
        return levelsDir;
    }

    /**
     * Save a level to JSON file (named by level number)
     */
    public static boolean saveLevel(LevelData level) {
        if (level == null || level.getLevelNumber() < 1) {
            return false;
        }

        String filename = "level_" + level.getLevelNumber() + ".json";
        Path filepath = getLevelsDirectory().resolve(filename);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath.toFile()))) {
            writer.println(toJson(level));
            System.out.println("Level saved to: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save level: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load a level from JSON file
     */
    public static LevelData loadLevel(String filename) {
        Path filepath = getLevelsDirectory().resolve(filename);
        return loadLevelFromPath(filepath);
    }

    /**
     * Load a level by its level number
     */
    public static LevelData loadLevelByNumber(int levelNumber) {
        String filename = "level_" + levelNumber + ".json";
        return loadLevel(filename);
    }

    /**
     * Check if a custom level exists for a given level number
     */
    public static boolean hasCustomLevel(int levelNumber) {
        String filename = "level_" + levelNumber + ".json";
        Path filepath = getLevelsDirectory().resolve(filename);
        return Files.exists(filepath);
    }

    /**
     * Delete a level by its level number
     */
    public static boolean deleteLevelByNumber(int levelNumber) {
        String filename = "level_" + levelNumber + ".json";
        return deleteLevel(filename);
    }

    /**
     * Get list of all custom level numbers that exist
     */
    public static List<Integer> getCustomLevelNumbers() {
        List<Integer> numbers = new ArrayList<>();
        Path levelsDir = getLevelsDirectory();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(levelsDir, "level_*.json")) {
            for (Path entry : stream) {
                String filename = entry.getFileName().toString();
                // Extract number from "level_X.json"
                String numStr = filename.replace("level_", "").replace(".json", "");
                try {
                    numbers.add(Integer.parseInt(numStr));
                } catch (NumberFormatException e) {
                    // Ignore invalid filenames
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to list levels: " + e.getMessage());
        }

        Collections.sort(numbers);
        return numbers;
    }

    /**
     * Load a level from a specific path
     */
    public static LevelData loadLevelFromPath(Path filepath) {
        if (!Files.exists(filepath)) {
            System.err.println("Level file not found: " + filepath);
            return null;
        }

        try {
            String json = new String(Files.readAllBytes(filepath));
            return fromJson(json);
        } catch (IOException e) {
            System.err.println("Failed to load level: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get list of all saved levels
     */
    public static List<String> getSavedLevels() {
        List<String> levels = new ArrayList<>();
        Path levelsDir = getLevelsDirectory();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(levelsDir, "*.json")) {
            for (Path entry : stream) {
                levels.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            System.err.println("Failed to list levels: " + e.getMessage());
        }

        Collections.sort(levels);
        return levels;
    }

    /**
     * Delete a level file
     */
    public static boolean deleteLevel(String filename) {
        Path filepath = getLevelsDirectory().resolve(filename);
        try {
            Files.deleteIfExists(filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete level: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sanitize filename to remove invalid characters
     */
    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    /**
     * Convert LevelData to JSON string (simple implementation without external libraries)
     */
    private static String toJson(LevelData level) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"levelNumber\": ").append(level.getLevelNumber()).append(",\n");
        sb.append("  \"name\": \"").append(escapeJson(level.getName() != null ? level.getName() : "")).append("\",\n");
        sb.append("  \"author\": \"").append(escapeJson(level.getAuthor() != null ? level.getAuthor() : "")).append("\",\n");
        sb.append("  \"createdAt\": ").append(level.getCreatedAt()).append(",\n");
        sb.append("  \"width\": ").append(level.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(level.getHeight()).append(",\n");
        sb.append("  \"tiles\": [\n");

        int[][] tiles = level.getTiles();
        for (int row = 0; row < tiles.length; row++) {
            sb.append("    [");
            for (int col = 0; col < tiles[row].length; col++) {
                sb.append(tiles[row][col]);
                if (col < tiles[row].length - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            if (row < tiles.length - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parse JSON string to LevelData (simple implementation without external libraries)
     */
    private static LevelData fromJson(String json) {
        LevelData level = new LevelData();

        // Extract level number
        level.setLevelNumber(extractInt(json, "levelNumber"));

        // Extract name
        level.setName(extractString(json, "name"));

        // Extract author
        level.setAuthor(extractString(json, "author"));

        // Extract createdAt
        level.setCreatedAt(extractLong(json, "createdAt"));

        // Extract width and height
        level.setWidth(extractInt(json, "width"));
        level.setHeight(extractInt(json, "height"));

        // Extract tiles array
        level.setTiles(extractTiles(json, level.getHeight(), level.getWidth()));

        return level;
    }

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return unescapeJson(m.group(1));
        }
        return "";
    }

    private static int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static long extractLong(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return 0;
    }

    private static int[][] extractTiles(String json, int height, int width) {
        int[][] tiles = new int[height][width];

        // Find tiles array
        int tilesStart = json.indexOf("\"tiles\"");
        if (tilesStart < 0) return tiles;

        int arrayStart = json.indexOf("[", tilesStart);
        if (arrayStart < 0) return tiles;

        // Parse each row
        int row = 0;
        int pos = arrayStart + 1;

        while (row < height && pos < json.length()) {
            // Find next row array
            int rowStart = json.indexOf("[", pos);
            if (rowStart < 0) break;

            int rowEnd = json.indexOf("]", rowStart);
            if (rowEnd < 0) break;

            // Parse row values
            String rowStr = json.substring(rowStart + 1, rowEnd);
            String[] values = rowStr.split(",");

            for (int col = 0; col < Math.min(values.length, width); col++) {
                try {
                    tiles[row][col] = Integer.parseInt(values[col].trim());
                } catch (NumberFormatException e) {
                    tiles[row][col] = 0;
                }
            }

            row++;
            pos = rowEnd + 1;
        }

        return tiles;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
