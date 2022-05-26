package cally72jhb.addon.mixin;

import cally72jhb.addon.system.modules.render.notexture.NoTextures;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin {
    @Shadow @Final private Sprite[] lavaSprites;
    @Shadow @Final private Sprite[] waterSprites;

    @Shadow private Sprite waterOverlaySprite;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockRenderView world, BlockPos pos, VertexConsumer consumer, BlockState state, FluidState fluidState, CallbackInfoReturnable<Boolean> info) {
        if (Modules.get() != null && Modules.get().isActive(NoTextures.class)) {
            NoTextures module = Modules.get().get(NoTextures.class);

            boolean lava = fluidState.isIn(FluidTags.LAVA);
            Sprite[] sprites = lava ? lavaSprites : waterSprites;

            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            float a = 1.0F;

            if (module.isAutomatic()) {
                if (lava) {
                    r = 0.988235F;
                    g = 0.407843F;
                    b = 0.0F;
                } else {
                    int i = BiomeColors.getWaterColor(world, pos);

                    r = (float) (i >> 16 & 255) / 255.0F;
                    g = (float) (i >> 8 & 255) / 255.0F;
                    b = (float) (i & 255) / 255.0F;
                }
            } else {
                Color color = lava ? module.getLavaColor() : module.getWaterColor();

                if (color != null) {
                    r = (float) ((double) color.r / 255);
                    g = (float) ((double) color.g / 255);
                    b = (float) ((double) color.b / 255);
                    a = (float) ((double) color.a / 255);
                }
            }

            BlockState downState = world.getBlockState(pos.offset(Direction.DOWN));
            FluidState downFluid = downState.getFluidState();

            BlockState upState = world.getBlockState(pos.offset(Direction.UP));
            FluidState upFluid = upState.getFluidState();

            BlockState northState = world.getBlockState(pos.offset(Direction.NORTH));
            FluidState northFluid = northState.getFluidState();

            BlockState eastState = world.getBlockState(pos.offset(Direction.EAST));
            FluidState eastFluid = eastState.getFluidState();

            BlockState southState = world.getBlockState(pos.offset(Direction.SOUTH));
            FluidState southFluid = southState.getFluidState();

            BlockState westState = world.getBlockState(pos.offset(Direction.WEST));
            FluidState westFluid = westState.getFluidState();

            boolean renderUp = isOtherFluid(fluidState, upFluid);

            boolean renderDown = shouldRender(world, pos, fluidState, state, Direction.DOWN, downFluid) && !isCovered(world, downState, pos, Direction.DOWN, 0.8888889F);
            boolean renderNorth = shouldRender(world, pos, fluidState, state, Direction.NORTH, northFluid);
            boolean renderEast = shouldRender(world, pos, fluidState, state, Direction.EAST, eastFluid);
            boolean renderSouth = shouldRender(world, pos, fluidState, state, Direction.SOUTH, southFluid);
            boolean renderWest = shouldRender(world, pos, fluidState, state, Direction.WEST, westFluid);

            if (!renderUp && !renderDown && !renderEast && !renderWest && !renderNorth && !renderSouth) {
                info.setReturnValue(false);
            } else {
                boolean rendered = false;

                float brightnessDown = world.getBrightness(Direction.DOWN, true);
                float brightnessUp = world.getBrightness(Direction.UP, true);
                float brightnessNorth = world.getBrightness(Direction.NORTH, true);
                float brightnessWest = world.getBrightness(Direction.WEST, true);

                Fluid fluid = fluidState.getFluid();

                float height = getFluidHeight(world, fluid, pos, state, fluidState);

                float northEast;
                float northWest;
                float southEast;
                float southWest;

                if (height >= 1.0F) {
                    northEast = 1.0F;
                    northWest = 1.0F;
                    southEast = 1.0F;
                    southWest = 1.0F;
                } else {
                    float north = getFluidHeight(world, fluid, pos.north(), northState, northFluid);
                    float south = getFluidHeight(world, fluid, pos.south(), southState, southFluid);
                    float east = getFluidHeight(world, fluid, pos.east(), eastState, eastFluid);
                    float west = getFluidHeight(world, fluid, pos.west(), westState, westFluid);

                    northEast = adjustForState(world, fluid, height, north, east, pos.offset(Direction.NORTH).offset(Direction.EAST));
                    northWest = adjustForState(world, fluid, height, north, west, pos.offset(Direction.NORTH).offset(Direction.WEST));
                    southEast = adjustForState(world, fluid, height, south, east, pos.offset(Direction.SOUTH).offset(Direction.EAST));
                    southWest = adjustForState(world, fluid, height, south, west, pos.offset(Direction.SOUTH).offset(Direction.WEST));
                }

                double x = (pos.getX() & 15);
                double y = (pos.getY() & 15);
                double z = (pos.getZ() & 15);

                float yOffset = renderDown ? 0.001F : 0.0F;

                float minU1;
                float maxU1;
                float minV1;
                float maxV1;

                float minU2;
                float maxU2;
                float minV2;
                float maxV2;

                if (renderUp && !isCovered(world, upState, pos, Direction.UP, Math.min(Math.min(northWest, southWest), Math.min(southEast, northEast)))) {
                    rendered = true;

                    Vec3d velocity = fluidState.getVelocity(world, pos);
                    Sprite sprite;

                    float max1;
                    float max2;

                    float boundMin;
                    float boundMax;

                    if (!lava) {

                        if (velocity.x == 0.0 && velocity.z == 0.0) {
                            sprite = sprites[0];
                            minU1 = sprite.getFrameU(0.0);
                            minU2 = sprite.getFrameV(0.0);
                            maxU1 = minU1;
                            maxU2 = sprite.getFrameV(16.0);
                            minV1 = sprite.getFrameU(16.0);
                            minV2 = maxU2;
                            maxV1 = minV1;
                            maxV2 = minU2;
                        } else {
                            sprite = sprites[1];
                            max1 = (float) MathHelper.atan2(velocity.z, velocity.x) - 1.5707964F;

                            boundMin = MathHelper.sin(max1) * 0.25F;
                            boundMax = MathHelper.cos(max1) * 0.25F;

                            minU1 = sprite.getFrameU((8.0F + (boundMax + boundMin) * 16.0F));
                            minU2 = sprite.getFrameV((8.0F + (boundMax - boundMin) * 16.0F));
                            maxU1 = sprite.getFrameU((8.0F + (boundMax - boundMin) * 16.0F));
                            maxU2 = sprite.getFrameV((8.0F + (boundMax + boundMin) * 16.0F));
                            minV1 = sprite.getFrameU((8.0F + (boundMax + boundMin) * 16.0F));
                            minV2 = sprite.getFrameV((8.0F + (boundMax - boundMin) * 16.0F));
                            maxV1 = sprite.getFrameU((8.0F + (boundMax - boundMin) * 16.0F));
                            maxV2 = sprite.getFrameV((8.0F + (boundMax + boundMin) * 16.0F));
                        }

                        max2 = (minU1 + maxU1 + minV1 + maxV1) / 4.0F;
                        max1 = (minU2 + maxU2 + minV2 + maxV2) / 4.0F;

                        boundMin = (float) sprites[0].getWidth() / (sprites[0].getMaxU() - sprites[0].getMinU());
                        boundMax = (float) sprites[0].getHeight() / (sprites[0].getMaxV() - sprites[0].getMinV());

                        float min = 4.0F / Math.max(boundMax, boundMin);

                        minU1 = MathHelper.lerp(min, minU1, max2);
                        maxU1 = MathHelper.lerp(min, maxU1, max2);
                        minV1 = MathHelper.lerp(min, minV1, max2);
                        maxV1 = MathHelper.lerp(min, maxV1, max2);

                        minU2 = MathHelper.lerp(min, minU2, max1);
                        maxU2 = MathHelper.lerp(min, maxU2, max1);
                        minV2 = MathHelper.lerp(min, minV2, max1);
                        maxV2 = MathHelper.lerp(min, maxV2, max1);
                    } else {
                        minU1 = 0.0F;
                        maxU1 = 0.0F;
                        minV1 = 0.0F;
                        maxV1 = 0.0F;

                        minU2 = 0.0F;
                        maxU2 = 0.0F;
                        minV2 = 0.0F;
                        maxV2 = 0.0F;
                    }

                    int light = getLight(world, pos);

                    float red = brightnessUp * r;
                    float green = brightnessUp * g;
                    float blue = brightnessUp * b;

                    vertex(consumer, x + 0.0, y + (double) northWest, z + 0.0, red, green, blue, a, minU1, minU2, light);
                    vertex(consumer, x + 0.0, y + (double) southWest, z + 1.0, red, green, blue, a, maxU1, maxU2, light);
                    vertex(consumer, x + 1.0, y + (double) southEast, z + 1.0, red, green, blue, a, minV1, minV2, light);
                    vertex(consumer, x + 1.0, y + (double) northEast, z + 0.0, red, green, blue, a, maxV1, maxV2, light);

                    if (checkOpaque(world, fluidState, pos.up())) {
                        vertex(consumer, x + 0.0, y + (double) northWest, z + 0.0, red, green, blue, a, minU1, minU2, light);
                        vertex(consumer, x + 1.0, y + (double) northEast, z + 0.0, red, green, blue, a, maxV1, maxV2, light);
                        vertex(consumer, x + 1.0, y + (double) southEast, z + 1.0, red, green, blue, a, minV1, minV2, light);
                        vertex(consumer, x + 0.0, y + (double) southWest, z + 1.0, red, green, blue, a, maxU1, maxU2, light);
                    }
                }

                if (renderDown) {
                    if (!lava) {
                        minU1 = sprites[0].getMinU();
                        maxU1 = sprites[0].getMaxU();
                        minV1 = sprites[0].getMinV();
                        maxV1 = sprites[0].getMaxV();
                    } else {
                        minU1 = 0.0F;
                        maxU1 = 0.0F;
                        minV1 = 0.0F;
                        maxV1 = 0.0F;
                    }

                    int light = getLight(world, pos.down());

                    float red = brightnessDown * r;
                    float green = brightnessDown * g;
                    float blue = brightnessDown * b;

                    vertex(consumer, x, y + (double) yOffset, z + 1.0, red, green, blue, a, minU1, maxV1, light);
                    vertex(consumer, x, y + (double) yOffset, z, red, green, blue, a, minU1, minV1, light);
                    vertex(consumer, x + 1.0, y + (double) yOffset, z, red, green, blue, a, maxU1, minV1, light);
                    vertex(consumer, x + 1.0, y + (double) yOffset, z + 1.0, red, green, blue, a, maxU1, maxV1, light);

                    rendered = true;
                }

                int light = getLight(world, pos);
                Iterator<Direction> directions = Direction.Type.HORIZONTAL.iterator();

                while (true) {
                    Direction direction;

                    double xx;
                    double zz;
                    double xxx;
                    double ah;
                    boolean aj;

                    do {
                        do {
                            if (!directions.hasNext()) {
                                info.setReturnValue(rendered);
                                return;
                            }

                            direction = directions.next();

                            switch (direction) {
                                case NORTH -> {
                                    maxV1 = northWest;
                                    minU2 = northEast;
                                    xx = x;
                                    xxx = x + 1.0;
                                    zz = z;
                                    ah = z;
                                    aj = renderNorth;
                                }

                                case SOUTH -> {
                                    maxV1 = southEast;
                                    minU2 = southWest;
                                    xx = x + 1.0;
                                    xxx = x;
                                    zz = z + 1.0;
                                    ah = z + 1.0;
                                    aj = renderSouth;
                                }

                                case WEST -> {
                                    maxV1 = southWest;
                                    minU2 = northWest;
                                    xx = x;
                                    xxx = x;
                                    zz = z + 1.0;
                                    ah = z;
                                    aj = renderWest;
                                }

                                default -> {
                                    maxV1 = northEast;
                                    minU2 = southEast;
                                    xx = x + 1.0;
                                    xxx = x + 1.0;
                                    zz = z;
                                    ah = z + 1.0;
                                    aj = renderEast;
                                }
                            }
                        } while (!aj);
                    } while (
                        isCovered(
                            world,
                            world.getBlockState(pos.offset(direction)),
                            pos,
                            direction,
                            Math.max(maxV1, minU2))
                    );

                    rendered = true;

                    BlockPos ak = pos.offset(direction);
                    Sprite sprite = sprites[1];

                    if (!lava) {
                        Block block = world.getBlockState(ak).getBlock();
                        if (block instanceof TransparentBlock || block instanceof LeavesBlock) {
                            sprite = waterOverlaySprite;
                        }
                    }

                    float u1;
                    float u2;

                    float v1;
                    float v2;
                    float v3;

                    if (!lava) {
                        u1 = sprite.getFrameU(0.0);
                        u2 = sprite.getFrameU(8.0);

                        v1 = sprite.getFrameV(((1.0F - maxV1) * 16.0F * 0.5F));
                        v2 = sprite.getFrameV(((1.0F - minU2) * 16.0F * 0.5F));
                        v3 = sprite.getFrameV(8.0);
                    } else {
                        u1 = 0.0F;
                        u2 = 0.0F;

                        v1 = 0.0F;
                        v2 = 0.0F;
                        v3 = 0.0F;
                    }

                    float brightness = direction.getAxis() == Direction.Axis.Z ? brightnessNorth : brightnessWest;

                    float red = brightnessUp * brightness * r;
                    float green = brightnessUp * brightness * g;
                    float blue = brightnessUp * brightness * b;

                    vertex(consumer, xx, y + (double) maxV1, zz, red, green, blue, a, u1, v1, light);
                    vertex(consumer, xxx, y + (double) minU2, ah, red, green, blue, a, u2, v2, light);
                    vertex(consumer, xxx, y + (double) yOffset, ah, red, green, blue, a, u2, v3, light);
                    vertex(consumer, xx, y + (double) yOffset, zz, red, green, blue, a, u1, v3, light);

                    if (sprite != waterOverlaySprite) {
                        vertex(consumer, xx, y + (double) yOffset, zz, red, green, blue, a, u1, v3, light);
                        vertex(consumer, xxx, y + (double) yOffset, ah, red, green, blue, a, u2, v3, light);
                        vertex(consumer, xxx, y + (double) minU2, ah, red, green, blue, a, u2, v2, light);
                        vertex(consumer, xx, y + (double) maxV1, zz, red, green, blue, a, u1, v1, light);
                    }
                }
            }
        }
    }

    private void vertex(VertexConsumer consumer, double x, double y, double z, float red, float green, float blue, float alpha, float u, float v, int light) {
        consumer.vertex(x, y, z).color(red, green, blue, alpha).texture(u, v).light(light).normal(0.0F, 1.0F, 0.0F).next();
    }

    private boolean isOtherFluid(FluidState fluid1, FluidState fluid2) {
        return !fluid2.getFluid().matchesType(fluid1.getFluid());
    }

    private boolean checkSideCovered(BlockView world, BlockState state, BlockPos pos, Direction direction, float maxDeviation) {
        if (state.isOpaque()) {
            VoxelShape shape1 = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, maxDeviation, 1.0);
            VoxelShape shape2 = state.getCullingShape(world, pos);

            return VoxelShapes.isSideCovered(shape1, shape2, direction);
        } else {
            return false;
        }
    }

    private boolean isCovered(BlockView world, BlockState state, BlockPos pos, Direction direction, float maxDeviation) {
        return checkSideCovered(world, state, pos.offset(direction), direction, maxDeviation);
    }

    private boolean isOppositeCovered(BlockView world, BlockPos pos, BlockState state, Direction direction) {
        return checkSideCovered(world, state, pos, direction.getOpposite(), 1.0F);
    }

    private boolean shouldRender(BlockRenderView world, BlockPos pos, FluidState fluid1, BlockState state, Direction direction, FluidState fluid2) {
        return !isOppositeCovered(world, pos, state, direction) && isOtherFluid(fluid1, fluid2);
    }

    private float adjustForState(BlockRenderView world, Fluid fluid, float y, float x, float z, BlockPos pos) {
        if (x < 1.0F && z < 1.0F) {
            float[] matrix = new float[2];

            if (z > 0.0F || x > 0.0F) {
                BlockState state = world.getBlockState(pos);
                float height = getFluidHeight(world, fluid, pos, state, state.getFluidState());

                if (height >= 1.0F) {
                    return 1.0F;
                }

                adjust(matrix, height);
            }

            adjust(matrix, y);
            adjust(matrix, z);
            adjust(matrix, x);

            return matrix[0] / matrix[1];
        } else {
            return 1.0F;
        }
    }

    private float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos blockPos, BlockState state, FluidState fluidState) {
        if (fluid.matchesType(fluidState.getFluid())) {
            return fluid.matchesType(world.getBlockState(blockPos.up()).getFluidState().getFluid()) ? 1.0F : fluidState.getHeight();
        } else {
            return !state.getMaterial().isSolid() ? 0.0F : -1.0F;
        }
    }

    private void adjust(float[] matrix, float pos) {
        if (pos >= 0.8F) {
            matrix[0] += pos * 10.0F;
            matrix[1] += 10.0F;
        } else if (pos >= 0.0F) {
            matrix[0] += pos;
            matrix[1]++;
        }
    }

    private int getLight(BlockRenderView world, BlockPos pos) {
        int lower = getLight(world, world.getBlockState(pos), pos);
        int upper = getLight(world, world.getBlockState(pos.up()), pos.up());

        return Math.max(lower & 255, upper & 255) | Math.max(lower >> 16 & 255, upper >> 16 & 255) << 16;
    }

    private int getLight(BlockRenderView world, BlockState state, BlockPos pos) {
        if (state.hasEmissiveLighting(world, pos)) {
            return 15728880;
        } else {
            FluidState fluidState = state.getFluidState();

            if (fluidState != null && fluidState.isIn(FluidTags.LAVA)) {
                return 15728880;
            } else {
                int sky = world.getLightLevel(LightType.SKY, pos);
                int block = world.getLightLevel(LightType.BLOCK, pos);
                int luminance = state.getLuminance();

                if (block < luminance) {
                    block = luminance;
                }

                return sky << 20 | block << 4;
            }
        }
    }

    public boolean checkOpaque(BlockView world, FluidState fluidState, BlockPos pos) {
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                BlockPos position = pos.add(x, 0, z);

                if (!world.getFluidState(position).getFluid().matchesType(fluidState.getFluid()) && !world.getBlockState(position).isOpaqueFullCube(world, position)) {
                    return true;
                }
            }
        }

        return false;
    }
}
