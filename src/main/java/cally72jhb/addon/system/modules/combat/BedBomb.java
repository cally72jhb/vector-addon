package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.mixin.RecipeResultCollectionAccessor;
import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.BlockEntityIterator;
import cally72jhb.addon.utils.misc.FindItemResult;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchableContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BedBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgCalc = settings.createGroup("Calculation");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgCity = settings.createGroup("Auto City");
    private final SettingGroup sgCraft = settings.createGroup("Craft");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgDisplay = settings.createGroup("Display");
    private final SettingGroup sgRender = settings.createGroup("Render");


    // General


    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius players can be in to be targeted.")
        .defaultValue(15)
        .sliderMin(5)
        .sliderMax(15)
        .min(0)
        .build()
    );

    private final Setting<Integer> maxTargets = sgGeneral.add(new IntSetting.Builder()
        .name("max-targets")
        .description("Maximum targets this module will target at once.")
        .defaultValue(2)
        .sliderMin(1)
        .sliderMax(5)
        .min(1)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place or break beds that can kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxSelfDamage = sgGeneral.add(new IntSetting.Builder()
        .name("max-self-damage")
        .description("The maximum damage a bed can deal you for it to be placed or broken.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(36)
        .min(0)
        .max(36)
        .noSlider()
        .build()
    );

    private final Setting<Integer> minTargetDamage = sgGeneral.add(new IntSetting.Builder()
        .name("min-target-damage")
        .description("The minimum damage a bed has to deal a target for it to be placed or broken.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(36)
        .min(0)
        .max(36)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> checkFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-friends")
        .description("Checks the damage to friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiFriendPop = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-friend-pop")
        .description("Stops you from placing or breaking beds that pop your friends.")
        .defaultValue(true)
        .visible(checkFriends::get)
        .build()
    );

    private final Setting<Integer> maxFriendDamage = sgGeneral.add(new IntSetting.Builder()
        .name("max-friend-damage")
        .description("The maximum damage a bed can deal to friends for it to be broken.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(10)
        .min(0)
        .max(36)
        .visible(checkFriends::get)
        .noSlider()
        .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("How targets are selected.")
        .defaultValue(SortMode.LowestDistance)
        .build()
    );

    private final Setting<Boolean> pitchRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("pitch-rotate")
        .description("Faces the bed your placing pitch-wise.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debugMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-log")
        .description("Prints out debug messages when some calculations fail.")
        .defaultValue(true)
        .build()
    );


    // Bypass


    private final Setting<Boolean> packetPlace = sgBypass.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Places the beds with packets instead of the normal way.")
        .defaultValue(true)
        .build()
    );


    // Keybindings


    private final Setting<Keybind> speedPlace = sgBypass.add(new KeybindSetting.Builder()
        .name("speed-place")
        .description("The keybinding used to speed up placement.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> speedBreak = sgBypass.add(new KeybindSetting.Builder()
        .name("speed-break")
        .description("The keybinding used to speed up bed breaking.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Integer> speedPlaceDelay = sgBypass.add(new IntSetting.Builder()
        .name("speed-place-delay")
        .description("How many ticks to wait before placing a bed when the force speed place key is pressed.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .build()
    );

    private final Setting<Integer> speedBreakDelay = sgBypass.add(new IntSetting.Builder()
        .name("speed-break-delay")
        .description("How many ticks to wait before breaking a bed when the force speed break key is pressed.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .build()
    );


    // Calculation


    private final Setting<Boolean> smartDelay = sgCalc.add(new BoolSetting.Builder()
        .name("smart-delay")
        .description("Calculates the damage under consideration of the targets hurt time.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> smartCalc = sgCalc.add(new BoolSetting.Builder()
        .name("smart-calc")
        .description("Will not raycast from the irrelevant blocks that will probably not hurt any target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> calcRange = sgCalc.add(new DoubleSetting.Builder()
        .name("calc-range")
        .description("The radius around targets that is considered a valid position which deals damage.")
        .defaultValue(10)
        .sliderMin(7.5)
        .sliderMax(10)
        .min(1)
        .visible(smartCalc::get)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgCalc.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predicts the players next movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTerrain = sgCalc.add(new BoolSetting.Builder()
        .name("ignore-terrain")
        .description("Ignores the explodable terrain around you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreGamemode = sgCalc.add(new BoolSetting.Builder()
        .name("ignore-gamemode")
        .description("Ignores the targets gamemode.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> stabilize = sgCalc.add(new BoolSetting.Builder()
        .name("stabilize")
        .description("Stabilizes the bed bomb calculation.")
        .defaultValue(false)
        .build()
    );

    private final Setting<RaytraceMode> raytraceMode = sgCalc.add(new EnumSetting.Builder<RaytraceMode>()
        .name("raytrace-mode")
        .description("From what position to check if the beds deal damage.")
        .defaultValue(RaytraceMode.Eyes)
        .build()
    );

    private final Setting<Boolean> placeThreading = sgCalc.add(new BoolSetting.Builder()
        .name("place-threading")
        .description("Calculates the placing in a separate thread to remove lag spikes.")
        .defaultValue(true)
        .onChanged(bool -> {
            multiThread = new ArrayList<>();
            bestPositions = new ArrayList<>();

            placeThread = null;
            breakThread = null;
        })
        .build()
    );

    private final Setting<Boolean> multiPlaceThreading = sgCalc.add(new BoolSetting.Builder()
        .name("multithreading-place")
        .description("Uses multiple threads to calculate the placing positions.")
        .defaultValue(false)
        .onChanged(bool -> {
            multiThread = new ArrayList<>();
            bestPositions = new ArrayList<>();

            placeThread = null;
            breakThread = null;
        })
        .visible(placeThreading::get)
        .build()
    );

    private final Setting<Integer> placeThreads = sgCalc.add(new IntSetting.Builder()
        .name("place-threads")
        .description("How many threads should be used for placing.")
        .defaultValue(4)
        .sliderMin(2)
        .sliderMax(16)
        .min(2)
        .max(128)
        .onChanged(integer -> {
            multiThread = new ArrayList<>();
            bestPositions = new ArrayList<>();

            placeThread = null;
            breakThread = null;
        })
        .visible(() -> placeThreading.get() && multiPlaceThreading.get())
        .noSlider()
        .build()
    );

    private final Setting<Boolean> smartPlaceThreading = sgCalc.add(new BoolSetting.Builder()
        .name("smart-place-threading")
        .description("Decides whether to use threads or not.")
        .defaultValue(false)
        .onChanged(bool -> {
            multiThread = new ArrayList<>();
            bestPositions = new ArrayList<>();

            placeThread = null;
            breakThread = null;
        })
        .build()
    );

    private final Setting<Boolean> breakThreading = sgCalc.add(new BoolSetting.Builder()
        .name("break-threading")
        .description("Calculates the breaking in a separate thread to remove lag spikes.")
        .defaultValue(true)
        .build()
    );


    // Inventory Clicking


    private final Setting<ClickMode> clickMode = sgInventory.add(new EnumSetting.Builder<ClickMode>()
        .name("click-mode")
        .description("How slots in your inventory are clicked.")
        .defaultValue(ClickMode.Packet)
        .build()
    );


    // Inventory Swapping


    private final Setting<SwitchMode> switchMode = sgInventory.add(new EnumSetting.Builder<SwitchMode>()
        .name("auto-switch-mode")
        .description("How to swap to the slots where the beds are.")
        .defaultValue(SwitchMode.Normal)
        .build()
    );

    private final Setting<Boolean> swapBack = sgInventory.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the selected slot after you placed the bed.")
        .defaultValue(true)
        .build()
    );


    // Inventory Moving


    private final Setting<Boolean> autoMove = sgInventory.add(new BoolSetting.Builder()
        .name("auto-move")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> itemsPerTick = sgInventory.add(new IntSetting.Builder()
        .name("items-per-tick")
        .description("How many items are moved in one tick.")
        .defaultValue(1)
        .sliderMin(1)
        .sliderMax(5)
        .min(1)
        .max(10)
        .visible(autoMove::get)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> bedMove = sgInventory.add(new BoolSetting.Builder()
        .name("move-bed")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(true)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> tableMove = sgInventory.add(new BoolSetting.Builder()
        .name("move-table")
        .description("Moves the crafting table into a selected hotbar slot.")
        .defaultValue(true)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> fixedMove = sgInventory.add(new BoolSetting.Builder()
        .name("fixed-move")
        .description("Moves beds into a set hotbar slot if there are no empty slots.")
        .defaultValue(false)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<SwapMode> swapMode = sgInventory.add(new EnumSetting.Builder<SwapMode>()
        .name("swap-mode")
        .description("How items are swapped in your inventory.")
        .defaultValue(SwapMode.Pickup)
        .visible(() -> autoMove.get() && fixedMove.get())
        .build()
    );

    private final Setting<Integer> bedSlot = sgInventory.add(new IntSetting.Builder()
        .name("bed-slot")
        .description("In which slot the bed should be put.")
        .defaultValue(3)
        .sliderMin(1)
        .sliderMax(9)
        .min(1)
        .max(9)
        .visible(() -> autoMove.get() && fixedMove.get() && bedMove.get())
        .noSlider()
        .build()
    );

    private final Setting<Integer> tableSlot = sgInventory.add(new IntSetting.Builder()
        .name("table-slot")
        .description("In which slot the crafting table should be put.")
        .defaultValue(5)
        .sliderMin(1)
        .sliderMax(9)
        .min(1)
        .max(9)
        .visible(() -> autoMove.get() && fixedMove.get() && tableMove.get())
        .noSlider()
        .build()
    );

    private final Setting<Boolean> fillEmptySlots = sgInventory.add(new BoolSetting.Builder()
        .name("fill-empty-slots")
        .description("Fills empty slots in your hotbar with beds.")
        .defaultValue(true)
        .visible(autoMove::get)
        .build()
    );


    // Place


    private final Setting<PlaceTicking> placeTicking = sgPlace.add(new EnumSetting.Builder<PlaceTicking>()
        .name("place-ticking")
        .description("Whether to place a bed after the old one is broken or before.")
        .defaultValue(PlaceTicking.Pre)
        .build()
    );

    private final Setting<PlaceMode> placeMode = sgPlace.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("What blocks to scan first.")
        .defaultValue(PlaceMode.Smart)
        .build()
    );

    private final Setting<PlaceScanMode> placeScanMode = sgPlace.add(new EnumSetting.Builder<PlaceScanMode>()
        .name("place-scan-mode")
        .description("What blocks to scan first.")
        .defaultValue(PlaceScanMode.Closest)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The radius around you in which beds can be placed in.")
        .defaultValue(4.5)
        .sliderMin(0)
        .sliderMax(8)
        .min(1)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("How many ticks to wait before placing a bed.")
        .defaultValue(10)
        .sliderMin(0)
        .sliderMax(15)
        .min(0)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Boolean> resetDelayOnPlace = sgPlace.add(new BoolSetting.Builder()
        .name("reset-delay-on-place")
        .description("Resets the break delay after placing a bed.")
        .defaultValue(false)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Boolean> airPlace = sgPlace.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Places beds in the air.")
        .defaultValue(true)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Boolean> allowPlaceInside = sgPlace.add(new BoolSetting.Builder()
        .name("allow-place-inside")
        .description("Allows the module to place beds inside of players.")
        .defaultValue(true)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Boolean> waitForBreak = sgPlace.add(new BoolSetting.Builder()
        .name("wait-for-break")
        .description("Will wait for the bed to be broken before placing again.")
        .defaultValue(true)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Boolean> checkFriendsOnPlace = sgPlace.add(new BoolSetting.Builder()
        .name("check-friends-on-place")
        .description("Will check the damage a bed deals friends before placing it.")
        .defaultValue(true)
        .visible(() -> checkFriends.get() && placeMode.get() != PlaceMode.None)
        .build()
    );


    // Break


    private final Setting<BreakMode> breakMode = sgBreak.add(new EnumSetting.Builder<BreakMode>()
        .name("break-mode")
        .description("How the beds are broken.")
        .defaultValue(BreakMode.Both)
        .build()
    );

    private final Setting<BreakScanMode> breakScanMode = sgBreak.add(new EnumSetting.Builder<BreakScanMode>()
        .name("break-scan-mode")
        .description("What beds are broken first.")
        .defaultValue(BreakScanMode.Closest)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("The radius around you in which beds can be broken in.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(10)
        .min(1)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("How many ticks to wait before breaking a bed.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Boolean> resetDelayOnBreak = sgBreak.add(new BoolSetting.Builder()
        .name("reset-delay-on-break")
        .description("Resets the place delay after breaking the bed.")
        .defaultValue(true)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Boolean> antiSuicideBreak = sgBreak.add(new BoolSetting.Builder()
        .name("anti-suicide-break")
        .description("Will not break beds that can kill or pop you.")
        .defaultValue(true)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Boolean> checkSelfBreak = sgBreak.add(new BoolSetting.Builder()
        .name("check-self")
        .description("Checks the damage the bed deals you.")
        .defaultValue(true)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Boolean> checkFriendsOnBreak = sgBreak.add(new BoolSetting.Builder()
        .name("check-friends-on-break")
        .description("Will check the damage a bed deals friends before breaking it.")
        .defaultValue(true)
        .visible(() -> checkFriends.get() && breakMode.get() != BreakMode.None)
        .build()
    );


    // Auto City


    private final Setting<Boolean> autoCity = sgCity.add(new BoolSetting.Builder()
        .name("auto-city")
        .description("Breaks the targets surround to allow the bed bomb to deal damage again.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> forceCity = sgCity.add(new KeybindSetting.Builder()
        .name("force-city")
        .description("The keybinding used to city your targets.")
        .defaultValue(Keybind.fromKey(-1))
        .visible(autoCity::get)
        .build()
    );

    private final Setting<Boolean> automatic = sgCity.add(new BoolSetting.Builder()
        .name("automatic")
        .description("Will automatically start citing targets that are fully surrounded.")
        .defaultValue(false)
        .visible(autoCity::get)
        .build()
    );

    private final Setting<Boolean> rotate = sgCity.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the blocks being broken.")
        .defaultValue(true)
        .visible(autoCity::get)
        .build()
    );

    private final Setting<Integer> cityDelay = sgCity.add(new IntSetting.Builder()
        .name("city-delay")
        .description("How many ticks to wait before citing again.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .visible(autoCity::get)
        .noSlider()
        .build()
    );

    private final Setting<Double> cityRange = sgCity.add(new DoubleSetting.Builder()
        .name("city-range")
        .description("How far city can mine and place blocks.")
        .defaultValue(4.5)
        .sliderMin(0)
        .sliderMax(8)
        .min(1)
        .visible(autoCity::get)
        .build()
    );

    private final Setting<CityScanMode> cityScanMode = sgCity.add(new EnumSetting.Builder<CityScanMode>()
        .name("city-scan-mode")
        .description("What blocks to prioritize.")
        .defaultValue(CityScanMode.Closest)
        .visible(autoCity::get)
        .build()
    );

    private final Setting<Boolean> instaMine = sgCity.add(new BoolSetting.Builder()
        .name("insta-mine")
        .description("Tryes to mine blocks instant.")
        .defaultValue(false)
        .visible(autoCity::get)
        .build()
    );

    private final Setting<Double> maxHardness = sgCity.add(new DoubleSetting.Builder()
        .name("max-hardness")
        .description("The maximum hardness a block can have for it to be instant mined.")
        .defaultValue(0.3)
        .min(0)
        .sliderMax(1)
        .visible(() -> autoCity.get() && instaMine.get())
        .build()
    );

    private final Setting<Integer> instaTickDelay = sgCity.add(new IntSetting.Builder()
        .name("mine-tick-delay")
        .description("The delay between the attempted breaks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(() -> autoCity.get() && instaMine.get())
        .build()
    );

    private final Setting<Boolean> pausePlacingOnCity = sgCity.add(new BoolSetting.Builder()
        .name("pause-placing-on-city")
        .description("Pauses the normal placing while citing.")
        .defaultValue(true)
        .visible(autoCity::get)
        .build()
    );

    private final Setting<Boolean> pauseBreakingOnCity = sgCity.add(new BoolSetting.Builder()
        .name("pause-breaking-on-city")
        .description("Pauses the normal breaking while citing.")
        .defaultValue(true)
        .visible(autoCity::get)
        .build()
    );


    // Auto Craft


    private final Setting<Boolean> autoCraft = sgCraft.add(new BoolSetting.Builder()
        .name("auto-craft")
        .description("Automatically crafts new beds if your out of beds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> craftPlace = sgCraft.add(new BoolSetting.Builder()
        .name("place-table")
        .description("Automatically places a crafting table near you.")
        .defaultValue(true)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> smartPrioritization = sgCraft.add(new BoolSetting.Builder()
        .name("smart-prioritization")
        .description("Will exclude irrelevant items when looping through the recipes.")
        .defaultValue(false)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> smartTablePlace = sgCraft.add(new BoolSetting.Builder()
        .name("smart-table-place")
        .description("Automatically opens the crafting table.")
        .defaultValue(true)
        .visible(() -> autoCraft.get() && craftPlace.get())
        .build()
    );

    private final Setting<Boolean> openRecipeBook = sgCraft.add(new BoolSetting.Builder()
        .name("open-recipe-book")
        .description("Automatically opens the recipe book to bypass some anti cheats.")
        .defaultValue(true)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> autoOpen = sgCraft.add(new BoolSetting.Builder()
        .name("auto-open")
        .description("Automatically opens the crafting table.")
        .defaultValue(true)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Double> craftRange = sgCraft.add(new DoubleSetting.Builder()
        .name("table-range")
        .description("The radius around you in which the crafting table / opened will be placed.")
        .defaultValue(2.5)
        .min(1)
        .sliderMin(2)
        .sliderMax(5)
        .visible(() -> autoCraft.get() && (craftPlace.get() || autoOpen.get()))
        .build()
    );

    private final Setting<Boolean> craftOnlyOnGround = sgCraft.add(new BoolSetting.Builder()
        .name("only-on-ground-craft")
        .description("Only crafts when your on ground.")
        .defaultValue(true)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> craftOnlyInHole = sgCraft.add(new BoolSetting.Builder()
        .name("only-in-hole-craft")
        .description("Only crafts when your in a safe hole.")
        .defaultValue(false)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> silentCraft = sgCraft.add(new BoolSetting.Builder()
        .name("silent-craft")
        .description("Hides the crafting inventory screen client-side.")
        .defaultValue(false)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> antiDesync = sgCraft.add(new BoolSetting.Builder()
        .name("anti-desync")
        .description("Updates your inventory after being done with crafting.")
        .defaultValue(true)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> bypass = sgCraft.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Trys to avoid flagging for crafting too often.")
        .defaultValue(false)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Integer> craftDelay = sgCraft.add(new IntSetting.Builder()
        .name("craft-delay")
        .description("How many ticks to wait before crafting the next beds.")
        .defaultValue(1)
        .min(0)
        .sliderMin(0)
        .sliderMax(10)
        .visible(autoCraft::get)
        .noSlider()
        .build()
    );

    private final Setting<Integer> grabTimes = sgCraft.add(new IntSetting.Builder()
        .name("grab-times")
        .description("How often beds are grabbed while crafting.")
        .defaultValue(1)
        .min(1)
        .sliderMin(1)
        .sliderMax(3)
        .visible(() -> autoCraft.get() && bypass.get())
        .noSlider()
        .build()
    );

    private final Setting<Integer> minCraftHealth = sgCraft.add(new IntSetting.Builder()
        .name("min-craft-health")
        .description("Min health required to be able to craft new beds.")
        .defaultValue(5)
        .min(0)
        .max(36)
        .sliderMin(0)
        .sliderMax(36)
        .visible(autoCraft::get)
        .noSlider()
        .build()
    );

    private final Setting<Integer> bedAmount = sgCraft.add(new IntSetting.Builder()
        .name("craft-amount")
        .description("How many beds to craft at once.")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .sliderMax(10)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Integer> minBeds = sgCraft.add(new IntSetting.Builder()
        .name("min-beds")
        .description("From how few beds in your inventory new ones should be crafted again.")
        .defaultValue(2)
        .min(0)
        .sliderMin(0)
        .sliderMax(5)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Integer> minEmptySlots = sgCraft.add(new IntSetting.Builder()
        .name("min-empty-slots")
        .description("How many slots in your inventory have to be empty to craft new beds.")
        .defaultValue(1)
        .min(1)
        .sliderMin(1)
        .sliderMax(10)
        .visible(autoCraft::get)
        .build()
    );


    // Pause


    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses bed-aura when your eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses bed-aura when your drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> killAuraPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-kill-aura")
        .description("Pauses bed-aura when kill aura is active.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> crystalAuraPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-crystal-aura")
        .description("Pauses bed-aura when crystal aura is active.")
        .defaultValue(false)
        .build()
    );


    // Display


    private final Setting<Boolean> display = sgDisplay.add(new BoolSetting.Builder()
        .name("display")
        .description("Displays information behind the modules lore.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayTarget = sgDisplay.add(new BoolSetting.Builder()
        .name("display-target")
        .description("Displays the current target.")
        .defaultValue(true)
        .visible(display::get)
        .build()
    );

    private final Setting<Boolean> displayState = sgDisplay.add(new BoolSetting.Builder()
        .name("display-state")
        .description("Displays the current placing or breaking state.")
        .defaultValue(false)
        .visible(display::get)
        .build()
    );

    private final Setting<Boolean> displayDelay = sgDisplay.add(new BoolSetting.Builder()
        .name("display-delay")
        .description("Displays the current delay.")
        .defaultValue(true)
        .visible(display::get)
        .build()
    );

    private final Setting<Boolean> displayMS = sgDisplay.add(new BoolSetting.Builder()
        .name("display-ms")
        .description("Displays the time it takes to calculate.")
        .defaultValue(true)
        .visible(display::get)
        .build()
    );


    // Render


    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand swing animation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<RenderType> renderType = sgRender.add(new EnumSetting.Builder<RenderType>()
        .name("render-type")
        .description("How the beds are rendered.")
        .defaultValue(RenderType.Advanced)
        .build()
    );

    private final Setting<Boolean> renderOnlyOnce = sgRender.add(new BoolSetting.Builder()
        .name("only-once-render")
        .description("Doesn't render a place animation, if there already is a block being rendered.")
        .defaultValue(true)
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<Integer> ticksLeftPercent = sgRender.add(new IntSetting.Builder()
        .name("ticks-left-percent")
        .description("How many ticks have to be left in percent for the beds to be rendered.")
        .defaultValue(75)
        .min(1)
        .max(100)
        .sliderMin(1)
        .sliderMax(75)
        .visible(() -> renderType.get() != RenderType.None && renderOnlyOnce.get())
        .noSlider()
        .build()
    );

    private final Setting<Integer> renderTicks = sgRender.add(new IntSetting.Builder()
        .name("render-ticks")
        .description("How many ticks it should take for a block to disappear.")
        .defaultValue(15)
        .min(5)
        .max(50)
        .sliderMin(5)
        .sliderMax(25)
        .visible(() -> renderType.get() != RenderType.None)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> renderExtra = sgRender.add(new BoolSetting.Builder()
        .name("render-extra")
        .description("Renders a extra element in the middle of the bed.")
        .defaultValue(false)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Boolean> shrink = sgRender.add(new BoolSetting.Builder()
        .name("shrink")
        .description("Shrinks the block overlay after a while.")
        .defaultValue(false)
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<Integer> shrinkTicks = sgRender.add(new IntSetting.Builder()
        .name("shrink-ticks")
        .description("How many ticks to wait before shrinking the block.")
        .defaultValue(5)
        .min(0)
        .max(50)
        .sliderMax(25)
        .visible(() -> renderType.get() != RenderType.None && shrink.get())
        .noSlider()
        .build()
    );

    private final Setting<Double> shrinkSpeed = sgRender.add(new DoubleSetting.Builder()
        .name("shrink-speed")
        .description("How fast to shrink the overlay.")
        .defaultValue(2.5)
        .min(1)
        .max(50)
        .sliderMin(1)
        .sliderMax(25)
        .visible(() -> renderType.get() != RenderType.None && shrink.get())
        .noSlider()
        .build()
    );

    private final Setting<Boolean> renderInnerLines = sgRender.add(new BoolSetting.Builder()
        .name("render-inner-lines")
        .description("Renders the inner lines of the bed.")
        .defaultValue(true)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Boolean> renderInnerSides = sgRender.add(new BoolSetting.Builder()
        .name("render-inner-sides")
        .description("Renders the inner sides of the bed.")
        .defaultValue(true)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Double> feetLength = sgRender.add(new DoubleSetting.Builder()
        .name("feet-lenght")
        .description("How long the feet of the bed are.")
        .defaultValue(1.875)
        .min(1.25)
        .max(3)
        .sliderMin(1.25)
        .sliderMax(3)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Double> bedHeight = sgRender.add(new DoubleSetting.Builder()
        .name("bed-height")
        .description("How high the bed is.")
        .defaultValue(5.62)
        .min(4.5)
        .max(6.5)
        .sliderMin(4.5)
        .sliderMax(6.5)
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> sideColorTop = sgRender.add(new ColorSetting.Builder()
        .name("side-color-top")
        .description("The top side color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 5, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> sideColorBottom = sgRender.add(new ColorSetting.Builder()
        .name("side-color-bottom")
        .description("The bottom side color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 25, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> lineColorTop = sgRender.add(new ColorSetting.Builder()
        .name("line-color-top")
        .description("The top line color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 50, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> lineColorBottom = sgRender.add(new ColorSetting.Builder()
        .name("line-color-bottom")
        .description("The bottom line color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 255, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private List<PlayerEntity> targets = new ArrayList<>();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private SearchableContainer<RecipeResultCollection> container;

    private int placeTicks;
    private int breakTicks;
    private int craftTicks;
    private int cityTicks;
    private int instaTicks;

    private int slot = -1;
    private boolean citing;

    private List<Thread> multiThread;
    private List<Triplet<BlockPos, Direction, Double>> bestPositions;

    private Thread placeThread;
    private Thread breakThread;

    private Direction placeDir;
    private BlockPos placePos;
    private BlockPos breakPos;

    private BlockPos breakingPos;

    private Explosion explosion;
    private RaycastContext raycastContext;

    private CraftingScreenHandler silentHandler;

    private State state;

    private long calcTime;

    public BedBomb() {
        super(Categories.Combat, "bed-bomber", "Places and blows up beds near targets to deal alot of damage.");
    }

    @Override
    public void onActivate() {
        targets.clear();

        placeTicks = 0;
        breakTicks = 0;
        craftTicks = 0;
        cityTicks = 0;
        instaTicks = 0;

        calcTime = 0;

        slot = -1;
        citing = false;

        placeDir = null;
        placePos = null;
        breakPos = null;

        silentHandler = null;

        state = State.Idling;

        multiThread = new ArrayList<>();
        bestPositions = new ArrayList<>();

        if (placeThread != null && placeThread.isAlive()) placeThread.interrupt();
        if (breakThread != null && breakThread.isAlive()) breakThread.interrupt();
        placeThread = null;
        breakThread = null;

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }

        if (mc != null && mc.world != null && mc.player != null) {
            if (mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);

            explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
            raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc != null && mc.world != null && mc.player != null && mc.getNetworkHandler() != null && (silentHandler != null || mc.player.currentScreenHandler != null)) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(silentCraft.get() && silentHandler != null ? silentHandler.syncId : mc.player.currentScreenHandler.syncId));
            mc.player.currentScreenHandler = mc.player.playerScreenHandler;
            mc.setScreen(null);

            silentHandler = null;
        }

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @Override
    public String getInfoString() {
        if (display.get()) {
            String info = "";

            if (displayTarget.get() && !targets.isEmpty()) info += "[" + targets.get(0).getGameProfile().getName() + "] ";
            if (displayDelay.get()) {
                if (state == State.Placing) info += "[" + (speedPlace.get().isPressed() ? speedPlaceDelay.get() : placeDelay.get()) + (displayMS.get() ? " | " : "] ");
                else if (state == State.Breaking) info += "[" + (speedBreak.get().isPressed() ? speedBreakDelay.get() : breakDelay.get()) + (displayMS.get() ? " | " : "] ");
                else info += "[0" + (displayMS.get() ? " | " : "] ");
            }
            if (displayMS.get()) info += (!displayDelay.get() ? "[" : "") + calcTime + "ms]";
            if (displayState.get()) info += " [" + state + "]";

            if (info.endsWith(" ")) info = info.substring(0, info.length() - 1);

            return info.isEmpty() ? null : info;
        }

        return null;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        targets.clear();

        placeTicks++;
        breakTicks++;
        craftTicks++;
        cityTicks++;
        instaTicks++;

        citing = false;

        if (shouldPause()) return;
        if (mc.world.getDimension().isBedWorking()) {
            error("You can't blow up beds in this dimension, disabling.");
            toggle();
            return;
        }

        if (autoMove.get()) {
            FindItemResult bed = VectorUtils.find(stack -> stack.getItem() instanceof BedItem, 9, 35);
            FindItemResult table = VectorUtils.find(stack -> stack.getItem() instanceof BedItem, 9, 35);

            if (slot >= 0 && slot <= 8) {
                clickSlot(0, slot, 1, SlotActionType.PICKUP);
                slot = -1;
            } else {
                if (tableMove.get() && table.found() && !table.isOffhand()) {
                    boolean move = true;

                    for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).getItem() == Items.CRAFTING_TABLE) move = false;

                    if (move) {
                        if (fixedMove.get() && getEmptySlots(0, 8) <= 1) {
                            if (swapMode.get() == SwapMode.Pickup) {
                                boolean extraClick = isEmpty(mc.player.getInventory().getStack(table.getSlot()));

                                clickSlot(0, table.getSlot(), 1, SlotActionType.PICKUP);
                                clickSlot(0, tableSlot.get() + 35, 1, SlotActionType.PICKUP);
                                if (extraClick) slot = table.getSlot();
                            } else {
                                clickSlot(0, table.getSlot(), tableSlot.get(), SlotActionType.SWAP);
                            }
                        } else if (fillEmptySlots.get()) {
                            for (int i = 0; i < 9; i++) {
                                if (isEmpty(mc.player.getInventory().getStack(i))) {
                                    clickSlot(0, table.getSlot(), 1, SlotActionType.QUICK_MOVE);

                                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                                    break;
                                }
                            }
                        }
                    }
                }

                if (bedMove.get() && bed.found() && !bed.isOffhand()) {
                    if (fixedMove.get() && getEmptySlots(0, 8) <= 0) {
                        boolean move = true;

                        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).getItem() instanceof BedItem) move = false;

                        if (move) {
                            if (swapMode.get() == SwapMode.Pickup) {
                                boolean extraClick = isEmpty(mc.player.getInventory().getStack(bed.getSlot()));

                                clickSlot(0, bed.getSlot(), 1, SlotActionType.PICKUP);
                                clickSlot(0, (bedSlot.get().equals(tableSlot.get()) ? (tableSlot.get() >= 8 ? tableSlot.get() - 1 : tableSlot.get() + 1) : bedSlot.get()) + 35, 1, SlotActionType.PICKUP);
                                if (extraClick) slot = bed.getSlot();
                            } else {
                                clickSlot(0, bed.getSlot(), bedSlot.get(), SlotActionType.SWAP);
                            }
                        }
                    } else if (fillEmptySlots.get()) {
                        int item = 0;

                        for (int i = 0; i <= 8; i++) {
                            if (isEmpty(mc.player.getInventory().getStack(i))) {
                                clickSlot(0, bed.getSlot(), 1, SlotActionType.QUICK_MOVE);

                                item++;

                                if (antiDesync.get()) mc.player.getInventory().updateItems();
                                if (item >= itemsPerTick.get()) break;
                            }
                        }
                    }
                }
            }
        }

        if (autoCraft.get() && (craftTicks >= craftDelay.get() || craftDelay.get() == 0)) {
            if (autoOpen.get() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
                if (canRefill() && needRefill() && !isInventoryFull()) {
                    FindItemResult table = VectorUtils.findInHotbar(stack -> stack.getItem() == Items.CRAFTING_TABLE);

                    if (table.found()) {
                        BlockPos pos = null;
                        ArrayList<BlockPos> placePositions = new ArrayList<>();

                        double pX = mc.player.getX() - 0.5;
                        double pY = mc.player.getY();
                        double pZ = mc.player.getZ() - 0.5;

                        int craftMinX = (int) Math.floor(pX - craftRange.get());
                        int craftMinY = (int) Math.floor(pY - craftRange.get());
                        int craftMinZ = (int) Math.floor(pZ - craftRange.get());

                        int craftMaxX = (int) Math.floor(pX + craftRange.get());
                        int craftMaxY = (int) Math.floor(pY + craftRange.get());
                        int craftMaxZ = (int) Math.floor(pZ + craftRange.get());

                        double craftRangeSq = Math.pow(craftRange.get(), 2);

                        for (int y = craftMinY; y <= craftMaxY; y++) {
                            for (int x = craftMinX; x <= craftMaxX; x++) {
                                for (int z = craftMinZ; z <= craftMaxZ; z++) {
                                    if (VectorUtils.distance(pX, pY, pZ, x, y, z) <= craftRangeSq) {
                                        BlockPos position = new BlockPos(x, y, z);

                                        if (VectorUtils.getBlock(position) == Blocks.CRAFTING_TABLE) {
                                            pos = position;
                                            break;
                                        } else if (VectorUtils.getBlockState(position).getMaterial().isReplaceable()) {
                                            placePositions.add(position);
                                        }
                                    }
                                }
                            }
                        }

                        if (pos != null) {
                            BlockHitResult result = new BlockHitResult(Vec3d.ofCenter(pos), getSide(pos), pos, false);

                            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));

                            swingHand(Hand.MAIN_HAND);
                        } else if (!placePositions.isEmpty() && craftPlace.get()) {
                            List<BlockPos> insecure = smartTablePlace.get() ? getAffectedBlocks(mc.player.getBlockPos()) : new ArrayList<>();

                            placePositions.sort(Comparator.comparingDouble(position -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(position))));

                            for (BlockPos position : placePositions) {
                                if (canPlace(position, Blocks.CRAFTING_TABLE.getDefaultState(), true)
                                    && (!smartTablePlace.get() || smartTablePlace.get() && !insecure.contains(position))
                                    && (switchMode.get() != SwitchMode.None || (switchMode.get() == SwitchMode.None
                                    && mc.player.getOffHandStack().getItem() == Items.CRAFTING_TABLE
                                    && mc.player.getMainHandStack().getItem() == Items.CRAFTING_TABLE))) {
                                    place(position, null, table, renderSwing.get(), true);
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (mc.player.currentScreenHandler instanceof CraftingScreenHandler || silentHandler != null && silentCraft.get()) {
                CraftingScreenHandler handler = silentHandler != null && silentCraft.get() ? silentHandler : (CraftingScreenHandler) mc.player.currentScreenHandler;

                if (!canRefill() || !needRefill() || isInventoryFull()) {
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
                    mc.player.currentScreenHandler = mc.player.playerScreenHandler;
                    mc.setScreen(null);

                    if (antiDesync.get()) mc.player.getInventory().updateItems();

                    state = State.Idling;
                    silentHandler = null;
                } else {
                    if (openRecipeBook.get() && mc.player.getRecipeBook() != null && !mc.player.getRecipeBook().isGuiOpen(RecipeBookCategory.CRAFTING)) {
                        mc.player.getRecipeBook().setGuiOpen(RecipeBookCategory.CRAFTING, true);
                    }

                    state = State.Crafting;

                    if (container == null && mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);

                    for (RecipeResultCollection collection : container.findAll(smartPrioritization.get() ? "bed" : "")) {
                        for (Recipe<?> recipe : ((RecipeResultCollectionAccessor) collection).getRecipes()) {
                            if (canCraft(recipe) && recipe.getOutput().getItem() instanceof BedItem) {
                                int grab = 0;

                                for (int i = 0; i < getEmptySlots(0, 35) && i <= bedAmount.get(); i++) {
                                    if (mc.interactionManager != null && clickMode.get() == ClickMode.Normal) {
                                        mc.interactionManager.clickRecipe(handler.syncId, recipe, false);
                                    } else {
                                        mc.getNetworkHandler().sendPacket(new CraftRequestC2SPacket(handler.syncId, recipe, false));
                                    }

                                    if (bypass.get() && grabTimes.get() != 1 && grab < grabTimes.get()) {
                                        clickSlot(handler.syncId, 0, 1, SlotActionType.QUICK_MOVE);
                                        grab++;
                                    }
                                }

                                clickSlot(handler.syncId, 0, 1, SlotActionType.QUICK_MOVE);

                                craftTicks = 0;
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isAlive() && !player.isDead() && player != mc.player && Friends.get().shouldAttack(player) && !(player instanceof FakePlayerEntity)
                && VectorUtils.distance(mc.player.getPos(), player.getPos()) <= targetRange.get()) {
                targets.add(player);
            }
        }

        for (FakePlayerEntity player : FakePlayerManager.getPlayers()) {
            if (VectorUtils.distance(mc.player.getPos(), player.getPos()) <= targetRange.get()) targets.add(player);
        }

        // Placing & Breaking & Citing

        if (!targets.isEmpty()) {
            targets = targets.stream().sorted(Comparator.comparing(player -> Players.get().isTargeted(player))).collect(Collectors.toList());

            switch (sortMode.get()) {
                case LowestHealth -> targets.sort(Comparator.comparingDouble(this::getTotalHealth));
                case HighestHealth -> targets.sort(Comparator.comparingDouble(this::getTotalHealth).reversed());
                case LowestDistance, HighestDistance -> targets.sort(Comparator.comparingDouble(player -> VectorUtils.distance(mc.player.getPos(), player.getPos())));
            }

            if (sortMode.get() == SortMode.HighestDistance) Collections.reverse(targets);

            // Auto Bed City

            if (autoCity.get() && (forceCity.get().isPressed() || automatic.get())) {
                if (canBreak(breakingPos)) {
                    state = State.Citing;

                    if (instaMine.get() && (instaTicks >= instaTickDelay.get() || instaTickDelay.get() == 0)) {
                        ItemStack stack = mc.player.getMainHandStack();
                        float hardness = VectorUtils.getBlock(breakingPos).getHardness();

                        if (stack != null && stack.getItem() instanceof ToolItem && VectorUtils.getBlock(breakingPos).getHardness() > 0
                            && ((hardness / stack.getItem().getMiningSpeedMultiplier(mc.player.getMainHandStack(), VectorUtils.getBlockState(breakingPos))) <= maxHardness.get()
                            || hardness <= maxHardness.get())) {

                            instaTicks = 0;

                            if (rotate.get()) {
                                Rotations.rotate(Rotations.getYaw(breakingPos), Rotations.getPitch(breakingPos), () -> mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakingPos, getClosestDirection(breakingPos).get(0))));
                            } else {
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakingPos, getClosestDirection(breakingPos).get(0)));
                            }

                            swingHand(Hand.MAIN_HAND);
                        }
                    } else {
                        BlockUtils.breakBlock(breakingPos, renderSwing.get());
                    }
                } else {
                    breakingPos = null;

                    state = State.Idling;
                }

                // Getting best Block

                if (cityDelay.get() == 0 || cityTicks >= cityDelay.get()) {
                    boolean shouldCity = false;

                    if (breakingPos != null) {
                        for (CardinalDirection dir : CardinalDirection.values()) {
                            for (PlayerEntity target : targets) {
                                if (breakingPos.down().equals(target.getBlockPos().up())
                                    || breakingPos.offset(dir.toDirection()).equals(target.getBlockPos())
                                    || breakingPos.offset(dir.toDirection()).equals(target.getBlockPos().up())) {
                                    shouldCity = true;
                                }
                            }
                        }
                    } else {
                        shouldCity = true;
                    }

                    if (shouldCity && (breakingPos == null
                        || canPlace(breakingPos, Blocks.RED_BED.getDefaultState(), true)
                        || VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(breakingPos)) > cityRange.get())) {

                        cityTicks = 0;

                        // Collection Blocks

                        ArrayList<BlockPos> positions = new ArrayList<>();

                        for (PlayerEntity target : targets) {
                            positions.add(target.getBlockPos());
                            positions.add(target.getBlockPos().up());
                            positions.add(target.getBlockPos().up(2));

                            for (CardinalDirection dir : CardinalDirection.values()) {
                                positions.add(target.getBlockPos().offset(dir.toDirection()));
                                positions.add(target.getBlockPos().offset(dir.toDirection()).up());
                            }
                        }

                        positions.removeIf(pos -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) > cityRange.get());

                        if (cityScanMode.get() == CityScanMode.Closest) positions.sort(Comparator.comparingDouble(pos -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos))));
                        if (cityScanMode.get() == CityScanMode.Random) Collections.shuffle(positions);

                        for (BlockPos pos : positions) {
                            if (VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= placeRange.get()
                                && VectorUtils.getBlockState(pos).getMaterial().isReplaceable()) {
                                shouldCity = false;
                            }
                        }

                        // Evaluating Blocks

                        if (shouldCity) {
                            for (BlockPos pos : positions) {
                                if (canBreak(pos)) {
                                    breakingPos = pos;
                                    citing = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Calculating

            double pX = mc.player.getX() - 0.5;
            double pY = mc.player.getY();
            double pZ = mc.player.getZ() - 0.5;

            // Placing

            FindItemResult bed = VectorUtils.findInHotbar(stack -> stack.getItem() instanceof BedItem);

            if ((placeTicks >= (speedPlace.get().isPressed() ? speedPlaceDelay.get() : placeDelay.get())
                || (speedPlace.get().isPressed() ? speedPlaceDelay.get() : placeDelay.get()) == 0)
                && (!pausePlacingOnCity.get() || pausePlacingOnCity.get() && !citing)
                && bedCount(0, 35) > 0 && bed.found()
                && placeMode.get() != PlaceMode.None
                && (switchMode.get() != SwitchMode.None || (switchMode.get() == SwitchMode.None
                && mc.player.getOffHandStack().getItem() instanceof BedItem
                && mc.player.getMainHandStack().getItem() instanceof BedItem))) {

                state = State.Placing;

                // PLace Calculating

                int placeMinX = (int) Math.floor(pX - placeRange.get());
                int placeMinY = (int) Math.floor(pY - placeRange.get());
                int placeMinZ = (int) Math.floor(pZ - placeRange.get());

                int placeMaxX = (int) Math.floor(pX + placeRange.get());
                int placeMaxY = (int) Math.floor(pY + placeRange.get());
                int placeMaxZ = (int) Math.floor(pZ + placeRange.get());

                double placeRangeSq = Math.pow(placeRange.get(), 2);

                if (!placeThreading.get() || placeThreading.get() && !multiPlaceThreading.get()) {
                    placeThread = new Thread(() -> {
                        long prevTime = System.currentTimeMillis();
                        double bestDamage = 0;

                        if (placeMode.get() == PlaceMode.Strict || placeMode.get() == PlaceMode.Both) {
                            if (!targets.isEmpty()) {
                                for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                                    PlayerEntity target = targets.get(i);

                                    for (BlockPos position : surround) {
                                        BlockPos pos = target.getBlockPos().add(position);

                                        double damage = 0;

                                        if (VectorUtils.distance(pX, pY, pZ, pos.getX(), pos.getY(), pos.getZ()) <= placeRangeSq) {
                                            if ((!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos, calcRange.get()))
                                                && canPlace(pos, Blocks.RED_BED.getDefaultState(), false) && !getDirectionsForBlock(pos).isEmpty()) {

                                                boolean inside = true;

                                                if (!allowPlaceInside.get()) {
                                                    for (PlayerEntity player : targets) {
                                                        if (player.getBlockPos().equals(pos) || player.getBlockPos().up().equals(pos)) {
                                                            inside = false;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (inside && isVisibleToTargets(pos, null)) {
                                                    double selfDamage = mc.player.getAbilities().creativeMode ? 0 : bedDamage(mc.player, pos, null, true);

                                                    if (selfDamage < maxSelfDamage.get() && (!antiSuicide.get() || (antiSuicide.get() && selfDamage < getTotalHealth(mc.player)))) {
                                                        if (!checkFriendsOnPlace.get() || checkFriends.get() && checkFriendsOnPlace.get() && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(pos, null))) {
                                                            double dmg = getDamageToTargets(pos, null, true);

                                                            if (dmg > bestDamage) {
                                                                Direction direction = null;

                                                                for (Direction dir : getDirectionsForBlock(pos)) {
                                                                    if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                                                        direction = dir;
                                                                        break;
                                                                    }
                                                                }

                                                                if (direction != null
                                                                    && VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= placeRange.get()
                                                                    && (airPlace.get() || !airPlace.get() && getPlaceSide(pos.offset(direction)) != null)) {
                                                                    pos = pos.offset(direction);
                                                                    direction = direction.getOpposite();

                                                                    placePos = pos;
                                                                    placeDir = direction;

                                                                    bestDamage = dmg;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        bestDamage = damage > 0 ? damage : bestDamage;
                                    }
                                }
                            }
                        }

                        if (placeMode.get() == PlaceMode.Smart || placeMode.get() == PlaceMode.Both && bestDamage >= minTargetDamage.get()) {
                            for (int y = placeMinY; y <= placeMaxY; y++) {
                                for (int x = placeMinX; x <= placeMaxX; x++) {
                                    for (int z = placeMinZ; z <= placeMaxZ; z++) {
                                        if (VectorUtils.distance(pX, pY, pZ, x, y, z) <= placeRangeSq) {
                                            BlockPos pos = new BlockPos(x, y, z);

                                            if ((!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos, calcRange.get()))
                                                && canPlace(pos, Blocks.RED_BED.getDefaultState(), false) && !getDirectionsForBlock(pos).isEmpty()) {

                                                boolean inside = true;

                                                if (!allowPlaceInside.get()) {
                                                    for (PlayerEntity player : targets) {
                                                        if (player.getBlockPos().equals(pos) || player.getBlockPos().up().equals(pos)) {
                                                            inside = false;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (inside && isVisibleToTargets(pos, null)) {
                                                    double selfDamage = mc.player.getAbilities().creativeMode ? 0 : bedDamage(mc.player, pos, null, true);

                                                    if (selfDamage < maxSelfDamage.get() && (!antiSuicide.get() || (antiSuicide.get() && selfDamage < getTotalHealth(mc.player)))) {
                                                        if (!checkFriendsOnPlace.get() || checkFriends.get() && checkFriendsOnPlace.get() && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(pos, null))) {
                                                            double dmg = getDamageToTargets(pos, null, true);

                                                            if (dmg > bestDamage) {
                                                                Direction direction = null;

                                                                for (Direction dir : getDirectionsForBlock(pos)) {
                                                                    if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                                                        direction = dir;
                                                                        break;
                                                                    }
                                                                }

                                                                if (direction != null
                                                                    && VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= placeRange.get()
                                                                    && (airPlace.get() || !airPlace.get() && getPlaceSide(pos.offset(direction)) != null)) {
                                                                    pos = pos.offset(direction);
                                                                    direction = direction.getOpposite();

                                                                    placePos = pos;
                                                                    placeDir = direction;

                                                                    bestDamage = dmg;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (bestDamage < minTargetDamage.get()) {
                            placeDir = null;
                            placePos = null;
                        }

                        calcTime = Math.max(prevTime - System.currentTimeMillis(), calcTime);
                    });
                } else if (placeThreading.get() && multiPlaceThreading.get() && shouldPlace()) {
                    if (multiThread != null && multiThread.isEmpty()) {
                        ArrayList<BlockPos> tempPositions = new ArrayList<>();

                        if (placeMode.get() == PlaceMode.Strict || placeMode.get() == PlaceMode.Both) {
                            if (placeMode.get() == PlaceMode.Strict || placeMode.get() == PlaceMode.Both) {
                                if (!targets.isEmpty()) {
                                    for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                                        PlayerEntity target = targets.get(i);

                                        for (BlockPos position : surround) {
                                            BlockPos pos = target.getBlockPos().add(position);

                                            if ((!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos, calcRange.get()))
                                                && canPlace(pos, Blocks.RED_BED.getDefaultState(), false) && !getDirectionsForBlock(pos).isEmpty()) {

                                                boolean inside = true;

                                                if (!allowPlaceInside.get()) {
                                                    for (PlayerEntity player : targets) {
                                                        if (player.getBlockPos().equals(pos) || player.getBlockPos().up().equals(pos)) {
                                                            inside = false;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (inside) tempPositions.add(pos);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (placeMode.get() == PlaceMode.Smart || placeMode.get() == PlaceMode.Both) {
                            for (int y = placeMinY; y <= placeMaxY; y++) {
                                for (int x = placeMinX; x <= placeMaxX; x++) {
                                    for (int z = placeMinZ; z <= placeMaxZ; z++) {
                                        if (VectorUtils.distance(pX, pY, pZ, x, y, z) <= placeRangeSq) {
                                            BlockPos pos = new BlockPos(x, y, z);

                                            if ((!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos, calcRange.get()))
                                                && canPlace(pos, Blocks.RED_BED.getDefaultState(), false) && !getDirectionsForBlock(pos).isEmpty()) {

                                                boolean inside = true;

                                                if (!allowPlaceInside.get()) {
                                                    for (PlayerEntity player : targets) {
                                                        if (player.getBlockPos().equals(pos) || player.getBlockPos().up().equals(pos)) {
                                                            inside = false;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (inside) tempPositions.add(pos);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        List<BlockPos> positions = new ArrayList<>();
                        for (BlockPos pos : tempPositions) if (!positions.contains(pos)) positions.add(pos);

                        if (!positions.isEmpty()) {
                            List<List<BlockPos>> partition = partition(positions, placeThreads.get());

                            if (partition.size() != placeThreads.get() && debugMessages.get()) {
                                info("error when dividing the positions among the threads: " + partition.size() + "partitions / " + placeThreads.get() + "expected.");
                            }

                            for (List<BlockPos> tempThreadPartition : partition) {
                                multiThread.add(new Thread(() -> {
                                    long prevTime = System.currentTimeMillis();

                                    BlockPos bestPos = null;
                                    Direction bestDir = null;
                                    double bestDamage = 0;

                                    for (BlockPos pos : new ArrayList<>(tempThreadPartition)) {
                                        if (isVisibleToTargets(pos, null)) {
                                            double selfDamage = mc.player.getAbilities().creativeMode ? 0 : bedDamage(mc.player, pos, null, true);

                                            if (selfDamage < maxSelfDamage.get() && (!antiSuicide.get() || (antiSuicide.get() && selfDamage < getTotalHealth(mc.player)))) {
                                                if (!checkFriendsOnPlace.get() || checkFriends.get() && checkFriendsOnPlace.get() && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(pos, null))) {
                                                    double dmg = getDamageToTargets(pos, null, true);

                                                    if (dmg > bestDamage) {
                                                        Direction direction = null;

                                                        for (Direction dir : getDirectionsForBlock(pos)) {
                                                            if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                                                direction = dir;
                                                                break;
                                                            }
                                                        }

                                                        if (direction != null
                                                            && VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= placeRange.get()
                                                            && (airPlace.get() || !airPlace.get() && getPlaceSide(pos.offset(direction)) != null)) {
                                                            pos = pos.offset(direction);
                                                            direction = direction.getOpposite();

                                                            bestPos = pos;
                                                            bestDir = direction;

                                                            bestDamage = dmg;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    addPos(new Triplet<>(bestPos, bestDir, bestDamage));

                                    calcTime = Math.max(prevTime - System.currentTimeMillis(), calcTime);
                                }));
                            }

                            for (Thread thread : multiThread) if (thread != null && thread.getState() == Thread.State.NEW) thread.start();
                        }
                    }
                }
            }

            // Actual Placement

            if (shouldPlace() && bed.found() && placeTicking.get() == PlaceTicking.Pre) {
                if (placePos == null || placeDir == null) {
                    if ((!placeThreading.get() || placeThreading.get() && !multiPlaceThreading.get()) && placeThread != null && placeThread.getState() == Thread.State.NEW) {
                        if (placeThreading.get()) {
                            placeThread.start();
                        } else {
                            placeThread.run();
                        }
                    } else if (placeThreading.get() && multiPlaceThreading.get()
                        && multiThread != null && !multiThread.isEmpty()
                        && bestPositions != null && !bestPositions.isEmpty() && bestPositions.size() >= placeThreads.get()) {
                        double bestDamage = 0;

                        for (Triplet<BlockPos, Direction, Double> value : new ArrayList<>(bestPositions)) {
                            if (value.getC() > bestDamage) {
                                placePos = value.getA();
                                placeDir = value.getB();
                                bestDamage = value.getC();
                            }
                        }

                        if (bestDamage < minTargetDamage.get()) {
                            placePos = null;
                            placeDir = null;
                        }

                        multiThread = new ArrayList<>();
                        bestPositions = new ArrayList<>();
                    }
                }

                if (placePos != null && placeDir != null) {
                    if (renderType.get() != RenderType.None) {
                        boolean shouldRender = true;

                        if (renderOnlyOnce.get()) {
                            for (RenderBlock block : new ArrayList<>(renderBlocks)) {
                                if (block.pos.equals(placePos) && block.ticks < (renderTicks.get() * (ticksLeftPercent.get() / 100))) {
                                    shouldRender = false;
                                    break;
                                }
                            }
                        }

                        if (!renderOnlyOnce.get() || renderOnlyOnce.get() && shouldRender) renderBlocks.add(renderBlockPool.get().set(placePos, placeDir));
                    }

                    place(placePos, placeDir, bed, renderSwing.get(), true);

                    if (breakDelay.get() == 0) breakPos = placePos;
                    if (resetDelayOnPlace.get()) breakTicks = 0;

                    placeDir = null;
                    placePos = null;
                    placeTicks = 0;

                    if (breakDelay.get() != 0) return;
                }
            }

            // Breaking

            if (breakMode.get() != BreakMode.None
                && breakTicks >= (speedBreak.get().isPressed() ? speedBreakDelay.get() : breakDelay.get())
                && (!pauseBreakingOnCity.get() || pauseBreakingOnCity.get() && !citing)) {

                state = State.Breaking;

                // Break Calculation

                Runnable breaking = () -> {
                    long prevTime = System.currentTimeMillis();

                    for (BlockEntity entity : getBlockEntities()) {
                        if (entity instanceof BedBlockEntity) {
                            BlockPos pos = entity.getPos();

                            boolean valid = breakMode.get() != BreakMode.Strict;

                            if (breakMode.get() == BreakMode.Strict) {
                                if (!targets.isEmpty()) {
                                    for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                                        PlayerEntity target = targets.get(i);

                                        for (BlockPos position : surround) {
                                            if (pos.equals(target.getBlockPos().add(position))) {
                                                valid = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (valid && World.isValid(pos) && VectorUtils.getBlock(pos) instanceof BedBlock
                                && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= breakRange.get()) {

                                BlockPos head = VectorUtils.getBlockState(pos).get(Properties.BED_PART) == BedPart.HEAD ? pos : null;
                                BlockPos foot = VectorUtils.getBlockState(pos).get(Properties.BED_PART) == BedPart.FOOT ? pos : null;

                                if (head == null) {
                                    Direction direction = VectorUtils.getBlockState(foot).get(HorizontalFacingBlock.FACING);

                                    if (direction != null
                                        && VectorUtils.getBlock(foot.offset(direction)) instanceof BedBlock
                                        && VectorUtils.getBlockState(foot.offset(direction)).get(Properties.BED_PART) == BedPart.HEAD
                                        && VectorUtils.getBlockState(foot.offset(direction)).get(HorizontalFacingBlock.FACING) == direction) {
                                        head = foot.offset(direction);
                                    }

                                    if (head == null || !(VectorUtils.getBlock(head) instanceof BedBlock) && !getBreakDirections(foot).isEmpty()) {
                                        for (Direction dir : getBreakDirections(foot)) {
                                            if (VectorUtils.getBlock(foot.offset(dir)) instanceof BedBlock
                                                && VectorUtils.getBlockState(foot.offset(dir)).get(Properties.BED_PART) == BedPart.HEAD) {
                                                head = foot.offset(dir);
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (foot == null) {
                                    Direction direction = VectorUtils.getBlockState(head).get(HorizontalFacingBlock.FACING);

                                    if (direction != null
                                        && VectorUtils.getBlock(pos.offset(direction.getOpposite())) instanceof BedBlock
                                        && VectorUtils.getBlockState(pos.offset(direction.getOpposite())).get(Properties.BED_PART) == BedPart.FOOT
                                        && VectorUtils.getBlockState(pos.offset(direction.getOpposite())).get(HorizontalFacingBlock.FACING).getOpposite() == direction) {
                                        foot = pos.offset(direction.getOpposite());
                                    }

                                    if (head == null || !(VectorUtils.getBlock(foot) instanceof BedBlock) && !getBreakDirections(head).isEmpty()) {
                                        for (Direction dir : getBreakDirections(head)) {
                                            if (VectorUtils.getBlock(head.offset(dir)) instanceof BedBlock
                                                && VectorUtils.getBlockState(head.offset(dir)).get(Properties.BED_PART) == BedPart.FOOT) {
                                                foot = head.offset(dir);
                                                break;
                                            }
                                        }
                                    }
                                }

                                Pair<BlockPos, BlockPos> ignoreBed = new Pair<>(head, foot);

                                if (head != null && foot != null || head != null && (head != null || foot != null) && !(head != null && foot != null)) {
                                    double selfDamage = !checkSelfBreak.get() ? 0 : bedDamage(mc.player, pos, ignoreBed, true);

                                    if (!checkSelfBreak.get() || selfDamage < maxSelfDamage.get()
                                        && (!antiSuicideBreak.get() || (antiSuicideBreak.get() && selfDamage < getTotalHealth(mc.player)))
                                        && (minTargetDamage.get() == 0 || getDamageToTargets(pos, ignoreBed, false) >= minTargetDamage.get())) {
                                        if (checkFriends.get() && checkFriendsOnBreak.get()) {
                                            if (!getBreakDirections(pos).isEmpty() && isVisibleToTargets(head, ignoreBed)
                                                && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(head, ignoreBed))
                                                && getDamageToFriends(head, ignoreBed) <= maxFriendDamage.get()) {

                                                breakPos = pos;
                                            }
                                        } else {
                                            breakPos = pos;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    calcTime = Math.max(prevTime - System.currentTimeMillis(), calcTime);
                };

                breakThread = new Thread(breaking);

                if (breakThread.getState() == Thread.State.NEW) {
                    if (breakThreading.get()) {
                        breakThread.start();
                    } else {
                        breaking.run();
                    }
                }

                // Actual Breaking

                if (breakPos != null) {
                    breakBed(breakPos);

                    breakPos = null;
                    breakTicks = 0;

                    state = State.Idling;

                    if (resetDelayOnBreak.get()) placeTicks = 0;
                }
            }

            // Actual Placement

            if (shouldPlace() && bed.found() && placeTicking.get() == PlaceTicking.Post) {
                if (placePos == null || placeDir == null) {
                    if ((!placeThreading.get() || placeThreading.get() && !multiPlaceThreading.get()) && placeThread != null && placeThread.getState() == Thread.State.NEW) {
                        if (placeThreading.get()) {
                            placeThread.start();
                        } else {
                            placeThread.run();
                        }
                    } else if (placeThreading.get() && multiPlaceThreading.get()
                        && multiThread != null && !multiThread.isEmpty()
                        && bestPositions != null && !bestPositions.isEmpty() && bestPositions.size() >= placeThreads.get()) {
                        double bestDamage = 0;

                        for (Triplet<BlockPos, Direction, Double> value : new ArrayList<>(bestPositions)) {
                            if (value.getC() > bestDamage) {
                                placePos = value.getA();
                                placeDir = value.getB();
                                bestDamage = value.getC();
                            }
                        }

                        if (bestDamage < minTargetDamage.get()) {
                            placePos = null;
                            placeDir = null;
                        }

                        multiThread = new ArrayList<>();
                        bestPositions = new ArrayList<>();
                    }
                }

                if (placePos != null && placeDir != null) {
                    if (renderType.get() != RenderType.None) {
                        boolean shouldRender = true;

                        if (renderOnlyOnce.get()) {
                            for (RenderBlock block : new ArrayList<>(renderBlocks)) {
                                if (block.pos.equals(placePos) && block.ticks < (renderTicks.get() * (ticksLeftPercent.get() / 100))) {
                                    shouldRender = false;
                                    break;
                                }
                            }
                        }

                        if (!renderOnlyOnce.get() || renderOnlyOnce.get() && shouldRender) renderBlocks.add(renderBlockPool.get().set(placePos, placeDir));
                    }

                    place(placePos, placeDir, bed, renderSwing.get(), true);

                    if (breakDelay.get() == 0) breakPos = placePos;
                    if (resetDelayOnPlace.get()) breakTicks = 0;

                    placeDir = null;
                    placePos = null;
                    placeTicks = 0;
                }
            }
        }
    }

    // Reloading

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (mc != null && mc.world != null && mc.player != null) {
            if (mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);

            explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
            raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
        }
    }

    // Silent Craft

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (silentCraft.get() && event.screen instanceof CraftingScreen) {
            silentHandler = ((CraftingScreen) event.screen).getScreenHandler();

            mc.setScreen(null);
            event.cancel();
        }
    }

    // Render

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBlocks.isEmpty()) {
            List<RenderBlock> blocks = new ArrayList<>(renderBlocks);

            blocks.sort(Comparator.comparingInt(block -> -block.ticks));
            blocks.forEach(block -> block.render(event, shapeMode.get()));
        }
    }

    // Multithreading

    private List<List<BlockPos>> partition(List<BlockPos> positions, int parts) {
        List<List<BlockPos>> partition = new ArrayList<>();

        for (int i = 0; i < parts; i++) partition.add(new ArrayList<>());
        for (int i = 0; i < positions.size(); i++) {
            partition.get(i % parts).add(positions.get(i));
        }

        return partition;
    }

    public synchronized void addPos(Triplet<BlockPos, Direction, Double> element) {
        bestPositions.add(element);
    }

    // Inventory Utils

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.isEmpty() || stack.getCount() == 0 || stack.getItem() instanceof AirBlockItem;
    }

    private int bedCount(int start, int end) {
        int count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.getItem() instanceof BedItem) count += stack.getCount();
        }

        return count;
    }

    private int getEmptySlots(int start, int end) {
        int slots = 0;

        for (int i = start; i <= end; i++) if (isEmpty(mc.player.getInventory().getStack(i))) slots++;

        return slots;
    }

    private boolean isInventoryFull() {
        for (int i = 0; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isEmpty(stack)) return false;
        }

        return true;
    }

    private boolean canRefill() {
        return ((bedCount(0, 35) == 0 || bedCount(0, 35) <= minBeds.get()))
            && getEmptySlots(0, 35) >= minEmptySlots.get()
            && (!craftOnlyOnGround.get() || craftOnlyOnGround.get() && mc.player.isOnGround())
            && (!craftOnlyInHole.get() || craftOnlyInHole.get() && isSurrounded())
            && getTotalHealth(mc.player) >= minCraftHealth.get();
    }

    private boolean needRefill() {
        boolean wool = false;
        boolean plank = false;

        for (Item item : wools) if (getCount(item) >= 3) wool = true;
        for (Item item : planks) if (getCount(item) >= 3) plank = true;

        return wool && plank;
    }

    private int getCount(Item item) {
        int count = 0;

        for (int i = 0; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.getItem() == item) count += stack.getCount();
        }

        return count;
    }

    private boolean canCraft(Recipe<?> recipe) {
        boolean wool = false;
        boolean plank = false;

        for (Ingredient ingredient : recipe.getIngredients()) {
            for (int i = 0; i <= 35; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);

                if (wools.contains(stack.getItem()) && getCount(stack.getItem()) >= 3 && ingredient.test(stack)) wool = true;
                if (planks.contains(stack.getItem()) && getCount(stack.getItem()) >= 3 && ingredient.test(stack)) plank = true;

                if (wool && plank) return true;
            }
        }

        return false;
    }

    private void clickSlot(int syncId, int id, int button, SlotActionType action) {
        if (mc.interactionManager != null && clickMode.get() == ClickMode.Normal) {
            mc.interactionManager.clickSlot(syncId, id, button, action, mc.player);
        } else {
            ScreenHandler handler = mc.player.currentScreenHandler;

            DefaultedList<Slot> slots = handler.slots;
            int i = slots.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slot : slots) list.add(slot.getStack().copy());

            handler.onSlotClick(id, button, action, mc.player);
            Int2ObjectMap<ItemStack> stacks = new Int2ObjectOpenHashMap();

            for (int slot = 0; slot < i; slot++) {
                ItemStack stack1 = list.get(slot);
                ItemStack stack2 = slots.get(slot).getStack();

                if (!ItemStack.areEqual(stack1, stack2)) stacks.put(slot, stack2.copy());
            }

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, handler.getRevision(), id, button, action, handler.getCursorStack().copy(), stacks));
        }
    }

    // Utils

    private Iterable<BlockEntity> getBlockEntities() {
        return BlockEntityIterator::new;
    }

    private void swingHand(Hand hand) {
        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    private boolean shouldPause() {
        if (killAuraPause.get() && Modules.get() != null && Modules.get().isActive(KillAura.class)) return true;
        if (crystalAuraPause.get() && Modules.get() != null && Modules.get().isActive(CrystalAura.class)) return true;
        if (eatPause.get() && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem().isFood() || mc.player.getOffHandStack().getItem().isFood()))) return true;
        return drinkPause.get() && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem() instanceof PotionItem));
    }

    private boolean isSurrounded() {
        int i = 0;

        for (CardinalDirection dir : CardinalDirection.values()) {
            if (dir.toDirection() != null) {
                Block block = VectorUtils.getBlock(mc.player.getBlockPos().offset(dir.toDirection()));
                if (block != null && block.getBlastResistance() >= 600.0F) i++;
            }
        }

        return i == 4;
    }

    private boolean canBreak(BlockPos pos) {
        return pos != null && !VectorUtils.getBlockState(pos).isAir() &&
            VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= cityRange.get()
            && VectorUtils.getBlockState(pos).getHardness(mc.world, pos) > 0
            && !VectorUtils.getBlockState(pos).getOutlineShape(mc.world, pos).isEmpty();
    }

    private boolean shouldPlace() {
        boolean checkPlace = true;

        if (waitForBreak.get()) {
            for (BlockEntity entity : getBlockEntities()) {
                if (entity instanceof BedBlockEntity) {
                    BlockPos pos = entity.getPos();

                    if (VectorUtils.getBlock(pos) instanceof BedBlock && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= placeRange.get()) {
                        BlockPos head = VectorUtils.getBlockState(pos).get(Properties.BED_PART) == BedPart.HEAD ? pos : null;
                        BlockPos foot = VectorUtils.getBlockState(pos).get(Properties.BED_PART) == BedPart.FOOT ? pos : null;

                        if (head == null) {
                            Direction direction = VectorUtils.getBlockState(foot).get(HorizontalFacingBlock.FACING);

                            if (direction != null
                                && VectorUtils.getBlock(foot.offset(direction)) instanceof BedBlock
                                && VectorUtils.getBlockState(foot.offset(direction)).get(Properties.BED_PART) == BedPart.HEAD
                                && VectorUtils.getBlockState(foot.offset(direction)).get(HorizontalFacingBlock.FACING) == direction) {
                                head = foot.offset(direction);
                            }

                            if (head == null || !(VectorUtils.getBlock(head) instanceof BedBlock) && !getBreakDirections(foot).isEmpty()) {
                                for (Direction dir : getBreakDirections(foot)) {
                                    if (VectorUtils.getBlock(foot.offset(dir)) instanceof BedBlock
                                        && VectorUtils.getBlockState(foot.offset(dir)).get(Properties.BED_PART) == BedPart.HEAD) {
                                        head = foot.offset(dir);
                                        break;
                                    }
                                }
                            }
                        }

                        if (foot == null) {
                            Direction direction = VectorUtils.getBlockState(head).get(HorizontalFacingBlock.FACING);

                            if (direction != null
                                && VectorUtils.getBlock(pos.offset(direction.getOpposite())) instanceof BedBlock
                                && VectorUtils.getBlockState(pos.offset(direction.getOpposite())).get(Properties.BED_PART) == BedPart.FOOT
                                && VectorUtils.getBlockState(pos.offset(direction.getOpposite())).get(HorizontalFacingBlock.FACING).getOpposite() == direction) {
                                foot = pos.offset(direction.getOpposite());
                            }

                            if (head == null || !(VectorUtils.getBlock(foot) instanceof BedBlock) && !getBreakDirections(head).isEmpty()) {
                                for (Direction dir : getBreakDirections(head)) {
                                    if (VectorUtils.getBlock(head.offset(dir)) instanceof BedBlock
                                        && VectorUtils.getBlockState(head.offset(dir)).get(Properties.BED_PART) == BedPart.FOOT) {
                                        foot = head.offset(dir);
                                        break;
                                    }
                                }
                            }
                        }

                        Pair<BlockPos, BlockPos> bed = new Pair<>(head, foot);

                        if (head != null && foot != null
                            && canDealDamageToTargets(pos, calcRange.get())
                            && isVisibleToTargets(head, bed)) {
                            double damage = getDamageToTargets(head, bed, false);

                            if (damage < minTargetDamage.get()) {
                                checkPlace = true;
                                break;
                            } else {
                                checkPlace = false;
                            }
                        }
                    }
                }
            }
        }

        return checkPlace;
    }

    // Valid Bed Directions

    private List<Direction> getDirectionsForBlock(BlockPos pos) {
        if (placeScanMode.get() == PlaceScanMode.Lock) return List.of(mc.player.getHorizontalFacing());
        List<Direction> directions = new ArrayList<>();

        for (CardinalDirection dir : CardinalDirection.values()) {
            if (dir.toDirection() != null && VectorUtils.getBlockState(pos.offset(dir.toDirection())).getMaterial().isReplaceable()) {
                directions.add(dir.toDirection());
            }
        }

        if (placeScanMode.get() == PlaceScanMode.Closest) directions.sort(Comparator.comparingDouble(dir -> dir != null ? VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(dir))) : VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos))));
        if (placeScanMode.get() == PlaceScanMode.Random) Collections.shuffle(directions);
        return directions;
    }

    private List<Direction> getBreakDirections(BlockPos pos) {
        List<Direction> directions = new ArrayList<>();

        for (CardinalDirection dir : CardinalDirection.values()) if (dir.toDirection() != null) directions.add(dir.toDirection());

        if (breakScanMode.get() == BreakScanMode.Closest) directions.sort(Comparator.comparingDouble(dir -> VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(dir)))));
        if (breakScanMode.get() == BreakScanMode.Random) Collections.shuffle(directions);
        return directions;
    }

    private Direction getBestSide(BlockPos pos) {
        ArrayList<Direction> directions = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (VectorUtils.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) directions.add(dir);
        }

        if (directions.isEmpty()) directions.addAll(List.of(Direction.values()));
        if (directions.isEmpty()) return null;

        directions.sort(Comparator.comparingDouble(dir -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos.offset(dir)))));
        return directions.get(0);
    }

    private List<Direction> getClosestDirection(BlockPos pos) {
        List<Direction> directions = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (dir != null && VectorUtils.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) {
                directions.add(dir);
            }
        }

        if (directions.isEmpty()) directions.addAll(List.of(Direction.values()));

        directions.sort(Comparator.comparingDouble(dir -> dir != null ? VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(dir))) : VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos))));
        return directions;
    }

    // Damage Calculation

    private boolean canDealDamageToTargets(BlockPos pos, double range) {
        boolean inRange = false;

        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                if (VectorUtils.distance(targets.get(i).getPos(), Vec3d.ofCenter(pos)) <= range) inRange = true;
            }
        }

        return inRange;
    }

    private boolean isVisibleToTargets(BlockPos pos, Pair<BlockPos, BlockPos> bed) {
        if (raytraceMode.get() == RaytraceMode.None) return true;

        boolean visible = false;

        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) if (isVisible(pos, targets.get(i), bed)) {
                visible = true;
                break;
            }
        }

        return visible;
    }

    private double getDamageToTargets(BlockPos pos, Pair<BlockPos, BlockPos> bed, boolean shouldIgnoreTerrain) {
        double bestDamage = 0;
        double damage = 0;

        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                PlayerEntity player = targets.get(i);

                if ((!smartDelay.get() || player.hurtTime <= 0) && VectorUtils.distance(Vec3d.ofCenter(pos), player.getPos()) < targetRange.get() + 1) {
                    double dmg = bedDamage(player, pos, bed, shouldIgnoreTerrain);

                    if (dmg > bestDamage) bestDamage = dmg;

                    damage += dmg;
                }
            }
        }

        return damage;
    }

    private double getDamageToFriends(BlockPos pos, Pair<BlockPos, BlockPos> bed) {
        double bestDamage = 0;

        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                PlayerEntity target = targets.get(i);

                if ((!smartDelay.get() || target.hurtTime <= 0) && target != mc.player && Friends.get().isFriend(target) && VectorUtils.distance(Vec3d.ofCenter(pos), target.getPos()) < 11) {
                    double dmg = bedDamage(target, pos, bed, true);

                    if (dmg > bestDamage) bestDamage = dmg;
                }
            }
        }

        return bestDamage;
    }

    private boolean checkDamageToFriends(BlockPos pos, Pair<BlockPos, BlockPos> bed) {
        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                PlayerEntity target = targets.get(i);

                if ((!smartDelay.get() || target.hurtTime <= 0) && target != mc.player && Friends.get().isFriend(target) && VectorUtils.distance(Vec3d.ofCenter(pos), target.getPos()) < 11) {
                    if (bedDamage(target, pos, bed, true) >= getTotalHealth(target)) return false;
                }
            }
        }
        return true;
    }

    // Raycasting

    private boolean isVisible(BlockPos end, Entity entity, Pair<BlockPos, BlockPos> bed) {
        double y = entity.getY();

        if (raytraceMode.get() == RaytraceMode.Body) y = entity.getY() + entity.getHeight() / 2;
        if (raytraceMode.get() == RaytraceMode.Eyes) y = entity.getEyeY();

        return raytraceMode.get() != RaytraceMode.Any && isVisible(new Vec3d(entity.getX(), y, entity.getZ()), end, entity, bed)
            || raytraceMode.get() == RaytraceMode.Any && (isVisible(new Vec3d(entity.getX(), y, entity.getZ()), end, entity, bed)
            || isVisible(new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ()), end, entity, bed)
            || isVisible(new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ()), end, entity, bed));
    }

    private boolean isVisible(Vec3d start, BlockPos end, Entity entity, Pair<BlockPos, BlockPos> bed) {
        ((IRaycastContext) raycastContext).set(start, Vec3d.ofCenter(end), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);

        BlockHitResult result = BlockView.raycast(start, Vec3d.ofCenter(end), raycastContext, (raycast, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (bed != null && state.getBlock() instanceof BedBlock && (pos.equals(bed.getA()) || pos.equals(bed.getB()))) state = Blocks.AIR.getDefaultState();

            BlockHitResult blockResult = mc.world.raycastBlock(start, raycast.getEnd(), pos, raycast.getBlockShape(state, mc.world, pos), state);
            BlockHitResult emptyResult = VoxelShapes.empty().raycast(start, raycast.getEnd(), pos);

            double block = blockResult == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(blockResult.getPos());
            double empty = emptyResult == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(emptyResult.getPos());

            return block <= empty ? blockResult : emptyResult;
        }, (raycast) -> {
            Vec3d diff = raycast.getStart().subtract(raycast.getEnd());
            return BlockHitResult.createMissed(raycast.getEnd(), Direction.getFacing(diff.x, diff.y, diff.z), new BlockPos(raycast.getEnd()));
        });

        return result.getType() == HitResult.Type.MISS;
    }

    // Bed Damage

    private double bedDamage(PlayerEntity player, BlockPos pos, Pair<BlockPos, BlockPos> bed, boolean shouldIgnoreTerrain) {
        if (player == null || player.getAbilities().creativeMode && !ignoreGamemode.get() && !(player instanceof FakePlayerEntity)) return 0;

        Vec3d position = Vec3d.ofCenter(pos);
        if (explosion == null) explosion = new Explosion(mc.world, null, position.x, position.y, position.z, 5.0F, true, Explosion.DestructionType.DESTROY);
        else ((IExplosion) explosion).set(position, 5.0F, true);

        double distance = Math.sqrt(player.squaredDistanceTo(position));
        if (distance > 10) return 0;

        if (raycastContext == null) raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);

        double exposure = getExposure(position, player, raycastContext, bed, shouldIgnoreTerrain ? ignoreTerrain.get() : false);
        double impact = (1.0 - (distance / 10.0)) * exposure;
        double damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1;

        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        // Damage calculation
        damage = getDamageForDifficulty(damage);
        damage = resistanceReduction(player, damage);
        damage = getDamageLeft((float) damage, (float) player.getArmor(), attribute != null ? (float) attribute.getValue() : 0);
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    private double getExposure(Vec3d source, Entity entity, RaycastContext context, Pair<BlockPos, BlockPos> bed, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();

        if (predictMovement.get() && !entity.isOnGround()) {
            Vec3d velocity = entity.getVelocity();
            box = box.offset(velocity.x, velocity.y, velocity.z);
        }

        double d = 1 / ((box.maxX - box.minX) * 2 + 1);
        double e = 1 / ((box.maxY - box.minY) * 2 + 1);
        double f = 1 / ((box.maxZ - box.minZ) * 2 + 1);

        if (!(d < 0) && !(e < 0) && !(f < 0)) {
            int i = 0;
            int j = 0;

            for (double k = 0; k <= 1; k += d) {
                for (double l = 0; l <= 1; l += e) {
                    for (double m = 0; m <= 1; m += f) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);

                        ((IRaycastContext) context).set(new Vec3d(n + ((1 - Math.floor(1 / d) * d) / 2), o, p + ((1 - Math.floor(1 / f) * f) / 2)), source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
                        if (raycast(context, bed, ignoreTerrain).getType() == HitResult.Type.MISS) i++;

                        j++;
                    }
                }
            }

            return (double) i / j;
        }

        return 0;
    }

    private BlockHitResult raycast(RaycastContext context, Pair<BlockPos, BlockPos> bed, boolean ignoreTerrain) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycast, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (ignoreTerrain && state.getBlock().getBlastResistance() < 600 && !(state.getBlock() instanceof BedBlock)) state = Blocks.AIR.getDefaultState();
            if (bed != null && state.getBlock() instanceof BedBlock && (pos.equals(bed.getA()) || pos.equals(bed.getB()))) state = Blocks.AIR.getDefaultState();
            if (stabilize.get() && state.getBlock() instanceof BedBlock) {
                boolean inside = false;

                if (!targets.isEmpty()) {
                    for (PlayerEntity target : targets) {
                        Box box = new Box(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1.0, pos.getY() + 0.5625, pos.getZ() + 1.0
                        );

                        if (mc.world.getBlockCollisions(target, box).iterator().hasNext()) {
                            inside = true;
                            break;
                        }
                    }
                }

                if (inside) state = Blocks.BEDROCK.getDefaultState();
            }

            Vec3d start = raycast.getStart();
            Vec3d end = raycast.getEnd();

            BlockHitResult blockResult = mc.world.raycastBlock(start, end, pos, raycast.getBlockShape(state, mc.world, pos), state);
            BlockHitResult emptyResult = VoxelShapes.empty().raycast(start, end, pos);

            double block = blockResult == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(blockResult.getPos());
            double empty = emptyResult == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(emptyResult.getPos());

            return block <= empty ? blockResult : emptyResult;
        }, (raycast) -> {
            Vec3d diff = raycast.getStart().subtract(raycast.getEnd());
            return BlockHitResult.createMissed(raycast.getEnd(), Direction.getFacing(diff.x, diff.y, diff.z), new BlockPos(raycast.getEnd()));
        });
    }

    // Reduction

    private double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY     -> Math.min(damage / 2 + 1, damage);
            case HARD     -> damage * 3 / 2;
            default       -> damage;
        };
    }

    private double blastProtReduction(Entity player, double damage, Explosion explosion) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (protLevel > 20) protLevel = 20;

        damage *= (1 - (protLevel / 25.0));
        return damage < 0 ? 0 : damage;
    }

    private double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= (1 - (lvl * 0.2));
        }

        return damage < 0 ? 0 : damage;
    }

    private float getDamageLeft(float damage, float armor, float armorToughness) {
        float f = 2.0F + armorToughness / 4.0F;
        float g = MathHelper.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - g / 25.0F);
    }

    private float getTotalHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private List<BlockPos> getAffectedBlocks(BlockPos pos) {
        ((IExplosion) explosion).set(Vec3d.ofCenter(pos), 5.0F, true);

        return explosion.getAffectedBlocks();
    }

    // Placing

    private void place(BlockPos pos, Direction dir, FindItemResult item, boolean swingHand, boolean checkEntities) {
        if (pos != null && item != null && item.found()) {
            if (item.isOffhand()) {
                place(pos, dir, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, swingHand, checkEntities);
            } else if (item.isHotbar()) {
                place(pos, dir, Hand.MAIN_HAND, item.getSlot(), swingHand, checkEntities);
            }
        }
    }

    private void place(BlockPos pos, Direction dir, Hand hand, int slot, boolean swingHand, boolean checkEntities) {
        if ((slot >= 0 && slot <= 9 || slot == 45) && pos != null) {
            Vec3d hitPos = Vec3d.ofCenter(pos);

            BlockPos neighbour = getNeighbourPos(pos);
            Direction side = getSide(pos);

            double yaw = mc.player.getYaw();

            if (dir != null) {
                yaw = switch (dir) {
                    case EAST -> -90;
                    case SOUTH -> 0;
                    case WEST -> 90;
                    default -> -180;
                };
            }

            Rotations.rotate(yaw, pitchRotate.get() ? Rotations.getPitch(hitPos) : mc.player.getPitch(), 100, () -> {
                VectorUtils.swap(slot, swapBack.get());
                place(new BlockHitResult(hitPos, side, neighbour, false), hand, packetPlace.get(), swingHand);
                if (swapBack.get() && switchMode.get() != SwitchMode.None) VectorUtils.swapBack();
            });
        }
    }

    private void place(BlockHitResult result, Hand hand, boolean packetPlace, boolean swing) {
        if (hand != null && result != null && mc.world.getWorldBorder().contains(result.getBlockPos()) && mc.player.getStackInHand(hand).getItem() instanceof BlockItem) {
            if (packetPlace) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

                Block block = ((BlockItem) mc.player.getStackInHand(hand).getItem()).getBlock();
                BlockSoundGroup group = block.getSoundGroup(block.getDefaultState());

                mc.world.playSound(result.getBlockPos(), group.getPlaceSound(), SoundCategory.BLOCKS, group.volume, group.pitch, true);

                if (swing) mc.player.swingHand(hand);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            } else {
                boolean wasSneaking = mc.player.input.sneaking;
                mc.player.input.sneaking = false;

                if (mc.interactionManager.interactBlock(mc.player, mc.world, hand, result).shouldSwingHand()) {
                    if (swing) mc.player.swingHand(hand);
                    else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
                }

                mc.player.input.sneaking = wasSneaking;
            }
        }
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
            BlockState state = VectorUtils.getBlockState(pos.offset(side));

            if (!state.getMaterial().isReplaceable() && !VectorUtils.isClickable(state.getBlock()) && state.getFluidState().isEmpty()) return side.getOpposite();
        }

        return null;
    }

    private boolean canPlace(BlockPos pos, BlockState state, boolean checkEntities) {
        if (pos == null || mc.world == null || !World.isValid(pos) || !VectorUtils.getBlockState(pos).getMaterial().isReplaceable()) return false;
        return checkEntities ? mc.world.canPlace(state, pos, ShapeContext.absent()) : VectorUtils.getBlockState(pos).getMaterial().isReplaceable();
    }

    // Breaking

    private void breakBed(BlockPos pos) {
        if (pos != null) breakBed(new BlockHitResult(Vec3d.ofCenter(pos), getBestSide(pos), pos, false), Hand.OFF_HAND);
    }

    private void breakBed(BlockHitResult result, Hand hand) {
        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) {
            mc.player.setSneaking(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

        swingHand(hand);

        mc.player.setSneaking(wasSneaking);
    }

    // Constants

    private final ArrayList<BlockPos> surround = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));

        add(new BlockPos(1, 1, 0));
        add(new BlockPos(-1, 1, 0));
        add(new BlockPos(0, 1, 1));
        add(new BlockPos(0, 1, -1));

        add(new BlockPos(0, 0, 0));
        add(new BlockPos(0, 1, 0));
        add(new BlockPos(0, 2, 0));
    }};

    private final ArrayList<Item> wools = new ArrayList<>() {{
        add(Items.BLACK_WOOL);
        add(Items.BLUE_WOOL);
        add(Items.BROWN_WOOL);
        add(Items.CYAN_WOOL);
        add(Items.GRAY_WOOL);
        add(Items.GREEN_WOOL);
        add(Items.LIGHT_BLUE_WOOL);
        add(Items.LIGHT_GRAY_WOOL);
        add(Items.LIME_WOOL);
        add(Items.MAGENTA_WOOL);
        add(Items.ORANGE_WOOL);
        add(Items.PINK_WOOL);
        add(Items.PURPLE_WOOL);
        add(Items.RED_WOOL);
        add(Items.YELLOW_WOOL);
        add(Items.WHITE_WOOL);
    }};

    private final ArrayList<Item> planks = new ArrayList<>() {{
        add(Items.ACACIA_PLANKS);
        add(Items.BIRCH_PLANKS);
        add(Items.DARK_OAK_PLANKS);
        add(Items.JUNGLE_PLANKS);
        add(Items.SPRUCE_PLANKS);
        add(Items.OAK_PLANKS);
    }};

    private enum State {
        Breaking,
        Placing,
        Crafting,
        Citing,
        Idling
    }

    public enum RaytraceMode {
        None,
        Feet,
        Body,
        Eyes,
        Any
    }

    public enum SortMode {
        Normal,
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

    public enum ClickMode {
        Normal,
        Packet
    }

    public enum SwitchMode {
        Normal,
        Silent,
        None
    }

    public enum SwapMode {
        Pickup,
        Swap
    }

    public enum PlaceTicking {
        Pre,
        Post
    }

    public enum PlaceMode {
        None,
        Strict,
        Smart,
        Both
    }

    public enum BreakMode {
        None,
        Strict,
        Smart,
        Both
    }

    public enum PlaceScanMode {
        Normal,
        Random,
        Lock,
        Closest
    }

    public enum BreakScanMode {
        Normal,
        Random,
        Closest
    }

    public enum CityScanMode {
        Normal,
        Random,
        Closest
    }

    public enum RenderType {
        None,
        Normal,
        Advanced
    }

    // Render Block

    private class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public Direction dir;
        public int ticks;
        public double offset;

        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;
        private Color linesBottom;

        public RenderBlock set(BlockPos position, Direction direction) {
            if (pos == null || direction == null) return new RenderBlock();
            pos.set(position);
            dir = direction;
            ticks = renderTicks.get();
            offset = 1;

            sidesTop = new Color(sideColorTop.get());
            sidesBottom = new Color(sideColorBottom.get());
            linesTop = new Color(lineColorTop.get());
            linesBottom = new Color(lineColorBottom.get());

            return this;
        }

        public void tick() {
            ticks--;
            if (shrink.get() && offset >= 0 && renderTicks.get() - ticks >= shrinkTicks.get()) offset -= shrinkSpeed.get() / 100;
        }

        public void render(Render3DEvent event, ShapeMode shapeMode) {
            if (sidesTop == null || sidesBottom == null || linesTop == null || linesBottom == null || pos == null) return;

            int preSideTopA = sidesTop.a;
            int preSideBottomA = sidesBottom.a;
            int preLineTopA = linesTop.a;
            int preLineBottomA = linesBottom.a;

            sidesTop.a *= (double) ticks / 8;
            sidesBottom.a *= (double) ticks / 8;
            linesTop.a *= (double) ticks / 8;
            linesBottom.a *= (double) ticks / 8;

            double x = pos.getX() + (shrink.get() ? 0.5 - offset / 2 : 0);
            double y = pos.getY() + (shrink.get() ? 0.5 - offset / 2 : 0);
            double z = pos.getZ() + (shrink.get() ? 0.5 - offset / 2 : 0);

            double px3 = feetLength.get() / 10 * (shrink.get() ? offset : 1);
            double px8 = bedHeight.get() / 10 * (shrink.get() ? offset : 1);

            double px16 = 1 * (shrink.get() ? offset : 1);
            double px32 = 2 * (shrink.get() ? offset : 1);

            if (renderType.get() == RenderType.Advanced) {
                if (dir == Direction.NORTH) z -= 1;
                else if (dir == Direction.WEST) x -= 1;

                // Lines

                if (shapeMode.lines()) {
                    if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                        // Edges

                        renderEdgeLines(x, y, z, px3, 1, event);
                        renderEdgeLines(x, y, z + px32 - px3, px3, 2, event);
                        renderEdgeLines(x + px16 - px3, y, z, px3, 3, event);
                        renderEdgeLines(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                        // High Lines

                        event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                        event.renderer.line(x, y + px3, z + px32, x, y + px8, z + px32, linesBottom, linesTop);
                        event.renderer.line(x + px16, y + px3, z, x + px16, y + px8, z, linesBottom, linesTop);
                        event.renderer.line(x + px16, y + px3, z + px32, x + px16, y + px8, z + px32, linesBottom, linesTop);

                        // Connections

                        event.renderer.line(x + px3, y + px3, z, x + px16 - px3, y + px3, z, linesBottom);
                        event.renderer.line(x + px3, y + px3, z + px32, x + px16 - px3, y + px3, z + px32, linesBottom);

                        event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px32 - px3, linesBottom);
                        event.renderer.line(x + px16, y + px3, z + px3, x + px16, y + px3, z + px32 - px3, linesBottom);

                        if (renderExtra.get()) event.renderer.line(x, y + px3, z, x + px16, y + px8, z + px32, linesBottom, linesTop);

                        // Top

                        event.renderer.line(x, y + px8, z, x + px16, y + px8, z, linesTop);
                        event.renderer.line(x, y + px8, z + px32, x + px16, y + px8, z + px32, linesTop);
                        event.renderer.line(x, y + px8, z, x , y + px8, z + px32, linesTop);
                        event.renderer.line(x + px16, y + px8, z, x + px16, y + px8, z + px32, linesTop);
                    } else {
                        // Edges

                        renderEdgeLines(x, y, z, px3, 1, event);
                        renderEdgeLines(x, y, z + px16 - px3, px3, 2, event);
                        renderEdgeLines(x + px32 - px3, y, z, px3, 3, event);
                        renderEdgeLines(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                        // High Lines

                        event.renderer.line(x, y + px3, z, x, y + px8, z, linesBottom, linesTop);
                        event.renderer.line(x + px32, y + px3, z, x + px32, y + px8, z, linesBottom, linesTop);
                        event.renderer.line(x, y + px3, z + px16, x, y + px8, z + px16, linesBottom, linesTop);
                        event.renderer.line(x + px32, y + px3, z + px16, x + px32, y + px8, z + px16, linesBottom, linesTop);

                        // Connections

                        event.renderer.line(x, y + px3, z + px3, x, y + px3, z + px16 - px3, linesBottom);
                        event.renderer.line(x + px32, y + px3, z + px3, x + px32, y + px3, z + px16 - px3, linesBottom);

                        event.renderer.line(x + px3, y + px3, z, x + px32 - px3, y + px3, z, linesBottom);
                        event.renderer.line(x + px3, y + px3, z + px16, x + px32 - px3, y + px3, z + px16, linesBottom);

                        if (renderExtra.get()) event.renderer.line(x, y + px8, z, x + px32, y + px3, z + px16, linesBottom, linesTop);

                        // Top

                        event.renderer.line(x, y + px8, z, x, y + px8, z + px16, linesTop);
                        event.renderer.line(x + px32, y + px8, z, x + px32, y + px8, z + px16, linesTop);
                        event.renderer.line(x, y + px8, z, x + px32 , y + px8, z, linesTop);
                        event.renderer.line(x, y + px8, z + px16, x + px32, y + px8, z + px16, linesTop);
                    }
                }

                // Sides

                if (shapeMode.sides()) {
                    if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                        // Horizontal

                        // Bottom

                        if (renderInnerSides.get()) {
                            sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                            sideHorizontal(x + px16 - px3, y, z, x + px16, z + px3, event, sidesBottom, sidesBottom);
                            sideHorizontal(x, y, z + px32 - px3, x + px3, z + px32, event, sidesBottom, sidesBottom);
                            sideHorizontal(x + px16 - px3, y, z + px32 - px3, x + px16, z + px32, event, sidesBottom, sidesBottom);
                        }

                        // Middle & Top

                        if (renderInnerSides.get()) {
                            sideHorizontal(x + px3, y + px3, z, x + px16 - px3, z + px3, event, sidesBottom, sidesBottom);
                            sideHorizontal(x + px3, y + px3, z + px32 - px3, x + px16 - px3, z + px32, event, sidesBottom, sidesBottom);

                            sideHorizontal(x, y + px3, z + px3, x + px16, z + px32 - px3, event, sidesBottom, sidesBottom);
                        } else {
                            sideHorizontal(x, y + px3, z, x + px16, z + px32, event, sidesBottom, sidesBottom);
                        }

                        sideHorizontal(x, y + px8, z, x + px16, z + px32, event, sidesTop, sidesTop);

                        // Vertical

                        // Edges

                        renderEdgeSides(x, y, z, px3, 1, event);
                        renderEdgeSides(x, y, z + px32 - px3, px3, 2, event);
                        renderEdgeSides(x + px16 - px3, y, z, px3, 3, event);
                        renderEdgeSides(x + px16 - px3, y, z + px32 - px3, px3, 4, event);

                        // Sides

                        sideVertical(x, y + px3, z, x + px16, y + px8, z, event, sidesBottom, sidesTop);
                        sideVertical(x, y + px3, z + px32, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                        sideVertical(x, y + px3, z, x, y + px8, z + px32, event, sidesBottom, sidesTop);
                        sideVertical(x + px16, y + px3, z, x + px16, y + px8, z + px32, event, sidesBottom, sidesTop);
                    } else {
                        // Horizontal

                        // Bottom

                        if (renderInnerSides.get()) {
                            sideHorizontal(x, y, z, x + px3, z + px3, event, sidesBottom, sidesBottom);
                            sideHorizontal(x, y, z + px16 - px3, x + px3, z + px16, event, sidesBottom, sidesBottom);
                            sideHorizontal(x + px32 - px3, y, z, x + px32, z + px3, event, sidesBottom, sidesBottom);
                            sideHorizontal(x + px32 - px3, y, z + px16 - px3, x + px32, z + px16, event, sidesBottom, sidesBottom);
                        }

                        // Middle & Top

                        if (renderInnerSides.get()) {
                            sideHorizontal(x, y + px3, z + px3, x + px3, z + px16 - px3, event, sidesBottom, sidesBottom);
                            sideHorizontal(x + px32 - px3, y + px3, z + px3, x + px32, z + px16 - px3, event, sidesBottom, sidesBottom);

                            sideHorizontal(x + px3, y + px3, z, x + px32 - px3, z + px16, event, sidesBottom, sidesBottom);
                        } else {
                            sideHorizontal(x, y + px3, z, x + px32, z + px16, event, sidesBottom, sidesBottom);
                        }

                        sideHorizontal(x, y + px8, z, x + px32, z + px16, event, sidesTop, sidesTop);

                        // Vertical

                        // Edges

                        renderEdgeSides(x, y, z, px3, 1, event);
                        renderEdgeSides(x + px32 - px3, y, z, px3, 3, event);
                        renderEdgeSides(x, y, z + px16 - px3, px3, 2, event);
                        renderEdgeSides(x + px32 - px3, y, z + px16 - px3, px3, 4, event);

                        // Sides

                        sideVertical(x, y + px3, z, x, y + px8, z + px16, event, sidesBottom, sidesTop);
                        sideVertical(x + px32, y + px3, z, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                        sideVertical(x, y + px3, z, x + px32, y + px8, z, event, sidesBottom, sidesTop);
                        sideVertical(x, y + px3, z + px16, x + px32, y + px8, z + px16, event, sidesBottom, sidesTop);
                    }
                }
            } else if (renderType.get() == RenderType.Normal) {
                switch (dir) {
                    case NORTH -> event.renderer.box(x, y, z - 1, x + px16, y + px8, z + px32 - 1, sidesBottom, linesBottom, shapeMode, 0);
                    case SOUTH -> event.renderer.box(x, y, z - px16 + 1, x + px16, y + px8, z + px16 + 1, sidesBottom, linesBottom, shapeMode, 0);
                    case EAST -> event.renderer.box(x - px16 + 1, y, z, x + px16 + 1, y + px8, z + px16, sidesBottom, linesBottom, shapeMode, 0);
                    case WEST -> event.renderer.box(x - 1, y, z, x + px32 - 1, y + px8, z + px16, sidesBottom, linesBottom, shapeMode, 0);
                }
            }

            // Resetting the Colors

            sidesTop.a = preSideTopA;
            sidesBottom.a = preSideBottomA;
            linesTop.a = preLineTopA;
            linesBottom.a = preLineBottomA;
        }

        // Render Utils

        private void renderEdgeLines(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            if (renderInnerLines.get()) edge = 0;

            // Horizontal

            if (edge != 2 && edge != 4) event.renderer.line(x, y, z, x + px3, y, z, linesBottom);
            if (edge != 3 && edge != 4) event.renderer.line(x, y, z, x, y, z + px3, linesBottom);

            if (edge != 1 && edge != 2) event.renderer.line(x + px3, y, z, x + px3, y, z + px3, linesBottom);
            if (edge != 1 && edge != 3) event.renderer.line(x, y, z + px3, x + px3, y, z + px3, linesBottom);

            // Vertical

            if (edge != 4) event.renderer.line(x, y, z, x, y + px3, z, linesBottom);
            if (edge != 2) event.renderer.line(x + px3, y, z, x + px3, y + px3, z, linesBottom);
            if (edge != 3) event.renderer.line(x, y, z + px3, x, y + px3, z + px3, linesBottom);
            if (edge != 1) event.renderer.line(x + px3, y, z + px3, x + px3, y + px3, z + px3, linesBottom);
        }

        private void renderEdgeSides(double x, double y, double z, double px3, int edge, Render3DEvent event) {
            if (renderInnerSides.get()) edge = 0;

            // Horizontal

            if (edge != 4 && edge != 2) sideVertical(x, y, z, x + px3, y + px3, z, event, sidesBottom, sidesBottom);
            if (edge != 4 && edge != 3) sideVertical(x, y, z, x, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 2) sideVertical(x + px3, y, z, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
            if (edge != 1 && edge != 3) sideVertical(x, y, z + px3, x + px3, y + px3, z + px3, event, sidesBottom, sidesBottom);
        }

        public void sideHorizontal(double x1, double y, double z1, double x2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, event, bottomSideColor, topSideColor);
        }

        public void sideVertical(double x1, double y1, double z1, double x2, double y2, double z2, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            side(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, event, bottomSideColor, topSideColor);
        }

        private void side(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Render3DEvent event, Color bottomSideColor, Color topSideColor) {
            event.renderer.quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, topSideColor, topSideColor, bottomSideColor, bottomSideColor);
        }
    }
}
