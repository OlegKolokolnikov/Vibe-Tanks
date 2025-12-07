package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Direction Enum Tests")
class DirectionTest {

    @Test
    @DisplayName("UP direction should have dx=0 and dy=-1")
    void upDirectionHasCorrectDeltas() {
        assertEquals(0, Direction.UP.getDx());
        assertEquals(-1, Direction.UP.getDy());
    }

    @Test
    @DisplayName("DOWN direction should have dx=0 and dy=1")
    void downDirectionHasCorrectDeltas() {
        assertEquals(0, Direction.DOWN.getDx());
        assertEquals(1, Direction.DOWN.getDy());
    }

    @Test
    @DisplayName("LEFT direction should have dx=-1 and dy=0")
    void leftDirectionHasCorrectDeltas() {
        assertEquals(-1, Direction.LEFT.getDx());
        assertEquals(0, Direction.LEFT.getDy());
    }

    @Test
    @DisplayName("RIGHT direction should have dx=1 and dy=0")
    void rightDirectionHasCorrectDeltas() {
        assertEquals(1, Direction.RIGHT.getDx());
        assertEquals(0, Direction.RIGHT.getDy());
    }

    @Test
    @DisplayName("UP opposite should be DOWN")
    void upOppositeIsDown() {
        assertEquals(Direction.DOWN, Direction.UP.opposite());
    }

    @Test
    @DisplayName("DOWN opposite should be UP")
    void downOppositeIsUp() {
        assertEquals(Direction.UP, Direction.DOWN.opposite());
    }

    @Test
    @DisplayName("LEFT opposite should be RIGHT")
    void leftOppositeIsRight() {
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
    }

    @Test
    @DisplayName("RIGHT opposite should be LEFT")
    void rightOppositeIsLeft() {
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite());
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("Opposite of opposite should return original direction")
    void oppositeOfOppositeIsOriginal(Direction direction) {
        assertEquals(direction, direction.opposite().opposite());
    }

    @Test
    @DisplayName("All four directions should exist")
    void allDirectionsExist() {
        assertEquals(4, Direction.values().length);
    }

    @Test
    @DisplayName("Direction ordinals should be sequential")
    void ordinalsAreSequential() {
        assertEquals(0, Direction.UP.ordinal());
        assertEquals(1, Direction.DOWN.ordinal());
        assertEquals(2, Direction.LEFT.ordinal());
        assertEquals(3, Direction.RIGHT.ordinal());
    }

    @Test
    @DisplayName("Vertical directions should have zero horizontal movement")
    void verticalDirectionsHaveZeroHorizontalMovement() {
        assertEquals(0, Direction.UP.getDx());
        assertEquals(0, Direction.DOWN.getDx());
    }

    @Test
    @DisplayName("Horizontal directions should have zero vertical movement")
    void horizontalDirectionsHaveZeroVerticalMovement() {
        assertEquals(0, Direction.LEFT.getDy());
        assertEquals(0, Direction.RIGHT.getDy());
    }
}
