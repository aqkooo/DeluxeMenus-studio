package com.ejyqyl.dmstudio.service.yaml;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser for DeluxeMenus requirements and requirement lists from YAML maps.
 */
public final class YamlRequirementParser {

    private YamlRequirementParser() {}

    @SuppressWarnings("unchecked")
    public static RequirementList parseRequirementList(Map<String, Object> map) {
        if (map == null) return null;
        RequirementList list = new RequirementList();
        Set<String> known = Set.of("requirements", "minimum_requirements", "stop_at_success", "deny_commands", "success_commands");

        if (map.containsKey("requirements") && map.get("requirements") instanceof Map<?, ?> reqsMap) {
            for (Map.Entry<?, ?> entry : reqsMap.entrySet()) {
                String reqName = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> singleMap) {
                    list.addRequirement(reqName, parseSingleRequirement((Map<String, Object>) singleMap));
                }
            }
        }

        if (map.containsKey("minimum_requirements")) {
            try {
                list.setMinimumRequirements(Integer.parseInt(String.valueOf(map.get("minimum_requirements"))));
            } catch (NumberFormatException ignored) {}
        }

        if (map.containsKey("stop_at_success")) {
            list.setStopAtSuccess(Boolean.parseBoolean(String.valueOf(map.get("stop_at_success"))));
        }

        if (map.containsKey("deny_commands") && map.get("deny_commands") instanceof List<?> denies) {
            for (Object o : denies) list.getDenyCommands().add(Action.parse(String.valueOf(o)));
        }

        if (map.containsKey("success_commands") && map.get("success_commands") instanceof List<?> succs) {
            for (Object o : succs) list.getSuccessCommands().add(Action.parse(String.valueOf(o)));
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!known.contains(entry.getKey())) {
                list.getUnknownFields().put(entry.getKey(), entry.getValue());
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static Requirement parseSingleRequirement(Map<String, Object> map) {
        if (map == null) return null;
        String type = map.containsKey("type") ? String.valueOf(map.get("type")) : "";
        Requirement req = new Requirement(type);
        Set<String> known = Set.of("type", "optional", "deny_commands", "success_commands");

        if (map.containsKey("optional")) {
            req.setOptional(Boolean.parseBoolean(String.valueOf(map.get("optional"))));
        }

        if (map.containsKey("deny_commands") && map.get("deny_commands") instanceof List<?> denies) {
            for (Object o : denies) req.getDenyCommands().add(Action.parse(String.valueOf(o)));
        }

        if (map.containsKey("success_commands") && map.get("success_commands") instanceof List<?> succs) {
            for (Object o : succs) req.getSuccessCommands().add(Action.parse(String.valueOf(o)));
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!known.contains(entry.getKey())) {
                req.setField(entry.getKey(), entry.getValue());
            }
        }

        return req;
    }
}
