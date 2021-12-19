package cally72jhb.addon.utils.titlescreen.modules;

import cally72jhb.addon.utils.titlescreen.CustomTitleScreen;
import cally72jhb.addon.utils.titlescreen.ScreenRenderer;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.AccountsScreen;
import meteordevelopment.meteorclient.gui.screens.ProxiesScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public abstract class PressableElement extends ScreenElement {
    protected final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Action> action;

    protected final Setting<Boolean> big = sgGeneral.add(new BoolSetting.Builder()
        .name("big")
        .description("Whether or not to render the font higher quality.")
        .defaultValue(true)
        .build()
    );

    protected final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the text.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    protected final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the displayed text.")
        .defaultValue(3)
        .min(0.5)
        .max(50)
        .sliderMin(0)
        .sliderMax(5)
        .build()
    );

    protected final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("How much the text is offset vertically.")
        .defaultValue(5)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    protected final Setting<Double> xOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("x-offset")
        .description("How much the text is offset horizontally.")
        .defaultValue(5)
        .min(0)
        .max(500)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );

    private final String name;
    private final boolean text;

    public PressableElement(CustomTitleScreen hud, String name, Action action, boolean text) {
        super(hud, name, "A custom button.", true);

        this.name = name;
        this.text = text;
        this.action = sgGeneral.add(new EnumSetting.Builder<Action>()
            .name("action")
            .description("What action to perform when you press the button.")
            .defaultValue(action)
            .build()
        );
    }

    @Override
    public void update(ScreenRenderer renderer) {
        if (text) box.setSize((renderer.textWidth(name) + xOffset.get() * 2) * scale.get(), (renderer.textHeight() + yOffset.get() * 2) * scale.get());
    }

    @Override
    public void render(ScreenRenderer renderer) {
        if (text) renderer.text(name, scale.get(), box.getX() + xOffset.get() * scale.get(), box.getY() + yOffset.get() * scale.get(), big.get(), color.get());
    }

    @Override
    public void onPress() {
        switch (action.get()) {
            default -> {}
            case QUIT -> mc.scheduleStop();
            case LOGIN -> mc.setScreen(new AccountsScreen(GuiThemes.get()));
            case PROXY -> mc.setScreen(new ProxiesScreen(GuiThemes.get()));
            case OPTIONS -> mc.setScreen(new OptionsScreen(CustomTitleScreen.INSTANCE, mc.options));
            case MULTIPLAYER -> mc.setScreen(new MultiplayerScreen(CustomTitleScreen.INSTANCE));
            case SINGLEPLAYER -> mc.setScreen(new SelectWorldScreen(CustomTitleScreen.INSTANCE));
        }
    }

    public SettingGroup getSettings() {
        return sgGeneral;
    }

    public double getScale() {
        return scale.get();
    }

    public boolean isBig() {
        return big.get();
    }

    // Enums

    public enum Action {
        NONE,
        SINGLEPLAYER,
        MULTIPLAYER,
        OPTIONS,
        LOGIN,
        PROXY,
        QUIT
    }
}
