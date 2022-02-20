package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;

public class BorderBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers the world-border to a set position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> borderX = sgGeneral.add(new IntSetting.Builder()
        .name("border-pos-x")
        .description("At what x coordinate the world-border should be.")
        .defaultValue(0)
        .visible(center::get)
        .noSlider()
        .build()
    );

    private final Setting<Integer> borderZ = sgGeneral.add(new IntSetting.Builder()
        .name("border-pos-z")
        .description("At what z coordinate the world-border should be.")
        .defaultValue(0)
        .visible(center::get)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> moveToPlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("move-to-player")
        .description("Always centers the world-border to your position.")
        .defaultValue(true)
        .visible(() -> !center.get())
        .build()
    );

    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
        .name("size")
        .description("How big the world-border should be.")
        .defaultValue(250)
        .min(0.25)
        .max(29999984)
        .sliderMin(50)
        .sliderMax(500)
        .build()
    );

    public BorderBypass() {
        super(VectorAddon.Misc, "border-bypass", "Bypasses the world border.");
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!center.get() && moveToPlayer.get()) {
            mc.world.getWorldBorder().setCenter(mc.player.getBlockX() * (PlayerUtils.getDimension() == Dimension.Nether ? 8 : 1), mc.player.getBlockZ() * (PlayerUtils.getDimension() == Dimension.Nether ? 8 : 1));
        } else if (center.get()) {
            mc.world.getWorldBorder().setCenter(borderX.get(), borderZ.get());
        }

        mc.world.getWorldBorder().setSize(size.get());
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        // Pause
        WButton reset = table.add(theme.button("Reset")).widget();
        reset.action = () -> {
            mc.world.getWorldBorder().setCenter(0, 0);
            mc.world.getWorldBorder().setSize(29999984);
        };

        return table;
    }
}
