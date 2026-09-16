package com.ejyqyl.dmstudio.model;

import java.util.*;

/**
 * Visual and metadata properties of a menu item.
 */
public class ItemAppearance {

    private String displayName;
    private final List<String> lore = new ArrayList<>();
    private String loreAppendMode; // OVERRIDE, IGNORE, TOP, BOTTOM
    private final List<String> itemFlags = new ArrayList<>();
    private final List<String> enchantments = new ArrayList<>();
    private Boolean unbreakable;
    private Boolean enchantmentGlintOverride;
    private Boolean hideTooltip;
    private String rarity;
    private Integer modelData;
    private String modelDataComponent;
    private String itemModel;
    private String tooltipStyle;
    private String rgb;
    private final List<String> potionEffects = new ArrayList<>();
    private final List<String> bannerMeta = new ArrayList<>();
    private String baseColor;
    private String trimMaterial;
    private String trimPattern;
    private Integer lightLevel;
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public ItemAppearance() {}

    public ItemAppearance copy() {
        ItemAppearance copy = new ItemAppearance();
        copy.displayName = this.displayName;
        copy.lore.addAll(this.lore);
        copy.loreAppendMode = this.loreAppendMode;
        copy.itemFlags.addAll(this.itemFlags);
        copy.enchantments.addAll(this.enchantments);
        copy.unbreakable = this.unbreakable;
        copy.enchantmentGlintOverride = this.enchantmentGlintOverride;
        copy.hideTooltip = this.hideTooltip;
        copy.rarity = this.rarity;
        copy.modelData = this.modelData;
        copy.modelDataComponent = this.modelDataComponent;
        copy.itemModel = this.itemModel;
        copy.tooltipStyle = this.tooltipStyle;
        copy.rgb = this.rgb;
        copy.potionEffects.addAll(this.potionEffects);
        copy.bannerMeta.addAll(this.bannerMeta);
        copy.baseColor = this.baseColor;
        copy.trimMaterial = this.trimMaterial;
        copy.trimPattern = this.trimPattern;
        copy.lightLevel = this.lightLevel;
        copy.unknownFields.putAll(this.unknownFields);
        return copy;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<String> getLore() { return lore; }

    public String getLoreAppendMode() { return loreAppendMode; }
    public void setLoreAppendMode(String loreAppendMode) { this.loreAppendMode = loreAppendMode; }

    public List<String> getItemFlags() { return itemFlags; }

    public List<String> getEnchantments() { return enchantments; }

    public Boolean getUnbreakable() { return unbreakable; }
    public void setUnbreakable(Boolean unbreakable) { this.unbreakable = unbreakable; }

    public Boolean getEnchantmentGlintOverride() { return enchantmentGlintOverride; }
    public void setEnchantmentGlintOverride(Boolean glint) { this.enchantmentGlintOverride = glint; }

    public Boolean getHideTooltip() { return hideTooltip; }
    public void setHideTooltip(Boolean hideTooltip) { this.hideTooltip = hideTooltip; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public Integer getModelData() { return modelData; }
    public void setModelData(Integer modelData) { this.modelData = modelData; }

    public String getModelDataComponent() { return modelDataComponent; }
    public void setModelDataComponent(String modelDataComponent) { this.modelDataComponent = modelDataComponent; }

    public String getItemModel() { return itemModel; }
    public void setItemModel(String itemModel) { this.itemModel = itemModel; }

    public String getTooltipStyle() { return tooltipStyle; }
    public void setTooltipStyle(String tooltipStyle) { this.tooltipStyle = tooltipStyle; }

    public String getRgb() { return rgb; }
    public void setRgb(String rgb) { this.rgb = rgb; }

    public List<String> getPotionEffects() { return potionEffects; }

    public List<String> getBannerMeta() { return bannerMeta; }

    public String getBaseColor() { return baseColor; }
    public void setBaseColor(String baseColor) { this.baseColor = baseColor; }

    public String getTrimMaterial() { return trimMaterial; }
    public void setTrimMaterial(String trimMaterial) { this.trimMaterial = trimMaterial; }

    public String getTrimPattern() { return trimPattern; }
    public void setTrimPattern(String trimPattern) { this.trimPattern = trimPattern; }

    public Integer getLightLevel() { return lightLevel; }
    public void setLightLevel(Integer lightLevel) { this.lightLevel = lightLevel; }

    public Map<String, Object> getUnknownFields() { return unknownFields; }
}
