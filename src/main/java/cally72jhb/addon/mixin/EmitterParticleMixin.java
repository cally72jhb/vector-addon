package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.PopRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EmitterParticle.class)
public class EmitterParticleMixin extends NoRenderParticle {
    protected EmitterParticleMixin(ClientWorld world, double d, double e, double f) {
        super(world, d, e, f);
    }

    @Final @Shadow private Entity entity;
    @Final @Shadow private ParticleEffect parameters;
    @Final @Shadow private int maxEmitterAge;

    @Shadow private int emitterAge;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void onTick(CallbackInfo info) {
        PopRenderer renderer = Modules.get().get(PopRenderer.class);

        if (renderer.isActive() && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            for (int i = 0; i < renderer.getAmount() * 16; i++) {
                double d = random.nextFloat() * 2 - 1;
                double e = random.nextFloat() * 2 - 1;
                double f = random.nextFloat() * 2 - 1;

                if (d * d + e * e + f * f < 1) {
                    double g = entity.offsetX(d / 4);
                    double h = entity.getBodyY(0.5 + e / 4);
                    double j = entity.offsetZ(f / 4);

                    world.addParticle(parameters, false, g, h, j, d, e + 0.2, f);
                }
            }

            if (emitterAge++ >= maxEmitterAge) markDead();

            info.cancel();
        }
    }
}
