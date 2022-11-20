package cally72jhb.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class JumpEvent extends Cancellable {
    private static final JumpEvent INSTANCE = new JumpEvent();

    public static JumpEvent get() {
        return INSTANCE;
    }
}
