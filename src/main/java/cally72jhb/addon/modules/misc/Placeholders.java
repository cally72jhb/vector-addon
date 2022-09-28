package cally72jhb.addon.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class Placeholders extends Module {
    private List<Pair<String, String>> placeholders = new ArrayList<>();
    private boolean cancel;

    public Placeholders() {
        super(Categories.Misc, "placeholders", "Replaces chat messages with your own custom placeholders.");
    }

    @Override
    public void onActivate() {
        cancel = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ChatMessageC2SPacket packet && !placeholders.isEmpty() && !cancel) {
            for (Pair<String, String> placeholder : placeholders) {
                if (placeholder != null && placeholder.getA() != null && packet.chatMessage().contains(placeholder.getA())) {
                    cancel = true;
                    mc.player.sendChatMessage(packet.chatMessage().replaceAll(placeholder.getA(), placeholder.getB()), Text.of(packet.chatMessage().replaceAll(placeholder.getA(), placeholder.getB())));
                    cancel = false;

                    event.cancel();
                }
            }
        }
    }

    // Placeholders

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        if (!placeholders.isEmpty()) {
            placeholders.removeIf(placeholder -> !isValidPlaceholder(placeholder.getA()));

            for (int i = 0; i < placeholders.size(); i++) {
                int messageI = i;

                if (placeholders.get(i) != null && placeholders.get(i).getA() != null && placeholders.get(i).getB() != null
                    && !placeholders.get(i).getA().isEmpty() && !placeholders.get(i).getB().isEmpty()) {

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
                } else if (placeholders.get(i) != null && placeholders.get(i).getA() == null && placeholders.get(i).getB() != null) {
                    WMinus delete = table.add(theme.minus()).widget();
                    delete.action = () -> {
                        placeholders.remove(messageI);

                        table.clear();
                        fillTable(theme, table);
                    };

                    table.add(theme.label(placeholders.get(i).getB()));
                    table.row();
                }
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
            resetPlaceholders();

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

        placeholders.removeIf(placeholder -> !isValidPlaceholder(placeholder.getA()));

        NbtList leftTag = new NbtList();
        NbtList rightTag = new NbtList();

        for (Pair<String, String> placeholder : placeholders) {
            leftTag.add(placeholder.getA() == null ? NbtString.of("") : NbtString.of(placeholder.getA().replace("\\", "\\")));
            rightTag.add(NbtString.of(placeholder.getB()));
        }

        tag.put("left", leftTag);
        tag.put("right", rightTag);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        placeholders.clear();

        List<String> left = new ArrayList<>();
        List<String> right = new ArrayList<>();

        if (tag.contains("left") && tag.contains("right")) {
            NbtList leftTag = tag.getList("left", 8);
            NbtList rightTag = tag.getList("right", 8);

            for (NbtElement element : leftTag) left.add(element.asString().isEmpty() ? null : element.asString());
            for (NbtElement element : rightTag) right.add(element.asString());

            for (int i = 0; i < left.size() && i < right.size(); i++) {
                placeholders.add(new Pair<>(left.get(i), right.get(i)));
            }
        } else {
            resetPlaceholders();
        }

        placeholders.removeIf(placeholder -> !isValidPlaceholder(placeholder.getA()));

        return super.fromTag(tag);
    }

    private void resetPlaceholders() {
        placeholders = new ArrayList<>() {{
            add(new Pair<>(":skull:", "☠"));
            add(new Pair<>(":sword:", "\uD83D\uDDE1"));
            add(new Pair<>(":pick:", "⛏"));
            add(new Pair<>(":blade:", "⚔"));
            add(new Pair<>(":bow:", "\uD83C\uDFF9"));
            add(new Pair<>(":trident:", "\uD83D\uDD31"));
            add(new Pair<>(":fork:", "ψ"));
            add(new Pair<>(":rod:", "\uD83C\uDFA3"));
            add(new Pair<>(":shield:", "\uD83D\uDEE1"));

            add(new Pair<>(null, "Containers"));

            add(new Pair<>(":potion:", "\uD83E\uDDEA"));
            add(new Pair<>(":bottle:", "⚗"));
            add(new Pair<>(":bucket:", "\uD83E\uDEA3"));

            add(new Pair<>(null, "Directions"));

            add(new Pair<>(":right:", "»"));
            add(new Pair<>(":left:", "«"));
            add(new Pair<>(":up:", "↑"));
            add(new Pair<>(":down:", "↓"));

            add(new Pair<>(null, "Faces"));

            add(new Pair<>(":smiley-1:", "☻"));
            add(new Pair<>(":smiley-2:", "☺"));
            add(new Pair<>(":smiley-3:", "☹"));

            add(new Pair<>(":face-1:", "( ❛ʖ ❛)"));
            add(new Pair<>(":face-2:", "ಠ_ಠ"));
            add(new Pair<>(":face-3:", "( ¯ʖ¯)"));
            add(new Pair<>(":face-4:", "¯\\_( •ʖ •)_/¯"));
            add(new Pair<>(":face-5:", "¯\\_( ❛ʖ ❛)_/¯"));

            add(new Pair<>(null, "Environmental"));

            add(new Pair<>(":cloud:", "☁"));
            add(new Pair<>(":rain-cloud:", "\uD83C\uDF27"));
            add(new Pair<>(":thunder-cloud:", "⛈"));
            add(new Pair<>(":thunder:", "⚡"));
            add(new Pair<>(":fire:", "\uD83D\uDD25"));
            add(new Pair<>(":star-1:", "⭐"));
            add(new Pair<>(":star-2:", "☆"));
            add(new Pair<>(":meteor:", "☄"));
            add(new Pair<>(":sun:", "☀"));
            add(new Pair<>(":moon:", "☽"));
            add(new Pair<>(":snow:", "❄"));
            add(new Pair<>(":atomic:", "☣"));

            add(new Pair<>(null, "Other"));

            add(new Pair<>(":hearth:", "❤"));
            add(new Pair<>(":food:", "\uD83C\uDF56"));
            add(new Pair<>(":anchor:", "⚓"));
            add(new Pair<>(":scissor:", "✂"));
            add(new Pair<>(":mail:", "✉"));
            add(new Pair<>(":yin-yang:", "☯"));
            add(new Pair<>(":peace:", "☮"));
            add(new Pair<>(":infinity:", "∞"));
            add(new Pair<>(":umbrella:", "☂"));
            add(new Pair<>(":cock:", "╭ᑎ╮"));
            add(new Pair<>(":dick:", "┌▎┐"));
            add(new Pair<>(":penis:", "┌∩┐"));
            add(new Pair<>(":?:", "�"));
            add(new Pair<>(":tm:", "™"));
            add(new Pair<>(":reg:", "®"));
            add(new Pair<>(":copy:", "©"));

            add(new Pair<>(":note-1:", "♩"));
            add(new Pair<>(":note-2:", "♪"));
            add(new Pair<>(":note-3:", "♫"));
            add(new Pair<>(":note-4:", "♬"));

            add(new Pair<>(":clock-1:", "⌚"));
            add(new Pair<>(":clock-2:", "⌛"));
            add(new Pair<>(":clock-3:", "⏳"));

            add(new Pair<>(":vector:", "https://cally72jhb.github.io/website/"));
        }};
    }

    private boolean isValidPlaceholder(String placeholder) {
        return placeholder != null && !placeholder.isEmpty() && !placeholder.contains("\\") && !placeholder.contains("$");
    }
}
