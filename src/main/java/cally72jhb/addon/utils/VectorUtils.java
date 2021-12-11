package cally72jhb.addon.utils;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.config.VectorConfig;
import cally72jhb.addon.utils.login.Login;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class VectorUtils {
    public static int CPS = 0;
    public static MinecraftClient mc;

    public static void init() {
        mc = MinecraftClient.getInstance();

        new Timer().scheduleAtFixedRate(newTimerTaskFromLambda(() -> CPS = 0), 0, 1000);
    }

    public static TimerTask newTimerTaskFromLambda(Runnable runnable) {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                runnable.run();
            }
        };
    }

    public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;
        return dX * dX + dY * dY + dZ * dZ;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(double y1, double y2) {
        double dY = y2 - y1;
        return Math.sqrt(dY * dY);
    }

    public static double distance(Vec3d vec1, Vec3d vec2) {
        double dX = vec2.x - vec1.x;
        double dY = vec2.y - vec1.y;
        double dZ = vec2.z - vec1.z;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(BlockPos block1, BlockPos block2) {
        double dX = block2.getX() - block1.getX();
        double dY = block2.getY() - block1.getY();
        double dZ = block2.getZ() - block1.getZ();
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distanceBetweenY(BlockPos pos1, BlockPos pos2) {
        return MathHelper.sqrt((float) (pos1.getY() + pos2.getY()));
    }

    public static double distanceBetweenXZ(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + f * f));
    }

    public static String getPos() {
        if (mc.player != null) return mc.player.getBlockPos().getX() + " " + mc.player.getBlockPos().getY() + " " + mc.player.getBlockPos().getZ();
        else return "0 0 0";
    }

    // Blocks

    public static Block getBlock(BlockPos pos) {
        if (pos == null) return null;
        return mc.world.getBlockState(pos).getBlock();
    }

    public static BlockState getBlockState(BlockPos pos) {
        if (pos == null) return null;
        return mc.world.getBlockState(pos);
    }

    public static VoxelShape getCollision(BlockPos pos) {
        return getBlockState(pos).getCollisionShape(mc.world, pos);
    }

    public static boolean isSolid(BlockPos pos) {
        return getBlockState(pos).isSolidBlock(mc.world, pos);
    }

    public static boolean canBreak(BlockPos blockPos, BlockState state) {
        if (!mc.player.isCreative() && state.getHardness(mc.world, blockPos) < 0) return false;
        return state.getOutlineShape(mc.world, blockPos) != VoxelShapes.empty();
    }

    public static boolean canBreak(BlockPos blockPos) {
        return canBreak(blockPos, mc.world.getBlockState(blockPos));
    }

    public static boolean isSurroundBlock(BlockPos blockPos) {
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.RESPAWN_ANCHOR || block == Blocks.ANVIL;
    }

    public static boolean isSurroundBlock(Block block) {
        return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.RESPAWN_ANCHOR || block == Blocks.ANVIL;
    }

    public static boolean canPlace(BlockPos pos, boolean checkEntities) {
        if (pos == null) return false;
        if (mc.world == null) return false;

        // Check y level
        if (!World.isValid(pos)) return false;

        // Check if current block is replaceable
        if (!getBlockState(pos).getMaterial().isReplaceable()) return false;

        // Check for entities or if the block is air
        return checkEntities ? mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent()) : getBlockState(pos).getMaterial().isReplaceable();
    }

    public static boolean canPlace(BlockPos pos) {
        return canPlace(pos, true);
    }

    public static double[] directionSpeed(float speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (side < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            side = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }

        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = forward * speed * cos + side * speed * sin;
        final double posZ = forward * speed * sin - side * speed * cos;

        return new double[] {posX, posZ};
    }

    public static void changeIcon() {
        if (VectorConfig.get().windowIcon) {
            try {
                InputStream stream16 = VectorAddon.class.getClassLoader().getResourceAsStream("assets/vector/icon16.png");
                InputStream stream32 = VectorAddon.class.getClassLoader().getResourceAsStream("assets/vector/icon32.png");

                if (stream16 != null && stream32 != null) mc.getWindow().setIcon(stream16, stream32);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static List<String> members = new ArrayList<>();

    public static void members() {
        Thread thread = new Thread(() -> {
            try {
                members = Login.getUserNames();
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        thread.start();
    }
}
