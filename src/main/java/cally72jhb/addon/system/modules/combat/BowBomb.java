package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class BowBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to spoof.")
            .defaultValue(getDefaultItems())
            .filter(this::itemFilter)
            .build()
    );

    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder()
            .name("timeout")
            .description("How many ticks to wait to detect a timeout.")
            .defaultValue(20)
            .min(0)
            .sliderMin(0)
            .sliderMax(0)
            .build()
    );

    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder()
            .name("spoofs")
            .description("How often to spoof.")
            .defaultValue(5)
            .min(1)
            .sliderMin(0)
            .sliderMax(0)
            .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
            .name("bypass")
            .description("Whether or not to bypass.")
            .defaultValue(false)
            .build()
    );

    private boolean shooting;
    private long lastShootTime;

    public BowBomb() {
        super(VectorAddon.CATEGORY, "bow-bomb", "");
    }

    @Override
    public void onActivate() {
        shooting = false;
        lastShootTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;

            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                ItemStack stack = mc.player.getMainHandStack();

                if (!stack.isEmpty() && stack.getItem() != null && stack.getItem() instanceof BowItem && items.get().contains(stack.getItem())) {
                    doSpoofs();
                }
            }

        } else if (event.packet instanceof PlayerInteractItemC2SPacket) {
            PlayerInteractItemC2SPacket packet2 = (PlayerInteractItemC2SPacket) event.packet;

            if (packet2.getHand() == Hand.MAIN_HAND) {
                ItemStack stack = mc.player.getMainHandStack();

                if (!stack.isEmpty() && stack.getItem() != null && items.get().contains(stack.getItem())) {
                    doSpoofs();
                }
            }
        }
    }

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            shooting = true;
            lastShootTime = System.currentTimeMillis();

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int i = 0; i < spoofs.get(); ++i) {
                if (bypass.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }
            }

            shooting = false;
        }
    }

    private boolean itemFilter(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof FishingRodItem || item instanceof TridentItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem;
    }

    private List<Item> getDefaultItems() {
        List<Item> items = new ArrayList<>();

        for (Item item : Registry.ITEM) {
            if (itemFilter(item)) items.add(item);
        }

        return items;
    }
}
