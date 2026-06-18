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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
        Player player = event.getPlayer();
        Item caughtItem = (Item) event.getCaught();
        // A magic pond always yields fish — if vanilla rolled junk/treasure, swap it for one.
        // (Depleted ponds are handled earlier: they keep the overfishing junk and skip the bonus.)
        if (!isFish(caughtItem.getItemStack().getType())) {
            caughtItem.setItemStack(new ItemStack(randomFish()));
        }
        caughtItem.setGlowing(true);

        // Lob the bonus fish from the catch spot to the player on a vanilla-style arc,
        // rather than copying the caught item's velocity (which could overshoot past them).
        Location catchLoc = caughtItem.getLocation().clone();
        ItemStack bonusStack = caughtItem.getItemStack().clone();
        new BukkitRunnable() {
            public void run() {
                if (dropTropical()) {
                    lobToPlayer(world, catchLoc, new ItemStack(Material.TROPICAL_FISH), player);
                    player.sendActionBar(
                            Component.text("The Oracle blesses you with a tropical fish!")
                                    .color(NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.ITALIC)
                    );
                }
                lobToPlayer(world, catchLoc, bonusStack, player);
                world.spawnParticle(Particle.HAPPY_VILLAGER, catchLoc.clone().add(0, 1.5, 0), 5);
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

    /**
     * Drops a glowing item at {@code from} and lobs it toward the player using the same arc
     * vanilla uses to reel a catch in, so it lands on the player instead of flying past.
     */
    private void lobToPlayer(World world, Location from, ItemStack stack, Player player) {
        Item item = world.dropItem(from, stack);
        item.setGlowing(true);
        Location to = player.getLocation();
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double lob = Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08;
        item.setVelocity(new Vector(dx * 0.1, dy * 0.1 + lob, dz * 0.1));
    }
}
