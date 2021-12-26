package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.utils.misc.Vec2;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;

public class Strafe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> moveSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("How much to multiply your movement speed.")
            .defaultValue(0.3)
            .build()
    );

    private final Setting<Boolean> speedLimit = sgGeneral.add(new BoolSetting.Builder()
            .name("speed-limit")
            .description("The speed limit.")
            .defaultValue(true)
            .build()
    );

    private long timer = 0L;

    public Strafe() {
        super(VectorAddon.MOVEMENT, "strafe", "Increases your movement speed in every direction.");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        double speed = Math.max(moveSpeed.get(), getDefaultSpeed());

        if (speedLimit.get()) {
            if (System.currentTimeMillis() - timer > 2500L) {
                timer = System.currentTimeMillis();
            }

            speed = Math.min(speed, System.currentTimeMillis() - timer > 1250L ? 0.44D : 0.43D);
        }

        Vec2 change = transformStrafe(speed);

        double velX = change.x;
        double velZ = change.y;

        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            velX = anchor.deltaX;
            velZ = anchor.deltaZ;
        }

        ((IVec3d) event.movement).setXZ(velX, velZ);
    }

    private Vec2 transformStrafe(double speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getTickDelta();

        double velX, velZ;

        if (forward == 0.0f && side == 0.0f) return new Vec2(0, 0);

        else if (forward != 0.0f) {
            if (side >= 1.0f) {
                yaw += (float) (forward > 0.0f ? -45 : 45);
                side = 0.0f;
            } else if (side <= -1.0f) {
                yaw += (float) (forward > 0.0f ? 45 : -45);
                side = 0.0f;
            }

            if (forward > 0.0f)
                forward = 1.0f;

            else if (forward < 0.0f)
                forward = -1.0f;
        }

        double mx = Math.cos(Math.toRadians(yaw + 90.0f));
        double mz = Math.sin(Math.toRadians(yaw + 90.0f));

        velX = (double) forward * speed * mx + (double) side * speed * mz;
        velZ = (double) forward * speed * mz - (double) side * speed * mx;

        return new Vec2(velX, velZ);
    }

    private double getDefaultSpeed() {
        double defaultSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return defaultSpeed;
    }
}
