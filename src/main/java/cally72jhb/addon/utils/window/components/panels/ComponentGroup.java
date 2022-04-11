package cally72jhb.addon.utils.window.components.panels;

import javax.swing.*;

public class ComponentGroup extends JPanel {
    public ComponentGroup() {
        this.setBackground(null);
        this.setBorder(null);
        this.setOpaque(false);
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }
}
