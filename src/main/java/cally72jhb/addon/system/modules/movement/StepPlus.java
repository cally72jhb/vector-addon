package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class StepPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutomatic = settings.createGroup("Automatic");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How to bypass.")
        .defaultValue(Mode.NCP)
        .build()
    );

    private final Setting<ActiveWhen> activeWhen = sgGeneral.add(new EnumSetting.Builder<ActiveWhen>()
        .name("active-when")
        .description("Step is active when you meet these requirements.")
        .defaultValue(ActiveWhen.Always)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Whether or not to step even if you are in the air.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useTimer = sgGeneral.add(new BoolSetting.Builder()
        .name("use-timer")
        .description("Whether or not to use a timer.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("The timer speed.")
        .defaultValue(1.125)
        .min(0.1)
        .sliderMin(1.25)
        .sliderMax(1.75)
        .visible(() -> mode.get() == Mode.NCP && useTimer.get())
        .build()
    );

    private final Setting<Boolean> extraPacket = sgGeneral.add(new BoolSetting.Builder()
        .name("extra-packet")
        .description("Sends an extra packet after steping up a block.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Boolean> spoofOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-on-ground")
        .description("Spoofs you server-side on ground.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Sends more accurate packets to the server.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Informs you about the step height when stepping up a block.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Allows to step a little higher then one block.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Double> steps = sgGeneral.add(new DoubleSetting.Builder()
        .name("steps")
        .description("The step while checking for collision.")
        .defaultValue(0.01)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.015)
        .visible(() -> mode.get() == Mode.NCP && bypass.get())
        .build()
    );


    // Automatic


    private final Setting<Integer> packets = sgAutomatic.add(new IntSetting.Builder()
        .name("packets")
        .description("How many packets are send.")
        .defaultValue(2)
        .min(1)
        .max(7)
        .sliderMin(1)
        .sliderMax(7)
        .noSlider()
        .visible(() -> mode.get() == Mode.NCP)
        .build()
    );

    private final Setting<Boolean> automatic = sgAutomatic.add(new BoolSetting.Builder()
        .name("automatic")
        .description("Automatically detects various block heights and trys to bypass by sending more or less packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> packet1Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-1-height")
        .description("After what step height the first packet is send.")
        .defaultValue(0.753)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private final Setting<Double> packet2Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-2-height")
        .description("After what step height the second packet is send.")
        .defaultValue(0.753)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private final Setting<Double> packet3Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-3-height")
        .description("After what step height the third packet is send.")
        .defaultValue(1.05)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private final Setting<Double> packet4Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-4-height")
        .description("After what step height the fourth packet is send.")
        .defaultValue(1.125)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private final Setting<Double> packet5Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-5-height")
        .description("After what step height the fifth packet is send.")
        .defaultValue(1.125)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private final Setting<Double> packet6Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-6-height")
        .description("After what step height the sixth packet is send.")
        .defaultValue(1.125)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private final Setting<Double> packet7Height = sgAutomatic.add(new DoubleSetting.Builder()
        .name("packet-7-height")
        .description("After what step height the seventh packet is send.")
        .defaultValue(1.25)
        .min(0.55)
        .sliderMin(0.55)
        .sliderMax(1.249)
        .visible(() -> mode.get() == Mode.NCP && automatic.get())
        .build()
    );

    private float prevStepHeight;

    public StepPlus() {
        super(Categories.Movement, "step-plus", "Allows you to walk up full blocks.");
    }

    @Override
    public void onActivate() {
        prevStepHeight = mc.player.stepHeight;
    }

    @Override
    public void onDeactivate() {
        mc.player.stepHeight = prevStepHeight;

        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if ((activeWhen.get() == ActiveWhen.Sneaking && !mc.player.isSneaking())
            || (activeWhen.get() == ActiveWhen.NotSneaking && mc.player.isSneaking())
            || (!mc.player.isOnGround() && onlyOnGround.get())
            || !isPlayerMoving()) {
            return;
        }

        if (mode.get() == Mode.NORMAL) {
            mc.player.stepHeight = 1.001F;
            return;
        }

        if (!useTimer.get()) Modules.get().get(Timer.class).setOverride(Timer.OFF);

        double[] dir = directionSpeed(0.025F);

        if (doesCollide(dir[0], 0.6, dir[1]) && !doesCollide(dir[0], 1.2, dir[1])) {
            if (useTimer.get()) Modules.get().get(Timer.class).setOverride(timer.get());

            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            double height = -1;

            if (bypass.get()) {
                for (double dy = y + 0.6; dy <= y + 1.2; dy += steps.get()) {
                    if (!doesCollide(dir[0], dy - y, dir[1])) {
                        height = ((double) Math.round((dy - y) * 10000) / 10000);
                        break;
                    }
                }
            } else {
                height = 1;
            }

            double y1 = accurate.get() ? 0.41999998688698 : 0.420;
            double y2 = accurate.get() ? 0.75319998052120 : 0.753;
            double y3 = accurate.get() ? 1.00133597911214 : 1.001;
            double y4 = accurate.get() ? 1.16610926093821 : 1.166;
            double y5 = accurate.get() ? 1.24918707874468 : 1.249;
            double y6 = accurate.get() ? 1.17675927506424 : 1.177;
            double y7 = accurate.get() ? 1.02442408821369 : 1.024;

            if (packets.get() > 0 || height >= packet1Height.get() && automatic.get()) sendPacket(x, y + y1, z);
            if (packets.get() > 1 || height >= packet2Height.get() && automatic.get()) sendPacket(x, y + y2, z);
            if (packets.get() > 2 || height >= packet3Height.get() && automatic.get()) sendPacket(x, y + y3, z);
            if (packets.get() > 3 || height >= packet4Height.get() && automatic.get()) sendPacket(x, y + y4, z);
            if (packets.get() > 4 || height >= packet5Height.get() && automatic.get()) sendPacket(x, y + y5, z);
            if (packets.get() > 5 || height >= packet6Height.get() && automatic.get()) sendPacket(x, y + y6, z);
            if (packets.get() > 6 || height >= packet7Height.get() && automatic.get()) sendPacket(x, y + y7, z);

            if (debug.get()) info("Height: " + height);

            mc.player.setPosition(x, y + height, z);
            if (extraPacket.get()) sendPacket(x, y + height, z);
        } else if (useTimer.get()) {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }

    // Utils

    private void sendPacket(double x, double y, double z) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, spoofOnGround.get() || mc.player.isOnGround()));
    }

    private boolean isPlayerMoving() {
        return mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F;
    }

    private boolean doesCollide(double x, double y, double z) {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(x, y, z)).iterator().hasNext();
    }

    private double[] directionSpeed(float speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
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

    public enum Mode {
        NORMAL,
        NCP,
    }

    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }
}
