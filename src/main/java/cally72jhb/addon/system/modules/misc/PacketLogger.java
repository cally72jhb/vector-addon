package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.MeteorClient;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;

import java.lang.reflect.Constructor;
import java.util.Set;

public class PacketLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> s2c = sgGeneral.add(new BoolSetting.Builder()
        .name("server-to-client")
        .description("Prints out the server-to-client packets in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> s2cConstructor = sgGeneral.add(new BoolSetting.Builder()
        .name("s2c-constructor")
        .description("Prints out the packets constructors.")
        .defaultValue(false)
        .visible(s2c::get)
        .build()
    );

    private final Setting<Boolean> c2s = sgGeneral.add(new BoolSetting.Builder()
        .name("client-to-server")
        .description("Prints out the client-to-server packets in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> c2sConstructor = sgGeneral.add(new BoolSetting.Builder()
        .name("c2s-constructor")
        .description("Prints out the packets constructors.")
        .defaultValue(true)
        .visible(c2s::get)
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("s2c-packets")
        .description("Server-to-client packets to not print out.")
        .filter(klass -> PacketUtils.getS2CPackets().contains(klass))
        .visible(s2c::get)
        .build()
    );

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("c2s-packets")
        .description("Client-to-server packets to not print out.")
        .filter(klass -> PacketUtils.getC2SPackets().contains(klass))
        .visible(c2s::get)
        .build()
    );

    public PacketLogger() {
        super(Categories.Misc, "packet-logger", "Writes out the send / receive packets.");

        MeteorClient.EVENT_BUS.subscribe(new Listener());
    }

    private class Listener {
        @EventHandler(priority = EventPriority.HIGHEST + 1)
        private void onReceivePacket(PacketEvent.Receive event) {
            if (isActive() && s2c.get() && !s2cPackets.get().contains(event.packet.getClass()) && !s2cPackets.get().contains(event.packet.getClass().getSuperclass())) {
                Class<? extends Packet<?>> packet = (Class<? extends Packet<ClientPlayPacketListener>>) event.packet.getClass();

                String name = PacketUtils.getName(packet);

                if (name == null) {
                    packet = (Class<? extends Packet<?>>) event.packet.getClass().getSuperclass();
                    name = PacketUtils.getName(packet);
                }

                info(name);

                if (s2cConstructor.get()) {
                    for (Constructor constructor : packet.getConstructors()) info("constructor: " + constructor);
                    for (Constructor constructor : packet.getDeclaredConstructors()) info("declared constructor: " + constructor);
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST + 1)
        private void onSendPacket(PacketEvent.Send event) {
            if (isActive() && c2s.get() && !c2sPackets.get().contains(event.packet.getClass()) && !c2sPackets.get().contains(event.packet.getClass().getSuperclass())) {
                Class<? extends Packet<?>> packet = (Class<? extends Packet<ServerPlayPacketListener>>) event.packet.getClass();

                String name = PacketUtils.getName(packet);

                if (name == null) {
                    packet = (Class<? extends Packet<ServerPlayPacketListener>>) event.packet.getClass().getSuperclass();
                    name = PacketUtils.getName(packet);
                }

                info(name);

                if (c2sConstructor.get()) {
                    for (Constructor constructor : packet.getConstructors()) info("constructor: " + constructor);
                    for (Constructor constructor : packet.getDeclaredConstructors()) info("declared constructor: " + constructor);
                }
            }
        }
    }
}
