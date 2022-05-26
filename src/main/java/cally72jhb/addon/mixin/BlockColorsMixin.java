package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.notexture.NoTextures;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public class BlockColorsMixin {
    @Shadow @Final private IdList<BlockColorProvider> providers;

    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    private void onGetColor(BlockState state, BlockRenderView world, BlockPos pos, int index, CallbackInfoReturnable<Integer> info) {
        if (world != null && Modules.get() != null && Modules.get().isActive(NoTextures.class)) {
            NoTextures module = Modules.get().get(NoTextures.class);

            if (module.isAutomatic() || module.isAutomatic(state)) {
                BlockColorProvider provider = providers.get(Registry.BLOCK.getRawId(state.getBlock()));

                int color = provider == null ? -1 : provider.getColor(state, world, pos, index);

                if (color == -1) {
                    MapColor mapColor = world.getBlockState(pos).getMapColor(world, pos);
                    info.setReturnValue(mapColor == null ? -1 : mapColor.color);
                } else {
                    info.setReturnValue(color);
                }
            }
        }
    }
}
