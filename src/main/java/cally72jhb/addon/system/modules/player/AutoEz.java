package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.*;

public class AutoEz extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPops = settings.createGroup("Pops");
    private final SettingGroup sgKills = settings.createGroup("Kills");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");

    private final Setting<Boolean> clearOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("clear-on-death")
        .description("Resets your scores on death.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("message")
        .description("Sends messages in the chat when you kill or pop players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Sends client-side messages with your kill and pop streak after you kill players.")
        .defaultValue(false)
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

    // Pops

    private final Setting<Boolean> popMsg = sgPops.add(new BoolSetting.Builder()
        .name("pop")
        .description("Sends a messages everytime you pop a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> popString = sgPops.add(new StringSetting.Builder()
        .name("pop-message")
        .description("The message to send when you poped a player.")
        .defaultValue("ez pop {player}")
        .visible(() -> !randomMsg.get() && popMsg.get())
        .build()
    );

    private final Setting<List<String>> popMessages = sgPops.add(new StringListSetting.Builder()
        .name("pop-messages")
        .description("The random messages to send when you poped a player.")
        .defaultValue(List.of("vector on top", "ez {player}", "poped {player} with vector", "{pops} pops on {player} already"))
        .visible(() -> randomMsg.get() && popMsg.get())
        .build()
    );

    private final Setting<Integer> popDelay = sgPops.add(new IntSetting.Builder()
        .name("pop-delay")
        .description("How long to wait in ticks before sending a pop message again.")
        .defaultValue(15)
        .min(0)
        .visible(popMsg::get)
        .build()
    );

    // Kills

    private final Setting<Boolean> killMsg = sgKills.add(new BoolSetting.Builder()
        .name("kill")
        .description("Sends a messages everytime you kill a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> killString = sgKills.add(new StringSetting.Builder()
        .name("kill-message")
        .description("The message to send when you killed someone.")
        .defaultValue("killed {player}")
        .visible(() -> !randomMsg.get() && killMsg.get())
        .build()
    );

    private final Setting<List<String>> killMessages = sgKills.add(new StringListSetting.Builder()
        .name("kill-messages")
        .description("The random messages to send when you kill someone.")
        .defaultValue(List.of("ez {player}", "killed {player} with vector", "currently at {kills} kill streak", "{playerkills} kills on {player} already", "join vector for free: https://discord.gg/A3nYgbKeXR", "{online} people saw {player} die to the power of vector"))
        .visible(() -> randomMsg.get() && killMsg.get())
        .build()
    );

    private final Setting<Integer> killDelay = sgKills.add(new IntSetting.Builder()
        .name("kill-delay")
        .description("How long to wait in ticks before sending a kill message again.")
        .defaultValue(5)
        .min(0)
        .visible(killMsg::get)
        .build()
    );

    // Targeting

    private final Setting<Boolean> ignoreFriends = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friended players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkTargets = sgTargeting.add(new BoolSetting.Builder()
        .name("check-targets")
        .description("Checks the current target form every module.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range a player has to be in with to detect a pop.")
        .defaultValue(7)
        .min(0)
        .max(50)
        .build()
    );

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    private HashMap<UUID, Integer> kills;
    private HashMap<UUID, Integer> pops;
    private Random random;
    private int allKills;
    private int killTimer;
    private int popTimer;

    public AutoEz() {
        super(VectorAddon.MISC, "auto-ez", "Send a chat message after killing or poping a player.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @Override
    public void onActivate() {
        kills = new HashMap<>();
        pops = new HashMap<>();
        random = new Random();

        allKills = 0;
        killTimer = 0;
        popTimer = 0;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() != 35 && packet.getStatus() != 3) return;
            if (packet.getStatus() == 3 && packet.getEntity(mc.world) == mc.player && clearOnDeath.get()) onActivate();

            Entity entity = packet.getEntity(mc.world);

            if (!check(entity)) return;

            if (packet.getStatus() == 35) {
                pops.putIfAbsent(entity.getUuid(), 0);
                pops.replace(entity.getUuid(), pops.get(entity.getUuid()) + 1);

                if (popMsg.get() && (popTimer >= popDelay.get() || popDelay.get() == 0)) {
                    sendPopMsg((PlayerEntity) entity);
                    popTimer = 0;
                    return;
                }
            }

            if (packet.getStatus() == 3) {
                kills.putIfAbsent(entity.getUuid(), 0);
                kills.replace(entity.getUuid(), kills.get(entity.getUuid()) + 1);

                allKills++;

                if (killMsg.get() && (killTimer >= killDelay.get() || killDelay.get() == 0)) {
                    sendKillMsg((PlayerEntity) entity);
                    killTimer = 0;
                }
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        killTimer++;
        popTimer++;
    }

    // Messaging

    private void sendPopMsg(PlayerEntity player) {
        sendMsg(apply(player, randomMsg.get() ? popMessages.get().get(random.nextInt(popMessages.get().size())) : popString.get()));
    }

    private void sendKillMsg(PlayerEntity player) {
        if (message.get()) {
            sendMsg(apply(player, randomMsg.get() ? killMessages.get().get(random.nextInt(killMessages.get().size())) : killString.get()));
        }

        int pop = pops.get(player.getUuid()) == null ? 0 : pops.get(player.getUuid());
        int kill = kills.get(player.getUuid()) == null ? 0 : kills.get(player.getUuid());
        if (notify.get()) info("poped " + player.getEntityName()
                + " " + pop + (pop == 1 ? " time" : " times") + " and killed him "
                + kill + (kill == 1 ? " time." : " times."));
    }

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

        if (message.get()) {
            mc.player.sendChatMessage(sb.toString());
        } else {
            info(sb.toString());
        }
    }

    // Utils

    private String apply(PlayerEntity player, String message) {
        String string = message.replace("{player}", player.getEntityName());

        string = string.replace("{online}", String.valueOf(mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 0));
        string = string.replace("{pops}", String.valueOf(pops.get(player.getUuid())));
        string = string.replace("{playerkills}", String.valueOf(kills.get(player.getUuid())));
        string = string.replace("{kills}", String.valueOf(allKills));

        return string;
    }

    private boolean check(Entity entity) {
        if (!(entity instanceof PlayerEntity) || entity == mc.player
            || (Friends.get().isFriend((PlayerEntity) entity) && ignoreFriends.get())
            || VectorUtils.distance(mc.player.getPos(), entity.getPos()) > range.get()) return false;

        if (checkTargets.get()) {
            boolean target = true;

            for (Module module : Modules.get().getAll()) {
                if (module.getInfoString() != null && module.getInfoString().contains(entity.getEntityName())) {
                    target = false;
                    break;
                }
            }

            return !target;
        }

        return true;
    }

    private enum Armor {
        Helm,
        Chest,
        Legs,
        Boots,
        None
    }

    public static boolean isHelm(Item item) {
        if (item == null) return false;
        if (item == Items.NETHERITE_HELMET) return true;
        if (item == Items.DIAMOND_HELMET) return true;
        if (item == Items.GOLDEN_HELMET) return true;
        if (item == Items.IRON_HELMET) return true;
        if (item == Items.CHAINMAIL_HELMET) return true;
        return item == Items.LEATHER_HELMET;
    }

    public static boolean isChest(Item item) {
        if (item == Items.NETHERITE_CHESTPLATE) return true;
        if (item == Items.DIAMOND_CHESTPLATE) return true;
        if (item == Items.GOLDEN_CHESTPLATE) return true;
        if (item == Items.IRON_CHESTPLATE) return true;
        if (item == Items.CHAINMAIL_CHESTPLATE) return true;
        return item == Items.LEATHER_CHESTPLATE;
    }

    public static boolean isLegs(Item item) {
        if (item == Items.NETHERITE_LEGGINGS) return true;
        if (item == Items.DIAMOND_LEGGINGS) return true;
        if (item == Items.GOLDEN_LEGGINGS) return true;
        if (item == Items.IRON_LEGGINGS) return true;
        if (item == Items.CHAINMAIL_LEGGINGS) return true;
        return item == Items.LEATHER_LEGGINGS;
    }

    public static boolean isBoots(Item item) {
        if (item == Items.NETHERITE_BOOTS) return true;
        if (item == Items.DIAMOND_BOOTS) return true;
        if (item == Items.GOLDEN_BOOTS) return true;
        if (item == Items.IRON_BOOTS) return true;
        if (item == Items.CHAINMAIL_BOOTS) return true;
        return item == Items.LEATHER_BOOTS;
    }
}
