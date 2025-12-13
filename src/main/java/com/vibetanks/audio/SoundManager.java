package com.vibetanks.audio;

import com.vibetanks.util.GameLogger;
import javax.sound.sampled.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SoundManager {
    private static final GameLogger LOG = GameLogger.getLogger(SoundManager.class);
    private byte[] shootSoundData;
    private byte[] explosionSoundData;
    private byte[] introSoundData;
    private byte[] sadSoundData;
    private byte[] playerDeathSoundData;
    private byte[] baseDestroyedSoundData;
    private byte[] explanationMusicData;
    private byte[] treeBurnSoundData;
    private byte[] laserSoundData;
    private byte[] victorySoundData;
    private byte[] powerUpSpawnSoundData;

    private AudioFormat audioFormat;
    private ExecutorService soundExecutor;

    // Pre-opened lines for low-latency playback (needed for HDMI)
    private SourceDataLine shootLine;
    private SourceDataLine explosionLine;
    private volatile boolean shutdown = false;

    // Music playback
    private volatile boolean musicPlaying = false;
    private volatile boolean stopMusicRequested = false;
    private Thread musicThread;

    public SoundManager() {
        try {
            // Generate sounds if they don't exist
            generateSoundsIfNeeded();

            // Create thread pool with daemon threads that will terminate when app closes
            soundExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true); // Daemon threads die when app closes
                    return t;
                }
            });

            // Standard format for our generated WAV files
            audioFormat = new AudioFormat(22050, 16, 1, true, false);

            // Load sound files into memory as byte arrays
            shootSoundData = loadSoundData("/sounds/shoot.wav");
            explosionSoundData = loadSoundData("/sounds/explosion.wav");
            introSoundData = loadSoundData("/sounds/intro.wav");
            sadSoundData = loadSoundData("/sounds/sad.wav");
            playerDeathSoundData = loadSoundData("/sounds/player_death.wav");
            baseDestroyedSoundData = loadSoundData("/sounds/base_destroyed.wav");
            explanationMusicData = loadSoundData("/sounds/explanation_music.wav");
            treeBurnSoundData = loadSoundData("/sounds/tree_burn.wav");
            laserSoundData = loadSoundData("/sounds/laser.wav");
            victorySoundData = loadSoundData("/sounds/victory.wav");
            powerUpSpawnSoundData = loadSoundData("/sounds/powerup_spawn.wav");

            if (shootSoundData == null || explosionSoundData == null ||
                introSoundData == null || sadSoundData == null ||
                playerDeathSoundData == null || baseDestroyedSoundData == null ||
                explanationMusicData == null || treeBurnSoundData == null ||
                laserSoundData == null || victorySoundData == null ||
                powerUpSpawnSoundData == null) {
                LOG.warn("Some sounds could not be loaded. Game will run without sound effects.");
            } else {
                LOG.info("All sounds loaded successfully!");
                // Pre-open audio lines for shoot and explosion (fixes HDMI latency)
                initializeAudioLines();
                // Register shutdown hook to clean up audio lines
                registerShutdownHook();
            }
        } catch (Exception e) {
            LOG.error("Error initializing sounds: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeAudioLines() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            // Pre-open lines for low latency
            shootLine = (SourceDataLine) AudioSystem.getLine(info);
            shootLine.open(audioFormat, 4096);
            shootLine.start();

            explosionLine = (SourceDataLine) AudioSystem.getLine(info);
            explosionLine.open(audioFormat, 4096);
            explosionLine.start();

            LOG.info("Audio lines pre-opened for low latency!");
        } catch (LineUnavailableException e) {
            LOG.warn("Could not pre-open audio lines: {}", e.getMessage());
            shootLine = null;
            explosionLine = null;
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown = true;
            closeAudioLines();
        }));
    }

    private void closeAudioLines() {
        try {
            if (shootLine != null) {
                shootLine.stop();
                shootLine.flush();
                shootLine.close();
                shootLine = null;
            }
            if (explosionLine != null) {
                explosionLine.stop();
                explosionLine.flush();
                explosionLine.close();
                explosionLine = null;
            }
            if (soundExecutor != null) {
                soundExecutor.shutdownNow();
            }
        } catch (Exception e) {
            LOG.debug("Error closing audio lines during shutdown: {}", e.getMessage());
        }
    }

    public void stopGameplaySounds() {
        // Stop shoot and explosion sounds (used when game ends)
        try {
            if (shootLine != null && shootLine.isOpen()) {
                shootLine.flush();
            }
            if (explosionLine != null && explosionLine.isOpen()) {
                explosionLine.flush();
            }
        } catch (Exception e) {
            LOG.debug("Error stopping gameplay sounds: {}", e.getMessage());
        }
    }

    private void generateSoundsIfNeeded() {
        File shootFile = new File("src/main/resources/sounds/shoot.wav");
        File explosionFile = new File("src/main/resources/sounds/explosion.wav");
        File introFile = new File("src/main/resources/sounds/intro.wav");
        File sadFile = new File("src/main/resources/sounds/sad.wav");
        File playerDeathFile = new File("src/main/resources/sounds/player_death.wav");
        File baseDestroyedFile = new File("src/main/resources/sounds/base_destroyed.wav");
        File explanationMusicFile = new File("src/main/resources/sounds/explanation_music.wav");
        File treeBurnFile = new File("src/main/resources/sounds/tree_burn.wav");
        File laserFile = new File("src/main/resources/sounds/laser.wav");
        File victoryFile = new File("src/main/resources/sounds/victory.wav");
        File powerUpSpawnFile = new File("src/main/resources/sounds/powerup_spawn.wav");

        if (!shootFile.exists() || !explosionFile.exists() || !introFile.exists() || !sadFile.exists() ||
            !playerDeathFile.exists() || !baseDestroyedFile.exists() || !explanationMusicFile.exists() ||
            !treeBurnFile.exists() || !laserFile.exists() || !victoryFile.exists() || !powerUpSpawnFile.exists()) {
            LOG.info("Generating sound files...");
            SoundGenerator.generateAllSounds();
        }
    }

    private byte[] loadSoundData(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            AudioInputStream audioStream = null;

            if (resource != null) {
                audioStream = AudioSystem.getAudioInputStream(resource);
            } else {
                File file = new File("src/main/resources" + resourcePath);
                if (file.exists()) {
                    audioStream = AudioSystem.getAudioInputStream(file);
                }
            }

            if (audioStream != null) {
                byte[] data = audioStream.readAllBytes();
                audioStream.close();
                return data;
            }
        } catch (Exception e) {
            LOG.warn("Could not load sound: {} - {}", resourcePath, e.getMessage());
        }
        return null;
    }

    private void playSoundDirect(SourceDataLine line, byte[] soundData) {
        if (shutdown || line == null || soundData == null) return;

        soundExecutor.submit(() -> {
            try {
                if (!shutdown && line.isOpen()) {
                    // Stop and flush any previous sound to prevent queue buildup
                    line.stop();
                    line.flush();
                    line.start();
                    line.write(soundData, 0, soundData.length);
                }
            } catch (Exception e) {
                LOG.debug("Sound playback error (direct): {}", e.getMessage());
            }
        });
    }

    private void playSoundNew(byte[] soundData) {
        if (shutdown || soundData == null || audioFormat == null) return;

        soundExecutor.submit(() -> {
            SourceDataLine line = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat, 4096);
                line.start();
                line.write(soundData, 0, soundData.length);
                line.drain();
            } catch (Exception e) {
                LOG.debug("Sound playback error (new line): {}", e.getMessage());
            } finally {
                if (line != null) {
                    line.close();
                }
            }
        });
    }

    public void playShoot() {
        if (shootLine != null) {
            playSoundDirect(shootLine, shootSoundData);
        } else {
            playSoundNew(shootSoundData);
        }
    }

    public void playExplosion() {
        if (explosionLine != null) {
            playSoundDirect(explosionLine, explosionSoundData);
        } else {
            playSoundNew(explosionSoundData);
        }
    }

    public void playIntro() {
        playSoundNew(introSoundData);
    }

    public void playSad() {
        playSoundNew(sadSoundData);
    }

    public void playPlayerDeath() {
        playSoundNew(playerDeathSoundData);
    }

    public void playBaseDestroyed() {
        playSoundNew(baseDestroyedSoundData);
    }

    public void playTreeBurn() {
        playSoundNew(treeBurnSoundData);
    }

    public void playLaser() {
        playSoundNew(laserSoundData);
    }

    public void playVictory() {
        playSoundNew(victorySoundData);
    }

    public void playPowerUpSpawn() {
        playSoundNew(powerUpSpawnSoundData);
    }

    public void playExplanationMusic() {
        if (musicPlaying || explanationMusicData == null || audioFormat == null) return;

        stopMusicRequested = false;
        musicPlaying = true;

        musicThread = new Thread(() -> {
            SourceDataLine line = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat, 8192);
                line.start();

                // Loop the music until stopped
                while (!stopMusicRequested && !shutdown) {
                    // Write in chunks to allow for stopping
                    int chunkSize = 4096;
                    int offset = 0;
                    while (offset < explanationMusicData.length && !stopMusicRequested && !shutdown) {
                        int bytesToWrite = Math.min(chunkSize, explanationMusicData.length - offset);
                        line.write(explanationMusicData, offset, bytesToWrite);
                        offset += bytesToWrite;
                    }
                }

                line.drain();
            } catch (Exception e) {
                LOG.debug("Music playback error: {}", e.getMessage());
            } finally {
                if (line != null) {
                    line.stop();
                    line.close();
                }
                musicPlaying = false;
            }
        });
        musicThread.setDaemon(true);
        musicThread.start();
    }

    public void stopExplanationMusic() {
        stopMusicRequested = true;
        if (musicThread != null) {
            try {
                musicThread.join(500); // Wait up to 500ms for thread to stop
            } catch (InterruptedException e) {
                LOG.debug("Interrupted while stopping music: {}", e.getMessage());
                Thread.currentThread().interrupt(); // Preserve interrupt status
            }
        }
        musicPlaying = false;
    }

    public void shutdown() {
        shutdown = true;
        stopExplanationMusic();
        closeAudioLines();
    }
}
