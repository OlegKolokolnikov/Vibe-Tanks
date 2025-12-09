package com.vibetanks.rendering;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.File;
import java.net.URL;

/**
 * Utility class for loading game images (victory/game over screens).
 * Extracted from Game.java to reduce complexity.
 */
public class ImageLoader {

    /**
     * Result of loading an end-game image.
     */
    public static class LoadResult {
        public final ImageView imageView;
        public final boolean success;

        public LoadResult(ImageView imageView, boolean success) {
            this.imageView = imageView;
            this.success = success;
        }
    }

    /**
     * Load victory image from local file, resources, or fallback URLs.
     *
     * @param root The pane to add the ImageView to
     * @param width Screen width for positioning
     * @param height Screen height for positioning
     * @param classForResources Class to use for resource loading
     * @return LoadResult with ImageView (may be null if loading failed)
     */
    public static LoadResult loadVictoryImage(Pane root, int width, int height, Class<?> classForResources) {
        String[] fallbackUrls = {
            "https://i.imgur.com/7kZ8Lrb.gif",
            "https://media.tenor.com/fSBeKScbxIkAAAAM/anime-dance.gif",
            "https://media.giphy.com/media/nAvSNP8Y3F94hq9Rga/giphy.gif"
        };

        return loadEndGameImage(root, width, height, classForResources,
                "victory.gif", "victory", fallbackUrls);
    }

    /**
     * Load game over image from local file, resources, or fallback URLs.
     *
     * @param root The pane to add the ImageView to
     * @param width Screen width for positioning
     * @param height Screen height for positioning
     * @param classForResources Class to use for resource loading
     * @return LoadResult with ImageView (may be null if loading failed)
     */
    public static LoadResult loadGameOverImage(Pane root, int width, int height, Class<?> classForResources) {
        String[] fallbackUrls = {
            "https://media.tenor.com/wD7yF6gA1XwAAAAM/skeleton-dance.gif",
            "https://media.giphy.com/media/3oKIPsx2VAYAgEHC12/giphy.gif",
            "https://i.imgur.com/bJPo2.gif",
            "https://media1.tenor.com/m/p0wM4WV3XPAAAAAC/skeleton-dancing.gif"
        };

        return loadEndGameImage(root, width, height, classForResources,
                "gameover.gif", "game over", fallbackUrls);
    }

    /**
     * Generic method to load an end-game image with fallbacks.
     */
    private static LoadResult loadEndGameImage(Pane root, int width, int height,
                                                Class<?> classForResources,
                                                String filename, String displayName,
                                                String[] fallbackUrls) {
        try {
            Image image = null;

            // Try to load from local resources first
            image = tryLoadFromLocalFile(filename, displayName);

            // If local file failed, try resource path
            if (image == null || image.isError()) {
                image = tryLoadFromResources(classForResources, filename, displayName);
            }

            // If still no image, try URLs as fallback
            if (image == null || image.isError()) {
                image = tryLoadFromUrls(fallbackUrls, displayName);
            }

            if (image != null && !image.isError()) {
                ImageView imageView = createImageView(image, width, height);
                root.getChildren().add(imageView);
                System.out.println(capitalize(displayName) + " image view added successfully!");
                return new LoadResult(imageView, true);
            } else {
                System.out.println("Could not load " + displayName + " image from any source");
                System.out.println("Please place a '" + filename + "' file in: src/main/resources/images/");
                return new LoadResult(null, false);
            }
        } catch (Exception e) {
            System.out.println("Could not load " + displayName + " image: " + e.getMessage());
            e.printStackTrace();
            return new LoadResult(null, false);
        }
    }

    /**
     * Try to load image from local file path.
     */
    private static Image tryLoadFromLocalFile(String filename, String displayName) {
        try {
            File localFile = new File("src/main/resources/images/" + filename);
            if (localFile.exists()) {
                System.out.println("Loading " + displayName + " image from local file: " + localFile.getPath());
                Image image = new Image(localFile.toURI().toString());
                if (!image.isError()) {
                    System.out.println("Successfully loaded " + displayName + " image from local file!");
                    return image;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load from local file: " + e.getMessage());
        }
        return null;
    }

    /**
     * Try to load image from classpath resources.
     */
    private static Image tryLoadFromResources(Class<?> clazz, String filename, String displayName) {
        try {
            URL resourceUrl = clazz.getResource("/images/" + filename);
            if (resourceUrl != null) {
                System.out.println("Loading " + displayName + " image from resources");
                Image image = new Image(resourceUrl.toString());
                if (!image.isError()) {
                    System.out.println("Successfully loaded " + displayName + " image from resources!");
                    return image;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load from resources: " + e.getMessage());
        }
        return null;
    }

    /**
     * Try to load image from fallback URLs.
     */
    private static Image tryLoadFromUrls(String[] urls, String displayName) {
        for (String url : urls) {
            try {
                System.out.println("Trying to load " + displayName + " image from URL: " + url);
                Image image = new Image(url, true);
                if (!image.isError()) {
                    System.out.println("Successfully loaded " + displayName + " image from: " + url);
                    return image;
                }
            } catch (Exception e) {
                System.out.println("Failed to load from: " + url);
            }
        }
        return null;
    }

    /**
     * Create and configure an ImageView for end-game display.
     */
    private static ImageView createImageView(Image image, int width, int height) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);
        imageView.setLayoutX(width / 2.0 - 150);
        imageView.setLayoutY(height / 2.0 - 250);
        imageView.setVisible(false);
        return imageView;
    }

    /**
     * Capitalize the first letter of a string.
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
