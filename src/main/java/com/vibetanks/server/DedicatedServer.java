package com.vibetanks.server;

import com.vibetanks.core.GameSettings;
import com.vibetanks.network.GameState;
import com.vibetanks.network.PlayerInput;

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

    // Game state (headless)
    private ServerGameState gameState;
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

            System.out.println("========================================");
            System.out.println("  VibeTanks Dedicated Server");
            System.out.println("========================================");
            System.out.println("Server started on port " + port);
            System.out.println("Game settings:");
            System.out.println("  - Player speed: " + (GameSettings.getPlayerSpeedMultiplier() * 100) + "%");
            System.out.println("  - Enemy speed: " + (GameSettings.getEnemySpeedMultiplier() * 100) + "%");
            System.out.println("  - Player shoot speed: " + (GameSettings.getPlayerShootSpeedMultiplier() * 100) + "%");
            System.out.println("  - Enemy shoot speed: " + (GameSettings.getEnemyShootSpeedMultiplier() * 100) + "%");
            System.out.println("Waiting for players to connect...");
            System.out.println("Game will start when first player connects.");
            System.out.println("----------------------------------------");

            // Start accept thread
            Thread acceptThread = new Thread(this::acceptClients, "AcceptThread");
            acceptThread.start();

            // Main game loop
            runGameLoop();

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
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

                System.out.println("[+] Player " + playerNum + " connected from " +
                    clientSocket.getInetAddress().getHostAddress());

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
                if (gameStarted && gameState != null) {
                    gameState.addPlayer(playerNum);
                    System.out.println("[*] Player " + playerNum + " joined the game in progress");
                }

                System.out.println("[*] Total players: " + clients.size());

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
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
                System.out.println("[-] Player " + client.playerNumber + " disconnected: " + e.getMessage());
                client.setActive(false);
            }
        }
    }

    private void startGame() {
        System.out.println("\n========================================");
        System.out.println("  GAME STARTING!");
        System.out.println("========================================\n");

        gameStarted = true;
        gameState = new ServerGameState(clients.size());
    }

    private void runGameLoop() {
        long lastFrameTime = System.nanoTime();
        int frameCount = 0;
        long lastFpsTime = System.currentTimeMillis();

        System.out.println("[*] Game loop started, waiting for connections...");

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastFrameTime;

            // Only process frame if enough time has passed (~60 FPS)
            if (elapsed >= FRAME_TIME_NS) {
                lastFrameTime = now;

                if (gameStarted && gameState != null) {
                    // Update connected player count
                    gameState.setConnectedPlayers(getActiveClientCount());

                    // Process player inputs
                    for (int i = 1; i <= MAX_PLAYERS; i++) {
                        PlayerInput input = playerInputs.remove(i);
                        if (input != null) {
                            gameState.processInput(i, input);
                        }
                    }

                    // Update game state
                    gameState.update();

                    // Send state to all clients
                    GameState state = gameState.buildNetworkState();
                    broadcastState(state);

                    // Handle game over / victory
                    if (gameState.isGameOver()) {
                        if (!gameOverLogged) {
                            System.out.println("[!] GAME OVER - Press ENTER to restart");
                            gameOverLogged = true;
                        }
                        // Check for restart requests
                        for (int i = 1; i <= clients.size(); i++) {
                            PlayerInput input = playerInputs.get(i);
                            if (input != null && input.requestRestart) {
                                System.out.println("[*] Restarting game by request from Player " + i);
                                gameState.restartLevel();
                                gameOverLogged = false;
                                victoryLogged = false;
                                break;
                            }
                        }
                    }

                    if (gameState.isVictory()) {
                        if (!victoryLogged) {
                            System.out.println("[!] VICTORY - Level " + gameState.getCurrentLevel() + " complete");
                            victoryLogged = true;
                        }
                        // Check for next level requests
                        for (int i = 1; i <= clients.size(); i++) {
                            PlayerInput input = playerInputs.get(i);
                            if (input != null && input.requestNextLevel) {
                                System.out.println("[*] Starting next level by request from Player " + i);
                                gameState.nextLevel();
                                gameOverLogged = false;
                                victoryLogged = false;
                                break;
                            }
                        }
                    }

                    frameCount++;
                }

                // Print status every 5 seconds
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFpsTime >= 5000) {
                    if (gameStarted && gameState != null) {
                        double fps = frameCount / 5.0;
                        System.out.println("[*] Server FPS: " + String.format("%.1f", fps) +
                            " | Players: " + getActiveClientCount() + "/" + MAX_PLAYERS +
                            " | Level: " + gameState.getCurrentLevel() +
                            " | Enemies: " + gameState.getRemainingEnemies());
                    } else {
                        System.out.println("[*] Waiting for players... (" + getActiveClientCount() + " connected)");
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
                    System.out.println("[*] All players disconnected - resetting to waiting state");
                    gameStarted = false;
                    gameState = null;
                    gameOverLogged = false;
                    victoryLogged = false;
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
                    System.out.println("[-] Failed to send to Player " + client.playerNumber);
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
        System.out.println("\n[*] Shutting down server...");

        for (ClientConnection client : clients) {
            client.close();
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        System.out.println("[*] Server stopped.");
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
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Usage: java -cp <classpath> com.vibetanks.server.DedicatedServer [port]");
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
