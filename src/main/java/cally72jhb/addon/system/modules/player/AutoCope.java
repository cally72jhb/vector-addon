package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AutoCope extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> smallCaps = sgGeneral.add(new BoolSetting.Builder()
        .name("small-caps")
        .description("Sends all messages with small caps.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> copeOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("cope-on-death")
        .description("Sends a messages everytime you die.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> randomMsg = sgGeneral.add(new BoolSetting.Builder()
        .name("random")
        .description("Sends random messages every time you die.")
        .defaultValue(true)
        .visible(copeOnDeath::get)
        .build()
    );

    private final Setting<String> deathString = sgGeneral.add(new StringSetting.Builder()
        .name("death-message")
        .description("The message to send when you died.")
        .defaultValue("I lagged")
        .visible(() -> copeOnDeath.get() && !randomMsg.get())
        .build()
    );

    private final Setting<List<String>> deathMessages = sgGeneral.add(new StringListSetting.Builder()
        .name("death-messages")
        .description("The random messages to send when you died.")
        .defaultValue(List.of("☠", "☠☠","gg", "b+ user", "you should pitch a tent for camping"))
        .visible(() -> copeOnDeath.get() && randomMsg.get())
        .build()
    );

    private List<Pair<String, String>> respondations = new ArrayList<>();

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();
    private final Random random = new Random();

    private boolean dead;

    public AutoCope() {
        super(Categories.Misc, "auto-cope", "Automatically copes for you.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @Override
    public void onActivate() {
        dead = false;
    }

    @EventHandler(priority = EventPriority.HIGH + 10)
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (event.screen instanceof DeathScreen) dead = true;
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (dead && !(mc.currentScreen instanceof DeathScreen)) sendMsg(randomMsg.get() && !deathMessages.get().isEmpty() ? deathMessages.get().get(random.nextInt(deathMessages.get().size())) : deathString.get());
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!respondations.isEmpty() && !event.isModified()) {
            String message = event.getMessage().getString();

            if (!message.contains(mc.player.getGameProfile().getName())) {
                List<Pair<String, String>> list = new ArrayList<>(respondations);
                Collections.shuffle(list);

                for (Pair<String, String> respond : list) if (message.contains(respond.getA())) {
                    sendMsg(respond.getB());
                    return;
                }
            }
        }
    }

    // Messaging

    private void sendMsg(String string) {
        dead = false;

        StringBuilder builder = new StringBuilder();

        if (smallCaps.get()) {
            for (char ch : string.toCharArray()) builder.append(SMALL_CAPS.containsKey(ch) ? SMALL_CAPS.get(ch) : ch);
        } else {
            builder.append(string);
        }

        mc.getNetworkHandler().sendPacket(new ChatMessageC2SPacket(builder.toString()));
    }

    // Respondations

    @Override
    public WWidget getWidget(GuiTheme theme) {
        respondations.removeIf(respond -> respond.getA().isEmpty());

        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        if (!respondations.isEmpty()) {
            for (int i = 0; i < respondations.size(); i++) {
                int messageI = i;
                String messageA = respondations.get(i).getA();
                String messageB = respondations.get(i).getB();

                WTextBox left = table.add(theme.textBox(messageA)).minWidth(300).expandX().widget();
                left.action = () -> respondations.set(messageI, new Pair<>(left.get(), messageB));

                WTextBox right = table.add(theme.textBox(messageB)).minWidth(300).expandX().widget();
                right.action = () -> respondations.set(messageI, new Pair<>(messageA, right.get()));

                WMinus delete = table.add(theme.minus()).widget();
                delete.action = () -> {
                    respondations.remove(messageI);

                    table.clear();
                    fillTable(theme, table);
                };

                table.row();
            }
        }

        WTextBox left = table.add(theme.textBox("example")).minWidth(300).expandX().widget();
        WTextBox right = table.add(theme.textBox("")).minWidth(300).expandX().widget();

        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            respondations.add(new Pair<>(left.get(), right.get()));
            respondations.removeIf(respond -> respond.getA().isEmpty());

            table.clear();
            fillTable(theme, table);
        };

        table.row();

        // Reset
        WButton reset = table.add(theme.button("Reset")).widget();
        reset.action = () -> {
            respondations = new ArrayList<>() {{
                add(new Pair<>("died", "☠"));
                add(new Pair<>("dead", "☠☠"));
                add(new Pair<>("!vector", "https://cally72jhb.github.io/website/"));
            }};
            table.clear();
            fillTable(theme, table);
        };

        // Clear
        WButton clear = table.add(theme.button("Clear")).widget();
        clear.action = () -> {
            respondations.clear();
            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        respondations.removeIf(respond -> respond.getA().isEmpty());
        NbtList leftTag = new NbtList();
        NbtList rightTag = new NbtList();

        for (Pair<String, String> respond : respondations) {
            leftTag.add(NbtString.of(respond.getA()));
            rightTag.add(NbtString.of(respond.getB()));
        }

        tag.put("left", leftTag);
        tag.put("right", rightTag);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        respondations.clear();

        ArrayList<String> left = new ArrayList<>();
        ArrayList<String> right = new ArrayList<>();

        if (tag.contains("left") && tag.contains("right")) {
            NbtList leftTag = tag.getList("left", 8);
            NbtList rightTag = tag.getList("right", 8);

            for (NbtElement element : leftTag) left.add(element.asString());
            for (NbtElement element : rightTag) right.add(element.asString());

            for (int i = 0; i < left.size() && i < right.size(); i++) {
                respondations.add(new Pair<>(left.get(i), right.get(i)));
            }
        } else {
            respondations = new ArrayList<>() {{
                add(new Pair<>("died", "☠"));
                add(new Pair<>("dead", "☠☠"));
                add(new Pair<>("!vector", "https://cally72jhb.github.io/website/"));
            }};
        }

        return super.fromTag(tag);
    }
}
