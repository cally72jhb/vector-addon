package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class PacketPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to ignore when placing blocks.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<Boolean> actualHitPos = sgGeneral.add(new BoolSetting.Builder()
        .name("actual-hit-pos")
        .description("Places the block at your actual hit position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> randomOffset = sgGeneral.add(new BoolSetting.Builder()
        .name("random-offset")
        .description("Randomly offsets the position you interact at to bypass some anti-cheats.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block you placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand-swing animation when you place the block.")
        .defaultValue(true)
        .build()
    );

    private final Random random = new Random();

    public PacketPlace() {
        super(VectorAddon.MISC, "packet-place", "Place blocks with packets.");
    }

    @EventHandler
    private void onPlaceBlock(InteractBlockEvent event) {
        if (event.hand != null && event.result != null && checkModules()) {
            ItemStack stack = mc.player.getStackInHand(event.hand);
            BlockPos pos = event.result.getBlockPos();

            if (stack.getItem() instanceof BlockItem && (!checkClickable(pos) || mc.player.isSneaking() && checkClickable(pos))) {
                event.cancel();

                if (actualHitPos.get()) {
                    place(event.result, findInHotbar(stack).getHand(), renderSwing.get());
                    return;
                }

                if (VectorUtils.getBlockState(pos).getMaterial().isReplaceable()) {
                    place(pos, findInHotbar(stack), rotate.get(), 0, renderSwing.get(), true, false, randomOffset.get());
                } else {
                    place(pos.offset(event.result.getSide()), findInHotbar(stack), rotate.get(), 0, renderSwing.get(), true, false, randomOffset.get());
                }
            }
        }
    }

    private boolean checkClickable(BlockPos pos) {
        return VectorUtils.isClickable(VectorUtils.getBlock(pos));
    }

    private FindItemResult findInHotbar(ItemStack stack) {
        return findInHotbar(itemStack -> itemStack == stack);
    }

    private FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (isGood.test(mc.player.getOffHandStack())) return new FindItemResult(45, mc.player.getOffHandStack().getCount());

        if (isGood.test(mc.player.getMainHandStack())) return new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());

        int slot = -1, count = 0;

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    // Placing

    private void place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack, boolean random) {
        if (findItemResult.isOffhand()) {
            place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate, rotationPriority, swingHand, checkEntities, swapBack, random);
        } else {
            if (findItemResult.isHotbar()) {
                place(blockPos, Hand.MAIN_HAND, findItemResult.slot(), rotate, rotationPriority, swingHand, checkEntities, swapBack, random);
            }
        }
    }

    private void place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack, boolean offsetRandom) {
        if (slot >= 0 && slot <= 8 || slot == 45) {
            if (!canPlace(blockPos, checkEntities)) {
            } else {
                Vec3d hitPos = getHitPos(blockPos, offsetRandom);
                BlockPos neighbour = getNeighbourPos(blockPos);
                Direction side = getSide(blockPos);

                if (rotate) {
                    Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> {
                        InvUtils.swap(slot, swapBack);

                        place(new BlockHitResult(hitPos, side, neighbour, false), hand, swingHand);

                        if (swapBack) InvUtils.swapBack();
                    });
                } else {
                    InvUtils.swap(slot, swapBack);

                    place(new BlockHitResult(hitPos, side, neighbour, false), hand, swingHand);

                    if (swapBack) InvUtils.swapBack();
                }

            }
        }
    }

    private void place(BlockHitResult result, Hand hand, boolean swing) {
        if (hand != null && result != null && mc.world.getWorldBorder().contains(result.getBlockPos()) && mc.player.getStackInHand(hand).getItem() instanceof BlockItem) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

            Block block = ((BlockItem) mc.player.getStackInHand(hand).getItem()).getBlock();
            BlockSoundGroup group = block.getSoundGroup(block.getDefaultState());

            mc.world.playSound(result.getBlockPos(), group.getPlaceSound(), SoundCategory.BLOCKS, group.volume, group.pitch, true);

            if (swing) {
                mc.player.swingHand(hand);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
        }
    }

    private Vec3d getHitPos(BlockPos pos, boolean offsetRandom) {
        double px = random.nextDouble(0.9) + 0.5;
        double py = random.nextDouble(0.9) + 0.5;
        double pz = random.nextDouble(0.9) + 0.5;

        double x = offsetRandom ? px : 0.5;
        double y = offsetRandom ? py : 0.5;
        double z = offsetRandom ? pz : 0.5;

        Vec3d hitPos = new Vec3d(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
        Direction side = getPlaceSide(pos);

        if (side != null) hitPos.add(side.getOffsetX() * 0.5D, side.getOffsetY() * 0.5D, side.getOffsetZ() * 0.5D);

        return hitPos;
    }

    private BlockPos getNeighbourPos(BlockPos blockPos) {
        Direction side = getPlaceSide(blockPos);
        BlockPos neighbour;

        if (side == null) {
            neighbour = blockPos;
        } else {
            neighbour = blockPos.offset(side.getOpposite());
        }

        return neighbour;
    }

    private Direction getSide(BlockPos blockPos) {
        Direction side = getPlaceSide(blockPos);

        return side == null ? Direction.UP : side;
    }

    private boolean canPlace(BlockPos pos, boolean checkEntities) {
        if (pos == null || mc.world == null || !World.isValid(pos) || !VectorUtils.getBlockState(pos).getMaterial().isReplaceable()) return false;

        return checkEntities ? mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent()) : VectorUtils.getBlockState(pos).getMaterial().isReplaceable();
    }

    private Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction opposite = side.getOpposite();
            BlockState state = VectorUtils.getBlockState(neighbor);

            if (!state.isAir() && !VectorUtils.isClickable(state.getBlock()) && state.getFluidState().isEmpty()) {
                return opposite;
            }
        }

        return null;
    }

    private boolean checkModules() {
        if (mc.world == null || mc.player == null) return false;
        if (modules.get().isEmpty()) return true;

        for (Module module : modules.get()) {
            if (module.isActive() && Modules.get().getList().contains(module)) return false;
        }

        return true;
    }
}
