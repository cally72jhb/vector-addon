package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.Packet;
import net.minecraft.text.Text;

import java.util.Set;

public class DebugTools extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> s2c = sgGeneral.add(new BoolSetting.Builder()
        .name("server-to-client")
        .description("Writes all server-to-client packets in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> c2s = sgGeneral.add(new BoolSetting.Builder()
        .name("client-to-server")
        .description("Writes all client-to-server packets in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("s2c-packets")
        .description("Server-to-client packets to ignore.")
        .defaultValue(new ObjectOpenHashSet<>(0))
        .filter(packet -> PacketUtils.getS2CPackets().contains(packet))
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("c2s-packets")
        .description("Client-to-server packets to ignore.")
        .defaultValue(new ObjectOpenHashSet<>(0))
        .filter(packet -> PacketUtils.getC2SPackets().contains(packet))
        .build()
    );

    public static Object2BooleanMap<Class<? extends Packet<?>>> S2C_PACKETS = new Object2BooleanArrayMap<>();
    public static Object2BooleanMap<Class<? extends Packet<?>>> C2S_PACKETS = new Object2BooleanArrayMap<>();

    public DebugTools() {
        super(VectorAddon.CATEGORY, "debug-tools", "Uses the chat to write debug stuff.");

        for (Class<? extends Packet<?>> packet : PacketUtils.getS2CPackets()) S2C_PACKETS.put(packet, false);
        for (Class<? extends Packet<?>> packet : PacketUtils.getC2SPackets()) C2S_PACKETS.put(packet, false);
    }

    @EventHandler(priority = EventPriority.MEDIUM + 50)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!s2c.get() || event.isCancelled()) return;
        if (s2cPackets.get().contains(event.packet.getClass())) return;
        String string = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass());
        if (string == null) string = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass().getSuperclass());

        ChatUtils.sendMsg(Text.of(string));
    }

    @EventHandler(priority = EventPriority.MEDIUM + 50)
    private void onSendPacket(PacketEvent.Send event) {
        if (!c2s.get() || event.isCancelled()) return;
        if (c2sPackets.get().contains(event.packet.getClass())) return;
        String string = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass());
        if (string == null) string = PacketUtils.getName((Class<? extends Packet<?>>) event.packet.getClass().getSuperclass());

        ChatUtils.sendMsg(Text.of(string));
    }
}
