package cally72jhb.addon;

import cally72jhb.addon.system.Systems;
import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.players.Player;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.text.ColoredText;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class VectorAddon extends MeteorAddon {
    public static final Logger LOG = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vector Addon");

        VectorUtils.init();
        Systems.init();

        MeteorClient.EVENT_BUS.subscribe(this);

        VectorUtils.members();
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

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("cally72jhb", "vector-addon");
    }

    @Override
    public String getWebsite() {
        return "https://cally72jhb.github.io/website";
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
        VectorUtils.members();

        ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        LiteralText finalText = new LiteralText("");
        Text message = event.getMessage();

        boolean replace = false;

        ArrayList<String> strings = new ArrayList<>();
        if (VectorConfig.get() != null && VectorConfig.get().highlightMembers && VectorUtils.members != null && !VectorUtils.members.isEmpty()) strings.addAll(VectorUtils.members);

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
