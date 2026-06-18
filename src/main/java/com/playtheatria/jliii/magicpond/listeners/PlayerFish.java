package com.playtheatria.jliii.magicpond.listeners;

import com.playtheatria.jliii.magicpond.Magicpond;
import com.playtheatria.jliii.magicpond.config.Settings;
import com.playtheatria.jliii.magicpond.pond.PondManager;
import com.playtheatria.jliii.magicpond.tracking.FishingPressureTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerFish implements Listener {

    private final PondManager ponds;
    private final Magicpond magicpond;
    private final FishingPressureTracker tracker;
    private final Settings settings;

    public PlayerFish(PondManager ponds, Magicpond magicpond, FishingPressureTracker tracker, Settings settings) {
        this.ponds = ponds;
        this.magicpond = magicpond;
        this.tracker = tracker;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void OnFishEvent(PlayerFishEvent event) {
        if (event.getCaught() == null || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Location hook = event.getHook().getLocation();
        if (!ponds.isPond(hook)) return;

        // No bonus on an overfished spot — the player reeled in junk, not a fish.
        if (settings.enabled()
                && tracker.isDepleted(event.getPlayer().getUniqueId(), tracker.cellOf(hook))) {
            return;
        }

        World world = event.getPlayer().getWorld();
        Item caughtItem = (Item) event.getCaught();
        // A magic pond always yields fish — if vanilla rolled junk/treasure, swap it for one.
        // (Depleted ponds are handled earlier: they keep the overfishing junk and skip the bonus.)
        if (!isFish(caughtItem.getItemStack().getType())) {
            caughtItem.setItemStack(new ItemStack(randomFish()));
        }
        new BukkitRunnable() {
            public void run() {
                if (dropTropical()) {
                    Item item = world.dropItem(caughtItem.getLocation(), new ItemStack(Material.TROPICAL_FISH));
                    item.setGlowing(true);
                    item.setVelocity(caughtItem.getVelocity());
                    event.getPlayer().sendActionBar(
                            Component.text("The Oracle blesses you with a tropical fish!")
                                    .color(NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.ITALIC)
                    );
                }
                Item item = world.dropItem(caughtItem.getLocation(), new ItemStack(caughtItem.getItemStack()));
                item.setVelocity(caughtItem.getVelocity());
                item.setGlowing(true);
                world.spawnParticle(Particle.HAPPY_VILLAGER, event.getHook().getLocation().add(0, 1.5, 0), 5);
                event.getCaught().setGlowing(true);
            }
        }.runTaskLater(magicpond, 5);
    }

    private boolean dropTropical() {
        return range() <= 2;
    }

    private int range() {
        int range = (10) + 1;
        return (int) (Math.random() * range);
    }

    private boolean isFish(Material material) {
        return material == Material.COD
                || material == Material.SALMON
                || material == Material.TROPICAL_FISH
                || material == Material.PUFFERFISH;
    }

    private Material randomFish() {
        double roll = Math.random();
        if (roll < 0.60) return Material.COD;
        if (roll < 0.90) return Material.SALMON;
        if (roll < 0.98) return Material.PUFFERFISH;
        return Material.TROPICAL_FISH;
    }
}
