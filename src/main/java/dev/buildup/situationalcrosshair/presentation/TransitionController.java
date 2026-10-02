package dev.buildup.situationalcrosshair.presentation;

/** One current glyph set, no trails or stale-action crossfades. Clock is supplied for deterministic tests. */
public final class TransitionController {
    public static final long DURATION_NANOS = 100_000_000L;
    public record Frame(CrosshairPresentation presentation, float detailOpacity) { }
    private CrosshairPresentation previous;
    private long started;
    private TransitionMode previousMode;

    public Frame update(CrosshairPresentation current, TransitionMode mode, long now) {
        java.util.Objects.requireNonNull(current);
        java.util.Objects.requireNonNull(mode);
        if (!current.customVisible()) { reset(); return new Frame(current, 1); }
        if (mode == TransitionMode.OFF) {
            previous = current; previousMode = mode; started = now;
            return new Frame(current, 1);
        }
        if (!current.equals(previous) || mode != previousMode || now < started) started = now;
        previous = current; previousMode = mode;
        float progress = Math.clamp((now - started) / (float) DURATION_NANOS, 0, 1);
        float eased = progress * progress * (3 - 2 * progress);
        // The unchanged base is always opaque; new details remain legible while appearing.
        return new Frame(current, 0.35f + 0.65f * eased);
    }
    public void reset() { previous = null; previousMode = null; started = 0; }
}
