package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameSettings Tests")
class GameSettingsTest {

    // Save original values to restore after tests
    private double originalPlayerSpeed;
    private double originalEnemySpeed;
    private double originalPlayerShootSpeed;
    private double originalEnemyShootSpeed;
    private double originalSoundVolume;
    private double originalMusicVolume;

    @BeforeEach
    void saveOriginalSettings() {
        originalPlayerSpeed = GameSettings.getPlayerSpeedMultiplier();
        originalEnemySpeed = GameSettings.getEnemySpeedMultiplier();
        originalPlayerShootSpeed = GameSettings.getPlayerShootSpeedMultiplier();
        originalEnemyShootSpeed = GameSettings.getEnemyShootSpeedMultiplier();
        originalSoundVolume = GameSettings.getSoundVolume();
        originalMusicVolume = GameSettings.getMusicVolume();

        // Clear host settings
        GameSettings.clearHostSettings();
    }

    @AfterEach
    void restoreOriginalSettings() {
        GameSettings.setPlayerSpeedMultiplier(originalPlayerSpeed);
        GameSettings.setEnemySpeedMultiplier(originalEnemySpeed);
        GameSettings.setPlayerShootSpeedMultiplier(originalPlayerShootSpeed);
        GameSettings.setEnemyShootSpeedMultiplier(originalEnemyShootSpeed);
        GameSettings.setSoundVolume(originalSoundVolume);
        GameSettings.setMusicVolume(originalMusicVolume);
        GameSettings.clearHostSettings();
    }

    @Nested
    @DisplayName("Speed Multiplier Tests")
    class SpeedMultiplierTests {

        @Test
        @DisplayName("setPlayerSpeedMultiplier should update value")
        void setPlayerSpeedMultiplierUpdatesValue() {
            GameSettings.setPlayerSpeedMultiplier(1.5);

            assertEquals(1.5, GameSettings.getPlayerSpeedMultiplier());
        }

        @Test
        @DisplayName("setEnemySpeedMultiplier should update value")
        void setEnemySpeedMultiplierUpdatesValue() {
            GameSettings.setEnemySpeedMultiplier(2.0);

            assertEquals(2.0, GameSettings.getEnemySpeedMultiplier());
        }

        @Test
        @DisplayName("Speed multiplier should be clamped to minimum 0.25")
        void speedMultiplierClampedToMinimum() {
            GameSettings.setPlayerSpeedMultiplier(0.1);

            assertEquals(0.25, GameSettings.getPlayerSpeedMultiplier());
        }

        @Test
        @DisplayName("Speed multiplier should be clamped to maximum 3.0")
        void speedMultiplierClampedToMaximum() {
            GameSettings.setPlayerSpeedMultiplier(5.0);

            assertEquals(3.0, GameSettings.getPlayerSpeedMultiplier());
        }

        @Test
        @DisplayName("Speed multiplier exactly at boundary should be accepted")
        void speedMultiplierAtBoundaryAccepted() {
            GameSettings.setPlayerSpeedMultiplier(0.25);
            assertEquals(0.25, GameSettings.getPlayerSpeedMultiplier());

            GameSettings.setPlayerSpeedMultiplier(3.0);
            assertEquals(3.0, GameSettings.getPlayerSpeedMultiplier());
        }
    }

    @Nested
    @DisplayName("Shoot Speed Multiplier Tests")
    class ShootSpeedMultiplierTests {

        @Test
        @DisplayName("setPlayerShootSpeedMultiplier should update value")
        void setPlayerShootSpeedMultiplierUpdatesValue() {
            GameSettings.setPlayerShootSpeedMultiplier(1.5);

            assertEquals(1.5, GameSettings.getPlayerShootSpeedMultiplier());
        }

        @Test
        @DisplayName("setEnemyShootSpeedMultiplier should update value")
        void setEnemyShootSpeedMultiplierUpdatesValue() {
            GameSettings.setEnemyShootSpeedMultiplier(2.0);

            assertEquals(2.0, GameSettings.getEnemyShootSpeedMultiplier());
        }

        @Test
        @DisplayName("Shoot speed should be clamped to valid range")
        void shootSpeedClampedToValidRange() {
            GameSettings.setPlayerShootSpeedMultiplier(0.1);
            assertEquals(0.25, GameSettings.getPlayerShootSpeedMultiplier());

            GameSettings.setPlayerShootSpeedMultiplier(5.0);
            assertEquals(3.0, GameSettings.getPlayerShootSpeedMultiplier());
        }
    }

    @Nested
    @DisplayName("Volume Tests")
    class VolumeTests {

        @Test
        @DisplayName("setSoundVolume should update value")
        void setSoundVolumeUpdatesValue() {
            GameSettings.setSoundVolume(0.5);

            assertEquals(0.5, GameSettings.getSoundVolume());
        }

        @Test
        @DisplayName("setMusicVolume should update value")
        void setMusicVolumeUpdatesValue() {
            GameSettings.setMusicVolume(0.75);

            assertEquals(0.75, GameSettings.getMusicVolume());
        }

        @Test
        @DisplayName("Volume should be clamped to minimum 0.0")
        void volumeClampedToMinimum() {
            GameSettings.setSoundVolume(-0.5);

            assertEquals(0.0, GameSettings.getSoundVolume());
        }

        @Test
        @DisplayName("Volume should be clamped to maximum 1.0")
        void volumeClampedToMaximum() {
            GameSettings.setSoundVolume(1.5);

            assertEquals(1.0, GameSettings.getSoundVolume());
        }

        @Test
        @DisplayName("Volume exactly at boundaries should be accepted")
        void volumeAtBoundariesAccepted() {
            GameSettings.setSoundVolume(0.0);
            assertEquals(0.0, GameSettings.getSoundVolume());

            GameSettings.setSoundVolume(1.0);
            assertEquals(1.0, GameSettings.getSoundVolume());
        }
    }

    @Nested
    @DisplayName("Host Settings Tests")
    class HostSettingsTests {

        @Test
        @DisplayName("setHostSettings should override local settings for getEffective")
        void setHostSettingsOverridesLocal() {
            GameSettings.setPlayerSpeedMultiplier(1.0);
            GameSettings.setEnemySpeedMultiplier(1.0);

            GameSettings.setHostSettings(2.0, 2.5, 1.5, 1.8);

            assertEquals(2.0, GameSettings.getEffectivePlayerSpeed());
            assertEquals(2.5, GameSettings.getEffectiveEnemySpeed());
            assertEquals(1.5, GameSettings.getEffectivePlayerShootSpeed());
            assertEquals(1.8, GameSettings.getEffectiveEnemyShootSpeed());
        }

        @Test
        @DisplayName("clearHostSettings should restore local settings")
        void clearHostSettingsRestoresLocal() {
            GameSettings.setPlayerSpeedMultiplier(1.2);
            GameSettings.setHostSettings(2.0, 2.0, 2.0, 2.0);

            GameSettings.clearHostSettings();

            assertEquals(1.2, GameSettings.getEffectivePlayerSpeed());
        }

        @Test
        @DisplayName("getEffective should return local when host not set")
        void getEffectiveReturnsLocalWhenNoHost() {
            GameSettings.clearHostSettings();
            GameSettings.setPlayerSpeedMultiplier(1.3);

            assertEquals(1.3, GameSettings.getEffectivePlayerSpeed());
        }

        @Test
        @DisplayName("getPlayerSpeedMultiplier should return local even with host set")
        void getLocalSpeedIgnoresHost() {
            GameSettings.setPlayerSpeedMultiplier(1.0);
            GameSettings.setHostSettings(2.0, 2.0, 2.0, 2.0);

            // getPlayerSpeedMultiplier returns local, not effective
            assertEquals(1.0, GameSettings.getPlayerSpeedMultiplier());
            // getEffectivePlayerSpeed returns host override
            assertEquals(2.0, GameSettings.getEffectivePlayerSpeed());
        }
    }

    @Nested
    @DisplayName("Enemy Count Tests")
    class EnemyCountTests {

        @Test
        @DisplayName("setEnemyCount should update value")
        void setEnemyCountUpdatesValue() {
            GameSettings.setEnemyCount(30);

            assertEquals(30, GameSettings.getEnemyCount());
        }

        @Test
        @DisplayName("Enemy count should be clamped to minimum 1")
        void enemyCountClampedToMinimum() {
            GameSettings.setEnemyCount(0);

            assertEquals(1, GameSettings.getEnemyCount());
        }

        @Test
        @DisplayName("Enemy count should be clamped to maximum 100")
        void enemyCountClampedToMaximum() {
            GameSettings.setEnemyCount(150);

            assertEquals(100, GameSettings.getEnemyCount());
        }
    }

    @Nested
    @DisplayName("Nickname Tests")
    class NicknameTests {

        @Test
        @DisplayName("setPlayerNickname should update value")
        void setPlayerNicknameUpdatesValue() {
            GameSettings.setPlayerNickname("TestPlayer");

            assertEquals("TestPlayer", GameSettings.getPlayerNickname());
        }

        @Test
        @DisplayName("Nickname should be trimmed")
        void nicknameShouldBeTrimmed() {
            GameSettings.setPlayerNickname("  SpacedName  ");

            assertEquals("SpacedName", GameSettings.getPlayerNickname());
        }

        @Test
        @DisplayName("Null nickname should become empty string")
        void nullNicknameShouldBecomeEmpty() {
            GameSettings.setPlayerNickname(null);

            assertEquals("", GameSettings.getPlayerNickname());
        }
    }

    @Nested
    @DisplayName("Reset to Defaults Tests")
    class ResetToDefaultsTests {

        @Test
        @DisplayName("resetToDefaults should restore default values")
        void resetToDefaultsShouldRestoreDefaults() {
            // Change all values
            GameSettings.setPlayerSpeedMultiplier(2.0);
            GameSettings.setEnemySpeedMultiplier(2.0);
            GameSettings.setPlayerShootSpeedMultiplier(2.0);
            GameSettings.setEnemyShootSpeedMultiplier(2.0);
            GameSettings.setSoundVolume(0.5);
            GameSettings.setMusicVolume(0.5);

            GameSettings.resetToDefaults();

            assertEquals(1.0, GameSettings.getPlayerSpeedMultiplier());
            assertEquals(1.0, GameSettings.getEnemySpeedMultiplier());
            assertEquals(1.0, GameSettings.getPlayerShootSpeedMultiplier());
            assertEquals(1.0, GameSettings.getEnemyShootSpeedMultiplier());
            assertEquals(1.0, GameSettings.getSoundVolume());
            assertEquals(1.0, GameSettings.getMusicVolume());
        }

        @Test
        @DisplayName("resetToDefaults should not reset nickname")
        void resetToDefaultsShouldNotResetNickname() {
            GameSettings.setPlayerNickname("MyNickname");

            GameSettings.resetToDefaults();

            assertEquals("MyNickname", GameSettings.getPlayerNickname());
        }
    }
}
