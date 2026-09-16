package com.ejyqyl.dmstudio.view;

import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.texture.TextureManager;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * Reusable visual slot cell with sunken Minecraft styling, priority badges, and texture rendering (§6.2).
 */
public class SlotCell extends StackPane {

    private final int slotIndex;
    private final Label slotIndexLabel;
    private final StackPane iconContainer;
    private final Label amountLabel;
    private final Label priorityBadge;

    public SlotCell(int slotIndex) {
        this.slotIndex = slotIndex;

        setPrefSize(48, 48);
        setMinSize(48, 48);
        setMaxSize(48, 48);
        getStyleClass().add("inventory-slot");

        // 1. Slot Index Label (Top-Left, ~40% opacity, shown only when empty)
        slotIndexLabel = new Label(String.valueOf(slotIndex));
        slotIndexLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(227, 229, 232, 0.4); -fx-padding: 3 0 0 4;");
        StackPane.setAlignment(slotIndexLabel, Pos.TOP_LEFT);

        // 2. Icon Container
        iconContainer = new StackPane();
        iconContainer.setMouseTransparent(true);

        // 3. Amount Label (Bottom-Right, dropshadow)
        amountLabel = new Label();
        amountLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-effect: dropshadow(one-pass-box, black, 2, 0, 1, 1); -fx-padding: 0 3 2 0;");
        amountLabel.setMouseTransparent(true);
        StackPane.setAlignment(amountLabel, Pos.BOTTOM_RIGHT);

        // 4. Priority Badge (Bottom-Left or Bottom-Right)
        priorityBadge = new Label();
        priorityBadge.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #ffd166; -fx-background-color: rgba(0,0,0,0.75); -fx-padding: 1 3 1 3; -fx-background-radius: 2;");
        priorityBadge.setMouseTransparent(true);
        StackPane.setAlignment(priorityBadge, Pos.BOTTOM_LEFT);

        getChildren().addAll(slotIndexLabel, iconContainer, amountLabel, priorityBadge);
    }

    private Tooltip currentTooltip;

    public int getSlotIndex() {
        return slotIndex;
    }

    public void update(List<MenuItem> items, boolean isSelected) {
        // Handle selection style class
        if (isSelected) {
            if (!getStyleClass().contains("inventory-slot-selected")) {
                getStyleClass().add("inventory-slot-selected");
            }
        } else {
            getStyleClass().remove("inventory-slot-selected");
        }

        // Handle collision (multiple items with equal priority)
        boolean hasCollision = false;
        if (items.size() > 1) {
            int p0 = items.get(0).getPriority();
            for (int i = 1; i < items.size(); i++) {
                if (items.get(i).getPriority() == p0) {
                    hasCollision = true;
                    break;
                }
            }
        }
        if (hasCollision) {
            if (!getStyleClass().contains("inventory-slot-collision")) {
                getStyleClass().add("inventory-slot-collision");
            }
        } else {
            getStyleClass().remove("inventory-slot-collision");
        }

        if (items.isEmpty()) {
            // Empty slot state
            slotIndexLabel.setVisible(true);
            iconContainer.getChildren().clear();
            amountLabel.setVisible(false);
            priorityBadge.setVisible(false);
            if (currentTooltip != null) {
                Tooltip.uninstall(this, currentTooltip);
                currentTooltip = null;
            }
        } else {
            // Occupied slot state: hide index number
            slotIndexLabel.setVisible(false);

            MenuItem primary = items.get(0);
            Node iconNode = TextureManager.getInstance().createIconNode(primary.getMaterial(), 34);
            iconContainer.getChildren().setAll(iconNode);

            // Amount
            if (primary.getAmount() > 1) {
                amountLabel.setText(String.valueOf(primary.getAmount()));
                amountLabel.setVisible(true);
            } else {
                amountLabel.setVisible(false);
            }

            // Priority badge
            if (items.size() > 1 || primary.getPriority() > 0) {
                priorityBadge.setText("P:" + primary.getPriority());
                priorityBadge.setVisible(true);
            } else {
                priorityBadge.setVisible(false);
            }

            // Tooltip
            if (currentTooltip != null) {
                Tooltip.uninstall(this, currentTooltip);
            }
            String title = primary.getAppearance().getDisplayName() != null
                    ? primary.getAppearance().getDisplayName() : primary.getKey();
            currentTooltip = new Tooltip(I18nService.getInstance().get("grid.slot_tooltip", slotIndex, title, primary.getPriority()));
            Tooltip.install(this, currentTooltip);
        }
    }
}
