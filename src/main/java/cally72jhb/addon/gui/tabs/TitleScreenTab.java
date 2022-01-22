package cally72jhb.addon.gui.tabs;

import cally72jhb.addon.gui.screens.TitleScreen;
import cally72jhb.addon.gui.screens.TitleScreenEditor;
import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TitleScreenTab extends Tab {
    public TitleScreenTab() {
        super("Title Screen");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new TabTitleScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof TabTitleScreen;
    }

    public static class TabTitleScreen extends WindowTabScreen {
        private final TitleScreenManager titleScreenManager;

        public TabTitleScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            this.parent = new net.minecraft.client.gui.screen.TitleScreen();

            titleScreenManager = Systems.get(TitleScreenManager.class);
            titleScreenManager.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(titleScreenManager.settings)).expandX();
            add(theme.horizontalSeparator()).expandX();

            WButton openEditor = add(theme.button("Edit")).expandX().widget();
            openEditor.action = () -> mc.setScreen(new TitleScreenEditor(theme, this));

            WButton resetTitleScreen = add(theme.button("Reset")).expandX().widget();
            resetTitleScreen.action = titleScreenManager.reset;

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

            bottom.add(theme.label("Active: "));
            WCheckbox active = bottom.add(theme.checkbox(titleScreenManager.active)).expandCellX().widget();
            active.action = () -> titleScreenManager.active = active.checked;

            WButton resetSettings = bottom.add(theme.button(GuiRenderer.RESET)).widget();
            resetSettings.action = titleScreenManager.settings::reset;
        }

        @Override
        public void onClose() {
            super.onClose();
            if (VectorUtils.mc != null && MeteorClient.mc.world == null) VectorUtils.mc.setScreen(new TitleScreen());
            else VectorUtils.mc.setScreen(null);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard("title-screen-settings", titleScreenManager.settings.toTag());
        }

        @Override
        public boolean fromClipboard() {
            NbtCompound clipboard = NbtUtils.fromClipboard(titleScreenManager.settings.toTag());

            if (clipboard != null) {
                titleScreenManager.settings.fromTag(clipboard);
                return true;
            }

            return false;
        }
    }
}
