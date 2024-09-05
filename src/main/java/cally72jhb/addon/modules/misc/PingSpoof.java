package cally72jhb.addon.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;

import java.util.HashSet;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder()
        .name("ping")
        .description("The ping in milliseconds to add to your ping.")
        .defaultValue(250)
        .min(1)
        .sliderMin(100)
        .sliderMax(500)
        .noSlider()
        .build()
    );

    // Variables

    private final Object2LongMap<KeepAliveC2SPacket> packets = new Object2LongOpenHashMap<>();

    // Constructor

    public PingSpoof() {
        super(Categories.Misc, "ping-spoof", "Modify your ping.");
    }

    // Overrides

    @Override
    public void onActivate() {
        this.packets.clear();
    }

    public void onDeactivate() {
        if (!this.packets.isEmpty()) {
            for (KeepAliveC2SPacket packet : new HashSet<>(this.packets.keySet())) {
                if (this.packets.getLong(packet) + (long) this.ping.get() <= System.currentTimeMillis()) {
                    mc.getNetworkHandler().sendPacket(packet);
                }
            }
        }
    }

    // Packet Send Event

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof KeepAliveC2SPacket packet) {
            if (!this.packets.isEmpty() && new HashSet<>(this.packets.keySet()).contains(packet)) {
                this.packets.removeLong(packet);

                return;
            }

            this.packets.put(packet, System.currentTimeMillis());

            event.cancel();
        }
    }

    // Packet Receive Event

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        for (KeepAliveC2SPacket packet : new HashSet<>(this.packets.keySet())) {
            if (this.packets.getLong(packet) + (long) this.ping.get() <= System.currentTimeMillis()) {
                mc.getNetworkHandler().sendPacket(packet);
                break;
            }
        }
    }
}
