package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("When the spoofing will be performed.")
        .defaultValue(Mode.Render)
        .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Responds as fast as possible to keep alive packets send by the server.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder()
        .name("ping")
        .description("The Ping to set.")
        .defaultValue(200)
        .min(0)
        .sliderMin(100)
        .sliderMax(1000)
        .visible(() -> !strict.get())
        .build()
    );

    private final Setting<Integer> ticks = sgGeneral.add(new IntSetting.Builder()
        .name("ticks")
        .description("How many ticks to wait before sending the keep alive packet.")
        .defaultValue(0)
        .min(0)
        .sliderMin(0)
        .sliderMax(50)
        .visible(() -> !strict.get())
        .build()
    );

    public PingSpoof() {
        super(Categories.Misc, "ping-spoof", "Modify your ping.");
    }

    private long id;
    private int tick;
    private long timer;

    @Override
    public void onActivate() {
        id = 0;
        tick = 0;
        timer = System.currentTimeMillis();
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
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
    public void onReceivePacket(PacketEvent.Receive event) {
        if (strict.get() && event.packet instanceof KeepAliveS2CPacket packet) {
            id = packet.getId();

            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (mode.get() ==  Mode.Pre) spoof();
    }

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        if (mode.get() ==  Mode.Post) spoof();
    }

    @EventHandler
    public void onRender(Render2DEvent event) {
        if (mode.get() ==  Mode.Render) spoof();
    }

    @Override
    public String getInfoString() {
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() + "ms" : null;
    }

    private void spoof() {
        if (strict.get()) return;

        if (tick >= ticks.get() || ticks.get() == 0) {
            if (System.currentTimeMillis() - timer >= ping.get() && id != -1) {
                mc.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(id));

                id = -1;
            }

            tick = 0;
        } else {
            tick++;
        }
    }

    public enum Mode {
        Render,
        Post,
        Pre
    }
}
