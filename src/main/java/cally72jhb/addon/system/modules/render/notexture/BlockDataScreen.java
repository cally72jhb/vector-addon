package cally72jhb.addon.system.modules.render.notexture;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;

public class BlockDataScreen extends WindowScreen {
    private final BlockData data;
    private final Block block;
    private final BlockDataSetting<BlockData> setting;

    public BlockDataScreen(GuiTheme theme, BlockData data, Block block, BlockDataSetting<BlockData> setting) {
        super(theme, "Configure Block");

        this.data = data;
        this.block = block;
        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        Settings settings = new Settings();
        SettingGroup sgGeneral = settings.getDefaultGroup();

        sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("Color of sides of the block.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .onModuleActivated(setting -> setting.set(data.sideColor))
            .onChanged(color -> {
                data.sideColor.set(color);
                changed(data, block, setting);
            })
            .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
            .name("automatic")
            .description("Automatically uses the best color for the block.")
            .defaultValue(true)
            .onModuleActivated(setting -> setting.set(data.automatic))
            .onChanged(bool -> {
                data.automatic = bool;
                changed(data, block, setting);
            })
            .build()
        );

        settings.onActivated();
        add(theme.settings(settings)).expandX();
    }

    private void changed(BlockData data, Block block, BlockDataSetting<BlockData> setting) {
        if (!data.isChanged() && block != null && setting != null) {
            setting.get().put(block, data);
            setting.onChanged();
        }

        data.changed();
    }
}
