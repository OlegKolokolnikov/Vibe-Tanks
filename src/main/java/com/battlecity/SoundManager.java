package com.battlecity;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private byte[] shootSoundData;
    private byte[] explosionSoundData;
    private byte[] introSoundData;
    private byte[] sadSoundData;

    private AudioFormat audioFormat;
    private ExecutorService soundExecutor;

    // Pre-opened lines for low-latency playback
    private SourceDataLine shootLine;
    private SourceDataLine explosionLine;

    public SoundManager() {
        try {
            // Generate sounds if they don't exist
            generateSoundsIfNeeded();

            // Create thread pool for async sound playback
            soundExecutor = Executors.newFixedThreadPool(4);

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
                // Pre-open audio lines for shoot and explosion (frequently used)
                initializeAudioLines();
            }
        } catch (Exception e) {
            System.out.println("Error initializing sounds: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeAudioLines() {
        try {
            // Standard format for our generated WAV files
            audioFormat = new AudioFormat(22050, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            // Pre-open lines for low latency
            shootLine = (SourceDataLine) AudioSystem.getLine(info);
            shootLine.open(audioFormat, 4096); // Small buffer for low latency
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

    private void playSoundDirect(SourceDataLine line, byte[] soundData) {
        if (line != null && soundData != null) {
            soundExecutor.submit(() -> {
                try {
                    // Write directly to pre-opened line
                    line.write(soundData, 0, soundData.length);
                } catch (Exception e) {
                    // Ignore playback errors
                }
            });
        }
    }

    private void playSoundNew(byte[] soundData) {
        if (soundData == null || audioFormat == null) return;

        soundExecutor.submit(() -> {
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audioFormat);
                line.start();
                line.write(soundData, 0, soundData.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                // Ignore playback errors
            }
        });
    }

    public void playShoot() {
        if (shootLine != null && shootSoundData != null) {
            playSoundDirect(shootLine, shootSoundData);
        } else {
            playSoundNew(shootSoundData);
        }
    }

    public void playExplosion() {
        if (explosionLine != null && explosionSoundData != null) {
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

    public void shutdown() {
        if (soundExecutor != null) {
            soundExecutor.shutdown();
        }
        if (shootLine != null) {
            shootLine.close();
        }
        if (explosionLine != null) {
            explosionLine.close();
        }
    }
}
