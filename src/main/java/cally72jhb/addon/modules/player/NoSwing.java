package cally72jhb.addon.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;

public class NoSwing extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> clientSide = sgGeneral.add(new BoolSetting.Builder()
        .name("client-side")
        .description("Removes the hand swing animation only for you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> others = sgGeneral.add(new BoolSetting.Builder()
        .name("others")
        .description("Removes the hand swing animation of others.")
        .defaultValue(false)
        .build()
    );

    // Constructor

    public NoSwing() {
        super(Categories.Misc, "no-swing", "Removes the hand swing animation client-side.");
    }

    // Listeners

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityAnimationS2CPacket packet && others.get()
            && (packet.getAnimationId() == EntityAnimationS2CPacket.SWING_MAIN_HAND
            || packet.getAnimationId() == EntityAnimationS2CPacket.SWING_OFF_HAND)) {

            event.cancel();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof HandSwingC2SPacket && !clientSide.get()) {
            event.cancel();
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        mc.player.handSwinging = false;
        mc.player.handSwingProgress = 0;
        mc.player.lastHandSwingProgress = 0;
        mc.player.handSwingTicks = 0;
        mc.player.preferredHand = null;
    }
}
