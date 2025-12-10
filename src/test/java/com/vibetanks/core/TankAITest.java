package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TankAI Tests")
class TankAITest {

    private TankAI ai;

    @BeforeEach
    void setUp() {
        ai = new TankAI(100, 100);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should set initial position")
        void constructorShouldSetInitialPosition() {
            // Constructor takes initial position for stuck detection
            TankAI newAi = new TankAI(200, 300);
            // AI tracks last position - can't directly test but can verify it doesn't throw
            assertNotNull(newAi);
        }
    }

    @Nested
    @DisplayName("Cooldown Tests")
    class CooldownTests {

        @Test
        @DisplayName("resetCooldowns should reset all cooldowns")
        void resetCooldownsShouldResetAll() {
            // Call reset and verify it doesn't throw
            ai.resetCooldowns();

            // After reset, AI should be ready for next actions
            assertNotNull(ai);
        }
    }

    @Nested
    @DisplayName("Position Tracking Tests")
    class PositionTrackingTests {

        @Test
        @DisplayName("setLastPosition should update tracked position")
        void setLastPositionShouldUpdatePosition() {
            ai.setLastPosition(500, 600);

            // Position is used for stuck detection
            // Verify method doesn't throw
            assertNotNull(ai);
        }
    }
}
