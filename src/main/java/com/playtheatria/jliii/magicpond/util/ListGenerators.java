package com.playtheatria.jliii.magicpond.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class ListGenerators {

    private static final String world = "world";
    private static final String the_ark = "The_Ark";

    public static List<Location> getRegionBlocks(World world, Location location1, Location location2) {
        List<Location> blockLocations = new ArrayList<>();

        int x1 = Math.min(location1.getBlockX(), location2.getBlockX());
        int y1 = Math.min(location1.getBlockY(), location2.getBlockY());
        int z1 = Math.min(location1.getBlockZ(), location2.getBlockZ());

        int x2 = Math.max(location1.getBlockX(), location2.getBlockX());
        int y2 = Math.max(location1.getBlockY(), location2.getBlockY());
        int z2 = Math.max(location1.getBlockZ(), location2.getBlockZ());

        for(int x = x1; x <= x2; x++) {
            for(int y = y1; y <= y2; y++) {
                for(int z = z1; z <= z2; z++) {
                    blockLocations.add(new Location(world, x, y, z));
                }
            }
        }
        return blockLocations;
    }

    public static List<Location> loadMagicPondLocations() {
        return new ArrayList<>(){
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
    }
}
