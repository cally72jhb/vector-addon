package cally72jhb.addon.system.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class SendRawMessageEvent extends Cancellable {
    private static final SendRawMessageEvent INSTANCE = new SendRawMessageEvent();

    public String message;

    public static SendRawMessageEvent get(String message) {
        INSTANCE.setCancelled(false);
        INSTANCE.message = message;
        return INSTANCE;
    }
}
