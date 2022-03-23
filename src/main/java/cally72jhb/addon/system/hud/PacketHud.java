package cally72jhb.addon.system.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;

public class PacketHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> swap = sgGeneral.add(new BoolSetting.Builder()
        .name("swap")
        .description("Swaps the order of the text.")
        .defaultValue(false)
        .build()
    );

    public PacketHud(HUD hud) {
        super(hud, "packet-hud", "Displays your average send packets.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        int send = mc.getNetworkHandler() == null ? 23 : (int) mc.getNetworkHandler().getConnection().getAveragePacketsSent();
        int received = mc.getNetworkHandler() == null ? 86 : (int) mc.getNetworkHandler().getConnection().getAveragePacketsReceived();

        double width = 0;
        double height = 0;

        width += renderer.textWidth("Send: " + send + 10);
        height += renderer.textHeight() * 2;
        if (renderer.textWidth("Received: " + received + 10) > width) width = renderer.textWidth("Received: " + received + 10);

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        int send = mc.getNetworkHandler() == null ? 23 : (int) mc.getNetworkHandler().getConnection().getAveragePacketsSent();
        int received = mc.getNetworkHandler() == null ? 86 : (int) mc.getNetworkHandler().getConnection().getAveragePacketsReceived();

        double x = box.getX();
        double y = box.getY();

        if (swap.get()) {
            renderer.text("Received: ", x, y, hud.primaryColor.get());
            x += renderer.textWidth("Received: ");
            renderer.text("" + received, x, y, hud.secondaryColor.get());
        } else {
            renderer.text("Send: ", x, y, hud.primaryColor.get());
            x += renderer.textWidth("Send: ");
            renderer.text("" + send, x, y, hud.secondaryColor.get());
        }

        y += renderer.textHeight();
        x = box.getX();

        if (swap.get()) {
            renderer.text("Send: ", x, y, hud.primaryColor.get());
            x += renderer.textWidth("Send: ");
            renderer.text("" + send, x, y, hud.secondaryColor.get());
        } else {
            renderer.text("Received: ", x, y, hud.primaryColor.get());
            x += renderer.textWidth("Received: ");
            renderer.text("" + received, x, y, hud.secondaryColor.get());
        }
    }
}
