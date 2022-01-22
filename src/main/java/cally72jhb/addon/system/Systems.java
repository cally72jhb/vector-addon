package cally72jhb.addon.system;

import cally72jhb.addon.gui.tabs.TitleScreenTab;
import cally72jhb.addon.gui.tabs.PlayersTab;
import cally72jhb.addon.gui.tabs.VectorConfigTab;
import cally72jhb.addon.system.commands.*;
import cally72jhb.addon.system.hud.CustomChatHud;
import cally72jhb.addon.system.hud.StatsHud;
import cally72jhb.addon.system.modules.combat.*;
import cally72jhb.addon.system.modules.misc.*;
import cally72jhb.addon.system.modules.movement.*;
import cally72jhb.addon.system.modules.player.*;
import cally72jhb.addon.system.modules.render.*;
import cally72jhb.addon.system.players.Players;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.Value;

import static cally72jhb.addon.utils.VectorUtils.mc;

public class Systems {
    private static HUD hud;

    public static void init() {
        hud = meteordevelopment.meteorclient.systems.Systems.get(HUD.class);

        // Modules
        add(new ActionRenderer());
        add(new AntiDesync());
        add(new AntiGhost());
        add(new AntiPistonPush());
        add(new AutoEz());
        add(new BedrockWalk());
        add(new BlinkPlus());
        add(new BowBomb());
        add(new CevBreaker());
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
        add(new VectorPresence());
        add(new Welcomer());

        // Commands
        add(new MuteCommand());
        add(new TargetCommand());
        add(new StatsCommand());

        // Hud
        hud.topCenter.add(new CustomChatHud(hud));
        hud.topLeft.add(new StatsHud(hud));

        Players.get().init();

        Tabs.get().add(new TitleScreenTab());
        Tabs.get().add(new VectorConfigTab());
        Tabs.get().add(new PlayersTab());

        MeteorStarscript.ss.set("modules", () -> Value.string(Modules.get().getActive().size() + " / " + Modules.get().getCount()));
        MeteorStarscript.ss.set("active_modules", () -> Value.string(String.valueOf(Modules.get().getActive().size())));
        MeteorStarscript.ss.set("module_count",   () -> Value.string(String.valueOf(Modules.get().getCount())));

        mc.options.skipMultiplayerWarning = true;
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
