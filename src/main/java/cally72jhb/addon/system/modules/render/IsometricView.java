package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class IsometricView extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScrolling = settings.createGroup("Scrolling");

    private final Setting<Double> cameraDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The distance the camera is away from you.")
        .defaultValue(10)
        .min(1)
        .build()
    );

    private final Setting<Boolean> scrollingEnabled = sgScrolling.add(new BoolSetting.Builder()
        .name("scrolling-enabled")
        .description("Allows you to scroll to change camera distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> scrollSensitivity = sgScrolling.add(new DoubleSetting.Builder()
        .name("scroll-sensitivity")
        .description("Scroll sensitivity when changing the cameras distance.")
        .visible(scrollingEnabled::get)
        .defaultValue(1)
        .min(0)
        .build()
    );

    public double distance;

    public IsometricView() {
        super(VectorAddon.CATEGORY, "isometric-view", "Renders your world isometric. Why would you want this.");
    }

    @Override
    public void onActivate() {
        distance = cameraDistance.get();
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (mc.currentScreen != null || !scrollingEnabled.get()) return;

        if (scrollSensitivity.get() > 0) {
            distance -= event.value * 0.25 * (scrollSensitivity.get() * distance);

            event.cancel();
        }
    }

    public double getDistance() {
        return distance;
    }
}
