package com.battlecity;

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

    private AudioFormat audioFormat;
    private ExecutorService soundExecutor;

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

            if (shootSoundData == null || explosionSoundData == null ||
                introSoundData == null || sadSoundData == null) {
                System.out.println("Some sounds could not be loaded. Game will run without sound effects.");
            } else {
                System.out.println("All sounds loaded successfully!");
            }
        } catch (Exception e) {
            System.out.println("Error initializing sounds: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateSoundsIfNeeded() {
        File shootFile = new File("src/main/resources/sounds/shoot.wav");
        File explosionFile = new File("src/main/resources/sounds/explosion.wav");
        File introFile = new File("src/main/resources/sounds/intro.wav");
        File sadFile = new File("src/main/resources/sounds/sad.wav");

        if (!shootFile.exists() || !explosionFile.exists() || !introFile.exists() || !sadFile.exists()) {
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
                // Store the format from first file
                if (audioFormat == null) {
                    audioFormat = audioStream.getFormat();
                }
                byte[] data = audioStream.readAllBytes();
                audioStream.close();
                return data;
            }
        } catch (Exception e) {
            System.out.println("Could not load sound: " + resourcePath + " - " + e.getMessage());
        }
        return null;
    }

    private void playSound(byte[] soundData) {
        if (soundData == null || audioFormat == null) return;

        soundExecutor.submit(() -> {
            SourceDataLine line = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat, 4096); // Small buffer for low latency
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
        playSound(shootSoundData);
    }

    public void playExplosion() {
        playSound(explosionSoundData);
    }

    public void playIntro() {
        playSound(introSoundData);
    }

    public void playSad() {
        playSound(sadSoundData);
    }

    public void shutdown() {
        if (soundExecutor != null) {
            soundExecutor.shutdownNow();
        }
    }
}
