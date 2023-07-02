package cally72jhb.addon.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DesyncCommand extends Command {
    private Entity entity = null;

    public DesyncCommand() {
        super("desync", "Desyncs yourself or the vehicle you're riding from the server.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (this.entity == null) {
                if (mc.player.hasVehicle()) {
                    this.entity = mc.player.getVehicle();

                    mc.player.dismountVehicle();
                    mc.world.removeEntity(this.entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);

                    info("Successfully desynced your vehicle");
                } else {
                    error("You are not riding an entity.");
                }
            } else {
                if (!mc.player.hasVehicle()) {
                    mc.world.addEntity(this.entity.getId(), this.entity);
                    mc.player.startRiding(this.entity, true);

                    this.entity = null;

                    info("Successfully resynced your vehicle");
                } else {
                    error("You are not riding another entity.");
                }
            }

            return SINGLE_SUCCESS;
        });

        builder.then(literal("entity")).executes(context -> {
            if (this.entity == null) {
                if (mc.player.hasVehicle()) {
                    this.entity = mc.player.getVehicle();

                    mc.player.dismountVehicle();
                    mc.world.removeEntity(this.entity.getId(), Entity.RemovalReason.UNLOADED_TO_CHUNK);

                    info("Successfully desynced your vehicle");
                } else {
                    error("You are not riding an entity.");
                }
            } else {
                if (!mc.player.hasVehicle()) {
                    mc.world.addEntity(this.entity.getId(), this.entity);
                    mc.player.startRiding(this.entity, true);

                    this.entity = null;

                    info("Successfully resynced your vehicle");
                } else {
                    error("You are not riding another entity.");
                }
            }

            return SINGLE_SUCCESS;
        });

        builder.then(literal("player")).executes(context -> {
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(0));
            info("Successfully desynced your player entity");

            return SINGLE_SUCCESS;
        });
    }
}
