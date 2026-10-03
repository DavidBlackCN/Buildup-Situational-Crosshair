package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.semantic.Visibility;
import java.util.Objects;

/** Pure fixed-role composition. No general-purpose status channels. */
public record CrosshairPresentation(Visibility visibility, ClassicCrosshairType base,
        StateSidecar leftSidecar, ActionSidecar rightSidecar) {
    public enum StateSidecar { NONE, BLOCKED }
    public enum ActionSidecar { NONE, INTERACT, USE, PLACE, TRANSFORM }
    public CrosshairPresentation {
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(leftSidecar);
        Objects.requireNonNull(rightSidecar);
        if ((base == null || visibility != Visibility.SHOW) && (leftSidecar != StateSidecar.NONE || rightSidecar != ActionSidecar.NONE))
            throw new IllegalArgumentException("Hidden/fallback presentations cannot carry sidecars");
        if (leftSidecar != StateSidecar.NONE && rightSidecar == ActionSidecar.NONE)
            throw new IllegalArgumentException("A state cue must qualify a visible action");
    }
    public boolean customVisible() { return visibility == Visibility.SHOW && base != null; }
    public int sidecarCount() { return (leftSidecar == StateSidecar.NONE ? 0 : 1) + (rightSidecar == ActionSidecar.NONE ? 0 : 1); }
    public static CrosshairPresentation baseOnly(ClassicCrosshairType base) {
        return new CrosshairPresentation(Visibility.SHOW, base, StateSidecar.NONE, ActionSidecar.NONE);
    }
    public static CrosshairPresentation unavailable(Visibility visibility) {
        return new CrosshairPresentation(visibility, null, StateSidecar.NONE, ActionSidecar.NONE);
    }
}
