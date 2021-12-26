package cally72jhb.addon.utils.titlescreen;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;

public class TitleScreenConfigScreen extends WindowScreen {
    private static final Settings settings = new Settings();
    private static final SettingGroup sgGeneral = settings.getDefaultGroup();

    public TitleScreenConfigScreen(GuiTheme theme) {
        super(theme, "TitleScreenConfig");

        settings.onActivated();
    }

    @Override
    public void initWidgets() {
        add(theme.settings(settings)).expandX();
    }

    @Override
    public void tick() {
        super.tick();

        settings.tick(window, theme);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(TitleScreenConfig.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(TitleScreenConfig.get());
    }
}
