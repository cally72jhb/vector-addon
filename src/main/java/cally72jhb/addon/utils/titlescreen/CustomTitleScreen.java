package cally72jhb.addon.utils.titlescreen;

import cally72jhb.addon.utils.titlescreen.modules.ScreenElement;
import cally72jhb.addon.utils.titlescreen.modules.SkinTitleScreen;
import cally72jhb.addon.utils.titlescreen.modules.PressableElement;
import cally72jhb.addon.utils.titlescreen.modules.TextTitleScreen;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.render.AlignmentX;
import meteordevelopment.meteorclient.utils.render.AlignmentY;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static cally72jhb.addon.utils.VectorUtils.mc;

public class CustomTitleScreen extends Screen {
    public static CustomTitleScreen INSTANCE;
    public static boolean editing;

    private final Color HOVER_BG_COLOR = new Color(200, 200, 200, 50);
    private final Color HOVER_OL_COLOR = new Color(200, 200, 200, 200);

    private final Color INACTIVE_BG_COLOR = new Color(200, 25, 25, 50);
    private final Color INACTIVE_OL_COLOR = new Color(200, 25, 25, 200);

    private static final ScreenRenderer RENDERER = new ScreenRenderer();

    public final List<ScreenElement> elements = new ArrayList<>();
    public final ElementLayer bottomLeft, topRight;

    public final Runnable reset = () -> {
        align();

        elements.forEach(element -> {
            element.active = element.defaultActive;
            element.settings.forEach(group -> group.forEach(Setting::reset));
        });
    };

    private boolean selecting;
    private double mouseStartX, mouseStartY;

    private boolean dragging, dragged;
    private double lastMouseX, lastMouseY;
    private ScreenElement hoveredModule;
    private final List<ScreenElement> selectedElements = new ArrayList<>();

    public CustomTitleScreen() {
        super(Text.of("Title Screen"));

        INSTANCE = this;
        editing = false;

        // Bottom Left
        bottomLeft = new ElementLayer(RENDERER, elements, AlignmentX.Left, AlignmentY.Bottom, 50, 50);
        bottomLeft.add(new TextTitleScreen(this, "singleplayer", PressableElement.Action.SINGLEPLAYER));
        bottomLeft.add(new TextTitleScreen(this, "multiplayer", PressableElement.Action.MULTIPLAYER));
        bottomLeft.add(new TextTitleScreen(this, "options", PressableElement.Action.OPTIONS));
        bottomLeft.add(new TextTitleScreen(this, "quit", PressableElement.Action.QUIT));

        topRight = new ElementLayer(RENDERER, elements, AlignmentX.Right, AlignmentY.Top, 50, 50);
        topRight.add(new SkinTitleScreen(this, "accounts", PressableElement.Action.LOGIN));

        reset.run();

        align();
    }

    private void align() {
        RENDERER.begin(1, 0, true);

        bottomLeft.align();
        topRight.align();

        RENDERER.end();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_E || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editing = !editing;
            selectedElements.clear();
            selecting = false;
        }

        if (editing) return true;

        if (keyCode == GLFW.GLFW_KEY_S) mc.setScreen(new SelectWorldScreen(this));
        if (keyCode == GLFW.GLFW_KEY_M) mc.setScreen(new MultiplayerScreen(this));
        if (keyCode == GLFW.GLFW_KEY_O) mc.setScreen(new OptionsScreen(this, mc.options));
        if (keyCode == GLFW.GLFW_KEY_Q) mc.scheduleStop();

        if (KeyBinds.OPEN_CLICK_GUI.matchesKey(keyCode, scanCode)) Tabs.get().get(0).openScreen(GuiThemes.get());

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredModule != null && editing) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (!selectedElements.isEmpty()) selectedElements.clear();
                mc.setScreen(new ElementScreen(GuiThemes.get(), hoveredModule));
            } else {
                dragging = true;
                dragged = false;

                if (!selectedElements.contains(hoveredModule)) {
                    selectedElements.clear();
                    selectedElements.add(hoveredModule);
                }
            }

            return true;
        } else if (hoveredModule != null && hoveredModule.active) {
            hoveredModule.onPress();
            return true;
        }

        double s = mc.getWindow().getScaleFactor();

        selecting = true;
        mouseStartX = mouseX * s;
        mouseStartY = mouseY * s;

        if (!selectedElements.isEmpty()) {
            selectedElements.clear();
            return true;
        }

        return false;
    }

    private boolean isInSelection(double mouseX, double mouseY, double x, double y) {
        double sx, sy;
        double sw, sh;

        if (mouseX >= mouseStartX) {
            sx = mouseStartX;
            sw = mouseX - mouseStartX;
        } else {
            sx = mouseX;
            sw = mouseStartX - mouseX;
        }

        if (mouseY >= mouseStartY) {
            sy = mouseStartY;
            sh = mouseY - mouseStartY;
        } else {
            sy = mouseY;
            sh = mouseStartY - mouseY;
        }

        return x >= sx && x <= sx + sw && y >= sy && y <= sy + sh;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double s = mc.getWindow().getScaleFactor();

        mouseX *= s;
        mouseY *= s;

        if (selecting) {
            selectedElements.clear();

            for (ScreenElement module : elements) {
                double mX = module.box.getX();
                double mY = module.box.getY();
                double mW = module.box.width;
                double mH = module.box.height;

                if (isInSelection(mouseX, mouseY, mX, mY) || isInSelection(mouseX, mouseY, mX + mW, mY) || (isInSelection(mouseX, mouseY, mX, mY + mH) || isInSelection(mouseX, mouseY, mX + mW, mY + mH))) {
                    selectedElements.add(module);
                }
            }
        } else if (dragging) {
            for (ScreenElement element : selectedElements) {
                element.box.addPos(mouseX - lastMouseX, mouseY - lastMouseY);
            }

            double r = 5;

            double x = Double.MAX_VALUE;
            double y = Double.MAX_VALUE;
            double w = 0;
            double h = 0;

            for (ScreenElement element : selectedElements) {
                x = Math.min(x, element.box.getX());
                y = Math.min(y, element.box.getY());
            }

            for (ScreenElement element : selectedElements) {
                w = Math.max(w, element.box.getX() - x + element.box.width);
                h = Math.max(h, element.box.getY() - y + element.box.height);
            }

            boolean movedX = false;
            boolean movedY = false;

            for (ScreenElement element : elements) {
                if (selectedElements.contains(element)) continue;

                double eX = element.box.getX();
                double eY = element.box.getY();
                double eW = element.box.width;
                double eH = element.box.height;

                boolean isHorizontallyIn = isPointBetween(x, w, eX) || isPointBetween(x, w, eX + eW) || isPointBetween(eX, eW, x) || isPointBetween(eX, eW, x + w);
                boolean isVerticallyIn = isPointBetween(y, h, eY) || isPointBetween(y, h, eY + eH) || isPointBetween(eY, eH, y) || isPointBetween(eY, eH, y + h);

                double moveX = 0;
                double moveY = 0;

                if (!movedX && isVerticallyIn) {
                    double x2 = x + w;
                    double eX2 = eX + eW;

                    if (Math.abs(eX - x) < r) moveX = eX - x;
                    else if (Math.abs(eX2 - x2) <= r) moveX = eX2 - x2;
                    else if (Math.abs(eX2 - x) <= r) moveX = eX2 - x;
                    else if (Math.abs(eX - x2) <= r) moveX = eX - x2;
                }

                if (!movedY && isHorizontallyIn) {
                    double y2 = y + h;
                    double eY2 = eY + eH;

                    if (Math.abs(eY - y) <= r) moveY = eY - y;
                    else if (Math.abs(eY2 - y2) <= r) moveY = eY2 - y2;
                    else if (Math.abs(eY2 - y) <= r) moveY = eY2 - y;
                    else if (Math.abs(eY - y2) <= r) moveY = eY - y2;
                }

                if (moveX != 0 || moveY != 0) {
                    for (ScreenElement e : selectedElements) e.box.addPos(moveX, moveY);

                    if (moveX != 0) movedX = true;
                    if (moveY != 0) movedY = true;
                }

                if (movedX && movedY) break;
            }

            dragged = true;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private boolean isPointBetween(double start, double size, double point) {
        return point >= start && point <= start + size;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;

            if (!dragged && !selectedElements.isEmpty()) {
                selectedElements.forEach(ScreenElement::toggle);
                selectedElements.clear();
            }

            if (selectedElements.size() <= 1) selectedElements.clear();

            return true;
        }

        if (selecting) {
            selecting = false;
            return true;
        }

        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        double s = mc.getWindow().getScaleFactor();

        mouseX *= s;
        mouseY *= s;

        drawBackground(matrices);

        Utils.unscaledProjection();
        render(Render2DEvent.get(0, 0, delta));

        Renderer2D.COLOR.begin();

        for (ScreenElement element : elements) {
            if (element.active) continue;

            renderElement(element, INACTIVE_BG_COLOR, INACTIVE_OL_COLOR);
        }

        for (ScreenElement element : selectedElements) {
            renderElement(element, HOVER_BG_COLOR, HOVER_OL_COLOR);
        }

        if (!dragging) {
            hoveredModule = null;

            for (ScreenElement module : elements) {
                if (module.box.isOver(mouseX, mouseY)) {
                    if (!selectedElements.contains(module)) renderElement(module, HOVER_BG_COLOR, HOVER_OL_COLOR);
                    hoveredModule = module;

                    break;
                }
            }

            if (selecting && editing) {
                renderQuad(mouseStartX, mouseStartY, mouseX - mouseStartX, mouseY - mouseStartY, HOVER_BG_COLOR, HOVER_OL_COLOR);
            }
        }

        Renderer2D.COLOR.render(new MatrixStack());
        Utils.scaledProjection();
    }

    private void renderElement(ScreenElement module, Color bgColor, Color olColor) {
        if (editing) renderQuad(module.box.getX(), module.box.getY(), module.box.width, module.box.height, bgColor, olColor);
    }

    private void renderQuad(double x, double y, double w, double h, Color bgColor, Color olColor) {
        Renderer2D.COLOR.quad(x, y, w, h, bgColor);
        Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, 1, olColor);
        Renderer2D.COLOR.quad(x - 1, y + h - 1, w + 2, 1, olColor);
        Renderer2D.COLOR.quad(x - 1, y, 1, h, olColor);
        Renderer2D.COLOR.quad(x + w, y, 1, h, olColor);
    }

    public void render(Render2DEvent event) {
        RENDERER.begin(1, event.frameTime, false);

        for (ScreenElement element : elements) {
            if (element.active || editing) {
                element.update(RENDERER);
                element.render(RENDERER);
            }
        }

        RENDERER.end();
    }

    private void drawBackground(MatrixStack matrices) {
        renderBackground(matrices);
    }
}
