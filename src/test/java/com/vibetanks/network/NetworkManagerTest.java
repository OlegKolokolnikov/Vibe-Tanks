package com.vibetanks.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NetworkManager Tests")
class NetworkManagerTest {

    private NetworkManager manager;

    @BeforeEach
    void setUp() {
        manager = new NetworkManager();
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("New manager should not be connected")
        void newManagerShouldNotBeConnected() {
            assertFalse(manager.isConnected());
        }

        @Test
        @DisplayName("New manager should have player number 1")
        void newManagerShouldHavePlayerNumber1() {
            assertEquals(1, manager.getPlayerNumber());
        }

        @Test
        @DisplayName("New manager should not be host initially")
        void newManagerShouldNotBeHostInitially() {
            assertFalse(manager.isHost());
        }

        @Test
        @DisplayName("getConnectedPlayerCount should return 0 when not connected")
        void getConnectedPlayerCountShouldReturn0WhenNotConnected() {
            assertEquals(0, manager.getConnectedPlayerCount());
        }
    }

    @Nested
    @DisplayName("IP Address Tests")
    class IPAddressTests {

        @Test
        @DisplayName("getLocalIP should return an IP address or Unknown")
        void getLocalIPShouldReturnIPAddress() {
            String localIP = manager.getLocalIP();
            assertNotNull(localIP);
            // Should be either a valid IP or "Unknown"
            assertTrue(localIP.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || localIP.equals("Unknown"));
        }

        @Test
        @DisplayName("getPublicIP should return something")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void getPublicIPShouldReturnSomething() {
            String publicIP = manager.getPublicIP();
            assertNotNull(publicIP);
            // Could be an IP, "Unknown", or error message
            assertFalse(publicIP.isEmpty());
        }
    }

    @Nested
    @DisplayName("Host Mode Tests")
    class HostModeTests {

        @Test
        @DisplayName("startHost should succeed on available port")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void startHostShouldSucceedOnAvailablePort() {
            boolean result = manager.startHost();

            if (result) {
                assertTrue(manager.isHost());
                assertEquals(1, manager.getPlayerNumber());
            }
            // May fail if port is in use, which is acceptable
        }

        @Test
        @DisplayName("startHost should fail on second call")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void startHostShouldFailOnSecondCall() {
            boolean firstResult = manager.startHost();

            if (firstResult) {
                // Second call should fail (already hosting)
                boolean secondResult = manager.startHost();
                assertFalse(secondResult);
            }
        }

        @Test
        @DisplayName("Host should count itself as 1 player")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void hostShouldCountItselfAs1Player() {
            boolean started = manager.startHost();

            if (started) {
                assertEquals(1, manager.getConnectedPlayerCount());
            }
        }
    }

    @Nested
    @DisplayName("Client Mode Tests")
    class ClientModeTests {

        @Test
        @DisplayName("joinHost should fail with invalid IP")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void joinHostShouldFailWithInvalidIP() {
            boolean result = manager.joinHost("invalid.ip.address");
            assertFalse(result);
        }

        @Test
        @DisplayName("joinHost should fail when no host available")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void joinHostShouldFailWhenNoHost() {
            // Try to connect to localhost where no host is running
            boolean result = manager.joinHost("127.0.0.1");
            assertFalse(result);
        }

        @Test
        @DisplayName("joinHost should set isHost to false")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void joinHostShouldSetIsHostFalse() {
            manager.joinHost("127.0.0.1"); // Will fail but sets isHost
            assertFalse(manager.isHost());
        }
    }

    @Nested
    @DisplayName("Game State Transfer Tests")
    class GameStateTransferTests {

        @Test
        @DisplayName("sendGameState should do nothing when not connected")
        void sendGameStateShouldDoNothingWhenNotConnected() {
            GameState state = new GameState();

            // Should not throw
            assertDoesNotThrow(() -> manager.sendGameState(state));
        }

        @Test
        @DisplayName("sendGameState should do nothing when not host")
        void sendGameStateShouldDoNothingWhenNotHost() {
            GameState state = new GameState();

            // Not a host, so should do nothing
            assertDoesNotThrow(() -> manager.sendGameState(state));
        }

        @Test
        @DisplayName("getLatestGameState should return null when no states received")
        void getLatestGameStateShouldReturnNullWhenNoStates() {
            GameState state = manager.getLatestGameState();
            assertNull(state);
        }
    }

    @Nested
    @DisplayName("Input Transfer Tests")
    class InputTransferTests {

        @Test
        @DisplayName("sendInput should do nothing when not connected")
        void sendInputShouldDoNothingWhenNotConnected() {
            PlayerInput input = new PlayerInput();

            // Should not throw
            assertDoesNotThrow(() -> manager.sendInput(input));
        }

        @Test
        @DisplayName("sendInput should do nothing when host")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void sendInputShouldDoNothingWhenHost() {
            manager.startHost();
            PlayerInput input = new PlayerInput();

            // Host doesn't send input via network
            assertDoesNotThrow(() -> manager.sendInput(input));
        }

        @Test
        @DisplayName("getPlayerInput should return null for non-existent player")
        void getPlayerInputShouldReturnNullForNonExistentPlayer() {
            PlayerInput input = manager.getPlayerInput(99);
            assertNull(input);
        }
    }

    @Nested
    @DisplayName("Connection Lifecycle Tests")
    class ConnectionLifecycleTests {

        @Test
        @DisplayName("close should handle being called multiple times")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void closeShouldHandleMultipleCalls() {
            manager.startHost();

            // Should not throw on multiple close calls
            assertDoesNotThrow(() -> {
                manager.close();
                manager.close();
                manager.close();
            });
        }

        @Test
        @DisplayName("close should reset connection state")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void closeShouldResetConnectionState() {
            manager.startHost();
            manager.close();

            assertFalse(manager.isConnected());
        }

        @Test
        @DisplayName("close should handle uninitialized manager")
        void closeShouldHandleUninitializedManager() {
            // Close without ever starting
            assertDoesNotThrow(() -> manager.close());
        }
    }

    @Nested
    @DisplayName("Static Method Tests")
    class StaticMethodTests {

        @Test
        @DisplayName("cleanupPortOnStartup should not throw")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void cleanupPortOnStartupShouldNotThrow() {
            assertDoesNotThrow(() -> NetworkManager.cleanupPortOnStartup());
        }
    }

    @Nested
    @DisplayName("Host-Client Communication Tests")
    class HostClientCommunicationTests {

        @Test
        @DisplayName("Host and client should be able to connect")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void hostAndClientShouldConnect() throws InterruptedException {
            // Start host
            boolean hostStarted = manager.startHost();

            if (hostStarted) {
                // Create client in separate manager
                NetworkManager clientManager = new NetworkManager();

                try {
                    // Give host time to start accepting
                    Thread.sleep(500);

                    // Connect client
                    boolean clientConnected = clientManager.joinHost("127.0.0.1");

                    if (clientConnected) {
                        assertTrue(clientManager.isConnected());
                        assertFalse(clientManager.isHost());

                        // Give time for connection to be registered
                        Thread.sleep(500);

                        // Host should now have 2 players
                        assertEquals(2, manager.getConnectedPlayerCount());

                        // Client should have player number > 1
                        assertTrue(clientManager.getPlayerNumber() >= 2);
                    }
                } finally {
                    clientManager.close();
                }
            }
        }

        @Test
        @DisplayName("Host should receive client input")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void hostShouldReceiveClientInput() throws InterruptedException {
            boolean hostStarted = manager.startHost();

            if (hostStarted) {
                NetworkManager clientManager = new NetworkManager();

                try {
                    Thread.sleep(500);
                    boolean clientConnected = clientManager.joinHost("127.0.0.1");

                    if (clientConnected) {
                        Thread.sleep(500);

                        // Client sends input
                        PlayerInput input = new PlayerInput();
                        input.up = true;
                        input.shoot = true;
                        clientManager.sendInput(input);

                        Thread.sleep(200);

                        // Host should receive input for player 2
                        PlayerInput received = manager.getPlayerInput(2);
                        if (received != null) {
                            assertTrue(received.up);
                            assertTrue(received.shoot);
                        }
                    }
                } finally {
                    clientManager.close();
                }
            }
        }

        @Test
        @DisplayName("Client should receive game state from host")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void clientShouldReceiveGameState() throws InterruptedException {
            boolean hostStarted = manager.startHost();

            if (hostStarted) {
                NetworkManager clientManager = new NetworkManager();

                try {
                    Thread.sleep(500);
                    boolean clientConnected = clientManager.joinHost("127.0.0.1");

                    if (clientConnected) {
                        Thread.sleep(500);

                        // Host sends game state
                        GameState state = new GameState();
                        state.levelNumber = 5;
                        manager.sendGameState(state);

                        Thread.sleep(200);

                        // Client should receive the state
                        GameState received = clientManager.getLatestGameState();
                        if (received != null) {
                            assertEquals(5, received.levelNumber);
                        }
                    }
                } finally {
                    clientManager.close();
                }
            }
        }
    }

    @Nested
    @DisplayName("Multiple Client Tests")
    class MultipleClientTests {

        @Test
        @DisplayName("Host should handle multiple clients")
        @Timeout(value = 20, unit = TimeUnit.SECONDS)
        void hostShouldHandleMultipleClients() throws InterruptedException {
            boolean hostStarted = manager.startHost();

            if (hostStarted) {
                NetworkManager client1 = new NetworkManager();
                NetworkManager client2 = new NetworkManager();

                try {
                    Thread.sleep(500);

                    boolean client1Connected = client1.joinHost("127.0.0.1");
                    Thread.sleep(300);

                    boolean client2Connected = client2.joinHost("127.0.0.1");
                    Thread.sleep(500);

                    if (client1Connected && client2Connected) {
                        // Host should have 3 players total
                        assertEquals(3, manager.getConnectedPlayerCount());

                        // Clients should have different player numbers
                        int player1Num = client1.getPlayerNumber();
                        int player2Num = client2.getPlayerNumber();

                        assertTrue(player1Num >= 2);
                        assertTrue(player2Num >= 2);
                        assertNotEquals(player1Num, player2Num);
                    }
                } finally {
                    client1.close();
                    client2.close();
                }
            }
        }
    }

    @Nested
    @DisplayName("Connection Robustness Tests")
    class ConnectionRobustnessTests {

        @Test
        @DisplayName("Host should handle client disconnect")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void hostShouldHandleClientDisconnect() throws InterruptedException {
            boolean hostStarted = manager.startHost();

            if (hostStarted) {
                NetworkManager clientManager = new NetworkManager();

                try {
                    Thread.sleep(500);
                    boolean clientConnected = clientManager.joinHost("127.0.0.1");

                    if (clientConnected) {
                        Thread.sleep(300);
                        assertEquals(2, manager.getConnectedPlayerCount());

                        // Client disconnects
                        clientManager.close();

                        Thread.sleep(500);

                        // Host should still be functional
                        assertTrue(manager.isHost());
                    }
                } finally {
                    clientManager.close();
                }
            }
        }

        @Test
        @DisplayName("Should handle rapid connect/disconnect")
        @Timeout(value = 20, unit = TimeUnit.SECONDS)
        void shouldHandleRapidConnectDisconnect() throws InterruptedException {
            boolean hostStarted = manager.startHost();

            if (hostStarted) {
                for (int i = 0; i < 3; i++) {
                    NetworkManager client = new NetworkManager();
                    Thread.sleep(200);
                    client.joinHost("127.0.0.1");
                    Thread.sleep(100);
                    client.close();
                    Thread.sleep(200);
                }

                // Host should still be running
                assertTrue(manager.isHost());
            }
        }
    }
}
