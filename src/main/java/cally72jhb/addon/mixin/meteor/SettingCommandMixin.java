package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.VectorAddon;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.commands.arguments.SettingArgumentType;
import meteordevelopment.meteorclient.systems.commands.arguments.SettingValueArgumentType;
import meteordevelopment.meteorclient.systems.commands.commands.SettingCommand;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@Mixin(SettingCommand.class)
public class SettingCommandMixin extends Command {
    public SettingCommandMixin() {
        super("setting", "Allows you to view and change module settings.");
    }

    @Inject(method = "build", at = @At("HEAD"), remap = false)
    private void onInit(LiteralArgumentBuilder<CommandSource> builder, CallbackInfo info) {
        builder.then(argument("module", ModuleArgumentType.module()).executes(context -> {
            Module module = ModuleArgumentType.getModule(context, "module");

            VectorAddon.screen = new ModuleScreen(GuiThemes.get(), module);

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("module", ModuleArgumentType.module())
                .then(argument("setting", SettingArgumentType.setting()).executes(context -> {
                    // Get setting value
                    Setting<?> setting = SettingArgumentType.getSetting(context);

                    ModuleArgumentType.getModule(context, "module").info("Setting (highlight)%s(default) is (highlight)%s(default).", setting.title, setting.get());

                    return SINGLE_SUCCESS;
                }).then(argument("value", SettingValueArgumentType.value()).executes(context -> {
                    // Set setting value
                    Setting<?> setting = SettingArgumentType.getSetting(context);
                    String value = context.getArgument("value", String.class);

                    if (setting.parse(value)) ModuleArgumentType.getModule(context, "module").info("Setting (highlight)%s(default) changed to (highlight)%s(default).", setting.title, value);

                    return SINGLE_SUCCESS;
                })))
        );
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

    }
}
