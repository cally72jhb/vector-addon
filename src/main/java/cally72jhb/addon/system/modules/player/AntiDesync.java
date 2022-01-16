package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.system.modules.movement.ClipPhase;
import cally72jhb.addon.system.modules.movement.PacketFly;
import cally72jhb.addon.system.modules.movement.StepPlus;
import cally72jhb.addon.system.modules.movement.Tower;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AntiDesync extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to ignore.")
        .defaultValue(PacketFly.class, ClipPhase.class, StepPlus.class, Tower.class)
        .build()
    );

    private final Setting<Boolean> resend = sgGeneral.add(new BoolSetting.Builder()
        .name("resend")
        .description("Resends some packets to spoof desyncing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
        .name("place")
        .description("Updates placed blocks to check if they are ghostblocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> broke = sgGeneral.add(new BoolSetting.Builder()
        .name("break")
        .description("Updates broken blocks to check if they are ghostblocks.")
        .defaultValue(true)
        .build()
    );

    private ArrayList<Integer> ignore;
    private ArrayList<Integer> teleportIDs;
    private ArrayList<BlockPos> toCheck;

    public AntiDesync() {
        super(VectorAddon.MISC, "anti-desync-plus", "Stops you from desyncing with the server.");
    }

    @Override
    public void onActivate() {
        teleportIDs = new ArrayList<>();
        toCheck = new ArrayList<>();
        ignore = new ArrayList<>();
    }

    @EventHandler
    private void onPlaceBlock(PlaceBlockEvent event) {
        if (place.get() && !toCheck.contains(event.blockPos)) toCheck.add(event.blockPos);
    }

    @EventHandler
    private void onBreakBlock(BreakBlockEvent event) {
        if (broke.get() && !toCheck.contains(event.blockPos)) toCheck.add(event.blockPos);
    }

    @EventHandler(priority = EventPriority.HIGHEST + 250)
    private void onSentPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket packet) {
            int id = packet.getTeleportId();

            if (!ignore.isEmpty() && ignore.contains(id)) {
                ignore.remove((Object) id);
                return;
            }

            if (checkModules()) teleportIDs.add(id);
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!teleportIDs.isEmpty() && checkModules()) {
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportIDs.get(0)));
            ignore.add(teleportIDs.get(0));
            teleportIDs.remove(0);
        }

        if (!toCheck.isEmpty()) {
            FindItemResult item = InvUtils.findInHotbar(stack -> stack.getUseAction() == UseAction.NONE);
            Hand hand = (item.getHand() != null && item.found()) ? item.getHand() : Hand.OFF_HAND;

            for (BlockPos pos : new ArrayList<>(toCheck)) {
                BlockHitResult result = new BlockHitResult(Utils.vec3d(pos), getBestSide(pos), pos, false);

                if (VectorUtils.isClickable(VectorUtils.getBlock(pos))) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

                mc.interactionManager.interactBlock(mc.player, mc.world, hand, result);

                toCheck.remove(pos);
            }
        }
    }

    private boolean checkModules() {
        if (mc.world == null || mc.player == null || !resend.get()) return false;
        if (modules.get().isEmpty()) return true;

        for (Module module : modules.get()) {
            if (module.isActive() && Modules.get().getList().contains(module)) return false;
        }

        return true;
    }

    private Direction getBestSide(BlockPos pos) {
        ArrayList<Direction> sides = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (VectorUtils.getBlockState(pos.offset(dir)).isOpaque()) sides.add(dir);
        }

        sides.sort(Comparator.comparingDouble(side -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos.offset(side)))));

        return sides.isEmpty() ? Direction.UP : sides.get(0);
    }
}
