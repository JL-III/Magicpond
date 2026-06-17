package com.playtheatria.jliii.magicpond.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Holds the tunable values for the overfishing / fishing-pressure system.
 * <p>
 * A single instance is shared by every component so that {@link #load(FileConfiguration)}
 * (called on plugin enable and on {@code /magicpond reload}) updates all of them at once.
 * All values live under the {@code overfishing} section of {@code config.yml}.
 */
public class Settings {

    private boolean enabled;
    private int cellSize;
    private double pressureCap;
    private double resumeThreshold;
    private double gainPerCatch;
    private double recoveryHalfLifeSeconds;
    private boolean notifyPlayer;
    private int sweepIntervalSeconds;

    public void load(FileConfiguration config) {
        enabled = config.getBoolean("overfishing.enabled", true);
        cellSize = Math.max(1, config.getInt("overfishing.cell-size", 8));
        pressureCap = config.getDouble("overfishing.pressure-cap", 10.0);
        resumeThreshold = config.getDouble("overfishing.resume-threshold", 6.0);
        gainPerCatch = config.getDouble("overfishing.gain-per-catch", 1.0);
        recoveryHalfLifeSeconds = Math.max(1.0, config.getDouble("overfishing.recovery-half-life-seconds", 180.0));
        notifyPlayer = config.getBoolean("overfishing.notify-player", true);
        sweepIntervalSeconds = Math.max(5, config.getInt("overfishing.sweep-interval-seconds", 120));

        // A resume threshold above the cap would make depletion permanent; clamp it.
        if (resumeThreshold > pressureCap) {
            resumeThreshold = pressureCap;
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

    /** Half-life expressed in milliseconds, used by the decay math. */
    public long halfLifeMillis() {
        return (long) (recoveryHalfLifeSeconds * 1000.0);
    }
}
