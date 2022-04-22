package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AntiLagBack extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to ignore when rubberbanding.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<Boolean> antiMovementLagBack = sgGeneral.add(new BoolSetting.Builder()
        .name("movement-rubberband")
        .description("Will stop you being flagged for wrong movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> removePositionFlag = sgGeneral.add(new BoolSetting.Builder()
        .name("remove-pos-flag")
        .description("Removes the position flags form the teleport packets.")
        .defaultValue(false)
        .visible(() -> !antiMovementLagBack.get())
        .build()
    );

    private final Setting<Boolean> removeRotationFlag = sgGeneral.add(new BoolSetting.Builder()
        .name("remove-rotation-flag")
        .description("Removes the rotation flags form the teleport packets.")
        .defaultValue(true)
        .visible(() -> !antiMovementLagBack.get())
        .build()
    );

    private final Setting<Boolean> updatePos = sgGeneral.add(new BoolSetting.Builder()
        .name("update-position")
        .description("Updates your position when rubberbanding.")
        .defaultValue(true)
        .visible(antiMovementLagBack::get)
        .build()
    );

    private final Setting<Boolean> updateRotation = sgGeneral.add(new BoolSetting.Builder()
        .name("update-rotation")
        .description("Updates your rotation when rubberbanding.")
        .defaultValue(true)
        .visible(antiMovementLagBack::get)
        .build()
    );

    private final Setting<Boolean> allowDismount = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-dismount")
        .description("Whether or not to dismount from entities when the server tell you to.")
        .defaultValue(true)
        .visible(antiMovementLagBack::get)
        .build()
    );

    private final Setting<Boolean> antiInteractLagBack = sgGeneral.add(new BoolSetting.Builder()
        .name("interact-rubberband")
        .description("Will stop you being flagged when consuming items or interacting with blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("The items to ignore when spoofing interacting.")
        .defaultValue(List.of())
        .build()
    );

    private int teleportID;

    private Vec3d pos;
    private float yaw;
    private float pitch;
    private boolean dismount;

    private boolean prevEnabled;
    private int waitTicks;

    private Hand hand;

    public AntiLagBack() {
        super(Categories.Movement, "anti-lag-back", "Prevents you from getting flagged by the anti-cheat in some cases.");
    }

    @Override
    public void onActivate() {
        teleportID = -1;

        pos = null;
        yaw = 0;
        pitch = 0;
        dismount = false;

        prevEnabled = false;

        hand = null;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (prevEnabled) {
            waitTicks = 0;
            prevEnabled = false;
        }

        if (teleportID >= 0 && antiMovementLagBack.get()) {
            int prevID = teleportID;
            teleportID = -1;

            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(prevID));

            if (updatePos.get()) mc.player.updatePosition(pos.getX(), pos.getY(), pos.getZ());
            if (updateRotation.get()) mc.player.setYaw(yaw);
            if (updateRotation.get()) mc.player.setPitch(pitch);

            if (dismount && allowDismount.get()) mc.player.dismountVehicle();

            pos = null;
            yaw = 0;
            pitch = 0;
            dismount = false;
        }

        hand = null;
        waitTicks++;
    }

    @EventHandler
    public void onPacketSent(PacketEvent.Send event) {
        if (canSpoof()) {
            if (event.packet instanceof TeleportConfirmC2SPacket && teleportID >= 0) {
                event.cancel();
            } else if (event.packet instanceof PlayerInteractItemC2SPacket packet && antiInteractLagBack.get()) {
                ItemStack stack = mc.player.getStackInHand(packet.getHand());

                if (stack != null && stack.getItem() != null && !items.get().contains(stack.getItem())) {
                    if (hand != null && packet.getHand() == hand) {
                        event.cancel();
                    } else if (hand == null) {
                        hand = packet.getHand();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH + 50)
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet && canSpoof()) {
            if (antiMovementLagBack.get()) {
                teleportID = packet.getTeleportId();

                pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                yaw = packet.getYaw();
                pitch = packet.getPitch();
                dismount = packet.shouldDismount();

                event.cancel();
            } else {
                if (removePositionFlag.get()) {
                    packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X);
                    packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y);
                    packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Z);
                }

                if (removeRotationFlag.get()) {
                    packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
                    packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
                }
            }
        }
    }

    // Utils

    private boolean canSpoof() {
        if (modules.get().isEmpty() || mc.world == null || mc.player == null || mc.player.currentScreenHandler == null || mc.currentScreen instanceof DownloadingTerrainScreen
            || mc.player.isRegionUnloaded() || mc.player.isRegionUnloaded() || waitTicks < 15) {
            return false;
        }

        for (Module module : modules.get()) {
            if (module.isActive() && Modules.get().getList().contains(module)) {
                prevEnabled = true;
                return false;
            }
        }

        return true;
    }
}
