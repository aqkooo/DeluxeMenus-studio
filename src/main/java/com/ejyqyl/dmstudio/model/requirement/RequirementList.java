package com.ejyqyl.dmstudio.model.requirement;

import com.ejyqyl.dmstudio.model.Action;

import java.util.*;

/**
 * A block of requirements in DeluxeMenus, with optional OR-logic (minimum_requirements),
 * stop_at_success, and block-level deny/success commands.
 */
public class RequirementList {

    private final Map<String, Requirement> requirements = new LinkedHashMap<>();
    private Integer minimumRequirements;
    private Boolean stopAtSuccess;
    private final List<Action> denyCommands = new ArrayList<>();
    private final List<Action> successCommands = new ArrayList<>();
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public RequirementList() {}

    public boolean isEmpty() {
        return requirements.isEmpty() && denyCommands.isEmpty() && successCommands.isEmpty() && unknownFields.isEmpty();
    }

    public RequirementList copy() {
        RequirementList copy = new RequirementList();
        copy.minimumRequirements = this.minimumRequirements;
        copy.stopAtSuccess = this.stopAtSuccess;
        for (Map.Entry<String, Requirement> entry : this.requirements.entrySet()) {
            copy.requirements.put(entry.getKey(), entry.getValue().copy());
        }
        for (Action action : this.denyCommands) {
            copy.denyCommands.add(action.copy());
        }
        for (Action action : this.successCommands) {
            copy.successCommands.add(action.copy());
        }
        copy.unknownFields.putAll(this.unknownFields);
        return copy;
    }

    public Map<String, Requirement> getRequirements() {
        return requirements;
    }

    public void addRequirement(String name, Requirement req) {
        requirements.put(name, req);
    }

    public void removeRequirement(String name) {
        requirements.remove(name);
    }

    public Integer getMinimumRequirements() {
        return minimumRequirements;
    }

    public void setMinimumRequirements(Integer minimumRequirements) {
        this.minimumRequirements = minimumRequirements;
    }

    public Boolean getStopAtSuccess() {
        return stopAtSuccess;
    }

    public void setStopAtSuccess(Boolean stopAtSuccess) {
        this.stopAtSuccess = stopAtSuccess;
    }

    public List<Action> getDenyCommands() {
        return denyCommands;
    }

    public List<Action> getSuccessCommands() {
        return successCommands;
    }

    public Map<String, Object> getUnknownFields() {
        return unknownFields;
    }
}
