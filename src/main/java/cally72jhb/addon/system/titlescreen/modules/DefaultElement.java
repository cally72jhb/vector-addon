package cally72jhb.addon.system.titlescreen.modules;

import cally72jhb.addon.gui.screens.TitleScreenEditor;
import cally72jhb.addon.gui.screens.TitleScreenElementScreen;
import cally72jhb.addon.system.titlescreen.BoundingBox;
import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.system.titlescreen.TitleScreenRenderer;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;

public abstract class DefaultElement implements ISerializable<DefaultElement> {
    public final String name, title;
    public final String description;

    public boolean active;
    public final boolean defaultActive;

    protected final TitleScreenManager screen;

    public final Settings settings = new Settings();
    public final BoundingBox box = new BoundingBox();

    protected final MinecraftClient mc;

    public DefaultElement(TitleScreenManager screen, String name, String description, boolean defaultActive) {
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

    public abstract void update(TitleScreenRenderer renderer);

    public abstract void render(TitleScreenRenderer renderer);

    protected boolean isEditing() {
        return (mc.currentScreen instanceof TitleScreenEditor || mc.currentScreen instanceof TitleScreenElementScreen || !Utils.canUpdate());
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
    public DefaultElement fromTag(NbtCompound tag) {
        active = tag.contains("active") ? tag.getBoolean("active") : defaultActive;
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));
        box.fromTag(tag.getCompound("box"));

        return this;
    }

    public void onPress() {

    }
}
