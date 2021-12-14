package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;

public class AutoCityPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The radius players can be in to be targeted.")
            .defaultValue(5)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestDistance)
            .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
            .name("packet-mine")
            .description("Will mine the blocks using packets.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> silentSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("silent-switch")
            .description("Changes slots only clientside.")
            .defaultValue(false)
            .build()
    );

    // Pause

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses cev breaker when eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses cev breaker when drinking.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders a hand swing animation.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the obsidian will be placed.")
            .defaultValue(true)
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
            .defaultValue(new SettingColor(140, 245, 165, 25))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(140, 245, 165, 255))
            .visible(render::get)
            .build()
    );

    public static ArrayList<BlockPos> surrPositions = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    public static ArrayList<BlockPos> allPositions = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(-1, 0, -1));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(0, 0, -1));
        add(new BlockPos(1, 0, -1));

        add(new BlockPos(1, -1, 0));
        add(new BlockPos(1, -1, 1));
        add(new BlockPos(-1, -1, 0));
        add(new BlockPos(-1, -1, -1));
        add(new BlockPos(0, -1, 1));
        add(new BlockPos(-1, -1, 1));
        add(new BlockPos(0, -1, -1));
        add(new BlockPos(1, -1, -1));
    }};

    private ArrayList<BlockPos> positions = new ArrayList<>();
    private PlayerEntity target;

    public AutoCityPlus() {
        super(VectorAddon.CATEGORY, "auto-city-plus", "Breaks the targets surround.");
    }

    @Override
    public void onActivate() {
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());

        FindItemResult obby = InvUtils.findInHotbar(Items.OBSIDIAN);
        FindItemResult pick = InvUtils.findInHotbar(stack -> stack.getItem() == Items.DIAMOND_PICKAXE || stack.getItem() == Items.NETHERITE_PICKAXE);
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);

        if (!obby.found()) {
            error("Can't find obsidian in your hotbar, disabling...");
            toggle();
            return;
        }

        if (!pick.found()) {
            error("Can't find a pickaxe in your hotbar, disabling...");
            toggle();
            return;
        }

        if (!crystal.found()) {
            error("Can't find crystals in your hotbar, disabling...");
            toggle();
            return;
        }

        if (PlayerUtils.shouldPause(false, eatPause.get(), drinkPause.get())) return;

        if (target != null) {
            BlockPos pos = target.getBlockPos();

            if (canCity(pos) && shouldCity(pos)) {
                for (BlockPos p : getSurround(pos)) {
                    if (VectorUtils.canBreak(p)) {
                        if (getAir(p).isEmpty()) {
                            for (BlockPos position : getCity(pos)) {
                                if (VectorUtils.canBreak(position)) mine(pos, pick);
                            }
                        }

                        if (!getAir(p).isEmpty() && getCrystal(p) == null) {
                            for (BlockPos position : getAir(p)) {
                                if (VectorUtils.getBlock(position.down()) == Blocks.OBSIDIAN && canInteract(position)) {
                                    interact(position.down(), crystal, Direction.UP);
                                    return;
                                }
                            }
                        }

                        if (getCrystal(p) != null) {
                            mine(p, pick);
                        }
                    }
                }
            }
        }
    }

    private ArrayList<BlockPos> getAir(BlockPos pos) {
        ArrayList<BlockPos> air = new ArrayList<>();

        for (BlockPos position : allPositions) {
            if (VectorUtils.getBlockState(pos.add(position)).isAir()) air.add(pos.add(position));
        }

        air.sort(Comparator.comparingDouble(pos2 -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos2))));
        return air;
    }

    private ArrayList<BlockPos> getCity(BlockPos pos) {
        ArrayList<BlockPos> city = new ArrayList<>();

        for (BlockPos position : allPositions) {
            if (!VectorUtils.getBlockState(pos.add(position)).isAir() && VectorUtils.canBreak(pos.add(position))) city.add(pos.add(position));
        }

        city.sort(Comparator.comparingDouble(pos2 -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos2))));
        return city;
    }

    private ArrayList<BlockPos> getSurround(BlockPos pos) {
        ArrayList<BlockPos> surr = new ArrayList<>();

        for (BlockPos position : surrPositions) {
            if (!VectorUtils.getBlockState(pos.add(position)).isAir()) surr.add(pos.add(position));
        }

        surr.sort(Comparator.comparingDouble(pos2 -> VectorUtils.distance(mc.player.getPos(), Utils.vec3d(pos2))));
        return surr;
    }

    private boolean canCity(BlockPos pos) {
        int i = 0;

        for (BlockPos position : surrPositions) {
            if (VectorUtils.canBreak(pos.add(position))) i++;
        }

        return i != 0;
    }

    private boolean shouldCity(BlockPos pos) {
        int i = 0;

        for (BlockPos position : surrPositions) {
            if (VectorUtils.getBlockState(pos.add(position)).isAir()) i++;
        }

        return i == 0;
    }

    private boolean canInteract(BlockPos pos) {
        Entity entity = null;

        for (Entity e : mc.world.getEntities()) {
            if (pos.equals(e.getBlockPos())) entity = e;
        }

        return entity == null && VectorUtils.getBlockState(pos).isAir();
    }

    private void mine(BlockPos pos, FindItemResult pick) {
        if (!packetMine.get()) {
            BlockUtils.breakBlock(pos, renderSwing.get());
        } else {
            if (mc.player.getInventory().selectedSlot != pick.getSlot()) InvUtils.swap(pick.getSlot(), false);

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }

    private void attack(EndCrystalEntity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
    }

    private void interact(BlockPos pos, FindItemResult item, Direction direction) {
        if (item == null) return;
        if (item.getHand() == null || !silentSwitch.get()) InvUtils.swap(item.getSlot(), false);

        if (silentSwitch.get() && item.getHand() != null) {
            mc.interactionManager.interactBlock(mc.player, mc.world, item.getHand(), new BlockHitResult(mc.player.getPos(), direction, pos, true));
        } else {
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), direction, pos, true));
        }
    }

    private Entity getCrystal(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            Vec3d epos = entity.getPos();
            if (entity instanceof EndCrystalEntity
                    && VectorUtils.distance(Utils.vec3d(pos), epos) <= 2.5
                    && (entity.getBlockPos().getY() == pos.getY()
                    || entity.getBlockPos().getY() == pos.getY() - 1)) {
                return entity;
            }
        }

        return null;
    }

    @Override
    public String getInfoString() {
        return target != null ? target.getEntityName() : null;
    }

    // Render

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (positions.isEmpty() || !render.get()) return;

        for (BlockPos pos : positions) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            positions.remove(pos);
        }
    }
}
