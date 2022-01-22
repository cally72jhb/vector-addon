package cally72jhb.addon;

import cally72jhb.addon.system.Systems;
import cally72jhb.addon.system.players.Player;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.Stats;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.value.Value;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public class VectorAddon extends MeteorAddon {
    public static final Category MISC = new Category("Vector Misc", Items.AMETHYST_SHARD.getDefaultStack());
    public static final Category MOVEMENT = new Category("Vector Motion", Items.AMETHYST_SHARD.getDefaultStack());
    public static final Logger LOG = LogManager.getLogger();

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public static Screen screen;
    public static Stats scores;

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vector Addon");

        VectorUtils.init();
        Systems.init();

        MeteorClient.EVENT_BUS.subscribe(this);

        VectorUtils.members();
        VectorUtils.changeIcon();

        scores = new Stats(true, true, true, false, 10);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(MISC);
        Modules.registerCategory(MOVEMENT);
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        VectorUtils.members();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (screen != null && mc.currentScreen == null) {
            mc.setScreen(screen);
            screen = null;
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        for (Player player : Players.get()) {
            if (!event.getMessage().getString().contains("muted " + player.name) && player.muted && event.getMessage().getString().contains(player.name)) {
                System.out.println(event.getMessage().getString());
                event.cancel();
            }
        }
    }
}
