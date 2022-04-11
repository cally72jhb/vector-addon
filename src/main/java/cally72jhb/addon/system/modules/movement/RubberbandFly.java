package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class RubberbandFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");
    private final SettingGroup sgIdle = settings.createGroup("Idle");

    private final Setting<Double> velocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity")
        .description("Your client-side velocity.")
        .defaultValue(0.25)
        .min(0)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("At which speed to travel.")
        .defaultValue(1)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> startFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("start-factor")
        .description("At what point to start.")
        .defaultValue(0.625)
        .min(0)
        .sliderMin(0.001)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> airFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("air-factor")
        .description("The movement distance when in air.")
        .defaultValue(0.249)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.312)
        .build()
    );

    private final Setting<Double> waterFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("water-factor")
        .description("The movement distance when in water.")
        .defaultValue(0.160)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.212)
        .build()
    );

    private final Setting<Double> sneakFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("sneak-factor")
        .description("The movement distance when sneaking.")
        .defaultValue(0.212)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.3)
        .build()
    );


    // Bypass


    private final Setting<Boolean> spoofOnGround = sgBypass.add(new BoolSetting.Builder()
        .name("spoof-on-ground")
        .description("Sets you server-side on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cancelPackets = sgBypass.add(new BoolSetting.Builder()
        .name("cancel-packets")
        .description("Cancels non required movement packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sprint = sgBypass.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Automatically sprints to allow higher speeds.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bypass = sgBypass.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Allows to travel at higher speed on some servers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> strict = sgBypass.add(new BoolSetting.Builder()
        .name("strict")
        .description("Stricts your movement.")
        .defaultValue(false)
        .visible(bypass::get)
        .build()
    );

    private final Setting<Double> rubberbandHeight = sgBypass.add(new DoubleSetting.Builder()
        .name("rubberband-height")
        .description("The vertical rubberband height.")
        .defaultValue(50)
        .sliderMin(0)
        .sliderMax(100)
        .visible(() -> bypass.get() && !strict.get())
        .build()
    );

    private final Setting<PosMode> positionMode = sgBypass.add(new EnumSetting.Builder<PosMode>()
        .name("set-pos-mode")
        .description("How to update your position.")
        .defaultValue(PosMode.None)
        .build()
    );


    // Anti Kick


    private final Setting<DownMode> downwardsMode = sgAntiKick.add(new EnumSetting.Builder<DownMode>()
        .name("downwards-mode")
        .description("How to go downwards.")
        .defaultValue(DownMode.Pre)
        .build()
    );

    private final Setting<Integer> downwardsDelay = sgAntiKick.add(new IntSetting.Builder()
        .name("downwards-delay")
        .description("How long to wait before going down again.")
        .defaultValue(5)
        .min(0)
        .noSlider()
        .visible(() -> downwardsMode.get() != DownMode.None)
        .build()
    );

    private final Setting<Double> downwardsSpeed = sgAntiKick.add(new DoubleSetting.Builder()
        .name("downwards-speed")
        .description("How fast to go downwards.")
        .defaultValue(0.05)
        .min(0)
        .sliderMin(0)
        .sliderMax(1)
        .visible(() -> downwardsMode.get() != DownMode.None)
        .build()
    );

    private final Setting<Boolean> tpBack = sgAntiKick.add(new BoolSetting.Builder()
        .name("tp-back")
        .description("Teleports you back to your original position.")
        .defaultValue(true)
        .visible(() -> downwardsMode.get() != DownMode.None)
        .build()
    );


    // Idle


    private final Setting<IdleMode> idleMode = sgIdle.add(new EnumSetting.Builder<IdleMode>()
        .name("idle-mode")
        .description("What to do when not moving.")
        .defaultValue(IdleMode.Flight)
        .build()
    );

    private final Setting<Integer> idleDelay = sgIdle.add(new IntSetting.Builder()
        .name("idle-delay")
        .description("How long to wait before spoofing again.")
        .defaultValue(0)
        .min(0)
        .noSlider()
        .visible(() -> idleMode.get() != IdleMode.None)
        .build()
    );

    private final Setting<Double> idleRubberbandHeight = sgIdle.add(new DoubleSetting.Builder()
        .name("downwards-rubberband-height")
        .description("How high to teleport when idling.")
        .defaultValue(100)
        .visible(() -> idleMode.get() == IdleMode.Rubberband)
        .build()
    );

    private final Setting<Double> idleVelocity = sgIdle.add(new DoubleSetting.Builder()
        .name("downwards-velocity")
        .description("Your velocity when idling.")
        .defaultValue(0.1)
        .visible(() -> idleMode.get() == IdleMode.Velocity)
        .build()
    );

    private int downTicks;
    private int idleTicks;

    private List<PlayerMoveC2SPacket> packets;

    public RubberbandFly() {
        super(Categories.Movement, "rubberband-fly", "Fly with rubberbanding.");
    }

    @Override
    public void onActivate() {
        downTicks = 0;
        idleTicks = 0;

        packets = new ArrayList<>();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (isPlayerMoving()) {
            Vec3d prevPos;

            if (sprint.get()) {
                if (!mc.player.isSprinting()) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

                mc.player.setSprinting(true);
            }

            double down = (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0) ? (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -downwardsSpeed.get(), 0)).iterator().hasNext() ? 0 : downwardsSpeed.get()) : 0;
            double y = strict.get() ? (mc.player.getY() <= 10 ? 0 : 0.5) : rubberbandHeight.get();
            double[] dir = directionSpeed(velocity.get());

            double factor = mc.player.isSneaking() ? sneakFactor.get() : (mc.player.isTouchingWater() ? waterFactor.get() : airFactor.get());

            for (double i = (startFactor.get() / 10); i < speed.get(); i += factor) {
                double[] factorDir = directionSpeed(i);

                if (downwardsMode.get() == DownMode.Pre && (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0)) downTicks = 0;
                double d = (downwardsMode.get() == DownMode.Pre && downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0 ? down : 0);

                sendPacket(mc.player.getX() + factorDir[0], mc.player.getY() - d, mc.player.getZ() + factorDir[1]);

                if (downwardsMode.get() == DownMode.Pre) downTicks++;
            }

            double dx = (dir[0] * speed.get() > speed.get() ? speed.get() * (dir[0] > 0 ? 1 : -1) : dir[0] * speed.get());
            double dz = (dir[1] * speed.get() > speed.get() ? speed.get() * (dir[1] > 0 ? 1 : -1) : dir[1] * speed.get());

            prevPos = new Vec3d(mc.player.getX() + dx, mc.player.getY() - down, mc.player.getZ() + dz);

            double deltaX = dir[0] * speed.get();
            double deltaZ = dir[1] * speed.get();

            if (velocity.get() == 0) {
                deltaX = mc.player.getVelocity().getX();
                deltaZ = mc.player.getVelocity().getZ();
            }

            mc.player.setVelocity(deltaX, 0, deltaZ);

            if (positionMode.get() == PosMode.Pre || positionMode.get() == PosMode.Both) {
                mc.player.setPos(prevPos.x, prevPos.y, prevPos.z);
            }

            if (bypass.get()) {
                sendPacket(mc.player.getX() + dir[0], y, mc.player.getZ() + dir[1]);
                prevPos = new Vec3d(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);

                if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) mc.player.setPos(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);
            }

            if (downwardsMode.get() == DownMode.Post && (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0)) {
                sendPacket(prevPos.x, prevPos.y - (downwardsMode.get() == DownMode.Post ? down : 0), prevPos.z);
                if (tpBack.get()) sendPacket(prevPos.x, prevPos.y + (downwardsMode.get() == DownMode.Post ? down : 0), prevPos.z);

                downTicks = 0;
            }
        } else if (idleMode.get() != IdleMode.None && (idleTicks >= idleDelay.get() || idleDelay.get() == 0)) {
            switch (idleMode.get()) {
                case Velocity -> {
                    Vec3d velocity = mc.player.getVelocity();
                    mc.player.setVelocity(velocity.getX(), idleVelocity.get(), velocity.getZ());
                }

                case Rubberband -> {
                    sendPacket(mc.player.getX(), idleRubberbandHeight.get(), mc.player.getZ());
                }

                case Flight -> {
                    mc.player.setVelocity(0, 0, 0);
                }

                case TP -> {
                    sendPacket(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                    mc.player.updatePosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                    mc.player.setVelocity(0, 0, 0);
                }
            }

            if (sprint.get()) {
                if (!mc.player.isSprinting()) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

                mc.player.setSprinting(false);
            }

            idleTicks = 0;
        }

        downTicks++;
        idleTicks++;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (cancelPackets.get() && isPlayerMoving() && event.packet instanceof PlayerMoveC2SPacket packet && !packets.contains(packet)) {
            packets.remove(packet);
            event.cancel();
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());

            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
        }
    }

    // Utils

    private void sendPacket(double x, double y, double z) {
        boolean onGround = spoofOnGround.get() || mc.player.isOnGround();

        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround);

        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    private boolean isPlayerMoving() {
        return mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F || mc.player.upwardSpeed != 0.0F;
    }

    private double[] directionSpeed(double speed) {
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

    // Constants

    public enum PosMode {
        None,
        Pre,
        Post,
        Both
    }

    public enum DownMode {
        None,
        Pre,
        Post
    }

    public enum IdleMode {
        None,
        Velocity,
        Rubberband,
        Flight,
        TP
    }
}
