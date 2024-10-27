package cally72jhb.addon.modules.player;

import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoSteal extends Module {
    private final SettingGroup sgStealDump = settings.createGroup("Steal and Dump");
    private final SettingGroup sgAutoSteal = settings.createGroup("Auto Steal");

    // Steal & Dump

    public final Setting<List<ScreenHandlerType<?>>> stealScreens = sgStealDump.add(new ScreenHandlerListSetting.Builder()
            .name("steal-screens")
            .description("Select the screens to display buttons and auto steal.")
            .defaultValue(Arrays.asList(ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X6))
            .build()
    );

    private final Setting<Boolean> anyScreens = sgStealDump.add(new BoolSetting.Builder()
            .name("any-screens")
            .description("Select the screens to display buttons and auto steal.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> stealDrop = sgStealDump.add(new BoolSetting.Builder()
            .name("steal-drop")
            .description("Drop items to the ground instead of stealing them.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> dropBackwards = sgStealDump.add(new BoolSetting.Builder()
            .name("drop-backwards")
            .description("Drop items behind you.")
            .defaultValue(false)
            .visible(stealDrop::get)
            .build()
    );

    private final Setting<meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks.ListMode> stealFilter = sgStealDump.add(new EnumSetting.Builder<meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks.ListMode>()
            .name("steal-filter")
            .description("Steal mode.")
            .defaultValue(meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks.ListMode.None)
            .build()
    );

    private final Setting<List<Item>> stealItems = sgStealDump.add(new ItemListSetting.Builder()
            .name("steal-items")
            .description("Items to steal.")
            .build()
    );

    // Auto Steal

    private final Setting<Boolean> autoSteal = sgAutoSteal.add(new BoolSetting.Builder()
            .name("auto-steal")
            .description("Automatically removes all possible items when you open a container.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> minItemCount = sgAutoSteal.add(new IntSetting.Builder()
            .name("min-item-count")
            .description("How many items must be in one slot to steal it.")
            .defaultValue(1)
            .min(0)
            .sliderMax(64)
            .build()
    );

    private final Setting<Integer> autoStealDelay = sgAutoSteal.add(new IntSetting.Builder()
            .name("delay")
            .description("The minimum delay between stealing the next stack in milliseconds.")
            .defaultValue(20)
            .sliderMax(1000)
            .build()
    );

    private final Setting<Integer> autoStealInitDelay = sgAutoSteal.add(new IntSetting.Builder()
            .name("initial-delay")
            .description("The initial delay before stealing in milliseconds. 0 to use normal delay instead.")
            .defaultValue(50)
            .sliderMax(1000)
            .build()
    );

    private final Setting<Integer> autoStealRandomDelay = sgAutoSteal.add(new IntSetting.Builder()
            .name("random")
            .description("Randomly adds a delay of up to the specified time in milliseconds.")
            .min(0)
            .sliderMax(1000)
            .defaultValue(50)
            .build()
    );

    private boolean invOpened;

    public AutoSteal() {
        super(Categories.Misc, "auto-steal", "Automatically steals from gui screens.");
    }

    @Override
    public void onActivate() {
        invOpened = false;
    }

    @Override
    public void onDeactivate() {
        if (invOpened) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
        }
    }

    private int getSleepTime() {
        return autoStealDelay.get() + (autoStealRandomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, autoStealRandomDelay.get()) : 0);
    }

    private void moveSlots(ScreenHandler handler, int start, int end, boolean steal) {
        boolean initial = autoStealInitDelay.get() != 0;
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack() || handler.getSlot(i).getStack().getCount() < minItemCount.get()) continue;

            int sleep;
            if (initial) {
                sleep = autoStealInitDelay.get();
                initial = false;
            } else sleep = getSleepTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exit if user closes screen or exit world
            if (mc.currentScreen == null || !Utils.canUpdate()) break;

            Item item = handler.getSlot(i).getStack().getItem();
            if (steal) {
                if (stealFilter.get() == meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks.ListMode.Whitelist && !stealItems.get().contains(item))
                    continue;
                if (stealFilter.get() == meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks.ListMode.Blacklist && stealItems.get().contains(item))
                    continue;
            }

            if (steal && stealDrop.get()) {
                if (dropBackwards.get()) {
                    int iCopy = i;
                    Rotations.rotate(mc.player.getYaw() - 180, mc.player.getPitch(), () -> InvUtils.drop().slotId(iCopy));
                }
            } else InvUtils.quickSwap().slotId(i);
        }
    }

    public void steal(ScreenHandler handler) {
        MeteorExecutor.execute(() -> moveSlots(handler, 0, SlotUtils.indexToId(SlotUtils.MAIN_START), true));
    }

    public boolean canSteal(ScreenHandler handler) {
        try {
            return anyScreens.get() || stealScreens.get().contains(handler.getType());
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    @EventHandler
    private void onInventory(InventoryEvent event) {
        if (autoSteal.get()) {
            ScreenHandler handler = mc.player.currentScreenHandler;

            if (canSteal(handler)) {
                steal(handler);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (autoSteal.get()) {
            ScreenHandler handler = mc.player.currentScreenHandler;

            if (canSteal(handler)) {
                steal(handler);
            }
        }
    }

    public enum ListMode {
        Whitelist,
        Blacklist,
        None
    }
}
