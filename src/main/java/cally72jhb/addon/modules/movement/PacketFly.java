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
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PacketFly extends Module {
    private final SettingGroup sgBounds = settings.createGroup("Bounds");
    private final SettingGroup sgFlight = settings.createGroup("Flight");
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
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

    private final Setting<BoundsUpdateMode> boundsUpdateMode = sgBounds.add(new EnumSetting.Builder<BoundsUpdateMode>()
            .name("bounds-mode")
            .description("How to update the bounds normal position.")
            .defaultValue(BoundsUpdateMode.Passive)
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
            .sliderMax(50)
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

    private final Setting<Boolean> boundsOnGround = sgBounds.add(new BoolSetting.Builder()
            .name("bounds-on-ground")
            .description("Whether or not to send onground or offground bounds.")
            .defaultValue(true)
            .build()
    );


    // Flight


    private final Setting<Type> type = sgFlight.add(new EnumSetting.Builder<Type>()
            .name("bypass-type")
            .description("How to bypass the servers anti-cheat.")
            .defaultValue(Type.FACTOR)
            .build()
    );

    private final Setting<Boolean> multiAxis = sgFlight.add(new BoolSetting.Builder()
            .name("multi-axis")
            .description("Allows for multi-axis flight.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> speed = sgFlight.add(new DoubleSetting.Builder()
            .name("factor")
            .description("Your flight factor.")
            .defaultValue(5)
            .min(0)
            .visible(() -> type.get() == Type.FACTOR || type.get() == Type.DESYNC)
            .build()
    );

    private final Setting<Boolean> phase = sgFlight.add(new BoolSetting.Builder()
            .name("phase")
            .description("Tries to phase when on older versions.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> phaseNoSlow = sgFlight.add(new BoolSetting.Builder()
            .name("phase-no-slow")
            .description("Allows for higher speeds when phasing.")
            .defaultValue(true)
            .visible(phase::get)
            .build()
    );


    // Bypass


    private final Setting<Boolean> spoofOnGround = sgBypass.add(new BoolSetting.Builder()
            .name("spoof-on-ground")
            .description("Sets you server-side on ground.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> sprint = sgBypass.add(new BoolSetting.Builder()
            .name("sprint")
            .description("Automatically sprints to allow higher speeds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noCollision = sgBypass.add(new BoolSetting.Builder()
            .name("no-collision")
            .description("Removes block collisions client-side.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> smallBounds = sgBypass.add(new BoolSetting.Builder()
            .name("small-bounds")
            .description("Uses smaller bounds when flying horizontally.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> bind = sgBypass.add(new BoolSetting.Builder()
            .name("bind")
            .description("Bounds for the player.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> bypass = sgBypass.add(new BoolSetting.Builder()
            .name("bypass")
            .description("Changes the behaviour of anti-kick.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ncp = sgBypass.add(new BoolSetting.Builder()
            .name("ncp")
            .description("Limits your movement to bypass NCP.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyOnMove = sgBypass.add(new BoolSetting.Builder()
            .name("only-on-move")
            .description("Only sends packets if your moving.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> updateRotation = sgBypass.add(new BoolSetting.Builder()
            .name("update-rotation")
            .description("Updates your rotation while flying.")
            .defaultValue(true)
            .build()
    );

    private final Setting<AntiFallMode> antiFallMode = sgBypass.add(new EnumSetting.Builder<AntiFallMode>()
            .name("anti-fall-mode")
            .description("How to vertically rubberband you once after enabling the module to reset your velocity.")
            .defaultValue(AntiFallMode.Up)
            .build()
    );


    // Anti Kick


    private final Setting<AntiKick> antiKick = sgAntiKick.add(new EnumSetting.Builder<AntiKick>()
            .name("anti-kick")
            .description("What anti-kick mode to use to prevent you from being kicked by the server.")
            .defaultValue(AntiKick.NORMAL)
            .build()
    );

    private final Setting<Limit> limit = sgAntiKick.add(new EnumSetting.Builder<Limit>()
            .name("limit")
            .description("How to limit the flight to prevent you from getting kicked.")
            .defaultValue(Limit.STRICT)
            .build()
    );

    private final Setting<Boolean> constrict = sgAntiKick.add(new BoolSetting.Builder()
            .name("constrict")
            .description("Whether or not to stricken your movement.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> jitter = sgAntiKick.add(new BoolSetting.Builder()
            .name("jitter")
            .description("Randomizes the movement ever so slightly.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> stopOnGround = sgAntiKick.add(new BoolSetting.Builder()
            .name("stop-on-ground")
            .description("Disables anti-kick when you are on ground.")
            .defaultValue(true)
            .build()
    );

    // Variables

    private int antiKickTicks;
    private int verticalTicks;
    private int horizontalTicks;

    private boolean oddJitter;
    private boolean limitStrict;
    private int limitTicks;
    private int jitterTicks;

    private int factorCounter;

    private double speedX;
    private double speedY;
    private double speedZ;

    private int ticksExisted;

    private boolean updated;

    private int teleportID;
    private Vec3d teleportPos;

    private List<PlayerMoveC2SPacket> packets;
    private Map<Integer, TimedVec3d> teleports;

    private final Random random = new Random();

    // Constructor

    public PacketFly() {
        super(Categories.Movement, "packet-fly", "Bypasses the servers anti-cheat to allow you to fly.");
    }

    // Activation

    @Override
    public void onActivate() {
        packets = new ArrayList<>();
        teleports = new ConcurrentHashMap<>();

        antiKickTicks = 0;
        verticalTicks = 0;
        horizontalTicks = 0;

        oddJitter = false;
        limitStrict = false;
        limitTicks = 0;
        jitterTicks = 0;

        factorCounter = 0;

        speedX = 0;
        speedY = 0;
        speedZ = 0;

        ticksExisted = 0;

        updated = false;

        teleportID = -1;
        teleportPos = null;

        if (antiFallMode.get() != AntiFallMode.None && mc != null && mc.world != null && mc.getNetworkHandler() != null) {
            if (antiFallMode.get() == AntiFallMode.Bounds) {
                sendBoundsPacket(getBounds());
            } else if (!doesCollide(mc.player.getBoundingBox().offset(0, -0.001, 0))) {
                double factor = ((double) Math.round(0.06 * 1000) / 1000) * (antiFallMode.get() == AntiFallMode.Down ? 1 : -1);

                if (!doesCollide(mc.player.getBoundingBox().offset(0, -factor + (antiFallMode.get() == AntiFallMode.Down ? 0.001 : -0.001), 0))) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - factor, mc.player.getZ(), mc.player.isOnGround()));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 0, mc.player.getZ(), spoofOnGround.get() || mc.player.isOnGround()));
                }
            }
        }
    }

    // Deactivation

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
            if (teleportID >= 0 && mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportID));
            if (mc != null && mc.player != null) mc.player.updatePosition(teleportPos.getX(), teleportPos.getY(), teleportPos.getZ());

            teleportID = -1;
            teleportPos = null;
        }
    }

    // Collision Shape Event

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (noCollision.get()) {
            event.shape = VoxelShapes.empty();
        }
    }

    // Main Tick

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (ticksExisted % 20 == 0) {
            teleports.forEach((id, position) -> {
                if (System.currentTimeMillis() - position.getTime() > TimeUnit.SECONDS.toMillis(30L)) {
                    teleports.remove(id);
                }
            });
        }

        ticksExisted++;
        updated = false;

        mc.player.setVelocity(0.0D, 0.0D, 0.0D);

        if (teleportID <= 0 && type.get() != Type.SETBACK) {
            sendBoundsPacket(getBounds());
            return;
        }

        speedX = 0;
        speedY = 0;
        speedZ = 0;

        boolean phasing = doesCollide(mc.player.getBoundingBox());

        Vec3d bounds = getBounds();

        // Vertical Movement

        if (mc.options.jumpKey.isPressed() && (horizontalTicks < 1 || (multiAxis.get() && phasing))) {
            if (ticksExisted % (type.get() == Type.SETBACK || type.get() == Type.SLOW || limit.get() == Limit.STRICT ? 10 : 20) == 0) {
                speedY = (antiKick.get() != AntiKick.NONE && onGround()) ? -0.032 : 0.062;
            } else {
                speedY = 0.062;
            }

            antiKickTicks = 0;
            verticalTicks = 5;
        } else if (mc.options.sneakKey.isPressed() && (horizontalTicks < 1 || (multiAxis.get() && phasing))) {
            speedY = -0.062;
            antiKickTicks = 0;
            verticalTicks = 5;
        }

        // Horizontal Movement & Anti Kick

        if ((multiAxis.get() && phasing) || !(mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed())) {
            if (isPlayerMoving()) {
                double[] dir = directionSpeed((((phasing && phase.get()) || ncp.get()) ? (phaseNoSlow.get() ? (multiAxis.get() ? 0.0465 : 0.062) : 0.031) : 0.26) * speed.get());

                if ((dir[0] != 0 || dir[1] != 0) && (verticalTicks < 1 || (multiAxis.get() && phasing))) {
                    speedX = dir[0];
                    speedZ = dir[1];

                    horizontalTicks = 5;
                }
            }

            if (antiKick.get() != AntiKick.NONE && onGround() && ((limit.get() == Limit.NONE) || limitTicks != 0)) {
                if (antiKickTicks < (bypass.get() && !bind.get() ? 1 : 3)) {
                    antiKickTicks++;
                } else {
                    antiKickTicks = 0;

                    if ((antiKick.get() != AntiKick.LIMITED && onGround()) || !phasing) {
                        speedY = (antiKick.get() == AntiKick.STRICT && onGround()) ? -0.08 : -0.04;
                    }
                }
            }
        }

        if (((phasing && phase.get()) || ncp.get()) && mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F && speedY != 0) {
            speedY /= 2.5;
        }

        if (limit.get() != Limit.NONE) {
            if (limitTicks == 0) {
                speedX = 0;
                speedY = 0;
                speedZ = 0;
            } else if (limitTicks == 2 && jitter.get()) {
                if (oddJitter) {
                    speedX = 0;
                    speedY = 0;
                    speedZ = 0;
                }

                oddJitter = !oddJitter;
            }
        } else if (jitter.get() && jitterTicks == 7) {
            speedX = 0;
            speedY = 0;
            speedZ = 0;
        }

        switch (type.get()) {
            case FAST -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, bounds, true, false);
            }

            case SLOW -> {
                if (!isMoving()) break;
                sendPackets(speedX, speedY, speedZ, bounds, true, false);
            }

            case SETBACK -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, bounds, false, false);
            }

            case VECTOR -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, bounds, true, true);
            }

            case FACTOR, DESYNC -> {
                int factorInt = (int) Math.floor(speed.get().floatValue());

                factorCounter++;

                if (factorCounter > (int) (20D / ((speed.get().floatValue() - (double) factorInt) * 20D))) {
                    factorInt += 1;
                    factorCounter = 0;
                }

                for (int i = 1; i <= factorInt; i++) {
                    mc.player.setVelocity(speedX * i, speedY * i, speedZ * i);
                    sendPackets(isMoving() ? speedX * i : 0, speedY * i, isMoving() ? speedZ * i : 0, bounds, true, false);
                }

                speedX = mc.player.getVelocity().x;
                speedY = mc.player.getVelocity().y;
                speedZ = mc.player.getVelocity().z;
            }
        }

        verticalTicks--;
        horizontalTicks--;

        if (constrict.get() && ((limit.get() == Limit.NONE) || limitTicks > 1)) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
        }

        limitTicks++;
        jitterTicks++;

        if (limitTicks > ((limit.get() == Limit.STRICT) ? (limitStrict ? 1 : 2) : 3)) {
            limitTicks = 0;
            limitStrict = !limitStrict;
        }

        if (jitterTicks > 7) {
            jitterTicks = 0;
        }
    }

    // Accept Server Packets & Cancel Rotation

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            if (mc.player.isAlive()) {
                if (this.teleportID <= 0) {
                    this.teleportID = ((PlayerPositionLookS2CPacket) event.packet).getTeleportId();
                } else {
                    if (mc.world.isChunkLoaded(ChunkSectionPos.getSectionCoord(mc.player.getBlockX()), ChunkSectionPos.getSectionCoord(mc.player.getBlockZ())) && type.get() != Type.SETBACK) {
                        if (type.get() == Type.DESYNC) {
                            teleports.remove(packet.getTeleportId());

                            event.cancel();

                            if (type.get() == Type.SLOW) {
                                mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                            }

                            return;
                        } else if (teleports.containsKey(packet.getTeleportId())) {
                            TimedVec3d vec = teleports.get(packet.getTeleportId());

                            if (vec.x == packet.getX() && vec.y == packet.getY() && vec.z == packet.getZ()) {
                                teleports.remove(packet.getTeleportId());

                                event.cancel();

                                if (type.get() == Type.SLOW) {
                                    mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                                }

                                return;
                            }
                        }
                    }
                }
            }

            ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
            packet.getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
            teleportID = packet.getTeleportId();
        }
    }

    // Movement

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (type.get() != Type.SETBACK && teleportID <= 0) return;
        if (type.get() != Type.SLOW) ((IVec3d) event.movement).set(speedX, speedY, speedZ);
    }

    @EventHandler
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && !(event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround)) {
            event.cancel();
        }

        if (event.packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;

            if (this.packets.contains(packet)) {
                this.packets.remove(packet);
                return;
            }

            event.cancel();
        }
    }

    // Packets

    private void sendBoundsPacket(Vec3d bound) {
        Vec3d bounds = smallBounds.get() ? new Vec3d(mc.player.getX(), bound.getY(), mc.player.getZ()) : bound;
        PlayerMoveC2SPacket.PositionAndOnGround boundsPacket = new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, boundsOnGround.get());
        packets.add(boundsPacket);
        mc.getNetworkHandler().sendPacket(boundsPacket);
    }

    private void sendPackets(double x, double y, double z, Vec3d bound, boolean confirm, boolean extra) {
        Vec3d position = new Vec3d(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
        Vec3d bounds = boundsUpdateMode.get() == BoundsUpdateMode.Active ? getBounds() : bound;

        // Next Position Packet

        if ((mc.player.prevYaw != mc.player.getYaw() || mc.player.prevPitch != mc.player.getPitch()) && updateRotation.get() && !updated) {
            PlayerMoveC2SPacket.Full packet = new PlayerMoveC2SPacket.Full(position.x, position.y, position.z, updateRotation.get() ? mc.player.getYaw() : 0.0F, updateRotation.get() ? mc.player.getPitch() : 0.0F, spoofOnGround.get() || mc.player.isOnGround());
            updated = true;
            packets.add(packet);
            mc.getNetworkHandler().sendPacket(packet);
        } else {
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(position.x, position.y, position.z, spoofOnGround.get() || mc.player.isOnGround());
            packets.add(packet);
            mc.getNetworkHandler().sendPacket(packet);
        }

        // Limiting

        if ((limit.get() != Limit.NONE) && limitTicks == 0) return;

        // Bounds Packet

        PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, spoofOnGround.get() || mc.player.isOnGround());
        packets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);

        // Teleport ID Spoofing

        if (confirm) {
            teleportID++;

            if (extra) {
                mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportID - 1));
            }

            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportID));
            teleports.put(teleportID, new TimedVec3d(position.x, position.y, position.z, System.currentTimeMillis()));

            if (extra) {
                mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportID + 1));
            }
        }
    }

    // Utils

    private boolean doesCollide(Box box) {
        return !noCollision.get() && mc.world.getBlockCollisions(mc.player, box).iterator().hasNext();
    }

    private boolean onGround() {
        if (stopOnGround.get()) {
            return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.01, 0)).iterator().hasNext();
        } else {
            return true;
        }
    }

    private boolean isPlayerMoving() {
        if (mc.options.jumpKey.isPressed()) return true;
        if (mc.options.forwardKey.isPressed()) return true;
        if (mc.options.backKey.isPressed()) return true;
        if (mc.options.leftKey.isPressed()) return true;
        if (mc.options.rightKey.isPressed()) return true;

        return false;
    }

    private boolean isMoving() {
        if (onlyOnMove.get()) {
            if (mc.options.jumpKey.isPressed()) return true;
            if (mc.options.sneakKey.isPressed()) return true;
            if (mc.options.forwardKey.isPressed()) return true;
            if (mc.options.backKey.isPressed()) return true;
            if (mc.options.leftKey.isPressed()) return true;
            if (mc.options.rightKey.isPressed()) return true;

            return false;
        }

        return true;
    }

    private double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0F) {
            if (side > 0.0F) {
                yaw += ((forward > 0.0F) ? -45 : 45);
            } else if (side < 0.0F) {
                yaw += ((forward > 0.0F) ? 45 : -45);
            }

            side = 0.0F;

            if (forward > 0.0F) {
                forward = 1.0F;
            } else if (forward < 0.0F) {
                forward = -1.0F;
            }
        }

        double sin = Math.sin(Math.toRadians(yaw + 90.0F));
        double cos = Math.cos(Math.toRadians(yaw + 90.0F));
        double dx = forward * speed * cos + side * speed * sin;
        double dz = forward * speed * sin - side * speed * cos;

        return new double[] { dx, dz };
    }

    private Vec3d getBounds() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        double infinity = 3.2E+7;
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

    // Enums

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

    public enum BoundsUpdateMode {
        Active,
        Passive
    }

    public enum Type {
        FACTOR,
        SETBACK,
        FAST,
        SLOW,
        DESYNC,
        VECTOR
    }

    public enum AntiFallMode {
        None,
        Up,
        Down,
        Bounds
    }

    public enum AntiKick {
        NONE,
        NORMAL,
        LIMITED,
        STRICT
    }

    public enum Limit {
        NONE,
        STRONG,
        STRICT
    }

    // Classes

    private static class TimedVec3d extends Vec3d {
        private final long time;

        public TimedVec3d(double x, double y, double z, long time) {
            super(x, y, z);

            this.time = time;
        }

        public long getTime() {
            return time;
        }
    }
}
