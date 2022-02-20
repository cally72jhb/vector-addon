package cally72jhb.addon.gui.screens;

import cally72jhb.addon.gui.tabs.TitleScreenTab;
import cally72jhb.addon.system.titlescreen.TitleScreenManager;
import cally72jhb.addon.system.titlescreen.modules.DefaultElement;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.AccountsScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static cally72jhb.addon.utils.VectorUtils.mc;

public class TitleScreen extends Screen {
    private TitleScreenManager titleScreenManager;
    private DefaultElement hoveredModule;

    private final Identifier BACKGROUND = new Identifier("vector-addon", "background.png");

    public TitleScreen() {
        super(Text.of("Title Screen"));

        titleScreenManager = TitleScreenManager.get();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredModule != null) {
            hoveredModule.onPress();

            return true;
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double scale = mc.getWindow().getScaleFactor();

        hoveredModule = null;

        for (DefaultElement module : titleScreenManager.elements) {
            if (module.box.isOver(mouseX * scale, mouseY * scale)) {
                hoveredModule = module;

                break;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_E || keyCode == GLFW.GLFW_KEY_ESCAPE || KeyBinds.OPEN_COMMANDS.matchesKey(keyCode, scanCode)) {
            mc.setScreen(new TitleScreenTab.TabTitleScreen(GuiThemes.get(), new TitleScreenTab()));
        }

        if (keyCode == GLFW.GLFW_KEY_A) mc.setScreen(new AccountsScreen(GuiThemes.get()));
        if (keyCode == GLFW.GLFW_KEY_S) mc.setScreen(new SelectWorldScreen(this));
        if (keyCode == GLFW.GLFW_KEY_M) mc.setScreen(new MultiplayerScreen(this));
        if (keyCode == GLFW.GLFW_KEY_O) mc.setScreen(new OptionsScreen(this, mc.options));
        if (keyCode == GLFW.GLFW_KEY_Q) mc.scheduleStop();

        if (KeyBinds.OPEN_GUI.matchesKey(keyCode, scanCode)) Tabs.get().get(0).openScreen(GuiThemes.get());

        return true;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        Utils.unscaledProjection();

        double width = mc.getWindow().getWidth();
        double height = mc.getWindow().getHeight();

        double scale = Math.max(width, height) * (titleScreenManager.scale.get() + 0.15);

        double x = -(titleScreenManager.scale.get() + 0.15) / 4;
        double y = -(titleScreenManager.scale.get() + 0.15) / 2 * ((1024 * titleScreenManager.yOffset.get()) / (titleScreenManager.scale.get() + 0.15));

        if (titleScreenManager.factor.get() <= -1 || titleScreenManager.factor.get() >= 1) {
            x = (mouseX - width / 2) / titleScreenManager.factor.get() - titleScreenManager.factor.get() * 10;
            y += (mouseY - height / 2) / titleScreenManager.factor.get();
        }

        GL.bindTexture(BACKGROUND);

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, scale, scale, new Color(255, 255, 255));
        Renderer2D.TEXTURE.render(null);

        if (titleScreenManager != null) {
            TitleScreenManager.get().onRender(Render2DEvent.get(0, 0, delta));
        } else {
            titleScreenManager = TitleScreenManager.get();
        }

        Utils.scaledProjection();
    }
}
