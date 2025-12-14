import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Generates app icons for VibeTanks (macOS and Windows).
 * Run with: java tools/GenerateIcon.java
 */
public class GenerateIcon {

    public static void main(String[] args) throws Exception {
        // Create iconset directory for macOS
        File iconsetDir = new File("VibeTanks.iconset");
        iconsetDir.mkdirs();

        // Generate icons at required sizes
        int[] sizes = {16, 32, 64, 128, 256, 512, 1024};

        for (int size : sizes) {
            BufferedImage img = createIcon(size);

            // Save standard size
            ImageIO.write(img, "PNG", new File(iconsetDir, "icon_" + size + "x" + size + ".png"));

            // Save @2x version (half the pixel size name)
            if (size >= 32) {
                int halfSize = size / 2;
                ImageIO.write(img, "PNG", new File(iconsetDir, "icon_" + halfSize + "x" + halfSize + "@2x.png"));
            }
        }

        System.out.println("macOS icon set created in VibeTanks.iconset/");
        System.out.println("Run: iconutil -c icns VibeTanks.iconset");

        // Generate Windows ICO file
        generateWindowsIco();
        System.out.println("Windows icon created: VibeTanks.ico");
    }

    /**
     * Generate Windows ICO file with multiple sizes.
     * ICO format: Header + Directory entries + Image data (PNG format)
     */
    private static void generateWindowsIco() throws Exception {
        // Windows icon sizes (standard sizes for best compatibility)
        int[] icoSizes = {16, 32, 48, 64, 128, 256};

        // Generate PNG data for each size
        byte[][] pngData = new byte[icoSizes.length][];
        for (int i = 0; i < icoSizes.length; i++) {
            BufferedImage img = createIcon(icoSizes[i]);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            pngData[i] = baos.toByteArray();
        }

        // Write ICO file
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream("VibeTanks.ico")))) {

            // ICO Header (6 bytes)
            dos.writeShort(swapShort(0));     // Reserved, must be 0
            dos.writeShort(swapShort(1));     // Image type: 1 = ICO
            dos.writeShort(swapShort(icoSizes.length)); // Number of images

            // Calculate offset to first image data
            int dataOffset = 6 + (icoSizes.length * 16); // Header + directory entries

            // Write directory entries (16 bytes each)
            for (int i = 0; i < icoSizes.length; i++) {
                int size = icoSizes[i];
                dos.writeByte(size == 256 ? 0 : size); // Width (0 = 256)
                dos.writeByte(size == 256 ? 0 : size); // Height (0 = 256)
                dos.writeByte(0);              // Color palette (0 = no palette)
                dos.writeByte(0);              // Reserved
                dos.writeShort(swapShort(1));  // Color planes
                dos.writeShort(swapShort(32)); // Bits per pixel
                dos.writeInt(swapInt(pngData[i].length)); // Image data size
                dos.writeInt(swapInt(dataOffset));        // Offset to image data

                dataOffset += pngData[i].length;
            }

            // Write image data (PNG format)
            for (byte[] data : pngData) {
                dos.write(data);
            }
        }
    }

    // Swap bytes for little-endian (ICO uses little-endian)
    private static short swapShort(int value) {
        return (short) (((value & 0xFF) << 8) | ((value >> 8) & 0xFF));
    }

    private static int swapInt(int value) {
        return ((value & 0xFF) << 24) |
               ((value & 0xFF00) << 8) |
               ((value >> 8) & 0xFF00) |
               ((value >> 24) & 0xFF);
    }

    private static BufferedImage createIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = size / 64.0;  // Base design is 64px

        // Background - dark gradient
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(30, 30, 40),
            size, size, new Color(10, 10, 20)
        );
        g.setPaint(bgGradient);
        g.fillRoundRect(0, 0, size, size, (int)(12 * scale), (int)(12 * scale));

        // Draw border
        g.setColor(new Color(80, 80, 100));
        g.setStroke(new BasicStroke((float)(2 * scale)));
        g.drawRoundRect(1, 1, size - 3, size - 3, (int)(12 * scale), (int)(12 * scale));

        // Tank parameters
        double tankSize = 40 * scale;
        double tankX = (size - tankSize) / 2;
        double tankY = (size - tankSize) / 2 + 2 * scale; // Slightly lower to account for cannon

        // Tank colors (green player tank)
        Color tankColor = new Color(0, 200, 0);
        Color darkColor = tankColor.darker();

        // Save transform for tank rotation (facing up)
        AffineTransform originalTransform = g.getTransform();

        // Translate to tank center
        g.translate(tankX + tankSize / 2, tankY + tankSize / 2);
        g.translate(-tankSize / 2, -tankSize / 2);

        double ts = tankSize / 32.0; // Tank scale factor

        // Draw tracks (left)
        g.setColor(darkColor);
        g.fill(new Rectangle2D.Double(0, 0, 8 * ts, tankSize));
        g.setColor(new Color(40, 40, 40));
        for (int i = 0; i < 5; i++) {
            g.fill(new Rectangle2D.Double(1 * ts, i * 7 * ts, 6 * ts, 3 * ts));
        }

        // Draw tracks (right)
        g.setColor(darkColor);
        g.fill(new Rectangle2D.Double(tankSize - 8 * ts, 0, 8 * ts, tankSize));
        g.setColor(new Color(40, 40, 40));
        for (int i = 0; i < 5; i++) {
            g.fill(new Rectangle2D.Double(tankSize - 7 * ts, i * 7 * ts, 6 * ts, 3 * ts));
        }

        // Draw tank body
        g.setColor(tankColor);
        g.fill(new Rectangle2D.Double(6 * ts, 4 * ts, tankSize - 12 * ts, tankSize - 8 * ts));

        // Draw turret (outer ring)
        g.setColor(darkColor);
        g.fill(new Ellipse2D.Double(tankSize / 2 - 7 * ts, tankSize / 2 - 7 * ts, 14 * ts, 14 * ts));

        // Draw turret (inner)
        g.setColor(tankColor);
        g.fill(new Ellipse2D.Double(tankSize / 2 - 5 * ts, tankSize / 2 - 5 * ts, 10 * ts, 10 * ts));

        // Draw cannon
        g.setColor(Color.DARK_GRAY);
        g.fill(new Rectangle2D.Double(tankSize / 2 - 2 * ts, -2 * ts, 4 * ts, tankSize / 2 + 2 * ts));
        g.setColor(Color.GRAY);
        g.fill(new Rectangle2D.Double(tankSize / 2 - 1 * ts, -2 * ts, 2 * ts, tankSize / 2));

        g.setTransform(originalTransform);

        // Add subtle glow around tank
        g.setColor(new Color(0, 255, 0, 30));
        g.setStroke(new BasicStroke((float)(3 * scale)));
        g.drawOval((int)(tankX - 4 * scale), (int)(tankY - 4 * scale),
                   (int)(tankSize + 8 * scale), (int)(tankSize + 8 * scale));

        g.dispose();
        return img;
    }
}
