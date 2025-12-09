package com.vibetanks.animation;

import com.vibetanks.core.Base;
import com.vibetanks.core.GameConstants;
import com.vibetanks.core.Tank;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages victory and game-over celebration animations.
 * Extracted from Game.java to reduce complexity.
 */
public class CelebrationManager {

    private final List<DancingCharacter> dancingCharacters = new ArrayList<>();
    private final List<DancingGirl> victoryDancingGirls = new ArrayList<>();
    private boolean dancingInitialized = false;
    private boolean victoryDancingInitialized = false;

    /**
     * Reset all celebration state for new level/game restart.
     */
    public void reset() {
        dancingInitialized = false;
        dancingCharacters.clear();
        victoryDancingInitialized = false;
        victoryDancingGirls.clear();
    }

    /**
     * Initialize dancing characters for game-over animation (base destroyed).
     */
    public void initializeDancingCharacters(Base base, List<Tank> enemyTanks) {
        if (dancingInitialized) return;
        dancingInitialized = true;

        // Raise the skull flag on the destroyed base
        base.raiseFlag();

        // Create dancing aliens/humans from enemy tank positions
        if (!enemyTanks.isEmpty()) {
            for (Tank enemy : enemyTanks) {
                // Each enemy tank spawns 1-2 characters
                int numCharacters = 1 + GameConstants.RANDOM.nextInt(2);
                for (int i = 0; i < numCharacters; i++) {
                    double offsetX = (GameConstants.RANDOM.nextDouble() - 0.5) * 40;
                    double offsetY = (GameConstants.RANDOM.nextDouble() - 0.5) * 40;
                    boolean isAlien = GameConstants.RANDOM.nextBoolean();
                    int danceStyle = GameConstants.RANDOM.nextInt(3);
                    dancingCharacters.add(new DancingCharacter(
                        enemy.getX() + 16 + offsetX,
                        enemy.getY() + 16 + offsetY,
                        isAlien,
                        danceStyle
                    ));
                }
            }
        }

        // Also spawn some around the destroyed base
        double baseX = base.getX() + 32;
        double baseY = base.getY() + 32;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 * i) / 6;
            double radius = 60 + GameConstants.RANDOM.nextDouble() * 30;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + Math.sin(angle) * radius;
            boolean isAlien = GameConstants.RANDOM.nextBoolean();
            int danceStyle = GameConstants.RANDOM.nextInt(3);
            dancingCharacters.add(new DancingCharacter(x, y, isAlien, danceStyle));
        }
    }

    /**
     * Initialize victory celebration (dancing girls + flag).
     */
    public void initializeVictoryCelebration(Base base, int playerCount) {
        if (victoryDancingInitialized) return;
        victoryDancingInitialized = true;

        // Raise the Soviet victory flag on the base
        base.raiseVictoryFlag();

        // Spawn dancing girls based on player count (1-2 girls per player)
        int girlCount = playerCount + GameConstants.RANDOM.nextInt(playerCount + 1);

        // Position girls around the base
        double baseX = base.getX() + 16;
        double baseY = base.getY() - 20; // Above the base

        for (int i = 0; i < girlCount; i++) {
            // Spread girls in a semi-circle above the base
            double angle = Math.PI + (Math.PI * (i + 0.5) / girlCount);
            double radius = 80 + GameConstants.RANDOM.nextDouble() * 40;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + Math.sin(angle) * radius * 0.6;
            int danceStyle = GameConstants.RANDOM.nextInt(4);

            victoryDancingGirls.add(new DancingGirl(x, y, danceStyle));
        }

        System.out.println("Victory celebration initialized with " + girlCount +
            " dancing girls for " + playerCount + " players");
    }

    /**
     * Update all dancing characters (call each frame during game over).
     */
    public void updateDancingCharacters() {
        for (DancingCharacter dancer : dancingCharacters) {
            dancer.update();
        }
    }

    /**
     * Update all victory dancing girls (call each frame during victory).
     */
    public void updateVictoryGirls() {
        for (DancingGirl girl : victoryDancingGirls) {
            girl.update();
        }
    }

    // Getters for rendering and network sync
    public List<DancingCharacter> getDancingCharacters() { return dancingCharacters; }
    public List<DancingGirl> getVictoryDancingGirls() { return victoryDancingGirls; }
    public boolean isDancingInitialized() { return dancingInitialized; }
    public boolean isVictoryDancingInitialized() { return victoryDancingInitialized; }

    // Setters for network sync
    public void setDancingInitialized(boolean value) { this.dancingInitialized = value; }
    public void setVictoryDancingInitialized(boolean value) { this.victoryDancingInitialized = value; }
}
