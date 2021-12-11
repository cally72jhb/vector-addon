package cally72jhb.addon.utils.misc;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;

public class TimeVec extends Vec3d {
    private final long time;

    public TimeVec(double x, double y, double z, long time) {
        super(x, y, z);
        this.time = time;
    }

    public TimeVec(Vec3i vector, long time) {
        super(new Vec3f(vector.getX(), vector.getY(), vector.getZ()));
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
