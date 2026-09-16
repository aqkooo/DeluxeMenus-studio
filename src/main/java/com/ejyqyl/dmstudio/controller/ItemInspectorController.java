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
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;

import java.util.Arrays;
import java.util.List;

/**
 * Controller for Item Inspector accordion (§6.3) with live Minecraft tooltip preview.
 */
public class ItemInspectorController {

    // Identity
    @FXML private TextField itemKeyField;
    @FXML private ComboBox<String> materialCombo;
    @FXML private Spinner<Integer> slotSpinner;
    @FXML private TextField slotsField;
    @FXML private Spinner<Integer> prioritySpinner;
    @FXML private Spinner<Integer> amountSpinner;
    @FXML private TextField dynamicAmountField;
    @FXML private Spinner<Integer> damageSpinner;
    @FXML private CheckBox updateCheck;

    // Appearance
    @FXML private TextField displayNameField;
    @FXML private TextArea loreArea;
    @FXML private ComboBox<String> loreAppendModeCombo;
    @FXML private CheckBox unbreakableCheck;
    @FXML private CheckBox glintOverrideCheck;
    @FXML private CheckBox hideTooltipCheck;
    @FXML private TextField modelDataField;
    @FXML private TextField itemModelField;
    @FXML private TextField rgbField;
    @FXML private TextField enchantmentsField;

    // Item Flags
    @FXML private CheckBox flagEnchants;
    @FXML private CheckBox flagAttributes;
    @FXML private CheckBox flagUnbreakable;
    @FXML private CheckBox flagDestroys;
    @FXML private CheckBox flagPlacedOn;
    @FXML private CheckBox flagPotionEffects;
    @FXML private CheckBox flagDye;

    // Minecraft Tooltip Preview
    @FXML private VBox tooltipPreviewBox;

    // Click Actions
    @FXML private ComboBox<String> clickTypeCombo;
    @FXML private TableView<Action> actionsTable;
    @FXML private TableColumn<Action, String> colActionType;
    @FXML private TableColumn<Action, String> colActionArg;
    @FXML private TableColumn<Action, String> colActionDelay;
    @FXML private TableColumn<Action, String> colActionChance;
    @FXML private ComboBox<String> newActionTypeCombo;
    @FXML private TextField newActionArgField;
    @FXML private Spinner<Integer> newActionDelaySpinner;
    @FXML private Spinner<Integer> newActionChanceSpinner;
    @FXML private Button btnAddAction;
    @FXML private Button btnDeleteAction;

    // Requirements
    @FXML private ComboBox<String> reqTypeCombo;
    @FXML private CheckBox reqInvertedCheck;
    @FXML private TextField reqField1;
    @FXML private TextField reqField2;
    @FXML private Spinner<Integer> minReqSpinner;
    @FXML private CheckBox stopAtSuccessCheck;
    @FXML private Button btnSaveRequirement;

    private MainViewModel mainViewModel;
    private ItemEditorViewModel editorViewModel;
    private boolean updating = false;

    @FXML
    public void initialize() {
        initMaterials();
        initSpinners();
        initClickActions();
        initRequirements();
        setupListeners();
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
            int delay = newActionDelaySpinner.getValue();
            int chance = newActionChanceSpinner.getValue();
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
            commitIfChanged();
        });
        loreArea.textProperty().addListener((obs, oldV, newV) -> {
            updateTooltipPreview();
            commitIfChanged();
        });

        List.of(itemKeyField, slotsField, dynamicAmountField, modelDataField, itemModelField, rgbField, enchantmentsField)
                .forEach(f -> f.textProperty().addListener((o, ov, nv) -> commitIfChanged()));

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

    private void loadItem(MenuItem item) {
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
            flagEnchants.setSelected(flags.contains("HIDE_ENCHANTS"));
            flagAttributes.setSelected(flags.contains("HIDE_ATTRIBUTES"));
            flagUnbreakable.setSelected(flags.contains("HIDE_UNBREAKABLE"));
            flagDestroys.setSelected(flags.contains("HIDE_DESTROYS"));
            flagPlacedOn.setSelected(flags.contains("HIDE_PLACED_ON"));
            flagPotionEffects.setSelected(flags.contains("HIDE_POTION_EFFECTS"));
            flagDye.setSelected(flags.contains("HIDE_DYE"));

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
        editorViewModel.keyProperty().set(itemKeyField.getText());
        editorViewModel.materialProperty().set(materialCombo.getValue() != null ? materialCombo.getValue() : "STONE");
        editorViewModel.slotProperty().set(slotSpinner.getValue());
        editorViewModel.slotsStringProperty().set(slotsField.getText());
        editorViewModel.priorityProperty().set(prioritySpinner.getValue());
        editorViewModel.amountProperty().set(amountSpinner.getValue());
        editorViewModel.dynamicAmountProperty().set(dynamicAmountField.getText());
        editorViewModel.damageProperty().set(damageSpinner.getValue());
        editorViewModel.updateProperty().set(updateCheck.isSelected());

        editorViewModel.displayNameProperty().set(displayNameField.getText());
        editorViewModel.loreTextProperty().set(loreArea.getText());
        editorViewModel.loreAppendModeProperty().set(loreAppendModeCombo.getValue());
        editorViewModel.unbreakableProperty().set(unbreakableCheck.isSelected());
        editorViewModel.glintOverrideProperty().set(glintOverrideCheck.isSelected());
        editorViewModel.hideTooltipProperty().set(hideTooltipCheck.isSelected());
        editorViewModel.itemModelProperty().set(itemModelField.getText());
        editorViewModel.rgbProperty().set(rgbField.getText());

        try {
            int md = Integer.parseInt(modelDataField.getText().trim());
            editorViewModel.modelDataProperty().set(md);
        } catch (NumberFormatException e) {
            editorViewModel.modelDataProperty().set(0);
        }

        // Flags
        editorViewModel.getSelectedFlags().clear();
        if (flagEnchants.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_ENCHANTS");
        if (flagAttributes.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_ATTRIBUTES");
        if (flagUnbreakable.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_UNBREAKABLE");
        if (flagDestroys.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_DESTROYS");
        if (flagPlacedOn.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_PLACED_ON");
        if (flagPotionEffects.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_POTION_EFFECTS");
        if (flagDye.isSelected()) editorViewModel.getSelectedFlags().add("HIDE_DYE");

        // Enchantments
        editorViewModel.getEnchantments().clear();
        String enchRaw = enchantmentsField.getText().trim();
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
            placeholder.setStyle("-fx-font-style: italic; -fx-text-fill: #555566; -fx-font-size: 11px;");
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
        int minReq = minReqSpinner.getValue();
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
