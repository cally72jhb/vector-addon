package cally72jhb.addon.mixin;

import cally72jhb.addon.modules.movement.NoFallPlus;
import cally72jhb.addon.modules.movement.PacketDigits;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Shadow public abstract boolean isOpen();
    @Shadow protected abstract void sendQueuedPackets();
    @Shadow protected abstract void sendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks);

    @Shadow @Final private Queue<ClientConnection.QueuedPacket> packetQueue;

    @Inject(method = "send(Lnet/minecraft/network/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo info) {
        PacketDigits digits = Modules.get().get(PacketDigits.class);
        NoFallPlus noFall = Modules.get().get(NoFallPlus.class);
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc != null && mc.player != null) {
            if (noFall.isActive()) {
                if (packet instanceof PlayerMoveC2SPacket && ((IPlayerMoveC2SPacket) packet).getTag() != 1337
                        && !mc.player.getAbilities().creativeMode
                        && !mc.player.isFallFlying() && mc.player.getVelocity().getY() < 0.1) {
                    ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
                }
            }

            if (digits.isActive() || digits.shouldModifyFirstPacket() && mc.player.age <= 20) {
                if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround move) {
                    PlayerMoveC2SPacket.PositionAndOnGround modified = new PlayerMoveC2SPacket.PositionAndOnGround(
                            digits.round(move.getX(0)),
                            digits.shouldModifyY() ? digits.round(move.getY(0)) : move.getY(0),
                            digits.round(move.getZ(0)),

                            move.isOnGround()
                    );

                    info.cancel();

                    if (this.isOpen()) {
                        this.sendQueuedPackets();
                        this.sendImmediately(modified, callbacks);
                    } else {
                        this.packetQueue.add(new ClientConnection.QueuedPacket(modified, callbacks));
                    }
                } else if (packet instanceof PlayerMoveC2SPacket.Full move) {
                    PlayerMoveC2SPacket.Full modified = new PlayerMoveC2SPacket.Full(
                            digits.round(move.getX(0)),
                            digits.shouldModifyY() ? digits.round(move.getY(0)) : move.getY(0),
                            digits.round(move.getZ(0)),

                            move.getYaw(mc.player.getYaw()),
                            move.getPitch(mc.player.getPitch()),
                            move.isOnGround()
                    );

                    info.cancel();

                    if (this.isOpen()) {
                        this.sendQueuedPackets();
                        this.sendImmediately(modified, callbacks);
                    } else {
                        this.packetQueue.add(new ClientConnection.QueuedPacket(modified, callbacks));
                    }
                } else if (packet instanceof VehicleMoveC2SPacket move) {
                    BoatEntity entity = new BoatEntity(EntityType.BOAT, mc.world);

                    entity.setPos(
                            digits.round(move.getX()),
                            digits.shouldModifyY() ? digits.round(move.getY()) : move.getY(),
                            digits.round(move.getZ())
                    );

                    entity.setYaw(move.getYaw());
                    entity.setPitch(move.getPitch());

                    VehicleMoveC2SPacket modified = new VehicleMoveC2SPacket(entity);

                    info.cancel();

                    if (this.isOpen()) {
                        this.sendQueuedPackets();
                        this.sendImmediately(modified, callbacks);
                    } else {
                        this.packetQueue.add(new ClientConnection.QueuedPacket(modified, callbacks));
                    }
                }
            }
        }
    }
}
