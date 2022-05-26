package cally72jhb.addon.system.hud;

import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.MetricsData;

public class FPSGraphHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBox = settings.createGroup("FPS Box");

    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("How to render the fps graph.")
        .defaultValue(RenderMode.Middle)
        .build()
    );

    private final Setting<Double> outherOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("outher-offset")
        .description("The offset of the outside box.")
        .defaultValue(0.5)
        .min(0)
        .max(2)
        .sliderMin(0)
        .sliderMax(1.5)
        .build()
    );

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
        .name("width")
        .description("The width of the box.")
        .defaultValue(175)
        .min(60)
        .max(1000)
        .sliderMin(150)
        .sliderMax(200)
        .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("The height of the box.")
        .defaultValue(75)
        .min(20)
        .max(500)
        .sliderMin(50)
        .sliderMax(125)
        .build()
    );

    private final Setting<Boolean> smoothen = sgGeneral.add(new BoolSetting.Builder()
        .name("smoothen")
        .description("Smoothens the graph.")
        .defaultValue(false)
        .build()
    );


    // Box


    private final Setting<Boolean> averageFPSLine = sgBox.add(new BoolSetting.Builder()
        .name("average-fps-line")
        .description("Renders a line at the average fps.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lines = sgBox.add(new BoolSetting.Builder()
        .name("lines")
        .description("Renders lines instead of filling.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> yOffset = sgBox.add(new IntSetting.Builder()
        .name("y-offset")
        .description("How much to offset both lines from the graphs average line.")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .sliderMax(15)
        .visible(() -> renderMode.get() == RenderMode.Middle)
        .build()
    );

    private final Setting<FrameType> frameType = sgBox.add(new EnumSetting.Builder<FrameType>()
        .name("frame-type")
        .description("What frame to display around the box.")
        .defaultValue(FrameType.Default)
        .build()
    );

    private final Setting<Integer> semiLength = sgBox.add(new IntSetting.Builder()
        .name("semi-length")
        .description("How long the frame should be.")
        .defaultValue(75)
        .min(0)
        .sliderMin(50)
        .sliderMax(125)
        .visible(() -> frameType.get() == FrameType.Semi || frameType.get() == FrameType.SemiBoth)
        .build()
    );

    private final Setting<Integer> semiExtraLength = sgBox.add(new IntSetting.Builder()
        .name("semi-extra-length")
        .description("How long the extra frame should be.")
        .defaultValue(50)
        .min(0)
        .sliderMin(25)
        .sliderMax(100)
        .visible(() -> frameType.get() == FrameType.SemiExtra || frameType.get() == FrameType.SemiBoth)
        .build()
    );

    private final Setting<Boolean> background = sgBox.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBox.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(0, 0, 0, 100))
        .visible(background::get)
        .build()
    );

    private final Setting<SettingColor> foregroundColor = sgBox.add(new ColorSetting.Builder()
        .name("foreground-color")
        .description("Color of the foreground.")
        .defaultValue(new SettingColor(225, 225, 225, 255))
        .build()
    );

    private final Setting<SettingColor> frameColor = sgBox.add(new ColorSetting.Builder()
        .name("frame-color")
        .description("Color of the frame.")
        .defaultValue(new SettingColor(225, 225, 225, 255))
        .visible(() -> frameType.get() != FrameType.None)
        .build()
    );

    public FPSGraphHud(HUD hud) {
        super(hud, "fps-graph", "Renders a fps graph of the last 240 ticks.", true);
    }

    double textX = box.getX();
    double textY = box.getY();

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(width.get(), height.get());

        textX = box.getX();
        textY = box.getY() + box.height - outherOffset.get() * 20;
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        double height = box.height;
        double width = box.width;

        Renderer2D.COLOR.begin();

        if (background.get()) {
            double offset = outherOffset.get() * 5;
            Renderer2D.COLOR.quad(x - offset, y - offset - 1, box.width + offset * 2 + 1, box.height + offset * 2, backgroundColor.get());
        }

        switch (frameType.get()) {
            case Semi, SemiExtra, SemiBoth ->  {
                double length = semiLength.get() + 1;
                double extraLength = semiExtraLength.get() + 1;

                if (frameType.get() == FrameType.Semi || frameType.get() == FrameType.SemiBoth) {
                    // Vertical

                    double tempLength1 = Math.min(length, height / 2);
                    double tempLength2 = length >= height / 2 ? tempLength1 + 1 : tempLength1;

                    Renderer2D.COLOR.quad(
                        x - 1,
                        y + height / 2 - tempLength1,
                        1,
                        tempLength1 * 2 - 1,
                        frameColor.get()
                    );

                    Renderer2D.COLOR.quad(
                        x + width,
                        y + height / 2 - tempLength2,
                        1,
                        tempLength2 * 2 - 1,
                        frameColor.get()
                    );

                    // Horizontal

                    if (length * 2 > height) {
                        length = length - height / 2 <= 0 ? 1 : length - height / 2 + 1;
                        length = length > width / 2 ? width / 2 + 1 : length;

                        // Top

                        Renderer2D.COLOR.quad(
                            x - 1,
                            y - 1,
                            length,
                            1,
                            frameColor.get()
                        );

                        Renderer2D.COLOR.quad(
                            x - 1,
                            y + height - 1,
                            length,
                            1,
                            frameColor.get()
                        );

                        // Bottom

                        Renderer2D.COLOR.quad(
                            x + width,
                            y - 1,
                            -length,
                            1,
                            frameColor.get()
                        );

                        Renderer2D.COLOR.quad(
                            x + width,
                            y + height - 1,
                            -length,
                            1,
                            frameColor.get()
                        );
                    }
                }

                if (frameType.get() == FrameType.SemiExtra || frameType.get() == FrameType.SemiBoth) {
                    // Horizontal

                    if (extraLength * 2 > height) {
                        extraLength = extraLength - height / 2 <= 0 ? 1 : extraLength - height / 2 + 1;
                        extraLength = extraLength > width / 2 ? width / 2 + 1 : extraLength;

                        // Top

                        Renderer2D.COLOR.quad(
                            x + width / 2 - extraLength - 1,
                            y - 1,
                            extraLength * 2,
                            1,
                            frameColor.get()
                        );

                        // Bottom

                        Renderer2D.COLOR.quad(
                            x + width / 2 - extraLength - 1,
                            y + height - 1,
                            extraLength * 2,
                            1,
                            frameColor.get()
                        );
                    }
                }
            }

            case Default -> {
                Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, 1, frameColor.get());
                Renderer2D.COLOR.quad(x - 1, y + height - 1, width + 2, 1, frameColor.get());
                Renderer2D.COLOR.quad(x - 1, y, 1, height, frameColor.get());
                Renderer2D.COLOR.quad(x + width, y, 1, height, frameColor.get());
            }
        }

        if (averageFPSLine.get()) {
            Renderer2D.COLOR.quad(x - 1, y + height / 2 - 1, width + 1, 1, frameColor.get());
        }

        MetricsData metrics = mc.getMetricsData();

        int start = metrics.getStartIndex();
        int end = metrics.getCurrentIndex();
        long[] samples = metrics.getSamples();

        if (smoothen.get()) {
            long[] tempSamples = new long[samples.length];

            tempSamples[0] = samples[0];

            for (int i = 1; i < samples.length; i++) {
                long average = samples[i];

                average += samples[i - 1];
                if (i < samples.length - 1) average += samples[i + 1];

                average /= i < samples.length - 1 ? 3 : 2;

                tempSamples[i] = average;
            }

            samples = tempSamples.clone();
        }

        x++;
        int displayed = Math.max(0, samples.length - (int) width);
        double factor = (width / (samples.length > width ? width : samples.length));

        if (renderMode.get() == RenderMode.Default) {
            if (lines.get()) {
                for (int i = (start + displayed) % 240; i != end; i = (i + 1) % 240) {
                    if (i < samples.length - 1 && x <= box.getX() + width) {
                        int t1 = (int) (((double) samples[i] / (1000000000L / 60)) * 30);
                        int t2 = (int) (((double) samples[i + 1] / (1000000000L / 60)) * 30);

                        double x1 = x;
                        double y1 = y + height - 2 - (Math.abs(t1 > height - 4 ? height - 4 : t1));
                        double x2 = x + factor;
                        double y2 = y + height - 2 - (Math.abs(t2 > height - 4 ? height - 4 : t2));

                        Renderer2D.COLOR.line(x1, y1, x2, y2, foregroundColor.get());

                        x += factor;
                    }
                }
            } else {
                for (int i = (start + displayed) % 240; i != end; i = (i + 1) % 240) {
                    if (i < samples.length && x <= box.getX() + width) {
                        int t = (int) (((double) samples[i] / (1000000000L / 60)) * 30);

                        double tempX = x;
                        double tempY = y + height - 2;
                        double tempWidth = x + factor + 1 >= box.getX() + width ? x - (box.getX() + width) - 1 : factor;
                        double tempHeight = -(t > height - 3 ? height - 3 : t <= 0 ? 0 : t > height - 3 ? height - 3 : t);

                        if (averageFPSLine.get() && y + height / 2 + 1 >= tempY + tempHeight) {
                            Renderer2D.COLOR.quad(tempX, tempY, tempWidth, -height / 2 + 3, foregroundColor.get());
                            Renderer2D.COLOR.quad(tempX, y + height / 2 - 2, tempWidth, tempHeight + height / 2, foregroundColor.get());
                        } else {
                            Renderer2D.COLOR.quad(tempX, tempY, tempWidth, tempHeight, foregroundColor.get());
                        }

                        x += factor;
                    }
                }
            }
        } else {
            double offset = ((double) yOffset.get()) + 0.01;

            if (lines.get()) {
                for (int i = (start + displayed) % 240; i != end; i = (i + 1) % 240) {
                    if (i < samples.length - 1 && x <= box.getX() + width) {
                        int t1 = (int) (((double) samples[i] / (1000000000L / 60)) * 30);
                        int t2 = (int) (((double) samples[i + 1] / (1000000000L / 60)) * 30);

                        double h1 = Math.max(Math.min(t1 + offset, height - 4), 0);
                        double h2 = Math.max(Math.min(t2 + offset, height - 4), 0);

                        double x1 = x;
                        double y1 = y + height - 2 - h1;
                        double x2 = x + factor;
                        double y2 = y + height - 2 - h2;

                        double y3 = y + h1 + 2;
                        double y4 = y + h2 + 2;

                        Renderer2D.COLOR.line(x1, y1, x2, y2, foregroundColor.get());
                        Renderer2D.COLOR.line(x1, y3, x2, y4, foregroundColor.get());

                        x += factor;
                    }
                }
            } else {
                for (int i = (start + displayed) % 240; i != end; i = (i + 1) % 240) {
                    if (i < samples.length && x <= box.getX() + width) {
                        int t = (int) (((double) samples[i] / (1000000000L / 60)) * 30);

                        double tempX = x;
                        double tempY = y + height / 2;
                        double tempWidth = x + factor + 1 >= box.getX() + width ? x - (box.getX() + width) - 1 : factor;
                        double tempHeight = -(Math.max(Math.min((double) t / 2 + yOffset.get(), height - 2), 0)) / 2;

                        if (averageFPSLine.get() && y + height / 2 + 1 >= tempY + tempHeight) {
                            Renderer2D.COLOR.quad(tempX, tempY - 2, tempWidth, tempHeight + 1.9, foregroundColor.get());
                            Renderer2D.COLOR.quad(tempX, tempY + 1, tempWidth, -tempHeight - 2, foregroundColor.get());
                        } else {
                            Renderer2D.COLOR.quad(tempX, tempY, tempWidth, tempHeight + 0.1, foregroundColor.get());
                            Renderer2D.COLOR.quad(tempX, tempY, tempWidth, -tempHeight - 0.1, foregroundColor.get());
                        }

                        x += factor;
                    }
                }
            }
        }

        Renderer2D.COLOR.render(null);
    }

    // Enums

    public enum RenderMode {
        Default,
        Middle
    }

    public enum FrameType {
        None,
        Default,
        Semi,
        SemiExtra,
        SemiBoth
    }
}
