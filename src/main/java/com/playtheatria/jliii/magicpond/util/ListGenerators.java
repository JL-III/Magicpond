package com.playtheatria.jliii.magicpond.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class ListGenerators {
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
}
