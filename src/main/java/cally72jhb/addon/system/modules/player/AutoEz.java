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
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.*;

public class AutoEz extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");

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

    private final Setting<Boolean> clearOnDeath = sgGeneral.add(new BoolSetting.Builder()
            .name("clear-on-death")
            .description("Resets your scores on death.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> randomMsg = sgGeneral.add(new BoolSetting.Builder()
            .name("random")
            .description("Sends random messages every kill or pop.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> popString = sgGeneral.add(new StringSetting.Builder()
            .name("pop-message")
            .description("The message to send when you poped a player.")
            .defaultValue("ez pop {player}")
            .visible(() -> !randomMsg.get())
            .build()
    );

    private final Setting<String> killString = sgGeneral.add(new StringSetting.Builder()
            .name("kill-message")
            .description("The message to send when you killed someone.")
            .defaultValue("killed {player}")
            .visible(() -> !randomMsg.get())
            .build()
    );

    private final Setting<List<String>> popMessages = sgGeneral.add(new StringListSetting.Builder()
            .name("pop-messages")
            .description("The random messages to send when you poped a player.")
            .defaultValue(List.of("vector on top", "ez {player}", "poped {player} with vector", "{pops} on {player} already"))
            .visible(randomMsg::get)
            .build()
    );

    private final Setting<List<String>> killMessages = sgGeneral.add(new StringListSetting.Builder()
            .name("kill-messages")
            .description("The random messages to send when you kill someone.")
            .defaultValue(List.of("ez {player}", "killed {player} with vector", "currently at {kills} kill streak", "{playerkills} on {player} already"))
            .visible(randomMsg::get)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("How long to wait in ticks after sending a message again.")
            .defaultValue(40)
            .min(0)
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

    private final Setting<Double> minPops = sgTargeting.add(new DoubleSetting.Builder()
            .name("minimum-pops")
            .description("The minimum pops required to send a message that contains {pops}.")
            .defaultValue(7)
            .min(1)
            .build()
    );

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    private HashMap<UUID, Integer> kills;
    private HashMap<UUID, Integer> pops;
    private Random random;
    private int allKills;
    private int timer;

    public AutoEz() {
        super(VectorAddon.CATEGORY, "auto-ez", "Send a chat message after killing a player.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @Override
    public void onActivate() {
        kills = new HashMap<>();
        pops = new HashMap<>();
        random = new Random();
        allKills = 0;
        timer = 0;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 35 && packet.getStatus() != 3) return;
        if (packet.getStatus() == 3 && packet.getEntity(mc.world) == mc.player && clearOnDeath.get()) onActivate();

        if (timer >= delay.get() || delay.get() == 0) {
            Entity entity = packet.getEntity(mc.world);

            if (!(entity instanceof PlayerEntity) || entity == mc.player
                    || (Friends.get().isFriend((PlayerEntity) entity) && ignoreFriends.get())
                    || VectorUtils.distance(mc.player.getPos(), entity.getPos()) > range.get()) return;

            if (checkTargets.get()) {
                boolean target = true;

                for (Module module : Modules.get().getAll()) {
                    if (module.getInfoString() != null && module.getInfoString().contains(entity.getEntityName())) {
                        target = false;
                        break;
                    }
                }

                if (target) return;
            }

            timer = 0;

            if (packet.getStatus() == 35) sendPopMsg((PlayerEntity) entity);
            if (packet.getStatus() == 3) sendKillMsg((PlayerEntity) entity);
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        timer++;
    }

    // Messaging

    private void sendPopMsg(PlayerEntity player) {
        pops.putIfAbsent(player.getUuid(), 0);
        pops.replace(player.getUuid(), pops.get(player.getUuid()) + 1);

        String string = apply(player, randomMsg.get() ? popMessages.get() : List.of(popString.get()));

        sendMsg(string);
    }

    private void sendKillMsg(PlayerEntity player) {
        kills.putIfAbsent(player.getUuid(), 0);
        kills.replace(player.getUuid(), kills.get(player.getUuid()) + 1);

        allKills++;

        String string = apply(player, randomMsg.get() ? killMessages.get() : List.of(killString.get()));

        if (message.get()) sendMsg(string);

        int pop = pops.get(player.getUuid()) == null ? 0 : pops.get(player.getUuid());
        int kill = kills.get(player.getUuid()) == null ? 0 : kills.get(player.getUuid());
        if (notify.get()) info("poped " + player.getEntityName()
                + " " + pop + (pop == 1 ? " time" : " times") + " and killed him "
                + kill + (kill == 1 ? " time." : " times."));
    }

    // Utils

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

    private String apply(PlayerEntity player, List<String> strings) {
        String string = strings.get(random.nextInt(strings.size())).replace("{player}", player.getEntityName());

        if (string.contains("{pops}") && pops.get(player.getUuid()) <= minPops.get()) {
            string = string.replace("{pops}", String.valueOf(pops.get(player.getUuid())));
        }

        string = string.replace("{playerkills}", String.valueOf(kills.get(player.getUuid())));
        string = string.replace("{kills}", String.valueOf(allKills));

        return string;
    }
}
