# Vibe Tanks - JavaFX Game

A classic tank battle game using JavaFX.

## Features

- **Start Menu** with game mode selection
- **Single-player mode** - Battle enemy tanks solo
- **Local 2-player mode** - Two players on the same keyboard
- **Online multiplayer** - Up to 4 players can team up over the network
- **Auto-generated sound effects** - Sounds are created automatically on first run
- 25 AI enemy tanks per level (max 5 on screen at once)
- 6 different enemy tank types with varying abilities
- Multiple power-ups:
  - **GUN**: Bullets can destroy steel walls
  - **STAR**: Faster shooting (stackable)
  - **CAR**: Faster movement (stackable)
  - **SHIP**: Can move through water
  - **SHOVEL**: Base protected by steel walls for 1 minute
  - **SAW**: Can destroy trees/forest
  - **TANK**: Extra life
  - **SHIELD**: Temporary invincibility (1 minute)
  - **MACHINEGUN**: Multiple bullets in a line (stackable)
  - **FREEZE**: Freeze all enemies for 10 seconds
  - **BOMB**: Destroy all enemies on screen
- Destructible terrain:
  - Brick walls: Destroyed by bullets
  - Steel walls: Destroyed only with GUN power-up
  - Water: Cannot be crossed (unless SHIP power-up)
  - Trees: Tanks can pass through, provide cover (destroyed by SAW power-up)
  - Ice: Tanks slide when moving
- Sound effects: Shooting, explosions, and intro music (automatically generated)
- Base defense gameplay: Protect your base from enemy tanks
- Victory celebration with Soviet flag and dancing girls!

## How to Play

### Single Player
1. **Start Menu**: When you launch the game, you'll see a start menu
2. **Select "PLAY"**: Start a solo game against enemy tanks
3. **Defend Your Base**: Destroy all enemy tanks before they destroy your base
4. **Press ENTER**: Continue to next level after victory, or restart after game over
5. **Return to Menu**: Press **ESC** to return to menu

### Local 2 Players (Same Keyboard)
1. **Select "PLAY 2 PLAYERS"**: Start a local co-op game
2. **Player 1 Controls**: WASD to move, SPACE to shoot
3. **Player 2 Controls**: Arrow keys to move, ENTER to shoot
4. **Team Up**: Work together on the same computer to defend the base!

### Online Multiplayer (2-4 Players)
1. **Host**: One player clicks "HOST GAME (ONLINE)" and shares their IP address
2. **Join**: Other players click "JOIN GAME (ONLINE)" and enter the host's IP
3. **Team Up**: Work together to defend the base from enemy tanks
4. **Per-Player Pause**: Press ESC to pause with shield (game continues for others)
5. **Share Lives**: Press ENTER to take a life from a teammate when dead
6. **Have Fun**: Coordinate with your teammates to survive!

### Dedicated Server (Cloud Hosting)
Run a headless server that players can connect to:

#### Using Maven (Development)
```bash
mvn exec:java -Pserver                    # Default port 25565
mvn exec:java -Pserver -Dexec.args=12345  # Custom port
```

#### Using compiled classes
```bash
mvn clean compile -DskipTests
java -cp target/classes com.vibetanks.DedicatedServer        # Default port
java -cp target/classes com.vibetanks.DedicatedServer 12345  # Custom port
```

#### Server Features
- Game starts automatically when first player connects
- Up to 4 players can join (including mid-game)
- Server runs at 60 FPS without graphics
- Resets to waiting state when all players disconnect
- Default port: 25565

#### Connecting to Server
1. Start the dedicated server
2. Launch the game client: `mvn javafx:run`
3. Click "JOIN GAME (ONLINE)"
4. Enter `localhost` (if local) or server IP address
5. Play!

## Controls

### Single Player / Online Multiplayer
- **Arrow Keys or WASD**: Move tank
- **SPACE**: Shoot
- **ENTER**: Take life from teammate (when dead) / Next level / Restart
- **ESC**: Pause (single player) / Pause with shield (multiplayer) / Return to menu

### Local 2 Players
| | Player 1 | Player 2 |
|---|---|---|
| **Move** | WASD | Arrow Keys |
| **Shoot** | SPACE | ENTER |

## Installation

### Option 1: Download Pre-built Executable (Recommended)

Download the latest release for your platform from the [Releases](https://github.com/OlegKolokolnikov/Vibe-Tanks/releases) page.

- **Windows**: Download and extract `VibeTanks-Windows.zip`, then run `VibeTanks.exe`
- **Linux**: Download and extract `VibeTanks-Linux.tar.gz`, then run `./VibeTanks`
- **macOS**: Download `VibeTanks-macOS.zip`, extract, and open `VibeTanks.app`

No Java installation required - everything is bundled!

### Option 2: Build Native Executable from Source

Build a native executable that doesn't require Java to be installed:

#### Prerequisites
- JDK 17 or higher (with jpackage tool)
- Maven

#### Windows
```bash
build-windows.bat
```
After building, find the executable at `target\dist\VibeTanks\VibeTanks.exe` or use the `VibeTanks.lnk` shortcut in the project root.

#### Linux
```bash
chmod +x build-linux.sh
./build-linux.sh
```
After building, run `./VibeTanks` from the project root (symlink) or `./target/dist/VibeTanks/bin/VibeTanks`.

#### macOS
```bash
chmod +x build-mac.sh
./build-mac.sh
```
After building, open `VibeTanks.app` from the project root (symlink) or `target/dist/VibeTanks.app`.

### Option 3: Run with Maven (Development)

Run directly using Maven (requires Java 17+ and Maven):

```bash
mvn clean javafx:run
```

### Option 4: Run the JAR file

Build and run the shaded JAR (requires Java 17+):

```bash
mvn clean package -DskipTests
java -jar target/vibe-tanks-1.0-SNAPSHOT-shaded.jar
```

## Sound Effects

The game automatically generates basic sound effects on first run:
- `shoot.wav` - Sound when tanks shoot
- `explosion.wav` - Sound when tanks are destroyed
- `intro.wav` - Intro music when game starts

These files are created in `src/main/resources/sounds/` using procedurally generated audio.

**Custom Sounds**: You can replace these generated files with your own .wav files for better quality sound effects. Find free sounds at:
- https://freesound.org/
- https://opengameart.org/

## Game Mechanics

### Victory Condition
Destroy all enemy tanks without losing your base.

### Game Over Conditions
- All players lose all their lives
- The base is destroyed

### Score System
- **1 point** per regular enemy kill
- **2 points** for POWER (rainbow) tanks
- **5 points** for HEAVY (black) tanks
- **10 points** for BOSS
- **Extra life** every 100 points!

### Power-ups
Power-ups randomly spawn on the map. They last for 10 seconds before disappearing.
Some power-ups are stackable (STAR, CAR, MACHINEGUN) - collect multiple for enhanced effects!

**Warning**: Enemies can also collect power-ups! FREEZE will freeze players (but they can still shoot), and BOMB will damage all players.

### Lives System
- Each player starts with 3 lives
- Display shows remaining respawns (lives - 1)
- Players respawn with a temporary shield after losing a life
- Shield power-up provides 1 minute of invincibility
- Press ENTER to take a life from a teammate who has lives to spare

### Enemy Types
6 different enemy tank types with increasing difficulty:
- **REGULAR**: Standard enemy tank (red) - 1 point
- **FAST**: Moves faster than normal (light red) - 1 point
- **ARMORED**: Takes 2 hits to destroy (dark red) - 1 point
- **POWER**: Drops power-ups on every hit (rainbow) - 2 points
- **HEAVY**: Fast, 3 health, appears in last 5 enemies (black) - 5 points
- **BOSS**: 4x size, 12 health, rainbow color, immune to freeze, big bullets - spawns last! 10 points

### Network Multiplayer
- Host runs the authoritative game logic
- Client-authoritative movement for smooth gameplay
- Supports up to 4 players cooperating
- Works best on local network (LAN)
- Uses TCP on port 25565
- Per-player pause with shield protection

## Technical Details

- Grid-based movement (26x26 tiles)
- 60 FPS game loop
- Collision detection for tanks, bullets, and terrain
- AI pathfinding with obstacle avoidance
- Network synchronization at 60Hz
- Client-authoritative movement for responsive controls

Enjoy playing Vibe Tanks!
