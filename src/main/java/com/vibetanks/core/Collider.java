package com.vibetanks.core;

/**
 * Utility class for collision detection.
 * Provides common AABB (Axis-Aligned Bounding Box) collision methods
 * to reduce code duplication across entity classes.
 */
public final class Collider {
    private Collider() {} // Prevent instantiation

    /**
     * Check if two axis-aligned bounding boxes overlap.
     * @param x1 First box X position
     * @param y1 First box Y position
     * @param w1 First box width
     * @param h1 First box height
     * @param x2 Second box X position
     * @param y2 Second box Y position
     * @param w2 Second box width
     * @param h2 Second box height
     * @return true if boxes overlap
     */
    public static boolean checkAABB(double x1, double y1, double w1, double h1,
                                    double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 &&
               x1 + w1 > x2 &&
               y1 < y2 + h2 &&
               y1 + h1 > y2;
    }

    /**
     * Check if two square entities collide.
     * @param x1 First entity X position
     * @param y1 First entity Y position
     * @param size1 First entity size (width and height)
     * @param x2 Second entity X position
     * @param y2 Second entity Y position
     * @param size2 Second entity size (width and height)
     * @return true if entities collide
     */
    public static boolean checkSquare(double x1, double y1, int size1,
                                      double x2, double y2, int size2) {
        return checkAABB(x1, y1, size1, size1, x2, y2, size2, size2);
    }

    /**
     * Check if a point is inside a rectangle.
     * @param px Point X
     * @param py Point Y
     * @param rx Rectangle X
     * @param ry Rectangle Y
     * @param rw Rectangle width
     * @param rh Rectangle height
     * @return true if point is inside rectangle
     */
    public static boolean pointInRect(double px, double py,
                                      double rx, double ry, double rw, double rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    /**
     * Check if a small entity (bullet) collides with a larger entity (tank).
     * @param bulletX Bullet center X
     * @param bulletY Bullet center Y
     * @param bulletSize Bullet size
     * @param targetX Target X
     * @param targetY Target Y
     * @param targetSize Target size
     * @return true if bullet hits target
     */
    public static boolean bulletHitsTarget(double bulletX, double bulletY, int bulletSize,
                                           double targetX, double targetY, int targetSize) {
        return checkAABB(bulletX, bulletY, bulletSize, bulletSize,
                        targetX, targetY, targetSize, targetSize);
    }

    /**
     * Check if two circular entities collide.
     * @param x1 First entity center X
     * @param y1 First entity center Y
     * @param r1 First entity radius
     * @param x2 Second entity center X
     * @param y2 Second entity center Y
     * @param r2 Second entity radius
     * @return true if circles overlap
     */
    public static boolean checkCircle(double x1, double y1, double r1,
                                      double x2, double y2, double r2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double distSq = dx * dx + dy * dy;
        double radiusSum = r1 + r2;
        return distSq < radiusSum * radiusSum;
    }

    /**
     * Calculate squared distance between two points.
     * Use this instead of actual distance when only comparing (avoids sqrt).
     */
    public static double distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /**
     * Calculate actual distance between two points.
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSquared(x1, y1, x2, y2));
    }

    /**
     * Check if a laser beam (line) intersects with a rectangular target.
     * @param laserStartX Laser start X
     * @param laserStartY Laser start Y
     * @param laserEndX Laser end X
     * @param laserEndY Laser end Y
     * @param laserWidth Laser beam width
     * @param targetX Target X
     * @param targetY Target Y
     * @param targetSize Target size
     * @return true if laser hits target
     */
    public static boolean laserHitsTarget(double laserStartX, double laserStartY,
                                          double laserEndX, double laserEndY,
                                          int laserWidth, double targetX, double targetY, int targetSize) {
        // Simplified: treat laser as a rectangle for AABB collision
        double laserMinX = Math.min(laserStartX, laserEndX);
        double laserMaxX = Math.max(laserStartX, laserEndX);
        double laserMinY = Math.min(laserStartY, laserEndY);
        double laserMaxY = Math.max(laserStartY, laserEndY);

        // Expand by laser width
        if (laserMinX == laserMaxX) {
            // Vertical laser
            laserMinX -= laserWidth / 2.0;
            laserMaxX += laserWidth / 2.0;
        } else {
            // Horizontal laser
            laserMinY -= laserWidth / 2.0;
            laserMaxY += laserWidth / 2.0;
        }

        double laserW = laserMaxX - laserMinX;
        double laserH = laserMaxY - laserMinY;

        return checkAABB(laserMinX, laserMinY, laserW, laserH,
                        targetX, targetY, targetSize, targetSize);
    }
}
