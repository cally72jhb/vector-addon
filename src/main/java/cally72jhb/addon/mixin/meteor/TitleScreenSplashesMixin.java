package cally72jhb.addon.mixin.meteor;

import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mixin(SplashTextResourceSupplier.class)
public class TitleScreenSplashesMixin {
    private final Random random = new Random();

    private final List<String> splashes = getSplashes();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<String> info) {
        if (Config.get() == null || !Config.get().titleScreenSplashes) return;

        info.setReturnValue(splashes.get(random.nextInt(splashes.size())));
    }

    private static List<String> getSplashes() {
        return Arrays.asList(
                "Vector on Top!",
                "nuffin",
                "cally was here",
                "pweease"
        );
    }
}
