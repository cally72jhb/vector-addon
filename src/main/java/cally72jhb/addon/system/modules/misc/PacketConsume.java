package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class PacketConsume extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("The modules that when they are active will stop this module from working.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<Boolean> cancel = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel")
        .description("Cancels the right clicking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopUsing = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-using")
        .description("Stops your current item from being consumed normally.")
        .defaultValue(false)
        .visible(cancel::get)
        .build()
    );

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
        .name("auto-switch")
        .description("Switches to the consumable in your hotbar.")
        .defaultValue(AutoSwitchMode.Silent)
        .build()
    );

    private final Setting<Boolean> sync = sgGeneral.add(new BoolSetting.Builder()
        .name("sync")
        .description("Will synchronize your slot after you finish eating.")
        .defaultValue(true)
        .visible(() -> autoSwitch.get() == AutoSwitchMode.Silent)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Will swap back to your previous slot.")
        .defaultValue(true)
        .visible(() -> autoSwitch.get() != AutoSwitchMode.None && autoSwitch.get() != AutoSwitchMode.Silent)
        .build()
    );

    private final Setting<Integer> swapDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-delay")
        .description("How many ticks before you finish consuming to swap back to the consumable.")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .sliderMax(8)
        .visible(() -> autoSwitch.get() == AutoSwitchMode.OnConsume)
        .build()
    );

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("message")
        .description("Notifies you when you stoped eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
        .name("smart")
        .description("Uses the items use delay as delay.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> smartDelay = sgGeneral.add(new IntSetting.Builder()
        .name("smart-delay")
        .description("How long to wait in ticks before sending the finish eating packet.")
        .defaultValue(-1)
        .min(-50)
        .max(50)
        .sliderMin(-10)
        .sliderMax(10)
        .visible(smart::get)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait in ticks before sending the finish eating packet.")
        .defaultValue(35)
        .min(0)
        .sliderMin(20)
        .sliderMax(50)
        .visible(() -> !smart.get())
        .build()
    );

    private final Setting<Keybind> stopEat = sgGeneral.add(new KeybindSetting.Builder()
        .name("stop-eating")
        .description("The keybind that stops you from eating.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private ArrayList<Packet<ServerPlayPacketListener>> packets;

    private boolean swap;
    private boolean eating;
    private boolean eat;
    private int timer;

    public PacketConsume() {
        super(VectorAddon.Misc, "packet-consume", "Consume stuff with packets.");
    }

    @Override
    public void onActivate() {
        packets = new ArrayList<>();

        swap = false;
        eating = false;
        eat = false;
        timer = 0;
    }

    @Override
    public void onDeactivate() {
        if (eating) stopEating();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (packets.isEmpty()) eating = false;

        double ticks = delay.get();

        if (smart.get()) {
            ItemStack stack = mc.player.getMainHandStack();
            if (isConsumable(stack)) ticks = stack.getItem().getMaxUseTime(stack) + smartDelay.get();
        }

        if (timer >= ticks && eating) {
            stopEating();
        } else if ((timer >= ticks - swapDelay.get() && autoSwitch.get() != AutoSwitchMode.None && autoSwitch.get() != AutoSwitchMode.Silent || autoSwitch.get() == AutoSwitchMode.Normal) && eating) {
            FindItemResult item = InvUtils.findInHotbar(stack -> stack.getItem() != null && (stack.getItem().isFood() || stack.getItem() instanceof PotionItem));

            if (item.found() && !item.isOffhand() && !item.isMainHand()) InvUtils.swap(item.slot(), swapBack.get());

            timer++;
        } else {
            timer++;
        }

        if (cancel.get() && stopUsing.get()) mc.player.stopUsingItem();

        if (swap && !eating) {
            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
            if (sync.get()) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));

            swap = false;
        } else if (eat) {
            if (isConsumable(Hand.MAIN_HAND)) startEating(Hand.MAIN_HAND);
            else if (isConsumable(Hand.OFF_HAND)) startEating(Hand.OFF_HAND);

            eat = false;
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (stopEat.get().isPressed()) stopEating();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (cancel.get() && mc.currentScreen == null && (isConsumable(Hand.MAIN_HAND) || isConsumable(Hand.OFF_HAND)) && event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_RIGHT) {
            event.cancel();

            eat = true;
        }
    }

    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        startEating(event.hand);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (eating) {
            if (autoSwitch.get() == AutoSwitchMode.Silent && event.packet instanceof UpdateSelectedSlotC2SPacket) event.cancel();

            if (!packets.contains(event.packet)) {
                if (event.packet instanceof PlayerInteractItemC2SPacket packet && isConsumable(mc.player.getStackInHand(packet.getHand()))) {
                    packets.remove(packet);
                    event.cancel();
                }

                if (event.packet instanceof HandSwingC2SPacket || event.packet instanceof PlayerActionC2SPacket) {
                    packets.remove(event.packet);
                    event.cancel();
                }
            }
        }
    }

    // Utils

    private boolean isConsumable(Hand hand) {
        return hand != null && isConsumable(mc.player.getStackInHand(hand));
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack == null) return false;

        Item item = stack.getItem();

        return item != null && (item.isFood() || (item instanceof PotionItem && !(item instanceof ThrowablePotionItem)));
    }

    private void startEating(Hand hand) {
        if (!eating && mc.currentScreen == null && isConsumable(hand) && shouldEat()) {
            if (message.get()) info("Started eating.");

            Packet<ServerPlayPacketListener> packet = new PlayerInteractItemC2SPacket(hand);

            packets.add(packet);
            mc.getNetworkHandler().sendPacket(packet);

            timer = 0;
            eating = true;
        }
    }

    private void stopEating() {
        if (message.get()) info("Stopped eating.");

        Packet<ServerPlayPacketListener> interact = new PlayerInteractItemC2SPacket(Hand.MAIN_HAND);
        Packet<ServerPlayPacketListener> release = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, new BlockPos(0, 0, 0), Direction.DOWN);

        packets.add(interact);
        packets.add(release);

        mc.getNetworkHandler().sendPacket(interact);
        mc.getNetworkHandler().sendPacket(release);

        eating = false;
        timer = 0;

        if (autoSwitch.get() == AutoSwitchMode.Silent) swap = true;
    }

    private boolean shouldEat() {
        if (!modules.get().isEmpty()) {
            for (Module module : modules.get()) {
                if (module.isActive()) {
                    return false;
                }
            }
        }

        return true;
    }

    // Enums

    public enum AutoSwitchMode {
        OnConsume,
        Normal,
        Silent,
        None
    }
}
