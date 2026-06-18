package com.playtheatria.jliii.magicpond.managers;

import com.playtheatria.jliii.magicpond.Magicpond;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Central owner of {@code config.yml}.
 * <p>
 * It is the single place that touches Bukkit's plugin config: it saves the bundled
 * default on construction, parses + validates every tunable into typed fields, and
 * exposes them via getters. Nothing else in the plugin should call
 * {@code getConfig()} / {@code reloadConfig()} / {@code saveDefaultConfig()} — every
 * config read routes through this manager.
 * <p>
 * {@link #reload()} re-reads the file from disk and is wired to {@code /magicpond reload}.
 */
public class ConfigManager {

    private static final List<String> DEFAULT_GARBAGE = List.of(
            "LEATHER_BOOTS", "LEATHER", "BONE", "STRING", "BOWL",
            "STICK", "INK_SAC", "ROTTEN_FLESH", "LILY_PAD", "BAMBOO");

    private final Magicpond plugin;

    private boolean debug;
    private boolean overfishingEnabled;
    private int cellSize;
    private double pressureCap;
    private double resumeThreshold;
    private double warnThreshold;
    private double gainPerCatch;
    private double recoveryHalfLifeSeconds;
    private boolean notifyPlayer;
    private int sweepIntervalSeconds;
    private List<Material> garbageItems;

    public ConfigManager(Magicpond plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    /** Re-reads config.yml from disk. Invoked by {@code /magicpond reload}. */
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        FileConfiguration config = plugin.getConfig();

        debug = config.getBoolean("debug", false);
        overfishingEnabled = config.getBoolean("overfishing.enabled", true);
        cellSize = Math.max(1, config.getInt("overfishing.cell-size", 6));
        pressureCap = config.getDouble("overfishing.pressure-cap", 5.0);
        resumeThreshold = config.getDouble("overfishing.resume-threshold", 2.0);
        warnThreshold = config.getDouble("overfishing.warn-threshold", 3.0);
        gainPerCatch = config.getDouble("overfishing.gain-per-catch", 1.0);
        recoveryHalfLifeSeconds = Math.max(1.0, config.getDouble("overfishing.recovery-half-life-seconds", 120.0));
        notifyPlayer = config.getBoolean("overfishing.notify-player", true);
        sweepIntervalSeconds = Math.max(5, config.getInt("overfishing.sweep-interval-seconds", 120));

        // A resume threshold above the cap would make depletion permanent; clamp it.
        if (resumeThreshold > pressureCap) {
            resumeThreshold = pressureCap;
        }
        // The warning must fire before depletion to be useful.
        if (warnThreshold > pressureCap) {
            warnThreshold = pressureCap;
        }

        // What a depleted spot yields instead of fish. An explicitly-empty list means
        // "cancel the catch"; an absent key falls back to the vanilla-junk defaults.
        List<String> names = config.isSet("overfishing.garbage-items")
                ? config.getStringList("overfishing.garbage-items")
                : DEFAULT_GARBAGE;
        garbageItems = new ArrayList<>();
        for (String name : names) {
            Material material = Material.matchMaterial(name.trim());
            if (material != null && material.isItem()) {
                garbageItems.add(material);
            }
        }
    }

    public boolean debug() {
        return debug;
    }

    /** Whether the overfishing system is active. Does not affect the magic-pond bonus. */
    public boolean overfishingEnabled() {
        return overfishingEnabled;
    }

    public int cellSize() {
        return cellSize;
    }

    public double pressureCap() {
        return pressureCap;
    }

    public double resumeThreshold() {
        return resumeThreshold;
    }

    public double warnThreshold() {
        return warnThreshold;
    }

    public double gainPerCatch() {
        return gainPerCatch;
    }

    public double recoveryHalfLifeSeconds() {
        return recoveryHalfLifeSeconds;
    }

    public boolean notifyPlayer() {
        return notifyPlayer;
    }

    public int sweepIntervalSeconds() {
        return sweepIntervalSeconds;
    }

    /** Junk items a depleted spot can yield; empty means the catch is cancelled instead. */
    public List<Material> garbageItems() {
        return garbageItems;
    }

    /** Half-life expressed in milliseconds, used by the decay math. */
    public long halfLifeMillis() {
        return (long) (recoveryHalfLifeSeconds * 1000.0);
    }
}
