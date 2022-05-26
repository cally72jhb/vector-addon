package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.notexture.NoTextures;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LeavesBlock.class)
@Environment(EnvType.CLIENT)
public class LeavesBlockMixin extends Block {
    public LeavesBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSideInvisible(BlockState state, BlockState neighborState, Direction offset) {
        if (Modules.get() != null && Modules.get().isActive(NoTextures.class) && Modules.get().get(NoTextures.class).disableCulling()) {
            return neighborState.getBlock() instanceof LeavesBlock;
        }

        return false;
    }
}
