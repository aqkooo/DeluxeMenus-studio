package com.ejyqyl.dmstudio.service.pagination;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.inventory.InventoryType;
import com.ejyqyl.dmstudio.service.yaml.YamlExportService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service generating multi-page linked menu configurations (§Pagination Generator).
 */
public class PaginationService {

    public record PaginationOptions(
            String baseKey,
            String titlePattern,
            int totalPages,
            int size,
            int prevSlot,
            int nextSlot,
            int infoSlot,
            String prevMaterial,
            String nextMaterial,
            String infoMaterial,
            boolean includeBorder
    ) {
        public static PaginationOptions defaults(String baseKey, int totalPages) {
            return new PaginationOptions(
                    baseKey != null && !baseKey.isBlank() ? baseKey.trim() : "menu",
                    "&8Menu (Page {page}/{total})",
                    Math.max(1, totalPages),
                    27,
                    18,
                    26,
                    22,
                    "ARROW",
                    "ARROW",
                    "BOOK",
                    true
            );
        }
    }

    public List<MenuConfig> generatePages(PaginationOptions options) {
        List<MenuConfig> pages = new ArrayList<>();
        int total = Math.max(1, options.totalPages());
        String base = options.baseKey() != null && !options.baseKey().isBlank() ? options.baseKey().trim() : "menu";

        for (int p = 1; p <= total; p++) {
            MenuConfig cfg = new MenuConfig();
            cfg.setSize(options.size());
            cfg.setMenuType(InventoryType.CHEST);
            String title = options.titlePattern()
                    .replace("{page}", String.valueOf(p))
                    .replace("{total}", String.valueOf(total));
            cfg.setMenuTitle(title);
            cfg.getOpenCommands().add(base + "_" + p);
            cfg.setRegisterCommand(true);

            // 1. Perimeter Border Preset
            if (options.includeBorder()) {
                addBorder(cfg);
            }

            // 2. Previous Page Button (pages 2..N)
            if (p > 1 && options.prevSlot() >= 0 && options.prevSlot() < options.size()) {
                MenuItem prevBtn = new MenuItem("nav_prev", options.prevMaterial());
                prevBtn.setSlot(options.prevSlot());
                prevBtn.getAppearance().setDisplayName("&a< Предыдущая страница");
                prevBtn.getClickHandlers().getLeftClickCommands().add(
                        new Action("[openguimenu]", base + "_" + (p - 1), null, null)
                );
                cfg.getItems().put("nav_prev", prevBtn);
            }

            // 3. Next Page Button (pages 1..N-1)
            if (p < total && options.nextSlot() >= 0 && options.nextSlot() < options.size()) {
                MenuItem nextBtn = new MenuItem("nav_next", options.nextMaterial());
                nextBtn.setSlot(options.nextSlot());
                nextBtn.getAppearance().setDisplayName("&aСледующая страница >");
                nextBtn.getClickHandlers().getLeftClickCommands().add(
                        new Action("[openguimenu]", base + "_" + (p + 1), null, null)
                );
                cfg.getItems().put("nav_next", nextBtn);
            }

            // 4. Page Indicator Info Item
            if (options.infoSlot() >= 0 && options.infoSlot() < options.size()) {
                MenuItem infoBtn = new MenuItem("nav_info", options.infoMaterial());
                infoBtn.setSlot(options.infoSlot());
                infoBtn.getAppearance().setDisplayName("&eСтраница " + p + " из " + total);
                infoBtn.getAppearance().getLore().add("&7Кликайте по стрелкам для навигации");
                cfg.getItems().put("nav_info", infoBtn);
            }

            pages.add(cfg);
        }

        return pages;
    }

    public List<Path> savePagesToDirectory(List<MenuConfig> pages, File targetDir, String baseKey) throws IOException {
        if (!targetDir.exists()) {
            Files.createDirectories(targetDir.toPath());
        }
        YamlExportService exporter = new YamlExportService();
        List<Path> written = new ArrayList<>();

        for (int i = 0; i < pages.size(); i++) {
            MenuConfig cfg = pages.get(i);
            String fileName = baseKey + "_" + (i + 1) + ".yml";
            File targetFile = new File(targetDir, fileName);
            exporter.exportToFile(cfg, targetFile.toPath());
            written.add(targetFile.toPath());
        }

        return written;
    }

    private void addBorder(MenuConfig config) {
        int size = config.getSize();
        int cols = 9;
        int rows = size / cols;

        MenuItem borderItem = new MenuItem("border", "GRAY_STAINED_GLASS_PANE");
        borderItem.getAppearance().setDisplayName(" ");
        borderItem.setPriority(10);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    borderItem.getSlots().add(String.valueOf(r * cols + c));
                }
            }
        }
        config.getItems().put("border", borderItem);
    }
}
