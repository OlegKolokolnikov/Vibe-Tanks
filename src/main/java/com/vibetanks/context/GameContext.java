package com.vibetanks.context;

import com.vibetanks.animation.DancingCharacter;
import com.vibetanks.animation.DancingGirl;
import com.vibetanks.audio.SoundManager;
import com.vibetanks.core.*;
import com.vibetanks.network.GameState;
import com.vibetanks.network.NetworkManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.*;

/**
 * Shared game state container for all game components.
 * This class holds all the state that needs to be accessed by multiple subsystems
 * (rendering, logic, network, etc.) to eliminate the need for a monolithic Game class.
 */
public class GameContext {
    // Core rendering
    private final Pane root;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final int width;
    private final int height;
    private final Stage stage;

    // Game configuration
    private final int playerCount;
    private final int totalEnemies;

    // Core game entities
    private GameMap gameMap;
    private List<Tank> playerTanks = new ArrayList<>();
    private List<Tank> enemyTanks = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private EnemySpawner enemySpawner;
    private Base base;
    private double[][] playerStartPositions;

    // Audio
    private SoundManager soundManager;

    // Network
    private NetworkManager network;
    private boolean isNetworkGame = false;
    private int networkConnectedPlayers = 1;
    private List<GameState.TileChange> mapChanges = new ArrayList<>();

    // Game state flags
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean paused = false;
    private int pauseMenuSelection = 0;
    private boolean[] playerPaused = new boolean[4];

    // Score tracking (consolidated in PlayerStats)
    private PlayerStats playerStats = new PlayerStats();
    // Legacy arrays - delegate to playerStats for backwards compatibility during migration
    private int[] playerKills = playerStats.getKillsArray();
    private int[] playerScores = playerStats.getScoresArray();
    private int[] playerLevelScores = playerStats.getLevelScoresArray();
    private int[][] playerKillsByType = playerStats.getKillsByTypeMatrix();
    private boolean winnerBonusAwarded = false;

    // Power-up effect durations
    private int baseProtectionDuration = 0;
    private boolean isFlashing = false;
    private int flashCount = 0;
    private int flashTimer = 0;
    private int enemyFreezeDuration = 0;
    private int playerFreezeDuration = 0;
    private int enemyTeamSpeedBoostDuration = 0;
    private Tank enemyWithPermanentSpeedBoost = null;

    // UFO and Easter Egg
    private UFO ufo;
    private boolean ufoSpawnedThisLevel = false;
    private int[] playerMachinegunKills = new int[4];
    private boolean ufoWasKilled = false;
    private int ufoLostMessageTimer = 0;
    private int ufoKilledMessageTimer = 0;
    private EasterEgg easterEgg = null;

    // Victory/GameOver state
    private boolean victoryConditionMet = false;
    private int victoryDelayTimer = 0;
    private boolean gameOverSoundPlayed = false;
    private int bossKillerPlayerIndex = -1;
    private PowerUp.Type bossKillPowerUpReward = null;

    // Dancing animations
    private List<DancingCharacter> dancingCharacters = new ArrayList<>();
    private boolean dancingInitialized = false;
    private List<DancingGirl> victoryDancingGirls = new ArrayList<>();
    private boolean victoryDancingInitialized = false;

    // Player nicknames
    private String[] playerNicknames = new String[4];

    // Client sound tracking (for network sync)
    private int prevEnemyCount = 0;
    private Set<Long> seenBulletIds = new HashSet<>();
    private Set<Long> seenLaserIds = new HashSet<>();
    private Set<Integer> seenBurningTileKeys = new HashSet<>();
    private boolean firstStateReceived = false;
    private int respawnSyncFrames = 0;

    public GameContext(Pane root, Canvas canvas, int width, int height, int playerCount,
                       int totalEnemies, Stage stage) {
        this.root = root;
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.width = width;
        this.height = height;
        this.playerCount = playerCount;
        this.totalEnemies = totalEnemies;
        this.stage = stage;
    }

    // Core rendering getters
    public Pane getRoot() { return root; }
    public Canvas getCanvas() { return canvas; }
    public GraphicsContext getGc() { return gc; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Stage getStage() { return stage; }

    // Game configuration getters
    public int getPlayerCount() { return playerCount; }
    public int getTotalEnemies() { return totalEnemies; }

    // Core game entities getters/setters
    public GameMap getGameMap() { return gameMap; }
    public void setGameMap(GameMap gameMap) { this.gameMap = gameMap; }

    public List<Tank> getPlayerTanks() { return playerTanks; }
    public void setPlayerTanks(List<Tank> playerTanks) { this.playerTanks = playerTanks; }

    public List<Tank> getEnemyTanks() { return enemyTanks; }
    public void setEnemyTanks(List<Tank> enemyTanks) { this.enemyTanks = enemyTanks; }

    public List<Bullet> getBullets() { return bullets; }
    public void setBullets(List<Bullet> bullets) { this.bullets = bullets; }

    public List<Laser> getLasers() { return lasers; }
    public void setLasers(List<Laser> lasers) { this.lasers = lasers; }

    public List<PowerUp> getPowerUps() { return powerUps; }
    public void setPowerUps(List<PowerUp> powerUps) { this.powerUps = powerUps; }

    public EnemySpawner getEnemySpawner() { return enemySpawner; }
    public void setEnemySpawner(EnemySpawner enemySpawner) { this.enemySpawner = enemySpawner; }

    public Base getBase() { return base; }
    public void setBase(Base base) { this.base = base; }

    public double[][] getPlayerStartPositions() { return playerStartPositions; }
    public void setPlayerStartPositions(double[][] playerStartPositions) { this.playerStartPositions = playerStartPositions; }

    // Audio
    public SoundManager getSoundManager() { return soundManager; }
    public void setSoundManager(SoundManager soundManager) { this.soundManager = soundManager; }

    // Network
    public NetworkManager getNetwork() { return network; }
    public void setNetwork(NetworkManager network) { this.network = network; }

    public boolean isNetworkGame() { return isNetworkGame; }
    public void setNetworkGame(boolean networkGame) { isNetworkGame = networkGame; }

    public int getNetworkConnectedPlayers() { return networkConnectedPlayers; }
    public void setNetworkConnectedPlayers(int networkConnectedPlayers) { this.networkConnectedPlayers = networkConnectedPlayers; }

    public List<GameState.TileChange> getMapChanges() { return mapChanges; }
    public void setMapChanges(List<GameState.TileChange> mapChanges) { this.mapChanges = mapChanges; }

    // Game state flags
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public boolean isVictory() { return victory; }
    public void setVictory(boolean victory) { this.victory = victory; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public int getPauseMenuSelection() { return pauseMenuSelection; }
    public void setPauseMenuSelection(int pauseMenuSelection) { this.pauseMenuSelection = pauseMenuSelection; }

    public boolean[] getPlayerPaused() { return playerPaused; }
    public void setPlayerPaused(boolean[] playerPaused) { this.playerPaused = playerPaused; }

    // Score tracking
    public PlayerStats getPlayerStats() { return playerStats; }
    public void setPlayerStats(PlayerStats playerStats) {
        this.playerStats = playerStats;
        // Update legacy array references
        this.playerKills = playerStats.getKillsArray();
        this.playerScores = playerStats.getScoresArray();
        this.playerLevelScores = playerStats.getLevelScoresArray();
        this.playerKillsByType = playerStats.getKillsByTypeMatrix();
    }

    public int[] getPlayerKills() { return playerKills; }
    public void setPlayerKills(int[] playerKills) { this.playerKills = playerKills; }

    public int[] getPlayerScores() { return playerScores; }
    public void setPlayerScores(int[] playerScores) { this.playerScores = playerScores; }

    public int[] getPlayerLevelScores() { return playerLevelScores; }
    public void setPlayerLevelScores(int[] playerLevelScores) { this.playerLevelScores = playerLevelScores; }

    public int[][] getPlayerKillsByType() { return playerKillsByType; }
    public void setPlayerKillsByType(int[][] playerKillsByType) { this.playerKillsByType = playerKillsByType; }

    public boolean isWinnerBonusAwarded() { return winnerBonusAwarded; }
    public void setWinnerBonusAwarded(boolean winnerBonusAwarded) { this.winnerBonusAwarded = winnerBonusAwarded; }

    // Power-up effect durations
    public int getBaseProtectionDuration() { return baseProtectionDuration; }
    public void setBaseProtectionDuration(int baseProtectionDuration) { this.baseProtectionDuration = baseProtectionDuration; }

    public boolean isFlashing() { return isFlashing; }
    public void setFlashing(boolean flashing) { isFlashing = flashing; }

    public int getFlashCount() { return flashCount; }
    public void setFlashCount(int flashCount) { this.flashCount = flashCount; }

    public int getFlashTimer() { return flashTimer; }
    public void setFlashTimer(int flashTimer) { this.flashTimer = flashTimer; }

    public int getEnemyFreezeDuration() { return enemyFreezeDuration; }
    public void setEnemyFreezeDuration(int enemyFreezeDuration) { this.enemyFreezeDuration = enemyFreezeDuration; }

    public int getPlayerFreezeDuration() { return playerFreezeDuration; }
    public void setPlayerFreezeDuration(int playerFreezeDuration) { this.playerFreezeDuration = playerFreezeDuration; }

    public int getEnemyTeamSpeedBoostDuration() { return enemyTeamSpeedBoostDuration; }
    public void setEnemyTeamSpeedBoostDuration(int enemyTeamSpeedBoostDuration) { this.enemyTeamSpeedBoostDuration = enemyTeamSpeedBoostDuration; }

    public Tank getEnemyWithPermanentSpeedBoost() { return enemyWithPermanentSpeedBoost; }
    public void setEnemyWithPermanentSpeedBoost(Tank enemyWithPermanentSpeedBoost) { this.enemyWithPermanentSpeedBoost = enemyWithPermanentSpeedBoost; }

    // UFO and Easter Egg
    public UFO getUfo() { return ufo; }
    public void setUfo(UFO ufo) { this.ufo = ufo; }

    public boolean isUfoSpawnedThisLevel() { return ufoSpawnedThisLevel; }
    public void setUfoSpawnedThisLevel(boolean ufoSpawnedThisLevel) { this.ufoSpawnedThisLevel = ufoSpawnedThisLevel; }

    public int[] getPlayerMachinegunKills() { return playerMachinegunKills; }
    public void setPlayerMachinegunKills(int[] playerMachinegunKills) { this.playerMachinegunKills = playerMachinegunKills; }

    public boolean isUfoWasKilled() { return ufoWasKilled; }
    public void setUfoWasKilled(boolean ufoWasKilled) { this.ufoWasKilled = ufoWasKilled; }

    public int getUfoLostMessageTimer() { return ufoLostMessageTimer; }
    public void setUfoLostMessageTimer(int ufoLostMessageTimer) { this.ufoLostMessageTimer = ufoLostMessageTimer; }

    public int getUfoKilledMessageTimer() { return ufoKilledMessageTimer; }
    public void setUfoKilledMessageTimer(int ufoKilledMessageTimer) { this.ufoKilledMessageTimer = ufoKilledMessageTimer; }

    public EasterEgg getEasterEgg() { return easterEgg; }
    public void setEasterEgg(EasterEgg easterEgg) { this.easterEgg = easterEgg; }

    // Victory/GameOver state
    public boolean isVictoryConditionMet() { return victoryConditionMet; }
    public void setVictoryConditionMet(boolean victoryConditionMet) { this.victoryConditionMet = victoryConditionMet; }

    public int getVictoryDelayTimer() { return victoryDelayTimer; }
    public void setVictoryDelayTimer(int victoryDelayTimer) { this.victoryDelayTimer = victoryDelayTimer; }

    public boolean isGameOverSoundPlayed() { return gameOverSoundPlayed; }
    public void setGameOverSoundPlayed(boolean gameOverSoundPlayed) { this.gameOverSoundPlayed = gameOverSoundPlayed; }

    public int getBossKillerPlayerIndex() { return bossKillerPlayerIndex; }
    public void setBossKillerPlayerIndex(int bossKillerPlayerIndex) { this.bossKillerPlayerIndex = bossKillerPlayerIndex; }

    public PowerUp.Type getBossKillPowerUpReward() { return bossKillPowerUpReward; }
    public void setBossKillPowerUpReward(PowerUp.Type bossKillPowerUpReward) { this.bossKillPowerUpReward = bossKillPowerUpReward; }

    // Dancing animations
    public List<DancingCharacter> getDancingCharacters() { return dancingCharacters; }
    public void setDancingCharacters(List<DancingCharacter> dancingCharacters) { this.dancingCharacters = dancingCharacters; }

    public boolean isDancingInitialized() { return dancingInitialized; }
    public void setDancingInitialized(boolean dancingInitialized) { this.dancingInitialized = dancingInitialized; }

    public List<DancingGirl> getVictoryDancingGirls() { return victoryDancingGirls; }
    public void setVictoryDancingGirls(List<DancingGirl> victoryDancingGirls) { this.victoryDancingGirls = victoryDancingGirls; }

    public boolean isVictoryDancingInitialized() { return victoryDancingInitialized; }
    public void setVictoryDancingInitialized(boolean victoryDancingInitialized) { this.victoryDancingInitialized = victoryDancingInitialized; }

    // Player nicknames
    public String[] getPlayerNicknames() { return playerNicknames; }
    public void setPlayerNicknames(String[] playerNicknames) { this.playerNicknames = playerNicknames; }

    public String getPlayerNickname(int index) {
        if (index >= 0 && index < playerNicknames.length) {
            return playerNicknames[index];
        }
        return null;
    }

    public void setPlayerNickname(int index, String nickname) {
        if (index >= 0 && index < playerNicknames.length) {
            playerNicknames[index] = nickname;
        }
    }

    // Client sound tracking
    public int getPrevEnemyCount() { return prevEnemyCount; }
    public void setPrevEnemyCount(int prevEnemyCount) { this.prevEnemyCount = prevEnemyCount; }

    public Set<Long> getSeenBulletIds() { return seenBulletIds; }
    public void setSeenBulletIds(Set<Long> seenBulletIds) { this.seenBulletIds = seenBulletIds; }

    public Set<Long> getSeenLaserIds() { return seenLaserIds; }
    public void setSeenLaserIds(Set<Long> seenLaserIds) { this.seenLaserIds = seenLaserIds; }

    public Set<Integer> getSeenBurningTileKeys() { return seenBurningTileKeys; }
    public void setSeenBurningTileKeys(Set<Integer> seenBurningTileKeys) { this.seenBurningTileKeys = seenBurningTileKeys; }

    public boolean isFirstStateReceived() { return firstStateReceived; }
    public void setFirstStateReceived(boolean firstStateReceived) { this.firstStateReceived = firstStateReceived; }

    public int getRespawnSyncFrames() { return respawnSyncFrames; }
    public void setRespawnSyncFrames(int respawnSyncFrames) { this.respawnSyncFrames = respawnSyncFrames; }

    // Utility methods
    public boolean isHost() {
        return !isNetworkGame || network == null || network.isHost();
    }

    public int getActivePlayers() {
        if (isNetworkGame) {
            return networkConnectedPlayers;
        }
        return playerCount;
    }

    /**
     * Check if any player tank is alive.
     */
    public boolean hasAlivePlayers() {
        for (Tank player : playerTanks) {
            if (player.isAlive() || player.getLives() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the map dimensions in tiles.
     */
    public int getMapSize() {
        return GameConstants.MAP_SIZE;
    }

    /**
     * Get the tile size in pixels.
     */
    public int getTileSize() {
        return GameConstants.TILE_SIZE;
    }
}
