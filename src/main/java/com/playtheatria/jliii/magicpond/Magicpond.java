package com.playtheatria.jliii.magicpond;

import com.playtheatria.jliii.magicpond.commands.MagicpondCommand;
import com.playtheatria.jliii.magicpond.config.Settings;
import com.playtheatria.jliii.magicpond.listeners.OverfishingListener;
import com.playtheatria.jliii.magicpond.listeners.PlayerFish;
import com.playtheatria.jliii.magicpond.pond.PondManager;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Magicpond extends JavaPlugin {

    private final Settings settings = new Settings();
    private PondManager ponds;
    private FishingPressureTracker tracker;
    private BukkitTask sweepTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings.load(getConfig());

        ponds = new PondManager(this);
        ponds.load();
        tracker = new FishingPressureTracker(settings);

        getServer().getPluginManager().registerEvents(new PlayerFish(ponds, this, tracker, settings), this);
        getServer().getPluginManager().registerEvents(new OverfishingListener(tracker, settings), this);

        PluginCommand command = getCommand("magicpond");
        if (command != null) {
            command.setExecutor(new MagicpondCommand(this, tracker, settings, ponds));
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
