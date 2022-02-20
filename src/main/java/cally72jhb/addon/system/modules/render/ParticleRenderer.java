package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ParticleRenderer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPop = settings.createGroup("Popping");

    private final Setting<Boolean> velocity = sgGeneral.add(new BoolSetting.Builder()
        .name("modify-velocity")
        .description("Modifies the particles velocity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> xModifier = sgGeneral.add(new DoubleSetting.Builder()
        .name("x-modifier")
        .description("With what the x velocity should be divided.")
        .defaultValue(5)
        .sliderMin(0.25)
        .sliderMax(1.5)
        .visible(velocity::get)
        .build()
    );

    private final Setting<Double> yModifier = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-modifier")
        .description("With what the y velocity should be divided.")
        .defaultValue(4.5)
        .sliderMin(0.25)
        .sliderMax(1.5)
        .visible(velocity::get)
        .build()
    );

    private final Setting<Double> zModifier = sgGeneral.add(new DoubleSetting.Builder()
        .name("z-modifier")
        .description("With what the z velocity should be divided.")
        .defaultValue(5)
        .sliderMin(0.25)
        .sliderMax(1.5)
        .visible(velocity::get)
        .build()
    );

    private final Setting<Boolean> modifyLifeTime = sgGeneral.add(new BoolSetting.Builder()
        .name("modify-life-time")
        .description("Modifies the particles life time.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxTicks = sgGeneral.add(new IntSetting.Builder()
        .name("max-ticks")
        .description("The maximum ticks the particles will exist.")
        .defaultValue(500)
        .min(0)
        .sliderMin(200)
        .sliderMax(750)
        .visible(modifyLifeTime::get)
        .build()
    );


    // Popping


    private final Setting<Integer> amount = sgPop.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of spawned particles.")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Double> size = sgPop.add(new DoubleSetting.Builder()
        .name("size")
        .description("The size of the spawned particles.")
        .defaultValue(0.5)
        .min(0.1)
        .sliderMin(1)
        .sliderMax(1.5)
        .build()
    );

    private final Setting<Integer> randomness = sgPop.add(new IntSetting.Builder()
        .name("randomness")
        .description("How often the second color should be used.")
        .defaultValue(5)
        .min(1)
        .sliderMin(2)
        .sliderMax(10)
        .build()
    );

    private final Setting<SettingColor> firstColor = sgPop.add(new ColorSetting.Builder()
        .name("first-color")
        .description("The first color the particles should have.")
        .defaultValue(new SettingColor(140, 245, 165))
        .build()
    );

    private final Setting<SettingColor> secondColor = sgPop.add(new ColorSetting.Builder()
        .name("second-color")
        .description("The second color the particles should have.")
        .defaultValue(new SettingColor(140, 245, 235))
        .build()
    );

    public ParticleRenderer() {
        super(VectorAddon.Misc, "particle-renderer", "Renders a custom poping animation when a player pops a totem.");
    }

    public boolean shouldModifyVelocity() {
        return velocity.get();
    }

    public double getXModifier() {
        return xModifier.get();
    }

    public double getYModifier() {
        return yModifier.get();
    }

    public double getZModifier() {
        return zModifier.get();
    }

    public boolean shouldModifyLifeTime() {
        return modifyLifeTime.get();
    }

    public int getMaxLifeTicks() {
        return maxTicks.get();
    }

    public int getRandomness() {
        return randomness.get();
    }

    public int getAmount() {
        return amount.get();
    }

    public double getParticleSize() {
        return size.get();
    }

    public Color getFirstColor() {
        return firstColor.get();
    }

    public Color getSecondColor() {
        return secondColor.get();
    }
}
