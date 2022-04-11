package cally72jhb.addon.utils.window.components.buttons;

import cally72jhb.addon.utils.window.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CustomMenuButton extends JButton implements MouseListener, MouseMotionListener, MouseWheelListener {
    private final String[] defaultMenu;
    private String[] menu;
    private String selected;

    private final int shown;
    private int scroll;

    private boolean extended;

    private final Font font;
    private final Theme theme;
    private final Toolkit toolkit;

    public CustomMenuButton(String[] menu, Theme theme, int shown) {
        this.setOpaque(false);
        this.setBackground(null);

        this.menu = menu;
        this.selected = menu.length > 0 && menu[0] != null ? menu[0] : null;

        this.defaultMenu = menu.clone();
        this.shown = shown;
        this.theme = theme;

        this.scroll = 0;
        this.extended = false;

        this.font = new Font("", Font.PLAIN, 16);
        this.toolkit = Toolkit.getDefaultToolkit();

        updateSize();

        this.setVisible(true);
        this.setBorder(null);

        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        this.addMouseMotionListener(this);
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    // Rendering

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        int width = getWidth() - 1;
        int height = (int) (getFontMetrics().getHeight() * 1.5625) - 1;

        Graphics2D graphics2D = (Graphics2D) graphics.create();

        graphics2D.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics2D.setFont(font);

        if (selected != null && !selected.isEmpty()) {
            graphics2D.setColor(theme.getLightForegroundColor());
            graphics2D.fillRect(0, 0, width, getHeight());

            graphics2D.setColor(theme.getLightBackgroundColor());
            graphics2D.fillRect(0, 0, width, height);

            graphics2D.setColor(theme.getDefaultForegroundColor());
            graphics2D.drawString(selected, width / 2 - getFontMetrics().stringWidth(selected) / 2, (int) (getFontMetrics().getHeight() * 1.5625 - getFontMetrics().getHeight() / 2));
            graphics2D.setColor(theme.getDefaultButtonPressedColor());
            graphics2D.drawRect(0, 0, width, height);
        }

        if (extended) {
            int element = 0;
            int y = height * 2;

            for (int i = scroll; i < menu.length; i++) {
                String string = menu[i];

                if (string != null && !string.isEmpty() && element < shown + 1) {
                    graphics2D.setColor(theme.getDefaultForegroundColor());
                    graphics2D.drawString(string, width / 2 - getFontMetrics().stringWidth(string) / 2, (y - getFontMetrics().getHeight() / 2));

                    graphics2D.setColor(theme.getDefaultButtonPressedColor());
                    graphics2D.drawRect(0, y - height, width, height);

                    y += height;
                    element++;
                }
            }
        }

        graphics2D.dispose();
    }

    // Mouse Events

    @Override
    public void mouseClicked(MouseEvent event) {
        setBackground(theme.getDefaultButtonHoverColor());

        if (extended) {
            int x = event.getX();
            int y = event.getY();

            int width = getWidth();
            int height = (int) (getFontMetrics().getHeight() * 1.5625);

            int yPos = height * 2;

            for (int i = scroll; i < menu.length; i++) {
                if (x > 0 && x < width && y > yPos - height && y < yPos && i >= 0) {
                    menu = defaultMenu.clone();

                    selected = menu[i];
                }

                yPos += height;
            }
        }

        extended = !extended;

        updateSize();
    }

    @Override
    public void mousePressed(MouseEvent event) {

    }

    @Override
    public void mouseReleased(MouseEvent event) {

    }

    @Override
    public void mouseEntered(MouseEvent event) {

    }

    @Override
    public void mouseExited(MouseEvent event) {

    }

    @Override
    public void mouseDragged(MouseEvent event) {

    }

    @Override
    public void mouseMoved(MouseEvent event) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    // Mouse Wheel Event

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        if (menu.length > shown && extended) {
            scroll += event.getUnitsToScroll() > 1 ? 1 : -1;

            if (scroll < 0) scroll = 0;
            if (scroll > menu.length - shown) scroll = menu.length - shown;

            repaint();
        }
    }

    // Utils

    public void hide() {
        this.extended = false;
    }

    public void updateSize() {
        int preferredWidth = Math.max(0, getFontMetrics().stringWidth(selected));
        for (String string : menu) preferredWidth = Math.max(preferredWidth, getFontMetrics().stringWidth(string));

        int width = (int) (preferredWidth * 1.25);
        int height = (int) ((getFontMetrics().getHeight() * 1.5625 - (extended ? 1.5625 : 0.25)) * (extended ? Math.min(menu.length, shown) + 1 : 1));

        Dimension size = new Dimension(width, height);

        setPreferredSize(size);
        setSize(size);

        repaint();
    }

    private FontMetrics getFontMetrics() {
        return toolkit.getFontMetrics(font);
    }

    // Getter

    public String getSelected() {
        return selected;
    }
}
