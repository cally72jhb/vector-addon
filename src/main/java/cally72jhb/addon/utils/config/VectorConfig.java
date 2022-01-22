package cally72jhb.addon.utils.config;

import cally72jhb.addon.gui.tabs.VectorConfigTab;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.NbtCompound;

public class VectorConfig extends System<VectorConfig> {
    public static Version version;

    public String clientName = VectorConfigTab.clientName.get();
    public String clientPrefix = VectorConfigTab.clientPrefix.get();
    public String clientSuffix = VectorConfigTab.clientSuffix.get();

    public boolean windowIcon = VectorConfigTab.windowIcon.get();
    public boolean highlightMembers = VectorConfigTab.highlightMembers.get();

    public SettingColor memberColor = VectorConfigTab.memberColor.get();

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
    public void init() {
        RainbowColors.add(memberColor);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", clientName);
        tag.putString("suffix", clientPrefix);
        tag.putString("suffix", clientSuffix);
        tag.putBoolean("icon", windowIcon);
        tag.putBoolean("members", highlightMembers);
        tag.put("color", memberColor.toTag());

        return tag;
    }

    @Override
    public VectorConfig fromTag(NbtCompound tag) {
        clientName = getString(tag, "name", VectorConfigTab.clientName);
        clientPrefix = getString(tag, "suffix", VectorConfigTab.clientPrefix);
        clientSuffix = getString(tag, "suffix", VectorConfigTab.clientSuffix);
        windowIcon = getBoolean(tag, "icon", VectorConfigTab.windowIcon);
        highlightMembers = getBoolean(tag, "members", VectorConfigTab.highlightMembers);

        if (tag.contains("color")) {
            memberColor.fromTag(tag.getCompound("color"));
        } else {
            memberColor = VectorConfigTab.memberColor.getDefaultValue();
        }

        return this;
    }

    // Utils

    private boolean getBoolean(NbtCompound tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.getDefaultValue();
    }

    private String getString(NbtCompound tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.getDefaultValue();
    }
}
