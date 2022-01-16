package cally72jhb.addon.system.hud;

import com.google.gson.Gson;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.ProfileResponse;
import meteordevelopment.meteorclient.systems.accounts.TexturesJson;
import meteordevelopment.meteorclient.systems.accounts.UuidToProfileResponse;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

public class RadarHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBox = settings.createGroup("Radar Box");

    private final Setting<Double> playerScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the players displayed on the radar.")
        .defaultValue(7.5)
        .min(0)
        .max(25)
        .sliderMin(0)
        .build()
    );

    private final Setting<Double> zoom = sgGeneral.add(new DoubleSetting.Builder()
        .name("zoom")
        .description("How much the radar is zoomed out.")
        .defaultValue(2.5)
        .min(0.1)
        .max(10)
        .sliderMin(0.1)
        .sliderMax(3)
        .build()
    );

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
        .name("width")
        .description("The width of the box.")
        .defaultValue(200)
        .min(50)
        .max(500)
        .sliderMin(50)
        .sliderMax(300)
        .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("The height of the box.")
        .defaultValue(200)
        .min(50)
        .max(500)
        .sliderMin(50)
        .sliderMax(300)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Default color of players.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Boolean> texture = sgGeneral.add(new BoolSetting.Builder()
        .name("texture")
        .description("Displays the players head instead of his name.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> background = sgBox.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background behind the players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBox.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of background.")
        .defaultValue(new SettingColor(0, 0, 0, 130))
        .visible(background::get)
        .build()
    );

    private HashMap<String, Texture> textures = new HashMap<>();

    public RadarHud(HUD hud) {
        super(hud, "radar", "Displays a player radar.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(width.get(), height.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        if (background.get()) {
            double x = box.getX();
            double y = box.getY();

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x - 5, y - 5, box.width + 10, box.height + 10, backgroundColor.get());
            Renderer2D.COLOR.render(null);
        }

        if (isInEditor() && (mc.world == null || mc.player == null) || mc.world == null || mc.player == null) return;

        double scale = playerScale.get() + 10;

        if (texture.get()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player) {
                    double x = box.getX() + box.width / 2 + getPosRelativeToPlayer(player).getX() - scale / 2;
                    double y = box.getY() + box.height / 2 + getPosRelativeToPlayer(player).getZ() - scale / 2;

                    if (box.isOver(x, y) && box.isOver(x + scale, y + scale)) renderPlayerHead(player.getGameProfile().getName(), x, y, scale);
                }
            }

            double x = box.getX() + box.width / 2 - scale / 2;
            double y = box.getY() + box.height / 2 - scale / 2;

            renderPlayerHead(mc.player.getGameProfile().getName(), x, y, scale);
        } else {
            for (PlayerEntity player : mc.world.getPlayers()) {
                double x = box.getX() + box.width / 2 + getPosRelativeToPlayer(player).getX() - TextRenderer.get().getWidth(player.getGameProfile().getName()) / 2;
                double y = box.getY() + box.height / 2 + getPosRelativeToPlayer(player).getZ() - TextRenderer.get().getHeight() / 2;

                if (box.isOver(x, y) && box.isOver(x + TextRenderer.get().getWidth(player.getGameProfile().getName()), y + TextRenderer.get().getHeight() / 2 )) {
                    renderer.text(player.getGameProfile().getName(), x, y, PlayerUtils.getPlayerColor(player, color.get()));
                }
            }
        }
    }

    private Vec3d getPosRelativeToPlayer(PlayerEntity player) {
        return mc.player.getPos().subtract(player.getPos()).rotateY((float) Math.toRadians(mc.player.getYaw())).multiply(zoom.get());
    }

    private void renderPlayerHead(String name, double x, double y, double scale) {
        if (textures.containsKey(name) && textures.get(name) != null) {
            Texture texture = textures.get(name);

            texture.bind();

            Renderer2D.TEXTURE.begin();
            Renderer2D.TEXTURE.texQuad(x, y, scale, scale, 90, 0, 0, 1, 1, new Color(255, 255, 255));
            Renderer2D.TEXTURE.render(null);
        } else if (!textures.containsKey(name)) {
            putHead(name, getSkinUrl(name));
        }
    }

    private void putHead(String name, String url) {
        textures.putIfAbsent(name, null);

        Thread thread = new Thread(() -> {
            try {
                BufferedImage skin = ImageIO.read(Http.get(url).sendInputStream());

                byte[] head = new byte[8 * 8 * 3];
                int[] pixel = new int[4];

                int i = 0;
                for (int x = 8; x < 16; x++) {
                    for (int y = 8; y < 16; y++) {
                        skin.getData().getPixel(x, y, pixel);

                        for (int j = 0; j < 3; j++) {
                            head[i] = (byte) pixel[j];
                            i++;
                        }
                    }
                }

                i = 0;
                for (int x = 40; x < 48; x++) {
                    for (int y = 8; y < 16; y++) {
                        skin.getData().getPixel(x, y, pixel);

                        if (pixel[3] != 0) {
                            for (int j = 0; j < 3; j++) {
                                head[i] = (byte) pixel[j];
                                i++;
                            }
                        }
                        else i += 3;
                    }
                }

                textures.replace(name, new Texture(8, 8, head, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest));
            } catch (IOException e) {
                MeteorClient.LOG.error("Failed to read skin url (" + url + ").");
                textures.putIfAbsent(name, null);
            }
        });

        thread.start();
    }

    private String getSkinUrl(String username) {
        ProfileResponse res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + username).sendJson(ProfileResponse.class);
        if (res == null) return null;

        UuidToProfileResponse res2 = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + res.id).sendJson(UuidToProfileResponse.class);
        if (res2 == null) return null;

        String base64Textures = res2.getPropertyValue("textures");
        if (base64Textures == null) return null;

        TexturesJson textures = new Gson().fromJson(new String(Base64.getDecoder().decode(base64Textures)), TexturesJson.class);
        if (textures.textures.SKIN == null) return null;

        return textures.textures.SKIN.url;
    }
}
