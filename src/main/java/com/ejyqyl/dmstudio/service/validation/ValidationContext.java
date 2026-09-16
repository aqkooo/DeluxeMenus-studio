package com.ejyqyl.dmstudio.service.validation;

/**
 * Execution context for validation rules (target Minecraft version, plugin version).
 */
public class ValidationContext {

    private String mcVersion = "1.21";
    private String pluginVersion = "1.14.0";

    public ValidationContext() {}

    public ValidationContext(String mcVersion, String pluginVersion) {
        this.mcVersion = mcVersion != null ? mcVersion : "1.21";
        this.pluginVersion = pluginVersion != null ? pluginVersion : "1.14.0";
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public boolean isMcAtLeast(String targetVersion) {
        return compareVersions(this.mcVersion, targetVersion) >= 0;
    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.replace("-SNAPSHOT", "").split("\\.");
        String[] parts2 = v2.replace("-SNAPSHOT", "").split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("\\D", "")) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("\\D", "")) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}
