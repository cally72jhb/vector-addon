package cally72jhb.addon.utils.titlescreen;

import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public class ScreenRenderer {
    public double scale;
    public double delta;
    public boolean scaleOnly;
    public MatrixStack matrices;
    private final List<Runnable> postTasks = new ArrayList<>();

    public void begin(double scale, double frameDelta, boolean scaleOnly) {
        TextRenderer.get().begin(scale, scaleOnly, false);

        this.scaleOnly = scaleOnly;
        this.delta = frameDelta;
        this.scale = scale;
    }

    public void begin(MatrixStack matrices, double scale, double frameDelta, boolean scaleOnly) {
        TextRenderer.get().begin(scale, scaleOnly, false);

        this.matrices = matrices;
        this.scaleOnly = scaleOnly;
        this.delta = frameDelta;
        this.scale = scale;
    }

    public void end() {
        TextRenderer.get().end();

        for (Runnable runnable : postTasks) {
            runnable.run();
        }

        postTasks.clear();
    }

    public void text(String text, double x, double y, Color color) {
        TextRenderer.get().render(text, x, y, color, true);
    }

    public void text(String text, double scale, double x, double y, boolean big, Color color) {
        if (TextRenderer.get().isBuilding()) TextRenderer.get().end();
        TextRenderer.get().begin(scale, scaleOnly, big);
        TextRenderer.get().render(text, x, y, color, true);
        TextRenderer.get().end();
        TextRenderer.get().begin(this.scale, this.scaleOnly, false);
    }

    public double textWidth(String text) {
        return TextRenderer.get().getWidth(text);
    }

    public double textHeight() {
        return TextRenderer.get().getHeight();
    }

    public void addPostTask(Runnable runnable) {
        postTasks.add(runnable);
    }
}
