package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class DeathAnimations extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEffect = settings.createGroup("Effect");


    // General


    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Won't summon particles when you die.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Won't summon particles when friends die.")
        .defaultValue(true)
        .build()
    );


    // Effect


    private final Setting<Boolean> lightning = sgEffect.add(new BoolSetting.Builder()
        .name("summon-lightning")
        .description("Summons a lightning on a players death.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> silentLightning = sgEffect.add(new BoolSetting.Builder()
        .name("silent-lightning")
        .description("Makes the lightning bold silent.")
        .defaultValue(false)
        .visible(lightning::get)
        .build()
    );

    private final Setting<Integer> lightningAmount = sgEffect.add(new IntSetting.Builder()
        .name("lightning-amount")
        .description("How many lightning bolts to spawn.")
        .defaultValue(1)
        .sliderMin(1)
        .sliderMax(5)
        .min(1)
        .visible(lightning::get)
        .noSlider()
        .build()
    );

    public DeathAnimations() {
        super(Categories.Misc, "death-animations", "Creates an animation when a player dies.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() != 35 && packet.getStatus() != 3) return;

            Entity entity = packet.getEntity(mc.world);

            if (entity instanceof PlayerEntity
                && (!ignoreSelf.get() || ignoreSelf.get() && entity != mc.player)
                && (!ignoreFriends.get() || ignoreFriends.get() && Friends.get() != null && !Friends.get().isFriend((PlayerEntity) entity))) {

                if (lightning.get()) {
                    for (int i = 0; i < lightningAmount.get(); i++) {
                        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                        lightning.setPos(entity.getX(), entity.getY(), entity.getZ());
                        lightning.setSilent(silentLightning.get());

                        mc.world.spawnEntity(lightning);
                    }
                }
            }
        }
    }
}
