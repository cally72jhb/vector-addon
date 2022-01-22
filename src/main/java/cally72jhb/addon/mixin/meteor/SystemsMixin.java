package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

@Mixin(Systems.class)
public class SystemsMixin {
    @Shadow @Final private static Map<Class<? extends System>, System<?>> systems = new HashMap<>();

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private static void onInit(CallbackInfo info) {
        MeteorClient.EVENT_BUS.registerLambdaFactory("cally72jhb.addon", (method, klass) -> (MethodHandles.Lookup) method.invoke(null, klass, MethodHandles.lookup()));

        System<?> config = add(new Config());
        config.init();
        config.load();

        add(new TitleScreenManager());
        add(new Players());
        add(new VectorConfig());

        MeteorClient.EVENT_BUS.subscribe(Systems.class);
    }

    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();

        return system;
    }
}
