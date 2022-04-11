package cally72jhb.addon.utils.window.components.buttons;

import cally72jhb.addon.utils.window.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CustomTextButton extends JButton implements MouseListener, MouseMotionListener {
    private final String text;

    private int width;
    private int height;

    private final Font font;
    private final Theme theme;
    private final Toolkit toolkit;

    private Runnable clickAction;

    public CustomTextButton(String text, Theme theme, Font font, int width, int height) {
        this.setOpaque(false);
        this.setBackground(null);

        this.text = text;
        this.theme = theme;

        this.width = width;
        this.height = height;

        this.font = font != null ? font : new Font("", Font.PLAIN, 14);
        this.toolkit = Toolkit.getDefaultToolkit();

        updateSize();

        this.setVisible(true);
        this.setBorder(null);

        this.addMouseListener(this);
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

        Graphics2D graphics2D = (Graphics2D) graphics.create();

        graphics2D.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics2D.setFont(font);

        if (text != null && !text.isEmpty()) {
            graphics2D.setColor(theme.getLightForegroundColor());
            graphics2D.fillRect(0, 0, getWidth() - 1, getHeight());

            graphics2D.setColor(theme.getDefaultForegroundColor());
            graphics2D.drawString(text, getWidth() / 2 - getFontMetrics().stringWidth(text) / 2, (int) (getFontMetrics().getHeight() * 1.5625 - getFontMetrics().getHeight() / 2));
            graphics2D.setColor(theme.getDefaultButtonPressedColor());
            graphics2D.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }

        graphics2D.dispose();
    }

    // Mouse Events

    @Override
    public void mouseClicked(MouseEvent event) {
        setBackground(theme.getDefaultButtonHoverColor());

        if (clickAction != null) clickAction.run();

        updateSize();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        setBackground(theme.getDefaultButtonHoverColor());
    }

    @Override
    public void mouseReleased(MouseEvent event) {

    }

    @Override
    public void mouseEntered(MouseEvent event) {
        setBackground(theme.getDefaultBackgroundColor());
    }

    @Override
    public void mouseExited(MouseEvent event) {
        setBackground(theme.getDefaultBackgroundColor());
    }

    @Override
    public void mouseDragged(MouseEvent event) {

    }

    @Override
    public void mouseMoved(MouseEvent event) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    // Utils

    private void updateSize() {
        this.width = Math.max(this.width, (int) (getFontMetrics().stringWidth(text) * 1.25));
        this.height = Math.max(this.height, (int) (getFontMetrics().getHeight() * 1.5625 - 0.25));

        Dimension size = new Dimension(this.width, this.height);

        setPreferredSize(size);
        setSize(size);
    }

    private FontMetrics getFontMetrics() {
        return toolkit.getFontMetrics(font);
    }

    // Setter

    private void setClickAction(Runnable clickAction) {
        this.clickAction = clickAction;
    }
}
