package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;

import java.util.*;

public class ArmorAlert extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("message")
        .description("Sends messages in the chat when you kill or pop players.")
        .defaultValue(true)
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

    // Armor

    private final Setting<Boolean> armorMsg = sgArmor.add(new BoolSetting.Builder()
        .name("armor")
        .description("Sends a messages everytime you break a armor piece of a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> armorString = sgArmor.add(new StringSetting.Builder()
        .name("armor-message")
        .description("The message to send when you broke a armor piece of a player.")
        .defaultValue("broke {player}'s {piece}")
        .visible(() -> !randomMsg.get() && armorMsg.get())
        .build()
    );

    private final Setting<List<String>> armorMessages = sgArmor.add(new StringListSetting.Builder()
        .name("armor-messages")
        .description("The random messages to send when you broke a armor piece of a player.")
        .defaultValue(List.of("broke {player}'s armor", "broke {player}'s {piece}", "broke {player}'s armor with vector"))
        .visible(() -> randomMsg.get() && armorMsg.get())
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

    private HashMap<UUID, ArrayList<Armor>> armor;
    private Random random;

    public ArmorAlert() {
        super(VectorAddon.MISC, "armor-alert", "Sends a chat message when a players armor gets on low durability.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @Override
    public void onActivate() {
        armor = new HashMap<>();
        random = new Random();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityEquipmentUpdateS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getId());

            if (!check(entity) || packet.getEquipmentList() == null || packet.getEquipmentList().isEmpty()) return;

            ArrayList<Armor> stacks = new ArrayList<>();

            for (ItemStack stack : entity.getArmorItems()) {
                Item item = stack.getItem();

                Armor armor = isHelm(item) ? Armor.Helm : (isChest(item) ? Armor.Chest : (isLegs(item) ? Armor.Legs : (isBoots(item) ? Armor.Boots : Armor.None)));

                if (armor != Armor.None) stacks.add(armor);
            }

            if (armor.containsKey(entity.getUuid()) && !stacks.isEmpty()) {
                if (mc.options.keySneak.isPressed()) {
                    info(stacks + " -- ");
                    info(armor.get(entity.getUuid()) + " ++ ");
                }

                ArrayList<Armor> old = stacks;

                stacks.retainAll(armor.get(entity.getUuid()));

                if (stacks.size() < armor.get(entity.getUuid()).size() && !stacks.isEmpty()) {
                    sendArmorMsg((PlayerEntity) entity, stacks.get(0));

                    if (mc.options.keySneak.isPressed()) info(stacks + " == ");

                    //armor.remove(entity.getUuid());
                } else if (!old.isEmpty()) {
                    armor.replace(entity.getUuid(), old);
                }
            } else {
                armor.putIfAbsent(entity.getUuid(), stacks);
            }
        }
    }

    // Messaging

    private void sendArmorMsg(PlayerEntity player, Armor piece) {
        if (piece != Armor.None) {
            String string = apply(player, randomMsg.get() ? armorMessages.get().get(random.nextInt(armorMessages.get().size())) : armorString.get(), piece);

            info(string);
        }
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

    private String apply(PlayerEntity player, String message, Armor piece) {
        String string = message.replace("{player}", player.getEntityName());

        string = string.replace("{piece}", piece.name());

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
