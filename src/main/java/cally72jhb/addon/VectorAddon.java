package cally72jhb.addon;

import cally72jhb.addon.system.Systems;
import cally72jhb.addon.system.players.Player;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import cally72jhb.addon.utils.misc.Stats;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.text.ColoredText;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class VectorAddon extends MeteorAddon {
    public static final Category Misc = new Category("Vector Misc", Items.AMETHYST_SHARD.getDefaultStack());
    public static final Category Movement = new Category("Vector Motion", Items.AMETHYST_SHARD.getDefaultStack());
    public static final Category Combat = new Category("Vector Combat", Items.AMETHYST_SHARD.getDefaultStack());

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

        scores = new Stats( 10);

        Systems.load();
        Runtime.getRuntime().addShutdownHook(new Thread(Systems::save));
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Misc);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Combat);
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
        LiteralText finalText = new LiteralText("");
        Text message = event.getMessage();

        boolean replace = false;

        ArrayList<String> strings = new ArrayList<>();
        if (VectorConfig.get() != null && VectorConfig.get().highlightMembers && VectorUtils.members != null && !VectorUtils.members.isEmpty()) strings.addAll(VectorUtils.members);
        strings.add("VECTOR");
        strings.add("Vector");
        strings.add("vector");

        for (String string : strings) {
            for (ColoredText text : TextUtils.toColoredTextList(message)) {
                if (text.getText().contains(string)) {
                    replace = true;

                    String first = text.getText().substring(0, text.getText().indexOf(string));
                    String middle = text.getText().substring(text.getText().indexOf(string), text.getText().indexOf(string) + string.length());
                    String last = text.getText().substring(text.getText().indexOf(string) + string.length());

                    finalText.append(new LiteralText(first).setStyle(Style.EMPTY.withColor(text.getColor().getPacked())));
                    finalText.append(new LiteralText(middle).formatted(Formatting.GOLD));
                    finalText.append(new LiteralText(last).setStyle(Style.EMPTY.withColor(text.getColor().getPacked())));
                } else {
                    finalText.append(new LiteralText(text.getText()).setStyle(Style.EMPTY.withColor(text.getColor().getPacked())));
                }
            }

            message = finalText;
            finalText = new LiteralText("");
        }

        if (replace) event.setMessage(message);

        if (Players.get() != null) {
            for (Player player : Players.get()) {
                if (!event.getMessage().getString().contains("muted " + player.name) && player.muted && event.getMessage().getString().contains(player.name)) {
                    System.out.println(event.getMessage().getString());
                    event.cancel();
                }
            }
        }
    }
}
