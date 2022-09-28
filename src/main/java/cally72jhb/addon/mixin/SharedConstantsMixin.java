package cally72jhb.addon.mixin;

import cally72jhb.addon.modules.misc.Paragraph;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SharedConstants.class)
public class SharedConstantsMixin {
    @Inject(method = "isValidChar", at = @At("HEAD"), cancellable = true)
    private static void onIsValidChar(char character, CallbackInfoReturnable<Boolean> info) {
        if (Modules.get() != null && Modules.get().isActive(Paragraph.class)) info.setReturnValue(true);
    }
}
