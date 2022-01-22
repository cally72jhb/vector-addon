package cally72jhb.addon.system.titlescreen;

import cally72jhb.addon.system.titlescreen.modules.DefaultElement;
import meteordevelopment.meteorclient.utils.render.AlignmentX;
import meteordevelopment.meteorclient.utils.render.AlignmentY;

import java.util.ArrayList;
import java.util.List;

public class ElementLayer {
    private final TitleScreenRenderer renderer;
    private final List<DefaultElement> allElements;
    private final List<DefaultElement> elements;

    private final AlignmentX xAlign;
    private final AlignmentY yAlign;

    private final int xOffset, yOffset;

    public ElementLayer(TitleScreenRenderer renderer, List<DefaultElement> allElements, AlignmentX xAlign, AlignmentY yAlign, int xOffset, int yOffset) {
        this.renderer = renderer;
        this.allElements = allElements;
        this.elements = new ArrayList<>();
        this.xAlign = xAlign;
        this.yAlign = yAlign;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public void add(DefaultElement element) {
        allElements.add(element);
        elements.add(element);

        element.settings.registerColorSettings(null);
    }

    public void align() {
        double x = xOffset * (xAlign == AlignmentX.Right ? -1 : 1);
        double y = yOffset * (yAlign == AlignmentY.Bottom ? -1 : 1);

        for (DefaultElement element : elements) {
            element.update(renderer);

            element.box.x = xAlign;
            element.box.y = yAlign;
            element.box.xOffset = (int) Math.round(x);
            element.box.yOffset = (int) Math.round(y);

            if (yAlign == AlignmentY.Bottom) y -= element.box.height;
            else y += element.box.height;
        }
    }
}
