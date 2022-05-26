package cally72jhb.addon.system.modules.render.notexture;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import java.util.Map;

public class NoTextures extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgFluid = settings.createGroup("Fluids");

    // General

    private final Setting<Boolean> reloadChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("reload-chunks")
        .description("Reloads the chunks when needed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableCulling = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-culling")
        .description("Disables minecraft's culling algorithm.")
        .defaultValue(true)
        .build()
    );

    // Blocks

    private final Setting<ColorMode> colorMode = sgBlocks.add(new EnumSetting.Builder<ColorMode>()
        .name("color-mode")
        .description("How colors are chosen for specific blocks.")
        .defaultValue(ColorMode.Automatic)
        .onChanged(color -> reload())
        .build()
    );

    private final Setting<SettingColor> defaultSideColor = sgBlocks.add(new ColorSetting.Builder()
        .name("default-side-color")
        .description("The default color of a blocks sides.")
        .defaultValue(new SettingColor(255, 255, 225, 255))
        .onChanged(color -> reload())
        .visible(() -> colorMode.get() == ColorMode.Custom)
        .build()
    );

    private final Setting<Map<Block, BlockData>> blockConfigs = sgBlocks.add(new BlockDataSetting.Builder<BlockData>()
        .name("block-configs")
        .description("Config for each block.")
        .defaultData(
            new GenericSetting.Builder<BlockData>()
                .name("block-config")
                .description("How to render blocks.")
                .defaultValue(new BlockData(new SettingColor(255, 255, 255), true))
                .build()
        )
        .onChanged(map -> reload())
        .visible(() -> colorMode.get() == ColorMode.Custom)
        .build()
    );

    // Fluids

    private final Setting<SettingColor> waterColor = sgFluid.add(new ColorSetting.Builder()
        .name("water-color")
        .description("The color of the water.")
        .defaultValue(new SettingColor(62, 117, 225, 125))
        .onChanged(color -> reload())
        .build()
    );

    private final Setting<SettingColor> lavaColor = sgFluid.add(new ColorSetting.Builder()
        .name("lava-color")
        .description("The color of the lava.")
        .defaultValue(new SettingColor(252, 104, 0, 255))
        .onChanged(color -> reload())
        .build()
    );

    private int ticksExisted;

    public NoTextures() {
        super(Categories.Misc, "no-textures", "Disables texture rendering.");
    }

    @Override
    public void onActivate() {
        if (mc != null && mc.world != null && ticksExisted >= 80) mc.reloadResources();

        reload();
    }

    @Override
    public void onDeactivate() {
        reload();
    }

    private void reload() {
        if (mc != null && mc.world != null && reloadChunks.get()) {
            mc.worldRenderer.reload();
        }

        ticksExisted = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (ticksExisted <= 80) ticksExisted++;
    }

    // Getter

    public boolean disableCulling() {
        return disableCulling.get();
    }

    public boolean isAutomatic() {
        return colorMode.get() == ColorMode.Automatic;
    }

    public boolean isAutomatic(BlockState state) {
        Map<Block, BlockData> blocks = blockConfigs.get();

        if (blocks.isEmpty()) {
            return true;
        } else {
            BlockData data = blocks.get(state.getBlock());
            return data == null || data.automatic || !blocks.containsKey(state.getBlock());
        }
    }

    public Color getColor(BlockState state) {
        BlockData data = blockConfigs.get().get(state.getBlock());
        return data == null ? defaultSideColor.get() : data.sideColor;
    }

    public Color getWaterColor() {
        return waterColor.get();
    }

    public Color getLavaColor() {
        return lavaColor.get();
    }

    // Enums

    public enum ColorMode {
        Automatic,
        Custom
    }
}
