# Battle City - JavaFX Game

A recreation of the classic Battle City tank game using JavaFX.

## Features

- **Start Menu** with game mode selection
- **1-player or 2-player modes** - Choose your preferred game mode
- **Auto-generated sound effects** - Sounds are created automatically on first run
- 100 AI enemy tanks (max 10 on screen at once)
- Multiple power-ups:
  - Shield: Temporary invincibility
  - Speed: Increased movement speed
  - Power: More powerful bullets that can destroy steel
  - Life: Extra life
- Destructible terrain:
  - Brick walls: Destroyed by bullets
  - Steel walls: Indestructible (unless using power bullets)
  - Water: Cannot be crossed by tanks
  - Trees: Tanks can pass through but provide cover
- Sound effects: Shooting, explosions, and intro music (automatically generated)
- Base defense gameplay: Protect your base from enemy tanks

## How to Play

1. **Start Menu**: When you launch the game, you'll see a start menu
2. **Choose Mode**: Select "1 PLAYER" or "2 PLAYERS"
3. **Defend Your Base**: Destroy all 100 enemy tanks before they destroy your base
4. **Return to Menu**: Press **ESC** when game is over or you win

## Controls

### Player 1
- **W/A/S/D**: Move up/left/down/right
- **Space**: Shoot

### Player 2
- **Arrow Keys**: Move up/left/down/right
- **Enter**: Shoot

### General
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
- Both players lose all their lives
- The base is destroyed

### Power-ups
Power-ups randomly drop when you destroy enemy tanks (30% chance). They last for 10 seconds before disappearing.

### Lives System
- Each player starts with 3 lives
- Players respawn with a temporary shield after losing a life
- Shields also provide temporary invincibility

## Technical Details

- Grid-based movement (26x26 tiles)
- 60 FPS game loop
- Collision detection for tanks, bullets, and terrain
- AI pathfinding towards the player base

## Future Enhancements

Possible additions for future versions:
- Multiple levels with different layouts
- Different enemy tank types
- High score tracking
- More power-up types
- Improved AI behavior

Enjoy playing Battle City!
