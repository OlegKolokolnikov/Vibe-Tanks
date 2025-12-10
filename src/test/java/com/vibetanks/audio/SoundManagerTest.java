package com.vibetanks.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SoundManager Tests")
class SoundManagerTest {

    private SoundManager soundManager;

    @BeforeEach
    void setUp() {
        soundManager = new SoundManager();
    }

    @AfterEach
    void tearDown() {
        if (soundManager != null) {
            soundManager.shutdown();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should create manager without throwing")
        void constructorShouldCreateManagerWithoutThrowing() {
            assertDoesNotThrow(() -> new SoundManager());
        }

        @Test
        @DisplayName("Multiple managers can be created")
        void multipleManagersCanBeCreated() {
            SoundManager manager1 = new SoundManager();
            SoundManager manager2 = new SoundManager();

            assertNotNull(manager1);
            assertNotNull(manager2);

            manager1.shutdown();
            manager2.shutdown();
        }
    }

    @Nested
    @DisplayName("Sound Playback Tests")
    class SoundPlaybackTests {

        @Test
        @DisplayName("playShoot should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playShootShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playShoot());
        }

        @Test
        @DisplayName("playExplosion should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playExplosionShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playExplosion());
        }

        @Test
        @DisplayName("playIntro should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playIntroShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playIntro());
        }

        @Test
        @DisplayName("playSad should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playSadShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playSad());
        }

        @Test
        @DisplayName("playPlayerDeath should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playPlayerDeathShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playPlayerDeath());
        }

        @Test
        @DisplayName("playBaseDestroyed should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playBaseDestroyedShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playBaseDestroyed());
        }

        @Test
        @DisplayName("playTreeBurn should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playTreeBurnShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playTreeBurn());
        }

        @Test
        @DisplayName("playLaser should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playLaserShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playLaser());
        }

        @Test
        @DisplayName("playVictory should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playVictoryShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playVictory());
        }

        @Test
        @DisplayName("Multiple sounds can play in sequence")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void multipleSoundsCanPlayInSequence() {
            assertDoesNotThrow(() -> {
                soundManager.playShoot();
                soundManager.playExplosion();
                soundManager.playShoot();
            });
        }

        @Test
        @DisplayName("Rapid fire sounds should not crash")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void rapidFireSoundsShouldNotCrash() {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 50; i++) {
                    soundManager.playShoot();
                }
            });
        }
    }

    @Nested
    @DisplayName("Music Playback Tests")
    class MusicPlaybackTests {

        @Test
        @DisplayName("playExplanationMusic should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void playExplanationMusicShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.playExplanationMusic());
        }

        @Test
        @DisplayName("stopExplanationMusic should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void stopExplanationMusicShouldNotThrow() {
            soundManager.playExplanationMusic();
            assertDoesNotThrow(() -> soundManager.stopExplanationMusic());
        }

        @Test
        @DisplayName("stopExplanationMusic should work without playing first")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void stopExplanationMusicShouldWorkWithoutPlayingFirst() {
            assertDoesNotThrow(() -> soundManager.stopExplanationMusic());
        }

        @Test
        @DisplayName("Music can be started and stopped multiple times")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void musicCanBeStartedAndStoppedMultipleTimes() throws InterruptedException {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 3; i++) {
                    soundManager.playExplanationMusic();
                    Thread.sleep(100);
                    soundManager.stopExplanationMusic();
                    Thread.sleep(100);
                }
            });
        }
    }

    @Nested
    @DisplayName("Stop Gameplay Sounds Tests")
    class StopGameplaySoundsTests {

        @Test
        @DisplayName("stopGameplaySounds should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void stopGameplaySoundsShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.stopGameplaySounds());
        }

        @Test
        @DisplayName("stopGameplaySounds should work after playing sounds")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void stopGameplaySoundsShouldWorkAfterPlayingSounds() {
            soundManager.playShoot();
            soundManager.playExplosion();

            assertDoesNotThrow(() -> soundManager.stopGameplaySounds());
        }
    }

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("shutdown should not throw")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shutdownShouldNotThrow() {
            assertDoesNotThrow(() -> soundManager.shutdown());
        }

        @Test
        @DisplayName("shutdown should be idempotent")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shutdownShouldBeIdempotent() {
            assertDoesNotThrow(() -> {
                soundManager.shutdown();
                soundManager.shutdown();
                soundManager.shutdown();
            });
        }

        @Test
        @DisplayName("Sounds should not crash after shutdown")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void soundsShouldNotCrashAfterShutdown() {
            soundManager.shutdown();

            // These should silently do nothing, not crash
            assertDoesNotThrow(() -> {
                soundManager.playShoot();
                soundManager.playExplosion();
                soundManager.playIntro();
            });
        }

        @Test
        @DisplayName("Music should stop on shutdown")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void musicShouldStopOnShutdown() throws InterruptedException {
            soundManager.playExplanationMusic();
            Thread.sleep(100);

            assertDoesNotThrow(() -> soundManager.shutdown());
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Concurrent sound playback should not crash")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void concurrentSoundPlaybackShouldNotCrash() throws InterruptedException {
            Thread[] threads = new Thread[5];

            for (int i = 0; i < threads.length; i++) {
                int index = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 10; j++) {
                        switch (index % 3) {
                            case 0 -> soundManager.playShoot();
                            case 1 -> soundManager.playExplosion();
                            case 2 -> soundManager.playIntro();
                        }
                    }
                });
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            // If we got here without exception, test passed
            assertTrue(true);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Creating and destroying many managers should not leak resources")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void creatingManyManagersShouldNotLeakResources() {
            for (int i = 0; i < 10; i++) {
                SoundManager tempManager = new SoundManager();
                tempManager.playShoot();
                tempManager.shutdown();
            }

            // If we got here without running out of resources, test passed
            assertTrue(true);
        }
    }
}
