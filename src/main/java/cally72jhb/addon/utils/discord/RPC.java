package cally72jhb.addon.utils.discord;

import cally72jhb.addon.utils.config.VectorConfig;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

public class RPC {
    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;
    private static int ticks, key;

    private static String getLargeKey() {
        if (key > 3) {
            key = 1;
            return "vector-client-1";
        }
        key++;
        return "vector-client-" + (key - 1);
    }

    public static void init() {
        key = 1;

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("885412312511561728", handlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;

        rpc.largeImageKey = getLargeKey();
        String largeText = "Vector Client " + VectorConfig.get().version;
        rpc.largeImageText = largeText;

        rpc.details = "In the Main Menu";

        ticks = 0;
    }

    public static void tick() {
        if (ticks > 120) {
            rpc.largeImageKey = getLargeKey();
            ticks = 0;
            instance.Discord_UpdatePresence(rpc);
        }

        ticks++;

        instance.Discord_RunCallbacks();
    }

    public static void deactivate() {
        try {
            instance.Discord_ClearPresence();
            instance.Discord_Shutdown();
        } catch (Exception ignored) {

        }
    }
}
