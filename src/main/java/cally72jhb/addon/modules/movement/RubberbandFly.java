package cally72jhb.addon.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static cally72jhb.addon.utils.Utils.directionSpeed;
import static cally72jhb.addon.utils.Utils.isPlayerMoving;

public class RubberbandFly extends Module {
    private final SettingGroup sgBounds = settings.createGroup("Bounds");
    private final SettingGroup sgHorizontal = settings.createGroup("Horizontal");
    private final SettingGroup sgVertical = settings.createGroup("Vertical");
    private final SettingGroup sgOther = settings.createGroup("Other");
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");


    // Bounds


    private final Setting<BoundsType> boundsType = sgBounds.add(new EnumSetting.Builder<BoundsType>()
            .name("bounds-type")
            .description("How to rubberband you.")
            .defaultValue(BoundsType.Normal)
            .build()
    );

    private final Setting<BoundsMode> boundsMode = sgBounds.add(new EnumSetting.Builder<BoundsMode>()
            .name("bounds-mode")
            .description("What positions to spoof.")
            .defaultValue(BoundsMode.Both)
            .visible(() -> boundsType.get() == BoundsType.Small || boundsType.get() == BoundsType.Infinitive)
            .build()
    );

    private final Setting<CustomMode> customMode = sgBounds.add(new EnumSetting.Builder<CustomMode>()
            .name("bounds-mode")
            .description("How the positions are merged with your position.")
            .defaultValue(CustomMode.SmartRelative)
            .visible(() -> boundsType.get() == BoundsType.Custom || boundsType.get() == BoundsType.Random)
            .build()
    );

    private final Setting<Double> ceilRadius = sgBounds.add(new DoubleSetting.Builder()
            .name("ceil-radius")
            .description("The ceil's radius.")
            .defaultValue(25)
            .sliderMin(0.1)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Ceil)
            .build()
    );

    private final Setting<Double> bypassHeight = sgBounds.add(new DoubleSetting.Builder()
            .name("bypass-height")
            .description("How high to rubberband you.")
            .defaultValue(69420)
            .sliderMin(5)
            .sliderMax(70000)
            .visible(() -> boundsType.get() == BoundsType.Bypass || boundsType.get() == BoundsType.Alternative || boundsType.get() == BoundsType.Up || boundsType.get() == BoundsType.Down)
            .build()
    );

    private final Setting<Integer> digits = sgBounds.add(new IntSetting.Builder()
            .name("digits")
            .description("The number digits of the small bounds.")
            .defaultValue(7)
            .sliderMin(5)
            .sliderMax(9)
            .visible(() -> boundsType.get() == BoundsType.Small)
            .build()
    );

    private final Setting<Double> customX = sgBounds.add(new DoubleSetting.Builder()
            .name("custom-x")
            .description("The custom x factor.")
            .defaultValue(100)
            .sliderMin(-250)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Custom)
            .build()
    );

    private final Setting<Double> customY = sgBounds.add(new DoubleSetting.Builder()
            .name("custom-y")
            .description("The custom y factor.")
            .defaultValue(100)
            .sliderMin(-250)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Custom)
            .build()
    );

    private final Setting<Double> customZ = sgBounds.add(new DoubleSetting.Builder()
            .name("custom-z")
            .description("The custom z factor.")
            .defaultValue(100)
            .sliderMin(-250)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Custom)
            .build()
    );

    private final Setting<Double> randomMinX = sgBounds.add(new DoubleSetting.Builder()
            .name("random-min-x")
            .description("The minimum x.")
            .defaultValue(50)
            .sliderMin(-250)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Random)
            .build()
    );

    private final Setting<Double> randomMinY = sgBounds.add(new DoubleSetting.Builder()
            .name("random-min-y")
            .description("The minimum y.")
            .defaultValue(50)
            .sliderMin(-250)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Random)
            .build()
    );

    private final Setting<Double> randomMinZ = sgBounds.add(new DoubleSetting.Builder()
            .name("random-min-z")
            .description("The minimum z.")
            .defaultValue(50)
            .sliderMin(-250)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Random)
            .build()
    );

    private final Setting<Double> randomMaxX = sgBounds.add(new DoubleSetting.Builder()
            .name("random-max-x")
            .description("The maximum x.")
            .defaultValue(50)
            .min(0.001)
            .sliderMin(0.1)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Random)
            .build()
    );

    private final Setting<Double> randomMaxY = sgBounds.add(new DoubleSetting.Builder()
            .name("random-max-y")
            .description("The maximum y.")
            .defaultValue(50)
            .min(0.001)
            .sliderMin(0.1)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Random)
            .build()
    );

    private final Setting<Double> randomMaxZ = sgBounds.add(new DoubleSetting.Builder()
            .name("random-max-z")
            .description("The maximum z.")
            .defaultValue(50)
            .min(0.001)
            .sliderMin(0.1)
            .sliderMax(250)
            .visible(() -> boundsType.get() == BoundsType.Random)
            .build()
    );


    // Horizontal


    private final Setting<HorizontalMode> horizontalMode = sgHorizontal.add(new EnumSetting.Builder<HorizontalMode>()
            .name("horizontal-mode")
            .description("How to fly horizontally on servers.")
            .defaultValue(HorizontalMode.Precise)
            .build()
    );

    private final Setting<Integer> packetDigits = sgHorizontal.add(new IntSetting.Builder()
            .name("packet-digits")
            .description("How far to.")
            .defaultValue(0)
            .sliderMin(5)
            .sliderMax(9)
            .noSlider()
            .visible(() -> horizontalMode.get() == HorizontalMode.Small)
            .build()
    );

    private final Setting<Double> speed = sgHorizontal.add(new DoubleSetting.Builder()
            .name("speed")
            .description("At which speed to travel.")
            .defaultValue(1)
            .min(0.001)
            .sliderMin(0.001)
            .sliderMax(2)
            .visible(() -> horizontalMode.get() != HorizontalMode.Small)
            .build()
    );

    private final Setting<Integer> horizontalClips = sgHorizontal.add(new IntSetting.Builder()
            .name("horizontal-clips")
            .description("How many times to clip when traveling.")
            .defaultValue(5)
            .min(1)
            .sliderMin(1)
            .sliderMax(5)
            .noSlider()
            .visible(() -> horizontalMode.get() == HorizontalMode.Small)
            .build()
    );

    private final Setting<Double> horizontalStartDistance = sgHorizontal.add(new DoubleSetting.Builder()
            .name("horizontal-start-distance")
            .description("After what distance to start clipping.")
            .defaultValue(0.625)
            .min(0)
            .sliderMin(0)
            .sliderMax(1)
            .visible(() -> horizontalMode.get() != HorizontalMode.Clip && horizontalMode.get() != HorizontalMode.Small)
            .build()
    );

    private final Setting<Integer> horizontalStartClip = sgHorizontal.add(new IntSetting.Builder()
            .name("horizontal-start-clip")
            .description("After what clip to start.")
            .defaultValue(1)
            .min(0)
            .sliderMin(0)
            .sliderMax(3)
            .noSlider()
            .visible(() -> horizontalMode.get() == HorizontalMode.Small)
            .build()
    );

    private final Setting<Double> clipDistance = sgHorizontal.add(new DoubleSetting.Builder()
            .name("clip-distance")
            .description("How far to clip forwards per cycle.")
            .defaultValue(0.262)
            .min(0.001)
            .sliderMin(0.001)
            .sliderMax(0.312)
            .visible(() -> horizontalMode.get() != HorizontalMode.Clip)
            .build()
    );

    private final Setting<Double> slowClipDistance = sgHorizontal.add(new DoubleSetting.Builder()
            .name("slow-clip-distance")
            .description("How far to clip forwards when sneaking or sprinting per cycle.")
            .defaultValue(0.212)
            .min(0.001)
            .sliderMin(0.001)
            .sliderMax(0.3)
            .visible(() -> horizontalMode.get() != HorizontalMode.Clip)
            .build()
    );

    private final Setting<Boolean> acceptTeleport = sgHorizontal.add(new BoolSetting.Builder()
            .name("accept-teleport")
            .description("Sends a teleport confirm packet to the server after the bounds.")
            .defaultValue(true)
            .build()
    );


    // Vertical


    private final Setting<VerticalMode> verticalMode = sgVertical.add(new EnumSetting.Builder<VerticalMode>()
            .name("vertical-mode")
            .description("How to fly vertically on servers.")
            .defaultValue(VerticalMode.Simple)
            .build()
    );

    private final Setting<Double> verticalSpeed = sgVertical.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("Your vertical speed.")
            .defaultValue(0.060)
            .min(0.060)
            .sliderMin(0.060)
            .sliderMax(1.5)
            .visible(() -> verticalMode.get() == VerticalMode.Clip || verticalMode.get() == VerticalMode.Fast)
            .build()
    );

    private final Setting<Double> verticalStartDistance = sgVertical.add(new DoubleSetting.Builder()
            .name("vertical-start-distance")
            .description("After what distance to start clipping.")
            .defaultValue(0.015)
            .min(0)
            .sliderMin(0)
            .sliderMax(1)
            .visible(() -> verticalMode.get() == VerticalMode.Clip || verticalMode.get() == VerticalMode.Fast)
            .build()
    );

    private final Setting<Double> verticalClipDistance = sgVertical.add(new DoubleSetting.Builder()
            .name("vertical-clip-distance")
            .description("The clip distance when traveling vertically.")
            .defaultValue(0.062)
            .min(0.001)
            .sliderMin(0.001)
            .sliderMax(1.5)
            .visible(() -> verticalMode.get() != VerticalMode.None)
            .build()
    );


    // Other


    private final Setting<Boolean> spoofOnGround = sgOther.add(new BoolSetting.Builder()
            .name("spoof-on-ground")
            .description("Sets you server-side on ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> sprint = sgOther.add(new BoolSetting.Builder()
            .name("sprint")
            .description("Automatically sprints to allow higher speeds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noCollision = sgOther.add(new BoolSetting.Builder()
            .name("no-collision")
            .description("Removes block collisions client-side.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> smallBounds = sgOther.add(new BoolSetting.Builder()
            .name("small-bounds")
            .description("Uses smaller bounds when flying horizontally.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> bigPackets = sgOther.add(new BoolSetting.Builder()
            .name("big-packets")
            .description("Uses packets bigger in size.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> updateRotation = sgOther.add(new BoolSetting.Builder()
            .name("update-rotation")
            .description("Updates your rotation while flying.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> updateVelocity = sgOther.add(new BoolSetting.Builder()
            .name("update-velocity")
            .description("Updates your client-side velocity.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> velocityMultiplier = sgOther.add(new DoubleSetting.Builder()
            .name("velocity-multiplier")
            .description("The value to multiply your client-side velocity with.")
            .defaultValue(1)
            .min(0)
            .sliderMin(0.1)
            .sliderMax(2.25)
            .visible(updateVelocity::get)
            .build()
    );

    // Anti Kick

    private final Setting<AntiFallMode> antiFallMode = sgAntiKick.add(new EnumSetting.Builder<AntiFallMode>()
            .name("anti-fall-mode")
            .description("How to vertically rubberband you once after enabling the module to reset your velocity.")
            .defaultValue(AntiFallMode.Up)
            .build()
    );

    private final Setting<Boolean> antiKickOnMove = sgAntiKick.add(new BoolSetting.Builder()
            .name("anti-kick-on-move")
            .description("Goes downwards after some time to prevent you from being kicked for flight.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> antiKickDelay = sgAntiKick.add(new IntSetting.Builder()
            .name("anti-kick-delay")
            .description("How long to wait before going down again.")
            .defaultValue(15)
            .min(2)
            .sliderMin(10)
            .sliderMax(25)
            .noSlider()
            .visible(antiKickOnMove::get)
            .build()
    );

    private final Setting<IdleMode> idleMode = sgAntiKick.add(new EnumSetting.Builder<IdleMode>()
            .name("idle-mode")
            .description("How to stop the server from kicking you.")
            .defaultValue(IdleMode.UpDown)
            .build()
    );

    private final Setting<Integer> idleDelay = sgAntiKick.add(new IntSetting.Builder()
            .name("idle-delay")
            .description("How long to wait before spoofing again.")
            .defaultValue(15)
            .min(0)
            .sliderMin(15)
            .sliderMax(25)
            .noSlider()
            .visible(() -> idleMode.get() == IdleMode.Down || idleMode.get() == IdleMode.UpDown)
            .build()
    );

    private final Setting<Integer> tpBackDelay = sgAntiKick.add(new IntSetting.Builder()
            .name("tp-back-delay")
            .description("How long to wait before going back up.")
            .defaultValue(5)
            .min(1)
            .sliderMin(15)
            .sliderMax(25)
            .noSlider()
            .visible(() -> idleMode.get() == IdleMode.UpDown)
            .build()
    );

    private final Setting<PosUpdateAction> posUpdateAction = sgAntiKick.add(new EnumSetting.Builder<PosUpdateAction>()
            .name("pos-update-action")
            .description("What to do when the server requires you to update your position.")
            .defaultValue(PosUpdateAction.AcceptRequired)
            .build()
    );

    private int antiKickTicks;
    private int idleTicks;
    private int tpTicks;

    private int ticksExisted;

    private int teleportID;
    private Vec3d teleportPos;

    private Vec3d velocity;

    private boolean up;
    private boolean updated;

    private final Random random = new Random();

    private List<PlayerMoveC2SPacket> packets;
    private List<TeleportConfirmC2SPacket> tpPackets;

    public RubberbandFly() {
        super(Categories.Movement, "rubberband-fly", "Fly with rubberbanding.");
    }

    @Override
    public void onActivate() {
        antiKickTicks = 0;
        idleTicks = 0;
        tpTicks = 0;

        ticksExisted = 0;

        teleportID = -1;
        teleportPos = null;

        velocity = null;

        up = false;
        updated = true;

        packets = new ArrayList<>();
        tpPackets = new ArrayList<>();

        if (antiFallMode.get() != AntiFallMode.None && mc != null && mc.world != null && mc.getNetworkHandler() != null && (antiFallMode.get() == AntiFallMode.UpPlus || !doesCollide(mc.player.getBoundingBox().offset(0, -0.001, 0)))) {
            double factor = ((double) Math.round(verticalClipDistance.get() * 1000) / 1000) * (antiFallMode.get() == AntiFallMode.Down ? 1 : -1);

            if (!doesCollide(mc.player.getBoundingBox().offset(0, -factor + (antiFallMode.get() == AntiFallMode.Down ? 0.001 : -0.001), 0))) {
                sendPacket(mc.player.getX(), mc.player.getY() - factor, mc.player.getZ(), mc.player.isOnGround());
                sendPacket(mc.player.getX(), 0, mc.player.getZ());
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (sprint.get() && mc.getNetworkHandler() != null) {
            if (!mc.player.isSprinting()) mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

            if (mc.player != null) mc.player.setSprinting(false);
        }

        if (mc != null && mc.player != null) {
            mc.player.setVelocity(0, 0, 0);
        }

        if (teleportPos != null) {
            if (teleportID >= 0 && mc.getNetworkHandler() != null) sendTpPacket(teleportID);
            if (mc != null && mc.player != null) mc.player.updatePosition(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ());

            teleportID = -1;
            teleportPos = null;
        }
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (noCollision.get()) {
            event.shape = VoxelShapes.empty();
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (ticksExisted == 0) {
            ticksExisted = 1;
            return;
        }

        updated = false;

        ticksExisted++;

        if (teleportPos != null) {
            if (teleportID >= 0) sendTpPacket(teleportID);
            mc.player.updatePosition(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ());

            teleportID = -1;
            teleportPos = null;
        }

        mc.player.setVelocity(0, 0, 0);

        Vec3d bounds = getBounds();
        Vec3d position = mc.player.getPos();

        boolean move = true;

        // Vertical Movement / Anti Kick / Idling

        if ((idleTicks >= idleDelay.get() || idleDelay.get() == 0) && up) {
            tpTicks++;
        }

        boolean antiKick = antiKickOnMove.get() && (antiKickTicks >= antiKickDelay.get()) && (isPlayerMoving() || mc.options.jumpKey.isPressed());
        boolean idle = (idleMode.get() == IdleMode.Down || idleMode.get() == IdleMode.UpDown) && !isPlayerMoving() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()
                && ((idleTicks >= idleDelay.get() || idleDelay.get() == 0) && !up || tpTicks >= tpBackDelay.get() && up && idleMode.get() == IdleMode.UpDown);

        if (antiKick) {
            antiKickTicks = 0;
        } else if (idle) {
            move = false;
        }

        if (tpTicks >= tpBackDelay.get() && up && idleMode.get() == IdleMode.UpDown) {
            idleTicks = 0;
            tpTicks = 0;
        }

        if (verticalMode.get() != VerticalMode.None && (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed()) || antiKick || idle) {
            double factor = ((double) Math.round(verticalClipDistance.get() * 1000) / 1000);
            boolean shouldGoDown = antiKick || idle;

            if (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed()) move = false;

            switch (verticalMode.get()) {
                case Simple -> {
                    if (mc.options.jumpKey.isPressed() && !shouldGoDown || up && idleMode.get() == IdleMode.UpDown && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                        if (!doesCollide(mc.player.getBoundingBox().offset(0, factor - 0.001, 0))) {
                            sendPacket(mc.player.getX(), mc.player.getY() + factor, mc.player.getZ(), mc.player.isOnGround());
                            sendPacket(mc.player.getX(), 0, mc.player.getZ());

                            position = new Vec3d(mc.player.getX(), mc.player.getY() + factor, mc.player.getZ());
                        }

                        if (idleMode.get() == IdleMode.UpDown) up = false;
                        tpTicks = 0;
                    } else if (mc.options.sneakKey.isPressed() || shouldGoDown) {
                        if (!doesCollide(mc.player.getBoundingBox().offset(0, -factor + 0.001, 0))) {
                            sendPacket(mc.player.getX(), mc.player.getY() - factor, mc.player.getZ(), mc.player.isOnGround());
                            sendPacket(mc.player.getX(), 0, mc.player.getZ());

                            position = new Vec3d(mc.player.getX(), mc.player.getY() - factor, mc.player.getZ());
                        }

                        if (!doesCollide(mc.player.getBoundingBox().offset(0, -factor - 0.001, 0))) {
                            if (idleMode.get() == IdleMode.UpDown) up = true;
                        }
                    }
                }

                case Clip -> {
                    if (mc.options.jumpKey.isPressed() && !shouldGoDown || up && idleMode.get() == IdleMode.UpDown && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                        boolean update = false;

                        for (double y = (verticalStartDistance.get() / 10); y < verticalSpeed.get() + 0.01; y += factor) {
                            if (!doesCollide(mc.player.getBoundingBox().offset(0, y - 0.001, 0))) {
                                sendPacket(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), mc.player.isOnGround());

                                update = true;
                            }
                        }

                        if (idleMode.get() == IdleMode.UpDown) up = false;
                        tpTicks = 0;

                        if (update) {
                            sendBoundsPacket(bounds);
                            position = new Vec3d(mc.player.getX(), mc.player.getY() + verticalSpeed.get(), mc.player.getZ());
                        }
                    } else if (mc.options.sneakKey.isPressed() || shouldGoDown) {
                        boolean update = false;

                        for (double y = (verticalStartDistance.get() / 10); y < verticalSpeed.get() + 0.01; y += factor) {
                            if (!doesCollide(mc.player.getBoundingBox().offset(0, -y + 0.001, 0))) {
                                sendPacket(mc.player.getX(), mc.player.getY() - y, mc.player.getZ(), mc.player.isOnGround());

                                update = true;
                            }
                        }

                        if (!doesCollide(mc.player.getBoundingBox().offset(0, -verticalSpeed.get() - 0.001, 0))) {
                            if (idleMode.get() == IdleMode.UpDown) up = true;
                        }

                        if (update) {
                            sendBoundsPacket(bounds);
                            position = new Vec3d(mc.player.getX(), mc.player.getY() - verticalSpeed.get(), mc.player.getZ());
                        }
                    }
                }

                case Fast -> {
                    if (mc.options.jumpKey.isPressed() && !shouldGoDown || up && idleMode.get() == IdleMode.UpDown && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                        boolean update = false;

                        for (double y = (verticalStartDistance.get() / 10); y < verticalSpeed.get() + 0.01; y += factor) {
                            if (!doesCollide(mc.player.getBoundingBox().offset(0, y - 0.001, 0))) {
                                sendPacket(mc.player.getX(), mc.player.getY() + factor, mc.player.getZ(), mc.player.isOnGround());

                                update = true;
                            }
                        }

                        if (idleMode.get() == IdleMode.UpDown) up = false;
                        tpTicks = 0;

                        if (update) {
                            sendBoundsPacket(bounds);
                            position = new Vec3d(mc.player.getX(), mc.player.getY() + verticalSpeed.get(), mc.player.getZ());
                        }
                    } else if (mc.options.sneakKey.isPressed() || shouldGoDown) {
                        boolean update = false;

                        for (double y = (verticalStartDistance.get() / 10); y < verticalSpeed.get() + 0.01; y += factor) {
                            if (!doesCollide(mc.player.getBoundingBox().offset(0, -y + 0.001, 0))) {
                                sendPacket(mc.player.getX(), mc.player.getY() - factor, mc.player.getZ(), mc.player.isOnGround());

                                update = true;
                            }
                        }

                        if (!doesCollide(mc.player.getBoundingBox().offset(0, -verticalSpeed.get() - 0.001, 0))) {
                            if (idleMode.get() == IdleMode.UpDown) up = true;
                        }

                        if (update) {
                            sendBoundsPacket(bounds);
                            position = new Vec3d(mc.player.getX(), mc.player.getY() - verticalSpeed.get(), mc.player.getZ());
                        }
                    }
                }
            }
        }

        if (move && isPlayerMoving()) {
            double[] dir = directionSpeed(speed.get());

            // Sprinting

            if (sprint.get()) {
                if (!mc.player.isSprinting()) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                }

                mc.player.setSprinting(true);
            }

            // Different Modes

            double factor = mc.player.isSneaking() || mc.player.getHungerManager().getFoodLevel() <= 6 ? slowClipDistance.get() : clipDistance.get();

            switch (horizontalMode.get()) {
                case Clip -> {
                    double[] distance = directionSpeed(((double) Math.round((speed.get() / 16.666) * 1000) / 1000));

                    sendPacket(mc.player.getX() + distance[0], mc.player.getY(), mc.player.getZ() + distance[1], mc.player.isOnGround());
                    sendBoundsPacket(bounds);
                    if (acceptTeleport.get()) sendTpPacket(teleportID);

                    position = new Vec3d(mc.player.getX() + distance[0], mc.player.getY(), mc.player.getZ() + distance[1]);
                }

                case Small -> {
                    double small = factor * Math.pow(10, -packetDigits.get());

                    for (int clip = horizontalStartClip.get(); clip < horizontalClips.get(); clip++) {
                        double[] speed = directionSpeed(clip * small);

                        sendPacket(mc.player.getX() + speed[0], mc.player.getY(), mc.player.getZ() + speed[1]);
                    }

                    sendBoundsPacket(bounds);
                    if (acceptTeleport.get()) sendTpPacket(teleportID);
                    double[] speed = directionSpeed((Math.max(horizontalClips.get() - 1, 0)) * small);

                    position = new Vec3d(mc.player.getX() + speed[0], mc.player.getY(), mc.player.getZ() + speed[1]);
                }

                case Precise -> {
                    for (double i = (horizontalStartDistance.get() / 10); i < speed.get(); i += factor) {
                        double[] speed = directionSpeed(i);

                        sendPacket(mc.player.getX() + speed[0], mc.player.getY(), mc.player.getZ() + speed[1]);
                    }

                    sendBoundsPacket(bounds);
                    if (acceptTeleport.get()) sendTpPacket(teleportID);

                    position = new Vec3d(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);
                }

                case Fast -> {
                    for (double i = (horizontalStartDistance.get() / 10); i < speed.get(); i += factor) {
                        double[] speed = directionSpeed(factor);

                        sendPacket(mc.player.getX() + speed[0], mc.player.getY(), mc.player.getZ() + speed[1]);
                    }

                    sendBoundsPacket(bounds);
                    if (acceptTeleport.get()) sendTpPacket(teleportID);

                    position = new Vec3d(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1]);
                }
            }
        }

        if (updateVelocity.get()) velocity = new Vec3d(position.getX() - mc.player.getX(), position.getY() - mc.player.getY(), position.getZ() - mc.player.getZ()).multiply(velocityMultiplier.get());

        antiKickTicks++;
        idleTicks++;
    }

    // Updating Velocity

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (velocity != null && updateVelocity.get()) {
            ((IVec3d) event.movement).set(velocity.getX(), velocity.getY(), velocity.getZ());
            velocity = null;
        }
    }

    // Irrelevant Packet Cancel

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet && !packets.remove(packet)) {
            event.cancel();
        } else if (event.packet instanceof TeleportConfirmC2SPacket packet
                && !tpPackets.remove(packet) && (posUpdateAction.get() == PosUpdateAction.AcceptAll
                || posUpdateAction.get() == PosUpdateAction.AcceptRequired)) {

            event.cancel();
        }
    }

    // No Rotate / Updating the Position

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            teleportID = packet.getTeleportId();

            if (posUpdateAction.get() == PosUpdateAction.IgnoreRotation) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());

                packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
                packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
            } else if (posUpdateAction.get() != PosUpdateAction.Ignore) {
                switch (posUpdateAction.get()) {
                    case AcceptAll -> {
                        mc.player.updatePosition(packet.getX(), packet.getY(), packet.getZ());
                        sendTpPacket(packet.getTeleportId());
                        event.cancel();
                    }

                    case AcceptRequired -> {
                        teleportID = packet.getTeleportId();
                        teleportPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                        event.cancel();
                    }

                    case Update -> {
                        teleportPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                    }
                }
            }
        }
    }

    // Utils

    private void sendTpPacket(int id) {
        TeleportConfirmC2SPacket packet = new TeleportConfirmC2SPacket(id);

        tpPackets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    private void sendBoundsPacket(Vec3d bounds) {
        Vec3d tempBounds = smallBounds.get() ? new Vec3d(mc.player.getX(), bounds.getY(), mc.player.getZ()) : bounds;

        sendPacket(tempBounds.getX(), tempBounds.getY(), tempBounds.getZ());
    }

    private void sendPacket(double x, double y, double z) {
        sendPacket(x, y, z, spoofOnGround.get() || mc.player.isOnGround());
    }

    private void sendPacket(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround);

        if (updateRotation.get() && !updated || bigPackets.get()) {
            packet = new PlayerMoveC2SPacket.Full(x, y, z, mc.player.getYaw(), mc.player.getPitch(), onGround);
            updated = true;
        }

        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    private Vec3d getBounds() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        double infinity = 7.2E+4;
        double small = 0.95 * Math.pow(10, -digits.get());

        switch (boundsType.get()) {
            case Up -> {
                return new Vec3d(x, y + bypassHeight.get(), z);
            }

            case Down -> {
                return new Vec3d(x, y - bypassHeight.get(), z);
            }

            case Ceil -> {
                double[] speed = directionSpeed(ceilRadius.get());

                double vx = speed[0];
                double vz = speed[1];

                return new Vec3d(x + vx, y, z + vz);
            }

            case Zero -> {
                return new Vec3d(0, 0, 0);
            }

            case Small -> {
                switch (boundsMode.get()) {
                    case Horizontal -> {
                        return new Vec3d(x > 0 ? small : -small, y, z > 0 ? small : -small);
                    }

                    case Vertical -> {
                        new Vec3d(x, y > 0 ? small : -small, z);
                    }
                }

                return new Vec3d(x > 0 ? small : -small, y > 0 ? small : -small, z > 0 ? small : -small);
            }

            case Alternative -> {
                if (ticksExisted % 2 == 0) {
                    return new Vec3d(x, y + bypassHeight.get(), z);
                } else {
                    return new Vec3d(x, y - bypassHeight.get(), z);
                }
            }

            case Infinitive -> {
                switch (boundsMode.get()) {
                    case Horizontal -> {
                        return new Vec3d(x > 0 ? infinity : -infinity, y, z > 0 ? infinity : -infinity);
                    }

                    case Vertical -> {
                        return new Vec3d(x, y > 0 ? infinity : -infinity, z);
                    }
                }

                return new Vec3d(x > 0 ? infinity : -infinity, y > 0 ? infinity : -infinity, z > 0 ? infinity : -infinity);
            }

            case Preserve -> {
                return new Vec3d(x, y, z);
            }

            case Obscure -> {
                return new Vec3d(x + random.nextDouble(80) + 20, MathHelper.clamp(y, 1.5, 253.5), z + random.nextDouble(80) + 20);
            }

            case Bypass -> {
                return new Vec3d(x, y - bypassHeight.get() < 0 ? 0 : y - bypassHeight.get(), z);
            }

            case Random -> {
                Vec3d bounds = new Vec3d(0, 0, 0);

                double dx = randomMinX.get() + random.nextDouble(randomMaxX.get());
                double dy = randomMinY.get() + random.nextDouble(randomMaxY.get());
                double dz = randomMinZ.get() + random.nextDouble(randomMaxZ.get());

                switch (customMode.get()) {
                    case Polar -> {
                        bounds = new Vec3d(dx, dy, dz);
                    }

                    case SmartPolar -> {
                        bounds = new Vec3d(dx * (x > 0 ? 1 : -1), dy * (y > 0 ? 1 : -1), dz * (z > 0 ? 1 : -1));
                    }

                    case Relative -> {
                        bounds = new Vec3d(x + dx, y + dy, z + dz);
                    }

                    case SmartRelative -> {
                        bounds = new Vec3d(x + dx * (x > 0 ? 1 : -1), y + dy * (y > 0 ? 1 : -1), z + dz * (z > 0 ? 1 : -1));
                    }

                    case Multiply -> {
                        bounds = new Vec3d(x * dx, y * dy, z * dz);
                    }
                }

                return bounds;
            }

            case Custom -> {
                Vec3d bounds = new Vec3d(0, 0, 0);

                switch (customMode.get()) {
                    case Polar -> {
                        bounds = new Vec3d(customX.get(), customY.get(), customZ.get());
                    }

                    case SmartPolar -> {
                        bounds = new Vec3d(customX.get() * (x > 0 ? 1 : -1), customY.get() * (y > 0 ? 1 : -1), customZ.get() * (z > 0 ? 1 : -1));
                    }

                    case Relative -> {
                        bounds = new Vec3d(x + customX.get(), y + customY.get(), z + customZ.get());
                    }

                    case SmartRelative -> {
                        bounds = new Vec3d(x + customX.get() * (x > 0 ? 1 : -1), y + customY.get() * (y > 0 ? 1 : -1), z + customZ.get() * (z > 0 ? 1 : -1));
                    }

                    case Multiply -> {
                        bounds = new Vec3d(x * customX.get(), y * customY.get(), z * customZ.get());
                    }
                }

                return bounds;
            }

            default -> {
                return new Vec3d(x, 0, z);
            }
        }
    }

    private boolean doesCollide(Box box) {
        return !noCollision.get() && mc.world.getBlockCollisions(mc.player, box).iterator().hasNext();
    }

    // Constants

    public enum BoundsType {
        Up,
        Down,
        Zero,

        Ceil,

        Small,
        Alternative,
        Infinitive,

        Preserve,

        Obscure,
        Bypass,
        Random,
        Custom,
        Normal
    }

    public enum BoundsMode {
        Both,
        Horizontal,
        Vertical
    }

    public enum CustomMode {
        Polar,
        Relative,
        SmartPolar,
        SmartRelative,
        Multiply
    }

    public enum HorizontalMode {
        Clip,
        Small,
        Precise,
        Fast
    }

    public enum VerticalMode {
        None,
        Simple,
        Clip,
        Fast
    }

    public enum AntiFallMode {
        None,
        Up,
        Down,
        UpPlus
    }

    public enum IdleMode {
        None,
        Down,
        UpDown,
        Semi
    }

    public enum PosUpdateAction {
        Ignore,
        Update,
        AcceptAll,
        IgnoreRotation,
        AcceptRequired
    }
}
