package cally72jhb.addon.system.modules.render;

import cally72jhb.addon.VectorAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MissHitResult;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class PearlPredict extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> simulation = sgGeneral.add(new IntSetting.Builder()
        .name("simulation-times")
        .description("How long to simulate the enderpearl.")
        .defaultValue(128)
        .min(10)
        .sliderMin(100)
        .sliderMax(200)
        .min(5)
        .build()
    );

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Whether or not to calculate more accurate.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(0, 255, 100, 35))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(0, 255, 100, 255))
        .build()
    );

    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();

    private final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
    private final List<Path> paths = new ArrayList<>();

    public PearlPredict() {
        super(VectorAddon.Misc, "pearl-predict", "Predicts where pearls will land and shows the path they will take.");
    }

    private Path getEmptyPath() {
        for (Path path : paths) {
            if (path.points.isEmpty()) return path;
        }

        Path path = new Path();
        paths.add(path);
        return path;
    }

    private void calculatePath(Entity entity, double tickDelta) {
        for (Path path : paths) path.clear();

        // Calculate paths
        simulator.set(entity, 1.5, 0.03, 0.8, accurate.get(), tickDelta);;
        getEmptyPath().calculate();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity || entity instanceof SnowballEntity || entity instanceof EggEntity) {
                calculatePath(entity, event.tickDelta);
                for (Path path : paths) path.render(event);
            }
        }
    }

    private class Path {
        private final List<Vec3> points = new ArrayList<>();

        private boolean hitQuad, hitQuadHorizontal;
        private double hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2;

        private Entity entity;

        public void clear() {
            for (Vec3 point : points) vec3s.free(point);
            points.clear();

            hitQuad = false;
            entity = null;
        }

        public void calculate() {
            addPoint();

            for (int i = 0; i < simulation.get(); i++) {
                HitResult result = simulator.tick();

                if (result != null) {
                    processHitResult(result);
                    break;
                }

                addPoint();
            }
        }

        private void addPoint() {
            points.add(vec3s.get().set(simulator.pos));
        }

        private void processHitResult(HitResult result) {
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult res = (BlockHitResult) result;

                hitQuad = true;
                hitQuadX1 = res.getPos().x;
                hitQuadY1 = res.getPos().y;
                hitQuadZ1 = res.getPos().z;
                hitQuadX2 = res.getPos().x;
                hitQuadY2 = res.getPos().y;
                hitQuadZ2 = res.getPos().z;

                if (res.getSide() == Direction.UP || res.getSide() == Direction.DOWN) {
                    hitQuadHorizontal = true;
                    hitQuadX1 -= 0.25;
                    hitQuadZ1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadZ2 += 0.25;
                } else if (res.getSide() == Direction.NORTH || res.getSide() == Direction.SOUTH) {
                    hitQuadHorizontal = false;
                    hitQuadX1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadX2 += 0.25;
                    hitQuadY2 += 0.25;
                } else {
                    hitQuadHorizontal = false;
                    hitQuadZ1 -= 0.25;
                    hitQuadY1 -= 0.25;
                    hitQuadZ2 += 0.25;
                    hitQuadY2 += 0.25;
                }

                points.add(vec3s.get().set(result.getPos()));
            } else if (result.getType() == HitResult.Type.ENTITY) {
                entity = ((EntityHitResult) result).getEntity();

                points.add(vec3s.get().set(result.getPos()).add(0, entity.getHeight() / 2, 0));
            }
        }

        public void render(Render3DEvent event) {
            // Render path
            Vec3 lastPoint = null;

            for (Vec3 point : points) {
                if (lastPoint != null) event.renderer.line(lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z, lineColor.get());
                lastPoint = point;
            }

            // Render hit quad
            if (hitQuad) {
                if (hitQuadHorizontal) event.renderer.sideHorizontal(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX1 + 0.5, hitQuadZ1 + 0.5, sideColor.get(), lineColor.get(), shapeMode.get());
                else event.renderer.sideVertical(hitQuadX1, hitQuadY1, hitQuadZ1, hitQuadX2, hitQuadY2, hitQuadZ2, sideColor.get(), lineColor.get(), shapeMode.get());
            }

            // Render entity
            if (entity != null) {
                double x = (entity.getX() - entity.prevX) * event.tickDelta;
                double y = (entity.getY() - entity.prevY) * event.tickDelta;
                double z = (entity.getZ() - entity.prevZ) * event.tickDelta;

                Box box = entity.getBoundingBox();
                event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    private class ProjectileEntitySimulator {
        private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();

        private static final Vec3d pos3d = new Vec3d(0, 0, 0);
        private static final Vec3d prevPos3d = new Vec3d(0, 0, 0);

        public final Vec3 pos = new Vec3();
        private final Vec3 velocity = new Vec3();

        private double gravity;
        private double airDrag, waterDrag;

        public void set(Entity entity, double speed, double gravity, double waterDrag, boolean accurate, double tickDelta) {
            pos.set(entity, tickDelta);

            velocity.set(entity.getVelocity()).normalize().multiply(speed);

            if (accurate) {
                Vec3d vel = entity.getVelocity();
                velocity.add(vel.x, entity.isOnGround() ? 0.0D : vel.y, vel.z);
            }

            this.gravity = gravity;
            this.airDrag = 0.99;
            this.waterDrag = waterDrag;
        }

        public HitResult tick() {
            // Apply velocity
            ((IVec3d) prevPos3d).set(pos);
            pos.add(velocity);

            // Update velocity
            velocity.multiply(isTouchingWater() ? waterDrag : airDrag);
            velocity.subtract(0, gravity, 0);

            // Check if below 0
            if (pos.y < 0) return MissHitResult.INSTANCE;

            // Check if chunk is loaded
            int chunkX = (int) (pos.x / 16);
            int chunkZ = (int) (pos.z / 16);
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return MissHitResult.INSTANCE;

            // Check for collision
            ((IVec3d) pos3d).set(pos);
            HitResult hitResult = getCollision();

            return hitResult.getType() == HitResult.Type.MISS ? null : hitResult;
        }

        private boolean isTouchingWater() {
            blockPos.set(pos.x, pos.y, pos.z);

            FluidState fluidState = mc.world.getFluidState(blockPos);
            if (fluidState.getFluid() != Fluids.WATER && fluidState.getFluid() != Fluids.FLOWING_WATER) return false;

            return pos.y - (int) pos.y <= fluidState.getHeight();
        }

        private HitResult getCollision() {
            Vec3d vec3d3 = prevPos3d;

            HitResult hitResult = mc.world.raycast(new RaycastContext(vec3d3, pos3d, RaycastContext.ShapeType.COLLIDER, waterDrag == 0 ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, mc.player));
            if (hitResult.getType() != HitResult.Type.MISS) {
                vec3d3 = hitResult.getPos();
            }

            HitResult hitResult2 = ProjectileUtil.getEntityCollision(mc.world, mc.player, vec3d3, pos3d, new Box(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z).stretch(mc.player.getVelocity()).expand(1.0D), entity -> !entity.isSpectator() && entity.isAlive() && entity.collides());
            if (hitResult2 != null) {
                hitResult = hitResult2;
            }

            return hitResult;
        }
    }
}
