package com.vibetanks.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpatialGrid")
class SpatialGridTest {

    private SpatialGrid<String> grid;
    private static final int MAP_SIZE = 832; // 26 tiles * 32 pixels

    @BeforeEach
    void setUp() {
        grid = new SpatialGrid<>(MAP_SIZE, MAP_SIZE, 64);
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("clear removes all entities")
        void clearRemovesAllEntities() {
            grid.insert("entity1", 100, 100);
            grid.insert("entity2", 200, 200);
            grid.clear();

            List<String> result = grid.getNearby(100, 100);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("insert adds entity to correct cell")
        void insertAddsEntityToCorrectCell() {
            grid.insert("entity", 100, 100);

            List<String> result = grid.getInCell(100, 100);
            assertTrue(result.contains("entity"));
        }

        @Test
        @DisplayName("insert at boundary positions works")
        void insertAtBoundaryPositions() {
            grid.insert("corner", 0, 0);
            grid.insert("edge", MAP_SIZE - 1, 0);

            List<String> corner = grid.getInCell(0, 0);
            assertTrue(corner.contains("corner"));

            List<String> edge = grid.getInCell(MAP_SIZE - 1, 0);
            assertTrue(edge.contains("edge"));
        }
    }

    @Nested
    @DisplayName("Nearby Queries")
    class NearbyQueries {

        @Test
        @DisplayName("getNearby returns entities in same cell")
        void getNearbyReturnsSameCell() {
            grid.insert("entity", 100, 100);

            List<String> result = grid.getNearby(100, 100);
            assertTrue(result.contains("entity"));
        }

        @Test
        @DisplayName("getNearby returns entities in adjacent cells")
        void getNearbyReturnsAdjacentCells() {
            // Insert in center of a cell
            grid.insert("center", 100, 100);

            // Query from adjacent cell (cell size is 64)
            List<String> result = grid.getNearby(160, 100); // Next cell to the right
            assertTrue(result.contains("center"));
        }

        @Test
        @DisplayName("getNearby does not return entities in distant cells")
        void getNearbyDoesNotReturnDistantEntities() {
            grid.insert("farAway", 500, 500);

            List<String> result = grid.getNearby(100, 100);
            assertFalse(result.contains("farAway"));
        }

        @Test
        @DisplayName("getNearby returns multiple entities")
        void getNearbyReturnsMultipleEntities() {
            grid.insert("e1", 100, 100);
            grid.insert("e2", 105, 105);
            grid.insert("e3", 150, 150);

            List<String> result = grid.getNearby(100, 100);
            assertEquals(3, result.size());
        }
    }

    @Nested
    @DisplayName("Size-based Insert")
    class SizeBasedInsert {

        @Test
        @DisplayName("insertWithSize adds entity to all covered cells")
        void insertWithSizeCoversMultipleCells() {
            // Insert a large entity spanning multiple cells
            grid.insertWithSize("bigEntity", 60, 60, 70); // Spans from cell (0,0) to (1,1)

            // Should be found from any of the covered cells
            assertTrue(grid.getInCell(60, 60).contains("bigEntity"));
            assertTrue(grid.getInCell(100, 100).contains("bigEntity")); // In cell (1,1)
        }

        @Test
        @DisplayName("insertWithSize prevents duplicate entries in same cell")
        void insertWithSizePreventsDuplicates() {
            grid.insertWithSize("entity", 0, 0, 32);

            List<String> result = grid.getInCell(0, 0);
            long count = result.stream().filter(e -> e.equals("entity")).count();
            assertEquals(1, count);
        }
    }

    @Nested
    @DisplayName("Area Queries")
    class AreaQueries {

        @Test
        @DisplayName("getInArea returns entities within bounds")
        void getInAreaReturnsEntitiesInBounds() {
            grid.insert("inside", 150, 150);
            grid.insert("outside", 500, 500);

            List<String> result = grid.getInArea(100, 100, 200, 200);
            assertTrue(result.contains("inside"));
            assertFalse(result.contains("outside"));
        }

        @Test
        @DisplayName("getInArea includes boundary cells")
        void getInAreaIncludesBoundaryCells() {
            grid.insert("boundary", 90, 90);

            List<String> result = grid.getInArea(100, 100, 100, 100);
            // Should include entity near boundary due to cell expansion
            assertTrue(result.contains("boundary"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("empty grid returns empty results")
        void emptyGridReturnsEmpty() {
            List<String> result = grid.getNearby(100, 100);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("negative coordinates are handled safely")
        void negativeCoordinatesHandled() {
            // Should not throw
            assertDoesNotThrow(() -> grid.insert("entity", -10, -10));

            // Entity should be placed in first cell
            List<String> result = grid.getInCell(0, 0);
            assertTrue(result.contains("entity"));
        }

        @Test
        @DisplayName("coordinates beyond map size are handled safely")
        void largeCoordinatesHandled() {
            assertDoesNotThrow(() -> grid.insert("entity", 10000, 10000));

            // Entity should be placed in last cell
            List<String> result = grid.getInCell(MAP_SIZE - 1, MAP_SIZE - 1);
            assertTrue(result.contains("entity"));
        }

        @Test
        @DisplayName("query result lists are independent to prevent aliasing bugs")
        void queryResultListsAreIndependent() {
            grid.insert("e1", 100, 100);
            List<String> result1 = grid.getNearby(100, 100);

            grid.insert("e2", 200, 200);
            List<String> result2 = grid.getNearby(200, 200);

            // Lists should NOT be same reference to prevent aliasing bugs in nested loops
            assertNotSame(result1, result2);
            // Original result should still contain original data
            assertTrue(result1.contains("e1"));
            assertTrue(result2.contains("e2"));
        }
    }

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        @Test
        @DisplayName("getStats returns formatted string")
        void getStatsReturnsFormattedString() {
            grid.insert("e1", 100, 100);
            grid.insert("e2", 100, 105);
            grid.insert("e3", 500, 500);

            String stats = grid.getStats();
            assertTrue(stats.contains("SpatialGrid"));
            assertTrue(stats.contains("3 entities"));
            assertTrue(stats.contains("2 non-empty"));
        }
    }

    @Nested
    @DisplayName("Performance Characteristics")
    class PerformanceCharacteristics {

        @Test
        @DisplayName("spatial grid provides better locality than linear search")
        void spatialGridProvidesLocality() {
            // Insert many entities spread across the map
            for (int i = 0; i < 100; i++) {
                grid.insert("entity" + i, (i * 7) % MAP_SIZE, (i * 11) % MAP_SIZE);
            }

            // Query in one corner - should not return all entities
            List<String> nearby = grid.getNearby(50, 50);

            // Should return much fewer than total entities
            assertTrue(nearby.size() < 50, "Spatial query should filter out distant entities");
        }
    }
}
