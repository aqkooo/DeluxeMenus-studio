package com.ejyqyl.dmstudio.service.simulator;

import com.ejyqyl.dmstudio.model.Action;
import com.ejyqyl.dmstudio.model.ClickHandler;
import com.ejyqyl.dmstudio.model.MenuConfig;
import com.ejyqyl.dmstudio.model.MenuItem;
import com.ejyqyl.dmstudio.model.requirement.Requirement;
import com.ejyqyl.dmstudio.model.requirement.RequirementList;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Core engine simulating DeluxeMenus execution and requirement resolution (§Play Mode).
 */
public class MenuSimulatorEngine {

    public record SimulationResult(
            MenuItem clickedItem,
            boolean success,
            List<String> messages,
            List<String> executedCommands,
            String soundToPlay,
            boolean shouldClose,
            boolean shouldRefresh,
            String openGuiMenu
    ) {}

    private final MenuConfig config;
    private final SimulatedPlayerState playerState;

    public MenuSimulatorEngine(MenuConfig config, SimulatedPlayerState playerState) {
        this.config = config;
        this.playerState = playerState != null ? playerState : new SimulatedPlayerState();
    }

    public SimulatedPlayerState getPlayerState() {
        return playerState;
    }

    /**
     * Resolves the visible item for a slot based on priority and view requirements.
     */
    public MenuItem resolveVisibleItem(int slotIndex) {
        if (config == null || config.getItems() == null) return null;

        List<MenuItem> candidates = config.getItemsAtSlot(slotIndex);
        for (MenuItem item : candidates) {
            if (evaluateRequirements(item.getViewRequirement())) {
                return item;
            }
        }

        return null;
    }

    /**
     * Evaluates a list of requirements against the simulated player state.
     */
    public boolean evaluateRequirements(RequirementList reqList) {
        if (reqList == null || reqList.getRequirements().isEmpty()) {
            return true;
        }

        int passed = 0;
        int total = reqList.getRequirements().size();
        int minRequired = reqList.getMinimumRequirements() != null ? reqList.getMinimumRequirements() : total;

        for (Requirement req : reqList.getRequirements().values()) {
            if (evaluateSingleRequirement(req)) {
                passed++;
            }
        }

        return passed >= minRequired;
    }

    private boolean evaluateSingleRequirement(Requirement req) {
        String type = req.getType() != null ? req.getType().trim().toLowerCase(Locale.ROOT) : "";
        boolean inverted = req.isInverted() || type.startsWith("!");
        String cleanType = type.startsWith("!") ? type.substring(1).trim() : type;

        boolean result = switch (cleanType) {
            case "has permission" -> playerState.hasPermission(req.getPermission());
            case "has money" -> {
                double reqAmount = req.getAmount() != null ? req.getAmount() : 0.0;
                yield playerState.getVaultBalance() >= reqAmount;
            }
            case "string equals", "string equals ignorecase" -> {
                String in = playerState.replacePlaceholders(req.getInput());
                String out = playerState.replacePlaceholders(req.getOutput());
                yield cleanType.contains("ignorecase")
                        ? Objects.toString(in, "").equalsIgnoreCase(Objects.toString(out, ""))
                        : Objects.toString(in, "").equals(Objects.toString(out, ""));
            }
            case "regex matches" -> {
                try {
                    String in = playerState.replacePlaceholders(req.getInput());
                    String regex = req.getRegex();
                    yield in != null && regex != null && Pattern.compile(regex).matcher(in).find();
                } catch (Exception e) {
                    yield false;
                }
            }
            default -> true;
        };

        return inverted != result;
    }

    /**
     * Handles a simulated click on a slot and dispatches resulting commands.
     */
    public SimulationResult handleClick(int slotIndex, boolean isLeftClick) {
        MenuItem item = resolveVisibleItem(slotIndex);
        if (item == null) {
            return new SimulationResult(null, false, List.of(), List.of(), null, false, false, null);
        }

        ClickHandler clickHandler = item.getClickHandlers();
        RequirementList clickReq = isLeftClick
                ? (item.getLeftClickRequirement() != null ? item.getLeftClickRequirement() : (clickHandler != null ? clickHandler.getClickRequirement() : null))
                : (item.getRightClickRequirement() != null ? item.getRightClickRequirement() : (clickHandler != null ? clickHandler.getClickRequirement() : null));

        boolean allowed = evaluateRequirements(clickReq);
        List<String> messages = new ArrayList<>();
        List<String> executed = new ArrayList<>();
        String[] soundHolder = new String[1];
        boolean[] closeHolder = new boolean[1];
        boolean[] refreshHolder = new boolean[1];
        String[] openGuiHolder = new String[1];

        List<Action> actionsToRun = new ArrayList<>();
        if (!allowed) {
            soundHolder[0] = "ENTITY_VILLAGER_NO";
            if (clickReq != null && clickReq.getDenyCommands() != null) {
                actionsToRun.addAll(clickReq.getDenyCommands());
            }
        } else {
            soundHolder[0] = "UI_BUTTON_CLICK";
            if (clickHandler != null) {
                List<Action> cmds = isLeftClick ? clickHandler.getLeftClickCommands() : clickHandler.getRightClickCommands();
                if (cmds != null) actionsToRun.addAll(cmds);
                if (clickHandler.getClickCommands() != null) actionsToRun.addAll(clickHandler.getClickCommands());
            }
        }

        for (Action act : actionsToRun) {
            String raw = playerState.replacePlaceholders(act.getRawCommand());
            executed.add(raw);
            String lower = raw.toLowerCase(Locale.ROOT);

            if (lower.startsWith("[message]")) {
                messages.add(raw.substring(9).trim());
            } else if (lower.startsWith("[sound]")) {
                soundHolder[0] = raw.substring(7).trim();
            } else if (lower.startsWith("[close]")) {
                closeHolder[0] = true;
            } else if (lower.startsWith("[refresh]")) {
                refreshHolder[0] = true;
            } else if (lower.startsWith("[openguimenu]")) {
                openGuiHolder[0] = raw.substring(13).trim();
            } else if (lower.startsWith("[take_money]")) {
                try {
                    double amt = Double.parseDouble(raw.substring(12).trim());
                    playerState.setVaultBalance(Math.max(0, playerState.getVaultBalance() - amt));
                    refreshHolder[0] = true;
                } catch (Exception ignored) {}
            }
        }

        if (soundHolder[0] != null) {
            SoundPlaybackService.getInstance().playSound(soundHolder[0]);
        }

        return new SimulationResult(item, allowed, messages, executed, soundHolder[0], closeHolder[0], refreshHolder[0], openGuiHolder[0]);
    }
}
