package cally72jhb.addon.modules.movement;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class PacketDigits extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> digits = sgGeneral.add(new IntSetting.Builder()
            .name("digits")
            .description("How many digits to remove.")
            .defaultValue(2)
            .sliderMin(0)
            .sliderMax(5)
            .noSlider()
            .build()
    );

    private final Setting<Boolean> modifyY = sgGeneral.add(new BoolSetting.Builder()
            .name("modify-y")
            .description("Rounds your y coordinate.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> firstPacket = sgGeneral.add(new BoolSetting.Builder()
            .name("first-packet")
            .description("Modifies the first movement packet you send when joining a server. (note: this settings doesn't depend on this module being activated)")
            .defaultValue(false)
            .build()
    );

    // Constructor

    public PacketDigits() {
        super(Categories.Movement, "packet-digits", "Removes digits from your movement packets to make them smaller.");
    }

    // Getter

    public boolean shouldModifyFirstPacket() {
        return firstPacket.get();
    }

    public boolean shouldModifyY() {
        return modifyY.get();
    }

    // Utils

    public double round(double value) {
        int digit = (int) Math.pow(10, digits.get());
        double round = ((double) (Math.round(value * digit)) / digit);
        return Math.nextAfter(round, round + Math.signum(round));
    }
}
