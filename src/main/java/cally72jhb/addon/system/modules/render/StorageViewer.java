package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class StorageViewer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to display.")
        .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
        .build()
    );

    private final Setting<Boolean> closeInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("close-inventory")
        .description("Closes the inventory after opening the container.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignore = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore")
        .description("Ignores chests that can't be opened.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnce = sgGeneral.add(new BoolSetting.Builder()
        .name("only-once")
        .description("Interacts with every storage block only once.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> closest = sgGeneral.add(new BoolSetting.Builder()
        .name("closest")
        .description("Interacts at the closest possible side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> visible = sgGeneral.add(new BoolSetting.Builder()
        .name("visible")
        .description("Interacts at the closest possible side that is visible.")
        .defaultValue(true)
        .visible(closest::get)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait in ticks after testing again.")
        .defaultValue(10)
        .min(0)
        .sliderMin(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("What randomness to use for jitter.")
        .defaultValue(5)
        .min(1)
        .max(7.5)
        .sliderMin(2.5)
        .sliderMax(7.5)
        .build()
    );

    // Render

    private final Setting<Double> yOffset = sgRender.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("The y offset of the container.")
        .defaultValue(-0.35)
        .sliderMin(-0.5)
        .sliderMax(0.25)
        .build()
    );

    private final Setting<Double> containerScale = sgRender.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the inventory.")
        .defaultValue(1.25)
        .min(0.25)
        .sliderMin(0.75)
        .sliderMax(2)
        .build()
    );

    private final Setting<Boolean> distanceScale = sgRender.add(new BoolSetting.Builder()
        .name("distance-scale")
        .description("Makes the container smaller the further away you are.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> factor = sgRender.add(new DoubleSetting.Builder()
        .name("scale-factor")
        .description("How much the container is scaled down per block.")
        .defaultValue(5)
        .min(1)
        .visible(distanceScale::get)
        .build()
    );

    private final Setting<Boolean> customColor = sgRender.add(new BoolSetting.Builder()
        .name("custom-color")
        .description("Whether or not to use a custom color for the container.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> containerColor = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the container.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(customColor::get)
        .build()
    );

    private static final Identifier TEXTURE = new Identifier("meteor-client", "assets/textures/container.png");
    private final ItemStack[] inventory = new ItemStack[9 * 3];

    private HashMap<BlockPos, List<ItemStack>> chests;
    private ArrayList<BlockPos> positions;

    private BlockPos pos;
    private boolean close;
    private boolean chest;
    private int timer;
    private int id;

    public StorageViewer() {
        super(VectorAddon.MISC, "storage-viewer", "Shows you the inventory of storage blocks.");
    }

    @Override
    public void onActivate() {
        positions = new ArrayList<>();
        chests = new HashMap<>();

        close = false;
        chest = false;
        timer = 0;
        id = -1;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (id != -1) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(id));
            id = -1;
        }

        if (mc.currentScreen instanceof HandledScreen<?> screen && !(screen instanceof InventoryScreen)
            && !(screen instanceof CreativeInventoryScreen) && closeInventory.get() && pos == null && close) {
            screen.onClose();

            mc.setScreen(null);
            id = screen.getScreenHandler().syncId;

            close = false;
            chest = false;
        }

        if (timer >= delay.get() || delay.get() == 0) {
            timer = 0;

            ArrayList<BlockEntity> entities = new ArrayList<>();

            for (BlockEntity entity : Utils.blockEntities()) if (storageBlocks.get().contains(entity.getType())) entities.add(entity);

            if (!entities.isEmpty()) {
                if (closest.get()) entities.sort(Comparator.comparingDouble(entity -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(entity.getPos()))));

                for (BlockEntity entity : entities) {
                    if ((!onlyOnce.get() || onlyOnce.get() && !chests.containsKey(entity.getPos())) && (!ignore.get() || ignore.get() && !positions.contains(entity.getPos()))) {
                        if (storageBlocks.get().contains(entity.getType()) && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(entity.getPos())) <= range.get()) {
                            BlockPos position = entity.getPos();
                            FindItemResult item = InvUtils.findInHotbar(stack -> stack.getUseAction() == UseAction.NONE);

                            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket((item.getHand() != null && item.found()) ? item.getHand() : Hand.OFF_HAND, new BlockHitResult(Utils.vec3d(position), getDir(position), position, false)));

                            chest = VectorUtils.getBlock(position) instanceof ChestBlock && ChestBlock.getDoubleBlockType(VectorUtils.getBlockState(position)) == DoubleBlockProperties.Type.SECOND;

                            close = true;
                            pos = position;
                            positions.add(position);

                            return;
                        }
                    }
                }
            }
        } else {
            timer++;
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        close = !chests.containsKey(pos) || chests.get(pos) == null;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof InventoryS2CPacket packet && pos != null) {
            List<ItemStack> content = chest ? packet.getContents().subList(27, packet.getContents().size()) : packet.getContents();

            if (chests.containsKey(pos)) chests.replace(pos, content);
            else chests.putIfAbsent(pos, content);

            pos = null;
            close = true;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (chests.isEmpty()) return;

        for (BlockPos pos : new HashSet<>(chests.keySet())) {
            BlockEntity entity = mc.world.getBlockEntity(pos);

            if (entity == null || !World.isValid(pos) || VectorUtils.distanceBetweenXZ(mc.player.getPos(), Vec3d.ofCenter(pos)) >= mc.options.viewDistance * 17) {
                chests.remove(pos);
            } else if (chests.containsKey(pos) && storageBlocks.get().contains(mc.world.getBlockEntity(pos).getType())) {
                Vec3 vec = new Vec3();

                vec = vec.set(Vec3d.ofCenter(pos.up()));
                vec = vec.add(0, yOffset.get(), 0);

                double scale = containerScale.get();

                Freecam cam = Modules.get().get(Freecam.class);

                if (distanceScale.get()) {
                    Vec3d vec3d = cam.isActive() ? new Vec3d(cam.getX(event.tickDelta), cam.getY(event.tickDelta), cam.getZ(event.tickDelta)) : mc.player.getEyePos();
                    scale /= VectorUtils.distance(vec3d, new Vec3d(vec.x, vec.y, vec.z)) / factor.get();
                }

                if (scale > containerScale.get()) scale = containerScale.get();
                if (scale < containerScale.get() / 2.5) scale = containerScale.get() / 2.5;

                if (NametagUtils.to2D(vec, scale)) {
                    NametagUtils.begin(vec);

                    double x = -((176 * scale) / 2);
                    double y = -((67 * scale) / 2);

                    drawBackground((int) x, (int) y, scale, pos);

                    Arrays.fill(inventory, ItemStack.EMPTY);

                    List<ItemStack> items = new ArrayList<>(chests.get(pos));

                    for (int i = 0; i < 27; i++) {
                        if (i < items.size()) inventory[i] = items.get(i);
                    }

                    for (int row = 0; row < 3; row++) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = inventory[row * 9 + i];
                            if (stack == null || stack.isEmpty()) continue;

                            RenderUtils.drawItem(stack, (int) (x + (8 + i * 18) * scale), (int) (y + (7 + row * 18) * scale), scale, true);
                        }
                    }

                    NametagUtils.end();
                }
            }
        }
    }

    // Utils

    private Direction getClosestSide(BlockPos pos) {
        ArrayList<Direction> sides = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (closest.get()) {
                if (visible.get() && !VectorUtils.isSolid(pos.offset(dir))) sides.add(dir);
            } else {
                sides.add(dir);
            }
        }

        if (sides.isEmpty()) return null;
        sides.sort(Comparator.comparingDouble(side -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos.offset(side)))));
        return sides.get(0);
    }

    private Direction getDir(BlockPos pos) {
        return closest.get() ? (getClosestSide(pos) == null ? Direction.UP : getClosestSide(pos)) : Direction.UP;
    }

    private void drawBackground(int x, int y, double scale, BlockPos pos) {
        GL.bindTexture(TEXTURE);

        Color color = customColor.get() ? containerColor.get() : Utils.getShulkerColor(VectorUtils.getBlock(pos).asItem().getDefaultStack());

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, 176 * scale, 67 * scale, color);
        Renderer2D.TEXTURE.render(null);
    }
}
