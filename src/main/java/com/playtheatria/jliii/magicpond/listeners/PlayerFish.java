package com.playtheatria.jliii.magicpond.listeners;

import com.playtheatria.jliii.magicpond.Magicpond;
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

import java.util.List;

public class PlayerFish implements Listener {

    private final List<Location> pondLocation;
    private final Magicpond magicpond;

    public PlayerFish(List<Location> location, Magicpond magicpond) {
        this.pondLocation = location;
        this.magicpond = magicpond;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void OnFishEvent(PlayerFishEvent event) {
        if (event.getCaught() == null || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!pondLocation.contains(event.getHook().getLocation().getBlock().getLocation())) return;

        World world = event.getPlayer().getWorld();
        Item caughtItem = (Item) event.getCaught();
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
}
