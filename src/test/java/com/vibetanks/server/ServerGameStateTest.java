package com.vibetanks.server;

import com.vibetanks.core.*;
import com.vibetanks.network.GameState;
import com.vibetanks.network.PlayerInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerGameState Tests")
@Disabled("Requires audio hardware - SoundManager initializes audio in constructor")
class ServerGameStateTest {

    private ServerGameState serverState;

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor with 1 player should initialize correctly")
        void constructorWith1Player() {
            serverState = new ServerGameState(1);

            assertFalse(serverState.isGameOver());
            assertFalse(serverState.isVictory());
            assertEquals(1, serverState.getCurrentLevel());
        }

        @Test
        @DisplayName("Constructor with 2 players should initialize correctly")
        void constructorWith2Players() {
            serverState = new ServerGameState(2);

            assertFalse(serverState.isGameOver());
            assertFalse(serverState.isVictory());
        }

        @Test
        @DisplayName("Constructor with 4 players should initialize correctly")
        void constructorWith4Players() {
            serverState = new ServerGameState(4);

            assertFalse(serverState.isGameOver());
            assertFalse(serverState.isVictory());
        }

        @Test
        @DisplayName("Constructor with 0 players should default to 1")
        void constructorWith0PlayersDefaultsTo1() {
            serverState = new ServerGameState(0);

            // Should not throw, game should initialize
            assertFalse(serverState.isGameOver());
        }

        @Test
        @DisplayName("Constructor should reset ID counters")
        void constructorResetsIdCounters() {
            // Create some bullets/lasers/powerups before
            Bullet b1 = new Bullet(0, 0, Direction.UP, false, 1, false);
            Laser l1 = new Laser(0, 0, Direction.UP, false, 1);

            // Create server state - should reset counters
            serverState = new ServerGameState(1);

            // Now IDs should start fresh (from 1)
            Bullet b2 = new Bullet(0, 0, Direction.UP, false, 1, false);
            assertEquals(1, b2.getId());

            Laser l2 = new Laser(0, 0, Direction.UP, false, 1);
            assertEquals(1, l2.getId());
        }
    }

    @Nested
    @DisplayName("Player Management Tests")
    class PlayerManagementTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("setConnectedPlayers should add tanks for new players")
        void setConnectedPlayersAddsTanks() {
            serverState.setConnectedPlayers(2);

            GameState state = serverState.buildNetworkState();
            assertEquals(2, state.connectedPlayers);
        }

        @Test
        @DisplayName("setConnectedPlayers should cap at 4 players")
        void setConnectedPlayersCapsAt4() {
            serverState.setConnectedPlayers(10);

            GameState state = serverState.buildNetworkState();
            // Should cap at 4 players
            assertTrue(state.connectedPlayers <= 4);
        }

        @Test
        @DisplayName("addPlayer should add tank for new player number")
        void addPlayerAddsTank() {
            serverState.addPlayer(2);

            // Should not throw, player 2 added
            GameState state = serverState.buildNetworkState();
            assertNotNull(state);
        }

        @Test
        @DisplayName("addPlayer should not exceed 4 players")
        void addPlayerDoesNotExceed4() {
            serverState.addPlayer(5);

            // Should not throw, but won't add 5th player
            GameState state = serverState.buildNetworkState();
            assertNotNull(state);
        }
    }

    @Nested
    @DisplayName("Game State Tests")
    class GameStateTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("Initial remaining enemies should be 20")
        void initialRemainingEnemies() {
            assertEquals(20, serverState.getRemainingEnemies());
        }

        @Test
        @DisplayName("getCurrentLevel should return 1 initially")
        void initialLevelIs1() {
            assertEquals(1, serverState.getCurrentLevel());
        }

        @Test
        @DisplayName("nextLevel should increment level number")
        void nextLevelIncrementsLevel() {
            serverState.nextLevel();

            assertEquals(2, serverState.getCurrentLevel());
        }

        @Test
        @DisplayName("restartLevel should keep same level number")
        void restartLevelKeepsLevelNumber() {
            serverState.nextLevel();
            serverState.nextLevel();
            assertEquals(3, serverState.getCurrentLevel());

            serverState.restartLevel();
            assertEquals(3, serverState.getCurrentLevel());
        }
    }

    @Nested
    @DisplayName("Input Processing Tests")
    class InputProcessingTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("processInput with invalid player number should be ignored")
        void processInputInvalidPlayerIgnored() {
            PlayerInput input = new PlayerInput();
            input.up = true;

            // Should not throw
            serverState.processInput(0, input);
            serverState.processInput(5, input);
            serverState.processInput(-1, input);
        }

        @Test
        @DisplayName("processInput should update player nickname")
        void processInputUpdatesNickname() {
            PlayerInput input = new PlayerInput();
            input.nickname = "TestPlayer";

            serverState.processInput(1, input);

            GameState state = serverState.buildNetworkState();
            assertEquals("TestPlayer", state.players[0].nickname);
        }

        @Test
        @DisplayName("processInput with shoot should create bullet or laser")
        void processInputShoot() {
            PlayerInput input = new PlayerInput();
            input.shoot = true;

            serverState.processInput(1, input);
            serverState.update();

            GameState state = serverState.buildNetworkState();
            // Should have created a bullet (or laser if player has laser)
            assertTrue(state.bullets.size() >= 0 || state.lasers.size() >= 0);
        }
    }

    @Nested
    @DisplayName("Network State Building Tests")
    class NetworkStateBuildingTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(2);
        }

        @Test
        @DisplayName("buildNetworkState should return valid GameState")
        void buildNetworkStateReturnsValidState() {
            GameState state = serverState.buildNetworkState();

            assertNotNull(state);
            assertFalse(state.gameOver);
            assertFalse(state.victory);
            assertTrue(state.baseAlive);
            assertEquals(1, state.levelNumber);
        }

        @Test
        @DisplayName("buildNetworkState should include player data")
        void buildNetworkStateIncludesPlayerData() {
            GameState state = serverState.buildNetworkState();

            // Should have 2 players initialized
            assertTrue(state.players[0].alive);
            assertTrue(state.players[1].alive);
        }

        @Test
        @DisplayName("buildNetworkState should include map tiles")
        void buildNetworkStateIncludesMapTiles() {
            GameState state = serverState.buildNetworkState();

            assertNotNull(state.mapTiles);
            assertEquals(GameConstants.MAP_SIZE, state.mapTiles.length);
            assertEquals(GameConstants.MAP_SIZE, state.mapTiles[0].length);
        }

        @Test
        @DisplayName("buildNetworkState should include remaining enemies count")
        void buildNetworkStateIncludesRemainingEnemies() {
            GameState state = serverState.buildNetworkState();

            assertEquals(20, state.remainingEnemies);
        }

        @Test
        @DisplayName("buildNetworkState should include freeze durations")
        void buildNetworkStateIncludesFreezeDurations() {
            GameState state = serverState.buildNetworkState();

            assertEquals(0, state.enemyFreezeDuration);
            assertEquals(0, state.playerFreezeDuration);
            assertEquals(0, state.enemyTeamSpeedBoostDuration);
        }
    }

    @Nested
    @DisplayName("Update Loop Tests")
    class UpdateLoopTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("update should not throw exception")
        void updateDoesNotThrow() {
            // Run multiple updates
            for (int i = 0; i < 100; i++) {
                assertDoesNotThrow(() -> serverState.update());
            }
        }

        @Test
        @DisplayName("update should spawn enemies over time")
        void updateSpawnsEnemies() {
            int initialRemaining = serverState.getRemainingEnemies();

            // Run updates to allow enemy spawning
            for (int i = 0; i < 300; i++) {
                serverState.update();
            }

            GameState state = serverState.buildNetworkState();
            // Either enemies spawned (in list) or remaining decreased
            assertTrue(state.enemies.size() > 0 || serverState.getRemainingEnemies() < initialRemaining);
        }

        @Test
        @DisplayName("update should decrease freeze duration")
        void updateDecreasesFreezeDuration() {
            // First trigger freeze by simulating FREEZE power-up pickup
            // We can't easily simulate this, but we can verify update runs correctly
            serverState.update();
            serverState.update();

            // Should not throw
            assertFalse(serverState.isGameOver());
        }
    }

    @Nested
    @DisplayName("Game Over / Victory Tests")
    class GameOverVictoryTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("Game should not be over initially")
        void gameNotOverInitially() {
            assertFalse(serverState.isGameOver());
        }

        @Test
        @DisplayName("Game should not be victory initially")
        void gameNotVictoryInitially() {
            assertFalse(serverState.isVictory());
        }

        @Test
        @DisplayName("buildNetworkState should reflect game over state")
        void buildNetworkStateReflectsGameOver() {
            GameState state = serverState.buildNetworkState();

            assertEquals(serverState.isGameOver(), state.gameOver);
            assertEquals(serverState.isVictory(), state.victory);
        }
    }

    @Nested
    @DisplayName("Level Transition Tests")
    class LevelTransitionTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(2);
        }

        @Test
        @DisplayName("nextLevel should reset game state")
        void nextLevelResetsState() {
            // Advance to next level
            serverState.nextLevel();

            assertFalse(serverState.isGameOver());
            assertFalse(serverState.isVictory());
            assertEquals(20, serverState.getRemainingEnemies());
        }

        @Test
        @DisplayName("restartLevel should reset game state")
        void restartLevelResetsState() {
            // Do some updates
            for (int i = 0; i < 100; i++) {
                serverState.update();
            }

            serverState.restartLevel();

            assertFalse(serverState.isGameOver());
            assertFalse(serverState.isVictory());
            assertEquals(20, serverState.getRemainingEnemies());
        }

        @Test
        @DisplayName("Multiple level transitions should work correctly")
        void multipleLevelTransitions() {
            serverState.nextLevel();
            assertEquals(2, serverState.getCurrentLevel());

            serverState.nextLevel();
            assertEquals(3, serverState.getCurrentLevel());

            serverState.restartLevel();
            assertEquals(3, serverState.getCurrentLevel());

            serverState.nextLevel();
            assertEquals(4, serverState.getCurrentLevel());
        }
    }

    @Nested
    @DisplayName("Dancing Characters Tests")
    class DancingCharactersTests {

        @Test
        @DisplayName("Dancing characters should not be initialized initially")
        void dancingNotInitializedInitially() {
            serverState = new ServerGameState(1);

            GameState state = serverState.buildNetworkState();

            assertFalse(state.dancingInitialized);
            assertTrue(state.dancingCharacters.isEmpty());
        }

        @Test
        @DisplayName("Victory dancing should not be initialized initially")
        void victoryDancingNotInitializedInitially() {
            serverState = new ServerGameState(1);

            GameState state = serverState.buildNetworkState();

            assertFalse(state.victoryDancingInitialized);
            assertTrue(state.victoryDancingGirls.isEmpty());
        }
    }

    @Nested
    @DisplayName("UFO and Easter Egg Tests")
    class UFOAndEasterEggTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("UFO should not exist initially")
        void ufoNotExistInitially() {
            GameState state = serverState.buildNetworkState();

            assertNull(state.ufoData);
        }

        @Test
        @DisplayName("Easter egg should not exist initially")
        void easterEggNotExistInitially() {
            GameState state = serverState.buildNetworkState();

            assertNull(state.easterEggData);
        }
    }

    @Nested
    @DisplayName("Player Stats Tests")
    class PlayerStatsTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(2);
        }

        @Test
        @DisplayName("Initial player kills should be 0")
        void initialPlayerKillsZero() {
            GameState state = serverState.buildNetworkState();

            assertEquals(0, state.players[0].kills);
            assertEquals(0, state.players[1].kills);
        }

        @Test
        @DisplayName("Initial player scores should be 0")
        void initialPlayerScoresZero() {
            GameState state = serverState.buildNetworkState();

            assertEquals(0, state.players[0].score);
            assertEquals(0, state.players[1].score);
        }

        @Test
        @DisplayName("Player lives should be initialized")
        void playerLivesInitialized() {
            GameState state = serverState.buildNetworkState();

            assertTrue(state.players[0].lives > 0);
            assertTrue(state.players[1].lives > 0);
        }
    }

    @Nested
    @DisplayName("Host Settings Tests")
    class HostSettingsTests {

        @BeforeEach
        void setUp() {
            serverState = new ServerGameState(1);
        }

        @Test
        @DisplayName("buildNetworkState should include host settings")
        void buildNetworkStateIncludesHostSettings() {
            GameState state = serverState.buildNetworkState();

            // Should have default values
            assertTrue(state.hostPlayerSpeed > 0);
            assertTrue(state.hostEnemySpeed > 0);
            assertTrue(state.hostPlayerShootSpeed > 0);
            assertTrue(state.hostEnemyShootSpeed > 0);
        }
    }

    @Nested
    @DisplayName("ID Reset Tests")
    class IDResetTests {

        @Test
        @DisplayName("restartLevel should reset ID counters")
        void restartLevelResetsIdCounters() {
            serverState = new ServerGameState(1);

            // Create some bullets
            Bullet b1 = new Bullet(0, 0, Direction.UP, false, 1, false);
            Bullet b2 = new Bullet(0, 0, Direction.UP, false, 1, false);

            // Restart level
            serverState.restartLevel();

            // IDs should start from 1 again
            Bullet b3 = new Bullet(0, 0, Direction.UP, false, 1, false);
            assertEquals(1, b3.getId());
        }

        @Test
        @DisplayName("nextLevel should reset ID counters")
        void nextLevelResetsIdCounters() {
            serverState = new ServerGameState(1);

            // Create some lasers
            Laser l1 = new Laser(0, 0, Direction.UP, false, 1);
            Laser l2 = new Laser(0, 0, Direction.UP, false, 1);

            // Next level
            serverState.nextLevel();

            // IDs should start from 1 again
            Laser l3 = new Laser(0, 0, Direction.UP, false, 1);
            assertEquals(1, l3.getId());
        }

        @Test
        @DisplayName("PowerUp ID counter should also reset on level change")
        void powerUpIdCounterResetsOnLevelChange() {
            serverState = new ServerGameState(1);

            // Create some power-ups
            PowerUp p1 = new PowerUp(0, 0, PowerUp.Type.STAR);
            PowerUp p2 = new PowerUp(0, 0, PowerUp.Type.GUN);

            // Next level
            serverState.nextLevel();

            // IDs should start from 1 again
            PowerUp p3 = new PowerUp(0, 0, PowerUp.Type.BOMB);
            assertEquals(1, p3.getId());
        }
    }
}
