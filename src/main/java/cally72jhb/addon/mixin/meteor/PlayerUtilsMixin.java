package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
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
        Color color = new Color();

        if (VectorConfig.get().highlightMembers && VectorConfig.get().highlightMembers && !VectorUtils.members.isEmpty() && VectorUtils.members.contains(entity.getEntityName())) {
            info.setReturnValue(new Color(VectorConfig.get().memberColor).a(defaultColor.a));
            return;
        }
        if (Friends.get().isFriend(entity)) {
            info.setReturnValue(new Color(Friends.get().color).a(defaultColor.a));
            return;
        }
        if (Players.get().isTargeted(entity)) {
            info.setReturnValue(new Color(Players.get().color).a(defaultColor.a));
            return;
        }
        if (!color.set(TextUtils.getMostPopularColor(entity.getDisplayName())).equals(new Color(255)) && Config.get().useTeamColor) {
            info.setReturnValue(new Color(color).a(defaultColor.a));
            return;
        }

        info.setReturnValue(defaultColor);
    }
}
