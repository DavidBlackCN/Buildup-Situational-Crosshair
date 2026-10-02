package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.crosshair.ClassicPresentation;
import dev.buildup.situationalcrosshair.semantic.*;
import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.SecondaryModifier;

/** Presentation policy sees resolved actions, never re-ranks the candidate pool. */
public final class PresentationResolver {
    private PresentationResolver() { }
    public static CrosshairPresentation resolve(CrosshairSemanticState state, CrosshairTheme theme) {
        java.util.Objects.requireNonNull(theme);
        if (state.visibility() != Visibility.SHOW) return CrosshairPresentation.unavailable(state.visibility());
        if (theme == CrosshairTheme.CLASSIC) {
            var base = ClassicPresentation.map(state);
            return base == null ? CrosshairPresentation.unavailable(Visibility.VANILLA)
                    : new CrosshairPresentation(Visibility.SHOW, base, SecondaryModifier.NONE, ActionState.NORMAL, ActionState.NORMAL);
        }
        var primary = state.primary();
        var secondary = state.secondary();
        // Classic+ keeps target-based glyphs. Ranged READY is shown on USE, not as an attack base.
        var base = switch (state.target()) {
            case MISS -> ClassicCrosshairType.DOT;
            case ENTITY -> primary.action() == ActionKind.ATTACK ? ClassicCrosshairType.ATTACK : ClassicCrosshairType.DOT;
            case BLOCK -> primary.action() == ActionKind.MINE
                    ? primary.state() == ActionState.INVALID ? ClassicCrosshairType.ERROR : ClassicCrosshairType.BLOCK
                    : ClassicCrosshairType.DOT;
        };
        var modifier = switch (secondary.action()) {
            case INTERACT -> SecondaryModifier.INTERACT;
            case USE -> SecondaryModifier.USE;
            case PLACE -> SecondaryModifier.PLACE;
            case TRANSFORM -> SecondaryModifier.TRANSFORM;
            case SPECIAL -> SecondaryModifier.SPECIAL;
            default -> SecondaryModifier.NONE;
        };
        return new CrosshairPresentation(Visibility.SHOW, base, modifier,
                primary.action() == ActionKind.NONE ? ActionState.NORMAL : primary.state(),
                modifier == SecondaryModifier.NONE ? ActionState.NORMAL : secondary.state());
    }
}
