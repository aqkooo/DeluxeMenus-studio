package com.ejyqyl.dmstudio.service.simulator;

import java.util.*;

/**
 * Encapsulates the simulated player context for live menu testing (§Play Mode).
 */
public class SimulatedPlayerState {

    private String playerName = "Steve";
    private double vaultBalance = 1000.0;
    private final Set<String> permissions = new HashSet<>();
    private final Map<String, String> placeholders = new HashMap<>();

    public SimulatedPlayerState() {
        // Default realistic permissions
        permissions.add("group.default");
        permissions.add("essentials.spawn");

        // Default realistic placeholders
        placeholders.put("%player_name%", "Steve");
        placeholders.put("%player_ping%", "35");
        placeholders.put("%player_world%", "world");
        placeholders.put("%server_online%", "42");
        placeholders.put("%luckperms_primary_group_name%", "default");
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : "Steve";
        placeholders.put("%player_name%", this.playerName);
    }

    public double getVaultBalance() {
        return vaultBalance;
    }

    public void setVaultBalance(double vaultBalance) {
        this.vaultBalance = vaultBalance;
        placeholders.put("%vault_eco_balance%", String.format(Locale.ROOT, "%.2f", vaultBalance));
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(String perm) {
        if (perm == null || perm.isEmpty()) return true;
        if (permissions.contains("*")) return true;
        return permissions.contains(perm.toLowerCase(Locale.ROOT));
    }

    public void addPermission(String perm) {
        if (perm != null) permissions.add(perm.toLowerCase(Locale.ROOT));
    }

    public void removePermission(String perm) {
        if (perm != null) permissions.remove(perm.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public String resolvePlaceholder(String ph) {
        if (ph == null) return "";
        if ("%player_name%".equalsIgnoreCase(ph)) return playerName;
        if ("%vault_eco_balance%".equalsIgnoreCase(ph)) return String.format(Locale.ROOT, "%.2f", vaultBalance);
        return placeholders.getOrDefault(ph.toLowerCase(Locale.ROOT), ph);
    }

    public String replacePlaceholders(String text) {
        if (text == null || text.isEmpty()) return "";
        String result = text.replace("%player_name%", playerName)
                .replace("%vault_eco_balance%", String.format(Locale.ROOT, "%.2f", vaultBalance));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
