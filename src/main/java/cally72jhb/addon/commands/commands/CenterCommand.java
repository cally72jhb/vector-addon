package cally72jhb.addon.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CenterCommand extends Command {
    public CenterCommand() {
        super("center", "Centers you on the block you are currently standing on.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            double x = mc.player.getBlockX() + 0.5D;
            double z = mc.player.getBlockZ() + 0.5D;

            mc.player.setVelocity(0.0D, 0.0D, 0.0D);
            mc.player.setPosition(x, mc.player.getY(), z);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, mc.player.getY(), z, mc.player.isOnGround()));

            return SINGLE_SUCCESS;
        });
    }
}
