package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BobView extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disable = sgGeneral.add(new BoolSetting.Builder()
        .name("disable")
        .description("Disables bobing completely.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("How fast the animation goes.")
        .defaultValue(1)
        .sliderMin(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> horizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal")
        .description("How fast to bob from left to right.")
        .defaultValue(0.5)
        .sliderMin(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> vertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical")
        .description("How fast to bob up and down.")
        .defaultValue(0.2)
        .sliderMin(0)
        .sliderMax(0.5)
        .build()
    );

    private final Setting<Double> rotate = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate")
        .description("How fast to rotate your screen when walking.")
        .defaultValue(3)
        .sliderMin(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> shake = sgGeneral.add(new DoubleSetting.Builder()
        .name("shake")
        .description("How fast to shake up and down.")
        .defaultValue(5.0)
        .sliderMin(2.5)
        .sliderMax(7.5)
        .build()
    );

    public BobView() {
        super(Categories.Misc, "bob-view", "Changes the way your camara moves around with motion.");
    }

    // Getters

    public boolean shouldDisable() {
        return disable.get();
    }

    public float getSpeed() {
        return speed.get().floatValue();
    }

    public float getHorizontal() {
        return horizontal.get().floatValue();
    }

    public float getVertical() {
        return vertical.get().floatValue();
    }

    public float getRotate() {
        return rotate.get().floatValue();
    }

    public float getShake() {
        return shake.get().floatValue();
    }
}
