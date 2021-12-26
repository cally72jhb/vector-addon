package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.system.modules.movement.ClipPhase;
import cally72jhb.addon.system.modules.movement.PacketFly;
import cally72jhb.addon.system.modules.movement.StepPlus;
import cally72jhb.addon.system.modules.movement.Tower;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class AntiDesync extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to ignore")
        .defaultValue(PacketFly.class, ClipPhase.class, StepPlus.class, Tower.class)
        .build()
    );

    private ArrayList<Integer> teleportIDs;

    public AntiDesync() {
        super(VectorAddon.MISC, "anti-desync", "Stops you from desyncing with the server.");
    }

    @Override
    public void onActivate() {
        teleportIDs = new ArrayList<>();
    }

    @EventHandler
    private void onSentPacket(PacketEvent.Send event) {
        if (checkModules() && event.packet instanceof TeleportConfirmC2SPacket packet) teleportIDs.add(packet.getTeleportId());
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!teleportIDs.isEmpty() && checkModules()) {
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportIDs.get(0)));
            teleportIDs.remove(0);
        }
    }

    private boolean checkModules() {
        List<Module> all = Modules.get().getList();

        for (Module module : modules.get()) {
            if (all.contains(module) && module.isActive()) return false;
        }

        return true;
    }
}
