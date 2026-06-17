package com.playtheatria.jliii.magicpond.pond;

import com.playtheatria.jliii.magicpond.Magicpond;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which chunks are designated "magic ponds" (bonus-fish areas), persisted to
 * {@code ponds.yml} in the plugin's data folder. Designation is per-chunk so it can be
 * toggled in-game with {@code /magicpond set} / {@code unset} instead of hardcoding regions.
 */
public class PondManager {

    private final Magicpond plugin;
    private final File file;
    private final Set<String> chunks = new HashSet<>();

    public PondManager(Magicpond plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ponds.yml");
    }

    private static String key(World world, int chunkX, int chunkZ) {
        return world.getName() + ":" + chunkX + ":" + chunkZ;
    }

    public void load() {
        chunks.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        chunks.addAll(config.getStringList("ponds"));
    }

    private void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("ponds", new ArrayList<>(chunks));
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ponds.yml: " + e.getMessage());
        }
    }

    /** Designates the given chunk as a magic pond. Returns false if it already was one. */
    public boolean add(Chunk chunk) {
        if (!chunks.add(key(chunk.getWorld(), chunk.getX(), chunk.getZ()))) {
            return false;
        }
        save();
        return true;
    }

    /** Removes the magic-pond designation from the given chunk. Returns false if it wasn't one. */
    public boolean remove(Chunk chunk) {
        if (!chunks.remove(key(chunk.getWorld(), chunk.getX(), chunk.getZ()))) {
            return false;
        }
        save();
        return true;
    }

    /** Whether the chunk containing this location is a magic pond (no chunk load forced). */
    public boolean isPond(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return chunks.contains(key(world, location.getBlockX() >> 4, location.getBlockZ() >> 4));
    }

    public int count() {
        return chunks.size();
    }

    /** Unmodifiable view of the designated chunk keys ({@code world:x:z}), for display. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(chunks);
    }
}
