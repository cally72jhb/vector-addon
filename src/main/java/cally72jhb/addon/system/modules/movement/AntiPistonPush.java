package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;

public class AntiPistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spoof = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof")
        .description("Spoofs your position to bypass horizontal piston pushing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sendTeleport = sgGeneral.add(new BoolSetting.Builder()
        .name("send-teleport")
        .description("Sends a teleport confirm packet when spoofing.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> invalidPacket = sgGeneral.add(new BoolSetting.Builder()
        .name("invalid-packet")
        .description("Sends invalid position packets to bypass some anti cheats.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> updatePosition = sgGeneral.add(new BoolSetting.Builder()
        .name("update-Position")
        .description("Updates your position to stop you from desyncing.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> updateY = sgGeneral.add(new BoolSetting.Builder()
        .name("update-Y")
        .description("Updates your y position.")
        .defaultValue(true)
        .visible(() -> spoof.get() && updatePosition.get())
        .build()
    );

    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private int teleportID = 0;

    public AntiPistonPush() {
        super(Categories.Movement, "anti-piston-push", "Prevents you from being pushed by pistons.");
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event) {
        if (spoof.get()) {
            Vec3d velocity = mc.player.getVelocity();

            if (updatePosition.get()) {
                mc.player.setVelocity(0, updateY.get() ? 0 : velocity.y, 0);
                velocity = new Vec3d(0, updateY.get() ? 0 : velocity.y, 0);
            }

            sendPackets(velocity.x, velocity.y, velocity.z, sendTeleport.get());
        }
    }

    @EventHandler
    public void onPacketSent(PacketEvent.Send event) {
        if (spoof.get() && event.packet instanceof PlayerMoveC2SPacket && !packets.remove(event.packet)) event.cancel();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (spoof.get() && event.packet instanceof PlayerPositionLookS2CPacket packet && !(mc.player == null || mc.world == null)) {
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());

            teleportID = packet.getTeleportId();
        }
    }

    private void sendPackets(double x, double y, double z, boolean teleport) {
        Vec3d pos = mc.player.getPos().add(x, y, z);
        Vec3d bounds = outOfBoundsVec(pos);

        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, mc.player.isOnGround()));
        if (invalidPacket.get()) sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, mc.player.isOnGround()));

        mc.player.setPos(pos.x, pos.y, pos.z);
        if (teleport) mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportID++));
    }

    private Vec3d outOfBoundsVec(Vec3d position) {
        return position.add(0.0, 1500.0, 0.0);
    }

    private void sendPacket(PlayerMoveC2SPacket packet) {
        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }
}
