package com.battlecity;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

public class SoundGenerator {

    private static final float SAMPLE_RATE = 22050;

    public static void generateAllSounds() {
        try {
            // Create resources directory if it doesn't exist
            File soundsDir = new File("src/main/resources/sounds");
            if (!soundsDir.exists()) {
                soundsDir.mkdirs();
            }

            // Generate shoot sound - gunshot with noise burst
            generateShootSound("src/main/resources/sounds/shoot.wav");

            // Generate explosion sound - lower pitched longer sound
            generateSound("src/main/resources/sounds/explosion.wav", 200, 0.3, 0.5);

            // Generate intro sound - simple melody
            generateIntroSound("src/main/resources/sounds/intro.wav");

            // Generate sad sound - descending sad melody for game over
            generateSadSound("src/main/resources/sounds/sad.wav");

            System.out.println("Sound files generated successfully!");
        } catch (Exception e) {
            System.err.println("Error generating sounds: " + e.getMessage());
        }
    }

    private static void generateShootSound(String filename) throws Exception {
        // Create a realistic gunshot sound with:
        // 1. Initial sharp attack (noise burst)
        // 2. Quick decay
        // 3. Low frequency thump

        double duration = 0.15; // Short, punchy
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];
        java.util.Random random = new java.util.Random(42);

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / numSamples;

            // Sharp exponential decay envelope
            double envelope = Math.exp(-t * 25);

            // Initial click/crack (white noise with fast decay)
            double noise = (random.nextDouble() * 2 - 1) * Math.exp(-t * 40);

            // Low frequency thump (gives body to the shot)
            double thump = Math.sin(2.0 * Math.PI * i * 80 / SAMPLE_RATE) * Math.exp(-t * 15);

            // Mid frequency punch
            double punch = Math.sin(2.0 * Math.PI * i * 200 / SAMPLE_RATE) * Math.exp(-t * 30);

            // High frequency crack
            double crack = Math.sin(2.0 * Math.PI * i * 800 / SAMPLE_RATE) * Math.exp(-t * 50);

            // Mix components
            double sample = (noise * 0.5 + thump * 0.3 + punch * 0.15 + crack * 0.05) * envelope;

            // Clip and convert to short
            sample = Math.max(-1.0, Math.min(1.0, sample));
            short shortSample = (short) (sample * 0.7 * Short.MAX_VALUE);

            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }

        saveWav(filename, buffer);
    }

    private static void generateSound(String filename, double frequency, double duration, double volume) throws Exception {
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / SAMPLE_RATE;
            short sample = (short) (Math.sin(angle) * volume * Short.MAX_VALUE);

            // Apply envelope for smoother sound
            double envelope = 1.0;
            if (i < numSamples * 0.1) {
                envelope = i / (numSamples * 0.1);
            } else if (i > numSamples * 0.8) {
                envelope = (numSamples - i) / (numSamples * 0.2);
            }
            sample = (short) (sample * envelope);

            buffer[i * 2] = (byte) (sample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        saveWav(filename, buffer);
    }

    private static void generateIntroSound(String filename) throws Exception {
        // Simple melody with multiple notes
        double[] frequencies = {523.25, 587.33, 659.25, 698.46, 784.00}; // C, D, E, F, G
        double noteDuration = 0.2;
        int totalSamples = (int) (frequencies.length * noteDuration * SAMPLE_RATE);
        byte[] buffer = new byte[totalSamples * 2];

        int bufferIndex = 0;
        for (double freq : frequencies) {
            int numSamples = (int) (noteDuration * SAMPLE_RATE);
            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
                short sample = (short) (Math.sin(angle) * 0.3 * Short.MAX_VALUE);

                // Apply envelope
                double envelope = 1.0;
                if (i < numSamples * 0.1) {
                    envelope = i / (numSamples * 0.1);
                } else if (i > numSamples * 0.7) {
                    envelope = (numSamples - i) / (numSamples * 0.3);
                }
                sample = (short) (sample * envelope);

                buffer[bufferIndex++] = (byte) (sample & 0xFF);
                buffer[bufferIndex++] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        saveWav(filename, buffer);
    }

    private static void generateSadSound(String filename) throws Exception {
        // Sad descending melody - minor scale
        double[] frequencies = {392.00, 349.23, 329.63, 293.66, 261.63}; // G, F, E, D, C (descending)
        double noteDuration = 0.4; // Slower, sadder
        int totalSamples = (int) (frequencies.length * noteDuration * SAMPLE_RATE);
        byte[] buffer = new byte[totalSamples * 2];

        int bufferIndex = 0;
        for (double freq : frequencies) {
            int numSamples = (int) (noteDuration * SAMPLE_RATE);
            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
                // Add slight wobble for sadder effect
                double wobble = 1.0 + 0.05 * Math.sin(2.0 * Math.PI * i * 3.0 / SAMPLE_RATE);
                short sample = (short) (Math.sin(angle * wobble) * 0.4 * Short.MAX_VALUE);

                // Apply longer envelope for sustained sad sound
                double envelope = 1.0;
                if (i < numSamples * 0.1) {
                    envelope = i / (numSamples * 0.1);
                } else if (i > numSamples * 0.6) {
                    envelope = (numSamples - i) / (numSamples * 0.4);
                }
                sample = (short) (sample * envelope);

                buffer[bufferIndex++] = (byte) (sample & 0xFF);
                buffer[bufferIndex++] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        saveWav(filename, buffer);
    }

    private static void saveWav(String filename, byte[] buffer) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        AudioInputStream audioInputStream = new AudioInputStream(bais, format, buffer.length / 2);

        File file = new File(filename);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
        audioInputStream.close();
    }

    public static void main(String[] args) {
        generateAllSounds();
    }
}
