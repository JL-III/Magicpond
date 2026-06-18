package com.playtheatria.jliii.magicpond.commands;

import com.playtheatria.jliii.magicpond.managers.ConfigManager;
import com.playtheatria.jliii.magicpond.pond.PondManager;
import com.playtheatria.jliii.magicpond.tracking.CellKey;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import com.playtheatria.jliii.magicpond.tracking.FishingSpot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin / debugging entry point for the magic pond and overfishing system.
 * <ul>
 *   <li>{@code /magicpond set} – mark the chunk you're standing in as a magic pond.</li>
 *   <li>{@code /magicpond unset} – remove the magic-pond designation from your chunk.</li>
 *   <li>{@code /magicpond list} – list designated pond chunks.</li>
 *   <li>{@code /magicpond info} – summary of current settings and tracked players.</li>
 *   <li>{@code /magicpond check} – pressure of the cell you are standing in.</li>
 *   <li>{@code /magicpond clear} – wipe all tracked fishing pressure.</li>
 *   <li>{@code /magicpond reload} – reload config.yml.</li>
 * </ul>
 */
public class MagicpondCommand implements CommandExecutor {

    private final FishingPressureTracker tracker;
    private final ConfigManager configManager;
    private final PondManager ponds;

    public MagicpondCommand(FishingPressureTracker tracker, ConfigManager configManager, PondManager ponds) {
        this.tracker = tracker;
        this.configManager = configManager;
        this.ponds = ponds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "Only a player can set a magic pond.", NamedTextColor.RED);
                    return true;
                }
                Chunk chunk = player.getLocation().getChunk();
                if (ponds.add(chunk)) {
                    send(sender, "Magic pond enabled in chunk (" + chunk.getX() + ", " + chunk.getZ()
                            + ") of " + chunk.getWorld().getName() + ".", NamedTextColor.GREEN);
                } else {
                    send(sender, "This chunk is already a magic pond.", NamedTextColor.GRAY);
                }
            }
            case "unset", "remove" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "Only a player can unset a magic pond.", NamedTextColor.RED);
                    return true;
                }
                Chunk chunk = player.getLocation().getChunk();
                if (ponds.remove(chunk)) {
                    send(sender, "Magic pond disabled in chunk (" + chunk.getX() + ", " + chunk.getZ()
                            + ").", NamedTextColor.GREEN);
                } else {
                    send(sender, "This chunk is not a magic pond.", NamedTextColor.GRAY);
                }
            }
            case "list" -> {
                if (ponds.count() == 0) {
                    send(sender, "No magic pond chunks designated. Stand in one and run /" + label + " set.",
                            NamedTextColor.GRAY);
                } else {
                    send(sender, "Magic pond chunks (" + ponds.count() + "): "
                            + String.join(", ", ponds.keys()), NamedTextColor.AQUA);
                }
            }
            case "reload" -> {
                configManager.reload();
                send(sender, "Configuration reloaded.", NamedTextColor.GREEN);
            }
            case "clear" -> {
                tracker.clearAll();
                send(sender, "Cleared all tracked fishing pressure.", NamedTextColor.GREEN);
            }
            case "info" -> {
                send(sender, "Magicpond: " + (configManager.enabled() ? "enabled" : "disabled")
                        + " | cell-size=" + configManager.cellSize()
                        + " | cap=" + configManager.pressureCap()
                        + " | warn=" + configManager.warnThreshold()
                        + " | resume=" + configManager.resumeThreshold()
                        + " | gain=" + configManager.gainPerCatch()
                        + " | half-life=" + configManager.recoveryHalfLifeSeconds() + "s"
                        + " | ponds=" + ponds.count()
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
                            key.world(), key.x(), key.z(), spot.pressure(), configManager.pressureCap(),
                            spot.depleted() ? " [DEPLETED]" : ""),
                            spot.depleted() ? NamedTextColor.RED : NamedTextColor.YELLOW);
                }
            }
            default -> usage(sender, label);
        }
        return true;
    }

    private void usage(CommandSender sender, String label) {
        send(sender, "Usage: /" + label + " <set|unset|list|info|check|clear|reload>", NamedTextColor.GRAY);
    }

    private void send(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(Component.text(message).color(color));
    }
}
