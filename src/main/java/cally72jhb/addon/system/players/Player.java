package cally72jhb.addon.system.players;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

public class Player implements ISerializable<Player> {
    public String name;
    public boolean muted = false;
    public boolean target = false;

    public Player(String name) {
        this.name = name;
    }
    public Player(NbtCompound tag) {
        fromTag(tag);
    }
    public Player(PlayerEntity player) {
        this(player.getEntityName());
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("name", name);
        tag.putBoolean("muted", muted);
        tag.putBoolean("target", target);
        return tag;
    }

    @Override
    public Player fromTag(NbtCompound tag) {
        name = tag.getString("name");
        muted = tag.getBoolean("muted");
        target = tag.getBoolean("target");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(name, player.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public void mute() {
        muted = !muted;

        if (muted) ChatUtils.info("muted " + name);
        if (!muted) ChatUtils.info("unmuted " + name);
    }

    public void target() {
        target = !target;

        if (target) ChatUtils.info("targeting " + name);
        if (!target) ChatUtils.info("untargeting " + name);
    }
}
