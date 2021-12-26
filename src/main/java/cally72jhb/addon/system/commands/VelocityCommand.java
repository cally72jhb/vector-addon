package cally72jhb.addon.system.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class VelocityCommand extends Command {
    public VelocityCommand() {
        super("velocity", "Sets the players velocity.", "vel");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("y", DoubleArgumentType.doubleArg())).executes(context -> {
            mc.player.setVelocity(mc.player.getVelocity().x, context.getArgument("y", Double.class), mc.player.getVelocity().z);

            return SINGLE_SUCCESS;
        });

        builder.then(argument("x", DoubleArgumentType.doubleArg()).then(argument("z", DoubleArgumentType.doubleArg()))).executes(context -> {
            double x = context.getArgument("x", Double.class);
            double z = context.getArgument("z", Double.class);

            mc.player.setVelocity(x, mc.player.getVelocity().y, z);

            return SINGLE_SUCCESS;
        });

        builder.then(argument("x", DoubleArgumentType.doubleArg()).then(argument("y", DoubleArgumentType.doubleArg()).then(argument("z", DoubleArgumentType.doubleArg())))).executes(context -> {
            double x = context.getArgument("x", Double.class);
            double y = context.getArgument("y", Double.class);
            double z = context.getArgument("z", Double.class);

            mc.player.setVelocity(x, y, z);

            return SINGLE_SUCCESS;
        });
    }
}
