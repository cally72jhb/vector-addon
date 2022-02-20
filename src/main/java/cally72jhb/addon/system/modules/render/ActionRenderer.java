package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActionRenderer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .defaultValue(new ArrayList<>())
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
        .name("advanced")
        .description("Shows a more advanced outline on different types of shape blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
        .name("place")
        .description("Whether or not to render placing blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breaking = sgGeneral.add(new BoolSetting.Builder()
        .name("break")
        .description("Whether or not to render breaking blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> interact = sgGeneral.add(new BoolSetting.Builder()
        .name("interact")
        .description("Whether or not to render on interact with block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> renderTicks = sgRender.add(new IntSetting.Builder()
        .name("ticks")
        .description("How many ticks it should take for a block to disappear.")
        .defaultValue(10)
        .min(1)
        .sliderMax(8)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    public ActionRenderer() {
        super(VectorAddon.Misc, "action-render", "Outlines interacted, placed and broken blocks.");
    }

    @Override
    public void onActivate() {
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

    // Events

    @EventHandler
    private void onPlace(PlaceBlockEvent event) {
        if (!place.get() || shouldRender(event.block)) return;

        RenderBlock block = renderBlockPool.get().set(event.blockPos);

        if (!renderBlocks.contains(block)) renderBlocks.add(block);
    }

    @EventHandler
    private void onBreak(BreakBlockEvent event) {
        if (!breaking.get() || shouldRender(VectorUtils.getBlock(event.blockPos))) return;

        RenderBlock block = renderBlockPool.get().set(event.blockPos);

        if (!renderBlocks.contains(block)) renderBlocks.add(block);
    }

    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (mc.player == null || event.hand == null || mc.player.getStackInHand(event.hand) == null) return;
        if (!interact.get() || shouldRender(VectorUtils.getBlock(event.result.getBlockPos()))) return;
        if (mc.player.getStackInHand(event.hand).getItem() instanceof BlockItem) return;

        RenderBlock block = renderBlockPool.get().set(event.result.getBlockPos());

        if (!renderBlocks.contains(block)) renderBlocks.add(block);
    }

    // Ticking fade animation

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
    }

    // Rendering

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderBlocks.isEmpty()) {
            renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
            renderBlocks.forEach(block -> block.render(event, shapeMode.get()));
        }
    }

    private boolean shouldRender(Block block) {
        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist || !blocks.get().contains(block)) return false;
        return true;
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public BlockState state;
        public int ticks;

        private Color sides;
        private Color lines;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            state = VectorUtils.getBlockState(blockPos);
            ticks = renderTicks.get();

            sides = new Color(sideColor.get());
            lines = new Color(lineColor.get());

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            if (!advanced.get()) {
                event.renderer.box(pos, sides, lines, shapeMode, 0);
            } else {
                VoxelShape shape = state.getOutlineShape(mc.world, pos);

                if (shape == null || shape.isEmpty()) return;

                if (shapeMode == ShapeMode.Both || shapeMode == ShapeMode.Lines) {
                    shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
                        event.renderer.line(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ, pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ, lines);
                    });
                }

                if (shapeMode == ShapeMode.Both || shapeMode == ShapeMode.Sides) {
                    for (Box box : shape.getBoundingBoxes()) {
                        event.renderer.box(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ, pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ, sides, lines, shapeMode, 0);
                    }
                }
            }

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}
