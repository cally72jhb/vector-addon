package cally72jhb.addon;

import cally72jhb.addon.commands.commands.*;
import cally72jhb.addon.modules.combat.PacketHoleFill;
import cally72jhb.addon.modules.combat.SurroundPlus;
import cally72jhb.addon.modules.misc.*;
import cally72jhb.addon.modules.movement.*;
import cally72jhb.addon.modules.player.*;
import cally72jhb.addon.modules.player.NoSwing;
import cally72jhb.addon.modules.player.PortalGodMode;
import cally72jhb.addon.modules.render.HoleRenderer;
import cally72jhb.addon.modules.render.SkeletonESP;
import cally72jhb.addon.utils.ExecutorTask;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("vector-addon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vector Addon...");

        // Initialisation

        ExecutorTask.init();

        // Combat

        Modules.get().add(new PacketHoleFill());
        Modules.get().add(new SurroundPlus());

        // Misc

        //Modules.get().add(new AntiScreen());
        Modules.get().add(new NoCollision());
        Modules.get().add(new NoWorldBorder());
        Modules.get().add(new PacketPlace());
        Modules.get().add(new PingSpoof());
        Modules.get().add(new Placeholders());

        // Movement

        Modules.get().add(new EdgeJump());
        Modules.get().add(new EntityFly());
        Modules.get().add(new EntityPhase());
        Modules.get().add(new Fly());
        Modules.get().add(new NoFallPlus());
        Modules.get().add(new PacketDigits());
        Modules.get().add(new PacketFly());
        Modules.get().add(new RubberbandFly());
        Modules.get().add(new SmartSprint());

        Modules.get().add(new PacketFlyOld());
        Modules.get().add(new TickShift());

        // Player

        Modules.get().add(new AutoRagequit());
        Modules.get().add(new NoSwing());
        Modules.get().add(new PortalGodMode());

        // Render

        Modules.get().add(new HoleRenderer());
        Modules.get().add(new SkeletonESP());
        //Modules.get().add(new StrafeDirection());

        // Commands

        Commands.add(new CenterCommand());
        Commands.add(new DesyncCommand());
        Commands.add(new ItemCommand());
        //Commands.add(new PlayerHeadCommand());
        Commands.add(new TeleportCommand());
        Commands.add(new TrashCommand());
        Commands.add(new UUIDCommand());

        // Done Initializing

        LOG.info("Done Initializing Vector Addon...");
    }

    @Override
    public String getWebsite() {
        return "https://cally72jhb.github.io/website/";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("cally72jhb", "vector-addon");
    }

    public String getPackage() {
        return "cally72jhb.addon";
    }
}
