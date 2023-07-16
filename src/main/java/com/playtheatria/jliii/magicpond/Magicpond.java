package com.playtheatria.jliii.magicpond;

import com.playtheatria.jliii.magicpond.listeners.PlayerFish;
import com.playtheatria.jliii.magicpond.util.ListGenerators;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Magicpond extends JavaPlugin {
    private final String world = "world";
    private final String the_ark = "The_Ark";

    @Override
    public void onEnable() {
        final List<Location> magicPondLocations = new ArrayList<>(){
            {
                if (Bukkit.getWorld(world) != null) {
                    addAll(ListGenerators.getRegionBlocks(Bukkit.getWorld(world),
                            new Location(Bukkit.getWorld(world), -2, 60, -32),
                            new Location(Bukkit.getWorld(world), 11, 63, -26)
                    ));
                    Bukkit.getConsoleSender().sendMessage("Added world locations to magic pond.");
                }
                if (Bukkit.getWorld(the_ark) != null) {
                    addAll(ListGenerators.getRegionBlocks(Bukkit.getWorld(the_ark),
                            new Location(Bukkit.getWorld(the_ark), 5693, 79, -3305),
                            new Location(Bukkit.getWorld(the_ark), 5711, 84, -3281)
                    ));
                    Bukkit.getConsoleSender().sendMessage("Added the_ark locations to magic pond.");
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(new PlayerFish(magicPondLocations, this), this);

    }

}
