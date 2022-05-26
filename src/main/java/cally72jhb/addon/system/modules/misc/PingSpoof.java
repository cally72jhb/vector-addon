package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Responds as fast as possible to keep alive packets send by the server.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder()
        .name("ping")
        .description("The ping to set.")
        .defaultValue(250)
        .min(0)
        .sliderMin(100)
        .sliderMax(1000)
        .noSlider()
        .visible(() -> !strict.get())
        .build()
    );

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

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (System.currentTimeMillis() - timer >= ping.get() && id >= 0 && !strict.get()) {
            mc.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(id));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof KeepAliveC2SPacket packet && id != packet.getId()) {
            if (strict.get()) {
                event.cancel();
            } else if (ping.get() != 0) {
                id = packet.getId();
                timer = System.currentTimeMillis();

                event.cancel();
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (strict.get() && event.packet instanceof KeepAliveS2CPacket packet) {
            id = packet.getId();
            mc.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(id));
        }
    }

    @Override
    public String getInfoString() {
        if (mc != null && mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            return entry != null ? entry.getLatency() + "ms" : null;
        }

        return null;
    }
}
