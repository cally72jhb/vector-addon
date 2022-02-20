package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Welcomer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWelcome = settings.createGroup("Welcome");
    private final SettingGroup sgGoodbye = settings.createGroup("Goodbye");

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friended players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("only-friends")
        .description("Only welcomes / goodbyes friended players.")
        .defaultValue(true)
        .visible(() -> !ignoreFriends.get())
        .build()
    );

    private final Setting<Boolean> smallCaps = sgGeneral.add(new BoolSetting.Builder()
        .name("small-caps")
        .description("Sends all messages with small caps.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> randomMsg = sgGeneral.add(new BoolSetting.Builder()
        .name("random")
        .description("Sends random messages every kill or pop.")
        .defaultValue(true)
        .build()
    );

    // Welcome

    private final Setting<Boolean> welcome = sgWelcome.add(new BoolSetting.Builder()
        .name("welcome")
        .description("Sends messages in the chat when a player joins.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> welcomeString = sgWelcome.add(new StringSetting.Builder()
        .name("welcome-message")
        .description("The message to send when a player joins.")
        .defaultValue("welcome {player}")
        .visible(() -> !randomMsg.get() && welcome.get())
        .build()
    );

    private final Setting<List<String>> welcomeMessages = sgWelcome.add(new StringListSetting.Builder()
        .name("welcome-messages")
        .description("The random messages to send when a player joins.")
        .defaultValue(List.of("welcome {player}", "hello {player}"))
        .visible(() -> randomMsg.get() && welcome.get())
        .build()
    );

    private final Setting<Integer> welcomeDelay = sgWelcome.add(new IntSetting.Builder()
        .name("welcome-delay")
        .description("How long to wait in ticks before sending another welcome message.")
        .defaultValue(20)
        .min(0)
        .visible(welcome::get)
        .build()
    );

    // Goodbype

    private final Setting<Boolean> bye = sgGoodbye.add(new BoolSetting.Builder()
        .name("goodbye")
        .description("Sends messages in the chat when a player joins.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> byeString = sgGoodbye.add(new StringSetting.Builder()
        .name("bye-message")
        .description("The message to send when a player joins.")
        .defaultValue("farewell {player}")
        .visible(() -> !randomMsg.get() && bye.get())
        .build()
    );

    private final Setting<List<String>> byeMessages = sgGoodbye.add(new StringListSetting.Builder()
        .name("bye-messages")
        .description("The random messages to send when a player joins.")
        .defaultValue(List.of("goodbye {player}", "farewell {player}", "bye {player}"))
        .visible(() -> randomMsg.get() && bye.get())
        .build()
    );

    private final Setting<Integer> byeDelay = sgGoodbye.add(new IntSetting.Builder()
        .name("bye-delay")
        .description("How long to wait in ticks before sending another welcome message.")
        .defaultValue(20)
        .min(0)
        .visible(bye::get)
        .build()
    );

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    private List<PlayerListEntry> prevEntries;
    private List<PlayerListS2CPacket.Entry> entries;
    private Random random;
    private int wTimer;
    private int bTimer;

    public Welcomer() {
        super(VectorAddon.Misc, "welcomer", "Sends a chat message when a player joins or leaves.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @Override
    public void onActivate() {
        prevEntries = new ArrayList<>();
        entries = new ArrayList<>();
        random = new Random();
        wTimer = 0;
        bTimer = 0;

        prevEntries = List.copyOf(mc.getNetworkHandler().getPlayerList());
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerListS2CPacket packet)) return;
        if (packet.getAction() != PlayerListS2CPacket.Action.ADD_PLAYER && packet.getAction() != PlayerListS2CPacket.Action.REMOVE_PLAYER) return;

        entries = packet.getEntries();

        if (welcome.get() && (wTimer >= welcomeDelay.get() || welcomeDelay.get() == 0) && packet.getAction() == PlayerListS2CPacket.Action.ADD_PLAYER) {
            for (PlayerListS2CPacket.Entry entry : entries) {
                if (entry != null && entry.getProfile() != null && !entry.getProfile().getName().equals(mc.player.getGameProfile().getName())) {
                    String name = entry.getProfile().getName();

                    boolean existed = true;

                    for (PlayerListEntry prevEntry : prevEntries) {
                        if (prevEntry != null && prevEntry.getDisplayName() != null
                            && entry.getDisplayName().asString().equals(prevEntry.getDisplayName().asString())) existed = false;
                    }

                    if (existed && isFriend(name)) {
                        sendMsg(apply(name, randomMsg.get() ? welcomeMessages.get() : List.of(welcomeString.get())));
                        wTimer = 0;
                    }
                }
            }
        }

        if (bye.get() && (bTimer >= byeDelay.get() || byeDelay.get() == 0) && packet.getAction() == PlayerListS2CPacket.Action.REMOVE_PLAYER) {
            for (PlayerListS2CPacket.Entry entry : entries) {
                if (entry != null && entry.getDisplayName() != null) {
                    String name = entry.getDisplayName().getString();

                    boolean existed = true;

                    for (PlayerListEntry prevEntry : prevEntries) {
                        if (prevEntry != null && prevEntry.getDisplayName() != null
                            && entry.getDisplayName().asString().equals(prevEntry.getDisplayName().asString())) existed = false;
                    }

                    if (!existed && isFriend(name)) {
                        sendMsg(apply(name, randomMsg.get() ? byeMessages.get() : List.of(byeString.get())));
                        bTimer = 0;
                    }
                }
            }
        }

        prevEntries = List.copyOf(mc.getNetworkHandler().getPlayerList());
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        wTimer++;
        bTimer++;
    }

    // Messaging

    private void sendMsg(String string) {
        StringBuilder sb = new StringBuilder();

        if (smallCaps.get()) {
            for (char ch : string.toCharArray()) {
                if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
                else sb.append(ch);
            }
        } else {
            sb.append(string);
        }

        mc.player.sendChatMessage(sb.toString());
    }

    // Utils

    private String apply(String player, List<String> strings) {
        return strings.get(random.nextInt(strings.size())).replace("{player}", player);
    }

    private boolean isFriend(String name) {
        if (!ignoreFriends.get()) return true;

        boolean friended = false;

        for (Friend friend : Friends.get()) {
            if (friend.name.contains(name)) {
                friended = true;
                break;
            }
        }

        return (onlyFriends.get() && !ignoreFriends.get()) != friended;
    }
}
