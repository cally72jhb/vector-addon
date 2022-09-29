package cally72jhb.addon.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class PortalGodMode extends Module {
    public PortalGodMode() {
        super(Categories.Misc, "portal-god-mode", "Exploits some bugs to make you invincible after you left a portal.");
    }

    private List<TeleportConfirmC2SPacket> packets;

    @Override
    public void onActivate() {
        packets = new ArrayList<>();
    }

    @Override
    public void onDeactivate() {
        if (!packets.isEmpty()) {
            for (TeleportConfirmC2SPacket packet : packets) {
                mc.getNetworkHandler().sendPacket(packet);
            }
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket packet) {
            packets.add(packet);
            event.setCancelled(true);
        }
    }
}
