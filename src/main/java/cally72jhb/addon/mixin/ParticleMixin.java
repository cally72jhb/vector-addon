package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.PopRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class ParticleMixin {
    @Shadow @Mutable protected double velocityX;
    @Shadow @Mutable protected double velocityY;
    @Shadow @Mutable protected double velocityZ;
    @Shadow @Mutable protected double prevPosX;
    @Shadow @Mutable protected double prevPosY;
    @Shadow @Mutable protected double prevPosZ;
    @Shadow @Mutable protected double x;
    @Shadow @Mutable protected double y;
    @Shadow @Mutable protected double z;

    @Shadow @Mutable protected int age;
    @Shadow @Mutable protected int maxAge;

    @Shadow public abstract void setPos(double x, double y, double z);
    @Shadow public abstract void markDead();

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void onSetVelocity(CallbackInfo info) {
        PopRenderer renderer = Modules.get().get(PopRenderer.class);

        if (renderer != null && renderer.isActive() && renderer.shouldModifyVelocity()) {
            if (age++ >= maxAge) {
                markDead();
            } else {
                prevPosX = x;
                prevPosY = y;
                prevPosZ = z;

                double dx = (renderer.getXModifier() != 0) ? velocityX / renderer.getXModifier() : velocityX;
                double dy = (renderer.getYModifier() != 0) ? velocityY / renderer.getYModifier() : velocityY;
                double dz = (renderer.getZModifier() != 0) ? velocityZ / renderer.getZModifier() : velocityZ;

                setPos(x + dx, y + dy, z + dz);
            }

            info.cancel();
        }
    }
}
