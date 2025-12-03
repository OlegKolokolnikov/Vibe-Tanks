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

            // Generate player death sound - classic 8-bit death
            generatePlayerDeathSound("src/main/resources/sounds/player_death.wav");

            // Generate base destroyed sound - dramatic explosion
            generateBaseDestroyedSound("src/main/resources/sounds/base_destroyed.wav");

            System.out.println("Sound files generated successfully!");
        } catch (Exception e) {
            System.err.println("Error generating sounds: " + e.getMessage());
        }
    }

    private static void generateShootSound(String filename) throws Exception {
        // Retro 8-bit blip sound - classic arcade style
        double duration = 0.08; // Very short
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double tNorm = (double) i / numSamples;

            // Frequency sweep from high to low (classic 8-bit pew)
            double startFreq = 1200;
            double endFreq = 400;
            double freq = startFreq - (startFreq - endFreq) * tNorm;

            // Square wave for that classic 8-bit sound
            double phase = 2.0 * Math.PI * t * freq;
            double sample = Math.sin(phase) > 0 ? 1.0 : -1.0;

            // Add a bit of the fundamental sine for smoothness
            sample = sample * 0.7 + Math.sin(phase) * 0.3;

            // Quick attack, fast decay envelope
            double envelope;
            if (tNorm < 0.05) {
                envelope = tNorm / 0.05; // Quick attack
            } else {
                envelope = 1.0 - ((tNorm - 0.05) / 0.95); // Decay
                envelope = envelope * envelope; // Exponential decay
            }

            sample *= envelope * 0.6;

            // Convert to short
            short shortSample = (short) (sample * Short.MAX_VALUE);

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

    private static void generatePlayerDeathSound(String filename) throws Exception {
        // Classic 8-bit death sound - descending pitch with wobble
        double duration = 0.5;
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double tNorm = (double) i / numSamples;

            // Descending frequency (high to low - classic death sound)
            double startFreq = 800;
            double endFreq = 100;
            double freq = startFreq - (startFreq - endFreq) * tNorm;

            // Add wobble for dramatic effect
            double wobble = 1.0 + 0.1 * Math.sin(2.0 * Math.PI * t * 15);
            freq *= wobble;

            // Square wave for 8-bit feel
            double phase = 2.0 * Math.PI * t * freq;
            double sample = Math.sin(phase) > 0 ? 1.0 : -1.0;

            // Mix with triangle wave for fuller sound
            double triangle = 2.0 * Math.abs(2.0 * ((t * freq) % 1.0) - 1.0) - 1.0;
            sample = sample * 0.6 + triangle * 0.4;

            // Envelope - sustain then quick fade
            double envelope;
            if (tNorm < 0.1) {
                envelope = tNorm / 0.1;
            } else if (tNorm > 0.8) {
                envelope = (1.0 - tNorm) / 0.2;
            } else {
                envelope = 1.0;
            }

            sample *= envelope * 0.5;

            short shortSample = (short) (sample * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }

        saveWav(filename, buffer);
    }

    private static void generateBaseDestroyedSound(String filename) throws Exception {
        // Dramatic base explosion - big boom with alarm-like elements
        double duration = 1.0;
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];
        java.util.Random random = new java.util.Random(123);

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double tNorm = (double) i / numSamples;

            double sample = 0;

            // Low rumbling explosion base
            double explosionFreq = 60 - t * 30;
            if (explosionFreq > 20) {
                double explosionEnv = Math.exp(-t * 2);
                sample += Math.sin(2.0 * Math.PI * t * explosionFreq) * explosionEnv * 0.6;
            }

            // Alarm-like descending tone (like classic game over)
            double alarmFreq = 400 - tNorm * 300;
            double alarmPhase = 2.0 * Math.PI * t * alarmFreq;
            double alarm = Math.sin(alarmPhase) > 0 ? 1.0 : -1.0; // Square wave
            double alarmEnv = Math.exp(-t * 1.5);
            sample += alarm * alarmEnv * 0.3;

            // Noise burst at start
            if (t < 0.15) {
                double noiseEnv = Math.exp(-t * 10);
                sample += (random.nextDouble() * 2 - 1) * noiseEnv * 0.5;
            }

            // Secondary explosion hit
            if (t > 0.2 && t < 0.5) {
                double t2 = t - 0.2;
                double secondaryEnv = Math.exp(-t2 * 5);
                sample += Math.sin(2.0 * Math.PI * t2 * 80) * secondaryEnv * 0.4;
            }

            // Overall envelope
            double envelope = 1.0;
            if (tNorm > 0.7) {
                envelope = (1.0 - tNorm) / 0.3;
            }

            sample *= envelope * 0.7;

            // Soft clip
            sample = Math.tanh(sample);

            short shortSample = (short) (sample * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
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
