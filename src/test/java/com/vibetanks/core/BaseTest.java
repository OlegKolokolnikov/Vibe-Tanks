package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Base Tests")
class BaseTest {

    private Base base;

    @BeforeEach
    void setUp() {
        base = new Base(100, 200);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should set position correctly")
        void constructorSetsPositionCorrectly() {
            assertEquals(100, base.getX());
            assertEquals(200, base.getY());
        }

        @Test
        @DisplayName("New base should be alive")
        void newBaseShouldBeAlive() {
            assertTrue(base.isAlive());
        }

        @Test
        @DisplayName("New base should not show flag")
        void newBaseShouldNotShowFlag() {
            assertFalse(base.isShowingFlag());
        }

        @Test
        @DisplayName("New base should not show victory flag")
        void newBaseShouldNotShowVictoryFlag() {
            assertFalse(base.isShowingVictoryFlag());
        }

        @Test
        @DisplayName("Constructor should set size")
        void constructorShouldSetSize() {
            assertTrue(base.getSize() > 0);
        }
    }

    @Nested
    @DisplayName("Destroy Tests")
    class DestroyTests {

        @Test
        @DisplayName("destroy should mark base as not alive")
        void destroyShouldMarkAsNotAlive() {
            assertTrue(base.isAlive());

            base.destroy();

            assertFalse(base.isAlive());
        }

        @Test
        @DisplayName("Multiple destroy calls should not throw")
        void multipleDestroyCallsShouldNotThrow() {
            base.destroy();
            assertDoesNotThrow(() -> base.destroy());
        }
    }

    @Nested
    @DisplayName("Flag Tests")
    class FlagTests {

        @Test
        @DisplayName("raiseFlag should show flag")
        void raiseFlagShouldShowFlag() {
            base.raiseFlag();

            assertTrue(base.isShowingFlag());
        }

        @Test
        @DisplayName("setFlagState should control flag visibility")
        void setFlagStateShouldControlVisibility() {
            base.setFlagState(true, 10.0);

            assertTrue(base.isShowingFlag());
            assertEquals(10.0, base.getFlagHeight());
        }

        @Test
        @DisplayName("setFlagState can hide flag")
        void setFlagStateCanHideFlag() {
            base.setFlagState(true, 10.0);
            base.setFlagState(false, 0.0);

            assertFalse(base.isShowingFlag());
        }

        @Test
        @DisplayName("getFlagHeight should return correct height")
        void getFlagHeightShouldReturnCorrectHeight() {
            base.setFlagState(true, 25.5);

            assertEquals(25.5, base.getFlagHeight());
        }
    }

    @Nested
    @DisplayName("Victory Flag Tests")
    class VictoryFlagTests {

        @Test
        @DisplayName("raiseVictoryFlag should show victory flag")
        void raiseVictoryFlagShouldShowFlag() {
            base.raiseVictoryFlag();

            assertTrue(base.isShowingVictoryFlag());
        }

        @Test
        @DisplayName("setVictoryFlagState should control flag visibility")
        void setVictoryFlagStateShouldControlVisibility() {
            base.setVictoryFlagState(true, 15.0);

            assertTrue(base.isShowingVictoryFlag());
            assertEquals(15.0, base.getVictoryFlagHeight());
        }

        @Test
        @DisplayName("setVictoryFlagState can hide victory flag")
        void setVictoryFlagStateCanHideFlag() {
            base.setVictoryFlagState(true, 15.0);
            base.setVictoryFlagState(false, 0.0);

            assertFalse(base.isShowingVictoryFlag());
        }

        @Test
        @DisplayName("getVictoryFlagHeight should return correct height")
        void getVictoryFlagHeightShouldReturnCorrectHeight() {
            base.setVictoryFlagState(true, 30.0);

            assertEquals(30.0, base.getVictoryFlagHeight());
        }
    }

    @Nested
    @DisplayName("Cat Mode Tests")
    class CatModeTests {

        @Test
        @DisplayName("Cat mode should be off by default")
        void catModeOffByDefault() {
            assertFalse(base.isCatMode());
        }

        @Test
        @DisplayName("setCatMode should enable cat mode")
        void setCatModeShouldEnable() {
            base.setCatMode(true);

            assertTrue(base.isCatMode());
        }

        @Test
        @DisplayName("setCatMode should disable cat mode")
        void setCatModeShouldDisable() {
            base.setCatMode(true);
            base.setCatMode(false);

            assertFalse(base.isCatMode());
        }
    }

    @Nested
    @DisplayName("Position Tests")
    class PositionTests {

        @Test
        @DisplayName("Base can be created at origin")
        void baseCanBeCreatedAtOrigin() {
            Base originBase = new Base(0, 0);

            assertEquals(0, originBase.getX());
            assertEquals(0, originBase.getY());
        }

        @Test
        @DisplayName("Base can be created at any position")
        void baseCanBeCreatedAtAnyPosition() {
            Base customBase = new Base(500.5, 600.75);

            assertEquals(500.5, customBase.getX());
            assertEquals(600.75, customBase.getY());
        }
    }

    @Nested
    @DisplayName("State Combinations Tests")
    class StateCombinationsTests {

        @Test
        @DisplayName("Both flags can be raised simultaneously")
        void bothFlagsCanBeRaised() {
            base.raiseFlag();
            base.raiseVictoryFlag();

            assertTrue(base.isShowingFlag());
            assertTrue(base.isShowingVictoryFlag());
        }

        @Test
        @DisplayName("Flags can be raised even when base is destroyed")
        void flagsCanBeRaisedWhenDestroyed() {
            base.destroy();
            base.raiseFlag();

            assertFalse(base.isAlive());
            assertTrue(base.isShowingFlag());
        }

        @Test
        @DisplayName("Cat mode doesn't affect alive status")
        void catModeDoesntAffectAliveStatus() {
            base.setCatMode(true);

            assertTrue(base.isAlive());
        }
    }

    @Nested
    @DisplayName("Cat Escape Animation Tests")
    class CatEscapeAnimationTests {

        @Test
        @DisplayName("Cat escape should be off by default")
        void catEscapeOffByDefault() {
            assertFalse(base.isCatEscaping());
        }

        @Test
        @DisplayName("startCatEscape should not work without cat mode")
        void startCatEscapeRequiresCatMode() {
            assertFalse(base.isCatMode());

            base.startCatEscape();

            assertFalse(base.isCatEscaping());
        }

        @Test
        @DisplayName("startCatEscape should work with cat mode enabled")
        void startCatEscapeWorksWithCatMode() {
            base.setCatMode(true);

            base.startCatEscape();

            assertTrue(base.isCatEscaping());
        }

        @Test
        @DisplayName("startCatEscape should initialize escape position to base position")
        void startCatEscapeInitializesPosition() {
            base.setCatMode(true);

            base.startCatEscape();

            assertEquals(base.getX(), base.getCatEscapeX());
            assertEquals(base.getY(), base.getCatEscapeY());
        }

        @Test
        @DisplayName("startCatEscape should set toy position to the right of base")
        void startCatEscapeSetsToPosition() {
            base.setCatMode(true);

            base.startCatEscape();

            assertTrue(base.getToyX() > base.getX());
        }

        @Test
        @DisplayName("startCatEscape should reset frame counter")
        void startCatEscapeResetsFrameCounter() {
            base.setCatMode(true);

            base.startCatEscape();

            assertEquals(0, base.getCatEscapeFrame());
        }

        @Test
        @DisplayName("startCatEscape should set random toy type (0-2)")
        void startCatEscapeSetsRandomToyType() {
            base.setCatMode(true);

            base.startCatEscape();

            int toyType = base.getToyType();
            assertTrue(toyType >= 0 && toyType <= 2,
                "Toy type should be 0, 1, or 2 but was " + toyType);
        }

        @Test
        @DisplayName("startCatEscape should not restart if already escaping")
        void startCatEscapeDoesNotRestartIfEscaping() {
            base.setCatMode(true);
            base.startCatEscape();

            double initialToyX = base.getToyX();
            int initialToyType = base.getToyType();

            // Try to start again
            base.startCatEscape();

            // Should remain unchanged
            assertEquals(initialToyX, base.getToyX());
            assertEquals(initialToyType, base.getToyType());
        }

        @Test
        @DisplayName("setCatEscapeState should set all escape properties")
        void setCatEscapeStateSetsAllProperties() {
            base.setCatEscapeState(true, 150.0, 250.0, 30, 400.0, 180.0, 2);

            assertTrue(base.isCatEscaping());
            assertEquals(150.0, base.getCatEscapeX());
            assertEquals(250.0, base.getCatEscapeY());
            assertEquals(30, base.getCatEscapeFrame());
            assertEquals(400.0, base.getToyX());
            assertEquals(180.0, base.getToyY());
            assertEquals(2, base.getToyType());
        }

        @Test
        @DisplayName("setCatEscapeState can disable escaping")
        void setCatEscapeStateCanDisable() {
            base.setCatMode(true);
            base.startCatEscape();
            assertTrue(base.isCatEscaping());

            base.setCatEscapeState(false, 0, 0, 0, 0, 0, 0);

            assertFalse(base.isCatEscaping());
        }

        @Test
        @DisplayName("reset should clear cat escape state")
        void resetClearsCatEscapeState() {
            base.setCatMode(true);
            base.startCatEscape();
            assertTrue(base.isCatEscaping());

            base.reset();

            assertFalse(base.isCatEscaping());
            assertFalse(base.isCatMode());
            assertEquals(0, base.getCatEscapeFrame());
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("reset should restore base to alive")
        void resetRestoresAlive() {
            base.destroy();
            assertFalse(base.isAlive());

            base.reset();

            assertTrue(base.isAlive());
        }

        @Test
        @DisplayName("reset should hide all flags")
        void resetHidesFlags() {
            base.raiseFlag();
            base.raiseVictoryFlag();

            base.reset();

            assertFalse(base.isShowingFlag());
            assertFalse(base.isShowingVictoryFlag());
        }

        @Test
        @DisplayName("reset should clear flag heights")
        void resetClearsFlagHeights() {
            base.setFlagState(true, 50.0);
            base.setVictoryFlagState(true, 40.0);

            base.reset();

            assertEquals(0, base.getFlagHeight());
            assertEquals(0, base.getVictoryFlagHeight());
        }

        @Test
        @DisplayName("reset should clear cat mode")
        void resetClearsCatMode() {
            base.setCatMode(true);

            base.reset();

            assertFalse(base.isCatMode());
        }
    }
}
