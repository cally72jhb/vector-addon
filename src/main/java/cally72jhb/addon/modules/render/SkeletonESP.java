package cally72jhb.addon.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;

public class SkeletonESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<SettingColor> playersColor = sgGeneral.add(new ColorSetting.Builder()
            .name("players-color")
            .description("The color of the skeleton of other players.")
            .defaultValue(new SettingColor(140, 245, 165))
            .build()
    );

    public final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
            .name("distance-colors")
            .description("Changes the color of skeletons depending on their distance to you.")
            .defaultValue(true)
            .build()
    );

    // Constructor

    public SkeletonESP() {
        super(Categories.Misc, "skeleton-esp", "Renders the skeleton of players.");
    }

    // Render 3D Event

    @SuppressWarnings("unchecked")
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        MatrixStack matrices = event.matrices;
        Freecam freecam = Modules.get().get(Freecam.class);

        float delta = event.tickDelta;

        // Render System GL States

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
        RenderSystem.enableCull();

        // Looping through all Players

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON && freecam.isActive() || mc.player != player) {
                Color color = distance.get() ? getColorFromDistance(player) : PlayerUtils.getPlayerColor(player, playersColor.get());

                Vec3d position = getEntityRenderPosition(player, delta);
                PlayerEntityModel<AbstractClientPlayerEntity> model = ((PlayerEntityRenderer) mc.getEntityRenderDispatcher().getRenderer(player)).getModel();

                float lerpBody = MathHelper.lerpAngleDegrees(delta, player.prevBodyYaw, player.bodyYaw);
                float lerpHead = MathHelper.lerpAngleDegrees(delta, player.prevHeadYaw, player.headYaw);

                float angel = player.limbAngle - player.limbDistance * (1.0F - delta);
                float distance = MathHelper.lerp(delta, player.lastLimbDistance, player.limbDistance);
                float progress = (float) player.age + delta;
                float headYaw = lerpHead - lerpBody;
                float headPitch = player.getPitch(delta);

                model.animateModel(player, angel, distance, delta);
                model.setAngles(player, angel, distance, progress, headYaw, headPitch);

                // Model States

                boolean swimming = player.isInSwimmingPose();
                boolean sneaking = player.isSneaking();
                boolean flying = player.isFallFlying();

                // Model Parts

                ModelPart head = model.head;
                ModelPart leftArm = model.leftArm;
                ModelPart rightArm = model.rightArm;
                ModelPart leftLeg = model.leftLeg;
                ModelPart rightLeg = model.rightLeg;

                // Translating Matrix

                matrices.translate(position.x, position.y, position.z);
                if (swimming) matrices.translate(0, 0.35f, 0);

                matrices.multiply(new Quaternion(new Vec3f(0, -1, 0), lerpBody + 180, true));
                if (swimming || flying) matrices.multiply(new Quaternion(new Vec3f(-1, 0, 0), 90 + headPitch, true));
                if (swimming) matrices.translate(0, -0.95f, 0);

                // Setting Up Buffered Builder

                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                Matrix4f matrix4f = matrices.peek().getPositionMatrix();

                // Spine

                bufferBuilder.vertex(matrix4f, 0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0, sneaking ? 1.05f : 1.4f, 0).color(color.r, color.g, color.b, color.a).next();

                // Shoulders

                bufferBuilder.vertex(matrix4f, -0.37f, sneaking ? 1.05f : 1.35f, 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0.37f, sneaking ? 1.05f : 1.35f, 0).color(color.r, color.g, color.b, color.a).next();

                // Pelvis

                bufferBuilder.vertex(matrix4f, -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(color.r, color.g, color.b, color.a).next();

                // Head

                matrices.push();
                matrices.translate(0, sneaking ? 1.05f : 1.4f, 0);
                rotate(matrices, head);
                matrix4f = matrices.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0, 0.15f, 0).color(color.r, color.g, color.b, color.a).next();
                matrices.pop();

                // Right Leg

                matrices.push();
                matrices.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotate(matrices, rightLeg);
                matrix4f = matrices.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(color.r, color.g, color.b, color.a).next();
                matrices.pop();

                // Left Leg

                matrices.push();
                matrices.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotate(matrices, leftLeg);
                matrix4f = matrices.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(color.r, color.g, color.b, color.a).next();
                matrices.pop();

                // Right Arm

                matrices.push();
                matrices.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotate(matrices, rightArm);
                matrix4f = matrices.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(color.r, color.g, color.b, color.a).next();
                matrices.pop();

                // Left Arm

                matrices.push();
                matrices.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotate(matrices, leftArm);
                matrix4f = matrices.peek().getPositionMatrix();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(color.r, color.g, color.b, color.a).next();
                matrices.pop();

                // Drawing Built Buffer

                BufferRenderer.drawWithShader(bufferBuilder.end());

                // Resetting Matrix Translation

                if (swimming) matrices.translate(0, 0.95f, 0);
                if (swimming || flying) matrices.multiply(new Quaternion(new Vec3f(1, 0, 0), 90 + headPitch, true));
                if (swimming) matrices.translate(0, -0.35f, 0);

                matrices.multiply(new Quaternion(new Vec3f(0, 1, 0), lerpBody + 180, true));
                matrices.translate(-position.x, -position.y, -position.z);
            }
        }

        // Resetting Render System GL States

        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void rotate(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0F) {
            matrix.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(modelPart.roll));
        }

        if (modelPart.yaw != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_Y.getRadialQuaternion(modelPart.yaw));
        }

        if (modelPart.pitch != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_X.getRadialQuaternion(modelPart.pitch));
        }
    }

    private Vec3d getEntityRenderPosition(Entity entity, double partial) {
        double x = entity.prevX + ((entity.getX() - entity.prevX) * partial) - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.prevY + ((entity.getY() - entity.prevY) * partial) - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.prevZ + ((entity.getZ() - entity.prevZ) * partial) - mc.getEntityRenderDispatcher().camera.getPos().z;
        return new Vec3d(x, y, z);
    }

    private Color getColorFromDistance(Entity entity) {
        double distance = mc.gameRenderer.getCamera().getPos().distanceTo(entity.getPos());
        double percent = distance / 60;

        if (percent < 0 || percent > 1) {
            color.set(0, 255, 0, 255);
            return color;
        }

        int r, g;

        if (percent < 0.5) {
            r = 255;
            g = (int) (255 * percent / 0.5);
        }
        else {
            g = 255;
            r = 255 - (int) (255 * (percent - 0.5) / 0.5);
        }

        color.set(r, g, 0, 255);
        return color;
    }
}
