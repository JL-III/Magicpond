package com.playtheatria.jliii.magicpond.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the tunable values for the overfishing / fishing-pressure system.
 * <p>
 * A single instance is shared by every component so that {@link #load(FileConfiguration)}
 * (called on plugin enable and on {@code /magicpond reload}) updates all of them at once.
 * All values live under the {@code overfishing} section of {@code config.yml}.
 */
public class Settings {

    private static final List<String> DEFAULT_GARBAGE = List.of(
            "LEATHER_BOOTS", "LEATHER", "BONE", "STRING", "BOWL",
            "STICK", "INK_SAC", "ROTTEN_FLESH", "LILY_PAD", "BAMBOO");

    private boolean enabled;
    private int cellSize;
    private double pressureCap;
    private double resumeThreshold;
    private double warnThreshold;
    private double gainPerCatch;
    private double recoveryHalfLifeSeconds;
    private boolean notifyPlayer;
    private int sweepIntervalSeconds;
    private List<Material> garbageItems;

    public void load(FileConfiguration config) {
        enabled = config.getBoolean("overfishing.enabled", true);
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

    public boolean enabled() {
        return enabled;
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
