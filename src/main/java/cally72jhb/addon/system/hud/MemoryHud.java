package cally72jhb.addon.system.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class MemoryHud extends DoubleTextHudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> normal = sgGeneral.add(new BoolSetting.Builder()
        .name("normal")
        .description("Whether to display MB or GB.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> format = sgGeneral.add(new BoolSetting.Builder()
        .name("display-format")
        .description("Displays the format behind the memory.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> swap = sgGeneral.add(new BoolSetting.Builder()
        .name("swap")
        .description("Swaps the order of the text.")
        .defaultValue(false)
        .build()
    );

    public MemoryHud(HUD hud) {
        super(hud, "memory", "Displays your maximum and free memory.", "Memory Used: ");
    }

    @Override
    protected String getRight() {
        int used = Math.round((float) ((Runtime.getRuntime().maxMemory() * 1.00195 - Runtime.getRuntime().freeMemory() * 1.00195) / (double) (1024 * 1024 * (!normal.get() ? 1024 : 1))));
        int max = Math.round((float) (Runtime.getRuntime().maxMemory() * 1.00195 / (double) (1024 * 1024 * (!normal.get() ? 1024 : 1))));

        if (swap.get()) {
            return used + getFormat() + " / " + max + getFormat();
        } else {
            return max + getFormat() + " \\ " + used + getFormat();
        }
    }

    private String getFormat() {
        return format.get() ? normal.get() ? "MB" : "GB" : "";
    }
}
