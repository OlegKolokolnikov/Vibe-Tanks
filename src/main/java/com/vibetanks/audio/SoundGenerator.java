package com.vibetanks.audio;

import com.vibetanks.util.GameLogger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

public class SoundGenerator {
    private static final GameLogger LOG = GameLogger.getLogger(SoundGenerator.class);

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

            // Generate explanation screen music - 8-bit style loop
            generateExplanationMusic("src/main/resources/sounds/explanation_music.wav");

            // Generate tree burn sound - crackling fire
            generateTreeBurnSound("src/main/resources/sounds/tree_burn.wav");

            // Generate laser sound - sci-fi zap
            generateLaserSound("src/main/resources/sounds/laser.wav");

            LOG.info("Sound files generated successfully!");
        } catch (Exception e) {
            LOG.error("Error generating sounds: {}", e.getMessage());
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

    private static void generateExplanationMusic(String filename) throws Exception {
        // 8-bit style background music - upbeat chiptune loop (~15 seconds)
        double duration = 15.0;
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];

        // Musical parameters
        double bpm = 120;
        double beatDuration = 60.0 / bpm;
        double sixteenthNote = beatDuration / 4;

        // C major scale frequencies
        double C4 = 261.63, D4 = 293.66, E4 = 329.63, F4 = 349.23, G4 = 392.00, A4 = 440.00, B4 = 493.88;
        double C5 = 523.25, D5 = 587.33, E5 = 659.25, G5 = 784.00;
        double C3 = 130.81, E3 = 164.81, G3 = 196.00, A3 = 220.00;

        // Melody pattern (notes with durations in sixteenth notes)
        double[][] melody = {
            {C5, 2}, {E5, 2}, {G5, 2}, {E5, 2},  // Bar 1
            {D5, 2}, {G4, 2}, {A4, 2}, {B4, 2},  // Bar 2
            {C5, 4}, {G4, 4},                     // Bar 3
            {E4, 2}, {F4, 2}, {G4, 2}, {A4, 2},  // Bar 4
            {G4, 2}, {E4, 2}, {C4, 2}, {E4, 2},  // Bar 5
            {D4, 2}, {F4, 2}, {A4, 2}, {G4, 2},  // Bar 6
            {E4, 4}, {C5, 4},                     // Bar 7
            {G4, 2}, {A4, 2}, {B4, 2}, {C5, 2},  // Bar 8
        };

        // Bass pattern (root notes)
        double[][] bass = {
            {C3, 8}, {G3, 8},   // Bar 1-2
            {A3, 8}, {E3, 8},   // Bar 3-4
            {C3, 8}, {G3, 8},   // Bar 5-6
            {A3, 8}, {G3, 8},   // Bar 7-8
        };

        // Arpeggio pattern for texture
        double[] arpNotes = {C4, E4, G4, E4};

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sample = 0;

            // Calculate current beat position (loops every 8 bars = 16 beats)
            double loopDuration = 16 * beatDuration;
            double loopT = t % loopDuration;
            double beatPos = loopT / sixteenthNote; // Position in sixteenth notes

            // --- Melody (square wave, main voice) ---
            double melodyFreq = 0;
            double melodyNoteStart = 0;
            double melodyNoteDur = 0;
            double notePos = 0;
            for (double[] note : melody) {
                if (beatPos >= notePos && beatPos < notePos + note[1]) {
                    melodyFreq = note[0];
                    melodyNoteStart = notePos * sixteenthNote;
                    melodyNoteDur = note[1] * sixteenthNote;
                    break;
                }
                notePos += note[1];
            }

            if (melodyFreq > 0) {
                double noteT = loopT - melodyNoteStart;
                double phase = 2.0 * Math.PI * noteT * melodyFreq;
                // Pulse wave (25% duty cycle for chiptune sound)
                double pulse = (Math.sin(phase) > 0.5) ? 1.0 : -1.0;
                // Envelope
                double env = 1.0;
                if (noteT < 0.02) env = noteT / 0.02;
                else if (noteT > melodyNoteDur - 0.05) env = (melodyNoteDur - noteT) / 0.05;
                env = Math.max(0, Math.min(1, env));
                sample += pulse * env * 0.25;
            }

            // --- Bass (triangle wave) ---
            double bassFreq = 0;
            double bassNoteStart = 0;
            double bassNoteDur = 0;
            notePos = 0;
            for (double[] note : bass) {
                if (beatPos >= notePos && beatPos < notePos + note[1]) {
                    bassFreq = note[0];
                    bassNoteStart = notePos * sixteenthNote;
                    bassNoteDur = note[1] * sixteenthNote;
                    break;
                }
                notePos += note[1];
            }

            if (bassFreq > 0) {
                double noteT = loopT - bassNoteStart;
                // Triangle wave
                double period = 1.0 / bassFreq;
                double triPhase = (noteT % period) / period;
                double triangle = 4.0 * Math.abs(triPhase - 0.5) - 1.0;
                // Envelope
                double env = 1.0;
                if (noteT < 0.01) env = noteT / 0.01;
                else if (noteT > bassNoteDur - 0.1) env = (bassNoteDur - noteT) / 0.1;
                env = Math.max(0, Math.min(1, env));
                sample += triangle * env * 0.2;
            }

            // --- Arpeggio (background texture, quiet) ---
            double arpSpeed = sixteenthNote / 2; // Fast arpeggios
            int arpIndex = (int) ((loopT / arpSpeed) % arpNotes.length);
            double arpFreq = arpNotes[arpIndex];
            double arpPhase = 2.0 * Math.PI * t * arpFreq;
            double arp = Math.sin(arpPhase);
            sample += arp * 0.08;

            // --- Simple drum pattern (noise hits on beat) ---
            double beatInLoop = loopT / beatDuration;
            double beatFrac = beatInLoop % 1.0;
            if (beatFrac < 0.05) {
                // Kick-like thump on every beat
                double kickEnv = 1.0 - (beatFrac / 0.05);
                double kickFreq = 80 - beatFrac * 400;
                sample += Math.sin(2.0 * Math.PI * t * kickFreq) * kickEnv * 0.15;
            }
            // Hi-hat on off-beats
            double halfBeatFrac = (beatInLoop + 0.5) % 1.0;
            if (halfBeatFrac < 0.03) {
                double hatEnv = 1.0 - (halfBeatFrac / 0.03);
                // Noise for hi-hat
                sample += (Math.random() * 2 - 1) * hatEnv * 0.06;
            }

            // Soft clip to prevent distortion
            sample = Math.tanh(sample * 1.2);

            // Overall volume
            sample *= 0.7;

            short shortSample = (short) (sample * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }

        saveWav(filename, buffer);
    }

    private static void generateTreeBurnSound(String filename) throws Exception {
        // Classic 8-bit fire/burning sound - crackling with whoosh
        double duration = 0.25;
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];
        java.util.Random random = new java.util.Random(456);

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double tNorm = (double) i / numSamples;

            double sample = 0;

            // Crackling noise bursts (like fire)
            double crackle = (random.nextDouble() * 2 - 1);
            // Filter the noise to make it more fire-like
            double crackleFreq = 200 + random.nextDouble() * 800;
            crackle *= Math.sin(2.0 * Math.PI * t * crackleFreq);

            // Whoosh sound - rising then falling frequency
            double whooshFreq;
            if (tNorm < 0.3) {
                whooshFreq = 100 + tNorm * 400; // Rising
            } else {
                whooshFreq = 220 - (tNorm - 0.3) * 200; // Falling
            }
            double whoosh = Math.sin(2.0 * Math.PI * t * whooshFreq);

            // Mix crackle and whoosh
            sample = crackle * 0.4 + whoosh * 0.3;

            // Add some low rumble
            sample += Math.sin(2.0 * Math.PI * t * 80) * 0.2;

            // Envelope - quick attack, medium decay
            double envelope;
            if (tNorm < 0.1) {
                envelope = tNorm / 0.1;
            } else {
                envelope = 1.0 - ((tNorm - 0.1) / 0.9);
                envelope = Math.sqrt(envelope); // Slower decay
            }

            sample *= envelope * 0.6;

            // Soft clip
            sample = Math.tanh(sample);

            short shortSample = (short) (sample * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }

        saveWav(filename, buffer);
    }

    private static void generateLaserSound(String filename) throws Exception {
        // Sci-fi laser beam sound - electronic zap with harmonics
        double duration = 0.2;
        int numSamples = (int) (duration * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double tNorm = (double) i / numSamples;

            double sample = 0;

            // Main laser frequency - high pitched sweep down then up
            double mainFreq;
            if (tNorm < 0.3) {
                // Quick sweep down
                mainFreq = 2000 - tNorm * 2000;
            } else {
                // Hold and slight rise
                mainFreq = 1400 + (tNorm - 0.3) * 800;
            }

            // Add vibrato for that classic laser wobble
            double vibrato = 1.0 + 0.05 * Math.sin(2.0 * Math.PI * t * 40);
            mainFreq *= vibrato;

            // Saw wave for harsh laser sound
            double phase = (t * mainFreq) % 1.0;
            double saw = 2.0 * phase - 1.0;

            // Square wave harmonic
            double square = Math.sin(2.0 * Math.PI * t * mainFreq * 0.5) > 0 ? 1.0 : -1.0;

            // Mix saw and square for rich laser tone
            sample = saw * 0.5 + square * 0.3;

            // Add high frequency sizzle
            sample += Math.sin(2.0 * Math.PI * t * mainFreq * 2) * 0.15;

            // Add some noise for energy feel
            if (tNorm < 0.1) {
                sample += (Math.random() * 2 - 1) * 0.2 * (1 - tNorm * 10);
            }

            // Envelope - quick attack, sustain, quick release
            double envelope;
            if (tNorm < 0.05) {
                envelope = tNorm / 0.05; // Quick attack
            } else if (tNorm > 0.85) {
                envelope = (1.0 - tNorm) / 0.15; // Quick release
            } else {
                envelope = 1.0; // Sustain
            }

            sample *= envelope * 0.5;

            // Soft clip for warmth
            sample = Math.tanh(sample * 1.5);

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
