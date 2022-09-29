package cally72jhb.addon.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder()
        .name("ping")
        .description("The ping to set.")
        .defaultValue(200)
        .min(1)
        .sliderMin(100)
        .sliderMax(1000)
        .noSlider()
        .build()
    );

    // Constructor

    public PingSpoof() {
        super(Categories.Misc, "ping-spoof", "Modify your ping.");
    }

    private long id;
    private long timer;

    @Override
    public void onActivate() {
        id = -1;
        timer = System.currentTimeMillis();
    }

    // Listeners

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (System.currentTimeMillis() - timer >= ping.get() && id >= 0) {
            mc.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(id));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof KeepAliveC2SPacket packet && id != packet.getId()) {
            id = packet.getId();
            timer = System.currentTimeMillis();

            event.cancel();
        }
    }

    // Info String

    @Override
    public String getInfoString() {
        if (mc != null && mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            return entry != null ? entry.getLatency() + "ms" : null;
        }

        return null;
    }
}
