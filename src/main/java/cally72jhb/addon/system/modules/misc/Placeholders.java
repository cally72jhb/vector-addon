package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.events.SendRawMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Placeholders extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> useRawType = sgGeneral.add(new BoolSetting.Builder()
        .name("use-raw-type")
        .description("Replaces the placeholders in commands too.")
        .defaultValue(true)
        .build()
    );

    private List<Pair<String, String>> placeholders = new ArrayList<>();

    public Placeholders() {
        super(Categories.Misc, "placeholders", "Replaces chat messages with your own custom placeholders.");
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        if (!placeholders.isEmpty()) {
            for (Pair<String, String> placeholder : placeholders) {
                if (event.message.contains(placeholder.getA())) event.message = event.message.replaceAll(placeholder.getA(), placeholder.getB());
            }
        }
    }

    @EventHandler
    private void onSendRawMessage(SendRawMessageEvent event) {
        if (useRawType.get() && !placeholders.isEmpty()) {
            for (Pair<String, String> placeholder : placeholders) {
                if (event.message.contains(placeholder.getA())) event.message = event.message.replaceAll(placeholder.getA(), placeholder.getB());
            }
        }
    }

    // Utils

    public Collection<String> getPlaceholders() {
        if (placeholders != null && !placeholders.isEmpty()) {
            List<String> list = new ArrayList<>();

            for (Pair<String, String> placeholder : placeholders) list.add(placeholder.getA());

            return list;
        } else {
            return null;
        }
    }

    // Placeholders

    @Override
    public WWidget getWidget(GuiTheme theme) {
        placeholders.removeIf(placeholder -> placeholder.getA().isEmpty());

        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        if (!placeholders.isEmpty()) {
            for (int i = 0; i < placeholders.size(); i++) {
                int messageI = i;
                String messageA = placeholders.get(i).getA();
                String messageB = placeholders.get(i).getB();

                WTextBox left = table.add(theme.textBox(messageA)).minWidth(300).expandX().widget();
                left.action = () -> placeholders.set(messageI, new Pair<>(left.get(), messageB));

                WTextBox right = table.add(theme.textBox(messageB)).minWidth(300).expandX().widget();
                right.action = () -> placeholders.set(messageI, new Pair<>(messageA, right.get()));

                WMinus delete = table.add(theme.minus()).widget();
                delete.action = () -> {
                    placeholders.remove(messageI);

                    table.clear();
                    fillTable(theme, table);
                };

                table.row();
            }
        }

        WTextBox left = table.add(theme.textBox(":example:")).minWidth(300).expandX().widget();
        WTextBox right = table.add(theme.textBox("")).minWidth(300).expandX().widget();

        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            placeholders.add(new Pair<>(left.get(), right.get()));

            table.clear();
            fillTable(theme, table);
        };

        table.row();

        // Reset
        WButton reset = table.add(theme.button("Reset")).widget();
        reset.action = () -> {
            placeholders = new ArrayList<>() {{
                add(new Pair<>(":vector:", "https://cally72jhb.github.io/website/"));
                add(new Pair<>(":skull:", "☠"));
                add(new Pair<>(":sword:", "\uD83D\uDDE1"));
                add(new Pair<>(":blade:", "\uD83D\uDDE1"));
                add(new Pair<>(":bow:", "\uD83C\uDFF9"));
                add(new Pair<>(":trident:", "\uD83D\uDD31"));
                add(new Pair<>(":potion:", "\uD83E\uDDEA"));
                add(new Pair<>(":bottle:", "⚗"));
                add(new Pair<>(":rod:", "\uD83C\uDFA3"));
                add(new Pair<>(":shield:", "\uD83D\uDEE1"));
            }};
            table.clear();
            fillTable(theme, table);
        };

        // Clear
        WButton clear = table.add(theme.button("Clear")).widget();
        clear.action = () -> {
            placeholders.clear();
            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        placeholders.removeIf(placeholder -> placeholder.getA().isEmpty());
        NbtList leftTag = new NbtList();
        NbtList rightTag = new NbtList();

        for (Pair<String, String> placeholder : placeholders) {
            leftTag.add(NbtString.of(placeholder.getA()));
            rightTag.add(NbtString.of(placeholder.getB()));
        }

        tag.put("left", leftTag);
        tag.put("right", rightTag);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        placeholders.clear();

        ArrayList<String> left = new ArrayList<>();
        ArrayList<String> right = new ArrayList<>();

        if (tag.contains("left") && tag.contains("right")) {
            NbtList leftTag = tag.getList("left", 8);
            NbtList rightTag = tag.getList("right", 8);

            for (NbtElement element : leftTag) left.add(element.asString());
            for (NbtElement element : rightTag) right.add(element.asString());

            for (int i = 0; i < left.size() && i < right.size(); i++) {
                placeholders.add(new Pair<>(left.get(i), right.get(i)));
            }
        } else {
            placeholders = new ArrayList<>() {{
                add(new Pair<>(":vector:", "https://cally72jhb.github.io/website/"));
                add(new Pair<>(":skull:", "☠"));
                add(new Pair<>(":sword:", "\uD83D\uDDE1"));
                add(new Pair<>(":blade:", "\uD83D\uDDE1"));
                add(new Pair<>(":bow:", "\uD83C\uDFF9"));
                add(new Pair<>(":trident:", "\uD83D\uDD31"));
                add(new Pair<>(":potion:", "\uD83E\uDDEA"));
                add(new Pair<>(":bottle:", "⚗"));
                add(new Pair<>(":rod:", "\uD83C\uDFA3"));
                add(new Pair<>(":shield:", "\uD83D\uDEE1"));
            }};
        }

        return super.fromTag(tag);
    }
}
