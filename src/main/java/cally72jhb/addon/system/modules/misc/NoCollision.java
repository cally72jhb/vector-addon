package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;

public class NoCollision extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("The blocks to fill the holes with.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<ListMode> blockFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("block-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<Boolean> borderCollision = sgGeneral.add(new BoolSetting.Builder()
        .name("no-world-border-collision")
        .description("Cancels the collision with the world border to allow you to walk through it.")
        .defaultValue(true)
        .build()
    );

    public NoCollision() {
        super(Categories.Misc, "no-collision", "Removes block and world border collision client-side.");
    }

    // Canceling Collision

    @EventHandler(priority = EventPriority.LOWEST + 25)
    private void onCollisionShape(CollisionShapeEvent event) {
        if (validBlock(event.state.getBlock())) event.shape = VoxelShapes.empty();
    }

    // Utils

    private boolean validBlock(Block block) {
        if (blockFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else return blockFilter.get() != ListMode.Whitelist || blocks.get().contains(block);
    }

    // Getter

    public boolean shouldCancelBorderCollision() {
        return borderCollision.get();
    }

    // Constant

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
