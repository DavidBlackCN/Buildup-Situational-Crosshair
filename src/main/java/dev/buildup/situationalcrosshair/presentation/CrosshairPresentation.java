package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.semantic.ActionState;
import dev.buildup.situationalcrosshair.semantic.Visibility;
import java.util.Objects;

/** Render values only. Null base explicitly delegates to vanilla. */
public record CrosshairPresentation(Visibility visibility, ClassicCrosshairType base,
        SecondaryModifier modifier, ActionState primaryStatus, ActionState secondaryStatus) {
    public enum SecondaryModifier { NONE, INTERACT, USE, PLACE, TRANSFORM, SPECIAL }
    public CrosshairPresentation {
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(modifier);
        Objects.requireNonNull(primaryStatus);
        Objects.requireNonNull(secondaryStatus);
        if (base == null || visibility != Visibility.SHOW) {
            if (modifier != SecondaryModifier.NONE || primaryStatus != ActionState.NORMAL || secondaryStatus != ActionState.NORMAL)
                throw new IllegalArgumentException("Hidden/fallback presentations cannot carry modifiers");
        }
        if (modifier == SecondaryModifier.NONE && secondaryStatus != ActionState.NORMAL)
            throw new IllegalArgumentException("Secondary status requires an effective secondary modifier");
    }
    public boolean customVisible() { return visibility == Visibility.SHOW && base != null; }
    public static CrosshairPresentation unavailable(Visibility visibility) {
        return new CrosshairPresentation(visibility, null, SecondaryModifier.NONE, ActionState.NORMAL, ActionState.NORMAL);
    }
}
