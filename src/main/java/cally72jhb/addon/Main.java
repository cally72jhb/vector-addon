package cally72jhb.addon;

import cally72jhb.addon.utils.window.ExternalPanel;
import cally72jhb.addon.utils.window.Text;
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

        content.add(getLine("- ActionRenderer", new Color(0, 0, 0), 0));
        content.add(getLine("- AntiPistonPush", new Color(0, 0, 0), 0));
        content.add(getLine("- AntiProne", new Color(0, 0, 0), 0));
        content.add(getLine("- AutoCope", new Color(0, 0, 0), 0));
        content.add(getLine("- AutoCraft", new Color(0, 0, 0), 0));
        content.add(getLine("- AutoEz", new Color(0, 0, 0), 0));
        content.add(getLine("- AutoInteract", new Color(0, 0, 0), 0));
        content.add(getLine("- BedBomb", new Color(0, 0, 0), 0));
        content.add(getLine("- BlinkPlus", new Color(0, 0, 0), 0));
        content.add(getLine("- BorderBypass", new Color(0, 0, 0), 0));
        content.add(getLine("- BowBomb", new Color(0, 0, 0), 0));
        content.add(getLine("- ChatEncryption", new Color(0, 0, 0), 0));
        content.add(getLine("- ChorusPredict", new Color(0, 0, 0), 0));
        content.add(getLine("- DeathAnimations", new Color(0, 0, 0), 0));
        content.add(getLine("- EntityFly", new Color(0, 0, 0), 0));
        content.add(getLine("- InstaMinePlus", new Color(0, 0, 0), 0));
        content.add(getLine("- InventoryScroll", new Color(0, 0, 0), 0));
        content.add(getLine("- ItemRelease", new Color(0, 0, 0), 0));
        content.add(getLine("- MultiTask", new Color(0, 0, 0), 0));
        content.add(getLine("- NoFluid", new Color(0, 0, 0), 0));
        content.add(getLine("- PacketConsume", new Color(0, 0, 0), 0));
        content.add(getLine("- PacketFly", new Color(0, 0, 0), 0));
        content.add(getLine("- PacketLogger", new Color(0, 0, 0), 0));
        content.add(getLine("- PacketPlace", new Color(0, 0, 0), 0));
        content.add(getLine("- PingSpoof", new Color(0, 0, 0), 0));
        content.add(getLine("- Placeholders", new Color(0, 0, 0), 0));
        content.add(getLine("- PopRenderer", new Color(0, 0, 0), 0));
        content.add(getLine("- PortalGodMode", new Color(0, 0, 0), 0));
        content.add(getLine("- SkeletonESP", new Color(0, 0, 0), 0));
        content.add(getLine("- SpeedBypass", new Color(0, 0, 0), 0));
        content.add(getLine("- StepPlus", new Color(0, 0, 0), 0));
        content.add(getLine("- StorageViewer", new Color(0, 0, 0), 0));
        content.add(getLine("- TickShift", new Color(0, 0, 0), 0));
        content.add(getLine("- Tower", new Color(0, 0, 0), 0));
        content.add(getLine("- VectorPresence", new Color(0, 0, 0), 0));
        content.add(getLine("- VectorSurround", new Color(0, 0, 0), 0));
        content.add(getLine("- Welcomer", new Color(0, 0, 0), 0));

        content.add(getSpace());
        content.add(getSpace());

        // Commands

        content.add(getLine("Commands:", new Color(75, 75, 75), 1));

        content.add(getLine("- ItemCommand", new Color(0, 0, 0), 0));
        content.add(getLine("- MuteCommand", new Color(0, 0, 0), 0));
        content.add(getLine("- StatsCommand", new Color(0, 0, 0), 0));
        content.add(getLine("- TargetCommand", new Color(0, 0, 0), 0));
        content.add(getLine("- TrashCommand", new Color(0, 0, 0), 0));
        content.add(getLine("- UUIDCommand", new Color(0, 0, 0), 0));

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
