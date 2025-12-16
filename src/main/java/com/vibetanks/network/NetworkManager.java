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
    private final Map<Integer, PlayerInput> lastKnownInputs = new ConcurrentHashMap<>(); // Fallback for missed packets
    private final Map<Integer, Long> lastSequenceNumbers = new ConcurrentHashMap<>(); // Track sequence numbers
    private final Object inputLock = new Object(); // Lock for atomic input operations
    private Thread acceptThread; // Track accept thread

    // Input buffer settings
    private static final int INPUT_BUFFER_FRAMES = 3; // Use last input for up to 3 frames if no new input

    // For client: single connection to host
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BlockingQueue<GameState> receivedStates = new LinkedBlockingQueue<>();
    private Thread receiveThread;
    private volatile long lastHostHeartbeat = System.currentTimeMillis(); // Track last received state from host

    // Heartbeat timeout - mark client as disconnected if no input for 5 seconds
    private static final long HEARTBEAT_TIMEOUT_MS = 5000;

    // Client handler for host
    private class ClientHandler {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int playerNumber;
        private volatile boolean active = true;
        private volatile long lastHeartbeat = System.currentTimeMillis();

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
                    try {
                        Object obj = in.readObject();
                        if (obj instanceof PlayerInput) {
                            PlayerInput input = (PlayerInput) obj;

                            // Synchronized block for atomic input update
                            synchronized (inputLock) {
                                long lastSeq = lastSequenceNumbers.getOrDefault(playerNumber, -1L);

                                // Check for out-of-order packets (skip if older than current)
                                if (input.sequenceNumber > lastSeq) {
                                    // Check for missed packets
                                    if (lastSeq >= 0 && input.sequenceNumber > lastSeq + 1) {
                                        LOG.debug("Player {} missed {} packets (seq {} -> {})",
                                            playerNumber, input.sequenceNumber - lastSeq - 1, lastSeq, input.sequenceNumber);
                                    }

                                    playerInputs.put(playerNumber, input);
                                    lastKnownInputs.put(playerNumber, input); // Store as fallback
                                    lastSequenceNumbers.put(playerNumber, input.sequenceNumber);
                                }
                            }

                            // Synchronized heartbeat update to avoid race with timeout check
                            synchronized (ClientHandler.this) {
                                lastHeartbeat = System.currentTimeMillis();
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        // Read timeout - continue loop if still active
                        // Timeout check will handle disconnection detection
                        continue;
                    }
                }
            } catch (Exception e) {
                if (active) {
                    LOG.warn("Lost connection to Player {}: {}", playerNumber, e.getMessage());
                    active = false;
                }
            }
        }

        /**
         * Check if client has timed out (no input for HEARTBEAT_TIMEOUT_MS)
         */
        public synchronized boolean isTimedOut() {
            return System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT_MS;
        }

        /**
         * Get ping/latency estimate in milliseconds
         */
        public synchronized long getPingMs() {
            return System.currentTimeMillis() - lastHeartbeat;
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
            // Clean up input maps to prevent memory leak
            playerInputs.remove(playerNumber);
            lastKnownInputs.remove(playerNumber);
            lastSequenceNumbers.remove(playerNumber);
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

    // Kill any process using the specified port (cross-platform)
    private void killProcessOnPort(int port) {
        String os = System.getProperty("os.name").toLowerCase();
        Process findProcess = null;
        Process killProcess = null;

        try {
            LOG.debug("Checking for process on port {} (OS: {})...", port, os);
            String pid = null;

            if (os.contains("win")) {
                // Windows: use netstat
                findProcess = Runtime.getRuntime().exec("netstat -ano");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(findProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(":" + port + " ") && line.contains("LISTENING")) {
                            String[] parts = line.trim().split("\\s+");
                            pid = parts[parts.length - 1];
                            break;
                        }
                    }
                }
                findProcess.waitFor();

                if (pid != null && !pid.isEmpty()) {
                    LOG.info("Found process {} using port {}, killing it...", pid, port);
                    killProcess = Runtime.getRuntime().exec(
                        "powershell -Command \"Stop-Process -Id " + pid + " -Force\""
                    );
                    killProcess.waitFor();
                }
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                // macOS/Linux: use lsof
                findProcess = Runtime.getRuntime().exec(new String[]{"lsof", "-t", "-i:" + port});
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(findProcess.getInputStream()))) {
                    pid = reader.readLine();
                }
                findProcess.waitFor();

                if (pid != null && !pid.trim().isEmpty()) {
                    pid = pid.trim();
                    LOG.info("Found process {} using port {}, killing it...", pid, port);
                    killProcess = Runtime.getRuntime().exec(new String[]{"kill", "-9", pid});
                    killProcess.waitFor();
                }
            } else {
                LOG.warn("Unknown OS '{}', cannot check port usage", os);
                return;
            }

            if (pid != null && !pid.isEmpty()) {
                LOG.debug("Process killed, waiting for port release...");
                Thread.sleep(1000);
                LOG.debug("Port should now be available");
            } else {
                LOG.debug("No process found using port {}", port);
            }
        } catch (Exception e) {
            LOG.error("Error checking/killing process on port: {}", e.getMessage());
        } finally {
            // Ensure processes are destroyed to prevent zombie processes
            if (findProcess != null) {
                findProcess.destroyForcibly();
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
            serverSocket.setSoTimeout(1000); // 1 second accept timeout to allow clean shutdown
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

                        try {
                            Socket clientSocket = serverSocket.accept();
                            // Set read timeout on client socket to prevent blocking forever
                            clientSocket.setSoTimeout(10000); // 10 second read timeout

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
                        } catch (SocketTimeoutException e) {
                            // Accept timeout - just continue loop to check if still hosting
                            continue;
                        }
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
            socket.connect(new InetSocketAddress(hostIP, PORT), 5000); // 5 second connection timeout
            socket.setSoTimeout(10000); // 10 second read timeout to prevent blocking forever
            LOG.info("Connected to host!");

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;
            lastHostHeartbeat = System.currentTimeMillis(); // Reset heartbeat on actual connection

            // Start receiving game states
            receiveThread = new Thread(() -> {
                try {
                    // First message from host tells us our player number
                    Object firstObj = in.readObject();
                    lastHostHeartbeat = System.currentTimeMillis(); // Reset on first message
                    if (firstObj instanceof Integer) {
                        playerNumber = (Integer) firstObj;
                        LOG.info("Received player number from host: {}", playerNumber);
                    } else if (firstObj instanceof GameState) {
                        // Fallback for backwards compatibility
                        receivedStates.offer((GameState) firstObj);
                    }

                    while (connected && !Thread.interrupted()) {
                        try {
                            Object obj = in.readObject();
                            if (obj instanceof GameState) {
                                receivedStates.offer((GameState) obj);
                                lastHostHeartbeat = System.currentTimeMillis(); // Update heartbeat
                            }
                        } catch (SocketTimeoutException e) {
                            // Read timeout - check if connection still valid
                            if (System.currentTimeMillis() - lastHostHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                                LOG.warn("Host heartbeat timeout - connection may be lost");
                            }
                            // Continue loop to try again
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
        GameState current;
        // Drain queue atomically using poll() which returns null when empty
        while ((current = receivedStates.poll()) != null) {
            latest = current;
        }
        return latest;
    }

    // Get player input for specific player (for host)
    // Returns new input if available, otherwise returns last known input (for buffering)
    public PlayerInput getPlayerInput(int playerNum) {
        synchronized (inputLock) {
            PlayerInput newInput = playerInputs.remove(playerNum);
            if (newInput != null) {
                return newInput;
            }

            // No new input - use last known input as fallback (input buffering)
            // This keeps the player moving in their last direction during brief lag spikes
            PlayerInput lastKnown = lastKnownInputs.get(playerNum);
            if (lastKnown != null) {
                // Clear shoot flag on buffered input to prevent repeated shots
                // Movement continues but shooting requires fresh input
                PlayerInput buffered = new PlayerInput();
                buffered.up = lastKnown.up;
                buffered.down = lastKnown.down;
                buffered.left = lastKnown.left;
                buffered.right = lastKnown.right;
                buffered.shoot = false; // Don't repeat shooting
                buffered.posX = lastKnown.posX;
                buffered.posY = lastKnown.posY;
                buffered.direction = lastKnown.direction;
                buffered.nickname = lastKnown.nickname;
                buffered.requestLife = false; // Don't repeat requests
                buffered.requestNextLevel = false;
                buffered.requestRestart = false;
                return buffered;
            }

            return null;
        }
    }

    /**
     * Check if there's a fresh (non-buffered) input available for a player.
     */
    public boolean hasFreshInput(int playerNum) {
        return playerInputs.containsKey(playerNum);
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
        // For clients, also check heartbeat timeout
        if (!isHost && connected) {
            if (System.currentTimeMillis() - lastHostHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                LOG.warn("Host heartbeat timeout - connection lost");
                connected = false;
            }
        }
        return connected;
    }

    /**
     * Get time since last host update in milliseconds (for clients).
     * Returns 0 for host.
     */
    public long getHostPing() {
        if (isHost) return 0;
        return System.currentTimeMillis() - lastHostHeartbeat;
    }

    public boolean isHost() {
        return isHost;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    /**
     * Check if a specific player is still connected.
     * Host is always connected (player 1). Clients check their handler's active status.
     * Also checks heartbeat timeout for clients.
     */
    public boolean isPlayerConnected(int playerNum) {
        if (!isHost) return connected; // Client only knows its own status

        if (playerNum == 1) return true; // Host is always connected

        // Find client handler for this player
        for (ClientHandler client : clients) {
            if (client.playerNumber == playerNum) {
                // Synchronized to make timeout check and status modification atomic
                synchronized (client) {
                    if (client.active && client.isTimedOut()) {
                        LOG.warn("Player {} heartbeat timeout - marking as disconnected", playerNum);
                        client.active = false;
                    }
                    return client.active;
                }
            }
        }
        return false; // Player not found = not connected
    }

    /**
     * Get connection status array for all 4 player slots.
     * Index 0 = Player 1, etc.
     * Also checks heartbeat timeouts.
     */
    public boolean[] getPlayerConnectionStatus() {
        boolean[] status = new boolean[4];

        if (isHost) {
            status[0] = true; // Host (Player 1) always connected
            for (ClientHandler client : clients) {
                if (client.playerNumber >= 2 && client.playerNumber <= 4) {
                    // Synchronized to make timeout check and status modification atomic
                    synchronized (client) {
                        if (client.active && client.isTimedOut()) {
                            LOG.warn("Player {} heartbeat timeout - marking as disconnected", client.playerNumber);
                            client.active = false;
                        }
                        status[client.playerNumber - 1] = client.active;
                    }
                }
            }
        } else {
            // Client doesn't know other players' status, will be updated from GameState
            status[playerNumber - 1] = connected;
        }

        return status;
    }

    /**
     * Get ping estimate for a player in milliseconds.
     * Returns -1 if player not found or not host.
     */
    public long getPlayerPing(int playerNum) {
        if (!isHost || playerNum == 1) return 0; // Host has 0 ping

        for (ClientHandler client : clients) {
            if (client.playerNumber == playerNum && client.active) {
                return client.getPingMs();
            }
        }
        return -1;
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
