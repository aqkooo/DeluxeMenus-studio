package com.ejyqyl.dmstudio.ui.dialog;

import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
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
                    setText("✔ " + item);
                    ContextMenu cm = new ContextMenu();
                    javafx.scene.control.MenuItem remove = new javafx.scene.control.MenuItem("Удалить право");
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

        btnResetState.setOnAction(e -> {
            playerState = new SimulatedPlayerState();
            playerField.setText(playerState.getPlayerName());
            balanceSpinner.getValueFactory().setValue(playerState.getVaultBalance());
            updatePermissionsList();
            eventLogList.getItems().clear();
            logEvent("ℹ Состояние игрока сброшено к начальным значениям.");
            refreshGrid();
        });

        btnClearLog.setOnAction(e -> eventLogList.getItems().clear());
        btnClose.setOnAction(e -> {
            if (stage != null) stage.close();
        });

        updatePermissionsList();
    }

    public void setConfig(MenuConfig config) {
        this.activeConfig = config;
        this.engine = new MenuSimulatorEngine(config, playerState);

        String title = config.getMenuTitle() != null ? config.getMenuTitle() : "Chest";
        menuTitleLabel.setText(ColorCodeParser.stripColor(title));

        buildGrid();
        refreshGrid();
        logEvent("▶ Запущена симуляция меню: " + title);
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
        if (result.clickedItem() == null) {
            logEvent("ℹ Клик по пустому слоту #" + slotIndex);
            return;
        }

        String itemKey = result.clickedItem().getKey();
        String clickType = isLeftClick ? "Левый клик" : "Правый клик";

        if (result.success()) {
            logEvent("✔ [" + clickType + "] Слот #" + slotIndex + " ('" + itemKey + "') - Условия выполнены.");
        } else {
            logEvent("✖ [" + clickType + "] Слот #" + slotIndex + " ('" + itemKey + "') - Отказано в доступе (Deny).");
        }

        for (String msg : result.messages()) {
            logEvent("  💬 [Чат] " + ColorCodeParser.stripColor(msg));
        }
        for (String cmd : result.executedCommands()) {
            logEvent("  ⚙ [Команда] " + cmd);
        }

        if (result.openGuiMenu() != null) {
            String target = result.openGuiMenu();
            logEvent("  🚪 [Переход] [openguimenu] " + target);
            Optional<MenuConfig> next = WorkspaceManager.getInstance().loadMenu(target);
            if (next.isPresent()) {
                setConfig(next.get());
                logEvent("  ✨ Открыто меню: " + target);
            } else {
                logEvent("  ⚠ Меню '" + target + "' не найдено в текущей рабочей области.");
            }
        }

        if (result.shouldClose()) {
            logEvent("  🔒 Меню закрыто командой [close].");
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
            scene.getStylesheets().add(
                    SimulatorDialog.class.getResource("/css/theme-dark.css").toExternalForm()
            );

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
