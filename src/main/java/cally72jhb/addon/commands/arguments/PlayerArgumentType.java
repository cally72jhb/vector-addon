package cally72jhb.addon.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerArgumentType implements ArgumentType<PlayerEntity> {
    private static Collection<String> EXAMPLES;

    static {
        if (mc.world != null) {
            EXAMPLES = mc.world.getPlayers()
                    .stream()
                    .map(PlayerEntity::getEntityName)
                    .collect(Collectors.toList()
            );
        }
    }

    private static final DynamicCommandExceptionType NO_SUCH_PLAYER = new DynamicCommandExceptionType(object -> Text.literal("Player with name " + object + " doesn't exist."));

    public static PlayerArgumentType player() {
        return new PlayerArgumentType();
    }

    public static PlayerEntity getPlayer(CommandContext<?> context, final String name) {
        return context.getArgument(name, PlayerEntity.class);
    }

    @Override
    public PlayerEntity parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        while (reader.canRead() && isValidChar(reader.peek())) {
            reader.skip();
        }

        String argument = reader.getString().substring(start, reader.getCursor());

        PlayerEntity player = null;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity.getEntityName().equalsIgnoreCase(argument)) {
                player = entity;
                break;
            }
        }

        if (player == null) throw NO_SUCH_PLAYER.create(argument);

        return player;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(mc.world.getPlayers().stream().map(PlayerEntity::getEntityName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private boolean isValidChar(char character) {
        return character != '\\' && (character >= '0' && character <= '9' || character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
                || character >= '!' && character <= '/' || character >= ':' && character <= '@' || character >= '[' && character <= '`' || character >= '{' && character <= '~');
    }
}
