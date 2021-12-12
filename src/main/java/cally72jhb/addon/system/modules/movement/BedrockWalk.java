package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
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
            .defaultValue(2)
            .min(0.1)
            .build()
    );

    public BedrockWalk() {
        super(VectorAddon.CATEGORY, "bedrock-walk", "Makes navigating over bedrock easier.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        double[] dir = VectorUtils.directionSpeed(1f);

        double x = mc.player.getBlockX();
        double y = mc.player.getBlockZ();

        double deltaX = Utils.clamp(x + 0.5 - mc.player.getX(), -0.05, 0.05);
        double deltaZ = Utils.clamp(y + 0.5 - mc.player.getZ(), -0.05, 0.05);

        Vec3d vel = mc.player.getVelocity();

        ((IVec3d) mc.player.getVelocity()).set((dir[0] > 0.1 || dir[0] < -0.1) ? vel.x : (deltaX / speed.get()), mc.player.getVelocity().y, (dir[1] > 0.1 || dir[1] < -0.1) ? vel.z : (deltaZ / speed.get()));
    }
}
