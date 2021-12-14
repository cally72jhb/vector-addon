package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class BedrockWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Your movement speed.")
            .defaultValue(1)
            .min(0.5)
            .sliderMin(0.5)
            .build()
    );

    private final Setting<Double> unlock = sgGeneral.add(new DoubleSetting.Builder()
            .name("unlock-speed")
            .description("At what speed to disable pulling in a direction.")
            .defaultValue(1)
            .min(0.1)
            .max(7.5)
            .sliderMin(0.1)
            .sliderMax(7.5)
            .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
            .name("sneak")
            .description("Whether or not to center when your sneaking.")
            .defaultValue(true)
            .build()
    );

    public BedrockWalk() {
        super(VectorAddon.CATEGORY, "bedrock-walk", "Makes navigating over bedrock easier.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!sneak.get() && mc.player.isSneaking()) return;
        double[] dir = VectorUtils.directionSpeed(1f);

        double x = mc.player.getBlockX();
        double y = mc.player.getBlockZ();

        double deltaX = Utils.clamp(x + 0.5 - mc.player.getX(), -0.05, 0.05);
        double deltaZ = Utils.clamp(y + 0.5 - mc.player.getZ(), -0.05, 0.05);

        Vec3d vel = mc.player.getVelocity();

        ((IVec3d) mc.player.getVelocity()).set((dir[0] > (unlock.get() / 10) || dir[0] < -(unlock.get() / 10)) ? vel.x : (deltaX / speed.get()), mc.player.getVelocity().y, (dir[1] > (unlock.get() / 10) || dir[1] < -(unlock.get() / 10)) ? vel.z : (deltaZ / speed.get()));
    }
}
