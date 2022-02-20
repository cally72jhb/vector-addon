package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.movement.ReverseStep;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class BowBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> base = sgGeneral.add(new DoubleSetting.Builder()
        .name("hide-base")
        .description("Base for the exponent number for rubberband.")
        .defaultValue(10)
        .min(1)
        .sliderMin(5)
        .sliderMax(15)
        .build());

    private final Setting<Double> exponent = sgGeneral.add(new DoubleSetting.Builder()
        .name("hide-exponent")
        .description("The exponent for the base number to rubberband.")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> pause = sgGeneral.add(new BoolSetting.Builder()
        .name("pause")
        .description("Pauses modules that could interfere the spoofing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to disable while blinking.")
        .defaultValue(new ArrayList<>() {{
            add(Modules.get().get(AntiHunger.class));
            add(Modules.get().get(NoFall.class));
            add(Modules.get().get(ReverseStep.class));
        }})
        .visible(pause::get)
        .build()
    );

    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder()
        .name("timeout")
        .defaultValue(5000)
        .min(10)
        .sliderMin(2500)
        .sliderMax(10000)
        .build()
    );

    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder()
        .name("spoofs")
        .defaultValue(10)
        .min(1)
        .sliderMin(1)
        .sliderMax(300)
        .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Makes you sprint while spoofing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Bypasses on some servers.")
        .defaultValue(false)
        .build()
    );

    public BowBomb() {
        super(VectorAddon.Combat, "bow-bomb", "Kills / Pops entities 1 shot with the bow.");
    }

    private long lastShoot;
    private boolean spoofed;

    private ArrayList<Module> toActivate;

    @Override
    public void onActivate() {
        toActivate = new ArrayList<>();

        lastShoot = System.currentTimeMillis();
    }

    @Override
    public void onDeactivate() {
        activate();
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (isSpoofable()) {
            if (mc.player.getItemUseTime() > 0 && mc.options.keyUse.isPressed()) activate();
            else if (pause.get()) deactivate();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (spoofed && !mc.options.keyUse.isPressed() && pause.get()) activate();
        else if (pause.get()) deactivate();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet == null) return;

        if (event.packet instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;

            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                if (isSpoofable()) {
                    deactivate();
                    spoofed = false;

                    if (System.currentTimeMillis() - lastShoot >= timeout.get()) {
                        lastShoot = System.currentTimeMillis();

                        float value = (float) Math.pow(base.get(), -exponent.get());

                        if (sprint.get()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

                        for (int index = 0; index < spoofs.get(); ++index) {
                            if (bypass.get()) {
                                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + value, mc.player.getZ(), false));
                                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - value, mc.player.getZ(), true));
                            } else {
                                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - value, mc.player.getZ(), true));
                                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + value, mc.player.getZ(), false));
                            }
                        }

                        spoofed = true;
                    }
                }
            }
        }
    }

    private void activate() {
        if (!toActivate.isEmpty()) {
            for (Module module : toActivate) {
                if (!module.isActive()) {
                    module.toggle();
                }
            }
        }
    }

    private void deactivate() {
        if (!modules.get().isEmpty()) {
            for (Module module : modules.get()) {
                if (module.isActive()) {
                    module.toggle();
                    toActivate.add(module);
                }
            }
        }
    }

    private boolean isSpoofable() {
        Item mainHand = mc.player.getMainHandStack().getItem();
        Item offHand = mc.player.getOffHandStack().getItem();

        return mainHand instanceof TridentItem || offHand instanceof TridentItem
            || mainHand instanceof BowItem || offHand instanceof BowItem
            || mainHand instanceof ThrowablePotionItem || offHand instanceof  ThrowablePotionItem
            || mainHand instanceof EnderPearlItem || offHand instanceof EnderPearlItem
            || mainHand instanceof SnowballItem || offHand instanceof SnowballItem;
    }
}
