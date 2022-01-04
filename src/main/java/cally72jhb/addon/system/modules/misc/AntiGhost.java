package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AntiGhost extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgInteract = settings.createGroup("Interact");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder()
        .name("silent")
        .description("Removes blocks that are impossible to exist.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> entities = sgGeneral.add(new BoolSetting.Builder()
        .name("entities")
        .description("Removes blocks that have certain entities inside of them.")
        .defaultValue(true)
        .build()
    );

    // Interact

    private final Setting<Boolean> interact = sgInteract.add(new BoolSetting.Builder()
        .name("interact")
        .description("Interacts with blocks around you to test if they are real.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnce = sgInteract.add(new BoolSetting.Builder()
        .name("only-once")
        .description("Interacts every block only once.")
        .defaultValue(true)
        .visible(interact::get)
        .build()
    );

    private final Setting<Boolean> closest = sgInteract.add(new BoolSetting.Builder()
        .name("closest")
        .description("Interacts at the closest possible side.")
        .defaultValue(true)
        .visible(interact::get)
        .build()
    );

    private final Setting<Boolean> visible = sgInteract.add(new BoolSetting.Builder()
        .name("visible")
        .description("Interacts at the closest possible side that is visible.")
        .defaultValue(true)
        .visible(() -> interact.get() && closest.get())
        .build()
    );

    private final Setting<Integer> delay = sgInteract.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait in ticks after testing again.")
        .defaultValue(10)
        .min(0)
        .sliderMin(0)
        .sliderMax(50)
        .visible(interact::get)
        .build()
    );

    private final Setting<Double> range = sgInteract.add(new DoubleSetting.Builder()
        .name("range")
        .description("How far to interact with blocks.")
        .defaultValue(1.5)
        .min(1)
        .sliderMin(1)
        .sliderMax(5)
        .visible(interact::get)
        .build()
    );

    private final Setting<Integer> atATime = sgInteract.add(new IntSetting.Builder()
        .name("at-a-time")
        .description("How many blocks to interact at a time.")
        .defaultValue(2)
        .min(1)
        .sliderMin(1)
        .sliderMax(5)
        .visible(interact::get)
        .build()
    );

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    private ArrayList<BlockPos> positions;
    private ArrayList<BlockPos> interacted;
    private int timer;

    public AntiGhost() {
        super(VectorAddon.MISC, "anti-ghost", "Removes client-side blocks around you.");
    }

    @Override
    public void onActivate() {
        positions = new ArrayList<>();
        interacted = new ArrayList<>();
        timer = 0;
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (silent.get() && event.entity instanceof EndCrystalEntity entity && !VectorUtils.getBlockState(entity.getBlockPos()).isAir() && shouldTest(VectorUtils.getBlock(entity.getBlockPos()))) {
            setAir(entity.getBlockPos());
        }
    }

    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (onlyOnce.get()) interacted.add(event.result.getBlockPos());
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (entities.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (VectorUtils.getCollision(entity.getBlockPos()) == VoxelShapes.fullCube() && (entity instanceof EndCrystalEntity || entity instanceof ItemEntity)) {
                    setAir(entity.getBlockPos());
                }
            }
        }

        if (interact.get() && (timer >= delay.get() || delay.get() == 0)) {
            timer = 0;

            positions = VectorUtils.getPositionsAroundPlayer(range.get());
            positions.removeIf(pos -> BlockUtils.isClickable(VectorUtils.getBlock(pos)) || !interacted.isEmpty() && onlyOnce.get() && interacted.contains(pos));

            if (!positions.isEmpty()) {
                positions.sort(Comparator.comparingDouble(pos -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos))));

                for (int i = 0; i < positions.size() - 1; i++) {
                    if (i > atATime.get()) break;
                    BlockPos pos = positions.get(i);

                    FindItemResult item = InvUtils.findInHotbar(stack -> stack.getUseAction() == UseAction.NONE);

                    mc.interactionManager.interactBlock(mc.player, mc.world, (item.getHand() != null && item.found()) ? item.getHand() : Hand.OFF_HAND, new BlockHitResult(Utils.vec3d(pos), getDir(pos), pos, false));

                    positions.remove(0);
                }
            }
        } else {
            timer++;
        }
    }

    private void setAir(BlockPos pos) {
        mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    private boolean shouldTest(Block block) {
        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist || !blocks.get().contains(block)) return false;
        return true;
    }

    private Direction getClosestSide(BlockPos pos) {
        ArrayList<Direction> sides = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (closest.get()) {
                if (visible.get() && !VectorUtils.isSolid(pos.offset(dir))) sides.add(dir);
            } else {
                sides.add(dir);
            }
        }

        if (sides.isEmpty()) return null;

        sides.sort(Comparator.comparingDouble(side -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos.offset(side)))));

        return sides.get(0);
    }

    private Direction getDir(BlockPos pos) {
        return closest.get() ? (getClosestSide(pos) == null ? Direction.UP : getClosestSide(pos)) : Direction.UP;
    }
}
