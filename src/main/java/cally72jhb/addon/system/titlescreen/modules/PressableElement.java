package cally72jhb.addon.system.titlescreen.modules;

import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.AccountsScreen;
import meteordevelopment.meteorclient.gui.screens.ProxiesScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public abstract class PressableElement extends DefaultElement {
    protected final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Action> action;

    protected final Setting<Double> scale;

    protected final Setting<SettingColor> color;

    public final String name;

    public PressableElement(TitleScreenManager manager, String name, Action action, double defaultScale, boolean defaultActive) {
        super(manager, name, "A custom button.", defaultActive);

        this.name = name;

        this.scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the displayed text.")
            .defaultValue(defaultScale)
            .min(0.5)
            .max(50)
            .sliderMin(0)
            .sliderMax(5)
            .build()
        );

        this.action = sgGeneral.add(new EnumSetting.Builder<Action>()
            .name("action")
            .description("What action to perform when you press the button.")
            .defaultValue(action)
            .build()
        );

        this.color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of the text.")
            .defaultValue(new SettingColor(72, 72, 72, 255))
            .build()
        );
    }

    @Override
    public void onPress() {
        switch (action.get()) {
            default -> {}
            case QUIT -> mc.scheduleStop();
            case LOGIN -> mc.setScreen(new AccountsScreen(GuiThemes.get()));
            case PROXY -> mc.setScreen(new ProxiesScreen(GuiThemes.get()));
            case OPTIONS -> mc.setScreen(new OptionsScreen(mc.currentScreen, mc.options));
            case MULTIPLAYER -> mc.setScreen(new MultiplayerScreen(mc.currentScreen));
            case SINGLEPLAYER -> mc.setScreen(new SelectWorldScreen(mc.currentScreen));
        }
    }

    public SettingGroup getSettings() {
        return sgGeneral;
    }

    public double getScale() {
        return scale.get();
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
