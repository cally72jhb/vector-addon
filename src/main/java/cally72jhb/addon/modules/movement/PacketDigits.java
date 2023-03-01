package cally72jhb.addon.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.mixin.PlayerMoveC2SPacketAccessor;
import cally72jhb.addon.mixin.VehicleMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;

public class PacketDigits extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> digits = sgGeneral.add(new IntSetting.Builder()
            .name("digits")
            .description("How many digits to remove.")
            .defaultValue(2)
            .sliderMin(0)
            .sliderMax(5)
            .noSlider()
            .build()
    );

    private final Setting<Boolean> modifyY = sgGeneral.add(new BoolSetting.Builder()
            .name("modify-y")
            .description("Rounds your y coordinate.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> firstPacket = sgGeneral.add(new BoolSetting.Builder()
            .name("first-packet")
            .description("Modifies the first movement packet you send when joining a server. (note: this settings doesn't depend on this module being activated)")
            .defaultValue(false)
            .build()
    );

    public PacketDigits() {
        super(VectorAddon.CATEGORY, "packet-digits", "Removes digits from your movement packets to make them smaller.");
    }

    public double round(double value) {
        int digit = (int) Math.pow(10, digits.get());
        double round = ((double) (Math.round(value * digit)) / digit);
        return Math.nextAfter(round, round + Math.signum(round));
    }

    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (firstPacket.get() && mc.player.age <= 20) {
            if (event.packet instanceof PlayerMoveC2SPacket packet) {
                if (packet.changesPosition()) {
                    ((PlayerMoveC2SPacketAccessor) packet).setX(round(packet.getX(0)));
                    ((PlayerMoveC2SPacketAccessor) packet).setY(modifyY.get() ? round(packet.getY(0)) : packet.getY(0));
                    ((PlayerMoveC2SPacketAccessor) packet).setZ(round(packet.getZ(0)));
                }
            } else if (event.packet instanceof VehicleMoveC2SPacket packet) {
                ((VehicleMoveC2SPacketAccessor) packet).setX(round(packet.getX()));
                ((VehicleMoveC2SPacketAccessor) packet).setY(modifyY.get() ? round(packet.getY()) : packet.getY());
                ((VehicleMoveC2SPacketAccessor) packet).setZ(round(packet.getZ()));
            }
        }
    }
}
