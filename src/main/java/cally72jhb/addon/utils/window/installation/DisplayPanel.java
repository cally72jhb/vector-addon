package cally72jhb.addon.utils.window.installation;

import cally72jhb.addon.utils.window.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class DisplayPanel extends JPanel {
    private Font font;

    public DisplayPanel(int width, int height, Theme theme) {
        this.setFocusable(true);
        this.setBackground(theme.getDefaultBackgroundColor());

        Dimension size = new Dimension(width, height);

        this.setPreferredSize(size);
        this.setSize(size);

        this.font = new Font("", Font.PLAIN, 17);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
    }
}
