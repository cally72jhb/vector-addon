package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;

public class PacketBurrow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCheck = settings.createGroup("Check");

    private final Setting<BypassMode> bypassMode = sgGeneral.add(new EnumSetting.Builder<BypassMode>()
        .name("bypass-mode")
        .description("How to bypass the anti-cheat.")
        .defaultValue(BypassMode.Rubberband)
        .build()
    );

    private final Setting<Block> block = sgGeneral.add(new EnumSetting.Builder<Block>()
        .name("block-to-use")
        .description("Which block to use for burrow.")
        .defaultValue(Block.Anvil)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Jumps with packets rather than vanilla jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> automatic = sgGeneral.add(new BoolSetting.Builder()
        .name("automatic")
        .description("Automatically burrows on activate rather than waiting for jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> triggerHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("trigger-height")
        .description("How high you have to jump before a rubberband is triggered.")
        .defaultValue(1.12)
        .sliderMin(0)
        .sliderMax(1.5)
        .min(0)
        .max(1.5)
        .build()
    );

    private final Setting<Double> rubberbandHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("rubberband-height")
        .description("How far to attempt to cause rubberband.")
        .defaultValue(12)
        .sliderMin(-30)
        .sliderMax(30)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("Timer override.")
        .defaultValue(1)
        .sliderMin(0.01)
        .sliderMax(10)
        .min(0.01)
        .build()
    );

    private final Setting<Integer> placePosition = sgGeneral.add(new IntSetting.Builder()
        .name("place-position")
        .description("When the block is placed.")
        .defaultValue(6)
        .sliderMin(0)
        .sliderMax(12)
        .min(0)
        .max(12)
        .noSlider()
        .visible(() -> bypassMode.get() == BypassMode.PacketJump)
        .build()
    );

    private final Setting<Boolean> spoofOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-on-ground")
        .description("Spoofs you on ground server-side.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you to the middle of the block before burrowing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block you place server-side.")
        .defaultValue(true)
        .build()
    );

    // Checking

    private final Setting<Boolean> onlyInHole = sgCheck.add(new BoolSetting.Builder()
        .name("only-in-holes")
        .description("Stops you from burrowing when not in a hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgCheck.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Stops you from burrowing when not in a hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> checkHead = sgCheck.add(new BoolSetting.Builder()
        .name("check-head")
        .description("Only burrows when there is enough headroom to burrow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> reburrow = sgCheck.add(new BoolSetting.Builder()
        .name("reburrow")
        .description("Allows you to burrow even if already burrowed.")
        .defaultValue(true)
        .build()
    );

    private BlockPos pos;
    private boolean shouldBurrow;

    public PacketBurrow() {
        super(Categories.Combat, "packet-burrow", "Attempts to clip you into a block to prevent loads of damage.");
    }

    @Override
    public void onActivate() {
        if (!VectorUtils.getBlockState(mc.player.getBlockPos()).getMaterial().isReplaceable() && !reburrow.get()) {
            error("Already burrowed, disabling.");
            toggle();
            return;
        }

        if (!isInHole() && onlyInHole.get()) {
            error("Not in a hole, disabling.");
            toggle();
            return;
        }

        if (!mc.player.isOnGround() && onlyOnGround.get()) {
            error("Not on Ground, disabling.");
            toggle();
            return;
        }

        if (!checkHead() && checkHead.get()) {
            error("Not enough headroom to burrow, disabling.");
            toggle();
            return;
        }

        FindItemResult block = getBestBlock();

        if (!block.isHotbar() && !block.isOffhand()) {
            error("No burrow block found, disabling.");
            toggle();
            return;
        }

        Modules.get().get(Timer.class).setOverride(timer.get());

        pos = mc.player.getBlockPos();
        shouldBurrow = false;

        if (automatic.get()) {
            if (instant.get()) shouldBurrow = true;
            else mc.player.jump();
        } else {
            info("Waiting for manual jump.");
        }
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!instant.get()) shouldBurrow = mc.player.getY() > (pos.getY() + triggerHeight.get());
        if (!shouldBurrow && instant.get()) pos = mc.player.getBlockPos();

        if (shouldBurrow) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::burrow);
            } else {
                burrow();
            }

            toggle();
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (instant.get() && !shouldBurrow) {
            if (event.action == KeyAction.Press && mc.options.jumpKey.matchesKey(event.key, 0)) shouldBurrow = true;

            pos = mc.player.getBlockPos();
        }
    }

    private void burrow() {
        if (center.get()) PlayerUtils.centerPlayer();

        boolean onGround = spoofOnGround.get();

        if (bypassMode.get() == BypassMode.Rubberband) {
            if (instant.get()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), onGround));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), onGround));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), onGround));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), onGround));
            }

            place();

            if (instant.get()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false));
            } else {
                mc.player.updatePosition(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ());
            }
        } else {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            if (placePosition.get() == 0) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
            if (placePosition.get() == 1) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.41999998688698, z, onGround));
            if (placePosition.get() == 2) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.75319998052120, z, onGround));
            if (placePosition.get() == 3) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.00133597911214, z, onGround));
            if (placePosition.get() == 4) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.16610926093821, z, onGround));
            if (placePosition.get() == 5) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.24918707874468, z, onGround));
            if (placePosition.get() == 6) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.17675927506424, z, onGround));
            if (placePosition.get() == 7) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.02442408821369, z, onGround));
            if (placePosition.get() == 8) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.79673560066871, z, onGround));
            if (placePosition.get() == 9) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.49520087700593, z, onGround));
            if (placePosition.get() == 10) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.12129684053920, z, onGround));
            if (placePosition.get() == 11) place();
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
            if (placePosition.get() == 12) place();

            if (instant.get()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false));
            } else {
                mc.player.updatePosition(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ());
            }
        }
    }

    // Utils

    private void place() {
        FindItemResult block = getBestBlock();

        if (!(mc.player.getInventory().getStack(block.slot()).getItem() instanceof BlockItem)) return;
        InvUtils.swap(block.slot(), true);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InvUtils.swapBack();
    }

    private boolean checkHead() {
        BlockState state1 = VectorUtils.getBlockState(new BlockPos(mc.player.getX() + 0.3, mc.player.getY() + 2.3, mc.player.getZ() + 0.3));
        BlockState state2 = VectorUtils.getBlockState(new BlockPos(mc.player.getX() + 0.3, mc.player.getY() + 2.3, mc.player.getZ() - 0.3));
        BlockState state3 = VectorUtils.getBlockState(new BlockPos(mc.player.getX() - 0.3, mc.player.getY() + 2.3, mc.player.getZ() - 0.3));
        BlockState state4 = VectorUtils.getBlockState(new BlockPos(mc.player.getX() - 0.3, mc.player.getY() + 2.3, mc.player.getZ() + 0.3));

        return state1.getMaterial().isReplaceable() & state2.getMaterial().isReplaceable() & state3.getMaterial().isReplaceable() & state4.getMaterial().isReplaceable();
    }

    private boolean isInHole() {
        int i = 0;

        for (BlockPos pos : surround) {
            net.minecraft.block.Block block = VectorUtils.getBlock(mc.player.getBlockPos().add(pos));
            if (block != null && block.getBlastResistance() >= 600.0F) i++;
        }

        return i == 4;
    }

    private FindItemResult getBestBlock() {
        return switch (block.get()) {
            case Chest -> InvUtils.findInHotbar(stack -> stack.getItem() == Items.CHEST || stack.getItem() == Items.TRAPPED_CHEST || stack.getItem() == Items.ENDER_CHEST);
            case Anvil -> InvUtils.findInHotbar(itemStack -> net.minecraft.block.Block.getBlockFromItem(itemStack.getItem()) instanceof AnvilBlock);
            case Held -> new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());
            case Inexplodable -> InvUtils.findInHotbar(stack -> isInexplodable(stack.getItem()));
            default -> InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN);
        };
    }

    private boolean isInexplodable(Item item) {
        return Registry.BLOCK.stream().allMatch(block -> block.getBlastResistance() >= 600.0F && block.asItem() == item);
    }

    // Constants

    private final ArrayList<BlockPos> surround = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    // Enums

    public enum BypassMode {
        PacketJump,
        Rubberband
    }

    public enum Block {
        Chest,
        Obsidian,
        Inexplodable,
        Anvil,
        Held
    }
}
