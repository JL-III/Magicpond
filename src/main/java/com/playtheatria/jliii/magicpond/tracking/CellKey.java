package com.playtheatria.jliii.magicpond.tracking;

/**
 * Identifies a fixed grid cell in the world. The hook's block X/Z are floor-divided
 * by {@code cell-size} so that every cast inside the same cell maps to one key.
 * Records give us correct {@code equals}/{@code hashCode} for free as a map key.
 */
public record CellKey(String world, int x, int z) {
}
