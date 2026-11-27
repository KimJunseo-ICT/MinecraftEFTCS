package com.minecrafteft.client;

/** Render-time interpolation between 250 ms server snapshots; never changes gameplay values. */
public final class VitalGaugeAnimation {
    private static final long DURATION = 250_000_000L;
    private double from, target;
    private long changedAt, lastUpdatedAt;
    private boolean initialized;

    public void snap(double fraction, long now) {
        from = target = clamp(fraction);
        changedAt = now;
        lastUpdatedAt = now;
        initialized = true;
    }
    public void target(double fraction, long now) {
        double next = clamp(fraction);
        boolean stale = !initialized || now - lastUpdatedAt > 1_000_000_000L;
        lastUpdatedAt = now;
        if (stale) { snap(next, now); return; }
        if (next == target) return;
        from = value(now);
        target = next;
        changedAt = now;
    }
    public double value(long now) {
        double progress = Math.max(0, Math.min(1, (double) (now - changedAt) / DURATION));
        return from + (target - from) * progress;
    }
    private static double clamp(double fraction) {
        return Double.isFinite(fraction) ? Math.max(0, Math.min(1, fraction)) : 0;
    }
}
