package cally72jhb.addon.modules.movement;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFallPlus extends Module {
    public NoFallPlus() {
        super(VectorAddon.CATEGORY, "no-fall-plus", "A better version of no-fall");
    }

    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet && ((IPlayerMoveC2SPacket) packet).getTag() != 1337
                && !mc.player.getAbilities().creativeMode
                && !mc.player.isFallFlying() && mc.player.getVelocity().getY() < 0.1) {
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
        }
    }
}
