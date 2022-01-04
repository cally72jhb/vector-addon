package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.TimeVec;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClipPhase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> extreme = sgGeneral.add(new BoolSetting.Builder()
        .name("extreme")
        .description("Whether or not to do random big moves.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> hspeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("At which speed to travel horizontal.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Double> vspeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("At which speed to travel up and down.")
        .defaultValue(0.1)
        .min(0)
        .build()
    );

    private int teleportId = 0;

    private ArrayList<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private Map<Integer, TimeVec> posLooks = new ConcurrentHashMap<>();

    double speedX = 0;
    double speedY = 0;
    double speedZ = 0;

    private int ticksExisted;

    private static final Random random = new Random();

    public ClipPhase() {
        super(VectorAddon.MOVEMENT, "clip-phase", "Allows you to phase with packets.");
    }

    @Override
    public void onActivate() {
        mc.worldRenderer.reload();
        packets.clear();
        posLooks.clear();
        teleportId = -1;
        ticksExisted = 0;
    }

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        if (ticksExisted % 20 == 0) {
            posLooks.forEach((tp, timeVec3d) -> {
                if (System.currentTimeMillis() - timeVec3d.getTime() > TimeUnit.SECONDS.toMillis(30L)) {
                    posLooks.remove(tp);
                }
            });
        }

        ticksExisted++;

        mc.player.setVelocity(0, 0, 0);

        if (teleportId <= 0) {
            PlayerMoveC2SPacket startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + randomLimitedVertical(), mc.player.getZ(), mc.player.isOnGround());
            packets.add(startingOutOfBoundsPos);
            mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
        }

        double[] dir = VectorUtils.directionSpeed(hspeed.get().floatValue() / 10);

        speedX = dir[0];
        speedY = mc.options.keyJump.isPressed() ? (vspeed.get() / 10) : (mc.options.keySneak.isPressed() ? -(vspeed.get() / 10) : 0);
        speedZ = dir[1];

        Vec3d newPos = new Vec3d(mc.player.getX() + speedX, mc.player.getY(), mc.player.getZ() + speedZ);
        Vec3d blockCenter = new Vec3d(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()).add(0.5, 0, 0.5);

        Vec3d min = newPos.subtract(0.3, 0, 0.3);
        Vec3d max = newPos.add(0.3, 0, 0.3);

        Vec3i minI = new Vec3i(Math.floor(min.x), Math.floor(min.y), Math.floor(min.z));
        Vec3i maxI = new Vec3i(Math.floor(max.x), Math.floor(max.y), Math.floor(max.z));

        if (!minI.equals(maxI) && newPos.distanceTo(blockCenter) > mc.player.getPos().distanceTo(blockCenter) && !extreme.get()) {
            dir = VectorUtils.directionSpeed(0.064f);
            speedX = dir[0];
            speedY = mc.options.keyJump.isPressed() ? (vspeed.get() / 10) : (mc.options.keySneak.isPressed() ? -(vspeed.get() / 10) : 0);
            speedZ = dir[1];

            PlayerMoveC2SPacket move = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + speedX, mc.player.getY() + speedY, mc.player.getZ() + speedZ, mc.player.isOnGround());
            packets.add(move);
            mc.getNetworkHandler().sendPacket(move);
            PlayerMoveC2SPacket extremeMove = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + speedX, mc.player.getY() + randomLimitedVertical(), mc.player.getZ() + speedZ, mc.player.isOnGround());
            packets.add(extremeMove);
            mc.getNetworkHandler().sendPacket(extremeMove);
            teleportId++;
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId));
            posLooks.put(teleportId, new TimeVec(mc.player.getX(), mc.player.getY(), mc.player.getZ(), System.currentTimeMillis()));
        } else {
            PlayerMoveC2SPacket move = new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, mc.player.isOnGround());
            packets.add(move);
            mc.getNetworkHandler().sendPacket(move);
        }

        mc.player.setVelocity(speedX, speedY, speedZ);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        ((IVec3d) event.movement).set(speedX, speedY, speedZ);
    }

    @EventHandler
    public void isCube(CollisionShapeEvent event) {
        if (VectorUtils.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), event.pos.getX(), event.pos.getY(), event.pos.getZ()) > 3) return;

        event.shape = VoxelShapes.empty();
    }

    @EventHandler
    public void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && !(event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround)) {
            event.cancel();
        }
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (this.packets.contains(packet)) {
                this.packets.remove(packet);
                return;
            }
            event.cancel();
        }
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            if (mc.player.isAlive()) {
                if (this.teleportId <= 0) {
                    this.teleportId = packet.getTeleportId();
                } else {
                    if (mc.world.isPosLoaded(mc.player.getBlockX(), mc.player.getBlockZ())) {
                        if (posLooks.containsKey(packet.getTeleportId())) {
                            TimeVec vec = posLooks.get(packet.getTeleportId());
                            if (vec.x == packet.getX() && vec.y == packet.getY() && vec.z == packet.getZ()) {
                                posLooks.remove(packet.getTeleportId());
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }

            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
            teleportId = packet.getTeleportId();
        }
    }

    private double randomLimitedVertical() {
        int randomValue = random.nextInt(22);
        randomValue += 70;
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }
}
