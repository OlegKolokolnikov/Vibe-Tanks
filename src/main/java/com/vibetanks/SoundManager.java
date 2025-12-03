package com.vibetanks;

import javax.sound.sampled.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SoundManager {
    private byte[] shootSoundData;
    private byte[] explosionSoundData;
    private byte[] introSoundData;
    private byte[] sadSoundData;
    private byte[] playerDeathSoundData;
    private byte[] baseDestroyedSoundData;

    private AudioFormat audioFormat;
    private ExecutorService soundExecutor;

    // Pre-opened lines for low-latency playback (needed for HDMI)
    private SourceDataLine shootLine;
    private SourceDataLine explosionLine;
    private volatile boolean shutdown = false;

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

            if (shootSoundData == null || explosionSoundData == null ||
                introSoundData == null || sadSoundData == null ||
                playerDeathSoundData == null || baseDestroyedSoundData == null) {
                System.out.println("Some sounds could not be loaded. Game will run without sound effects.");
            } else {
                System.out.println("All sounds loaded successfully!");
                // Pre-open audio lines for shoot and explosion (fixes HDMI latency)
                initializeAudioLines();
                // Register shutdown hook to clean up audio lines
                registerShutdownHook();
            }
        } catch (Exception e) {
            System.out.println("Error initializing sounds: " + e.getMessage());
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

            System.out.println("Audio lines pre-opened for low latency!");
        } catch (LineUnavailableException e) {
            System.out.println("Could not pre-open audio lines: " + e.getMessage());
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
            // Ignore errors during shutdown
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
            // Ignore errors
        }
    }

    private void generateSoundsIfNeeded() {
        File shootFile = new File("src/main/resources/sounds/shoot.wav");
        File explosionFile = new File("src/main/resources/sounds/explosion.wav");
        File introFile = new File("src/main/resources/sounds/intro.wav");
        File sadFile = new File("src/main/resources/sounds/sad.wav");
        File playerDeathFile = new File("src/main/resources/sounds/player_death.wav");
        File baseDestroyedFile = new File("src/main/resources/sounds/base_destroyed.wav");

        if (!shootFile.exists() || !explosionFile.exists() || !introFile.exists() || !sadFile.exists() ||
            !playerDeathFile.exists() || !baseDestroyedFile.exists()) {
            System.out.println("Generating sound files...");
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
            System.out.println("Could not load sound: " + resourcePath + " - " + e.getMessage());
        }
        return null;
    }

    private void playSoundDirect(SourceDataLine line, byte[] soundData) {
        if (shutdown || line == null || soundData == null) return;

        soundExecutor.submit(() -> {
            try {
                if (!shutdown && line.isOpen()) {
                    line.write(soundData, 0, soundData.length);
                }
            } catch (Exception e) {
                // Ignore playback errors
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
                // Ignore playback errors
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

    public void shutdown() {
        shutdown = true;
        closeAudioLines();
    }
}
