package cally72jhb.addon.mixin.meteor;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cally72jhb.addon.utils.VectorUtils.mc;

@Mixin(Sprint.class)
public class SprintMixin {
    @Shadow @Final private SettingGroup sgGeneral;

    private Setting<Boolean> disable = null;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo info) {
        ((SettingAccessor) sgGeneral.get("when-stationary")).setDefaultValue(false);

        disable = sgGeneral.add(new BoolSetting.Builder()
                .name("disable-afterwards")
                .description("Stops sprinting when you deactivate the module.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onDeactivate", at = @At("HEAD"), remap = false, cancellable = true)
    private void onDeactivate(CallbackInfo info) {
        info.cancel();

        if (mc.player == null || disable == null || !disable.get()) return;

        mc.player.setSprinting(false);
    }
}
