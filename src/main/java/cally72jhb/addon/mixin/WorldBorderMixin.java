package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.misc.BorderBypass;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public class WorldBorderMixin {
    @Inject(method = "asVoxelShape", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfoReturnable<VoxelShape> info) {
        if (Modules.get() != null && Modules.get().isActive(BorderBypass.class) && Modules.get().get(BorderBypass.class).shouldCancelCollision()) {
            info.setReturnValue(VoxelShapes.empty());
        }
    }
}
