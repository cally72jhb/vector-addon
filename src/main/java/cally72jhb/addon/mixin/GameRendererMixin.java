package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.IsometricView;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"), cancellable = true)
    private void onGetBasicProjectionMatrix(double d, CallbackInfoReturnable<Matrix4f> info) {
        IsometricView iso = Modules.get().get(IsometricView.class);

        if (iso != null && iso.isActive()) info.setReturnValue(getIsometricProjection(((float) iso.getDistance())));
    }

    private Matrix4f getIsometricProjection(float viewLength) {
        int w = client.getWindow().getFramebufferWidth();
        int h = client.getWindow().getFramebufferHeight();

        float wView = (viewLength / h) * w;

        float near = -2000;
        float far = 2000;

        float left = -wView / 2;
        float right = wView / 2;

        float top = viewLength / 2;
        float bottom = -viewLength / 2;

        float[] arr = new float[]{
                2.0f / (right - left), 0, 0, -(right + left) / (right - left),
                0, 2.0f / (top - bottom), 0, -(top + bottom) / (top - bottom),
                0, 0, -2.0f / (far - near), -(far + near) / (far - near),
                0, 0, 0, 1
        };

        Matrix4f matrix = new Matrix4f();
        ((Matrix4fMixin) (Object) matrix).set00(arr[0]);
        ((Matrix4fMixin) (Object) matrix).set01(arr[1]);
        ((Matrix4fMixin) (Object) matrix).set02(arr[2]);
        ((Matrix4fMixin) (Object) matrix).set03(arr[3]);
        ((Matrix4fMixin) (Object) matrix).set10(arr[4]);
        ((Matrix4fMixin) (Object) matrix).set11(arr[5]);
        ((Matrix4fMixin) (Object) matrix).set12(arr[6]);
        ((Matrix4fMixin) (Object) matrix).set13(arr[7]);
        ((Matrix4fMixin) (Object) matrix).set20(arr[8]);
        ((Matrix4fMixin) (Object) matrix).set21(arr[9]);
        ((Matrix4fMixin) (Object) matrix).set22(arr[10]);
        ((Matrix4fMixin) (Object) matrix).set23(arr[11]);
        ((Matrix4fMixin) (Object) matrix).set30(arr[12]);
        ((Matrix4fMixin) (Object) matrix).set31(arr[13]);
        ((Matrix4fMixin) (Object) matrix).set32(arr[14]);
        ((Matrix4fMixin) (Object) matrix).set33(arr[15]);


        return matrix;
    }
}
