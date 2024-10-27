package cally72jhb.addon.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class SmartSprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SprintMode> sprintMode = sgGeneral.add(new EnumSetting.Builder<SprintMode>()
            .name("sprint-mode")
            .description("What mode to use.")
            .defaultValue(SprintMode.Advanced)
            .build()
    );

    private final Setting<Boolean> whenStationary = sgGeneral.add(new BoolSetting.Builder()
            .name("when-stationary")
            .description("Continues sprinting even if you do not move.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> stopOnDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("stop-on-disable")
            .description("Stops sprinting when you disable the module.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder()
            .name("strict")
            .description("Stops sprinting when you normally would.")
            .defaultValue(false)
            .visible(() -> this.sprintMode.get() == SprintMode.Advanced)
            .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
            .name("sprint-cooldown")
            .description("How long to wait before sprinting again after failing to sprint.")
            .defaultValue(2)
            .min(1)
            .max(10)
            .visible(() -> this.sprintMode.get() == SprintMode.Advanced && this.strict.get())
            .noSlider()
            .build()
    );

    private final Setting<Boolean> screens = sgGeneral.add(new BoolSetting.Builder()
            .name("screens")
            .description("Stops sprinting when you have your inventory opened.")
            .defaultValue(true)
            .visible(() -> this.sprintMode.get() == SprintMode.Advanced)
            .build()
    );

    private final Setting<Boolean> sprintKeyOpposite = sgGeneral.add(new BoolSetting.Builder()
            .name("sprint-key-opposite")
            .description("Use the sprint key to stop sprinting when this module decides to sprint.")
            .defaultValue(false)
            .visible(() -> this.sprintMode.get() == SprintMode.Normal)
            .build()
    );

    // Variables

    private int prevTickSprint;

    // Constructor

    public SmartSprint() {
        super(Categories.Movement, "smart-sprint", "Automatically sprints.");
    }

    // Overrides

    @Override
    public void onActivate() {
        this.prevTickSprint = 0;
    }

    @Override
    public void onDeactivate() {
        if (mc != null && mc.player != null && mc.world != null && this.stopOnDisable.get()) {
            if (this.sprintMode.get() == SprintMode.Key) {
                mc.options.sprintKey.setPressed(false);
            } else {
                mc.player.setSprinting(false);
            }
        }
    }

    @Override
    public String getInfoString() {
        return (mc == null || mc.player == null || mc.world == null) ? null : (mc.player.isSprinting() ? "sprinting" : null);
    }

    // Post Tick Event

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (this.strict.get() && !mc.player.isSprinting() && this.prevTickSprint > 0) {
            this.prevTickSprint--;
            return;
        }

        if (this.sprintMode.get() != SprintMode.Key && this.sprintKeyOpposite.get() && mc.options.sprintKey.isPressed()) {
            return;
        }

        if (this.sprintMode.get() == SprintMode.Key) {
            if (mc.player.input.hasForwardMovement() && !this.whenStationary.get() || this.whenStationary.get()) {
                mc.options.sprintKey.setPressed(true);
                this.prevTickSprint = this.cooldown.get();
            }
        } else if (this.sprintMode.get() == SprintMode.Advanced) {
            boolean sprinting = mc.player.isSprinting();
            boolean sprint = !this.sprintKeyOpposite.get() || !mc.options.sprintKey.isPressed();

            if (mc.player.input.hasForwardMovement() && !this.whenStationary.get() || this.whenStationary.get()) {
                if (this.sprintMode.get() == SprintMode.Advanced) {
                    boolean generalCheck = !mc.player.input.hasForwardMovement() || !(mc.player.hasVehicle() || mc.player.getHungerManager().getFoodLevel() > 6.0F || mc.player.getAbilities().allowFlying);
                    boolean collisionCheck = generalCheck || mc.player.horizontalCollision && !mc.player.collidedSoftly || mc.player.isTouchingWater() && !mc.player.isSubmergedInWater();

                    if (mc.player.isSwimming()) {
                        if (mc.player.isOnGround() || mc.player.input.sneaking || generalCheck && mc.player.isTouchingWater()) {
                            this.sprint(sprint);
                            sprinting = sprint;
                        } else if (this.screens.get() || mc.currentScreen != null) {
                            this.sprint(!sprint);
                            sprinting = !sprint;
                        }
                    } else if (!collisionCheck) {
                        this.sprint(sprint);
                        sprinting = sprint;
                    } else if (this.screens.get() || mc.currentScreen != null) {
                        this.sprint(!sprint);
                        sprinting = !sprint;
                    }
                } else {
                    this.sprint(sprint);
                    sprinting = sprint;
                }
            }

            this.prevTickSprint = sprinting ? this.cooldown.get() : 0;
        } else {
            if (mc.player.input.hasForwardMovement() && !this.whenStationary.get() || this.whenStationary.get()) {
                mc.player.setSprinting(true);
            }
        }
    }

    // Utils

    private void sprint(boolean sprint) {
        if (sprint) {
            if (this.sprintMode.get() != SprintMode.Advanced || !mc.player.isFallFlying() && (!this.screens.get() || mc.currentScreen == null)) {
                mc.player.setSprinting(true);
            }
        } else {
            mc.player.setSprinting(false);
        }
    }

    // Enums

    public enum SprintMode {
        Advanced,
        Normal,
        Key
    }
}
