package com.vibetanks.rendering;

import com.vibetanks.core.Tank;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Renders special visual effects like UFO messages, boss health bar,
 * freeze effects, and the game over laughing skull animation.
 */
public class EffectRenderer {
    private final GraphicsContext gc;
    private final int width;
    private final int height;

    public EffectRenderer(GraphicsContext gc, int width, int height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
    }

    /**
     * Render "Lost it!" message when UFO escapes.
     */
    public void renderUfoLostMessage(int ufoLostMessageTimer) {
        gc.save();

        // Calculate fade effect (fade out in last second)
        double alpha = 1.0;
        if (ufoLostMessageTimer < 60) {
            alpha = ufoLostMessageTimer / 60.0;
        }

        // Pulsing effect
        double pulse = 1.0 + Math.sin(System.currentTimeMillis() / 100.0) * 0.1;
        int fontSize = (int)(50 * pulse);

        // Draw "Lost it!" message in the center of the screen
        gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));

        // Shadow/outline effect
        gc.setFill(Color.rgb(0, 0, 0, alpha * 0.7));
        gc.fillText("Lost it!", width / 2 - 85 + 3, height / 3 + 3);

        // Main text with red color
        gc.setFill(Color.rgb(255, 50, 50, alpha));
        gc.fillText("Lost it!", width / 2 - 85, height / 3);

        // UFO icon above the text
        double iconX = width / 2;
        double iconY = height / 3 - 60;
        gc.setFill(Color.rgb(120, 120, 140, alpha));
        gc.fillOval(iconX - 25, iconY, 50, 20);
        gc.setFill(Color.rgb(150, 200, 255, alpha * 0.7));
        gc.fillOval(iconX - 12, iconY - 15, 24, 20);

        gc.restore();
    }

    /**
     * Render "Zed is dead!" message when UFO is killed.
     */
    public void renderUfoKilledMessage(int ufoKilledMessageTimer) {
        Font savedFont = gc.getFont();

        // Calculate fade effect (fade out in last second)
        double alpha = 1.0;
        if (ufoKilledMessageTimer < 60) {
            alpha = ufoKilledMessageTimer / 60.0;
        }

        int fontSize = 50;

        // Draw "Zed is dead!" message
        gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));

        // Shadow/outline effect
        gc.setFill(Color.rgb(0, 0, 0, alpha * 0.7));
        gc.fillText("Zed is dead!", width / 2 - 130 + 3, height / 3 + 3);

        // Main text with green color (victory!)
        gc.setFill(Color.rgb(50, 255, 50, alpha));
        gc.fillText("Zed is dead!", width / 2 - 130, height / 3);

        // Explosion effect around text (pulsing particles)
        double centerX = width / 2;
        double centerY = height / 3 - 40;
        gc.setFill(Color.rgb(255, 200, 50, alpha * 0.6));
        for (int i = 0; i < 8; i++) {
            double angle = (System.currentTimeMillis() / 50.0 + i * Math.PI / 4) % (2 * Math.PI);
            double dist = 50 + Math.sin(System.currentTimeMillis() / 100.0 + i) * 10;
            double starX = centerX + Math.cos(angle) * dist;
            double starY = centerY + Math.sin(angle) * dist * 0.5;
            gc.fillOval(starX - 5, starY - 5, 10, 10);
        }

        gc.setFont(savedFont);
    }

    /**
     * Render boss health bar at the top of the screen.
     */
    public void renderBossHealthBar(List<Tank> enemyTanks) {
        // Find BOSS tank if alive
        Tank boss = null;
        for (Tank enemy : enemyTanks) {
            if (enemy.isAlive() && enemy.getEnemyType() == Tank.EnemyType.BOSS) {
                boss = enemy;
                break;
            }
        }

        if (boss == null) return;

        // Draw BOSS health bar at the top center of the screen
        double barWidth = 300;
        double barHeight = 20;
        double barX = (width - barWidth) / 2;
        double barY = 10;

        // Background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX - 2, barY - 2, barWidth + 4, barHeight + 4);

        // Health bar background (red)
        gc.setFill(Color.DARKRED);
        gc.fillRect(barX, barY, barWidth, barHeight);

        // Current health (pulsing red like BOSS tank)
        double healthPercent = (double) boss.getHealth() / boss.getMaxHealth();
        double healthWidth = barWidth * healthPercent;

        // Pulsing red color matching BOSS tank
        double pulse = (Math.sin(System.currentTimeMillis() / 150.0) + 1) / 2;
        int red = (int) (150 + pulse * 105);
        int green = (int) (pulse * 50);
        gc.setFill(Color.rgb(red, green, 0));
        gc.fillRect(barX, barY, healthWidth, barHeight);

        // Border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);

        // BOSS label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("BOSS", barX - 50, barY + 15);

        // Health text
        gc.fillText(boss.getHealth() + "/" + boss.getMaxHealth(), barX + barWidth + 10, barY + 15);
    }

    /**
     * Render ice/freeze effect on a tank.
     */
    public void renderFreezeEffect(Tank tank) {
        gc.setFill(Color.rgb(150, 200, 255, 0.5)); // Semi-transparent ice blue
        gc.fillRect(tank.getX(), tank.getY(), tank.getSize(), tank.getSize());

        // Draw snowflake/ice crystals
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        double cx = tank.getX() + tank.getSize() / 2;
        double cy = tank.getY() + tank.getSize() / 2;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * i) / 3;
            gc.strokeLine(cx, cy, cx + 10 * Math.cos(angle), cy + 10 * Math.sin(angle));
        }
    }

    /**
     * Render laughing skull animation for game over screen.
     */
    public void renderLaughingSkull(double centerX, double centerY) {
        double scale = 3.0;
        double time = System.currentTimeMillis() / 100.0;

        // Laughing animation - skull bobs up and down
        double bobY = Math.sin(time * 0.5) * 5;
        double cy = centerY + bobY;

        // Jaw opening animation for laughing
        double jawOpen = (Math.sin(time * 2) + 1) * 8 * scale;

        // Skull background glow (pulsing red)
        double pulse = (Math.sin(time * 0.3) + 1) / 2;
        gc.setFill(Color.rgb((int)(100 + pulse * 100), 0, 0, 0.3));
        gc.fillOval(centerX - 70 * scale, cy - 60 * scale, 140 * scale, 130 * scale);

        // Main skull (cream/bone color)
        gc.setFill(Color.rgb(255, 250, 240));
        gc.fillOval(centerX - 50 * scale, cy - 45 * scale, 100 * scale, 90 * scale);

        // Eye sockets (black with red glow inside)
        gc.setFill(Color.BLACK);
        gc.fillOval(centerX - 30 * scale, cy - 20 * scale, 25 * scale, 30 * scale);
        gc.fillOval(centerX + 5 * scale, cy - 20 * scale, 25 * scale, 30 * scale);

        // Evil red eyes (pulsing)
        gc.setFill(Color.rgb(255, (int)(50 * pulse), 0));
        gc.fillOval(centerX - 25 * scale, cy - 12 * scale, 15 * scale, 15 * scale);
        gc.fillOval(centerX + 10 * scale, cy - 12 * scale, 15 * scale, 15 * scale);

        // Eye highlights
        gc.setFill(Color.YELLOW);
        gc.fillOval(centerX - 22 * scale, cy - 10 * scale, 5 * scale, 5 * scale);
        gc.fillOval(centerX + 13 * scale, cy - 10 * scale, 5 * scale, 5 * scale);

        // Nose hole (triangle)
        gc.setFill(Color.BLACK);
        gc.fillPolygon(
            new double[]{centerX - 8 * scale, centerX + 8 * scale, centerX},
            new double[]{cy + 15 * scale, cy + 15 * scale, cy + 30 * scale},
            3
        );

        // Upper teeth row
        gc.setFill(Color.rgb(255, 250, 240));
        gc.fillRect(centerX - 30 * scale, cy + 32 * scale, 60 * scale, 12 * scale);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        for (int i = 0; i < 6; i++) {
            double toothX = centerX - 25 * scale + i * 10 * scale;
            gc.strokeLine(toothX, cy + 32 * scale, toothX, cy + 44 * scale);
        }

        // Lower jaw (moves for laughing)
        gc.setFill(Color.rgb(240, 235, 225));
        gc.fillRect(centerX - 28 * scale, cy + 32 * scale + jawOpen, 56 * scale, 10 * scale);
        for (int i = 0; i < 5; i++) {
            double toothX = centerX - 23 * scale + i * 10 * scale;
            gc.strokeLine(toothX, cy + 32 * scale + jawOpen, toothX, cy + 42 * scale + jawOpen);
        }

        // Skull outline
        gc.setStroke(Color.rgb(200, 180, 160));
        gc.setLineWidth(2);
        gc.strokeOval(centerX - 50 * scale, cy - 45 * scale, 100 * scale, 90 * scale);

        // Crossbones behind
        gc.setStroke(Color.rgb(255, 250, 240));
        gc.setLineWidth(8 * scale);
        gc.strokeLine(centerX - 80 * scale, cy - 50 * scale, centerX + 80 * scale, cy + 60 * scale);
        gc.strokeLine(centerX + 80 * scale, cy - 50 * scale, centerX - 80 * scale, cy + 60 * scale);

        // Bone ends
        gc.setFill(Color.rgb(255, 250, 240));
        double boneEndSize = 12 * scale;
        gc.fillOval(centerX - 85 * scale, cy - 55 * scale, boneEndSize, boneEndSize);
        gc.fillOval(centerX + 75 * scale, cy - 55 * scale, boneEndSize, boneEndSize);
        gc.fillOval(centerX - 85 * scale, cy + 55 * scale, boneEndSize, boneEndSize);
        gc.fillOval(centerX + 75 * scale, cy + 55 * scale, boneEndSize, boneEndSize);

        // "HA HA HA" text floating around
        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        double textWave = Math.sin(time * 0.8) * 10;
        gc.fillText("HA", centerX - 100, cy - 30 + textWave);
        gc.fillText("HA", centerX + 80, cy - 20 - textWave);
        gc.fillText("HA", centerX - 90, cy + 70 - textWave);
        gc.fillText("HA", centerX + 70, cy + 80 + textWave);
    }
}
