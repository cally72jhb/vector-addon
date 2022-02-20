package cally72jhb.addon.system.titlescreen.modules;

import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.system.titlescreen.TitleScreenRenderer;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CreditsElement extends PressableElement {
    private final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("How much the text is offset vertically.")
        .defaultValue(20)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> xOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("x-offset")
        .description("How much the text is offset horizontally.")
        .defaultValue(20)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private static final List<Credit> credits = new ArrayList<>();

    public CreditsElement(TitleScreenManager manager, String name, Action action, boolean defaultActive) {
        super(manager, name, action, 1, defaultActive);
    }

    private void init() {
        add(AddonManager.METEOR);
        for (MeteorAddon addon : AddonManager.ADDONS) add(addon);

        credits.sort(Comparator.comparingInt(value -> value.sections.get(0).text.equals("Meteor Client ") ? Integer.MIN_VALUE : -value.width));
    }

    @Override
    public void update(TitleScreenRenderer renderer) {
        if (credits.isEmpty()) init();
        else {
            double width = 0;
            for (Section section : credits.get(0).sections) width += section.width;

            box.setSize((width + xOffset.get() * 2) * scale.get(), (renderer.textHeight() * getScale() * credits.size() + yOffset.get() * 2) * scale.get());
        }
    }

    @Override
    public void render(TitleScreenRenderer renderer) {
        if (credits.isEmpty()) init();

        double y = box.getY() + yOffset.get() * getScale();

        for (Credit credit : credits) {
            double x = box.getX() + xOffset.get() * getScale();

            for (Section section : credit.sections) {
                renderer.text(section.text, getScale(), x, y, false, section.color);
                x += renderer.textWidth(section.text) * getScale();
            }

            y += renderer.textHeight() * getScale();
        }
    }

    private void add(MeteorAddon addon) {
        Credit credit = new Credit();

        credit.sections.add(new Section(addon.name + " ", addon.color));
        credit.sections.add(new Section("by ", new Color(255, 255, 255)));

        for (int i = 0; i < addon.authors.length; i++) {
            if (i > 0) credit.sections.add(new Section(i == addon.authors.length - 1 ? " & " : ", ", new Color(255, 255, 255)));

            credit.sections.add(new Section(addon.authors[i], new Color(255, 255, 255)));
        }

        credits.add(credit);
    }

    private static class Credit {
        public final List<Section> sections = new ArrayList<>();
        public int width;
    }

    private class Section {
        public final String text;
        public final Color color;
        public double width;

        public Section(String text, Color color) {
            this.text = text;
            this.color = color;
            this.width = mc.textRenderer.getWidth(text);
        }
    }
}
