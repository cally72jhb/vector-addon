package cally72jhb.addon;

import cally72jhb.addon.system.Systems;
import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VectorAddon extends MeteorAddon {
    public static final Logger LOG = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vector Addon");

        VectorUtils.init();
        Systems.init();

        MeteorClient.EVENT_BUS.subscribe(this);

        VectorUtils.changeIcon();

        VectorUtils.postInit();

        Systems.load();
        Runtime.getRuntime().addShutdownHook(new Thread(Systems::save));

        Systems.postInit();
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Categories.Misc);
        Modules.registerCategory(Categories.Movement);
        Modules.registerCategory(Categories.Combat);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (VectorUtils.screen != null) {
            VectorUtils.mc.setScreen(VectorUtils.screen);
            VectorUtils.screen = null;
        }
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
    }
}
