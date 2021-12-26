package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class BedrockWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The movement mode.")
        .defaultValue(Mode.TP)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Your movement speed.")
            .defaultValue(1)
            .min(0.01)
            .sliderMin(0.75)
            .build()
    );

    private final Setting<Double> unlock = sgGeneral.add(new DoubleSetting.Builder()
            .name("unlock-speed")
            .description("At what speed to disable pulling in a direction.")
            .defaultValue(1)
            .min(0.1)
            .max(5)
            .sliderMin(0.1)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
            .name("sneak")
            .description("Whether or not to center when your sneaking.")
            .defaultValue(false)
            .build()
    );

    public BedrockWalk() {
        super(VectorAddon.MOVEMENT, "bedrock-walk", "Makes navigating over bedrock easier.");
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (shouldCenter()) return;

        double[] dir = VectorUtils.directionSpeed(1f);

        double deltaX = Utils.clamp(mc.player.getBlockX() + 0.5 - mc.player.getX(), -0.05, 0.05);
        double deltaZ = Utils.clamp(mc.player.getBlockZ() + 0.5 - mc.player.getZ(), -0.05, 0.05);

        Vec3d vel = mc.player.getVelocity();

        double x = (dir[0] > (unlock.get() / 10) || dir[0] < -(unlock.get() / 10)) ? vel.x : (deltaX / speed.get());
        double z = (dir[1] > (unlock.get() / 10) || dir[1] < -(unlock.get() / 10)) ? vel.z : (deltaZ / speed.get());

        switch (mode.get()) {
            case NORMAL:
                mc.player.setVelocity(x, mc.player.getVelocity().y, z);
            case TP:
                if (!mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(x, 0, z)).iterator().hasNext()) {
                    mc.player.setPosition(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                    mc.player.setVelocity(vel.x / (speed.get() * 2), vel.y, vel.z / (speed.get() * 2));
                }
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.SET) {
            if (shouldCenter()) return;
            double[] dir = VectorUtils.directionSpeed(1f);

            double deltaX = Utils.clamp(mc.player.getBlockX() + 0.5 - mc.player.getX(), -0.05, 0.05);
            double deltaZ = Utils.clamp(mc.player.getBlockZ() + 0.5 - mc.player.getZ(), -0.05, 0.05);

            Vec3d vel = mc.player.getVelocity();

            double x = (dir[0] > (unlock.get() / 10) || dir[0] < -(unlock.get() / 10)) ? vel.x : (deltaX / speed.get());
            double z = (dir[1] > (unlock.get() / 10) || dir[1] < -(unlock.get() / 10)) ? vel.z : (deltaZ / speed.get());

            ((IVec3d) event.movement).setXZ(x, z);
        }
    }

    private boolean shouldCenter() {
        return ((!sneak.get() && mc.player.isSneaking()) || mc.player.isTouchingWater() || mc.player.isInLava() || (mc.player.prevY < mc.player.getY()));
    }

    public enum Mode {
        NORMAL,
        SET,
        TP
    }
}
