package cally72jhb.addon.system.modules.movement;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class NoFluid extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> swimming = sgGeneral.add(new BoolSetting.Builder()
        .name("swimming")
        .description("Disables the swimming animation in water.")
        .onChanged(changed -> update())
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> water = sgGeneral.add(new BoolSetting.Builder()
        .name("water")
        .description("Whether or not to remove water collisions.")
        .onChanged(changed -> update())
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lava = sgGeneral.add(new BoolSetting.Builder()
        .name("lava")
        .description("Whether or not to remove lava collisions.")
        .defaultValue(true)
        .onChanged(changed -> update())
        .build()
    );

    public NoFluid() {
        super(Categories.Movement, "no-fluid", "Removes liquid collisions.");
    }

    @Override
    public void onActivate() {
        mc.player.updateSwimming();
    }

    @Override
    public void onDeactivate() {
        mc.player.updateSwimming();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        update();

        if (mc.player.isTouchingWater()) mc.player.setSwimming(false);
    }

    private void update() {
        if (mc != null && mc.world != null && mc.player != null) mc.player.updateSwimming();
    }

    public boolean inLava() {
        return lava.get() && getFluid(mc.player.getBoundingBox()) instanceof LavaFluid;
    }

    public boolean inWater() {
        return water.get() && getFluid(mc.player.getBoundingBox()) instanceof WaterFluid;
    }

    public boolean disableSwimming() {
        return swimming.get();
    }

    private Fluid getFluid(Box box) {
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.ceil(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for(int o = i; o < j; ++o) {
            for(int p = k; p < l; ++p) {
                for(int q = m; q < n; ++q) {
                    BlockState state = VectorUtils.getBlockState(mutable.set(o, p, q));
                    if (state.getFluidState() != null) return state.getFluidState().getFluid();
                }
            }
        }

        return null;
    }
}
