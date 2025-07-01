package thunder.hack.utility.client;

/**
 * Utility class for time measurement, ported from LiquidBounce Kotlin version
 */
public class Chronometer {
    private long lastUpdate;

    public Chronometer() {
        this.lastUpdate = 0;
    }

    public Chronometer(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Get elapsed time since last update
     */
    public long getElapsed() {
        return System.currentTimeMillis() - lastUpdate;
    }

    /**
     * Get time elapsed until specified time
     */
    public long elapsedUntil(long time) {
        return time - lastUpdate;
    }

    /**
     * Check if specified time has elapsed
     */
    public boolean hasElapsed(long ms) {
        return lastUpdate + ms < System.currentTimeMillis();
    }

    /**
     * Check if at least specified time has elapsed
     */
    public boolean hasAtLeastElapsed(long ms) {
        return lastUpdate + ms <= System.currentTimeMillis();
    }

    /**
     * Reset the chronometer to current time or specified time
     */
    public void reset() {
        this.lastUpdate = System.currentTimeMillis();
    }

    public void reset(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Wait for at least specified milliseconds
     */
    public void waitForAtLeast(long ms) {
        this.lastUpdate = Math.max(this.lastUpdate, System.currentTimeMillis() + ms);
    }

    /**
     * Get the last update timestamp
     */
    public long getLastUpdate() {
        return lastUpdate;
    }
}