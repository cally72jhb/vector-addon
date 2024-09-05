package cally72jhb.addon.commands.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class ItemCommand extends Command {
    public ItemCommand() {
        super("item", "Allows you to put any item in any slot in your inventory.", "move");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("slot", IntegerArgumentType.integer()).executes(context -> {
            clickSlot(context.getArgument("slot", Integer.class));

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("from", IntegerArgumentType.integer()).then(argument("to", IntegerArgumentType.integer()).executes(context -> {
            clickSlot(0, context.getArgument("from", Integer.class), context.getArgument("to", Integer.class), SlotActionType.SWAP);

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("bypass").then(argument("from", IntegerArgumentType.integer()).then(argument("to", IntegerArgumentType.integer()).executes(context -> {
            clickSlot(mc.player != null && mc.player.currentScreenHandler != null ? mc.player.currentScreenHandler.syncId : 0, context.getArgument("from", Integer.class), context.getArgument("to", Integer.class), SlotActionType.SWAP);

            return SINGLE_SUCCESS;
        }))));

        builder.then(literal("head").executes(context -> {
            clickSlot(39);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("chest").executes(context -> {
            clickSlot(38);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("leggings").executes(context -> {
            clickSlot(37);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("boots").executes(context -> {
            clickSlot(36);

            return SINGLE_SUCCESS;
        }));
    }

    // Utils

    private void clickSlot(int slot) {
        clickSlot(0, 36 + mc.player.getInventory().selectedSlot, slot, SlotActionType.SWAP);
    }

    private void clickSlot(int syncId, int id, int button, SlotActionType action) {
        if (id > 0 && button > 0) {
            ScreenHandler handler = mc.player.currentScreenHandler;

            DefaultedList<Slot> slots = handler.slots;
            int i = slots.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slot : slots) list.add(slot.getStack().copy());

            try {
                handler.onSlotClick(id, button, action, mc.player);
            } catch (IndexOutOfBoundsException exception) {
                exception.printStackTrace();
            }

            Int2ObjectMap<ItemStack> stacks = new Int2ObjectOpenHashMap<>();

            for (int slot = 0; slot < i; slot++) {
                ItemStack stack1 = list.get(slot);
                ItemStack stack2 = slots.get(slot).getStack();

                if (!ItemStack.areEqual(stack1, stack2)) stacks.put(slot, stack2.copy());
            }

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, handler.getRevision(), id, button, action, handler.getCursorStack().copy(), stacks));
        }
    }
}
