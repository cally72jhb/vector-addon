package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Burrow;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class SurroundPlusPlus extends Module {
    public enum Mode {
        Toggle,
        Hold
    }

    public enum ToggleMode {
        Both,
        Double,
        Top
    }

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiBreak = settings.createGroup("Anti Break");
    private final SettingGroup sgSurroundBreak = settings.createGroup("Anti Surround Break");
    private final SettingGroup sgGhost = settings.createGroup("Anti Ghost Blocks");
    private final SettingGroup sgAuto = settings.createGroup("Auto");
    private final SettingGroup sgExtra = settings.createGroup("Extra");
    private final SettingGroup sgBurrow = settings.createGroup("Burrow");
    private final SettingGroup sgReplace = settings.createGroup("Replace");
    private final SettingGroup sgInterfering = settings.createGroup("Interfering");
    private final SettingGroup sgKeybind = settings.createGroup("Keybind");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("double-height")
        .description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Works only when you standing on blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyWhenSneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-sneaking")
        .description("Places blocks only after sneaking.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
        .name("turn-off")
        .description("Toggles off when all blocks are placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Center> center = sgGeneral.add(new EnumSetting.Builder<Center>()
        .name("center")
        .description("Teleports you to the center of the block.")
        .defaultValue(Center.OnActivate)
        .build()
    );

    private final Setting<Boolean> onEntity = sgGeneral.add(new BoolSetting.Builder()
        .name("place-on-entity")
        .description("Places blocks even if there is a entity.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> stopMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-movement")
        .description("Stops your movement for a fwe seconds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-jump")
        .description("Automatically disables when you jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnYChange = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-y-change")
        .description("Automatically disables when your y level (step, jumping, atc).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shortInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("short-info")
        .description("Uses the first character of the default info instead of the whole word.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the obsidian being placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks to use for surround.")
        .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay, in ticks, between block placements.")
            .min(0)
            .defaultValue(0)
            .build()
    );

    // Anti Break

    private final Setting<Boolean> antiButton = sgAntiBreak.add(new BoolSetting.Builder()
        .name("anti-button")
        .description("Anti Button Trap.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> breakButton = sgAntiBreak.add(new BoolSetting.Builder()
        .name("break-button")
        .description("Whether or not to break the button.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> antiButtonBlocks = sgAntiBreak.add(new BlockListSetting.Builder()
        .name("anti-button-blocks")
        .description("Blocks to break if they are in your surounnd.")
        .defaultValue(new ArrayList<>() {{
            add(Blocks.ACACIA_BUTTON);
            add(Blocks.BIRCH_BUTTON);
            add(Blocks.CRIMSON_BUTTON);
            add(Blocks.DARK_OAK_BUTTON);
            add(Blocks.JUNGLE_BUTTON);
            add(Blocks.OAK_BUTTON);
            add(Blocks.POLISHED_BLACKSTONE_BUTTON);
            add(Blocks.SPRUCE_BUTTON);
            add(Blocks.STONE_BUTTON);
            add(Blocks.WARPED_BUTTON);
            add(Blocks.TRIPWIRE);
        }})
        .build()
    );

    private final Setting<Double> buttonRange = sgAntiBreak.add(new DoubleSetting.Builder()
        .name("button-range")
        .description("The range for anti button to scan for blocks and surround them.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    // Replace

    private final Setting<Boolean> replace = sgReplace.add(new BoolSetting.Builder()
        .name("replace")
        .description("When a block is broken it will be replaced with obby again.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreOwn = sgReplace.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Ignores the blocks that are broken by you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> replaceRange = sgReplace.add(new DoubleSetting.Builder()
        .name("replace-range")
        .description("The range for replace.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    // Auto

    private final Setting<Boolean> autoToggle = sgAuto.add(new BoolSetting.Builder()
        .name("auto-toggle")
        .description("Toggles double-height or top when certain blocks are placed near you.")
        .defaultValue(false)
        .build()
    );

    public final Setting<ToggleMode> autoToggleMode = sgAuto.add(new EnumSetting.Builder<ToggleMode>()
        .name("auto-toggle-mode")
        .description("The mode for toggling.")
        .defaultValue(ToggleMode.Both)
        .build()
    );

    private final Setting<List<Block>> toggleBlocks = sgAuto.add(new BlockListSetting.Builder()
        .name("auto-toggle-blocks")
        .description("Blocks to toggle automatically when placed near you.")
        .defaultValue(new ArrayList<>() {{
            add(Blocks.RESPAWN_ANCHOR);
            add(Blocks.BLACK_BED);
            add(Blocks.BLUE_BED);
            add(Blocks.BROWN_BED);
            add(Blocks.CYAN_BED);
            add(Blocks.GRAY_BED);
            add(Blocks.GREEN_BED);
            add(Blocks.LIGHT_BLUE_BED);
            add(Blocks.LIGHT_GRAY_BED);
            add(Blocks.LIME_BED);
            add(Blocks.MAGENTA_BED);
            add(Blocks.ORANGE_BED);
            add(Blocks.PINK_BED);
            add(Blocks.PURPLE_BED);
            add(Blocks.RED_BED);
            add(Blocks.WHITE_BED);
            add(Blocks.YELLOW_BED);
        }})
        .build()
    );

    private final Setting<Double> toggleRange = sgAuto.add(new DoubleSetting.Builder()
        .name("auto-toggle-range")
        .description("The range for auto-toggle to trigger.")
        .defaultValue(5)
        .min(0)
        .max(25)
        .build()
    );

    // Extra Surround

    private final Setting<Boolean> extraLayer = sgExtra.add(new BoolSetting.Builder()
        .name("extra-layer")
        .description("Toggles a extra secure layer around your default surround.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onbreak = sgExtra.add(new BoolSetting.Builder()
        .name("on-break")
        .description("Toggles the extra layer when a surround block is broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fullBreak = sgExtra.add(new BoolSetting.Builder()
        .name("full-break")
        .description("Requires the block to be fully broken.")
        .defaultValue(true)
        .visible(onbreak::get)
        .build()
    );

    private final Setting<Integer> attempts = sgExtra.add(new IntSetting.Builder()
        .name("attempts")
        .description("How often to try to place the block.")
        .defaultValue(2)
        .min(0)
        .max(20)
        .visible(onbreak::get)
        .build()
    );

    // Burrow

    private final Setting<Boolean> burrow = sgBurrow.add(new BoolSetting.Builder()
        .name("burrow")
        .description("Burrow when your surround is fully build.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgBurrow.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only burrows if your in a hole.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> burrowDelay = sgBurrow.add(new IntSetting.Builder()
        .name("burrow-delay")
        .description("How long to wait after enabling.")
        .defaultValue(3)
        .build()
    );

    private final Setting<Boolean> burrowHelp = sgBurrow.add(new BoolSetting.Builder()
        .name("burrow-help")
        .description("Allows you to walk in your burrow block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noInteract = sgBurrow.add(new BoolSetting.Builder()
        .name("no-interact")
        .description("Ignores anvils completely.")
        .defaultValue(true)
        .build()
    );

    // Interfering

    private final Setting<Boolean> interfering = sgInterfering.add(new BoolSetting.Builder()
        .name("interfering")
        .description("Remove interfering crystals before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> interferingDelay = sgInterfering.add(new IntSetting.Builder()
        .name("interfering-delay")
        .description("The delay when hitting crystals that are in the way.")
        .defaultValue(5)
        .min(0)
        .build()
    );

    private final Setting<Double> interferingRange = sgInterfering.add(new DoubleSetting.Builder()
        .name("interfering-range")
        .description("The range to search for crystals around the surroundblock.")
        .defaultValue(2)
        .min(0)
        .build()
    );

    // Keybind

    private final Setting<Boolean> message = sgKeybind.add(new BoolSetting.Builder()
        .name("keybind-message")
        .description("Whether or not to send you a message when toggled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> keybindMode = sgKeybind.add(new EnumSetting.Builder<Mode>()
        .name("keybind-mode")
        .description("Weather to toggle or hold the keybinds for them to function.")
        .defaultValue(Mode.Toggle)
        .build()
    );

    private final Setting<Keybind> forceDoubleHeight = sgKeybind.add(new KeybindSetting.Builder()
        .name("force-double")
        .description("Places a extra row when pressed.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceTop = sgKeybind.add(new KeybindSetting.Builder()
        .name("force-top")
        .description("Places a block over your head when pressed.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceCev = sgKeybind.add(new KeybindSetting.Builder()
        .name("force-cev")
        .description("Places a block over your head to protect you from getting cevbreaked.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceExtra = sgKeybind.add(new KeybindSetting.Builder()
        .name("force-extra")
        .description("Enables / Disables a extra layer.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceProtect = sgKeybind.add(new KeybindSetting.Builder()
        .name("force-protect")
        .description("Enables / Disables protect mode.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceBurrow = sgKeybind.add(new KeybindSetting.Builder()
        .name("force-burrow")
        .description("Enables / Disables a auto burrow.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    // Anti Surround Break

    private final Setting<Boolean> flight = sgSurroundBreak.add(new BoolSetting.Builder()
        .name("flight")
        .description("Replaces the block beneth you if broken so that you don't fall down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> protect = sgSurroundBreak.add(new BoolSetting.Builder()
        .name("protect")
        .description("Breaks crystals near you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> safe = sgSurroundBreak.add(new BoolSetting.Builder()
        .name("safe")
        .description("Doesn't break crystals that can kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> preplace = sgSurroundBreak.add(new BoolSetting.Builder()
        .name("protect-replace")
        .description("Breaks crystals near you and places a block there were the crystal was.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> protectrange = sgSurroundBreak.add(new DoubleSetting.Builder()
        .name("protect-range")
        .description("The range for protect to trigger.")
        .defaultValue(2)
        .min(0)
        .build()
    );

    private final Setting<Integer> maxdamage = sgSurroundBreak.add(new IntSetting.Builder()
        .name("max-damage")
        .description("The maximum damage that can be dealt when breaking crystals.")
        .defaultValue(5)
        .min(0)
        .build()
    );

    private final Setting<Integer> protectdelay = sgSurroundBreak.add(new IntSetting.Builder()
        .name("protect-delay")
        .description("The delay when hitting crystals.")
        .defaultValue(10)
        .min(0)
        .build()
    );

    // Anti Ghost Blocks

    private final Setting<Boolean> invalid = sgGhost.add(new BoolSetting.Builder()
        .name("invalid")
        .description("Remove invalid blocks around you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> ghostblocks = sgGhost.add(new BlockListSetting.Builder()
        .name("ghost-blocks")
        .description("Blocks to remove.")
        .defaultValue(new ArrayList<>(0) {{
            add(Blocks.OBSIDIAN);
            add(Blocks.ENDER_CHEST);
            add(Blocks.NETHERITE_BLOCK);
            add(Blocks.CRYING_OBSIDIAN);
            add(Blocks.RESPAWN_ANCHOR);
            add(Blocks.ANVIL);
        }})
        .build()
    );

    // Pause

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses Surround when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses Surround when drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses Surround when mining.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders hand swinging client side.")
        .defaultValue(true)
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
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(255, 255, 255, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    // Positions

    public static ArrayList<BlockPos> surroundPositions = new ArrayList<BlockPos>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    public static ArrayList<BlockPos> extraSurround = new ArrayList<BlockPos>() {{
        add(new BlockPos(2, 0, 0));
        add(new BlockPos(-2, 0, 0));
        add(new BlockPos(0, 0, 2));
        add(new BlockPos(0, 0, -2));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(1, 0, -1));
        add(new BlockPos(-1, 0, -1));
    }};

    private final ArrayList<Vec3d> doubleSurround = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    private ArrayList<Vec3d> getSurround() {
        ArrayList<Vec3d> surround = new ArrayList<Vec3d>() {{
            add(new Vec3d(1, 0, 0));
            add(new Vec3d(-1, 0, 0));
            add(new Vec3d(0, 0, 1));
            add(new Vec3d(0, 0, -1));
        }};
        if (doubleHeight.get()) surround.addAll(doubleSurround);
        return surround;
    }

    // Init Stuff

    private Step step;
    private boolean active;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private Entity ghostentity;

    private boolean stoped;

    private final List<BlockPos> bblocks = new ArrayList<>();
    private List<BlockPos> placepositions;
    private List<BlockPos> cpositions;
    private List<BlockPos> epositions;

    private int btimer;
    private int timer;
    private int breaktimer;
    private int burrowtimer;

    boolean forced = false;
    boolean forcet = false;
    boolean forcec = false;
    boolean forcep = false;
    boolean forcee = false;
    boolean forceb = false;

    private boolean return_;

    public SurroundPlusPlus() {
        super(VectorAddon.CATEGORY, "surround-plus-plus", "Surrounds you in blocks to prevent you from taking crystal damage.");
    }

    // Burrow

    @EventHandler
    public void isCube(CollisionShapeEvent event) {
        if ((burrowHelp.get() && mc.player.getBlockPos().equals(event.pos))
            || (burrowHelp.get() && mc.player.getBlockPos().up(1).equals(event.pos)) && !mc.player.isInSwimmingPose()) {
            event.shape = VoxelShapes.empty();
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if ((mc.player.getBlockPos().equals(event.result.getBlockPos())
            || mc.player.getBlockPos().up().equals(event.result.getBlockPos()))
            && VectorUtils.getBlock(event.result.getBlockPos()) instanceof AnvilBlock
            && noInteract.get()) event.cancel();
    }

    // For Automation & Replace & Flight

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos pos = event.pos;

        if (replace.get() && !ignoreOwn.get() && event.newState.isAir() && VectorUtils.isSurroundBlock(event.oldState.getBlock())) {
            placepositions.add(pos);
        }

        if (onbreak.get() && fullBreak.get() && VectorUtils.isSurroundBlock(event.oldState.getBlock()) && isSurround(pos)) {
            for (BlockPos position : surroundPositions) {
                if (VectorUtils.canPlace(pos.add(position), true)) epositions.add(pos.add(position));
            }
        }

        if (autoToggle.get() && (toggleBlocks.get().contains(event.newState.getBlock()) || toggleBlocks.get().contains(event.oldState.getBlock())) && VectorUtils.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX(), pos.getY(), pos.getZ()) <= toggleRange.get()) {
            switch (autoToggleMode.get()) {
                case Both -> {
                    forcet = true;
                    forced = true;
                }
                case Double -> forced = true;
                case Top    -> forcet = true;
            }
        }
    }

    // For StopMovement

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (stopMovement.get() && stoped) {
            ((IVec3d) event.movement).setXZ(0, 0);
            stoped = false;
        }
    }

    // For Toggling

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!(mc.currentScreen == null)) return;

        if (forceDoubleHeight.get().isPressed() && keybindMode.get() == Mode.Toggle || doubleHeight.get()) {
            forced = !forced;
            if (message.get()) ChatUtils.sendMsg(Text.of(forced ? "Activated Double Height" : "Disabled Double Height"));
        }
        if (forceTop.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            forcet = !forcet;
            if (message.get()) ChatUtils.sendMsg(Text.of(forcet ? "Activated Top Block" : "Disabled Top Block"));
        }
        if (forceCev.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            forcec = !forcec;
            if (message.get()) ChatUtils.sendMsg(Text.of(forcec ? "Activated Anti Cev" : "Disabled Anti Cev"));
        }
        if (forceProtect.get().isPressed() && keybindMode.get() == Mode.Toggle || protect.get()) {
            forcep = !forcep;
            if (message.get()) ChatUtils.sendMsg(Text.of(forcep ? "Activated Protect" : "Disabled Protect"));
        }
        if (forceExtra.get().isPressed() && keybindMode.get() == Mode.Toggle || extraLayer.get()) {
            forcee = !forcee;
            if (message.get()) ChatUtils.sendMsg(Text.of(forcee ? "Activated Extra Layer" : "Disabled Extra Layer"));
        }
        if (forceBurrow.get().isPressed() && keybindMode.get() == Mode.Toggle || burrow.get()) {
            forceb = !forceb;
            if (message.get()) ChatUtils.sendMsg(Text.of(forceb ? "Activated Auto Burrow" : "Disabled Auto Burrow"));
        }
    }



    @Override
    public void onActivate() {
        if (center.get() == Center.OnActivate) PlayerUtils.centerPlayer();
        if (stopMovement.get()) mc.player.setVelocity(0, mc.player.getVelocity().getY(), 0);

        forced = false;
        forcet = false;
        forcec = false;
        forcep = false;
        forcee = false;
        forceb = false;

        stoped = true;

        btimer = 0;
        timer = 0;
        burrowtimer = 0;

        breaktimer = 0;

        ghostentity = null;
        placepositions = new ArrayList<>();
        cpositions = new ArrayList<>();
        epositions = new ArrayList<>();

        bblocks.clear();

        step = Modules.get().get(Step.class);
        active = step.isActive();

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        if (mc.world != null && mc.player != null && active && !step.isActive()) step.toggle();

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    // Main Loop

    @EventHandler
    private void onTickPre(TickEvent.Post event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (center.get() == Center.Always) PlayerUtils.centerPlayer();

        if ((disableOnJump.get() && (mc.options.keyJump.isPressed() || mc.player.input.jumping)) || (disableOnYChange.get() && mc.player.prevY < mc.player.getY())) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (onlyWhenSneaking.get() && !mc.options.keySneak.isPressed()) return;

        // Burrow Help
        if (step.isActive()) step.toggle();

        // Pause Surround
        boolean pause = PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get());
        boolean screen = mc.currentScreen == null;

        if (pause) return;

        // Extra Layer & Replace

        for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            Entity entity = mc.world.getEntityById(value.getActorId());
            BlockPos pos = value.getPos();

            if (entity instanceof PlayerEntity && entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity)) {
                if (onbreak.get() && !fullBreak.get()) {
                    if (isSurround(pos)) {
                        for (BlockPos position : surroundPositions) {
                            if (VectorUtils.canPlace(pos.add(position), true)
                                && !pos.equals(mc.player.getBlockPos())) epositions.add(pos.add(position));
                        }
                    }
                }

                if (replace.get() && ignoreOwn.get() && VectorUtils.isSurroundBlock(pos)) {
                    placepositions.add(pos);
                }
            }
        }

        // Protect & Anti Ghost Blocks

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                BlockPos crystalPos = entity.getBlockPos();

                if (invalid.get() && ghostblocks.get().contains(mc.world.getBlockState(crystalPos).getBlock())) {
                    if (ghostentity != null && !ghostentity.isLiving()) mc.world.setBlockState(crystalPos, Blocks.AIR.getDefaultState());

                    ghostentity = entity;
                }

                if (timer > protectdelay.get() && isDangerousCrystal(crystalPos)) {
                    if (forcep || forceProtect.get().isPressed() && screen || protect.get()) {
                        if (safe.get() && DamageUtils.crystalDamage(mc.player, entity.getPos()) < maxdamage.get()) {
                            breakCrystal(entity);
                            timer = 0;
                            if (preplace.get()) cpositions.add(crystalPos);
                            return;
                        }
                    }
                }
            }
        }

        // Replace

        if (placepositions != null && !placepositions.isEmpty()) {
            for (BlockPos pos : placepositions) {
                if (BlockUtils.canPlace(pos) && VectorUtils.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= replaceRange.get()) {
                    BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100);
                    renderBlocks.add(renderBlockPool.get().set(pos));
                    placepositions.remove(pos);
                    return;
                }
            }
        }

        // Protect

        if (cpositions != null && !cpositions.isEmpty()) {
            for (BlockPos pos : cpositions) {
                if (VectorUtils.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= protectrange.get()) {
                    BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100);
                    renderBlocks.add(renderBlockPool.get().set(pos));
                    cpositions.remove(pos);
                    return;
                }
            }
        }

        // Extra Layer

        try {
            if (epositions != null && !epositions.isEmpty()) {
                for (BlockPos pos : epositions) {
                    if (VectorUtils.canPlace(pos) && !pos.equals(mc.player.getBlockPos())) {
                        BlockUtils.place(pos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 100);
                        renderBlocks.add(renderBlockPool.get().set(pos));
                        epositions.remove(pos);
                        return;
                    } else if (!VectorUtils.canPlace(pos)) {
                        epositions.remove(pos);
                    }
                }
            }
        } catch (Exception e) {

        }

        // Anti Button

        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        if (antiButton.get() && PlayerUtils.isInHole(false)) {
            for (BlockPos pos : surroundPositions) {
                BlockState state = VectorUtils.getBlockState(pos);
                if (BlockUtils.canBreak(pos, state)
                    || !(VectorUtils.squaredDistance(pX, pY, pZ,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5) > buttonRange.get()))  {
                    for (BlockPos position : surroundPositions) {
                        if (antiButtonBlocks.get().contains(VectorUtils.getBlock(position))) {
                            for (BlockPos pos1 : surroundPositions) {
                                bblocks.add(position.add(pos1));
                            }
                        }
                    }
                }
            }

            if (btimer > 2) {
                if (!bblocks.isEmpty() && VectorUtils.canPlace(bblocks.get(0), true)) {
                    place(bblocks.get(0));
                    bblocks.remove(0);
                    btimer = 0;
                }
            }

            btimer++;
        }

        timer++;

        // Place
        return_ = false;

        // Bottom
        place(0, -1, 0);
        if (return_) return;

        // Sides
        place(1, 0, 0);
        if (return_) return;
        place(-1, 0, 0);
        if (return_) return;
        place(0, 0, 1);
        if (return_) return;
        place(0, 0, -1);
        if (return_) return;

        // Sides up
        boolean doubleHeightPlaced = false;
        if (doubleHeight.get() || (forceDoubleHeight.get().isPressed() && screen) || forced) {
            boolean p6 = place(1, 1, 0);
            if (return_) return;
            boolean p7 = place(-1, 1, 0);
            if (return_) return;
            boolean p8 = place(0, 1, 1);
            if (return_) return;
            boolean p9 = place(0, 1, -1);
            if (return_) return;

            if (p6 && p7 && p8 && p9) doubleHeightPlaced = true;
        }

        // Top Block
        if ((forceTop.get().isPressed() && screen) || forcet) {
            place(0, 2, 0);
            if (return_) return;
        }

        // Anti Cev
        if ((forceCev.get().isPressed() && screen) || forcec) {
            place(0, 3, 0);
            if (return_) return;
        }

        if (center.get() == Center.Incomplete) PlayerUtils.centerPlayer();

        // Extra Layer
        if ((forceExtra.get().isPressed() && screen) || forcee || extraLayer.get()) {
            for (BlockPos pos : extraSurround) {
                BlockPos.Mutable blockPos = new BlockPos.Mutable(
                    mc.player.getX() + pos.getX(),
                    mc.player.getY() + pos.getY(),
                    mc.player.getZ() + pos.getZ());

                if (VectorUtils.canPlace(blockPos)) {
                    place(pos.getX(), pos.getY(), pos.getZ());
                    if (return_) return;
                }
            }
        }

        // Burrow
        if (VectorUtils.getBlockState(mc.player.getBlockPos()).isAir()) {
            boolean b = !onlyInHole.get() || PlayerUtils.isInHole(false);
            for (Entity entity : mc.world.getEntities()) {
                if (entity.getBlockPos().equals(mc.player.getBlockPos())) b = false;
            }

            if (b) {
                if (VectorUtils.canPlace(mc.player.getBlockPos(), false) && (burrow.get() || forceb) && burrowtimer > burrowDelay.get()) {
                    Modules.get().get(Burrow.class).toggle();
                    burrowtimer = 0;
                }

                burrowtimer++;
            }
        }

        // Auto turn off
        if (turnOff.get() && PlayerUtils.isInHole(doubleHeightPlaced || doubleHeight.get())) {
            toggle();
        }
    }

    // Render

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.forEach(renderBlock -> {
            renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get());
        });
    }

    // Info Sting

    @Override
    public String getInfoString() {
        String info = "";

        if (!shortInfo.get()) {
            if (forced) info += "[Double] ";
            if (forcet) info += "[Top] ";
            if (forcec) info += "[Cev] ";
            if (forcep) info += "[Protect] ";
            if (forcee) info += "[Extra] ";
            if (forceb) info += "[Burrow]";
        } else if (shortInfo.get()) {
            if (forced) info += "[D] ";
            if (forcet) info += "[T] ";
            if (forcec) info += "[C] ";
            if (forcep) info += "[P] ";
            if (forcee) info += "[E] ";
            if (forceb) info += "[B]";
        }

        return !info.equals("") ? info : null;
    }

    // Other Stuff

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR ||
            block == Blocks.ANVIL;
    }

    private boolean place(int x, int y, int z) {
        BlockPos pos = mc.player.getBlockPos().add(x, y, z);

        if (!VectorUtils.getBlockState(pos).getMaterial().isReplaceable()) return true;
        if (!VectorUtils.canPlace(pos)) return false;

        boolean placed = BlockUtils.place(pos,
            InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))),
            rotate.get(), 100, !onEntity.get());

        if (placed) {
            renderBlocks.add(renderBlockPool.get().set(pos));
        }

        // Check if the block is being mined
        boolean beingMined = false;
        for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            if (value.getPos().equals(pos)) {
                beingMined = true;
                break;
            }
        }

        boolean isThreat = mc.world.getBlockState(pos).getMaterial().isReplaceable() || beingMined;

        // If the block is air or is being mined, destroy nearby crystals to be safe
        if (interfering.get() && !placed && isThreat) {
            Box box = new Box(
                    pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
            );

            Predicate<Entity> entityPredicate = entity -> entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(mc.player, entity.getPos()) < PlayerUtils.getTotalHealth();

            for (Entity crystal : mc.world.getOtherEntities(null, box, entityPredicate)) {
                if (rotate.get()) {
                    Rotations.rotate(Rotations.getPitch(crystal), Rotations.getYaw(crystal), () -> {
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                    });
                }
                else {
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                }

                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        return placed;
    }

    private void place(BlockPos pos) {
        if (!VectorUtils.canPlace(pos, true)) return;
        BlockUtils.place(pos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, true);
        renderBlocks.add(renderBlockPool.get().set(pos));
    }

    private boolean isOtherSurround(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player) {
                BlockPos epos = entity.getBlockPos();

                for (BlockPos position : surroundPositions) {
                    if (epos.add(position) == pos && VectorUtils.isSurroundBlock(epos.add(position))) return true;
                }
            }
        }

        return false;
    }

    private boolean isSurround(BlockPos pos) {
        BlockPos epos = mc.player.getBlockPos();

        for (BlockPos position : surroundPositions) {
            if (epos.add(position).equals(pos) && VectorUtils.isSurroundBlock(epos.add(position))) return true;
        }

        return false;
    }

    private Entity getCrystal(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            Vec3d epos = entity.getPos();
            if (entity instanceof EndCrystalEntity && VectorUtils.distance(Utils.vec3d(pos), epos) <= 2.5) return entity;
        }

        return null;
    }

    private void breakCrystal(Entity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));

        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;

        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    private boolean isDangerousCrystal(BlockPos pos) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b : getSurround()) {
            BlockPos bpos = ppos.add(b.x, b.y, b.z);
            if (!pos.equals(bpos) && VectorUtils.distanceBetweenXZ(bpos, pos) <= 2) return true;
        }
        return false;
    }

    // For Rendering

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            if (pos == null) return null;

            pos.set(blockPos);
            ticks = 8;

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
