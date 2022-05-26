package cally72jhb.addon.utils.window.components.buttons;

import cally72jhb.addon.utils.window.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CustomTypeButton extends JButton implements MouseListener, MouseMotionListener {
    public ButtonType type;
    private final Theme theme;

    public CustomTypeButton(ButtonType type, Theme theme) {
        this.setOpaque(false);
        this.setBackground(null);
        this.addMouseListener(this);

        this.setUI(null);

        this.type = type;
        this.theme = theme;
    }

    public void setButtonType(ButtonType type) {
        this.type = type;
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int width = getWidth();
        int height = getHeight();

        Graphics2D graphics2D = (Graphics2D) graphics.create();

        graphics2D.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics2D.setColor(theme.getLightForegroundColor());

        Point start = new Point(width / 2 - 5, height / 2 - 5);
        Point end = new Point(width / 2 + 5, height / 2 + 5);

        switch (type) {
            case MINIMIZE:
                start = new Point(width / 2 - 5,height / 2);
                end = new Point(width / 2 + 5,height / 2);
                graphics2D.drawLine(start.x, start.y, end.x, end.y);
                break;
            case MAXIMIZE:
                graphics2D.drawRect(start.x, start.y, 10, 10);
                break;
            case CLOSE:
                graphics2D.drawLine(start.x, start.y, end.x, end.y);
                start = new Point(width / 2 + 5,height / 2 - 5);
                end = new Point(width / 2 - 5,height / 2 + 5);
                graphics2D.drawLine(start.x, start.y, end.x, end.y);
                break;
        }

        graphics2D.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent event) {

    }

    @Override
    public void mousePressed(MouseEvent event) {
        setBackground(theme.getDefaultButtonPressedColor());

        resetCursor();
    }

    @Override
    public void mouseReleased(MouseEvent event) {

    }

    @Override
    public void mouseEntered(MouseEvent event) {
        setBackground(theme.getDefaultButtonHoverColor());

        resetCursor();
    }

    @Override
    public void mouseExited(MouseEvent event) {
        setBackground(theme.getDefaultBackgroundColor());

        resetCursor();
    }

    @Override
    public void mouseDragged(MouseEvent event) {

    }

    @Override
    public void mouseMoved(MouseEvent event) {
        resetCursor();
    }

    // Utils

    private void resetCursor() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
}
