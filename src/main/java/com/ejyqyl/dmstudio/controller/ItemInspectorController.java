package com.ejyqyl.dmstudio.controller;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ItemAppearance;
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
import java.util.Objects;

/**
 * Controller for Item Inspector accordion (§6.3) with live Minecraft tooltip preview.
 */
public class ItemInspectorController {

    @FXML private TextField itemKeyField, slotsField, dynamicAmountField, displayNameField, modelDataField, itemModelField, rgbField, enchantmentsField;
    @FXML private TextArea loreArea;
    @FXML private ComboBox<String> materialCombo, loreAppendModeCombo;
    @FXML private Button btnBrowseMaterial, btnBrowseHead, btnSpecialProperties;
    @FXML private Spinner<Integer> slotSpinner, prioritySpinner, amountSpinner, damageSpinner;
    @FXML private CheckBox updateCheck, unbreakableCheck, glintOverrideCheck, hideTooltipCheck, flagEnchants, flagAttributes, flagUnbreakable, flagDestroys, flagPlacedOn, flagPotionEffects, flagDye;
    @FXML private VBox tooltipPreviewBox;

    @FXML private ComboBox<String> clickTypeCombo, newActionTypeCombo;
    @FXML private TableView<Action> actionsTable;
    @FXML private TableColumn<Action, String> colActionType, colActionArg, colActionDelay, colActionChance;
    @FXML private TextField newActionArgField;
    @FXML private Spinner<Integer> newActionDelaySpinner, newActionChanceSpinner;
    @FXML private Button btnAddAction, btnDeleteAction;

    @FXML private ComboBox<String> reqTypeCombo;
    @FXML private CheckBox reqInvertedCheck, stopAtSuccessCheck;
    @FXML private TextField reqField1, reqField2;
    @FXML private Spinner<Integer> minReqSpinner;
    @FXML private Button btnSaveRequirement;

    private static final List<String> ALL_FLAGS = List.of(
            "HIDE_ENCHANTS", "HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE", "HIDE_DESTROYS",
            "HIDE_PLACED_ON", "HIDE_POTION_EFFECTS", "HIDE_DYE"
    );

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
        if (btnSpecialProperties != null) {
            btnSpecialProperties.setOnAction(e -> openSpecialProperties());
        }
        debounceTimer.setOnFinished(e -> commitIfChanged());
        setupListeners();
    }

    private void requestDebouncedCommit() {
        if (!updating) debounceTimer.playFromStart();
    }

    private int getSpinnerValue(Spinner<Integer> spinner, int defaultValue) {
        return (spinner != null && spinner.getValue() != null) ? spinner.getValue() : defaultValue;
    }

    private void initMaterials() {
        List<String> mats = CapabilityRegistry.getInstance().getMaterials().stream().map(MaterialDescriptor::id).toList();
        materialCombo.setItems(FXCollections.observableArrayList(mats));
        materialCombo.setEditable(true);
        loreAppendModeCombo.setItems(FXCollections.observableArrayList("OVERRIDE", "IGNORE", "TOP", "BOTTOM"));
        loreAppendModeCombo.setValue("OVERRIDE");

        if (btnBrowseMaterial != null) {
            btnBrowseMaterial.setOnAction(e -> com.ejyqyl.dmstudio.ui.dialog.MaterialPickerDialog.show(
                    materialCombo.getScene().getWindow(), materialCombo.getValue(), materialCombo::setValue));
        }
        if (btnBrowseHead != null) {
            btnBrowseHead.setOnAction(e -> com.ejyqyl.dmstudio.ui.dialog.HeadConfigurationDialog.show(
                    materialCombo.getScene().getWindow(), materialCombo.getValue(), materialCombo::setValue));
        }
    }

    private void initSpinners() {
        Object[][] cfgs = {{slotSpinner, 0, 53, 0}, {prioritySpinner, 0, 999, 0}, {amountSpinner, 1, 64, 1},
                {damageSpinner, 0, 32767, 0}, {newActionDelaySpinner, 0, 1200, 0}, {newActionChanceSpinner, 0, 100, 100}, {minReqSpinner, 0, 10, 0}};
        for (Object[] c : cfgs) {
            @SuppressWarnings("unchecked") Spinner<Integer> s = (Spinner<Integer>) c[0];
            s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory((int) c[1], (int) c[2], (int) c[3]));
            s.setEditable(true);
        }
    }

    private void initClickActions() {
        clickTypeCombo.setItems(FXCollections.observableArrayList("left_click", "right_click", "click", "shift_left_click", "shift_right_click", "middle_click"));
        clickTypeCombo.setValue("left_click");
        List<String> prefixes = CapabilityRegistry.getInstance().getActions().stream().map(ActionDescriptor::prefix).toList();
        newActionTypeCombo.setItems(FXCollections.observableArrayList(prefixes));
        newActionTypeCombo.setValue("[console]");

        colActionType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colActionArg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getArgument()));
        colActionDelay.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDelay() != null ? String.valueOf(d.getValue().getDelay()) : ""));
        colActionChance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChance() != null ? String.valueOf(d.getValue().getChance()) : ""));

        btnAddAction.setOnAction(e -> {
            int delay = getSpinnerValue(newActionDelaySpinner, 0);
            int chance = getSpinnerValue(newActionChanceSpinner, 100);
            Action action = new Action(newActionTypeCombo.getValue(), newActionArgField.getText(), delay > 0 ? delay : null, chance < 100 ? (double) chance : null);
            editorViewModel.addAction(action);
            newActionArgField.clear();
        });

        btnDeleteAction.setOnAction(e -> {
            int sel = actionsTable.getSelectionModel().getSelectedIndex();
            if (sel >= 0) editorViewModel.removeAction(sel);
        });
    }

    private void initRequirements() {
        List<String> reqTypes = CapabilityRegistry.getInstance().getRequirements().stream().map(RequirementDescriptor::type).toList();
        reqTypeCombo.setItems(FXCollections.observableArrayList(reqTypes));
        reqTypeCombo.setValue("has permission");
        btnSaveRequirement.setOnAction(e -> saveRequirement());
    }

    private void setupListeners() {
        displayNameField.textProperty().addListener((o, ov, nv) -> { updateTooltipPreview(); requestDebouncedCommit(); });
        loreArea.textProperty().addListener((o, ov, nv) -> { updateTooltipPreview(); requestDebouncedCommit(); });

        List<TextInputControl> fields = List.of(displayNameField, loreArea, itemKeyField, slotsField, dynamicAmountField, modelDataField, itemModelField, rgbField, enchantmentsField);
        for (TextInputControl f : fields) {
            if (f != displayNameField && f != loreArea) f.textProperty().addListener((o, ov, nv) -> requestDebouncedCommit());
            f.focusedProperty().addListener((obs, of, nf) -> {
                if (!nf && debounceTimer.getStatus() == Animation.Status.RUNNING) {
                    debounceTimer.stop();
                    commitIfChanged();
                }
            });
        }

        materialCombo.valueProperty().addListener((o, ov, nv) -> commitIfChanged());
        loreAppendModeCombo.valueProperty().addListener((o, ov, nv) -> commitIfChanged());
        List.of(slotSpinner, prioritySpinner, amountSpinner, damageSpinner).forEach(s -> s.valueProperty().addListener((o, ov, nv) -> commitIfChanged()));
        List.of(updateCheck, unbreakableCheck, glintOverrideCheck, hideTooltipCheck, flagEnchants, flagAttributes, flagUnbreakable, flagDestroys, flagPlacedOn, flagPotionEffects, flagDye)
                .forEach(c -> c.selectedProperty().addListener((o, ov, nv) -> commitIfChanged()));

        clickTypeCombo.valueProperty().addListener((obs, ov, nv) -> {
            if (editorViewModel != null) {
                editorViewModel.currentClickTypeProperty().set(nv);
                editorViewModel.loadCurrentClickActions();
            }
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.editorViewModel = mainViewModel.getItemEditorViewModel();
        actionsTable.setItems(editorViewModel.getCurrentActions());
        mainViewModel.getGridViewModel().activeItemProperty().addListener((obs, ov, nv) -> loadItem(nv));
        loadItem(mainViewModel.getGridViewModel().getActiveItem());
    }

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
            dynamicAmountField.setText(Objects.toString(item.getDynamicAmount(), ""));
            damageSpinner.getValueFactory().setValue(item.getDamage() != null ? item.getDamage() : 0);
            updateCheck.setSelected(item.isUpdate());

            ItemAppearance app = item.getAppearance();
            displayNameField.setText(Objects.toString(app.getDisplayName(), ""));
            loreArea.setText(String.join("\n", app.getLore()));
            loreAppendModeCombo.setValue(app.getLoreAppendMode() != null ? app.getLoreAppendMode() : "OVERRIDE");
            unbreakableCheck.setSelected(Boolean.TRUE.equals(app.getUnbreakable()));
            glintOverrideCheck.setSelected(Boolean.TRUE.equals(app.getEnchantmentGlintOverride()));
            hideTooltipCheck.setSelected(Boolean.TRUE.equals(app.getHideTooltip()));
            modelDataField.setText(app.getModelData() != null ? String.valueOf(app.getModelData()) : "");
            itemModelField.setText(Objects.toString(app.getItemModel(), ""));
            rgbField.setText(Objects.toString(app.getRgb(), ""));
            enchantmentsField.setText(String.join(", ", app.getEnchantments()));

            List<String> flags = app.getItemFlags();
            List<CheckBox> boxes = getFlagBoxes();
            for (int i = 0; i < ALL_FLAGS.size(); i++) boxes.get(i).setSelected(flags.contains(ALL_FLAGS.get(i)));

            loadRequirementData(item);
            updateTooltipPreview();
        } finally {
            updating = false;
        }
    }

    private void clearAllFields() {
        int selSlot = mainViewModel != null && mainViewModel.getGridViewModel().getSelectedSlot() >= 0
                ? mainViewModel.getGridViewModel().getSelectedSlot() : 0;
        slotSpinner.getValueFactory().setValue(selSlot);
        itemKeyField.setText("");
        itemKeyField.setPromptText("item_" + selSlot);
        slotsField.setText("");
        materialCombo.setValue("STONE");
        displayNameField.setText("");
        loreArea.setText("");
        updateTooltipPreview();
    }

    private void commitIfChanged() {
        if (updating || editorViewModel == null) return;
        syncToViewModel();
        editorViewModel.commitChanges();
    }

    private void syncToViewModel() {
        editorViewModel.keyProperty().set(Objects.toString(itemKeyField.getText(), ""));
        editorViewModel.materialProperty().set(materialCombo.getValue() != null ? materialCombo.getValue() : "STONE");
        editorViewModel.slotProperty().set(getSpinnerValue(slotSpinner, 0));
        editorViewModel.slotsStringProperty().set(Objects.toString(slotsField.getText(), ""));
        editorViewModel.priorityProperty().set(getSpinnerValue(prioritySpinner, 0));
        editorViewModel.amountProperty().set(getSpinnerValue(amountSpinner, 1));
        editorViewModel.dynamicAmountProperty().set(Objects.toString(dynamicAmountField.getText(), ""));
        editorViewModel.damageProperty().set(getSpinnerValue(damageSpinner, 0));
        editorViewModel.updateProperty().set(updateCheck.isSelected());

        editorViewModel.displayNameProperty().set(Objects.toString(displayNameField.getText(), ""));
        editorViewModel.loreTextProperty().set(Objects.toString(loreArea.getText(), ""));
        editorViewModel.loreAppendModeProperty().set(loreAppendModeCombo.getValue() != null ? loreAppendModeCombo.getValue() : "OVERRIDE");
        editorViewModel.unbreakableProperty().set(unbreakableCheck.isSelected());
        editorViewModel.glintOverrideProperty().set(glintOverrideCheck.isSelected());
        editorViewModel.hideTooltipProperty().set(hideTooltipCheck.isSelected());
        editorViewModel.itemModelProperty().set(Objects.toString(itemModelField.getText(), ""));
        editorViewModel.rgbProperty().set(Objects.toString(rgbField.getText(), ""));

        try {
            String raw = modelDataField.getText();
            editorViewModel.modelDataProperty().set((raw != null && !raw.isBlank()) ? Integer.parseInt(raw.trim()) : 0);
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
            for (String line : lore.split("\n", -1)) {
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
            Object v1 = first.getFields().get("permission");
            if (v1 == null) v1 = first.getFields().get("input");
            reqField1.setText(v1 != null ? v1.toString() : "");

            Object v2 = first.getFields().get("amount");
            if (v2 == null) v2 = first.getFields().get("output");
            reqField2.setText(v2 != null ? v2.toString() : "");
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
        if (reqField1.getText() != null && !reqField1.getText().isBlank()) {
            req.setField(type.contains("permission") ? "permission" : "input", reqField1.getText().trim());
        }
        if (reqField2.getText() != null && !reqField2.getText().isBlank()) {
            req.setField(type.contains("money") ? "amount" : "output", reqField2.getText().trim());
        }

        RequirementList list = new RequirementList();
        list.addRequirement("req_1", req);
        if (minReq > 0) list.setMinimumRequirements(minReq);
        if (stopAtSuccess) list.setStopAtSuccess(true);

        item.setViewRequirement(list);
        commitIfChanged();
    }

    private void openSpecialProperties() {
        if (editorViewModel == null) return;
        MenuItem item = editorViewModel.getCurrentItem();
        if (item == null) return;
        com.ejyqyl.dmstudio.ui.dialog.SpecialPropertiesDialog.show(
                btnSpecialProperties.getScene().getWindow(),
                item.getAppearance(),
                updatedApp -> {
                    item.getAppearance().setTrimMaterial(updatedApp.getTrimMaterial());
                    item.getAppearance().setTrimPattern(updatedApp.getTrimPattern());
                    item.getAppearance().setBaseColor(updatedApp.getBaseColor());
                    item.getAppearance().getBannerMeta().clear();
                    item.getAppearance().getBannerMeta().addAll(updatedApp.getBannerMeta());
                    item.getAppearance().getPotionEffects().clear();
                    item.getAppearance().getPotionEffects().addAll(updatedApp.getPotionEffects());
                    commitIfChanged();
                    updateTooltipPreview();
                }
        );
    }
}
