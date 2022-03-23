package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.PopRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemParticle.class)
public abstract class TotemParticleMixin extends AnimatedParticle {
    protected TotemParticleMixin(ClientWorld world, double x, double y, double z, SpriteProvider provider, float acceleration) {
        super(world, x, y, z, provider, acceleration);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider provider, CallbackInfo info) {
        PopRenderer renderer = Modules.get().get(PopRenderer.class);
        TotemParticle particle = ((TotemParticle) (Object) this);

        if (renderer != null && renderer.isActive()) {
            Color firstColor = renderer.getFirstColor();
            Color secondColor = renderer.getSecondColor();

            if (random.nextInt(renderer.getRandomness()) == 0) {
                particle.setColor((float) ((double) firstColor.r / 255), (float) ((double) firstColor.g / 255), (float) ((double) firstColor.b / 255));
            } else {
                particle.setColor((float) ((double) secondColor.r / 255), (float) ((double) secondColor.g / 255), (float) ((double) secondColor.b / 255));
            }

            scale *= renderer.getParticleSize();
            if (renderer.shouldModifyLifeTime()) maxAge = renderer.getMaxLifeTicks();
        }
    }
}
