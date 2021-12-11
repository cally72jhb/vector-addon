package cally72jhb.addon.system.commands;

import cally72jhb.addon.system.players.Players;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TargetCommand extends Command {
    public TargetCommand() {
        super("target", "Target certain players.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.player()).executes(context -> {
            Players.get().target(PlayerArgumentType.getPlayer(context));

            return SINGLE_SUCCESS;
        }));
    }
}
