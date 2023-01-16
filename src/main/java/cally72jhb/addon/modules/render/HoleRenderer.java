package cally72jhb.addon.modules.render;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.Utils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HoleRenderer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");


    // General


    private final Setting<Double> horizontalDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-distance")
            .description("The horizontal radius around you in which holes are rendered.")
            .defaultValue(15)
            .sliderMin(10)
            .sliderMax(20)
            .min(0)
            .build()
    );

    private final Setting<Double> verticalDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-distance")
            .description("The vertical radius around you in which holes are rendered.")
            .defaultValue(10)
            .sliderMin(8)
            .sliderMax(15)
            .min(0)
            .build()
    );

    private final Setting<UpdateHoles> updateHoles = sgGeneral.add(new EnumSetting.Builder<UpdateHoles>()
            .name("update-holes")
            .description("When to update the holes to check if they are still valid.")
            .defaultValue(UpdateHoles.Render)
            .build()
    );

    private final Setting<Boolean> allowHalf = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-half")
            .description("Renders holes which are hard to get inside.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> webs = sgGeneral.add(new BoolSetting.Builder()
            .name("webs")
            .description("Whether to show holes that have webs inside of them.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-own")
            .description("Ignores the hole you are sitting in.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreBurrow = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-burrow")
            .description("Ignores your burrow block.")
            .defaultValue(true)
            .build()
    );


    // Render


    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the blocks being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fadeIn = sgRender.add(new BoolSetting.Builder()
            .name("fade-in")
            .description("Fades in the hole when rendering a new one.")
            .defaultValue(true)
            .visible(render::get)
            .build()
    );

    private final Setting<Boolean> fadeOut = sgRender.add(new BoolSetting.Builder()
            .name("fade-out")
            .description("Fades out the hole when removing it.")
            .defaultValue(true)
            .visible(render::get)
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

    private final Setting<SettingColor> bedrockSidesTop = sgRender.add(new ColorSetting.Builder()
            .name("bedrock-sides-top")
            .description("The top side color for holes that are completely bedrock.")
            .defaultValue(new SettingColor(100, 255, 0, 0))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> bedrockSidesBottom = sgRender.add(new ColorSetting.Builder()
            .name("bedrock-sides-bottom")
            .description("The bottom side color for holes that are completely bedrock.")
            .defaultValue(new SettingColor(100, 255, 0, 25))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> bedrockLinesTop = sgRender.add(new ColorSetting.Builder()
            .name("bedrock-lines-top")
            .description("The top line color for holes that are completely bedrock.")
            .defaultValue(new SettingColor(100, 255, 0, 0))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> bedrockLinesBottom = sgRender.add(new ColorSetting.Builder()
            .name("bedrock-lines-bottom")
            .description("The bottom line color for holes that are completely bedrock.")
            .defaultValue(new SettingColor(100, 255, 0, 200))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> obsidianSidesTop = sgRender.add(new ColorSetting.Builder()
            .name("obsidian-sides-top")
            .description("The top side color for holes that are completely obsidian.")
            .defaultValue(new SettingColor(255, 0, 0, 0))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> obsidianSidesBottom = sgRender.add(new ColorSetting.Builder()
            .name("obsidian-sides-bottom")
            .description("The bottom side color for holes that are completely obsidian.")
            .defaultValue(new SettingColor(255, 0, 0, 25))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> obsidianLinesTop = sgRender.add(new ColorSetting.Builder()
            .name("obsidian-lines-top")
            .description("The top line color for holes that are completely obsidian.")
            .defaultValue(new SettingColor(255, 0, 0, 0))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> obsidianLinesBottom = sgRender.add(new ColorSetting.Builder()
            .name("obsidian-lines-bottom")
            .description("The bottom line color for holes that are completely obsidian.")
            .defaultValue(new SettingColor(255, 0, 0, 200))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> mixedSidesTop = sgRender.add(new ColorSetting.Builder()
            .name("mixed-sides-top")
            .description("The top side color for holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(255, 127, 0, 0))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> mixedSidesBottom = sgRender.add(new ColorSetting.Builder()
            .name("mixed-sides-bottom")
            .description("The bottom side color for holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(255, 127, 0, 25))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> mixedLinesTop = sgRender.add(new ColorSetting.Builder()
            .name("mixed-lines-top")
            .description("The top line color for holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(255, 127, 0, 0))
            .onChanged(changed -> updateAll())
            .build()
    );

    private final Setting<SettingColor> mixedLinesBottom = sgRender.add(new ColorSetting.Builder()
            .name("mixed-lines-bottom")
            .description("The bottom line color for holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(255, 127, 0, 200))
            .onChanged(changed -> updateAll())
            .build()
    );

    // Constructor

    public HoleRenderer() {
        super(VectorAddon.CATEGORY, "hole-renderer", "Renders close safe-holes.");
    }

    // Variables

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    // Overrides

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

    // Listeners

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        List<Hole> holes = new ArrayList<>();

        int pX = mc.player.getBlockX();
        int pY = mc.player.getBlockY();
        int pZ = mc.player.getBlockZ();

        int horizontal = (int) Math.floor(horizontalDistance.get());
        int vertical = (int) Math.floor(verticalDistance.get());

        // Calculating Holes Around Player

        for (int x = pX - horizontal; x <= pX + horizontal; x++) {
            for (int z = pZ - horizontal; z <= pZ + horizontal; z++) {
                for (int y = (Math.max(pY - vertical, mc.world.getBottomY() - 1)); y <= Math.min(pY + vertical, mc.world.getTopY()); y++) {
                    int dX = Math.abs(x - pX);
                    int dY = Math.abs(y - pY);
                    int dZ = Math.abs(z - pZ);

                    if (dX <= horizontalDistance.get() && dY <= verticalDistance.get() && dZ <= horizontalDistance.get()) {
                        BlockPos pos = new BlockPos(x, y, z);

                        if (isValidHole(pos, true) && isValidHole(pos.up(), false)) {
                            int air = 0;
                            int surr = 0;

                            int bedrock = 0;
                            int obsidian = 0;

                            BlockPos second = null;
                            Direction excludeDir = null;

                            if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK) {
                                bedrock++;
                            } else {
                                obsidian++;
                            }

                            for (CardinalDirection cardinal : CardinalDirection.values()) {
                                Direction direction = cardinal.toDirection();

                                if (isValidHole(pos.offset(direction), true) && isValidHole(pos.offset(direction).up(), false)) {
                                    int surrounded = 0;

                                    if (mc.world.getBlockState(pos.offset(direction).down()).getBlock() == Blocks.BEDROCK) {
                                        bedrock++;
                                    } else {
                                        obsidian++;
                                    }

                                    for (CardinalDirection dir : CardinalDirection.values()) {
                                        if (mc.world.getBlockState(pos.offset(direction).offset(dir.toDirection())).getBlock().getBlastResistance() >= 600.0F) {
                                            surrounded++;

                                            if (mc.world.getBlockState(pos.offset(direction).offset(dir.toDirection())).getBlock() == Blocks.BEDROCK) {
                                                bedrock++;
                                            } else {
                                                obsidian++;
                                            }
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

                                    if (mc.world.getBlockState(pos.offset(direction)).getBlock() == Blocks.BEDROCK) {
                                        bedrock++;
                                    } else {
                                        obsidian++;
                                    }
                                }
                            }

                            if (air == 1 && surr >= 3 && (!allowHalf.get() || (isValidHole(pos.up(2), false) || second != null && isValidHole(second.up(2), false))) || air == 0 && surr >= 4 && isValidHole(pos.up(2), false)) {
                                HoleType type = bedrock == 0 ? HoleType.Obsidian : bedrock > 0 && obsidian > 0 ? HoleType.Mixed : HoleType.Bedrock;
                                holes.add(new Hole(type, pos, excludeDir != null ? second : null, second != null ? excludeDir : null));
                            }
                        }
                    }
                }
            }
        }

        // Evaluating Collected Holes

        if (!holes.isEmpty()) {
            holes.sort(Comparator.comparingDouble(hole -> Utils.distance(mc.player.getPos(), Vec3d.ofCenter(hole.pos1))));

            for (Hole hole : holes) {
                if (hole.isDouble()) {
                    if (ignoreOwn.get() && (mc.player.getBlockPos().equals(hole.pos1) || mc.player.getBlockPos().equals(hole.pos2))
                            || ignoreBurrow.get() && (mc.player.getBlockPos().up().equals(hole.pos1) || mc.player.getBlockPos().up().equals(hole.pos2))) {

                        for (RenderBlock block : renderBlocks) {
                            if (hole.pos1.equals(block.pos) || hole.pos2.equals(block.pos)) {
                                block.invalidate();
                            }
                        }
                    } else {
                        if (canRender(hole.pos1)
                                && Utils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(hole.pos1)) <= horizontalDistance.get()
                                && Utils.distanceY(mc.player.getY(), hole.pos1.getY() + 0.5) <= verticalDistance.get()) {

                            renderBlocks.add(renderBlockPool.get().set(hole.type, hole.pos1, Dir.get(hole.direction)));
                        } else {
                            for (RenderBlock block : renderBlocks) {
                                if (block.pos.equals(hole.pos1)) {
                                    block.update(hole.type, Dir.get(hole.direction));
                                }
                            }
                        }

                        if (canRender(hole.pos2)
                                && Utils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(hole.pos2)) <= horizontalDistance.get()
                                && Utils.distanceY(mc.player.getY(), hole.pos2.getY() + 0.5) <= verticalDistance.get()) {

                            renderBlocks.add(renderBlockPool.get().set(hole.type, hole.pos2, Dir.get(hole.direction.getOpposite())));
                        } else {
                            for (RenderBlock block : renderBlocks) {
                                if (block.pos.equals(hole.pos2)) {
                                    block.update(hole.type, Dir.get(hole.direction.getOpposite()));
                                }
                            }
                        }
                    }
                } else {
                    if (ignoreOwn.get() && (mc.player.getBlockPos().equals(hole.pos1)) || ignoreBurrow.get() && mc.player.getBlockPos().up().equals(hole.pos1)) {
                        for (RenderBlock block : renderBlocks) {
                            if (hole.pos1.equals(block.pos)) {
                                block.invalidate();
                            }
                        }
                    } else if (canRender(hole.pos1)
                            && Utils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(hole.pos1)) <= horizontalDistance.get()
                            && Utils.distanceY(mc.player.getY(), hole.pos1.getY() + 0.5) <= verticalDistance.get()) {

                        renderBlocks.add(renderBlockPool.get().set(hole.type, hole.pos1, 0));
                    } else {
                        for (RenderBlock block : renderBlocks) {
                            if (block.pos.equals(hole.pos1)) {
                                block.update(hole.type, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    // Ticking Fade Animation

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(block -> block.ticks <= 0);
        if (updateHoles.get() == UpdateHoles.Tick) renderBlocks.removeIf(RenderBlock::isInvalid);
    }

    // Rendering

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBlocks.isEmpty()) {
            if (updateHoles.get() == UpdateHoles.Render) renderBlocks.removeIf(RenderBlock::isInvalid);
            renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
            renderBlocks.forEach(block -> block.render(event, shapeMode.get()));
        }
    }

    // Utils

    private boolean isValidHole(BlockPos pos, boolean checkDown) {
        return mc.world.getBlockState(pos).getMaterial().isReplaceable()
                && (mc.world.getBlockState(pos).getBlock() != Blocks.COBWEB || webs.get())
                && (!checkDown || (mc.world.getBlockState(pos.down()).getBlock().getBlastResistance() >= 600.0F
                && mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()) != null
                && !mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()))
                && (mc.world.getBlockState(pos).getCollisionShape(mc.world, pos) == null
                || mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty());
    }

    private void updateAll() {
        for (RenderBlock block : renderBlocks) {
            block.update(block.type, block.exclude);
        }
    }

    private boolean canRender(BlockPos pos) {
        for (RenderBlock block : renderBlocks) {
            if (block.pos.equals(pos)) {
                return false;
            }
        }

        return true;
    }

    private boolean isHole(BlockPos pos) {
        if (isValidHole(pos, true) && isValidHole(pos.up(), false)) {
            int air = 0;
            int surr = 0;

            BlockPos second = null;

            for (CardinalDirection cardinal : CardinalDirection.values()) {
                Direction direction = cardinal.toDirection();

                if (isValidHole(pos.offset(direction), true) && isValidHole(pos.offset(direction).up(), false)) {
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

            return air == 1 && surr >= 3 && (!allowHalf.get() || (isValidHole(pos.up(2), false) || second != null && isValidHole(second.up(2), false))) || air == 0 && surr >= 4 && isValidHole(pos.up(2), false);
        }

        return false;
    }

    // Constants

    private enum HoleType {
        Bedrock,
        Mixed,
        Obsidian
    }

    public enum UpdateHoles {
        Ignore,
        Render,
        Tick
    }

    // Hole

    private static class Hole {
        public HoleType type;
        public BlockPos pos1;
        public BlockPos pos2;
        public Direction direction;

        public Hole(HoleType type, BlockPos pos1, BlockPos pos2, Direction direction) {
            this.type = type;
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
        public HoleType type;
        public int exclude;
        public int ticks;

        private double height;
        private boolean valid;

        private Color constSidesTop;
        private Color constSidesBottom;
        private Color constLinesTop;
        private Color constLinesBottom;

        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;
        private Color linesBottom;

        public RenderBlock set(HoleType type, BlockPos pos, int exclude) {
            this.pos.set(pos);
            this.ticks = fadeIn.get() ? 1 : renderTicks.get();

            this.valid = true;

            this.update(type, exclude);

            this.height = renderHeight.get();

            return this;
        }

        public void tick() {
            if (Utils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(pos)) > horizontalDistance.get()
                    || Utils.distanceY(mc.player.getY(), pos.getY() + 0.5) > verticalDistance.get()
                    || !isHole(pos.mutableCopy())
                    || !valid) {

                if (fadeOut.get()) {
                    ticks--;
                    height = height - shrinkSpeed.get() <= 0 ? 0 : height - shrinkSpeed.get();
                } else {
                    ticks = 0;
                    height = 0;
                }
            } else {
                if (fadeIn.get() && ticks < renderTicks.get()) {
                    ticks++;
                    height = height + shrinkSpeed.get() > renderHeight.get() ? renderHeight.get() : height + shrinkSpeed.get();
                }
            }
        }

        public void update(HoleType type, int exclude) {
            this.exclude = exclude;
            this.type = type;

            this.constSidesTop = new Color(type == HoleType.Bedrock ? bedrockSidesTop.get() : type == HoleType.Obsidian ? obsidianSidesTop.get() : mixedSidesTop.get());
            this.constSidesBottom = new Color(type == HoleType.Bedrock ? bedrockSidesBottom.get() : type == HoleType.Obsidian ? obsidianSidesBottom.get() : mixedSidesBottom.get());
            this.constLinesTop = new Color(type == HoleType.Bedrock ? bedrockLinesTop.get() : type == HoleType.Obsidian ? obsidianLinesTop.get() : mixedLinesTop.get());
            this.constLinesBottom = new Color(type == HoleType.Bedrock ? bedrockLinesBottom.get() : type == HoleType.Obsidian ? obsidianLinesBottom.get() : mixedLinesBottom.get());

            this.sidesTop = this.constSidesTop;
            this.sidesBottom = this.constSidesBottom;
            this.linesTop = this.constLinesTop;
            this.linesBottom = this.constLinesBottom;
        }

        public boolean isInvalid() {
            return (!fadeOut.get()
                    && (Utils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(pos)) > horizontalDistance.get()
                    || Utils.distanceY(mc.player.getY(), pos.getY() + 0.5) > verticalDistance.get()))
                    || (!fadeOut.get() && (!isHole(pos.mutableCopy()) || !valid));
        }

        public void invalidate() {
            valid = false;
        }

        public void render(Render3DEvent event, ShapeMode shapeMode) {
            Color prevSidesTop = sidesTop.copy();
            Color prevSidesBottom = sidesBottom.copy();
            Color prevLinesTop = linesTop.copy();
            Color prevLinesBottom = linesBottom.copy();

            // Color Fading

            sidesTop.a *= (double) ticks / 8;
            sidesBottom.a *= (double) ticks / 8;
            linesTop.a *= (double) ticks / 8;
            linesBottom.a *= (double) ticks / 8;

            sidesTop = sidesTop.a > constSidesTop.a ? constSidesTop : sidesTop;
            sidesBottom = sidesBottom.a > constSidesBottom.a ? constSidesBottom : sidesBottom;
            linesTop = linesTop.a > constLinesTop.a ? constLinesTop : linesTop;
            linesBottom = linesBottom.a > constLinesBottom.a ? constLinesBottom : linesBottom;

            // Main Rendering

            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            if (shapeMode.lines()) {
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x, y, z, x, y + height, z, linesBottom, linesTop);
                if (Dir.isNot(exclude, Dir.WEST) && Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x, y, z + 1, x, y + height, z + 1, linesBottom, linesTop);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x + 1, y, z, x + 1, y + height, z, linesBottom, linesTop);
                if (Dir.isNot(exclude, Dir.EAST) && Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x + 1, y, z + 1, x + 1, y + height, z + 1, linesBottom, linesTop);

                if (Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x, y, z, x + 1, y, z, linesBottom);
                if (Dir.isNot(exclude, Dir.NORTH)) event.renderer.line(x, y + height, z, x + 1, y + height, z, linesTop);
                if (Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x, y, z + 1, x + 1, y, z + 1, linesBottom);
                if (Dir.isNot(exclude, Dir.SOUTH)) event.renderer.line(x, y + height, z + 1, x + 1, y + height, z + 1, linesTop);

                if (Dir.isNot(exclude, Dir.WEST)) event.renderer.line(x, y, z, x, y, z + 1, linesBottom);
                if (Dir.isNot(exclude, Dir.WEST)) event.renderer.line(x, y + height, z, x, y + height, z + 1, linesTop);
                if (Dir.isNot(exclude, Dir.EAST)) event.renderer.line(x + 1, y, z, x + 1, y, z + 1, linesBottom);
                if (Dir.isNot(exclude, Dir.EAST)) event.renderer.line(x + 1, y + height, z, x + 1, y + height, z + 1, linesTop);
            }

            if (shapeMode.sides()) {
                if (Dir.isNot(exclude, Dir.UP) && topQuad.get()) event.renderer.quad(x, y + height, z, x, y + height, z + 1, x + 1, y + height, z + 1, x + 1, y + height, z, sidesTop);
                if (Dir.isNot(exclude, Dir.DOWN) && bottomQuad.get()) event.renderer.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, sidesBottom);

                if (Dir.isNot(exclude, Dir.NORTH)) event.renderer.gradientQuadVertical(x, y, z, x + 1, y + height, z, sidesTop, sidesBottom);
                if (Dir.isNot(exclude, Dir.SOUTH)) event.renderer.gradientQuadVertical(x, y, z + 1, x + 1, y + height, z + 1, sidesTop, sidesBottom);

                if (Dir.isNot(exclude, Dir.WEST)) event.renderer.gradientQuadVertical(x, y, z, x, y + height, z + 1, sidesTop, sidesBottom);
                if (Dir.isNot(exclude, Dir.EAST)) event.renderer.gradientQuadVertical(x + 1, y, z, x + 1, y + height, z + 1, sidesTop, sidesBottom);
            }

            sidesTop = prevSidesTop.copy();
            sidesBottom = prevSidesBottom.copy();
            linesTop = prevLinesTop.copy();
            linesBottom = prevLinesBottom.copy();
        }
    }
}
