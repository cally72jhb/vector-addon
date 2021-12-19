package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;

import java.util.ArrayList;

public class SilentAntiGhost extends Module {
    private ArrayList<Entity> crystals;

    public SilentAntiGhost() {
        super(VectorAddon.CATEGORY, "silent-anti-ghost", "Removes ghost blocks around you.");
    }

    @Override
    public void onActivate() {
        crystals = new ArrayList<>();
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof EndCrystalEntity entity) crystals.add(entity);
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity entity && !VectorUtils.getBlockState(entity.getBlockPos()).isAir()) {
            mc.world.setBlockState(entity.getBlockPos(), Blocks.AIR.getDefaultState());
            crystals.remove(entity);
        }
    }
}
