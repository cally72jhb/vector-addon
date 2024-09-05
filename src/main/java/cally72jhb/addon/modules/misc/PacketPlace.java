package cally72jhb.addon.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Burrow;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class PacketPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("modules")
            .description("Which modules to ignore when placing blocks.")
            .defaultValue(Burrow.class)
            .build()
    );

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
            .name("play-sound")
            .description("Plays a client-side sound when you place a block with packets.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
            .name("render-swing")
            .description("Renders a hand-swing animation when you place the block.")
            .defaultValue(true)
            .build()
    );

    // Constructor

    public PacketPlace() {
        super(Categories.Misc, "packet-place", "Place blocks with packets to prevent desync from the server.");
    }

    // Listeners

    @EventHandler
    private void onPlaceBlock(InteractBlockEvent event) {
        if (event.hand != null && event.result != null && checkModules()) {
            ItemStack stack = mc.player.getStackInHand(event.hand);
            BlockPos pos = event.result.getBlockPos();

            if (stack.getItem() instanceof BlockItem item
                    && (!isClickable(mc.world.getBlockState(pos).getBlock()) || mc.player.isSneaking())
                    && canPlace(pos.offset(event.result.getSide()), item.getBlock().getDefaultState())) {

                event.cancel();

                place(event.result, event.hand);
            }
        }
    }

    // Utils

    private boolean checkModules() {
        if (mc.world == null || mc.player == null) return false;
        if (modules.get().isEmpty()) return true;

        for (Module module : modules.get()) {
            if (Modules.get().isActive(module.getClass())) return false;
        }

        return true;
    }

    private boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof ButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    // Placing

    private void place(BlockHitResult result, Hand hand) {
        if (hand != null && result != null) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result, 0));

            if (playSound.get() && mc.player.getStackInHand(hand).getItem() instanceof BlockItem item) {
                Block block = item.getBlock();
                BlockSoundGroup group = block.getSoundGroup(block.getDefaultState());

                mc.getSoundManager().play(new PositionedSoundInstance(group.getPlaceSound(), SoundCategory.BLOCKS, (group.getVolume() + 1.0F) / 8.0F, group.getPitch() * 0.5F, Random.create(), result.getBlockPos()));
            }

            if (renderSwing.get()) {
                mc.player.swingHand(hand);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
        }
    }

    private boolean canPlace(BlockPos pos, BlockState state) {
        if (pos == null || mc.world == null || !World.isValid(pos) || !mc.world.getBlockState(pos).isReplaceable()) return false;
        return mc.world.getWorldBorder().contains(pos) && mc.world.canPlace(state, pos, ShapeContext.absent());
    }
}
