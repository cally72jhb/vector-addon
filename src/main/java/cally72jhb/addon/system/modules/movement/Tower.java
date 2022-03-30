package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.FindItemResult;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class Tower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgJump = settings.createGroup("Packet Jump");
    private final SettingGroup sgCheck = settings.createGroup("Check");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .defaultValue(new ArrayList<>())
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What mode to use for tower.")
        .defaultValue(Mode.Bypass)
        .build()
    );

    private final Setting<Double> up = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-up")
        .description("How fast to go upwards.")
        .defaultValue(1)
        .sliderMin(1)
        .min(0)
        .visible(() -> mode.get() == Mode.Normal)
        .build()
    );

    private final Setting<Double> down = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-down")
        .description("How fast to go downwards.")
        .defaultValue(5)
        .sliderMin(1)
        .min(0)
        .visible(() -> mode.get() == Mode.Normal)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("How far to attempt to cause rubberband.")
        .defaultValue(1)
        .sliderMin(1)
        .sliderMax(30)
        .min(0.05)
        .visible(() -> getBypass() && mode.get() == Mode.Bypass)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait after towering up once in ticks.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(10)
        .min(0)
        .visible(() -> getBypass() && mode.get() == Mode.Bypass)
        .build()
    );

    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder()
        .name("packet")
        .description("Towers with faking jump packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> downPacket = sgGeneral.add(new BoolSetting.Builder()
        .name("down-packet")
        .description("Sends a extra packet after going down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Only towers when your jumping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Jumps with packets rather than vanilla jump.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Bypass)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Bypasses some anti cheats.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Bypass)
        .build()
    );

    private final Setting<Boolean> onGroundSpoof = sgGeneral.add(new BoolSetting.Builder()
        .name("on-ground-spoof")
        .description("Spoofs you on ground server-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block you place server-side.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Bypass)
        .build()
    );

    // Packet Jump

    private final Setting<Integer> jumpStart = sgJump.add(new IntSetting.Builder()
        .name("jump-start")
        .description("From what point on the start sending the jump packets.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(12)
        .min(0)
        .max(12)
        .noSlider()
        .build()
    );

    private final Setting<Integer> jumpEnd = sgJump.add(new IntSetting.Builder()
        .name("jump-end")
        .description("Till what point to send the jump packets.")
        .defaultValue(12)
        .sliderMin(1)
        .sliderMax(12)
        .min(1)
        .max(12)
        .noSlider()
        .build()
    );

    private final Setting<Integer> placePosition = sgJump.add(new IntSetting.Builder()
        .name("place-position")
        .description("When the block is placed.")
        .defaultValue(6)
        .sliderMin(0)
        .sliderMax(12)
        .min(0)
        .max(12)
        .noSlider()
        .build()
    );

    // Extra Safe

    private final Setting<Boolean> safe = sgCheck.add(new BoolSetting.Builder()
        .name("safe")
        .description("Doesn't tower if you are in a non full block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Sends packets to the server as if you are on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antikick = sgCheck.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Stops you from getting kicked for fly while towering.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopmove = sgCheck.add(new BoolSetting.Builder()
        .name("stop-move")
        .description("Stops your movement.")
        .defaultValue(true)
        .build()
    );

    private int timer;

    public Tower() {
        super(Categories.Movement, "tower", "Automatically towers up fast.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        mc.player.setJumping(false);

        FindItemResult item = VectorUtils.findInHotbar(itemStack -> validItem(itemStack, mc.player.getBlockPos()));

        if (!item.found() || onlyOnGround.get() && !mc.player.isOnGround()) return;

        if (mode.get() == Mode.Bypass) {
            if (validBlock(mc.player.getBlockPos()) || validBlock(mc.player.getBlockPos().down())) return;

            if (!jump.get() || (mc.options.jumpKey.isPressed() && jump.get())) {
                if (stopmove.get() && mc.player.prevY < mc.player.getY()) mc.player.setVelocity(0, 0, 0);

                if (timer > delay.get() || delay.get() == 0) {
                    if (rotate.get())
                        Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::tower);
                    else tower();

                    timer = 0;
                }

                timer++;
            }
        } else if (mode.get() == Mode.Normal && !jump.get() || (mc.options.jumpKey.isPressed() && jump.get())) {
            Vec3d velocity = mc.player.getVelocity();

            if (mc.player.isOnGround()) mc.player.setVelocity(velocity.x * 0.3, up.get() / 100, mc.player.getVelocity().z * 0.3);
            if (VectorUtils.getCollision(mc.player.getBlockPos().down()) == null || VectorUtils.getCollision(mc.player.getBlockPos().down()).isEmpty()) {
                mc.player.setVelocity(velocity.x * 0.3, -(down.get() / 100), velocity.z * 0.3);
            }
        } else if (mode.get() == Mode.PacketJump && !jump.get() || (mc.options.jumpKey.isPressed() && jump.get())) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            BlockPos pos = mc.player.getBlockPos();

            int start = jumpStart.get();
            int end = jumpEnd.get();
            int position = placePosition.get();

            boolean onGround = onGroundSpoof.get();

            if (position == 0) place(pos, item);
            if (start >= 0 && end <= 0) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
            if (position == 1) place(pos, item);
            if (start >= 1 && end <= 1) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.41999998688698, z, onGround));
            if (position == 2) place(pos, item);
            if (start >= 2 && end <= 2) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.75319998052120, z, onGround));
            if (position == 3) place(pos, item);
            if (start >= 3 && end <= 3) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.00133597911214, z, onGround));
            if (position == 4) place(pos, item);
            if (start >= 4 && end <= 4) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.16610926093821, z, onGround));
            if (position == 5) place(pos, item);
            if (start >= 5 && end <= 5) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.24918707874468, z, onGround));
            if (position == 6) place(pos, item);
            if (start >= 6 && end <= 6) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.17675927506424, z, onGround));
            if (position == 7) place(pos, item);
            if (start >= 7 && end <= 7) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.02442408821369, z, onGround));
            if (position == 8) place(pos, item);
            if (start >= 8 && end <= 8) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.79673560066871, z, onGround));
            if (position == 9) place(pos, item);
            if (start >= 9 && end <= 9) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.49520087700593, z, onGround));
            if (position == 10) place(pos, item);
            if (start >= 10 && end <= 10) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.12129684053920, z, onGround));
            if (position == 11) place(pos, item);
            if (start >= 11 && end <= 11) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
            if (position == 12) place(pos, item);
        }

        if (packet.get() && !jump.get() || (mc.options.jumpKey.isPressed() && jump.get())) {
            if (!mc.player.isOnGround()) {
                VoxelShape shape = VectorUtils.getCollision(new BlockPos(mc.player.getPos().x, (int) Math.round(mc.player.getPos().y), mc.player.getPos().z).down());

                if (shape == null || shape.isEmpty() && downPacket.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), Math.floor(mc.player.getY()), mc.player.getZ(), true));
                    mc.player.setPosition(mc.player.getX(), Math.floor(mc.player.getY()), mc.player.getZ());
                }

                return;
            }

            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.41999998688698, z, true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.75319998052120, z, true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.00133597911214, z, true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.16610926093821, z, true));

            mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ());
        }
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        BlockPos pos = mc.player.getBlockPos();

        if (!Block.isShapeFullCube(VectorUtils.getBlockState(pos).getCollisionShape(mc.world, pos))) return;
        if (stopmove.get() && mc.player.prevY < mc.player.getY()) {
            ((IVec3d) event.movement).setY(-1);
        }
    }

    private boolean validBlock(BlockPos pos) {
        if (!safe.get()) return false;
        Block block = VectorUtils.getBlock(pos);
        if (block == Blocks.BEDROCK) return false;
        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(block)) return false;
        if (VectorUtils.getBlockState(pos).isAir()) return false;
        if (VectorUtils.getBlock(pos) == Blocks.ANVIL) return false;

        return VectorUtils.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(block)) return false;

        if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(mc.world.getBlockState(pos));
    }

    private boolean getBypass() {
        return !bypass.get();
    }

    private void tower() {
        BlockPos pos = mc.player.getBlockPos();
        FindItemResult block = VectorUtils.findInHotbar(itemStack -> validItem(itemStack, pos));

        boolean onGround = onGroundSpoof.get();

        if (!block.found() || !checkHead(pos)) return;

        if (mc.player.getY() != mc.player.getBlockPos().getY() && antikick.get()) {
            mc.player.updatePosition(mc.player.getX(), (int) mc.player.getY(), mc.player.getZ());
        }

        if (instant.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), onGround));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), onGround));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), onGround));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), onGround));
        }

        InvUtils.swap(block.getSlot(), true);

        if (shouldSneak(pos)) mc.player.setSneaking(true);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InvUtils.swapBack();

        double height = bypass.get() ? -mc.player.getY() - 2.5 : speed.get();

        if (instant.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), (int) (mc.player.getY() + height), mc.player.getZ(), onGround));
        } else {
            mc.player.updatePosition(mc.player.getX(), (int) (mc.player.getY() + height), mc.player.getZ());
        }
    }

    private void place(BlockPos pos, FindItemResult item) {
        InvUtils.swap(item.getSlot(), true);

        if (shouldSneak(pos)) mc.player.setSneaking(true);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InvUtils.swapBack();
    }

    private boolean shouldSneak(BlockPos pos) {
        return VectorUtils.getBlock(pos) == Blocks.ANVIL || VectorUtils.getBlock(pos) == Blocks.CRAFTING_TABLE || VectorUtils.getBlock(pos.down()) == Blocks.ANVIL || VectorUtils.getBlock(pos.down()) == Blocks.CRAFTING_TABLE;
    }

    private boolean checkHead(BlockPos blockPos) {
        BlockPos.Mutable pos = new BlockPos(blockPos).mutableCopy();
        BlockState blockState1 = mc.world.getBlockState(pos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        BlockState blockState2 = mc.world.getBlockState(pos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState3 = mc.world.getBlockState(pos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState4 = mc.world.getBlockState(pos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        boolean air1 = blockState1.getMaterial().isReplaceable();
        boolean air2 = blockState2.getMaterial().isReplaceable();
        boolean air3 = blockState3.getMaterial().isReplaceable();
        boolean air4 = blockState4.getMaterial().isReplaceable();
        return air1 & air2 & air3 & air4;
    }

    private boolean blockFilter(Block block) {
        return block.getDefaultState().getMaterial().isSolid();
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum Mode {
        PacketJump,
        Normal,
        Bypass
    }
}
