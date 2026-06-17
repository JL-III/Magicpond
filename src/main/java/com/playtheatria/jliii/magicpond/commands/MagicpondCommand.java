package com.playtheatria.jliii.magicpond.commands;

import com.playtheatria.jliii.magicpond.Magicpond;
import com.playtheatria.jliii.magicpond.config.Settings;
import com.playtheatria.jliii.magicpond.tracking.CellKey;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import com.playtheatria.jliii.magicpond.tracking.FishingSpot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin / debugging entry point for tuning and verifying the overfishing system.
 * <ul>
 *   <li>{@code /magicpond info} – summary of current settings and tracked players.</li>
 *   <li>{@code /magicpond check} – pressure of the cell you are standing in.</li>
 *   <li>{@code /magicpond clear} – wipe all tracked fishing pressure.</li>
 *   <li>{@code /magicpond reload} – reload config.yml.</li>
 * </ul>
 */
public class MagicpondCommand implements CommandExecutor {

    private final Magicpond plugin;
    private final FishingPressureTracker tracker;
    private final Settings settings;

    public MagicpondCommand(Magicpond plugin, FishingPressureTracker tracker, Settings settings) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                send(sender, "Configuration reloaded.", NamedTextColor.GREEN);
            }
            case "clear" -> {
                tracker.clearAll();
                send(sender, "Cleared all tracked fishing pressure.", NamedTextColor.GREEN);
            }
            case "info" -> {
                send(sender, "Overfishing: " + (settings.enabled() ? "enabled" : "disabled")
                        + " | cell-size=" + settings.cellSize()
                        + " | cap=" + settings.pressureCap()
                        + " | resume=" + settings.resumeThreshold()
                        + " | gain=" + settings.gainPerCatch()
                        + " | half-life=" + settings.recoveryHalfLifeSeconds() + "s"
                        + " | tracked-players=" + tracker.trackedPlayers(), NamedTextColor.AQUA);
            }
            case "check" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "Only a player can check a location.", NamedTextColor.RED);
                    return true;
                }
                CellKey key = tracker.cellOf(player.getLocation());
                FishingSpot spot = tracker.inspect(player.getUniqueId(), key);
                if (spot == null) {
                    send(sender, "No fishing pressure recorded for your current cell "
                            + "(" + key.world() + " " + key.x() + "," + key.z() + ").", NamedTextColor.GRAY);
                } else {
                    send(sender, String.format("Cell (%s %d,%d): pressure=%.2f / cap %.2f%s",
                            key.world(), key.x(), key.z(), spot.pressure(), settings.pressureCap(),
                            spot.depleted() ? " [DEPLETED]" : ""),
                            spot.depleted() ? NamedTextColor.RED : NamedTextColor.YELLOW);
                }
            }
            default -> usage(sender, label);
        }
        return true;
    }

    private void usage(CommandSender sender, String label) {
        send(sender, "Usage: /" + label + " <info|check|clear|reload>", NamedTextColor.GRAY);
    }

    private void send(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(Component.text(message).color(color));
    }
}
