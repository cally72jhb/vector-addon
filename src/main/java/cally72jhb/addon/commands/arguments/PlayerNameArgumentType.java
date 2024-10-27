package cally72jhb.addon.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerNameArgumentType implements ArgumentType<String> {
    private static Collection<String> EXAMPLES;

    static {
        if (mc.world != null && mc.getNetworkHandler() != null) {
            EXAMPLES = mc.world.getPlayers()
                .stream()
                .map(PlayerEntity::getNameForScoreboard)
                .collect(Collectors.toList()
            );

            EXAMPLES.addAll(mc.getNetworkHandler().getPlayerList()
                .stream()
                .limit(3)
                .map(entry -> entry.getProfile().getName())
                .toList()
            );
        }
    }

    public static PlayerNameArgumentType player() {
        return new PlayerNameArgumentType();
    }

    public static String getPlayer(CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        while (reader.canRead() && isValidChar(reader.peek())) {
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Collection<String> suggestions = new ArrayList<>(
            mc.world.getPlayers()
            .stream()
            .map(PlayerEntity::getNameForScoreboard)
            .toList()
        );

        suggestions.addAll(mc.getNetworkHandler().getPlayerList()
            .stream()
            .limit(3)
            .map(entry -> entry.getProfile().getName())
            .toList()
        );

        return CommandSource.suggestMatching(suggestions, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private boolean isValidChar(char character) {
        return character != '\\' && (character >= '0' && character <= '9' || character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z' || character >= '!' && character <= '/' || character >= ':' && character <= '@' || character >= '[' && character <= '`' || character >= '{' && character <= '~');
    }
}
