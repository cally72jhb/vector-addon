package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.StatusEffectInstanceAccessor;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;

import static net.minecraft.entity.effect.StatusEffects.HASTE;

public class SpeedMineBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> canBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("can-break")
        .description("Only tries to break blocks you can break.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> vanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("vanilla")
        .description("Spoofs the packets the default way.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> simple = sgGeneral.add(new BoolSetting.Builder()
        .name("simple")
        .description("A simple mode to break the blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder()
        .name("packet")
        .description("Packet-spoofs the blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> updateTick = sgGeneral.add(new IntSetting.Builder()
        .name("update-tick")
        .description("The last update tick of the block.")
        .defaultValue(1)
        .min(0)
        .visible(packet::get)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Trys to break the block instantly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> facing = sgGeneral.add(new BoolSetting.Builder()
        .name("facing")
        .description("Whether or not to face at the block.")
        .defaultValue(false)
        .visible(instant::get)
        .build()
    );

    private final Setting<InstantMode> instantMode = sgGeneral.add(new EnumSetting.Builder<InstantMode>()
        .name("mode")
        .description("What mode to use for mining.")
        .defaultValue(InstantMode.AUTOMATIC)
        .visible(instant::get)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The mining speed.")
        .defaultValue(1.5)
        .min(0)
        .build()
    );

    // Bypass

    private final Setting<Boolean> strict = sgBypass.add(new BoolSetting.Builder()
        .name("strict")
        .description("How to handle certain packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> once = sgBypass.add(new BoolSetting.Builder()
        .name("only-once")
        .description("Only mines a block once.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> silent = sgBypass.add(new BoolSetting.Builder()
        .name("silent")
        .description("Mines the block silently.")
        .defaultValue(false)
        .visible(() -> !packet.get())
        .build()
    );

    private final Setting<Boolean> interact = sgBypass.add(new BoolSetting.Builder()
        .name("interact")
        .description("Interacts with the block after a certain time.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> store = sgBypass.add(new BoolSetting.Builder()
        .name("store")
        .description("Stores the blocks and mines them after a certain time again.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> storeDelay = sgBypass.add(new IntSetting.Builder()
        .name("store-delay")
        .description("The delay in ticks to wait before interacting with a block again.")
        .defaultValue(20)
        .min(0)
        .visible(store::get)
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
        .description("Renders a block overlay where the obsidian will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(165, 140, 245, 25))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(165, 140, 245, 255))
        .visible(render::get)
        .build()
    );

    private ArrayList<BlockPos> positions;

    private Direction direction;
    private BlockPos pos;

    private int timer;

    public SpeedMineBypass() {
        super(VectorAddon.MISC, "speed-mine-bypass", "Mine blocks faster.");
    }

    @Override
    public void onActivate() {
        positions = new ArrayList<>();

        timer = 0;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (event.blockPos.equals(pos) || PlayerUtils.getGameMode() == GameMode.CREATIVE) return;

        direction = event.direction;
        pos = event.blockPos;

        if (once.get()) mine(pos);
        if (!packet.get() && silent.get()) {
            mc.interactionManager.cancelBlockBreaking();
            event.cancel();
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (positions.contains(event.pos)) positions.remove(pos);
        if (event.pos.equals(pos)) pos = null;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!packet.get() && silent.get()) mc.interactionManager.cancelBlockBreaking();
        if (pos != null && direction != null) mine(pos);
        if (once.get() || PlayerUtils.getGameMode() == GameMode.CREATIVE) return;

        if (store.get() && !positions.isEmpty()) {
            if (timer >= storeDelay.get() || storeDelay.get() == 0) {
                for (BlockPos position : new ArrayList<>(positions)) {
                    mine(position);
                    positions.remove(position);
                }

                timer = 0;
            } else {
                timer++;
            }
        }
    }

    private void mine(BlockPos pos) {
        if (canBreak.get() && !BlockUtils.canBreak(pos) || PlayerUtils.getGameMode() == GameMode.CREATIVE || BlockUtils.canInstaBreak(pos)) return;

        if (interact.get() && !VectorUtils.isClickable(VectorUtils.getBlock(pos))) {
            FindItemResult item = InvUtils.findInHotbar(stack -> stack.getUseAction() == UseAction.NONE);
            Hand hand = (item.getHand() != null && item.found()) ? item.getHand() : Hand.OFF_HAND;

            mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(Vec3d.ofCenter(pos), direction, pos, false));

            swingHand(hand);
        }

        if (!positions.contains(pos) && pos != null) positions.add(pos);

        if (vanilla.get()) {
            int amplifier = speed.get().intValue();

            if (!mc.player.hasStatusEffect(HASTE)) {
                mc.player.addStatusEffect(new StatusEffectInstance(HASTE, 255, amplifier, false, false, false));
            }

            StatusEffectInstance effect = mc.player.getStatusEffect(HASTE);
            ((StatusEffectInstanceAccessor) effect).setAmplifier(amplifier);
            if (effect.getDuration() < 20) ((StatusEffectInstanceAccessor) effect).setDuration(20);
        }

        if (simple.get()) mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));

        if (packet.get()) {
            if (!VectorUtils.getBlockState(pos).isAir() && BlockUtils.canBreak(pos)) {
                for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
                    if (value.getPos().equals(pos)) {
                        value.setStage(value.getStage() + speed.get().intValue());
                        value.setLastUpdateTick(updateTick.get());
                    }
                }
            }
        }

        if (instant.get()) {
            swingHand(Hand.MAIN_HAND);

            if (BlockUtils.canBreak(pos)) {
                if (instantMode.get() == InstantMode.MANUAL) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                } else if (instantMode.get() == InstantMode.NONE) {
                    swingHand(Hand.MAIN_HAND);
                } else {
                    if (strict.get()) {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                        if (facing.get())
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, direction.getOpposite()));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                    } else {
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                        if (facing.get())
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, direction.getOpposite()));
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                    }
                }
            }
        }

        swingHand(Hand.MAIN_HAND);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!positions.isEmpty()) {
            for (BlockPos pos : new ArrayList<>(positions)) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    private void swingHand(Hand hand) {
        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    public enum InstantMode {
        AUTOMATIC, MANUAL, NONE
    }
}
