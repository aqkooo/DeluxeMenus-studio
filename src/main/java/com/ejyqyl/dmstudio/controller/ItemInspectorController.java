package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.service.registry.ActionDescriptor;
import com.ejyqyl.dmstudio.service.registry.CapabilityRegistry;
import com.ejyqyl.dmstudio.service.registry.MaterialDescriptor;
import com.ejyqyl.dmstudio.service.registry.RequirementDescriptor;
import com.ejyqyl.dmstudio.service.render.ColorCodeParser;
import com.ejyqyl.dmstudio.viewmodel.ItemEditorViewModel;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

/**
 * Controller for Item Inspector accordion (§6.3) with live Minecraft tooltip preview.
 */
public class ItemInspectorController {

    // Identity & Appearance Controls
    @FXML private TextField itemKeyField, slotsField, dynamicAmountField, displayNameField, modelDataField, itemModelField, rgbField, enchantmentsField;
    @FXML private TextArea loreArea;
    @FXML private ComboBox<String> materialCombo, loreAppendModeCombo;
    @FXML private Spinner<Integer> slotSpinner, prioritySpinner, amountSpinner, damageSpinner;
    @FXML private CheckBox updateCheck, unbreakableCheck, glintOverrideCheck, hideTooltipCheck;

    // Item Flags
    @FXML private CheckBox flagEnchants, flagAttributes, flagUnbreakable, flagDestroys, flagPlacedOn, flagPotionEffects, flagDye;

    // Minecraft Tooltip Preview
    @FXML private VBox tooltipPreviewBox;

    // Click Actions
    @FXML private ComboBox<String> clickTypeCombo, newActionTypeCombo;
    @FXML private TableView<Action> actionsTable;
    @FXML private TableColumn<Action, String> colActionType, colActionArg, colActionDelay, colActionChance;
    @FXML private TextField newActionArgField;
    @FXML private Spinner<Integer> newActionDelaySpinner, newActionChanceSpinner;
    @FXML private Button btnAddAction, btnDeleteAction;

    // Requirements
    @FXML private ComboBox<String> reqTypeCombo;
    @FXML private CheckBox reqInvertedCheck, stopAtSuccessCheck;
    @FXML private TextField reqField1, reqField2;
    @FXML private Spinner<Integer> minReqSpinner;
    @FXML private Button btnSaveRequirement;

    private MainViewModel mainViewModel;
    private ItemEditorViewModel editorViewModel;
    private boolean updating = false;
    private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        initMaterials();
        initSpinners();
        initClickActions();
        initRequirements();
        debounceTimer.setOnFinished(e -> commitIfChanged());
        setupListeners();
    }

    private void requestDebouncedCommit() {
        if (updating) return;
        debounceTimer.playFromStart();
    }

    private int getSpinnerValue(Spinner<Integer> spinner, int defaultValue) {
        if (spinner == null || spinner.getValue() == null) return defaultValue;
        return spinner.getValue();
    }

    private void initMaterials() {
        List<String> mats = CapabilityRegistry.getInstance().getMaterials().stream()
                .map(MaterialDescriptor::id).toList();
        materialCombo.setItems(FXCollections.observableArrayList(mats));
        materialCombo.setEditable(true);

        loreAppendModeCombo.setItems(FXCollections.observableArrayList("OVERRIDE", "IGNORE", "TOP", "BOTTOM"));
        loreAppendModeCombo.setValue("OVERRIDE");
    }

    private void initSpinners() {
        slotSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 53, 0));
        prioritySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 0));
        amountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 64, 1));
        damageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 32767, 0));
        newActionDelaySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1200, 0));
        newActionChanceSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 100));
        minReqSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));

        List.of(slotSpinner, prioritySpinner, amountSpinner, damageSpinner, newActionDelaySpinner, newActionChanceSpinner, minReqSpinner)
                .forEach(s -> s.setEditable(true));
    }

    private void initClickActions() {
        clickTypeCombo.setItems(FXCollections.observableArrayList(
                "left_click", "right_click", "click", "shift_left_click", "shift_right_click", "middle_click"
        ));
        clickTypeCombo.setValue("left_click");

        List<String> actionPrefixes = CapabilityRegistry.getInstance().getActions().stream()
                .map(ActionDescriptor::prefix).toList();
        newActionTypeCombo.setItems(FXCollections.observableArrayList(actionPrefixes));
        newActionTypeCombo.setValue("[console]");

        colActionType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colActionArg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getArgument()));
        colActionDelay.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDelay() != null ? String.valueOf(d.getValue().getDelay()) : ""));
        colActionChance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChance() != null ? String.valueOf(d.getValue().getChance()) : ""));

        btnAddAction.setOnAction(e -> {
            String type = newActionTypeCombo.getValue();
            String arg = newActionArgField.getText();
            int delay = getSpinnerValue(newActionDelaySpinner, 0);
            int chance = getSpinnerValue(newActionChanceSpinner, 100);
            Action action = new Action(type, arg, delay > 0 ? delay : null, chance < 100 ? (double) chance : null);
            editorViewModel.addAction(action);
            newActionArgField.clear();
        });

        btnDeleteAction.setOnAction(e -> {
            int selected = actionsTable.getSelectionModel().getSelectedIndex();
            if (selected >= 0) {
                editorViewModel.removeAction(selected);
            }
        });
    }

    private void initRequirements() {
        List<String> reqTypes = CapabilityRegistry.getInstance().getRequirements().stream()
                .map(RequirementDescriptor::type).toList();
        reqTypeCombo.setItems(FXCollections.observableArrayList(reqTypes));
        reqTypeCombo.setValue("has permission");

        btnSaveRequirement.setOnAction(e -> saveRequirement());
    }

    private void setupListeners() {
        displayNameField.textProperty().addListener((obs, oldV, newV) -> {
            updateTooltipPreview();
            requestDebouncedCommit();
        });
        loreArea.textProperty().addListener((obs, oldV, newV) -> {
            updateTooltipPreview();
            requestDebouncedCommit();
        });

        List<TextInputControl> textControls = List.of(displayNameField, loreArea, itemKeyField, slotsField,
                dynamicAmountField, modelDataField, itemModelField, rgbField, enchantmentsField);
        for (TextInputControl f : textControls) {
            if (f != displayNameField && f != loreArea) {
                f.textProperty().addListener((o, ov, nv) -> requestDebouncedCommit());
            }
            f.focusedProperty().addListener((obs, oldF, newF) -> {
                if (!newF && debounceTimer.getStatus() == Animation.Status.RUNNING) {
                    debounceTimer.stop();
                    commitIfChanged();
                }
            });
        }

        materialCombo.valueProperty().addListener((o, ov, nv) -> commitIfChanged());
        loreAppendModeCombo.valueProperty().addListener((o, ov, nv) -> commitIfChanged());

        List.of(slotSpinner, prioritySpinner, amountSpinner, damageSpinner)
                .forEach(s -> s.valueProperty().addListener((o, ov, nv) -> commitIfChanged()));

        List.of(updateCheck, unbreakableCheck, glintOverrideCheck, hideTooltipCheck,
                flagEnchants, flagAttributes, flagUnbreakable, flagDestroys, flagPlacedOn, flagPotionEffects, flagDye)
                .forEach(c -> c.selectedProperty().addListener((o, ov, nv) -> commitIfChanged()));

        clickTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (editorViewModel != null) {
                editorViewModel.currentClickTypeProperty().set(newV);
                editorViewModel.loadCurrentClickActions();
            }
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.editorViewModel = mainViewModel.getItemEditorViewModel();
        actionsTable.setItems(editorViewModel.getCurrentActions());

        mainViewModel.getGridViewModel().activeItemProperty().addListener((obs, oldV, newV) -> loadItem(newV));
        loadItem(mainViewModel.getGridViewModel().getActiveItem());
    }

    private static final List<String> ALL_FLAGS = List.of(
            "HIDE_ENCHANTS", "HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE", "HIDE_DESTROYS",
            "HIDE_PLACED_ON", "HIDE_POTION_EFFECTS", "HIDE_DYE"
    );

    private List<CheckBox> getFlagBoxes() {
        return List.of(flagEnchants, flagAttributes, flagUnbreakable, flagDestroys, flagPlacedOn, flagPotionEffects, flagDye);
    }

    private void loadItem(MenuItem item) {
        if (debounceTimer.getStatus() == Animation.Status.RUNNING) {
            debounceTimer.stop();
            commitIfChanged();
        }
        updating = true;
        try {
            if (item == null) {
                clearAllFields();
                return;
            }
            itemKeyField.setText(item.getKey());
            materialCombo.setValue(item.getMaterial());
            slotSpinner.getValueFactory().setValue(item.getSlot() != null ? item.getSlot() : 0);
            slotsField.setText(String.join(", ", item.getSlots()));
            prioritySpinner.getValueFactory().setValue(item.getPriority());
            amountSpinner.getValueFactory().setValue(item.getAmount());
            dynamicAmountField.setText(item.getDynamicAmount() != null ? item.getDynamicAmount() : "");
            damageSpinner.getValueFactory().setValue(item.getDamage() != null ? item.getDamage() : 0);
            updateCheck.setSelected(item.isUpdate());

            displayNameField.setText(item.getAppearance().getDisplayName() != null ? item.getAppearance().getDisplayName() : "");
            loreArea.setText(String.join("\n", item.getAppearance().getLore()));
            loreAppendModeCombo.setValue(item.getAppearance().getLoreAppendMode() != null ? item.getAppearance().getLoreAppendMode() : "OVERRIDE");
            unbreakableCheck.setSelected(Boolean.TRUE.equals(item.getAppearance().getUnbreakable()));
            glintOverrideCheck.setSelected(Boolean.TRUE.equals(item.getAppearance().getEnchantmentGlintOverride()));
            hideTooltipCheck.setSelected(Boolean.TRUE.equals(item.getAppearance().getHideTooltip()));
            modelDataField.setText(item.getAppearance().getModelData() != null ? String.valueOf(item.getAppearance().getModelData()) : "");
            itemModelField.setText(item.getAppearance().getItemModel() != null ? item.getAppearance().getItemModel() : "");
            rgbField.setText(item.getAppearance().getRgb() != null ? item.getAppearance().getRgb() : "");
            enchantmentsField.setText(String.join(", ", item.getAppearance().getEnchantments()));

            List<String> flags = item.getAppearance().getItemFlags();
            List<CheckBox> boxes = getFlagBoxes();
            for (int i = 0; i < ALL_FLAGS.size(); i++) {
                boxes.get(i).setSelected(flags.contains(ALL_FLAGS.get(i)));
            }

            loadRequirementData(item);
            updateTooltipPreview();
        } finally {
            updating = false;
        }
    }

    private void clearAllFields() {
        itemKeyField.setText("");
        materialCombo.setValue("STONE");
        displayNameField.setText("");
        loreArea.setText("");
        updateTooltipPreview();
    }

    private void commitIfChanged() {
        if (updating || editorViewModel == null || editorViewModel.getCurrentItem() == null) return;
        syncToViewModel();
        editorViewModel.commitChanges();
    }

    private void syncToViewModel() {
        editorViewModel.keyProperty().set(itemKeyField.getText() != null ? itemKeyField.getText() : "");
        editorViewModel.materialProperty().set(materialCombo.getValue() != null ? materialCombo.getValue() : "STONE");
        editorViewModel.slotProperty().set(getSpinnerValue(slotSpinner, 0));
        editorViewModel.slotsStringProperty().set(slotsField.getText() != null ? slotsField.getText() : "");
        editorViewModel.priorityProperty().set(getSpinnerValue(prioritySpinner, 0));
        editorViewModel.amountProperty().set(getSpinnerValue(amountSpinner, 1));
        editorViewModel.dynamicAmountProperty().set(dynamicAmountField.getText() != null ? dynamicAmountField.getText() : "");
        editorViewModel.damageProperty().set(getSpinnerValue(damageSpinner, 0));
        editorViewModel.updateProperty().set(updateCheck.isSelected());

        editorViewModel.displayNameProperty().set(displayNameField.getText() != null ? displayNameField.getText() : "");
        editorViewModel.loreTextProperty().set(loreArea.getText() != null ? loreArea.getText() : "");
        editorViewModel.loreAppendModeProperty().set(loreAppendModeCombo.getValue() != null ? loreAppendModeCombo.getValue() : "OVERRIDE");
        editorViewModel.unbreakableProperty().set(unbreakableCheck.isSelected());
        editorViewModel.glintOverrideProperty().set(glintOverrideCheck.isSelected());
        editorViewModel.hideTooltipProperty().set(hideTooltipCheck.isSelected());
        editorViewModel.itemModelProperty().set(itemModelField.getText() != null ? itemModelField.getText() : "");
        editorViewModel.rgbProperty().set(rgbField.getText() != null ? rgbField.getText() : "");

        try {
            int md = Integer.parseInt(modelDataField.getText().trim());
            editorViewModel.modelDataProperty().set(md);
        } catch (Exception e) {
            editorViewModel.modelDataProperty().set(0);
        }

        editorViewModel.getSelectedFlags().clear();
        List<CheckBox> boxes = getFlagBoxes();
        for (int i = 0; i < ALL_FLAGS.size(); i++) {
            if (boxes.get(i).isSelected()) editorViewModel.getSelectedFlags().add(ALL_FLAGS.get(i));
        }

        editorViewModel.getEnchantments().clear();
        String enchRaw = enchantmentsField.getText() != null ? enchantmentsField.getText().trim() : "";
        if (!enchRaw.isEmpty()) {
            for (String e : enchRaw.split("[,\\s]+")) {
                if (!e.isBlank()) editorViewModel.getEnchantments().add(e.trim());
            }
        }
    }

    private void updateTooltipPreview() {
        tooltipPreviewBox.getChildren().clear();
        String name = displayNameField.getText();
        String lore = loreArea.getText();
        boolean hasName = name != null && !name.isBlank();
        boolean hasLore = lore != null && !lore.isBlank();

        if (!hasName && !hasLore) {
            Label placeholder = new Label(I18nService.getInstance().get("inspector.tooltip_empty"));
            placeholder.setStyle("-fx-font-style: italic; -fx-text-fill: #787f8f; -fx-font-size: 11px;");
            tooltipPreviewBox.getChildren().add(placeholder);
            return;
        }

        if (hasName) {
            TextFlow nameFlow = new TextFlow();
            ColorCodeParser.populateTextFlow(nameFlow, name, Color.WHITE);
            tooltipPreviewBox.getChildren().add(nameFlow);
        }

        if (hasLore) {
            String[] lines = lore.split("\n", -1);
            for (String line : lines) {
                TextFlow lineFlow = new TextFlow();
                ColorCodeParser.populateTextFlow(lineFlow, line, Color.web("#AAAAAA"));
                tooltipPreviewBox.getChildren().add(lineFlow);
            }
        }
    }

    private void loadRequirementData(MenuItem item) {
        RequirementList viewReq = item.getViewRequirement();
        if (viewReq != null && !viewReq.getRequirements().isEmpty()) {
            Requirement first = viewReq.getRequirements().values().iterator().next();
            reqTypeCombo.setValue(first.getType());
            reqInvertedCheck.setSelected(first.isInverted());
            minReqSpinner.getValueFactory().setValue(viewReq.getMinimumRequirements() != null ? viewReq.getMinimumRequirements() : 0);
            stopAtSuccessCheck.setSelected(Boolean.TRUE.equals(viewReq.getStopAtSuccess()));
            reqField1.setText(String.valueOf(first.getFields().getOrDefault("permission", first.getFields().getOrDefault("input", ""))));
            reqField2.setText(String.valueOf(first.getFields().getOrDefault("amount", first.getFields().getOrDefault("output", ""))));
        } else {
            reqInvertedCheck.setSelected(false);
            minReqSpinner.getValueFactory().setValue(0);
            stopAtSuccessCheck.setSelected(false);
            reqField1.clear();
            reqField2.clear();
        }
    }

    private void saveRequirement() {
        MenuItem item = editorViewModel.getCurrentItem();
        if (item == null) return;

        String type = reqTypeCombo.getValue();
        boolean inverted = reqInvertedCheck.isSelected();
        int minReq = getSpinnerValue(minReqSpinner, 0);
        boolean stopAtSuccess = stopAtSuccessCheck.isSelected();

        Requirement req = new Requirement(type);
        req.setInverted(inverted);
        if (!reqField1.getText().isBlank()) {
            req.setField(type.contains("permission") ? "permission" : "input", reqField1.getText().trim());
        }
        if (!reqField2.getText().isBlank()) {
            req.setField(type.contains("money") ? "amount" : "output", reqField2.getText().trim());
        }

        RequirementList list = new RequirementList();
        list.addRequirement("req_1", req);
        if (minReq > 0) list.setMinimumRequirements(minReq);
        if (stopAtSuccess) list.setStopAtSuccess(true);

        item.setViewRequirement(list);
        commitIfChanged();
    }
}
