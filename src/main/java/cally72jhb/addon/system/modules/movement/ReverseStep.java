package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;

public class ReverseStep extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Whether or not to render the font higher quality.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> timer = sgGeneral.add(new BoolSetting.Builder()
        .name("timer")
        .description("Whether or not to speed up the game while falling.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("The maximum fall distance this setting will activate at.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    private boolean valid = false;

    public ReverseStep() {
        super(VectorAddon.MOVEMENT, "reverse-step-plus", "Fall faster down.");
    }

    @Override
    public void onActivate() {
        valid = false;
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        if (onGround()) Modules.get().get(Timer.class).setOverride(Timer.OFF);
        if (mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, (float) -(fallDistance.get() + 0.01), 0.0))) return;

        if (timer.get()) {
            if (!onGround()) {
                if (mc.player.getVelocity().y < 0 && valid) {
                    Modules.get().get(Timer.class).setOverride(strict.get() ? 2.5 : 5);

                    return;
                } else {
                    valid = false;
                }
            } else if (onGround() && !(mc.player.isTouchingWater() || mc.player.isInLava())) {
                ((IVec3d) mc.player.getVelocity()).setY(-0.08);
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                valid = true;
            }
        } else if (mc.player.isOnGround() && !onGround() && !(mc.player.isTouchingWater() || mc.player.isInLava())) {
            ((IVec3d) mc.player.getVelocity()).setY(strict.get() ? -1 : -5);
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }

    private boolean onGround() {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.05, 0)).iterator().hasNext();
    }
}
