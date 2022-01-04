package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class SpeedBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("At which speed to travel.")
        .defaultValue(5)
        .min(0)
        .build()
    );

    private final Setting<Double> check = sgGeneral.add(new DoubleSetting.Builder()
        .name("forward-check")
        .description("How many blocks to look forwards to scan if you can bypass.")
        .defaultValue(2)
        .min(0)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only bypasses when you are on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onGround = sgGeneral.add(new BoolSetting.Builder()
        .name("on-ground")
        .description("Sets you server-side on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Allows to travel at higher speed on some servers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Stricts your movement.")
        .defaultValue(false)
        .visible(bypass::get)
        .build()
    );

    public SpeedBypass() {
        super(VectorAddon.MOVEMENT, "speed-bypass", "Modifies your movement speed when moving on the ground.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if ((!onlyOnGround.get() || onGround()) && isPlayerMoving()) {
            if (mc.options.keyJump.isPressed() && onlyOnGround.get()) return;

            Vec3d vel = mc.player.getVelocity();

            for (double i = 0.0625; i < speed.get(); i += 0.262) {
                double[] dir = VectorUtils.directionSpeed((float) i);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1], mc.player.isOnGround()));
            }

            if (bypass.get()) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + vel.x, mc.player.getY() <= 10 ? (strict.get() ? 10 : 255) : (strict.get() ? 0.5 : 1), mc.player.getZ() + vel.z, mc.player.isOnGround()));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (onGround.get()) ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
        }
    }

    private boolean onGround() {
        if (check.get() != 0) {
            for (double i = 0.0625; i < check.get() / 3; i += 0.262) {
                double[] dir = VectorUtils.directionSpeed((float) i);
                if (!mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(dir[0], -0.05, dir[1])).iterator().hasNext()
                    || mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(dir[0], 0, dir[1])).iterator().hasNext()) return false;
            }
        }

        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.05, 0)).iterator().hasNext();
    }

    private boolean isPlayerMoving() {
        if (mc.options.keyForward.isPressed()) return true;
        if (mc.options.keyBack.isPressed()) return true;
        if (mc.options.keyLeft.isPressed()) return true;
        if (mc.options.keyRight.isPressed()) return true;

        return false;
    }
}
