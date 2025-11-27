package com.battlecity;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkManager {
    private static final int PORT = 25565;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerSocket serverSocket;
    private boolean isHost;
    private boolean connected = false;

    private BlockingQueue<GameState> receivedStates = new LinkedBlockingQueue<>();
    private BlockingQueue<PlayerInput> receivedInputs = new LinkedBlockingQueue<>();

    private Thread receiveThread;

    // Host mode - start server and wait for connection
    public boolean startHost() {
        isHost = true;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Waiting for player 2 to connect on port " + PORT + "...");

            // Accept connection in background
            new Thread(() -> {
                try {
                    socket = serverSocket.accept();
                    System.out.println("Player 2 connected from: " + socket.getInetAddress());
                    setupStreams();
                    connected = true;
                    startReceiving();
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
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
            setupStreams();
            connected = true;
            startReceiving();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    private void startReceiving() {
        receiveThread = new Thread(() -> {
            try {
                while (connected && !Thread.interrupted()) {
                    Object obj = in.readObject();

                    if (obj instanceof GameState) {
                        receivedStates.offer((GameState) obj);
                    } else if (obj instanceof PlayerInput) {
                        receivedInputs.offer((PlayerInput) obj);
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
    }

    // Host sends game state to client
    public void sendGameState(GameState state) {
        if (!connected || !isHost) return;

        try {
            out.writeObject(state);
            out.flush();
            out.reset(); // Prevent memory leak from object caching
        } catch (IOException e) {
            System.err.println("Error sending game state: " + e.getMessage());
            connected = false;
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

    // Get player input (for host)
    public PlayerInput getPlayerInput() {
        return receivedInputs.poll();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isHost() {
        return isHost;
    }

    public void close() {
        connected = false;
        try {
            if (receiveThread != null) receiveThread.interrupt();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
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
}
