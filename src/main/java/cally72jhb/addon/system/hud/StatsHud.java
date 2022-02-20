package cally72jhb.addon.system.hud;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.misc.Stats;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;

public class StatsHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> header = sgGeneral.add(new BoolSetting.Builder()
        .name("header")
        .description("Renders a header over the stats.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores friends.")
        .defaultValue(true)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Boolean> checkTargets = sgGeneral.add(new BoolSetting.Builder()
        .name("check-targets")
        .description("Checks if any of the active modules targets a player that died / poped near you.")
        .defaultValue(false)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Boolean> clearPopsOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("clear-pos-on-death")
        .description("Resets your pop scores on death.")
        .defaultValue(true)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Boolean> clearKillsOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("clear-kills-on-death")
        .description("Resets your kill scores on death.")
        .defaultValue(true)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Boolean> pops = sgGeneral.add(new BoolSetting.Builder()
        .name("pops")
        .description("Shows how often you poped players.")
        .defaultValue(true)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Boolean> kills = sgGeneral.add(new BoolSetting.Builder()
        .name("kills")
        .description("Shows how often you killed players.")
        .defaultValue(true)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Boolean> deaths = sgGeneral.add(new BoolSetting.Builder()
        .name("deaths")
        .description("Shows how often you poped players.")
        .defaultValue(true)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("deaths")
        .description("Shows how often you poped players.")
        .defaultValue(7.5)
        .min(2)
        .max(50)
        .sliderMin(2)
        .sliderMax(10)
        .onChanged(changed -> updateChanges())
        .build()
    );

    private Stats scores;

    private int allPops = 0;
    private int allKills = 0;
    private int allDeaths = 0;

    public StatsHud(HUD hud) {
        super(hud, "stats", "Displays if selected modules are enabled or disabled.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        if (scores == null) scores = VectorAddon.scores;

        double width = 0;
        double height = 0;

        if (header.get()) height += renderer.textHeight();

        if (pops.get()) {
            width += renderer.textWidth("Pops: " + allPops);
            height += renderer.textHeight();
        }
        if (kills.get()) {
            width += renderer.textWidth("Kills: " + allKills);
            height += renderer.textHeight();
        }
        if (deaths.get()) {
            if (renderer.textWidth("Deaths: " + allDeaths) > width) width = renderer.textWidth("Deaths: " + allDeaths);
            height += renderer.textHeight();
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (scores != null) {
            allPops = isInEditor() && mc.world == null ? 18 : scores.allPops;
            allKills = isInEditor() && mc.world == null ? 5 : scores.allKills;
            allDeaths = isInEditor() && mc.world == null ? 0 : scores.deaths;

            double x = box.getX();
            double y = box.getY();

            if (header.get()) {
                renderer.text("Scores:", x, y, hud.primaryColor.get());
                y += renderer.textHeight();
            }

            if (pops.get()) {
                renderer.text("Pops:", x, y, hud.primaryColor.get());
                x += renderer.textWidth("Pops:");
                renderer.text(" " + allPops, x, y, hud.secondaryColor.get());
                y += renderer.textHeight();
            }
            if (kills.get()) {
                x = box.getX();
                renderer.text("Kills:", x, y, hud.primaryColor.get());
                x += renderer.textWidth("Kills:");
                renderer.text(" " + allKills, x, y, hud.secondaryColor.get());
                y += renderer.textHeight();
            }
            if (deaths.get()) {
                x = box.getX();
                renderer.text("Deaths:", x, y, hud.primaryColor.get());
                x += renderer.textWidth("Deaths:");
                renderer.text(" " + allDeaths, x, y, hud.secondaryColor.get());
            }
        }
    }

    private void updateChanges() {
        if (scores != null) {
            scores.setIgnoreFriends(ignoreFriends.get());
            scores.setClearPopsOnDeath(clearPopsOnDeath.get());
            scores.setClearKillsOnDeath(clearKillsOnDeath.get());
            scores.setCheckTargets(checkTargets.get());
            scores.setRange(range.get());
        }
    }
}
