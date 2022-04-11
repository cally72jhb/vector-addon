package cally72jhb.addon.utils.window.installation;

import cally72jhb.addon.Main;
import cally72jhb.addon.utils.window.components.buttons.ButtonType;
import cally72jhb.addon.utils.window.components.buttons.CustomMenuButton;
import cally72jhb.addon.utils.window.components.buttons.CustomTypeButton;
import cally72jhb.addon.utils.window.components.panels.ComponentGroup;
import cally72jhb.addon.utils.window.components.text.CustomText;
import cally72jhb.addon.utils.window.theme.LightTheme;
import cally72jhb.addon.utils.window.theme.Theme;
import org.json.JSONArray;
import org.json.JSONObject;
import oshi.util.tuples.Pair;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

public class InstallationWindow extends JFrame {
    private static final String releases = "https://api.github.com/repos/cally72jhb/vector-addon/releases";

    private final Theme theme;

    private int prevX, prevY;

    private Point startDrag, startLoc;
    private final int minWidth, minHeight;
    private final int snappingRange = 5;

    public static void main(String[] args) {
        System.out.println();

        InstallationWindow window = new InstallationWindow(700, 350, "Vector Installation", new LightTheme(), false);

        window.setVisible(true);
    }

    public InstallationWindow(int width, int height, String title, Theme theme, boolean betterGUI) {
        this.minWidth = width;
        this.minHeight = height;

        this.theme = theme;

        DisplayPanel panel = new DisplayPanel(width, height, theme);

        panel.setLayout(new OverlayLayout(panel));

        add(panel);

        setUndecorated(betterGUI);
        setResizable(!betterGUI);

        pack();

        setTitle(title);
        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        List<Pair<String, String>> versions = new ArrayList<>();

        try {
            versions = getVersions();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        if (!versions.isEmpty() || true) {
            String[] versionString = new String[versions.size()];

            for (int i = 0; i < versions.size(); i++) {
                versionString[i] = versions.get(i).getA();
            }

            //versionString = new String[] { "vector-addon-0.3.1", "vector-addon-0.3.0", "vector-addon-0.2.9", "vector-addon-0.2.8", "vector-addon-0.2.7", "vector-addon-0.2.6", "vector-addon-0.2.5", "vector-addon-0.2.4", "vector-addon-0.2.3", "vector-addon-alpha-0.2.2", "vector-addon-beta-0.2.1", "vector-addon-beta-0.2.0", "vector-addon-beta-0.1.9" };

            System.out.println(Arrays.toString(versionString));

            ComponentGroup vector = new ComponentGroup();

            CustomText vectorText = new CustomText("Vector Versions:", theme, null, 225, 0);

            CustomMenuButton vectorVersion = new CustomMenuButton(versionString, theme, 4);

            vector.setAlignmentX(0.0f);
            vector.setAlignmentY(0.0f);

            vector.add(vectorText);
            vector.add(vectorVersion);

            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    super.mouseClicked(event);

                    vectorVersion.hide();
                    vectorVersion.repaint();
                    vectorVersion.updateSize();
                    panel.repaint();
                    repaint();
                }
            });

            panel.add(Box.createRigidArea(new Dimension(0, 2)));
            panel.add(vector);
        }

        setIcon();

        if (betterGUI) {
            System.out.println("Setting up custom window & title bar");

            setTitleBar();
        }
    }

    // Window Utils

    private boolean setIcon() {
        try {
            InputStream stream = Main.class.getClassLoader().getResourceAsStream("assets/vector-addon/vector/icon32.png");
            if (stream != null) setIconImage(new ImageIcon(ImageIO.read(stream)).getImage());

            return stream != null;
        } catch (Exception exception) {
            exception.printStackTrace();

            return false;
        }
    }

    private void setTitleBar() {
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

        addMouseListener(new MouseAdapter() {
            private int count = 0;
            private final java.util.Timer timer = new java.util.Timer("doubleClickTimer", false);

            @Override
            public void mouseClicked(final MouseEvent event) {
                count = event.getClickCount();

                if (event.getClickCount() == 1 && getCursorType() == Cursor.DEFAULT_CURSOR) {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (count > 1) setSize(minWidth, minHeight);

                            count = 0;
                        }
                    }, 175);
                }
            }
        });
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


    // Utils


    public static List<Pair<String, String>> getVersions() throws IOException {
        String data = readUrl(releases);
        JSONArray array = new JSONArray(data);

        List<Pair<String, String>> versions = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            JSONArray assets = object.getJSONArray("assets");

            versions.add(new Pair<>(object.getString("name"), assets.getJSONObject(0).getString("browser_download_url")));
        }

        return versions;
    }

    private static String readUrl(String url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            StringBuilder builder = new StringBuilder();

            int read;
            char[] chars = new char[1024];

            while ((read = reader.read(chars)) != -1) {
                builder.append(chars, 0, read);
            }

            return builder.toString();
        }
    }
}
