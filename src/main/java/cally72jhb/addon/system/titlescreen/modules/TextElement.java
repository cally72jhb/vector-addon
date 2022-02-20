package cally72jhb.addon.system.titlescreen.modules;

import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.system.titlescreen.TitleScreenRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;

public class TextElement extends PressableElement {
    private final Setting<String> text = sgGeneral.add(new StringSetting.Builder()
        .name("text")
        .description("What text to display.")
        .defaultValue(name)
        .build()
    );

    private final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("How much the text is offset vertically.")
        .defaultValue(5)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> xOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("x-offset")
        .description("How much the text is offset horizontally.")
        .defaultValue(5)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> big = sgGeneral.add(new BoolSetting.Builder()
        .name("big")
        .description("Whether or not to render the font with big letters.")
        .defaultValue(false)
        .build()
    );

    public TextElement(TitleScreenManager manager, String name, Action action, boolean defaultActive) {
        super(manager, name, action, 3, defaultActive);
    }

    @Override
    public void update(TitleScreenRenderer renderer) {
        box.setSize((renderer.textWidth(text.get()) + xOffset.get() * 2) * scale.get(), (renderer.textHeight() + yOffset.get() * 2) * scale.get());
    }

    @Override
    public void render(TitleScreenRenderer renderer) {
        renderer.text(text.get(), scale.get(), box.getX() + xOffset.get() * scale.get(), box.getY() + yOffset.get() * scale.get(), big.get(), color.get());
    }
}
