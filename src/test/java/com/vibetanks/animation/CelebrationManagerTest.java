package com.vibetanks.animation;

import com.vibetanks.core.Base;
import com.vibetanks.core.Direction;
import com.vibetanks.core.Tank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CelebrationManager Tests")
class CelebrationManagerTest {

    private CelebrationManager manager;
    private Base base;

    @BeforeEach
    void setUp() {
        manager = new CelebrationManager();
        base = new Base(400, 700);
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("New manager should have no dancing characters")
        void newManagerShouldHaveNoDancingCharacters() {
            assertTrue(manager.getDancingCharacters().isEmpty());
        }

        @Test
        @DisplayName("New manager should have no victory dancing girls")
        void newManagerShouldHaveNoVictoryGirls() {
            assertTrue(manager.getVictoryDancingGirls().isEmpty());
        }

        @Test
        @DisplayName("New manager should not be initialized")
        void newManagerShouldNotBeInitialized() {
            assertFalse(manager.isDancingInitialized());
            assertFalse(manager.isVictoryDancingInitialized());
        }
    }

    @Nested
    @DisplayName("Game Over Dancing Character Tests")
    class GameOverDancingTests {

        @Test
        @DisplayName("initializeDancingCharacters should create characters from enemies")
        void initializeDancingCharactersShouldCreateFromEnemies() {
            List<Tank> enemies = new ArrayList<>();
            enemies.add(new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR));
            enemies.add(new Tank(200, 200, Direction.DOWN, false, 0, Tank.EnemyType.FAST));

            manager.initializeDancingCharacters(base, enemies);

            assertTrue(manager.isDancingInitialized());
            assertFalse(manager.getDancingCharacters().isEmpty());
        }

        @Test
        @DisplayName("initializeDancingCharacters should spawn characters around base")
        void initializeDancingCharactersShouldSpawnAroundBase() {
            List<Tank> enemies = new ArrayList<>();

            manager.initializeDancingCharacters(base, enemies);

            assertTrue(manager.isDancingInitialized());
            // Should spawn 6 characters around base even with no enemies
            assertEquals(6, manager.getDancingCharacters().size());
        }

        @Test
        @DisplayName("initializeDancingCharacters should only initialize once")
        void initializeDancingCharactersShouldOnlyInitOnce() {
            List<Tank> enemies = new ArrayList<>();

            manager.initializeDancingCharacters(base, enemies);
            int initialCount = manager.getDancingCharacters().size();

            // Try to initialize again
            manager.initializeDancingCharacters(base, enemies);

            assertEquals(initialCount, manager.getDancingCharacters().size());
        }

        @Test
        @DisplayName("updateDancingCharacters should update all characters")
        void updateDancingCharactersShouldUpdateAll() {
            List<Tank> enemies = new ArrayList<>();
            enemies.add(new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR));

            manager.initializeDancingCharacters(base, enemies);

            // Should not throw
            assertDoesNotThrow(() -> manager.updateDancingCharacters());
        }
    }

    @Nested
    @DisplayName("Victory Celebration Tests")
    class VictoryCelebrationTests {

        @Test
        @DisplayName("initializeVictoryCelebration should create dancing girls")
        void initializeVictoryCelebrationShouldCreateGirls() {
            manager.initializeVictoryCelebration(base, 1);

            assertTrue(manager.isVictoryDancingInitialized());
            assertFalse(manager.getVictoryDancingGirls().isEmpty());
        }

        @Test
        @DisplayName("initializeVictoryCelebration should scale with player count")
        void initializeVictoryCelebrationShouldScaleWithPlayers() {
            CelebrationManager manager1 = new CelebrationManager();
            CelebrationManager manager2 = new CelebrationManager();
            Base base1 = new Base(400, 700);
            Base base2 = new Base(400, 700);

            manager1.initializeVictoryCelebration(base1, 1);
            manager2.initializeVictoryCelebration(base2, 2);

            // With more players, there should generally be more girls
            // (though randomness can vary, minimum is playerCount)
            assertTrue(manager1.getVictoryDancingGirls().size() >= 1);
            assertTrue(manager2.getVictoryDancingGirls().size() >= 2);
        }

        @Test
        @DisplayName("initializeVictoryCelebration should only initialize once")
        void initializeVictoryCelebrationShouldOnlyInitOnce() {
            manager.initializeVictoryCelebration(base, 1);
            int initialCount = manager.getVictoryDancingGirls().size();

            // Try to initialize again
            manager.initializeVictoryCelebration(base, 2);

            assertEquals(initialCount, manager.getVictoryDancingGirls().size());
        }

        @Test
        @DisplayName("updateVictoryGirls should update all girls")
        void updateVictoryGirlsShouldUpdateAll() {
            manager.initializeVictoryCelebration(base, 2);

            // Should not throw
            assertDoesNotThrow(() -> manager.updateVictoryGirls());
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("reset should clear dancing characters")
        void resetShouldClearDancingCharacters() {
            List<Tank> enemies = new ArrayList<>();
            enemies.add(new Tank(100, 100, Direction.UP, false, 0, Tank.EnemyType.REGULAR));
            manager.initializeDancingCharacters(base, enemies);

            manager.reset();

            assertTrue(manager.getDancingCharacters().isEmpty());
            assertFalse(manager.isDancingInitialized());
        }

        @Test
        @DisplayName("reset should clear victory girls")
        void resetShouldClearVictoryGirls() {
            manager.initializeVictoryCelebration(base, 2);

            manager.reset();

            assertTrue(manager.getVictoryDancingGirls().isEmpty());
            assertFalse(manager.isVictoryDancingInitialized());
        }

        @Test
        @DisplayName("reset should allow re-initialization")
        void resetShouldAllowReinitialization() {
            manager.initializeVictoryCelebration(base, 1);
            int firstCount = manager.getVictoryDancingGirls().size();

            manager.reset();
            manager.initializeVictoryCelebration(base, 1);

            assertTrue(manager.isVictoryDancingInitialized());
            assertFalse(manager.getVictoryDancingGirls().isEmpty());
        }
    }

    @Nested
    @DisplayName("Network Sync Setter Tests")
    class NetworkSyncTests {

        @Test
        @DisplayName("setDancingInitialized should update flag")
        void setDancingInitializedShouldUpdateFlag() {
            assertFalse(manager.isDancingInitialized());

            manager.setDancingInitialized(true);

            assertTrue(manager.isDancingInitialized());
        }

        @Test
        @DisplayName("setVictoryDancingInitialized should update flag")
        void setVictoryDancingInitializedShouldUpdateFlag() {
            assertFalse(manager.isVictoryDancingInitialized());

            manager.setVictoryDancingInitialized(true);

            assertTrue(manager.isVictoryDancingInitialized());
        }
    }
}
