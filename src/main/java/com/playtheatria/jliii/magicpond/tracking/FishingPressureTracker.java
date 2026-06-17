package com.playtheatria.jliii.magicpond.tracking;

import com.playtheatria.jliii.magicpond.config.Settings;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player fishing pressure across a grid of cells.
 * <p>
 * Unlike mcMMO's single-slot "last cast" memory, every cell a player fishes keeps its
 * own decaying pressure value, so alternating between spots no longer evades detection.
 * <p>
 * All access happens on the server main thread (Bukkit events + a scheduled sweep), so
 * plain {@link HashMap}s are sufficient and no synchronization is required.
 */
public class FishingPressureTracker {

    private final Settings settings;
    private final Map<UUID, Map<CellKey, FishingSpot>> data = new HashMap<>();

    public FishingPressureTracker(Settings settings) {
        this.settings = settings;
    }

    /** Maps a hook location to its grid cell using the configured cell size. */
    public CellKey cellOf(Location location) {
        int size = settings.cellSize();
        return new CellKey(
                location.getWorld().getName(),
                Math.floorDiv(location.getBlockX(), size),
                Math.floorDiv(location.getBlockZ(), size)
        );
    }

    /**
     * Records a catch attempt in a cell and decides whether it is allowed.
     * <p>
     * The decision is made on the pressure accumulated <em>before</em> this catch, then
     * the catch adds its own pressure regardless of the outcome — so an autofisher who
     * keeps casting into a depleted spot keeps it pinned high and continues to get nothing.
     *
     * @return {@code true} if the player may keep the fish, {@code false} if the spot is depleted.
     */
    public boolean recordCatch(UUID playerId, CellKey key) {
        long now = System.currentTimeMillis();
        FishingSpot spot = data.computeIfAbsent(playerId, id -> new HashMap<>())
                .computeIfAbsent(key, k -> new FishingSpot(now));
        spot.decay(now, settings.halfLifeMillis());

        boolean allowed;
        if (spot.depleted) {
            // Stay depleted until the spot has recovered below the resume threshold (hysteresis).
            if (spot.pressure < settings.resumeThreshold()) {
                spot.depleted = false;
                allowed = true;
            } else {
                allowed = false;
            }
        } else if (spot.pressure >= settings.pressureCap()) {
            spot.depleted = true;
            allowed = false;
        } else {
            allowed = true;
        }

        spot.pressure += settings.gainPerCatch();
        spot.lastUpdate = now;
        return allowed;
    }

    /**
     * Returns the (decayed) current state of a cell for diagnostics, without recording a catch.
     * {@code null} if the player has no record for that cell.
     */
    public FishingSpot inspect(UUID playerId, CellKey key) {
        Map<CellKey, FishingSpot> spots = data.get(playerId);
        if (spots == null) {
            return null;
        }
        FishingSpot spot = spots.get(key);
        if (spot == null) {
            return null;
        }
        spot.decay(System.currentTimeMillis(), settings.halfLifeMillis());
        return spot;
    }

    public void clear(UUID playerId) {
        data.remove(playerId);
    }

    public void clearAll() {
        data.clear();
    }

    public int trackedPlayers() {
        return data.size();
    }

    /**
     * Periodic maintenance: decays every spot and evicts ones that have fully recovered,
     * keeping memory bounded by currently-active fishers rather than all-time fishers.
     */
    public void sweep() {
        long now = System.currentTimeMillis();
        long halfLife = settings.halfLifeMillis();
        Iterator<Map.Entry<UUID, Map<CellKey, FishingSpot>>> players = data.entrySet().iterator();
        while (players.hasNext()) {
            Map<CellKey, FishingSpot> spots = players.next().getValue();
            spots.values().removeIf(spot -> {
                spot.decay(now, halfLife);
                return spot.pressure <= 0 && !spot.depleted;
            });
            if (spots.isEmpty()) {
                players.remove();
            }
        }
    }
}
