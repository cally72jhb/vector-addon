package cally72jhb.addon.commands.commands;

import cally72jhb.addon.utils.Utils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.slot.SlotActionType;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TrashCommand extends Command {
    public TrashCommand() {
        super("trash", "Destroys the item you are holding in your hand.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Utils.clickInvSlot(36 + mc.player.getInventory().selectedSlot, 50, SlotActionType.SWAP);

            return SINGLE_SUCCESS;
        });
    }
}
