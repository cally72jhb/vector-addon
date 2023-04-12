package cally72jhb.addon.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

    // Variables

    private Freecam freecam;

    // Constructor

    public SkeletonESP() {
        super(Categories.Misc, "skeleton-esp", "Renders the skeleton of players.");
    }

    // Overrides

    @Override
    public void onActivate() {
        this.freecam = Modules.get().get(Freecam.class);
    }

    // Render 3D Event

    @EventHandler
    private void onRender(Render3DEvent event) {
        MatrixStack matrixStack = event.matrices;
        float delta = event.tickDelta;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.isFancyGraphicsOrBetter());
        RenderSystem.enableCull();

        mc.world.getEntities().forEach(entity -> {
            if (!(entity instanceof AbstractClientPlayerEntity player)) return;
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !freecam.isActive() && mc.player == entity) return;
            int rotationHoldTicks = Config.get().rotationHoldTicks.get();

            Color color = PlayerUtils.getPlayerColor((PlayerEntity) entity, playersColor.get());
            if (distance.get()) color = getColorFromDistance(entity);

            Vec3d footPos = getEntityRenderPosition(player, delta);
            PlayerEntityRenderer livingEntityRenderer = (PlayerEntityRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
            PlayerEntityModel<AbstractClientPlayerEntity> playerEntityModel = livingEntityRenderer.getModel();

            float lerpBody = MathHelper.lerpAngleDegrees(delta, player.prevBodyYaw, player.bodyYaw);
            float lerpHead = MathHelper.lerpAngleDegrees(delta, player.prevHeadYaw, player.headYaw);
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) lerpBody = Rotations.serverYaw;
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) lerpHead = Rotations.serverYaw;

            float angle = player.limbAnimator.getPos() - player.limbAnimator.getSpeed() * (1.0F - delta);
            float distance = player.limbAnimator.getSpeed(delta);
            float progress = (float) player.age + delta;
            float headYaw = lerpHead - lerpBody;
            float headPitch = player.getPitch(delta);
            
            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) headPitch = Rotations.serverPitch;

            playerEntityModel.animateModel(player, angle, distance, delta);
            playerEntityModel.setAngles(player, angle, distance, progress, headYaw, headPitch);

            // Model States

            boolean swimming = player.isInSwimmingPose();
            boolean sneaking = player.isSneaking();
            boolean flying = player.isFallFlying();

            // Model Parts

            ModelPart head = playerEntityModel.head;
            ModelPart leftArm = playerEntityModel.leftArm;
            ModelPart rightArm = playerEntityModel.rightArm;
            ModelPart leftLeg = playerEntityModel.leftLeg;
            ModelPart rightLeg = playerEntityModel.rightLeg;

            // Translating Matrix

            matrixStack.translate(footPos.x, footPos.y, footPos.z);
            if (swimming) matrixStack.translate(0, 0.35F, 0);

            matrixStack.multiply(new Quaternionf().setAngleAxis((lerpBody + 180) * Math.PI / 180.0F, 0, -1, 0));
            if (swimming || flying) matrixStack.multiply(new Quaternionf().setAngleAxis((90 + headPitch) * Math.PI / 180.0F, -1, 0, 0));
            if (swimming) matrixStack.translate(0, -0.95F, 0);

            // Setting Up Buffered Builder

            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            Matrix4f matrix = matrixStack.peek().getPositionMatrix();

            // Spine

            buffer.vertex(matrix, 0, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0, sneaking ? 1.05F : 1.4F, 0).color(color.r, color.g, color.b, color.a).next();

            // Shoulders

            buffer.vertex(matrix, -0.37F, sneaking ? 1.05F : 1.35F, 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0.37F, sneaking ? 1.05F : 1.35F, 0).color(color.r, color.g, color.b, color.a).next();

            // Pelvis

            buffer.vertex(matrix, -0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0).color(color.r, color.g, color.b, color.a).next();

            // Head
            matrixStack.push();
            matrixStack.translate(0, sneaking ? 1.05F : 1.4F, 0);
            rotate(matrixStack, head);
            matrix = matrixStack.peek().getPositionMatrix();
            buffer.vertex(matrix, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0, 0.15F, 0).color(color.r, color.g, color.b, color.a).next();
            matrixStack.pop();

            // Right Leg

            matrixStack.push();
            matrixStack.translate(0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0);
            rotate(matrixStack, rightLeg);
            matrix = matrixStack.peek().getPositionMatrix();
            buffer.vertex(matrix, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0, -0.6F, 0).color(color.r, color.g, color.b, color.a).next();
            matrixStack.pop();

            // Left Leg

            matrixStack.push();
            matrixStack.translate(-0.15F, sneaking ? 0.6F : 0.7F, sneaking ? 0.23F : 0);
            rotate(matrixStack, leftLeg);
            matrix = matrixStack.peek().getPositionMatrix();
            buffer.vertex(matrix, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0, -0.6F, 0).color(color.r, color.g, color.b, color.a).next();
            matrixStack.pop();

            // Right Arm

            matrixStack.push();
            matrixStack.translate(0.37F, sneaking ? 1.05F : 1.35F, 0);
            rotate(matrixStack, rightArm);
            matrix = matrixStack.peek().getPositionMatrix();
            buffer.vertex(matrix, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0, -0.55F, 0).color(color.r, color.g, color.b, color.a).next();
            matrixStack.pop();

            // Left Arm

            matrixStack.push();
            matrixStack.translate(-0.37F, sneaking ? 1.05F : 1.35F, 0);
            rotate(matrixStack, leftArm);
            matrix = matrixStack.peek().getPositionMatrix();
            buffer.vertex(matrix, 0, 0, 0).color(color.r, color.g, color.b, color.a).next();
            buffer.vertex(matrix, 0, -0.55F, 0).color(color.r, color.g, color.b, color.a).next();
            matrixStack.pop();

            // Drawing Built Buffer

            buffer.clear();
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            // Resetting Matrix Translation

            if (swimming) matrixStack.translate(0, 0.95F, 0);
            if (swimming || flying) matrixStack.multiply(new Quaternionf().setAngleAxis((90 + headPitch) * Math.PI / 180.0F, 1, 0, 0));
            if (swimming) matrixStack.translate(0, -0.35F, 0);

            matrixStack.multiply(new Quaternionf().setAngleAxis((lerpBody + 180.0F) * Math.PI / 180.0F, 0, 1, 0));
            matrixStack.translate(-footPos.x, -footPos.y, -footPos.z);
        });

        // Resetting Render System GL States

        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    private void rotate(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0F) {
            matrix.multiply(RotationAxis.POSITIVE_Z.rotation(modelPart.roll));
        }

        if (modelPart.yaw != 0.0F) {
            matrix.multiply(RotationAxis.NEGATIVE_Y.rotation(modelPart.yaw));
        }

        if (modelPart.pitch != 0.0F) {
            matrix.multiply(RotationAxis.NEGATIVE_X.rotation(modelPart.pitch));
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

        int red;
        int green;

        if (percent < 0.5) {
            red = 255;
            green = (int) (255 * percent / 0.5);
        } else {
            green = 255;
            red = 255 - (int) (255 * (percent - 0.5) / 0.5);
        }

        color.set(red, green, 0, 255);

        return color;
    }
}
