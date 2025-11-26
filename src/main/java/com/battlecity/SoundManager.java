package com.battlecity;

import javafx.scene.media.AudioClip;
import java.io.File;
import java.net.URL;

public class SoundManager {
    private AudioClip shootSound;
    private AudioClip explosionSound;
    private AudioClip introSound;

    public SoundManager() {
        try {
            // Generate sounds if they don't exist
            generateSoundsIfNeeded();

            // Try to load sound files
            shootSound = loadSound("/sounds/shoot.wav");
            explosionSound = loadSound("/sounds/explosion.wav");
            introSound = loadSound("/sounds/intro.wav");

            if (shootSound == null || explosionSound == null || introSound == null) {
                System.out.println("Some sounds could not be loaded. Game will run without sound effects.");
            } else {
                System.out.println("All sounds loaded successfully!");
            }
        } catch (Exception e) {
            System.out.println("Error initializing sounds: " + e.getMessage());
        }
    }

    private void generateSoundsIfNeeded() {
        File shootFile = new File("src/main/resources/sounds/shoot.wav");
        File explosionFile = new File("src/main/resources/sounds/explosion.wav");
        File introFile = new File("src/main/resources/sounds/intro.wav");

        if (!shootFile.exists() || !explosionFile.exists() || !introFile.exists()) {
            System.out.println("Generating sound files...");
            SoundGenerator.generateAllSounds();
        }
    }

    private AudioClip loadSound(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource != null) {
                AudioClip clip = new AudioClip(resource.toString());
                clip.setVolume(0.5); // Set volume to 50%
                return clip;
            } else {
                // Try loading from file system
                File file = new File("src/main/resources" + resourcePath);
                if (file.exists()) {
                    AudioClip clip = new AudioClip(file.toURI().toString());
                    clip.setVolume(0.5);
                    return clip;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load sound: " + resourcePath);
        }
        return null;
    }

    public void playShoot() {
        if (shootSound != null) {
            shootSound.play();
        }
    }

    public void playExplosion() {
        if (explosionSound != null) {
            explosionSound.play();
        }
    }

    public void playIntro() {
        if (introSound != null) {
            introSound.play();
        }
    }
}
