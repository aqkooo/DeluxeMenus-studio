package com.ejyqyl.dmstudio.service.template;

import com.ejyqyl.dmstudio.command.Command;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Representation of a reusable layout template or pattern.
 */
public record LayoutTemplate(
        String id,
        String category,
        String nameRu,
        String nameEn,
        String descriptionRu,
        String descriptionEn,
        boolean isCustom,
        List<MenuItem> staticItems,
        BiFunction<MenuConfig, Boolean, List<Command>> dynamicGenerator
) {
    public String getName(String lang) {
        return "ru".equalsIgnoreCase(lang) ? nameRu : nameEn;
    }

    public String getDescription(String lang) {
        return "ru".equalsIgnoreCase(lang) ? descriptionRu : descriptionEn;
    }
}
