package cally72jhb.addon;

import cally72jhb.addon.utils.window.ExternalPanel;
import cally72jhb.addon.utils.window.components.text.Text;
import cally72jhb.addon.utils.window.theme.LightTheme;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Main {
    private static boolean nogui = false;
    private static boolean debug = false;

    public static void main(String[] args) {
        System.out.println();
        System.out.println("arguments: -nogui -bettergui -debug");
        System.out.println("arguments: /nogui /bettergui /debug");
        System.out.println();
        System.out.println("used arguments: " + Arrays.toString(args));
        System.out.println("validating arguments:");

        for (String string : args) {
            if ((string.startsWith("-") || string.startsWith("/"))) {
                if (string.contains("nogui") || string.contains("bettergui")) {
                    nogui = true;
                    System.out.println("Better GUI disabled!");
                    break;
                } else if (string.contains("debug")) {
                    debug = true;
                    print("entering debug mode");
                    break;
                }
            }
        }

        System.out.println("Done validating arguments!");
        System.out.println("Creating main Window...");
        System.out.println();

        ExternalPanel panel = new ExternalPanel(600, 320, 600, 320, new LightTheme(), "Vector Addon", !nogui);

        System.out.println("width: 600 | min-width: 600 | height: 320 | min-height: 320 | title: Vector Addon | Custom GUI: " + !nogui);
        System.out.println("Created window");
        print("now obtaining home path...");

        ArrayList<ArrayList<Text>> content = new ArrayList<>();

        String path = switch (getOperatingSystem()) {
            case WINDOWS -> System.getenv("AppData") + "/.minecraft/mods";
            case OSX -> System.getProperty("user.home") + "/Library/Application Support/minecraft/mods";
            default -> System.getProperty("user.home") + "/.minecraft";
        };

        File mods = new File(path);
        if (!mods.exists()) {
            print("Creating mods folder here: " + mods.getAbsolutePath());

            if (mods.mkdirs()) {
                print("Successfully created mods folder!");
            } else {
                print("Failed to create mods folder!");
            }
        } else {
            print("Mods folder found: " + mods.getAbsolutePath());
        }

        print("Setting up content...");

        content.add(getSpace());
        content.add(getLine("Vector Addon", new Color(75, 125, 85), 1));
        content.add(getSpace());
        content.add(getLines(
            new Text("Vector is a free open-source Addon for ", new Color(0, 0, 0), 0),
            new Text("Meteor Client", new Color(145,61,226), 0),
            new Text(".", new Color(0, 0, 0), 0)));
        content.add(getLine("It provides many utility and strong combat modules.", new Color(0, 0, 0), 0));
        content.add(getLine("To install Vector correctly put it along side with Meteor Client in your", new Color(0, 0, 0), 0));
        content.add(getLine("mods folder which can be found here: ", new Color(0, 0, 0), 0));
        content.add(getSpace());
        content.add(getLine(mods.getAbsolutePath(), new Color(33, 67, 122), 0));
        content.add(getSpace());

        // Modules

        content.add(getLine("Modules:", new Color(75, 75, 75), 1));

        content.add(getLine("- Action Renderer", new Color(0, 0, 0), 0));
        content.add(getLine("- Anti LagBack", new Color(0, 0, 0), 0));
        content.add(getLine("- Anti PistonPush", new Color(0, 0, 0), 0));
        content.add(getLine("- Anti Prone", new Color(0, 0, 0), 0));
        content.add(getLine("- Auto Cope", new Color(0, 0, 0), 0));
        content.add(getLine("- Auto Ez", new Color(0, 0, 0), 0));
        content.add(getLine("- Auto Interact", new Color(0, 0, 0), 0));
        content.add(getLine("- Bed Bomb", new Color(0, 0, 0), 0));
        content.add(getLine("- Bow Bomb", new Color(0, 0, 0), 0));
        content.add(getLine("- Chat Encryption", new Color(0, 0, 0), 0));
        content.add(getLine("- Chorus Predict", new Color(0, 0, 0), 0));
        content.add(getLine("- Death Animations", new Color(0, 0, 0), 0));
        content.add(getLine("- Entity Fly", new Color(0, 0, 0), 0));
        content.add(getLine("- Insta Mine Plus", new Color(0, 0, 0), 0));
        content.add(getLine("- Inventory Scroll", new Color(0, 0, 0), 0));
        content.add(getLine("- Item Release", new Color(0, 0, 0), 0));
        content.add(getLine("- No BlockTrace", new Color(0, 0, 0), 0));
        content.add(getLine("- No Collision", new Color(0, 0, 0), 0));
        content.add(getLine("- No Fluid", new Color(0, 0, 0), 0));
        content.add(getLine("- Packet Consume", new Color(0, 0, 0), 0));
        content.add(getLine("- Packet Fly", new Color(0, 0, 0), 0));
        content.add(getLine("- Packet Hole Fill", new Color(0, 0, 0), 0));
        content.add(getLine("- Packet Logger", new Color(0, 0, 0), 0));
        content.add(getLine("- Packet Place", new Color(0, 0, 0), 0));
        content.add(getLine("- Ping Spoof", new Color(0, 0, 0), 0));
        content.add(getLine("- Placeholders", new Color(0, 0, 0), 0));
        content.add(getLine("- Pop Renderer", new Color(0, 0, 0), 0));
        content.add(getLine("- Portal GodMode", new Color(0, 0, 0), 0));
        content.add(getLine("- Reverse Step Bypass", new Color(0, 0, 0), 0));
        content.add(getLine("- Rubberband Fly", new Color(0, 0, 0), 0));
        content.add(getLine("- Skeleton ESP", new Color(0, 0, 0), 0));
        content.add(getLine("- Step Plus", new Color(0, 0, 0), 0));
        content.add(getLine("- Storage Viewer", new Color(0, 0, 0), 0));
        content.add(getLine("- Tick Shift", new Color(0, 0, 0), 0));
        content.add(getLine("- Tower", new Color(0, 0, 0), 0));
        content.add(getLine("- Vector Presence", new Color(0, 0, 0), 0));
        content.add(getLine("- Vector Surround", new Color(0, 0, 0), 0));
        content.add(getLine("- Welcomer", new Color(0, 0, 0), 0));

        content.add(getSpace());
        content.add(getSpace());

        // Commands

        content.add(getLine("Commands:", new Color(75, 75, 75), 1));

        content.add(getLine("- Item Command", new Color(0, 0, 0), 0));
        content.add(getLine("- Mute Command", new Color(0, 0, 0), 0));
        content.add(getLine("- Stats Command", new Color(0, 0, 0), 0));
        content.add(getLine("- Target Command", new Color(0, 0, 0), 0));
        content.add(getLine("- Trash Command", new Color(0, 0, 0), 0));
        content.add(getLine("- UUID Command", new Color(0, 0, 0), 0));

        print();
        print("Refreshing content & making window visible");
        print();

        panel.setContent(content);
        panel.setVisible(true);

        print("Window successfully created!");
    }

    // Debug Logger

    private static void print() {
        print("");
    }

    private static void print(String string) {
        if (debug) {
            if (string.isEmpty()) {
                System.out.println();
            } else {
                System.out.println(string);
            }
        }
    }

    // Text Utils

    private static ArrayList<Text> getSpace() {
        print();

        return new ArrayList<>() {{
            add(new Text("", new Color(0, 0, 0), 0));
        }};
    }

    private static ArrayList<Text> getLine(String string, Color color, int type) {
        print(string);

        return new ArrayList<>() {{
            add(new Text(string, color, type));
        }};
    }

    private static ArrayList<Text> getLines(Text... message) {
        return new ArrayList<>() {{
            for (Text text : message) {
                add(text);

                print(text.getText());
            }
        }};
    }

    // Operating System

    private static OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.OSX;
        } else if (os.contains("solaris")) {
            return OperatingSystem.SOLARIS;
        } else if (os.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else if (os.contains("linux")) {
            return OperatingSystem.LINUX;
        } else {
            return os.contains("unix") ? OperatingSystem.LINUX : OperatingSystem.UNKNOWN;
        }
    }

    private enum OperatingSystem {
        OSX,
        LINUX,
        SOLARIS,
        WINDOWS,
        UNKNOWN
    }
}
