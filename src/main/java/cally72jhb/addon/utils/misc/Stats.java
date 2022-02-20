package cally72jhb.addon.utils.misc;

import cally72jhb.addon.utils.VectorUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Stats {
    private final MinecraftClient mc;

    private final ArrayList<String> strings;
    public final HashMap<UUID, Integer> kills;
    public final HashMap<UUID, Integer> pops;

    public int allKills;
    public int allPops;
    public int deaths;

    private boolean ignoreFriends;
    private boolean clearPopsOnDeath;
    private boolean clearKillsOnDeath;
    private boolean checkTargets;
    private double range;

    public Stats(double range) {
        MeteorClient.EVENT_BUS.subscribe(this);
        mc = VectorUtils.mc;

        strings = new ArrayList<>();
        kills = new HashMap<>();
        pops = new HashMap<>();

        allKills = 0;
        allPops = 0;
        deaths = 0;

        this.ignoreFriends = true;
        this.clearPopsOnDeath = true;
        this.clearKillsOnDeath = true;
        this.checkTargets = false;
        this.range = range;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() != 35 && packet.getStatus() != 3) return;
            if (packet.getStatus() == 3 && packet.getEntity(mc.world) == mc.player) {
                if (clearPopsOnDeath) {
                    pops.clear();
                    allPops = 0;
                }
                if (clearKillsOnDeath) {
                    kills.clear();
                    allKills = 0;
                }

                deaths++;
            }

            Entity entity = packet.getEntity(mc.world);

            if (!check(entity)) return;

            if (packet.getStatus() == 35) {
                pops.putIfAbsent(entity.getUuid(), 0);
                pops.replace(entity.getUuid(), pops.get(entity.getUuid()) + 1);

                allPops++;
            }

            if (packet.getStatus() == 3) {
                kills.putIfAbsent(entity.getUuid(), 0);
                kills.replace(entity.getUuid(), kills.get(entity.getUuid()) + 1);

                allKills++;
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        strings.clear();
        kills.clear();
        pops.clear();

        allKills = 0;
        allPops = 0;
        deaths = 0;
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (checkTargets) {
            strings.clear();
            for (Module module : Modules.get().getAll()) {
                if (module != null && module.getInfoString() != null) strings.add(module.getInfoString());
            }
        }
    }

    private boolean check(Entity entity) {
        if (!(entity instanceof PlayerEntity) || entity == mc.player
            || (Friends.get().isFriend((PlayerEntity) entity) && ignoreFriends)
            || VectorUtils.distance(mc.player.getPos(), entity.getPos()) > range) return false;

        return !checkTargets || !strings.isEmpty() && strings.contains(entity.getEntityName());
    }

    public void setIgnoreFriends(boolean ignoreFriends) {
        this.ignoreFriends = ignoreFriends;
    }

    public void setClearPopsOnDeath(boolean clearPopsOnDeath) {
        this.clearPopsOnDeath = clearPopsOnDeath;
    }

    public void setClearKillsOnDeath(boolean clearKillsOnDeath) {
        this.clearKillsOnDeath = clearKillsOnDeath;
    }

    public void setCheckTargets(boolean checkTargets) {
        this.checkTargets = checkTargets;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public void setAllKills(int allKills) {
        this.allKills = allKills;
    }

    public void setAllPops(int allPops) {
        this.allPops = allPops;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
}
