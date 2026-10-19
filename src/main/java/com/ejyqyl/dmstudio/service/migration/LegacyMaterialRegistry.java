package com.ejyqyl.dmstudio.service.migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry mapping Minecraft 1.12 legacy numeric and named materials to modern 1.20+ equivalents.
 */
public final class LegacyMaterialRegistry {

    private static final Map<String, String> EXACT_MAP = new HashMap<>();
    private static final Map<Integer, Map<Integer, String>> DATA_MAP = new HashMap<>();
    private static final String[] COLORS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    static {
        registerColorFamilies();
        registerStoneAndWood();
        registerDyesAndSkulls();
        registerNamedItems();
        registerCommonNumericIds();
    }

    private static void registerColorFamilies() {
        for (int i = 0; i < 16; i++) {
            String color = COLORS[i];
            putData(35, i, color + "_WOOL");
            putData(95, i, color + "_STAINED_GLASS");
            putData(160, i, color + "_STAINED_GLASS_PANE");
            putData(159, i, color + "_TERRACOTTA");
            putData(251, i, color + "_CONCRETE");
            putData(252, i, color + "_CONCRETE_POWDER");
            putData(355, i, color + "_BED");
            putData(171, i, color + "_CARPET");

            EXACT_MAP.put("WOOL_" + color, color + "_WOOL");
            EXACT_MAP.put("STAINED_GLASS_" + color, color + "_STAINED_GLASS");
            EXACT_MAP.put("STAINED_GLASS_PANE_" + color, color + "_STAINED_GLASS_PANE");
            EXACT_MAP.put("CONCRETE_" + color, color + "_CONCRETE");
        }
    }

    private static void registerStoneAndWood() {
        putData(1, 0, "STONE");
        putData(1, 1, "GRANITE");
        putData(1, 2, "POLISHED_GRANITE");
        putData(1, 3, "DIORITE");
        putData(1, 4, "POLISHED_DIORITE");
        putData(1, 5, "ANDESITE");
        putData(1, 6, "POLISHED_ANDESITE");

        String[] woods = {"OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK"};
        for (int i = 0; i < woods.length; i++) {
            putData(5, i, woods[i] + "_PLANKS");
            putData(6, i, woods[i] + "_SAPLING");
            putData(17, i % 4, woods[i % 4] + "_LOG");
            putData(162, i % 2, (i == 0 ? "ACACIA" : "DARK_OAK") + "_LOG");
            putData(18, i % 4, woods[i % 4] + "_LEAVES");
            putData(161, i % 2, (i == 0 ? "ACACIA" : "DARK_OAK") + "_LEAVES");
        }
    }

    private static void registerDyesAndSkulls() {
        String[] dyes = {
                "BLACK_DYE", "RED_DYE", "GREEN_DYE", "COCOA_BEANS",
                "LAPIS_LAZULI", "PURPLE_DYE", "CYAN_DYE", "LIGHT_GRAY_DYE",
                "GRAY_DYE", "PINK_DYE", "LIME_DYE", "YELLOW_DYE",
                "LIGHT_BLUE_DYE", "MAGENTA_DYE", "ORANGE_DYE", "BONE_MEAL"
        };
        for (int i = 0; i < dyes.length; i++) {
            putData(351, i, dyes[i]);
        }

        String[] heads = {
                "SKELETON_SKULL", "WITHER_SKELETON_SKULL", "ZOMBIE_HEAD",
                "PLAYER_HEAD", "CREEPER_HEAD", "DRAGON_HEAD"
        };
        for (int i = 0; i < heads.length; i++) {
            putData(397, i, heads[i]);
        }
    }

    private static void registerNamedItems() {
        EXACT_MAP.put("WORKBENCH", "CRAFTING_TABLE");
        EXACT_MAP.put("WOOD_SWORD", "WOODEN_SWORD");
        EXACT_MAP.put("WOOD_PICKAXE", "WOODEN_PICKAXE");
        EXACT_MAP.put("WOOD_AXE", "WOODEN_AXE");
        EXACT_MAP.put("WOOD_SPADE", "WOODEN_SHOVEL");
        EXACT_MAP.put("WOOD_HOE", "WOODEN_HOE");
        EXACT_MAP.put("GOLD_SWORD", "GOLDEN_SWORD");
        EXACT_MAP.put("GOLD_PICKAXE", "GOLDEN_PICKAXE");
        EXACT_MAP.put("GOLD_AXE", "GOLDEN_AXE");
        EXACT_MAP.put("GOLD_SPADE", "GOLDEN_SHOVEL");
        EXACT_MAP.put("GOLD_HOE", "GOLDEN_HOE");
        EXACT_MAP.put("EXP_BOTTLE", "EXPERIENCE_BOTTLE");
        EXACT_MAP.put("EYE_OF_ENDER", "ENDER_EYE");
        EXACT_MAP.put("SKULL_ITEM", "PLAYER_HEAD");
        EXACT_MAP.put("MOB_SPAWNER", "SPAWNER");
        EXACT_MAP.put("COMMAND", "COMMAND_BLOCK");
        EXACT_MAP.put("COMMAND_MINECART", "COMMAND_BLOCK_MINECART");
        EXACT_MAP.put("SIGN", "OAK_SIGN");
        EXACT_MAP.put("BED", "RED_BED");
        EXACT_MAP.put("BOAT", "OAK_BOAT");
        EXACT_MAP.put("IRON_FENCE", "IRON_BARS");
        EXACT_MAP.put("THIN_GLASS", "GLASS_PANE");
        EXACT_MAP.put("SMOOTH_BRICK", "STONE_BRICKS");
        EXACT_MAP.put("HARD_CLAY", "TERRACOTTA");
        EXACT_MAP.put("GRASS", "GRASS_BLOCK");
        EXACT_MAP.put("SULPHUR", "GUNPOWDER");
        EXACT_MAP.put("PORK", "PORKCHOP");
        EXACT_MAP.put("GRILLED_PORK", "COOKED_PORKCHOP");
        EXACT_MAP.put("RAW_FISH", "COD");
        EXACT_MAP.put("COOKED_FISH", "COOKED_COD");
        EXACT_MAP.put("GOLDEN_APPLE_ENCHANTED", "ENCHANTED_GOLDEN_APPLE");
        EXACT_MAP.put("INK_SACK", "BLACK_DYE");
    }

    private static void registerCommonNumericIds() {
        EXACT_MAP.put("264", "DIAMOND");
        EXACT_MAP.put("265", "IRON_INGOT");
        EXACT_MAP.put("266", "GOLD_INGOT");
        EXACT_MAP.put("388", "EMERALD");
        EXACT_MAP.put("331", "REDSTONE");
        EXACT_MAP.put("348", "GLOWSTONE_DUST");
        EXACT_MAP.put("263", "COAL");
        EXACT_MAP.put("384", "EXPERIENCE_BOTTLE");
        EXACT_MAP.put("345", "COMPASS");
        EXACT_MAP.put("347", "CLOCK");
        EXACT_MAP.put("340", "BOOK");
        EXACT_MAP.put("288", "FEATHER");
        EXACT_MAP.put("166", "BARRIER");
        EXACT_MAP.put("54", "CHEST");
        EXACT_MAP.put("130", "ENDER_CHEST");
        EXACT_MAP.put("145", "ANVIL");
        EXACT_MAP.put("58", "CRAFTING_TABLE");
        EXACT_MAP.put("61", "FURNACE");
        EXACT_MAP.put("267", "IRON_SWORD");
        EXACT_MAP.put("276", "DIAMOND_SWORD");
        EXACT_MAP.put("283", "GOLDEN_SWORD");
        EXACT_MAP.put("268", "WOODEN_SWORD");
        EXACT_MAP.put("272", "STONE_SWORD");
        EXACT_MAP.put("261", "BOW");
        EXACT_MAP.put("262", "ARROW");
        EXACT_MAP.put("368", "ENDER_PEARL");
        EXACT_MAP.put("381", "ENDER_EYE");
        EXACT_MAP.put("322", "GOLDEN_APPLE");
        EXACT_MAP.put("373", "POTION");
        EXACT_MAP.put("438", "SPLASH_POTION");
        EXACT_MAP.put("441", "LINGERING_POTION");
        EXACT_MAP.put("401", "FIREWORK_ROCKET");
        EXACT_MAP.put("403", "ENCHANTED_BOOK");
        EXACT_MAP.put("421", "NAME_TAG");
        EXACT_MAP.put("371", "GOLD_NUGGET");
        EXACT_MAP.put("452", "IRON_NUGGET");
        EXACT_MAP.put("20", "GLASS");
        EXACT_MAP.put("102", "GLASS_PANE");
        EXACT_MAP.put("3", "DIRT");
        EXACT_MAP.put("2", "GRASS_BLOCK");
        EXACT_MAP.put("12", "SAND");
        EXACT_MAP.put("13", "GRAVEL");
        EXACT_MAP.put("4", "COBBLESTONE");
        EXACT_MAP.put("7", "BEDROCK");
    }

    private static void putData(int id, int data, String modern) {
        DATA_MAP.computeIfAbsent(id, k -> new HashMap<>()).put(data, modern);
    }

    public static Optional<String> findModernMaterial(String rawMaterial, Integer data) {
        if (rawMaterial == null || rawMaterial.isBlank()) {
            return Optional.empty();
        }

        String cleaned = rawMaterial.trim().toUpperCase();

        // Check format "35:14" or "35-14" or "160;7"
        if (cleaned.contains(":") || cleaned.contains("-") || cleaned.contains(";")) {
            String[] tokens = cleaned.split("[:\\-;]");
            if (tokens.length >= 2) {
                try {
                    int id = Integer.parseInt(tokens[0].trim());
                    int d = Integer.parseInt(tokens[1].trim());
                    if (DATA_MAP.containsKey(id) && DATA_MAP.get(id).containsKey(d)) {
                        return Optional.of(DATA_MAP.get(id).get(d));
                    }
                } catch (NumberFormatException ignored) {}
                String namePart = tokens[0].trim();
                try {
                    int d = Integer.parseInt(tokens[1].trim());
                    if (d >= 0 && d < 16) {
                        String color = COLORS[d];
                        if (namePart.equals("WOOL")) return Optional.of(color + "_WOOL");
                        if (namePart.equals("STAINED_GLASS")) return Optional.of(color + "_STAINED_GLASS");
                        if (namePart.equals("STAINED_GLASS_PANE")) return Optional.of(color + "_STAINED_GLASS_PANE");
                        if (namePart.equals("CONCRETE")) return Optional.of(color + "_CONCRETE");
                        if (namePart.equals("CONCRETE_POWDER")) return Optional.of(color + "_CONCRETE_POWDER");
                        if (namePart.equals("TERRACOTTA") || namePart.equals("STAINED_CLAY")) return Optional.of(color + "_TERRACOTTA");
                        if (namePart.equals("INK_SACK")) return Optional.ofNullable(DATA_MAP.get(351).get(d));
                        if (namePart.equals("SKULL_ITEM")) return Optional.ofNullable(DATA_MAP.get(397).get(d));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Check if numeric ID with data parameter
        try {
            int numId = Integer.parseInt(cleaned);
            int d = data != null ? data : 0;
            if (DATA_MAP.containsKey(numId) && DATA_MAP.get(numId).containsKey(d)) {
                return Optional.of(DATA_MAP.get(numId).get(d));
            }
        } catch (NumberFormatException ignored) {}

        // Check named with data
        if (data != null && data >= 0 && data < 16) {
            String color = COLORS[data];
            if (cleaned.equals("WOOL")) return Optional.of(color + "_WOOL");
            if (cleaned.equals("STAINED_GLASS")) return Optional.of(color + "_STAINED_GLASS");
            if (cleaned.equals("STAINED_GLASS_PANE")) return Optional.of(color + "_STAINED_GLASS_PANE");
            if (cleaned.equals("CONCRETE")) return Optional.of(color + "_CONCRETE");
            if (cleaned.equals("TERRACOTTA") || cleaned.equals("STAINED_CLAY")) return Optional.of(color + "_TERRACOTTA");
            if (cleaned.equals("INK_SACK")) return Optional.ofNullable(DATA_MAP.get(351).get(data));
            if (cleaned.equals("SKULL_ITEM")) return Optional.ofNullable(DATA_MAP.get(397).get(data));
        }

        // Check exact map
        if (EXACT_MAP.containsKey(cleaned)) {
            return Optional.of(EXACT_MAP.get(cleaned));
        }

        return Optional.empty();
    }
}
