package cally72jhb.addon.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PositionArgumentType implements ArgumentType<Vec3d> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");

    private static final DynamicCommandExceptionType MISSING_COORDINATE = new DynamicCommandExceptionType(object -> Text.literal("Command is incomplete missing 1 or more coordinates."));
    private static final DynamicCommandExceptionType INCOMPLETE_EXCEPTION = new DynamicCommandExceptionType(object -> Text.literal("Incomplete position argument."));
    private static final DynamicCommandExceptionType MIXED_COORDINATE_EXCEPTION = new DynamicCommandExceptionType(object -> Text.literal("Can't mix two or more position argument types."));

    private final boolean centerIntegers;

    public PositionArgumentType(boolean centerIntegers) {
        this.centerIntegers = centerIntegers;
    }

    public static PositionArgumentType pos() {
        return new PositionArgumentType(true);
    }

    public static PositionArgumentType pos(boolean centerIntegers) {
        return new PositionArgumentType(centerIntegers);
    }

    public static Vec3d getPos(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Vec3d.class);
    }

    public Vec3d parse(StringReader stringReader) throws CommandSyntaxException {
        return stringReader.canRead() && stringReader.peek() == '^' ? parseLookingPos(stringReader) : parsePos(stringReader, this.centerIntegers);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof CommandSource)) {
            return Suggestions.empty();
        } else {
            String string = builder.getRemaining();

            if (!string.isEmpty() && string.charAt(0) == '^') {
                Set<CommandSource.RelativePosition> collection = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
                return CommandSource.suggestPositions(string, collection, builder, CommandManager.getCommandValidator(this::parse));
            } else {
                Collection<CommandSource.RelativePosition> collection = ((CommandSource) context.getSource()).getPositionSuggestions();
                return CommandSource.suggestPositions(string, collection, builder, CommandManager.getCommandValidator(this::parse));
            }
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    // Utils

    // Looking Pos

    private Vec3d parseLookingPos(StringReader reader) throws CommandSyntaxException {
        String argument = reader.getString();
        int cursor = reader.getCursor();

        double x = readCoordinate(reader, cursor);

        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();

            double y = readCoordinate(reader, cursor);

            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();

                double z = readCoordinate(reader, cursor);

                return toAbsolutePos(new Vec3d(x, y, z));
            } else {
                reader.setCursor(cursor);

                throw INCOMPLETE_EXCEPTION.create(argument);
            }
        } else {
            reader.setCursor(cursor);

            throw INCOMPLETE_EXCEPTION.create(argument);
        }
    }

    public Vec3d toAbsolutePos(Vec3d original) {
        Vec2f rotation = mc.player.getRotationClient();
        Vec3d position = mc.player.getPos();

        float cos1 = MathHelper.cos((rotation.y + 90.0F) * 0.017453292F);
        float sin1 = MathHelper.sin((rotation.y + 90.0F) * 0.017453292F);
        float cos2 = MathHelper.cos(-rotation.x * 0.017453292F);
        float sin2 = MathHelper.sin(-rotation.x * 0.017453292F);
        float cos3 = MathHelper.cos((-rotation.x + 90.0F) * 0.017453292F);
        float sin3 = MathHelper.sin((-rotation.x + 90.0F) * 0.017453292F);

        Vec3d vector1 = new Vec3d(cos1 * cos2, sin2, sin1 * cos2);
        Vec3d vector2 = new Vec3d(cos1 * cos3, sin3, sin1 * cos3);
        Vec3d product = vector1.crossProduct(vector2).multiply(-1.0D);

        double x = vector1.x * original.getZ() + vector2.x * original.getY() + product.x * original.getX();
        double y = vector1.y * original.getZ() + vector2.y * original.getY() + product.y * original.getX();
        double z = vector1.z * original.getZ() + vector2.z * original.getY() + product.z * original.getX();

        return new Vec3d(position.x + x, position.y + y, position.z + z);
    }

    private double readCoordinate(StringReader reader, int start) throws CommandSyntaxException {
        String argument = reader.getString();

        if (!reader.canRead()) {
            throw MISSING_COORDINATE.create(argument);
        } else if (reader.peek() != '^') {
            reader.setCursor(start);

            throw MIXED_COORDINATE_EXCEPTION.create(argument);
        } else {
            reader.skip();

            return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
        }
    }

    // Default Pos

    private Vec3d parsePos(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        String argument = reader.getString();
        int cursor = reader.getCursor();

        Pair<Boolean, Double> x = parseCoordinate(reader, centerIntegers);

        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();

            Pair<Boolean, Double> y = parseCoordinate(reader, false);

            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();

                Pair<Boolean, Double> z = parseCoordinate(reader, centerIntegers);

                return new Vec3d(
                        x.getLeft() ? x.getRight() + mc.player.getX() : x.getRight(),
                        y.getLeft() ? y.getRight() + mc.player.getY() : y.getRight(),
                        z.getLeft() ? z.getRight() + mc.player.getZ() : z.getRight()
                );
            } else {
                reader.setCursor(cursor);

                throw INCOMPLETE_EXCEPTION.create(argument);
            }
        } else {
            reader.setCursor(cursor);

            throw INCOMPLETE_EXCEPTION.create(argument);
        }
    }

    private Pair<Boolean, Double> parseCoordinate(StringReader reader, boolean center) throws CommandSyntaxException {
        String argument = reader.getString();

        if (reader.canRead() && reader.peek() == '^') {
            throw MIXED_COORDINATE_EXCEPTION.create(argument);
        } else if (!reader.canRead()) {
            throw MISSING_COORDINATE.create(argument);
        } else {
            boolean relative = isRelative(reader);
            int cursor = reader.getCursor();
            double value = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;

            String string = reader.getString().substring(cursor, reader.getCursor());

            if (relative && string.isEmpty()) {
                return new Pair<>(true, 0.0);
            } else {
                if (!string.contains(".") && !relative && center) {
                    value += 0.5;
                }

                return new Pair<>(relative, value);
            }
        }
    }

    private boolean isRelative(StringReader reader) {
        boolean relative;

        if (reader.peek() == '~') {
            relative = true;
            reader.skip();
        } else {
            relative = false;
        }

        return relative;
    }
}
