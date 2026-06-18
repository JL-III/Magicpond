package com.playtheatria.jliii.magicpond;

import com.playtheatria.jliii.magicpond.commands.MagicpondCommand;
import com.playtheatria.jliii.magicpond.listeners.OverfishingListener;
import com.playtheatria.jliii.magicpond.listeners.PlayerFish;
import com.playtheatria.jliii.magicpond.managers.ConfigManager;
import com.playtheatria.jliii.magicpond.pond.PondManager;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
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
        ponds = new PondManager(this);
        ponds.load();
        tracker = new FishingPressureTracker(configManager);

        getServer().getPluginManager().registerEvents(new PlayerFish(ponds, this, tracker, configManager), this);
        getServer().getPluginManager().registerEvents(new OverfishingListener(tracker, configManager), this);

        PluginCommand command = getCommand("magicpond");
        if (command != null) {
            command.setExecutor(new MagicpondCommand(this, tracker, configManager, ponds));
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

    public ConfigManager getConfigManager() {
        return configManager;
    }

    /** Logs a message at INFO only when {@code debug} is enabled in config.yml. */
    public void debug(String message) {
        if (configManager.debug()) {
            getLogger().info("[debug] " + message);
        }
    }

    /** Reloads config via the {@link ConfigManager} and reschedules the sweep task. */
    public void reload() {
        configManager.reload();
        startSweepTask();
    }

    private void startSweepTask() {
        if (sweepTask != null) {
            sweepTask.cancel();
        }
        long sweepTicks = configManager.sweepIntervalSeconds() * 20L;
        sweepTask = getServer().getScheduler().runTaskTimer(this, tracker::sweep, sweepTicks, sweepTicks);
    }
}
