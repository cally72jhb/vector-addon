package cally72jhb.addon.utils.window;

import cally72jhb.addon.Main;
import cally72jhb.addon.utils.window.components.buttons.ButtonType;
import cally72jhb.addon.utils.window.components.buttons.CustomTypeButton;
import cally72jhb.addon.utils.window.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.InputStream;
import java.util.ArrayList;

public class ExternalPanel extends JFrame {
    private final ExternalWindow window;

    private int prevX, prevY;

    private Point startDrag, startLoc;
    private final int minWidth, minHeight;
    private final int snappingRange = 10;

    public ExternalPanel(int width, int height, int minWidth, int minHeight, Theme theme, String title, boolean customTitleBar) {

        this.minWidth = minWidth;
        this.minHeight = minHeight;

        window = new ExternalWindow(width, height);

        add(window);
        setUndecorated(customTitleBar);
        setResizable(!customTitleBar);

        pack();

        setTitle(title);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        window.setBackground(theme.getDefaultBackgroundColor());

        // Icon

        try {
            InputStream stream = Main.class.getClassLoader().getResourceAsStream("assets/vector-addon/vector/icon32.png");

            if (stream != null) setIconImage(new ImageIcon(ImageIO.read(stream)).getImage());
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // Title Bar

        if (customTitleBar) {
            JPanel titleBar = new JPanel();
            titleBar.setLayout(new BorderLayout());
            titleBar.setOpaque(false);

            // Buttons

            JPanel controlBox = new JPanel();
            controlBox.setOpaque(false);

            controlBox.setLayout(new GridLayout(1, 3, -1, 0));

            // Minimize Button

            CustomTypeButton minimizeButton = new CustomTypeButton(ButtonType.MINIMIZE, theme);
            minimizeButton.setPreferredSize(new Dimension(50, 30));
            minimizeButton.setBackground(theme.getDefaultBackgroundColor());

            minimizeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    setExtendedState(JFrame.ICONIFIED);
                }
            });

            // Always On Top Button

            CustomTypeButton maximizeButton = new CustomTypeButton(ButtonType.MAXIMIZE, theme);
            maximizeButton.setPreferredSize(new Dimension(50, 30));
            maximizeButton.setBackground(theme.getDefaultBackgroundColor());

            maximizeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    setAlwaysOnTop(!isAlwaysOnTop());
                }
            });

            // Close Button

            CustomTypeButton closeButton = new CustomTypeButton(ButtonType.CLOSE, theme);
            closeButton.setPreferredSize(new Dimension(50, 30));
            closeButton.setBackground(theme.getDefaultBackgroundColor());

            closeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    System.exit(0);
                }
            });

            controlBox.add(minimizeButton);
            controlBox.add(maximizeButton);
            controlBox.add(closeButton);

            titleBar.add(controlBox, BorderLayout.EAST);

            JPanel content = new JPanel();
            content.setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
            content.setOpaque(false);

            titleBar.add(content, BorderLayout.WEST);

            add(titleBar, BorderLayout.NORTH);

            pack();

            // Listeners

            // Close Button & Close on Deactivate

            closeButton.removeMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    System.exit(0);
                }
            });

            closeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    System.exit(0);
                }
            });

            // Dragging

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    prevX = event.getX();
                    prevY = event.getY();
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (getCursorType() == Cursor.DEFAULT_CURSOR) {
                        setLocation(getLocation().x + event.getX() - prevX, getLocation().y + event.getY() - prevY);
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent event) {
                    if (getCursorType() == Cursor.DEFAULT_CURSOR) {
                        setLocation(getLocation().x + event.getX() - prevX, getLocation().y + event.getY() - prevY);
                    }
                }
            });

            // Resizing

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    resizeFrame(event);
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    Point location = event.getPoint();
                    int xPos = location.x;
                    int yPos = location.y;

                    if (xPos >= snappingRange && xPos <= getWidth() - snappingRange && yPos >= getHeight() - snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    } else if (xPos >= getWidth() - snappingRange && yPos >= snappingRange && yPos <= getHeight() - snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    } else if (xPos <= snappingRange && yPos >= snappingRange && yPos <= getHeight() - snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    } else if (xPos >= snappingRange && xPos <= getWidth() - snappingRange && yPos <= snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    } else if (xPos <= snappingRange && yPos <= snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    } else if (xPos >= getWidth() - snappingRange && yPos <= snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    } else if (xPos >= getWidth() - snappingRange && yPos >= getHeight() - snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    } else if (xPos <= snappingRange && yPos >= getHeight() - snappingRange) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    startDrag = getScreenLocation(event);
                    startLoc = getLocation();
                }
            });
        }

        // Scrolling

        addMouseWheelListener(event -> window.scroll((int) Math.floor(event.getPreciseWheelRotation())));
    }

    public ArrayList<ArrayList<Text>> getText() {
        return window.getContent();
    }

    public void setContent(ArrayList<ArrayList<Text>> content) {
        window.setContent(content);

        window.repaint();
        repaint();
    }

    public void addLine(ArrayList<Text> line) {
        window.addLine(line);
    }

    private void resizeFrame(MouseEvent event) {
        Object source = event.getSource();
        Point current = getScreenLocation(event);
        Point offset = new Point((int) current.getX()- (int) startDrag.getX(), (int) current.getY()- (int) startDrag.getY());

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        if (source instanceof JPanel && getCursor().equals(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))) {
            setLocation((int) (startLoc.getX() + offset.getX()), (int) (startLoc.getY() + offset.getY()));
        } else if (!getCursor().equals(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))) {
            int oldLocationX = (int) getLocation().getX();
            int oldLocationY = (int) getLocation().getY();
            int newLocationX = (int) (startLoc.getX() + offset.getX());
            int newLocationY = (int) (startLoc.getY() + offset.getY());

            boolean N_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            boolean NE_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
            boolean NW_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            boolean E_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            boolean W_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
            boolean S_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            boolean SW_Resize = getCursor().equals(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
            boolean setLocation = false;

            int newWidth = event.getX();
            int newHeight = event.getY();

            if (NE_Resize) {
                newHeight = getHeight() - (newLocationY - oldLocationY);
                newLocationX = (int)getLocation().getX();
                setLocation = true;
            } else if (E_Resize) {
                newHeight = getHeight();
            } else if (S_Resize) {
                newWidth = getWidth();
            } else if (N_Resize) {
                newLocationX = (int)getLocation().getX();
                newWidth = getWidth();
                newHeight = getHeight() - (newLocationY - oldLocationY);
                setLocation = true;
            } else if (NW_Resize) {
                newWidth = getWidth() - (newLocationX - oldLocationX);
                newHeight = getHeight() - (newLocationY - oldLocationY);
                setLocation =true;
            } else if (SW_Resize) {
                newWidth = getWidth() - (newLocationX - oldLocationX);
                newLocationY = (int)getLocation().getY();
                setLocation =true;
            } else if (W_Resize) {
                newWidth = getWidth() - (newLocationX - oldLocationX);
                newLocationY = (int)getLocation().getY();
                newHeight = getHeight();
                setLocation =true;
            }

            if (newWidth >= (int) toolkit.getScreenSize().getWidth() || newWidth <= minWidth) {
                newLocationX = oldLocationX;
                newWidth = getWidth();
            }

            if (newHeight >= (int) toolkit.getScreenSize().getHeight() - 30 || newHeight <= minHeight) {
                newLocationY = oldLocationY;
                newHeight = getHeight();
            }

            if (newWidth != getWidth() || newHeight != getHeight()) {
                setSize(newWidth, newHeight);

                if (setLocation) setLocation(newLocationX, newLocationY);
            }
        }
    }

    private Point getScreenLocation(MouseEvent event) {
        Point cursor = event.getPoint();
        Point view_location = getLocationOnScreen();

        return new Point((int) (view_location.getX() + cursor.getX()), (int) (view_location.getY() + cursor.getY()));
    }
}
