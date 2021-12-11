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
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ColorPlace extends Module {
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

    private final Setting<Integer> renderticks = sgRender.add(new IntSetting.Builder()
        .name("ticks")
        .description("How many ticks it should take for a block to disappear.")
        .defaultValue(8)
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
            .defaultValue(new SettingColor(255, 255, 255, 15))
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

    public ColorPlace() {
        super(VectorAddon.CATEGORY, "color-place", "Outlines your placed and broken blocks.");
    }

    @Override
    public void onActivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    // Events

    @EventHandler
    private void onPlace(PlaceBlockEvent event) {
        if (!place.get()) return;
        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(event.block)) return;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(event.block)) return;

        renderBlocks.add(renderBlockPool.get().set(event.blockPos));
    }

    @EventHandler
    private void onBreak(BreakBlockEvent event) {
        if (!breaking.get()) return;
        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(VectorUtils.getBlock(event.blockPos))) return;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(VectorUtils.getBlock(event.blockPos))) return;

        renderBlocks.add(renderBlockPool.get().set(event.blockPos));
    }

    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (!interact.get()) return;
        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(VectorUtils.getBlock(event.result.getBlockPos()))) return;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(VectorUtils.getBlock(event.result.getBlockPos()))) return;

        if (mc.player.getMainHandStack().getItem() instanceof BlockItem) return;

        renderBlocks.add(renderBlockPool.get().set(event.result.getBlockPos()));
    }

    // Ticking fade animation

    @EventHandler
    private void onTick(TickEvent.Post event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);
    }

    // Rendering

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(b -> -b.ticks));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = renderticks.get();

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            event.renderer.box(pos, sides, lines, shapeMode, 0);

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}
