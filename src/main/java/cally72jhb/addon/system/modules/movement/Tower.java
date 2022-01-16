package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Tower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSafe = settings.createGroup("Extra Safe");

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

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("How far to attempt to cause rubberband.")
        .defaultValue(1)
        .sliderMin(1)
        .sliderMax(30)
        .min(0.1)
        .visible(this::getBypass)
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

    private final Setting<Boolean> onGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Sends packets to the server as if you are on ground.")
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

    // Extra Safe

    private final Setting<Boolean> safe = sgSafe.add(new BoolSetting.Builder()
        .name("safe")
        .description("Doesn't tower if you are in a non full block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antikick = sgSafe.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Stops you from being kicked while towering.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopmove = sgSafe.add(new BoolSetting.Builder()
        .name("stop-move")
        .description("Stops your movement that you tower right.")
        .defaultValue(true)
        .build()
    );

    private int timer;

    public Tower() {
        super(VectorAddon.MOVEMENT, "tower", "Automatically towers up fast.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        mc.player.setJumping(false);

        FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack, mc.player.getBlockPos()));

        if (!item.found()) return;

        if (mode.get() == Mode.Bypass) {
            if (validBlock(mc.player.getBlockPos()) || validBlock(mc.player.getBlockPos().down()) || isDangerousCrystal()) return;

            if ((mc.options.keyJump.isPressed() && jump.get()) || !jump.get()) {
                if (stopmove.get() && mc.player.prevY < mc.player.getY()) mc.player.setVelocity(0, 0, 0);

                if (timer > delay.get() || delay.get() == 0) {
                    if (rotate.get())
                        Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::tower);
                    else tower();

                    timer = 0;
                }

                timer++;
            }
        } else if ((mc.options.keyJump.isPressed() && jump.get()) || !jump.get()) {
            Vec3d vel = mc.player.getVelocity();

            int y = mc.player.getBlockY();

            for (int i = mc.player.getBlockY(); i < mc.world.getHeight(); i--) {
                if (!VectorUtils.getBlockState(new BlockPos(mc.player.getBlockX(), i, mc.player.getBlockZ())).isAir()) {
                    y = i + 1;
                    break;
                }
            }

            if (mc.player.getY() - y >= 7.5) {
                VectorUtils.place(mc.player.getBlockPos().down(), item, rotate.get(), 50);
            } else {
                VectorUtils.place(new BlockPos(mc.player.getBlockX(), y, mc.player.getBlockZ()), item, rotate.get(), 50);
            }

            mc.player.setVelocity(vel.x, speed.get(), vel.z);
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

    private boolean isDangerousCrystal() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && VectorUtils.distance(mc.player.getBlockPos(), entity.getBlockPos()) <= 1.5) return true;
        }

        return false;
    }

    private boolean getBypass() {
        return !bypass.get();
    }

    private void tower() {
        BlockPos pos = mc.player.getBlockPos();
        FindItemResult block = InvUtils.findInHotbar(itemStack -> validItem(itemStack, pos));

        if (!block.found()) return;
        if (!checkHead(pos)) return;
        if (isDangerousCrystal()) return;

        if (mc.player.getY() != mc.player.getBlockPos().getY() && antikick.get()) {
            mc.player.updatePosition(mc.player.getX(), (int) mc.player.getY(), mc.player.getZ());
        }

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), onGround.get()));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), onGround.get()));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), onGround.get()));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), onGround.get()));
        }

        InvUtils.swap(block.getSlot(), true);

        if (shouldSneak(pos)) mc.player.setSneaking(true);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), Direction.UP, pos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InvUtils.swapBack();

        double height = bypass.get() ? -mc.player.getY() - 2.5 : speed.get();

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), (int) (mc.player.getY() + height), mc.player.getZ(), onGround.get()));
        } else {
            mc.player.updatePosition(mc.player.getX(), (int) (mc.player.getY() + height), mc.player.getZ());
        }
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
        Normal,
        Bypass
    }
}
