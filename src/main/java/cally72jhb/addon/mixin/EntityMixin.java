package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.movement.AntiPistonPush;
import cally72jhb.addon.system.modules.movement.NoFluid;
import cally72jhb.addon.system.modules.movement.AntiProne;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.fluid.Fluid;
import net.minecraft.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static cally72jhb.addon.utils.VectorUtils.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow private int id;

    private NoFluid noFluid;
    private AntiProne antiProne;

    // Anti Piston

    @Inject(method = "getPistonBehavior", at = @At(value = "HEAD"), cancellable = true)
    protected void onAdjustMovementForPiston(CallbackInfoReturnable<PistonBehavior> info) {
        if (Modules.get().isActive(AntiPistonPush.class)) info.setReturnValue(PistonBehavior.IGNORE);
    }

    // No Fluid

    @Inject(method = "updateMovementInFluid", at = @At(value = "HEAD"), cancellable = true)
    protected void onUpdateMovementInFluid(TagKey<Fluid> tag, double speed, CallbackInfoReturnable<Boolean> info) {
        if (shouldRemoveCollision()) info.setReturnValue(false);
    }

    @Inject(method = "isPushedByFluids", at = @At(value = "HEAD"), cancellable = true)
    protected void onIsPushedByFluids(CallbackInfoReturnable<Boolean> info) {
        if (shouldRemoveCollision()) info.setReturnValue(false);
    }

    @Inject(method = "getFluidHeight", at = @At(value = "HEAD"), cancellable = true)
    protected void onGetFluidHeight(TagKey<Fluid> fluid, CallbackInfoReturnable<Double> info) {
        if (shouldRemoveCollision()) info.setReturnValue(0D);
    }

    @Inject(method = "isSwimming", at = @At(value = "HEAD"), cancellable = true)
    protected void onIsSwimming(CallbackInfoReturnable<Boolean> info) {
        if (shouldRemoveCollision() && noFluid.disableSwimming()) info.setReturnValue(false);
    }

    // No Prone

    @Inject(method = "setPose", at = @At(value = "HEAD"), cancellable = true)
    protected void onSetPose(EntityPose pose, CallbackInfo info) {
        if (antiProne == null && Modules.get() != null && Modules.get().get(AntiProne.class) != null) antiProne = Modules.get().get(AntiProne.class);
        if (antiProne != null && antiProne.isActive() && mc != null && mc.player != null && !mc.player.isTouchingWater() && pose == EntityPose.SWIMMING) info.cancel();
    }

    // No Fluid

    private boolean shouldRemoveCollision() {
        if (noFluid == null && Modules.get() != null) noFluid = Modules.get().get(NoFluid.class);
        if (mc == null || mc.world == null || mc.player == null) return false;

        return mc.world.getEntityById(id) == mc.player && noFluid.isActive() && (noFluid.inWater() || noFluid.inLava());
    }
}
