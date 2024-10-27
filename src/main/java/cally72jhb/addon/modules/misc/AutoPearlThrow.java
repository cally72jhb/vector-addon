package cally72jhb.addon.modules.misc;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class AutoPearlThrow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlane = settings.createGroup("Target Plane");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Which blocks to target.")
            .defaultValue(
                    Blocks.AMETHYST_BLOCK,
                    Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.RAW_IRON_BLOCK,
                    Blocks.COPPER_BLOCK,
                    Blocks.RAW_COPPER_BLOCK,
                    Blocks.DEEPSLATE
            )
            .build()
    );

    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("How long to wait before throwing another pearl.")
            .defaultValue(25)
            .min(0)
            .max(500)
            .sliderMin(0)
            .sliderMax(50)
            .build()
    );

    private final Setting<Double> velocity = sgGeneral.add(new DoubleSetting.Builder()
            .name("velocity")
            .description("The velocity.")
            .defaultValue(1.5)
            .min(0)
            .max(5)
            .sliderMin(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> gravity = sgGeneral.add(new DoubleSetting.Builder()
            .name("gravity")
            .description("The gravity.")
            .defaultValue(0.003)
            .min(0)
            .max(1)
            .sliderMin(0)
            .sliderMax(0.1)
            .build()
    );

    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
            .name("multiplier")
            .description("The multiplier.")
            .defaultValue(1)
            .min(-5)
            .max(5)
            .sliderMin(-5)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("Whether to debug.")
            .defaultValue(false)
            .build()
    );

    // Plane

    private final Setting<Integer> x1 = sgPlane.add(new IntSetting.Builder()
            .name("x1")
            .description("x1.")
            .defaultValue(0)
            .noSlider()
            .build()
    );

    private final Setting<Integer> y1 = sgPlane.add(new IntSetting.Builder()
            .name("y1")
            .description("y1.")
            .defaultValue(0)
            .noSlider()
            .build()
    );

    private final Setting<Integer> z1 = sgPlane.add(new IntSetting.Builder()
            .name("z1")
            .description("z1.")
            .defaultValue(0)
            .noSlider()
            .build()
    );

    private final Setting<Integer> x2 = sgPlane.add(new IntSetting.Builder()
            .name("x2")
            .description("x2.")
            .defaultValue(0)
            .noSlider()
            .build()
    );

    private final Setting<Integer> y2 = sgPlane.add(new IntSetting.Builder()
            .name("y2")
            .description("y2.")
            .defaultValue(0)
            .noSlider()
            .build()
    );

    private final Setting<Integer> z2 = sgPlane.add(new IntSetting.Builder()
            .name("z2")
            .description("z2.")
            .defaultValue(0)
            .noSlider()
            .build()
    );

    private final Setting<Double> offsetX = sgPlane.add(new DoubleSetting.Builder()
            .name("offsetX")
            .description("offsetX.")
            .defaultValue(0)
            .sliderMin(-1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> offsetY = sgPlane.add(new DoubleSetting.Builder()
            .name("offsetY")
            .description("offsetY.")
            .defaultValue(0)
            .sliderMin(-1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> offsetZ = sgPlane.add(new DoubleSetting.Builder()
            .name("offsetZ")
            .description("offsetZ.")
            .defaultValue(0)
            .sliderMin(-1)
            .sliderMax(1)
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
            .description("The side color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255, 45))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the block overlay.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    // Constructor

    public AutoPearlThrow() {
        super(Categories.Misc, "auto-pearl-throw", "Automatically throws enderpearls at certain blocks.");
    }

    // Variables

    private int timer;

    // Overrides

    @Override
    public void onActivate() {
        this.timer = 0;
    }

    // Events

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.timer > this.delay.get() && (mc.player.getMainHandStack().getItem() instanceof EnderPearlItem || mc.player.getOffHandStack().getItem() instanceof EnderPearlItem)) {
            int fromX = this.x1.get();
            int fromY = this.y1.get();
            int fromZ = this.z1.get();

            int toX = this.x2.get();
            int toY = this.y2.get();
            int toZ = this.z2.get();

            boolean incrementX = fromX < toX;
            boolean incrementY = fromY < toY;
            boolean incrementZ = fromZ < toZ;

            Hand hand = mc.player.getMainHandStack().getItem() instanceof EnderPearlItem ? Hand.MAIN_HAND : Hand.OFF_HAND;

            for (int x = fromX; incrementX ? x <= toX : x >= toX; x += incrementX ? 1 : -1) {
                for (int y = fromY; incrementY ? y <= toY : y >= toY; y += incrementY ? 1 : -1) {
                    for (int z = fromZ; incrementZ ? z <= toZ : z >= toZ; z += incrementZ ? 1 : -1) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(pos);

                        for (Block block : this.blocks.get()) {
                            if (state.getBlock() == block) {
                                this.aimAt(pos);
                                if (this.debug.get()) info("target: [ " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " ]");
                                mc.interactionManager.sendSequencedPacket(mc.world, (sequence) -> new PlayerInteractItemC2SPacket(hand, sequence, mc.player.getYaw(), mc.player.getPitch()));
                                this.timer = 0;

                                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

                                return;
                            }
                        }
                    }
                }
            }
        }

        this.timer++;
    }

    // Functions

    private void aimAt(BlockPos pos) {
        double posX = pos.getX() + (this.x1.get() == this.x2.get() ? 0 : 0.5) + this.offsetX.get();
        double posY = pos.getY() + (this.y1.get() == this.y2.get() ? 0 : 0.5) + this.offsetY.get();
        double posZ = pos.getZ() + (this.z1.get() == this.z2.get() ? 0 : 0.5) + this.offsetZ.get();

        float yaw;
        float pitch;

        double relativeX = posX - mc.player.getX();
        double relativeY = posY - mc.player.getY();
        double relativeZ = posZ - mc.player.getZ();

        double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
        double hDistanceSq = hDistance * hDistance;

        float gravity = this.gravity.get().floatValue();
        float velocitySq = (float) (this.velocity.get() * this.velocity.get());
        pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - gravity * (gravity * hDistanceSq + 2 * relativeY * velocitySq))) / (gravity * hDistance))) * this.multiplier.get().floatValue();

        if (Float.isNaN(pitch)) {
            yaw = (float) getYaw(posX, posZ);
            pitch = (float) getPitch(posX, posY, posZ);
        } else {
            yaw = (float) getYaw(posX, posZ);
        }

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private double getYaw(double x, double z) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(z - mc.player.getZ(), x - mc.player.getX())) - 90.0F - mc.player.getYaw());
    }

    private double getPitch(double x, double y, double z) {
        double diffX = x - mc.player.getX();
        double diffY = y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = z - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }
}
