# Battle City - Image Resources

## How to Add Custom GIFs

Place your GIF files in this directory to customize the victory and game over screens.

### Required Files

1. **victory.gif** - Dancing anime girl shown when you win
   - Recommended size: 300x300 pixels
   - Format: Animated GIF
   - Suggested sources:
     - https://tenor.com/search/anime-dance-gifs
     - https://giphy.com/search/anime-dance
     - Any dancing anime girl GIF you like!

2. **gameover.gif** - Dancing skeleton/death shown when you lose
   - Recommended size: 300x300 pixels
   - Format: Animated GIF
   - Suggested sources:
     - https://tenor.com/search/skeleton-dance-gifs
     - https://giphy.com/search/dancing-skeleton
     - Any dancing skeleton/death GIF you like!

### How to Add GIFs

1. Download your desired GIF files
2. Rename them to exactly:
   - `victory.gif` (for victory screen)
   - `gameover.gif` (for game over screen)
3. Place them in this directory: `src/main/resources/images/`
4. Run the game!

### Fallback Behavior

If the local GIF files are not found, the game will automatically try to load them from internet URLs. However, local files are recommended for:
- Faster loading
- No internet connection required
- Custom personalization

### Tips

- Keep GIF file sizes reasonable (under 5MB) for better performance
- Use square or near-square aspect ratios for best display
- The GIFs will be displayed as 300x300 pixels in-game
- Test your GIFs by winning/losing a test game!
