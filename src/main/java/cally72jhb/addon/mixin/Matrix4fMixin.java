package cally72jhb.addon.mixin;

import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix4f.class)
public interface Matrix4fMixin {
    // Meteor doesn't allow it with a separate interface ..

    @Mutable @Accessor("a00") void set00(float value);
    @Mutable @Accessor("a01") void set01(float value);
    @Mutable @Accessor("a02") void set02(float value);
    @Mutable @Accessor("a03") void set03(float value);
    @Mutable @Accessor("a10") void set10(float value);
    @Mutable @Accessor("a11") void set11(float value);
    @Mutable @Accessor("a12") void set12(float value);
    @Mutable @Accessor("a13") void set13(float value);
    @Mutable @Accessor("a20") void set20(float value);
    @Mutable @Accessor("a21") void set21(float value);
    @Mutable @Accessor("a22") void set22(float value);
    @Mutable @Accessor("a23") void set23(float value);
    @Mutable @Accessor("a30") void set30(float value);
    @Mutable @Accessor("a31") void set31(float value);
    @Mutable @Accessor("a32") void set32(float value);
    @Mutable @Accessor("a33") void set33(float value);
}
