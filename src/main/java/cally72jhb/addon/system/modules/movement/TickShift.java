package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TickShift extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> ticks = sgGeneral.add(new IntSetting.Builder()
        .name("tick-duration")
        .description("The duration of the timer phase.")
        .defaultValue(50)
        .sliderMin(25)
        .sliderMax(75)
        .min(1)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("How much to speed up the game when in the timer phase.")
        .defaultValue(2)
        .sliderMin(1.5)
        .sliderMax(15)
        .min(1)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Whether or not to tick-shift in the air.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ActiveWhen> activeWhen = sgGeneral.add(new EnumSetting.Builder<ActiveWhen>()
        .name("active-when")
        .description("When the module should be active.")
        .defaultValue(ActiveWhen.Always)
        .build()
    );

    private final Setting<RubberbandMode> rubberbandMode = sgGeneral.add(new EnumSetting.Builder<RubberbandMode>()
        .name("rubberband-mode")
        .description("What to do when rubberbanding.")
        .defaultValue(RubberbandMode.Disable)
        .build()
    );

    private final Setting<Boolean> rubberbandInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("rubberband-info")
        .description("Informs you when you got rubberbanded.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> messageCooldown = sgGeneral.add(new IntSetting.Builder()
        .name("message-cooldown")
        .description("How long to wait befor informing you another time.")
        .defaultValue(500)
        .sliderMin(450)
        .sliderMax(750)
        .min(0)
        .visible(rubberbandInfo::get)
        .build()
    );

    private final Setting<Double> maxTimerDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-timer-distance")
        .description("The maximum distance in which you can timer.")
        .defaultValue(5)
        .sliderMin(1.5)
        .sliderMax(10)
        .min(0)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render your starting position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> accurate = sgRender.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Whether or not to render the position accurate.")
        .defaultValue(true)
        .visible(render::get)
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
        .description("The side color of the start position rendering.")
        .defaultValue(new SettingColor(255, 255, 255, 10))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the start position rendering.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> tracerColor = sgRender.add(new ColorSetting.Builder()
        .name("tracer-color")
        .description("The color of the tracer.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(render::get)
        .build()
    );

    private int tick;
    private int messageTicks;
    private Vec3d startVec;

    public TickShift() {
        super(Categories.Movement, "tick-shift", "Allows to essentially timer for a certain time.");
    }

    @Override
    public void onActivate() {
        tick = 0;
        messageTicks = messageCooldown.get();

        startVec = null;
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @Override
    public String getInfoString() {
        return isMoving() && tick > 0 ? "[" + tick + "]" : null;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        messageTicks++;

        Modules.get().get(Timer.class).setOverride(Timer.OFF);

        if (tick <= 0) tick = 0;

        if ((activeWhen.get() == ActiveWhen.Sneaking && mc.player.isSneaking())
            || (activeWhen.get() == ActiveWhen.NotSneaking && !mc.player.isSneaking())
            || (onlyOnGround.get() && mc.player.isOnGround())) {

            if (isMoving() && (maxTimerDistance.get() == 0 || maxTimerDistance.get() != 0 && startVec != null && VectorUtils.distance(mc.player.getPos(), startVec) <= maxTimerDistance.get())) {
                if (tick > 0) {
                    Modules.get().get(Timer.class).setOverride(timer.get());
                    tick--;
                }
            }

            if (!isMoving()) startVec = mc.player.getPos();
        }

        if (!isMoving()) tick++;
        if (tick >= ticks.get()) tick = ticks.get();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && rubberbandMode.get() != RubberbandMode.None) {
            if (rubberbandInfo.get() && (messageCooldown.get() == 0 || messageTicks >= messageCooldown.get())) {
                info("Rubberband detected! Disabling...");
                messageTicks = 0;
            }

            if (rubberbandMode.get() == RubberbandMode.Disable) {
                toggle();
            } else if (rubberbandMode.get() == RubberbandMode.Toggle) {
                tick = 0;
                startVec = null;
            }
        }
    }

    // Render

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            if (startVec != null && !accurate.get()) {
                event.renderer.box(new BlockPos(startVec.x, startVec.y, startVec.z), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, startVec.getX() + 0.5, startVec.getY() + 0.5, startVec.getZ() + 0.5, tracerColor.get());
            } else if (startVec != null && accurate.get()) {
                event.renderer.box(startVec.x - 0.25, startVec.y, startVec.z - 0.25, startVec.x + 0.25, startVec.y + 1.5, startVec.z + 0.25, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, startVec.getX(), startVec.getY() + 0.75, startVec.getZ(), tracerColor.get());
            }
        }
    }

    // Utils

    private boolean isMoving() {
        return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
    }

    // Constants

    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }

    public enum RubberbandMode {
        None,
        Disable,
        Toggle
    }
}
