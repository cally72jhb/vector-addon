package cally72jhb.addon.system.titlescreen;

import cally72jhb.addon.gui.screens.TitleScreenEditor;
import cally72jhb.addon.gui.screens.TitleScreenElementScreen;
import cally72jhb.addon.gui.screens.TitleScreen;
import cally72jhb.addon.system.titlescreen.modules.*;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.utils.render.AlignmentX;
import meteordevelopment.meteorclient.utils.render.AlignmentY;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

import static cally72jhb.addon.utils.VectorUtils.mc;

public class TitleScreenManager extends System<TitleScreenManager> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEditor = settings.createGroup("Editor");

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the Title Screen.")
        .defaultValue(1)
        .min(1)
        .max(5)
        .sliderMin(1)
        .sliderMax(5)
        .build()
    );

    public final Setting<Double> factor = sgGeneral.add(new DoubleSetting.Builder()
        .name("factor")
        .description("How fast the background moves.")
        .defaultValue(8)
        .min(-50)
        .max(50)
        .sliderMin(-25)
        .sliderMax(25)
        .build()
    );

    public final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("The vertical offset of the background.")
        .defaultValue(1.75)
        .sliderMin(0.5)
        .sliderMax(2)
        .build()
    );

    public final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders the shadow of text.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
        .name("primary-color")
        .description("Primary color of text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    // Editor

    public final Setting<Integer> snappingRange = sgEditor.add(new IntSetting.Builder()
        .name("snapping-range")
        .description("Snapping range in editor.")
        .defaultValue(5)
        .min(0)
        .max(50)
        .build()
    );

    private final TitleScreenRenderer RENDERER = new TitleScreenRenderer();

    public final List<DefaultElement> elements = new ArrayList<>();
    public final ElementLayer bottomLeft, topRight;

    public boolean active;

    public final Runnable reset = () -> {
        align();
        elements.forEach(element -> {
            element.active = element.defaultActive;
            element.settings.forEach(group -> group.forEach(Setting::reset));
        });
    };

    public TitleScreenManager() {
        super("title-screen");

        // Bottom Left
        bottomLeft = new ElementLayer(RENDERER, elements, AlignmentX.Center, AlignmentY.Center, 0, 0);
        bottomLeft.add(new TextElement(this, "Singleplayer", PressableElement.Action.SINGLEPLAYER, true));
        bottomLeft.add(new TextElement(this, "Multiplayer", PressableElement.Action.MULTIPLAYER, true));
        bottomLeft.add(new TextElement(this, "Options", PressableElement.Action.OPTIONS, true));
        bottomLeft.add(new TextElement(this, "Quit", PressableElement.Action.QUIT, true));

        // Top Right
        topRight = new ElementLayer(RENDERER, elements, AlignmentX.Right, AlignmentY.Top, 50, 50);
        topRight.add(new SkinElement(this, "Accounts", PressableElement.Action.LOGIN, true));

        align();
    }

    private void align() {
        RENDERER.begin(1, 0, true);

        bottomLeft.align();
        topRight.align();

        RENDERER.end();
    }

    @EventHandler
    public void onRender(Render2DEvent event) {
        if (mc == null) return;
        if (mc.world != null && !(mc.currentScreen instanceof TitleScreenEditor)) return;

        RENDERER.begin(1, event.frameTime, false);

        for (DefaultElement element : elements) {
            if (element.active && (mc.currentScreen instanceof TitleScreen || mc.currentScreen instanceof TitleScreenElementScreen) || mc.currentScreen instanceof TitleScreenEditor) {
                element.update(RENDERER);
                element.render(RENDERER);
            }
        }

        RENDERER.end();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putBoolean("active", active);
        tag.put("settings", settings.toTag());

        NbtList modulesTag = new NbtList();
        for (DefaultElement module : elements) modulesTag.add(module.toTag());
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public TitleScreenManager fromTag(NbtCompound tag) {
        if (tag.contains("active")) active = tag.getBoolean("active");
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));

        if (tag.contains("modules")) {
            NbtList modulesTag = tag.getList("modules", 10);

            for (NbtElement t : modulesTag) {
                NbtCompound moduleTag = (NbtCompound) t;

                for (DefaultElement module : elements) {
                    if (module.name.equals(moduleTag.getString("name"))) {
                        module.fromTag(moduleTag);
                        break;
                    }
                }
            }
        }

        return super.fromTag(tag);
    }
}
