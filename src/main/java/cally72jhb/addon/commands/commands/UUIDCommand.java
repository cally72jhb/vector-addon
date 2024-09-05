package cally72jhb.addon.commands.commands;

import cally72jhb.addon.commands.arguments.PlayerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class UUIDCommand extends Command {
    public UUIDCommand() {
        super("uuid", "Returns a players uuid.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("Your UUID is " + mc.player.getUuid().toString());

            return SINGLE_SUCCESS;
        });

        builder.then(argument("player", PlayerArgumentType.player()).executes(context -> {
            PlayerEntity player = PlayerArgumentType.getPlayer(context, "player");

            if (player != null) {
                info(player.getDisplayName() + "'s UUID is " + player.getUuid().toString());
            }

            return SINGLE_SUCCESS;
        }));
    }
}
