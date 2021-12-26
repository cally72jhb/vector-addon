package cally72jhb.addon.mixin.meteor;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.commands.FakePlayerCommand;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.FakePlayer;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@Mixin(FakePlayerCommand.class)
public class FakePlayerCommandMixin extends Command {
    public FakePlayerCommandMixin() {
        super("fakeplayer", "Spawn client-side fake players to test modules.");
    }

    @Inject(method = "build", at = @At("HEAD"), remap = false)
    private void onInit(LiteralArgumentBuilder<CommandSource> builder, CallbackInfo info) {
        builder.then(literal("spawn").executes(context -> {
                            if (active()) FakePlayerManager.add(mc.getSession().getProfile().getName(), 72, true);
                            return SINGLE_SUCCESS;
                        })
                        .then(argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    if (active()) FakePlayerManager.add(StringArgumentType.getString(context, "name"), 36, true);
                                    return SINGLE_SUCCESS;
                                })
                                .then(argument("health", FloatArgumentType.floatArg(0))
                                        .executes(context -> {
                                            if (active()) FakePlayerManager.add(StringArgumentType.getString(context, "name"), FloatArgumentType.getFloat(context, "health"), true);
                                            return SINGLE_SUCCESS;
                                        })
                                        .then(argument("copy-inv", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    if (active()) FakePlayerManager.add(StringArgumentType.getString(context, "name"), FloatArgumentType.getFloat(context, "health"), BoolArgumentType.getBool(context, "copy-inv"));
                                                    return SINGLE_SUCCESS;
                                                })
                                        )))
        );

        builder.then(literal("clear").executes(context -> {
            if (active()) FakePlayerManager.clear();
            return SINGLE_SUCCESS;
        }));
    }

    private boolean active() {
        if (!Modules.get().isActive(FakePlayer.class)) {
            error("The FakePlayer module must be enabled.");
            return false;
        }
        else return true;
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

    }
}
