package com.playtheatria.jliii.magicpond.listeners;

import com.playtheatria.jliii.magicpond.config.Settings;
import com.playtheatria.jliii.magicpond.tracking.CellKey;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the overfishing mechanic to caught fish.
 * <ul>
 *   <li><b>Thinning out</b> — warns the player so depletion isn't a surprise.</li>
 *   <li><b>Depleted</b> — turns the catch into junk (configurable) and tells the player to
 *       move on; if no junk items are configured, cancels the catch instead.</li>
 * </ul>
 * Runs at {@link EventPriority#LOWEST} so the depletion decision is made before the Magicpond
 * bonus listener (MONITOR) and any other reward handlers. Purely Bukkit/Paper API.
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
        CellKey key = tracker.cellOf(hook);
        switch (tracker.recordCatch(player.getUniqueId(), key)) {
            case ALLOWED -> { /* plenty of fish; nothing to do */ }
            case WARNING -> notify(player,
                    "The fish here are thinning out — find a fresh spot soon.", NamedTextColor.GOLD);
            case DEPLETED -> deplete(event, player);
        }
    }

    private void deplete(PlayerFishEvent event, Player player) {
        List<Material> garbage = settings.garbageItems();
        if (!garbage.isEmpty() && event.getCaught() instanceof Item caught) {
            // Swap the reeled-in fish for a random piece of junk; no XP for overfishing.
            Material junk = garbage.get(ThreadLocalRandom.current().nextInt(garbage.size()));
            caught.setItemStack(new ItemStack(junk));
            event.setExpToDrop(0);
        } else {
            // No junk configured (or nothing to swap): deny the catch outright.
            event.setCancelled(true);
        }
        notify(player, "Overfished here — move to a new spot for better catches.", NamedTextColor.RED);
    }

    private void notify(Player player, String message, NamedTextColor color) {
        if (!settings.notifyPlayer()) return;
        player.sendActionBar(Component.text(message).color(color).decorate(TextDecoration.ITALIC));
    }
}
