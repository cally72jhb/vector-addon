package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

public class PortalGodMode extends Module {
    public PortalGodMode() {
        super(Categories.Misc, "portal-god-mode", "Exploits some bugs to make you invincible after you left a portal.");
    }

    private TeleportConfirmC2SPacket packet = null;

    @Override
    public void onActivate() {
        packet = null;
    }

    @Override
    public void onDeactivate() {
        if (packet == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket) {
            event.setCancelled(true);
        }
    }
}
