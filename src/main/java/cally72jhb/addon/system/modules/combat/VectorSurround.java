package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.modules.movement.StepPlus;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.FindItemResult;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;

import java.util.*;

public class VectorSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCenter = settings.createGroup("Center");
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgBurrow = settings.createGroup("Burrow");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgReplace = settings.createGroup("Replace");
    private final SettingGroup sgAntiBed = settings.createGroup("Anti Bed");
    private final SettingGroup agAntiButton = settings.createGroup("Anti Button");
    private final SettingGroup sgAuto = settings.createGroup("Auto");
    private final SettingGroup sgExtra = settings.createGroup("Extra Layer");
    private final SettingGroup sgKeybindings = settings.createGroup("Keybindings");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks to use for surround.")
        .defaultValue(Blocks.OBSIDIAN)
        .filter(block -> block == Blocks.ANCIENT_DEBRIS ||
            block == Blocks.ANVIL ||
            block == Blocks.CHIPPED_ANVIL ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.DAMAGED_ANVIL ||
            block == Blocks.ENCHANTING_TABLE ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.OBSIDIAN ||
            block == Blocks.RESPAWN_ANCHOR)
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

    private final Setting<Integer> generalDelay = sgGeneral.add(new IntSetting.Builder()
        .name("general-delay")
        .description("The delay in ticks between every action.")
        .min(0)
        .defaultValue(0)
        .noSlider()
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The delay in ticks between block placements.")
        .min(0)
        .defaultValue(0)
        .noSlider()
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("The maximum amount of blocks placed in a tick.")
        .min(1)
        .defaultValue(4)
        .sliderMin(1)
        .sliderMax(5)
        .noSlider()
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What blocks to place first.")
        .defaultValue(Mode.Strict)
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

    private final Setting<Boolean> feet = sgGeneral.add(new BoolSetting.Builder()
        .name("feet")
        .description("Places a block below your feet to prevent you from falling down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> stopMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-movement")
        .description("Stops your y-movement when your surrounded to prevent falling down.")
        .defaultValue(true)
        .visible(feet::get)
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

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the selected slot before you placed / mined a block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the obsidian being placed or crystals being broken.")
        .defaultValue(false)
        .build()
    );


    // Center


    private final Setting<Center> centerMode = sgCenter.add(new EnumSetting.Builder<Center>()
        .name("center")
        .description("Teleports you to the center of the block.")
        .defaultValue(Center.Always)
        .build()
    );

    private final Setting<Boolean> inAir = sgCenter.add(new BoolSetting.Builder()
        .name("in-air")
        .description("Whether or not to center you in air.")
        .defaultValue(true)
        .visible(() -> centerMode.get() != Center.Never)
        .build()
    );

    private final Setting<Double> inAirHeight = sgCenter.add(new DoubleSetting.Builder()
        .name("over-ground")
        .description("How many blocks you have to be in air to center you.")
        .defaultValue(15)
        .min(1)
        .sliderMin(1)
        .sliderMax(20)
        .visible(inAir::get)
        .visible(() -> centerMode.get() != Center.Never)
        .build()
    );

    private final Setting<SmoothCenter> smoothCenter = sgCenter.add(new EnumSetting.Builder<SmoothCenter>()
        .name("smooth-center")
        .description("How smooth to center you.")
        .defaultValue(SmoothCenter.Default)
        .visible(() -> centerMode.get() != Center.Never)
        .build()
    );

    private final Setting<Double> centerSpeed = sgCenter.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The centring speed.")
        .defaultValue(1)
        .min(0)
        .sliderMin(0.75)
        .sliderMax(2)
        .visible(() -> centerMode.get() != Center.Never && smoothCenter.get() != SmoothCenter.None)
        .build()
    );

    private final Setting<Boolean> stabilize = sgCenter.add(new BoolSetting.Builder()
        .name("stabilize")
        .description("Attempts to stabilize your position while centering.")
        .defaultValue(true)
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
        .visible(() -> centerMode.get() != Center.Never && smoothCenter.get() != SmoothCenter.None)
        .build()
    );

    private final Setting<CenterMode> centerType = sgCenter.add(new EnumSetting.Builder<CenterMode>()
        .name("center-mode")
        .description("How to center you.")
        .defaultValue(CenterMode.TP)
        .visible(() -> centerMode.get() != Center.Never && smoothCenter.get() != SmoothCenter.None)
        .build()
    );


    // Bypass


    private final Setting<Boolean> bypass = sgBypass.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Attempts to bypass block placing on some servers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> innerDelay = sgBypass.add(new IntSetting.Builder()
        .name("inner-delay")
        .description("How many ticks to wait after failing to place a block in the inner layer of you surround.")
        .defaultValue(1)
        .min(0)
        .sliderMin(0)
        .sliderMax(5)
        .noSlider()
        .visible(bypass::get)
        .build()
    );

    private final Setting<Integer> outerDelay = sgBypass.add(new IntSetting.Builder()
        .name("outer-delay")
        .description("How many ticks to wait after failing to place a block in the outer layer of you surround.")
        .defaultValue(7)
        .min(0)
        .sliderMin(0)
        .sliderMax(5)
        .noSlider()
        .visible(bypass::get)
        .build()
    );

    private final Setting<Boolean> checkPlace = sgBypass.add(new BoolSetting.Builder()
        .name("check-place")
        .description("Checks whether or not you can place a block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> packetPlace = sgBypass.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Places blocks with packets instead of normally.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onEntityPlace = sgBypass.add(new BoolSetting.Builder()
        .name("on-entity")
        .description("Attempts to place blocks even if entities are in its way.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("How far blocks are mined.")
        .defaultValue(3.5)
        .min(1)
        .sliderMin(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Boolean> mineTwice = sgBypass.add(new BoolSetting.Builder()
        .name("mine-twice")
        .description("Tyes to mine a block twice a tick.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> instaMine = sgBypass.add(new BoolSetting.Builder()
        .name("insta-mine")
        .description("Tryes to mine blocks instant.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxHardness = sgBypass.add(new DoubleSetting.Builder()
        .name("max-hardness")
        .description("The maximum hardness a block can have for it to be instant mined.")
        .defaultValue(0.3)
        .min(0)
        .sliderMax(1)
        .visible(instaMine::get)
        .build()
    );

    private final Setting<Integer> mineTickDelay = sgBypass.add(new IntSetting.Builder()
        .name("mine-tick-delay")
        .description("The delay between the attempted breaks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(instaMine::get)
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


    // Crystals


    private final Setting<Boolean> interfering = sgCrystals.add(new BoolSetting.Builder()
        .name("interfering")
        .description("Removes interfering crystals before placing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> protectSetting = sgCrystals.add(new BoolSetting.Builder()
        .name("protect")
        .description("Breaks crystals near you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> crystalRange = sgCrystals.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range in what crystals can be to be broken.")
        .defaultValue(3.5)
        .min(0)
        .visible(() -> interfering.get() || protectSetting.get())
        .build()
    );

    private final Setting<Boolean> crystalReplace = sgCrystals.add(new BoolSetting.Builder()
        .name("replace")
        .description("Replaces the broken crystals with obsidian.")
        .defaultValue(false)
        .visible(() -> interfering.get() || protectSetting.get())
        .build()
    );

    private final Setting<Boolean> replaceOnce = sgCrystals.add(new BoolSetting.Builder()
        .name("replace-once")
        .description("Replaces the crystals only once.")
        .defaultValue(false)
        .visible(() -> (interfering.get() || protectSetting.get()) && crystalReplace.get())
        .build()
    );

    private final Setting<Boolean> safe = sgCrystals.add(new BoolSetting.Builder()
        .name("safe")
        .description("Doesn't break crystals that can kill you.")
        .defaultValue(true)
        .visible(() -> interfering.get() || protectSetting.get())
        .build()
    );

    private final Setting<Integer> maxDamage = sgCrystals.add(new IntSetting.Builder()
        .name("max-damage")
        .description("The maximum damage that can be dealt when breaking crystals.")
        .defaultValue(10)
        .min(1)
        .max(50)
        .sliderMin(1)
        .sliderMax(32)
        .noSlider()
        .visible(() -> (interfering.get() || protectSetting.get()) && safe.get())
        .build()
    );

    private final Setting<Integer> crystalTicks = sgCrystals.add(new IntSetting.Builder()
        .name("crystal-ticks-existed")
        .description("How many ticks the crystal has to have existed.")
        .defaultValue(1)
        .min(0)
        .noSlider()
        .visible(() -> interfering.get() || protectSetting.get())
        .build()
    );

    private final Setting<Integer> crystalDelay = sgCrystals.add(new IntSetting.Builder()
        .name("crystal-delay")
        .description("The delay when hitting crystals.")
        .defaultValue(10)
        .min(0)
        .noSlider()
        .visible(() -> interfering.get() || protectSetting.get())
        .build()
    );


    // Replace


    private final Setting<Boolean> replace = sgReplace.add(new BoolSetting.Builder()
        .name("block-replace")
        .description("When a block is broken it will be replaced with obsidian again.")
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


    // Anti Bed


    private final Setting<Boolean> antiBed = sgAntiBed.add(new BoolSetting.Builder()
        .name("anti-bed")
        .description("Places strings or breaks the beds to prevent bed-aura.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> string = sgAntiBed.add(new BoolSetting.Builder()
        .name("place-string")
        .description("Places one of the selected blocks in you to prevent bed-aura.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgAntiBed.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Stops you from breaking the string.")
        .defaultValue(true)
        .visible(string::get)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgAntiBed.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only protects you from beds if you are fully surrounded.")
        .defaultValue(true)
        .visible(() -> antiBed.get() || string.get())
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
        .filter(block -> block == Blocks.ACACIA_BUTTON ||
            block == Blocks.BIRCH_BUTTON ||
            block == Blocks.CRIMSON_BUTTON ||
            block == Blocks.DARK_OAK_BUTTON ||
            block == Blocks.JUNGLE_BUTTON ||
            block == Blocks.OAK_BUTTON ||
            block == Blocks.POLISHED_BLACKSTONE_BUTTON ||
            block == Blocks.SPRUCE_BUTTON ||
            block == Blocks.STONE_BUTTON ||
            block == Blocks.WARPED_BUTTON ||
            block == Blocks.TRIPWIRE)
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


    private final Setting<Boolean> placeButton = agAntiButton.add(new BoolSetting.Builder()
        .name("place")
        .description("Places around the button to prevent crystals.")
        .defaultValue(true)
        .visible(antiButton::get)
        .build()
    );

    private final Setting<Boolean> breakButton = agAntiButton.add(new BoolSetting.Builder()
        .name("break-button")
        .description("Whether or not to break the button.")
        .defaultValue(true)
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
        .description("Blocks to break if they are in your surround.")
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


    // Auto


    private final Setting<Boolean> autoToggle = sgAuto.add(new BoolSetting.Builder()
        .name("auto-toggle")
        .description("Toggles double-height or top when certain blocks are placed near you.")
        .defaultValue(false)
        .build()
    );

    public final Setting<ScanMode> scanMode = sgAuto.add(new EnumSetting.Builder<ScanMode>()
        .name("scan-mode")
        .description("How to search for the selected blocks.")
        .defaultValue(ScanMode.Update)
        .visible(autoToggle::get)
        .build()
    );

    public final Setting<ToggleMode> autoToggleMode = sgAuto.add(new EnumSetting.Builder<ToggleMode>()
        .name("auto-toggle-mode")
        .description("The mode for toggling.")
        .defaultValue(ToggleMode.Both)
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


    // Extra Surround


    private final Setting<Boolean> extraLayer = sgExtra.add(new BoolSetting.Builder()
        .name("extra-layer")
        .description("Toggles a extra secure layer around your default surround.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onInterfere = sgExtra.add(new BoolSetting.Builder()
        .name("on-interfere")
        .description("Toggles the extra layer when a entity stands in your surround.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> checkCollision = sgExtra.add(new BoolSetting.Builder()
        .name("check-collision")
        .description("Checks if the entity is in one of your surround blocks.")
        .defaultValue(true)
        .visible(onInterfere::get)
        .build()
    );

    private final Setting<Boolean> onBreak = sgExtra.add(new BoolSetting.Builder()
        .name("on-break")
        .description("Toggles the extra layer when a surround block is broken.")
        .defaultValue(false)
        .build()
    );

    public final Setting<BreakMode> breakMode = sgExtra.add(new EnumSetting.Builder<BreakMode>()
        .name("break-mode")
        .description("How the block has to be broken for it to trigger the extra layer.")
        .defaultValue(BreakMode.Both)
        .visible(onBreak::get)
        .build()
    );


    // Keybindings


    private final Setting<Boolean> message = sgKeybindings.add(new BoolSetting.Builder()
        .name("message")
        .description("Whether or not to send you a message when toggled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> forceDouble = sgKeybindings.add(new KeybindSetting.Builder()
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
        .description("Places a block over your head to protect you from getting cev-breaker.")
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

    private final Setting<Keybind> forceCenter = sgKeybindings.add(new KeybindSetting.Builder()
        .name("force-center")
        .description("Enables / Disables centering.")
        .defaultValue(Keybind.fromKey(-1))
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
        .noSlider()
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
    private ArrayList<BlockPos> breakPositions;

    private BlockPos breakingPos;

    private IntSet crystals;
    private HashMap<BlockPos, Integer> placedMap;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private int blocksInTick;

    private int timer;
    private int placeTimer;
    private int breakTimer;
    private int crystalTimer;

    private boolean high;
    private boolean top;
    private boolean cev;
    private boolean layer;
    private boolean protect;
    private boolean center;

    private final Random random = new Random();

    public VectorSurround() {
        super(Categories.Combat, "vector-surround", "Surrounds you in blocks to prevent you from taking crystal damage.");
    }

    // Initialising

    @Override
    public void onActivate() {
        if (centerMode.get() == Center.OnActivate) center(true);

        toActivate = new ArrayList<>();
        bedPositions = new ArrayList<>();
        placePositions = new ArrayList<>();
        breakPositions = new ArrayList<>();

        crystals = new IntOpenHashSet();
        placedMap = new HashMap<>();

        blocksInTick = 0;

        high = false;
        top = false;
        cev = false;
        layer = false;
        protect = false;
        center = true;

        timer = generalDelay.get();
        placeTimer = placeDelay.get();
        breakTimer = mineTickDelay.get();
        crystalTimer = crystalDelay.get();

        breakingPos = null;

        for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();

        activate();
    }

    // Info Sting

    @Override
    public String getInfoString() {
        String info = "";

        if (!shortInfo.get()) {
            if (cev) info += "[Cev] ";
            if (high) info += "[Double] ";
            if (layer) info += "[Extra] ";
            if (protect) info += "[Protect] ";
            if (top) info += "[Top]";
        } else if (shortInfo.get()) {
            if (cev) info += "[C] ";
            if (high) info += "[D] ";
            if (layer) info += "[E] ";
            if (protect) info += "[P] ";
            if (top) info += "[T]";
        }

        return !info.equals("") ? info : null;
    }

    // Keybindings

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.currentScreen != null || event.action != KeyAction.Press) return;

        if (forceDouble.get().isPressed()) {
            high = !high;
            if (message.get()) ChatUtils.sendMsg(Text.of(high ? "Activated Double Height" : "Disabled Double Height"));
        }
        if (forceTop.get().isPressed()) {
            top = !top;
            if (message.get()) ChatUtils.sendMsg(Text.of(top ? "Activated Top Block" : "Disabled Top Block"));
        }
        if (forceCev.get().isPressed()) {
            cev = !cev;
            if (message.get()) ChatUtils.sendMsg(Text.of(cev ? "Activated Anti Cev" : "Disabled Anti Cev"));
        }
        if (forceProtect.get().isPressed()) {
            protect = !protect;
            if (message.get()) ChatUtils.sendMsg(Text.of(protect ? "Activated Protect" : "Disabled Protect"));
        }
        if (forceExtra.get().isPressed()) {
            layer = !layer;
            if (message.get()) ChatUtils.sendMsg(Text.of(layer ? "Activated Extra Layer" : "Disabled Extra Layer"));
        }
        if (forceCenter.get().isPressed()) {
            center = !center;
            if (message.get()) ChatUtils.sendMsg(Text.of(center ? "Activated Centering" : "Disabled Centering"));
        }
    }

    // For Automation & Replace

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (!event.newState.getMaterial().isReplaceable()) return;

        BlockPos pos = event.pos;

        if (replace.get() && !ignoreOwn.get() && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= replaceRange.get()) {
            placePositions.add(pos);
        }

        if (onBreak.get() && (breakMode.get() == BreakMode.Full || breakMode.get() == BreakMode.Both)
            && isInSurround(pos) && isSurroundBlock(event.oldState.getBlock())
            && !isSurroundBlock(event.newState.getBlock())) {
            for (BlockPos position : getSurround()) {
                if (canPlace(pos.add(position), true)) placePositions.add(pos.add(position));
            }
        }

        if (autoToggle.get() && (scanMode.get() == ScanMode.Update || scanMode.get() == ScanMode.Both)
            && (toggleBlocks.get().contains(event.newState.getBlock()) || toggleBlocks.get().contains(event.oldState.getBlock()))
            && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= toggleRange.get()) {
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
        if (mc.player.isOnGround() && event.shape != null && event.shape != VoxelShapes.fullCube()
            && ((burrowHelp.get() && mc.player.getBlockPos().equals(event.pos))
            || (burrowHelp.get() && mc.player.getBlockPos().up(1).equals(event.pos))
            && !mc.player.isInSwimmingPose()) && mc.player.getY() == mc.player.getBlockY()) {
            event.shape = VoxelShapes.empty();
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();

        if (noInteract.get() && (mc.player.getBlockPos().equals(pos)
            || mc.player.getBlockPos().up().equals(pos))
            && VectorUtils.isClickable(mc.world.getBlockState(pos).getBlock())) {

            event.cancel();
        }
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        BlockPos pos = event.blockPos;

        if (string.get() && antiBreak.get() && isSurrounded(mc.player.getBlockPos())
            && (mc.player.getBlockPos().equals(pos) || mc.player.getBlockPos().up().equals(pos))) {
            event.cancel();
        }
    }

    // Feet / Stop Movement

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (feet.get() && stopMovement.get() && !isSurroundBlock(mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock()) && event.movement.y < 0) {
            ((IVec3d) event.movement).setY(0);
        }
    }

    // Main Loop

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(block -> block.ticks <= 0);

        crystalTimer++;
        breakTimer++;

        if (blink.get() && (Modules.get().isActive(Blink.class))) {
            activate();
            return;
        }

        if (toggleOnYChange.get() && mc.player.prevY + yLevel.get() < mc.player.getY()) {
            toggle();
            return;
        }

        if (feet.get()) place(mc.player.getBlockPos().down());
        if (onlyOnGround.get() && !mc.player.isOnGround() || !getBestBlock().found()) return;

        deactivate();

        if (timer >= generalDelay.get() && generalDelay.get() != 0) {
            timer = 0;
            return;
        } else {
            timer++;
        }

        blocksInTick = 0;

        // Anti bed

        if (antiBed.get() && (!onlyInHole.get() || onlyInHole.get() && checkForBed(mc.player.getBlockPos()))) {
            FindItemResult item = VectorUtils.findInHotbar(stack -> bedBlocks.get().contains(Block.getBlockFromItem(stack.getItem())));

            if (string.get() && item.found() && item.getHand() != null) {
                BlockPos pos = mc.player.getBlockPos();

                place(pos.up(1), item);
                place(pos.up(2), item);
            }

            if (breakBed.get()) {
                ArrayList<BlockPos> positions;

                positions = VectorUtils.getPositionsAroundPlayer(bedRange.get());
                positions.removeIf(pos -> !replaceBlocks.get().contains(mc.world.getBlockState(pos).getBlock()) || VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) > bedRange.get());

                if (!positions.isEmpty()) {
                    positions.sort(Comparator.comparingDouble(pos -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos))));

                    breakingPos = positions.get(0);
                    if (BlockUtils.breakBlock(positions.get(0), renderSwing.get()) && replaceBed.get()) {
                        bedPositions.add(positions.get(0));
                    }
                }
            }
        }

        // Crystals

        if (!crystals.isEmpty() && (crystalTimer >= crystalDelay.get() || crystalDelay.get() == 0)) {
            for (IntIterator it = crystals.iterator(); it.hasNext();) {
                int id = it.nextInt();
                Entity entity = mc.world.getEntityById(id);

                if (entity == null) {
                    it.remove();
                } else if (shouldAttackCrystal(entity.getBlockPos())
                    && VectorUtils.distance(mc.player.getPos(), entity.getPos()) <= crystalRange.get()
                    && entity.age >= crystalTicks.get()) {
                    attack(entity);
                    it.remove();

                    if (crystalReplace.get() && VectorUtils.distance(mc.player.getPos(), entity.getPos()) <= replaceRange.get()) placePositions.add(entity.getBlockPos());

                    crystalTimer = 0;
                    break;
                }
            }
        }

        // Auto Toggle

        if (autoToggle.get() && (scanMode.get() == ScanMode.Accurate || scanMode.get() == ScanMode.Both)) {
            ArrayList<BlockPos> positions;

            positions = VectorUtils.getPositionsAroundPlayer(toggleRange.get());
            positions.removeIf(pos -> !toggleBlocks.get().contains(mc.world.getBlockState(pos).getBlock()) || VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) > toggleRange.get());

            if (!positions.isEmpty()) {
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

        // Extra Layer & Replace

        for (BlockBreakingInfo info : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            Entity entity = mc.world.getEntityById(info.getActorId());
            BlockPos pos = info.getPos();

            if (entity instanceof PlayerEntity && entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity)) {
                if (onBreak.get() && isInSurround(pos) && (breakMode.get() == BreakMode.Start || breakMode.get() == BreakMode.Both)
                    && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= replaceRange.get()) {
                    for (BlockPos position : surround) {
                        if (!placePositions.contains(pos.add(position))) placePositions.add(pos.add(position));
                    }
                }

                if (replace.get() && ignoreOwn.get() && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= replaceRange.get()) {
                    if (!pos.equals(mc.player.getBlockPos()) && !pos.equals(mc.player.getBlockPos().up()) && !placePositions.contains(pos)) {
                        placePositions.add(pos);
                    }
                }
            }
        }

        // Protect

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                if ((protect || protectSetting.get()) && isDangerousCrystal(entity.getBlockPos())) crystals.add(entity.getId());
            }
        }

        // Placing the Anti Bed Blocks

        if (!bedPositions.isEmpty() && replaceBed.get()) {
            FindItemResult item = VectorUtils.findInHotbar(stack -> bedBlocks.get().contains(Block.getBlockFromItem(stack.getItem())));

            if (item.found()) {
                for (BlockPos pos : new ArrayList<>(bedPositions)) {
                    if (place(pos, item)) {
                        if (bedOnlyOnce.get()) bedPositions.remove(pos);
                        if (placeDelay.get() != 0) break;
                    }
                }
            }
        }

        // Placing the Blocks

        if (!placePositions.isEmpty()) {
            placePositions.removeIf(pos -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) >= 8);

            for (BlockPos pos : new ArrayList<>(placePositions)) {
                if ((!checkPlace.get() || checkPlace.get() && doesIntersect(new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))) && place(pos)) {
                    if (replaceOnce.get()) placePositions.remove(pos);
                    if (placeDelay.get() != 0) break;
                }
            }
        }

        if (placeTimer >= placeDelay.get() && placeDelay.get() != 0) {
            placeTimer = 0;
            return;
        } else {
            placeTimer++;
        }

        // Default Surround

        for (BlockPos pos : getSurround()) {
            if (place(pos, 0) && placeDelay.get() != 0) break;
        }

        // Important Modes

        // Anti Button

        if (antiButton.get()) {
            for (BlockPos position : surround) {
                BlockPos pos = mc.player.getBlockPos().add(position);
                if (antiButtonBlocks.get().contains(mc.world.getBlockState(pos).getBlock())) {
                    if (placeButton.get()) {
                        for (BlockPos p : surround) {
                            if (canPlace(pos.add(p), true) && !placePositions.contains(pos.add(p))) {
                                placePositions.add(pos.add(p));
                            } else if (antiButtonBlocks.get().contains(mc.world.getBlockState(pos.add(p)).getBlock()) && BlockUtils.canBreak(pos.add(p))) {
                                breakPositions.add(pos.add(p));
                                break;
                            }
                        }
                    }

                    if (breakButton.get() && (!fastBreak.get() || getSurround(pos) >= 3)) {
                        breakPositions.add(pos);
                    }
                }
            }
        }

        // Interfering

        if (onInterfere.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (VectorUtils.distanceXZ(mc.player.getPos(), entity.getPos())<= 3.15) {
                    BlockPos entityPos = entity.getBlockPos();

                    if (entity instanceof PlayerEntity && entity != mc.player) {
                        if (isInSurround(entityPos)) {
                            for (BlockPos pos : surround) {
                                BlockPos position = entityPos.add(pos);
                                if (canPlace(position, true) && !placePositions.contains(position)) {
                                    placePositions.add(position);
                                }
                            }
                        } else if (isInSurround(entityPos.up())) {
                            for (BlockPos pos : surround) {
                                BlockPos position = entityPos.add(pos).up();
                                if (canPlace(position, true) && !placePositions.contains(position)) {
                                    placePositions.add(position);
                                }
                            }
                        }
                    }

                    if (checkCollision.get() && entity != mc.player) {
                        for (BlockPos position : surround) {
                            if (doesIntersect(mc.world.getBlockState(mc.player.getBlockPos().add(position)).getCollisionShape(mc.world, mc.player.getBlockPos().add(position)).getBoundingBox())) {
                                for (BlockPos pos : extra) {
                                    BlockPos placePos = entityPos.add(pos);

                                    if (isInRelativeArray(placePos, extra) && !placePositions.contains(placePos)) {
                                        placePositions.add(placePos);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Double Surround

        if ((high || doubleHeight.get() || forceDouble.get().isPressed() && mc.currentScreen == null) && isSurrounded(mc.player.getBlockPos())) {
            for (BlockPos pos : getSurround()) {
                if (place(pos, 1) && placeDelay.get() != 0) break;
            }
        }

        if (top || forceTop.get().isPressed() && mc.currentScreen == null) place(mc.player.getBlockPos().up(2));
        if (cev || forceCev.get().isPressed() && mc.currentScreen == null) place(mc.player.getBlockPos().up(3));

        if (layer || extraLayer.get() || forceExtra.get().isPressed()  && mc.currentScreen == null) {
            for (BlockPos pos : extra) {
                if (place(pos, 0) && placeDelay.get() != 0) break;
            }
        }

        // Disabling

        if (toggleOnComplete.get() && isSurrounded(mc.player.getBlockPos())
            && (!doubleHeight.get() || isSurrounded(mc.player.getBlockPos().up()))) {
            toggle();
        }

        // Bypass

        if (!placedMap.isEmpty()) {
            for (BlockPos pos : new HashSet<>(placedMap.keySet())) {
                if (placedMap.containsKey(pos)) {
                    if (!isSurroundBlock(mc.world.getBlockState(pos).getBlock())) {
                        placedMap.replace(pos, placedMap.get(pos) + 1);
                    } else {
                        placedMap.remove(pos);
                    }
                }
            }
        }

        // Breaking the Blocks

        breakPositions.removeIf(pos -> !canBreak(pos));

        if (breakingPos == null && !breakPositions.isEmpty()) {
            BlockPos pos = breakPositions.get(0);
            if (mineTwice.get()) BlockUtils.breakBlock(pos, renderSwing.get());

            breakPositions.remove(pos);
            breakingPos = pos;
        }

        if (canBreak(breakingPos)) {
            if (instaMine.get() && (breakTimer >= mineTickDelay.get() || mineTickDelay.get() == 0) && checkHardness(breakingPos)) {
                breakTimer = 0;

                if (rotate.get()) {
                    Rotations.rotate(Rotations.getYaw(breakingPos), Rotations.getPitch(breakingPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakingPos, getClosestDirection(breakingPos).get(0))));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakingPos, getClosestDirection(breakingPos).get(0)));
                }

                if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            } else {
                BlockUtils.breakBlock(breakingPos, renderSwing.get());
            }
        } else {
            breakingPos = null;
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (blink.get() && (Modules.get().isActive(Blink.class))) return;

        if (toggleOnYChange.get() && mc.player.prevY + yLevel.get() < mc.player.getY()) {
            if (blink.get() && !Modules.get().isActive(Blink.class)) toggle();
            return;
        }

        if (!inAir.get() && !mc.player.isOnGround() || (inAir.get() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().stretch(0, -inAirHeight.get() + 0.05, 0)).iterator().hasNext())) return;
        if (centerMode.get() == Center.Always || (!isSurrounded(mc.player.getBlockPos()) && centerMode.get() == Center.Incomplete)) {
            center(true);
        }
    }

    // Render

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
        renderBlocks.forEach(block -> block.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    // Utils

    private boolean place(BlockPos pos, int y) {
        BlockPos position = mc.player.getBlockPos().add(pos).up(y);
        if (mc.player.getBlockPos().equals(position) || blocksInTick >= blocksPerTick.get() || checkForPause(position)) return false;

        boolean placed = place(position, getBestBlock(), rotate.get(), renderSwing.get(), !onEntityPlace.get());

        if (placed) {
            blocksInTick++;
            renderBlocks.add(renderBlockPool.get().set(position));
        } else {
            checkForInterfering(position);
        }

        return placed;
    }

    private boolean place(BlockPos pos) {
        if (mc.player.getBlockPos().equals(pos) || blocksInTick >= blocksPerTick.get() || checkForPause(pos)) return false;
        boolean placed = place(pos, getBestBlock(), rotate.get(), renderSwing.get(), !onEntityPlace.get());

        if (placed) {
            blocksInTick++;
            renderBlocks.add(renderBlockPool.get().set(pos));
        } else {
            checkForInterfering(pos);
        }

        return placed;
    }

    private boolean place(BlockPos pos, FindItemResult item) {
        if (mc.player.getBlockPos().equals(pos) || blocksInTick >= blocksPerTick.get() || checkForPause(pos)) return false;
        boolean placed = place(pos, item, rotate.get(), renderSwing.get(), !onEntityPlace.get());

        if (placed) {
            blocksInTick++;
            renderBlocks.add(renderBlockPool.get().set(pos));
        }

        return placed;
    }

    private void checkForInterfering(BlockPos pos) {
        boolean mined = false;
        for (BlockBreakingInfo info : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            if (info.getPos().equals(pos)) {
                mined = true;
                break;
            }
        }

        if (interfering.get() || mined) {
            Box box = new Box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
            );

            for (Entity crystal : mc.world.getOtherEntities(null, box, entity -> entity instanceof EndCrystalEntity)) {
                if (isDangerousCrystal(crystal.getBlockPos()) && !crystals.contains(crystal.getId())) crystals.add(crystal.getId());
            }
        }

        if (breakBlocks.get() && !isSurroundBlock(mc.world.getBlockState(pos).getBlock())
            && (!packetPlace.get() || packetPlace.get()
            && !mc.world.getBlockState(pos).getMaterial().isReplaceable())) {
            breakPositions.add(pos);
        }
    }

    private void center(boolean repeat) {
        if (!center) return;

        if (smoothCenter.get() != SmoothCenter.None) {
            if (repeat && stabilize.get()) center(false);

            double[] dir = VectorUtils.directionSpeed(1.0f);

            double deltaX = Utils.clamp(mc.player.getBlockX() + 0.5 - mc.player.getX(), -0.05, 0.05);
            double deltaZ = Utils.clamp(mc.player.getBlockZ() + 0.5 - mc.player.getZ(), -0.05, 0.05);

            Vec3d vel = mc.player.getVelocity();

            double speed = repeat ? centerSpeed.get() : 0.4;

            double newX = (speed != 0 ? deltaX / speed : deltaX);
            double newZ = (speed != 0 ? deltaZ / speed : deltaZ);

            double x = (dir[0] > (unlock.get() / 10) || dir[0] < -(unlock.get() / 10)) ? vel.x : newX;
            double z = (dir[1] > (unlock.get() / 10) || dir[1] < -(unlock.get() / 10)) ? vel.z : newZ;

            switch (centerType.get()) {
                case Normal:
                    mc.player.setVelocity(x, mc.player.getVelocity().y, z);
                    break;
                case Set:
                    ((IVec3d) mc.player.getVelocity()).setXZ(x, z);
                    break;
                case TP:
                    if (!mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(x, 0, z)).iterator().hasNext()) {
                        mc.player.setPosition(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                        mc.player.setVelocity((speed != 0 ? vel.x / (speed * 2) : vel.x), vel.y, (speed != 0 ? vel.z / (speed * 2) : vel.z));
                    }

                    break;
            }
        } else {
            double x = MathHelper.floor(mc.player.getX()) + 0.5;
            double z = MathHelper.floor(mc.player.getZ()) + 0.5;

            mc.player.setPosition(x, mc.player.getY(), z);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        }
    }

    private boolean isDangerousCrystal(BlockPos position) {
        for (BlockPos p : surround) {
            for (BlockPos pos : plus) {
                if (mc.player.getBlockPos().add(p).add(pos).equals(position)
                    || mc.player.getBlockPos().add(p).equals(position)
                    || mc.player.getBlockPos().add(p).down().equals(position)) {
                    return true;
                }
            }
        }

        return true;
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
        return VectorUtils.findInHotbar(stack -> blocks.get().contains(Block.getBlockFromItem(stack.getItem())));
    }

    private boolean isInSurround(BlockPos position) {
        for (BlockPos pos : surround) {
            if (position.equals(mc.player.getBlockPos().add(pos))) return true;
        }

        return false;
    }

    private List<Direction> getClosestDirection(BlockPos pos) {
        List<Direction> directions = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (dir != null && mc.world.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) {
                directions.add(dir);
            }
        }

        if (directions.isEmpty()) directions.addAll(List.of(Direction.values()));

        directions.sort(Comparator.comparingDouble(dir -> dir != null ? VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(dir))) : VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos))));
        return directions;
    }

    private boolean isInRelativeArray(BlockPos position, ArrayList<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (position.equals(mc.player.getBlockPos().add(pos))) return true;
        }

        return false;
    }

    private boolean isSurrounded(BlockPos position) {
        int i = 0;

        for (BlockPos pos : getSurround()) {
            Block block = mc.world.getBlockState(position.add(pos)).getBlock();
            if (block != null && isSurroundBlock(block)) i++;
        }

        return i == 4;
    }

    private int getSurround(BlockPos position) {
        int i = 0;

        for (BlockPos pos : getSurround()) {
            Block block = mc.world.getBlockState(position.add(pos)).getBlock();
            if (block != null && block.getBlastResistance() >= 600.0F) i++;
        }

        return i;
    }

    private boolean checkForBed(BlockPos position) {
        int i = 0;

        for (BlockPos pos : getSurround()) {
            Block block = mc.world.getBlockState(position.add(pos)).getBlock();
            if (block != null && isSurroundBlock(block)) i++;
        }

        return i == 4;
    }

    private boolean checkForPause(BlockPos pos) {
        return !isInSurround(pos) && PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get());
    }

    private boolean doesIntersect(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !(entity instanceof EndCrystalEntity) && !(entity instanceof ItemEntity) && !(entity instanceof TntEntity));
    }

    private boolean isSurroundBlock(Block block) {
        return (blocks.get().contains(block) || block.getBlastResistance() >= 600.0F);
    }

    private boolean shouldAttackCrystal(BlockPos pos) {
        return !safe.get()
            || DamageUtils.crystalDamage(mc.player, Vec3d.ofCenter(pos)) < maxDamage.get()
            && DamageUtils.crystalDamage(mc.player, Vec3d.ofCenter(pos)) < PlayerUtils.getTotalHealth();
    }

    // Activating / Disabling interfering Modules

    private void activate() {
        if (!toActivate.isEmpty() && mc != null && mc.world != null && mc.player != null) {
            for (Module module : toActivate) {
                if (!module.isActive()) {
                    module.toggle();
                }
            }
        }
    }

    private void deactivate() {
        if (!modules.get().isEmpty() && mc != null && mc.world != null && mc.player != null) {
            for (Module module : modules.get()) {
                if (module.isActive()) {
                    module.toggle();
                    toActivate.add(module);
                }
            }
        }
    }

    // Placing

    private boolean place(BlockPos pos, FindItemResult item, boolean rotate, boolean swingHand, boolean checkEntities) {
        placedMap.putIfAbsent(pos, isInSurround(pos) ? innerDelay.get() : outerDelay.get());

        if (!bypass.get() || bypass.get() && placedMap.containsKey(pos) && (placedMap.get(pos) >= innerDelay.get() && isInSurround(pos) || placedMap.get(pos) >= outerDelay.get() && !isInSurround(pos))) {
            placedMap.replace(pos, 0);

            if (packetPlace.get()) {
                if (item != null && item.found()) {
                    if (item.isOffhand()) {
                        return place(pos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate, swingHand, checkEntities);
                    } else if (item.isHotbar()) {
                        return item.isHotbar() && place(pos, Hand.MAIN_HAND, item.getSlot(), rotate, swingHand, checkEntities);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return VectorUtils.place(pos, item, rotate, 100, swingHand, checkEntities, true);
            }
        } else {
            return false;
        }
    }

    private boolean place(BlockPos pos, Hand hand, int slot, boolean rotate, boolean swingHand, boolean checkEntities) {
        if (slot >= 0 && slot <= 8 || slot == 45) {
            if (!canPlace(pos, checkEntities)) {
                return false;
            } else {
                Vec3d hitPos = getHitPos(pos);
                Direction side = getSide(pos);
                BlockPos neighbour = getNeighbourPos(pos);

                boolean sneak = !mc.player.isSneaking() && VectorUtils.isClickable(mc.world.getBlockState(neighbour).getBlock());

                if (rotate) {
                    Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 100, () -> {
                        VectorUtils.swap(slot, swapBack.get());
                        place(new BlockHitResult(hitPos, side, neighbour, false), hand, swingHand, sneak);
                        if (swapBack.get()) VectorUtils.swapBack();
                    });
                } else {
                    VectorUtils.swap(slot, swapBack.get());
                    place(new BlockHitResult(hitPos, side, neighbour, false), hand, swingHand, sneak);
                    if (swapBack.get()) VectorUtils.swapBack();
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private void place(BlockHitResult result, Hand hand, boolean swing, boolean sneak) {
        if (hand != null && result != null && mc.world.getWorldBorder().contains(result.getBlockPos()) && mc.player.getStackInHand(hand).getItem() instanceof BlockItem) {
            if (sneak) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            }

            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

            Block block = ((BlockItem) mc.player.getStackInHand(hand).getItem()).getBlock();
            BlockSoundGroup group = block.getSoundGroup(block.getDefaultState());

            mc.getSoundManager().play(new PositionedSoundInstance(group.getPlaceSound(), SoundCategory.BLOCKS, (group.getVolume() + 1.0F) / 8.0F, group.getPitch() * 0.5F, result.getBlockPos()));

            if (sneak) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }

            if (swing) {
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

            hitPos = hitPos.add(
                side.getOffsetX() == 0 ? 0 : (side.getOffsetX() > 0 ? 0.5 : -0.5),
                side.getOffsetY() == 0 ? 0 : (side.getOffsetY() > 0 ? 0.5 : -0.5),
                side.getOffsetZ() == 0 ? 0 : (side.getOffsetZ() > 0 ? 0.5 : -0.5)
            );
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

    private Direction getPlaceSide(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            Direction direction = side.getOpposite();
            BlockState state = mc.world.getBlockState(neighbor);

            if (!state.getMaterial().isReplaceable() && state.getFluidState().isEmpty() && !VectorUtils.isClickable(mc.world.getBlockState(pos.offset(direction)).getBlock())) {
                return direction;
            }
        }

        return null;
    }

    private boolean canPlace(BlockPos pos, boolean checkEntities) {
        if (pos == null || mc.world == null || !mc.world.getBlockState(pos).getMaterial().isReplaceable()) return false;

        return !checkEntities || mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent());
    }

    private boolean canBreak(BlockPos pos) {
        if (pos == null) return false;
        return !blocks.get().contains(mc.world.getBlockState(pos).getBlock())
            && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= breakRange.get()
            && mc.world.getBlockState(pos).getHardness(mc.world, pos) > 0
            && !mc.world.getBlockState(pos).getOutlineShape(mc.world, pos).isEmpty();
    }

    private boolean checkHardness(BlockPos pos) {
        return mc.world.getBlockState(pos).getHardness(mc.world, pos) / mc.player.getMainHandStack().getMiningSpeedMultiplier(mc.world.getBlockState(pos)) <= maxHardness.get();
    }

    // Constants

    private final ArrayList<BlockPos> extra = new ArrayList<>() {{
        add(new BlockPos(2, 0, 0));
        add(new BlockPos(-2, 0, 0));
        add(new BlockPos(0, 0, 2));
        add(new BlockPos(0, 0, -2));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(1, 0, -1));
        add(new BlockPos(-1, 0, -1));
    }};

    private final ArrayList<BlockPos> plus = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(1, 0, 1));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(-1, 0, -1));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(-1, 0, 1));
        add(new BlockPos(0, 0, -1));
        add(new BlockPos(1, 0, -1));
    }};

    private final ArrayList<BlockPos> surround = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    private ArrayList<BlockPos> getSurround() {
        ArrayList<BlockPos> shuffle = new ArrayList<>(surround);
        Collections.shuffle(shuffle);

        return mode.get() == Mode.Strict ? surround : shuffle;
    }



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
        Set,
        TP
    }

    public enum ToggleMode {
        Both,
        Double,
        Top
    }

    public enum ScanMode {
        Update,
        Accurate,
        Both
    }

    public enum BreakMode {
        Start,
        Full,
        Both
    }

    public enum Mode {
        Strict,
        Random
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
