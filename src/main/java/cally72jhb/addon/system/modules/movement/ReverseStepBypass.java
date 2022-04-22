package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ReverseStepBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRubberband = settings.createGroup("Anti Rubberband");

    private final Setting<ReverseStepMode> reverseStepMode = sgGeneral.add(new EnumSetting.Builder<ReverseStepMode>()
        .name("reverse-step-mode")
        .description("How to bypass stepping downwards.")
        .defaultValue(ReverseStepMode.NCP)
        .build()
    );

    private final Setting<ActiveWhen> activeWhen = sgGeneral.add(new EnumSetting.Builder<ActiveWhen>()
        .name("active-when")
        .description("This module is active when you meet these requirements.")
        .defaultValue(ActiveWhen.Always)
        .build()
    );

    private final Setting<List<Module>> interferingModules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("interfering-modules")
        .description("The modules that interfere with this module.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast to fall in blocks per second.")
        .defaultValue(0.5)
        .min(0)
        .visible(() -> reverseStepMode.get() == ReverseStepMode.Normal)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Whether or not to step down even if you are in the air.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only steps down in holes.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
        .name("doubles")
        .description("Allows you to step down in double holes.")
        .defaultValue(true)
        .visible(onlyInHole::get)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("How fast to speed up the game when stepping downwards.")
        .defaultValue(2.5)
        .min(0.1)
        .sliderMin(2)
        .sliderMax(3)
        .visible(() -> reverseStepMode.get() == ReverseStepMode.Timer)
        .build()
    );

    private final Setting<Double> steps = sgGeneral.add(new DoubleSetting.Builder()
        .name("steps")
        .description("The step while checking for collision.")
        .defaultValue(0.01)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.015)
        .build()
    );

    private final Setting<Double> minHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-height")
        .description("The minimum distance when stepping downwards.")
        .defaultValue(0.75)
        .min(0.5)
        .max(1)
        .sliderMin(0.5)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> maxHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-height")
        .description("The maximum distance when stepping downwards.")
        .defaultValue(3)
        .min(0.6)
        .max(5.65)
        .sliderMin(0.6)
        .sliderMax(3.5)
        .build()
    );

    private final Setting<Double> triggerDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("trigger-distance")
        .description("After what fall distance to teleoprt you down.")
        .defaultValue(0.15)
        .min(0)
        .max(3)
        .sliderMin(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Boolean> spoofOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-on-ground")
        .description("Spoofs you server-side on ground.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> stopMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-movement")
        .description("Stops your horizontal movement when stepping down.")
        .defaultValue(true)
        .visible(() -> reverseStepMode.get() != ReverseStepMode.Normal)
        .build()
    );

    private final Setting<Boolean> extraPacket = sgGeneral.add(new BoolSetting.Builder()
        .name("extra-packet")
        .description("Sends an extra packet after steping down up a block to stabelize your movement.")
        .defaultValue(true)
        .visible(() -> reverseStepMode.get() != ReverseStepMode.Normal)
        .build()
    );


    // Anti Rubberband


    private final Setting<RubberbandAction> rubberbandAction = sgRubberband.add(new EnumSetting.Builder<RubberbandAction>()
        .name("rubberband-action")
        .description("What to do when detecting rubberband.")
        .defaultValue(RubberbandAction.Ignore)
        .build()
    );

    private final Setting<List<Module>> rubberbandModules = sgRubberband.add(new ModuleListSetting.Builder()
        .name("rubberband-modules")
        .description("The modules that are ignored when rubberbanding.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<Boolean> disableModules = sgRubberband.add(new BoolSetting.Builder()
        .name("disable-modules")
        .description("Disables selected modules when stepping.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Module>> modulesToDisable = sgRubberband.add(new ModuleListSetting.Builder()
        .name("modules-to-disable")
        .description("The modules that are disabled when stepping.")
        .defaultValue(List.of())
        .visible(disableModules::get)
        .build()
    );

    private final Setting<Integer> reenableCoolDowm = sgRubberband.add(new IntSetting.Builder()
        .name("reenable-cool-down")
        .description("The cool down in ticks to wait before enabling the modules again.")
        .defaultValue(10)
        .min(0)
        .sliderMin(0)
        .sliderMax(20)
        .noSlider()
        .visible(disableModules::get)
        .build()
    );

    private final Setting<Boolean> checkStepDuration = sgRubberband.add(new BoolSetting.Builder()
        .name("check-step-duration")
        .description("Waits when reaching a set amount of steps in a set duration.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> duration = sgRubberband.add(new IntSetting.Builder()
        .name("duration")
        .description("The duration in ticks in which steps are counted and evaluated.")
        .defaultValue(50)
        .min(10)
        .sliderMin(40)
        .sliderMax(75)
        .noSlider()
        .visible(checkStepDuration::get)
        .build()
    );

    private final Setting<Integer> maxSteps = sgRubberband.add(new IntSetting.Builder()
        .name("max-steps")
        .description("The maximum amount of steps in a set duration.")
        .defaultValue(3)
        .min(1)
        .sliderMin(2)
        .sliderMax(4)
        .noSlider()
        .visible(checkStepDuration::get)
        .build()
    );

    private final Setting<Integer> failWaitDelay = sgRubberband.add(new IntSetting.Builder()
        .name("fail-wait-delay")
        .description("How long to wait in ticks before reverse stepping again.")
        .defaultValue(50)
        .min(5)
        .noSlider()
        .visible(() -> rubberbandAction.get() != RubberbandAction.Ignore || checkStepDuration.get())
        .build()
    );

    private final Setting<Boolean> infoOnFail = sgRubberband.add(new BoolSetting.Builder()
        .name("info-on-fail")
        .description("Sends you a client-side message when you fail to reverse step.")
        .defaultValue(true)
        .visible(() -> rubberbandAction.get() != RubberbandAction.Ignore || checkStepDuration.get())
        .build()
    );

    private int waitTicks;
    private int durationTicks;
    private int stepsInDuration;

    private int reenableTicks;

    private List<Module> prevModules;

    public ReverseStepBypass() {
        super(Categories.Movement, "reverse-step-bypass", "Allows to instantly step down blocks.");
    }

    @Override
    public void onActivate() {
        waitTicks = failWaitDelay.get();
        durationTicks = 0;
        stepsInDuration = 0;

        reenableTicks = 0;

        prevModules = new ArrayList<>();
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);

        enableModules();
    }

    // Main Tick

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        waitTicks++;
        durationTicks++;

        if (checkStepDuration.get() && (durationTicks >= duration.get() || stepsInDuration > maxSteps.get())) {
            if (stepsInDuration > maxSteps.get()) {
                waitTicks = 0;

                if (infoOnFail.get()) info("Stepped more then " + maxSteps.get() + (maxSteps.get() == 1 ? " time" : " times") + " downwards. Waiting " + failWaitDelay.get() + " ticks...");
            }

            durationTicks = 0;
            stepsInDuration = 0;
        }

        if (waitTicks >= failWaitDelay.get() && canReverseStep()
            && (activeWhen.get() != ActiveWhen.Sneaking || activeWhen.get() == ActiveWhen.Sneaking && mc.player.isSneaking())
            && (activeWhen.get() != ActiveWhen.NotSneaking || activeWhen.get() == ActiveWhen.NotSneaking && !mc.player.isSneaking())
            && (!onlyOnGround.get() || onlyOnGround.get() && mc.player.isOnGround())
            && isPlayerMoving()) {

            if (!doesCollide(0, -0.05, 0) && mc.player.fallDistance <= 0.1 && !mc.player.getAbilities().flying
                && mc.player.fallDistance <= triggerDistance.get()
                && mc.player.getVelocity().getY() <= 0 && mc.player.getVelocity().getY() >= -0.5
                && !mc.player.isFallFlying() && !mc.player.noClip && !mc.options.jumpKey.isPressed()
                && !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isHoldingOntoLadder()
                && !mc.world.containsFluid(mc.player.getBoundingBox())
                && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0, -(maxHeight.get() + 0.01), 0))) {

                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();

                double height = -1;
                double blockY = 0;

                boolean valid = !onlyInHole.get();

                for (double dy = y - 0.05; dy >= y - maxHeight.get() - steps.get(); dy -= steps.get()) {
                    if (doesCollide(0, dy - y, 0)) {
                        height = Math.abs(((double) Math.round((dy + (doesCollide(0, dy + 0.05 - y, 0) ? 0.05 : steps.get()) - y) * 10000) / 10000));
                        blockY = y - height;
                        break;
                    }
                }

                if (onlyInHole.get()) {
                    BlockPos pos = new BlockPos(mc.player.getBlockX(), blockY, mc.player.getBlockZ());

                    if (isValidHole(pos, true) && isValidHole(pos.up(), false)) {
                        int air = 0;
                        int surr = 0;

                        BlockPos second = null;

                        for (CardinalDirection cDir : CardinalDirection.values()) {
                            Direction direction = cDir.toDirection();

                            if (doubles.get() && isValidHole(pos.offset(direction), true) && isValidHole(pos.offset(direction).up(), false)) {
                                int surrounded = 0;

                                for (CardinalDirection dir : CardinalDirection.values()) {
                                    if (mc.world.getBlockState(pos.offset(direction).offset(dir.toDirection())).getBlock().getBlastResistance() >= 600.0F) {
                                        surrounded++;
                                    }
                                }

                                if (surrounded == 3) {
                                    second = pos.offset(direction);
                                    air++;
                                } else {
                                    air = 0;
                                }
                            } else if (mc.world.getBlockState(pos.offset(direction)).getBlock().getBlastResistance() >= 600.0F) {
                                surr++;
                            }
                        }

                        valid = doubles.get() ? air == 1 && surr >= 3 && (isValidHole(pos.up(2), false) || second != null && isValidHole(second.up(2), false)) || air == 0 && surr >= 4 && isValidHole(pos.up(2), false) : surr >= 4 && isValidHole(pos.up(2), false);
                    }
                }

                if (valid) {
                    if (disableModules.get()) disableModules();

                    if (height >= minHeight.get() && height <= maxHeight.get() && !mc.world.containsFluid(mc.player.getBoundingBox().stretch(0, -height, 0))) {
                        if (reverseStepMode.get() == ReverseStepMode.Timer) {
                            if (stopMovement.get()) mc.player.setVelocity(0, mc.player.getVelocity().getY(), 0);

                            Modules.get().get(Timer.class).setOverride(timer.get());
                        } else if (reverseStepMode.get() == ReverseStepMode.Normal) {
                            Vec3d velocity = mc.player.getVelocity();

                            mc.player.setVelocity(velocity.getX(), -fallSpeed.get(), velocity.getZ());
                        } else if (reverseStepMode.get() == ReverseStepMode.NCP || reverseStepMode.get() == ReverseStepMode.Plus) {
                            if (stopMovement.get()) mc.player.setVelocity(0, 0, 0);

                            double[] positions = new double[]{
                                0.07840000152588011,
                                0.23363200604247990,
                                0.46415937495544000,
                                0.76847620241297990,

                                1.14510670065164000,
                                1.59260459763506000,

                                2.10955254674003000,
                                2.69456154824879000,

                                3.34627038241139996,

                                4.06334505384699995,
                                4.84447824705687990,
                            };

                            if (reverseStepMode.get() == ReverseStepMode.NCP) {
                                if (!doesCollide(0, y - positions[0], 0) && y - positions[0] > blockY) sendPacket(x, y - positions[0], z);
                                if (!doesCollide(0, y - positions[1], 0) && y - positions[1] > blockY) sendPacket(x, y - positions[1], z);
                                if (!doesCollide(0, y - positions[2], 0) && y - positions[2] > blockY) sendPacket(x, y - positions[2], z);
                                if (!doesCollide(0, y - positions[3], 0) && y - positions[3] > blockY) sendPacket(x, y - positions[3], z);

                                if (!doesCollide(0, y - positions[4], 0) && y - positions[4] > blockY) sendPacket(x, y - positions[4], z);
                                if (!doesCollide(0, y - positions[5], 0) && y - positions[5] > blockY) sendPacket(x, y - positions[5], z);

                                if (!doesCollide(0, y - positions[6], 0) && y - positions[6] > blockY) sendPacket(x, y - positions[6], z);
                                if (!doesCollide(0, y - positions[7], 0) && y - positions[7] > blockY) sendPacket(x, y - positions[7], z);

                                if (!doesCollide(0, y - positions[8], 0) && y - positions[8] > blockY) sendPacket(x, y - positions[8], z);

                                if (!doesCollide(0, y - positions[9], 0) && y - positions[9] > blockY) sendPacket(x, y - positions[9], z);
                                if (!doesCollide(0, y - positions[10], 0) && y - positions[10] > blockY) sendPacket(x, y - positions[10], z);
                            } else if (reverseStepMode.get() == ReverseStepMode.Timer) {
                                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                            }

                            mc.player.updatePosition(x, blockY, z);
                            if (extraPacket.get()) sendPacket(x, blockY, z);

                            stepsInDuration++;
                        }
                    }
                }
            }
        }

        if (mc.player.isOnGround() || doesCollide(0, -0.1, 0)) {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);

            if (disableModules.get() && !prevModules.isEmpty()) {
                if (reenableTicks >= reenableCoolDowm.get() || reenableCoolDowm.get() == 0) {
                    reenableTicks = 0;
                    enableModules();
                }

                reenableTicks++;
            }
        }
    }

    // Anti Rubberband

    @EventHandler(priority = EventPriority.HIGHEST - 25)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && waitTicks >= failWaitDelay.get()) {
            boolean valid = rubberbandModules.get().isEmpty();

            if (!rubberbandModules.get().isEmpty()) {
                for (Module module : rubberbandModules.get()) {
                    if (module.isActive() && Modules.get().getList().contains(module)) valid = false;
                }
            }

            if (valid) {
                if (rubberbandAction.get() == RubberbandAction.Disable) {
                    if (infoOnFail.get()) info("Rubberband detected! Disabling...");
                    toggle();
                } else if (rubberbandAction.get() == RubberbandAction.Wait) {
                    if (infoOnFail.get()) info("Rubberband detected! Waiting...");
                    waitTicks = 0;
                }
            }
        }
    }

    // Utils

    private void sendPacket(double x, double y, double z) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, spoofOnGround.get() || mc.player.isOnGround()));
    }

    private boolean isPlayerMoving() {
        return mc.player.getVelocity().getX() != 0 || mc.player.getVelocity().getZ() != 0;
    }

    private boolean doesCollide(double x, double y, double z) {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(x, y, z)).iterator().hasNext();
    }

    private boolean isValidHole(BlockPos pos, boolean checkDown) {
        return mc.world.getBlockState(pos).getBlock() != Blocks.COBWEB
            && mc.world.getBlockState(pos).getBlock() != Blocks.POWDER_SNOW
            && (!checkDown || (mc.world.getBlockState(pos.down()).getBlock().getBlastResistance() >= 600.0F
            && mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()) != null
            && !mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()))
            && (mc.world.getBlockState(pos).getCollisionShape(mc.world, pos) == null
            || mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty());
    }

    private boolean canReverseStep() {
        if (interferingModules.get().isEmpty()) return true;

        for (Module module : interferingModules.get()) {
            if (module.isActive() && Modules.get().getList().contains(module)) return false;
        }

        return true;
    }

    private void disableModules() {
        if (!modulesToDisable.get().isEmpty()) {
            for (Module module : modulesToDisable.get()) {
                if (module != null && module.isActive()) {
                    prevModules.add(module);
                    module.toggle();
                }
            }
        }
    }

    private void enableModules() {
        if (prevModules != null && !prevModules.isEmpty()) {
            for (Module module : prevModules) {
                if (module != null && !module.isActive()) module.toggle();
            }

            prevModules.clear();
        }
    }

    public enum ReverseStepMode {
        Normal,
        Plus,
        Timer,
        NCP
    }

    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }

    public enum RubberbandAction {
        Ignore,
        Wait,
        Disable
    }
}
