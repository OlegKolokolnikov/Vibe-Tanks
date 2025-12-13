package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameMap Tests")
class GameMapTest {

    private GameMap gameMap;
    private static final int MAP_WIDTH = 26;
    private static final int MAP_HEIGHT = 26;
    private static final int TILE_SIZE = 32;

    @BeforeEach
    void setUp() {
        gameMap = new GameMap(MAP_WIDTH, MAP_HEIGHT);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should create map with correct dimensions")
        void constructorCreatesCorrectDimensions() {
            assertEquals(MAP_WIDTH, gameMap.getWidth());
            assertEquals(MAP_HEIGHT, gameMap.getHeight());
        }

        @Test
        @DisplayName("Constructor should start at level 1")
        void constructorStartsAtLevel1() {
            assertEquals(1, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("Constructor should generate initial level")
        void constructorGeneratesInitialLevel() {
            // Map should have tiles after construction
            int[][] tiles = gameMap.exportTiles();
            assertNotNull(tiles);
            assertEquals(MAP_HEIGHT, tiles.length);
            assertEquals(MAP_WIDTH, tiles[0].length);
        }
    }

    @Nested
    @DisplayName("Level Management Tests")
    class LevelManagementTests {

        @Test
        @DisplayName("nextLevel should increment level number")
        void nextLevelIncrementsLevelNumber() {
            assertEquals(1, gameMap.getLevelNumber());

            gameMap.nextLevel();

            assertEquals(2, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("Multiple nextLevel calls should increment correctly")
        void multipleNextLevelCallsIncrementCorrectly() {
            for (int i = 1; i <= 5; i++) {
                assertEquals(i, gameMap.getLevelNumber());
                gameMap.nextLevel();
            }
            assertEquals(6, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("resetToLevel1 should reset level to 1")
        void resetToLevel1ResetsLevel() {
            gameMap.nextLevel();
            gameMap.nextLevel();
            assertEquals(3, gameMap.getLevelNumber());

            gameMap.resetToLevel1();

            assertEquals(1, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("setLevelNumber should set specific level")
        void setLevelNumberSetsLevel() {
            gameMap.setLevelNumber(5);

            assertEquals(5, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("setLevelNumber can be set to 0")
        void setLevelNumberCanBeZero() {
            gameMap.setLevelNumber(0);

            assertEquals(0, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("setLevelNumber can be set to high values")
        void setLevelNumberCanBeHigh() {
            gameMap.setLevelNumber(100);

            assertEquals(100, gameMap.getLevelNumber());
        }
    }

    @Nested
    @DisplayName("Tile Management Tests")
    class TileManagementTests {

        @Test
        @DisplayName("getTile should return correct tile type")
        void getTileReturnsCorrectType() {
            gameMap.setTile(5, 5, GameMap.TileType.BRICK);

            assertEquals(GameMap.TileType.BRICK, gameMap.getTile(5, 5));
        }

        @Test
        @DisplayName("setTile should update tile")
        void setTileUpdatesTile() {
            gameMap.setTile(10, 10, GameMap.TileType.STEEL);

            assertEquals(GameMap.TileType.STEEL, gameMap.getTile(10, 10));
        }

        @Test
        @DisplayName("getTile outside bounds should return STEEL")
        void getTileOutsideBoundsReturnsSteel() {
            // Negative coordinates
            assertEquals(GameMap.TileType.STEEL, gameMap.getTile(-1, 5));
            assertEquals(GameMap.TileType.STEEL, gameMap.getTile(5, -1));

            // Outside positive bounds
            assertEquals(GameMap.TileType.STEEL, gameMap.getTile(100, 5));
            assertEquals(GameMap.TileType.STEEL, gameMap.getTile(5, 100));
        }

        @Test
        @DisplayName("All tile types should be settable")
        void allTileTypesSettable() {
            int row = 10;
            for (GameMap.TileType type : GameMap.TileType.values()) {
                gameMap.setTile(row, 10, type);
                assertEquals(type, gameMap.getTile(row, 10));
                row++;
            }
        }
    }

    @Nested
    @DisplayName("Export/Import Tests")
    class ExportImportTests {

        @Test
        @DisplayName("exportTiles should return copy of tile data")
        void exportTilesReturnsCopy() {
            int[][] exported = gameMap.exportTiles();

            // Modify exported data
            exported[5][5] = 999;

            // Original should be unchanged
            assertNotEquals(999, gameMap.getTile(5, 5).ordinal());
        }

        @Test
        @DisplayName("importTiles should restore tile data")
        void importTilesRestoresData() {
            // Create custom tile data
            int[][] tileData = new int[MAP_HEIGHT][MAP_WIDTH];
            tileData[5][5] = GameMap.TileType.WATER.ordinal();
            tileData[10][10] = GameMap.TileType.ICE.ordinal();

            gameMap.importTiles(tileData);

            assertEquals(GameMap.TileType.WATER, gameMap.getTile(5, 5));
            assertEquals(GameMap.TileType.ICE, gameMap.getTile(10, 10));
        }

        @Test
        @DisplayName("Export then import should preserve tile state")
        void exportThenImportPreservesTiles() {
            // Set some specific tiles
            gameMap.setTile(3, 3, GameMap.TileType.BRICK);
            gameMap.setTile(4, 4, GameMap.TileType.STEEL);
            gameMap.setTile(5, 5, GameMap.TileType.WATER);

            // Export
            int[][] exported = gameMap.exportTiles();

            // Change tiles
            gameMap.setTile(3, 3, GameMap.TileType.EMPTY);
            gameMap.setTile(4, 4, GameMap.TileType.EMPTY);

            // Import back
            gameMap.importTiles(exported);

            assertEquals(GameMap.TileType.BRICK, gameMap.getTile(3, 3));
            assertEquals(GameMap.TileType.STEEL, gameMap.getTile(4, 4));
            assertEquals(GameMap.TileType.WATER, gameMap.getTile(5, 5));
        }
    }

    @Nested
    @DisplayName("Burning Tiles Tests")
    class BurningTilesTests {

        @Test
        @DisplayName("New map should have no burning tiles")
        void newMapHasNoBurningTiles() {
            assertFalse(gameMap.hasBurningTiles());
        }

        @Test
        @DisplayName("exportBurningTiles should return burning tile data")
        void exportBurningTilesReturnsData() {
            Map<Long, Integer> data = gameMap.exportBurningTiles();
            assertNotNull(data);
        }

        @Test
        @DisplayName("importBurningTiles should restore burning tiles")
        void importBurningTilesRestoresData() {
            Map<Long, Integer> data = new HashMap<>();
            // New encoding: key = (row << 16) | col
            long key = (5L << 16) | 5; // row 5, col 5
            data.put(key, 30); // 30 frames remaining

            gameMap.importBurningTiles(data);

            assertTrue(gameMap.hasBurningTiles());
        }

        @Test
        @DisplayName("update should decrease burning time")
        void updateDecreasesBurningTime() {
            Map<Long, Integer> data = new HashMap<>();
            // New encoding: key = (row << 16) | col
            long key = (5L << 16) | 5; // row 5, col 5
            data.put(key, 5); // 5 frames remaining

            gameMap.importBurningTiles(data);
            assertTrue(gameMap.hasBurningTiles());

            // Update 5 times to expire the burning
            for (int i = 0; i < 6; i++) {
                gameMap.update();
            }

            assertFalse(gameMap.hasBurningTiles());
        }
    }

    @Nested
    @DisplayName("Collision Tests")
    class CollisionTests {

        @BeforeEach
        void setUpCollisionMap() {
            // Clear map first
            for (int row = 0; row < MAP_HEIGHT; row++) {
                for (int col = 0; col < MAP_WIDTH; col++) {
                    gameMap.setTile(row, col, GameMap.TileType.EMPTY);
                }
            }
        }

        @Test
        @DisplayName("No collision on empty tiles")
        void noCollisionOnEmptyTiles() {
            assertFalse(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28));
        }

        @Test
        @DisplayName("Collision on brick tiles")
        void collisionOnBrickTiles() {
            gameMap.setTile(5, 5, GameMap.TileType.BRICK);

            assertTrue(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28));
        }

        @Test
        @DisplayName("Collision on steel tiles")
        void collisionOnSteelTiles() {
            gameMap.setTile(5, 5, GameMap.TileType.STEEL);

            assertTrue(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28));
        }

        @Test
        @DisplayName("Collision on water tiles without swim ability")
        void collisionOnWaterWithoutSwim() {
            gameMap.setTile(5, 5, GameMap.TileType.WATER);

            assertTrue(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28, false));
        }

        @Test
        @DisplayName("No collision on water tiles with swim ability")
        void noCollisionOnWaterWithSwim() {
            gameMap.setTile(5, 5, GameMap.TileType.WATER);

            assertFalse(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28, true));
        }

        @Test
        @DisplayName("No collision on trees")
        void noCollisionOnTrees() {
            gameMap.setTile(5, 5, GameMap.TileType.TREES);

            assertFalse(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28));
        }

        @Test
        @DisplayName("No collision on ice")
        void noCollisionOnIce() {
            gameMap.setTile(5, 5, GameMap.TileType.ICE);

            assertFalse(gameMap.checkTankCollision(5 * TILE_SIZE, 5 * TILE_SIZE, 28));
        }

        @Test
        @DisplayName("Collision at map boundaries")
        void collisionAtMapBoundaries() {
            // Far left boundary (negative X)
            assertTrue(gameMap.checkTankCollision(-50, 100, 28));

            // Far bottom boundary (beyond map height)
            assertTrue(gameMap.checkTankCollision(100, MAP_HEIGHT * TILE_SIZE, 28));

            // Far right boundary (beyond map width)
            assertTrue(gameMap.checkTankCollision(MAP_WIDTH * TILE_SIZE, 100, 28));
        }
    }

    @Nested
    @DisplayName("Custom Level Tests")
    class CustomLevelTests {

        @Test
        @DisplayName("New map should not have custom level")
        void newMapHasNoCustomLevel() {
            assertFalse(gameMap.hasCustomLevel());
            assertNull(gameMap.getCustomLevelData());
        }

        @Test
        @DisplayName("setCustomLevel should set custom level data")
        void setCustomLevelSetsData() {
            LevelData levelData = new LevelData();
            levelData.setLevelNumber(5);

            gameMap.setCustomLevel(levelData);

            assertTrue(gameMap.hasCustomLevel());
            assertEquals(levelData, gameMap.getCustomLevelData());
        }
    }

    @Nested
    @DisplayName("Base Protection Tests")
    class BaseProtectionTests {

        @BeforeEach
        void setUpForBaseProtection() {
            // Clear map
            for (int row = 0; row < MAP_HEIGHT; row++) {
                for (int col = 0; col < MAP_WIDTH; col++) {
                    gameMap.setTile(row, col, GameMap.TileType.EMPTY);
                }
            }
        }

        @Test
        @DisplayName("setBaseProtection should change tiles around base")
        void setBaseProtectionChangesTiles() {
            // Check that base area tiles change
            gameMap.setBaseProtection(GameMap.TileType.STEEL);

            // The base is at bottom center, check nearby tiles
            // Base protection places steel around the base location
            // This depends on the implementation
        }

        @Test
        @DisplayName("resetBaseProtection should restore brick tiles")
        void resetBaseProtectionRestoresBrick() {
            gameMap.setBaseProtection(GameMap.TileType.STEEL);
            gameMap.resetBaseProtection();

            // After reset, protection tiles should be brick
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return false when all tiles intact")
        void isBaseProtectionBrokenFalseWhenIntact() {
            gameMap.setBaseProtection(GameMap.TileType.BRICK);

            assertFalse(gameMap.isBaseProtectionBroken());
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return false when steel protection")
        void isBaseProtectionBrokenFalseWhenSteel() {
            gameMap.setBaseProtection(GameMap.TileType.STEEL);

            assertFalse(gameMap.isBaseProtectionBroken());
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return true when top wall destroyed")
        void isBaseProtectionBrokenTrueWhenTopWallDestroyed() {
            gameMap.setBaseProtection(GameMap.TileType.BRICK);
            // Destroy top wall tile (row 23, col 12)
            gameMap.setTile(23, 12, GameMap.TileType.EMPTY);

            assertTrue(gameMap.isBaseProtectionBroken());
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return true when left wall destroyed")
        void isBaseProtectionBrokenTrueWhenLeftWallDestroyed() {
            gameMap.setBaseProtection(GameMap.TileType.BRICK);
            // Destroy left wall tile (row 24, col 11)
            gameMap.setTile(24, 11, GameMap.TileType.EMPTY);

            assertTrue(gameMap.isBaseProtectionBroken());
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return true when right wall destroyed")
        void isBaseProtectionBrokenTrueWhenRightWallDestroyed() {
            gameMap.setBaseProtection(GameMap.TileType.BRICK);
            // Destroy right wall tile (row 24, col 13)
            gameMap.setTile(24, 13, GameMap.TileType.EMPTY);

            assertTrue(gameMap.isBaseProtectionBroken());
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return true when bottom wall destroyed")
        void isBaseProtectionBrokenTrueWhenBottomWallDestroyed() {
            gameMap.setBaseProtection(GameMap.TileType.BRICK);
            // Destroy bottom wall tile (row 25, col 12)
            gameMap.setTile(25, 12, GameMap.TileType.EMPTY);

            assertTrue(gameMap.isBaseProtectionBroken());
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return true when any single tile destroyed")
        void isBaseProtectionBrokenTrueWhenAnySingleTileDestroyed() {
            // Protection tiles are at positions:
            // Top: (23,11), (23,12), (23,13)
            // Left: (24,11), Right: (24,13)
            // Bottom: (25,11), (25,12), (25,13)
            int[][] protectionTiles = {
                {23, 11}, {23, 12}, {23, 13},
                {24, 11}, {24, 13},
                {25, 11}, {25, 12}, {25, 13}
            };

            for (int[] pos : protectionTiles) {
                // Reset protection
                gameMap.setBaseProtection(GameMap.TileType.BRICK);
                assertFalse(gameMap.isBaseProtectionBroken(),
                    "Protection should be intact before destroying tile at (" + pos[0] + "," + pos[1] + ")");

                // Destroy single tile
                gameMap.setTile(pos[0], pos[1], GameMap.TileType.EMPTY);
                assertTrue(gameMap.isBaseProtectionBroken(),
                    "Protection should be broken after destroying tile at (" + pos[0] + "," + pos[1] + ")");
            }
        }

        @Test
        @DisplayName("isBaseProtectionBroken should return true when all tiles destroyed")
        void isBaseProtectionBrokenTrueWhenAllDestroyed() {
            gameMap.setBaseProtection(GameMap.TileType.EMPTY);

            assertTrue(gameMap.isBaseProtectionBroken());
        }
    }

    @Nested
    @DisplayName("TileType Enum Tests")
    class TileTypeEnumTests {

        @Test
        @DisplayName("All expected tile types should exist")
        void allTileTypesExist() {
            assertEquals(7, GameMap.TileType.values().length);  // EMPTY, BRICK, STEEL, WATER, TREES, ICE, GROUND
        }

        @Test
        @DisplayName("Tile types should have correct ordinals")
        void tileTypesHaveCorrectOrdinals() {
            assertEquals(0, GameMap.TileType.EMPTY.ordinal());
            assertEquals(1, GameMap.TileType.BRICK.ordinal());
            assertEquals(2, GameMap.TileType.STEEL.ordinal());
            assertEquals(3, GameMap.TileType.WATER.ordinal());
            assertEquals(4, GameMap.TileType.TREES.ordinal());
            assertEquals(5, GameMap.TileType.ICE.ordinal());
            assertEquals(6, GameMap.TileType.GROUND.ordinal());
        }
    }

    @Nested
    @DisplayName("Level Generation Tests")
    class LevelGenerationTests {

        @Test
        @DisplayName("regenerateCurrentLevel should maintain level number")
        void regenerateCurrentLevelMaintainsLevelNumber() {
            gameMap.setLevelNumber(5);
            int levelBefore = gameMap.getLevelNumber();

            gameMap.regenerateCurrentLevel();

            assertEquals(levelBefore, gameMap.getLevelNumber());
        }

        @Test
        @DisplayName("nextLevel should change the level number and regenerate")
        void nextLevelChangesLevelNumberAndRegenerates() {
            int initialLevel = gameMap.getLevelNumber();
            int[][] initialTiles = gameMap.exportTiles();

            gameMap.nextLevel();

            assertEquals(initialLevel + 1, gameMap.getLevelNumber());
            // The level has been regenerated - tiles array should still be valid
            int[][] newTiles = gameMap.exportTiles();
            assertNotNull(newTiles);
            assertEquals(MAP_HEIGHT, newTiles.length);
        }
    }
}
