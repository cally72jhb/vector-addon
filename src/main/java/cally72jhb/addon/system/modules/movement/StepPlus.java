package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.shape.VoxelShape;

public class StepPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("step-height")
            .description("Your step height.")
            .defaultValue(1)
            .min(1)
            .max(2.5)
            .sliderMin(1)
            .sliderMax(2.5)
            .build()
    );

    private final Setting<Boolean> timer = sgGeneral.add(new BoolSetting.Builder()
            .name("timer")
            .description("Whether or not to use a timer.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Whether or not to step even if you are in the air.")
            .defaultValue(true)
            .build()
    );

    private float prevStepHeight;

    public StepPlus() {
        super(VectorAddon.MOVEMENT, "step-plus", "Allows you to walk up full blocks.");
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
        if ((activeWhen.get() == ActiveWhen.Sneaking && !mc.player.isSneaking()) || (activeWhen.get() == ActiveWhen.NotSneaking && mc.player.isSneaking()) || (!mc.player.isOnGround() && onlyOnGround.get())) return;
        if (mode.get() == Mode.NCPPLUS) mc.player.stepHeight = height.get().floatValue();
        if (mode.get() == Mode.NORMAL) {
            mc.player.stepHeight = height.get().floatValue();
            return;
        }

        if (!timer.get()) Modules.get().get(Timer.class).setOverride(Timer.OFF);

        double[] dir = VectorUtils.directionSpeed(0.1f);

        // One Block

        if (shouldStep(dir, 1.0, 0.6) && height.get() >= 1.0){
            for (double y : new double[] { 0.42, 0.753 }) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), mc.player.isOnGround()));
            }

            if (timer.get()) Modules.get().get(Timer.class).setOverride(1.6);
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }

        // Tall One Block
        if (shouldStep(dir, 1.6, 1.4) && height.get() >= 1.5){
            for (double y : new double[] { 0.42, 0.75, 1.0, 1.16, 1.23, 1.2 }) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), mc.player.isOnGround()));
            }

            if (timer.get()) Modules.get().get(Timer.class).setOverride(1.35);
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.5, mc.player.getZ());
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }

        // Two Block
        if (shouldStep(dir, 2.1, 1.9) && height.get() >= 2.0){
            for (double y : new double[] { 0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43 }) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), mc.player.isOnGround()));
            }

            if (timer.get()) Modules.get().get(Timer.class).setOverride(1.25);
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 2.0, mc.player.getZ());
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }

        // Tall Two Block
        if (shouldStep(dir, 2.6, 2.4) && height.get() >= 2.5){
            for (double y : new double[] { 0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907 }) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), mc.player.isOnGround()));
            }

            if (timer.get()) Modules.get().get(Timer.class).setOverride(1.15);
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 2.5, mc.player.getZ());
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }

    // Utils

    private boolean shouldStep(double[] dir, double y1, double y2) {
        return !getCollisions(dir[0], y1, dir[1]).iterator().hasNext() && getCollisions(dir[0], y2, dir[1]).iterator().hasNext();
    }

    private Iterable<VoxelShape> getCollisions(double x, double y, double z) {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(x, y, z));
    }

    public enum Mode {
        NORMAL,
        NCP,
        NCPPLUS
    }

    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }
}
