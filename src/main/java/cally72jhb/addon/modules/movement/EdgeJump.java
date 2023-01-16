package cally72jhb.addon.modules.movement;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.events.JumpEvent;
import com.google.common.collect.Streams;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.shape.VoxelShape;

import java.util.stream.Stream;

public class EdgeJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("When to perform the jump.")
            .defaultValue(Mode.Post)
            .build()
    );

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-edge-distance")
            .description("How far you at least have to be from the edge to jump.")
            .defaultValue(0.001)
            .min(0)
            .max(0.999)
            .sliderMin(0.001)
            .sliderMax(0.1)
            .build()
    );

    private final Setting<Double> minHeight = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-height")
            .description("How high the distance between the point you are standing and the floor have to be to jump.")
            .defaultValue(0.5)
            .min(0.001)
            .sliderMin(0.001)
            .sliderMax(0.75)
            .build()
    );

    // Variables

    private boolean jumped;

    // Constructor

    public EdgeJump() {
        super(VectorAddon.CATEGORY, "edge-jump", "Automatically jumps at the edges of blocks.");
    }

    // Overrides

    @Override
    public void onActivate() {
        this.jumped = false;
    }

    // Pre Tick Event

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Pre) {
            if (!this.jumped && mc.player.isOnGround() && !mc.options.jumpKey.isPressed() && !mc.player.input.jumping
                    && !mc.player.isSneaking() && !mc.options.sneakKey.isPressed() && !mc.player.input.sneaking) {
                Stream<VoxelShape> collisions = Streams.stream(
                        mc.world.getBlockCollisions(
                                mc.player,
                                mc.player.getBoundingBox().stretch(0, -minHeight.get(), 0).expand(-distance.get(), 0, -distance.get())
                        )
                );

                // Collision Check

                if (collisions.findAny().isEmpty()) {
                    mc.player.jump();
                    this.jumped = true;
                }
            }
        } else {
            this.jumped = false;
        }
    }

    // Post Tick Event

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mode.get() == Mode.Post) {
            if (!this.jumped && mc.player.isOnGround() && !mc.options.jumpKey.isPressed() && !mc.player.input.jumping
                    && !mc.player.isSneaking() && !mc.options.sneakKey.isPressed() && !mc.player.input.sneaking) {
                Stream<VoxelShape> collisions = Streams.stream(
                        mc.world.getBlockCollisions(
                                mc.player,
                                mc.player.getBoundingBox().stretch(0, -minHeight.get(), 0).expand(-distance.get(), 0, -distance.get())
                        )
                );

                // Collision Check

                if (collisions.findAny().isEmpty()) {
                    mc.player.jump();
                    this.jumped = true;
                }
            }
        } else {
            this.jumped = false;
        }
    }

    // Jump Event

    @EventHandler(priority = EventPriority.HIGHEST + 250)
    private void onJump(JumpEvent event) {
        if (this.jumped) {
            event.cancel();
        }
    }

    // Enums

    public enum Mode {
        Pre,
        Post
    }
}
