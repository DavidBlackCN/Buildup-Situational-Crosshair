package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.semantic.Visibility;
import java.util.Objects;

/** Render values only. Null base explicitly delegates to vanilla. */
public record CrosshairPresentation(Visibility visibility, ClassicCrosshairType base, SecondaryModifier modifier) {
    public enum SecondaryModifier { NONE, INTERACT, USE, PLACE, TRANSFORM }
    public CrosshairPresentation {
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(modifier);
        if (base == null || visibility != Visibility.SHOW) {
            if (modifier != SecondaryModifier.NONE)
                throw new IllegalArgumentException("Hidden/fallback presentations cannot carry modifiers");
        }
    }
    public boolean customVisible() { return visibility == Visibility.SHOW && base != null; }
    public static CrosshairPresentation unavailable(Visibility visibility) {
        return new CrosshairPresentation(visibility, null, SecondaryModifier.NONE);
    }
}
