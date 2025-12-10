package com.vibetanks.animation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DancingGirl Tests")
class DancingGirlTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Basic constructor should set position")
        void basicConstructorShouldSetPosition() {
            DancingGirl girl = new DancingGirl(100, 200, 1);

            assertEquals(100, girl.getX());
            assertEquals(200, girl.getY());
        }

        @Test
        @DisplayName("Basic constructor should set dance style")
        void basicConstructorShouldSetDanceStyle() {
            DancingGirl girl = new DancingGirl(100, 200, 2);

            assertEquals(2, girl.getDanceStyle());
        }

        @Test
        @DisplayName("Basic constructor should initialize with random animFrame")
        void basicConstructorShouldInitWithRandomAnimFrame() {
            DancingGirl girl = new DancingGirl(100, 200, 1);

            // AnimFrame is random between 0 and 59
            int animFrame = girl.getAnimFrame();
            assertTrue(animFrame >= 0 && animFrame < 60);
        }

        @Test
        @DisplayName("Basic constructor should assign random dress color index")
        void basicConstructorShouldAssignRandomDressColorIndex() {
            DancingGirl girl = new DancingGirl(100, 200, 1);

            int dressColorIndex = girl.getDressColorIndex();
            assertTrue(dressColorIndex >= 0 && dressColorIndex < DancingGirl.DRESS_COLORS.length);
        }

        @Test
        @DisplayName("Basic constructor should assign random hair color index")
        void basicConstructorShouldAssignRandomHairColorIndex() {
            DancingGirl girl = new DancingGirl(100, 200, 1);

            int hairColorIndex = girl.getHairColorIndex();
            assertTrue(hairColorIndex >= 0 && hairColorIndex < DancingGirl.HAIR_COLORS.length);
        }

        @Test
        @DisplayName("Network sync constructor should set all values")
        void networkSyncConstructorShouldSetAllValues() {
            DancingGirl girl = new DancingGirl(150, 250, 30, 2, 3, 4);

            assertEquals(150, girl.getX());
            assertEquals(250, girl.getY());
            assertEquals(30, girl.getAnimFrame());
            assertEquals(2, girl.getDanceStyle());
            assertEquals(3, girl.getDressColorIndex());
            assertEquals(4, girl.getHairColorIndex());
        }

        @Test
        @DisplayName("Network sync constructor should handle color index modulo")
        void networkSyncConstructorShouldHandleColorModulo() {
            // Test with colorIndex larger than array sizes
            int largeDressIndex = DancingGirl.DRESS_COLORS.length + 2;
            int largeHairIndex = DancingGirl.HAIR_COLORS.length + 3;

            DancingGirl girl = new DancingGirl(100, 200, 0, 0, largeDressIndex, largeHairIndex);

            // Should not throw and should have valid state
            assertNotNull(girl);
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("update should increment animFrame")
        void updateShouldIncrementAnimFrame() {
            DancingGirl girl = new DancingGirl(100, 200, 0, 0, 0, 0);
            int initialFrame = girl.getAnimFrame();

            girl.update();

            assertEquals(initialFrame + 1, girl.getAnimFrame());
        }

        @Test
        @DisplayName("Multiple updates should accumulate")
        void multipleUpdatesShouldAccumulate() {
            DancingGirl girl = new DancingGirl(100, 200, 0, 0, 0, 0);
            int initialFrame = girl.getAnimFrame();

            for (int i = 0; i < 100; i++) {
                girl.update();
            }

            assertEquals(initialFrame + 100, girl.getAnimFrame());
        }
    }

    @Nested
    @DisplayName("Color Constants Tests")
    class ColorConstantsTests {

        @Test
        @DisplayName("DRESS_COLORS should have elements")
        void dressColorsShouldHaveElements() {
            assertTrue(DancingGirl.DRESS_COLORS.length > 0);
        }

        @Test
        @DisplayName("HAIR_COLORS should have elements")
        void hairColorsShouldHaveElements() {
            assertTrue(DancingGirl.HAIR_COLORS.length > 0);
        }

        @Test
        @DisplayName("All dress colors should be non-null")
        void allDressColorsShouldBeNonNull() {
            for (var color : DancingGirl.DRESS_COLORS) {
                assertNotNull(color);
            }
        }

        @Test
        @DisplayName("All hair colors should be non-null")
        void allHairColorsShouldBeNonNull() {
            for (var color : DancingGirl.HAIR_COLORS) {
                assertNotNull(color);
            }
        }

        @Test
        @DisplayName("DRESS_COLORS should have variety")
        void dressColorsShouldHaveVariety() {
            assertTrue(DancingGirl.DRESS_COLORS.length >= 4,
                "Should have at least 4 dress colors for variety");
        }

        @Test
        @DisplayName("HAIR_COLORS should have variety")
        void hairColorsShouldHaveVariety() {
            assertTrue(DancingGirl.HAIR_COLORS.length >= 3,
                "Should have at least 3 hair colors for variety");
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        private DancingGirl girl;

        @BeforeEach
        void setUp() {
            girl = new DancingGirl(100, 200, 10, 2, 1, 3);
        }

        @Test
        @DisplayName("getX should return x position")
        void getXShouldReturnXPosition() {
            assertEquals(100, girl.getX());
        }

        @Test
        @DisplayName("getY should return y position")
        void getYShouldReturnYPosition() {
            assertEquals(200, girl.getY());
        }

        @Test
        @DisplayName("getAnimFrame should return animation frame")
        void getAnimFrameShouldReturnAnimationFrame() {
            assertEquals(10, girl.getAnimFrame());
        }

        @Test
        @DisplayName("getDanceStyle should return dance style")
        void getDanceStyleShouldReturnDanceStyle() {
            assertEquals(2, girl.getDanceStyle());
        }

        @Test
        @DisplayName("getDressColorIndex should return dress color index")
        void getDressColorIndexShouldReturnDressColorIndex() {
            assertEquals(1, girl.getDressColorIndex());
        }

        @Test
        @DisplayName("getHairColorIndex should return hair color index")
        void getHairColorIndexShouldReturnHairColorIndex() {
            assertEquals(3, girl.getHairColorIndex());
        }
    }

    @Nested
    @DisplayName("Dance Style Tests")
    class DanceStyleTests {

        @Test
        @DisplayName("All dance styles should be valid")
        void allDanceStylesShouldBeValid() {
            for (int style = 0; style < 4; style++) {
                DancingGirl girl = new DancingGirl(100, 200, style);
                assertEquals(style, girl.getDanceStyle());
            }
        }
    }

    @Nested
    @DisplayName("Network Sync Tests")
    class NetworkSyncTests {

        @Test
        @DisplayName("Network constructor should create exact copy of state")
        void networkConstructorShouldCreateExactCopy() {
            // Simulate syncing from network
            double x = 150.5;
            double y = 250.5;
            int animFrame = 42;
            int danceStyle = 3;
            int dressColorIndex = 2;
            int hairColorIndex = 1;

            DancingGirl girl = new DancingGirl(x, y, animFrame, danceStyle, dressColorIndex, hairColorIndex);

            assertEquals(x, girl.getX());
            assertEquals(y, girl.getY());
            assertEquals(animFrame, girl.getAnimFrame());
            assertEquals(danceStyle, girl.getDanceStyle());
            assertEquals(dressColorIndex, girl.getDressColorIndex());
            assertEquals(hairColorIndex, girl.getHairColorIndex());
        }

        @Test
        @DisplayName("Animation state should be deterministic from network values")
        void animationStateShouldBeDeterministicFromNetworkValues() {
            // Two girls created with same network values should have same state
            DancingGirl girl1 = new DancingGirl(100, 200, 50, 2, 1, 3);
            DancingGirl girl2 = new DancingGirl(100, 200, 50, 2, 1, 3);

            assertEquals(girl1.getX(), girl2.getX());
            assertEquals(girl1.getY(), girl2.getY());
            assertEquals(girl1.getAnimFrame(), girl2.getAnimFrame());
            assertEquals(girl1.getDanceStyle(), girl2.getDanceStyle());
            assertEquals(girl1.getDressColorIndex(), girl2.getDressColorIndex());
            assertEquals(girl1.getHairColorIndex(), girl2.getHairColorIndex());
        }
    }
}
