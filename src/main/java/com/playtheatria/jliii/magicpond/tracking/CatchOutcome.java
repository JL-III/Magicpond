package com.playtheatria.jliii.magicpond.tracking;

/**
 * Result of recording a catch in a cell.
 *
 * <ul>
 *   <li>{@link #ALLOWED} – normal catch, plenty of fish left.</li>
 *   <li>{@link #WARNING} – catch succeeds, but the spot is thinning out (approaching the cap).</li>
 *   <li>{@link #DEPLETED} – the spot is fished out; the catch should be turned to junk or denied.</li>
 * </ul>
 */
public enum CatchOutcome {
    ALLOWED,
    WARNING,
    DEPLETED
}
