package cally72jhb.addon.system.players;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Players extends System<Players> implements Iterable<Player> {
    private List<Player> players = new ArrayList<>();

    public final SettingColor color = new SettingColor(255, 85, 85);
    public boolean muted = false;

    public Players() {
        super("players");
    }

    public static Players get() {
        return Systems.get(Players.class);
    }

    @Override
    public void init() {
        RainbowColors.add(color);
    }

    public boolean add(Player player) {
        if (player.name.isEmpty()) return false;

        if (!players.contains(player)) {
            players.add(player);
            save();

            return true;
        }

        return false;
    }

    public boolean remove(Player player) {
        if (players.remove(player)) {
            save();
            return true;
        }

        return false;
    }

    public Player get(String name) {
        for (Player player : players) {
            if (player.name.equals(name)) {
                return player;
            }
        }

        return null;
    }

    public Player get(PlayerEntity player) {
        return get(player.getEntityName());
    }

    public boolean isListed(PlayerEntity player) {
        return get(player) != null;
    }

    public boolean isListed(Player player) {
        for (Player p : players) {
            if (p == player) {
                return true;
            }
        }

        return false;
    }

    public boolean isMuted(PlayerEntity player) {
        return isListed(player) && get(player).muted;
    }

    public boolean isTargeted(PlayerEntity player) {
        return isListed(player) && get(player).target;
    }

    public void mute(PlayerEntity player) {
        if (!isListed(player)) add(new Player(player.getEntityName()));

        Players.get().get(player).mute();
    }

    public void mute(String player) {
        if (!isListed(new Player(player))) add(new Player(player));

        Players.get().get(player).mute();
    }

    public void target(PlayerEntity player) {
        if (!isListed(player)) add(new Player(player.getEntityName()));

        Players.get().get(player).target();
    }

    public void target(String player) {
        if (!isListed(new Player(player))) add(new Player(player));

        Players.get().get(player).target();
    }

    public int count() {
        return players.size();
    }

    @Override
    public @NotNull Iterator<Player> iterator() {
        return players.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        NbtList playersTag = new NbtList();

        for (Player player : players) playersTag.add(player.toTag());
        tag.put("players", playersTag);
        tag.put("color", color.toTag());
        tag.putBoolean("muted", muted);

        return tag;
    }

    @Override
    public Players fromTag(NbtCompound tag) {
        players = NbtUtils.listFromTag(tag.getList("players", 10), tag1 -> new Player((NbtCompound) tag1));
        if (tag.contains("color")) color.fromTag(tag.getCompound("color"));
        muted = tag.contains("muted") && tag.getBoolean("muted");
        return this;
    }
}
