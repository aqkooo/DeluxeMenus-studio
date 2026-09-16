package com.ejyqyl.dmstudio.service.export;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.render.ColorCodeParser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Service for rendering high-resolution PNG poster images of menus for Discord, SpigotMC, and Wiki (§Utilities).
 */
public class MenuPosterExportService {

    public static void exportPoster(MenuConfig config, File targetFile) throws IOException {
        int rows = config.getSize() > 0 ? Math.max(1, Math.min(6, config.getSize() / 9)) : 6;
        int cols = 9;

        int slotSize = 48;
        int slotGap = 4;
        int padding = 28;

        int gridW = cols * slotSize + (cols - 1) * slotGap;
        int gridH = rows * slotSize + (rows - 1) * slotGap;

        int canvasW = gridW + padding * 2;
        int headerH = 50;
        int footerH = 34;
        int canvasH = gridH + padding * 2 + headerH + footerH;

        // Render at 2x resolution for retina sharpness
        int scale = 2;
        BufferedImage image = new BufferedImage(canvasW * scale, canvasH * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Canvas Background
        g.setColor(new java.awt.Color(20, 21, 24));
        g.fillRect(0, 0, canvasW, canvasH);

        // 2. Outer Card Frame
        g.setColor(new java.awt.Color(28, 30, 35));
        g.fillRoundRect(padding / 2, padding / 2, canvasW - padding, canvasH - padding, 12, 12);
        g.setColor(new java.awt.Color(44, 47, 56));
        g.drawRoundRect(padding / 2, padding / 2, canvasW - padding, canvasH - padding, 12, 12);

        // 3. Header Title
        String rawTitle = config.getMenuTitle() != null ? config.getMenuTitle() : "Chest";
        String cleanTitle = ColorCodeParser.stripColor(rawTitle);
        g.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.setColor(new java.awt.Color(226, 229, 236));
        g.drawString(cleanTitle, padding + 4, padding + 22);

        // Subtitle / Type
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.setColor(new java.awt.Color(150, 156, 168));
        String typeInfo = (config.getMenuType() != null ? config.getMenuType().name() : "CHEST") + " • " + (rows * 9) + " Slots";
        g.drawString(typeInfo, padding + 4, padding + 38);

        // 4. Slots Workbench
        int startX = padding;
        int startY = padding + headerH;

        g.setColor(new java.awt.Color(22, 23, 27));
        g.fillRoundRect(startX - 6, startY - 6, gridW + 12, gridH + 12, 8, 8);
        g.setColor(new java.awt.Color(38, 41, 48));
        g.drawRoundRect(startX - 6, startY - 6, gridW + 12, gridH + 12, 8, 8);

        // Index items by slot
        Map<Integer, MenuItem> primaryItems = new HashMap<>();
        if (config.getItems() != null) {
            List<MenuItem> sorted = new ArrayList<>(config.getItems().values());
            sorted.sort(Comparator.comparingInt(MenuItem::getPriority));
            for (MenuItem item : sorted) {
                if (item.getSlot() != null && !primaryItems.containsKey(item.getSlot())) {
                    primaryItems.put(item.getSlot(), item);
                }
                if (item.getSlots() != null) {
                    for (String s : item.getSlots()) {
                        if (s.contains("-")) {
                            String[] p = s.split("-");
                            try {
                                int st = Integer.parseInt(p[0].trim());
                                int en = Integer.parseInt(p[1].trim());
                                for (int k = Math.min(st, en); k <= Math.max(st, en); k++) {
                                    primaryItems.putIfAbsent(k, item);
                                }
                            } catch (Exception ignored) {}
                        } else {
                            try {
                                primaryItems.putIfAbsent(Integer.parseInt(s.trim()), item);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }

        // Draw Slots
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slotIdx = r * 9 + c;
                int x = startX + c * (slotSize + slotGap);
                int y = startY + r * (slotSize + slotGap);

                // Sunken slot background
                g.setColor(new java.awt.Color(17, 18, 21));
                g.fillRoundRect(x, y, slotSize, slotSize, 4, 4);

                // Sunken bevel border
                g.setColor(new java.awt.Color(12, 13, 15));
                g.drawLine(x, y, x + slotSize, y);
                g.drawLine(x, y, x, y + slotSize);
                g.setColor(new java.awt.Color(38, 41, 47));
                g.drawLine(x + slotSize, y, x + slotSize, y + slotSize);
                g.drawLine(x, y + slotSize, x + slotSize, y + slotSize);

                MenuItem item = primaryItems.get(slotIdx);
                if (item != null) {
                    // Item background indicator
                    g.setColor(new java.awt.Color(42, 58, 82));
                    g.fillRoundRect(x + 4, y + 4, slotSize - 8, slotSize - 8, 4, 4);

                    // Item Material text abbreviation
                    String mat = item.getMaterial() != null ? item.getMaterial() : "ITM";
                    String abbr = mat.length() <= 3 ? mat : mat.substring(0, 3).toUpperCase(Locale.ROOT);
                    g.setFont(new Font("Monospaced", Font.BOLD, 11));
                    g.setColor(Color.WHITE);
                    FontMetrics fm = g.getFontMetrics();
                    int tw = fm.stringWidth(abbr);
                    g.drawString(abbr, x + (slotSize - tw) / 2, y + (slotSize + fm.getAscent()) / 2 - 2);

                    // Amount if > 1
                    if (item.getAmount() > 1) {
                        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        g.setColor(new java.awt.Color(255, 255, 255));
                        g.drawString(String.valueOf(item.getAmount()), x + slotSize - 12, y + slotSize - 4);
                    }
                } else {
                    // Subtle slot index
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    g.setColor(new java.awt.Color(80, 85, 96));
                    g.drawString(String.valueOf(slotIdx), x + 4, y + 11);
                }
            }
        }

        // 5. Watermark Footer
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.setColor(new java.awt.Color(105, 112, 126));
        String watermark = "DeluxeMenus Studio • Visual Configuration Editor";
        g.drawString(watermark, padding + 4, canvasH - padding + 6);

        g.dispose();

        ImageIO.write(image, "PNG", targetFile);
    }
}
