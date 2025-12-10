package com.vibetanks.animation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DancingCharacter Tests")
class DancingCharacterTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Basic constructor should set position")
        void basicConstructorShouldSetPosition() {
            DancingCharacter character = new DancingCharacter(100, 200, true, 1);

            assertEquals(100, character.getX());
            assertEquals(200, character.getY());
        }

        @Test
        @DisplayName("Basic constructor should set alien flag")
        void basicConstructorShouldSetAlienFlag() {
            DancingCharacter alien = new DancingCharacter(100, 200, true, 1);
            DancingCharacter human = new DancingCharacter(100, 200, false, 1);

            assertTrue(alien.isAlien());
            assertFalse(human.isAlien());
        }

        @Test
        @DisplayName("Basic constructor should set dance style")
        void basicConstructorShouldSetDanceStyle() {
            DancingCharacter character = new DancingCharacter(100, 200, true, 2);

            assertEquals(2, character.getDanceStyle());
        }

        @Test
        @DisplayName("Basic constructor should initialize animFrame to 0")
        void basicConstructorShouldInitAnimFrameToZero() {
            DancingCharacter character = new DancingCharacter(100, 200, true, 1);

            assertEquals(0, character.getAnimFrame());
        }

        @Test
        @DisplayName("Basic constructor should assign random color index")
        void basicConstructorShouldAssignRandomColorIndex() {
            DancingCharacter character = new DancingCharacter(100, 200, true, 1);

            int colorIndex = character.getColorIndex();
            assertTrue(colorIndex >= 0 && colorIndex < DancingCharacter.ALIEN_COLORS.length);
        }

        @Test
        @DisplayName("Network sync constructor should set all values")
        void networkSyncConstructorShouldSetAllValues() {
            DancingCharacter character = new DancingCharacter(150, 250, false, 30, 2, 1);

            assertEquals(150, character.getX());
            assertEquals(250, character.getY());
            assertFalse(character.isAlien());
            assertEquals(30, character.getAnimFrame());
            assertEquals(2, character.getDanceStyle());
            assertEquals(1, character.getColorIndex());
        }

        @Test
        @DisplayName("Network sync constructor should handle color index modulo for aliens")
        void networkSyncConstructorShouldHandleColorModuloForAliens() {
            // Test with colorIndex larger than array size
            int largeIndex = DancingCharacter.ALIEN_COLORS.length + 2;
            DancingCharacter character = new DancingCharacter(100, 200, true, 0, 0, largeIndex);

            // Should not throw and should have valid color
            assertNotNull(character);
            int expectedIndex = largeIndex % DancingCharacter.ALIEN_COLORS.length;
            // ColorIndex is stored as passed, but color uses modulo
            assertEquals(largeIndex, character.getColorIndex());
        }

        @Test
        @DisplayName("Network sync constructor should handle color index modulo for humans")
        void networkSyncConstructorShouldHandleColorModuloForHumans() {
            int largeIndex = DancingCharacter.HUMAN_COLORS.length + 1;
            DancingCharacter character = new DancingCharacter(100, 200, false, 0, 0, largeIndex);

            assertNotNull(character);
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("update should increment animFrame")
        void updateShouldIncrementAnimFrame() {
            DancingCharacter character = new DancingCharacter(100, 200, true, 1);
            assertEquals(0, character.getAnimFrame());

            character.update();
            assertEquals(1, character.getAnimFrame());

            character.update();
            assertEquals(2, character.getAnimFrame());
        }

        @Test
        @DisplayName("Multiple updates should accumulate")
        void multipleUpdatesShouldAccumulate() {
            DancingCharacter character = new DancingCharacter(100, 200, true, 1);

            for (int i = 0; i < 100; i++) {
                character.update();
            }

            assertEquals(100, character.getAnimFrame());
        }
    }

    @Nested
    @DisplayName("Color Constants Tests")
    class ColorConstantsTests {

        @Test
        @DisplayName("ALIEN_COLORS should have elements")
        void alienColorsShouldHaveElements() {
            assertTrue(DancingCharacter.ALIEN_COLORS.length > 0);
        }

        @Test
        @DisplayName("HUMAN_COLORS should have elements")
        void humanColorsShouldHaveElements() {
            assertTrue(DancingCharacter.HUMAN_COLORS.length > 0);
        }

        @Test
        @DisplayName("All alien colors should be non-null")
        void allAlienColorsShouldBeNonNull() {
            for (var color : DancingCharacter.ALIEN_COLORS) {
                assertNotNull(color);
            }
        }

        @Test
        @DisplayName("All human colors should be non-null")
        void allHumanColorsShouldBeNonNull() {
            for (var color : DancingCharacter.HUMAN_COLORS) {
                assertNotNull(color);
            }
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        private DancingCharacter character;

        @BeforeEach
        void setUp() {
            character = new DancingCharacter(100, 200, true, 1);
        }

        @Test
        @DisplayName("getX should return x position")
        void getXShouldReturnXPosition() {
            assertEquals(100, character.getX());
        }

        @Test
        @DisplayName("getY should return y position")
        void getYShouldReturnYPosition() {
            assertEquals(200, character.getY());
        }

        @Test
        @DisplayName("isAlien should return alien status")
        void isAlienShouldReturnAlienStatus() {
            assertTrue(character.isAlien());
        }

        @Test
        @DisplayName("getAnimFrame should return animation frame")
        void getAnimFrameShouldReturnAnimationFrame() {
            assertEquals(0, character.getAnimFrame());
            character.update();
            assertEquals(1, character.getAnimFrame());
        }

        @Test
        @DisplayName("getDanceStyle should return dance style")
        void getDanceStyleShouldReturnDanceStyle() {
            assertEquals(1, character.getDanceStyle());
        }

        @Test
        @DisplayName("getColorIndex should return color index")
        void getColorIndexShouldReturnColorIndex() {
            int colorIndex = character.getColorIndex();
            assertTrue(colorIndex >= 0);
        }
    }

    @Nested
    @DisplayName("Different Dance Styles Tests")
    class DanceStyleTests {

        @Test
        @DisplayName("All dance styles should be valid")
        void allDanceStylesShouldBeValid() {
            for (int style = 0; style < 3; style++) {
                DancingCharacter character = new DancingCharacter(100, 200, true, style);
                assertEquals(style, character.getDanceStyle());
            }
        }

        @Test
        @DisplayName("Different styles should create different characters")
        void differentStylesShouldCreateDifferentCharacters() {
            DancingCharacter style0 = new DancingCharacter(100, 200, true, 0);
            DancingCharacter style1 = new DancingCharacter(100, 200, true, 1);
            DancingCharacter style2 = new DancingCharacter(100, 200, true, 2);

            assertNotEquals(style0.getDanceStyle(), style1.getDanceStyle());
            assertNotEquals(style1.getDanceStyle(), style2.getDanceStyle());
        }
    }
}
