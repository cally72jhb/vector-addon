package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.BlockEntityIterator;
import cally72jhb.addon.utils.misc.FindItemResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchableContainer;
import net.minecraft.client.sound.PositionedSoundInstance;
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
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.explosion.Explosion;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

import java.util.*;

public class AutoBed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRotate = settings.createGroup("Rotation");
    private final SettingGroup sgOther = settings.createGroup("Other");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgCalc = settings.createGroup("Calculation");
    private final SettingGroup sgPredict = settings.createGroup("Movement Predict");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgCraft = settings.createGroup("Auto Craft");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgDisplay = settings.createGroup("Display");
    private final SettingGroup sgDebugRender = settings.createGroup("Debug Render");
    private final SettingGroup sgBedRender = settings.createGroup("Bed Render");



    // General



    private final Setting<Double> horizontalTargetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-target-range")
        .description("The horizontal radius players can be in to be targeted.")
        .defaultValue(13.5)
        .sliderMin(5)
        .sliderMax(15)
        .min(0)
        .build()
    );

    private final Setting<Double> verticalTargetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-target-range")
        .description("The vertical radius players can be in to be targeted.")
        .defaultValue(12)
        .sliderMin(5)
        .sliderMax(15)
        .min(0)
        .build()
    );

    private final Setting<SortPriority> sortMode = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("sort-mode")
        .description("How targets are selected.")
        .defaultValue(SortPriority.LowestDistance)
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
        .name("anti-friend-damage")
        .description("Checks the damage to friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiFriendPop = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-friend-pop")
        .description("Stops you from placing or breaking beds which could pop your friends.")
        .defaultValue(true)
        .visible(checkFriends::get)
        .build()
    );

    private final Setting<Integer> maxFriendDamage = sgGeneral.add(new IntSetting.Builder()
        .name("max-friend-damage")
        .description("The maximum damage a bed can deal to friends for it to be broken.")
        .defaultValue(7)
        .sliderMin(0)
        .sliderMax(10)
        .min(0)
        .max(36)
        .noSlider()
        .visible(checkFriends::get)
        .build()
    );

    private final Setting<Boolean> debugMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-log")
        .description("Prints out debug messages when calculations fail.")
        .defaultValue(true)
        .build()
    );



    // Rotation



    private final Setting<PitchMode> pitchMode = sgRotate.add(new EnumSetting.Builder<PitchMode>()
        .name("pitch-mode")
        .description("How to rotate pitch-wise.")
        .defaultValue(PitchMode.Zero)
        .build()
    );

    private final Setting<Double> customPitch = sgRotate.add(new DoubleSetting.Builder()
        .name("custom-pitch")
        .description("How much to rotate pitch-wise when placing a bed.")
        .defaultValue(45)
        .sliderMin(-90)
        .sliderMax(90)
        .visible(() -> pitchMode.get() == PitchMode.Custom)
        .build()
    );

    private final Setting<RotateMode> rotateMode = sgRotate.add(new EnumSetting.Builder<RotateMode>()
        .name("rotation-mode")
        .description("How to face beds.")
        .defaultValue(RotateMode.Normal)
        .build()
    );

    private final Setting<Boolean> ignoreRotationWhenRotated = sgRotate.add(new BoolSetting.Builder()
        .name("ignore-rotation-when-rotated")
        .description("Doesn't rotate you when you already face in the needed direction.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotateBack = sgRotate.add(new BoolSetting.Builder()
        .name("rotate-back")
        .description("Rotates back to the original facing after placing the bed.")
        .defaultValue(false)
        .visible(() -> rotateMode.get() != RotateMode.Normal)
        .build()
    );



    // Other



    private final Setting<Boolean> packetPlace = sgOther.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Places the beds with packets instead of the normal way.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreDimension = sgOther.add(new BoolSetting.Builder()
        .name("ignore-dimension")
        .description("Allows this module to work in any dimension even if beds can't be blown up there.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreGamemode = sgOther.add(new BoolSetting.Builder()
        .name("ignore-gamemode")
        .description("Ignores the targets gamemode.")
        .defaultValue(false)
        .build()
    );

    // Keybindings

    private final Setting<Keybind> keepPlacePosKey = sgOther.add(new KeybindSetting.Builder()
        .name("keep-place-key")
        .description("When pressed the current place position won't be updated and the bed aura will continue to place there.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> speedPlaceKey = sgOther.add(new KeybindSetting.Builder()
        .name("speed-place-key")
        .description("The keybinding used to speed up placement.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> speedBreakKey = sgOther.add(new KeybindSetting.Builder()
        .name("speed-break-key")
        .description("The keybinding used to speed up bed breaking.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Integer> speedPlaceDelay = sgOther.add(new IntSetting.Builder()
        .name("speed-place-delay")
        .description("How many ticks to wait before placing a bed when the force speed place key is pressed.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .build()
    );

    private final Setting<Integer> speedBreakDelay = sgOther.add(new IntSetting.Builder()
        .name("speed-break-delay")
        .description("How many ticks to wait before breaking a bed when the force speed break key is pressed.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .build()
    );

    // Moving Delay

    private final Setting<Boolean> movingDelay = sgOther.add(new BoolSetting.Builder()
        .name("moving-delay")
        .description("Whether or not to use different delays when targets are moving or not.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreInHole = sgOther.add(new BoolSetting.Builder()
        .name("ignore-in-hole")
        .description("Doesn't detect targets that are in a hole.")
        .defaultValue(true)
        .visible(movingDelay::get)
        .build()
    );

    private final Setting<Boolean> ignoreDoubles = sgOther.add(new BoolSetting.Builder()
        .name("ignore-doubles")
        .description("Ignores players in double holes.")
        .defaultValue(true)
        .visible(() -> movingDelay.get() && ignoreInHole.get())
        .build()
    );

    private final Setting<Integer> movingTicks = sgOther.add(new IntSetting.Builder()
        .name("moving-ticks")
        .description("How many ticks to scan for moving players.")
        .defaultValue(10)
        .min(2)
        .sliderMin(5)
        .sliderMax(20)
        .onChanged(integer -> prevPositions = new HashMap<>())
        .visible(movingDelay::get)
        .build()
    );

    private final Setting<Double> movingDistance = sgOther.add(new DoubleSetting.Builder()
        .name("moving-distance")
        .description("How far a targets previous position has to be away from the current position.")
        .defaultValue(1.25)
        .min(0.15)
        .sliderMin(0.5)
        .sliderMax(2)
        .visible(movingDelay::get)
        .build()
    );

    private final Setting<Integer> movingPlaceDelay = sgOther.add(new IntSetting.Builder()
        .name("moving-place-delay")
        .description("How many ticks to wait before placing beds when targets are moving.")
        .defaultValue(6)
        .sliderMin(3)
        .sliderMax(8)
        .min(0)
        .visible(movingDelay::get)
        .build()
    );

    private final Setting<Integer> movingBreakDelay = sgOther.add(new IntSetting.Builder()
        .name("moving-break-delay")
        .description("How many ticks to wait before breaking beds when targets are moving.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(4)
        .min(0)
        .visible(movingDelay::get)
        .build()
    );



    // Place



    private final Setting<PlaceMode> placeMode = sgPlace.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("What blocks to scan first.")
        .defaultValue(PlaceMode.Smart)
        .build()
    );

    private final Setting<PlaceDirectionPriority> placeDirectionPriority = sgPlace.add(new EnumSetting.Builder<PlaceDirectionPriority>()
        .name("place-direction-priority")
        .description("Which directions to scan first when placing a bed.")
        .defaultValue(PlaceDirectionPriority.Closest)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Double> horizontalPlaceRange = sgPlace.add(new DoubleSetting.Builder()
        .name("horizontal-place-range")
        .description("The horizontal radius around you in which beds can be placed in.")
        .defaultValue(4)
        .sliderMin(1)
        .sliderMax(6)
        .min(1)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Double> verticalPlaceRange = sgPlace.add(new DoubleSetting.Builder()
        .name("vertical-place-range")
        .description("The vertical radius around you in which beds can be placed in.")
        .defaultValue(3.5)
        .sliderMin(1)
        .sliderMax(6)
        .min(1)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Integer> bedPlaceDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("How many ticks to wait before placing a bed.")
        .defaultValue(0)
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

    private final Setting<Boolean> placeRangeBypass = sgPlace.add(new BoolSetting.Builder()
        .name("place-range-bypass")
        .description("Interacts at the closest possible position to allow a maximal place range.")
        .defaultValue(true)
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
        .description("Won't place beds when there are already existing beds near targets dealing enough damage.")
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

    private final Setting<BreakDirectionPriority> breakDirectionPriority = sgBreak.add(new EnumSetting.Builder<BreakDirectionPriority>()
        .name("break-direction-priority")
        .description("Which directions to scan first when breaking a bed.")
        .defaultValue(BreakDirectionPriority.Closest)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<BreakHandMode> breakHandMode = sgBreak.add(new EnumSetting.Builder<BreakHandMode>()
        .name("break-hand-mode")
        .description("With what hand to break the beds.")
        .defaultValue(BreakHandMode.OffHand)
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

    private final Setting<Integer> bedBreakDelay = sgBreak.add(new IntSetting.Builder()
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

    private final Setting<Boolean> breakRangeBypass = sgBreak.add(new BoolSetting.Builder()
        .name("break-range-bypass")
        .description("Breaks the bed at the closest possible position to allow a maximal break range.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakInside = sgBreak.add(new BoolSetting.Builder()
        .name("break-inside")
        .description("Tyres to break inside of the bed.")
        .defaultValue(true)
        .visible(() -> !breakRangeBypass.get())
        .build()
    );

    private final Setting<Boolean> updateSneaking = sgBreak.add(new BoolSetting.Builder()
        .name("update-sneaking")
        .description("Resneaks after unsneaking when breaking beds.")
        .defaultValue(true)
        .build()
    );



    // Calculation



    private final Setting<Boolean> hurtDelayCooldown = sgCalc.add(new BoolSetting.Builder()
        .name("hurt-delay-cooldown")
        .description("Calculates the damage under consideration of the targets hurt time.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> smartCalc = sgCalc.add(new BoolSetting.Builder()
        .name("raycast-distance")
        .description("Will only raycast when the raycast is in a set range from the target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> horizontalCalcRange = sgCalc.add(new DoubleSetting.Builder()
        .name("horizontal-calc-range")
        .description("The horizontal radius around targets that is considered a valid position which deals damage.")
        .defaultValue(10)
        .sliderMin(7.5)
        .sliderMax(10)
        .min(1)
        .visible(smartCalc::get)
        .build()
    );

    private final Setting<Double> verticalCalcRange = sgCalc.add(new DoubleSetting.Builder()
        .name("vertical-calc-range")
        .description("The vertical radius around targets that is considered a valid position which deals damage.")
        .defaultValue(7.5)
        .sliderMin(5)
        .sliderMax(10)
        .min(1)
        .visible(smartCalc::get)
        .build()
    );

    private final Setting<RaytraceMode> raytraceMode = sgCalc.add(new EnumSetting.Builder<RaytraceMode>()
        .name("raytrace-mode")
        .description("From what position to check if the beds deal damage.")
        .defaultValue(RaytraceMode.Eyes)
        .build()
    );

    private final Setting<Boolean> ignoreTerrain = sgCalc.add(new BoolSetting.Builder()
        .name("ignore-terrain")
        .description("Ignores the explodable terrain around you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stabilize = sgCalc.add(new BoolSetting.Builder()
        .name("stabilize")
        .description("Thinks of beds as inexplodable blocks to .")
        .defaultValue(false)
        .build()
    );

    // Threading

    private final Setting<Boolean> placeThreading = sgCalc.add(new BoolSetting.Builder()
        .name("place-threading")
        .description("Calculates the placing in a separate thread to shrink the calculation time.")
        .defaultValue(true)
        .onChanged(bool -> stopThreads())
        .build()
    );

    private final Setting<Boolean> multiPlaceThreading = sgCalc.add(new BoolSetting.Builder()
        .name("multithreading-place")
        .description("Uses multiple threads to calculate the placing positions.")
        .defaultValue(false)
        .onChanged(bool -> stopThreads())
        .visible(placeThreading::get)
        .build()
    );

    private final Setting<UnthreadedMode> unthreadedMode = sgCalc.add(new EnumSetting.Builder<UnthreadedMode>()
        .name("unthreaded-mode")
        .description("How unthreaded positions are sorted.")
        .defaultValue(UnthreadedMode.None)
        .onChanged(bool -> stopThreads())
        .visible(placeThreading::get)
        .build()
    );

    private final Setting<Integer> maxUnthreadedPositionsPerPlayer = sgCalc.add(new IntSetting.Builder()
        .name("max-unthreaded-per-player")
        .description("How many positions are calculated without any threads per player.")
        .defaultValue(4)
        .sliderMin(2)
        .sliderMax(8)
        .min(1)
        .max(50)
        .noSlider()
        .onChanged(integer -> stopThreads())
        .visible(() -> placeThreading.get() && unthreadedMode.get() == UnthreadedMode.Ceil)
        .build()
    );

    private final Setting<Double> horizontalUnthreadedRange = sgCalc.add(new DoubleSetting.Builder()
        .name("horizontal-unthreaded-range")
        .description("The horizontal radius for unthreaded positions.")
        .defaultValue(4)
        .sliderMin(1)
        .sliderMax(5)
        .min(1)
        .visible(() -> placeThreading.get() && unthreadedMode.get() == UnthreadedMode.Ceil)
        .build()
    );

    private final Setting<Double> verticalUnthreadedRange = sgCalc.add(new DoubleSetting.Builder()
        .name("vertical-unthreaded-range")
        .description("The vertical radius for unthreaded positions.")
        .defaultValue(4)
        .sliderMin(1)
        .sliderMax(5)
        .min(1)
        .visible(() -> placeThreading.get() && unthreadedMode.get() == UnthreadedMode.Ceil)
        .build()
    );

    private final Setting<Integer> maxUnthreadedPositions = sgCalc.add(new IntSetting.Builder()
        .name("max-unthreaded")
        .description("How many positions are calculated without any threads.")
        .defaultValue(8)
        .sliderMin(4)
        .sliderMax(16)
        .min(2)
        .max(75)
        .noSlider()
        .onChanged(integer -> stopThreads())
        .visible(() -> placeThreading.get() && unthreadedMode.get() != UnthreadedMode.None)
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
        .noSlider()
        .onChanged(integer -> stopThreads())
        .visible(() -> placeThreading.get() && multiPlaceThreading.get())
        .build()
    );

    private final Setting<Boolean> breakThreading = sgCalc.add(new BoolSetting.Builder()
        .name("break-threading")
        .description("Calculates the breaking in a separate thread to shrink the calculation time.")
        .defaultValue(true)
        .build()
    );



    // Movement Predict



    private final Setting<PredictMode> predictMode = sgPredict.add(new EnumSetting.Builder<PredictMode>()
        .name("predict-mode")
        .description("How to predict a players movement.")
        .defaultValue(PredictMode.None)
        .onChanged(bool -> {
            predictions = new HashMap<>();
            predictedResults = new HashMap<>();
        })
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgPredict.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself when predicting the movement.")
        .defaultValue(true)
        .visible(() -> predictMode.get() != PredictMode.None)
        .build()
    );

    private final Setting<Double> predictRange = sgPredict.add(new DoubleSetting.Builder()
        .name("predict-range")
        .description("The range in which players movement is predicted.")
        .defaultValue(50)
        .min(10)
        .sliderMin(25)
        .sliderMax(75)
        .visible(() -> predictMode.get() != PredictMode.None)
        .build()
    );

    private final Setting<Integer> predictTicks = sgPredict.add(new IntSetting.Builder()
        .name("predict-ticks")
        .description("How many ticks to predict a players movement.")
        .defaultValue(15)
        .min(1)
        .sliderMin(15)
        .sliderMax(50)
        .onChanged(integer -> {
            predictions = new HashMap<>();
            predictedResults = new HashMap<>();
        })
        .visible(() -> predictMode.get() == PredictMode.Average || predictMode.get() == PredictMode.Accurate)
        .build()
    );

    private final Setting<Double> maxHorizontalPredictDistance = sgPredict.add(new DoubleSetting.Builder()
        .name("max-horizontal-distance")
        .description("The maximum horizontal distance for the prediction.")
        .defaultValue(1.5)
        .min(0)
        .sliderMin(0)
        .sliderMax(5)
        .visible(() -> predictMode.get() == PredictMode.Average)
        .build()
    );

    private final Setting<Double> maxVerticalPredictDistance = sgPredict.add(new DoubleSetting.Builder()
        .name("max-vertical-distance")
        .description("The maximum vertical distance for the prediction.")
        .defaultValue(25)
        .min(0)
        .sliderMin(10)
        .sliderMax(40)
        .visible(() -> predictMode.get() == PredictMode.Average)
        .build()
    );

    private final Setting<Double> horizontalCalcFactor = sgPredict.add(new DoubleSetting.Builder()
        .name("horizontal-calc-factor")
        .description("How much to speed up the horizontal prediction.")
        .defaultValue(0.5)
        .min(0)
        .sliderMin(0.25)
        .sliderMax(0.75)
        .visible(() -> predictMode.get() == PredictMode.Average)
        .build()
    );

    private final Setting<Double> verticalCalcFactor = sgPredict.add(new DoubleSetting.Builder()
        .name("vertical-calc-factor")
        .description("How much to speed up the vertical prediction.")
        .defaultValue(0)
        .min(0)
        .sliderMin(0)
        .sliderMax(0.5)
        .visible(() -> predictMode.get() == PredictMode.Average)
        .build()
    );

    private final Setting<Boolean> checkCollision = sgPredict.add(new BoolSetting.Builder()
        .name("check-collision")
        .description("Checks block collisions of the predicted positions.")
        .defaultValue(true)
        .visible(() -> predictMode.get() != PredictMode.None)
        .build()
    );

    private final Setting<Integer> collisionSteps = sgPredict.add(new IntSetting.Builder()
        .name("collision-steps")
        .description("In how big steps to check for block collisions.")
        .defaultValue(15)
        .min(1)
        .max(100)
        .sliderMin(1)
        .sliderMax(100)
        .noSlider()
        .visible(() -> predictMode.get() == PredictMode.Average && checkCollision.get())
        .build()
    );



    // Inventory Clicking



    private final Setting<ClickMode> clickMode = sgInventory.add(new EnumSetting.Builder<ClickMode>()
        .name("click-mode")
        .description("How slots in your inventory are clicked.")
        .defaultValue(ClickMode.Packet)
        .build()
    );

    private final Setting<Boolean> antiDesync = sgInventory.add(new BoolSetting.Builder()
        .name("anti-desync")
        .description("Updates your inventory to not desync from the server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> closeAfterwards = sgInventory.add(new BoolSetting.Builder()
        .name("close-afterwards")
        .description("Closes your inventory after moving items.")
        .defaultValue(true)
        .visible(antiDesync::get)
        .build()
    );

    // Inventory Swapping

    private final Setting<Boolean> swapBack = sgInventory.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the selected slot after being done with action.")
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
        .noSlider()
        .visible(autoMove::get)
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
        .noSlider()
        .visible(() -> autoMove.get() && fixedMove.get() && bedMove.get())
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
        .noSlider()
        .visible(() -> autoMove.get() && fixedMove.get() && tableMove.get())
        .build()
    );

    private final Setting<Boolean> fillEmptySlots = sgInventory.add(new BoolSetting.Builder()
        .name("fill-empty-slots")
        .description("Fills empty slots in your hotbar with beds.")
        .defaultValue(true)
        .visible(autoMove::get)
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

    private final Setting<Double> horizontalCraftRange = sgCraft.add(new DoubleSetting.Builder()
        .name("horizontal-craft-range")
        .description("The horizontal radius around you in which the crafting table can be placed or opened")
        .defaultValue(3.5)
        .min(1)
        .sliderMin(2)
        .sliderMax(5)
        .visible(() -> autoCraft.get() && (craftPlace.get() || autoOpen.get()))
        .build()
    );

    private final Setting<Double> verticalCraftRange = sgCraft.add(new DoubleSetting.Builder()
        .name("vertical-craft-range")
        .description("The vertical radius around you in which the crafting table can be placed or opened.")
        .defaultValue(3)
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
        .onChanged(bool -> {
            resetSilentHandler(true);
            craftingHandler = null;
        })
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> continueOnCraft = sgCraft.add(new BoolSetting.Builder()
        .name("continue-on-craft")
        .description("Continues normal calculations while crafting.")
        .defaultValue(false)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> antiFail = sgCraft.add(new BoolSetting.Builder()
        .name("anti-fail")
        .description("Waits a set amount of ticks before crafting again after failing to craft some times.")
        .defaultValue(true)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> infoOnFail = sgCraft.add(new BoolSetting.Builder()
        .name("info-on-fail")
        .description("Notifies you when failing to craft.")
        .defaultValue(true)
        .visible(() -> autoCraft.get() && antiFail.get())
        .build()
    );

    private final Setting<Integer> failAmount = sgCraft.add(new IntSetting.Builder()
        .name("fail-amount")
        .description("How often crafting has to fail before triggering the fail process.")
        .defaultValue(2)
        .min(1)
        .sliderMin(1)
        .sliderMax(5)
        .visible(() -> autoCraft.get() && antiFail.get())
        .build()
    );

    private final Setting<Integer> failWaitDelay = sgCraft.add(new IntSetting.Builder()
        .name("fail-wait-delay")
        .description("How long to wait in ticks before crafting again after failing to craft.")
        .defaultValue(150)
        .min(50)
        .sliderMin(10)
        .sliderMax(300)
        .visible(() -> autoCraft.get() && antiFail.get())
        .build()
    );

    private final Setting<Boolean> closeDirectly = sgCraft.add(new BoolSetting.Builder()
        .name("close-directly")
        .description("Closes the crafting screen directly after being done with crafting.")
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
        .noSlider()
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Integer> grabTimes = sgCraft.add(new IntSetting.Builder()
        .name("grab-times")
        .description("How often beds are moved into your inventory while crafting.")
        .defaultValue(1)
        .min(1)
        .sliderMin(1)
        .sliderMax(3)
        .noSlider()
        .visible(() -> autoCraft.get() && bypass.get())
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
        .noSlider()
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Integer> bedAmount = sgCraft.add(new IntSetting.Builder()
        .name("craft-amount")
        .description("How many beds to craft at once.")
        .defaultValue(4)
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

    private final Setting<Boolean> displayMS = sgDisplay.add(new BoolSetting.Builder()
        .name("display-ms")
        .description("Displays the time it takes to calculate.")
        .defaultValue(true)
        .visible(display::get)
        .build()
    );



    // Debug Render



    private final Setting<Boolean> debugRender = sgDebugRender.add(new BoolSetting.Builder()
        .name("debug-render")
        .description("Renders a box at the predicted position.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> debugShapeMode = sgDebugRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("debug-shape-mode")
        .description("How the debug shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(debugRender::get)
        .build()
    );

    private final Setting<SettingColor> debugSideColor = sgDebugRender.add(new ColorSetting.Builder()
        .name("debug-side-color")
        .description("The side color of the start debug position rendering.")
        .defaultValue(new SettingColor(255, 255, 255, 10))
        .visible(debugRender::get)
        .build()
    );

    private final Setting<SettingColor> debugLineColor = sgDebugRender.add(new ColorSetting.Builder()
        .name("debug-line-color")
        .description("The line color of the start debug position rendering.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(debugRender::get)
        .build()
    );



    // Bed Render



    private final Setting<Boolean> renderSwing = sgBedRender.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand swing animation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<RenderType> renderType = sgBedRender.add(new EnumSetting.Builder<RenderType>()
        .name("render-type")
        .description("How the beds are rendered.")
        .defaultValue(RenderType.Advanced)
        .build()
    );

    private final Setting<Boolean> growIfAlreadyExist = sgBedRender.add(new BoolSetting.Builder()
        .name("grow-if-already-exist")
        .description("Grows the rendered block when there would be another block rendered.")
        .defaultValue(true)
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<Boolean> keepColor = sgBedRender.add(new BoolSetting.Builder()
        .name("keep-color")
        .description("Keeps the same color when growing.")
        .defaultValue(false)
        .visible(() -> renderType.get() != RenderType.None && growIfAlreadyExist.get())
        .build()
    );

    private final Setting<Integer> growTicks = sgBedRender.add(new IntSetting.Builder()
        .name("grow-ticks")
        .description("How many ticks to grow the block again.")
        .defaultValue(5)
        .min(1)
        .max(50)
        .sliderMin(5)
        .sliderMax(15)
        .noSlider()
        .visible(() -> renderType.get() != RenderType.None && growIfAlreadyExist.get())
        .build()
    );

    private final Setting<Integer> renderTicks = sgBedRender.add(new IntSetting.Builder()
        .name("render-ticks")
        .description("How many ticks it should take for a block to disappear.")
        .defaultValue(15)
        .min(5)
        .max(50)
        .sliderMin(5)
        .sliderMax(25)
        .noSlider()
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<Boolean> renderExtra = sgBedRender.add(new BoolSetting.Builder()
        .name("render-extra")
        .description("Renders a extra element in the middle of the bed.")
        .defaultValue(false)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Boolean> renderInnerLines = sgBedRender.add(new BoolSetting.Builder()
        .name("render-inner-lines")
        .description("Renders the inner lines of the bed.")
        .defaultValue(true)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Boolean> renderInnerSides = sgBedRender.add(new BoolSetting.Builder()
        .name("render-inner-sides")
        .description("Renders the inner sides of the bed.")
        .defaultValue(true)
        .visible(() -> renderType.get() == RenderType.Advanced)
        .build()
    );

    private final Setting<Double> feetLength = sgBedRender.add(new DoubleSetting.Builder()
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

    private final Setting<Double> bedHeight = sgBedRender.add(new DoubleSetting.Builder()
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

    private final Setting<ShapeMode> shapeMode = sgBedRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> sideColorTop = sgBedRender.add(new ColorSetting.Builder()
        .name("side-color-top")
        .description("The top side color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 5, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> sideColorBottom = sgBedRender.add(new ColorSetting.Builder()
        .name("side-color-bottom")
        .description("The bottom side color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 25, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> lineColorTop = sgBedRender.add(new ColorSetting.Builder()
        .name("line-color-top")
        .description("The top line color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 50, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );

    private final Setting<SettingColor> lineColorBottom = sgBedRender.add(new ColorSetting.Builder()
        .name("line-color-bottom")
        .description("The bottom line color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 255, true))
        .visible(() -> renderType.get() != RenderType.None)
        .build()
    );



    // Variables



    // Targets

    private List<PlayerEntity> targets;

    // Movement Delays

    private HashMap<Integer, List<Vec3d>> prevPositions;

    // Movement Predict

    private HashMap<Integer, List<Vec3d>> predictions;
    private HashMap<Integer, Vec3d> predictedResults;

    // Rendering

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    // Recipes

    private SearchableContainer<RecipeResultCollection> container;
    private List<Recipe<?>> recipes;

    // Damage Calc & Raycasting

    private Explosion explosion;
    private RaycastContext raycastContext;

    // Timers

    private int placeTicks;
    private int breakTicks;
    private int craftTicks;
    private int recalcTicks;

    // Results

    private Direction placeDirection;
    private BlockPos placePosition;
    private BlockPos breakPosition;

    private Direction tempPlaceDirection;
    private BlockPos tempPlacePosition;
    private BlockPos tempBreakPosition;

    // Threads

    private Thread placeThread;
    private Thread breakThread;

    private Thread[] multiThread;
    private List<Triplet<BlockPos, Direction, Double>> bestPositions;

    // Crafting

    private CraftingScreenHandler craftingHandler;
    private int craftFailTimes;
    private boolean failed;
    private boolean crafted;

    // Other

    private long calcTime;
    private long calcTicks;

    private int slot = -1;



    // Constructor



    public AutoBed() {
        super(Categories.Combat, "auto-bed", "Automatically places and blows up beds near targets to deal a lot of damage.");
    }



    // Module Events



    @Override
    public void onActivate() {
        targets = new ArrayList<>();
        prevPositions = new HashMap<>();
        predictions = new HashMap<>();
        predictedResults = new HashMap<>();

        placeTicks = 0;
        breakTicks = 0;
        craftTicks = 0;
        recalcTicks = 0;

        calcTime = 0;
        calcTicks = 0;

        slot = -1;

        placeDirection = null;
        placePosition = null;
        breakPosition = null;

        tempPlaceDirection = null;
        tempPlacePosition = null;
        tempBreakPosition = null;

        craftingHandler = null;
        craftFailTimes = 0;
        failed = false;
        crafted = true;

        multiThread = new Thread[placeThreads.get()];
        bestPositions = new ArrayList<>();

        recipes = new ArrayList<>();

        stopThreads();
        reload();

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @Override
    public void onDeactivate() {
        resetSilentHandler(true);

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @Override
    public String getInfoString() {
        if (display.get()) {
            String info = "";

            if (displayTarget.get() && targets != null && !targets.isEmpty()) info += "[" + targets.get(0).getGameProfile().getName() + "] ";
            if (displayMS.get()) info += "[" + calcTime + "ms]";

            if (info.endsWith(" ")) info = info.substring(0, info.length() - 1);

            return info.isEmpty() ? null : info;
        }

        return null;
    }


    // Main Tick


    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        // Ticking Render Blocks

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        // Resetting Targets

        targets.clear();

        // Ticking Delays

        placeTicks++;
        breakTicks++;
        craftTicks++;
        recalcTicks++;
        calcTicks++;

        // Pausing

        if (shouldPause() || mc == null || mc.world == null || mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.world.getDimension().isBedWorking() && !ignoreDimension.get()) {
            error("You can't blow up beds in this dimension, disabling.");
            toggle();
            return;
        }

        // Delays

        double placeDelay = bedPlaceDelay.get();
        double breakDelay = bedBreakDelay.get();


        // Auto Move


        if (autoMove.get()) {
            FindItemResult bed = VectorUtils.find(stack -> stack.getItem() instanceof BedItem, 9, 35);
            FindItemResult table = VectorUtils.find(stack -> stack.getItem() == Items.CRAFTING_TABLE, 9, 35);

            if (slot >= 0 && slot <= 8) {
                clickSlot(0, slot, 1, SlotActionType.PICKUP);
                updateInventory(true, true);

                slot = -1;
            } else {
                if (tableMove.get() && table.found() && !table.isOffhand()) {
                    boolean move = true;

                    for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).getItem() == Items.CRAFTING_TABLE) move = false;

                    if (move) {
                        if (fixedMove.get() && getEmptySlots(0, 8) == 0) {
                            if (swapMode.get() == SwapMode.Pickup) {
                                boolean extraClick = isEmpty(mc.player.getInventory().getStack(table.getSlot()));

                                clickSlot(0, table.getSlot(), 1, SlotActionType.PICKUP);
                                clickSlot(0, tableSlot.get() + 35, 1, SlotActionType.PICKUP);
                                if (extraClick) slot = table.getSlot();

                                updateInventory(true, false);
                            } else {
                                clickSlot(0, table.getSlot(), tableSlot.get(), SlotActionType.SWAP);

                                updateInventory(true, true);
                            }
                        } else if (fillEmptySlots.get() && getEmptySlots(0, 8) >= 1) {
                            for (int i = 0; i < 9; i++) {
                                if (isEmpty(mc.player.getInventory().getStack(i))) {
                                    clickSlot(0, table.getSlot(), 1, SlotActionType.QUICK_MOVE);

                                    break;
                                }
                            }

                            updateInventory(true, true);
                        }
                    }
                }

                if (bedMove.get() && bed.found() && !bed.isOffhand()) {
                    if (fixedMove.get() && getEmptySlots(0, 8) == 0) {
                        boolean move = true;

                        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).getItem() instanceof BedItem) move = false;

                        if (move) {
                            if (swapMode.get() == SwapMode.Pickup) {
                                boolean extraClick = isEmpty(mc.player.getInventory().getStack(bed.getSlot()));

                                clickSlot(0, bed.getSlot(), 1, SlotActionType.PICKUP);
                                clickSlot(0, (bedSlot.get().equals(tableSlot.get()) ? (tableSlot.get() >= 8 ? tableSlot.get() - 1 : tableSlot.get() + 1) : bedSlot.get()) + 35, 1, SlotActionType.PICKUP);
                                if (extraClick) slot = bed.getSlot();

                                updateInventory(true, false);
                            } else {
                                clickSlot(0, bed.getSlot(), bedSlot.get() - 1, SlotActionType.SWAP);

                                updateInventory(true, true);
                            }
                        }
                    } else if (fillEmptySlots.get() && getEmptySlots(0, 8) >= 1) {
                        int item = 0;

                        for (int i = 0; i <= 8; i++) {
                            if (isEmpty(mc.player.getInventory().getStack(i))) {
                                clickSlot(0, bed.getSlot(), 1, SlotActionType.QUICK_MOVE);

                                if (item++ >= itemsPerTick.get()) break;
                            }
                        }

                        updateInventory(true, true);
                    }
                }
            }
        }


        // Auto Craft


        if (autoCraft.get() && !failed && craftFailTimes >= failAmount.get() + 1 && canRefill() && needRefill() && !isInventoryFull()) {
            if (infoOnFail.get()) info("Failed to craft " + failAmount.get() + (failAmount.get() == 1 ? " time" : " times") + ". Waiting " + failWaitDelay.get() + " ticks.");

            failed = true;
            crafted = false;

            craftTicks = 0;

            CraftingScreenHandler handler = craftingHandler != null && silentCraft.get() ? craftingHandler : (mc.player.currentScreenHandler instanceof CraftingScreenHandler ? (CraftingScreenHandler) mc.player.currentScreenHandler : null);

            if (handler != null && handler.syncId != 0) {
                updateInventory(false, false);

                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
                mc.player.currentScreenHandler = mc.player.playerScreenHandler;
                mc.setScreen(null);

                craftingHandler = null;
            }
        }

        if (autoCraft.get() && (craftTicks >= ((craftFailTimes >= failAmount.get() + 1) ? failWaitDelay.get() : craftDelay.get()) || craftDelay.get() == 0 && craftFailTimes <= failAmount.get())) {
            if (autoOpen.get() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
                if (canRefill() && needRefill() && !isInventoryFull()) {
                    int table = -1;

                    if (mc.player.getMainHandStack().getItem() == Items.CRAFTING_TABLE) {
                        table = mc.player.getInventory().selectedSlot;
                    } else if (mc.player.getOffHandStack().getItem() == Items.CRAFTING_TABLE) {
                        table = 45;
                    } else {
                        for (int i = 0; i <= 8; i++) {
                            if (mc.player.getInventory().getStack(i).getItem() == Items.CRAFTING_TABLE) {
                                table = i;
                                break;
                            }
                        }
                    }

                    if (table >= 0) {
                        List<BlockPos> placePositions = new ArrayList<>();
                        List<BlockPos> craftPositions = new ArrayList<>();

                        int pX = mc.player.getBlockX();
                        int pY = mc.player.getBlockY();
                        int pZ = mc.player.getBlockZ();

                        int horizontal = (int) Math.floor(horizontalCraftRange.get());
                        int vertical = (int) Math.floor(verticalCraftRange.get());

                        for (int x = pX - horizontal; x <= pX + horizontal; x++) {
                            for (int z = pZ - horizontal; z <= pZ + horizontal; z++) {
                                for (int y = Math.max(mc.world.getBottomY(), pY - vertical); y <= Math.min(pY + vertical, mc.world.getTopY()); y++) {
                                    int dX = Math.abs(x - pX);
                                    int dY = Math.abs(y - pY);
                                    int dZ = Math.abs(z - pZ);

                                    if (dX <= horizontal && dY <= vertical && dZ <= horizontal) {
                                        BlockPos position = new BlockPos(x, y, z);

                                        if (mc.world.getBlockState(position).getBlock() == Blocks.CRAFTING_TABLE) {
                                            craftPositions.add(position);
                                        } else if (mc.world.getBlockState(position).getMaterial().isReplaceable()) {
                                            placePositions.add(position);
                                        }
                                    }
                                }
                            }
                        }

                        // Placing / Opening Crafting Table

                        if (!craftPositions.isEmpty()) {
                            craftPositions.sort(Comparator.comparingDouble(position -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(position))));
                            BlockPos pos = craftPositions.get(0);
                            BlockHitResult result = new BlockHitResult(Vec3d.ofCenter(pos), getSide(pos), pos, false);

                            boolean wasSneaking = mc.player.isSneaking();
                            if (wasSneaking) {
                                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                            }

                            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
                            swingHand(Hand.MAIN_HAND);

                            if (wasSneaking) {
                                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                            }
                        } else if (!placePositions.isEmpty() && craftPlace.get()) {
                            List<BlockPos> insecure = smartTablePlace.get() ? getAffectedBlocks(mc.player.getBlockPos()) : new ArrayList<>();

                            placePositions.sort(Comparator.comparingDouble(position -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(position))));

                            for (BlockPos position : placePositions) {
                                if (canPlace(position, Blocks.CRAFTING_TABLE.getDefaultState(), true)
                                    && (!smartTablePlace.get() || smartTablePlace.get() && !insecure.contains(position))) {

                                    place(position, null, table);
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (mc.player.currentScreenHandler instanceof CraftingScreenHandler || craftingHandler != null && silentCraft.get()) {
                CraftingScreenHandler handler = craftingHandler != null && silentCraft.get() ? craftingHandler : (CraftingScreenHandler) mc.player.currentScreenHandler;

                // Crafting / Closing

                if ((!canRefill() || !needRefill() || isInventoryFull()) && (craftingHandler != null && silentCraft.get()) && handler.syncId != 0) {
                    updateInventory(false, false);

                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
                    mc.player.currentScreenHandler = mc.player.playerScreenHandler;
                    mc.setScreen(null);

                    craftingHandler = null;

                    craftFailTimes = 0;
                    failed = false;
                } else if (canRefill() && needRefill() && !isInventoryFull() && handler.syncId != 0) {
                    if (openRecipeBook.get() && mc.player.getRecipeBook() != null && !mc.player.getRecipeBook().isGuiOpen(RecipeBookCategory.CRAFTING)) {
                        mc.player.getRecipeBook().setGuiOpen(RecipeBookCategory.CRAFTING, true);
                    }

                    if (container == null || recipes.isEmpty()) reload();

                    if (!recipes.isEmpty()) {
                        for (Recipe<?> recipe : recipes) {
                            if (canCraft(recipe)) {
                                int grab = 0;
                                boolean hasCrafted = false;

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

                                    if (!crafted) {
                                        crafted = true;
                                        hasCrafted = true;
                                        break;
                                    }
                                }

                                clickSlot(handler.syncId, 0, 1, SlotActionType.QUICK_MOVE);

                                if (closeDirectly.get() || hasCrafted) {
                                    updateInventory(false, false);

                                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
                                    mc.player.currentScreenHandler = mc.player.playerScreenHandler;
                                    mc.setScreen(null);

                                    craftingHandler = null;
                                }

                                craftTicks = 0;
                                craftFailTimes++;

                                if (!continueOnCraft.get()) return;
                            }
                        }
                    }
                }
            }
        }


        // Movement Predict


        if (predictMode.get() == PredictMode.Average) {
            if (!predictions.isEmpty()) {
                for (int id : new HashSet<>(predictions.keySet())) {
                    Entity entity = mc.world.getEntityById(id);

                    if (entity instanceof PlayerEntity
                        && (!ignoreSelf.get() || ignoreSelf.get() && entity != mc.player)
                        && VectorUtils.distance(mc.player.getPos(), entity.getPos()) <= predictRange.get()) {

                        List<Vec3d> positions = new ArrayList<>(predictions.get(id));

                        if (!positions.isEmpty() && positions.size() > predictTicks.get()) positions.remove(0);

                        positions.add(entity.getPos());
                        predictions.replace(id, positions);
                    } else if (ignoreSelf.get() && entity == mc.player) {
                        predictions.remove(id);
                    }
                }

                for (int id : new HashSet<>(predictions.keySet())) {
                    Entity entity = mc.world.getEntityById(id);

                    if ((!ignoreSelf.get() || ignoreSelf.get() && entity != mc.player)
                        && entity instanceof PlayerEntity && entity.isAlive()
                        && VectorUtils.distance(mc.player.getPos(), entity.getPos()) <= predictRange.get()) {

                        List<Vec3d> positions = predictions.get(id);

                        Vec3d result = new Vec3d(0, 0, 0);

                        if (!positions.isEmpty()) {
                            int size = 0;

                            for (int i = 0; i < positions.size() && i < predictTicks.get(); i++) {
                                result = result.add(positions.get(i));
                                size++;
                            }

                            result = new Vec3d(result.x / size, result.y / size, result.z / size);

                            double horizontal = horizontalCalcFactor.get();
                            double vertical = verticalCalcFactor.get();

                            double normalXZ = -(horizontal + 1);
                            double normalY = -(vertical + 1);

                            double steps = collisionSteps.get() / 100.0D;
                            boolean found = false;

                            Box box = entity.getBoundingBox();

                            if (checkCollision.get()) {
                                for (double i = 0; i <= (Math.max(horizontal, vertical) + 1); i += steps) {
                                    double previousXZ = -(i - steps);
                                    double previousY = -(i - steps);
                                    double currentXZ = -Math.min(i, (horizontal + 1));
                                    double currentY = -Math.min(i, (vertical + 1));

                                    Vec3d tempResult = result.subtract(entity.getPos()).multiply(currentXZ, currentY, currentXZ).add(entity.getPos());

                                    int chunkX = (int) (tempResult.x / 16);
                                    int chunkZ = (int) (tempResult.z / 16);

                                    if (mc.world.getBlockCollisions(entity, box.offset(tempResult.subtract(entity.getPos()))).iterator().hasNext()
                                        || VectorUtils.distanceXZ(entity.getPos(), tempResult) > maxHorizontalPredictDistance.get()
                                        || VectorUtils.distanceY(entity.getY(), tempResult.getY()) > maxVerticalPredictDistance.get()
                                        || !mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {

                                        result = result.subtract(entity.getPos()).multiply(previousXZ, previousY, previousXZ).add(entity.getPos());

                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) result = result.subtract(entity.getPos()).multiply(normalXZ, normalY, normalXZ).add(entity.getPos());
                            } else {
                                result = result.subtract(entity.getPos()).multiply(normalXZ, normalY, normalXZ).add(entity.getPos());
                            }

                            result = result.subtract(entity.getPos());
                        }

                        if (predictedResults.containsKey(entity.getId())) {
                            predictedResults.replace(entity.getId(), result);
                        } else {
                            predictedResults.put(entity.getId(), result);
                        }
                    } else if (!(entity instanceof PlayerEntity) || ignoreSelf.get() && entity == mc.player) {
                        predictions.remove(id);
                    }
                }
            }

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!predictions.containsKey(player.getId())
                    && (!ignoreSelf.get() || ignoreSelf.get() && player != mc.player)
                    && VectorUtils.distance(mc.player.getPos(), player.getPos()) <= predictRange.get()) {

                    predictions.put(player.getId(), List.of(player.getPos()));
                }
            }
        } else if (predictMode.get() == PredictMode.Accurate) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if ((!ignoreSelf.get() || ignoreSelf.get() && player != mc.player) && VectorUtils.distance(mc.player.getPos(), player.getPos()) <= predictRange.get()) {
                    Vec3d velocity = new Vec3d(player.getX() - player.prevX, player.getY() - player.prevY, player.getZ() - player.prevZ);

                    Box box = player.getBoundingBox();
                    Vec3d result = velocity;

                    for (int i = 0; i < predictTicks.get(); i++) {
                        velocity = velocity.multiply(0.99);
                        velocity = velocity.subtract(0, 0.5000001, 0);

                        Vec3d movement = adjustMovementForSneaking(player, velocity, new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
                        Vec3d collision = adjustMovementForCollisions(player, movement, new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));

                        // Normal Collision

                        if (collision.lengthSquared() > 1.0E-7) {
                            velocity = new Vec3d(collision.getX(), collision.getY(), collision.getZ());
                        }

                        boolean collideX = !MathHelper.approximatelyEquals(movement.x, collision.x);
                        boolean collideZ = !MathHelper.approximatelyEquals(movement.z, collision.z);

                        boolean horizontalCollision = collideX || collideZ;
                        boolean verticalCollision = movement.y != collision.y;

                        boolean onGround = verticalCollision && movement.y < 0.0;

                        // Horizontal Collision

                        if (horizontalCollision) {
                            velocity = new Vec3d(collideX ? 0.0 : velocity.getX(), velocity.getY(), collideZ ? 0.0 : velocity.getZ());
                        }

                        // Vertical Collision / Landing on Block

                        if (movement.y != collision.y || onGround && player.fallDistance > 0.0) {
                            velocity = velocity.multiply(1.0, 0.0, 1.0);
                        }

                        result = result.add(velocity);
                        box = box.offset(velocity);
                    }

                    if (checkCollision.get() && mc.world.getBlockCollisions(player, player.getBoundingBox().offset(result)).iterator().hasNext()) result = Vec3d.ZERO;

                    int id = player.getId();

                    if (predictedResults.containsKey(id)) {
                        predictedResults.replace(id, result);
                    } else {
                        predictedResults.put(id, result);
                    }
                }
            }
        }


        // Collecting Targets


        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isAlive() && !player.isDead() && player != mc.player && Friends.get().shouldAttack(player) && !(player instanceof FakePlayerEntity)
                && VectorUtils.distanceXZ(mc.player.getPos(), player.getPos()) <= horizontalTargetRange.get()
                && VectorUtils.distanceY(mc.player.getPos(), player.getPos()) <= verticalTargetRange.get()) {

                targets.add(player);
            }
        }

        for (FakePlayerEntity player : FakePlayerManager.getPlayers()) {
            if (VectorUtils.distanceXZ(mc.player.getPos(), player.getPos()) <= horizontalTargetRange.get()
                && VectorUtils.distanceY(mc.player.getPos(), player.getPos()) <= verticalTargetRange.get()) {

                targets.add(player);
            }
        }


        // Moving Delay


        if (movingDelay.get() && !targets.isEmpty()) {
            for (PlayerEntity target : targets) {
                int id = target.getId();

                if (prevPositions.containsKey(id) && prevPositions.get(id) != null) {
                    List<Vec3d> positions = new ArrayList<>(prevPositions.get(id));

                    if (!positions.isEmpty() && positions.size() > movingTicks.get()) positions.remove(0);

                    positions.add(target.getPos());
                    prevPositions.replace(id, positions);
                } else {
                    prevPositions.put(id, List.of(target.getPos()));
                }
            }

            for (int id : new HashSet<>(prevPositions.keySet())) {
                Entity entity = mc.world.getEntityById(id);

                if (entity instanceof PlayerEntity) {
                    List<Vec3d> positions = new ArrayList<>(prevPositions.get(id));

                    if (VectorUtils.distanceXZ(mc.player.getPos(), entity.getPos()) >= horizontalTargetRange.get()
                        || VectorUtils.distanceY(mc.player.getY(), entity.getY()) >= verticalTargetRange.get()) {

                        prevPositions.remove(id);
                    } else if (prevPositions.get(id) != null && !positions.isEmpty() && VectorUtils.distance(positions.get(0), positions.get(positions.size() - 1)) >= movingDistance.get()) {
                        boolean valid = !ignoreInHole.get();

                        if (ignoreInHole.get()) {
                            BlockPos pos = entity.getBlockPos();

                            if (isValidHole(pos, true) && isValidHole(pos.up(), false)) {
                                int air = 0;
                                int surr = 0;

                                for (Direction direction : Direction.values()) {
                                    if (direction != Direction.UP && direction != Direction.DOWN) {
                                        if (ignoreDoubles.get() && isValidHole(pos.offset(direction), true) && isValidHole(pos.offset(direction).up(), false)) {
                                            int surrounded = 0;

                                            for (Direction dir : Direction.values()) {
                                                if (dir != Direction.UP && dir != Direction.DOWN
                                                    && mc.world.getBlockState(pos.offset(direction).offset(dir)).getBlock().getBlastResistance() >= 600.0F) {

                                                    surrounded++;
                                                }
                                            }

                                            if (surrounded == 3) {
                                                air++;
                                            } else {
                                                air = 0;
                                            }
                                        } else if (mc.world.getBlockState(pos.offset(direction)).getBlock().getBlastResistance() >= 600.0F) {
                                            surr++;
                                        }
                                    }
                                }

                                valid = ignoreDoubles.get() ? (air != 1 || surr < 3) && (air != 0 || surr < 4) : surr < 4;
                            }
                        }

                        if (valid) {
                            placeDelay = movingPlaceDelay.get();
                            breakDelay = movingBreakDelay.get();

                            break;
                        }
                    }
                } else {
                    prevPositions.remove(id);
                }
            }
        }


        // Delay Hotkeys


        if (speedPlaceKey.get().isPressed()) placeDelay = speedPlaceDelay.get();
        if (speedBreakKey.get().isPressed()) breakDelay = speedBreakDelay.get();


        // Actual Place & Break Calculations


        if (targets != null && !targets.isEmpty()) {
            targets.sort((target1, target2) -> sort(target1, target2, sortMode.get()));

            // Calculating

            if (calcTicks > 10) {
                calcTime = 0;
                calcTicks = 0;
            }

            // Finding Items

            int bedItem = -1;

            if (mc.player.getMainHandStack().getItem() instanceof BedItem) {
                bedItem = mc.player.getInventory().selectedSlot;
            } else if (mc.player.getOffHandStack().getItem() instanceof BedItem) {
                bedItem = 45;
            } else {
                for (int i = 0; i <= 8; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() instanceof BedItem) {
                        bedItem = i;
                        break;
                    }
                }
            }

            // Place Calculation

            if ((placeTicks >= placeDelay || placeDelay == 0)
                && bedCount(0, 8) > 0 && bedItem >= 0
                && placeMode.get() != PlaceMode.None) {

                if (!keepPlacePosKey.get().isPressed() || placePosition == null || placeDirection == null) {
                    Pair<BlockPos, Direction> bed = getPlacePos();

                    if (bed != null && bed.getA() != null && bed.getB() != null) {
                        placePosition = bed.getA();
                        placeDirection = bed.getB();
                    }
                }

                if (placeDirection != null && isValidBedHead(placePosition.offset(placeDirection)) && canPlace(placePosition, Blocks.RED_BED.getDefaultState(), false)) {
                    addRenderBlock(placePosition, placeDirection);
                    place(placePosition, placeDirection, bedItem);

                    // Resetting Timers

                    placeTicks = 0;
                    if (resetDelayOnPlace.get()) breakTicks = 0;

                    // Check if should keep Position

                    if (!keepPlacePosKey.get().isPressed()) {
                        placePosition = null;
                        placeDirection = null;

                        tempPlacePosition = null;
                        tempPlaceDirection = null;
                    }

                    // 0 Tick Break

                    if (breakDelay == 0) {
                        breakPosition = placePosition;
                    } else {
                        return;
                    }
                }
            }



            // Break Calculation



            if (breakMode.get() != BreakMode.None && (breakTicks >= breakDelay || breakDelay == 0)) {
                breakPosition = getBreakPosition();

                // Actual Breaking

                if (breakPosition != null) {
                    breakBed(breakPosition);

                    // Resetting Position & Timers

                    tempBreakPosition = null;
                    breakPosition = null;

                    stopBreakThreads();

                    breakTicks = 0;
                    if (resetDelayOnBreak.get()) placeTicks = 0;
                }
            }
        }
    }



    // Reloading



    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        reload();
    }



    // Silent Craft



    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (silentCraft.get() && event.screen instanceof CraftingScreen) {
            craftingHandler = ((CraftingScreen) event.screen).getScreenHandler();

            mc.setScreen(null);
            event.cancel();
        }
    }



    // Movement Predict



    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityPositionS2CPacket packet && packet.getId() >= 0 && predictMode.get() == PredictMode.Packet) {
            int id = packet.getId();
            Entity entity = mc.world.getEntityById(id);

            if (entity instanceof PlayerEntity && VectorUtils.distance(mc.player.getPos(), entity.getPos()) <= predictRange.get()) {
                Vec3d relative = new Vec3d(entity.getX() - packet.getX(), entity.getY() - packet.getY(), entity.getZ() - packet.getZ());

                if (!mc.world.getBlockCollisions(entity, entity.getBoundingBox().offset(relative)).iterator().hasNext()) {
                    if (!predictedResults.containsKey(id)) {
                        predictedResults.put(id, relative);
                    } else {
                        predictedResults.replace(id, relative);
                    }
                }
            }
        } else if (event.packet instanceof CloseScreenS2CPacket) {
            resetSilentHandler(false);
        }
    }



    // Render



    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBlocks.isEmpty()) {
            renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
            renderBlocks.forEach(block -> block.render(event, shapeMode.get()));
        }

        if (predictedResults != null && !predictedResults.isEmpty() && debugRender.get()) {
            for (int id : new HashSet<>(predictedResults.keySet())) {
                Entity entity = mc.world.getEntityById(id);

                if (entity instanceof PlayerEntity) {
                    event.renderer.box(entity.getBoundingBox().offset(predictedResults.get(id)), debugSideColor.get(), debugLineColor.get(), debugShapeMode.get(), 0);
                }
            }
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if ((!ignoreSelf.get() || ignoreSelf.get() && player != mc.player) && VectorUtils.distance(mc.player.getPos(), player.getPos()) <= predictRange.get()) {
                Vec3d position = player.getPos();
                Vec3d velocity = new Vec3d(position.getX() - player.prevX, position.getY() - player.prevY, position.getZ() - player.prevZ);

                Box box = player.getBoundingBox();
                Vec3d result = velocity;

                for (int i = 0; i < predictTicks.get(); i++) {
                    Vec3d movement = adjustMovementForSneaking(player, velocity, new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
                    Vec3d collision = adjustMovementForCollisions(player, movement, new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));

                    // Normal Collision

                    if (collision.lengthSquared() > 1.0E-7) {
                        velocity = new Vec3d(collision.getX(), collision.getY(), collision.getZ());
                    }

                    boolean approximatelyX = !MathHelper.approximatelyEquals(movement.x, collision.x);
                    boolean approximatelyZ = !MathHelper.approximatelyEquals(movement.z, collision.z);

                    boolean horizontalCollision = approximatelyX || approximatelyZ;
                    boolean verticalCollision = movement.y != collision.y;

                    boolean onGround = verticalCollision && movement.y < 0.0;

                    // Horizontal Collision

                    if (horizontalCollision) {
                        velocity = new Vec3d(approximatelyX ? 0.0 : velocity.getX(), velocity.getY(), approximatelyZ ? 0.0 : velocity.getZ());
                    }

                    // Vertical Collision / Landing on Block

                    if (movement.y != collision.y || onGround && player.fallDistance > 0.0) {
                        velocity = velocity.multiply(1.0, 0.0, 1.0);
                    }

                    result = result.add(velocity);
                    box = box.offset(velocity);

                    if (mc.world.getBlockCollisions(player, box).iterator().hasNext()) {
                        result.subtract(velocity);
                        break;
                    }
                }

                //event.renderer.box(box, debugSideColor.get(), debugLineColor.get(), debugShapeMode.get(), 0);
            }
        }
    }



    // Place Calculation



    private Pair<BlockPos, Direction> getPlacePos() {
        List<BlockPos> positions = new ArrayList<>();

        int pX = mc.player.getBlockX();
        int pY = mc.player.getBlockY();
        int pZ = mc.player.getBlockZ();

        boolean unthreaded = false;

        // Collecting the best unthreaded Positions

        if (placeThreading.get() && unthreadedMode.get() != UnthreadedMode.None) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                PlayerEntity target = targets.get(i);

                if (VectorUtils.distanceXZ(mc.player.getPos(), target.getPos()) <= horizontalTargetRange.get()
                    && VectorUtils.distanceY(mc.player.getPos(), target.getPos()) <= verticalTargetRange.get()) {

                    if (allowPlaceInside.get() && unthreadedMode.get() == UnthreadedMode.Normal) {
                        for (BlockPos position : insidePositions) {
                            BlockPos pos = target.getBlockPos().add(position);

                            if (!positions.contains(pos) && isValidUnthreaded(pos)) positions.add(pos);
                        }
                    } else if (allowPlaceInside.get() && unthreadedMode.get() == UnthreadedMode.Inside) {
                        BlockPos pos = target.getBlockPos();

                        if (!positions.contains(pos) && isValidUnthreaded(pos)) positions.add(pos);
                        if (!positions.contains(pos.up()) && isValidUnthreaded(pos.up())) positions.add(pos.up());
                    } else if (unthreadedMode.get() == UnthreadedMode.Outside
                        || !allowPlaceInside.get() && (unthreadedMode.get() == UnthreadedMode.Normal || unthreadedMode.get() == UnthreadedMode.Inside)) {

                        for (BlockPos position : outsidePositions) {
                            BlockPos pos = target.getBlockPos().add(position);

                            if (!positions.contains(pos) && isValidUnthreaded(pos)) {
                                boolean inside = true;

                                if (!allowPlaceInside.get()) {
                                    for (PlayerEntity player : targets) {
                                        if (player.getBlockPos().equals(pos) || player.getBlockPos().up().equals(pos)) {
                                            inside = false;
                                            break;
                                        }
                                    }
                                }

                                if (inside) positions.add(pos);
                            }
                        }
                    } else if (unthreadedMode.get() == UnthreadedMode.Ceil) {
                        List<BlockPos> tempPositions = new ArrayList<>();

                        int tX = target.getBlockX();
                        int tY = target.getBlockY();
                        int tZ = target.getBlockZ();

                        int horizontal = (int) Math.floor(horizontalUnthreadedRange.get());
                        int vertical = (int) Math.floor(verticalUnthreadedRange.get());

                        for (int x = tX - horizontal; x <= tX + horizontal; x++) {
                            for (int z = tZ - horizontal; z <= tZ + horizontal; z++) {
                                for (int y = Math.max(mc.world.getBottomY(), tY - vertical); y <= Math.min(tY + vertical, mc.world.getTopY()); y++) {
                                    int dX = Math.abs(x - tX);
                                    int dY = Math.abs(y - tY);
                                    int dZ = Math.abs(z - tZ);

                                    if (dX <= horizontal && dY <= vertical && dZ <= horizontal) {
                                        BlockPos pos = new BlockPos(x, y, z);

                                        if (!positions.contains(pos) && isValidUnthreaded(pos)) {
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

                        tempPositions.sort(Comparator.comparingDouble(position -> VectorUtils.distance(target.getPos(), Vec3d.ofCenter(position))));

                        for (int pos = 0; pos < tempPositions.size() && pos < maxUnthreadedPositionsPerPlayer.get(); pos++) positions.add(tempPositions.get(pos));
                    }
                }

                positions.sort(Comparator.comparingDouble(this::distanceToTargets));
            }

            // Unthreaded Calculation

            double bestDamage = 0;

            if (!positions.isEmpty()) {
                for (int i = 0; i < positions.size() && i < maxUnthreadedPositions.get(); i++) {
                    BlockPos pos = positions.get(i);

                    if (isVisibleToTargets(pos, null)) {
                        double selfDamage = bedDamage(mc.player, pos, null, false);

                        if (selfDamage < maxSelfDamage.get() && (!antiSuicide.get() || (antiSuicide.get() && selfDamage < getTotalHealth(mc.player)))) {
                            if (!checkFriendsOnPlace.get() || checkFriends.get() && checkFriendsOnPlace.get() && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(pos, null))) {
                                double dmg = getDamageToTargets(pos, null, true);

                                if (dmg > bestDamage && dmg >= minTargetDamage.get()) {
                                    Direction direction = null;

                                    for (Direction dir : getDirectionsForBlock(pos)) {
                                        if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                            direction = dir;
                                            break;
                                        }
                                    }

                                    if (direction != null
                                        && VectorUtils.distanceXZ(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= horizontalPlaceRange.get()
                                        && VectorUtils.distanceY(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= verticalPlaceRange.get()
                                        && (airPlace.get() || !airPlace.get() && getPlaceSide(pos.offset(direction)) != null)) {
                                        pos = pos.offset(direction);
                                        direction = direction.getOpposite();

                                        tempPlacePosition = pos;
                                        tempPlaceDirection = direction;

                                        bestDamage = dmg;
                                    }
                                }
                            }
                        }
                    }
                }

                if (tempPlacePosition != null && tempPlaceDirection != null) unthreaded = true;
            }
        }

        // Normal / Threaded Calculations

        if (unthreadedMode.get() == UnthreadedMode.None || !unthreaded || (unthreadedMode.get() != UnthreadedMode.None && (positions.isEmpty() || tempPlacePosition == null || tempPlaceDirection == null))) {
            if (!placeThreading.get() || placeThreading.get() && !multiPlaceThreading.get()) {
                placeThread = new Thread(() -> {
                    long prevTime = System.currentTimeMillis();
                    double bestDamage = 0;

                    if (mc.world != null && mc.player != null) {
                        if (placeMode.get() == PlaceMode.Around || placeMode.get() == PlaceMode.Both) {
                            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                                PlayerEntity target = targets.get(i);

                                for (BlockPos position : outsidePositions) {
                                    BlockPos pos = target.getBlockPos().add(position);

                                    double damage = 0;

                                    if (VectorUtils.distanceXZ(pX, pZ, pos.getX(), pos.getZ()) <= horizontalPlaceRange.get()
                                        && VectorUtils.distanceY(pY, pos.getY()) <= verticalPlaceRange.get()
                                        && (!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos))
                                        && canPlace(pos, Blocks.RED_BED.getDefaultState(), false)
                                        && !getDirectionsForBlock(pos).isEmpty()) {

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
                                            double selfDamage = bedDamage(mc.player, pos, null, false);

                                            if (selfDamage < maxSelfDamage.get() && (!antiSuicide.get() || (antiSuicide.get() && selfDamage < getTotalHealth(mc.player)))) {
                                                if (!checkFriendsOnPlace.get() || checkFriends.get() && checkFriendsOnPlace.get() && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(pos, null))) {
                                                    double dmg = getDamageToTargets(pos, null, true);

                                                    if (dmg > bestDamage && dmg >= minTargetDamage.get()) {
                                                        Direction direction = null;

                                                        for (Direction dir : getDirectionsForBlock(pos)) {
                                                            if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                                                direction = dir;
                                                                break;
                                                            }
                                                        }

                                                        if (direction != null
                                                            && VectorUtils.distanceXZ(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= horizontalPlaceRange.get()
                                                            && VectorUtils.distanceY(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= verticalPlaceRange.get()
                                                            && (airPlace.get() || !airPlace.get() && getPlaceSide(pos.offset(direction)) != null)) {
                                                            pos = pos.offset(direction);
                                                            direction = direction.getOpposite();

                                                            tempPlacePosition = pos;
                                                            tempPlaceDirection = direction;

                                                            bestDamage = dmg;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    bestDamage = Math.max(damage, bestDamage);
                                }
                            }
                        }

                        if (placeMode.get() == PlaceMode.Smart || placeMode.get() == PlaceMode.Both && bestDamage <= minTargetDamage.get() || tempPlacePosition == null || tempPlaceDirection == null) {
                            int horizontal = (int) Math.floor(horizontalPlaceRange.get());
                            int vertical = (int) Math.floor(verticalPlaceRange.get());

                            for (int x = pX - horizontal; x <= pX + horizontal; x++) {
                                for (int z = pZ - horizontal; z <= pZ + horizontal; z++) {
                                    for (int y = Math.max(mc.world.getBottomY(), pY - vertical); y <= Math.min(pY + vertical, mc.world.getTopY()); y++) {
                                        int dX = Math.abs(x - pX);
                                        int dY = Math.abs(y - pY);
                                        int dZ = Math.abs(z - pZ);

                                        if (dX <= horizontal && dY <= vertical && dZ <= horizontal) {
                                            BlockPos pos = new BlockPos(x, y, z);

                                            if ((!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos))
                                                && canPlace(pos, Blocks.RED_BED.getDefaultState(), false)
                                                && !getDirectionsForBlock(pos).isEmpty()) {

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
                                                    double selfDamage = bedDamage(mc.player, pos, null, false);

                                                    if (selfDamage < maxSelfDamage.get() && (!antiSuicide.get() || (antiSuicide.get() && selfDamage < getTotalHealth(mc.player)))) {
                                                        if (!checkFriendsOnPlace.get() || checkFriends.get() && checkFriendsOnPlace.get() && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(pos, null))) {
                                                            double dmg = getDamageToTargets(pos, null, true);

                                                            if (dmg > bestDamage && dmg >= minTargetDamage.get()) {
                                                                Direction direction = null;

                                                                for (Direction dir : getDirectionsForBlock(pos)) {
                                                                    if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                                                        direction = dir;
                                                                        break;
                                                                    }
                                                                }

                                                                if (direction != null
                                                                    && VectorUtils.distanceXZ(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= horizontalPlaceRange.get()
                                                                    && VectorUtils.distanceY(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= verticalPlaceRange.get()
                                                                    && (airPlace.get() || !airPlace.get() && getPlaceSide(pos.offset(direction)) != null)) {
                                                                    pos = pos.offset(direction);
                                                                    direction = direction.getOpposite();

                                                                    tempPlacePosition = pos;
                                                                    tempPlaceDirection = direction;

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
                            tempPlacePosition = null;
                            tempPlaceDirection = null;
                        }
                    }

                    calcTime = Math.max(System.currentTimeMillis() - prevTime, calcTime);
                });
            } else if (placeThreading.get() && multiPlaceThreading.get() && shouldPlace()) {
                if (multiThread != null) {
                    List<BlockPos> tempPositions = new ArrayList<>();

                    if (placeMode.get() == PlaceMode.Around || placeMode.get() == PlaceMode.Both) {
                        for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                            PlayerEntity target = targets.get(i);

                            for (BlockPos position : insidePositions) {
                                BlockPos pos = target.getBlockPos().add(position);

                                if (!tempPositions.contains(pos)
                                    && (!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos))
                                    && canPlace(pos, Blocks.RED_BED.getDefaultState(), false)
                                    && !getDirectionsForBlock(pos).isEmpty()) {

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

                    if (placeMode.get() == PlaceMode.Smart || placeMode.get() == PlaceMode.Both) {
                        int horizontal = (int) Math.floor(horizontalPlaceRange.get());
                        int vertical = (int) Math.floor(verticalPlaceRange.get());

                        for (int x = pX - horizontal; x <= pX + horizontal; x++) {
                            for (int z = pZ - horizontal; z <= pZ + horizontal; z++) {
                                for (int y = Math.max(mc.world.getBottomY(), pY - vertical); y <= Math.min(pY + vertical, mc.world.getTopY()); y++) {
                                    int dX = Math.abs(x - pX);
                                    int dY = Math.abs(y - pY);
                                    int dZ = Math.abs(z - pZ);

                                    if (dX <= horizontal && dY <= vertical && dZ <= horizontal) {
                                        BlockPos pos = new BlockPos(x, y, z);

                                        if (!tempPositions.contains(pos)
                                            && (!smartCalc.get() || smartCalc.get() && canDealDamageToTargets(pos))
                                            && canPlace(pos, Blocks.RED_BED.getDefaultState(), false)
                                            && !getDirectionsForBlock(pos).isEmpty()) {

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

                    List<BlockPos> collected = new ArrayList<>();
                    for (BlockPos pos : tempPositions) if (!collected.contains(pos)) collected.add(pos);

                    if (!collected.isEmpty()) {
                        List<List<BlockPos>> partition = partition(collected, placeThreads.get());

                        if (partition.size() != placeThreads.get() && debugMessages.get()) {
                            info("error when dividing the positions among the threads: " + partition.size() + "partitions / " + placeThreads.get() + "expected.");
                        }

                        for (int i = 0; i < placeThreads.get(); i++) {
                            List<BlockPos> tempThreadPartition = partition.get(i);

                            multiThread[i] = new Thread(() -> {
                                long prevTime = System.currentTimeMillis();

                                BlockPos bestPos = null;
                                Direction bestDir = null;
                                double bestDamage = 0;

                                if (mc.world != null && mc.player != null) {
                                    for (BlockPos pos : new ArrayList<>(tempThreadPartition)) {
                                        if (isVisibleToTargets(pos, null)) {
                                            double selfDamage = bedDamage(mc.player, pos, null, false);

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
                                                            && VectorUtils.distanceXZ(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= horizontalPlaceRange.get()
                                                            && VectorUtils.distanceY(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= verticalPlaceRange.get()
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
                                }

                                calcTime = Math.max(System.currentTimeMillis() - prevTime, calcTime);
                            });
                        }

                        for (Thread thread : multiThread) {
                            if (thread != null && !thread.isAlive() && !thread.isInterrupted() && thread.getState() == Thread.State.NEW) thread.start();
                        }
                    }
                }
            }
        }

        if (tempPlacePosition != null && tempPlaceDirection != null) {
            stopPlaceThreads();

            return new Pair<>(tempPlacePosition, tempPlaceDirection);
        } else if (unthreadedMode.get() != UnthreadedMode.None && (!unthreaded || tempPlacePosition == null || tempPlaceDirection == null) || unthreadedMode.get() == UnthreadedMode.None) {
            if (tempPlacePosition == null || tempPlaceDirection == null) {
                if ((!placeThreading.get() || placeThreading.get() && !multiPlaceThreading.get()) && placeThread != null && placeThread.getState() == Thread.State.NEW) {
                    if (placeThreading.get()) {
                        placeThread.start();
                    } else {
                        placeThread.run();
                    }

                    if (tempPlacePosition != null && tempPlaceDirection != null) {
                        stopPlaceThreads();

                        return new Pair<>(tempPlacePosition, tempPlaceDirection);
                    }
                } else if (placeThreading.get() && multiPlaceThreading.get() && multiThread != null && bestPositions != null && !bestPositions.isEmpty() && bestPositions.size() >= placeThreads.get()) {
                    double bestDamage = 0;

                    for (Triplet<BlockPos, Direction, Double> value : new ArrayList<>(bestPositions)) {
                        if (value.getC() > bestDamage) {
                            tempPlacePosition = value.getA();
                            tempPlaceDirection = value.getB();
                            bestDamage = value.getC();
                        }
                    }

                    if (bestDamage < minTargetDamage.get()) {
                        tempPlacePosition = null;
                        tempPlaceDirection = null;
                    }

                    stopPlaceThreads();
                }
            }
        }

        return null;
    }



    // Break Calculation



    private BlockPos getBreakPosition() {
        Runnable breaking = () -> {
            long prevTime = System.currentTimeMillis();

            if (mc.world != null && mc.player != null) {
                for (BlockEntity entity : getBlockEntities()) {
                    if (entity instanceof BedBlockEntity) {
                        BlockPos pos = entity.getPos();

                        boolean valid = breakMode.get() != BreakMode.Around;

                        if (breakMode.get() == BreakMode.Around) {
                            if (!targets.isEmpty()) {
                                for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                                    PlayerEntity target = targets.get(i);

                                    for (BlockPos position : insidePositions) {
                                        if (pos.equals(target.getBlockPos().add(position))) {
                                            valid = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (valid && World.isValid(pos) && mc.world.getBlockState(pos).getBlock() instanceof BedBlock
                            && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= breakRange.get()) {

                            BlockPos head = mc.world.getBlockState(pos).get(Properties.BED_PART) == BedPart.HEAD ? pos : null;
                            BlockPos foot = mc.world.getBlockState(pos).get(Properties.BED_PART) == BedPart.FOOT ? pos : null;

                            if (head == null && foot != null) {
                                Direction direction = mc.world.getBlockState(foot).get(HorizontalFacingBlock.FACING);

                                if (direction != null
                                    && mc.world.getBlockState(foot.offset(direction)).getBlock() instanceof BedBlock
                                    && mc.world.getBlockState(foot.offset(direction)).get(Properties.BED_PART) == BedPart.HEAD
                                    && mc.world.getBlockState(foot.offset(direction)).get(HorizontalFacingBlock.FACING) == direction) {
                                    head = foot.offset(direction);
                                }

                                if (!getBreakDirections(foot).isEmpty()) {
                                    for (Direction dir : getBreakDirections(foot)) {
                                        if (mc.world.getBlockState(foot.offset(dir)).getBlock() instanceof BedBlock
                                            && mc.world.getBlockState(foot.offset(dir)).get(Properties.BED_PART) == BedPart.HEAD) {
                                            head = foot.offset(dir);
                                            break;
                                        }
                                    }
                                }
                            }

                            if (foot == null && head != null) {
                                Direction direction = mc.world.getBlockState(head).get(HorizontalFacingBlock.FACING);

                                if (direction != null
                                    && mc.world.getBlockState(pos.offset(direction.getOpposite())).getBlock() instanceof BedBlock
                                    && mc.world.getBlockState(pos.offset(direction.getOpposite())).get(Properties.BED_PART) == BedPart.FOOT
                                    && mc.world.getBlockState(pos.offset(direction.getOpposite())).get(HorizontalFacingBlock.FACING).getOpposite() == direction) {
                                    foot = pos.offset(direction.getOpposite());
                                }

                                if (!getBreakDirections(head).isEmpty()) {
                                    for (Direction dir : getBreakDirections(head)) {
                                        if (mc.world.getBlockState(head.offset(dir)).getBlock() instanceof BedBlock
                                            && mc.world.getBlockState(head.offset(dir)).get(Properties.BED_PART) == BedPart.FOOT) {
                                            foot = head.offset(dir);
                                            break;
                                        }
                                    }
                                }
                            }

                            Pair<BlockPos, BlockPos> bed = new Pair<>(head, foot);

                            if (head != null && foot != null) {
                                double selfDamage = !checkSelfBreak.get() ? 0 : bedDamage(mc.player, pos, bed, true);

                                if (!checkSelfBreak.get() || selfDamage < maxSelfDamage.get()
                                    && (!antiSuicideBreak.get() || (antiSuicideBreak.get() && selfDamage < getTotalHealth(mc.player)))
                                    && (minTargetDamage.get() == 0 || getDamageToTargets(pos, bed, false) >= minTargetDamage.get())) {
                                    if (checkFriends.get() && checkFriendsOnBreak.get()) {
                                        if (!getBreakDirections(pos).isEmpty() && isVisibleToTargets(head, bed)
                                            && (!antiFriendPop.get() || antiFriendPop.get() && checkDamageToFriends(head, bed))
                                            && getDamageToFriends(head, bed) <= maxFriendDamage.get()) {

                                            tempBreakPosition = pos;
                                        }
                                    } else {
                                        tempBreakPosition = pos;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            calcTime = Math.max(System.currentTimeMillis() - prevTime, calcTime);
        };

        breakThread = new Thread(breaking);

        if (breakThread.getState() == Thread.State.NEW) {
            if (breakThreading.get()) {
                breakThread.start();
            } else {
                breaking.run();
            }
        }

        return tempBreakPosition;
    }



    // Utilities



    // Threading

    private void stopThreads() {
        stopPlaceThreads();
        stopBreakThreads();
    }

    private void stopPlaceThreads() {
        if (multiThread != null) for (Thread thread : multiThread) if (canStop(thread)) thread.interrupt();

        if (canStop(placeThread)) placeThread.interrupt();

        multiThread = new Thread[placeThreads.get()];
        bestPositions = new ArrayList<>();

        placeThread = null;
    }

    private void stopBreakThreads() {
        if (canStop(breakThread)) breakThread.interrupt();

        breakThread = null;
    }

    private boolean canStop(Thread thread) {
        return thread != null && thread.isAlive() && !thread.isInterrupted()
            && thread.getState() != Thread.State.WAITING
            && thread.getState() != Thread.State.TIMED_WAITING
            && thread.getState() != Thread.State.TERMINATED;
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

    // Reloading

    private void reload() {
        if (mc != null && mc.world != null && mc.player != null) {
            reloadRecipes();

            explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
            raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
        }
    }

    private void reloadRecipes() {
        if (mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);

        if (container != null) {
            recipes = new ArrayList<>();

            for (RecipeResultCollection collection : container.findAll("bed")) {
                for (Recipe<?> recipe : collection.getAllRecipes()) {
                    if (recipe.getOutput().getItem() instanceof BedItem) {
                        recipes.add(recipe);
                    }
                }
            }
        }
    }

    private void resetSilentHandler(boolean close) {
        if (mc != null && mc.world != null && mc.player != null && mc.getNetworkHandler() != null
            && (craftingHandler != null
            || mc.player.currentScreenHandler != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler
            && mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {

            if (close) mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(silentCraft.get() && craftingHandler != null ? craftingHandler.syncId : mc.player.currentScreenHandler.syncId));
            mc.player.currentScreenHandler = mc.player.playerScreenHandler;
            mc.setScreen(null);

            craftingHandler = null;
        }
    }

    // Inventory Utils

    private void updateInventory(boolean force, boolean close) {
        if (force || antiDesync.get()) {
            mc.player.getInventory().updateItems();

            if (close && closeAfterwards.get()) {
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            }
        }
    }

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
        int i = 0;

        for (Direction direction : Direction.values()) {
            if (direction != null && direction != Direction.UP && direction != Direction.DOWN) {
                Block block = mc.world.getBlockState(mc.player.getBlockPos().offset(direction)).getBlock();
                if (block != null && block.getBlastResistance() >= 600.0F) i++;
            }
        }

        return ((bedCount(0, 35) == 0 || bedCount(0, 35) <= minBeds.get()))
            && getEmptySlots(0, 35) >= minEmptySlots.get()
            && (!craftOnlyOnGround.get() || craftOnlyOnGround.get() && mc.player.isOnGround())
            && (!craftOnlyInHole.get() || craftOnlyInHole.get() && i == 4)
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

        updateInventory(false, false);
    }

    // Bed Direction Utils

    private List<Direction> getDirectionsForBlock(BlockPos pos) {
        if (placeDirectionPriority.get() == PlaceDirectionPriority.Lock) return List.of(mc.player.getHorizontalFacing());
        List<Direction> directions = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction != null && direction != Direction.UP && direction != Direction.DOWN && mc.world.getBlockState(pos.offset(direction)).getMaterial().isReplaceable()) {
                directions.add(direction);
            }
        }

        if (placeDirectionPriority.get() == PlaceDirectionPriority.Closest) directions.sort(Comparator.comparingDouble(dir -> dir != null ? VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(dir))) : VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos))));
        if (placeDirectionPriority.get() == PlaceDirectionPriority.Random) Collections.shuffle(directions);

        return directions;
    }

    private List<Direction> getBreakDirections(BlockPos pos) {
        List<Direction> directions = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction != null && direction != Direction.UP && direction != Direction.DOWN) {
                directions.add(direction);
            }
        }

        if (breakDirectionPriority.get() == BreakDirectionPriority.Closest) directions.sort(Comparator.comparingDouble(dir -> VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(dir)))));
        if (breakDirectionPriority.get() == BreakDirectionPriority.Random) Collections.shuffle(directions);

        return directions;
    }

    private Direction getBestSide(BlockPos pos) {
        List<Direction> directions = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (mc.world.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) directions.add(dir);
        }

        if (directions.isEmpty()) directions.addAll(List.of(Direction.values()));
        if (directions.isEmpty()) return null;

        directions.sort(Comparator.comparingDouble(dir -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos.offset(dir)))));
        return directions.get(0);
    }

    // Movement Predict

    private Vec3d adjustMovementForSneaking(PlayerEntity player, Vec3d movement, Box box) {
        if (player.isSneaking() && (player.isOnGround() || player.fallDistance < 0 && !mc.world.isSpaceEmpty(player, box.offset(0, player.fallDistance, 0)))) {
            double x = movement.x;
            double z = movement.z;

            while (true) {
                // X Collision

                while (x != 0 && mc.world.isSpaceEmpty(player, box.offset(x, 0, 0))) {
                    if (x < 0.05 && x >= -0.05) {
                        x = 0;
                    } else if (x > 0) {
                        x -= 0.05;
                    } else {
                        x += 0.05;
                    }
                }

                while (true) {
                    // Z Collision

                    while (z != 0 && mc.world.isSpaceEmpty(player, box.offset(0, 0, z))) {
                        if (z < 0.05 && z >= -0.05) {
                            z = 0;
                        } else if (z > 0) {
                            z -= 0.05;
                        } else {
                            z += 0.05;
                        }
                    }

                    while (true) {
                        // Y Collision

                        while (x != 0 && z != 0 && mc.world.isSpaceEmpty(player, box.offset(x, 0, z))) {
                            if (x < 0.05 && x >= -0.05) {
                                x = 0.0;
                            } else if (x > 0) {
                                x -= 0.05;
                            } else {
                                x += 0.05;
                            }

                            if (z < 0.05 && z >= -0.05) {
                                z = 0;
                            } else if (z > 0) {
                                z -= 0.05;
                            } else {
                                z += 0.05;
                            }
                        }

                        // Result

                        return new Vec3d(x, movement.y, z);
                    }
                }
            }
        } else {
            return movement;
        }
    }

    private Vec3d adjustMovementForCollisions(PlayerEntity player, Vec3d movement, Box box) {
        return movement.lengthSquared() == 0 ? movement : adjustMovementForCollisions(mc.world.getEntityCollisions(player, box.stretch(movement)), player, movement, box);
    }

    private Vec3d adjustMovementForCollisions(List<VoxelShape> collisions, PlayerEntity player, Vec3d movement, Box box) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);

        if (!collisions.isEmpty()) {
            builder.addAll(collisions);
        }

        WorldBorder border = mc.world.getWorldBorder();
        if (border.canCollide(player, box.stretch(movement))) {
            builder.add(border.asVoxelShape());
        }

        builder.addAll(mc.world.getBlockCollisions(player, box.stretch(movement)));

        List<VoxelShape> tempCollisions = builder.build();

        if (tempCollisions.isEmpty()) {
            return movement;
        } else {
            double x = movement.x;
            double y = movement.y;
            double z = movement.z;

            boolean approximately = Math.abs(x) < Math.abs(z);

            if (y != 0) {
                y = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, box, tempCollisions, y);

                if (y != 0) {
                    box = box.offset(0, y, 0);
                }
            }

            if (approximately && z != 0) {
                z = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, box, tempCollisions, z);

                if (z != 0) {
                    box = box.offset(0, 0, z);
                }
            }

            if (x != 0) {
                x = VoxelShapes.calculateMaxOffset(Direction.Axis.X, box, tempCollisions, x);

                if (!approximately && x != 0) {
                    box = box.offset(x, 0, 0);
                }
            }

            if (!approximately && z != 0) {
                z = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, box, tempCollisions, z);
            }

            return new Vec3d(x, y, z);
        }
    }

    // Other

    private void swingHand(Hand hand) {
        if (renderSwing.get()) {
            mc.player.swingHand(hand);
        } else {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    private void swapTo(int slot) {
        if (slot >= 0 && slot <= 8) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.player.getInventory().selectedSlot = slot;
            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        }
    }

    private boolean shouldPause() {
        if (killAuraPause.get() && Modules.get() != null && Modules.get().isActive(KillAura.class)) return true;
        if (crystalAuraPause.get() && Modules.get() != null && Modules.get().isActive(CrystalAura.class)) return true;
        if (eatPause.get() && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem().isFood() || mc.player.getOffHandStack().getItem().isFood()))) return true;
        return drinkPause.get() && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem() instanceof PotionItem));
    }

    private boolean isValidHole(BlockPos pos, boolean checkDown) {
        return mc.world.getBlockState(pos).getBlock() != Blocks.COBWEB
            && mc.world.getBlockState(pos).getBlock() != Blocks.POWDER_SNOW
            && (!checkDown || (mc.world.getBlockState(pos.down()).getBlock().getBlastResistance() >= 600.0F
            && mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()) != null
            && !mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()))
            && (mc.world.getBlockState(pos).getCollisionShape(mc.world, pos) == null
            || mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty());
    }

    private boolean isValidUnthreaded(BlockPos pos) {
        return (!smartCalc.get() || smartCalc.get() && canPlace(pos, Blocks.RED_BED.getDefaultState(), false) && canDealDamageToTargets(pos))
            && !getDirectionsForBlock(pos).isEmpty()
            && VectorUtils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(pos)) <= horizontalUnthreadedRange.get()
            && VectorUtils.distanceY(mc.player.getPos(), Vec3d.ofCenter(pos)) <= verticalUnthreadedRange.get()
            && mc.world.getBlockState(pos).getMaterial().isReplaceable();
    }

    private double distanceToTargets(BlockPos pos) {
        if (!targets.isEmpty()) {
            Vec3d vec = Vec3d.ofCenter(pos);

            double result = VectorUtils.distance(mc.player.getPos(), vec);

            for (PlayerEntity target : targets) {
                result -= VectorUtils.distance(target.getPos(), vec);
            }

            return result < 0 ? 0 : result;
        }

        return Integer.MAX_VALUE;
    }

    private boolean shouldPlace() {
        boolean checkPlace = true;

        if (waitForBreak.get()) {
            for (BlockEntity entity : getBlockEntities()) {
                if (entity instanceof BedBlockEntity) {
                    BlockPos pos = entity.getPos();

                    if (mc.world.getBlockState(pos).getBlock() instanceof BedBlock
                        && VectorUtils.distanceXZ(mc.player.getPos(), Vec3d.ofCenter(pos)) <= horizontalPlaceRange.get()
                        && VectorUtils.distanceY(mc.player.getPos(), Vec3d.ofCenter(pos)) <= verticalPlaceRange.get()) {

                        BlockPos head = mc.world.getBlockState(pos).get(Properties.BED_PART) == BedPart.HEAD ? pos : null;
                        BlockPos foot = mc.world.getBlockState(pos).get(Properties.BED_PART) == BedPart.FOOT ? pos : null;

                        if (head == null) {
                            Direction direction = mc.world.getBlockState(foot).get(HorizontalFacingBlock.FACING);

                            if (direction != null
                                && mc.world.getBlockState(foot.offset(direction)).getBlock() instanceof BedBlock
                                && mc.world.getBlockState(foot.offset(direction)).get(Properties.BED_PART) == BedPart.HEAD
                                && mc.world.getBlockState(foot.offset(direction)).get(HorizontalFacingBlock.FACING) == direction) {
                                head = foot.offset(direction);
                            }

                            if (head == null && !getBreakDirections(foot).isEmpty()) {
                                for (Direction dir : getBreakDirections(foot)) {
                                    if (mc.world.getBlockState(foot.offset(dir)).getBlock() instanceof BedBlock
                                        && mc.world.getBlockState(foot.offset(dir)).get(Properties.BED_PART) == BedPart.HEAD) {
                                        head = foot.offset(dir);
                                        break;
                                    }
                                }
                            }
                        }

                        if (foot == null) {
                            Direction direction = mc.world.getBlockState(head).get(HorizontalFacingBlock.FACING);

                            if (direction != null
                                && mc.world.getBlockState(pos.offset(direction.getOpposite())).getBlock() instanceof BedBlock
                                && mc.world.getBlockState(pos.offset(direction.getOpposite())).get(Properties.BED_PART) == BedPart.FOOT
                                && mc.world.getBlockState(pos.offset(direction.getOpposite())).get(HorizontalFacingBlock.FACING).getOpposite() == direction) {
                                foot = pos.offset(direction.getOpposite());
                            }

                            if (head == null && !getBreakDirections(head).isEmpty()) {
                                for (Direction dir : getBreakDirections(head)) {
                                    if (mc.world.getBlockState(head.offset(dir)).getBlock() instanceof BedBlock
                                        && mc.world.getBlockState(head.offset(dir)).get(Properties.BED_PART) == BedPart.FOOT) {
                                        foot = head.offset(dir);
                                        break;
                                    }
                                }
                            }
                        }

                        Pair<BlockPos, BlockPos> bed = new Pair<>(head, foot);

                        if (head != null && foot != null
                            && canDealDamageToTargets(pos)
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

    private Iterable<BlockEntity> getBlockEntities() {
        return BlockEntityIterator::new;
    }

    private double getYaw(Entity entity) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90.0F - mc.player.getYaw());
    }

    private double getPitch(Entity entity) {
        return getPitch(new Vec3d(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ()));
    }

    private double getPitch(Vec3d pos) {
        double diffX = pos.getX() - mc.player.getX();
        double diffY = pos.getY() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    private int sort(PlayerEntity player1, PlayerEntity player2, SortPriority priority) {
        return switch (priority) {
            case LowestDistance -> Double.compare(VectorUtils.distance(mc.player.getPos(), player1.getPos()), VectorUtils.distance(mc.player.getPos(), player2.getPos()));
            case HighestDistance -> invertSort(Double.compare(VectorUtils.distance(mc.player.getPos(), player1.getPos()), VectorUtils.distance(mc.player.getPos(), player2.getPos())));
            case LowestHealth -> sortHealth(player1, player2);
            case HighestHealth -> invertSort(sortHealth(player1, player2));
            case ClosestAngle -> sortAngle(player1, player2);
        };
    }

    private int sortHealth(PlayerEntity player1, PlayerEntity player2) {
        boolean one = player1 != null && player1.isAlive() && !player1.isDead();
        boolean two = player2 != null && player2.isAlive() && !player2.isDead();

        if (!one && !two) {
            return 0;
        } else if (one && !two) {
            return 1;
        } else if (!one) {
            return -1;
        }

        return Float.compare(getTotalHealth(player1), getTotalHealth(player2));
    }

    private int sortAngle(PlayerEntity player1, PlayerEntity player2) {
        boolean one = player1 != null && player1.isAlive() && !player1.isDead();
        boolean two = player2 != null && player2.isAlive() && !player2.isDead();

        if (!one && !two) {
            return 0;
        } else if (one && !two) {
            return 1;
        } else if (!one) {
            return -1;
        }

        double yaw1 = Math.abs(getYaw(player1) - mc.player.getYaw());
        double yaw2 = Math.abs(getYaw(player2) - mc.player.getYaw());

        double pitch1 = Math.abs(getPitch(player1) - mc.player.getPitch());
        double pitch2 = Math.abs(getPitch(player2) - mc.player.getPitch());

        return Double.compare(Math.sqrt(yaw1 * yaw1 + pitch1 * pitch1), Math.sqrt(yaw2 * yaw2 + pitch2 * pitch2));
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }



    // Place Utilities



    private boolean canPlace(BlockPos pos, BlockState state, boolean checkEntities) {
        if (pos == null || mc.world == null || mc.world.getBottomY() > pos.getY() || mc.world.getTopY() < pos.getY() || !World.isValid(pos)) return false;
        return mc.world.getBlockState(pos).getMaterial().isReplaceable() && (!checkEntities || mc.world.canPlace(state, pos, ShapeContext.absent()));
    }

    private boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    private boolean isValidBedHead(BlockPos pos) {
        if (pos == null || mc.world == null || mc.world.getBottomY() > pos.getY() || mc.world.getTopY() < pos.getY() || !World.isValid(pos)) return false;
        return mc.world.getBlockState(pos).getMaterial().isReplaceable();
    }

    private Direction getSide(BlockPos pos) {
        Direction side = getPlaceSide(pos);

        return side == null ? (mc.player.getEyeY() > pos.getY() + 0.5 ? Direction.UP : Direction.DOWN) : side;
    }

    private Direction getPlaceSide(BlockPos pos) {
        List<Direction> sides = new ArrayList<>();

        for (Direction side : Direction.values()) {
            BlockState state = mc.world.getBlockState(pos.offset(side));

            if (!state.getMaterial().isReplaceable() && state.getFluidState().isEmpty()) {
                sides.add(side.getOpposite());
            }
        }

        if (sides.size() == 1) {
            return sides.get(0);
        } else if (!sides.isEmpty()) {
            sides.sort(Comparator.comparingDouble(side -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos.offset(side)))));

            return sides.get(0);
        }

        return null;
    }

    private BlockPos getNeighbourPos(BlockPos pos) {
        BlockPos neighbour;
        Direction side = getPlaceSide(pos);

        if (side == null) {
            neighbour = pos;
        } else {
            neighbour = pos.offset(side.getOpposite());
        }

        return neighbour;
    }

    private Vec3d getHitPos(BlockPos pos, Direction side) {
        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (side != null) {
            Direction opposite = side.getOpposite();

            if (!placeRangeBypass.get()) {
                hitPos = hitPos.add(
                    opposite.getOffsetX() == 0 ? 0 : (opposite.getOffsetX() > 0 ? 0.5 : -0.5),
                    opposite.getOffsetY() == 0 ? 0 : (opposite.getOffsetY() > 0 ? 0.5 : -0.5),
                    opposite.getOffsetZ() == 0 ? 0 : (opposite.getOffsetZ() > 0 ? 0.5 : -0.5)
                );
            } else {
                Vec3d target = mc.player.getEyePos();
                VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);

                if (shape != null && !shape.isEmpty()) {
                    Box box = new Box(
                        pos.getX() + (opposite == Direction.EAST ? shape.getMax(Direction.Axis.X) : shape.getMin(Direction.Axis.X)),
                        pos.getY() + (opposite == Direction.UP ? shape.getMax(Direction.Axis.Y) : shape.getMin(Direction.Axis.Y)),
                        pos.getZ() + (opposite == Direction.SOUTH ? shape.getMax(Direction.Axis.Z) : shape.getMin(Direction.Axis.Z)),
                        pos.getX() + (opposite != Direction.WEST ? shape.getMax(Direction.Axis.X) : shape.getMin(Direction.Axis.X)),
                        pos.getY() + (opposite != Direction.DOWN ? shape.getMax(Direction.Axis.Y) : shape.getMin(Direction.Axis.Y)),
                        pos.getZ() + (opposite != Direction.NORTH ? shape.getMax(Direction.Axis.Z) : shape.getMin(Direction.Axis.Z))
                    );

                    double x = MathHelper.clamp(target.getX(), box.minX, box.maxX);
                    double y = MathHelper.clamp(target.getY(), box.minY, box.maxY);
                    double z = MathHelper.clamp(target.getZ(), box.minZ, box.maxZ);

                    hitPos = new Vec3d(x, y, z);
                } else {
                    Box box = new Box(
                        pos.getX() + (opposite == Direction.EAST ? 1.0 : 0.0),
                        pos.getY() + (opposite == Direction.UP ? 1.0 : 0.0),
                        pos.getZ() + (opposite == Direction.SOUTH ? 1.0 : 0.0),
                        pos.getX() + (opposite != Direction.WEST ? 1.0 : 0.0),
                        pos.getY() + (opposite != Direction.DOWN ? 1.0 : 0.0),
                        pos.getZ() + (opposite != Direction.NORTH ? 1.0 : 0.0)
                    );

                    double x = MathHelper.clamp(target.getX(), box.minX, box.maxX);
                    double y = MathHelper.clamp(target.getY(), box.minY, box.maxY);
                    double z = MathHelper.clamp(target.getZ(), box.minZ, box.maxZ);

                    hitPos = new Vec3d(x, y, z);
                }
            }
        }

        return hitPos;
    }

    private void place(BlockPos pos, Direction dir, int slot) {
        if (pos != null && (slot >= 0 && slot <= 8 || slot == 45)) {
            if (slot == 45) {
                place(pos, dir, Hand.OFF_HAND, mc.player.getInventory().selectedSlot);
            } else {
                place(pos, dir, Hand.MAIN_HAND, slot);
            }
        }
    }

    private void place(BlockPos pos, Direction dir, Hand hand, int slot) {
        Direction side = getSide(pos);
        BlockPos neighbour = getNeighbourPos(pos);
        Vec3d hitPos = getHitPos(pos, side);

        boolean sneak = !mc.player.isSneaking() && isClickable(mc.world.getBlockState(neighbour).getBlock());

        float yaw = mc.player.getYaw();

        if (dir != null) {
            yaw = switch (dir) {
                case EAST -> -90.0F;
                case SOUTH -> 0.0F;
                case WEST -> 90.0F;
                default -> -180.0F;
            };
        }

        float pitch = pitchMode.get() == PitchMode.Face ? (float) getPitch(hitPos) : mc.player.getPitch();

        if (pitchMode.get() == PitchMode.Up) pitch = 90.0F;
        if (pitchMode.get() == PitchMode.Zero) pitch = 0.0F;
        if (pitchMode.get() == PitchMode.Down) pitch = -90.0F;
        if (pitchMode.get() == PitchMode.Custom) pitch = customPitch.get().floatValue();

        if (dir != null && ignoreRotationWhenRotated.get() && mc.player.getHorizontalFacing().equals(dir) || placeDirectionPriority.get() == PlaceDirectionPriority.Lock) {
            int prevSlot = -1;
            if (mc.player.getInventory().selectedSlot != slot) {
                prevSlot = mc.player.getInventory().selectedSlot;

                swapTo(slot);
            }

            place(new BlockHitResult(hitPos, side, neighbour, false), hand, packetPlace.get(), sneak);

            if (prevSlot != -1 && swapBack.get()) {
                swapTo(prevSlot);
            }
        } else {
            switch (rotateMode.get()) {
                case Normal -> {
                    Rotations.rotate(yaw, pitch, 500, () -> {
                        int prevSlot = -1;
                        if (mc.player.getInventory().selectedSlot != slot) {
                            prevSlot = mc.player.getInventory().selectedSlot;

                            swapTo(slot);
                        }

                        place(new BlockHitResult(hitPos, side, neighbour, false), hand, packetPlace.get(), sneak);

                        if (prevSlot != -1 && swapBack.get()) {
                            swapTo(prevSlot);
                        }
                    });
                }

                case Packet -> {
                    float prevYaw = mc.player.getYaw();
                    float prevPitch = mc.player.getPitch();

                    int prevSlot = -1;
                    if (mc.player.getInventory().selectedSlot != slot) {
                        prevSlot = mc.player.getInventory().selectedSlot;

                        swapTo(slot);
                    }

                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
                    place(new BlockHitResult(hitPos, side, neighbour, false), hand, packetPlace.get(), sneak);
                    if (rotateBack.get()) mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(prevYaw, prevPitch, mc.player.isOnGround()));

                    if (prevSlot != -1 && swapBack.get()) {
                        swapTo(prevSlot);
                    }
                }
            }
        }
    }

    private void place(BlockHitResult result, Hand hand, boolean packetPlace, boolean sneak) {
        if (hand != null && result != null && mc.world.getWorldBorder().contains(result.getBlockPos()) && mc.player.getStackInHand(hand).getItem() instanceof BlockItem) {
            if (packetPlace) {
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

                swingHand(hand);
            } else {
                if (sneak) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    mc.player.setSneaking(true);
                    mc.player.input.sneaking = false;
                }

                if (mc.interactionManager.interactBlock(mc.player, mc.world, hand, result).shouldSwingHand()) swingHand(hand);

                if (sneak) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                    mc.player.setSneaking(false);
                    mc.player.input.sneaking = false;
                }
            }
        }
    }



    // Break Utilities



    private void breakBed(BlockPos pos) {
        if (pos != null) {
            Vec3d vec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
            VoxelShape shape = mc.world.getBlockState(pos).getCollisionShape(mc.world, pos);

            if (breakRangeBypass.get() && shape != null && !shape.isEmpty()) {
                Vec3d[] closest = new Vec3d[1];
                Vec3d eyes = mc.player.getEyePos();

                shape = shape.offset(pos.getX(), pos.getY(), pos.getZ());

                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    double cX = MathHelper.clamp(eyes.getX(), minX, maxX);
                    double cY = MathHelper.clamp(eyes.getY(), minY, maxY);
                    double cZ = MathHelper.clamp(eyes.getZ(), minZ, maxZ);

                    if (closest[0] == null || eyes.squaredDistanceTo(cX, cY, cZ) < eyes.squaredDistanceTo(closest[0])) {
                        closest[0] = new Vec3d(cX, cY, cZ);
                    }
                });

                vec = closest[0];
            } else if (breakInside.get()) {
                vec = vec.subtract(0, 0.25, 0);
            }

            List<Direction> directions = new ArrayList<>();

            for (Direction dir : Direction.values()) {
                if (mc.world.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) directions.add(dir);
            }

            if (directions.isEmpty()) directions.addAll(List.of(Direction.values()));
            if (!directions.isEmpty()) {
                directions.sort(Comparator.comparingDouble(dir -> VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos.offset(dir)))));

                breakBed(new BlockHitResult(vec, directions.get(0), pos, false));
            }
        }
    }

    private void breakBed(BlockHitResult result) {
        Hand hand = breakHandMode.get() == BreakHandMode.MainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;
        boolean wasSneaking = mc.player.isSneaking();

        if (wasSneaking) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));
        swingHand(hand);

        if (wasSneaking && updateSneaking.get()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
    }



    // Damage Calculation



    private boolean canDealDamageToTargets(BlockPos pos) {
        boolean inRange = false;

        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                if (VectorUtils.distanceXZ(targets.get(i).getPos(), Vec3d.ofCenter(pos)) <= horizontalCalcRange.get()
                    && VectorUtils.distanceY(targets.get(i).getPos(), Vec3d.ofCenter(pos)) <= verticalCalcRange.get()) inRange = true;
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

                if ((!hurtDelayCooldown.get() || player.hurtTime <= 0)
                    && VectorUtils.distance(player.getPos(), Vec3d.ofCenter(pos)) <= horizontalTargetRange.get()
                    && VectorUtils.distanceY(mc.player.getPos(), Vec3d.ofCenter(pos)) <= verticalTargetRange.get()) {
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

                if ((!hurtDelayCooldown.get() || target.hurtTime <= 0) && target != mc.player && Friends.get().isFriend(target) && VectorUtils.distance(Vec3d.ofCenter(pos), target.getPos()) < 11) {
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

                if ((!hurtDelayCooldown.get() || target.hurtTime <= 0) && target != mc.player && Friends.get().isFriend(target) && VectorUtils.distance(Vec3d.ofCenter(pos), target.getPos()) < 11) {
                    double damage = bedDamage(target, pos, bed, true);
                    if (damage >= maxFriendDamage.get() || damage >= getTotalHealth(target)) return false;
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

        BlockHitResult result = BlockView.raycast(raycastContext.getStart(), raycastContext.getEnd(), raycastContext, (context, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (bed != null && state.getBlock() instanceof BedBlock && (pos.equals(bed.getA()) || pos.equals(bed.getB()))) state = Blocks.AIR.getDefaultState();

            return mc.world.raycastBlock(context.getStart(), context.getEnd(), pos, context.getBlockShape(state, mc.world, pos), state);
        }, (context) -> {
            Vec3d vec3d = context.getStart().subtract(context.getEnd());
            return BlockHitResult.createMissed(context.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(context.getEnd()));
        });

        return result != null && result.getBlockPos() != null && result.getBlockPos().equals(end);
    }

    // Bed Damage

    private double bedDamage(PlayerEntity player, BlockPos pos, Pair<BlockPos, BlockPos> bed, boolean shouldIgnoreTerrain) {
        if (player == null || player.getAbilities().creativeMode && !ignoreGamemode.get() && !(player instanceof FakePlayerEntity)) return 0;

        Vec3d position = Vec3d.ofCenter(pos);
        if (explosion == null) explosion = new Explosion(mc.world, null, position.x, position.y, position.z, 5.0F, true, Explosion.DestructionType.DESTROY);
        else ((IExplosion) explosion).set(position, 5.0F, true);

        double distance = Math.sqrt(player.squaredDistanceTo(position));
        if (distance > 10.5) return 0;

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

        if (predictMode.get() != PredictMode.None && predictedResults.containsKey(entity.getId())) {
            box = box.offset(predictedResults.get(entity.getId()));
        }

        double xFactor = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double yFactor = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zFactor = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

        double dX = (1.0 - Math.floor(1.0 / xFactor) * xFactor) / 2.0;
        double dZ = (1.0 - Math.floor(1.0 / zFactor) * zFactor) / 2.0;

        if (xFactor >= 0.0 && yFactor >= 0.0 && zFactor >= 0.0) {
            int miss = 0;
            int hit = 0;

            for (double x = 0.0; x <= 1.0; x += xFactor) {
                for (double y = 0.0; y <= 1.0; y += yFactor) {
                    for (double z = 0.0; z <= 1.0; z += zFactor) {
                        double nX = MathHelper.lerp(x, box.minX, box.maxX);
                        double nY = MathHelper.lerp(y, box.minY, box.maxY);
                        double nZ = MathHelper.lerp(z, box.minZ, box.maxZ);

                        ((IRaycastContext) context).set(new Vec3d(nX + dX, nY, nZ + dZ), source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
                        if (raycast(context, bed, ignoreTerrain).getType() == HitResult.Type.MISS) miss++;

                        hit++;
                    }
                }
            }

            return (float) miss / (float) hit;
        }

        return 0.0F;
    }

    private BlockHitResult raycast(RaycastContext context, Pair<BlockPos, BlockPos> bed, boolean ignoreTerrain) {
        if (mc == null || mc.world == null || mc.player == null) {
            Vec3d diff = context.getStart().subtract(context.getEnd());
            return BlockHitResult.createMissed(context.getEnd(), Direction.getFacing(diff.x, diff.y, diff.z), new BlockPos(context.getEnd()));
        }

        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycast, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (ignoreTerrain && state.getBlock().getBlastResistance() < 600.0F) state = Blocks.AIR.getDefaultState();
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
        int level = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (level > 20) level = 20;

        damage *= (1 - (level / 25.0));
        return damage < 0 ? 0 : damage;
    }

    private double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int level = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= (1 - (level * 0.2));
        }

        return damage < 0 ? 0 : damage;
    }

    private float getDamageLeft(float damage, float armor, float armorToughness) {
        float toughness = 2.0F + armorToughness / 4.0F;
        float general = MathHelper.clamp(armor - damage / toughness, armor * 0.2F, 20.0F);

        return damage * (1.0F - general / 25.0F);
    }

    private float getTotalHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private List<BlockPos> getAffectedBlocks(BlockPos pos) {
        ((IExplosion) explosion).set(Vec3d.ofCenter(pos), 5.0F, true);

        return explosion.getAffectedBlocks();
    }



    // Constants



    private final List<BlockPos> insidePositions = new ArrayList<>() {{
        add(new BlockPos(0, 0, 0));
        add(new BlockPos(0, 1, 0));

        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));

        add(new BlockPos(0, 2, 0));

        add(new BlockPos(1, 1, 0));
        add(new BlockPos(-1, 1, 0));
        add(new BlockPos(0, 1, 1));
        add(new BlockPos(0, 1, -1));
    }};

    private final List<BlockPos> outsidePositions = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));

        add(new BlockPos(1, 1, 0));
        add(new BlockPos(-1, 1, 0));
        add(new BlockPos(0, 1, 1));
        add(new BlockPos(0, 1, -1));

        add(new BlockPos(0, 2, 0));
    }};

    private final List<Item> wools = new ArrayList<>() {{
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

    private final List<Item> planks = new ArrayList<>() {{
        add(Items.ACACIA_PLANKS);
        add(Items.BIRCH_PLANKS);
        add(Items.DARK_OAK_PLANKS);
        add(Items.JUNGLE_PLANKS);
        add(Items.SPRUCE_PLANKS);
        add(Items.OAK_PLANKS);
    }};

    // Enums

    public enum SortPriority {
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth,
        ClosestAngle
    }

    public enum BreakHandMode {
        MainHand,
        OffHand
    }

    public enum RaytraceMode {
        None,
        Feet,
        Body,
        Eyes,
        Any
    }

    public enum UnthreadedMode {
        None,
        Ceil,
        Normal,
        Inside,
        Outside
    }

    public enum PitchMode {
        Keep,
        Up,
        Down,
        Zero,
        Custom,
        Face
    }

    public enum RotateMode {
        Normal,
        Packet
    }

    public enum ClickMode {
        Normal,
        Packet
    }

    public enum PredictMode {
        None,
        Average,
        Accurate,
        Packet
    }

    public enum SwapMode {
        Pickup,
        Swap
    }

    public enum PlaceType {
        Active,
        Passive,
        Recalc
    }

    public enum PlaceMode {
        None,
        Around,
        Smart,
        Both
    }

    public enum BreakMode {
        None,
        Around,
        Smart,
        Both
    }

    public enum PlaceDirectionPriority {
        Normal,
        Random,
        Lock,
        Closest
    }

    public enum BreakDirectionPriority {
        Normal,
        Random,
        Closest
    }

    public enum RenderType {
        None,
        Normal,
        Advanced
    }



    // Rendering



    private void addRenderBlock(BlockPos pos, Direction dir) {
        if (renderType.get() != RenderType.None && pos != null && dir != null) {
            int block = -1;

            pos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());

            if (growIfAlreadyExist.get()) {
                for (int i = 0; i < renderBlocks.size(); i++) {
                    if (renderBlocks.get(i).getPos().equals(pos)) {
                        block = i;
                        break;
                    }
                }
            }

            if (block < 0) {
                renderBlocks.add(renderBlockPool.get().set(pos, dir));
            } else if (growIfAlreadyExist.get()) {
                renderBlocks.get(block).setGrowTicks(growTicks.get());
                if (!keepColor.get()) renderBlocks.get(block).updateColor();
            }
        }
    }

    // Render Class

    private class RenderBlock {
        private BlockPos pos;
        private Direction dir;
        private int ticks;
        private int growTicks;

        private Color sidesTop;
        private Color sidesBottom;
        private Color linesTop;
        private Color linesBottom;

        public RenderBlock set(BlockPos position, Direction direction) {
            this.pos = position;
            this.dir = direction;

            this.ticks = renderTicks.get();
            this.growTicks = 0;

            this.sidesTop = new Color(sideColorTop.get());
            this.sidesBottom = new Color(sideColorBottom.get());
            this.linesTop = new Color(lineColorTop.get());
            this.linesBottom = new Color(lineColorBottom.get());

            return this;
        }

        public void tick() {
            if (this.growTicks > 0) {
                if (ticks < renderTicks.get()) ticks++;
                growTicks--;
            } else {
                this.ticks--;
            }
        }

        public void setGrowTicks(int growTicks) {
            this.growTicks = growTicks;
        }

        public void updateColor() {
            this.sidesTop = new Color(sideColorTop.get());
            this.sidesBottom = new Color(sideColorBottom.get());
            this.linesTop = new Color(lineColorTop.get());
            this.linesBottom = new Color(lineColorBottom.get());
        }

        public BlockPos getPos() {
            return pos;
        }

        public void render(Render3DEvent event, ShapeMode shapeMode) {
            if (this.sidesTop == null || this.sidesBottom == null || this.linesTop == null || this.linesBottom == null || pos == null || this.dir == null) return;

            int preSideTopA = this.sidesTop.a;
            int preSideBottomA = this.sidesBottom.a;
            int preLineTopA = this.linesTop.a;
            int preLineBottomA = this.linesBottom.a;

            this.sidesTop.a *= (double) this.ticks / 8;
            this.sidesBottom.a *= (double) this.ticks / 8;
            this.linesTop.a *= (double) this.ticks / 8;
            this.linesBottom.a *= (double) this.ticks / 8;

            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();

            double px3 = feetLength.get() / 10;
            double px8 = bedHeight.get() / 10;

            double px16 = 1;
            double px32 = 2;

            if (renderType.get() == RenderType.Advanced) {
                if (this.dir == Direction.NORTH) z -= 1;
                else if (this.dir == Direction.WEST) x -= 1;

                // Lines

                if (shapeMode.lines()) {
                    if (this.dir == Direction.NORTH || this.dir == Direction.SOUTH) {
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
                    if (this.dir == Direction.NORTH || this.dir == Direction.SOUTH) {
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
                switch (this.dir) {
                    case NORTH -> event.renderer.box(x, y, z - 1, x + px16, y + px8, z + px32 - 1, sidesBottom, linesBottom, shapeMode, 0);
                    case SOUTH -> event.renderer.box(x, y, z - px16 + 1, x + px16, y + px8, z + px16 + 1, sidesBottom, linesBottom, shapeMode, 0);
                    case EAST -> event.renderer.box(x - px16 + 1, y, z, x + px16 + 1, y + px8, z + px16, sidesBottom, linesBottom, shapeMode, 0);
                    case WEST -> event.renderer.box(x - 1, y, z, x + px32 - 1, y + px8, z + px16, sidesBottom, linesBottom, shapeMode, 0);
                }
            }

            // Resetting the Colors

            this.sidesTop.a = preSideTopA;
            this.sidesBottom.a = preSideBottomA;
            this.linesTop.a = preLineTopA;
            this.linesBottom.a = preLineBottomA;
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
