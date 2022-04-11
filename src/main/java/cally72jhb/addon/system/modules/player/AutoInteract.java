package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

public class AutoInteract extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> minRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-range")
        .description("The minimum range in which entities can be interacted with.")
        .defaultValue(3)
        .min(0)
        .sliderMin(0)
        .sliderMax(7.5)
        .noSlider()
        .build()
    );

    private final Setting<Double> maxRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-range")
        .description("The maximum range in which entities can be interacted with.")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .sliderMax(7.5)
        .noSlider()
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait in ticks before interacting with a entity again.")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .sliderMax(10)
        .noSlider()
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .build()
    );

    private final Setting<SneakMode> sneakMode = sgGeneral.add(new EnumSetting.Builder<SneakMode>()
        .name("sneak-mode")
        .description("How the sneak packets are send.")
        .defaultValue(SneakMode.Normal)
        .build()
    );

    private final Setting<Boolean> ignorePassengers = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-passengers")
        .description("Ignores your passengers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreOtherPassengers = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-other-passengers")
        .description("Ignores others passengers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("render-swing")
        .description("Renders a hand swing animation.")
        .defaultValue(true)
        .build()
    );

    private int ticks;

    public AutoInteract() {
        super(Categories.Misc, "auto-interact", "Automatically interacts with entities.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (delay.get() == 0 || ticks >= delay.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entities.get().getBoolean(entity.getType())
                    && VectorUtils.distance(mc.player.getPos(), entity.getPos()) >= minRange.get()
                    && VectorUtils.distance(mc.player.getPos(), entity.getPos()) <= maxRange.get()
                    && (!ignorePassengers.get() || ignorePassengers.get() && !mc.player.getPassengerList().contains(entity))) {

                    boolean valid = true;

                    if (ignoreOtherPassengers.get()) {
                        for (Entity entity1 : mc.world.getPlayers()) {
                            if (entity1 != mc.player && entity1.getPassengerList().contains(entity)) {
                                valid = false;
                                break;
                            }
                        }
                    }

                    if (valid) {
                        boolean sneaking = sneakMode.get() == SneakMode.Sneak || (sneakMode.get() != SneakMode.Unsneak && (sneakMode.get() == SneakMode.Normal && mc.player.isSneaking()));

                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(entity, sneaking, Hand.MAIN_HAND));
                        if (renderSwing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }
                }
            }

            ticks = 0;
        } else {
            ticks++;
        }
    }

    // Constants

    public enum SneakMode {
        Sneak,
        Unsneak,
        Normal
    }
}
