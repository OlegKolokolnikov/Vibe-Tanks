package com.vibetanks.network;

import com.vibetanks.util.GameLogger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkManager {
    private static final GameLogger LOG = GameLogger.getLogger(NetworkManager.class);
    private static final int PORT = 25565;
    private static final int MAX_PLAYERS = 4; // 1 host + 3 clients

    private ServerSocket serverSocket;
    private volatile boolean isHost;
    private volatile boolean connected = false;
    private volatile boolean isHosting = false; // Track if currently hosting
    private volatile int playerNumber = 1; // Which player this instance controls

    // For host: manage multiple clients (thread-safe list)
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<Integer, PlayerInput> playerInputs = new ConcurrentHashMap<>();
    private Thread acceptThread; // Track accept thread

    // For client: single connection to host
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BlockingQueue<GameState> receivedStates = new LinkedBlockingQueue<>();
    private Thread receiveThread;

    // Client handler for host
    private class ClientHandler {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int playerNumber;
        private volatile boolean active = true;

        public ClientHandler(Socket socket, int playerNumber, ObjectOutputStream out) throws IOException {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.out = out; // Use pre-created stream
            this.in = new ObjectInputStream(socket.getInputStream());

            // Start receiving inputs from this client
            new Thread(() -> receiveFromClient()).start();
        }

        private void receiveFromClient() {
            try {
                while (active && !Thread.interrupted()) {
                    Object obj = in.readObject();
                    if (obj instanceof PlayerInput) {
                        playerInputs.put(playerNumber, (PlayerInput) obj);
                    }
                }
            } catch (Exception e) {
                if (active) {
                    LOG.warn("Lost connection to Player {}: {}", playerNumber, e.getMessage());
                    active = false;
                }
            }
        }

        public void sendState(GameState state) {
            if (!active) return;
            try {
                out.writeObject(state);
                out.flush();
                out.reset();
            } catch (IOException e) {
                LOG.warn("Error sending to Player {}: {}", playerNumber, e.getMessage());
                active = false;
            }
        }

        public void close() {
            active = false;
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                LOG.warn("Error closing client: {}", e.getMessage());
            }
        }
    }

    // Check if port is available
    private boolean isPortAvailable(int port) {
        try (ServerSocket testSocket = new ServerSocket()) {
            testSocket.setReuseAddress(true);
            testSocket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Static method to clean up port at application startup
    public static void cleanupPortOnStartup() {
        NetworkManager temp = new NetworkManager();
        temp.killProcessOnPort(PORT);
    }

    // Kill any process using the specified port
    private void killProcessOnPort(int port) {
        Process netstatProcess = null;
        Process killProcess = null;
        try {
            LOG.debug("Checking for process on port {}...", port);

            // Find process using the port
            netstatProcess = Runtime.getRuntime().exec("netstat -ano");
            String pid = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(netstatProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(":" + port + " ") && line.contains("LISTENING")) {
                        // Extract PID from the end of the line
                        String[] parts = line.trim().split("\\s+");
                        pid = parts[parts.length - 1];
                        break;
                    }
                }
            }
            netstatProcess.waitFor();

            if (pid != null && !pid.isEmpty()) {
                LOG.info("Found process {} using port {}, killing it...", pid, port);

                // Kill the process using PowerShell
                killProcess = Runtime.getRuntime().exec(
                    "powershell -Command \"Stop-Process -Id " + pid + " -Force\""
                );
                killProcess.waitFor();

                LOG.debug("Process killed, waiting for port release...");
                Thread.sleep(1000); // Wait for port to be released
                LOG.debug("Port should now be available");
            } else {
                LOG.debug("No process found using port {}", port);
            }
        } catch (Exception e) {
            LOG.error("Error checking/killing process on port: {}", e.getMessage());
        } finally {
            // Ensure processes are destroyed to prevent zombie processes
            if (netstatProcess != null) {
                netstatProcess.destroyForcibly();
            }
            if (killProcess != null) {
                killProcess.destroyForcibly();
            }
        }
    }

    // Host mode - start server and accept up to 3 connections
    public boolean startHost() {
        // Prevent starting if already hosting
        if (isHosting) {
            LOG.debug("Already hosting - skipping");
            return false;
        }

        // Debug: Check if port is available
        LOG.info("Attempting to start host on port {}", PORT);

        // Kill any process using the port
        if (!isPortAvailable(PORT)) {
            LOG.info("Port {} is in use, attempting to free it...", PORT);
            killProcessOnPort(PORT);
        }

        // Wait for port to become available (up to 3 seconds)
        int attempts = 0;
        while (!isPortAvailable(PORT) && attempts < 6) {
            LOG.debug("Port {} not available yet, waiting... (attempt {}/6)", PORT, attempts + 1);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for port");
                return false;
            }
            attempts++;
        }

        if (!isPortAvailable(PORT)) {
            LOG.error("Port {} is still not available after waiting", PORT);
            return false;
        }

        isHost = true;
        isHosting = true;
        playerNumber = 1; // Host is always Player 1

        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true); // Allow immediate port reuse
            LOG.debug("Binding to port {}...", PORT);
            serverSocket.bind(new InetSocketAddress(PORT));
            LOG.info("Successfully bound to port {}", PORT);
            LOG.info("Waiting for players to connect on port {}...", PORT);

            // Accept connections in background - keeps running during gameplay
            acceptThread = new Thread(() -> {
                try {
                    // Keep accepting until max players or server closed
                    while (isHosting && serverSocket.isBound() && !serverSocket.isClosed()) {
                        // Check if we have room for more players
                        if (clients.size() >= MAX_PLAYERS - 1) {
                            LOG.info("Maximum players reached (4/4), stopping accept");
                            break;
                        }

                        Socket clientSocket = serverSocket.accept();
                        int playerNum = clients.size() + 2; // Player 2, 3, or 4
                        LOG.info("Player {} connected from: {}", playerNum, clientSocket.getInetAddress());

                        // Create ObjectOutputStream immediately so client can create its InputStream
                        ObjectOutputStream tempOut = new ObjectOutputStream(clientSocket.getOutputStream());
                        tempOut.flush();
                        LOG.debug("Server stream header sent to Player {}", playerNum);

                        ClientHandler client = new ClientHandler(clientSocket, playerNum, tempOut);
                        clients.add(client);

                        // Send player number to client so they know which player they are
                        tempOut.writeObject(Integer.valueOf(playerNum));
                        tempOut.flush();
                        LOG.debug("Sent player number {} to client", playerNum);

                        // First connection establishes game as ready
                        if (!connected) {
                            connected = true;
                        }

                        LOG.info("Player {} joined mid-game, total players: {}", playerNum, clients.size() + 1);
                    }
                } catch (IOException e) {
                    if (isHosting) {
                        LOG.debug("Accept loop ended: {}", e.getMessage());
                    }
                }
                LOG.debug("Accept thread exiting");
            });
            acceptThread.setDaemon(false); // Non-daemon so it properly cleans up
            acceptThread.start();

            return true;
        } catch (IOException e) {
            isHosting = false; // Reset flag on failure
            LOG.error("Failed to start host: {} - {}", e.getClass().getName(), e.getMessage());
            return false;
        }
    }

    // Client mode - connect to host
    public boolean joinHost(String hostIP) {
        isHost = false;

        try {
            LOG.info("Connecting to {}:{}...", hostIP, PORT);
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostIP, PORT), 5000); // 5 second timeout
            LOG.info("Connected to host!");

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;

            // Start receiving game states
            receiveThread = new Thread(() -> {
                try {
                    // First message from host tells us our player number
                    Object firstObj = in.readObject();
                    if (firstObj instanceof Integer) {
                        playerNumber = (Integer) firstObj;
                        LOG.info("Received player number from host: {}", playerNumber);
                    } else if (firstObj instanceof GameState) {
                        // Fallback for backwards compatibility
                        receivedStates.offer((GameState) firstObj);
                    }

                    while (connected && !Thread.interrupted()) {
                        Object obj = in.readObject();
                        if (obj instanceof GameState) {
                            receivedStates.offer((GameState) obj);
                        }
                    }
                } catch (Exception e) {
                    if (connected) {
                        LOG.warn("Connection lost: {}", e.getMessage());
                        connected = false;
                    }
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();

            return true;
        } catch (IOException e) {
            LOG.error("Failed to connect: {}", e.getMessage());
            return false;
        }
    }

    // Host sends game state to all clients
    public void sendGameState(GameState state) {
        if (!connected || !isHost) return;

        for (ClientHandler client : clients) {
            client.sendState(state);
        }
    }

    // Client sends input to host
    public void sendInput(PlayerInput input) {
        if (!connected || isHost) return;

        try {
            out.writeObject(input);
            out.flush();
        } catch (IOException e) {
            LOG.warn("Error sending input: {}", e.getMessage());
            connected = false;
        }
    }

    // Get latest game state (for client)
    public GameState getLatestGameState() {
        GameState latest = null;
        // Drain queue and get most recent
        while (!receivedStates.isEmpty()) {
            latest = receivedStates.poll();
        }
        return latest;
    }

    // Get player input for specific player (for host)
    public PlayerInput getPlayerInput(int playerNum) {
        return playerInputs.remove(playerNum);
    }

    // Get number of connected players (including host)
    public int getConnectedPlayerCount() {
        if (isHost) {
            return 1 + clients.size();
        } else {
            return connected ? 2 : 0; // Simplified - client doesn't know total count
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isHost() {
        return isHost;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void close() {
        LOG.debug("NetworkManager.close() called");
        connected = false;
        isHosting = false; // Reset hosting flag to stop accept loop

        try {
            // Close server socket FIRST to unblock accept() calls
            if (serverSocket != null && !serverSocket.isClosed()) {
                LOG.debug("Closing server socket...");
                serverSocket.close();
                LOG.debug("Server socket closed");
            }
        } catch (IOException e) {
            LOG.warn("Error closing server socket: {}", e.getMessage());
        }

        // Wait for accept thread to finish (it should exit when socket closes)
        if (acceptThread != null && acceptThread.isAlive()) {
            LOG.debug("Waiting for accept thread to exit...");
            try {
                acceptThread.join(2000); // Wait up to 2 seconds for thread to exit
                if (acceptThread.isAlive()) {
                    LOG.warn("Accept thread did not exit, interrupting...");
                    acceptThread.interrupt();
                    acceptThread.join(1000); // Wait another second after interrupt
                }
                LOG.debug("Accept thread terminated");
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for accept thread");
            }
        }

        // Null out the socket to ensure it's garbage collected
        serverSocket = null;
        acceptThread = null;

        if (isHost) {
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
        }

        try {
            if (receiveThread != null) receiveThread.interrupt();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            LOG.warn("Error closing connection: {}", e.getMessage());
        }

        LOG.debug("NetworkManager.close() complete");
    }

    public String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Unknown";
        }
    }

    public String getPublicIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String ip = reader.readLine();
                return ip != null ? ip : "Unknown";
            }
        } catch (Exception e) {
            return "Unknown (check internet connection)";
        }
    }
}
