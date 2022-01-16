package cally72jhb.addon.system;

import cally72jhb.addon.gui.tabs.PlayersTab;
import cally72jhb.addon.gui.tabs.VectorConfigTab;
import cally72jhb.addon.system.commands.*;
import cally72jhb.addon.system.hud.ExternalChatHud;
import cally72jhb.addon.system.hud.RadarHud;
import cally72jhb.addon.system.modules.combat.*;
import cally72jhb.addon.system.modules.misc.*;
import cally72jhb.addon.system.modules.movement.*;
import cally72jhb.addon.system.modules.player.*;
import cally72jhb.addon.system.modules.render.*;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.misc.VectorStarscript;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Systems {
    private static HUD hud;

    public static void init() {
        hud = meteordevelopment.meteorclient.systems.Systems.get(HUD.class);

        // Modules
        add(new AntiDesync());
        add(new AntiGhost());
        add(new AntiPistonPush());
        add(new ArmorAlert());
        add(new AutoEz());
        add(new BedrockWalk());
        add(new BlinkPlus());
        add(new BowBomb());
        add(new CevBreaker());
        add(new ActionRenderer());
        add(new ChorusPredict());
        add(new ClipPhase());
        add(new DebugTools());
        add(new EntityFly());
        add(new NoFluid());
        add(new PacketConsume());
        add(new PacketFly());
        add(new PacketPlace());
        add(new PearlPredict());
        add(new PingSpoof());
        add(new PortalGodMode());
        add(new SkeletonESP());
        add(new SpeedBypass());
        add(new StepPlus());
        add(new StorageViewer());
        add(new Strafe());
        add(new SurroundPlus());
        add(new Tower());
        add(new Welcomer());
        add(new VectorPresence());

        // Commands
        add(new MuteCommand());
        add(new TargetCommand());

        // Hud
        add(new ExternalChatHud(hud));
        add(new RadarHud(hud));

        Players.get().init();

        Tabs.get().add(new VectorConfigTab());
        Tabs.get().add(new PlayersTab());

        VectorStarscript.init();
    }

    // Utils

    private static void add(HudElement element) {
        hud.elements.add(element);
    }

    private static void add(Command command) {
        Commands.get().add(command);
    }

    private static void add(Module module) {
        Modules.get().add(module);
    }
}
