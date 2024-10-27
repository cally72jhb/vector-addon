package cally72jhb.addon.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DesyncCommand extends Command {
    public DesyncCommand() {
        super("desync", "Desyncs yourself or the vehicle you're riding from the server.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("player")).executes(context -> {
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(0));
            info("Successfully desynced your player entity");

            return SINGLE_SUCCESS;
        });
    }
}
