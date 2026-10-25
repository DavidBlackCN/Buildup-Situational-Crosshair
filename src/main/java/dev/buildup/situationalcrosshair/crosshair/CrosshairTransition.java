package dev.buildup.situationalcrosshair.crosshair;

import java.util.Arrays;

/** Small, time-based pixel transition. Owns no game state or graphics resources. */
public final class CrosshairTransition {
    public enum Mode { OFF, FADE, EXPAND }
    public static final int SIZE = 15;
    private static final int CENTER = 7 * SIZE + 7;
    private final float[][] masks;
    private final float[] from = new float[SIZE * SIZE];
    private final float[] frame = new float[SIZE * SIZE];
    private ClassicCrosshairType target;
    private Mode mode;
    private long start;
    private long duration;
    private boolean active;
    private int motion;

    public CrosshairTransition(float[][] masks) {
        this.masks = Arrays.stream(masks).map(float[]::clone).toArray(float[][]::new);
    }

    /** The returned buffer belongs to this controller and is valid until the next call. */
    public float[] update(ClassicCrosshairType next, Mode nextMode, long now) {
        if (next == null) { reset(); return frame; }
        if (target == null || nextMode == Mode.OFF || mode != nextMode) {
            target = next;
            mode = nextMode;
            active = false;
            copy(masks[target.ordinal()], frame);
            return frame;
        }
        sample(now);
        if (next != target) {
            boolean fromDot = !active && target == ClassicCrosshairType.DOT;
            copy(frame, from);
            target = next;
            start = now;
            duration = (next == ClassicCrosshairType.ERROR ? 65 : next == ClassicCrosshairType.DOT ? 100 : 140) * 1_000_000L;
            motion = nextMode != Mode.EXPAND ? 0 : next == ClassicCrosshairType.DOT ? -1
                    : fromDot && next != ClassicCrosshairType.ERROR ? 1 : 0;
            active = true;
            sample(now);
        }
        return frame;
    }

    private void sample(long now) {
        if (!active) return;
        float time = Math.clamp((float) (now - start) / duration, 0, 1);
        float t = 1 - (1 - time) * (1 - time) * (1 - time);
        var goal = masks[target.ordinal()];
        if (time >= 1) {
            copy(goal, frame);
            active = false;
        } else if (motion == 0) {
            for (int i = 0; i < frame.length; i++) frame[i] = from[i] + (goal[i] - from[i]) * t;
        } else {
            Arrays.fill(frame, 0);
            var moving = motion > 0 ? goal : from;
            float opacity = motion > 0 ? t : 1 - t;
            float inset = motion > 0 ? 1 - t : t;
            for (int i = 0; i < moving.length; i++) {
                if (i == CENTER || moving[i] == 0) continue;
                int x = i % SIZE, y = i / SIZE;
                // At most one GUI pixel of travel; no scaling of stroke width.
                float px = x - Math.signum(x - 7) * inset;
                float py = y - Math.signum(y - 7) * inset;
                int ix = (int) Math.floor(px), iy = (int) Math.floor(py);
                float fx = px - ix, fy = py - iy;
                add(ix, iy, moving[i] * opacity * (1 - fx) * (1 - fy));
                add(ix + 1, iy, moving[i] * opacity * fx * (1 - fy));
                add(ix, iy + 1, moving[i] * opacity * (1 - fx) * fy);
                add(ix + 1, iy + 1, moving[i] * opacity * fx * fy);
            }
            frame[CENTER] = 1;
        }
    }

    private void add(int x, int y, float value) {
        if (x >= 0 && y >= 0 && x < SIZE && y < SIZE)
            frame[y * SIZE + x] = Math.min(1, frame[y * SIZE + x] + value);
    }

    public boolean active() { return active; }

    public void reset() {
        target = null;
        active = false;
        Arrays.fill(frame, 0);
    }

    private static void copy(float[] source, float[] destination) {
        System.arraycopy(source, 0, destination, 0, destination.length);
    }
}
