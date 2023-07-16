package com.playtheatria.jliii.magicpond.listeners;

import com.playtheatria.jliii.magicpond.Magicpond;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void OnFishEvent(PlayerFishEvent event) {
        World world = event.getPlayer().getWorld();
        if (event.getCaught() != null && !event.isCancelled() && (event.getState() == PlayerFishEvent.State.CAUGHT_FISH)) {
            if (pondLocation.contains(event.getHook().getLocation().getBlock().getLocation())) {
                Item caughtItem = (Item) event.getCaught();
                new BukkitRunnable() {
                    public void run() {
                        if (dropTropical()) {
                            world.dropItem(caughtItem.getLocation(), new ItemStack(Material.TROPICAL_FISH)).setVelocity(caughtItem.getVelocity());
                            event.getPlayer().sendActionBar(ChatColor.YELLOW + "" + ChatColor.ITALIC + "The Oracle blesses you with a tropical fish!");
                        }
                        world.dropItem(caughtItem.getLocation(), new ItemStack(caughtItem.getItemStack())).setVelocity(caughtItem.getVelocity());
                        world.spawnParticle(Particle.VILLAGER_HAPPY, event.getHook().getLocation().add(0, 1.5, 0), 5);
                        event.getCaught().setGlowing(true);
                    }
                }.runTaskLater(magicpond, 5);
            }
        }
    }

    private boolean dropTropical() {
        return range() <= 2;
    }

    private int range() {
        int range = (10) + 1;
        return (int) (Math.random() * range);
    }

}

