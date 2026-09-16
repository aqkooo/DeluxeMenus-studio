package com.ejyqyl.dmstudio.service.render;

import com.ejyqyl.dmstudio.service.registry.MaterialDescriptor;
import com.ejyqyl.dmstudio.service.registry.MaterialRegistry;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Locale;

/**
 * Provides procedural vector badges for Minecraft materials without third-party textures (§10.2).
 */
public final class ItemIconProvider {

    private static final MaterialRegistry REGISTRY = MaterialRegistry.getInstance();

    private ItemIconProvider() {}

    public static Node createIcon(String rawMaterial, double size) {
        if (rawMaterial == null || rawMaterial.isBlank() || rawMaterial.equalsIgnoreCase("AIR")) {
            StackPane empty = new StackPane();
            empty.setPrefSize(size, size);
            empty.setMaxSize(size, size);
            return empty;
        }

        String trimmed = rawMaterial.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        String abbr = "Itm";
        String hexColor = "#7f8c8d";

        if (trimmed.startsWith("%") && trimmed.endsWith("%")) {
            abbr = "%";
            hexColor = "#4a78a6";
        } else if (lower.startsWith("head-") || lower.startsWith("basehead-")) {
            abbr = "Hed";
            hexColor = "#9a8264";
        } else if (lower.startsWith("hdb-")) {
            abbr = "Hdb";
            hexColor = "#a86c3d";
        } else if (lower.startsWith("itemsadder-")) {
            abbr = "IA";
            hexColor = "#7c5691";
        } else if (lower.startsWith("oraxen-")) {
            abbr = "Orx";
            hexColor = "#2d8575";
        } else if (lower.startsWith("nexo-")) {
            abbr = "Nex";
            hexColor = "#a84a40";
        } else {
            MaterialDescriptor desc = REGISTRY.getMaterial(trimmed);
            if (desc != null) {
                abbr = desc.abbr();
                hexColor = desc.color();
            } else {
                abbr = trimmed.length() <= 3 ? trimmed : trimmed.substring(0, 3);
            }
        }

        StackPane pane = new StackPane();
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);

        Rectangle bg = new Rectangle(size - 4, size - 4);
        bg.setArcWidth(6);
        bg.setArcHeight(6);
        try {
            Color color = Color.web(hexColor);
            bg.setFill(color);
            bg.setStroke(color.darker());
        } catch (Exception e) {
            bg.setFill(Color.web("#7f8c8d"));
            bg.setStroke(Color.web("#505050"));
        }
        bg.setStrokeWidth(1.5);

        Label label = new Label(abbr);
        label.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-font-size: "
                + Math.max(9, (int) (size * 0.32)) + "px; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.8), 2, 0, 0, 1);");
        label.setAlignment(Pos.CENTER);

        pane.getChildren().addAll(bg, label);
        return pane;
    }
}
