package cally72jhb.addon.utils.titlescreen;

import cally72jhb.addon.utils.titlescreen.modules.ScreenElement;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public class TitleScreenConfig extends System<TitleScreenConfig> {
    public TitleScreenConfig() {
        super("title-screen");
        init();
        load(MeteorClient.FOLDER);
    }

    public static TitleScreenConfig get() {
        return Systems.get(TitleScreenConfig.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        NbtList modulesTag = new NbtList();
        for (ScreenElement module : CustomTitleScreen.elements) modulesTag.add(module.toTag());
        tag.put("elements", modulesTag);

        return tag;
    }

    @Override
    public TitleScreenConfig fromTag(NbtCompound tag) {
        if (tag.contains("elements")) {
            NbtList modulesTag = tag.getList("elements", 10);

            for (NbtElement t : modulesTag) {
                NbtCompound moduleTag = (NbtCompound) t;

                ScreenElement module = getModule(moduleTag.getString("name"));
                if (module != null) module.fromTag(moduleTag);
            }
        }

        return this;
    }

    // Utils

    private ScreenElement getModule(String name) {
        for (ScreenElement element : CustomTitleScreen.elements) {
            if (element.name.equals(name)) return element;
        }

        return null;
    }

    private boolean getBoolean(NbtCompound tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.getDefaultValue();
    }

    private String getString(NbtCompound tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.getDefaultValue();
    }
}
