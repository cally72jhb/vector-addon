package cally72jhb.addon.mixin;

import cally72jhb.addon.modules.misc.AntiScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameStateChange", at = @At("HEAD"), cancellable = true)
    private void onSend(GameStateChangeS2CPacket packet, CallbackInfo info) {
        AntiScreen screen = Modules.get().get(AntiScreen.class);

        if (screen.isActive()
                && packet.getReason() == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN && packet.getValue() == 0.0F && screen.cancelDemoScreen()
                || packet.getReason() == GameStateChangeS2CPacket.GAME_WON && screen.cancelEndScreen()) {
            info.cancel();
        }
    }
}
