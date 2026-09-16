package com.ejyqyl.dmstudio.service.simulator;

import javax.sound.sampled.*;

/**
 * Procedural audio feedback service for simulator events without external audio assets (§Play Mode).
 */
public class SoundPlaybackService {

    private static final SoundPlaybackService INSTANCE = new SoundPlaybackService();

    public static SoundPlaybackService getInstance() {
        return INSTANCE;
    }

    private SoundPlaybackService() {}

    /**
     * Plays a procedural tone based on Minecraft sound name or semantic event.
     */
    public void playSound(String soundName) {
        if (soundName == null || soundName.isBlank()) return;

        String lower = soundName.toLowerCase();
        int freq = 440;
        int durationMs = 80;

        if (lower.contains("click") || lower.contains("button")) {
            freq = 600;
            durationMs = 40;
        } else if (lower.contains("levelup") || lower.contains("yes") || lower.contains("success")) {
            freq = 880;
            durationMs = 120;
        } else if (lower.contains("no") || lower.contains("close") || lower.contains("error") || lower.contains("villager_no")) {
            freq = 240;
            durationMs = 150;
        } else if (lower.contains("orb") || lower.contains("exp")) {
            freq = 1100;
            durationMs = 60;
        }

        playToneAsync(freq, durationMs);
    }

    private void playToneAsync(int hz, int msecs) {
        Thread thread = new Thread(() -> {
            try {
                float sampleRate = 8000F;
                byte[] buf = new byte[(int) (sampleRate * msecs / 1000)];
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 80.0);
                }
                AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
                try (SourceDataLine sdl = AudioSystem.getSourceDataLine(format)) {
                    sdl.open(format);
                    sdl.start();
                    sdl.write(buf, 0, buf.length);
                    sdl.drain();
                }
            } catch (Exception ignored) {
                // If audio output is unavailable on the OS, fail silently
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
