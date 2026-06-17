package com.playtheatria.jliii.magicpond.listeners;

import com.playtheatria.jliii.magicpond.config.Settings;
import com.playtheatria.jliii.magicpond.tracking.CellKey;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Denies fish catches in cells that have been over-fished.
 * <p>
 * Runs at {@link EventPriority#LOWEST} so that cancelling the event suppresses the whole
 * downstream reward chain. In particular the Magicpond bonus listener runs later with
 * {@code ignoreCancelled = true}, so a depleted catch yields no vanilla fish <em>and</em>
 * no pond bonus &mdash; this is what lets the magic pond run safely without mcMMO's
 * exploit check. Purely Bukkit/Paper API: {@link PlayerFishEvent} + {@code FishHook#getLocation()}.
 */
public class OverfishingListener implements Listener {

    private final FishingPressureTracker tracker;
    private final Settings settings;

    public OverfishingListener(FishingPressureTracker tracker, Settings settings) {
        this.tracker = tracker;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!settings.enabled()) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getHook() == null) return;

        Location hook = event.getHook().getLocation();
        if (hook == null || hook.getWorld() == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission("magicpond.bypass")) return;

        CellKey key = tracker.cellOf(hook);
        boolean allowed = tracker.recordCatch(player.getUniqueId(), key);
        if (allowed) return;

        event.setCancelled(true);
        if (settings.notifyPlayer()) {
            player.sendActionBar(
                    Component.text("The fish here have been overfished — try a new spot.")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.ITALIC)
            );
        }
    }
}
