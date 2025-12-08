package com.vibetanks.server;

import com.vibetanks.core.GameSettings;
import com.vibetanks.network.GameState;
import com.vibetanks.network.PlayerInput;
import com.vibetanks.util.GameLogger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Dedicated server for VibeTanks - runs headless without graphics.
 * Manages game state and synchronizes all connected players.
 *
 * Usage: java -cp <classpath> com.vibetanks.server.DedicatedServer [port]
 */
public class DedicatedServer {
    private static final GameLogger LOG = GameLogger.getLogger(DedicatedServer.class);
    private static final int DEFAULT_PORT = 25565;
    private static final int MAX_PLAYERS = 4;
    private static final long FRAME_TIME_NS = 16_666_667; // ~60 FPS

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private volatile boolean gameStarted = false;

    // Connected clients
    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();
    private final Map<Integer, PlayerInput> playerInputs = new ConcurrentHashMap<>();

    // Game state (headless) - synchronized access required
    private ServerGameState gameState;
    private final Object gameStateLock = new Object();
    private boolean gameOverLogged = false;
    private boolean victoryLogged = false;

    public DedicatedServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            running = true;

            // Configure logging for server mode
            GameLogger.configureForServer();

            LOG.info("========================================");
            LOG.info("  VibeTanks Dedicated Server");
            LOG.info("========================================");
            LOG.info("Server started on port {}", port);
            LOG.info("Game settings:");
            LOG.info("  - Player speed: {}%", GameSettings.getPlayerSpeedMultiplier() * 100);
            LOG.info("  - Enemy speed: {}%", GameSettings.getEnemySpeedMultiplier() * 100);
            LOG.info("  - Player shoot speed: {}%", GameSettings.getPlayerShootSpeedMultiplier() * 100);
            LOG.info("  - Enemy shoot speed: {}%", GameSettings.getEnemyShootSpeedMultiplier() * 100);
            LOG.info("Waiting for players to connect...");
            LOG.info("Game will start when first player connects.");
            LOG.info("----------------------------------------");

            // Start accept thread
            Thread acceptThread = new Thread(this::acceptClients, "AcceptThread");
            acceptThread.start();

            // Main game loop
            runGameLoop();

        } catch (IOException e) {
            LOG.error("Failed to start server: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void acceptClients() {
        while (running) {
            try {
                if (clients.size() >= MAX_PLAYERS) {
                    Thread.sleep(1000);
                    continue;
                }

                Socket clientSocket = serverSocket.accept();
                int playerNum = clients.size() + 1;

                LOG.info("Player {} connected from {}", playerNum, clientSocket.getInetAddress().getHostAddress());

                // Create output stream first
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                // Create input stream
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                // Send player number
                out.writeObject(Integer.valueOf(playerNum));
                out.flush();

                ClientConnection client = new ClientConnection(clientSocket, playerNum, in, out);
                clients.add(client);

                // Start receiving inputs from this client
                Thread receiveThread = new Thread(() -> receiveFromClient(client),
                    "ReceiveThread-P" + playerNum);
                receiveThread.setDaemon(true);
                receiveThread.start();

                // Start game on first connection
                if (!gameStarted && clients.size() >= 1) {
                    startGame();
                }

                // If game already running, add player to game
                if (gameStarted) {
                    synchronized (gameStateLock) {
                        if (gameState != null) {
                            gameState.addPlayer(playerNum);
                            LOG.info("Player {} joined the game in progress", playerNum);
                        }
                    }
                }

                LOG.info("Total players: {}", clients.size());

            } catch (IOException e) {
                if (running) {
                    LOG.error("Error accepting connection: {}", e.getMessage());
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void receiveFromClient(ClientConnection client) {
        try {
            while (running && client.isActive()) {
                Object obj = client.in.readObject();
                if (obj instanceof PlayerInput) {
                    playerInputs.put(client.playerNumber, (PlayerInput) obj);
                }
            }
        } catch (Exception e) {
            if (running && client.isActive()) {
                LOG.info("Player {} disconnected: {}", client.playerNumber, e.getMessage());
                client.setActive(false);
            }
        }
    }

    private void startGame() {
        LOG.info("========================================");
        LOG.info("  GAME STARTING!");
        LOG.info("========================================");

        gameStarted = true;
        gameState = new ServerGameState(clients.size());
    }

    private void runGameLoop() {
        long lastFrameTime = System.nanoTime();
        int frameCount = 0;
        long lastFpsTime = System.currentTimeMillis();

        LOG.info("Game loop started, waiting for connections...");

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastFrameTime;

            // Only process frame if enough time has passed (~60 FPS)
            if (elapsed >= FRAME_TIME_NS) {
                lastFrameTime = now;

                if (gameStarted) {
                    // Collect inputs atomically outside the lock to reduce lock contention
                    Map<Integer, PlayerInput> frameInputs = new HashMap<>();
                    for (int i = 1; i <= MAX_PLAYERS; i++) {
                        PlayerInput input = playerInputs.remove(i);
                        if (input != null) {
                            frameInputs.put(i, input);
                        }
                    }

                    // Build state inside lock, broadcast outside
                    GameState stateToSend = null;
                    synchronized (gameStateLock) {
                        if (gameState != null) {
                            // Update connected player count
                            gameState.setConnectedPlayers(getActiveClientCount());

                            // Process collected player inputs
                            for (Map.Entry<Integer, PlayerInput> entry : frameInputs.entrySet()) {
                                gameState.processInput(entry.getKey(), entry.getValue());
                            }

                            // Update game state
                            gameState.update();

                            // Build network state (immutable snapshot)
                            stateToSend = gameState.buildNetworkState();

                            // Handle game over / victory using already-collected inputs
                            if (gameState.isGameOver()) {
                                if (!gameOverLogged) {
                                    LOG.warn("GAME OVER - Press ENTER to restart");
                                    gameOverLogged = true;
                                }
                                // Check for restart requests from collected inputs
                                for (PlayerInput input : frameInputs.values()) {
                                    if (input != null && input.requestRestart) {
                                        LOG.info("Restarting game by player request");
                                        gameState.restartLevel();
                                        gameOverLogged = false;
                                        victoryLogged = false;
                                        break;
                                    }
                                }
                            } else if (gameState.isVictory()) {
                                if (!victoryLogged) {
                                    LOG.info("VICTORY - Level {} complete", gameState.getCurrentLevel());
                                    victoryLogged = true;
                                }
                                // Check for next level requests from collected inputs
                                for (PlayerInput input : frameInputs.values()) {
                                    if (input != null && input.requestNextLevel) {
                                        LOG.info("Starting next level by player request");
                                        gameState.nextLevel();
                                        gameOverLogged = false;
                                        victoryLogged = false;
                                        break;
                                    }
                                }
                            }

                            frameCount++;
                        }
                    }

                    // Broadcast state OUTSIDE the lock to prevent blocking game loop
                    if (stateToSend != null) {
                        broadcastState(stateToSend);
                    }
                }

                // Print status every 5 seconds
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFpsTime >= 5000) {
                    if (gameStarted) {
                        synchronized (gameStateLock) {
                            if (gameState != null) {
                                double fps = frameCount / 5.0;
                                LOG.info("Server FPS: {} | Players: {}/{} | Level: {} | Enemies: {}",
                                    String.format("%.1f", fps), getActiveClientCount(), MAX_PLAYERS,
                                    gameState.getCurrentLevel(), gameState.getRemainingEnemies());
                            }
                        }
                    } else {
                        LOG.info("Waiting for players... ({} connected)", getActiveClientCount());
                    }
                    frameCount = 0;
                    lastFpsTime = currentTime;
                }

                // Clean up disconnected clients
                int beforeCount = clients.size();
                clients.removeIf(c -> !c.isActive());
                int afterCount = clients.size();

                // If all players disconnected, reset to waiting state
                if (beforeCount > 0 && afterCount == 0 && gameStarted) {
                    LOG.info("All players disconnected - resetting to waiting state");
                    synchronized (gameStateLock) {
                        gameStarted = false;
                        gameState = null;
                        gameOverLogged = false;
                        victoryLogged = false;
                    }
                }
            } else {
                // Sleep for approximately the remaining time until next frame
                long remainingNs = FRAME_TIME_NS - elapsed;
                long sleepMs = remainingNs / 1_000_000;
                try {
                    if (sleepMs > 1) {
                        Thread.sleep(sleepMs - 1); // Sleep slightly less to avoid overshooting
                    } else {
                        Thread.yield(); // Give other threads a chance
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private void broadcastState(GameState state) {
        for (ClientConnection client : clients) {
            if (client.isActive()) {
                try {
                    client.out.writeObject(state);
                    client.out.flush();
                    client.out.reset();
                } catch (IOException e) {
                    LOG.warn("Failed to send to Player {}", client.playerNumber);
                    client.setActive(false);
                }
            }
        }
    }

    private int getActiveClientCount() {
        int count = 0;
        for (ClientConnection c : clients) {
            if (c.isActive()) count++;
        }
        return count;
    }

    public void stop() {
        running = false;
        LOG.info("Shutting down server...");

        for (ClientConnection client : clients) {
            client.close();
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOG.error("Error closing server socket: {}", e.getMessage());
        }

        LOG.info("Server stopped.");
    }

    // Client connection wrapper
    private static class ClientConnection {
        final Socket socket;
        final int playerNumber;
        final ObjectInputStream in;
        final ObjectOutputStream out;
        private volatile boolean active = true;

        ClientConnection(Socket socket, int playerNumber, ObjectInputStream in, ObjectOutputStream out) {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.in = in;
            this.out = out;
        }

        boolean isActive() { return active; }
        void setActive(boolean active) { this.active = active; }

        void close() {
            active = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                GameLogger.getLogger(DedicatedServer.class).error("Invalid port number: {}", args[0]);
                GameLogger.getLogger(DedicatedServer.class).error("Usage: java -cp <classpath> com.vibetanks.server.DedicatedServer [port]");
                System.exit(1);
            }
        }

        DedicatedServer server = new DedicatedServer(port);

        // Handle shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
        }));

        server.start();
    }
}
