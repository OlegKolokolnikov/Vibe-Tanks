package com.battlecity;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkManager {
    private static final int PORT = 25565;
    private static final int MAX_PLAYERS = 4; // 1 host + 3 clients

    private ServerSocket serverSocket;
    private boolean isHost;
    private boolean connected = false;
    private int playerNumber = 1; // Which player this instance controls

    // For host: manage multiple clients
    private List<ClientHandler> clients = new ArrayList<>();
    private Map<Integer, PlayerInput> playerInputs = new ConcurrentHashMap<>();

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
        private boolean active = true;

        public ClientHandler(Socket socket, int playerNumber) throws IOException {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
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
                    System.err.println("Lost connection to Player " + playerNumber + ": " + e.getMessage());
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
                System.err.println("Error sending to Player " + playerNumber + ": " + e.getMessage());
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
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }

    // Host mode - start server and accept up to 3 connections
    public boolean startHost() {
        isHost = true;
        playerNumber = 1; // Host is always Player 1

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Waiting for players to connect on port " + PORT + "...");

            // Accept connections in background
            new Thread(() -> {
                try {
                    // Accept up to 3 clients (Player 2, 3, 4)
                    for (int i = 2; i <= MAX_PLAYERS; i++) {
                        if (!connected) break; // Stop accepting if host disconnected

                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Player " + i + " connected from: " + clientSocket.getInetAddress());

                        ClientHandler client = new ClientHandler(clientSocket, i);
                        clients.add(client);

                        // First connection establishes game as ready
                        if (!connected) {
                            connected = true;
                        }
                    }
                    System.out.println("Maximum players reached (4/4)");
                } catch (IOException e) {
                    if (connected) {
                        System.err.println("Error accepting connections: " + e.getMessage());
                    }
                }
            }).start();

            return true;
        } catch (IOException e) {
            System.err.println("Failed to start host: " + e.getMessage());
            return false;
        }
    }

    // Client mode - connect to host
    public boolean joinHost(String hostIP) {
        isHost = false;

        try {
            System.out.println("Connecting to " + hostIP + ":" + PORT + "...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostIP, PORT), 5000); // 5 second timeout
            System.out.println("Connected to host!");

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;

            // Start receiving game states
            receiveThread = new Thread(() -> {
                try {
                    // First message from host tells us our player number
                    Object firstObj = in.readObject();
                    if (firstObj instanceof GameState) {
                        GameState state = (GameState) firstObj;
                        // Determine player number from state (simplified - would need better logic)
                        receivedStates.offer(state);
                    }

                    while (connected && !Thread.interrupted()) {
                        Object obj = in.readObject();
                        if (obj instanceof GameState) {
                            receivedStates.offer((GameState) obj);
                        }
                    }
                } catch (Exception e) {
                    if (connected) {
                        System.err.println("Connection lost: " + e.getMessage());
                        connected = false;
                    }
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();

            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
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
            System.err.println("Error sending input: " + e.getMessage());
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
        connected = false;

        try {
            // Close server socket FIRST to unblock accept() calls
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

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
            System.err.println("Error closing connection: " + e.getMessage());
        }
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
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String ip = in.readLine();
            in.close();
            return ip;
        } catch (Exception e) {
            return "Unknown (check internet connection)";
        }
    }
}
