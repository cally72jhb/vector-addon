package cally72jhb.addon.commands.commands;

import cally72jhb.addon.commands.arguments.PlayerNameArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.systems.accounts.UuidToProfileResponse;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PlayerHeadCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    private final static SimpleCommandExceptionType NO_SPACE = new SimpleCommandExceptionType(Text.literal("No space in hotbar."));

    public PlayerHeadCommand() {
        super("player-head", "Gives you an player-head in creative.", "head", "skull");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerNameArgumentType.player()).executes(context -> {
            giveHead(PlayerNameArgumentType.getPlayer(context, "player"), 1);

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerNameArgumentType.player()).then(argument("amount", IntegerArgumentType.integer(0, 64)).executes(context -> {
            giveHead(PlayerNameArgumentType.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"));

            return SINGLE_SUCCESS;
        })));

        builder.executes(context -> {
            giveHead(mc.player.getGameProfile().getName(), 1);

            return SINGLE_SUCCESS;
        });
    }

    // Utils

    private void giveHead(String player, int amount) throws CommandSyntaxException {
        if (!mc.player.getAbilities().creativeMode) {
            throw NOT_IN_CREATIVE.create();
        } else if (player != null) {
            MeteorExecutor.execute(() -> {
                try {
                    ItemStack stack = Items.PLAYER_HEAD.getDefaultStack();

                    Random random = new Random(player.hashCode());
                    String id = "[I;" + random.nextInt() + "," + random.nextInt() + "," + random.nextInt() + "," + random.nextInt() + "]";

                    APIResponse res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + player).sendJson(APIResponse.class);
                    if (res != null && res.name != null && res.id != null) {
                        UuidToProfileResponse res2 = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + res.id).sendJson(UuidToProfileResponse.class);
                        if (res2 != null) {
                            if (amount > 1) stack.setCount(MathHelper.clamp(amount, 1, 64));

                            stack.setNbt(StringNbtReader.parse(
                                    "{SkullOwner:{Id:" + id + ",Properties:{textures:[{Value:\""
                                            + res2.properties[0].value
                                            + "\"}]}}}")
                            );

                            FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
                            if (!fir.found()) throw NO_SPACE.create();

                            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + fir.slot(), stack));
                        }
                    }

                } catch (CommandSyntaxException exception) {
                    exception.printStackTrace();

                    error("Not enough space in your inventory.");
                }
            });
        }
    }

    private static class APIResponse {
        String name, id;
    }
}
