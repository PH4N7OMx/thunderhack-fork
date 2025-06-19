package thunder.hack.events.impl;

public class StopWatch {
    private long startTime;

    public StopWatch() {
        startTime = System.currentTimeMillis();
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public long elapsed() {
        return System.currentTimeMillis() - startTime;
    }

    public boolean hasElapsed(long ms) {
        return elapsed() >= ms;
    }
}
