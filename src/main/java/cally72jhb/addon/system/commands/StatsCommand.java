package cally72jhb.addon.system.commands;

import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.Stats;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;

import java.util.UUID;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class StatsCommand extends Command {
    private Stats scores;

    public StatsCommand() {
        super("stats", "Shows your current stats for yourself or others.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (scores == null) scores = VectorUtils.scores;
            if (scores != null) {
                info("Stats:");
                info("Pops: " + scores.allPops);
                info("Kills: " + scores.allKills);
                info("Deaths: " + scores.deaths);
            } else {
                info("No stats for you saved.");
            }

            return SINGLE_SUCCESS;
        });

        builder.then(argument("player", PlayerArgumentType.player()).executes(context -> {
            if (PlayerArgumentType.getPlayer(context) == null) return 0;

            String name = PlayerArgumentType.getPlayer(context).getEntityName();
            UUID uuid = PlayerArgumentType.getPlayer(context).getUuid();

            if (scores == null) scores = VectorUtils.scores;
            if (scores != null) {
                info("Stats for " + name + ":");
                if (!scores.pops.isEmpty() && scores.pops.containsKey(uuid)) info("Pops: " + scores.pops.get(uuid));
                else info("Pops: 0");
                if (!scores.kills.isEmpty() && scores.kills.containsKey(uuid)) info("Kills: " + scores.kills.get(uuid));
                else info("Kills: 0");
            } else {
                info("No stats saved for player " + name + ".");
            }

            return SINGLE_SUCCESS;
        }));
    }
}
