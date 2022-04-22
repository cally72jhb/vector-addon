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

    private final Setting<FlightMode> flightMode = sgGeneral.add(new EnumSetting.Builder<FlightMode>()
        .name("flight-mode")
        .description("How to fly on servers.")
        .defaultValue(FlightMode.Rubberband)
        .build()
    );

    private final Setting<Boolean> bypassVertical = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass-vertical")
        .description("Allows to essentially fly up and down on some servers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> velocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity")
        .description("Your client-side velocity.")
        .defaultValue(0)
        .min(0)
        .sliderMin(0)
        .sliderMax(1.25)
        .visible(() -> flightMode.get() != FlightMode.Fast)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("At which speed to travel.")
        .defaultValue(1)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(2)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("At which speed to travel vertically.")
        .defaultValue(1)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(1.5)
        .visible(() -> flightMode.get() == FlightMode.Packet || bypassVertical.get())
        .build()
    );

    private final Setting<Double> startFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("start-factor")
        .description("At what point to start.")
        .defaultValue(0.625)
        .min(0)
        .sliderMin(0.001)
        .sliderMax(1)
        .visible(() -> flightMode.get() != FlightMode.Packet)
        .build()
    );

    private final Setting<Double> airFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("air-factor")
        .description("The movement distance when in air.")
        .defaultValue(0.262)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.312)
        .visible(() -> flightMode.get() != FlightMode.Packet)
        .build()
    );

    private final Setting<Double> waterFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("water-factor")
        .description("The movement distance when in water.")
        .defaultValue(0.160)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.212)
        .visible(() -> flightMode.get() != FlightMode.Packet)
        .build()
    );

    private final Setting<Double> sneakFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("sneak-factor")
        .description("The movement distance when sneaking.")
        .defaultValue(0.212)
        .min(0.001)
        .sliderMin(0.001)
        .sliderMax(0.3)
        .visible(() -> flightMode.get() != FlightMode.Packet)
        .build()
    );


    // Bypass


    private final Setting<Boolean> spoofOnGround = sgBypass.add(new BoolSetting.Builder()
        .name("spoof-on-ground")
        .description("Sets you server-side on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sprint = sgBypass.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Automatically sprints to allow higher speeds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rubberbandBypass = sgBypass.add(new BoolSetting.Builder()
        .name("rubberband-bypass")
        .description("Allows to travel at higher speed on some servers.")
        .defaultValue(true)
        .visible(() -> flightMode.get() != FlightMode.Packet)
        .build()
    );

    private final Setting<Boolean> strict = sgBypass.add(new BoolSetting.Builder()
        .name("strict")
        .description("Stricts your movement.")
        .defaultValue(false)
        .visible(() -> flightMode.get() != FlightMode.Packet && rubberbandBypass.get())
        .build()
    );

    private final Setting<Double> rubberbandHeight = sgBypass.add(new DoubleSetting.Builder()
        .name("rubberband-height")
        .description("The vertical rubberband height.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(100)
        .visible(() -> flightMode.get() != FlightMode.Packet && rubberbandBypass.get() && !strict.get())
        .build()
    );

    private final Setting<PosMode> positionMode = sgBypass.add(new EnumSetting.Builder<PosMode>()
        .name("set-pos-mode")
        .description("How to update your position.")
        .defaultValue(PosMode.Post)
        .build()
    );

    private final Setting<CancelMode> cancelMode = sgBypass.add(new EnumSetting.Builder<CancelMode>()
        .name("cancel-packets-mode")
        .description("How to cancel irrelevant packets send by you.")
        .defaultValue(CancelMode.Strict)
        .build()
    );


    // Anti Kick


    private final Setting<DownMode> downwardsMode = sgAntiKick.add(new EnumSetting.Builder<DownMode>()
        .name("downwards-mode")
        .description("How to go downwards.")
        .defaultValue(DownMode.Post)
        .build()
    );

    private final Setting<Integer> downwardsDelay = sgAntiKick.add(new IntSetting.Builder()
        .name("downwards-delay")
        .description("How long to wait before going down again.")
        .defaultValue(15)
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

    private final Setting<Boolean> downwardsBypass = sgAntiKick.add(new BoolSetting.Builder()
        .name("downwards-bypass")
        .description("Sends downwards packets.")
        .defaultValue(true)
        .visible(() -> downwardsMode.get() == DownMode.Post)
        .build()
    );

    private final Setting<Boolean> tpBack = sgAntiKick.add(new BoolSetting.Builder()
        .name("tp-back")
        .description("Teleports you back to your original position.")
        .defaultValue(true)
        .visible(() -> downwardsMode.get() != DownMode.None && !downwardsBypass.get())
        .build()
    );


    // Idle


    private final Setting<IdleMode> idleMode = sgIdle.add(new EnumSetting.Builder<IdleMode>()
        .name("idle-mode")
        .description("What to do when not moving.")
        .defaultValue(IdleMode.Bypass)
        .build()
    );

    private final Setting<Integer> idleDelay = sgIdle.add(new IntSetting.Builder()
        .name("idle-delay")
        .description("How long to wait before spoofing again.")
        .defaultValue(10)
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
        .visible(() -> idleMode.get() == IdleMode.Velocity || idleMode.get() == IdleMode.Bypass)
        .build()
    );

    private final Setting<Boolean> tpBackOnIdle = sgIdle.add(new BoolSetting.Builder()
        .name("tp-back-on-idle")
        .description("Teleports you back to your original position after idling down.")
        .defaultValue(true)
        .visible(() -> idleMode.get() == IdleMode.Bypass)
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
        boolean shouldGoDown = false;
        boolean shouldGoUp = false;

        // Anti Kick

        boolean antiKick = downwardsMode.get() == DownMode.Post && (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0)
            && downwardsBypass.get() && mc.player.prevY <= mc.player.getY() && (isPlayerMoving() || mc.options.jumpKey.isPressed());

        if (antiKick) {
            shouldGoDown = true;
            downTicks = 0;
        }

        // Main Movement

        if ((isPlayerMoving() || flightMode.get() == FlightMode.Packet && (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed())) && flightMode.get() != FlightMode.Fast && !shouldGoDown) {
            double[] dir = directionSpeed(velocity.get());

            double down = (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0) ? (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -downwardsSpeed.get(), 0)).iterator().hasNext() ? 0 : downwardsSpeed.get()) : 0;

            double dx = (dir[0] * speed.get() > speed.get() ? speed.get() * (dir[0] > 0 ? 1 : -1) : dir[0] * speed.get());
            double dz = (dir[1] * speed.get() > speed.get() ? speed.get() * (dir[1] > 0 ? 1 : -1) : dir[1] * speed.get());

            Vec3d prevPos = new Vec3d(mc.player.getX() + dx, mc.player.getY() - down, mc.player.getZ() + dz);

            // Updating Velocity

            double deltaX = dir[0] * speed.get();
            double deltaZ = dir[1] * speed.get();

            if (velocity.get() == 0) {
                deltaX = mc.player.getVelocity().getX();
                deltaZ = mc.player.getVelocity().getZ();
            }

            mc.player.setVelocity(deltaX, 0, deltaZ);

            // Sprinting

            if (sprint.get()) {
                if (!mc.player.isSprinting()) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                }

                mc.player.setSprinting(true);
            }

            // Different Modes

            if (flightMode.get() == FlightMode.Rubberband) {
                double y = strict.get() ? (mc.player.getY() <= 10 ? 0 : 0.5) : rubberbandHeight.get();
                double factor = mc.player.isSneaking() ? sneakFactor.get() : (mc.player.isTouchingWater() ? waterFactor.get() : airFactor.get());

                for (double i = (startFactor.get() / 10); i < speed.get(); i += factor) {
                    double[] factorDir = directionSpeed(i);

                    if (downwardsMode.get() == DownMode.Pre && (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0)) downTicks = 0;
                    double d = (downwardsMode.get() == DownMode.Pre && downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0 ? down : 0);

                    sendPacket(mc.player.getX() + factorDir[0], mc.player.getY() - d, mc.player.getZ() + factorDir[1]);

                    if (downwardsMode.get() == DownMode.Pre) downTicks++;
                }

                if (positionMode.get() == PosMode.Pre || positionMode.get() == PosMode.Both) {
                    mc.player.setPos(prevPos.x, prevPos.y, prevPos.z);
                }

                if (rubberbandBypass.get()) {
                    sendPacket(mc.player.getX() + dir[0], y, mc.player.getZ() + dir[1]);
                    prevPos = new Vec3d(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);

                    if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) {
                        mc.player.setPos(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);
                    }
                }
            } else if (flightMode.get() == FlightMode.Packet) {
                double horizontalFactor = ((double) Math.round((speed.get() / 16.666) * 1000) / 1000);
                double verticalFactor = ((double) Math.round((verticalSpeed.get() / 16.666) * 1000) / 1000);

                if (isPlayerMoving()) {
                    double[] speed = directionSpeed(horizontalFactor);

                    sendPacket(mc.player.getX() + speed[0], mc.player.getY(), mc.player.getZ() + speed[1], mc.player.isOnGround());
                    sendPacket(mc.player.getX(), 0, mc.player.getZ());

                    if (positionMode.get() == PosMode.Pre || positionMode.get() == PosMode.Both) {
                        mc.player.setPos(mc.player.getX() + speed[0], mc.player.getY(), mc.player.getZ() + speed[1]);
                    }
                } else if (mc.options.jumpKey.isPressed()) {
                    sendPacket(mc.player.getX(), mc.player.getY() + verticalFactor, mc.player.getZ(), mc.player.isOnGround());
                    sendPacket(mc.player.getX(), 0, mc.player.getZ());

                    if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) {
                        mc.player.setPos(mc.player.getX(), mc.player.getY() + verticalFactor, mc.player.getZ());
                    }
                } else if (mc.options.sneakKey.isPressed()) {
                    sendPacket(mc.player.getX(), mc.player.getY() - verticalFactor, mc.player.getZ(), mc.player.isOnGround());
                    sendPacket(mc.player.getX(), 0, mc.player.getZ());

                    if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) {
                        mc.player.setPos(mc.player.getX(), mc.player.getY() - verticalFactor, mc.player.getZ());
                    }
                }
            }

            if (downwardsMode.get() == DownMode.Post && (downTicks >= downwardsDelay.get() || downwardsDelay.get() == 0)) {
                sendPacket(prevPos.x, prevPos.y - (downwardsMode.get() == DownMode.Post ? down : 0), prevPos.z);
                if (tpBack.get()) sendPacket(prevPos.x, prevPos.y + (downwardsMode.get() == DownMode.Post ? down : 0), prevPos.z);

                downTicks = 0;
            }
        } else if (flightMode.get() == FlightMode.Fast && !shouldGoDown && isPlayerMoving()) {
            Vec3d vel = mc.player.getVelocity();
            double factor = mc.player.isSneaking() ? sneakFactor.get() : (mc.player.isTouchingWater() ? waterFactor.get() : airFactor.get());

            for (double i = (startFactor.get() / 10); i < speed.get(); i += factor) {
                double[] dir = directionSpeed((float) i);
                sendPacket(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);
            }

            if (rubberbandBypass.get()) {
                sendPacket(mc.player.getX() + vel.x, rubberbandHeight.get(), mc.player.getZ() + vel.z);

                if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) {
                    mc.player.setPos(mc.player.getX() + vel.x, mc.player.getY(), mc.player.getZ() + vel.z);
                }
            }
        }

        // Idling

        if (idleMode.get() != IdleMode.None && (idleTicks >= idleDelay.get() || idleDelay.get() == 0) && !isPlayerMoving()) {
            switch (idleMode.get()) {
                case Velocity -> {
                    Vec3d velocity = mc.player.getVelocity();
                    mc.player.setVelocity(velocity.getX(), idleVelocity.get(), velocity.getZ());
                }

                case Rubberband -> {
                    sendPacket(mc.player.getX(), idleRubberbandHeight.get(), mc.player.getZ());
                    mc.player.setVelocity(0, 0, 0);
                }

                case Flight -> {
                    mc.player.setVelocity(0, 0, 0);
                }

                case TP -> {
                    sendPacket(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                    mc.player.updatePosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                    mc.player.setVelocity(0, 0, 0);
                }

                case Bypass -> {
                    mc.player.setVelocity(0, 0, 0);

                    if (!mc.options.sneakKey.isPressed() && mc.player.prevY > mc.player.getY() && tpBackOnIdle.get()) {
                        shouldGoUp = true;
                        shouldGoDown = false;
                    } else if (!mc.options.jumpKey.isPressed()) {
                        shouldGoDown = true;
                    }
                }
            }

            if (sprint.get()) {
                if (!mc.player.isSprinting()) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

                mc.player.setSprinting(false);
            }

            idleTicks = 0;
        } else if (!isPlayerMoving()) {
            mc.player.setVelocity(0, 0, 0);
        }

        // Vertical Anti Kick Movement

        if (bypassVertical.get() && !antiKick || shouldGoDown || shouldGoUp && tpBackOnIdle.get()) {
            double verticalFactor = ((double) Math.round(((shouldGoDown || shouldGoUp ? (isPlayerMoving() ? downwardsSpeed.get() * 20 : idleVelocity.get() * 10) : verticalSpeed.get()) / 16.666) * 1000) / 1000);

            if ((mc.options.jumpKey.isPressed() && !shouldGoDown || shouldGoUp) && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, verticalFactor, 0)).iterator().hasNext()) {
                sendPacket(mc.player.getX(), mc.player.getY() + verticalFactor, mc.player.getZ(), mc.player.isOnGround());
                sendPacket(mc.player.getX(), 0, mc.player.getZ());

                if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) {
                    mc.player.setPos(mc.player.getX(), mc.player.getY() + verticalFactor, mc.player.getZ());
                }
            } else if ((mc.options.sneakKey.isPressed() || shouldGoDown) && !shouldGoUp && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -verticalFactor, 0)).iterator().hasNext()) {
                sendPacket(mc.player.getX(), mc.player.getY() - verticalFactor, mc.player.getZ(), mc.player.isOnGround());
                sendPacket(mc.player.getX(), 0, mc.player.getZ());

                if (positionMode.get() == PosMode.Post || positionMode.get() == PosMode.Both) {
                    mc.player.setPos(mc.player.getX(), mc.player.getY() - verticalFactor, mc.player.getZ());
                }
            }
        }

        downTicks++;
        idleTicks++;
    }

    // Irrelevant Packet Cancel

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if ((cancelMode.get() == CancelMode.All || cancelMode.get() == CancelMode.Both) && !(event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround)
                || (cancelMode.get() == CancelMode.Strict || cancelMode.get() == CancelMode.Both) && !packets.contains(packet)) {

                packets.remove(packet);
                event.cancel();
            }
        }
    }

    // No Rotate

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
        sendPacket(x, y, z, spoofOnGround.get() || mc.player.isOnGround());
    }

    private void sendPacket(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround);

        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    private boolean isPlayerMoving() {
        return mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F || mc.player.upwardSpeed != 0.0F;
    }

    private double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
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

    public enum FlightMode {
        Fast,
        Packet,
        Rubberband
    }

    public enum CancelMode {
        None,
        All,
        Strict,
        Both
    }

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
        TP,
        Bypass
    }
}
