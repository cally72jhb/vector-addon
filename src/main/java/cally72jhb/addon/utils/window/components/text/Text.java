package cally72jhb.addon.utils.window.components.text;

import java.awt.*;

public record Text(String text, Color color, int type) {
    public String getText() {
        return this.text;
    }

    public Color getColor() {
        return this.color;
    }

    public int getType() {
        return this.type;
    }
}
