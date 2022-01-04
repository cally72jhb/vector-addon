package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.ArrayList;
import java.util.Random;

public class NoFallPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How to cancel the fall damage.")
        .defaultValue(Mode.PACKET)
        .build()
    );

    private final Setting<PacketMode> packetMode = sgGeneral.add(new EnumSetting.Builder<PacketMode>()
        .name("packet-mode")
        .description("Which packets to send to the server.")
        .defaultValue(PacketMode.DOWN)
        .visible(() -> mode.get() != Mode.SLOW)
        .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("After what fall distance to trigger this module.")
        .defaultValue(5)
        .min(3)
        .build()
    );

    private final Setting<Double> overGroundDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("over-ground-distance")
        .description("How much you need to be above the ground to trigger this module.")
        .defaultValue(1)
        .min(0.1)
        .visible(() -> mode.get() == Mode.SLOW)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The speed for nofall.")
        .defaultValue(5)
        .min(0.1)
        .max(10)
        .sliderMin(0.1)
        .sliderMax(7.5)
        .visible(() -> mode.get() == Mode.SLOW)
        .build()
    );

    private final Setting<Boolean> bounds = sgGeneral.add(new BoolSetting.Builder()
        .name("bounds")
        .description("Bounds for the player.")
        .defaultValue(true)
        .build()
    );

    public enum Mode {
        VANILLA,
        PACKET,
        ANTI,
        SLOW,
        TP
    }

    public enum PacketMode {
        PRESERVE,
        DOWN,
        BYPASS,
        OBSCURE
    }

    private double lowestY;
    private int ticksExisted;

    private int teleportId;
    private ArrayList<PlayerMoveC2SPacket> packets;
    private Random random;

    public NoFallPlus() {
        super(VectorAddon.MOVEMENT, "no-fall-plus", "Cancels fall damage.");
    }

    @Override
    public void onActivate() {
        packets = new ArrayList<>();
        random = new Random();
        ticksExisted = 0;
        lowestY = 320;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        ticksExisted++;

        switch (mode.get()) {
            case PACKET:
                if (mc.player.fallDistance > fallDistance.get()) {
                    if (teleportId <= 0) {
                        PlayerMoveC2SPacket boundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
                        packets.add(boundsPos);
                        mc.getNetworkHandler().sendPacket(boundsPos);
                    } else {
                        PlayerMoveC2SPacket nextPos = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.062, mc.player.getZ(), mc.player.isOnGround());
                        packets.add(nextPos);
                        mc.getNetworkHandler().sendPacket(nextPos);

                        PlayerMoveC2SPacket downPacket = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 1, mc.player.getZ(), mc.player.isOnGround());
                        packets.add(downPacket);
                        mc.getNetworkHandler().sendPacket(downPacket);

                        teleportId++;

                        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId - 1));
                        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId));
                        mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId + 1));
                    }
                }

                break;
            case TP:
                if (mc.player.fallDistance >= fallDistance.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), -1000, mc.player.getZ(), mc.player.isOnGround()));
                }

                break;
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            switch (mode.get()) {
                case ANTI:
                    if (mc.player.fallDistance > Math.min(3, fallDistance.get())) {
                        if (((PlayerMoveC2SPacket) event.packet).getY(mc.player.getY()) < lowestY) {
                            ((PlayerMoveC2SPacketAccessor) event.packet).setY(mc.player.getY() + Math.min(3.0F, fallDistance.get()));
                            lowestY = ((PlayerMoveC2SPacket) event.packet).getY(mc.player.getY());
                        } else {
                            lowestY = 256;
                            mc.player.fallDistance = 0;
                        }
                    }

                    break;
                case VANILLA:
                    if (mc.player.fallDistance > fallDistance.get()) ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
            }
        }
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player.isAlive() && mode.get() == Mode.PACKET && mc.player.fallDistance > fallDistance.get() && event.packet instanceof PlayerPositionLookS2CPacket packet) {
            if (teleportId <= 0) {
                teleportId = packet.getTeleportId();
            } else {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.PACKET && mc.player.fallDistance >= fallDistance.get() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.05, 0)).iterator().hasNext()) {
            ((IVec3d) event.movement).set(0, -0.062, 0);
        }

        if (mode.get() == Mode.SLOW && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().stretch(0, -overGroundDistance.get(), 0))
            && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.05, 0)).iterator().hasNext()
            && mc.player.fallDistance >= fallDistance.get() && mc.player.prevY > mc.player.getY()) {
            Modules.get().get(Timer.class).setOverride(speed.get() / 10);
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }

    private double randomHorizontal() {
        int value = random.nextInt(bounds.get() ? 80 : (packetMode.get() == PacketMode.OBSCURE ? (ticksExisted % 2 == 0 ? 480 : 100) : 320000)) + (bounds.get() ? 5 : 50);

        return random.nextBoolean() ? -value : value;
    }
}
