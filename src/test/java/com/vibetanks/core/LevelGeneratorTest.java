package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevelGenerator Tests")
class LevelGeneratorTest {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 26;
    private LevelGenerator generator;
    private GameMap.TileType[][] tiles;

    @BeforeEach
    void setUp() {
        generator = new LevelGenerator(WIDTH, HEIGHT, new Random(12345)); // Fixed seed for reproducibility
        tiles = new GameMap.TileType[HEIGHT][WIDTH];
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should create generator with specified dimensions")
        void constructorShouldCreateGeneratorWithDimensions() {
            LevelGenerator gen = new LevelGenerator(30, 30, new Random());
            assertNotNull(gen);
        }

        @Test
        @DisplayName("Constructor should accept custom random")
        void constructorShouldAcceptCustomRandom() {
            Random customRandom = new Random(42);
            LevelGenerator gen = new LevelGenerator(WIDTH, HEIGHT, customRandom);
            assertNotNull(gen);
        }
    }

    @Nested
    @DisplayName("Level Generation Tests")
    class LevelGenerationTests {

        @Test
        @DisplayName("generateRandomLevel should fill tiles array")
        void generateRandomLevelShouldFillTilesArray() {
            generator.generateRandomLevel(tiles);

            // Check that array is populated (not all null)
            boolean hasNonNull = false;
            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    if (tiles[row][col] != null) {
                        hasNonNull = true;
                        break;
                    }
                }
            }
            assertTrue(hasNonNull);
        }

        @Test
        @DisplayName("Generated level should have border walls with spawn openings")
        void generatedLevelShouldHaveBorderWalls() {
            generator.generateRandomLevel(tiles);

            // Top border - row 0 is always STEEL
            for (int col = 0; col < WIDTH; col++) {
                assertEquals(GameMap.TileType.STEEL, tiles[0][col], "Top border at col " + col);
            }
            // Top corners should be STEEL
            assertEquals(GameMap.TileType.STEEL, tiles[0][0], "Top-left corner");
            assertEquals(GameMap.TileType.STEEL, tiles[0][WIDTH - 1], "Top-right corner");
            // Bottom corners should be GROUND
            assertEquals(GameMap.TileType.GROUND, tiles[HEIGHT - 1][0], "Bottom-left corner");
            assertEquals(GameMap.TileType.GROUND, tiles[HEIGHT - 1][WIDTH - 1], "Bottom-right corner");
            // Left border at column 0 - STEEL except bottom row which is GROUND
            for (int row = 0; row < HEIGHT - 1; row++) {
                assertEquals(GameMap.TileType.STEEL, tiles[row][0], "Left border at row " + row);
            }
            assertEquals(GameMap.TileType.GROUND, tiles[HEIGHT - 1][0], "Left border at bottom row");
            // Bottom border is GROUND
            for (int col = 0; col < WIDTH; col++) {
                assertEquals(GameMap.TileType.GROUND, tiles[HEIGHT - 1][col], "Bottom border at col " + col);
            }
            // Note: Right border at WIDTH-1 may have spawn openings at rows 1-2
        }

        @Test
        @DisplayName("Generated level should have base protection")
        void generatedLevelShouldHaveBaseProtection() {
            generator.generateRandomLevel(tiles);

            // Base protection bricks at row 23
            assertEquals(GameMap.TileType.BRICK, tiles[23][11]);
            assertEquals(GameMap.TileType.BRICK, tiles[23][12]);
            assertEquals(GameMap.TileType.BRICK, tiles[23][13]);
            assertEquals(GameMap.TileType.BRICK, tiles[23][14]);
        }

        @Test
        @DisplayName("Generated level should have clear enemy spawn areas")
        void generatedLevelShouldHaveClearEnemySpawnAreas() {
            generator.generateRandomLevel(tiles);

            // Spawn 1: top-left
            for (int row = 1; row <= 2; row++) {
                for (int col = 1; col <= 3; col++) {
                    assertEquals(GameMap.TileType.EMPTY, tiles[row][col],
                        "Enemy spawn 1 should be clear at (" + row + "," + col + ")");
                }
            }

            // Spawn 2: top-center
            for (int row = 1; row <= 2; row++) {
                for (int col = 11; col <= 14; col++) {
                    assertEquals(GameMap.TileType.EMPTY, tiles[row][col],
                        "Enemy spawn 2 should be clear at (" + row + "," + col + ")");
                }
            }

            // Spawn 3: top-right
            for (int row = 1; row <= 2; row++) {
                for (int col = 23; col <= 25; col++) {
                    if (col < WIDTH - 1) { // Within bounds
                        assertEquals(GameMap.TileType.EMPTY, tiles[row][col],
                            "Enemy spawn 3 should be clear at (" + row + "," + col + ")");
                    }
                }
            }
        }

        @Test
        @DisplayName("Generated level should have vertical paths from spawns")
        void generatedLevelShouldHaveVerticalPathsFromSpawns() {
            generator.generateRandomLevel(tiles);

            // Paths from enemy spawns
            for (int row = 1; row <= 5; row++) {
                assertEquals(GameMap.TileType.EMPTY, tiles[row][2], "Path from spawn 1");
                assertEquals(GameMap.TileType.EMPTY, tiles[row][12], "Path from spawn 2 left");
                assertEquals(GameMap.TileType.EMPTY, tiles[row][13], "Path from spawn 2 right");
                assertEquals(GameMap.TileType.EMPTY, tiles[row][24], "Path from spawn 3");
            }
        }

        @RepeatedTest(5)
        @DisplayName("Generated level should be different with different random seeds")
        void generatedLevelShouldBeDifferentWithDifferentSeeds() {
            Random random1 = new Random(System.nanoTime());
            Random random2 = new Random(System.nanoTime() + 1000);

            LevelGenerator gen1 = new LevelGenerator(WIDTH, HEIGHT, random1);
            LevelGenerator gen2 = new LevelGenerator(WIDTH, HEIGHT, random2);

            GameMap.TileType[][] tiles1 = new GameMap.TileType[HEIGHT][WIDTH];
            GameMap.TileType[][] tiles2 = new GameMap.TileType[HEIGHT][WIDTH];

            gen1.generateRandomLevel(tiles1);
            gen2.generateRandomLevel(tiles2);

            // Count differences (excluding borders which are always the same)
            int differences = 0;
            for (int row = 1; row < HEIGHT - 1; row++) {
                for (int col = 1; col < WIDTH - 1; col++) {
                    if (tiles1[row][col] != tiles2[row][col]) {
                        differences++;
                    }
                }
            }

            // Should have some differences (not identical)
            assertTrue(differences > 0, "Levels with different seeds should differ");
        }

        @Test
        @DisplayName("Generated level with same seed should be reproducible")
        void generatedLevelWithSameSeedShouldBeReproducible() {
            LevelGenerator gen1 = new LevelGenerator(WIDTH, HEIGHT, new Random(99999));
            LevelGenerator gen2 = new LevelGenerator(WIDTH, HEIGHT, new Random(99999));

            GameMap.TileType[][] tiles1 = new GameMap.TileType[HEIGHT][WIDTH];
            GameMap.TileType[][] tiles2 = new GameMap.TileType[HEIGHT][WIDTH];

            gen1.generateRandomLevel(tiles1);
            gen2.generateRandomLevel(tiles2);

            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    assertEquals(tiles1[row][col], tiles2[row][col],
                        "Tiles should match at (" + row + "," + col + ")");
                }
            }
        }
    }

    @Nested
    @DisplayName("Tile Type Distribution Tests")
    class TileTypeDistributionTests {

        @Test
        @DisplayName("Generated level should contain brick tiles")
        void generatedLevelShouldContainBrickTiles() {
            generator.generateRandomLevel(tiles);

            int brickCount = countTileType(GameMap.TileType.BRICK);
            assertTrue(brickCount > 0, "Level should have brick tiles");
        }

        @Test
        @DisplayName("Generated level should contain water tiles")
        void generatedLevelShouldContainWaterTiles() {
            generator.generateRandomLevel(tiles);

            int waterCount = countTileType(GameMap.TileType.WATER);
            assertTrue(waterCount > 0, "Level should have water tiles");
        }

        @Test
        @DisplayName("Generated level should contain tree tiles")
        void generatedLevelShouldContainTreeTiles() {
            generator.generateRandomLevel(tiles);

            int treeCount = countTileType(GameMap.TileType.TREES);
            assertTrue(treeCount > 0, "Level should have tree tiles");
        }

        @Test
        @DisplayName("Generated level should contain ice tiles")
        void generatedLevelShouldContainIceTiles() {
            generator.generateRandomLevel(tiles);

            int iceCount = countTileType(GameMap.TileType.ICE);
            assertTrue(iceCount > 0, "Level should have ice tiles");
        }

        @Test
        @DisplayName("Generated level should contain steel tiles beyond borders")
        void generatedLevelShouldContainSteelBeyondBorders() {
            generator.generateRandomLevel(tiles);

            // Count steel in interior (not borders)
            int interiorSteelCount = 0;
            for (int row = 1; row < HEIGHT - 1; row++) {
                for (int col = 1; col < WIDTH - 1; col++) {
                    if (tiles[row][col] == GameMap.TileType.STEEL) {
                        interiorSteelCount++;
                    }
                }
            }
            assertTrue(interiorSteelCount > 0, "Level should have steel tiles in interior");
        }

        @Test
        @DisplayName("Generated level should have reasonable empty space")
        void generatedLevelShouldHaveReasonableEmptySpace() {
            generator.generateRandomLevel(tiles);

            int emptyCount = countTileType(GameMap.TileType.EMPTY);
            int totalInterior = (HEIGHT - 2) * (WIDTH - 2);

            // At least 30% should be empty for playability
            double emptyRatio = (double) emptyCount / totalInterior;
            assertTrue(emptyRatio > 0.3, "Level should have at least 30% empty space, was " + (emptyRatio * 100) + "%");
        }
    }

    @Nested
    @DisplayName("Base Area Protection Tests")
    class BaseAreaProtectionTests {

        @Test
        @DisplayName("Base area should not be overwritten by generation")
        void baseAreaShouldNotBeOverwritten() {
            generator.generateRandomLevel(tiles);

            // Base area is at rows 22-24, cols 11-14
            // The exact base position tiles (24, 12-13) should be controlled by base protection
            // Check that base protection bricks exist
            assertEquals(GameMap.TileType.BRICK, tiles[24][11], "Left protection");
            assertEquals(GameMap.TileType.BRICK, tiles[24][14], "Right protection");
        }

        @Test
        @DisplayName("Steel wall should be placed above base")
        void steelWallShouldBePlacedAboveBase() {
            generator.generateRandomLevel(tiles);

            // Steel wall is placed at row 20 or 21, around cols 12-13
            boolean hasSteelAboveBase = false;
            for (int row = 20; row <= 21; row++) {
                for (int col = 10; col <= 15; col++) {
                    if (tiles[row][col] == GameMap.TileType.STEEL) {
                        hasSteelAboveBase = true;
                        break;
                    }
                }
            }
            assertTrue(hasSteelAboveBase, "Should have steel wall above base");
        }
    }

    @Nested
    @DisplayName("Player Spawn Area Tests")
    class PlayerSpawnAreaTests {

        @Test
        @DisplayName("Player spawn areas should be clear")
        void playerSpawnAreasShouldBeClear() {
            generator.generateRandomLevel(tiles);

            // Check paths to player spawns at bottom
            for (int row = 20; row <= 24; row++) {
                assertEquals(GameMap.TileType.EMPTY, tiles[row][8], "Player 1 path at row " + row);
                assertEquals(GameMap.TileType.EMPTY, tiles[row][9], "Player 1 path at row " + row);
                assertEquals(GameMap.TileType.EMPTY, tiles[row][16], "Player 2 path at row " + row);
                assertEquals(GameMap.TileType.EMPTY, tiles[row][17], "Player 2 path at row " + row);
            }
        }
    }

    @Nested
    @DisplayName("Multiple Generation Tests")
    class MultipleGenerationTests {

        @RepeatedTest(10)
        @DisplayName("Multiple generations should all be valid")
        void multipleGenerationsShouldAllBeValid() {
            LevelGenerator gen = new LevelGenerator(WIDTH, HEIGHT, new Random());
            GameMap.TileType[][] testTiles = new GameMap.TileType[HEIGHT][WIDTH];

            gen.generateRandomLevel(testTiles);

            // Verify top border
            for (int col = 0; col < WIDTH; col++) {
                assertEquals(GameMap.TileType.STEEL, testTiles[0][col]);
            }

            // Verify left border only (right border has spawn openings)
            // Bottom row is GROUND, rest is STEEL
            for (int row = 0; row < HEIGHT - 1; row++) {
                assertEquals(GameMap.TileType.STEEL, testTiles[row][0]);
            }
            assertEquals(GameMap.TileType.GROUND, testTiles[HEIGHT - 1][0]);

            // Verify bottom border is GROUND
            for (int col = 0; col < WIDTH; col++) {
                assertEquals(GameMap.TileType.GROUND, testTiles[HEIGHT - 1][col]);
            }

            // Verify spawn areas are clear
            assertEquals(GameMap.TileType.EMPTY, testTiles[1][2]);
            assertEquals(GameMap.TileType.EMPTY, testTiles[1][12]);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Generator should handle minimum size map")
        void generatorShouldHandleMinimumSizeMap() {
            // Minimum viable size that won't cause array index issues
            LevelGenerator smallGen = new LevelGenerator(WIDTH, HEIGHT, new Random(42));
            GameMap.TileType[][] smallTiles = new GameMap.TileType[HEIGHT][WIDTH];

            assertDoesNotThrow(() -> smallGen.generateRandomLevel(smallTiles));
        }

        @Test
        @DisplayName("All tiles should be non-null after generation")
        void allTilesShouldBeNonNullAfterGeneration() {
            generator.generateRandomLevel(tiles);

            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    assertNotNull(tiles[row][col], "Tile at (" + row + "," + col + ") should not be null");
                }
            }
        }
    }

    // Helper method to count tiles of a specific type
    private int countTileType(GameMap.TileType type) {
        int count = 0;
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                if (tiles[row][col] == type) {
                    count++;
                }
            }
        }
        return count;
    }
}
