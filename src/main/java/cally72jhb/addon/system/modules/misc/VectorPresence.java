package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.config.VectorConfig;
import cally72jhb.addon.utils.misc.VectorStarscript;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;

import java.util.ArrayList;
import java.util.List;

public class VectorPresence extends Module {
    public enum SelectMode {
        Random,
        Sequential
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLine1 = settings.createGroup("Line 1");
    private final SettingGroup sgLine2 = settings.createGroup("Line 2");

    // General

    private final Setting<Boolean> updateLarge = sgGeneral.add(new BoolSetting.Builder()
            .name("update-large")
            .description("Whether or not to update the large image.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> updatedelay = sgGeneral.add(new IntSetting.Builder()
            .name("large-update-delay")
            .description("How fast to update the large image in ticks.")
            .defaultValue(500)
            .min(50)
            .sliderMin(10)
            .sliderMax(1000)
            .build()
    );

    // Line 1

    private final Setting<List<String>> line1Strings = sgLine1.add(new StringListSetting.Builder()
            .name("line-1-messages")
            .description("Messages used for the first line.")
            .defaultValue(List.of("{player} | {server}"))
            .onChanged(strings -> recompileLine1())
            .build()
    );

    private final Setting<Integer> line1UpdateDelay = sgLine1.add(new IntSetting.Builder()
            .name("line-1-update-delay")
            .description("How fast to update the first line in ticks.")
            .defaultValue(200)
            .min(10)
            .sliderMin(10).sliderMax(200)
            .build()
    );

    private final Setting<SelectMode> line1SelectMode = sgLine1.add(new EnumSetting.Builder<SelectMode>()
            .name("line-1-select-mode")
            .description("How to select messages for the first line.")
            .defaultValue(SelectMode.Sequential)
            .build()
    );

    // Line 2

    private final Setting<List<String>> line2Strings = sgLine2.add(new StringListSetting.Builder()
            .name("line-2-messages")
            .description("Messages used for the second line.")
            .defaultValue(List.of("Modules: {modules} active"))
            .onChanged(strings -> recompileLine2())
            .build()
    );

    private final Setting<Integer> line2UpdateDelay = sgLine2.add(new IntSetting.Builder()
            .name("line-2-update-delay")
            .description("How fast to update the second line in ticks.")
            .defaultValue(60)
            .min(10)
            .sliderMin(10).sliderMax(200)
            .build()
    );

    private final Setting<SelectMode> line2SelectMode = sgLine2.add(new EnumSetting.Builder<SelectMode>()
            .name("line-2-select-mode")
            .description("How to select messages for the second line.")
            .defaultValue(SelectMode.Sequential)
            .build()
    );

    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;
    private boolean forceUpdate;

    private final List<Script> line1Scripts = new ArrayList<>();
    private int line1Ticks, line1I, ticks, key;

    private final List<Script> line2Scripts = new ArrayList<>();
    private int line2Ticks, line2I;

    public VectorPresence() {
        super(VectorAddon.MISC, "vector-presence", "Displays a custom message as your presence on Discord.");
    }

    @Override
    public void onActivate() {
        init();
    }

    @Override
    public void onDeactivate() {
        deactivate();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        tick();
    }

    private void recompile(List<String> messages, List<Script> scripts) {
        scripts.clear();

        for (int i = 0; i < messages.size(); i++) {
            Parser.Result result = Parser.parse(messages.get(i));

            if (result.hasErrors()) {
                if (Utils.canUpdate()) {
                    VectorStarscript.printChatError(i, result.errors.get(0));
                }

                continue;
            }

            scripts.add(Compiler.compile(result));
        }

        forceUpdate = true;
    }

    private String getLargeKey() {
        key++;

        if (key >= 7) key = 1;
        if (key == 0) key = 1;

        return "vector-" + key;
    }

    private void recompileLine1() {
        recompile(line1Strings.get(), line1Scripts);
    }

    private void recompileLine2() {
        recompile(line2Strings.get(), line2Scripts);
    }

    // Other

    public void init() {
        key = 1;

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("903906351640616980", handlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;

        rpc.largeImageKey = getLargeKey();
        String largeText = "Vector Client " + VectorConfig.get().version;
        rpc.largeImageText = largeText;

        recompileLine1();
        recompileLine2();

        line1Ticks = 0;
        line2Ticks = 0;

        line1I = 0;
        line2I = 0;
    }

    public void tick() {
        if (!Utils.canUpdate()) return;
        boolean update = false;

        if (ticks > updatedelay.get() && updateLarge.get()) {
            rpc.largeImageKey = getLargeKey();
            ticks = 0;
        } else if (!updateLarge.get()) {
            rpc.largeImageKey = "vector-1";
        }

        ticks++;

        // Line 1
        if (line1Ticks >= line1UpdateDelay.get() || forceUpdate) {
            if (line1Scripts.size() > 0) {
                int i = Utils.random(0, line1Scripts.size());
                if (line1SelectMode.get() == SelectMode.Sequential) {
                    if (line1I >= line1Scripts.size()) line1I = 0;
                    i = line1I++;
                }

                try {
                    rpc.details = VectorStarscript.ss.run(line1Scripts.get(i));
                } catch (StarscriptError ex) {
                    ChatUtils.error("Starscript", ex.getMessage());
                }
            }
            update = true;

            line1Ticks = 0;
        }
        else line1Ticks++;

        // Line 2
        if (line2Ticks >= line2UpdateDelay.get() || forceUpdate) {
            if (line2Scripts.size() > 0) {
                int i = Utils.random(0, line2Scripts.size());
                if (line2SelectMode.get() == SelectMode.Sequential) {
                    if (line2I >= line2Scripts.size()) line2I = 0;
                    i = line2I++;
                }

                try {
                    rpc.state = VectorStarscript.ss.run(line2Scripts.get(i));
                } catch (StarscriptError e) {
                    ChatUtils.error("Starscript", e.getMessage());
                }
            }
            update = true;

            line2Ticks = 0;
        }
        else line2Ticks++;

        // Update
        if (update) instance.Discord_UpdatePresence(rpc);
        forceUpdate = false;

        instance.Discord_RunCallbacks();
    }

    public void deactivate() {
        instance.Discord_ClearPresence();
        instance.Discord_Shutdown();
    }
}
