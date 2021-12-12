package cally72jhb.addon.gui.tabs;

import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

public class VectorConfigTab extends Tab {
    private static final Settings settings = new Settings();

    private static final SettingGroup sgChat = settings.createGroup("Chat");
    private static final SettingGroup sgMisc = settings.createGroup("Misc");

    // Chat

    public static final Setting<String> clientName = sgChat.add(new StringSetting.Builder()
            .name("client-name")
            .description("The Client-Name.")
            .defaultValue("Vector")
            .onChanged(string -> VectorConfig.get().clientName = string)
            .onModuleActivated(string -> string.set(VectorConfig.get().clientName))
            .build()
    );

    public static final Setting<String> clientPrefix = sgChat.add(new StringSetting.Builder()
            .name("client-prefix")
            .description("The Client-Prefix.")
            .defaultValue("[")
            .onChanged(string -> VectorConfig.get().clientPrefix = string)
            .onModuleActivated(string -> string.set(VectorConfig.get().clientPrefix))
            .build()
    );

    public static final Setting<String> clientSuffix = sgChat.add(new StringSetting.Builder()
            .name("client-suffix")
            .description("The Client-Suffix.")
            .defaultValue("]")
            .onChanged(string -> VectorConfig.get().clientSuffix = string)
            .onModuleActivated(string -> string.set(VectorConfig.get().clientSuffix))
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

    public static final Setting<Boolean> highlightMembers = sgMisc.add(new BoolSetting.Builder()
            .name("highlight-members")
            .description("Highlights Vector Users for you.")
            .defaultValue(true)
            .onChanged(bool -> VectorConfig.get().highlightMembers = bool)
            .onModuleActivated(bool -> {
                bool.set(VectorConfig.get().highlightMembers);
                VectorUtils.members();
            }).build()
    );

    public static final Setting<SettingColor> memberColor = sgMisc.add(new ColorSetting.Builder()
            .name("member-color")
            .description("The color the Vector Users will be highlighted with.")
            .defaultValue(new SettingColor(255, 255, 145))
            .onChanged(color -> VectorConfig.get().memberColor = color)
            .onModuleActivated(color -> color.set(VectorConfig.get().memberColor))
            .visible(highlightMembers::get)
            .build()
    );

    public static VectorConfigTab.ConfigScreen currentScreen;

    public VectorConfigTab() {
        super("Vector Config");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return currentScreen = new VectorConfigTab.ConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof VectorConfigTab.ConfigScreen;
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
