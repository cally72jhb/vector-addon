package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.BlockEntityIterator;
import cally72jhb.addon.utils.misc.FindItemResult;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
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
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
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
import net.minecraft.world.explosion.Explosion;

import java.util.*;
import java.util.stream.Collectors;

public class BedBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgSwitch = settings.createGroup("Auto Switch");
    private final SettingGroup sgMove = settings.createGroup("Auto Move");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgCity = settings.createGroup("Auto City");
    private final SettingGroup sgCraft = settings.createGroup("Craft");
    private final SettingGroup sgKeys = settings.createGroup("Keybindings");
    private final SettingGroup sgPause = settings.createGroup("Pause");
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
        .sliderMin(0)
        .sliderMax(5)
        .min(1)
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


    // Bypass


    private final Setting<Boolean> smartDelay = sgBypass.add(new BoolSetting.Builder()
        .name("smart-delay")
        .description("Calculates the damage under consideration of the targets hurt time.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetPlace = sgBypass.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Places the beds with packets instead of the normal way.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> randomOffset = sgBypass.add(new BoolSetting.Builder()
        .name("random-offset")
        .description("Offsets the placement hit position randomly the actual position isn't affected.")
        .defaultValue(false)
        .visible(packetPlace::get)
        .build()
    );

    private final Setting<Boolean> placeThreading = sgBypass.add(new BoolSetting.Builder()
        .name("place-threading")
        .description("Calculates the placing in a separate thread to remove lag spikes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakThreading = sgBypass.add(new BoolSetting.Builder()
        .name("break-threading")
        .description("Calculates the breaking in a separate thread to remove lag spikes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgBypass.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predicts the players next movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTerrain = sgBypass.add(new BoolSetting.Builder()
        .name("ignore-terrain")
        .description("Ignores the explodable terrain around you.")
        .defaultValue(true)
        .build()
    );


    // Auto Switch


    private final Setting<SwitchMode> switchMode = sgSwitch.add(new EnumSetting.Builder<SwitchMode>()
        .name("auto-switch-mode")
        .description("How to swap to the slots where the beds are.")
        .defaultValue(SwitchMode.Normal)
        .build()
    );

    private final Setting<Boolean> swapBack = sgSwitch.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the selected slot before you placed the bed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetSwitch = sgSwitch.add(new BoolSetting.Builder()
        .name("packet-switch")
        .description("Switches slots using packets.")
        .defaultValue(true)
        .build()
    );


    // Auto Move


    private final Setting<Boolean> autoMove = sgMove.add(new BoolSetting.Builder()
        .name("auto-move")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> itemsPerTick = sgMove.add(new IntSetting.Builder()
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

    private final Setting<Boolean> bedMove = sgMove.add(new BoolSetting.Builder()
        .name("move-bed")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(true)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> tableMove = sgMove.add(new BoolSetting.Builder()
        .name("move-table")
        .description("Moves the crafting table into a selected hotbar slot.")
        .defaultValue(true)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> fixedMove = sgMove.add(new BoolSetting.Builder()
        .name("fixed-move")
        .description("Moves beds into a set hotbar slot if there are no empty slots.")
        .defaultValue(false)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Integer> bedSlot = sgMove.add(new IntSetting.Builder()
        .name("bed-slot")
        .description("In which slot the bed should be put.")
        .defaultValue(3)
        .sliderMin(1)
        .sliderMax(9)
        .min(1)
        .max(9)
        .visible(() -> autoMove.get() && fixedMove.get() && tableMove.get())
        .noSlider()
        .build()
    );

    private final Setting<Integer> tableSlot = sgMove.add(new IntSetting.Builder()
        .name("table-slot")
        .description("In which slot the crafting table should be put.")
        .defaultValue(5)
        .sliderMin(1)
        .sliderMax(9)
        .min(1)
        .max(9)
        .visible(() -> autoMove.get() && fixedMove.get() && bedMove.get())
        .noSlider()
        .build()
    );

    private final Setting<Boolean> fillEmptySlots = sgMove.add(new BoolSetting.Builder()
        .name("fill-empty-slots")
        .description("Fills empty slots in your hotbar with beds.")
        .defaultValue(true)
        .visible(autoMove::get)
        .build()
    );


    // Place


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
        .min(0)
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
        .description("Resets the break delay after placing the bed.")
        .defaultValue(true)
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

    private final Setting<Boolean> antiSuicidePlace = sgPlace.add(new BoolSetting.Builder()
        .name("anti-suicide-place")
        .description("Will not place bed that can kill you.")
        .defaultValue(true)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .build()
    );

    private final Setting<Integer> maxPlaceDamage = sgPlace.add(new IntSetting.Builder()
        .name("max-place-damage")
        .description("The maximum damage a bed can deal you for it to be placed.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(36)
        .min(0)
        .max(36)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .noSlider()
        .build()
    );

    private final Setting<Integer> minPlaceDamage = sgPlace.add(new IntSetting.Builder()
        .name("min-place-damage")
        .description("The minimum damage a bed has to deal a target for it to be placed.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(36)
        .min(0)
        .max(36)
        .visible(() -> placeMode.get() != PlaceMode.None)
        .noSlider()
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
        .min(0)
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

    private final Setting<Boolean> antiFriendPop = sgBreak.add(new BoolSetting.Builder()
        .name("anti-friend-pop")
        .description("Stops you from breaking beds that pop your friends.")
        .defaultValue(true)
        .visible(() -> breakMode.get() != BreakMode.None)
        .build()
    );

    private final Setting<Integer> maxFriendDamage = sgBreak.add(new IntSetting.Builder()
        .name("max-friend-damage")
        .description("The maximum damage a bed can deal to friends for it to be broken.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(10)
        .min(0)
        .max(36)
        .visible(() -> breakMode.get() != BreakMode.None && antiFriendPop.get())
        .noSlider()
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

    private final Setting<Integer> minBreakDamage = sgBreak.add(new IntSetting.Builder()
        .name("min-break-damage")
        .description("The minimum damage a bed has to deal a target for it to be broken.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(36)
        .min(0)
        .max(36)
        .visible(() -> breakMode.get() != BreakMode.None)
        .noSlider()
        .build()
    );

    private final Setting<Integer> maxBreakDamage = sgBreak.add(new IntSetting.Builder()
        .name("max-break-damage")
        .description("The maximum damage a bed can deal you for it to be broken.")
        .defaultValue(10)
        .sliderMin(0)
        .sliderMax(36)
        .min(0)
        .max(36)
        .visible(() -> breakMode.get() != BreakMode.None && checkSelfBreak.get())
        .noSlider()
        .build()
    );


    // Auto City


    private final Setting<Boolean> city = sgCity.add(new BoolSetting.Builder()
        .name("city")
        .description("Breaks the targets surround to allow the bed bomb to deal damage again.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> forceCity = sgCity.add(new KeybindSetting.Builder()
        .name("force-city")
        .description("The keybinding used to city your targets.")
        .defaultValue(Keybind.fromKey(-1))
        .visible(city::get)
        .build()
    );

    private final Setting<Boolean> autoCity = sgCity.add(new BoolSetting.Builder()
        .name("auto-city")
        .description("Will automatically start citing targets that are fully surrounded.")
        .defaultValue(false)
        .visible(city::get)
        .build()
    );

    private final Setting<Boolean> rotate = sgCity.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the blocks being broken.")
        .defaultValue(true)
        .visible(city::get)
        .build()
    );

    private final Setting<Integer> cityDelay = sgCity.add(new IntSetting.Builder()
        .name("city-delay")
        .description("How many ticks to wait before citing again.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .visible(city::get)
        .noSlider()
        .build()
    );

    private final Setting<Double> cityRange = sgCity.add(new DoubleSetting.Builder()
        .name("city-range")
        .description("How far city can mine and place blocks.")
        .defaultValue(4.5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .visible(city::get)
        .build()
    );

    private final Setting<CityScanMode> cityScanMode = sgCity.add(new EnumSetting.Builder<CityScanMode>()
        .name("city-scan-mode")
        .description("What blocks to prioritize.")
        .defaultValue(CityScanMode.Closest)
        .visible(city::get)
        .build()
    );

    private final Setting<Boolean> instaMine = sgCity.add(new BoolSetting.Builder()
        .name("insta-mine")
        .description("Tryes to mine blocks instant.")
        .defaultValue(false)
        .visible(city::get)
        .build()
    );

    private final Setting<Double> hardness = sgCity.add(new DoubleSetting.Builder()
        .name("hardness")
        .description("The maximum hardness a block can have for it to be instant mined.")
        .defaultValue(0.3)
        .min(0)
        .sliderMax(1)
        .visible(() -> city.get() && instaMine.get())
        .build()
    );

    private final Setting<Integer> instaTickDelay = sgCity.add(new IntSetting.Builder()
        .name("mine-tick-delay")
        .description("The delay between the attempted breaks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(() -> city.get() && instaMine.get())
        .build()
    );

    private final Setting<Boolean> pausePlacingOnCity = sgCity.add(new BoolSetting.Builder()
        .name("pause-placing-on-city")
        .description("Pauses the normal placing while citing.")
        .defaultValue(true)
        .visible(city::get)
        .build()
    );

    private final Setting<Boolean> pauseBreakingOnCity = sgCity.add(new BoolSetting.Builder()
        .name("pause-breaking-on-city")
        .description("Pauses the normal breaking while citing.")
        .defaultValue(true)
        .visible(city::get)
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
        .min(0)
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

    private final Setting<Boolean> craftOnlyWithoutScreen = sgCraft.add(new BoolSetting.Builder()
        .name("only-without-screen")
        .description("Only crafts when you have no screen opened.")
        .defaultValue(false)
        .visible(autoCraft::get)
        .build()
    );

    private final Setting<Boolean> antiDesync = sgCraft.add(new BoolSetting.Builder()
        .name("anti-desync")
        .description("Updates your inventory after your done with crafting.")
        .defaultValue(true)
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

    private final Setting<Integer> bedCount = sgCraft.add(new IntSetting.Builder()
        .name("craft-count")
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

    private final Setting<Integer> minCraftHealth = sgCraft.add(new IntSetting.Builder()
        .name("min-craft-health")
        .description("Min health required to be able to craft new beds.")
        .defaultValue(7)
        .min(0)
        .max(36)
        .sliderMin(0)
        .sliderMax(36)
        .visible(autoCraft::get)
        .noSlider()
        .build()
    );


    // Keybindings


    private final Setting<Keybind> speedPlace = sgKeys.add(new KeybindSetting.Builder()
        .name("speed-place")
        .description("The keybinding used to speed up placement.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> speedBreak = sgKeys.add(new KeybindSetting.Builder()
        .name("speed-break")
        .description("The keybinding used to speed up bed breaking.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Integer> speedPlaceDelay = sgKeys.add(new IntSetting.Builder()
        .name("speed-place-delay")
        .description("How many ticks to wait before placing a bed when the force speed place key is pressed.")
        .defaultValue(5)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
        .build()
    );

    private final Setting<Integer> speedBreakDelay = sgKeys.add(new IntSetting.Builder()
        .name("speed-break-delay")
        .description("How many ticks to wait before breaking a bed when the force speed break key is pressed.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(8)
        .min(0)
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


    // Render


    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand swing animation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the beds will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderOnlyOnce = sgRender.add(new BoolSetting.Builder()
        .name("only-once-render")
        .description("Doesn't render a place animation, if there already is a block being rendered.")
        .defaultValue(true)
        .visible(render::get)
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
        .visible(render::get)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> renderExtra = sgRender.add(new BoolSetting.Builder()
        .name("render-extra")
        .description("Renders a extra element in the middle of the bed.")
        .defaultValue(false)
        .visible(render::get)
        .build()
    );

    private final Setting<Boolean> shrink = sgRender.add(new BoolSetting.Builder()
        .name("shrink")
        .description("Shrinks the block overlay after a while.")
        .defaultValue(false)
        .visible(render::get)
        .build()
    );

    private final Setting<Integer> shrinkTicks = sgRender.add(new IntSetting.Builder()
        .name("shrink-ticks")
        .description("How many ticks to wait before shrinking the block.")
        .defaultValue(5)
        .min(0)
        .max(50)
        .sliderMax(25)
        .visible(() -> render.get() && shrink.get())
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
        .visible(() -> render.get() && shrink.get())
        .noSlider()
        .build()
    );

    private final Setting<Boolean> renderInnerLines = sgRender.add(new BoolSetting.Builder()
        .name("render-inner-lines")
        .description("Renders the inner lines of the bed.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<Boolean> renderInnerSides = sgRender.add(new BoolSetting.Builder()
        .name("render-inner-sides")
        .description("Renders the inner sides of the bed.")
        .defaultValue(true)
        .visible(render::get)
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
        .visible(render::get)
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
        .visible(render::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColorTop = sgRender.add(new ColorSetting.Builder()
        .name("side-color-top")
        .description("The top side color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 5, false))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColorBottom = sgRender.add(new ColorSetting.Builder()
        .name("side-color-bottom")
        .description("The bottom side color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 25, false))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColorTop = sgRender.add(new ColorSetting.Builder()
        .name("line-color-top")
        .description("The top line color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 50, false))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColorBottom = sgRender.add(new ColorSetting.Builder()
        .name("line-color-bottom")
        .description("The bottom line color of the bed.")
        .defaultValue(new SettingColor(205, 0, 255, 255, false))
        .visible(render::get)
        .build()
    );

    private final Random random = new Random();

    private List<PlayerEntity> targets = new ArrayList<>();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    private int placeTicks;
    private int breakTicks;
    private int craftTicks;
    private int cityTicks;
    private int instaTicks;

    private int slot = -1;
    private boolean citing;

    private Thread placeThread;
    private Thread breakThread;

    private Direction placeDir;
    private BlockPos placePos;
    private BlockPos breakPos;

    private BlockPos breakingPos;

    private Explosion explosion = null;
    private RaycastContext raycastContext = null;

    public BedBomb() {
        super(VectorAddon.Combat, "bed-bomb", "Places and blows up beds near your targets.");
    }

    @Override
    public void onActivate() {
        targets.clear();

        placeTicks = 0;
        breakTicks = 0;
        craftTicks = 0;
        cityTicks = 0;
        instaTicks = 0;

        slot = -1;
        citing = false;

        placeDir = null;
        placePos = null;

        if (placeThread != null && placeThread.isAlive()) placeThread.interrupt();
        if (breakThread != null && breakThread.isAlive()) breakThread.interrupt();
        placeThread = null;
        breakThread = null;

        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }

        explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
        if (mc.player != null) raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
    }

    @Override
    public void onDeactivate() {
        if (!renderBlocks.isEmpty()) {
            for (RenderBlock block : renderBlocks) renderBlockPool.free(block);
            renderBlocks.clear();
        }
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) return targets.get(0).getGameProfile().getName();
        else return null;
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
                            boolean extraClick = isEmpty(mc.player.getInventory().getStack(table.getSlot()));

                            clickSlot(0, table.getSlot(), 1, SlotActionType.PICKUP);
                            clickSlot(0, tableSlot.get() + 35, 1, SlotActionType.PICKUP);
                            if (extraClick) slot = table.getSlot();
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
                            boolean extraClick = isEmpty(mc.player.getInventory().getStack(bed.getSlot()));

                            clickSlot(0, bed.getSlot(), 1, SlotActionType.PICKUP);
                            clickSlot(0, (bedSlot.get().equals(tableSlot.get()) ? (tableSlot.get() >= 8 ? tableSlot.get() - 1 : tableSlot.get() + 1) : bedSlot.get()) + 35, 1, SlotActionType.PICKUP);
                            if (extraClick) slot = bed.getSlot();
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
                            BlockHitResult result = new BlockHitResult(randomOffset.get() ? offsetRandom(Vec3d.of(pos)) : Vec3d.ofCenter(pos), getSide(pos), pos, false);

                            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));

                            if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        } else if (!placePositions.isEmpty() && craftPlace.get()) {
                            List<BlockPos> insecure = smartTablePlace.get() ? getAffectedBlocks(mc.player.getBlockPos()) : new ArrayList<>();

                            for (BlockPos position : placePositions) {
                                if (canPlace(position, Blocks.CRAFTING_TABLE.getDefaultState(), true)
                                    && (!smartTablePlace.get() || smartTablePlace.get() && !insecure.contains(position))
                                    && (switchMode.get() != SwitchMode.None || (switchMode.get() == SwitchMode.None
                                    && mc.player.getOffHandStack().getItem() == Items.CRAFTING_TABLE
                                    && mc.player.getMainHandStack().getItem() == Items.CRAFTING_TABLE))) {
                                    place(position, null, table, renderSwing.get(), true, randomOffset.get());
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (mc.player.currentScreenHandler instanceof CraftingScreenHandler handler) {
                if (!canRefill() || !needRefill() || isInventoryFull()) {
                    mc.player.closeHandledScreen();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                } else {
                    if (mc.player.getRecipeBook() != null && !mc.player.getRecipeBook().isGuiOpen(RecipeBookCategory.CRAFTING)) {
                        mc.player.getRecipeBook().setGuiOpen(RecipeBookCategory.CRAFTING, true);
                    }

                    List<RecipeResultCollection> collections = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);

                    for (RecipeResultCollection result : collections) {
                        for (Recipe<?> recipe : result.getRecipes(true)) {
                            if (recipe.getOutput().getItem() instanceof BedItem) {
                                for (int i = 0; i < getEmptySlots(0, 35) && i <= bedCount.get(); i++) mc.getNetworkHandler().sendPacket(new CraftRequestC2SPacket(handler.syncId, recipe, false));
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

        // Placing & Breaking

        if (!targets.isEmpty()) {
            targets = targets.stream().sorted(Comparator.comparing(player -> Players.get().isTargeted(player))).collect(Collectors.toList());

            switch (sortMode.get()) {
                case LowestHealth -> targets.sort(Comparator.comparingDouble(this::getTotalHealth));
                case HighestHealth -> targets.sort(Comparator.comparingDouble(this::getTotalHealth).reversed());
                case LowestDistance, HighestDistance -> targets.sort(Comparator.comparingDouble(player -> VectorUtils.distance(mc.player.getPos(), player.getPos())));
            }

            if (sortMode.get() == SortMode.HighestDistance) Collections.reverse(targets);

            // Calculating

            double pX = mc.player.getX() - 0.5;
            double pY = mc.player.getY();
            double pZ = mc.player.getZ() - 0.5;

            // Auto Bed City

            if (city.get() && (forceCity.get().isPressed() || autoCity.get())) {
                if (canBreak(breakingPos)) {
                    if (instaMine.get() && (instaTicks >= instaTickDelay.get() || instaTickDelay.get() == 0) && VectorUtils.getBlock(breakingPos).getHardness() <= hardness.get()) {
                        instaTicks = 0;

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

                // Getting best Block

                if (cityDelay.get() == 0 || cityTicks >= cityDelay.get()) {
                    boolean shouldCity = false;

                    if (breakingPos != null) {
                        for (CardinalDirection dir : CardinalDirection.values()) {
                            for (PlayerEntity player : targets) {
                                if (breakingPos.down().equals(player.getBlockPos().up())
                                    || breakingPos.offset(dir.toDirection()).equals(player.getBlockPos())
                                    || breakingPos.offset(dir.toDirection()).equals(player.getBlockPos().up())) {
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

                        for (PlayerEntity player : targets) {
                            positions.add(player.getBlockPos());
                            positions.add(player.getBlockPos().up());
                            positions.add(player.getBlockPos().up(2));

                            for (CardinalDirection dir : CardinalDirection.values()) {
                                positions.add(player.getBlockPos().offset(dir.toDirection()));
                                positions.add(player.getBlockPos().offset(dir.toDirection()).up());
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

            // Placing

            if ((placeTicks >= (speedPlace.get().isPressed() ? speedPlaceDelay.get() : placeDelay.get())
                || (speedPlace.get().isPressed() ? speedPlaceDelay.get() : placeDelay.get()) == 0)
                && (!pausePlacingOnCity.get() || pausePlacingOnCity.get() && !citing)
                && bedCount(0, 35) > 0
                && placeMode.get() != PlaceMode.None
                && (switchMode.get() != SwitchMode.None || (switchMode.get() == SwitchMode.None
                && mc.player.getOffHandStack().getItem() instanceof BedItem
                && mc.player.getMainHandStack().getItem() instanceof BedItem))) {

                Runnable placing = () -> {
                    int placeMinX = (int) Math.floor(pX - placeRange.get());
                    int placeMinY = (int) Math.floor(pY - placeRange.get());
                    int placeMinZ = (int) Math.floor(pZ - placeRange.get());

                    int placeMaxX = (int) Math.floor(pX + placeRange.get());
                    int placeMaxY = (int) Math.floor(pY + placeRange.get());
                    int placeMaxZ = (int) Math.floor(pZ + placeRange.get());

                    double placeRangeSq = Math.pow(placeRange.get(), 2);

                    double bestDamage = 0;

                    boolean checkPlace = true;

                    if (waitForBreak.get()) {
                        for (BlockEntity entity : getBlockEntities()) {
                            if (entity instanceof BedBlockEntity) {
                                BlockPos pos = entity.getPos();

                                if (World.isValid(pos) && VectorUtils.getBlock(pos) instanceof BedBlock
                                    && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= placeRange.get()) {
                                    BlockPos checkPos = pos;

                                    if (VectorUtils.getBlock(pos) instanceof BedBlock
                                        && VectorUtils.getBlockState(pos).get(Properties.BED_PART) == BedPart.FOOT
                                        && !getBreakDirections(pos).isEmpty()) {
                                        for (Direction dir : getBreakDirections(pos)) {
                                            if (VectorUtils.getBlock(pos.offset(dir)) instanceof BedBlock
                                                && VectorUtils.getBlockState(pos.offset(dir)).get(Properties.BED_PART) == BedPart.HEAD) {
                                                checkPos = pos.offset(dir);
                                                break;
                                            }
                                        }
                                    }

                                    if (getDamageToTargets(checkPos, true) <= minBreakDamage.get()) {
                                        checkPlace = false;
                                    } else {
                                        checkPlace = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (checkPlace) {
                        for (int y = placeMinY; y <= placeMaxY; y++) {
                            for (int x = placeMinX; x <= placeMaxX; x++) {
                                for (int z = placeMinZ; z <= placeMaxZ; z++) {
                                    if (VectorUtils.distance(pX, pY, pZ, x, y, z) <= placeRangeSq) {
                                        BlockPos pos = new BlockPos(x, y, z);

                                        if (canPlace(pos, Blocks.RED_BED.getDefaultState(), false) && !getDirectionsForBlock(pos).isEmpty()) {
                                            boolean inside = true;

                                            if (!allowPlaceInside.get()) {
                                                for (PlayerEntity player : targets) {
                                                    if (player.getBlockPos().equals(pos) || player.getBlockPos().up().equals(pos)) {
                                                        inside = false;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (inside) {
                                                double selfDamage = mc.player.getAbilities().creativeMode ? 0 : bedDamage(mc.player, pos, false);

                                                if (selfDamage < maxPlaceDamage.get() && (!antiSuicidePlace.get()
                                                    || (antiSuicidePlace.get() && selfDamage < getTotalHealth(mc.player)))) {
                                                    double dmg = getDamageToTargets(pos, false);

                                                    if (dmg > bestDamage) {
                                                        Direction direction = null;

                                                        for (Direction dir : getDirectionsForBlock(pos)) {
                                                            if (canPlace(pos.offset(dir), Blocks.RED_BED.getDefaultState(), true)) {
                                                                direction = dir;
                                                                break;
                                                            }
                                                        }

                                                        if (direction != null && VectorUtils.distance(mc.player.getPos(), Vec3d.of(pos.offset(direction))) <= placeRange.get()
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

                    if (bestDamage < minPlaceDamage.get()) {
                        placeDir = null;
                        placePos = null;
                    }
                };

                if (placeThreading.get()) {
                    placeThread = new Thread(placing);
                    placeThread.start();
                } else {
                    placing.run();
                }
            }

            FindItemResult bed = VectorUtils.findInHotbar(stack -> stack.getItem() instanceof BedItem);

            if (placePos != null && placeDir != null && bed.found()) {
                if (render.get()) {
                    boolean shouldRender = true;

                    if (renderOnlyOnce.get()) {
                        for (RenderBlock block : renderBlocks) {
                            if (block.pos.equals(placePos) && block.ticks < renderTicks.get() / 2) {
                                shouldRender = false;
                                break;
                            }
                        }
                    }

                    if (!renderOnlyOnce.get() || renderOnlyOnce.get() && shouldRender) renderBlocks.add(renderBlockPool.get().set(placePos, placeDir));
                }

                place(placePos, placeDir, bed, renderSwing.get(), true, randomOffset.get());

                if (breakDelay.get() == 0) breakPos = placePos;
                if (resetDelayOnPlace.get()) breakTicks = 0;

                placeDir = null;
                placePos = null;
                placeTicks = 0;
            }

            // Breaking

            if (breakMode.get() != BreakMode.None
                && breakTicks >= (speedBreak.get().isPressed() ? speedBreakDelay.get() : breakDelay.get())
                && (!pauseBreakingOnCity.get() || pauseBreakingOnCity.get() && !citing)) {

                Runnable breaking = () -> {
                    for (BlockEntity entity : getBlockEntities()) {
                        if (entity instanceof BedBlockEntity) {
                            BlockPos pos = entity.getPos();

                            if (World.isValid(pos) && VectorUtils.getBlock(pos) instanceof BedBlock && VectorUtils.distance(mc.player.getPos(), Vec3d.ofCenter(pos)) <= breakRange.get()) {
                                double selfDamage = (!checkSelfBreak.get() || mc.player.getAbilities().creativeMode) ? 0 : bedDamage(mc.player, pos, true);

                                if (!checkSelfBreak.get() || selfDamage < maxBreakDamage.get()
                                    && (!antiSuicideBreak.get() || (antiSuicideBreak.get() && selfDamage < getTotalHealth(mc.player)))
                                    && (minBreakDamage.get() == 0 || getDamageToTargets(pos, true) >= minBreakDamage.get())) {
                                    if (antiFriendPop.get()) {
                                        BlockPos checkPos = pos;

                                        if (VectorUtils.getBlock(pos) instanceof BedBlock
                                            && VectorUtils.getBlockState(pos).get(Properties.BED_PART) == BedPart.FOOT
                                            && !getBreakDirections(pos).isEmpty()) {
                                            for (Direction dir : getBreakDirections(pos)) {
                                                if (VectorUtils.getBlock(pos.offset(dir)) instanceof BedBlock
                                                    && VectorUtils.getBlockState(pos.offset(dir)).get(Properties.BED_PART) == BedPart.HEAD) {
                                                    checkPos = pos.offset(dir);
                                                    break;
                                                }
                                            }
                                        }

                                        double damage = getDamageToFriends(checkPos, true);

                                        if (damage <= maxFriendDamage.get() && checkDamageToFriends(checkPos, true)) breakPos = pos;
                                    } else {
                                        breakPos = pos;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                };

                if (breakThreading.get()) {
                    breakThread = new Thread(breaking);
                    breakThread.start();
                } else {
                    breaking.run();
                }
            }

            if (breakPos != null) {
                breakBed(breakPos);
                breakPos = null;
                breakTicks = 0;

                if (resetDelayOnBreak.get()) placeTicks = 0;
            }
        }
    }

    // Packet Switch

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (packetSwitch.get() && event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            if (mc.player.getInventory().selectedSlot != packet.getSelectedSlot()) {
                Item item = mc.player.getInventory().getStack(packet.getSelectedSlot()).getItem();

                if (!(item instanceof BedItem) && item != Items.CRAFTING_TABLE) event.cancel();
            }
        }
    }

    // Render

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (render.get() && !renderBlocks.isEmpty()) {
            renderBlocks.sort(Comparator.comparingInt(block -> -block.ticks));
            renderBlocks.forEach(block -> block.render(event, shapeMode.get()));
        }
    }

    // Inventory Utils

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.isEmpty() || stack.getCount() == 0 || stack.getItem() instanceof AirBlockItem;
    }

    private double bedCount(int start, int end) {
        double count = 0;

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
            if (stack == null || stack.isEmpty() || stack.getItem() instanceof AirBlockItem) return false;
        }

        return true;
    }

    private boolean canRefill() {
        return (bedCount(0, 35) <= minBeds.get())
            && getEmptySlots(0, 35) >= minEmptySlots.get() && !isInventoryFull()
            && (!craftOnlyOnGround.get() || craftOnlyOnGround.get() && mc.player.isOnGround())
            && (!craftOnlyInHole.get() || craftOnlyInHole.get() && isSurrounded()
            && (!craftOnlyWithoutScreen.get() || craftOnlyWithoutScreen.get() && (mc.currentScreen == null || mc.currentScreen instanceof CraftingScreen)))
            && getTotalHealth(mc.player) >= minCraftHealth.get();
    }

    private boolean needRefill() {
        FindItemResult wool = VectorUtils.find(stack -> wools.contains(stack.getItem()));
        FindItemResult plank = VectorUtils.find(stack -> planks.contains(stack.getItem()));

        if (!wool.found() || !plank.found()) return false;
        return !(wool.getCount() < 3 || plank.getCount() < 3);
    }

    private void clickSlot(int syncId, int id, int button, SlotActionType action) {
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

    // Utils

    private Iterable<BlockEntity> getBlockEntities() {
        return BlockEntityIterator::new;
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

    private double getDamageToTargets(BlockPos pos, boolean ignoreBed) {
        double bestDamage = 0;
        double damage = 0;

        if (!targets.isEmpty()) {
            for (int i = 0; i < maxTargets.get() && i < targets.size(); i++) {
                PlayerEntity player = targets.get(i);

                if ((!smartDelay.get() || player.hurtTime <= 0) && VectorUtils.distance(Vec3d.ofCenter(pos), player.getPos()) < targetRange.get() + 1) {
                    double dmg = bedDamage(player, pos, ignoreBed);

                    if (dmg > bestDamage) bestDamage = dmg;

                    damage += dmg;
                }
            }
        }

        return damage;
    }

    private double getDamageToFriends(BlockPos pos, boolean ignoreBed) {
        double bestDamage = 0;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if ((!smartDelay.get() || player.hurtTime <= 0) && player != mc.player && Friends.get().isFriend(player) && VectorUtils.distance(Vec3d.ofCenter(pos), player.getPos()) < 11) {
                double dmg = bedDamage(player, pos, ignoreBed);

                if (dmg > bestDamage) bestDamage = dmg;
            }
        }

        return bestDamage;
    }

    private boolean checkDamageToFriends(BlockPos pos, boolean ignoreBed) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if ((!smartDelay.get() || player.hurtTime <= 0) && player != mc.player && Friends.get().isFriend(player) && VectorUtils.distance(Vec3d.ofCenter(pos), player.getPos()) < 11) {
                if (bedDamage(player, pos, ignoreBed) > getTotalHealth(player)) return false;
            }
        }

        return true;
    }

    // Bed Damage

    private double bedDamage(PlayerEntity player, BlockPos pos, boolean ignoreBed) {
        if (player == null || player.getAbilities().creativeMode && !(player instanceof FakePlayerEntity)) return 0;

        Vec3d position = Vec3d.ofCenter(pos);
        if (explosion == null) explosion = new Explosion(mc.world, null, position.x, position.y, position.z, 5.0F, true, Explosion.DestructionType.DESTROY);
        else ((IExplosion) explosion).set(position, 5.0F, true);

        double distance = Math.sqrt(player.squaredDistanceTo(position));
        if (distance > 10) return 0;

        if (raycastContext == null) raycastContext = new RaycastContext(null, null, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);

        double exposure = getExposure(position, player, predictMovement.get(), raycastContext, ignoreTerrain.get(), ignoreBed);
        double impact = (1.0 - (distance / 10.0)) * exposure;
        double damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1;

        // Damage calculation
        damage = getDamageForDifficulty(damage);
        damage = resistanceReduction(player, damage);
        damage = getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    private double getExposure(Vec3d source, Entity entity, boolean predictMovement, RaycastContext context, boolean ignoreTerrain, boolean ignoreBed) {
        Box box = entity.getBoundingBox();

        if (predictMovement && !entity.isOnGround()) {
            Vec3d v = entity.getVelocity();
            box = box.offset(v.x, v.y, v.z);
        }

        double d = 1 / ((box.maxX - box.minX) * 2 + 1);
        double e = 1 / ((box.maxY - box.minY) * 2 + 1);
        double f = 1 / ((box.maxZ - box.minZ) * 2 + 1);
        double g = (1 - Math.floor(1 / d) * d) / 2;
        double h = (1 - Math.floor(1 / f) * f) / 2;

        if (!(d < 0) && !(e < 0) && !(f < 0)) {
            int i = 0;
            int j = 0;

            for (double k = 0; k <= 1; k += d) {
                for (double l = 0; l <= 1; l += e) {
                    for (double m = 0; m <= 1; m += f) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);

                        ((IRaycastContext) context).set(new Vec3d(n + g, o, p + h), source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
                        if (raycast(context, ignoreTerrain, ignoreBed).getType() == HitResult.Type.MISS) i++;

                        j++;
                    }
                }
            }

            return (double) i / j;
        }

        return 0;
    }

    private BlockHitResult raycast(RaycastContext context, boolean ignoreTerrain, boolean ignoreBed) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycast, pos) -> {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock().getBlastResistance() < 600 && ignoreTerrain) state = Blocks.AIR.getDefaultState();
            if (ignoreBed && state.getBlock() instanceof BedBlock) state = Blocks.AIR.getDefaultState();

            Vec3d vec1 = raycast.getStart();
            Vec3d vec2 = raycast.getEnd();

            VoxelShape shape = raycast.getBlockShape(state, mc.world, pos);
            BlockHitResult result1 = mc.world.raycastBlock(vec1, vec2, pos, shape, state);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult result2 = voxelShape2.raycast(vec1, vec2, pos);

            double d = result1 == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(result1.getPos());
            double e = result2 == null ? Double.MAX_VALUE : raycast.getStart().squaredDistanceTo(result2.getPos());

            return d <= e ? result1 : result2;
        }, (raycast) -> {
            Vec3d vec = raycast.getStart().subtract(raycast.getEnd());
            return BlockHitResult.createMissed(raycast.getEnd(), Direction.getFacing(vec.x, vec.y, vec.z), new BlockPos(raycast.getEnd()));
        });
    }

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

    private void place(BlockPos pos, Direction dir, FindItemResult item, boolean swingHand, boolean checkEntities, boolean randomOffset) {
        if (item != null && item.found()) {
            if (item.isOffhand()) {
                place(pos, dir, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, swingHand, checkEntities, randomOffset);
            } else if (item.isHotbar()) {
                place(pos, dir, Hand.MAIN_HAND, item.getSlot(), swingHand, checkEntities, randomOffset);
            }
        }
    }

    private void place(BlockPos pos, Direction dir, Hand hand, int slot, boolean swingHand, boolean checkEntities, boolean offsetRandom) {
        if (slot >= 0 && slot <= 9 || slot == 45) {
            if (canPlace(pos, Blocks.RED_BED.getDefaultState(), checkEntities)) {
                Vec3d hitPos = getHitPos(pos, offsetRandom);
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

    private Vec3d getHitPos(BlockPos pos, boolean offsetRandom) {
        Vec3d hitPos = offsetRandom ? offsetRandom(Vec3d.of(pos)) : Vec3d.ofCenter(pos);
        Direction side = getPlaceSide(pos);

        if (side != null) hitPos.add(side.getOffsetX() * 0.5D, side.getOffsetY() * 0.5D, side.getOffsetZ() * 0.5D);

        return hitPos;
    }

    private Vec3d offsetRandom(Vec3d vec) {
        double px = random.nextDouble(0.9) + 0.5;
        double py = random.nextDouble(0.9) + 0.5;
        double pz = random.nextDouble(0.9) + 0.5;

        return new Vec3d(vec.getX() + px, vec.getY() + py, vec.getZ() + pz);
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
        breakBed(new BlockHitResult(randomOffset.get() ? offsetRandom(Vec3d.of(pos)) : Vec3d.ofCenter(pos), getBestSide(pos), pos, false), Hand.OFF_HAND);
    }

    private void breakBed(BlockHitResult result, Hand hand) {
        if (mc.player.isSneaking()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));

        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    // Constants

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

    public enum SortMode {
        Normal,
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

    public enum SwitchMode {
        Normal,
        Silent,
        None
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

    public enum InstaBreakMode {
        None,
        Normal,
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

    // Render Block

    public class RenderBlock {
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
