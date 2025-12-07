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
    @DisplayName("Easter Egg Mode Tests")
    class EasterEggModeTests {

        @Test
        @DisplayName("Easter egg mode should be off by default")
        void easterEggModeOffByDefault() {
            assertFalse(base.isEasterEggMode());
        }

        @Test
        @DisplayName("setEasterEggMode should enable easter egg mode")
        void setEasterEggModeShouldEnable() {
            base.setEasterEggMode(true);

            assertTrue(base.isEasterEggMode());
        }

        @Test
        @DisplayName("setEasterEggMode should disable easter egg mode")
        void setEasterEggModeShouldDisable() {
            base.setEasterEggMode(true);
            base.setEasterEggMode(false);

            assertFalse(base.isEasterEggMode());
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
        @DisplayName("Easter egg mode doesn't affect alive status")
        void easterEggModeDoesntAffectAliveStatus() {
            base.setEasterEggMode(true);

            assertTrue(base.isAlive());
        }
    }
}
