package cally72jhb.addon.gui.tabs;

import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

public class VectorConfigTab extends Tab {
    private static final Settings settings = new Settings();

    private static final SettingGroup sgChat = settings.createGroup("Chat");
    private static final SettingGroup sgMisc = settings.createGroup("Misc");

    // Chat

    public static final Setting<String> name = sgChat.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the addon.")
            .defaultValue("Vector")
            .onChanged(string -> {
                VectorConfig.get().name = string;
                ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
            })
            .onModuleActivated(string -> string.set(VectorConfig.get().name))
            .build()
    );

    public static final Setting<String> prefix = sgChat.add(new StringSetting.Builder()
        .name("prefix")
        .description("The prefix of the addon.")
        .defaultValue("[")
        .onChanged(string -> {
            VectorConfig.get().prefix = string;
            ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
        })
        .onModuleActivated(string -> string.set(VectorConfig.get().prefix))
        .build()
    );

    public static final Setting<String> suffix = sgChat.add(new StringSetting.Builder()
        .name("suffix")
        .description("The suffix of the addon.")
        .defaultValue("]")
        .onChanged(string -> {
            VectorConfig.get().suffix = string;
            ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
        })
        .onModuleActivated(string -> string.set(VectorConfig.get().suffix))
        .build()
    );

    public static final Setting<SettingColor> nameColor = sgMisc.add(new ColorSetting.Builder()
        .name("name-color")
        .description("The default color for the vector name.")
        .defaultValue(new SettingColor(140, 245, 165))
        .onChanged(color -> {
            VectorConfig.get().nameColor = color;
            ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
        })
        .onModuleActivated(color -> color.set(VectorConfig.get().nameColor))
        .build()
    );

    public static final Setting<SettingColor> otherColor = sgMisc.add(new ColorSetting.Builder()
        .name("other-color")
        .description("The default color for the prefix and suffix.")
        .defaultValue(new SettingColor(100, 100, 100))
        .onChanged(color -> {
            VectorConfig.get().otherColor = color;
            ChatUtils.registerCustomPrefix("cally72jhb.addon", VectorUtils::getPrefix);
        })
        .onModuleActivated(color -> color.set(VectorConfig.get().otherColor))
        .build()
    );

    public static final Setting<Boolean> windowIcon = sgMisc.add(new BoolSetting.Builder()
            .name("window-icon")
            .description("Changes the window-icon to vector.")
            .defaultValue(true)
            .onChanged(bool -> VectorConfig.get().windowIcon = bool)
            .onModuleActivated(bool -> {
                bool.set(VectorConfig.get().windowIcon);
                VectorUtils.changeIcon();
            }).build()
    );

    public static ConfigScreen currentScreen;

    public VectorConfigTab() {
        super("Vector Config");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return currentScreen = new ConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigScreen;
    }

    public static class ConfigScreen extends WindowTabScreen {
        public ConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

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
            return NbtUtils.toClipboard(VectorConfig.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(VectorConfig.get());
        }
    }
}
