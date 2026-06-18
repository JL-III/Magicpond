package com.playtheatria.jliii.magicpond;

import com.playtheatria.jliii.magicpond.commands.MagicpondCommand;
import com.playtheatria.jliii.magicpond.listeners.OverfishingListener;
import com.playtheatria.jliii.magicpond.listeners.PlayerFish;
import com.playtheatria.jliii.magicpond.managers.ConfigManager;
import com.playtheatria.jliii.magicpond.pond.PondManager;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import com.playtheatria.jliii.magicpond.util.CustomLogger;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Magicpond extends JavaPlugin {

    private ConfigManager configManager;
    private PondManager ponds;
    private FishingPressureTracker tracker;
    private BukkitTask sweepTask;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        CustomLogger logger = new CustomLogger(getLogger(), configManager);
        ponds = new PondManager(this);
        ponds.load();
        tracker = new FishingPressureTracker(configManager);

        getServer().getPluginManager().registerEvents(new PlayerFish(ponds, this, tracker, configManager, logger), this);
        getServer().getPluginManager().registerEvents(new OverfishingListener(tracker, configManager), this);

        PluginCommand command = getCommand("magicpond");
        if (command != null) {
            command.setExecutor(new MagicpondCommand(tracker, configManager, ponds));
        }

        scheduleSweep();
    }

    @Override
    public void onDisable() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
    }

    /**
     * Schedules the next pressure sweep, re-reading the interval each cycle so a reloaded
     * {@code sweep-interval-seconds} takes effect without the plugin handling config reloads.
     */
    private void scheduleSweep() {
        long ticks = Math.max(1L, configManager.sweepIntervalSeconds() * 20L);
        sweepTask = getServer().getScheduler().runTaskLater(this, () -> {
            tracker.sweep();
            scheduleSweep();
        }, ticks);
    }
}
