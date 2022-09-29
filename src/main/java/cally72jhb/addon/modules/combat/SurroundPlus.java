package cally72jhb.addon.modules.combat;

import cally72jhb.addon.utils.Utils;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.*;

public class SurroundPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgCenter = settings.createGroup("Center");
    private final SettingGroup sgCrystal = settings.createGroup("Crystal");
    private final SettingGroup sgOther = settings.createGroup("Other");

    // General

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only places when you are on ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnYChange = sgOther.add(new BoolSetting.Builder()
            .name("disable-on-y-change")
            .description("Disables the module as soon as your y-level changes.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableStep = sgOther.add(new BoolSetting.Builder()
            .name("disable-step")
            .description("Disables step when in surround.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> tolerance = sgGeneral.add(new DoubleSetting.Builder()
            .name("tolerance")
            .description("The height tolerance when standing on a non full block.")
            .defaultValue(0.25)
            .min(0)
            .sliderMin(0)
            .sliderMax(0.5)
            .build()
    );

    // Place

    private final Setting<PlaceMode> placeMode = sgPlace.add(new EnumSetting.Builder<PlaceMode>()
            .name("place-mode")
            .description("How blocks are placed.")
            .defaultValue(PlaceMode.Normal)
            .build()
    );

    private final Setting<Boolean> placeRangeBypass = sgPlace.add(new BoolSetting.Builder()
            .name("place-range-bypass")
            .description("Interacts at the closest possible position to allow a maximal place range.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> blocksPerTick = sgPlace.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("How many blocks to place per tick.")
            .defaultValue(1)
            .min(1)
            .noSlider()
            .build()
    );

    private final Setting<List<Block>> blocks = sgPlace.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("What blocks to use for surround.")
            .defaultValue(List.of(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ANCIENT_DEBRIS, Blocks.NETHERITE_BLOCK, Blocks.ENDER_CHEST, Blocks.RESPAWN_ANCHOR, Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL))
            .filter(block -> block.getBlastResistance() >= 600.0F)
            .build()
    );

    private final Setting<StabilizeType> stabilizeType = sgPlace.add(new EnumSetting.Builder<StabilizeType>()
            .name("stabilize-type")
            .description("Stabilizes the block placements for a high ping.")
            .defaultValue(StabilizeType.Wait)
            .build()
    );

    private final Setting<Integer> replaceDelay = sgPlace.add(new IntSetting.Builder()
            .name("replace-delay")
            .description("How many ticks to wait till trying to replace a block failing to place.")
            .defaultValue(2)
            .min(1)
            .sliderMin(0)
            .sliderMax(4)
            .visible(() -> stabilizeType.get() == StabilizeType.Wait)
            .build()
    );

    // Center

    private final Setting<CenterMode> centerMode = sgCenter.add(new EnumSetting.Builder<CenterMode>()
            .name("center-mode")
            .description("When to center you.")
            .defaultValue(CenterMode.OnActivate)
            .build()
    );

    private final Setting<CenterType> centerType = sgCenter.add(new EnumSetting.Builder<CenterType>()
            .name("center-type")
            .description("How to center you before surrounding.")
            .defaultValue(CenterType.Fast)
            .visible(() -> centerMode.get() != CenterMode.Never)
            .build()
    );


    private final Setting<Boolean> centerInAir = sgCenter.add(new BoolSetting.Builder()
            .name("center-in-air")
            .description("Centers you even if you are in the air.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> rangeFromCenter = sgCenter.add(new DoubleSetting.Builder()
            .name("range-from-center")
            .description("How far away from the center of your current block you have to be to be centered again.")
            .defaultValue(0.15)
            .min(0)
            .max(0.5)
            .sliderMin(0)
            .sliderMax(0.5)
            .visible(() -> centerMode.get() != CenterMode.Never)
            .build()
    );

    private final Setting<Double> centerSpeed = sgCenter.add(new DoubleSetting.Builder()
            .name("speed")
            .description("How fast to center you.")
            .defaultValue(0.5)
            .min(0)
            .sliderMin(0.25)
            .sliderMax(0.75)
            .visible(() -> centerMode.get() != CenterMode.Never && centerType.get() == CenterType.Smooth)
            .build()
    );

    // Crystal

    private final Setting<CrystalBreakMode> crystalBreakMode = sgCrystal.add(new EnumSetting.Builder<CrystalBreakMode>()
            .name("crystal-break-type")
            .description("How to break interfering crystals.")
            .defaultValue(CrystalBreakMode.Quick)
            .build()
    );

    // Other

    private final Setting<Keybind> doubleKeybind = sgOther.add(new KeybindSetting.Builder()
            .name("force-double")
            .description("Forces double high surround.")
            .defaultValue(Keybind.fromKey(-1))
            .build()
    );

    private final Setting<Boolean> swapBack = sgOther.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Swaps back to the selected slot after being done with action.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> message = sgOther.add(new BoolSetting.Builder()
            .name("message")
            .description("Informs you about keybinds being pressed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderSwing = sgOther.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders a hand-swing animation when you place the block.")
            .defaultValue(true)
            .build()
    );

    // Constructor

    public SurroundPlus() {
        super(Categories.Combat, "surround-plus", "Surrounds you in blocks to prevent you from taking explosion damage.");
    }

    private HashMap<BlockPos, Integer> places;

    private boolean stepWasActive;
    private boolean doubleHeight;

    @Override
    public void onActivate() {
        places = new HashMap<>();

        stepWasActive = Modules.get().isActive(Step.class);
        doubleHeight = false;

        // Centering

        if ((centerMode.get() == CenterMode.OnActivate || centerMode.get() == CenterMode.Always || centerMode.get() == CenterMode.Incomplete)
                && (centerInAir.get() && !mc.player.isOnGround() || mc.player.isOnGround())) {
            boolean incomplete = centerMode.get() != CenterMode.Incomplete;

            if (!incomplete) {
                Block block = getBlock();

                for (Direction direction : Direction.values()) {
                    if (direction != Direction.UP && direction != Direction.DOWN) {
                        Vec3d position = mc.player.getPos().add(0, tolerance.get(), 0);

                        BlockPos pos = new BlockPos(
                                Math.floor(position.getX()),
                                Math.floor(position.getY()),
                                Math.floor(position.getZ())
                        );

                        pos = pos.offset(direction);

                        if (mc.world.getBlockState(pos).getBlock().getBlastResistance() < 600.0F && canPlace(pos, block == null ? Blocks.OBSIDIAN.getDefaultState() : block.getDefaultState(), mc.world.getBlockState(pos))) {
                            incomplete = true;
                            break;
                        }
                    }
                }
            }

            if (incomplete) {
                center();
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (stepWasActive && disableStep.get()) {
            if (!Modules.get().isActive(Step.class)) {
                Modules.get().get(Step.class).toggle();
            }
        }
    }

    // Packet Event

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            int slot = getSlot();
            Block block = getBlock();

            surround(block, slot, packet);

            for (BlockPos pos : new HashSet<>(places.keySet())) {
                if (packet.getPos().equals(pos)) {
                    places.remove(pos);
                }
            }
        }
    }

    // Key Event

    @EventHandler
    private void onKey(KeyEvent event) {
        if (doubleKeybind.get().isPressed()) {
            doubleHeight = !doubleHeight;

            if (message.get()) {
                info(Text.literal("Toggled Double Height ").append(Text.literal(doubleHeight ? "On" : "Off").setStyle(Style.EMPTY.withColor(doubleHeight ? Formatting.GREEN : Formatting.RED))));
            }
        }
    }

    // Main Tick

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player.prevY + 0.015 < mc.player.getY() && disableOnYChange.get()) {
            this.toggle();
            return;
        }

        // Disabling Step

        if (Modules.get().isActive(Step.class) && disableStep.get() && mc.world != null && mc.currentScreen == null) {
            Modules.get().get(Step.class).toggle();
            stepWasActive = true;
        }

        // Item Picking

        int slot = getSlot();
        Block block = getBlock();

        // Centering

        if (centerMode.get() != CenterMode.Never && (centerInAir.get() && !mc.player.isOnGround() || mc.player.isOnGround())) {
            if (centerMode.get() == CenterMode.Always || centerInAir.get() && !mc.player.isOnGround()) {
                center();
            } else if (centerMode.get() == CenterMode.Incomplete && block != null) {
                boolean incomplete = false;

                for (Direction direction : Direction.values()) {
                    if (direction != Direction.UP && direction != Direction.DOWN) {
                        Vec3d position = mc.player.getPos().add(0, tolerance.get(), 0);

                        BlockPos pos = new BlockPos(
                                Math.floor(position.getX()),
                                Math.floor(position.getY()),
                                Math.floor(position.getZ())
                        );

                        pos = pos.offset(direction);

                        if (mc.world.getBlockState(pos).getBlock().getBlastResistance() < 600.0F && canPlace(pos, block.getDefaultState(), mc.world.getBlockState(pos))) {
                            incomplete = true;
                            break;
                        }
                    }
                }

                if (incomplete) {
                    center();
                }
            }
        }

        // Crystal Breaking

        if (crystalBreakMode.get() == CrystalBreakMode.Both || crystalBreakMode.get() == CrystalBreakMode.Normal) {
            for (EndCrystalEntity crystal : getCrystalsAroundSurround()) {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                swingHand(Hand.MAIN_HAND);
            }
        }

        // Checks

        if (onlyOnGround.get() && !mc.player.isOnGround()) {
            return;
        }

        // Default Surround

        surround(block, slot, null);
    }

    // Surround Logic

    private void surround(Block block, int slot, BlockUpdateS2CPacket packet) {
        if (block != null && (slot >= 0 && slot <= 8 || slot == 45)) {
            List<BlockPos> positions = new ArrayList<>();

            for (Direction direction : Direction.values()) {
                if (direction != Direction.UP && direction != Direction.DOWN) {
                    BlockPos pos = new BlockPos(
                            mc.player.getBlockX(),
                            Math.floor(mc.player.getY() + tolerance.get()),
                            mc.player.getBlockZ()
                    ).offset(direction);

                    if (mc.world.getBlockState(pos).getBlock().getBlastResistance() < 600.0F && canPlace(pos, block.getDefaultState(), mc.world.getBlockState(pos))) {
                        positions.add(pos);
                    }

                    if (doubleHeight) {
                        BlockPos position = pos.up();

                        if (packet == null && mc.world.getBlockState(position).getBlock().getBlastResistance() < 600.0F
                                && canPlace(position, block.getDefaultState(), mc.world.getBlockState(position))
                                || packet != null && position.equals(packet.getPos()) && packet.getState().getBlock().getBlastResistance() < 600.0F
                                && canPlace(position, block.getDefaultState(), packet.getState())) {
                            positions.add(position);
                        }
                    }
                }
            }

            if (!positions.isEmpty()) {
                int prevSlot = -1;
                if (mc.player.getInventory().selectedSlot != slot && slot != 45) {
                    prevSlot = mc.player.getInventory().selectedSlot;

                    swapTo(slot);
                }

                int blocks = 0;

                for (BlockPos pos : positions) {
                    if (blocks < blocksPerTick.get() && (stabilizeType.get() == StabilizeType.None || !places.containsKey(pos))) {
                        place(pos, slot);

                        if (stabilizeType.get() != StabilizeType.None) {
                            places.put(pos, 0);
                        }

                        blocks++;
                    }
                }

                if (prevSlot != -1 && swapBack.get()) {
                    swapTo(prevSlot);
                }
            }

            for (BlockPos pos : new HashSet<>(places.keySet())) {
                if (places.get(pos) >= replaceDelay.get()) {
                    places.remove(pos);
                } else {
                    places.replace(pos, places.get(pos) + 1);
                }
            }
        }
    }


    // Utils


    private void place(BlockPos pos, int slot) {
        if (pos != null && (slot >= 0 && slot <= 8 || slot == 45)) {
            if (slot == 45) {
                place(pos, Hand.OFF_HAND);
            } else {
                place(pos, Hand.MAIN_HAND);
            }
        }
    }

    private void place(BlockPos pos, Hand hand) {
        Direction side = getSide(pos);
        BlockPos neighbour = getNeighbourPos(pos);
        Vec3d hitPos = getHitPos(pos, side);

        boolean sneak = !mc.player.isSneaking() && isClickable(mc.world.getBlockState(neighbour).getBlock());

        if (crystalBreakMode.get() == CrystalBreakMode.Both || crystalBreakMode.get() == CrystalBreakMode.Quick) {
            for (EndCrystalEntity crystal : getCrystalsAroundBlock(pos)) {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                swingHand(Hand.MAIN_HAND);
            }
        }

        place(new BlockHitResult(hitPos, side, neighbour, false), hand, sneak);
    }

    // Placing

    private void place(BlockHitResult result, Hand hand, boolean sneak) {
        if (hand != null && result != null) {
            if (sneak) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            }

            if (placeMode.get() == PlaceMode.Packet) {
                mc.interactionManager.sendSequencedPacket(mc.world, sequence -> {
                    PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(hand, result, 0);
                    mc.getNetworkHandler().sendPacket(packet);
                    return packet;
                });
            } else {
                mc.interactionManager.interactBlock(mc.player, hand, result);
            }

            swingHand(hand);

            if (sneak) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            }
        }
    }

    private boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    private Direction getSide(BlockPos pos) {
        Direction side = getPlaceSide(pos);

        return side == null ? (mc.player.getEyeY() > pos.getY() + 0.5 ? Direction.UP : Direction.DOWN) : side;
    }

    private Direction getPlaceSide(BlockPos pos) {
        List<Direction> sides = new ArrayList<>();

        for (Direction side : Direction.values()) {
            BlockState state = mc.world.getBlockState(pos.offset(side));

            if (!state.getMaterial().isReplaceable() && state.getFluidState().isEmpty()) {
                sides.add(side.getOpposite());
            }
        }

        if (sides.size() == 1) {
            return sides.get(0);
        } else if (!sides.isEmpty()) {
            sides.sort(Comparator.comparingDouble(side -> Utils.distance(mc.player.getPos(), Vec3d.ofCenter(pos.offset(side)))));

            return sides.get(0);
        }

        return null;
    }

    private BlockPos getNeighbourPos(BlockPos pos) {
        BlockPos neighbour;
        Direction side = getPlaceSide(pos);

        if (side == null) {
            neighbour = pos;
        } else {
            neighbour = pos.offset(side.getOpposite());
        }

        return neighbour;
    }

    private Vec3d getHitPos(BlockPos pos, Direction side) {
        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (side != null) {
            Direction opposite = side.getOpposite();

            if (!placeRangeBypass.get()) {
                hitPos = hitPos.add(
                        opposite.getOffsetX() == 0 ? 0 : (opposite.getOffsetX() > 0 ? 0.5 : -0.5),
                        opposite.getOffsetY() == 0 ? 0 : (opposite.getOffsetY() > 0 ? 0.5 : -0.5),
                        opposite.getOffsetZ() == 0 ? 0 : (opposite.getOffsetZ() > 0 ? 0.5 : -0.5)
                );
            } else {
                Vec3d target = mc.player.getEyePos();
                VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);

                if (shape != null && !shape.isEmpty()) {
                    Box box = new Box(
                            pos.getX() + (opposite == Direction.EAST ? shape.getMax(Direction.Axis.X) : shape.getMin(Direction.Axis.X)),
                            pos.getY() + (opposite == Direction.UP ? shape.getMax(Direction.Axis.Y) : shape.getMin(Direction.Axis.Y)),
                            pos.getZ() + (opposite == Direction.SOUTH ? shape.getMax(Direction.Axis.Z) : shape.getMin(Direction.Axis.Z)),
                            pos.getX() + (opposite != Direction.WEST ? shape.getMax(Direction.Axis.X) : shape.getMin(Direction.Axis.X)),
                            pos.getY() + (opposite != Direction.DOWN ? shape.getMax(Direction.Axis.Y) : shape.getMin(Direction.Axis.Y)),
                            pos.getZ() + (opposite != Direction.NORTH ? shape.getMax(Direction.Axis.Z) : shape.getMin(Direction.Axis.Z))
                    );

                    double x = MathHelper.clamp(target.getX(), box.minX, box.maxX);
                    double y = MathHelper.clamp(target.getY(), box.minY, box.maxY);
                    double z = MathHelper.clamp(target.getZ(), box.minZ, box.maxZ);

                    hitPos = new Vec3d(x, y, z);
                } else {
                    Box box = new Box(
                            pos.getX() + (opposite == Direction.EAST ? 1.0 : 0.0),
                            pos.getY() + (opposite == Direction.UP ? 1.0 : 0.0),
                            pos.getZ() + (opposite == Direction.SOUTH ? 1.0 : 0.0),
                            pos.getX() + (opposite != Direction.WEST ? 1.0 : 0.0),
                            pos.getY() + (opposite != Direction.DOWN ? 1.0 : 0.0),
                            pos.getZ() + (opposite != Direction.NORTH ? 1.0 : 0.0)
                    );

                    double x = MathHelper.clamp(target.getX(), box.minX, box.maxX);
                    double y = MathHelper.clamp(target.getY(), box.minY, box.maxY);
                    double z = MathHelper.clamp(target.getZ(), box.minZ, box.maxZ);

                    hitPos = new Vec3d(x, y, z);
                }
            }
        }

        return hitPos;
    }

    private boolean canPlace(BlockPos pos, BlockState state, BlockState original) {
        if (pos == null || mc.world == null || !World.isValid(pos) || original != null && !original.getMaterial().isReplaceable()) return false;
        return mc.world.getWorldBorder().contains(pos) && canPlace(pos, state);
    }

    private boolean canPlace(BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(mc.world, pos, ShapeContext.absent());
        return shape.isEmpty() || mc.world.doesNotIntersectEntities(null, shape.offset(pos.getX(), pos.getY(), pos.getZ()));
    }

    // Other

    private int getSlot() {
        int slot = -1;

        for (Block block : blocks.get()) {
            if (mc.player.getMainHandStack().getItem() instanceof BlockItem item && block.asItem().equals(item)) {
                slot = mc.player.getInventory().selectedSlot;
            } else if (mc.player.getOffHandStack().getItem() instanceof BlockItem item && block.asItem().equals(item)) {
                slot = 45;
            } else {
                for (int i = 0; i <= 8; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem item && block.asItem().equals(item)) {
                        slot = i;
                        break;
                    }
                }
            }

            if (slot != -1) break;
        }

        return slot;
    }

    private Block getBlock() {
        Block block = null;

        for (Block stack : blocks.get()) {
            if (mc.player.getMainHandStack().getItem() instanceof BlockItem item && stack.asItem().equals(item)) {
                block = item.getBlock();
            } else if (mc.player.getOffHandStack().getItem() instanceof BlockItem item && stack.asItem().equals(item)) {
                block = item.getBlock();
            } else {
                for (int i = 0; i <= 8; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem item && stack.asItem().equals(item)) {
                        block = item.getBlock();
                        break;
                    }
                }
            }

            if (block != null) break;
        }

        return block;
    }

    private Direction getOppositePlaceSide(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && direction != Direction.DOWN) {
                BlockPos position = new BlockPos(
                        mc.player.getBlockX(),
                        Math.floor(mc.player.getY() + tolerance.get()),
                        mc.player.getBlockZ()
                );

                if (position.offset(direction).equals(pos)) {
                    return direction;
                }
            }
        }

        return null;
    }

    private List<EndCrystalEntity> getCrystalsAroundBlock(BlockPos pos) {
        List<EndCrystalEntity> crystals = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                for (BlockPos position : possibles) {
                    if (pos.add(position).equals(crystal.getBlockPos()) || pos.add(position).down().equals(crystal.getBlockPos())) {
                        crystals.add(crystal);
                        break;
                    }
                }
            }
        }

        return crystals;
    }

    private List<EndCrystalEntity> getCrystalsAroundSurround() {
        List<EndCrystalEntity> crystals = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                for (Direction direction : Direction.values()) {
                    if (direction != Direction.UP && direction != Direction.DOWN) {
                        BlockPos pos = new BlockPos(
                                mc.player.getBlockX(),
                                Math.floor(mc.player.getY() + tolerance.get()),
                                mc.player.getBlockZ()
                        ).offset(direction);

                        for (BlockPos position : possibles) {
                            if (pos.add(position).equals(crystal.getBlockPos()) || pos.add(position).down().equals(crystal.getBlockPos())) {
                                crystals.add(crystal);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return crystals;
    }

    private void swingHand(Hand hand) {
        if (renderSwing.get()) {
            mc.player.swingHand(hand);
        } else {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    private void swapTo(int slot) {
        if (slot >= 0 && slot <= 8) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.player.getInventory().selectedSlot = slot;
            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        }
    }

    // Centering

    private void center() {
        Vec3d pos = new Vec3d(
                mc.player.getBlockX() + 0.5D,
                mc.player.getY(),
                mc.player.getBlockZ() + 0.5D
        );

        if (rangeFromCenter.get() == 0 || Utils.distance(mc.player.getPos(), pos) >= rangeFromCenter.get()) {
            switch (centerType.get()) {
                case TP -> {
                    mc.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.PositionAndOnGround(
                                    pos.getX(),
                                    mc.player.getY(),
                                    pos.getZ(),
                                    mc.player.isOnGround()
                            )
                    );

                    mc.player.updatePosition(pos.getX(), mc.player.getY(), pos.getZ());
                }

                case Fast -> {
                    mc.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.PositionAndOnGround(
                                    mc.player.getX() + (pos.getX() - mc.player.getX()) / 2,
                                    mc.player.getY(),
                                    mc.player.getZ() + (pos.getZ() - mc.player.getZ()) / 2,
                                    mc.player.isOnGround()
                            )
                    );

                    mc.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.PositionAndOnGround(
                                    pos.getX(),
                                    mc.player.getY(),
                                    pos.getZ(),
                                    mc.player.isOnGround()
                            )
                    );

                    mc.player.updatePosition(pos.getX(), mc.player.getY(), pos.getZ());
                }

                case Smooth -> {
                    double deltaX = clamp(pos.getX() - mc.player.getX(), -0.05, 0.05);
                    double deltaZ = clamp(pos.getX() - mc.player.getZ(), -0.05, 0.05);

                    double speed = centerSpeed.get() + 1.0D;

                    double x = deltaX * speed;
                    double z = deltaZ * speed;

                    mc.player.setVelocity(x, mc.player.getVelocity().y, z);
                }
            }
        }
    }

    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    // Constants

    private final List<BlockPos> possibles = new ArrayList<>() {{
        add(new BlockPos(0, 0, 0));
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, -1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(1, 0, -1));
    }};

    // Enums

    public enum PlaceMode {
        Normal,
        Packet
    }

    public enum StabilizeType {
        None,
        Wait
    }

    public enum CenterMode {
        Never,
        OnActivate,
        Always,
        Incomplete
    }

    public enum CenterType {
        Normal,
        Smooth,
        Fast,
        TP
    }

    public enum CrystalBreakMode {
        None,
        Quick,
        Normal,
        Both
    }
}
