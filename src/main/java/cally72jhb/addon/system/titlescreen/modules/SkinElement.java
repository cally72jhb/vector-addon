package cally72jhb.addon.system.titlescreen.modules;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.system.titlescreen.TitleScreenRenderer;
import com.mojang.blaze3d.platform.TextureUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.accounts.ProfileResponse;
import meteordevelopment.meteorclient.utils.misc.FakeClientPlayer;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class SkinElement extends PressableElement {
    private Texture texture;
    private Texture STEVE_HEAD;
    private String prevName = "";

    private final Setting<Integer> offset = getSettings().add(new IntSetting.Builder()
        .name("offset")
        .description("How much the box is offset on each side.")
        .defaultValue(10)
        .min(5)
        .max(50)
        .sliderMin(5)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> text = getSettings().add(new BoolSetting.Builder()
        .name("text")
        .description("Whether or not to render a small text beneath the head.")
        .defaultValue(true)
        .build()
    );

    protected final Setting<Boolean> big = sgGeneral.add(new BoolSetting.Builder()
        .name("big")
        .description("Whether or not to render the font with big letters.")
        .defaultValue(false)
        .visible(text::get)
        .build()
    );

    private final Setting<Double> yOffset = getSettings().add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("How much to offset the name downwards.")
        .defaultValue(6)
        .min(0)
        .max(50)
        .sliderMin(0)
        .sliderMax(10)
        .visible(text::get)
        .build()
    );

    public SkinElement(TitleScreenManager manager, String name, Action action, boolean defaultActive) {
        super(manager, name, action, 3, defaultActive);
    }

    @Override
    public void update(TitleScreenRenderer renderer) {
        if (!prevName.equals(mc.getSession().getUsername())) createHead(mc.player == null ? FakeClientPlayer.getPlayer() : mc.player);
        prevName = mc.getSession().getUsername();

        box.setSize(offset.get() * 2 + getScale() * 50, offset.get() * 2 + (text.get() ? getScale() * (48 + yOffset.get()) + renderer.textHeight() : 0));
    }

    @Override
    public void render(TitleScreenRenderer renderer) {
        PlayerEntity player = mc.player == null ? FakeClientPlayer.getPlayer() : mc.player;

        if (texture == null) {
            createHead(player);
            return;
        }

        texture.bind();

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(offset.get() + box.getX(), offset.get() + box.getY(), getScale() * 50, getScale() * 50, new Color(255, 255, 255));
        Renderer2D.TEXTURE.render(null);

        if (text.get()) {
            renderer.text(player.getEntityName(), getScale() / 2, offset.get() + box.getX() + getScale() * 25 - (renderer.textWidth(player.getEntityName()) * getScale() / 2) / 2, offset.get() + box.getY() + getScale() * (46 + yOffset.get()), big.get(), new Color(255, 255, 255));
        }
    }

    private void createHead(PlayerEntity player) {
        ProfileResponse res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + player.getGameProfile().getName()).sendJson(ProfileResponse.class);

        Thread thread = new Thread(() -> loadHead(res == null ? "steve" : ("https://www.mc-heads.net/avatar/" + res.id + "/8")));
        thread.start();
    }

    private void loadHead(String skinUrl) {
        try {
            BufferedImage skin;
            byte[] head = new byte[8 * 8 * 3];
            int[] pixel = new int[4];

            if (skinUrl.equals("steve")) {
                texture = new Texture();
                return;
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
        } catch (Exception e) {
            VectorAddon.LOG.error("Failed to read skin url (" + skinUrl + ").");
            e.printStackTrace();
        }
    }

    private void loadSteveHead() {
        try {
            ByteBuffer data = TextureUtil.readResource(mc.getResourceManager().getResource(new Identifier(MeteorClient.MOD_ID, "textures/steve.png")).getInputStream());
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);

                STEVE_HEAD = new Texture();
                STEVE_HEAD.upload(width.get(0), height.get(0), image, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest, false);

                if (image != null) STBImage.stbi_image_free(image);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
