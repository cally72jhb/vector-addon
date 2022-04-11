package cally72jhb.addon.utils.window;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ExternalWindow extends JPanel {
    private ArrayList<ArrayList<Text>> content = new ArrayList<>();
    private Font font;

    private int scrollY;

    public ExternalWindow(int width, int height) {
        this.setBackground(Color.WHITE);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(width, height));

        this.scrollY = 0;

        try {
            this.font = new Font("", Font.PLAIN, 17);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        this.render(graphics);
    }

    public void render(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(Color.WHITE);
        graphics.setFont(font);

        if (!this.content.isEmpty()) {
            int x = 10;
            int y = (this.scrollY + 1) * (int) (graphics.getFontMetrics().getFont().getSize() * 1.25) - (graphics.getFontMetrics().getFont().getSize() / 4);

            for (ArrayList<Text> line : this.content) {
                for (Text text : line) {
                    graphics.setFont(new Font(font.getFontName(), text.getType(), font.getSize()));

                    graphics.setColor(text.getColor());
                    graphics.drawString(text.getText(), x, y);

                    x += graphics.getFontMetrics().stringWidth(text.getText());

                    graphics.setFont(new Font(font.getFontName(), Font.PLAIN, font.getSize()));
                }

                y += (int) (graphics.getFontMetrics().getFont().getSize() * 1.25);
                x = 10;
            }
        }

        Toolkit.getDefaultToolkit().sync();
    }

    public void setContent(ArrayList<ArrayList<Text>> content) {
        this.content = content;

        this.repaint();
    }

    public void addLine(ArrayList<Text> line) {
        this.content.add(line);

        this.repaint();
    }

    public ArrayList<ArrayList<Text>> getContent() {
        return this.content;
    }

    public void scroll(int y) {
        this.scrollY -= y;

        if (this.scrollY > 0) this.scrollY = 0;
        if (this.scrollY < -this.content.size() + 12) this.scrollY = -this.content.size() + 12;

        this.repaint();
    }
}
