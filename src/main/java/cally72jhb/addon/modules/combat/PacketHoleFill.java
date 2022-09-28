package cally72jhb.addon.modules.combat;

import cally72jhb.addon.utils.Utils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class PacketHoleFill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("The blocks to fill the holes with.")
            .defaultValue(new ArrayList<>() {{
                add(Blocks.ANCIENT_DEBRIS);
                add(Blocks.CRYING_OBSIDIAN);
                add(Blocks.ENDER_CHEST);
                add(Blocks.NETHERITE_BLOCK);
                add(Blocks.OBSIDIAN);
                add(Blocks.RESPAWN_ANCHOR);
            }})
            .build()
    );

    private final Setting<Double> horizontalPlaceRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-place-range")
            .description("The horizontal radius in which blocks can be placed.")
            .defaultValue(3)
            .sliderMin(3)
            .sliderMax(5)
            .min(0)
            .max(10)
            .build()
    );

    private final Setting<Double> verticalPlaceRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-place-range")
            .description("The vertical radius in which blocks can be placed.")
            .defaultValue(3.25)
            .sliderMin(3)
            .sliderMax(5)
            .min(0)
            .max(10)
            .build()
    );

    private final Setting<Boolean> onlyAroundTargets = sgGeneral.add(new BoolSetting.Builder()
            .name("only-around-targets")
            .description("Only fills holes around targets.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> horizontalTargetDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-target-distance")
            .description("The horizontal radius around a target in which holes are filled.")
            .defaultValue(1.5)
            .sliderMin(0.75)
            .sliderMax(2)
            .min(0)
            .max(10)
            .visible(onlyAroundTargets::get)
            .build()
    );

    private final Setting<Double> verticalTargetDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-target-distance")
            .description("The vertical radius around a target in which holes are filled.")
            .defaultValue(2.25)
            .sliderMin(1.5)
            .sliderMax(3)
            .min(0)
            .max(15)
            .visible(onlyAroundTargets::get)
            .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-bocks-per-tick")
            .description("The maximum amount of blocks placed in one tick.")
            .defaultValue(4)
            .sliderMin(3)
            .sliderMax(5)
            .min(1)
            .noSlider()
            .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Fills double holes.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fillBoth = sgGeneral.add(new BoolSetting.Builder()
            .name("fill-both")
            .description("Fills both blocks when a target comes close a double hole.")
            .defaultValue(true)
            .visible(doubles::get)
            .build()
    );

    private final Setting<Boolean> allowHalf = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-half")
            .description("Fills holes that are hard to get inside.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Faces the blocks being placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Swaps back to the previous slot after placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> placeRangeBypass = sgGeneral.add(new BoolSetting.Builder()
            .name("place-range-bypass")
            .description("Interacts at the closest possible position to allow a maximal place range.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> sneakRangeBypass = sgGeneral.add(new BoolSetting.Builder()
            .name("sneak-range-bypass")
            .description("Sneaks to lower your eye position to allow a little more vertical range.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> sneakActivationWindow = sgGeneral.add(new DoubleSetting.Builder()
            .name("sneak-activation-window")
            .description("From what range on to start sneaking when placing blocks.")
            .defaultValue(2.75)
            .sliderMin(1.5)
            .sliderMax(3)
            .min(0)
            .max(15)
            .visible(sneakRangeBypass::get)
            .build()
    );


    // Advanced


    private final Setting<Boolean> ignoreCloseHoles = sgAdvanced.add(new BoolSetting.Builder()
            .name("ignore-close-holes")
            .description("Ignores holes which are close to you.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreSelfInHole = sgAdvanced.add(new BoolSetting.Builder()
            .name("ignore-self-in-hole")
            .description("Ignores the holes around you when your in a safe-hole.")
            .defaultValue(true)
            .visible(ignoreCloseHoles::get)
            .build()
    );

    private final Setting<Double> horizontalIgnoreDistance = sgAdvanced.add(new DoubleSetting.Builder()
            .name("horizontal-ignore-distance")
            .description("The horizontal radius around you in which holes aren't filled.")
            .defaultValue(1)
            .sliderMin(0.75)
            .sliderMax(1.5)
            .min(0)
            .max(5)
            .visible(ignoreCloseHoles::get)
            .build()
    );

    private final Setting<Double> verticalIgnoreDistance = sgAdvanced.add(new DoubleSetting.Builder()
            .name("vertical-ignore-distance")
            .description("The vertical radius around you in which holes aren't filled.")
            .defaultValue(1.25)
            .sliderMin(1)
            .sliderMax(1.5)
            .min(0)
            .max(10)
            .visible(ignoreCloseHoles::get)
            .build()
    );

    private final Setting<Boolean> ignoreCloseFriends = sgAdvanced.add(new BoolSetting.Builder()
            .name("ignore-close-to-friends")
            .description("Ignores holes which are close to your friends.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreFriendInHole = sgAdvanced.add(new BoolSetting.Builder()
            .name("ignore-friend-in-hole")
            .description("Ignores the holes around friends when they are in a safe-hole.")
            .defaultValue(true)
            .visible(ignoreCloseFriends::get)
            .build()
    );

    private final Setting<Double> horizontalFriendDistance = sgAdvanced.add(new DoubleSetting.Builder()
            .name("horizontal-friend-distance")
            .description("The horizontal radius around your friends where holes aren't filled.")
            .defaultValue(1)
            .sliderMin(0.75)
            .sliderMax(1.5)
            .min(0)
            .max(5)
            .visible(ignoreCloseFriends::get)
            .build()
    );

    private final Setting<Double> verticalFriendDistance = sgAdvanced.add(new DoubleSetting.Builder()
            .name("vertical-friend-distance")
            .description("The vertical radius around your friends where holes aren't filled.")
            .defaultValue(1.5)
            .sliderMin(1)
            .sliderMax(2)
            .min(0)
            .max(10)
            .visible(ignoreCloseFriends::get)
            .build()
    );

    private final Setting<Boolean> ignoreOtherInHole = sgAdvanced.add(new BoolSetting.Builder()
            .name("ignore-other-in-hole")
            .description("Ignores the holes around targets when they are in a safe-hole.")
            .defaultValue(true)
            .build()
    );


    // Pause


    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses this module when your eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses this module when your drinking.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses this module when mining.")
            .defaultValue(true)
            .build()
    );


    // Render


    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders a hand-swing animation when you place a block.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the blocks being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> renderTicks = sgRender.add(new IntSetting.Builder()
            .name("ticks")
            .description("How many ticks it should take for a block to disappear.")
            .defaultValue(10)
            .min(1)
            .sliderMin(1)
            .sliderMax(15)
            .visible(render::get)
            .noSlider()
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build()
    );

    private final Setting<Double> renderHeight = sgRender.add(new DoubleSetting.Builder()
            .name("height")
            .description("The height of rendering.")
            .defaultValue(0.75)
            .min(0)
            .sliderMin(0.15)
            .sliderMax(1)
            .visible(render::get)
            .build()
    );

    private final Setting<Double> shrinkSpeed = sgRender.add(new DoubleSetting.Builder()
            .name("shrink-speed")
            .description("How fast the hole shrinks per tick.")
            .defaultValue(0.1)
            .min(0)
            .sliderMin(0)
            .sliderMax(0.25)
            .visible(render::get)
            .build()
    );

    private final Setting<Boolean> topQuad = sgRender.add(new BoolSetting.Builder()
            .name("top-quad")
            .description("Whether to render a quad at the top of the hole.")
            .defaultValue(false)
            .visible(render::get)
            .build()
    );

    private final Setting<Boolean> bottomQuad = sgRender.add(new BoolSetting.Builder()
            .name("bottom-quad")
            .description("Whether to render a quad at the bottom of the hole.")
            .defaultValue(true)
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> linesColorTop = sgRender.add(new ColorSetting.Builder()
            .name("lines-top")
            .description("The top color for the lines of the hole.")
            .defaultValue(new SettingColor(255, 0, 0, 15))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> linesColorBottom = sgRender.add(new ColorSetting.Builder()
            .name("lines-bottom")
            .description("The bottom color for the lines of the hole.")
            .defaultValue(new SettingColor(255, 0, 0, 150))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sidesColorTop = sgRender.add(new ColorSetting.Builder()
            .name("sides-top")
            .description("The top color for the sides of the hole.")
            .defaultValue(new SettingColor(255, 0, 0, 15))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sidesColorBottom = sgRender.add(new ColorSetting.Builder()
            .name("sides-bottom")
            .description("The bottom color for the sides of the hole.")
            .defaultValue(new SettingColor(255, 0, 0, 72))
            .visible(render::get)
            .build()
    );

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private boolean shouldUnsneak;

    public PacketHoleFill() {
        super(Categories.Combat, "packet-hole-fill", "Fills safe holes with obsidian using packets.");
    }

    @Override
    public void onActivate() {
        shouldUnsneak = false;

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @Override
    public void onDeactivate() {
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        FindItemResult item = findInHotbar(stack -> stack.getItem() instanceof BlockItem block && blocks.get().contains(block.getBlock()));

        if (shouldUnsneak) {
            mc.player.setSneaking(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

            shouldUnsneak = false;
        }

        if (item.found() && !shouldPause()) {
            List<Hole> holes = new ArrayList<>();

            boolean rotated = false;

            int pX = mc.player.getBlockX();
            int pY = mc.player.getBlockY();
            int pZ = mc.player.getBlockZ();

            int horizontal = (int) Math.floor(horizontalPlaceRange.get());
            int vertical = (int) Math.floor(verticalPlaceRange.get());

            for (int x = pX - horizontal; x <= pX + horizontal; x++) {
                for (int z = pZ - horizontal; z <= pZ + horizontal; z++) {
                    for (int y = (Math.max(pY - vertical, mc.world.getBottomY() - 1)); y <= Math.min(pY + vertical, mc.world.getTopY()); y++) {
                        int dX = Math.abs(x - pX);
                        int dY = Math.abs(y - pY);
                        int dZ = Math.abs(z - pZ);

                        if (dX <= horizontalPlaceRange.get() && dY <= verticalPlaceRange.get() && dZ <= horizontalPlaceRange.get()) {
                            BlockPos pos = new BlockPos(x, y, z);

                            if (isValidHole(pos, true) && isValidHole(pos.up(), false) && canPlace(pos)) {
                                int air = 0;
                                int surr = 0;

                                BlockPos second = null;
                                Direction excludeDir = null;

                                for (CardinalDirection cardinal : CardinalDirection.values()) {
                                    Direction direction = cardinal.toDirection();

                                    if (doubles.get() && isValidHole(pos.offset(direction), true) && isValidHole(pos.offset(direction).up(), false) && canPlace(pos.offset(direction))) {
                                        int surrounded = 0;

                                        for (CardinalDirection dir : CardinalDirection.values()) {
                                            if (mc.world.getBlockState(pos.offset(direction).offset(dir.toDirection())).getBlock().getBlastResistance() >= 600.0F) {
                                                surrounded++;
                                            }
                                        }

                                        if (surrounded == 3) {
                                            excludeDir = direction;
                                            second = pos.offset(direction);
                                            air++;
                                        } else {
                                            air = 0;
                                        }
                                    } else if (mc.world.getBlockState(pos.offset(direction)).getBlock().getBlastResistance() >= 600.0F) {
                                        surr++;
                                    }
                                }

                                if (doubles.get() ? air == 1 && surr >= 3 && (!allowHalf.get() || (isValidHole(pos.up(2), false) || second != null && isValidHole(second.up(2), false))) || air == 0 && surr >= 4 && isValidHole(pos.up(2), false) : surr >= 4 && isValidHole(pos.up(2), false)) {
                                    holes.add(new Hole(pos, doubles.get() && excludeDir != null ? second : null, doubles.get() && second != null ? excludeDir : null));
                                }
                            }
                        }
                    }
                }
            }

            if (!holes.isEmpty()) {
                holes.sort(Comparator.comparingDouble(hole -> Utils.distance(mc.player.getPos(), Vec3d.ofCenter(hole.pos1))));

                int block = 0;

                for (Hole hole : holes) {
                    boolean fill = !onlyAroundTargets.get();

                    if (onlyAroundTargets.get()) {
                        for (PlayerEntity player : mc.world.getPlayers()) {
                            Vec3d pos = player.getEyePos();

                            if ((player instanceof FakePlayerEntity || !Friends.get().isFriend(player)) && mc.player != player
                                    && (!ignoreOtherInHole.get() || ignoreOtherInHole.get() && !isHole(player.getBlockPos()))

                                    && (hole.isDouble() && doubles.get() && fillBoth.get()
                                    && (Utils.distanceXZ(Vec3d.ofCenter(hole.pos1), pos) <= horizontalTargetDistance.get()
                                    && Utils.distanceY(hole.pos1.getY() + 0.5, pos.getY()) <= verticalTargetDistance.get()
                                    || Utils.distanceXZ(Vec3d.ofCenter(hole.pos2), pos) <= horizontalTargetDistance.get()
                                    && Utils.distanceY(hole.pos2.getY() + 0.5, pos.getY()) <= verticalTargetDistance.get())

                                    || Utils.distanceXZ(Vec3d.ofCenter(hole.pos1), pos) <= horizontalTargetDistance.get()
                                    && Utils.distanceY(hole.pos1.getY() + 0.5, pos.getY()) <= verticalTargetDistance.get())) {

                                fill = true;
                                break;
                            }
                        }
                    }

                    if (ignoreCloseFriends.get()) {
                        for (PlayerEntity player : mc.world.getPlayers()) {
                            if (mc.player != player && Friends.get().isFriend(player)) {
                                Vec3d pos = player.getEyePos();

                                if ((!ignoreFriendInHole.get() || ignoreFriendInHole.get() && !isHole(player.getBlockPos()))

                                        && ((hole.isDouble() && doubles.get() && fillBoth.get()
                                        && (Utils.distanceXZ(pos, Vec3d.ofCenter(hole.pos1)) < horizontalFriendDistance.get()
                                        && Utils.distanceXZ(pos, Vec3d.ofCenter(hole.pos2)) < horizontalFriendDistance.get()
                                        || Utils.distanceY(pos.getY(), hole.pos1.getY() + 0.5) < verticalFriendDistance.get()
                                        && Utils.distanceY(pos.getY(), hole.pos2.getY() + 0.5) < verticalFriendDistance.get()))

                                        || Utils.distanceXZ(pos, Vec3d.ofCenter(hole.pos1)) < horizontalFriendDistance.get()
                                        || Utils.distanceY(pos.getY(), hole.pos1.getY() + 0.5) < verticalFriendDistance.get())) {

                                    fill = false;
                                    break;
                                }
                            }
                        }
                    }

                    Vec3d eyes = mc.player.getEyePos();

                    if (fill && (!ignoreCloseHoles.get() || ignoreCloseHoles.get() && ignoreSelfInHole.get() && isHole(mc.player.getBlockPos())

                            || ignoreCloseHoles.get() && ((hole.isDouble() && doubles.get() && fillBoth.get()
                            && (Utils.distanceXZ(eyes, Vec3d.ofCenter(hole.pos1)) >= horizontalIgnoreDistance.get()
                            && Utils.distanceXZ(eyes, Vec3d.ofCenter(hole.pos2)) >= horizontalIgnoreDistance.get()
                            || Utils.distanceY(eyes.getY(), hole.pos1.getY() + 0.5) >= verticalIgnoreDistance.get()
                            && Utils.distanceY(eyes.getY(), hole.pos2.getY() + 0.5) >= verticalIgnoreDistance.get()))

                            || ((!hole.isDouble() || !doubles.get() || !fillBoth.get())
                            && (Utils.distanceXZ(eyes, Vec3d.ofCenter(hole.pos1)) >= horizontalIgnoreDistance.get()
                            || Utils.distanceY(eyes.getY(), hole.pos1.getY() + 0.5) >= verticalIgnoreDistance.get()))))) {

                        if (hole.isDouble() && doubles.get() && fillBoth.get()) {
                            if (block < maxBlocksPerTick.get()) {
                                place(hole.pos1, item, !rotated ? rotate.get() : false);
                                if (!isRendered(hole.pos1)) renderBlocks.add(renderBlockPool.get().set(hole.pos1, Dir.get(hole.direction)));

                                rotated = true;
                                block++;
                            }

                            if (block < maxBlocksPerTick.get()) {
                                place(hole.pos2, item, !rotated ? rotate.get() : false);
                                if (!isRendered(hole.pos2)) renderBlocks.add(renderBlockPool.get().set(hole.pos2, Dir.get(hole.direction.getOpposite())));

                                rotated = true;
                                block++;
                            }
                        } else {
                            if (block < maxBlocksPerTick.get()) {
                                place(hole.pos1, item, !rotated ? rotate.get() : false);
                                if (!isRendered(hole.pos1)) renderBlocks.add(renderBlockPool.get().set(hole.pos1, 0));

                                rotated = true;
                                block++;
                            }
                        }

                        if (block > maxBlocksPerTick.get()) break;
                    }
                }
            }
        }
    }

    // Ticking fade animation

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
    }

    // Rendering

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBlocks.isEmpty()) {
            renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
            renderBlocks.forEach(block -> block.render(event, shapeMode.get()));
        }
    }

    // Utils

    private boolean isValidHole(BlockPos pos, boolean checkDown) {
        return mc.world.getBlockState(pos).getMaterial().isReplaceable()
                && (!checkDown || (mc.world.getBlockState(pos.down()).getBlock().getBlastResistance() >= 600.0F
                && mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()) != null
                && !mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()))
                && (mc.world.getBlockState(pos).getCollisionShape(mc.world, pos) == null
                || mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty());
    }

    private boolean shouldPause() {
        if (minePause.get() && mc.interactionManager.isBreakingBlock()) return true;
        if (eatPause.get() && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem().isFood() || mc.player.getOffHandStack().getItem().isFood()))) return true;
        return drinkPause.get() && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem() instanceof PotionItem));
    }

    private boolean isRendered(BlockPos pos) {
        for (RenderBlock block : renderBlocks) {
            if (block.pos.equals(pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean isHole(BlockPos pos) {
        if (isValidHole(pos, true) && isValidHole(pos.up(), false)) {
            int air = 0;
            int surr = 0;

            BlockPos second = null;

            for (CardinalDirection cardinal : CardinalDirection.values()) {
                Direction direction = cardinal.toDirection();

                if (doubles.get() && isValidHole(pos.offset(direction), true) && isValidHole(pos.offset(direction).up(), false)) {
                    int surrounded = 0;

                    for (CardinalDirection dir : CardinalDirection.values()) {
                        if (mc.world.getBlockState(pos.offset(direction).offset(dir.toDirection())).getBlock().getBlastResistance() >= 600.0F) {
                            surrounded++;
                        }
                    }

                    if (surrounded == 3) {
                        second = pos.offset(direction);
                        air++;
                    } else {
                        air = 0;
                    }
                } else if (mc.world.getBlockState(pos.offset(direction)).getBlock().getBlastResistance() >= 600.0F) {
                    surr++;
                }
            }

            return doubles.get() ? air == 1 && surr >= 3 && (!allowHalf.get() || (isValidHole(pos.up(2), false) || second != null && isValidHole(second.up(2), false))) || air == 0 && surr >= 4 && isValidHole(pos.up(2), false) : surr >= 4 && isValidHole(pos.up(2), false);
        }

        return false;
    }

    // Finding Items

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

    private void place(BlockPos pos, FindItemResult item, boolean rotate) {
        if (item.isOffhand()) {
            place(pos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate);
        } else if (item.isHotbar()) {
            place(pos, Hand.MAIN_HAND, item.slot(), rotate);
        }
    }

    private void place(BlockPos pos, Hand hand, int slot, boolean rotate) {
        if (slot >= 0 && slot <= 8 || slot == 45) {
            if (canPlace(pos)) {
                Vec3d hitPos = getHitPos(pos);
                Direction side = getSide(pos);
                BlockPos neighbour = getNeighbourPos(pos);

                boolean shouldSneak = !mc.player.isSneaking() && sneakRangeBypass.get() && mc.player.getEyeY() > hitPos.getY() + sneakActivationWindow.get();

                if (shouldSneak) {
                    mc.player.setSneaking(true);
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    shouldUnsneak = true;
                }

                if (rotate) {
                    Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 0, () -> {
                        int prevSlot = mc.player.getInventory().selectedSlot;
                        mc.player.getInventory().selectedSlot = slot;
                        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();

                        place(new BlockHitResult(hitPos, side, neighbour, false), hand);

                        if (swapBack.get()) {
                            mc.player.getInventory().selectedSlot = prevSlot;
                            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
                        }
                    });
                } else {
                    int prevSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = slot;
                    ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();

                    place(new BlockHitResult(hitPos, side, neighbour, false), hand);

                    if (swapBack.get()) {
                        mc.player.getInventory().selectedSlot = prevSlot;
                        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
                    }
                }
            }
        }
    }

    private void place(BlockHitResult result, Hand hand) {
        if (hand != null && result != null && mc.world.getWorldBorder().contains(result.getBlockPos()) && mc.player.getStackInHand(hand).getItem() instanceof BlockItem) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));

            Block block = ((BlockItem) mc.player.getStackInHand(hand).getItem()).getBlock();
            BlockSoundGroup group = block.getSoundGroup(block.getDefaultState());

            mc.getSoundManager().play(new PositionedSoundInstance(group.getPlaceSound(), SoundCategory.BLOCKS, (group.getVolume() + 1.0F) / 8.0F, group.getPitch() * 0.5F, Random.create(), result.getBlockPos()));

            if (renderSwing.get()) {
                mc.player.swingHand(hand);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
        }
    }

    private Vec3d getHitPos(BlockPos pos) {
        Direction side = getPlaceSide(pos);
        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (side != null) {
            side = side.getOpposite();

            if (!placeRangeBypass.get()) {
                hitPos = hitPos.add(
                        side.getOffsetX() == 0 ? 0 : (side.getOffsetX() > 0 ? 0.5 : -0.5),
                        side.getOffsetY() == 0 ? 0 : (side.getOffsetY() > 0 ? 0.5 : -0.5),
                        side.getOffsetZ() == 0 ? 0 : (side.getOffsetZ() > 0 ? 0.5 : -0.5)
                );
            }
        }

        if (placeRangeBypass.get()) {
            Vec3d target = mc.player.getEyePos();

            double x = MathHelper.clamp(target.getX(), pos.getX(), pos.getX() + 1);
            double y = MathHelper.clamp(target.getY(), pos.getY(), pos.getY() + 1);
            double z = MathHelper.clamp(target.getZ(), pos.getZ(), pos.getZ() + 1);

            hitPos = new Vec3d(x, y, z);
        }

        return hitPos;
    }

    private BlockPos getNeighbourPos(BlockPos pos) {
        Direction side = getPlaceSide(pos);
        BlockPos neighbour;

        if (side == null) {
            neighbour = pos;
        } else {
            neighbour = pos.offset(side.getOpposite());
        }

        return neighbour;
    }

    private Direction getSide(BlockPos pos) {
        Direction side = getPlaceSide(pos);

        return side == null ? Direction.UP : side;
    }

    private boolean canPlace(BlockPos pos) {
        if (pos == null || mc.world == null || mc.world.getBottomY() > pos.getY() || mc.world.getTopY() < pos.getY()) return false;
        return mc.world.getBlockState(pos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent());
    }

    private Direction getPlaceSide(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            Direction opposite = side.getOpposite();
            BlockState state = mc.world.getBlockState(neighbor);

            if (!state.getMaterial().isReplaceable() && state.getFluidState().isEmpty() && !Utils.isClickable(mc.world.getBlockState(pos.offset(side)).getBlock())) {
                return opposite;
            }
        }

        return null;
    }

    // Hole

    private class Hole {
        public BlockPos pos1;
        public BlockPos pos2;
        public Direction direction;

        public Hole(BlockPos pos1, BlockPos pos2, Direction direction) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.direction = direction;
        }

        public boolean isDouble() {
            return this.pos1 != null && this.pos2 != null && this.direction != null;
        }
    }

    // Hole Rendering

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int exclude;
        public int ticks;

        private double height;

        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;
        private Color linesBottom;

        public RenderBlock set(BlockPos pos, int exclude) {
            this.pos.set(pos);
            this.exclude = exclude;
            this.ticks = renderTicks.get();

            this.sidesTop = new Color(sidesColorTop.get());
            this.sidesBottom = new Color(sidesColorBottom.get());
            this.linesTop = new Color(linesColorTop.get());
            this.linesBottom = new Color(linesColorBottom.get());

            this.height = renderHeight.get();

            return this;
        }

        public void tick() {
            ticks--;
            height = height - shrinkSpeed.get() < 0 ? 0 : height - shrinkSpeed.get();
        }

        public void render(Render3DEvent event, ShapeMode shapeMode) {
            Color prevSidesTop = sidesTop.copy();
            Color prevSidesBottom = sidesBottom.copy();
            Color prevLinesTop = linesTop.copy();
            Color prevLinesBottom = linesBottom.copy();

            sidesTop.a *= (double) ticks / 8;
            sidesBottom.a *= (double) ticks / 8;
            linesTop.a *= (double) ticks / 8;
            linesBottom.a *= (double) ticks / 8;

            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            if (shapeMode.lines()) {
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x, y, z, x, y + height, z, sidesBottom, sidesTop);
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x, y, z + 1, x, y + height, z + 1, sidesBottom, sidesTop);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x + 1, y, z, x + 1, y + height, z, sidesBottom, sidesTop);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x + 1, y, z + 1, x + 1, y + height, z + 1, sidesBottom, sidesTop);

                if (Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x, y, z, x + 1, y, z, sidesBottom);
                if (Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x, y + height, z, x + 1, y + height, z, sidesTop);
                if (Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x, y, z + 1, x + 1, y, z + 1, sidesBottom);
                if (Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x, y + height, z + 1, x + 1, y + height, z + 1, sidesTop);

                if (Dir.isNot(exclude, Dir.WEST)) event.renderer.line(x, y, z, x, y, z + 1, sidesBottom);
                if (Dir.isNot(exclude, Dir.WEST)) event.renderer.line(x, y + height, z, x, y + height, z + 1, sidesTop);
                if (Dir.isNot(exclude, Dir.EAST)) event.renderer.line(x + 1, y, z, x + 1, y, z + 1, sidesBottom);
                if (Dir.isNot(exclude, Dir.EAST)) event.renderer.line(x + 1, y + height, z, x + 1, y + height, z + 1, sidesTop);
            }

            if (shapeMode.sides()) {
                if (Dir.isNot(exclude, Dir.UP) && topQuad.get()) event.renderer.quad(x, y + height, z, x, y + height, z + 1, x + 1, y + height, z + 1, x + 1, y + height, z, linesTop);
                if (Dir.isNot(exclude, Dir.DOWN) && bottomQuad.get()) event.renderer.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, linesBottom);

                if (Dir.isNot(exclude, Dir.NORTH)) event.renderer.gradientQuadVertical(x, y, z, x + 1, y + height, z, linesTop, linesBottom);
                if (Dir.isNot(exclude, Dir.SOUTH)) event.renderer.gradientQuadVertical(x, y, z + 1, x + 1, y + height, z + 1, linesTop, linesBottom);

                if (Dir.isNot(exclude, Dir.WEST)) event.renderer.gradientQuadVertical(x, y, z, x, y + height, z + 1, linesTop, linesBottom);
                if (Dir.isNot(exclude, Dir.EAST)) event.renderer.gradientQuadVertical(x + 1, y, z, x + 1, y + height, z + 1, linesTop, linesBottom);
            }

            sidesTop = prevSidesTop.copy();
            sidesBottom = prevSidesBottom.copy();
            linesTop = prevLinesTop.copy();
            linesBottom = prevLinesBottom.copy();
        }
    }
}
