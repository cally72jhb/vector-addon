package cally72jhb.addon.system.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class UUIDCommand extends Command {
    public UUIDCommand() {
        super("uuid", "Tells you a players uuid.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.player()).executes(context -> {
            PlayerEntity player = PlayerArgumentType.getPlayer(context);
            info(player.getGameProfile().getName() + ": " + player.getUuid().toString());

            return SINGLE_SUCCESS;
        }));
    }
}
