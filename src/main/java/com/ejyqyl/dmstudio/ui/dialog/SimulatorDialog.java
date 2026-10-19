package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.project.WorkspaceManager;
import com.ejyqyl.dmstudio.service.render.ColorCodeParser;
import com.ejyqyl.dmstudio.service.simulator.MenuSimulatorEngine;
import com.ejyqyl.dmstudio.service.simulator.MenuSimulatorEngine.SimulationResult;
import com.ejyqyl.dmstudio.service.simulator.SimulatedPlayerState;
import com.ejyqyl.dmstudio.view.SlotCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.*;

/**
 * Interactive dialog controller for live in-game simulation of menus (§Play Mode).
 */
public class SimulatorDialog {

    @FXML private TextField playerField;
    @FXML private Spinner<Double> balanceSpinner;
    @FXML private Button btnResetState;
    @FXML private Label menuTitleLabel;
    @FXML private GridPane simGrid;
    @FXML private TextField newPermField;
    @FXML private Button btnAddPerm;
    @FXML private ListView<String> permissionsList;
    @FXML private TextField phKeyField;
    @FXML private TextField phValField;
    @FXML private Button btnAddPlaceholder;
    @FXML private ListView<String> placeholdersList;
    @FXML private ListView<String> eventLogList;
    @FXML private Button btnClearLog;
    @FXML private Label statusFeedbackLabel;
    @FXML private Button btnClose;

    private MenuConfig activeConfig;
    private SimulatedPlayerState playerState;
    private MenuSimulatorEngine engine;
    private final List<SlotCell> slotCells = new ArrayList<>();
    private Stage stage;

    @FXML
    public void initialize() {
        playerState = new SimulatedPlayerState();

        balanceSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1000000.0, 1000.0, 50.0));
        balanceSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            playerState.setVaultBalance(newV != null ? newV : 0.0);
            refreshGrid();
        });

        playerField.textProperty().addListener((obs, oldV, newV) -> {
            playerState.setPlayerName(newV);
            refreshGrid();
        });

        btnAddPerm.setOnAction(e -> {
            String p = newPermField.getText();
            if (p != null && !p.isBlank()) {
                playerState.addPermission(p.trim());
                newPermField.clear();
                updatePermissionsList();
                refreshGrid();
            }
        });

        permissionsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText("[+] " + item);
                    ContextMenu cm = new ContextMenu();
                    MenuItem remove = new MenuItem(I18nService.getInstance().get("tools.simulator.remove_perm"));
                    remove.setOnAction(ev -> {
                        playerState.removePermission(item);
                        updatePermissionsList();
                        refreshGrid();
                    });
                    cm.getItems().add(remove);
                    setContextMenu(cm);
                }
            }
        });

        if (btnAddPlaceholder != null) {
            btnAddPlaceholder.setOnAction(e -> {
                String k = phKeyField.getText();
                String v = phValField.getText();
                if (k != null && !k.isBlank()) {
                    playerState.setPlaceholder(k.trim(), v != null ? v.trim() : "");
                    phKeyField.clear();
                    phValField.clear();
                    updatePlaceholdersList();
                    refreshGrid();
                }
            });
        }

        if (placeholdersList != null) {
            placeholdersList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setContextMenu(null);
                    } else {
                        setText(item);
                        ContextMenu cm = new ContextMenu();
                        MenuItem remove = new MenuItem(I18nService.getInstance().get("tools.simulator.remove_perm"));
                        remove.setOnAction(ev -> {
                            String key = item.split(" = ")[0].trim();
                            playerState.removePlaceholder(key);
                            updatePlaceholdersList();
                            refreshGrid();
                        });
                        cm.getItems().add(remove);
                        setContextMenu(cm);
                    }
                }
            });
        }

        // Styled monospaced console log
        eventLogList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    String color = "#c9d1d9";
                    if (item.contains("[OK]") || item.contains("[УСПЕХ]")) color = "#2ea043";
                    else if (item.contains("[DENY]") || item.contains("[ОТКАЗ]")) color = "#cf222e";
                    else if (item.contains("[RUN]") || item.contains("[СТАРТ]")) color = "#0969da";
                    else if (item.contains("[INFO]") || item.contains("[ИНФО]")) color = "#787f8f";
                    else if (item.contains("[WARN]") || item.contains("[Предупреждение]")) color = "#d97706";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'JetBrains Mono', Consolas, monospace; -fx-font-size: 11px;");
                }
            }
        });

        btnResetState.setOnAction(e -> {
            playerState = new SimulatedPlayerState();
            playerField.setText(playerState.getPlayerName());
            balanceSpinner.getValueFactory().setValue(playerState.getVaultBalance());
            updatePermissionsList();
            updatePlaceholdersList();
            eventLogList.getItems().clear();
            logEvent(I18nService.getInstance().get("tools.simulator.log_reset"));
            refreshGrid();
        });

        btnClearLog.setOnAction(e -> eventLogList.getItems().clear());
        btnClose.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        updatePermissionsList();
        updatePlaceholdersList();
    }

    public void setConfig(MenuConfig config) {
        this.activeConfig = config;
        this.engine = new MenuSimulatorEngine(config, playerState);

        String title = config.getMenuTitle() != null ? config.getMenuTitle() : "Chest";
        menuTitleLabel.setText(ColorCodeParser.stripColor(title));

        buildGrid();
        refreshGrid();
        logEvent(I18nService.getInstance().get("tools.simulator.log_start", title));
    }

    private void buildGrid() {
        simGrid.getChildren().clear();
        slotCells.clear();

        int totalSlots = activeConfig.getSize() > 0 ? activeConfig.getSize() : 54;
        int rows = Math.max(1, Math.min(6, totalSlots / 9));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                int slotIndex = r * 9 + c;
                if (slotIndex >= totalSlots) break;

                SlotCell cell = new SlotCell(slotIndex);
                cell.setOnMouseClicked(e -> {
                    boolean isLeft = e.getButton() == MouseButton.PRIMARY;
                    handleSimulatedClick(slotIndex, isLeft);
                });

                simGrid.add(cell, c, r);
                slotCells.add(cell);
            }
        }
    }

    private void refreshGrid() {
        if (engine == null || activeConfig == null) return;

        for (SlotCell cell : slotCells) {
            int slot = cell.getSlotIndex();
            com.ejyqyl.dmstudio.model.MenuItem visible = engine.resolveVisibleItem(slot);
            cell.update(visible != null ? List.of(visible) : List.of(), false);
        }
    }

    private void handleSimulatedClick(int slotIndex, boolean isLeftClick) {
        SimulationResult result = engine.handleClick(slotIndex, isLeftClick);
        I18nService i18n = I18nService.getInstance();

        if (result.clickedItem() == null) {
            logEvent(i18n.get("tools.simulator.log_empty_slot", slotIndex));
            return;
        }

        String itemKey = result.clickedItem().getKey();
        String clickType = isLeftClick ? i18n.get("tools.simulator.click_left") : i18n.get("tools.simulator.click_right");

        if (result.success()) {
            logEvent(i18n.get("tools.simulator.log_success", clickType, slotIndex, itemKey));
        } else {
            logEvent(i18n.get("tools.simulator.log_deny", clickType, slotIndex, itemKey));
        }

        for (String msg : result.messages()) {
            logEvent(i18n.get("tools.simulator.log_chat", ColorCodeParser.stripColor(msg)));
        }
        for (String cmd : result.executedCommands()) {
            logEvent(i18n.get("tools.simulator.log_command", cmd));
        }

        if (result.openGuiMenu() != null) {
            String target = result.openGuiMenu();
            logEvent(i18n.get("tools.simulator.log_opengui", target));
            Optional<MenuConfig> next = WorkspaceManager.getInstance().loadMenu(target);
            if (next.isPresent()) {
                setConfig(next.get());
                logEvent(i18n.get("tools.simulator.log_opened", target));
            } else {
                logEvent(i18n.get("tools.simulator.log_not_found", target));
            }
        }

        if (result.shouldClose()) {
            logEvent(i18n.get("tools.simulator.log_close"));
        }

        if (result.shouldRefresh()) {
            refreshGrid();
        }
    }

    private void logEvent(String line) {
        eventLogList.getItems().add(line);
        eventLogList.scrollTo(eventLogList.getItems().size() - 1);
    }

    private void updatePermissionsList() {
        permissionsList.setItems(FXCollections.observableArrayList(playerState.getPermissions()));
    }

    private void updatePlaceholdersList() {
        if (placeholdersList == null) return;
        List<String> list = new ArrayList<>();
        playerState.getPlaceholders().forEach((k, v) -> list.add(k + " = " + v));
        Collections.sort(list);
        placeholdersList.setItems(FXCollections.observableArrayList(list));
    }

    public static void show(Window owner, MenuConfig config) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SimulatorDialog.class.getResource("/fxml/simulator.fxml"),
                    I18nService.getInstance().getBundle()
            );
            Stage stage = new Stage();
            stage.setTitle(I18nService.getInstance().get("tools.simulator.title"));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);

            Scene scene = new Scene(loader.load());
            com.ejyqyl.dmstudio.service.theme.ThemeService.getInstance().applyTheme(scene);

            SimulatorDialog controller = loader.getController();
            controller.stage = stage;
            controller.setConfig(config);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
