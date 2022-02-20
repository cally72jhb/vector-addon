package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.misc.SystemTimer;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PacketFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFly = settings.createGroup("Fly");
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");
    private final SettingGroup sgKeybind = settings.createGroup("Keybind");
    private final SettingGroup sgPhase = settings.createGroup("Phase");

    private final Setting<Type> type = sgGeneral.add(new EnumSetting.Builder<Type>()
        .name("fly-type")
        .description("The way you are moved by this module.")
        .defaultValue(Type.FACTOR)
        .onChanged(this::updateFlying)
        .build()
    );

    private final Setting<Mode> packetMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("packet-mode")
        .description("Which packets to send to the server.")
        .defaultValue(Mode.DOWN)
        .build()
    );

    private final Setting<Bypass> bypass = sgGeneral.add(new EnumSetting.Builder<Bypass>()
            .name("bypass-mode")
            .description("What bypass mode to use.")
            .defaultValue(Bypass.NONE)
            .build()
    );

    private final Setting<Boolean> onlyOnMove = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-move")
        .description("Only sends packets if your moving.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-ground")
        .description("Disables Anti Kick when you are on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
        .name("strict")
        .description("How to handle certain packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bounds = sgGeneral.add(new BoolSetting.Builder()
        .name("bounds")
        .description("Bounds for the player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> multiAxis = sgGeneral.add(new BoolSetting.Builder()
            .name("multi-axis")
            .description("Whether or not to phase in every direction.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle")
        .description("Toggles the module on join and leave.")
        .defaultValue(true)
        .build()
    );

    // Fly

    private final Setting<Double> factor = sgFly.add(new DoubleSetting.Builder()
        .name("factor")
        .description("Your flight factor.")
        .defaultValue(5)
        .min(0)
        .visible(() -> type.get() == Type.FACTOR || type.get() == Type.DESYNC)
        .build()
    );

    private final Setting<Integer> ignoreSteps = sgFly.add(new IntSetting.Builder()
        .name("ignore-steps")
        .description("How many steps in a row should be ignored.")
        .defaultValue(0)
        .min(0)
        .visible(() -> type.get() == Type.FACTOR || type.get() == Type.DESYNC)
        .build()
    );

    private final Setting<Integer> exponent = sgFly.add(new IntSetting.Builder()
        .name("exponent")
        .description("How far to go per loop.")
        .defaultValue(1)
        .min(1)
        .visible(() -> type.get() == Type.FACTOR || type.get() == Type.DESYNC)
        .build()
    );

    private final Setting<Keybind> factorize = sgFly.add(new KeybindSetting.Builder()
        .name("factorize")
        .description("Key to toggle factor mode.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Boolean> boost = sgFly.add(new BoolSetting.Builder()
        .name("boost")
        .description("Boost the player.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgFly.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Your flight speed when flying horizontal.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Double> vspeed = sgFly.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Your flight speed when flying up and down.")
        .defaultValue(2)
        .min(0)
        .build()
    );

    private final Setting<Double> motion = sgFly.add(new DoubleSetting.Builder()
        .name("factorize-motion")
        .description("The motion applied when factorize is pressed.")
        .defaultValue(100)
        .min(0)
        .sliderMin(50)
        .sliderMax(200)
        .visible(() -> type.get() == Type.FACTOR || type.get() == Type.DESYNC)
        .build()
    );

    private final Setting<Double> boostTimer = sgFly.add(new DoubleSetting.Builder()
        .name("boost-timer")
        .description("The timer for boost.")
        .defaultValue(1.1)
        .min(0)
        .visible(boost::get)
        .build()
    );

    // Anti Kick

    private final Setting<AntiKick> antiKick = sgAntiKick.add(new EnumSetting.Builder<AntiKick>()
        .name("anti-kick")
        .description("The anti kick mode.")
        .defaultValue(AntiKick.NORMAL)
        .build()
    );

    private final Setting<Limit> limit = sgAntiKick.add(new EnumSetting.Builder<Limit>()
        .name("limit")
        .description("The flight limit.")
        .defaultValue(Limit.STRICT)
        .build()
    );

    private final Setting<Boolean> constrict = sgAntiKick.add(new BoolSetting.Builder()
        .name("constrict")
        .description("Already send the packets before the tick (only if the limit is none).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> jitter = sgAntiKick.add(new BoolSetting.Builder()
        .name("jitter")
        .description("Randomize the movement.")
        .defaultValue(false)
        .build()
    );

    // Keybind

    private final Setting<Boolean> message = sgKeybind.add(new BoolSetting.Builder()
        .name("keybind-message")
        .description("Whether or not to send you a message when toggled a mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> toggleLimit = sgKeybind.add(new KeybindSetting.Builder()
        .name("toggle-limit")
        .description("Key to toggle Limit on or off.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    private final Setting<Keybind> toggleAntiKick = sgKeybind.add(new KeybindSetting.Builder()
        .name("toggle-anti-kick")
        .description("Key to toggle anti kick on or off.")
        .defaultValue(Keybind.fromKey(-1))
        .build()
    );

    // Phase

    private final Setting<Phase> phase = sgPhase.add(new EnumSetting.Builder<Phase>()
        .name("phase")
        .description("Whether or not to phase through blocks.")
        .defaultValue(Phase.NONE)
        .build()
    );

    private final Setting<Boolean> noPhaseSlow = sgPhase.add(new BoolSetting.Builder()
        .name("no-phase-slow")
        .description("Whether or not to phase fast or slow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noCollision = sgPhase.add(new BoolSetting.Builder()
        .name("no-collision")
        .description("Whether or not to disable block collisions.")
        .defaultValue(false)
        .build()
    );

    public enum Type {
        FACTOR,
        SETBACK,
        FAST,
        SLOW,
        ELYTRA,
        DESYNC,
        VECTOR,
        OFFGROUND,
        ONGROUND
    }

    public enum Mode {
        PRESERVE,
        UP,
        DOWN,
        LIMITJITTER,
        BYPASS,
        OBSCURE
    }

    public enum Bypass {
        NONE,
        DEFAULT,
        NCP
    }

    public enum Phase {
        NONE,
        VANILLA,
        NCP
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

    private int teleportId;

    private PlayerMoveC2SPacket.PositionAndOnGround startingOutOfBoundsPos;

    private ArrayList<PlayerMoveC2SPacket> packets;
    private Map<Integer, TimeVec3d> posLooks;

    private int antiKickTicks;
    private int vDelay;
    private int hDelay;

    private boolean limitStrict;
    private int limitTicks;
    private int jitterTicks;
    private int ticksExisted;

    private boolean oddJitter;

    private boolean forceAntiKick;
    private boolean forceLimit;

    double speedX;
    double speedY;
    double speedZ;

    private int factorCounter;
    private SystemTimer intervalTimer = new SystemTimer();
    private static final Random random = new Random();

    public PacketFly() {
        super(VectorAddon.Movement, "packet-fly", "Fly with packets.");
    }

    @Override
    public void onActivate() {
        packets = new ArrayList<>();
        posLooks = new ConcurrentHashMap<>();
        teleportId = 0;
        vDelay = 0;
        hDelay = 0;
        antiKickTicks = 0;
        limitTicks = 0;
        jitterTicks = 0;
        ticksExisted = 0;
        limitStrict = false;
        speedX = 0;
        speedY = 0;
        speedZ = 0;
        oddJitter = false;
        forceAntiKick = true;
        forceLimit = true;
        startingOutOfBoundsPos = null;
        startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
        packets.add(startingOutOfBoundsPos);
        mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.setVelocity(0, 0, 0);
        }

        GameMode mode = EntityUtils.getGameMode(mc.player);
        if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }

        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    // Info Sting

    @Override
    public String getInfoString() {
        String info = "";

        info += "[" + type.get().name().substring(0, 1).toUpperCase() + type.get().name().substring(1).toLowerCase() + "] ";
        if (forceAntiKick) info += "[Anti Kick] ";
        if (forceLimit) info += "[Limit]";

        return info;
    }

    // For Disabling

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        if (autoToggle.get()) toggle();
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        if (autoToggle.get()) toggle();
    }

    // For Phase

    @EventHandler
    public void isCube(CollisionShapeEvent event) {
        if (phase.get() == Phase.VANILLA) mc.player.noClip = true;
        if (phase.get() != Phase.NONE && noCollision.get()) event.shape = VoxelShapes.empty();
    }

    // For Toggling

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!(mc.currentScreen == null)) return;

        if (toggleLimit.get().isPressed()) {
            forceLimit = !forceLimit;
            if (message.get()) ChatUtils.sendMsg(Text.of(forceLimit ? "Activated Packet Limit" : "Disabled Packet Limit"));
        }
        if (toggleAntiKick.get().isPressed()) {
            forceAntiKick = !forceAntiKick;
            if (message.get()) ChatUtils.sendMsg(Text.of(forceAntiKick ? "Activated Anti Kick" : "Disabled Anti Kick"));
        }
    }

    // For Boost

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (boost.get()) {
            Modules.get().get(Timer.class).setOverride(boostTimer.get().floatValue());
        } else {
            Modules.get().get(Timer.class).setOverride(Timer.OFF);
        }
    }

    // Main Loop

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        if (type.get() == Type.ELYTRA) {
            Vec3d vec3d = new Vec3d(0,0,0);

            if (mc.player.fallDistance <= 0.2) return;

            if (mc.options.keyForward.isPressed()) {
                vec3d.add(0, 0, speed.get());
                vec3d.rotateY(-(float) Math.toRadians(mc.player.getYaw()));
            } else if (mc.options.keyBack.isPressed()) {
                vec3d.add(0, 0, speed.get());
                vec3d.rotateY((float) Math.toRadians(mc.player.getYaw()));
            }

            if (mc.options.keyJump.isPressed()) {
                vec3d.add(0, vspeed.get(), 0);
            } else if (mc.options.keySneak.isPressed()) {
                vec3d.add(0, -vspeed.get(), 0);
            }

            mc.player.setVelocity(vec3d);
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));

            return;
        }

        if (ticksExisted % 20 == 0) {
            posLooks.forEach((tp, timeVec3d) -> {
                if (System.currentTimeMillis() - timeVec3d.getTime() > TimeUnit.SECONDS.toMillis(30L)) {
                    posLooks.remove(tp);
                }
            });
        }

        ticksExisted++;

        mc.player.setVelocity(0.0D, 0.0D, 0.0D);

        if (teleportId <= 0 && type.get() != Type.SETBACK) {
            startingOutOfBoundsPos = new PlayerMoveC2SPacket.PositionAndOnGround(randomHorizontal(), 1, randomHorizontal(), mc.player.isOnGround());
            packets.add(startingOutOfBoundsPos);
            mc.getNetworkHandler().sendPacket(startingOutOfBoundsPos);
            return;
        }

        boolean phasing = checkCollisionBox();

        speedX = 0;
        speedY = 0;
        speedZ = 0;

        if (mc.options.keyJump.isPressed() && (hDelay < 1 || (multiAxis.get() && phasing))) {
            if (ticksExisted % (type.get() == Type.SETBACK || type.get() == Type.SLOW || limit.get() == Limit.STRICT && forceLimit ? 10 : 20) == 0) {
                speedY = (antiKick.get() != AntiKick.NONE && forceAntiKick && onGround()) ? -0.032 : 0.062;
            } else {
                speedY = 0.062;
            }
            antiKickTicks = 0;
            vDelay = 5;
        } else if (mc.options.keySneak.isPressed() && (hDelay < 1 || (multiAxis.get() && phasing))) {
            speedY = -0.062;
            antiKickTicks = 0;
            vDelay = 5;
        }

        if ((multiAxis.get() && phasing) || !(mc.options.keySneak.isPressed() && mc.options.keyJump.isPressed())) {
            if (isPlayerMoving()) {
                double[] dir = directionSpeed((((phasing && phase.get() == Phase.NCP ) || bypass.get() == Bypass.NCP) ? (noPhaseSlow.get() ? (multiAxis.get() ? 0.0465 : 0.062) : 0.031) : 0.26) * speed.get());
                if ((dir[0] != 0 || dir[1] != 0) && (vDelay < 1 || (multiAxis.get() && phasing))) {
                    speedX = dir[0];
                    speedZ = dir[1];
                    hDelay = 5;
                }
            }

            if (antiKick.get() != AntiKick.NONE && forceAntiKick && onGround() && ((limit.get() == Limit.NONE && forceLimit) || limitTicks != 0)) {
                if (antiKickTicks < (packetMode.get() == Mode.BYPASS && !bounds.get() ? 1 : 3)) {
                    antiKickTicks++;
                } else {
                    antiKickTicks = 0;
                    if ((antiKick.get() != AntiKick.LIMITED && forceAntiKick && onGround()) || !phasing) {
                        speedY = (antiKick.get() == AntiKick.STRICT && forceAntiKick && onGround()) ? -0.08 : -0.04;
                    }
                }
            }
        }

        if (((phasing && phase.get() == Phase.NCP) || bypass.get() == Bypass.NCP) && (double) mc.player.forwardSpeed != 0.0 || (double) mc.player.sidewaysSpeed != 0.0 && speedY != 0) {
            speedY /= 2.5;
        }

        if (limit.get() != Limit.NONE && forceLimit) {
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
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, false);
            }
            case SLOW -> {
                if (!isMoving()) break;
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, false);
            }
            case SETBACK -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), false, false);
            }
            case VECTOR -> {
                if (!isMoving()) break;
                mc.player.setVelocity(speedX, speedY, speedZ);
                sendPackets(speedX, speedY, speedZ, packetMode.get(), true, true);
            }
            case FACTOR, DESYNC -> {
                float rawFactor = factor.get().floatValue();
                if (factorize.get().isPressed() && intervalTimer.hasPassed(3500)) {
                    intervalTimer.reset();
                    rawFactor = motion.get().floatValue();
                }
                int factorInt = (int) Math.floor(rawFactor);
                int ignore = 0;
                factorCounter++;
                if (factorCounter > (int) (20D / ((rawFactor - (double) factorInt) * 20D))) {
                    factorInt += 1;
                    factorCounter = 0;
                }
                for (int i = 1; i <= factorInt; i++) {
                    if (ignore <= 0) {
                        i *= exponent.get();
                        ignore = ignoreSteps.get();
                        mc.player.setVelocity(speedX * i, speedY * i, speedZ * i);
                        sendPackets(isMoving() ? speedX * i : 0, speedY * i, isMoving() ? speedZ * i : 0, packetMode.get(), true, false);
                    } else {
                        ignore--;
                    }
                }
                speedX = mc.player.getVelocity().x;
                speedY = mc.player.getVelocity().y;
                speedZ = mc.player.getVelocity().z;
            }
            case OFFGROUND -> {
                if (!isMoving()) break;
                for (double i = 0.0625; i < speed.get(); i += 0.262) {
                    sendPackets(speedX, speedY, speedZ, packetMode.get(), false, false);
                    sendPackets(speedX, speedY, speedZ, packetMode.get(), true, true);
                }
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + speedX, mc.player.getY() + (strict.get() ? 1 : 5), mc.player.getZ() + speedZ, mc.player.isOnGround()));
            }
            case ONGROUND -> {
                if (!isMoving()) break;
                Vec3d vel = mc.player.getVelocity();
                for (double i = 0.0625; i < speed.get(); i += 0.262) {
                    double[] dir = VectorUtils.directionSpeed((float) i);
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + dir[0], mc.player.getY(), mc.player.getZ() + dir[1], mc.player.isOnGround()));
                }
                if (bypass.get() == Bypass.DEFAULT)
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + vel.x, mc.player.getY() <= 10 ? (strict.get() ? 10 : 255) : (strict.get() ? 0.5 : 1), mc.player.getZ() + vel.z, mc.player.isOnGround()));
            }
        }

        vDelay--;
        hDelay--;

        if (constrict.get() && ((limit.get() == Limit.NONE && forceLimit) || limitTicks > 1)) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
        }

        limitTicks++;
        jitterTicks++;

        if (limitTicks > ((limit.get() == Limit.STRICT && forceLimit) ? (limitStrict ? 1 : 2) : 3)) {
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
        if (type.get() == Type.ELYTRA) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            if (mc.player.isAlive()) {
                if (this.teleportId <= 0) {
                    this.teleportId = ((PlayerPositionLookS2CPacket) event.packet).getTeleportId();
                } else {
                    if (mc.world.isPosLoaded(mc.player.getBlockX(), mc.player.getBlockZ()) && type.get() != Type.SETBACK) {
                        if (type.get() == Type.DESYNC) {
                            posLooks.remove(packet.getTeleportId());
                            event.cancel();
                            if (type.get() == Type.SLOW) {
                                mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                            }
                            return;
                        } else if (posLooks.containsKey(packet.getTeleportId())) {
                            TimeVec3d vec = posLooks.get(packet.getTeleportId());
                            if (vec.x == packet.getX() && vec.y == packet.getY() && vec.z == packet.getZ()) {
                                posLooks.remove(packet.getTeleportId());
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
            teleportId = packet.getTeleportId();
        }
    }

    // Movement

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (type.get() == Type.ELYTRA) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(speed.get().floatValue() / 20);
            return;
        }

        if (type.get() != Type.SETBACK && teleportId <= 0) return;
        if (type.get() != Type.SLOW) ((IVec3d) event.movement).set(speedX, speedY, speedZ);
    }

    @EventHandler
    public void onSend(PacketEvent.Send event) {
        if (type.get() == Type.ELYTRA && event.packet instanceof PlayerMoveC2SPacket) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return;
        }

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

    // Utils

    public void updateFlying(Type type) {
        if (mc.world != null && mc.player != null && type != Type.ELYTRA) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }
    }

    private void sendPackets(double x, double y, double z, Mode mode, boolean confirmTeleport, boolean sendExtraConfirmTeleport) {
        Vec3d nextPos = new Vec3d(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
        Vec3d bounds = getBoundsVec(x, y, z, mode);

        PlayerMoveC2SPacket nextPosPacket = new PlayerMoveC2SPacket.PositionAndOnGround(nextPos.x, nextPos.y, nextPos.z, mc.player.isOnGround());
        packets.add(nextPosPacket);
        mc.getNetworkHandler().sendPacket(nextPosPacket);

        if ((limit.get() != Limit.NONE && forceLimit) && limitTicks == 0) return;

        PlayerMoveC2SPacket boundsPacket = new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, mc.player.isOnGround());
        packets.add(boundsPacket);
        mc.getNetworkHandler().sendPacket(boundsPacket);

        if (confirmTeleport) {
            teleportId++;

            if (sendExtraConfirmTeleport) {
                mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId - 1));
            }

            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId));

            posLooks.put(teleportId, new TimeVec3d(nextPos.x, nextPos.y, nextPos.z, System.currentTimeMillis()));

            if (sendExtraConfirmTeleport) {
                mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(teleportId + 1));
            }
        }
    }

    private Vec3d getBoundsVec(double x, double y, double z, Mode mode) {
        switch (mode) {
            case UP:
                return new Vec3d(mc.player.getX() + x, bounds.get() ? (strict.get() ? 255 : 256) : mc.player.getY() + 420, mc.player.getZ() + z);
            case PRESERVE:
                return new Vec3d(bounds.get() ? mc.player.getX() + randomHorizontal() : randomHorizontal(), strict.get() ? (Math.max(mc.player.getY(), 2D)) : mc.player.getY(), bounds.get() ? mc.player.getZ() + randomHorizontal() : randomHorizontal());
            case LIMITJITTER:
                return new Vec3d(mc.player.getX() + (strict.get() ? x : randomLimitedHorizontal()), mc.player.getY() + randomLimitedVertical(), mc.player.getZ() + (strict.get() ? z : randomLimitedHorizontal()));
            case BYPASS:
                if (bounds.get()) {
                    double rawY = y * 510;
                    return new Vec3d(mc.player.getX() + x, mc.player.getY() + ((rawY > ((PlayerUtils.getDimension() == Dimension.End) ? 127 : 255)) ? -rawY : (rawY < 1) ? -rawY : rawY), mc.player.getZ() + z);
                } else {
                    return new Vec3d(mc.player.getX() + (x == 0D ? (random.nextBoolean() ? -10 : 10) : x * 38), mc.player.getY() + y, mc.player.getX() + (z == 0D ? (random.nextBoolean() ? -10 : 10) : z * 38));
                }
            case OBSCURE:
                return new Vec3d(mc.player.getX() + randomHorizontal(), Math.max(1.5D, Math.min(mc.player.getY() + y, 253.5D)), mc.player.getZ() + randomHorizontal());
            default:
                return new Vec3d(mc.player.getX() + x, bounds.get() ? (strict.get() ? 1 : 0) : mc.player.getY() - 1337, mc.player.getZ() + z);
        }
    }

    private double randomHorizontal() {
        int randomValue = random.nextInt(bounds.get() ? 80 : (packetMode.get() == Mode.OBSCURE ? (ticksExisted % 2 == 0 ? 480 : 100) : 29000000)) + (bounds.get() ? 5 : 500);
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }

    public static double randomLimitedVertical() {
        int randomValue = random.nextInt(22);
        randomValue += 70;
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }

    public static double randomLimitedHorizontal() {
        int randomValue = random.nextInt(10);
        if (random.nextBoolean()) {
            return randomValue;
        }
        return -randomValue;
    }

    private double[] directionSpeed(double speed) {
        float forward = mc.player.forwardSpeed;
        float side = mc.player.sidewaysSpeed;
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

    private boolean checkCollisionBox() {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox()).iterator().hasNext();
    }

    private boolean onGround() {
        if (stopOnGround.get()) return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.01, 0)).iterator().hasNext();

        return true;
    }

    private boolean isPlayerMoving() {
        if (mc.options.keyJump.isPressed()) return true;
        if (mc.options.keyForward.isPressed()) return true;
        if (mc.options.keyBack.isPressed()) return true;
        if (mc.options.keyLeft.isPressed()) return true;
        if (mc.options.keyRight.isPressed()) return true;

        return false;
    }

    private boolean isMoving() {
        if (onlyOnMove.get()) {
            if (mc.options.keyJump.isPressed()) return true;
            if (mc.options.keySneak.isPressed()) return true;
            if (mc.options.keyForward.isPressed()) return true;
            if (mc.options.keyBack.isPressed()) return true;
            if (mc.options.keyLeft.isPressed()) return true;
            if (mc.options.keyRight.isPressed()) return true;

            return false;
        }

        return true;
    }

    private static class TimeVec3d extends Vec3d {
        private final long time;

        public TimeVec3d(double xIn, double yIn, double zIn, long time) {
            super(xIn, yIn, zIn);
            this.time = time;
        }

        public long getTime() {
            return time;
        }
    }
}
