package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.command.impl.UpdateMenuPropertyCommand;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.render.ColorCodeParser;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Controller for menu-wide properties panel (§6.1).
 */
public class MenuPropertiesController {

    @FXML private TextField titleField;
    @FXML private TextFlow titlePreviewFlow;
    @FXML private ComboBox<InventoryType> typeCombo;
    @FXML private ComboBox<Integer> sizeCombo;
    @FXML private TextField openCommandField;
    @FXML private CheckBox registerCommandCheck;
    @FXML private Spinner<Integer> updateIntervalSpinner;

    private MainViewModel mainViewModel;
    private boolean updating = false;
    private final PauseTransition titleDebounce = new PauseTransition(Duration.millis(350));
    private final PauseTransition openCommandDebounce = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(InventoryType.values()));
        sizeCombo.setItems(FXCollections.observableArrayList(9, 18, 27, 36, 45, 54));

        updateIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        updateIntervalSpinner.setEditable(true);

        titleDebounce.setOnFinished(e -> {
            if (!updating && mainViewModel != null && mainViewModel.getConfig() != null) {
                commitTitle(titleField.getText());
            }
        });

        openCommandDebounce.setOnFinished(e -> {
            if (!updating && mainViewModel != null && mainViewModel.getConfig() != null) {
                commitOpenCommands(openCommandField.getText());
            }
        });

        titleField.textProperty().addListener((obs, oldV, newV) -> {
            ColorCodeParser.populateTextFlow(titlePreviewFlow, newV, javafx.scene.paint.Color.WHITE);
            if (!updating) titleDebounce.playFromStart();
        });

        titleField.focusedProperty().addListener((obs, oldF, newF) -> {
            if (!newF && titleDebounce.getStatus() == Animation.Status.RUNNING) {
                titleDebounce.stop();
                commitTitle(titleField.getText());
            }
        });

        typeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                boolean isChest = newV == InventoryType.CHEST;
                sizeCombo.setDisable(!isChest);
                if (!isChest) {
                    sizeCombo.setValue(newV.getDefaultSize());
                }
                if (!updating) commitType(newV);
            }
        });

        sizeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !updating) commitSize(newV);
        });

        openCommandField.textProperty().addListener((obs, oldV, newV) -> {
            if (!updating) openCommandDebounce.playFromStart();
        });

        openCommandField.focusedProperty().addListener((obs, oldF, newF) -> {
            if (!newF && openCommandDebounce.getStatus() == Animation.Status.RUNNING) {
                openCommandDebounce.stop();
                commitOpenCommands(openCommandField.getText());
            }
        });

        registerCommandCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!updating) commitRegisterCommand(newV);
        });

        updateIntervalSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updating) commitUpdateInterval(newV != null ? newV : 0);
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        loadFromConfig();
        mainViewModel.configProperty().addListener((obs, oldV, newV) -> loadFromConfig());
    }

    public void loadFromConfig() {
        if (mainViewModel == null || mainViewModel.getConfig() == null) return;
        if (titleDebounce.getStatus() == Animation.Status.RUNNING) titleDebounce.stop();
        if (openCommandDebounce.getStatus() == Animation.Status.RUNNING) openCommandDebounce.stop();
        MenuConfig cfg = mainViewModel.getConfig();
        updating = true;
        try {
            titleField.setText(cfg.getMenuTitle());
            typeCombo.setValue(cfg.getMenuType());
            sizeCombo.setValue(cfg.getSize());
            openCommandField.setText(String.join(", ", cfg.getOpenCommands()));
            registerCommandCheck.setSelected(cfg.isRegisterCommand());
            int interval = cfg.getUpdateInterval() != null ? cfg.getUpdateInterval() : 0;
            updateIntervalSpinner.getValueFactory().setValue(interval);
        } finally {
            updating = false;
        }
    }

    private void commitTitle(String newTitle) {
        MenuConfig cfg = mainViewModel.getConfig();
        String oldTitle = cfg.getMenuTitle();
        if (Objects.equals(oldTitle, newTitle)) return;
        mainViewModel.getCommandStack().execute(new UpdateMenuPropertyCommand(
                cfg, "Change menu title",
                c -> c.setMenuTitle(newTitle),
                c -> c.setMenuTitle(oldTitle)
        ));
    }

    private void commitType(InventoryType newType) {
        MenuConfig cfg = mainViewModel.getConfig();
        InventoryType oldType = cfg.getMenuType();
        if (Objects.equals(oldType, newType)) return;
        mainViewModel.getCommandStack().execute(new UpdateMenuPropertyCommand(
                cfg, "Change menu type",
                c -> {
                    c.setMenuType(newType);
                    mainViewModel.getGridViewModel().clampSelectionToBounds();
                },
                c -> {
                    c.setMenuType(oldType);
                    mainViewModel.getGridViewModel().clampSelectionToBounds();
                }
        ));
    }

    private void commitSize(int newSize) {
        MenuConfig cfg = mainViewModel.getConfig();
        int oldSize = cfg.getSize();
        if (oldSize == newSize) return;
        mainViewModel.getCommandStack().execute(new UpdateMenuPropertyCommand(
                cfg, "Change menu size",
                c -> {
                    c.setSize(newSize);
                    mainViewModel.getGridViewModel().clampSelectionToBounds();
                },
                c -> {
                    c.setSize(oldSize);
                    mainViewModel.getGridViewModel().clampSelectionToBounds();
                }
        ));
    }

    private void commitOpenCommands(String raw) {
        MenuConfig cfg = mainViewModel.getConfig();
        List<String> oldCommands = List.copyOf(cfg.getOpenCommands());
        List<String> newCommands = raw != null ? Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList() : List.of();
        if (oldCommands.equals(newCommands)) return;

        mainViewModel.getCommandStack().execute(new UpdateMenuPropertyCommand(
                cfg, "Change open commands",
                c -> { c.getOpenCommands().clear(); c.getOpenCommands().addAll(newCommands); },
                c -> { c.getOpenCommands().clear(); c.getOpenCommands().addAll(oldCommands); }
        ));
    }

    private void commitRegisterCommand(boolean reg) {
        MenuConfig cfg = mainViewModel.getConfig();
        boolean old = cfg.isRegisterCommand();
        if (old == reg) return;
        mainViewModel.getCommandStack().execute(new UpdateMenuPropertyCommand(
                cfg, "Change register command",
                c -> c.setRegisterCommand(reg),
                c -> c.setRegisterCommand(old)
        ));
    }

    private void commitUpdateInterval(int interval) {
        MenuConfig cfg = mainViewModel.getConfig();
        Integer old = cfg.getUpdateInterval();
        Integer newVal = interval > 0 ? interval : null;
        if (Objects.equals(old, newVal)) return;
        mainViewModel.getCommandStack().execute(new UpdateMenuPropertyCommand(
                cfg, "Change update interval",
                c -> c.setUpdateInterval(newVal),
                c -> c.setUpdateInterval(old)
        ));
    }
}
