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
        private final TitleScreenManager manager;

        public TabTitleScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            this.parent = new net.minecraft.client.gui.screen.TitleScreen();

            manager = TitleScreenManager.get();
            manager.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(manager.settings)).expandX();
            add(theme.horizontalSeparator()).expandX();

            WButton openEditor = add(theme.button("Edit")).expandX().widget();
            openEditor.action = () -> mc.setScreen(new TitleScreenEditor(theme, this));

            WButton resetTitleScreen = add(theme.button("Reset")).expandX().widget();
            resetTitleScreen.action = manager.reset;

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

            bottom.add(theme.label("Active: "));
            WCheckbox active = bottom.add(theme.checkbox(manager.active)).expandCellX().widget();
            active.action = () -> manager.active = active.checked;

            WButton resetSettings = bottom.add(theme.button(GuiRenderer.RESET)).widget();
            resetSettings.action = manager.settings::reset;
        }

        @Override
        public void onClose() {
            super.onClose();
            if (VectorUtils.mc != null && MeteorClient.mc.world == null && manager.active) VectorUtils.mc.setScreen(new TitleScreen());
            else VectorUtils.mc.setScreen(null);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard("title-screen-settings", manager.settings.toTag());
        }

        @Override
        public boolean fromClipboard() {
            NbtCompound clipboard = NbtUtils.fromClipboard(manager.settings.toTag());

            if (clipboard != null) {
                manager.settings.fromTag(clipboard);
                return true;
            }

            return false;
        }
    }
}
