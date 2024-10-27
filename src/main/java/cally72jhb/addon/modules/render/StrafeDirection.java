package cally72jhb.addon.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class StrafeDirection extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("The height, starting from the feet.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1.25)
            .build()
    );

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("The distance from the player.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(0.75)
            .build()
    );

    private final Setting<Double> length = sgGeneral.add(new DoubleSetting.Builder()
            .name("length")
            .description("The length of the line.")
            .defaultValue(0.5)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("The lines color.")
            .defaultValue(new SettingColor(255, 64, 64, 255))
            .build()
    );

    private final Setting<Boolean> distanceColor = sgGeneral.add(new BoolSetting.Builder()
            .name("distance-colors")
            .description("Changes the color of skeletons depending on their distance to you.")
            .defaultValue(true)
            .build()
    );

    // Constructor

    public StrafeDirection() {
        super(Categories.Render, "strafe-direction", "Used for diagonal walking in vanilla.");
    }

    // Events

    @EventHandler
    private void onRender(Render3DEvent event) {
        double[] speed = directionSpeed(1);

        double dir = Math.atan(speed[1] / speed[0]);

        Color colorStart = this.color.get();
        Color colorEnd = this.color.get();

        if (this.distanceColor.get()) {
            colorStart = getColorFromDistance(new Vec3d(
                    mc.player.getX() + Math.cos(dir) * this.distance.get(),
                    mc.player.getY() + this.height.get(),
                    mc.player.getZ() + Math.sin(dir) * this.distance.get()
            ));

            colorEnd = getColorFromDistance(new Vec3d(
                    mc.player.getX() + Math.cos(dir) * (this.distance.get() + length.get()),
                    mc.player.getY() + this.height.get(),
                    mc.player.getZ() + Math.sin(dir) * (this.distance.get() + length.get())
            ));
        }

        event.renderer.line(
                mc.player.getX() + Math.cos(dir) * this.distance.get(),
                mc.player.getY() + this.height.get(),
                mc.player.getZ() + Math.sin(dir) * this.distance.get(),
                mc.player.getX() + Math.cos(dir) * (this.distance.get() + length.get()),
                mc.player.getY() + this.height.get(),
                mc.player.getZ() + Math.sin(dir) * (this.distance.get() + length.get()),
                colorStart,
                colorEnd
        );
    }

    // Methods

    private double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0F) {
            if (side > 0.0F) {
                yaw += ((forward > 0.0F) ? -45 : 45);
            } else if (side < 0.0F) {
                yaw += ((forward > 0.0F) ? 45 : -45);
            }

            side = 0.0F;

            if (forward > 0.0F) {
                forward = 1.0F;
            } else if (forward < 0.0F) {
                forward = -1.0F;
            }
        }

        double sin = Math.sin(Math.toRadians(yaw + 90.0F));
        double cos = Math.cos(Math.toRadians(yaw + 90.0F));
        double dx = forward * speed * cos + side * speed * sin;
        double dz = forward * speed * sin - side * speed * cos;

        return new double[] { dx, dz };
    }

    private Color getColorFromDistance(Vec3d pos) {
        Color color = new Color();

        double distance = mc.gameRenderer.getCamera().getPos().distanceTo(pos);
        double percent = distance / 60;

        if (percent < 0 || percent > 1) {
            color.set(0, 255, 0, 255);
            return color;
        }

        int red;
        int green;

        if (percent < 0.5) {
            red = 255;
            green = (int) (255 * percent / 0.5);
        } else {
            green = 255;
            red = 255 - (int) (255 * (percent - 0.5) / 0.5);
        }

        color.set(red, green, 0, 255);

        return color;
    }
}
