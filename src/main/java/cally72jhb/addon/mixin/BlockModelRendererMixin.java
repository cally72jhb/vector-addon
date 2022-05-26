package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.notexture.NoTextures;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {
    @Shadow @Final private BlockColors colors;

    @Inject(method = "renderQuad", at = @At("HEAD"), cancellable = true)
    private void onRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer consumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float brightness0, float brightness1, float brightness2, float brightness3, int light0, int light1, int light2, int light3, int overlay, CallbackInfo info) {
        if (Modules.get() != null && Modules.get().isActive(NoTextures.class)) {
            NoTextures module = Modules.get().get(NoTextures.class);

            if (module.isAutomatic() || module.isAutomatic(state)) {
                int color = colors.getColor(state, world, pos, quad.getColorIndex());

                float r = (float) (color >> 16 & 255) / 255.0F;
                float g = (float) (color >> 8 & 255) / 255.0F;
                float b = (float) (color & 255) / 255.0F;

                consumer.quad(matrixEntry, quad, new float[] { brightness0, brightness1, brightness2, brightness3 }, r, g, b, new int[] { light0, light1, light2, light3 }, overlay, false);
            } else {
                float r = 255.0F;
                float g = 255.0F;
                float b = 255.0F;
                float a = 255.0F;

                Color color = module.getColor(state);

                if (color != null) {
                    r = (float) ((double) color.r / 255);
                    g = (float) ((double) color.g / 255);
                    b = (float) ((double) color.b / 255);
                    a = (float) ((double) color.a / 255);
                }

                quad(consumer, matrixEntry, quad, new float[] { brightness0, brightness1, brightness2, brightness3 }, r, g, b, a, new int[] { light0, light1, light2, light3 }, overlay, true);
            }

            info.cancel();
        }
    }

    private void quad(VertexConsumer consumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean useQuadColorData) {
        float[] brightness = new float[] { brightnesses[0], brightnesses[1], brightnesses[2], brightnesses[3] };
        int[] light = new int[] { lights[0], lights[1], lights[2], lights[3] };
        int[] data = quad.getVertexData();

        Vec3i tempFace = quad.getFace().getVector();
        Vec3f face = new Vec3f((float) tempFace.getX(), (float) tempFace.getY(), (float) tempFace.getZ());

        Matrix4f positionMatrix = matrixEntry.getPositionMatrix();
        face.transform(matrixEntry.getNormalMatrix());

        MemoryStack memoryStack = MemoryStack.stackPush();

        try {
            ByteBuffer byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for (int i = 0; i < data.length / 8; ++i) {
                intBuffer.clear();
                intBuffer.put(data, i * 8, 8);

                float x = byteBuffer.getFloat(0);
                float y = byteBuffer.getFloat(4);
                float z = byteBuffer.getFloat(8);

                float r;
                float g;
                float b;

                if (useQuadColorData) {
                    r = ((float) (byteBuffer.get(12) & 255) / 255.0F) * brightness[i] * red;
                    g = ((float) (byteBuffer.get(13) & 255) / 255.0F) * brightness[i] * green;
                    b = ((float) (byteBuffer.get(14) & 255) / 255.0F) * brightness[i] * blue;
                } else {
                    r = brightness[i] * red;
                    g = brightness[i] * green;
                    b = brightness[i] * blue;
                }

                float u = byteBuffer.getFloat(16);
                float v = byteBuffer.getFloat(20);

                Vector4f vertex = new Vector4f(x, y, z, 1.0F);
                vertex.transform(positionMatrix);

                consumer.vertex(vertex.getX(), vertex.getY(), vertex.getZ(), r, g, b, alpha, u, v, overlay, light[i], face.getX(), face.getY(), face.getZ());
            }
        } catch (Throwable var33) {
            if (memoryStack != null) {
                try {
                    memoryStack.close();
                } catch (Throwable var32) {
                    var33.addSuppressed(var32);
                }
            }

            throw var33;
        }

        if (memoryStack != null) {
            memoryStack.close();
        }

    }
}
