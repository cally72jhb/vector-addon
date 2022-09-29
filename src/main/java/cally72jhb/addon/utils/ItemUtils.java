package cally72jhb.addon.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemUtils {
    public static boolean insertCreativeStack(ItemStack stack) {
        ItemStack clone = stack.copy();

        int occupied = getOccupiedSlotWithRoomForStack(stack);
        int slot = occupied == -1 ? getEmptySlot() : occupied;

        if (slot < 0 || !mc.player.getInventory().insertStack(stack)) {
            return false;
        } else {
            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, clone));

            return true;
        }
    }

    // Other

    public static int getEmptySlot() {
        for (int i = 0; i <= 8; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i + 36;
            }
        }

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().main.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public static int getOccupiedSlotWithRoomForStack(ItemStack stack) {
        if (canStackAddMore(mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot), stack)) {
            return mc.player.getInventory().selectedSlot;
        } else if (canStackAddMore(mc.player.getInventory().getStack(40), stack)) {
            return 40;
        } else {
            for (int i = 0; i <= 8; i++) {
                if (canStackAddMore(mc.player.getInventory().getStack(i), stack)) {
                    return i + 36;
                }
            }

            for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
                if (canStackAddMore(mc.player.getInventory().main.get(i), stack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public static boolean canStackAddMore(ItemStack existing, ItemStack stack) {
        return !existing.isEmpty() && ItemStack.canCombine(existing, stack) && existing.isStackable() && existing.getCount() < existing.getMaxCount() && existing.getCount() < 64;
    }
}
