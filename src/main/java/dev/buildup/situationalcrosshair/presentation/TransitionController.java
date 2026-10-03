package dev.buildup.situationalcrosshair.presentation;

/** Independent, pixel-aligned docking of the current sidecars; never crossfades old meaning. */
public final class TransitionController {
    public static final long DURATION_NANOS = 100_000_000L;
    public static final int MAX_OFFSET = 2;
    public record Frame(CrosshairPresentation presentation, float leftOpacity, float rightOpacity, int leftOffset, int rightOffset) {
        public static Frame immediate(CrosshairPresentation presentation) { return new Frame(presentation, 1, 1, 0, 0); }
    }
    private CrosshairPresentation previous;
    private long leftStarted, rightStarted;
    private TransitionMode previousMode;

    public Frame update(CrosshairPresentation current, TransitionMode mode, long now) {
        java.util.Objects.requireNonNull(current);
        java.util.Objects.requireNonNull(mode);
        if (!current.customVisible()) { reset(); return Frame.immediate(current); }
        if (mode == TransitionMode.OFF) {
            previous = current; previousMode = mode; leftStarted = rightStarted = now;
            return Frame.immediate(current);
        }
        boolean reset = previous == null || mode != previousMode || now < leftStarted || now < rightStarted;
        // A changed base does not replay an unchanged cue. A changed action also
        // re-docks its qualifying state, while the unchanged right cue is stable.
        if (reset || current.rightSidecar() != previous.rightSidecar()) rightStarted = now;
        if (reset || current.leftSidecar() != previous.leftSidecar() || current.rightSidecar() != previous.rightSidecar()) leftStarted = now;
        previous = current; previousMode = mode;
        float left = current.leftSidecar() == CrosshairPresentation.StateSidecar.NONE ? 1 : ease(now, leftStarted);
        float right = current.rightSidecar() == CrosshairPresentation.ActionSidecar.NONE ? 1 : ease(now, rightStarted);
        return new Frame(current, 0.45f + 0.55f * left, 0.45f + 0.55f * right,
                Math.round(MAX_OFFSET * (1 - left)), -Math.round(MAX_OFFSET * (1 - right)));
    }
    private static float ease(long now, long started) {
        float p = Math.clamp((now - started) / (float) DURATION_NANOS, 0, 1);
        float remaining = 1 - p;
        return 1 - remaining * remaining * remaining;
    }
    public void reset() { previous = null; previousMode = null; leftStarted = rightStarted = 0; }
}
