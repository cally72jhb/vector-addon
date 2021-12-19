package cally72jhb.addon.utils.titlescreen.modules;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.titlescreen.CustomTitleScreen;
import cally72jhb.addon.utils.titlescreen.ScreenRenderer;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.accounts.ProfileResponse;
import meteordevelopment.meteorclient.utils.misc.FakeClientPlayer;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SkinTitleScreen extends PressableElement {
    private Texture texture;

    private final Setting<Integer> offset;
    private final Setting<Boolean> text;

    public SkinTitleScreen(CustomTitleScreen hud, String name, Action action) {
        super(hud, name, action, false);

        text = getSettings().add(new BoolSetting.Builder()
            .name("text")
            .description("Whether or not to render a small text beneath the head.")
            .defaultValue(true)
            .build()
        );

        offset = getSettings().add(new IntSetting.Builder()
            .name("offset")
            .description("How much to offset the name downwards.")
            .defaultValue(6)
            .min(0)
            .max(25)
            .sliderMin(0)
            .sliderMax(10)
            .visible(text::get)
            .build()
        );
    }

    @Override
    public void update(ScreenRenderer renderer) {
        box.setSize(getScale() * 50, getScale() * (47 + offset.get()) + (renderer.textHeight() * getScale() / 2));
    }

    @Override
    public void render(ScreenRenderer renderer) {
        if (texture == null) {
            createHead(mc.player == null ? FakeClientPlayer.getPlayer() : mc.player);
            return;
        }

        texture.bind();

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(box.getX(), box.getY(), getScale() * 50, getScale() * 50, new Color(255, 255, 255));
        Renderer2D.TEXTURE.render(null);

        if (text.get()) {
            PlayerEntity player = mc.player == null ? FakeClientPlayer.getPlayer() : mc.player;
            renderer.text(player.getEntityName(), getScale() / 2, box.getX() + getScale() * 25 - (renderer.textWidth(player.getEntityName()) * getScale() / 2) / 2, box.getY() + getScale() * (46 + offset.get()), isBig(), new Color(255, 255, 255));
        }
    }

    private void createHead(PlayerEntity player) {
        ProfileResponse res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + player.getGameProfile().getName()).sendJson(ProfileResponse.class);

        loadHead(res == null ? "steve" : ("https://www.mc-heads.net/avatar/" + res.id + "/8"));
    }

    private boolean loadHead(String skinUrl) {
        try {
            BufferedImage skin;
            byte[] head = new byte[8 * 8 * 3];
            int[] pixel = new int[4];

            if (skinUrl.equals("steve")) {
                skin = ImageIO.read(VectorUtils.mc.getResourceManager().getResource(new Identifier("meteor-client", "textures/steve.png")).getInputStream());
            } else {
                skin = ImageIO.read(Http.get(skinUrl).sendInputStream());
            }

            int width = skin.getWidth();
            int height = skin.getHeight();

            BufferedImage dest = new BufferedImage(height, width, skin.getType());

            Graphics2D graphics2D = dest.createGraphics();
            graphics2D.translate((height - width) / 2, (height - width) / 2);
            graphics2D.rotate(Math.PI / -2, height / 2, width / 2);
            graphics2D.drawRenderedImage(skin, null);

            skin = dest;

            int i = 0;
            for (int x = 0; x < 4 + 4; x++) {
                for (int y = 0; y < 4 + 4; y++) {
                    skin.getData().getPixel(x, y, pixel);

                    for (int j = 0; j < 3; j++) {
                        head[i] = (byte) pixel[j];
                        i++;
                    }
                }
            }

            texture = new Texture(8, 8, head, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest);
            return true;
        } catch (Exception e) {
            VectorAddon.LOG.error("Failed to read skin url (" + skinUrl + ").");
            e.printStackTrace();
            return false;
        }
    }
}
