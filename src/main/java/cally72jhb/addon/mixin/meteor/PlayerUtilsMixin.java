package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerUtils.class)
public class PlayerUtilsMixin {
    @Inject(method = "getPlayerColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetPlayerColor(PlayerEntity entity, Color defaultColor, CallbackInfoReturnable<Color> info) {
        if (VectorConfig.get() != null && VectorConfig.get().memberColor != null && VectorConfig.get().highlightMembers && !VectorUtils.members.isEmpty() && VectorUtils.members.contains(entity.getEntityName())) {
            info.setReturnValue(new Color(VectorConfig.get().memberColor).a(defaultColor.a));
        }
    }
}
