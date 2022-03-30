package cally72jhb.addon.mixin;

import cally72jhb.addon.system.events.InteractEvent;
import cally72jhb.addon.system.modules.player.MultiTask;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Redirect(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    public boolean onHandleBlockBreaking(ClientPlayerEntity player) {
        return !Modules.get().isActive(MultiTask.class) && MeteorClient.EVENT_BUS.post(InteractEvent.get(player.isUsingItem())).usingItem;
    }

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    public boolean onDoItemUse(ClientPlayerInteractionManager manager) {
        return !Modules.get().isActive(MultiTask.class) && MeteorClient.EVENT_BUS.post(InteractEvent.get(manager.isBreakingBlock())).usingItem;
    }
}
