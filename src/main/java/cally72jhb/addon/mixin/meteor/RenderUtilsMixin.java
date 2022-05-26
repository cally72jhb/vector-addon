package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.system.modules.render.BobView;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderUtils.class)
public class RenderUtilsMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private static void onBobView(MatrixStack matrices, CallbackInfo info) {
        if (Modules.get() != null && Modules.get().isActive(BobView.class)) {
            BobView module = Modules.get().get(BobView.class);

            if (!module.shouldDisable() && MinecraftClient.getInstance().getCameraEntity() instanceof PlayerEntity player) {
                float delta = MinecraftClient.getInstance().getTickDelta();
                float normal = -(player.horizontalSpeed * module.getSpeed() + (player.horizontalSpeed * module.getSpeed() - player.prevHorizontalSpeed * module.getSpeed()) * delta);
                float stride = MathHelper.lerp(delta, player.prevStrideDistance * module.getSpeed(), player.strideDistance * module.getSpeed());

                matrices.translate(-(MathHelper.sin(normal * 3.1415927F) * stride * module.getHorizontal()), Math.abs(MathHelper.cos(normal * 3.1415927F) * stride), 0);

                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(MathHelper.sin(normal * 3.1415927F) * stride * module.getRotate()));
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(Math.abs(MathHelper.cos(normal * 3.1415927F - module.getShake()) * stride) * module.getVertical()));
            }

            info.cancel();
        }
    }
}
