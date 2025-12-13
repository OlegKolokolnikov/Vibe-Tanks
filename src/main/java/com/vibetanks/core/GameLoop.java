package com.vibetanks.core;

import com.vibetanks.util.GameLogger;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the game loop timing using ScheduledExecutorService.
 * Provides consistent 60 FPS across all platforms.
 * (AnimationTimer was tied to VSync which caused 30 FPS on some Windows systems)
 */
public class GameLoop {
    private static final GameLogger LOG = GameLogger.getLogger(GameLoop.class);
    private static final long TARGET_FRAME_TIME_MS = 16; // ~60 FPS

    private ScheduledExecutorService executor;
    private volatile boolean running = false;
    private final Runnable updateCallback;
    private final Runnable renderCallback;

    // FPS tracking
    private int frameCount = 0;
    private long lastFpsTime = System.currentTimeMillis();
    private double currentFps = 60.0;

    // Debug counters
    private int localUpdateCount = 0;
    private long lastDebugTime = System.currentTimeMillis();

    /**
     * Create a new game loop.
     * @param updateCallback Called every frame to update game state
     * @param renderCallback Called every frame to render (run on JavaFX thread)
     */
    public GameLoop(Runnable updateCallback, Runnable renderCallback) {
        this.updateCallback = updateCallback;
        this.renderCallback = renderCallback;
    }

    /**
     * Start the game loop.
     */
    public void start() {
        if (running) return;
        running = true;

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameLoop");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(this::tick, 0, TARGET_FRAME_TIME_MS, TimeUnit.MILLISECONDS);
        LOG.info("Game loop started at {} FPS target", 1000 / TARGET_FRAME_TIME_MS);
    }

    /**
     * Stop the game loop.
     */
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        LOG.info("Game loop stopped");
    }

    /**
     * Check if the game loop is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the current measured FPS.
     */
    public double getCurrentFps() {
        return currentFps;
    }

    /**
     * Called every frame by the executor.
     */
    private void tick() {
        if (!running) return;

        try {
            // Update FPS counter
            updateFpsCounter();

            // Update game state
            if (updateCallback != null) {
                updateCallback.run();
            }

            // Render on JavaFX thread
            if (renderCallback != null) {
                Platform.runLater(renderCallback);
            }
        } catch (Exception e) {
            LOG.error("Error in game loop tick: {}", e.getMessage());
            if (LOG.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    private void updateFpsCounter() {
        frameCount++;
        localUpdateCount++;

        long now = System.currentTimeMillis();

        // Update FPS every second
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount * 1000.0 / (now - lastFpsTime);
            frameCount = 0;
            lastFpsTime = now;
        }

        // Debug logging every 5 seconds
        if (now - lastDebugTime >= 5000) {
            LOG.debug("Game loop updates per second: {}", localUpdateCount / 5.0);
            localUpdateCount = 0;
            lastDebugTime = now;
        }
    }

    /**
     * Get the target frame time in milliseconds.
     */
    public static long getTargetFrameTimeMs() {
        return TARGET_FRAME_TIME_MS;
    }

    /**
     * Get the target FPS.
     */
    public static int getTargetFps() {
        return (int) (1000 / TARGET_FRAME_TIME_MS);
    }
}
