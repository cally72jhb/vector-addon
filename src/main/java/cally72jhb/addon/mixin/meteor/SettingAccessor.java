package cally72jhb.addon.mixin.meteor;

import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Setting.class)
public interface SettingAccessor {
    @Mutable
    @Accessor("defaultValue")
    void setDefaultValue(Object value);

    @Mutable
    @Accessor("value")
    void setValue(Object value);

    @Mutable
    @Accessor("name")
    void setName(String name);

    @Mutable
    @Accessor("title")
    void setTitle(String name);

    @Mutable
    @Accessor("description")
    void setDescription(String name);

    @Mutable
    @Accessor("visible")
    void setName(IVisible visible);
}
