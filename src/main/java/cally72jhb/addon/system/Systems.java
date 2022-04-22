package cally72jhb.addon.system;

import cally72jhb.addon.gui.tabs.PlayersTab;
import cally72jhb.addon.gui.tabs.VectorConfigTab;
import cally72jhb.addon.system.commands.*;
import cally72jhb.addon.system.hud.CustomChatHud;
import cally72jhb.addon.system.hud.MemoryHud;
import cally72jhb.addon.system.hud.PacketHud;
import cally72jhb.addon.system.hud.StatsHud;
import cally72jhb.addon.system.modules.combat.BedBomb;
import cally72jhb.addon.system.modules.combat.BowBomb;
import cally72jhb.addon.system.modules.combat.PacketHoleFill;
import cally72jhb.addon.system.modules.combat.VectorSurround;
import cally72jhb.addon.system.modules.misc.*;
import cally72jhb.addon.system.modules.movement.*;
import cally72jhb.addon.system.modules.player.*;
import cally72jhb.addon.system.modules.render.ActionRenderer;
import cally72jhb.addon.system.modules.render.PopRenderer;
import cally72jhb.addon.system.modules.render.SkeletonESP;
import cally72jhb.addon.system.modules.render.StorageViewer;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.config.VectorConfig;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.value.Value;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static cally72jhb.addon.utils.VectorUtils.mc;

public class Systems {
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends System>, System<?>> systems = new HashMap<>();

    public static void init() {
        MeteorClient.EVENT_BUS.registerLambdaFactory("cally72jhb.addon", (method, klass) -> (MethodHandles.Lookup) method.invoke(null, klass, MethodHandles.lookup()));

        System<?> config = add(new Config());
        config.init();
        config.load();

        // Modules
        add(new ActionRenderer());
        add(new AntiLagBack());
        add(new AntiPistonPush());
        add(new AntiProne());
        add(new AutoCope());
        add(new AutoEz());
        add(new AutoInteract());
        add(new BedBomb());
        add(new BowBomb());
        add(new ChatEncryption());
        add(new ChorusPredict());
        add(new DeathAnimations());
        add(new EntityFly());
        add(new InstaMinePlus());
        add(new InventoryScroll());
        add(new ItemRelease());
        add(new NoBlockTrace());
        add(new NoCollision());
        add(new NoFluid());
        add(new PacketConsume());
        add(new PacketFly());
        add(new PacketHoleFill());
        add(new PacketLogger());
        add(new PacketPlace());
        add(new PingSpoof());
        add(new Placeholders());
        add(new PopRenderer());
        add(new PortalGodMode());
        add(new ReverseStepBypass());
        add(new RubberbandFly());
        add(new SkeletonESP());
        add(new StepPlus());
        add(new StorageViewer());
        add(new TickShift());
        add(new Tower());
        add(new VectorPresence());
        add(new VectorSurround());
        add(new Welcomer());

        // Commands
        add(new ItemCommand());
        add(new MuteCommand());
        add(new StatsCommand());
        add(new TargetCommand());
        add(new TrashCommand());
        add(new UUIDCommand());

        add(new Players());
        add(new VectorConfig());

        Players.get().init();
        VectorConfig.get().init();

        Tabs.get().add(new VectorConfigTab());
        Tabs.get().add(new PlayersTab());

        MeteorStarscript.ss.set("modules", () -> Value.string(Modules.get().getActive().size() + " / " + Modules.get().getCount()));
        MeteorStarscript.ss.set("active_modules", () -> Value.string(String.valueOf(Modules.get().getActive().size())));
        MeteorStarscript.ss.set("module_count",   () -> Value.string(String.valueOf(Modules.get().getCount())));

        MeteorClient.EVENT_BUS.subscribe(Systems.class);

        mc.options.skipMultiplayerWarning = true;
    }

    public static void postInit() {
        HUD hud = meteordevelopment.meteorclient.systems.Systems.get(HUD.class);

        hud.topCenter.add(new CustomChatHud(hud));
        hud.topLeft.add(new StatsHud(hud));
        hud.topLeft.add(new MemoryHud(hud));
        hud.topLeft.add(new PacketHud(hud));
        // hud.topLeft.add(new RadarHud(hud));
    }

    // Utils

    private static void add(Command command) {
        Commands.get().add(command);
    }

    private static void add(Module module) {
        Modules.get().add(module);
    }

    private static System<?> add(System<?> system) {
        systems.put(system.getClass(), system);
        MeteorClient.EVENT_BUS.subscribe(system);
        system.init();

        return system;
    }

    // Saving / Loading

    @EventHandler
    private static void onGameLeft(GameLeftEvent event) {
        save();
    }

    public static void save(File folder) {
        long start = java.lang.System.currentTimeMillis();
        MeteorClient.LOG.info("Saving Vector");

        for (System<?> system : systems.values()) system.save(folder);

        MeteorClient.LOG.info("Saved Vector in {} milliseconds.", java.lang.System.currentTimeMillis() - start);
    }

    public static void save() {
        save(null);
    }

    public static void load(File folder) {
        long start = java.lang.System.currentTimeMillis();
        MeteorClient.LOG.info("Loading Vector");

        for (System<?> system : systems.values()) system.load(folder);

        MeteorClient.LOG.info("Loaded Vector in {} milliseconds", java.lang.System.currentTimeMillis() - start);
    }

    public static void load() {
        load(null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends System<?>> T get(Class<T> klass) {
        return (T) systems.get(klass);
    }
}
