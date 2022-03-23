package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.*;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.*;

public class BlinkPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> interfere = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-modules")
        .description("Disables modules that could interfere with this module.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Module>> blinkModules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("modules")
        .description("Which modules to disable while blinking.")
        .defaultValue(getDefaultModules())
        .build()
    );

    private final Setting<Boolean> keepAlive = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-alive")
        .description("Cancels keep-alive packets sent to the server.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> blink = sgGeneral.add(new BoolSetting.Builder()
        .name("blink-on-rubberband")
        .description("Blinks on rubberband.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disable = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-rubberband")
        .description("Disables on rubberband.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only binks on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> jitter = sgGeneral.add(new BoolSetting.Builder()
        .name("jitter")
        .description("Randomly sends some packets to the server.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> randomness = sgGeneral.add(new IntSetting.Builder()
        .name("randomness")
        .description("What randomness to use for jitter.")
        .defaultValue(10)
        .min(5)
        .sliderMin(5)
        .sliderMax(50)
        .visible(jitter::get)
        .build()
    );

    private final Setting<Integer> maxTicks = sgGeneral.add(new IntSetting.Builder()
        .name("max-blink-ticks")
        .description("The maximum ticks you won't send any packet.")
        .defaultValue(50)
        .min(0)
        .sliderMin(25)
        .sliderMax(100)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the path you went while using blink.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> player = sgRender.add(new BoolSetting.Builder()
        .name("player")
        .description("Renders a fake player at the position visible for everybody else.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the trail.")
        .defaultValue(new SettingColor(140, 245, 165))
        .visible(render::get)
        .build()
    );

    private final Setting<Integer> maxSections = sgRender.add(new IntSetting.Builder()
        .name("max-sections")
        .description("The maximum number of sections.")
        .defaultValue(1000)
        .min(50)
        .max(5000)
        .sliderMin(500)
        .sliderMax(1500)
        .visible(render::get)
        .build()
    );

    private final Setting<Double> sectionLength = sgRender.add(new DoubleSetting.Builder()
        .name("section-length")
        .description("The section length in blocks.")
        .defaultValue(0.5)
        .min(0.01)
        .sliderMax(1)
        .visible(render::get)
        .build()
    );

    private ArrayList<Module> modules;

    private static final List<FakePlayerEntity> fakePlayers = new ArrayList<>();
    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private Random random;
    private int timer;
    private int ticks;
    private int delay;

    private final Pool<Section> sectionPool = new Pool<>(Section::new);
    private final Queue<Section> sections = new ArrayDeque<>();

    private Section section;

    public BlinkPlus() {
        super(Categories.Movement, "blink-plus", "Allows you to essentially teleport while suspending motion updates.");
    }

    @Override
    public void onActivate() {
        modules = new ArrayList<>();
        section = sectionPool.get();
        section.set1();

        random = new Random();

        timer = 0;
        ticks = 0;
        delay = 0;

        clear();

        if (player.get()) {
            FakePlayerEntity fakePlayer = new FakePlayerEntity(mc.player, mc.getSession().getProfile().getName(), mc.player.getHealth(), true);
            fakePlayers.add(fakePlayer);
            fakePlayer.spawn();
        }

        if (interfere.get() && !blinkModules.get().isEmpty()) {
            for (Module module : blinkModules.get()) {
                if (module.isActive()) {
                    module.toggle();
                    modules.add(module);
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.world == null || mc.player == null) return;

        clear();

        synchronized (packets) {
            packets.forEach(packet -> mc.getNetworkHandler().sendPacket(packet));
            packets.clear();
        }

        for (Section section : sections) sectionPool.free(section);
        sections.clear();

        for (Module module : modules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        timer++;
        ticks++;
        delay--;

        if ((!onlyOnGround.get() || mc.player.isOnGround()) || ticks >= maxTicks.get()) {
            if (mc.player.isOnGround()) clearPosLook();
        }

        if (render.get() && isFarEnough(section.x1, section.y1, section.z1)) {
            section.set2();

            if (sections.size() >= maxSections.get()) {
                Section section = sections.poll();
                if (section != null) sectionPool.free(section);
            }

            sections.add(section);
            section = sectionPool.get();
            section.set1();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (delay > 0) return;
        if (event.packet instanceof PlayerMoveC2SPacket) {
            event.cancel();

            synchronized (packets) {
                PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;
                PlayerMoveC2SPacket prev = packets.size() == 0 ? null : packets.get(packets.size() - 1);

                if (prev != null &&
                    packet.isOnGround() == prev.isOnGround() &&
                    packet.getYaw(-1) == prev.getYaw(-1) &&
                    packet.getPitch(-1) == prev.getPitch(-1) &&
                    packet.getX(-1) == prev.getX(-1) &&
                    packet.getY(-1) == prev.getY(-1) &&
                    packet.getZ(-1) == prev.getZ(-1)
                ) return;

                packets.add(packet);
            }
        } else if (keepAlive.get() && event.packet instanceof KeepAliveC2SPacket) {
            event.cancel();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (blink.get()) {
                clearPosLook();
            } else if (disable.get()) {
                info("Rubberband detected! Disabling...");
                toggle();
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        int iLast = -1;

        for (Section section : sections) {
            if (iLast == -1) {
                iLast = event.renderer.lines.vec3(section.x1, section.y1, section.z1).color(color.get()).next();
            }

            int i = event.renderer.lines.vec3(section.x2, section.y2, section.z2).color(color.get()).next();
            event.renderer.lines.line(iLast, i);
            iLast = i;
        }
    }

    @Override
    public String getInfoString() {
        return String.format("%.1f", timer / 20f);
    }

    private void clear() {
        if (!fakePlayers.isEmpty()) {
            fakePlayers.forEach(FakePlayerEntity::despawn);
            fakePlayers.clear();
        }
    }

    private void clearPosLook() {
        if (jitter.get() && !packets.isEmpty() && random.nextInt(randomness.get()) == 0) {
            delay = 10;
            synchronized (packets) {
                packets.forEach(packet -> mc.getNetworkHandler().sendPacket(packet));
                packets.clear();
            }

            if (player.get()) {
                clear();

                FakePlayerEntity fakePlayer = new FakePlayerEntity(mc.player, mc.getSession().getProfile().getName(), mc.player.getHealth(), true);
                fakePlayers.add(fakePlayer);
                fakePlayer.spawn();
            }
        }
    }

    private boolean isFarEnough(double x, double y, double z) {
        return Math.abs(mc.player.getX() - x) >= sectionLength.get() || Math.abs(mc.player.getY() - y) >= sectionLength.get() || Math.abs(mc.player.getZ() - z) >= sectionLength.get();
    }

    private static List<Module> getDefaultModules() {
        List<Module> modules = new ArrayList<>(5);
        modules.add(Modules.get().get(KillAura.class));
        modules.add(Modules.get().get(CrystalAura.class));
        modules.add(Modules.get().get(AnchorAura.class));
        modules.add(Modules.get().get(BedAura.class));
        modules.add(Modules.get().get(Surround.class));
        return modules;
    }

    private class Section {
        public float x1, y1, z1;
        public float x2, y2, z2;

        public void set1() {
            x1 = (float) mc.player.getX();
            y1 = (float) mc.player.getY();
            z1 = (float) mc.player.getZ();
        }

        public void set2() {
            x2 = (float) mc.player.getX();
            y2 = (float) mc.player.getY();
            z2 = (float) mc.player.getZ();
        }

        public void render(Render3DEvent event) {
            event.renderer.line(x1, y1, z1, x2, y2, z2, color.get());
        }
    }
}
