package thunder.hack.events.impl;

import thunder.hack.events.Event;

public class SlowWalkingEvent extends Event {
    private boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
