package com.playtheatria.jliii.magicpond.tracking;

/**
 * The mutable state of a single fishing cell for a single player: how much "pressure"
 * (recent fishing activity) it carries, when it was last touched, and whether it is
 * currently depleted (fished out).
 * <p>
 * Pressure decays exponentially toward zero so that a spot recovers if left alone.
 */
public class FishingSpot {

    /** Below this, pressure is treated as fully recovered and snapped to zero. */
    private static final double EPSILON = 1.0e-4;

    double pressure;
    long lastUpdate;
    boolean depleted;

    FishingSpot(long now) {
        this.lastUpdate = now;
    }

    /**
     * Applies time-based recovery: pressure is halved every {@code halfLifeMillis}.
     * Idempotent with respect to wall-clock time, so it is safe to call on every
     * access and from the periodic sweep.
     */
    void decay(long now, long halfLifeMillis) {
        long elapsed = now - lastUpdate;
        if (elapsed <= 0) {
            return;
        }
        if (pressure > 0) {
            pressure *= Math.pow(0.5, (double) elapsed / halfLifeMillis);
            if (pressure < EPSILON) {
                pressure = 0;
            }
        }
        lastUpdate = now;
    }

    public double pressure() {
        return pressure;
    }

    public boolean depleted() {
        return depleted;
    }
}
