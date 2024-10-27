package cally72jhb.addon.modules.player;

import cally72jhb.addon.utils.Utils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public class AutoRagequit extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("Messages to use for spam.")
        .defaultValue("/lobby")
        .build()
    );

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Toggles the module after it ran.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Vector3d> sphereCenter = sgGeneral.add(new Vector3dSetting.Builder()
        .name("sphere-center")
        .description("the center of the sphere.")
        .defaultValue(0.0, 0.0, 0.0)
        .sliderMin(-1.0)
        .sliderMax(1.0)
        .decimalPlaces(3)
        .noSlider()
        .build()
    );

    private final Setting<Double> sphereRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("sphere-radius")
        .description("how far you have to be from the center to rage quit.")
        .defaultValue(20.0)
        .min(0.0)
        .sliderMin(1.0)
        .sliderMax(50.0)
        .build()
    );

    private final Setting<Boolean> ignoreXAxis = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-x-axis")
        .description("If the x axis should be ignored.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreYAxis = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-y-axis")
        .description("If the y axis should be ignored.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreZAxis = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-z-axis")
        .description("If the z axis should be ignored.")
        .defaultValue(false)
        .build()
    );

    // Constructor

    public AutoRagequit() {
        super(Categories.Misc, "auto-ragequit", "Rages the moment you loose.");
    }

    // Tick Event

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        Vec3d pos = mc.player.getPos();
        Vec3d sphere = new Vec3d(sphereCenter.get().x, sphereCenter.get().y, sphereCenter.get().z);

        boolean x = Math.abs(pos.getX() - sphere.getX()) >= sphereRadius.get();
        boolean y = Math.abs(pos.getX() - sphere.getX()) >= sphereRadius.get();
        boolean z = Math.abs(pos.getX() - sphere.getX()) >= sphereRadius.get();

        if ((!ignoreXAxis.get() && x) || (!ignoreYAxis.get() && y) || (!ignoreZAxis.get() && z)
            || !ignoreXAxis.get() && !ignoreYAxis.get() && !ignoreZAxis.get() && Utils.distance(pos, sphere)   >= sphereRadius.get()
            || !ignoreXAxis.get() && !ignoreZAxis.get()                       && Utils.distanceXZ(pos, sphere) >= sphereRadius.get()) {

            ChatUtils.sendPlayerMsg(message.get());

            if (autoToggle.get()) {
                this.toggle();
                return;
            }
        }
    }
}
