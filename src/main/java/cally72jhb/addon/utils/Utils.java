package cally72jhb.addon.utils;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Utils {
    public static boolean isPlayerMoving() {
        return mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F;
    }

    public static double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0F) {
            if (side > 0.0F) {
                yaw += ((forward > 0.0F) ? -45 : 45);
            } else if (side < 0.0F) {
                yaw += ((forward > 0.0F) ? 45 : -45);
            }

            side = 0.0F;

            if (forward > 0.0F) {
                forward = 1.0F;
            } else if (forward < 0.0F) {
                forward = -1.0F;
            }
        }

        double sin = Math.sin(Math.toRadians(yaw + 90.0F));
        double cos = Math.cos(Math.toRadians(yaw + 90.0F));
        double dx = forward * speed * cos + side * speed * sin;
        double dz = forward * speed * sin - side * speed * cos;

        return new double[] { dx, dz };
    }

    // Inventory

    public static void clickInvSlot(int slot, int button, SlotActionType action) {
        clickSlot(0, 129, slot, button, action, mc.player.currentScreenHandler);
    }

    public static void clickSlot(int syncId, int slot, int button, SlotActionType action) {
        clickSlot(syncId, mc.player.currentScreenHandler.getRevision(), slot, button, action, mc.player.currentScreenHandler);
    }

    public static void clickSlot(int syncId, int revision, int slot, int button, SlotActionType action) {
        clickSlot(syncId, revision, slot, button, action, mc.player.currentScreenHandler);
    }

    public static void clickSlot(int syncId, int slot, int button, SlotActionType action, ScreenHandler handler) {
        clickSlot(syncId, handler.getRevision(), slot, button, action, handler.slots, handler.getCursorStack());
    }

    public static void clickSlot(int syncId, int revision, int slot, int button, SlotActionType action, ScreenHandler handler) {
        clickSlot(syncId, revision, slot, button, action, handler.slots, handler.getCursorStack());
    }

    public static void clickSlot(int syncId, int revision, int id, int button, SlotActionType action, DefaultedList<Slot> slots, ItemStack cursorStack) {
        Int2ObjectOpenHashMap<ItemStack> stacks = new Int2ObjectOpenHashMap<>();
        List<ItemStack> list = Lists.newArrayListWithCapacity(slots.size());

        for (Slot slot : slots) list.add(slot.getStack().copy());

        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack1 = list.get(slot);
            ItemStack stack2 = slots.get(slot).getStack();

            if (!ItemStack.areEqual(stack1, stack2)) stacks.put(slot, stack2.copy());
        }

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, revision, id, button, action, cursorStack.copy(), stacks));
    }

    // Vectors

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(Vec3d vec1, Vec3d vec2) {
        double dX = vec2.x - vec1.x;
        double dY = vec2.y - vec1.y;
        double dZ = vec2.z - vec1.z;

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(BlockPos pos1, BlockPos pos2) {
        double dX = pos2.getX() - pos1.getX();
        double dY = pos2.getY() - pos1.getY();
        double dZ = pos2.getZ() - pos1.getZ();

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distanceXZ(Vec3d pos1, Vec3d pos2) {
        double dX = pos1.getX() - pos2.getX();
        double dZ = pos1.getZ() - pos2.getZ();

        return MathHelper.sqrt((float) (dX * dX + dZ * dZ));
    }

    public static double distanceXZ(double x1, double x2, double z1, double z2) {
        double dX = x1 - x2;
        double dZ = z1 - z2;

        return MathHelper.sqrt((float) (dX * dX + dZ * dZ));
    }

    public static double distanceY(Vec3d pos1, Vec3d pos2) {
        return Math.abs(pos2.y - pos1.y);
    }

    public static double distanceY(double y1, double y2) {
        return Math.abs(y1 - y2);
    }

    // World

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }
}
