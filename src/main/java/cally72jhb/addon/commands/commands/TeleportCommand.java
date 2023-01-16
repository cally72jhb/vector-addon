package cally72jhb.addon.commands.commands;

import cally72jhb.addon.commands.arguments.PositionArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("teleport-advanced", "Allows to teleport small distances.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("pos", PositionArgumentType.pos()).executes(context -> {
            Vec3d pos = PositionArgumentType.getPos(context, "pos");

            if (mc.player.hasVehicle()) {
                Entity vehicle = mc.player.getVehicle();

                vehicle.setPosition(pos.getX(), pos.getY(), pos.getZ());
                mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(vehicle));
            }

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.getX(), pos.getY(), pos.getZ(), mc.player.isOnGround()));
            mc.player.updatePosition(pos.getX(), pos.getY(), pos.getZ());

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("pos", PositionArgumentType.pos()).then(argument("yaw", FloatArgumentType.floatArg()).then(argument("pitch", FloatArgumentType.floatArg()).executes(context -> {
            Vec3d pos = PositionArgumentType.getPos(context, "pos");

            float yaw = FloatArgumentType.getFloat(context, "yaw");
            float pitch = FloatArgumentType.getFloat(context, "pitch");

            if (mc.player.hasVehicle()) {
                Entity vehicle = mc.player.getVehicle();

                vehicle.setPosition(pos.getX(), pos.getY(), pos.getZ());
                mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(vehicle));
            }

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, mc.player.isOnGround()));
            mc.player.updatePositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);

            return SINGLE_SUCCESS;
        }))));

        // Bypassing

        builder.then(argument("pos", PositionArgumentType.pos()).then(argument("ticks", IntegerArgumentType.integer(0)).executes(context -> {
            Vec3d pos = PositionArgumentType.getPos(context, "pos");
            int ticks = IntegerArgumentType.getInteger(context, "ticks");

            for (int i = 0; i < (ticks <= 0 ? 1 : ticks); i++) {
                if (mc.player.hasVehicle()) {
                    Entity vehicle = mc.player.getVehicle();

                    vehicle.setPosition(pos.getX(), pos.getY(), pos.getZ());
                    mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(vehicle));
                }

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.getX(), pos.getY(), pos.getZ(), mc.player.isOnGround()));
                mc.player.updatePosition(pos.getX(), pos.getY(), pos.getZ());
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(argument("pos", PositionArgumentType.pos()).then(argument("yaw", FloatArgumentType.floatArg()).then(argument("pitch", FloatArgumentType.floatArg()).then(argument("ticks", IntegerArgumentType.integer(0)).executes(context -> {
            Vec3d pos = PositionArgumentType.getPos(context, "pos");
            int ticks = IntegerArgumentType.getInteger(context, "ticks");

            float yaw = FloatArgumentType.getFloat(context, "yaw");
            float pitch = FloatArgumentType.getFloat(context, "pitch");

            for (int i = 0; i < (ticks <= 0 ? 1 : ticks); i++) {
                if (mc.player.hasVehicle()) {
                    Entity vehicle = mc.player.getVehicle();

                    vehicle.setPosition(pos.getX(), pos.getY(), pos.getZ());
                    mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(vehicle));
                }

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, mc.player.isOnGround()));
                mc.player.updatePositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
            }

            return SINGLE_SUCCESS;
        })))));
    }
}
