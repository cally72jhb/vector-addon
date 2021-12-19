package cally72jhb.addon.mixin;

import cally72jhb.addon.utils.config.VectorConfig;
import cally72jhb.addon.utils.titlescreen.CustomTitleScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cally72jhb.addon.utils.VectorUtils.mc;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        if (VectorConfig.get().customTitleScreen) mc.setScreen(new CustomTitleScreen());
    }
}
