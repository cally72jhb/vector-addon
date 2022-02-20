package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.Packet;

import java.util.Set;

public class DebugTools extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> s2c = sgGeneral.add(new BoolSetting.Builder()
        .name("server-to-client")
        .description("Prints out the server-to-client packets in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> c2s = sgGeneral.add(new BoolSetting.Builder()
        .name("client-to-server")
        .description("Prints out the client-to-server packets in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to ignore.")
        .filter(klass -> PacketUtils.getS2CPackets().contains(klass))
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to ignore.")
        .filter(klass -> PacketUtils.getC2SPackets().contains(klass))
        .build()
    );

    public DebugTools() {
        super(VectorAddon.Misc, "debug-tools", "Writes out the send / receive packets.");
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (s2c.get() && !s2cPackets.get().contains(event.packet.getClass())) {
            String name = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass());
            if (name == null) name = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass().getSuperclass());
            info("" + name);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2s.get() && !c2sPackets.get().contains(event.packet.getClass())) {
            String name = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass());
            if (name == null) name = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass().getSuperclass());
            info("" + name);
        }
    }
}
