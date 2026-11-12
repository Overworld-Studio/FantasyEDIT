package com.fantasy.edit.paper.build;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 快速搭建的服务端限额。全部来自 config.yml 的 build 段。
 */
public final class BuildLimits {

    public final boolean enabled;
    public final boolean creativeOnly;
    public final int maxVolume;
    public final int maxRadius;
    public final int historyDepth;

    public BuildLimits(FileConfiguration config) {
        this.enabled = config.getBoolean("build.enabled", true);
        this.creativeOnly = config.getBoolean("build.creative-only", true);
        this.maxVolume = Math.max(1, config.getInt("build.max-volume", 250_000));
        this.maxRadius = Math.max(1, Math.min(32, config.getInt("build.max-radius", 16)));
        this.historyDepth = Math.max(1, Math.min(128, config.getInt("build.history-depth", 32)));
    }

    public String describe() {
        return "enabled=" + this.enabled + ", creativeOnly=" + this.creativeOnly
                + ", maxVolume=" + this.maxVolume + ", maxRadius=" + this.maxRadius
                + ", historyDepth=" + this.historyDepth;
    }
}
