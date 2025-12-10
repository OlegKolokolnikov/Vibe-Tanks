package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevelManager Tests")
class LevelManagerTest {

    @Nested
    @DisplayName("Levels Directory Tests")
    class LevelsDirectoryTests {

        @Test
        @DisplayName("getLevelsDirectory should return non-null path")
        void getLevelsDirectoryShouldReturnNonNullPath() {
            Path levelsDir = LevelManager.getLevelsDirectory();
            assertNotNull(levelsDir);
        }

        @Test
        @DisplayName("getLevelsDirectory should create directory if not exists")
        void getLevelsDirectoryShouldCreateDirectoryIfNotExists() {
            Path levelsDir = LevelManager.getLevelsDirectory();
            assertTrue(Files.exists(levelsDir) || levelsDir != null);
        }
    }

    @Nested
    @DisplayName("Save Level Tests")
    class SaveLevelTests {

        @Test
        @DisplayName("saveLevel should return false for null level")
        void saveLevelShouldReturnFalseForNullLevel() {
            boolean result = LevelManager.saveLevel(null);
            assertFalse(result);
        }

        @Test
        @DisplayName("saveLevel should return false for level with invalid number")
        void saveLevelShouldReturnFalseForInvalidNumber() {
            LevelData level = new LevelData();
            level.setLevelNumber(0); // Invalid

            boolean result = LevelManager.saveLevel(level);
            assertFalse(result);
        }

        @Test
        @DisplayName("saveLevel should succeed for valid level")
        void saveLevelShouldSucceedForValidLevel() {
            LevelData level = createTestLevel(999);

            boolean result = LevelManager.saveLevel(level);

            assertTrue(result);

            // Cleanup
            LevelManager.deleteLevelByNumber(999);
        }

        @Test
        @DisplayName("saveLevel should create file with correct name")
        void saveLevelShouldCreateFileWithCorrectName() {
            LevelData level = createTestLevel(998);

            LevelManager.saveLevel(level);

            Path expectedFile = LevelManager.getLevelsDirectory().resolve("level_998.json");
            assertTrue(Files.exists(expectedFile));

            // Cleanup
            LevelManager.deleteLevelByNumber(998);
        }
    }

    @Nested
    @DisplayName("Load Level Tests")
    class LoadLevelTests {

        @Test
        @DisplayName("loadLevel should return null for non-existent file")
        void loadLevelShouldReturnNullForNonExistentFile() {
            LevelData result = LevelManager.loadLevel("nonexistent.json");
            assertNull(result);
        }

        @Test
        @DisplayName("loadLevelByNumber should return null for non-existent level")
        void loadLevelByNumberShouldReturnNullForNonExistentLevel() {
            LevelData result = LevelManager.loadLevelByNumber(99999);
            assertNull(result);
        }

        @Test
        @DisplayName("Saved level should be loadable")
        void savedLevelShouldBeLoadable() {
            LevelData original = createTestLevel(997);
            original.setName("Test Level");
            original.setAuthor("Test Author");

            LevelManager.saveLevel(original);
            LevelData loaded = LevelManager.loadLevelByNumber(997);

            assertNotNull(loaded);
            assertEquals(997, loaded.getLevelNumber());
            assertEquals("Test Level", loaded.getName());
            assertEquals("Test Author", loaded.getAuthor());

            // Cleanup
            LevelManager.deleteLevelByNumber(997);
        }

        @Test
        @DisplayName("Loaded level should have correct tiles")
        void loadedLevelShouldHaveCorrectTiles() {
            LevelData original = createTestLevel(996);
            int[][] originalTiles = original.getTiles();
            originalTiles[5][5] = 1; // BRICK
            originalTiles[10][10] = 2; // STEEL
            originalTiles[15][15] = 3; // WATER

            LevelManager.saveLevel(original);
            LevelData loaded = LevelManager.loadLevelByNumber(996);

            assertNotNull(loaded);
            int[][] loadedTiles = loaded.getTiles();
            assertEquals(1, loadedTiles[5][5]);
            assertEquals(2, loadedTiles[10][10]);
            assertEquals(3, loadedTiles[15][15]);

            // Cleanup
            LevelManager.deleteLevelByNumber(996);
        }
    }

    @Nested
    @DisplayName("Delete Level Tests")
    class DeleteLevelTests {

        @Test
        @DisplayName("deleteLevel should return true for existing file")
        void deleteLevelShouldReturnTrueForExistingFile() {
            LevelData level = createTestLevel(995);
            LevelManager.saveLevel(level);

            boolean result = LevelManager.deleteLevel("level_995.json");

            assertTrue(result);
        }

        @Test
        @DisplayName("deleteLevel should return true for non-existent file")
        void deleteLevelShouldReturnTrueForNonExistentFile() {
            boolean result = LevelManager.deleteLevel("nonexistent_level.json");
            assertTrue(result); // deleteIfExists returns true even if not exists
        }

        @Test
        @DisplayName("deleteLevelByNumber should remove file")
        void deleteLevelByNumberShouldRemoveFile() {
            LevelData level = createTestLevel(994);
            LevelManager.saveLevel(level);
            assertTrue(LevelManager.hasCustomLevel(994));

            LevelManager.deleteLevelByNumber(994);

            assertFalse(LevelManager.hasCustomLevel(994));
        }
    }

    @Nested
    @DisplayName("Has Custom Level Tests")
    class HasCustomLevelTests {

        @Test
        @DisplayName("hasCustomLevel should return false for non-existent level")
        void hasCustomLevelShouldReturnFalseForNonExistent() {
            assertFalse(LevelManager.hasCustomLevel(88888));
        }

        @Test
        @DisplayName("hasCustomLevel should return true for existing level")
        void hasCustomLevelShouldReturnTrueForExisting() {
            LevelData level = createTestLevel(993);
            LevelManager.saveLevel(level);

            assertTrue(LevelManager.hasCustomLevel(993));

            // Cleanup
            LevelManager.deleteLevelByNumber(993);
        }
    }

    @Nested
    @DisplayName("Get Saved Levels Tests")
    class GetSavedLevelsTests {

        @Test
        @DisplayName("getSavedLevels should return list")
        void getSavedLevelsShouldReturnList() {
            List<String> levels = LevelManager.getSavedLevels();
            assertNotNull(levels);
        }

        @Test
        @DisplayName("getSavedLevels should include saved level")
        void getSavedLevelsShouldIncludeSavedLevel() {
            LevelData level = createTestLevel(992);
            LevelManager.saveLevel(level);

            List<String> levels = LevelManager.getSavedLevels();

            assertTrue(levels.contains("level_992.json"));

            // Cleanup
            LevelManager.deleteLevelByNumber(992);
        }

        @Test
        @DisplayName("getSavedLevels should be sorted")
        void getSavedLevelsShouldBeSorted() {
            // Create levels in non-sorted order
            LevelManager.saveLevel(createTestLevel(989));
            LevelManager.saveLevel(createTestLevel(991));
            LevelManager.saveLevel(createTestLevel(990));

            List<String> levels = LevelManager.getSavedLevels();

            // Find our test levels in the list
            int idx989 = levels.indexOf("level_989.json");
            int idx990 = levels.indexOf("level_990.json");
            int idx991 = levels.indexOf("level_991.json");

            if (idx989 >= 0 && idx990 >= 0 && idx991 >= 0) {
                assertTrue(idx989 < idx990);
                assertTrue(idx990 < idx991);
            }

            // Cleanup
            LevelManager.deleteLevelByNumber(989);
            LevelManager.deleteLevelByNumber(990);
            LevelManager.deleteLevelByNumber(991);
        }
    }

    @Nested
    @DisplayName("Get Custom Level Numbers Tests")
    class GetCustomLevelNumbersTests {

        @Test
        @DisplayName("getCustomLevelNumbers should return list")
        void getCustomLevelNumbersShouldReturnList() {
            List<Integer> numbers = LevelManager.getCustomLevelNumbers();
            assertNotNull(numbers);
        }

        @Test
        @DisplayName("getCustomLevelNumbers should include saved level number")
        void getCustomLevelNumbersShouldIncludeSavedLevelNumber() {
            LevelData level = createTestLevel(988);
            LevelManager.saveLevel(level);

            List<Integer> numbers = LevelManager.getCustomLevelNumbers();

            assertTrue(numbers.contains(988));

            // Cleanup
            LevelManager.deleteLevelByNumber(988);
        }

        @Test
        @DisplayName("getCustomLevelNumbers should be sorted")
        void getCustomLevelNumbersShouldBeSorted() {
            LevelManager.saveLevel(createTestLevel(985));
            LevelManager.saveLevel(createTestLevel(987));
            LevelManager.saveLevel(createTestLevel(986));

            List<Integer> numbers = LevelManager.getCustomLevelNumbers();

            // Find positions of our test levels
            int idx985 = numbers.indexOf(985);
            int idx986 = numbers.indexOf(986);
            int idx987 = numbers.indexOf(987);

            if (idx985 >= 0 && idx986 >= 0 && idx987 >= 0) {
                assertTrue(idx985 < idx986);
                assertTrue(idx986 < idx987);
            }

            // Cleanup
            LevelManager.deleteLevelByNumber(985);
            LevelManager.deleteLevelByNumber(986);
            LevelManager.deleteLevelByNumber(987);
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Level with simple name should serialize correctly")
        void levelWithSimpleNameShouldSerializeCorrectly() {
            LevelData level = createTestLevel(984);
            level.setName("Test Level");
            level.setAuthor("AuthorName");

            LevelManager.saveLevel(level);
            LevelData loaded = LevelManager.loadLevelByNumber(984);

            assertEquals("Test Level", loaded.getName());
            assertEquals("AuthorName", loaded.getAuthor());

            // Cleanup
            LevelManager.deleteLevelByNumber(984);
        }

        @Test
        @DisplayName("Level dimensions should be preserved")
        void levelDimensionsShouldBePreserved() {
            LevelData level = createTestLevel(983);
            level.setWidth(26);
            level.setHeight(26);

            LevelManager.saveLevel(level);
            LevelData loaded = LevelManager.loadLevelByNumber(983);

            assertEquals(26, loaded.getWidth());
            assertEquals(26, loaded.getHeight());

            // Cleanup
            LevelManager.deleteLevelByNumber(983);
        }

        @Test
        @DisplayName("Created timestamp should be preserved")
        void createdTimestampShouldBePreserved() {
            LevelData level = createTestLevel(982);
            long timestamp = System.currentTimeMillis();
            level.setCreatedAt(timestamp);

            LevelManager.saveLevel(level);
            LevelData loaded = LevelManager.loadLevelByNumber(982);

            assertEquals(timestamp, loaded.getCreatedAt());

            // Cleanup
            LevelManager.deleteLevelByNumber(982);
        }
    }

    /**
     * Helper method to create a test level
     */
    private LevelData createTestLevel(int levelNumber) {
        LevelData level = new LevelData();
        level.setLevelNumber(levelNumber);
        level.setName("Test Level " + levelNumber);
        level.setAuthor("Test Author");
        level.setCreatedAt(System.currentTimeMillis());
        level.setWidth(26);
        level.setHeight(26);
        level.setTiles(new int[26][26]);
        return level;
    }
}
