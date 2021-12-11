package cally72jhb.addon.gui.tabs;

import cally72jhb.addon.system.players.Player;
import cally72jhb.addon.system.players.Players;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

public class PlayersTab extends Tab {
    public PlayersTab() {
        super("Players");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new PlayersScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof PlayersScreen;
    }

    private static class PlayersScreen extends WindowTabScreen {
        private final Settings settings = new Settings();

        public PlayersScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            SettingGroup sgGeneral = settings.getDefaultGroup();

            sgGeneral.add(new ColorSetting.Builder()
                    .name("target-color")
                    .description("The color used to show the targeted players.")
                    .defaultValue(new SettingColor(255, 85, 85))
                    .onChanged(Players.get().color::set)
                    .onModuleActivated(colorSetting -> colorSetting.set(Players.get().color))
                    .build()
            );

            sgGeneral.add(new BoolSetting.Builder()
                    .name("mute")
                    .description("Whether to mute the players.")
                    .defaultValue(false)
                    .onChanged(aBoolean -> Players.get().muted = aBoolean)
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Players.get().muted))
                    .build()
            );

            settings.onActivated();
        }

        @Override
        public void initWidgets() {
            // Settings
            add(theme.settings(settings)).expandX();

            // Players
            WSection players = add(theme.section("Players")).expandX().widget();
            WTable table = players.add(theme.table()).expandX().widget();

            initTable(table);

            // New
            WHorizontalList list = players.add(theme.horizontalList()).expandX().widget();

            WTextBox nameW = list.add(theme.textBox("")).minWidth(400).expandX().widget();
            nameW.setFocused(true);

            WPlus add = list.add(theme.plus()).widget();
            add.action = () -> {
                String name = nameW.get().trim();

                if (Players.get().add(new Player(name))) {
                    nameW.set("");

                    table.clear();
                    initTable(table);
                }
            };

            enterAction = add.action;
        }

        private void initTable(WTable table) {
            for (Player player : Players.get()) {
                WLabel label = theme.label(player.name);
                if (player.target) label.color = new Color(255, 85, 85, 255);
                table.add(label);

                WButton mute = table.add(theme.button(player.muted ? "unmute" : "mute")).expandX().centerX().widget();
                mute.action = () -> {
                    Players.get().mute(player.name);

                    table.clear();
                    initTable(table);
                };

                WButton target = table.add(theme.button(player.target ? "untarget" : "target")).expandX().centerX().widget();
                target.action = () -> {
                    Players.get().target(player.name);

                    table.clear();
                    initTable(table);
                };

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    Players.get().remove(player);

                    table.clear();
                    initTable(table);
                };

                table.row();
            }
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Players.get());
        }

        @Override
        public boolean fromClipboard() {
            NbtCompound clipboard = NbtUtils.fromClipboard(Players.get().toTag());

            if (clipboard != null) {
                Players.get().fromTag(clipboard);
                return true;
            }

            return false;
        }
    }
}
