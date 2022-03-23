package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class InstaMinePlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range blocks need to be inside to be broken.")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .sliderMax(8)
        .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The delay between the attempted breaks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> keepOnTooFar = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-on-too-far")
        .description("Keeps the breaking position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> keepOnInvalid = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-on-invalid")
        .description("Keeps the breaking position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> keepTicks = sgGeneral.add(new IntSetting.Builder()
        .name("keep-ticks")
        .description("How long the position is kept after beeing unable to break.")
        .defaultValue(75)
        .min(50)
        .sliderMax(100)
        .visible(keepOnInvalid::get)
        .build()
    );

    private final Setting<Boolean> flexDirection = sgGeneral.add(new BoolSetting.Builder()
        .name("flex-direction")
        .description("Will mine at the closest possible direction.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyVisible = sgGeneral.add(new BoolSetting.Builder()
        .name("only-visible")
        .description("Will only mine at visible directions.")
        .defaultValue(true)
        .visible(flexDirection::get)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Only mines blocks with a certain hardness.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxHardness = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-hardness")
        .description("The maximum hardness a block can have for it to be instant mined.")
        .defaultValue(0.3)
        .min(0.05)
        .sliderMax(1)
        .visible(bypass::get)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the blocks being mined server side.")
        .defaultValue(true)
        .build()
    );


    // Render


    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay on the block being broken.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand swing animation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> advanced = sgRender.add(new BoolSetting.Builder()
        .name("advanced")
        .description("Shows a more advanced outline on different types of shape blocks.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(245, 65, 65, 10))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(245, 65, 65, 255))
        .visible(render::get)
        .build()
    );

    private int ticks;
    private int keeping;

    private BlockPos pos = null;
    private BlockState state = null;
    private Direction direction = null;

    public InstaMinePlus() {
        super(Categories.Misc, "insta-mine-plus", "Attempts to instantly mine blocks.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        keeping = 0;

        pos = null;
        state = null;
        direction = null;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!bypass.get() || bypass.get() && VectorUtils.getBlock(event.blockPos).getHardness() <= maxHardness.get()) {
            direction = event.direction;
            state = VectorUtils.getBlockState(event.blockPos);
            pos = event.blockPos;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (pos != null) {
            if (!shouldMine()) {
                keeping++;
            } else {
                keeping = 0;
            }

            if (keepOnInvalid.get() && keeping >= keepTicks.get()
                || !keepOnTooFar.get() && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) > range.get()
                || bypass.get() && VectorUtils.getBlock(pos).getHardness() > maxHardness.get()) {
                pos = null;
            }

            if (pos != null && ticks >= tickDelay.get()
                && (!keepOnInvalid.get() || keepOnInvalid.get() && keeping < keepTicks.get())
                && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= range.get()) {

                ticks = 0;

                if (flexDirection.get()) {
                    double bestDistance = Integer.MAX_VALUE;
                    Direction bestDirection = null;

                    for (Direction dir : Direction.values()) {
                        if (!onlyVisible.get() || onlyVisible.get() && VectorUtils.getBlockState(pos.offset(dir)).getOutlineShape(mc.world, pos.offset(dir)) != VoxelShapes.fullCube()) {
                            double distance = VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos.offset(dir)));

                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestDirection = dir;
                            }
                        }
                    }

                    if (bestDirection != null && bestDistance < range.get() + 0.5) direction = bestDirection;
                }

                if (shouldMine() && checkHardness(pos)) {
                    state = VectorUtils.getBlockState(pos);

                    if (rotate.get()) {
                        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction)));
                    } else {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                    }

                    if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                    else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            } else {
                ticks++;
            }
        }
    }

    private boolean checkHardness(BlockPos pos) {
        ItemStack stack = mc.player.getMainHandStack();
        float hardness = VectorUtils.getBlock(pos).getHardness();

        return stack != null && stack.getItem() instanceof ToolItem && VectorUtils.getBlock(pos).getHardness() > 0
            && ((hardness / stack.getItem().getMiningSpeedMultiplier(mc.player.getMainHandStack(), VectorUtils.getBlockState(pos))) <= maxHardness.get()
            || hardness <= maxHardness.get());
    }

    private boolean shouldMine() {
        return pos != null && World.isValid(pos) && canBreak(pos);
    }

    public boolean canBreak(BlockPos pos) {
        BlockState state = VectorUtils.getBlockState(pos);

        if (!mc.player.isCreative() && state.getHardness(mc.world, pos) <= 0) return false;
        return state.getOutlineShape(mc.world, pos) != VoxelShapes.empty();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && pos != null && state != null && direction != null) {
            if (!advanced.get() || state.getOutlineShape(mc.world, pos).isEmpty()) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            } else {
                VoxelShape shape = state.getOutlineShape(mc.world, pos);

                if (shape == null || shape.isEmpty()) return;

                if (shapeMode.get().lines()) {
                    shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
                        event.renderer.line(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ, pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ, lineColor.get());
                    });
                }

                if (shapeMode.get().sides()) {
                    for (Box box : shape.getBoundingBoxes()) {
                        event.renderer.box(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ, pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ, sideColor.get(), lineColor.get(), ShapeMode.Sides, 0);
                    }
                }
            }
        }
    }
}
