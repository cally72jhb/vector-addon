package cally72jhb.addon.utils.titlescreen.modules;

import cally72jhb.addon.utils.titlescreen.BoundingBox;
import cally72jhb.addon.utils.titlescreen.CustomTitleScreen;
import cally72jhb.addon.utils.titlescreen.ElementScreen;
import cally72jhb.addon.utils.titlescreen.ScreenRenderer;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;

public abstract class ScreenElement implements ISerializable<ScreenElement> {
    public final String name, title;
    public final String description;

    public boolean active;
    public final boolean defaultActive;

    protected final CustomTitleScreen screen;

    public final Settings settings = new Settings();
    public final BoundingBox box = new BoundingBox();

    protected final MinecraftClient mc;

    public ScreenElement(CustomTitleScreen screen, String name, String description, boolean defaultActive) {
        this.screen = screen;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.defaultActive = defaultActive;
        this.mc = MinecraftClient.getInstance();
    }

    public void toggle() {
        active = !active;
    }

    public abstract void update(ScreenRenderer renderer);

    public abstract void render(ScreenRenderer renderer);

    protected boolean isEditing() {
        return (CustomTitleScreen.editing || mc.currentScreen instanceof ElementScreen);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putBoolean("active", active);
        tag.put("settings", settings.toTag());
        tag.put("box", box.toTag());

        return tag;
    }

    @Override
    public ScreenElement fromTag(NbtCompound tag) {
        active = tag.contains("active") ? tag.getBoolean("active") : defaultActive;
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));
        box.fromTag(tag.getCompound("box"));

        return this;
    }

    public void onPress() {

    }
}
