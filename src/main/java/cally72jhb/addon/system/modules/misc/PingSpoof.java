package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.misc.SystemTimer;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;

import java.util.ArrayList;
import java.util.Random;

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
        .sliderMin(0)
        .sliderMax(1000)
        .visible(() -> !strict.get())
        .build()
    );

    private final Setting<Integer> ticks = sgGeneral.add(new IntSetting.Builder()
        .name("ticks")
        .description("How many ticks to wait before sending the keep alive packet.")
        .defaultValue(20)
        .min(0)
        .sliderMin(1)
        .sliderMax(40)
        .visible(() -> !strict.get())
        .build()
    );

    private final Setting<Integer> send = sgGeneral.add(new IntSetting.Builder()
        .name("packets")
        .description("How many packets to send when sending one keep alive packet.")
        .defaultValue(1)
        .min(1)
        .sliderMin(1)
        .sliderMax(5)
        .visible(() -> !strict.get())
        .build()
    );

    private final Setting<Boolean> negative = sgGeneral.add(new BoolSetting.Builder()
        .name("negative")
        .description("Makes you ping go down.")
        .defaultValue(false)
        .visible(() -> !strict.get())
        .build()
    );

    public PingSpoof() {
        super(VectorAddon.MISC, "ping-spoof", "Modify your ping.");
    }

    private int tick;
    private SystemTimer timer;
    private KeepAliveC2SPacket packet;

    private ArrayList<KeepAliveC2SPacket> packets;

    private final Random random = new Random();

    @Override
    public void onActivate() {
        packets = new ArrayList<>();
        timer = new SystemTimer();
        tick = 0;
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof KeepAliveC2SPacket keepAlive && packet != event.packet) {
            if (strict.get()) {
                event.cancel();
            } else if (ping.get() != 0) {
                packet = keepAlive;
                event.cancel();
                timer.reset();
            }
        }
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (strict.get() && event.packet instanceof KeepAliveS2CPacket keepAlive) {
            packet = new KeepAliveC2SPacket(keepAlive.getId());

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

        if (!packets.isEmpty() && send.get() > 1) {
            mc.getNetworkHandler().sendPacket(packets.get(0));
            packets.remove(0);
        }

        if (tick >= ticks.get() || ticks.get() == 0) {
            if (timer.hasPassed(negative.get() ? -ping.get() : ping.get()) && packet != null) {
                mc.getNetworkHandler().sendPacket(packet);

                if (send.get() > 1) {
                    KeepAliveC2SPacket keepAlive = new KeepAliveC2SPacket(packet.getId() + 1);

                    for (int i = 0; i < send.get() - 1; i++) {
                        packets.add(keepAlive);
                        keepAlive = new KeepAliveC2SPacket(random.nextInt(10) == 0 ? keepAlive.getId() + random.nextInt(3) + 1 : keepAlive.getId() + 1);
                    }
                }

                packet = null;
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
