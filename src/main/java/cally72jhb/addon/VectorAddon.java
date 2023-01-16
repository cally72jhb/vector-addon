package cally72jhb.addon;

import cally72jhb.addon.commands.commands.*;
import cally72jhb.addon.modules.combat.PacketHoleFill;
import cally72jhb.addon.modules.combat.SurroundPlus;
import cally72jhb.addon.modules.misc.*;
import cally72jhb.addon.modules.movement.*;
import cally72jhb.addon.modules.player.NoSwing;
import cally72jhb.addon.modules.player.PortalGodMode;
import cally72jhb.addon.modules.render.HoleRenderer;
import cally72jhb.addon.utils.ExecutorTask;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("vector-addon");
    public static final Category CATEGORY = new Category("Vector", Items.EMERALD_ORE.getDefaultStack());

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vector Addon...");

        // Initialisation

        ExecutorTask.init();

        // Combat

        Modules.get().add(new PacketHoleFill());
        Modules.get().add(new SurroundPlus());

        // Misc

        Modules.get().add(new AntiScreen());
        Modules.get().add(new NoCollision());
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

        // Player

        Modules.get().add(new NoSwing());
        Modules.get().add(new PortalGodMode());

        // Render

        Modules.get().add(new HoleRenderer());

        // Commands

        Commands.get().add(new CenterCommand());
        Commands.get().add(new DesyncCommand());
        Commands.get().add(new ItemCommand());
        Commands.get().add(new PlayerHeadCommand());
        Commands.get().add(new ShaderCommand());
        Commands.get().add(new TeleportCommand());
        Commands.get().add(new TrashCommand());
        Commands.get().add(new UUIDCommand());

        // Done Initializing

        LOG.info("Done Initializing Vector Addon...");
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    public String getPackage() {
        return "cally72jhb.addon";
    }
}
