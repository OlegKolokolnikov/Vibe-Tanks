package com.vibetanks;

/**
 * Launcher class for the application.
 * This is needed because JavaFX applications cannot be launched directly
 * from a shaded JAR when the main class extends Application.
 *
 * Usage:
 *   java -jar VibeTanks.jar          - Start game client (normal mode)
 *   java -jar VibeTanks.jar --server - Start dedicated server
 *   java -jar VibeTanks.jar --server 12345 - Start server on custom port
 */
public class Launcher {
    public static void main(String[] args) {
        // Check for server mode
        if (args.length > 0 && args[0].equals("--server")) {
            int port = 25565; // Default port
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port: " + args[1]);
                    System.exit(1);
                }
            }
            DedicatedServer server = new DedicatedServer(port);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            server.start();
        } else {
            // Normal game mode
            Main.main(args);
        }
    }
}
