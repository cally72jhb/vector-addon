package cally72jhb.addon.modules.movement;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;

public class Flight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> noUnloadedChunks = sgGeneral.add(new BoolSetting.Builder()
            .name("no-unloaded-chunks")
            .description("Stops you from flying into unloaded chunks.")
            .defaultValue(true)
            .build()
    );

    // General
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");
    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Horizontal speed in blocks per second.")
            .defaultValue(1)
            .min(0)
            .sliderMin(0.15)
            .sliderMax(1.75)
            .build()
    );
    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("Vertical speed in blocks per second.")
            .defaultValue(0.75)
            .min(0)
            .sliderMin(0.25)
            .sliderMax(2.5)
            .build()
    );
    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-speed")
            .description("How fast you fall in blocks per second.")
            .defaultValue(0.1)
            .min(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );
    private final Setting<Boolean> stopOnDisable = sgAntiKick.add(new BoolSetting.Builder()
            .name("stop-on-disable")
            .description("Resets your velocity once you disable the module.")
            .defaultValue(true)
            .build()
    );
    private final Setting<NoFallMode> noFallMode = sgGeneral.add(new EnumSetting.Builder<NoFallMode>()
            .name("no-fall-mode")
            .description("Prevents you from getting fall damage.")
            .defaultValue(NoFallMode.Bypass)
            .build()
    );
    private final Setting<Double> bypassHeight = sgGeneral.add(new DoubleSetting.Builder()
            .name("bypass-height")
            .description("How high to teleport you before disabling the module.")
            .defaultValue(1)
            .sliderMin(0.1)
            .sliderMax(5)
            .visible(() -> noFallMode.get() == NoFallMode.Bypass || noFallMode.get() == NoFallMode.Extra)
            .build()
    );

    // Anti Kick
    private final Setting<Boolean> antiKick = sgAntiKick.add(new BoolSetting.Builder()
            .name("anti-kick")
            .description("Prevents you from being kicked by the server.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onGround = sgAntiKick.add(new BoolSetting.Builder()
            .name("on-ground")
            .description("Tells the server you're on ground when sending the anti-kick packets.")
            .defaultValue(true)
            .visible(antiKick::get)
            .build()
    );

    private final Setting<Double> antiKickDistance = sgAntiKick.add(new DoubleSetting.Builder()
            .name("anti-kick-distance")
            .description("How fast you fall in blocks per second.")
            .defaultValue(0.1)
            .min(0)
            .visible(antiKick::get)
            .build()
    );

    private final Setting<Integer> antiKickDelay = sgAntiKick.add(new IntSetting.Builder()
            .name("anti-kick-delay")
            .description("How fast you fall in blocks per second.")
            .defaultValue(20)
            .min(0)
            .noSlider()
            .visible(antiKick::get)
            .build()
    );

    // Variables

    private int timer;

    // Constructor

    public Flight() {
        super(VectorAddon.CATEGORY, "vector-flight", "Allows you to fly in survival.");
    }

    // Overrides

    @Override
    public void onActivate() {
        timer = 0;
    }

    @Override
    public void onDeactivate() {
        switch (noFallMode.get()) {
            case Bypass -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(),
                        mc.player.getY() + bypassHeight.get(),
                        mc.player.getZ(),
                        onGround.get()
                ));
            }

            case Extra -> {
                BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos().add(0, 0.05, 0), mc.player.getPos().add(0, bypassHeight.get(), 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(),
                        result.getPos().y - mc.player.getBoundingBox().maxY,
                        mc.player.getZ(),
                        onGround.get()
                ));
            }
        }

        if (stopOnDisable.get()) {
            mc.player.setVelocity(0, 0, 0);
        }
    }

    // Post Tick Event

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (antiKick.get()) {
            timer = 0;

            if (timer >= antiKickDelay.get()) {
                BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getPos().add(0, 0.05, 0), mc.player.getPos().add(0, -antiKickDistance.get(), 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(),
                        result.getPos().y,
                        mc.player.getZ(),
                        onGround.get()
                ));
            } else {
                timer++;
            }
        }

        Vec3d velocity = getHorizontalVelocity(horizontalSpeed.get());

        double velocityX = velocity.getX();
        double velocityZ = velocity.getZ();

        if (noUnloadedChunks.get()) {
            int chunkX = (int) (mc.player.getX() / 16);
            int chunkZ = (int) (mc.player.getZ() / 16);
            int newChunkX = (int) ((mc.player.getX() + velocityX) / 16);
            int newChunkZ = (int) ((mc.player.getZ() + velocityZ) / 16);

            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, newChunkZ)) {
                velocityZ = 0;
            }

            if (!mc.world.getChunkManager().isChunkLoaded(newChunkX, chunkZ)) {
                velocityX = 0;
            }
        }

        mc.player.setVelocity(
                velocityX,
                mc.player.input.jumping ? verticalSpeed.get() : mc.player.input.sneaking ? -verticalSpeed.get() : -fallSpeed.get(),
                velocityZ
        );
    }

    // Send Packet Event

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && ((IPlayerMoveC2SPacket) event.packet).getTag() != 1337
                && !mc.player.getAbilities().creativeMode && noFallMode.get() != NoFallMode.None
                && !mc.player.isFallFlying() && mc.player.getVelocity().getY() < 0.1) {
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
        }
    }

    // Fluid Collision

    @EventHandler
    private void onFluidCollisionShape(CollisionShapeEvent event) {
        if (event.type == CollisionShapeEvent.CollisionType.FLUID) {
            event.shape = VoxelShapes.empty();
        }
    }

    // Utils

    private Vec3d getHorizontalVelocity(double speed) {
        float yaw = mc.player.getYaw();
        double diagonal = 1 / Math.sqrt(2);

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);

        double dx = 0;
        double dz = 0;

        boolean straight = false;
        boolean sideways = false;

        // Straight Movement

        if (mc.player.input.pressingForward) {
            dx += forward.x / 20 * speed;
            dz += forward.z / 20 * speed;
            straight = true;
        }

        if (mc.player.input.pressingBack) {
            dx -= forward.x / 20 * speed;
            dz -= forward.z / 20 * speed;
            straight = true;
        }

        // Sideways Movement

        if (mc.player.input.pressingRight) {
            dx += right.x / 20 * speed;
            dz += right.z / 20 * speed;
            sideways = true;
        }

        if (mc.player.input.pressingLeft) {
            dx -= right.x / 20 * speed;
            dz -= right.z / 20 * speed;
            sideways = true;
        }

        // Diagonal Calculations

        if (straight && sideways) {
            dx *= diagonal;
            dz *= diagonal;
        }

        return new Vec3d(dx, 0, dz);
    }

    // Enums

    public enum NoFallMode {
        None,
        Normal,
        Bypass,
        Extra
    }
}
