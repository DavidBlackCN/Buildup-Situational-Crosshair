package dev.buildup.situationalcrosshair.crosshair;

import java.nio.ByteBuffer;

/** Whole-glyph tone selection. Input is display-encoded RGBA8, not world lighting. */
public final class AdaptiveContrast {
    public static final int LIGHT = 0xfff4f4f4;
    public static final int DARK = 0xff181818;
    private static final double[] LINEAR = new double[256];
    static { for (int i = 0; i < LINEAR.length; i++) LINEAR[i] = linear(i); }
    private boolean initialized, dark;
    private double filtered;
    private long lastTime, candidateSince = -1, surfaceSince;
    private Object surface;

    public int accept(double luminance, long now, Object surfaceKey) {
        if (!Double.isFinite(luminance) || luminance < 0 || luminance > 1)
            throw new IllegalArgumentException("Invalid background luminance");
        if (!initialized) {
            initialized = true;
            filtered = luminance;
            lastTime = surfaceSince = now;
            surface = surfaceKey;
            dark = luminance >= 0.19;
        } else {
            long elapsed = Math.clamp(now - lastTime, 0, 500_000_000L);
            lastTime = now;
            if (!java.util.Objects.equals(surface, surfaceKey)) {
                surface = surfaceKey;
                surfaceSince = now;
            }
            filtered += (luminance - filtered) * (1 - Math.exp(-elapsed / 160_000_000.0));
            // Hold the selected tone on the same block face/entity. Texture texels are not new scenes.
            // Allow time to settle on a newly acquired surface, then switch only for sustained extremes.
            boolean anchored = surface != null && now - surfaceSince >= 400_000_000L;
            boolean switchTone = dark ? filtered < (anchored ? 0.04 : 0.17)
                    : filtered > (anchored ? 0.55 : 0.22);
            if (switchTone) {
                if (candidateSince < 0) candidateSince = now;
                if (now - candidateSince >= (anchored ? 350_000_000L : 150_000_000L)) {
                    dark = !dark;
                    candidateSince = -1;
                }
            } else candidateSince = -1;
        }
        return dark ? DARK : LIGHT;
    }

    public void reset() { initialized = false; candidateSince = -1; surface = null; }

    public static double luminance(ByteBuffer rgba) {
        int length = rgba.remaining();
        if (length == 0 || length % 4 != 0) throw new IllegalArgumentException("Expected RGBA8 pixels");
        double sum = 0;
        for (int i = rgba.position(); i < rgba.limit(); i += 4)
            sum += 0.2126 * LINEAR[rgba.get(i) & 255] + 0.7152 * LINEAR[rgba.get(i + 1) & 255]
                    + 0.0722 * LINEAR[rgba.get(i + 2) & 255];
        return sum / (length / 4);
    }

    private static double linear(int value) {
        double channel = value / 255.0;
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
