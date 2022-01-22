package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.PistonBlock;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class AntiPistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only spoofs your position if your in a hole.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you when a piston appears near you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> scanRange = sgGeneral.add(new IntSetting.Builder()
        .name("scan-range")
        .description("How far to scan for pistons.")
        .defaultValue(3)
        .min(1)
        .sliderMin(2)
        .sliderMax(5)
        .build()
    );

    private final ArrayList<BlockPos> surround = new ArrayList<>() {{
        add(new BlockPos(1, 0, 0));
        add(new BlockPos(-1, 0, 0));
        add(new BlockPos(0, 0, 1));
        add(new BlockPos(0, 0, -1));
    }};

    public AntiPistonPush() {
        super(VectorAddon.MOVEMENT, "anti-piston-push", "Prevents you from being pushed by a piston.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (center.get() && isInHole()) {
            ArrayList<BlockPos> positions;

            positions = VectorUtils.getPositionsAroundPlayer(scanRange.get());
            positions.removeIf(pos -> !(VectorUtils.getBlock(pos) instanceof PistonBlock));

            if (!positions.isEmpty()) {
                double x = MathHelper.floor(mc.player.getX()) + 0.5;
                double z = MathHelper.floor(mc.player.getZ()) + 0.5;

                mc.player.setPosition(x, mc.player.getY(), z);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
            }
        }
    }

    private boolean isInHole() {
        if (!onlyInHole.get()) return true;

        int i = 0;
        for (BlockPos pos : surround) {
            Block block = VectorUtils.getBlock(mc.player.getBlockPos().add(pos));
            if (block != null && block.getBlastResistance() >= 1200.0F) i++;
        }

        return i == 4;
    }
}
