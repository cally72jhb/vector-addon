package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.notexture.NoTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private ObjectArrayList<WorldRenderer.ChunkInfo> chunkInfos;
    @Shadow @Nullable private ChunkBuilder chunkBuilder;

    @Shadow private double lastTranslucentSortX;
    @Shadow private double lastTranslucentSortY;
    @Shadow private double lastTranslucentSortZ;

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    private void onRenderLayer(RenderLayer layer, MatrixStack matrices, double x, double y, double z, Matrix4f positionMatrix, CallbackInfo info) {
        if (Modules.get() != null && Modules.get().isActive(NoTextures.class)) {
            RenderSystem.assertOnRenderThread();
            layer.startDrawing();

            if (layer == RenderLayer.getTranslucent()) {
                client.getProfiler().push("translucent_sort");

                double dx = x - lastTranslucentSortX;
                double dy = y - lastTranslucentSortY;
                double dz = z - lastTranslucentSortZ;

                if (dx * dx + dy * dy + dz * dz > 1.0D) {
                    lastTranslucentSortX = x;
                    lastTranslucentSortY = y;
                    lastTranslucentSortZ = z;

                    int i = 0;

                    for (WorldRenderer.ChunkInfo chunkInfo : chunkInfos) {
                        if (i < 15 && chunkInfo.chunk.scheduleSort(layer, chunkBuilder)) {
                            ++i;
                        }
                    }
                }

                client.getProfiler().pop();
            }

            client.getProfiler().push("filterempty");
            client.getProfiler().swap(() -> "render_" + layer);

            boolean translucent = layer != RenderLayer.getTranslucent();

            ObjectListIterator<WorldRenderer.ChunkInfo> infos = chunkInfos.listIterator(translucent ? 0 : chunkInfos.size());

            VertexFormat format = layer.getVertexFormat();
            Shader shader = RenderSystem.getShader();
            BufferRenderer.unbindAll();

            if (shader.modelViewMat != null) {
                shader.modelViewMat.set(matrices.peek().getPositionMatrix());
            }

            if (shader.projectionMat != null) {
                shader.projectionMat.set(positionMatrix);
            }

            if (shader.colorModulator != null) {
                shader.colorModulator.set(RenderSystem.getShaderColor());
            }

            if (shader.fogStart != null) {
                shader.fogStart.set(RenderSystem.getShaderFogStart());
            }

            if (shader.fogEnd != null) {
                shader.fogEnd.set(RenderSystem.getShaderFogEnd());
            }

            if (shader.fogColor != null) {
                shader.fogColor.set(RenderSystem.getShaderFogColor());
            }

            if (shader.fogShape != null) {
                shader.fogShape.set(RenderSystem.getShaderFogShape().getId());
            }

            if (shader.gameTime != null) {
                shader.gameTime.set(RenderSystem.getShaderGameTime());
            }

            RenderSystem.setupShaderLights(shader);
            shader.bind();

            GlUniform uniform = shader.chunkOffset;
            boolean rendered = false;

            while (true) {
                if (translucent) {
                    if (!infos.hasNext()) {
                        break;
                    }
                } else if (!infos.hasPrevious()) {
                    break;
                }

                ChunkBuilder.BuiltChunk chunk = (translucent ? infos.next() : infos.previous()).chunk;

                if (!chunk.getData().isEmpty(layer)) {
                    VertexBuffer buffer = chunk.getBuffer(layer);
                    BlockPos pos = chunk.getOrigin();

                    if (uniform != null) {
                        uniform.set((float) ((double) pos.getX() - x), (float) ((double) pos.getY() - y), (float) ((double) pos.getZ() - z));
                        uniform.upload();
                    }

                    buffer.drawVertices();
                    rendered = true;
                }
            }

            if (uniform != null) {
                uniform.set(Vec3f.ZERO);
            }

            shader.unbind();

            if (rendered) {
                format.endDrawing();
            }

            VertexBuffer.unbind();
            VertexBuffer.unbindVertexArray();

            client.getProfiler().pop();
            layer.endDrawing();

            info.cancel();
        }
    }
}
