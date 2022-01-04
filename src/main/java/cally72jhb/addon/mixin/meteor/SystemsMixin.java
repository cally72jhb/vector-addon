package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.config.VectorConfig;
import cally72jhb.addon.utils.titlescreen.TitleScreenConfig;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Systems.class)
public class SystemsMixin {
    @Shadow @Final private static Map<Class<? extends System>, System<?>> systems;

    @Inject(method = "addPreLoadTask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onAddPreLoadTask(Runnable task, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private static void onInit(CallbackInfo info) {
        System<?> config = add(new Config());
        config.init();
        config.load();

        add(new Modules());
        add(new Commands());
        add(new Friends());
        add(new Macros());
        add(new Accounts());
        add(new Waypoints());
        add(new Profiles());
        add(new Proxies());
        add(new Players());
        add(new VectorConfig());
        add(new TitleScreenConfig());

        MeteorClient.EVENT_BUS.subscribe(Systems.class);
    }

    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();

        return system;
    }
}
