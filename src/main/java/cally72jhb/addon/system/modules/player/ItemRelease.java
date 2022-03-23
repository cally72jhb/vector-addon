package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class ItemRelease extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to display trajectories for.")
        .defaultValue(getDefaultItems())
        .filter(this::itemFilter)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait before releasing the item.")
        .defaultValue(10)
        .min(1)
        .sliderMin(1)
        .sliderMax(25)
        .build()
    );

    public ItemRelease() {
        super(Categories.Misc, "item-release", "Releases certain usable items after a set delay.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        ItemStack stack = mc.player.getMainHandStack();

        if (items.get().contains(stack.getItem()) && mc.player.isUsingItem() && mc.player.getItemUseTime() >= delay.get()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(mc.player.getActiveHand()));

            mc.player.clearActiveItem();
        }
    }

    private boolean itemFilter(Item item) {
        UseAction action = item.getUseAction(item.getDefaultStack());

        return action != UseAction.BLOCK && action != UseAction.NONE && action != UseAction.SPYGLASS;
    }

    private List<Item> getDefaultItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : Registry.ITEM) if (itemFilter(item)) items.add(item);

        return items;
    }
}
