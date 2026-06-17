package com.playtheatria.jliii.magicpond;

import com.playtheatria.jliii.magicpond.commands.MagicpondCommand;
import com.playtheatria.jliii.magicpond.config.Settings;
import com.playtheatria.jliii.magicpond.listeners.OverfishingListener;
import com.playtheatria.jliii.magicpond.listeners.PlayerFish;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import com.playtheatria.jliii.magicpond.util.ListGenerators;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class Magicpond extends JavaPlugin {

    private final Settings settings = new Settings();
    private FishingPressureTracker tracker;
    private BukkitTask sweepTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings.load(getConfig());

        List<Location> pondLocations = ListGenerators.loadMagicPondLocations();
        tracker = new FishingPressureTracker(settings);

        getServer().getPluginManager().registerEvents(new PlayerFish(pondLocations, this), this);
        getServer().getPluginManager().registerEvents(new OverfishingListener(tracker, settings, pondLocations), this);

        PluginCommand command = getCommand("magicpond");
        if (command != null) {
            command.setExecutor(new MagicpondCommand(this, tracker, settings));
        }

        startSweepTask();
    }

    @Override
    public void onDisable() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
    }

    /** Reloads config.yml into the shared {@link Settings} and reschedules the sweep task. */
    public void reload() {
        reloadConfig();
        settings.load(getConfig());
        startSweepTask();
    }

    private void startSweepTask() {
        if (sweepTask != null) {
            sweepTask.cancel();
        }
        long sweepTicks = settings.sweepIntervalSeconds() * 20L;
        sweepTask = getServer().getScheduler().runTaskTimer(this, tracker::sweep, sweepTicks, sweepTicks);
    }
}
