package dev.buildup.situationalcrosshair.crosshair;

/** Four-neighbor contour from the current frame, not a second independent animation. */
public final class CrosshairOutline {
    public static final int SIZE = 17;
    private CrosshairOutline() { }

    /** Output includes a one-pixel margin around the 15x15 source. */
    public static void write(float[] frame, float[] output) {
        for (int y = -1; y <= 15; y++) for (int x = -1; x <= 15; x++) {
            float edge = Math.max(Math.max(neighbor(frame, x - 1, y), neighbor(frame, x + 1, y)),
                    Math.max(neighbor(frame, x, y - 1), neighbor(frame, x, y + 1)));
            // Full body pixels are never darkened. Fractional edges blend continuously.
            output[(y + 1) * SIZE + x + 1] = edge * (1 - coverage(frame, x, y));
        }
    }

    private static float neighbor(float[] frame, int x, int y) {
        // Keep the one-pixel aiming point light: no diagonal box or opaque backing.
        return coverage(frame, x, y) * (x == 7 && y == 7 ? 0.22f : 0.42f);
    }

    private static float coverage(float[] frame, int x, int y) {
        return x < 0 || y < 0 || x >= 15 || y >= 15 ? 0 : frame[y * 15 + x];
    }
}
