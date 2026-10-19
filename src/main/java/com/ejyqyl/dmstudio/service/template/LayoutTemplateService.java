package com.ejyqyl.dmstudio.service.template;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.command.impl.BatchCommand;
import com.ejyqyl.dmstudio.command.impl.RemoveItemCommand;
import com.ejyqyl.dmstudio.command.impl.SetItemCommand;
import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service managing layout templates and user-defined structural presets.
 */
public class LayoutTemplateService {

    private static final LayoutTemplateService INSTANCE = new LayoutTemplateService();

    public static LayoutTemplateService getInstance() {
        return INSTANCE;
    }

    private final List<LayoutTemplate> builtInTemplates = new ArrayList<>();
    private final List<LayoutTemplate> customTemplates = new ArrayList<>();
    private final Path userTemplatesDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private LayoutTemplateService() {
        userTemplatesDir = Paths.get(System.getProperty("user.home"), ".deluxemenus-studio", "templates");
        registerBuiltInTemplates();
        reloadCustomTemplates();
    }

    private void registerBuiltInTemplates() {
        // 1. Perimeter Border
        builtInTemplates.add(new LayoutTemplate(
                "border_perimeter", "Borders",
                "Рамка по периметру", "Perimeter Border",
                "Заполняет крайние слоты меню серыми стеклянными панелями",
                "Fills outer boundary slots with gray stained glass panes",
                false, null,
                (config, replace) -> generatePerimeterBorder(config)
        ));

        // 2. Bottom Navigation Bar
        builtInTemplates.add(new LayoutTemplate(
                "bottom_navigation", "Navigation",
                "Нижняя панель навигации", "Bottom Navigation Bar",
                "Кнопки Назад, Закрыть и Вперёд в нижнем ряду меню",
                "Back, Close, and Next buttons arranged in bottom row",
                false, null,
                (config, replace) -> generateBottomNavigation(config)
        ));

        // 3. Confirmation Dialog
        builtInTemplates.add(new LayoutTemplate(
                "confirm_dialog", "Dialogs",
                "Подтверждение действия (Да / Нет)", "Confirmation Dialog (Yes / No)",
                "Кнопка подтверждения (зелёная) и отмены (красная) с карточкой в центре",
                "Confirm (green) and Cancel (red) buttons with target info in center",
                false, null,
                (config, replace) -> generateConfirmationPrompt(config)
        ));

        // 4. Player Profile Card
        builtInTemplates.add(new LayoutTemplate(
                "player_profile", "Cards",
                "Карточка профиля игрока", "Player Profile Card",
                "Голова игрока с отображением баланса, пинга и времени в игре",
                "Player head card displaying economy balance, ping, and playtime",
                false, null,
                (config, replace) -> generatePlayerProfileCard(config)
        ));
    }

    public synchronized void reloadCustomTemplates() {
        customTemplates.clear();
        if (!Files.exists(userTemplatesDir)) return;
        try (var stream = Files.list(userTemplatesDir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    Map<String, Object> map = gson.fromJson(r, new TypeToken<Map<String, Object>>() {}.getType());
                    if (map != null) {
                        String id = (String) map.get("id");
                        String name = (String) map.get("name");
                        String cat = (String) map.getOrDefault("category", "Custom");
                        String desc = (String) map.getOrDefault("description", "");
                        List<?> itemsRaw = (List<?>) map.get("items");
                        List<MenuItem> items = new ArrayList<>();
                        if (itemsRaw != null) {
                            for (Object o : itemsRaw) {
                                items.add(gson.fromJson(gson.toJsonTree(o), MenuItem.class));
                            }
                        }
                        customTemplates.add(new LayoutTemplate(
                                id, cat, name, name, desc, desc, true, items,
                                (config, replace) -> generateFromStaticItems(config, items)
                        ));
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public synchronized List<LayoutTemplate> getAllTemplates() {
        List<LayoutTemplate> all = new ArrayList<>(builtInTemplates);
        all.addAll(customTemplates);
        return all;
    }

    public synchronized void saveCustomTemplate(String name, String category, String description, List<MenuItem> items) throws IOException {
        Files.createDirectories(userTemplatesDir);
        String id = "tmpl_" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("name", name);
        payload.put("category", category != null ? category : "Custom");
        payload.put("description", description != null ? description : "");
        payload.put("items", items);

        Path file = userTemplatesDir.resolve(id + ".json");
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(payload, w);
        }
        reloadCustomTemplates();
    }

    public synchronized boolean deleteCustomTemplate(String id) {
        Path file = userTemplatesDir.resolve(id + ".json");
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) reloadCustomTemplates();
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }

    public Command applyTemplate(MenuConfig config, LayoutTemplate template) {
        List<Command> commands;
        if (template.dynamicGenerator() != null) {
            commands = template.dynamicGenerator().apply(config, true);
        } else if (template.staticItems() != null) {
            commands = generateFromStaticItems(config, template.staticItems());
        } else {
            commands = Collections.emptyList();
        }
        return new BatchCommand("Apply template " + template.nameEn(), commands);
    }

    private List<Command> generatePerimeterBorder(MenuConfig config) {
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int rows = Math.max(1, (size + cols - 1) / cols);

        List<Command> list = new ArrayList<>();
        MenuItem border = new MenuItem("border", "GRAY_STAINED_GLASS_PANE");
        border.getAppearance().setDisplayName(" ");
        border.setPriority(10);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    int s = r * cols + c;
                    if (s < size) border.getSlots().add(String.valueOf(s));
                }
            }
        }
        list.add(new SetItemCommand(config, border));
        return list;
    }

    private List<Command> generateBottomNavigation(MenuConfig config) {
        int size = config.getSize();
        int cols = config.getMenuType() == InventoryType.CHEST ? 9 : config.getMenuType().getColumns();
        int startRow = size - cols;
        if (startRow < 0) startRow = 0;

        List<Command> list = new ArrayList<>();

        // Back
        MenuItem back = new MenuItem("nav_back", "ARROW");
        back.setSlot(startRow);
        back.getAppearance().setDisplayName("&e&l<< Назад");
        back.getAppearance().getLore().add("&7Возврат в предыдущее меню.");
        back.getClickHandler().getLeftClickCommands().add(Action.parse("[sound] UI_BUTTON_CLICK"));
        back.getClickHandler().getLeftClickCommands().add(Action.parse("[openguimenu] main"));
        list.add(new SetItemCommand(config, back));

        // Close
        int closeSlot = Math.min(size - 1, startRow + (cols / 2));
        MenuItem close = new MenuItem("nav_close", "BARRIER");
        close.setSlot(closeSlot);
        close.getAppearance().setDisplayName("&c&lЗакрыть меню");
        close.getAppearance().getLore().add("&7Закрыть текущее окно.");
        close.getClickHandler().getLeftClickCommands().add(Action.parse("[sound] BLOCK_CHEST_CLOSE"));
        close.getClickHandler().getLeftClickCommands().add(Action.parse("[close]"));
        list.add(new SetItemCommand(config, close));

        // Next
        MenuItem next = new MenuItem("nav_next", "ARROW");
        next.setSlot(size - 1);
        next.getAppearance().setDisplayName("&e&lВперёд >>");
        next.getAppearance().getLore().add("&7Перейти на следующую страницу.");
        next.getClickHandler().getLeftClickCommands().add(Action.parse("[sound] UI_BUTTON_CLICK"));
        next.getClickHandler().getLeftClickCommands().add(Action.parse("[openguimenu] page_2"));
        list.add(new SetItemCommand(config, next));

        return list;
    }

    private List<Command> generateConfirmationPrompt(MenuConfig config) {
        int size = config.getSize();
        int baseSlot = size >= 27 ? 9 : 0;
        List<Command> list = new ArrayList<>();

        MenuItem confirm = new MenuItem("btn_confirm", "LIME_CONCRETE");
        confirm.setSlot(baseSlot + 2);
        confirm.getAppearance().setDisplayName("&a&lПодтвердить");
        confirm.getAppearance().getLore().add("&7Нажмите для завершения покупки.");
        confirm.getClickHandler().getLeftClickCommands().add(Action.parse("[sound] ENTITY_PLAYER_LEVELUP"));
        confirm.getClickHandler().getLeftClickCommands().add(Action.parse("[message] &aДействие подтверждено!"));
        list.add(new SetItemCommand(config, confirm));

        MenuItem info = new MenuItem("item_info", "GOLD_INGOT");
        info.setSlot(baseSlot + 4);
        info.getAppearance().setDisplayName("&e&lИнформация о покупке");
        info.getAppearance().getLore().add("&7Стоимость: &a500 монет");
        list.add(new SetItemCommand(config, info));

        MenuItem cancel = new MenuItem("btn_cancel", "RED_CONCRETE");
        cancel.setSlot(baseSlot + 6);
        cancel.getAppearance().setDisplayName("&c&lОтменить");
        cancel.getAppearance().getLore().add("&7Отмена и закрытие окна.");
        cancel.getClickHandler().getLeftClickCommands().add(Action.parse("[sound] UI_BUTTON_CLICK"));
        cancel.getClickHandler().getLeftClickCommands().add(Action.parse("[close]"));
        list.add(new SetItemCommand(config, cancel));

        return list;
    }

    private List<Command> generatePlayerProfileCard(MenuConfig config) {
        int slot = Math.min(4, config.getSize() - 1);
        MenuItem card = new MenuItem("player_card", "PLAYER_HEAD");
        card.setSlot(slot);
        card.setUpdate(true);
        card.getAppearance().setDisplayName("&b&lПрофиль: %player_name%");
        card.getAppearance().getLore().add("&7Баланс: &a%vault_eco_balance% монет");
        card.getAppearance().getLore().add("&7Время в игре: &e%statistic_time_played%");
        card.getAppearance().getLore().add("&7Пинг: &f%player_ping% ms");
        return List.of(new SetItemCommand(config, card));
    }

    private List<Command> generateFromStaticItems(MenuConfig config, List<MenuItem> items) {
        List<Command> commands = new ArrayList<>();
        for (MenuItem it : items) {
            MenuItem copy = it.copy();
            commands.add(new SetItemCommand(config, copy));
        }
        return commands;
    }
}
