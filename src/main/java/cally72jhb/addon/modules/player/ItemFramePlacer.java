package cally72jhb.addon.modules.player;

import cally72jhb.addon.utils.Utils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class ItemFramePlacer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> interactRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("interact-range")
            .description("How far to interact with item-frames.")
            .defaultValue(4.5)
            .sliderMin(3.5)
            .sliderMax(5)
            .min(0)
            .max(10)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("How long to wait before interacting with an item-frame again.")
            .defaultValue(1)
            .sliderMin(0)
            .sliderMax(5)
            .min(0)
            .build()
    );

    private final Setting<Boolean> sneakPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("sneak-place")
            .description("Interacts with the item-frame while sneaking before putting an item inside.")
            .defaultValue(true)
            .build()
    );

    // Variables

    private List<Integer> entities;
    private int timer;

    // Constructor

    public ItemFramePlacer() {
        super(Categories.Player, "item-frame-placer", "Places items into item-frames.");
    }

    // Overrides

    @Override
    public void onActivate() {
        this.entities = new ArrayList<>();
        this.timer = 0;
    }

    // Pre Tick Event

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity frame && Utils.distance(mc.player.getPos(), entity.getPos()) < this.interactRange.get() && !this.entities.contains(frame.getId())) {
                if (frame.getHeldItemStack() == null || frame.getHeldItemStack() == ItemStack.EMPTY) {
                    if (this.sneakPlace.get()) mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(frame, true, Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(frame, false, Hand.MAIN_HAND));

                    entities.add(frame.getId());
                    timer = this.delay.get();
                }
            }

            if (this.timer > 0) {
                break;
            }
        }

        if (this.timer > 0) {
            this.timer--;
        }
    }
}
