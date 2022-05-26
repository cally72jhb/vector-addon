package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.BobView;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(MatrixStack matrices, float delta, CallbackInfo info) {
        if (Modules.get() != null && Modules.get().isActive(BobView.class)) {
            BobView module = Modules.get().get(BobView.class);

            if (!module.shouldDisable() && client.getCameraEntity() instanceof PlayerEntity player) {
                float normal = -(player.horizontalSpeed * module.getSpeed() + (player.horizontalSpeed * module.getSpeed() - player.prevHorizontalSpeed * module.getSpeed()) * delta);
                float stride = MathHelper.lerp(delta, player.prevStrideDistance * module.getSpeed(), player.strideDistance * module.getSpeed());

                matrices.translate((MathHelper.sin(normal * 3.1415927F) * stride * module.getHorizontal()), (-Math.abs(MathHelper.cos(normal * 3.1415927F) * stride)), 0.0F);

                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(MathHelper.sin(normal * 3.1415927F) * stride * module.getRotate()));
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(Math.abs(MathHelper.cos(normal * 3.1415927F - module.getShake()) * stride) * module.getVertical()));
            }

            info.cancel();
        }
    }
}
