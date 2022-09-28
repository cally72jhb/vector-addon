package cally72jhb.addon.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Arrays;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ShaderCommand extends Command {
    private ShaderEffect shader;

    public ShaderCommand() {
        super("shader", "Changes your client-side shader.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (String string : Arrays.stream(Shader.values()).map(Enum::name).toList()) {
            builder.then(literal(string).executes(context -> {
                String name = string;

                if (string.equals("vibrant")) {
                    name = "color_convolve";
                } else if (string.equals("scanline")) {
                    name = "scan_pincushion";
                }

                Identifier shaderID = new Identifier(String.format("shaders/post/%s.json", name));

                try {
                    this.shader = new ShaderEffect(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), shaderID);

                    info("Successfully changed shader effect");
                } catch (IOException exception) {
                    this.shader = null;

                    info("Failed to create shader effect");
                }

                return SINGLE_SUCCESS;
            }));
        }

        builder.then(literal("none").executes(context -> {
            this.shader = null;

            info("Successfully reset shader effect");

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("reset").executes(context -> {
            this.shader = null;

            info("Successfully reset shader effect");

            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            this.shader = null;

            info("Successfully reset shader effect");

            return SINGLE_SUCCESS;
        });
    }

    // Utils

    public ShaderEffect getShader() {
        return this.shader;
    }

    public enum Shader {
        antialias,
        art,
        bits,
        blobs,
        blobs2,
        blur,
        bumpy,
        creeper,
        deconverge,
        desaturate,
        fxaa,
        flip,
        green,
        invert,
        ntsc,
        notch,
        outline,
        pencil,
        phosphor,
        scanline,
        sobel,
        spider,
        vibrant,
        wobble
    }
}
