package com.vibetanks;

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
        // Realistic cannon/tank shot sound
        // Components: initial blast, pressure wave, mechanical click, echo tail

        double duration = 0.25;
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];
        java.util.Random random = new java.util.Random(42);

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE; // Time in seconds
            double tNorm = (double) i / numSamples; // Normalized 0-1

            double sample = 0;

            // 1. Initial BLAST - very loud white noise burst (first 10ms)
            if (t < 0.01) {
                double blastEnv = 1.0 - (t / 0.01);
                sample += (random.nextDouble() * 2 - 1) * blastEnv * 1.2;
            }

            // 2. Pressure wave - low frequency boom (20-60 Hz)
            double pressureFreq = 40 - t * 100; // Frequency drops
            if (pressureFreq > 20) {
                double pressureEnv = Math.exp(-t * 8);
                sample += Math.sin(2.0 * Math.PI * t * pressureFreq) * pressureEnv * 0.8;
            }

            // 3. Main body - filtered noise with resonance
            double bodyEnv = Math.exp(-t * 12);
            double filteredNoise = 0;
            // Simulate low-pass filtered noise
            for (int h = 1; h <= 5; h++) {
                double freq = 100 + random.nextDouble() * 200;
                filteredNoise += Math.sin(2.0 * Math.PI * t * freq * h) / h;
            }
            sample += filteredNoise * bodyEnv * 0.3;

            // 4. Mechanical "clack" - sharp transient at 1-2kHz
            if (t < 0.02) {
                double clackEnv = Math.exp(-t * 150);
                sample += Math.sin(2.0 * Math.PI * t * 1500) * clackEnv * 0.4;
                sample += Math.sin(2.0 * Math.PI * t * 2200) * clackEnv * 0.2;
            }

            // 5. Sub-bass thump (felt more than heard)
            double subEnv = Math.exp(-t * 6);
            sample += Math.sin(2.0 * Math.PI * t * 30) * subEnv * 0.5;

            // 6. Echo/reverb tail - quieter delayed noise
            if (t > 0.05) {
                double echoT = t - 0.05;
                double echoEnv = Math.exp(-echoT * 5) * 0.15;
                sample += (random.nextDouble() * 2 - 1) * echoEnv;
            }

            // Soft clipping for natural distortion
            sample = Math.tanh(sample * 0.8);

            // Convert to short
            short shortSample = (short) (sample * 0.85 * Short.MAX_VALUE);

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
