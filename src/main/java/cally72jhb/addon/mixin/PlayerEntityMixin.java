package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.combat.SurroundPlus;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void onJump(CallbackInfo info) {
        // This does result in a crash
        // java.lang.NullPointerException: Cannot invoke "cally72jhb.addon.system.modules.combat.SurroundPlus.shouldCancelJumping()" because the return value of "meteordevelopment.meteorclient.systems.modules.Modules.get(java.lang.Class)" is null
        // at net.minecraft.class_1657.handler$clj000$onJump(class_1657.java:7302)
        // at net.minecraft.class_1657.method_6043(class_1657.java)
        // at net.minecraft.class_1309.method_6007(class_1309.java:2628)
        // at net.minecraft.class_1657.method_6007(class_1657.java:559)
        // at net.minecraft.class_746.method_6007(class_746.java:839)
        // at net.minecraft.class_1309.method_5773(class_1309.java:2349)
        // at net.minecraft.class_1657.method_5773(class_1657.java:274)
        // at net.minecraft.class_746.method_5773(class_746.java:215)
        // at net.minecraft.class_638.method_18646(class_638.java:218)
        // at net.minecraft.class_1937.method_18472(class_1937.java:487)
        // at net.minecraft.class_638.method_32124(class_638.java:201)
        // at net.minecraft.class_5574.method_31791(class_5574.java:54)
        // at net.minecraft.class_638.method_18116(class_638.java:197)
        // at net.minecraft.class_310.method_1574(class_310.java:1751)
        // at net.minecraft.class_310.method_1523(class_310.java:1086)
        // at net.minecraft.class_310.method_1514(class_310.java:733)
        // at net.minecraft.client.main.Main.main(Main.java:238)
        // at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
        // at java.base/java.lang.reflect.Method.invoke(Method.java:577)
        // at net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider.launch(MinecraftGameProvider.java:608)
        // at net.fabricmc.loader.impl.launch.knot.Knot.launch(Knot.java:77)
        // at net.fabricmc.loader.impl.launch.knot.KnotClient.main(KnotClient.java:23)
        // at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
        // at java.base/java.lang.reflect.Method.invoke(Method.java:577)
        // at org.multimc.onesix.OneSixLauncher.launchWithMainClass(OneSixLauncher.java:210)
        // at org.multimc.onesix.OneSixLauncher.launch(OneSixLauncher.java:245)
        // at org.multimc.EntryPoint.listen(EntryPoint.java:143)
        // at org.multimc.EntryPoint.main(EntryPoint.java:34)
        
        // if (Modules.get().get(SurroundPlus.class).shouldCancelJumping()) info.cancel();
    }
}
