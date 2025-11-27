# Battle City - JavaFX Game

A recreation of the classic Battle City tank game using JavaFX.

## Features

- **Start Menu** with game mode selection
- **Single-player mode** - Battle 100 enemy tanks solo
- **Online multiplayer** - Up to 4 players can team up over the network
- **Auto-generated sound effects** - Sounds are created automatically on first run
- 100 AI enemy tanks (max 10 on screen at once)
- 5 different enemy tank types with varying abilities
- Multiple power-ups:
  - **GUN**: Bullets can destroy steel walls
  - **STAR**: Faster shooting (stackable)
  - **CAR**: Faster movement (stackable)
  - **SHIP**: Can move through water
  - **SHOVEL**: Base protected by steel walls for 1 minute
  - **SAW**: Can destroy trees/forest
  - **TANK**: Extra life
  - **SHIELD**: Temporary invincibility (1 minute)
  - **MACHINEGUN**: Rapid-fire bullets
- Destructible terrain:
  - Brick walls: Destroyed by bullets
  - Steel walls: Destroyed only with GUN power-up
  - Water: Cannot be crossed (unless SHIP power-up)
  - Trees: Tanks can pass through, provide cover (destroyed by SAW power-up)
  - Ice: Tanks slide when moving
- Sound effects: Shooting, explosions, and intro music (automatically generated)
- Base defense gameplay: Protect your base from enemy tanks

## How to Play

### Single Player
1. **Start Menu**: When you launch the game, you'll see a start menu
2. **Select "1 PLAYER"**: Start a solo game against 100 enemy tanks
3. **Defend Your Base**: Destroy all enemy tanks before they destroy your base
4. **Return to Menu**: Press **ESC** when game is over or you win

### Online Multiplayer (Up to 4 Players)
1. **Host**: One player clicks "HOST GAME (ONLINE)" and shares their IP address
2. **Join**: Up to 3 other players click "JOIN GAME (ONLINE)" and enter the host's IP
3. **Team Up**: Work together to defend the base from 100 enemy tanks
4. **Have Fun**: Coordinate with your teammates to survive!

## Controls

**All Players** (both local and online):
- **Arrow Keys** (↑ ↓ ← →): Move tank
- **Space**: Shoot

**Other Controls**:
- **ESC**: Return to menu (after game over/victory)

## How to Run

### Prerequisites
- Java 17 or higher
- Maven

### Running the game

```bash
mvn clean javafx:run
```

### Building the project

```bash
mvn clean package
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
Destroy all 100 enemy tanks without losing your base.

### Game Over Conditions
- All players lose all their lives
- The base is destroyed

### Power-ups
Power-ups randomly spawn on the map every 15 seconds. They last for 10 seconds before disappearing.
Some power-ups are stackable (STAR, CAR, MACHINEGUN) - collect multiple for enhanced effects!

### Lives System
- Each player starts with 3 lives
- Players respawn with a temporary shield after losing a life
- Shield power-up provides 1 minute of invincibility

### Enemy Types
5 different enemy tank types with increasing difficulty:
- **REGULAR**: Standard enemy tank
- **FAST**: Moves faster than normal
- **POWER**: Shoots stronger bullets
- **ARMORED**: Takes 4 hits to destroy
- **ULTRA**: Fastest, strongest enemy tank

### Network Multiplayer
- Host runs the authoritative game logic
- Clients send input and receive game state
- Supports up to 4 players cooperating
- Works best on local network (LAN)
- Uses TCP on port 25565

## Technical Details

- Grid-based movement (26x26 tiles)
- 60 FPS game loop
- Collision detection for tanks, bullets, and terrain
- AI pathfinding with obstacle avoidance
- Network synchronization at 20Hz
- Peer-to-peer multiplayer architecture

Enjoy playing Battle City!
