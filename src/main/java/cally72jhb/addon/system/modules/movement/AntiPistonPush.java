package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class AntiPistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spoof = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof")
        .description("Spoofs your position after canceling the push event.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait before spoofing again.")
        .defaultValue(1)
        .min(0)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Integer> times = sgGeneral.add(new IntSetting.Builder()
        .name("times")
        .description("How often to spoof.")
        .defaultValue(25)
        .min(1)
        .sliderMin(5)
        .sliderMax(30)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Whether or not to center you.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only spoofs when you are in a hole.")
        .defaultValue(true)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Double> offset = sgGeneral.add(new DoubleSetting.Builder()
        .name("offset")
        .description("How much to offset you in a direction to prevent position updates.")
        .defaultValue(0.5)
        .min(0.3)
        .max(0.7)
        .sliderMin(0.3)
        .sliderMax(0.7)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("Whether or not to always center you.")
        .defaultValue(false)
        .visible(spoof::get)
        .build()
    );

    private final Setting<Boolean> redstone = sgGeneral.add(new BoolSetting.Builder()
        .name("redstone")
        .description("Whether or not to scan for redstone components too.")
        .defaultValue(false)
        .visible(() -> spoof.get() && !strict.get())
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("How far to scan for a piston.")
        .defaultValue(2)
        .min(1)
        .max(5)
        .sliderMin(1)
        .sliderMax(5)
        .visible(() -> spoof.get() && !strict.get())
        .build()
    );

    private int timer;
    private int time;

    public AntiPistonPush() {
        super(VectorAddon.MOVEMENT, "anti-piston-push", "Prevents piston pushing.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        time = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (onlyInHole.get() && !PlayerUtils.isInHole(false)) return;
        if (!strict.get()) {
            ArrayList<BlockPos> positions  = VectorUtils.getPositionsAroundPlayer(range.get());
            positions.removeIf(pos -> !(VectorUtils.getBlock(pos) instanceof PistonBlock) && (!redstone.get() || !(VectorUtils.getBlock(pos) == Blocks.REDSTONE_BLOCK || VectorUtils.getBlock(pos) == Blocks.REDSTONE_TORCH || VectorUtils.getBlock(pos) == Blocks.REDSTONE_WALL_TORCH)));

            if (timer >= delay.get() || !positions.isEmpty()) {
                spoof();
                timer = 0;
            } else {
                timer++;
            }

            if (positions.isEmpty()) time = 0;
        } else {
            if (timer >= delay.get()) {
                spoof();
            } else {
                timer++;
            }
        }
    }

    private void spoof() {
        if (time <= times.get() || strict.get()) {
            if (center.get()) {
                double x = MathHelper.floor(mc.player.getX()) + offset.get();
                double z = MathHelper.floor(mc.player.getZ()) + offset.get();

                mc.player.setPosition(x, mc.player.getY(), z);
            }

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        }

        time++;
    }
}
