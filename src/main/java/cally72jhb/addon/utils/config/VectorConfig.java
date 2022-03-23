package cally72jhb.addon.utils.config;

import cally72jhb.addon.gui.tabs.VectorConfigTab;
import cally72jhb.addon.system.Systems;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.NbtCompound;

public class VectorConfig extends System<VectorConfig> {
    public static Version version;

    public String name = VectorConfigTab.name.get();
    public String prefix = VectorConfigTab.prefix.get();
    public String suffix = VectorConfigTab.suffix.get();
    public SettingColor nameColor = VectorConfigTab.nameColor.get();
    public SettingColor otherColor = VectorConfigTab.otherColor.get();

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
        RainbowColors.add(nameColor);
        RainbowColors.add(memberColor);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putString("prefix", prefix);
        tag.putString("suffix", suffix);
        tag.put("name-color", nameColor.toTag());
        tag.put("other-color", otherColor.toTag());
        tag.putBoolean("icon", windowIcon);
        tag.putBoolean("members", highlightMembers);
        tag.put("member-color", memberColor.toTag());

        return tag;
    }

    @Override
    public VectorConfig fromTag(NbtCompound tag) {
        name = getString(tag, "name", VectorConfigTab.name);
        prefix = getString(tag, "prefix", VectorConfigTab.prefix);
        suffix = getString(tag, "suffix", VectorConfigTab.suffix);
        windowIcon = getBoolean(tag, "icon", VectorConfigTab.windowIcon);
        highlightMembers = getBoolean(tag, "members", VectorConfigTab.highlightMembers);

        nameColor = getColor(tag, "name-color", VectorConfigTab.nameColor);
        otherColor = getColor(tag, "other-color", VectorConfigTab.otherColor);
        memberColor = getColor(tag, "member-color", VectorConfigTab.memberColor);

        return this;
    }

    // Utils

    private boolean getBoolean(NbtCompound tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.getDefaultValue();
    }

    private String getString(NbtCompound tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.getDefaultValue();
    }

    private SettingColor getColor(NbtCompound tag, String key, Setting<SettingColor> setting) {
        return tag.contains(key) ? new SettingColor(0, 0, 0).fromTag(tag.getCompound(key)) : setting.getDefaultValue();
    }
}
