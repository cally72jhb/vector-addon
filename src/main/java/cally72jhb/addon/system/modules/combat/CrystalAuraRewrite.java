package cally72jhb.addon.system.modules.combat;

import cally72jhb.addon.VectorAddon;
import cally72jhb.addon.utils.VectorUtils;
import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.*;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class CrystalAuraRewrite extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("How long to wait before hitting the crystal again.")
        .defaultValue(4.5)
        .min(0)
        .build()
    );

    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("break-delay")
        .description("How long to wait before hitting the crystal again.")
        .defaultValue(2)
        .min(0)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("How long to wait before placing crystals again.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the crystals being placed or broken.")
        .defaultValue(true)
        .build()
    );

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
        .name("auto-switch")
        .description("Switches to crystals in your hotbar once a target is found.")
        .defaultValue(AutoSwitchMode.Normal)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break crystals if they will kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-damage")
        .description("Maximum damage crystals can deal to yourself.")
        .defaultValue(6)
        .range(0, 36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predicts target movement.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-terrain")
        .description("Completely ignores terrain if it can be blown up by end crystals.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> renderSwing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders hand swinging client side.")
        .defaultValue(true)
        .build()
    );

    private RaycastContext raycastContext;

    private ArrayList<PlayerEntity> targets;
    private ArrayList<Integer> crystals;

    private int breakTimer;
    private int placeTimer;

    public CrystalAuraRewrite() {
        super(VectorAddon.MISC, "crystal-aura-rewrite", "Automatically places and blows up crystals near targets.");
    }

    @Override
    public void onActivate() {
        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        targets = new ArrayList<>();
        crystals = new ArrayList<>();

        breakTimer = 0;
        placeTimer = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!crystals.isEmpty() && (breakTimer >= breakDelay.get() || breakDelay.get() == 0)) {
            for (int id : crystals) {
                Entity entity = mc.world.getEntityById(id);

                if (entity != null && entity.isAlive() && shouldBreak(entity.getPos())) {
                    attack(entity);
                }
            }
        }

        breakTimer++;
        placeTimer++;

        getTargets();

        if (!targets.isEmpty() && placeTimer >= placeDelay.get() || placeDelay.get() == 0) {
            ArrayList<BlockPos> positions = VectorUtils.getPositionsAroundPlayer(range.get());

            positions.removeIf(pos -> {
                BlockState state = VectorUtils.getBlockState(pos);
                Block block = VectorUtils.getBlock(pos.down());

                return !state.isAir() || (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK);
            });

            BlockPos crystalPos = null;
            double bestDamage = 0;

            for (BlockPos pos : positions) {
                double selfDamage = DamageUtils.crystalDamage(mc.player, Vec3d.ofCenter(pos), predictMovement.get(), pos, ignoreTerrain.get());
                if (selfDamage < maxDamage.get() && (!antiSuicide.get() || selfDamage < EntityUtils.getTotalHealth(mc.player))) {
                    double x = pos.getX();
                    double y = pos.getY() + 1;
                    double z = pos.getZ();

                    Box box = new Box(x - 0.5, y, z - 0.5, x + 1.5, y + 1.5, z + 1.5);

                    if (!EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator())) {
                        double damage = getDamageToTargets(Vec3d.ofCenter(pos), pos);
                        if (damage > bestDamage) {
                            bestDamage = damage;
                            crystalPos = pos.down();
                        }
                    }
                }
            }

            if (crystalPos != null) {
                info(crystalPos + "");
                placeCrystal(crystalPos);
                placeTimer = 0;
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(mc.player, entity.getPos()) < PlayerUtils.getTotalHealth()) {
                crystals.add(entity.getId());
            }
        }
    }

    private void getTargets() {
        targets.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isAlive() && !player.isDead() && !player.getAbilities().creativeMode && player != mc.player
                && !player.getAbilities().invulnerable && Friends.get().shouldAttack(player)
                && VectorUtils.distance(mc.player.getPos(), player.getPos()) <= range.get()) {
                targets.add(player);
            }
        }

        for (PlayerEntity player : FakePlayerManager.getPlayers()) {
            if (Friends.get().shouldAttack(player) && VectorUtils.distance(mc.player.getPos(), player.getPos()) <= range.get()) {
                targets.add(player);
            }
        }
    }

    private boolean shouldBreak(Vec3d pos) {
        for (PlayerEntity player : targets) {
            ((IRaycastContext) raycastContext).set(pos, player.getPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result != null && result.getType() != HitResult.Type.BLOCK) {
                return true;
            }
        }

        return false;
    }

    // Utils

    private void attack(Entity entity) {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getPitch(entity), Rotations.getYaw(entity), () -> mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking())));
        } else {
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        }

        swingHand(Hand.MAIN_HAND);
    }

    private void placeCrystal(BlockPos pos) {
        FindItemResult item = InvUtils.findInHotbar(Items.END_CRYSTAL);

        Hand hand = item.getHand();
        if (hand == null) return;

        BlockHitResult result = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand()) {
            InvUtils.swap(item.getSlot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
            swingHand(Hand.MAIN_HAND);
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, result));
            swingHand(hand);
        }
    }

    private double getDamageToTargets(Vec3d vec3d, BlockPos obsidianPos) {
        double damage = 0;

        for (PlayerEntity target : targets) {
            double dmg = DamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos, ignoreTerrain.get());

            damage += dmg;
        }

        return damage;
    }

    private void swingHand(Hand hand) {
        if (renderSwing.get()) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        ArrayList<BlockPos> positions = VectorUtils.getPositionsAroundPlayer(range.get());

        positions.removeIf(pos -> {
            BlockState state = VectorUtils.getBlockState(pos);
            Block block = VectorUtils.getBlock(pos.down());

            return !state.isAir() || (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK);
        });

        if (!positions.isEmpty()) {
            for (BlockPos pos : positions) {
                Color color = new Color(255, 255, 255, 255);
                event.renderer.box(pos, color, color, ShapeMode.Both, 0);
            }
        }
    }

    // Enums

    public enum AutoSwitchMode {
        Normal,
        Silent,
        None
    }
}
