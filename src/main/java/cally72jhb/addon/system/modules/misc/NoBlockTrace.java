package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class NoBlockTrace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("The blocks to fill the holes with.")
        .defaultValue(new ArrayList<>() {{
            add(Blocks.NETHER_PORTAL);
            add(Blocks.END_PORTAL);
            add(Blocks.GRASS);
            add(Blocks.TALL_GRASS);
            add(Blocks.TALL_SEAGRASS);
            add(Blocks.SEAGRASS);
        }})
        .build()
    );

    private final Setting<ListMode> blockFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("block-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingSword = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-holding-sword")
        .description("Whether or not to work only when holding a sword.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingPick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-holding-pick")
        .description("Whether or not to work only when holding a pickaxe.")
        .defaultValue(true)
        .build()
    );

    private boolean cancel;

    public NoBlockTrace() {
        super(Categories.Misc, "no-block-trace", "Allows you to interact through selected blocks.");
    }

    @Override
    public void onActivate() {
        cancel = true;
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (cancel && event.state != null && shouldCancelOutline(event.state.getBlock())) {
            cancel = false;
            event.shape = event.state.getCollisionShape(mc.world, event.pos);
            cancel = true;
        }
    }

    // Return

    public boolean shouldCancelOutline(BlockPos pos) {
        if (mc == null || mc.world == null || mc.player == null) return false;

        return mc.world.getBlockState(pos) != null && shouldCancelOutline(mc.world.getBlockState(pos).getBlock());
    }

    public boolean shouldCancelOutline(Block block) {
        if (mc == null || mc.world == null || mc.player == null || block == null) return false;

        cancel = true;

        Item mainhand = mc.player.getMainHandStack().getItem();
        Item offhand = mc.player.getOffHandStack().getItem();

        return validBlock(block)
            && (((onlyWhenHoldingSword.get() && (mainhand instanceof SwordItem || offhand instanceof SwordItem))
            || (onlyWhenHoldingPick.get() && (mainhand instanceof PickaxeItem || offhand instanceof PickaxeItem))
            || (!onlyWhenHoldingSword.get() && !onlyWhenHoldingPick.get())));
    }

    private boolean validBlock(Block block) {
        if (blockFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else return blockFilter.get() != ListMode.Whitelist || blocks.get().contains(block);
    }

    // Constants

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
