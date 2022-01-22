package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;

public class AntiDesync extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private ArrayList<BlockPos> toCheck;

    public AntiDesync() {
        super(VectorAddon.MISC, "anti-desync-plus", "Stops you from desyncing with the server.");
    }

    @Override
    public void onActivate() {
        toCheck = new ArrayList<>();
    }

    @EventHandler
    private void onPlaceBlock(PlaceBlockEvent event) {
        if (place.get() && !toCheck.contains(event.blockPos)) toCheck.add(event.blockPos);
    }

    @EventHandler
    private void onBreakBlock(BreakBlockEvent event) {
        if (broke.get() && !toCheck.contains(event.blockPos)) toCheck.add(event.blockPos);
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
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

    private Direction getBestSide(BlockPos pos) {
        ArrayList<Direction> sides = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (VectorUtils.getBlockState(pos.offset(dir)).isOpaque()) sides.add(dir);
        }

        sides.sort(Comparator.comparingDouble(side -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos.offset(side)))));
        return sides.isEmpty() ? Direction.UP : sides.get(0);
    }
}
