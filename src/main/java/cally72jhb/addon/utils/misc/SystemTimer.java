package cally72jhb.addon.utils.misc;

public class SystemTimer {
    private long time;

    public SystemTimer() {
        time = System.currentTimeMillis();
    }

    public boolean hasPassed(double ms) {
        return System.currentTimeMillis() - time >= ms;
    }

    public void reset() {
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
