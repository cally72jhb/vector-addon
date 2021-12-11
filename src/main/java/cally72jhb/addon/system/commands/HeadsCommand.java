package cally72jhb.addon.system.commands;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.gui.HeadScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class HeadsCommand extends Command {
    public HeadsCommand() {
        super("heads", "Display heads gui");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            VectorAddon.screen = new HeadScreen(GuiThemes.get());

            return SINGLE_SUCCESS;
        });
    }
}
