package cally72jhb.addon.system.hud;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.meteorclient.mixin.ChatHudAccessor;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.misc.text.ColoredText;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.Formatting.*;

public class ExternalChatHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBox = settings.createGroup("Chat Box");

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("The text scale.")
        .defaultValue(0.75)
        .min(0.5)
        .max(3)
        .sliderMin(1)
        .sliderMax(3)
        .build()
    );

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
        .name("width")
        .description("The width of the chatbox.")
        .defaultValue(475)
        .min(100)
        .max(1000)
        .sliderMin(300)
        .sliderMax(800)
        .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("The height of the chatbox.")
        .defaultValue(135)
        .min(60)
        .max(500)
        .sliderMin(150)
        .sliderMax(300)
        .build()
    );

    private final Setting<Boolean> background = sgBox.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBox.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of background.")
        .defaultValue(new SettingColor(0, 0, 0, 100))
        .visible(background::get)
        .build()
    );

    public ExternalChatHud(HUD hud) {
        super(hud, "custom-chat", "Displays a custom chat.", true);

        String[] a = "aabcdefghijklmnopqrrstuvwxyz<>^<>>".split("");
        String[] b = "ʌᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀɾsᴛᴜᴠᴡxʏᴢ←→↑«»Â".split("");
        for (int i = 0; i < b.length; i++) SMALL_CAPS.put(b[i].charAt(0), a[i].charAt(0));
    }

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    double textX = box.getX();
    double textY = box.getY();
    double ty;
    double tx;

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(width.get(), height.get());

        textX = box.getX();
        textY = box.getY() + box.height - scale.get() * 20;
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (background.get()) {
            double offset = 5 * scale.get();
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x - offset, y - offset, box.width + offset * 2, box.height + offset * 2, backgroundColor.get());
            Renderer2D.COLOR.render(null);
        }

        ChatHudAccessor accessor = ((ChatHudAccessor) mc.inGameHud.getChatHud());
        List<ChatHudLine<Text>> lines = accessor.getMessages();

        if (isInEditor() && mc.world == null) {
            if (!lines.isEmpty()) lines.clear();
            lines.add(new ChatHudLine<>(0, new LiteralText("cally pooped in here lol"), 0));
            lines.add(new ChatHudLine<>(1, new LiteralText("this is the second line").append(new LiteralText("(code written by cally)").setStyle(getStyle(GRAY))), 1));
            lines.add(new ChatHudLine<>(2, new LiteralText("nuffin here").setStyle(getStyle(BLUE)), 2));
            lines.add(new ChatHudLine<>(3, new LiteralText("vector on top").setStyle(getStyle(LIGHT_PURPLE)), 3));
            lines.add(new ChatHudLine<>(4, new LiteralText("this took me way too long").setStyle(getStyle(DARK_GRAY)), 4));
            lines.add(new ChatHudLine<>(5, new LiteralText("pwweasse").setStyle(getStyle(RED)), 5));
            lines.add(new ChatHudLine<>(6, new LiteralText("don't kill me").setStyle(getStyle(RED)), 6));
            lines.add(new ChatHudLine<>(7, new LiteralText("Just a random text thats long enough to demonstrate you linespacing. You can even change the scale of this text. This is only one line btw.").setStyle(getStyle(YELLOW)), 7));
        }

        if (!lines.isEmpty()) {
            for (ChatHudLine<Text> line : new ArrayList<>(lines)) {
                if (textY > (y + box.height)) return;

                List<ColoredText> texts = TextUtils.toColoredTextList(line.getText());

                if (texts.isEmpty()) return;

                tx = textX;
                ty = textY;

                for (ColoredText text : texts) {
                    StringBuilder sb = new StringBuilder();

                    for (char ch : text.getText().toCharArray()) {
                        if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
                        else sb.append(ch);
                    }

                    String[] strings = sb.toString().split(" ");

                    for (String string : strings) {
                        double width = TextRenderer.get().getWidth(string + " ");
                        string = string.replace("§", "");

                        if ((tx + width) > (x + box.width)) {
                            textY -= scale.get() * 20;
                            ty = textY;
                            tx = textX;
                        }

                        if (ty > (y + box.height - scale.get() * 20) || ty < y) return;

                        renderScaledText(scale.get(), string + " ", tx, ty, text.getColor());

                        tx += width * scale.get();
                    }
                }

                textY -= scale.get() * 20;
            }
        }
    }

    // Utils

    private void renderScaledText(Double scale, String string, double x, double y, Color color) {
        if (TextRenderer.get().isBuilding()) TextRenderer.get().end();
        TextRenderer.get().begin(scale);
        TextRenderer.get().render(string, x, y, color, false);
        TextRenderer.get().end();
        if (!TextRenderer.get().isBuilding()) TextRenderer.get().begin(Systems.get(HUD.class).scale.get(), false, false);
    }

    // Formatting

    private Style getStyle(Formatting formatting) {
        return Style.EMPTY.withFormatting(formatting);
    }
}
