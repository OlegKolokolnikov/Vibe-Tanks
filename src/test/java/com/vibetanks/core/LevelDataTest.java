package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevelData Tests")
class LevelDataTest {

    private LevelData levelData;

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor should create empty LevelData")
        void defaultConstructorCreatesEmptyLevelData() {
            levelData = new LevelData();

            assertEquals(0, levelData.getLevelNumber());
            assertNull(levelData.getName());
            assertNull(levelData.getAuthor());
            assertEquals(0, levelData.getWidth());
            assertEquals(0, levelData.getHeight());
            assertNull(levelData.getTiles());
        }

        @Test
        @DisplayName("Full constructor should set all properties")
        void fullConstructorSetsAllProperties() {
            int[][] tiles = {{1, 2}, {3, 4}};
            levelData = new LevelData(5, "Test Level", "Test Author", 26, 26, tiles);

            assertEquals(5, levelData.getLevelNumber());
            assertEquals("Test Level", levelData.getName());
            assertEquals("Test Author", levelData.getAuthor());
            assertEquals(26, levelData.getWidth());
            assertEquals(26, levelData.getHeight());
            assertNotNull(levelData.getTiles());
            assertTrue(levelData.getCreatedAt() > 0);
        }

        @Test
        @DisplayName("Constructor should set createdAt to current time")
        void constructorSetsCreatedAtToCurrentTime() {
            long before = System.currentTimeMillis();
            levelData = new LevelData(1, "Test", "Author", 10, 10, new int[10][10]);
            long after = System.currentTimeMillis();

            assertTrue(levelData.getCreatedAt() >= before);
            assertTrue(levelData.getCreatedAt() <= after);
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @BeforeEach
        void setUp() {
            levelData = new LevelData();
        }

        @Test
        @DisplayName("setLevelNumber should update level number")
        void setLevelNumberUpdatesLevelNumber() {
            levelData.setLevelNumber(10);
            assertEquals(10, levelData.getLevelNumber());
        }

        @Test
        @DisplayName("setName should update name")
        void setNameUpdatesName() {
            levelData.setName("New Name");
            assertEquals("New Name", levelData.getName());
        }

        @Test
        @DisplayName("setAuthor should update author")
        void setAuthorUpdatesAuthor() {
            levelData.setAuthor("New Author");
            assertEquals("New Author", levelData.getAuthor());
        }

        @Test
        @DisplayName("setCreatedAt should update timestamp")
        void setCreatedAtUpdatesTimestamp() {
            levelData.setCreatedAt(123456789L);
            assertEquals(123456789L, levelData.getCreatedAt());
        }

        @Test
        @DisplayName("setWidth should update width")
        void setWidthUpdatesWidth() {
            levelData.setWidth(32);
            assertEquals(32, levelData.getWidth());
        }

        @Test
        @DisplayName("setHeight should update height")
        void setHeightUpdatesHeight() {
            levelData.setHeight(32);
            assertEquals(32, levelData.getHeight());
        }

        @Test
        @DisplayName("setTiles should update tiles")
        void setTilesUpdatesTiles() {
            int[][] tiles = {{1, 2, 3}, {4, 5, 6}};
            levelData.setTiles(tiles);

            assertArrayEquals(tiles, levelData.getTiles());
        }
    }

    @Nested
    @DisplayName("Tiles Copy Tests")
    class TilesCopyTests {

        @Test
        @DisplayName("getTilesCopy should return null if tiles is null")
        void getTilesCopyReturnsNullIfTilesNull() {
            levelData = new LevelData();

            assertNull(levelData.getTilesCopy());
        }

        @Test
        @DisplayName("getTilesCopy should return deep copy of tiles")
        void getTilesCopyReturnsDeepCopy() {
            int[][] originalTiles = {{1, 2}, {3, 4}};
            levelData = new LevelData(1, "Test", "Author", 2, 2, originalTiles);

            int[][] copy = levelData.getTilesCopy();

            // Verify values are the same
            assertArrayEquals(originalTiles[0], copy[0]);
            assertArrayEquals(originalTiles[1], copy[1]);

            // Verify it's a different array (deep copy)
            assertNotSame(originalTiles, copy);
            assertNotSame(originalTiles[0], copy[0]);
            assertNotSame(originalTiles[1], copy[1]);
        }

        @Test
        @DisplayName("Modifying copy should not affect original tiles")
        void modifyingCopyShouldNotAffectOriginal() {
            int[][] originalTiles = {{1, 2}, {3, 4}};
            levelData = new LevelData(1, "Test", "Author", 2, 2, originalTiles);

            int[][] copy = levelData.getTilesCopy();
            copy[0][0] = 999;

            assertEquals(1, levelData.getTiles()[0][0]);
        }

        @Test
        @DisplayName("getTilesCopy should handle different array sizes")
        void getTilesCopyHandlesDifferentArraySizes() {
            int[][] rectangularTiles = {
                {1, 2, 3, 4, 5},
                {6, 7, 8, 9, 10},
                {11, 12, 13, 14, 15}
            };
            levelData = new LevelData(1, "Test", "Author", 5, 3, rectangularTiles);

            int[][] copy = levelData.getTilesCopy();

            assertEquals(3, copy.length);
            assertEquals(5, copy[0].length);
            assertEquals(5, copy[1].length);
            assertEquals(5, copy[2].length);
        }
    }

    @Nested
    @DisplayName("Tiles Array Tests")
    class TilesArrayTests {

        @Test
        @DisplayName("Tiles should preserve all values")
        void tilesPreserveAllValues() {
            int[][] tiles = {
                {0, 1, 2, 3},
                {4, 5, 6, 7},
                {8, 9, 10, 11}
            };
            levelData = new LevelData(1, "Test", "Author", 4, 3, tiles);

            int[][] result = levelData.getTiles();

            for (int i = 0; i < tiles.length; i++) {
                for (int j = 0; j < tiles[i].length; j++) {
                    assertEquals(tiles[i][j], result[i][j],
                        "Tile at [" + i + "][" + j + "] should match");
                }
            }
        }

        @Test
        @DisplayName("Empty tiles array should be handled")
        void emptyTilesArrayShouldBeHandled() {
            int[][] emptyTiles = new int[0][0];
            levelData = new LevelData(1, "Test", "Author", 0, 0, emptyTiles);

            assertNotNull(levelData.getTiles());
            assertEquals(0, levelData.getTiles().length);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Null name and author should be allowed")
        void nullNameAndAuthorShouldBeAllowed() {
            levelData = new LevelData(1, null, null, 10, 10, new int[10][10]);

            assertNull(levelData.getName());
            assertNull(levelData.getAuthor());
        }

        @Test
        @DisplayName("Empty strings for name and author should be allowed")
        void emptyStringsForNameAndAuthorShouldBeAllowed() {
            levelData = new LevelData(1, "", "", 10, 10, new int[10][10]);

            assertEquals("", levelData.getName());
            assertEquals("", levelData.getAuthor());
        }

        @Test
        @DisplayName("Zero dimensions should be allowed")
        void zeroDimensionsShouldBeAllowed() {
            levelData = new LevelData();
            levelData.setWidth(0);
            levelData.setHeight(0);

            assertEquals(0, levelData.getWidth());
            assertEquals(0, levelData.getHeight());
        }

        @Test
        @DisplayName("Negative level number should be allowed (no validation)")
        void negativeLevelNumberShouldBeAllowed() {
            levelData = new LevelData();
            levelData.setLevelNumber(-1);

            assertEquals(-1, levelData.getLevelNumber());
        }
    }
}
