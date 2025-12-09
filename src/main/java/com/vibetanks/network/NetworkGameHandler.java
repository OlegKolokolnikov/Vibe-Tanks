package com.vibetanks.network;

import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.util.GameLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles network game update logic for both client and host.
 * Extracted from Game.java to reduce complexity.
 */
public class NetworkGameHandler {
    private static final GameLogger LOG = GameLogger.getLogger(NetworkGameHandler.class);


    /**
     * Result of handling client update - indicates if main update should continue.
     */
    public static class ClientUpdateResult {
        public final boolean skipMainUpdate;

        public ClientUpdateResult(boolean skipMainUpdate) {
            this.skipMainUpdate = skipMainUpdate;
        }
    }

    /**
     * Result of handling host update - indicates if main update should continue.
     */
    public static class HostUpdateResult {
        public final boolean skipMainUpdate;

        public HostUpdateResult(boolean skipMainUpdate) {
            this.skipMainUpdate = skipMainUpdate;
        }
    }

    /**
     * Context interface for client network operations.
     */
    public interface ClientContext {
        NetworkManager getNetwork();
        GameStateApplier.GameContext getGameStateContext();
        boolean isGameOver();
        boolean isVictory();
        boolean isPaused();
        List<Tank> getPlayerTanks();
        List<Tank> getEnemyTanks();
        List<Bullet> getBullets();
        List<Laser> getLasers();
        GameMap getGameMap();
        Base getBase();
        PowerUpEffectManager getPowerUpEffectManager();
        SoundManager getSoundManager();
        boolean[] getPlayerPaused();
        boolean isFirstStateReceived();
        int getRespawnSyncFrames();
        void setRespawnSyncFrames(int frames);
        boolean isEnterPressed();
        PlayerInput capturePlayerInput();
    }

    /**
     * Context interface for host network operations.
     */
    public interface HostContext {
        NetworkManager getNetwork();
        List<Tank> getPlayerTanks();
        List<Laser> getLasers();
        List<Bullet> getBullets();
        SoundManager getSoundManager();
        String[] getPlayerNicknames();
        double[][] getPlayerStartPositions();
        void setPlayerStartPositions(double[][] positions);
        boolean isGameOver();
        boolean isVictory();
        boolean isPaused();
        void startNextLevel();
        void restartCurrentLevel();
        void tryTakeLifeFromTeammate(int playerIndex);
        GameState buildGameState();
        long getLastNetworkUpdate();
        void setLastNetworkUpdate(long time);
        long getNetworkUpdateInterval();
    }

    /**
     * Handle client-side network update.
     * @return result indicating whether to skip the main update loop
     */
    public static ClientUpdateResult handleClientUpdate(ClientContext ctx) {
        NetworkManager network = ctx.getNetwork();

        // Always receive and apply game state from host
        GameState state = network.getLatestGameState();
        if (state != null) {
            GameStateApplier.apply(state, ctx.getGameStateContext());
        }

        // Handle game over/victory - send restart/next level requests
        if (ctx.isGameOver() || ctx.isVictory()) {
            if (ctx.isEnterPressed()) {
                PlayerInput input = new PlayerInput();
                input.requestNextLevel = ctx.isVictory();
                input.requestRestart = ctx.isGameOver();
                input.nickname = NicknameManager.getNickname();
                network.sendInput(input);
            }
            return new ClientUpdateResult(true);
        }

        if (ctx.isPaused()) {
            return new ClientUpdateResult(true);
        }

        // Move locally and send position to host
        int myPlayerIndex = network.getPlayerNumber() - 1;
        List<Tank> playerTanks = ctx.getPlayerTanks();
        boolean[] playerPaused = ctx.getPlayerPaused();

        if (myPlayerIndex >= 0 && myPlayerIndex < playerTanks.size()) {
            Tank myTank = playerTanks.get(myPlayerIndex);

            // Capture input from InputHandler
            PlayerInput input = ctx.capturePlayerInput();

            // Apply movement locally (skip if paused or dead)
            if (myTank.isAlive() && !ctx.getPowerUpEffectManager().arePlayersFrozen() && !playerPaused[myPlayerIndex]) {
                List<Tank> allTanks = new ArrayList<>();
                allTanks.addAll(playerTanks);
                allTanks.addAll(ctx.getEnemyTanks());

                if (input.up) {
                    myTank.move(Direction.UP, ctx.getGameMap(), allTanks, ctx.getBase());
                } else if (input.down) {
                    myTank.move(Direction.DOWN, ctx.getGameMap(), allTanks, ctx.getBase());
                } else if (input.left) {
                    myTank.move(Direction.LEFT, ctx.getGameMap(), allTanks, ctx.getBase());
                } else if (input.right) {
                    myTank.move(Direction.RIGHT, ctx.getGameMap(), allTanks, ctx.getBase());
                }
            }

            // Shoot locally for sound (skip if paused)
            if (myTank.isAlive() && input.shoot && !playerPaused[myPlayerIndex]) {
                if (myTank.hasLaser()) {
                    Laser laser = myTank.shootLaser(ctx.getSoundManager());
                    if (laser != null) {
                        ctx.getLasers().add(laser);
                    }
                } else {
                    myTank.shoot(ctx.getBullets(), ctx.getSoundManager());
                }
            }

            // Send position and nickname to host
            int respawnSyncFrames = ctx.getRespawnSyncFrames();
            if (respawnSyncFrames > 0) {
                ctx.setRespawnSyncFrames(respawnSyncFrames - 1);
            }

            if (myTank.isAlive() && ctx.isFirstStateReceived() && ctx.getRespawnSyncFrames() == 0) {
                input.posX = myTank.getX();
                input.posY = myTank.getY();
                input.direction = myTank.getDirection().ordinal();
            } else {
                // When dead, not yet synced, or just respawned - send invalid position
                input.posX = -1;
                input.posY = -1;
                input.direction = 0;
            }
            input.nickname = NicknameManager.getNickname();
            network.sendInput(input);
        }

        return new ClientUpdateResult(true); // Client always skips main game logic
    }

    /**
     * Handle host-side network update (sending state and receiving client inputs).
     * @return result indicating whether to skip the main update loop
     */
    public static HostUpdateResult handleHostUpdate(HostContext ctx) {
        NetworkManager network = ctx.getNetwork();

        // Send game state to clients
        long now = System.nanoTime();
        if (now - ctx.getLastNetworkUpdate() >= ctx.getNetworkUpdateInterval()) {
            GameState state = ctx.buildGameState();
            network.sendGameState(state);
            ctx.setLastNetworkUpdate(now);
        }

        // Check for client requests for next level/restart during game over/victory
        if (ctx.isGameOver() || ctx.isVictory()) {
            List<Tank> playerTanks = ctx.getPlayerTanks();
            for (int i = 2; i <= Math.min(playerTanks.size(), network.getConnectedPlayerCount()); i++) {
                PlayerInput clientInput = network.getPlayerInput(i);
                if (clientInput != null) {
                    if (clientInput.requestNextLevel && ctx.isVictory()) {
                        LOG.info("HOST: Client {} requested next level", i);
                        ctx.startNextLevel();
                        return new HostUpdateResult(true);
                    }
                    if (clientInput.requestRestart && ctx.isGameOver()) {
                        LOG.info("HOST: Client {} requested restart", i);
                        ctx.restartCurrentLevel();
                        return new HostUpdateResult(true);
                    }
                }
            }
        }

        return new HostUpdateResult(false);
    }

    /**
     * Handle adding new player tanks when players connect (host only).
     */
    public static void handleNewPlayerConnections(HostContext ctx) {
        NetworkManager network = ctx.getNetwork();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        double[][] playerStartPositions = ctx.getPlayerStartPositions();

        int connectedCount = network.getConnectedPlayerCount();
        while (playerTanks.size() < connectedCount && playerTanks.size() < 4) {
            int playerNum = playerTanks.size() + 1;
            double x, y;
            switch (playerNum) {
                case 2 -> { x = 16 * 32; y = 24 * 32; }
                case 3 -> { x = 9 * 32; y = 24 * 32; }
                case 4 -> { x = 15 * 32; y = 24 * 32; }
                default -> { x = 8 * 32; y = 24 * 32; }
            }
            LOG.info("HOST: Adding Player {} tank (new player connected)", playerNum);
            Tank newPlayer = new Tank(x, y, Direction.UP, true, playerNum);
            newPlayer.giveTemporaryShield();
            playerTanks.add(newPlayer);

            // Update playerStartPositions array
            double[][] newStartPositions = new double[playerTanks.size()][2];
            for (int j = 0; j < playerStartPositions.length; j++) {
                newStartPositions[j] = playerStartPositions[j];
            }
            newStartPositions[playerNum - 1] = new double[]{x, y};
            ctx.setPlayerStartPositions(newStartPositions);
            playerStartPositions = newStartPositions;
            LOG.info("HOST: Updated playerStartPositions for Player {} at {}, {} (array size: {})",
                playerNum, x, y, playerStartPositions.length);
        }
    }

    /**
     * Receive and apply client inputs on host.
     */
    public static void receiveClientInputs(HostContext ctx) {
        NetworkManager network = ctx.getNetwork();
        List<Tank> playerTanks = ctx.getPlayerTanks();
        List<Bullet> bullets = ctx.getBullets();
        List<Laser> lasers = ctx.getLasers();
        SoundManager soundManager = ctx.getSoundManager();
        String[] playerNicknames = ctx.getPlayerNicknames();

        for (int i = 2; i <= playerTanks.size(); i++) {
            PlayerInput clientInput = network.getPlayerInput(i);
            if (clientInput != null) {
                Tank clientTank = playerTanks.get(i - 1);

                // Accept client's position directly (only if valid)
                if (clientTank.isAlive() && clientInput.posX >= 0 && clientInput.posY >= 0) {
                    clientTank.setPosition(clientInput.posX, clientInput.posY);
                    clientTank.setDirection(Direction.values()[clientInput.direction]);
                }

                // Handle shooting on host (for bullet sync)
                if (clientInput.shoot && clientTank.isAlive()) {
                    if (clientTank.hasLaser()) {
                        Laser laser = clientTank.shootLaser(soundManager);
                        if (laser != null) {
                            lasers.add(laser);
                        }
                    } else {
                        clientTank.shoot(bullets, soundManager);
                    }
                }

                // Check if client is requesting a life transfer
                if (clientInput.requestLife) {
                    ctx.tryTakeLifeFromTeammate(i - 1);
                }

                // Update client's nickname
                if (clientInput.nickname != null) {
                    playerNicknames[i - 1] = clientInput.nickname;
                }
            }
        }
    }
}
