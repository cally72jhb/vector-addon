package cally72jhb.addon.mixin;

import cally72jhb.addon.gui.screens.TitleScreen;
import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cally72jhb.addon.utils.VectorUtils.mc;

@Mixin(net.minecraft.client.gui.screen.TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo info) {
        if (Systems.get(TitleScreenManager.class) != null && Systems.get(TitleScreenManager.class).active) {
            mc.setScreen(new TitleScreen());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        if (Systems.get(TitleScreenManager.class) != null && Systems.get(TitleScreenManager.class).active) {
            mc.setScreen(new TitleScreen());
        }
    }
}
