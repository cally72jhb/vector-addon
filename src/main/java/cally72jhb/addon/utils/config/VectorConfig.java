package cally72jhb.addon.utils.config;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.NbtCompound;

public class VectorConfig extends System<VectorConfig> {
    public Version version;

    public String clientName;
    public String clientPrefix;
    public String clientSuffix;

    public boolean windowIcon;
    public boolean highlightMembers;

    public SettingColor memberColor;

    public VectorConfig() {
        super("vector-config");
        init();
        load(MeteorClient.FOLDER);

        if (!FabricLoader.getInstance().getModContainer("vector-addon").isPresent()) return;
        ModMetadata metadata = FabricLoader.getInstance().getModContainer("vector-addon").get().getMetadata();

        String versionString = metadata.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];

        version = new Version(versionString);
    }

    public static VectorConfig get() {
        return Systems.get(VectorConfig.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("clientname", clientName);
        tag.putString("clientsuffix", clientPrefix);
        tag.putString("clientsuffix", clientSuffix);
        tag.putBoolean("icon", windowIcon);
        tag.putBoolean("members", highlightMembers);
        tag.put("color", memberColor.toTag());

        return tag;
    }

    @Override
    public VectorConfig fromTag(NbtCompound tag) {
        clientName = tag.getString("clientname");
        clientPrefix = tag.getString("clientsuffix");
        clientSuffix = tag.getString("clientsuffix");
        windowIcon = tag.getBoolean("icon");
        highlightMembers = tag.getBoolean("members");
        if (tag.contains("color")) {
            if (memberColor == null) memberColor = new SettingColor();
            memberColor.fromTag(tag.getCompound("color"));
        }

        return this;
    }
}
