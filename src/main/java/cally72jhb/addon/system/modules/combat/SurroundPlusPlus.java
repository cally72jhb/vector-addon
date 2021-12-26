package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.system.modules.movement.StepPlus;
import cally72jhb.addon.system.modules.player.BlinkPlus;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
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
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.systems.modules.movement.ReverseStep;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public class SurroundPlusPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCenter = settings.createGroup("Center");
    private final SettingGroup sgBurrow = settings.createGroup("Burrow");
    private final SettingGroup sgInterfering = settings.createGroup("Interfering");
    private final SettingGroup agAntiButton = settings.createGroup("Anti Button");
    private final SettingGroup sgReplace = settings.createGroup("Replace");
    private final SettingGroup sgExtra = settings.createGroup("Extra Layer");
    private final SettingGroup sgAuto = settings.createGroup("Auto");
    private final SettingGroup sgProtect = settings.createGroup("Protect");
    private final SettingGroup sgAntiBed = settings.createGroup("Anti Bed");
    private final SettingGroup sgKeybindings = settings.createGroup("Keybindings");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks to use for surround.")
        .defaultValue(Blocks.OBSIDIAN)
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to disable while blinking.")
        .defaultValue(new ArrayList<>() {{
            add(Modules.get().get(Step.class));
            add(Modules.get().get(StepPlus.class));
            add(Modules.get().get(ReverseStep.class));
        }})
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in ticks between block placements.")
        .min(0)
        .defaultValue(0)
        .build()
    );

    private final Setting<Boolean> shortInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("short-info")
        .description("Uses the first character of the default info instead of the whole word.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("double-height")
        .description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only surrounds when you are standing on blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnYChange = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-y-change")
        .description("Automatically disables when your y level goes higher.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> yLevel = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-change")
        .description("The minimum distance required to toggle the module.")
        .defaultValue(0.25)
        .min(0)
        .max(5)
        .sliderMin(0)
        .sliderMax(5)
        .visible(toggleOnYChange::get)
        .build()
    );

    private final Setting<Boolean> toggleOnComplete = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-complete")
        .description("Toggles off when all blocks are placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> blink = sgGeneral.add(new BoolSetting.Builder()
        .name("blink")
        .description("Won't surround when you have blink active.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("break")
        .description("Will break blocks that are in your surround.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onEntityPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("on-entity")
        .description("Attempts to place blocks even if entities are in its way.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the obsidian being placed.")
        .defaultValue(false)
        .build()
    );

    // Center

    private final Setting<Center> center = sgCenter.add(new EnumSetting.Builder<Center>()
        .name("center")
        .description("Teleports you to the center of the block.")
        .defaultValue(Center.OnActivate)
        .build()
    );

    private final Setting<SmoothCenter> smoothCenter = sgCenter.add(new EnumSetting.Builder<SmoothCenter>()
        .name("smooth-center")
        .description("How smooth to center you.")
        .defaultValue(SmoothCenter.Default)
        .visible(() -> center.get() != Center.Never)
        .build()
    );

    private final Setting<Double> speed = sgCenter.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The centring speed.")
        .defaultValue(1)
        .min(0)
        .sliderMin(0.75)
        .sliderMax(2)
        .visible(() -> center.get() != Center.Never && smoothCenter.get() != SmoothCenter.None)
        .build()
    );

    private final Setting<Double> unlock = sgCenter.add(new DoubleSetting.Builder()
        .name("unlock-speed")
        .description("At what speed to disable pulling in a direction.")
        .defaultValue(1)
        .min(0.1)
        .max(5)
        .sliderMin(0.1)
        .sliderMax(5)
        .visible(() -> center.get() != Center.Never && smoothCenter.get() != SmoothCenter.None)
        .build()
    );

    private final Setting<CenterMode> centerMode = sgCenter.add(new EnumSetting.Builder<CenterMode>()
        .name("center-mode")
        .description("How to center you.")
        .defaultValue(CenterMode.Fast)
        .visible(() -> center.get() != Center.Never && smoothCenter.get() != SmoothCenter.None)
        .build()
    );

    private final Setting<Integer> packets = sgCenter.add(new IntSetting.Builder()
        .name("packets")
        .description("How many center packets to send.")
        .defaultValue(5)
        .min(2)
        .max(50)
        .sliderMin(2)
        .sliderMax(5)
        .visible(() -> center.get() != Center.Never && smoothCenter.get() != SmoothCenter.None && centerMode.get() == CenterMode.Fast)
        .build()
    );

    private final Setting<Integer> centerDelay = sgCenter.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait in ticks before centering again.")
        .defaultValue(1)
        .min(0)
        .sliderMin(0)
        .sliderMax(5)
        .visible(() -> center.get() != Center.Never && smoothCenter.get() != SmoothCenter.None && centerMode.get() == CenterMode.Fast)
        .build()
    );

    // Keybindings

    private final Setting<Boolean> message = sgKeybindings.add(new BoolSetting.Builder()
        .name("keybind-message")
        .description("Whether or not to send you a message when toggled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> keybindMode = sgKeybindings.add(new EnumSetting.Builder<Mode>()
        .name("keybind-mode")
        .description("Weather to toggle or hold the keybinds for them to function.")
        .defaultValue(Mode.Toggle)
        .build()
    );

    private final Setting<Keybind> forceDoubleHeight = sgKeybindings.add(new KeybindSetting.Builder()
        .name("force-double")
        .description("Places a extra row when pressed.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceTop = sgKeybindings.add(new KeybindSetting.Builder()
        .name("force-top")
        .description("Places a block over your head when pressed.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceCev = sgKeybindings.add(new KeybindSetting.Builder()
        .name("force-cev")
        .description("Places a block over your head to protect you from getting cevbreaked.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceExtra = sgKeybindings.add(new KeybindSetting.Builder()
        .name("force-extra")
        .description("Enables / Disables a extra layer.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> forceProtect = sgKeybindings.add(new KeybindSetting.Builder()
        .name("force-protect")
        .description("Enables / Disables protect mode.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    // Burrow

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
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> interferingReplace = sgInterfering.add(new BoolSetting.Builder()
        .name("interfering-replace")
        .description("Replaces the broken crystals with obsidian.")
        .defaultValue(false)
        .visible(interfering::get)
        .build()
    );

    private final Setting<Boolean> replaceOnce = sgInterfering.add(new BoolSetting.Builder()
        .name("replace-once")
        .description("Replaces the crystals only once.")
        .defaultValue(false)
        .visible(() -> interfering.get() && interferingReplace.get())
        .build()
    );

    private final Setting<Integer> interferingDelay = sgInterfering.add(new IntSetting.Builder()
        .name("interfering-delay")
        .description("The delay when hitting crystals that are in the way.")
        .defaultValue(5)
        .min(0)
        .visible(interfering::get)
        .build()
    );

    // Protect

    private final Setting<Boolean> protectSetting = sgProtect.add(new BoolSetting.Builder()
        .name("protect")
        .description("Breaks crystals near you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> protectReplace = sgProtect.add(new BoolSetting.Builder()
        .name("protect-replace")
        .description("Breaks crystals near you and places a block there were the crystal was.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> safe = sgProtect.add(new BoolSetting.Builder()
        .name("safe")
        .description("Doesn't break crystals that can kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxDamage = sgProtect.add(new IntSetting.Builder()
        .name("max-damage")
        .description("The maximum damage that can be dealt when breaking crystals.")
        .defaultValue(5)
        .min(0)
        .visible(safe::get)
        .build()
    );

    private final Setting<Double> protectRange = sgProtect.add(new DoubleSetting.Builder()
        .name("protect-range")
        .description("The range for protect to trigger.")
        .defaultValue(3.5)
        .min(0)
        .build()
    );

    private final Setting<Integer> protectDelay = sgProtect.add(new IntSetting.Builder()
        .name("protect-delay")
        .description("The delay when hitting crystals.")
        .defaultValue(10)
        .min(0)
        .build()
    );

    // Anti Bed

    private final Setting<Boolean> antiBed = sgAntiBed.add(new BoolSetting.Builder()
        .name("anti-bed")
        .description("Places strings to prevent bedaura.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> string = sgAntiBed.add(new BoolSetting.Builder()
        .name("place-string")
        .description("Places one of the selected blocks in you to prevent bedaura.")
        .defaultValue(false)
        .visible(antiBed::get)
        .build()
    );

    private final Setting<Boolean> breakBed = sgAntiBed.add(new BoolSetting.Builder()
        .name("break-bed")
        .description("Will break the placed bed.")
        .defaultValue(true)
        .visible(antiBed::get)
        .build()
    );

    private final Setting<Boolean> replaceBed = sgAntiBed.add(new BoolSetting.Builder()
        .name("replace-bed")
        .description("Replaces beds with the selected blocks.")
        .defaultValue(true)
        .visible(() -> antiBed.get() && breakBed.get())
        .build()
    );

    private final Setting<Boolean> bedOnlyOnce = sgAntiBed.add(new BoolSetting.Builder()
        .name("replace-only-once")
        .description("Replaces the beds only once.")
        .defaultValue(true)
        .visible(() -> antiBed.get() && breakBed.get() && replaceBed.get())
        .build()
    );

    private final Setting<Double> bedRange = sgAntiBed.add(new DoubleSetting.Builder()
        .name("bed-range")
        .description("The range for anti bed to work.")
        .defaultValue(3.5)
        .min(0)
        .visible(() -> antiBed.get() && breakBed.get())
        .build()
    );

    private final Setting<List<Block>> replaceBlocks = sgAntiBed.add(new BlockListSetting.Builder()
        .name("replace-blocks")
        .description("The blocks to break / replace.")
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
        .visible(() -> antiBed.get() && breakBed.get())
        .build()
    );

    private final Setting<List<Block>> bedBlocks = sgAntiBed.add(new BlockListSetting.Builder()
        .name("bed-blocks")
        .description("Blocks to place after breaking one of the selected.")
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
        .filter(this::bedFilter)
        .visible(() -> antiBed.get() && breakBed.get() && replaceBed.get())
        .build()
    );

    // Anti Button

    private final Setting<Boolean> antiButton = agAntiButton.add(new BoolSetting.Builder()
        .name("anti-button")
        .description("Anti Button Trap.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> breakButton = agAntiButton.add(new BoolSetting.Builder()
        .name("break-button")
        .description("Whether or not to break the button.")
        .defaultValue(false)
        .visible(antiButton::get)
        .build()
    );

    private final Setting<Boolean> fastBreak = agAntiButton.add(new BoolSetting.Builder()
        .name("fast-break")
        .description("Breaks the button before placing around it.")
        .defaultValue(false)
        .visible(() -> antiButton.get() && breakButton.get())
        .build()
    );

    private final Setting<List<Block>> antiButtonBlocks = agAntiButton.add(new BlockListSetting.Builder()
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
        .visible(antiButton::get)
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
        .visible(replace::get)
        .build()
    );

    private final Setting<Double> replaceRange = sgReplace.add(new DoubleSetting.Builder()
        .name("replace-range")
        .description("The range for replace.")
        .defaultValue(3)
        .min(0)
        .visible(replace::get)
        .build()
    );

    // Extra Surround

    private final Setting<Boolean> extraLayer = sgExtra.add(new BoolSetting.Builder()
        .name("extra-layer")
        .description("Toggles a extra secure layer around your default surround.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onBreak = sgExtra.add(new BoolSetting.Builder()
        .name("on-break")
        .description("Toggles the extra layer when a surround block is broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fullBreak = sgExtra.add(new BoolSetting.Builder()
        .name("full-break")
        .description("Requires the block to be fully broken.")
        .defaultValue(true)
        .visible(onBreak::get)
        .build()
    );

    // Auto

    private final Setting<Boolean> autoToggle = sgAuto.add(new BoolSetting.Builder()
        .name("auto-toggle")
        .description("Toggles double-height or top when certain blocks are placed near you.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SurroundPlus.ToggleMode> autoToggleMode = sgAuto.add(new EnumSetting.Builder<SurroundPlus.ToggleMode>()
        .name("auto-toggle-mode")
        .description("The mode for toggling.")
        .defaultValue(SurroundPlus.ToggleMode.Both)
        .visible(autoToggle::get)
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
        .visible(autoToggle::get)
        .build()
    );

    private final Setting<Double> toggleRange = sgAuto.add(new DoubleSetting.Builder()
        .name("auto-toggle-range")
        .description("The range for auto-toggle to trigger.")
        .defaultValue(5)
        .min(0)
        .max(25)
        .visible(autoToggle::get)
        .build()
    );

    // Pause

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses Surround when eating.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses Surround when drinking.")
        .defaultValue(false)
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

    private final Setting<Integer> renderTicks = sgRender.add(new IntSetting.Builder()
        .name("render-ticks")
        .description("How long to render each block.")
        .defaultValue(10)
        .min(1)
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

    private ArrayList<Module> toActivate;
    private ArrayList<BlockPos> bedPositions;
    private ArrayList<BlockPos> placePositions;
    private HashMap<Integer, Integer> crystals;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private boolean complete;
    private boolean centered;

    private int ticks;
    private int centerTicks;
    private int protectTicks;

    private boolean high;
    private boolean top;
    private boolean cev;
    private boolean layer;
    private boolean protect;

    public SurroundPlusPlus() {
        super(VectorAddon.MISC, "surround-plus", "Surrounds you in blocks to prevent you from taking crystal damage.");
    }

    // Initialising

    @Override
    public void onActivate() {
        if (center.get() == Center.OnActivate) center();

        toActivate = new ArrayList<>();
        bedPositions = new ArrayList<>();
        placePositions = new ArrayList<>();
        crystals = new HashMap<>();

        complete = false;
        centered = false;

        high = false;
        top = false;
        cev = false;
        layer = false;
        protect = false;

        ticks = 0;
        centerTicks = 0;
        protectTicks = 0;

        for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();

        if (mc.world == null || mc.player == null) return;

        for (Module module : toActivate) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
    }

    // Info Sting

    @Override
    public String getInfoString() {
        String info = "";

        if (!shortInfo.get()) {
            if (high) info += "[Double] ";
            if (top) info += "[Top] ";
            if (cev) info += "[Cev] ";
            if (protect) info += "[Protect] ";
            if (layer) info += "[Extra] ";
        } else if (shortInfo.get()) {
            if (high) info += "[D] ";
            if (top) info += "[T] ";
            if (cev) info += "[C] ";
            if (protect) info += "[P] ";
            if (layer) info += "[E] ";
        }

        return !info.equals("") ? info : null;
    }

    // Keybindings

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!(mc.currentScreen == null)) return;

        if (forceDoubleHeight.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            high = !high;
            if (message.get()) ChatUtils.sendMsg(Text.of(high ? "Activated Double Height" : "Disabled Double Height"));
        }
        if (forceTop.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            top = !top;
            if (message.get()) ChatUtils.sendMsg(Text.of(top ? "Activated Top Block" : "Disabled Top Block"));
        }
        if (forceCev.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            cev = !cev;
            if (message.get()) ChatUtils.sendMsg(Text.of(cev ? "Activated Anti Cev" : "Disabled Anti Cev"));
        }
        if (forceProtect.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            protect = !protect;
            if (message.get()) ChatUtils.sendMsg(Text.of(protect ? "Activated Protect" : "Disabled Protect"));
        }
        if (forceExtra.get().isPressed() && keybindMode.get() == Mode.Toggle) {
            layer = !layer;
            if (message.get()) ChatUtils.sendMsg(Text.of(layer ? "Activated Extra Layer" : "Disabled Extra Layer"));
        }
    }

    // For Automation & Replace

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos pos = event.pos;

        if (!event.newState.isAir()) return;

        if (replace.get() && !ignoreOwn.get() && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= replaceRange.get()) placePositions.add(pos);

        if (onBreak.get() && fullBreak.get() && isSurround(pos)) {
            for (BlockPos position : surround) {
                if (VectorUtils.canPlace(pos.add(position))) placePositions.add(pos.add(position));
            }
        }

        if (autoToggle.get() && (toggleBlocks.get().contains(event.newState.getBlock()) || toggleBlocks.get().contains(event.oldState.getBlock())) && VectorUtils.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX(), pos.getY(), pos.getZ()) <= toggleRange.get()) {
            switch (autoToggleMode.get()) {
                case Both -> {
                    top = true;
                    high = true;
                }

                case Double -> high = true;
                case Top    -> top = true;
            }
        }
    }

    // Burrow Help

    @EventHandler
    public void isCube(CollisionShapeEvent event) {
        if (((burrowHelp.get() && mc.player.getBlockPos().equals(event.pos))
            || (burrowHelp.get() && mc.player.getBlockPos().up(1).equals(event.pos)) && !mc.player.isInSwimmingPose())
            && mc.player.getY() == mc.player.getBlockY()) {
            event.shape = VoxelShapes.empty();
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();

        if ((mc.player.getBlockPos().equals(pos)
            || mc.player.getBlockPos().up().equals(pos))
            && VectorUtils.isClickable(VectorUtils.getBlock(pos))
            && noInteract.get()) event.cancel();
    }

    // Main Loop

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(block -> block.ticks <= 0);

        if (blink.get() && (Modules.get().get(Blink.class).isActive() || Modules.get().get(BlinkPlus.class).isActive())) return;

        if (toggleOnYChange.get() && mc.player.prevY + yLevel.get() < mc.player.getY()) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround() || !getBestBlock().found() || PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;

        for (Module module : modules.get()) {
            if (module.isActive()) {
                module.toggle();
                toActivate.add(module);
            }
        }

        if (ticks >= delay.get() && delay.get() != 0) {
            ticks = 0;
            return;
        } else {
            ticks++;
        }

        protectTicks++;

        // Anti bed

        if (antiBed.get()) {
            FindItemResult item = InvUtils.findInHotbar(stack -> bedBlocks.get().contains(Block.getBlockFromItem(stack.getItem())));

            if (item.found() && string.get()) {
                BlockPos p = mc.player.getBlockPos();

                place(p, item);
                place(p.up(1), item);
                place(p.up(2), item);
            }

            if (breakBed.get()) {
                ArrayList<BlockPos> positions;

                positions = VectorUtils.getPositionsAroundPlayer(bedRange.get());
                positions.removeIf(pos -> !replaceBlocks.get().contains(VectorUtils.getBlock(pos)) || VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) > bedRange.get());

                if (!positions.isEmpty()) {
                    positions.sort(Comparator.comparingDouble(pos -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos))));

                    for (BlockPos pos : positions) {
                        if (BlockUtils.breakBlock(pos, renderSwing.get())) {
                            if (replaceBed.get()) bedPositions.add(pos);

                            break;
                        }
                    }
                }
            }
        }

        // Extra Layer & Replace

        for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            Entity entity = mc.world.getEntityById(value.getActorId());
            BlockPos pos = value.getPos();

            if (entity instanceof PlayerEntity && entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity)) {
                if (onBreak.get() && !fullBreak.get() && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= replaceRange.get()) {
                    for (BlockPos p : surround) {
                        if (VectorUtils.canPlace(mc.player.getBlockPos().add(pos).add(p)) && place(pos.add(p), 0) && delay.get() != 0) break;
                    }
                }

                if (replace.get() && ignoreOwn.get()) {
                    if (!isSurrounded(entity.getBlockPos()) && !pos.equals(mc.player.getBlockPos()) && !placePositions.contains(pos)) {
                        placePositions.add(pos);
                    }
                }
            }
        }

        // Protect

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                BlockPos crystal = entity.getBlockPos();

                if ((protect || forceProtect.get().isPressed() || protectSetting.get()) && (protectTicks >= protectDelay.get() || protectDelay.get() == 0) && isDangerousCrystal(crystal) && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(crystal)) <= protectRange.get()) {
                    if (safe.get() && DamageUtils.crystalDamage(mc.player, entity.getPos()) < maxDamage.get() && DamageUtils.crystalDamage(mc.player, entity.getPos()) < PlayerUtils.getTotalHealth()) {
                        attack(entity);

                        protectTicks = 0;

                        if (protectReplace.get()) placePositions.add(crystal);

                        return;
                    }
                }
            }
        }

        // Placing the Anti Bed Blocks

        if (!bedPositions.isEmpty() && replaceBed.get()) {
            FindItemResult item = InvUtils.findInHotbar(stack -> bedBlocks.get().contains(Block.getBlockFromItem(stack.getItem())));

            if (item.found()) {
                for (BlockPos pos : new ArrayList<>(bedPositions)) {
                    if (place(pos, item)) {
                        if (bedOnlyOnce.get()) bedPositions.remove(pos);
                        if (delay.get() != 0) break;
                    }
                }
            }
        }

        // Placing the Blocks

        if (!placePositions.isEmpty()) {
            for (BlockPos pos : new ArrayList<>(placePositions)) {
                if (place(pos)) {
                    if (replaceOnce.get()) placePositions.remove(pos);
                    if (delay.get() != 0) break;
                }
            }
        }

        // Anti Button

        if (antiButton.get()) {
            for (BlockPos position : surround) {
                BlockPos pos = mc.player.getBlockPos().add(position);
                if (antiButtonBlocks.get().contains(VectorUtils.getBlock(pos))) {
                    for (BlockPos p : surround) {
                        if (VectorUtils.canPlace(pos.add(p)) && !placePositions.contains(pos.add(p))) {
                            placePositions.add(pos.add(p));
                        } else if (antiButtonBlocks.get().contains(VectorUtils.getBlock(pos.add(p))) && BlockUtils.canBreak(pos.add(p))) {
                            BlockUtils.breakBlock(pos, renderSwing.get());
                        }
                    }

                    if (breakButton.get() && (!fastBreak.get() || getSurround(pos) >= 3)) BlockUtils.breakBlock(pos, renderSwing.get());
                }
            }
        }

        int i = 0;

        // Default Surround
        for (BlockPos pos : surround) {
            if (place(pos, 0)) {
                if (delay.get() != 0) break;
                i++;
            }
        }

        // wait a tick before placing the second layer
        if (i == 4) return;

        i = 0;

        // Double Surround
        if ((doubleHeight.get() || forceDoubleHeight.get().isPressed() || high) && isSurrounded(mc.player.getBlockPos())) {
            for (BlockPos pos : surround) {
                if (place(pos, 1)) {
                    i++;
                    if (delay.get() != 0) break;
                }
            }
        }

        if (i == 4) return;

        if (forceTop.get().isPressed() || top) place(mc.player.getBlockPos().up(2));
        if (forceCev.get().isPressed() || cev) place(mc.player.getBlockPos().up(3));

        if (forceExtra.get().isPressed() || layer || extraLayer.get()) {
            for (BlockPos pos : extra) {
                if (place(pos, 0) && delay.get() != 0) break;
            }
        }

        complete = isSurrounded(mc.player.getBlockPos()) && (!doubleHeight.get() || isSurrounded(mc.player.getBlockPos().up()));

        // Disabling
        if (complete && toggleOnComplete.get()) toggle();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (blink.get() && (Modules.get().get(Blink.class).isActive() || Modules.get().get(BlinkPlus.class).isActive())) return;

        if (toggleOnYChange.get() && mc.player.prevY + yLevel.get() < mc.player.getY()) {
            if (blink.get() && !Modules.get().get(Blink.class).isActive() && !Modules.get().get(BlinkPlus.class).isActive()) toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround() || !getBestBlock().found() || PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;
        if (!centered && center.get() == Center.OnActivate && smoothCenter.get() != SmoothCenter.None && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(mc.player.getBlockPos())) >= 0.05) center();
        if (VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(mc.player.getBlockPos())) <= 0.05) centered = true;
        if (center.get() == Center.Always || (!complete && center.get() == Center.Incomplete)) center();
    }

    // Render

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
        renderBlocks.forEach(block -> block.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    // Utils

    private boolean place(BlockPos position, int y) {
        if (mc.player.getBlockPos().equals(position)) return false;
        BlockPos pos = mc.player.getBlockPos().add(position).up(y);

        boolean placed = VectorUtils.place(pos, getBestBlock(), rotate.get(), 100, !onEntityPlace.get());

        if (placed) renderBlocks.add(renderBlockPool.get().set(pos));
        if (!placed) checkForInterfering(pos);

        return placed;
    }

    private boolean place(BlockPos position) {
        boolean placed = VectorUtils.place(position, getBestBlock(), rotate.get(), 100, !onEntityPlace.get());

        if (placed) renderBlocks.add(renderBlockPool.get().set(position));
        if (!placed) checkForInterfering(position);

        return placed;
    }

    private boolean place(BlockPos position, FindItemResult item) {
        boolean placed = VectorUtils.place(position, item, rotate.get(), 100, !onEntityPlace.get());

        if (placed) renderBlocks.add(renderBlockPool.get().set(position));
        if (!placed) checkForInterfering(position);

        return placed;
    }

    private void checkForInterfering(BlockPos pos) {
        boolean mined = false;
        for (BlockBreakingInfo info : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            if (info.getPos().equals(pos) && mc.world.getEntityById(info.getActorId()) != mc.player) {
                mined = true;
                break;
            }
        }

        if (breakBlocks.get() && !blocks.get().contains(VectorUtils.getBlock(pos))) {
            BlockUtils.breakBlock(pos, renderSwing.get());
            return;
        }

        if (interfering.get() && VectorUtils.getBlock(pos).getBlastResistance() >= 1200.0F || mined) {
            if (VectorUtils.getCollision(pos) == null || VectorUtils.getCollision(pos).isEmpty()) return;

            Box box = new Box(
                pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
            );

            Predicate<Entity> entityPredicate = entity -> entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(mc.player, entity.getPos()) < PlayerUtils.getTotalHealth();

            for (Entity crystal : mc.world.getOtherEntities(null, box, entityPredicate)) {
                int id = crystal.getId();

                if (isDangerousCrystal(crystal.getBlockPos()) && (!crystals.containsKey(id) || (crystals.containsKey(id) && crystals.get(id) >= interferingDelay.get()))) {
                    if (interferingReplace.get()) placePositions.add(crystal.getBlockPos());
                    if (crystals.containsKey(id)) {
                        crystals.replace(id, crystals.get(id) + 1);
                    } else {
                        crystals.putIfAbsent(id, 0);
                    }

                    attack(crystal);
                }
            }
        }
    }

    private void center() {
        if (smoothCenter.get() != SmoothCenter.None) {
            double[] dir = VectorUtils.directionSpeed(1.0F);

            double deltaX = Utils.clamp(mc.player.getBlockX() + 0.5 - mc.player.getX(), -0.05, 0.05);
            double deltaZ = Utils.clamp(mc.player.getBlockZ() + 0.5 - mc.player.getZ(), -0.05, 0.05);

            Vec3d vel = mc.player.getVelocity();

            double x = (dir[0] > (unlock.get() / 10) || dir[0] < -(unlock.get() / 10)) ? vel.x : (speed.get() != 0 ? deltaX / speed.get() : deltaX);
            double z = (dir[1] > (unlock.get() / 10) || dir[1] < -(unlock.get() / 10)) ? vel.z : (speed.get() != 0 ? deltaZ / speed.get() : deltaZ);

            switch (centerMode.get()) {
                case Normal:
                    mc.player.setVelocity(x, mc.player.getVelocity().y, z);
                    break;
                case Set:
                    ((IVec3d) mc.player.getVelocity()).setXZ(x, z);
                    break;
                case TP:
                    if (!mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(x, 0, z)).iterator().hasNext()) {
                        mc.player.setPosition(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                        mc.player.setVelocity((speed.get() != 0 ? vel.x / (speed.get() * 2) : vel.x), vel.y, (speed.get() != 0 ? vel.z / (speed.get() * 2) : vel.z));
                    }

                    break;
                case Fast:
                    for (int i = packets.get() + 1; i > 0; i--) {
                        centerTicks++;
                        if (centerTicks >= centerDelay.get() || centerDelay.get() == 0) centerPlayer(0.5 / i, 0.5 / i);
                        else break;
                    }


                    break;
            }
        } else {
            centerPlayer(0.5, 0.5);
        }
    }

    private boolean isDangerousCrystal(BlockPos position) {
        for (BlockPos pos : surround) {
            for (BlockPos p : plus) {
                BlockPos crystal = mc.player.getBlockPos().add(pos.add(p));
                if (crystal.equals(position) || crystal.down().equals(position)
                    || crystal.equals(mc.player.getBlockPos().add(pos))
                    || crystal.equals(mc.player.getBlockPos().add(pos).down())) return true;
            }
        }

        return true;
    }

    private void centerPlayer(double offsetX, double offsetZ) {
        double x = MathHelper.floor(mc.player.getX()) + offsetX;
        double z = MathHelper.floor(mc.player.getZ()) + offsetZ;

        mc.player.setPosition(x, mc.player.getY(), z);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
    }

    private void attack(Entity entity) {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getPitch(entity), Rotations.getYaw(entity), () -> mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking())));
        } else {
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        }

        if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private FindItemResult getBestBlock() {
        return InvUtils.findInHotbar(stack -> blocks.get().contains(Block.getBlockFromItem(stack.getItem())));
    }

    private boolean isSurround(BlockPos position) {
        for (BlockPos pos : surround) {
            if (position.equals(mc.player.getBlockPos().add(pos))) return true;
        }

        return false;
    }

    private boolean isSurrounded(BlockPos position) {
        int i = 0;

        for (BlockPos pos : surround) {
            Block block = VectorUtils.getBlock(position.add(pos));
            if (block != null && block.getBlastResistance() >= 1200.0F) i++;
        }

        return i == 4;
    }

    private int getSurround(BlockPos position) {
        int i = 0;

        for (BlockPos pos : surround) {
            Block block = VectorUtils.getBlock(position.add(pos));
            if (block != null && block.getBlastResistance() >= 1200.0F) i++;
        }

        return i;
    }

    // Constants

    private boolean blockFilter(Block block) {
        return block.getBlastResistance() >= 1000.0F && (
            block == Blocks.ANCIENT_DEBRIS ||
            block == Blocks.ANVIL ||
            block == Blocks.CHIPPED_ANVIL ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.DAMAGED_ANVIL ||
            block == Blocks.ENCHANTING_TABLE ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.RESPAWN_ANCHOR
        );
    }

    private boolean bedFilter(Block block) {
        return block == Blocks.ACACIA_BUTTON ||
            block == Blocks.BIRCH_BUTTON ||
            block == Blocks.CRIMSON_BUTTON ||
            block == Blocks.DARK_OAK_BUTTON ||
            block == Blocks.JUNGLE_BUTTON ||
            block == Blocks.OAK_BUTTON ||
            block == Blocks.POLISHED_BLACKSTONE_BUTTON ||
            block == Blocks.SPRUCE_BUTTON ||
            block == Blocks.STONE_BUTTON ||
            block == Blocks.WARPED_BUTTON ||
            block == Blocks.TRIPWIRE;
    }

    public static ArrayList<BlockPos> extra = new ArrayList<>() {{
        add(new BlockPos(2, 0, 0));
        add(new BlockPos(-2, 0, 0));
        add(new BlockPos(0, 0, 2));
        add(new BlockPos(0, 0, -2));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(1, 0, -1));
        add(new BlockPos(-1, 0, -1));
    }};

    public static ArrayList<BlockPos> plus = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(-1, 0, -1));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(0, 0, -1));
        add(new BlockPos(1, 0, -1));
    }};

    public static ArrayList<BlockPos> surround = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }

    public enum SmoothCenter {
        None,
        Default
    }

    public enum CenterMode {
        Normal,
        Fast,
        Set,
        TP
    }

    public enum Mode {
        Toggle,
        Hold
    }

    // Rendering

    private class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            if (pos == null) return null;

            pos.set(blockPos);
            ticks = renderTicks.get();

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
