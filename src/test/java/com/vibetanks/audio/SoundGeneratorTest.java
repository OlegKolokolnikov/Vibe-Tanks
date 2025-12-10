package com.vibetanks.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SoundGenerator Tests")
class SoundGeneratorTest {

    @Nested
    @DisplayName("Static Method Tests")
    class StaticMethodTests {

        @Test
        @DisplayName("generateAllSounds method should exist")
        void generateAllSoundsMethodShouldExist() throws NoSuchMethodException {
            assertNotNull(SoundGenerator.class.getMethod("generateAllSounds"));
        }

        @Test
        @DisplayName("generateAllSounds method should be public static")
        void generateAllSoundsMethodShouldBePublicStatic() throws NoSuchMethodException {
            var method = SoundGenerator.class.getMethod("generateAllSounds");
            assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
        }

        @Test
        @DisplayName("main method should exist")
        void mainMethodShouldExist() throws NoSuchMethodException {
            assertNotNull(SoundGenerator.class.getMethod("main", String[].class));
        }
    }

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("Sample rate should be defined")
        void sampleRateShouldBeDefined() throws Exception {
            var field = SoundGenerator.class.getDeclaredField("SAMPLE_RATE");
            field.setAccessible(true);
            float sampleRate = (float) field.get(null);
            // Common sample rate is 22050 or 44100
            assertTrue(sampleRate > 0);
            assertTrue(sampleRate >= 22050);
        }
    }

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

        @Test
        @DisplayName("SoundGenerator class should exist")
        void soundGeneratorClassShouldExist() {
            assertNotNull(SoundGenerator.class);
        }

        @Test
        @DisplayName("SoundGenerator should have private sound generation methods")
        void soundGeneratorShouldHavePrivateSoundGenerationMethods() {
            // Verify class has methods (they are private, so we check via getDeclaredMethods)
            var methods = SoundGenerator.class.getDeclaredMethods();
            assertTrue(methods.length > 2); // Should have more than just main and generateAllSounds
        }
    }
}
