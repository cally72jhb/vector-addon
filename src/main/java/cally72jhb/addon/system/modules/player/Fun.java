package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;

public class Fun extends Module {
    private final SettingGroup sgOpposite = settings.createGroup("Opposite");
    private final SettingGroup sgPitch = settings.createGroup("Pitch");

    private final Setting<Boolean> opposite = sgOpposite.add(new BoolSetting.Builder()
        .name("opposite")
        .description("Makes your head the opposite rotation as your body.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> setCustomPitch = sgOpposite.add(new BoolSetting.Builder()
        .name("custom-pitch")
        .description("Whether or not to set a custom pitch.")
        .defaultValue(false)
        .visible(opposite::get)
        .build()
    );

    private final Setting<Integer> delay = sgOpposite.add(new IntSetting.Builder()
        .name("update-delay")
        .description("The delay for updating your rotation.")
        .defaultValue(1)
        .min(0)
        .max(50)
        .sliderMin(0)
        .sliderMax(50)
        .visible(opposite::get)
        .build()
    );

    private final Setting<Double> customPitch = sgOpposite.add(new DoubleSetting.Builder()
        .name("custom-pitch")
        .description("Your custom pitch.")
        .defaultValue(-135)
        .sliderMin(-180)
        .sliderMax(180)
        .visible(() -> opposite.get() && setCustomPitch.get())
        .build()
    );

    private final Setting<Boolean> usePitch = sgPitch.add(new BoolSetting.Builder()
        .name("use-pitch")
        .description("Spin your head around.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> pitch = sgPitch.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("How fast to spin you head around.")
        .defaultValue(5)
        .visible(usePitch::get)
        .build()
    );

    private float prevYaw;
    private float prevPitch;
    private int timer;

    public Fun() {
        super(VectorAddon.CATEGORY, "fun", "Does various fun stuff.");
    }

    @Override
    public void onActivate() {
        prevYaw = mc.player.getYaw();
        prevPitch = mc.player.getPitch();
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Utils.canUpdate()) {
            if (opposite.get()) {
                if (opposite.get()) prevYaw += 180;
                else prevYaw = mc.player.getYaw();

                Rotations.rotate(prevYaw, setCustomPitch.get() ? customPitch.get() : mc.player.getPitch(), -20, null);
            } else if (usePitch.get()) {
                prevPitch += pitch.get();
                Rotations.rotate(mc.player.getYaw(), prevPitch, -20, null);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        timer++;
        if (timer > delay.get() + 3) {
            prevYaw = mc.player.getYaw();
            timer = 0;
        }
    }
}
