package cally72jhb.addon.mixin.meteor;

import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.entity.TargetUtils.get;

@Mixin(TargetUtils.class)
public class TargetUtilsMixin {
    @Inject(method = "getPlayerTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetPlayerTarget(double range, SortPriority priority, CallbackInfoReturnable<PlayerEntity> info) {
        if (!Utils.canUpdate() || mc.world == null) {
            info.setReturnValue(null);
            return;
        }

        ArrayList<PlayerEntity> players = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && Players.get().isTargeted((PlayerEntity) entity)) {
                players.add((PlayerEntity) entity);
            }
        }

        if (!players.isEmpty()) {
            players.sort(Comparator.comparingDouble(player -> VectorUtils.distance(mc.player.getPos(), player.getPos())));
            if (VectorUtils.distance(mc.player.getPos(), players.get(0).getPos()) <= range) {
                info.setReturnValue(players.get(0));
                return;
            }
        }

        info.setReturnValue((PlayerEntity) get(entity -> {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) return false;
            if (((PlayerEntity) entity).isDead() || ((PlayerEntity) entity).getHealth() <= 0) return false;
            if (mc.player.distanceTo(entity) > range) return false;
            if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            return EntityUtils.getGameMode((PlayerEntity) entity) == GameMode.SURVIVAL || entity instanceof FakePlayerEntity;
        }, priority));
    }
}
