package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.mixin.RecipeResultCollectionAccessor;
import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.FindItemResult;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchableContainer;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.ArrayList;
import java.util.List;

public class AutoCraft extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgBeds = settings.createGroup("Beds");
    private final SettingGroup sgRender = settings.createGroup("Render");


    // General


    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items you want to be crafted.")
        .defaultValue(new ArrayList<>() {{
            add(Items.BLACK_BED);
            add(Items.BLUE_BED);
            add(Items.BROWN_BED);
            add(Items.CYAN_BED);
            add(Items.GRAY_BED);
            add(Items.GREEN_BED);
            add(Items.LIGHT_BLUE_BED);
            add(Items.LIGHT_GRAY_BED);
            add(Items.LIME_BED);
            add(Items.MAGENTA_BED);
            add(Items.ORANGE_BED);
            add(Items.PINK_BED);
            add(Items.PURPLE_BED);
            add(Items.RED_BED);
            add(Items.WHITE_BED);
            add(Items.YELLOW_BED);
        }})
        .filter(this::itemFilter)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the blocks being placed and interacted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> craftAll = sgGeneral.add(new BoolSetting.Builder()
        .name("craft-all")
        .description("Will craft as much of the item as possible.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> drop = sgGeneral.add(new BoolSetting.Builder()
        .name("drop")
        .description("Drops the items after crafting them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> craftPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("place-table")
        .description("Automatically places a crafting table near you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> openRecipeBook = sgGeneral.add(new BoolSetting.Builder()
        .name("open-recipe-book")
        .description("Automatically opens the recipe book to bypass some anti cheats.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoOpen = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-open")
        .description("Automatically opens the crafting table.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> craftRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("table-range")
        .description("The radius around you in which the crafting table / opened will be placed.")
        .defaultValue(2.5)
        .min(1)
        .sliderMin(2)
        .sliderMax(5)
        .visible(() -> craftPlace.get() || autoOpen.get())
        .build()
    );

    private final Setting<Boolean> craftOnlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only crafts when your on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> craftOnlyInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only crafts when your in a safe hole.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> silentCraft = sgGeneral.add(new BoolSetting.Builder()
        .name("silent-craft")
        .description("Hides the crafting inventory screen client-side.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> antiDesync = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-desync")
        .description("Updates your inventory after your done with crafting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> craftDelay = sgGeneral.add(new IntSetting.Builder()
        .name("craft-delay")
        .description("How many ticks to wait before crafting again.")
        .defaultValue(1)
        .min(0)
        .sliderMin(0)
        .sliderMax(10)
        .noSlider()
        .build()
    );

    private final Setting<Integer> minCraftHealth = sgGeneral.add(new IntSetting.Builder()
        .name("min-craft-health")
        .description("Min health required to be able to craft.")
        .defaultValue(5)
        .min(0)
        .max(36)
        .sliderMin(0)
        .sliderMax(36)
        .noSlider()
        .build()
    );

    private final Setting<Integer> craftAmount = sgGeneral.add(new IntSetting.Builder()
        .name("craft-count")
        .description("How many items should be crafted at once.")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> maxItems = sgGeneral.add(new IntSetting.Builder()
        .name("max-items")
        .description("How many different items are allowed to be  crafted at once.")
        .defaultValue(2)
        .min(1)
        .sliderMin(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Integer> minEmptySlots = sgGeneral.add(new IntSetting.Builder()
        .name("min-empty-slots")
        .description("How many slots in your inventory have to be empty to be able craft.")
        .defaultValue(1)
        .min(1)
        .sliderMin(1)
        .sliderMax(10)
        .build()
    );


    // Inventory


    private final Setting<ClickMode> clickMode = sgInventory.add(new EnumSetting.Builder<ClickMode>()
        .name("click-mode")
        .description("How slots in your inventory are clicked.")
        .defaultValue(ClickMode.Packet)
        .build()
    );

    private final Setting<SwitchMode> switchMode = sgInventory.add(new EnumSetting.Builder<SwitchMode>()
        .name("auto-switch-mode")
        .description("How to swap to the slots where the beds are.")
        .defaultValue(SwitchMode.Normal)
        .build()
    );

    private final Setting<Boolean> swapBack = sgInventory.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the selected slot after you placed a block.")
        .defaultValue(true)
        .build()
    );


    // Beds


    private final Setting<Boolean> smartTablePlace = sgBeds.add(new BoolSetting.Builder()
        .name("smart-table-place")
        .description("Automatically opens the crafting table.")
        .defaultValue(true)
        .visible(craftPlace::get)
        .build()
    );


    // Render


    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand swing animation.")
        .defaultValue(true)
        .build()
    );

    private SearchableContainer<RecipeResultCollection> container;
    private List<Recipe<?>> recipes;

    private CraftingScreenHandler silentHandler;

    private Explosion explosion;

    private int craftTicks;

    public AutoCraft() {
        super(Categories.Misc, "auto-craft", "Automatically crafts items for you.");
    }

    @Override
    public void onActivate() {
        craftTicks = 0;
        silentHandler = null;

        recipes = new ArrayList<>();

        if (mc.world != null) {
            explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
            if (mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (mc.world != null) {
            explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
            if (mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);
        }
    }

    private void onPreTick(TickEvent.Pre event) {
        if (craftTicks >= craftDelay.get() || craftDelay.get() == 0) {
            if (autoOpen.get() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler || silentHandler != null && silentCraft.get())) {
                if (canRefill() && !isInventoryFull()) {
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

                            Rotations.rotate(Rotations.getYaw(Vec3d.ofCenter(pos)), Rotations.getPitch(Vec3d.ofCenter(pos)));

                            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
                            swingHand(Hand.MAIN_HAND);
                        } else if (!placePositions.isEmpty() && craftPlace.get()) {
                            List<BlockPos> insecure = smartTablePlace.get() ? getAffectedBlocks(mc.player.getBlockPos()) : new ArrayList<>();

                            for (BlockPos position : placePositions) {
                                if (canPlace(position, Blocks.CRAFTING_TABLE.getDefaultState(), true)
                                    && (!smartTablePlace.get() || smartTablePlace.get() && !insecure.contains(position))
                                    && (switchMode.get() != SwitchMode.None || (switchMode.get() == SwitchMode.None
                                    && mc.player.getOffHandStack().getItem() == Items.CRAFTING_TABLE
                                    && mc.player.getMainHandStack().getItem() == Items.CRAFTING_TABLE))) {
                                    VectorUtils.place(position, table, rotate.get(), 100, renderSwing.get(), true, swapBack.get());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (mc.player.currentScreenHandler instanceof CraftingScreenHandler || silentHandler != null && silentCraft.get()) {
                CraftingScreenHandler handler = silentHandler != null && silentCraft.get() ? silentHandler : (CraftingScreenHandler) mc.player.currentScreenHandler;

                if (!canRefill() || isInventoryFull()) {
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
                    mc.player.currentScreenHandler = mc.player.playerScreenHandler;
                    mc.setScreen(null);

                    if (antiDesync.get()) mc.player.getInventory().updateItems();

                    silentHandler = null;
                } else {
                    if (openRecipeBook.get() && mc.player.getRecipeBook() != null && !mc.player.getRecipeBook().isGuiOpen(RecipeBookCategory.CRAFTING)) {
                        mc.player.getRecipeBook().setGuiOpen(RecipeBookCategory.CRAFTING, true);
                    }

                    if (container == null || recipes.isEmpty()) reload();

                    if (!recipes.isEmpty() && (getEmptySlots(0, 35) > 0 || drop.get())) {
                        int item = 0;

                        for (Recipe<?> recipe : recipes) {
                            if (items.get().contains(recipe.getOutput().getItem()) && canCraft(recipe)) {
                                if (!craftAll.get()) {
                                    for (int i = 0; i < getEmptySlots(0, 35) && i <= craftAmount.get(); i++)
                                        clickRecipe(handler, recipe, false);
                                } else {
                                    clickRecipe(handler, recipe, true);
                                }

                                clickSlot(handler.syncId, 0, 1, drop.get() ? SlotActionType.THROW : SlotActionType.QUICK_MOVE);

                                item++;
                                craftTicks = 0;
                                if (item >= maxItems.get() || getEmptySlots(0, 35) == 0 && !drop.get()) return;
                            }
                        }
                    }
                }
            }
        } else {
            craftTicks++;
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

    // Inventory Utils

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.isEmpty() || stack.getCount() == 0 || stack.getItem() instanceof AirBlockItem;
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
        return getEmptySlots(0, 35) >= minEmptySlots.get()
            && (!craftOnlyOnGround.get() || craftOnlyOnGround.get() && mc.player.isOnGround())
            && (!craftOnlyInHole.get() || craftOnlyInHole.get() && isSurrounded())
            && (mc.player.getHealth() + mc.player.getAbsorptionAmount()) >= minCraftHealth.get();
    }

    private boolean canCraft(Recipe<?> recipe) {
        int checked = 0;


        for (int i = 0; i < recipe.getIngredients().size() - 1; i++) {
            Ingredient ingredient = recipe.getIngredients().get(i);

            for (int a = 0; a <= 35; a++) {
                for (ItemStack stack : ingredient.getMatchingStacks()) {
                    if (ingredient.test(mc.player.getInventory().getStack(i)) && mc.player.getInventory().getStack(i).getCount() >= stack.getCount()) checked++;
                }
            }
        }

        return checked >= recipe.getIngredients().size() - 1;
    }

    private void clickRecipe(CraftingScreenHandler handler, Recipe<?> recipe, boolean craftAll) {
        if (mc.interactionManager != null && clickMode.get() == ClickMode.Normal) {
            mc.interactionManager.clickRecipe(handler.syncId, recipe, craftAll);
        } else {
            mc.getNetworkHandler().sendPacket(new CraftRequestC2SPacket(handler.syncId, recipe, craftAll));
        }
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

    // Explosion Utils

    private List<BlockPos> getAffectedBlocks(BlockPos pos) {
        ((IExplosion) explosion).set(Vec3d.ofCenter(pos), 5.0F, true);

        return explosion.getAffectedBlocks();
    }

    // Other Utils

    private void reload() {
        if (mc != null && mc.world != null && mc.player != null) {
            if (recipes == null || recipes.isEmpty()) recipes = new ArrayList<>();
            if (mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);

            if (container != null) {
                recipes = new ArrayList<>();

                for (RecipeResultCollection collection : container.findAll("")) {
                    recipes.addAll(((RecipeResultCollectionAccessor) collection).getRecipes());
                }
            }

            explosion = new Explosion(mc.world, null, 0, 0, 0, 5.0F, true, Explosion.DestructionType.DESTROY);
        }
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

    private void swingHand(Hand hand) {
        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    private boolean itemFilter(Item item) {
        if (container == null && mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT) != null) container = mc.getSearchableContainer(SearchManager.RECIPE_OUTPUT);

        if (container != null) {
            for (RecipeResultCollection collection : container.findAll("")) {
                for (Recipe<?> recipe : ((RecipeResultCollectionAccessor) collection).getRecipes()) {
                    if (recipe.getOutput().getItem() == item) return true;
                }
            }
        }

        return true;
    }

    // Constants

    public enum ClickMode {
        Normal,
        Packet
    }

    public enum SwitchMode {
        Normal,
        Silent,
        None
    }
}
