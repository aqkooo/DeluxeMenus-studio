package com.ejyqyl.dmstudio.model;

import com.ejyqyl.dmstudio.model.requirement.RequirementList;

import java.util.*;

/**
 * Encapsulates all click command sections and their respective click requirements.
 */
public class ClickHandler {

    private final List<Action> clickCommands = new ArrayList<>();
    private final List<Action> leftClickCommands = new ArrayList<>();
    private final List<Action> rightClickCommands = new ArrayList<>();
    private final List<Action> shiftLeftClickCommands = new ArrayList<>();
    private final List<Action> shiftRightClickCommands = new ArrayList<>();
    private final List<Action> middleClickCommands = new ArrayList<>();

    private RequirementList clickRequirement;
    private RequirementList leftClickRequirement;
    private RequirementList rightClickRequirement;
    private RequirementList shiftLeftClickRequirement;
    private RequirementList shiftRightClickRequirement;
    private RequirementList middleClickRequirement;

    public ClickHandler() {}

    public ClickHandler copy() {
        ClickHandler copy = new ClickHandler();
        copyActions(this.clickCommands, copy.clickCommands);
        copyActions(this.leftClickCommands, copy.leftClickCommands);
        copyActions(this.rightClickCommands, copy.rightClickCommands);
        copyActions(this.shiftLeftClickCommands, copy.shiftLeftClickCommands);
        copyActions(this.shiftRightClickCommands, copy.shiftRightClickCommands);
        copyActions(this.middleClickCommands, copy.middleClickCommands);

        if (this.clickRequirement != null) copy.clickRequirement = this.clickRequirement.copy();
        if (this.leftClickRequirement != null) copy.leftClickRequirement = this.leftClickRequirement.copy();
        if (this.rightClickRequirement != null) copy.rightClickRequirement = this.rightClickRequirement.copy();
        if (this.shiftLeftClickRequirement != null) copy.shiftLeftClickRequirement = this.shiftLeftClickRequirement.copy();
        if (this.shiftRightClickRequirement != null) copy.shiftRightClickRequirement = this.shiftRightClickRequirement.copy();
        if (this.middleClickRequirement != null) copy.middleClickRequirement = this.middleClickRequirement.copy();

        return copy;
    }

    private void copyActions(List<Action> src, List<Action> dst) {
        for (Action a : src) {
            dst.add(a.copy());
        }
    }

    public List<Action> getClickCommands() { return clickCommands; }
    public List<Action> getLeftClickCommands() { return leftClickCommands; }
    public List<Action> getRightClickCommands() { return rightClickCommands; }
    public List<Action> getShiftLeftClickCommands() { return shiftLeftClickCommands; }
    public List<Action> getShiftRightClickCommands() { return shiftRightClickCommands; }
    public List<Action> getMiddleClickCommands() { return middleClickCommands; }

    public RequirementList getClickRequirement() { return clickRequirement; }
    public void setClickRequirement(RequirementList req) { this.clickRequirement = req; }

    public RequirementList getLeftClickRequirement() { return leftClickRequirement; }
    public void setLeftClickRequirement(RequirementList req) { this.leftClickRequirement = req; }

    public RequirementList getRightClickRequirement() { return rightClickRequirement; }
    public void setRightClickRequirement(RequirementList req) { this.rightClickRequirement = req; }

    public RequirementList getShiftLeftClickRequirement() { return shiftLeftClickRequirement; }
    public void setShiftLeftClickRequirement(RequirementList req) { this.shiftLeftClickRequirement = req; }

    public RequirementList getShiftRightClickRequirement() { return shiftRightClickRequirement; }
    public void setShiftRightClickRequirement(RequirementList req) { this.shiftRightClickRequirement = req; }

    public RequirementList getMiddleClickRequirement() { return middleClickRequirement; }
    public void setMiddleClickRequirement(RequirementList req) { this.middleClickRequirement = req; }

    public List<Action> getAllActions() {
        List<Action> all = new ArrayList<>();
        all.addAll(clickCommands);
        all.addAll(leftClickCommands);
        all.addAll(rightClickCommands);
        all.addAll(shiftLeftClickCommands);
        all.addAll(shiftRightClickCommands);
        all.addAll(middleClickCommands);
        return all;
    }

    public List<RequirementList> getAllRequirements() {
        List<RequirementList> all = new ArrayList<>();
        if (clickRequirement != null) all.add(clickRequirement);
        if (leftClickRequirement != null) all.add(leftClickRequirement);
        if (rightClickRequirement != null) all.add(rightClickRequirement);
        if (shiftLeftClickRequirement != null) all.add(shiftLeftClickRequirement);
        if (shiftRightClickRequirement != null) all.add(shiftRightClickRequirement);
        if (middleClickRequirement != null) all.add(middleClickRequirement);
        return all;
    }

    public boolean isEmpty() {
        return clickCommands.isEmpty() && leftClickCommands.isEmpty() && rightClickCommands.isEmpty()
                && shiftLeftClickCommands.isEmpty() && shiftRightClickCommands.isEmpty() && middleClickCommands.isEmpty()
                && (clickRequirement == null || clickRequirement.isEmpty())
                && (leftClickRequirement == null || leftClickRequirement.isEmpty())
                && (rightClickRequirement == null || rightClickRequirement.isEmpty())
                && (shiftLeftClickRequirement == null || shiftLeftClickRequirement.isEmpty())
                && (shiftRightClickRequirement == null || shiftRightClickRequirement.isEmpty())
                && (middleClickRequirement == null || middleClickRequirement.isEmpty());
    }
}
