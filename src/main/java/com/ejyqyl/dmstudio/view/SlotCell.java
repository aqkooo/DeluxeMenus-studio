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

        // 1. Slot Index Label (Top-Left, subtle watermark, shown only when empty)
        slotIndexLabel = new Label(String.valueOf(slotIndex));
        slotIndexLabel.getStyleClass().add("slot-index-label");
        StackPane.setAlignment(slotIndexLabel, Pos.TOP_LEFT);

        // 2. Icon Container
        iconContainer = new StackPane();
        iconContainer.setMouseTransparent(true);

        // 3. Amount Label (Bottom-Right, dropshadow)
        amountLabel = new Label();
        amountLabel.getStyleClass().add("slot-amount-label");
        amountLabel.setMouseTransparent(true);
        StackPane.setAlignment(amountLabel, Pos.BOTTOM_RIGHT);

        // 4. Priority Badge (Bottom-Left)
        priorityBadge = new Label();
        priorityBadge.getStyleClass().add("slot-priority-badge");
        priorityBadge.setMouseTransparent(true);
        StackPane.setAlignment(priorityBadge, Pos.BOTTOM_LEFT);

        getChildren().addAll(slotIndexLabel, iconContainer, amountLabel, priorityBadge);
    }

    private Tooltip currentTooltip;
    private String currentMaterial;

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
            currentMaterial = null;
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
            String mat = primary.getMaterial();
            if (currentMaterial == null || !currentMaterial.equalsIgnoreCase(mat) || iconContainer.getChildren().isEmpty()) {
                Node iconNode = TextureManager.getInstance().createIconNode(mat, 32);
                iconContainer.getChildren().setAll(iconNode);
                currentMaterial = mat;
            }

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
            String title = primary.getAppearance().getDisplayName() != null
                    ? primary.getAppearance().getDisplayName() : primary.getKey();
            String tooltipText = I18nService.getInstance().get("grid.slot_tooltip", slotIndex, title, primary.getPriority());
            if (currentTooltip == null) {
                currentTooltip = new Tooltip(tooltipText);
                Tooltip.install(this, currentTooltip);
            } else {
                currentTooltip.setText(tooltipText);
            }
        }
    }

    public void setFilterHighlight(boolean isMatch, boolean filterActive) {
        if (!filterActive) {
            setOpacity(1.0);
            getStyleClass().remove("inventory-slot-search-match");
        } else if (isMatch) {
            setOpacity(1.0);
            if (!getStyleClass().contains("inventory-slot-search-match")) {
                getStyleClass().add("inventory-slot-search-match");
            }
        } else {
            setOpacity(0.35);
            getStyleClass().remove("inventory-slot-search-match");
        }
    }
}
