package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.model.ItemAppearance;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.theme.ThemeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog for editing Minecraft 1.20+ Armor Trims, Banner patterns, and Potion effects (§Item Inspector).
 */
public class SpecialPropertiesDialog {

    private static final List<String> TRIM_MATERIALS = List.of(
            "NONE", "NETHERITE", "DIAMOND", "GOLD", "AMETHYST", "COPPER", "REDSTONE", "LAPIS", "EMERALD", "IRON", "QUARTZ"
    );

    private static final List<String> TRIM_PATTERNS = List.of(
            "NONE", "COAST", "DUNE", "EYE", "HOST", "RAISER", "RIB", "SENTRY", "SHAPER", "SILENCE", "SNOUT", "SPIRE", "TIDE", "VEX", "WARD", "WAYFINDER", "WILD"
    );

    private static final List<String> DYES = List.of(
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    );

    private static final List<String> BANNER_PATTERNS = List.of(
            "BASE", "SQUARE_BOTTOM_LEFT", "SQUARE_BOTTOM_RIGHT", "SQUARE_TOP_LEFT", "SQUARE_TOP_RIGHT",
            "STRIPE_BOTTOM", "STRIPE_TOP", "STRIPE_LEFT", "STRIPE_RIGHT", "STRIPE_CENTER", "STRIPE_MIDDLE",
            "STRIPE_DOWNRIGHT", "STRIPE_DOWNLEFT", "SMALL_STRIPES", "CROSS", "STRAIGHT_CROSS",
            "TRIANGLE_BOTTOM", "TRIANGLE_TOP", "TRIANGLES_BOTTOM", "TRIANGLES_TOP",
            "DIAGONAL_LEFT", "DIAGONAL_RIGHT", "DIAGONAL_UP_LEFT", "DIAGONAL_UP_RIGHT",
            "CIRCLE", "RHOMBUS", "HALF_VERTICAL", "HALF_HORIZONTAL", "HALF_VERTICAL_RIGHT", "HALF_HORIZONTAL_BOTTOM",
            "BORDER", "CURLY_BORDER", "CREEPER", "GRADIENT", "GRADIENT_UP", "BRICKS", "SKULL", "FLOWER", "MOJANG", "GLOBE", "PIGLIN"
    );

    private static final List<String> POTION_TYPES = List.of(
            "SPEED", "SLOWNESS", "HASTE", "MINING_FATIGUE", "STRENGTH", "INSTANT_HEALTH", "INSTANT_DAMAGE",
            "JUMP_BOOST", "NAUSEA", "REGENERATION", "RESISTANCE", "FIRE_RESISTANCE", "WATER_BREATHING",
            "INVISIBILITY", "BLINDNESS", "NIGHT_VISION", "HUNGER", "WEAKNESS", "POISON", "WITHER",
            "HEALTH_BOOST", "ABSORPTION", "SATURATION", "GLOWING", "LEVITATION", "LUCK", "UNLUCK", "SLOW_FALLING", "CONDUIT_POWER", "DOLPHINS_GRACE", "BAD_OMEN", "HERO_OF_THE_VILLAGE"
    );

    public static void show(Window owner, ItemAppearance sourceApp, Consumer<ItemAppearance> onApply) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18nService.getInstance().get("dialog.special_props.title"));
        dialog.setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setPrefWidth(520);
        root.getStyleClass().add("root");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        ItemAppearance copy = sourceApp != null ? sourceApp.copy() : new ItemAppearance();

        // 1. Armor Trims Tab
        Tab tabArmor = new Tab(I18nService.getInstance().get("dialog.special_props.tab_armor"));
        VBox armorBox = new VBox(12);
        armorBox.setPadding(new Insets(14));

        GridPane armorGrid = new GridPane();
        armorGrid.setHgap(10);
        armorGrid.setVgap(10);

        ComboBox<String> cbTrimMat = new ComboBox<>(FXCollections.observableArrayList(TRIM_MATERIALS));
        cbTrimMat.setValue(copy.getTrimMaterial() != null ? copy.getTrimMaterial() : "NONE");

        ComboBox<String> cbTrimPat = new ComboBox<>(FXCollections.observableArrayList(TRIM_PATTERNS));
        cbTrimPat.setValue(copy.getTrimPattern() != null ? copy.getTrimPattern() : "NONE");

        armorGrid.add(new Label(I18nService.getInstance().get("dialog.special_props.trim_material")), 0, 0);
        armorGrid.add(cbTrimMat, 1, 0);
        armorGrid.add(new Label(I18nService.getInstance().get("dialog.special_props.trim_pattern")), 0, 1);
        armorGrid.add(cbTrimPat, 1, 1);

        Label trimNote = new Label(I18nService.getInstance().get("dialog.special_props.trim_note"));
        trimNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        armorBox.getChildren().addAll(armorGrid, trimNote);
        tabArmor.setContent(armorBox);

        // 2. Banners Tab
        Tab tabBanner = new Tab(I18nService.getInstance().get("dialog.special_props.tab_banner"));
        VBox bannerBox = new VBox(10);
        bannerBox.setPadding(new Insets(14));

        HBox baseColorBox = new HBox(8);
        baseColorBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> cbBaseColor = new ComboBox<>(FXCollections.observableArrayList(DYES));
        cbBaseColor.setValue(copy.getBaseColor() != null ? copy.getBaseColor() : "WHITE");
        baseColorBox.getChildren().addAll(new Label(I18nService.getInstance().get("dialog.special_props.base_color")), cbBaseColor);

        ObservableList<String> bannerLayers = FXCollections.observableArrayList(copy.getBannerMeta());
        ListView<String> bannerList = new ListView<>(bannerLayers);
        bannerList.setPrefHeight(120);
        bannerList.setPlaceholder(new Label(I18nService.getInstance().get("dialog.special_props.no_layers")));

        HBox addBannerBox = new HBox(6);
        ComboBox<String> cbPat = new ComboBox<>(FXCollections.observableArrayList(BANNER_PATTERNS));
        cbPat.setValue("STRIPE_TOP");
        ComboBox<String> cbDye = new ComboBox<>(FXCollections.observableArrayList(DYES));
        cbDye.setValue("RED");
        Button btnAddLayer = new Button(I18nService.getInstance().get("dialog.special_props.add_layer"));
        btnAddLayer.setOnAction(e -> bannerLayers.add(cbPat.getValue() + ":" + cbDye.getValue()));
        Button btnRemLayer = new Button(I18nService.getInstance().get("dialog.special_props.remove"));
        btnRemLayer.setOnAction(e -> {
            int sel = bannerList.getSelectionModel().getSelectedIndex();
            if (sel >= 0) bannerLayers.remove(sel);
        });
        addBannerBox.getChildren().addAll(cbPat, cbDye, btnAddLayer, btnRemLayer);
        bannerBox.getChildren().addAll(baseColorBox, new Label(I18nService.getInstance().get("dialog.special_props.layers")), bannerList, addBannerBox);
        tabBanner.setContent(bannerBox);

        // 3. Potions Tab
        Tab tabPotion = new Tab(I18nService.getInstance().get("dialog.special_props.tab_potion"));
        VBox potionBox = new VBox(10);
        potionBox.setPadding(new Insets(14));

        ObservableList<String> potionList = FXCollections.observableArrayList(copy.getPotionEffects());
        ListView<String> potListView = new ListView<>(potionList);
        potListView.setPrefHeight(130);
        potListView.setPlaceholder(new Label(I18nService.getInstance().get("dialog.special_props.no_effects")));

        HBox addPotBox = new HBox(6);
        ComboBox<String> cbPotType = new ComboBox<>(FXCollections.observableArrayList(POTION_TYPES));
        cbPotType.setValue("SPEED");
        Spinner<Integer> spDuration = new Spinner<>(1, 3600, 60);
        spDuration.setPrefWidth(70);
        spDuration.setEditable(true);
        Spinner<Integer> spLevel = new Spinner<>(1, 10, 1);
        spLevel.setPrefWidth(60);
        spLevel.setEditable(true);

        Button btnAddPot = new Button(I18nService.getInstance().get("dialog.special_props.add_effect"));
        btnAddPot.setOnAction(e -> potionList.add(cbPotType.getValue() + ";" + spDuration.getValue() + ";" + spLevel.getValue()));
        Button btnRemPot = new Button(I18nService.getInstance().get("dialog.special_props.remove"));
        btnRemPot.setOnAction(e -> {
            int sel = potListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0) potionList.remove(sel);
        });
        addPotBox.getChildren().addAll(cbPotType, spDuration, spLevel, btnAddPot, btnRemPot);
        potionBox.getChildren().addAll(new Label(I18nService.getInstance().get("dialog.special_props.effects")), potListView, addPotBox);
        tabPotion.setContent(potionBox);

        tabPane.getTabs().addAll(tabArmor, tabBanner, tabPotion);

        // Bottom buttons
        Button btnApply = new Button(I18nService.getInstance().get("dialog.special_props.apply"));
        btnApply.setDefaultButton(true);
        btnApply.setOnAction(e -> {
            String tm = cbTrimMat.getValue();
            copy.setTrimMaterial(!"NONE".equals(tm) ? tm : null);
            String tp = cbTrimPat.getValue();
            copy.setTrimPattern(!"NONE".equals(tp) ? tp : null);

            copy.setBaseColor(cbBaseColor.getValue());
            copy.getBannerMeta().clear();
            copy.getBannerMeta().addAll(bannerLayers);

            copy.getPotionEffects().clear();
            copy.getPotionEffects().addAll(potionList);

            if (onApply != null) onApply.accept(copy);
            dialog.close();
        });

        Button btnCancel = new Button(I18nService.getInstance().get("dialog.special_props.cancel"));
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, btnCancel, btnApply);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(tabPane, new Separator(), btnBox);

        Scene scene = new Scene(root);
        ThemeService.getInstance().applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
